package dev.platerelay.claim;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    boolean existsByDonationIdAndStatus(UUID donationId, ClaimStatus status);
    Optional<Claim> findByIdempotencyKey(String idempotencyKey);
    List<Claim> findByStatusAndReservedUntilBefore(ClaimStatus status, Instant now);
    long countByRecipientIdAndReservedAtGreaterThanEqualAndStatusIn(String recipientId, Instant start,
                                                                    Collection<ClaimStatus> statuses);
}
