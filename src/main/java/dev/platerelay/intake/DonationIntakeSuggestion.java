package dev.platerelay.intake;

import java.time.Instant;
import java.util.UUID;

public record DonationIntakeSuggestion(UUID id, DonationIntakeDraft suggestion, String status,
                                       boolean requiresHumanReview, Instant createdAt) {}
