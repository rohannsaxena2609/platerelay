package dev.platerelay.donation;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonationRepository extends JpaRepository<Donation, UUID> {
    @EntityGraph(attributePaths = "dietaryTags")
    List<Donation> findByStatusAndAvailableUntilAfter(DonationStatus status, Instant now);

    @EntityGraph(attributePaths = "dietaryTags")
    @Query("select distinct d from Donation d where d.status = :status and d.availableUntil > :now "
            + "and d.latitude between :south and :north and d.longitude between :west and :east")
    List<Donation> findCandidates(@Param("status") DonationStatus status, @Param("now") Instant now,
                                 @Param("south") double south, @Param("north") double north,
                                 @Param("west") double west, @Param("east") double east);

    @EntityGraph(attributePaths = "dietaryTags")
    @Query("select distinct d from Donation d where d.status = :status and d.availableUntil > :now "
            + "and d.latitude between :south and :north and (d.longitude >= :west or d.longitude <= :east)")
    List<Donation> findCandidatesAcrossDateLine(@Param("status") DonationStatus status, @Param("now") Instant now,
                                                @Param("south") double south, @Param("north") double north,
                                                @Param("west") double west, @Param("east") double east);

    @EntityGraph(attributePaths = "dietaryTags")
    @Query("select distinct d from Donation d where d.status = :status and d.availableUntil > :now "
            + "and d.latitude between :south and :north")
    List<Donation> findCandidatesAllLongitudes(@Param("status") DonationStatus status, @Param("now") Instant now,
                                                @Param("south") double south, @Param("north") double north);

    List<Donation> findByStatusAndAvailableUntilBefore(DonationStatus status, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Donation d where d.id = :id")
    Optional<Donation> findByIdForUpdate(@Param("id") UUID id);
}
