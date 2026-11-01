package dev.platerelay.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.platerelay.donation.Donation;
import dev.platerelay.donation.DonationRepository;
import dev.platerelay.donation.DonationStatus;
import dev.platerelay.donation.FoodUnit;
import dev.platerelay.recipient.RecipientRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MatchingServiceTest {
    private final Instant now = Instant.parse("2026-11-01T12:00:00Z");
    private final DonationRepository donations = mock(DonationRepository.class);
    private final RecipientRepository recipients = mock(RecipientRepository.class);
    private final MatchingService service = new MatchingService(donations, recipients,
            Clock.fixed(now, ZoneOffset.UTC), new SimpleMeterRegistry());

    @Test
    void ranksEligibleDonationAndExplainsScore() {
        Donation donation = donation("Fresh vegetables", 12.9716, 77.5946, Set.of("VEGAN"), now.plusSeconds(3600));
        when(donations.findCandidates(eq(DonationStatus.AVAILABLE), eq(now), anyDouble(), anyDouble(),
                anyDouble(), anyDouble())).thenReturn(List.of(donation));

        List<MatchResponse> result = service.findMatches(12.973, 77.6, 10, Set.of("VEGAN"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().donation().title()).isEqualTo("Fresh vegetables");
        assertThat(result.getFirst().distanceKm()).isGreaterThan(0).isLessThan(1);
        assertThat(result.getFirst().score()).isBetween(0, 100);
        assertThat(result.getFirst().reasons()).contains("Matches requested dietary tags");
    }

    @Test
    void excludesDonationMissingRequestedDietaryTags() {
        Donation donation = donation("Milk", 12.9716, 77.5946, Set.of("DAIRY"), now.plusSeconds(3600));
        when(donations.findCandidates(eq(DonationStatus.AVAILABLE), eq(now), anyDouble(), anyDouble(),
                anyDouble(), anyDouble())).thenReturn(List.of(donation));

        List<MatchResponse> result = service.findMatches(12.973, 77.6, 10, Set.of("VEGAN"));

        assertThat(result).isEmpty();
    }

    @Test
    void computesHaversineDistanceForKnownCoordinates() {
        double distance = MatchingService.distanceKm(12.973, 77.6, 12.9716, 77.5946);

        assertThat(distance).isBetween(0.55, 0.70);
    }

    private Donation donation(String title, double latitude, double longitude, Set<String> tags, Instant expiry) {
        return new Donation("market-17", title, "Test lot", 4, FoodUnit.BOX, expiry,
                latitude, longitude, tags, now.minusSeconds(60));
    }
}
