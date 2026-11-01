package dev.platerelay.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DomainEventRecorder {
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final EventMetrics metrics;

    public DomainEventRecorder(OutboxEventRepository outbox, ObjectMapper objectMapper, Clock clock,
                               EventMetrics metrics) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.metrics = metrics;
    }

    public UUID record(String eventType, String aggregateId, Object payload) {
        try {
            UUID eventId = UUID.randomUUID();
            outbox.save(new OutboxEvent(eventId, eventType, aggregateId,
                    objectMapper.writeValueAsString(payload), clock.instant()));
            metrics.recorded(eventType);
            return eventId;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize domain event " + eventType, exception);
        }
    }
}
