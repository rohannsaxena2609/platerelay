package dev.platerelay.donation;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record DonationResponse(
        UUID id,
        String donorId,
        String title,
        String description,
        int quantity,
        FoodUnit unit,
        Instant availableUntil,
        double latitude,
        double longitude,
        Set<String> dietaryTags,
        DonationStatus status,
        Instant createdAt) {

    public static DonationResponse from(Donation donation) {
        return new DonationResponse(donation.getId(), donation.getDonorId(), donation.getTitle(),
                donation.getDescription(), donation.getQuantity(), donation.getUnit(), donation.getAvailableUntil(),
                donation.getLatitude(), donation.getLongitude(), donation.getDietaryTags(), donation.getStatus(),
                donation.getCreatedAt());
    }
}
