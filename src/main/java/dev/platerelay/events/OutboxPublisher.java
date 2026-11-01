package dev.platerelay.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "platerelay.events.enabled", havingValue = "true")
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final Duration LEASE = Duration.ofSeconds(30);

    private final OutboxStore store;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final EventMetrics metrics;

    public OutboxPublisher(OutboxStore store, KafkaTemplate<String, String> kafka, ObjectMapper objectMapper,
                           @Value("${platerelay.events.topic:platerelay.domain-events}") String topic,
                           EventMetrics metrics) {
        this.store = store;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${platerelay.events.dispatch-delay:1000}")
    public void publishPendingEvents() {
        List<DomainEventEnvelope> events = store.leaseBatch(50, LEASE);
        for (DomainEventEnvelope event : events) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                kafka.send(topic, event.aggregateId(), payload).get(5, TimeUnit.SECONDS);
                store.published(event.id());
                metrics.published();
            } catch (Exception exception) {
                store.failed(event.id(), rootMessage(exception));
                metrics.failed();
                log.warn("Could not publish outbox event {} (attempt will be retried)", event.id(), exception);
            }
        }
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? cause.getClass().getSimpleName() : message;
    }
}
