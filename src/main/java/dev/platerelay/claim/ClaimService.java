package dev.platerelay.claim;

import dev.platerelay.donation.Donation;
import dev.platerelay.donation.DonationService;
import dev.platerelay.donation.DonationStatus;
import dev.platerelay.recipient.RecipientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimService {
    private static final Duration HOLD_WINDOW = Duration.ofMinutes(15);

    private final ClaimRepository claims;
    private final RecipientRepository recipients;
    private final DonationService donationService;

    public ClaimService(ClaimRepository claims, RecipientRepository recipients,
                       DonationService donationService) {
        this.claims = claims;
        this.recipients = recipients;
        this.donationService = donationService;
    }

    @Transactional
    public ClaimResponse claim(UUID donationId, ClaimRequest request, String idempotencyKey, String actorRecipientId) {
        if (actorRecipientId == null || !actorRecipientId.equals(request.recipientId())) {
            throw new AccessDeniedException("A recipient can claim food only for its own location.");
        }
        if (idempotencyKey != null && idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must be 128 characters or fewer.");
        }
        Donation donation = donationService.lock(donationId);
        if (idempotencyKey != null) {
            Claim previous = claims.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (previous != null) {
                if (!previous.getDonation().getId().equals(donationId)
                        || !previous.getRecipientId().equals(request.recipientId())) {
                    throw new IllegalStateException("This idempotency key was already used for another request.");
                }
                return ClaimResponse.from(previous);
            }
        }
        var recipient = recipients.findById(request.recipientId())
                .orElseThrow(() -> new EntityNotFoundException("Recipient " + request.recipientId() + " was not found."));
        Instant now = donationService.now();
        Instant startOfDay = LocalDate.ofInstant(now, ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        long inUse = claims.countByRecipientIdAndReservedAtGreaterThanEqualAndStatusIn(recipient.getId(), startOfDay,
                EnumSet.of(ClaimStatus.RESERVED, ClaimStatus.COMPLETED));
        if (inUse >= recipient.getDailyCapacity()) {
            throw new IllegalStateException("The recipient has reached its pickup capacity for today.");
        }
        if (!donation.getAvailableUntil().isAfter(now)) {
            throw new IllegalStateException("This donation has expired.");
        }
        if (donation.getStatus() != DonationStatus.AVAILABLE
                || claims.existsByDonation_IdAndStatus(donationId, ClaimStatus.RESERVED)) {
            throw new IllegalStateException("This donation is no longer available to claim.");
        }

        donation.reserve();
        Instant reservedUntil = now.plus(HOLD_WINDOW);
        if (donation.getAvailableUntil().isBefore(reservedUntil)) {
            reservedUntil = donation.getAvailableUntil();
        }
        Claim claim = new Claim(donation, request.recipientId(), idempotencyKey, now, reservedUntil);
        return ClaimResponse.from(claims.save(claim));
    }

    @Transactional
    public ClaimResponse complete(UUID claimId, String actorRecipientId) {
        Claim claim = claims.findById(claimId)
                .orElseThrow(() -> new EntityNotFoundException("Claim " + claimId + " was not found."));
        requireOwner(claim, actorRecipientId);
        Donation donation = donationService.lock(claim.getDonation().getId());
        Instant now = donationService.now();
        claim.complete(now);
        donation.complete();
        return ClaimResponse.from(claim);
    }

    @Transactional
    public ClaimResponse release(UUID claimId, String actorRecipientId) {
        Claim claim = claims.findById(claimId)
                .orElseThrow(() -> new EntityNotFoundException("Claim " + claimId + " was not found."));
        requireOwner(claim, actorRecipientId);
        Donation donation = donationService.lock(claim.getDonation().getId());
        claim.release();
        donation.release(donationService.now());
        return ClaimResponse.from(claim);
    }

    private void requireOwner(Claim claim, String actorRecipientId) {
        if (actorRecipientId == null || !actorRecipientId.equals(claim.getRecipientId())) {
            throw new AccessDeniedException("A recipient can manage only its own reservations.");
        }
    }

    @Transactional
    public int expireStaleReservations() {
        Instant now = donationService.now();
        List<Claim> stale = claims.findByStatusAndReservedUntilBefore(ClaimStatus.RESERVED, now);
        int expired = 0;
        for (Claim claim : stale) {
            Donation donation = donationService.lock(claim.getDonation().getId());
            if (claim.getStatus() != ClaimStatus.RESERVED || claim.getReservedUntil().isAfter(now)) {
                continue;
            }
            claim.expire();
            donation.release(now);
            expired++;
        }
        return expired;
    }
}
