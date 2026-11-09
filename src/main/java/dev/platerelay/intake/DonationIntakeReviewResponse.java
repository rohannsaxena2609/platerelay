package dev.platerelay.intake;

import dev.platerelay.donation.DonationResponse;
import java.time.Instant;
import java.util.UUID;

public record DonationIntakeReviewResponse(UUID suggestionId, String status, UUID donationId,
                                           DonationResponse donation, Instant reviewedAt) {}
