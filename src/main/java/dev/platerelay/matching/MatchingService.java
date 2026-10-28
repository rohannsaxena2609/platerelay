package dev.platerelay.matching;

import dev.platerelay.donation.Donation;
import dev.platerelay.donation.DonationRepository;
import dev.platerelay.donation.DonationResponse;
import dev.platerelay.donation.DonationStatus;
import dev.platerelay.recipient.Recipient;
import dev.platerelay.recipient.RecipientRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchingService {
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final double URGENCY_WINDOW_MINUTES = 72 * 60;
    private final DonationRepository donations;
    private final RecipientRepository recipients;
    private final Clock clock;

    public MatchingService(DonationRepository donations, RecipientRepository recipients, Clock clock) {
        this.donations = donations;
        this.recipients = recipients;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findMatches(double latitude, double longitude, double radiusKm,
                                           Set<String> requiredTags) {
        Instant now = clock.instant();
        List<Donation> candidates = candidates(latitude, longitude, radiusKm, now);
        return candidates.stream()
                .map(donation -> score(donation, latitude, longitude, radiusKm, requiredTags, now))
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparingInt(MatchResponse::score).reversed()
                        .thenComparingLong(MatchResponse::minutesUntilExpiry))
                .limit(50)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findMatchesForRecipient(String recipientId, double radiusKm) {
        Recipient recipient = recipients.findById(recipientId)
                .orElseThrow(() -> new EntityNotFoundException("Recipient " + recipientId + " was not found."));
        return findMatches(recipient.getLatitude(), recipient.getLongitude(), radiusKm, recipient.getDietaryTags());
    }

    private List<Donation> candidates(double latitude, double longitude, double radiusKm, Instant now) {
        double latitudeRadius = radiusKm / 111.32;
        double south = Math.max(-90, latitude - latitudeRadius);
        double north = Math.min(90, latitude + latitudeRadius);
        double cosine = Math.cos(Math.toRadians(latitude));
        double longitudeRadius = cosine < 0.000001 ? 180 : Math.min(180, radiusKm / (111.32 * cosine));
        if (longitudeRadius >= 180) {
            return donations.findCandidatesAllLongitudes(DonationStatus.AVAILABLE, now, south, north);
        }
        double west = longitude - longitudeRadius;
        double east = longitude + longitudeRadius;
        if (west < -180) {
            return donations.findCandidatesAcrossDateLine(DonationStatus.AVAILABLE, now, south, north,
                    west + 360, east);
        }
        if (east > 180) {
            return donations.findCandidatesAcrossDateLine(DonationStatus.AVAILABLE, now, south, north,
                    west, east - 360);
        }
        return donations.findCandidates(DonationStatus.AVAILABLE, now, south, north, west, east);
    }

    private java.util.Optional<MatchResponse> score(Donation donation, double latitude, double longitude,
                                                     double radiusKm, Set<String> requiredTags, Instant now) {
        if (!donation.getDietaryTags().containsAll(requiredTags)) {
            return java.util.Optional.empty();
        }
        double distance = distanceKm(latitude, longitude, donation.getLatitude(), donation.getLongitude());
        if (distance > radiusKm) {
            return java.util.Optional.empty();
        }
        long minutes = Math.max(0, Duration.between(now, donation.getAvailableUntil()).toMinutes());
        double proximity = 1 - (distance / radiusKm);
        double urgency = 1 - Math.min(minutes, URGENCY_WINDOW_MINUTES) / URGENCY_WINDOW_MINUTES;
        int score = (int) Math.round((proximity * 70) + (urgency * 30));
        List<String> reasons = new ArrayList<>();
        reasons.add(String.format(java.util.Locale.ROOT, "%.1f km away", distance));
        reasons.add("Expires in " + humanDuration(minutes));
        if (!requiredTags.isEmpty()) {
            reasons.add("Matches requested dietary tags");
        }
        return java.util.Optional.of(new MatchResponse(DonationResponse.from(donation),
                Math.round(distance * 100.0) / 100.0, minutes, score, List.copyOf(reasons)));
    }

    static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String humanDuration(long minutes) {
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (hours < 24) {
            return hours + " hr " + remainingMinutes + " min";
        }
        long days = hours / 24;
        return days + " d " + (hours % 24) + " hr";
    }
}
