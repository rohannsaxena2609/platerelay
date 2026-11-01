package dev.platerelay.events;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record DomainEventEnvelope(UUID id, String eventType, String aggregateId, Instant occurredAt,
                                 JsonNode payload) {}
