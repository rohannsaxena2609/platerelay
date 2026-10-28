package dev.platerelay.donation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public record DonationRequest(
        @NotBlank @Size(max = 120) String donorId,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 1000) String description,
        @Min(1) int quantity,
        @NotNull FoodUnit unit,
        @NotNull @Future Instant availableUntil,
        @NotNull @Valid PickupLocation pickup,
        @Size(max = 12) Set<@NotBlank @Size(max = 40) String> dietaryTags) {

    public record PickupLocation(
            @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") double longitude) {}
}
