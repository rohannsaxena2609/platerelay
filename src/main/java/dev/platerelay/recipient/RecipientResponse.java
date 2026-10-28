package dev.platerelay.recipient;

import java.time.Instant;
import java.util.Set;

public record RecipientResponse(String id, String name, double latitude, double longitude,
                                int dailyCapacity, Set<String> dietaryTags, Instant createdAt) {
    public static RecipientResponse from(Recipient recipient) {
        return new RecipientResponse(recipient.getId(), recipient.getName(), recipient.getLatitude(),
                recipient.getLongitude(), recipient.getDailyCapacity(), recipient.getDietaryTags(),
                recipient.getCreatedAt());
    }
}
