package dev.platerelay.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_outbox")
public class OutboxEvent {
    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected OutboxEvent() {}

    public OutboxEvent(UUID id, String eventType, String aggregateId, String payload, Instant occurredAt) {
        this.id = id;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.availableAt = occurredAt;
    }

    public void leaseUntil(Instant leaseUntil) {
        this.lockedUntil = leaseUntil;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lockedUntil = null;
        this.lastError = null;
    }

    public void markFailed(String error, Instant retryAt) {
        this.attempts++;
        this.lastError = error == null ? "Unknown publisher error" : error.substring(0, Math.min(error.length(), 1000));
        this.availableAt = retryAt;
        this.lockedUntil = null;
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
}
