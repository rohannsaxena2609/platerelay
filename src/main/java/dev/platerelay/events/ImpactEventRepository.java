package dev.platerelay.events;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImpactEventRepository extends JpaRepository<ImpactEvent, UUID> {
    @Modifying
    @Query(value = "insert into impact_events(event_id, event_type, aggregate_id, occurred_at) "
            + "values (:eventId, :eventType, :aggregateId, :occurredAt) on conflict (event_id) do nothing",
            nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId, @Param("eventType") String eventType,
                       @Param("aggregateId") String aggregateId, @Param("occurredAt") Instant occurredAt);

    long countByEventType(String eventType);
}
