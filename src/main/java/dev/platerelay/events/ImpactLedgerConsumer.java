package dev.platerelay.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "platerelay.events.enabled", havingValue = "true")
public class ImpactLedgerConsumer {
    private final ObjectMapper objectMapper;
    private final ImpactEventRepository events;
    private final EventMetrics metrics;

    public ImpactLedgerConsumer(ObjectMapper objectMapper, ImpactEventRepository events, EventMetrics metrics) {
        this.objectMapper = objectMapper;
        this.events = events;
        this.metrics = metrics;
    }

    @KafkaListener(topics = "${platerelay.events.topic:platerelay.domain-events}",
            groupId = "${spring.kafka.consumer.group-id:platerelay-impact-ledger}")
    @Transactional
    public void consume(String rawEvent) throws Exception {
        DomainEventEnvelope event = objectMapper.readValue(rawEvent, DomainEventEnvelope.class);
        int inserted = events.insertIfAbsent(event.id(), event.eventType(), event.aggregateId(), event.occurredAt());
        if (inserted == 1) {
            metrics.consumed();
        } else {
            metrics.deduplicated();
        }
    }
}
