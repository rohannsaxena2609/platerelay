package dev.platerelay.events;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class EventMetrics {
    private final MeterRegistry registry;

    public EventMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recorded(String eventType) {
        registry.counter("platerelay.domain.events.recorded", "event_type", eventType).increment();
    }

    public void published() {
        registry.counter("platerelay.outbox.published").increment();
    }

    public void failed() {
        registry.counter("platerelay.outbox.publish.failures").increment();
    }

    public void consumed() {
        registry.counter("platerelay.events.consumed").increment();
    }

    public void deduplicated() {
        registry.counter("platerelay.events.duplicate").increment();
    }
}
