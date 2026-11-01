package dev.platerelay.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxStore {
    private final OutboxEventRepository repository;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public OutboxStore(OutboxEventRepository repository, Clock clock, ObjectMapper objectMapper) {
        this.repository = repository;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<DomainEventEnvelope> leaseBatch(int batchSize, Duration leaseDuration) {
        Instant now = clock.instant();
        List<OutboxEvent> batch = repository.findDispatchable(now, PageRequest.of(0, batchSize));
        Instant leaseUntil = now.plus(leaseDuration);
        batch.forEach(event -> event.leaseUntil(leaseUntil));
        return batch.stream().map(event -> new DomainEventEnvelope(event.getId(), event.getEventType(),
                event.getAggregateId(), event.getOccurredAt(), readPayload(event.getPayload()))).toList();
    }

    @Transactional
    public void published(UUID eventId) {
        repository.findById(eventId).ifPresent(event -> event.markPublished(clock.instant()));
    }

    @Transactional
    public void failed(UUID eventId, String error) {
        repository.findById(eventId).ifPresent(event -> {
            int nextAttempt = event.getAttempts() + 1;
            long delaySeconds = Math.min(600, 1L << Math.min(nextAttempt, 9));
            event.markFailed(error, clock.instant().plusSeconds(delaySeconds));
        });
    }

    private JsonNode readPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Outbox payload is not valid JSON", exception);
        }
    }
}
