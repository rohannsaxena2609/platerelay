package dev.platerelay.claim;

import java.time.Instant;
import java.util.UUID;

public record ClaimResponse(UUID id, UUID donationId, String recipientId, ClaimStatus status,
                            Instant reservedAt, Instant reservedUntil, Instant completedAt) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getDonation().getId(), claim.getRecipientId(),
                claim.getStatus(), claim.getReservedAt(), claim.getReservedUntil(), claim.getCompletedAt());
    }
}
