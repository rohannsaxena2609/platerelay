package dev.platerelay.intake;

import dev.platerelay.donation.DonationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public record DonationIntakeReviewRequest(
        @NotNull Decision decision,
        @Future Instant availableUntil,
        @Valid DonationRequest.PickupLocation pickup,
        @Size(max = 12) Set<@NotBlank @Size(max = 40) String> dietaryTags) {
    public enum Decision {
        APPROVE,
        REJECT
    }
}
