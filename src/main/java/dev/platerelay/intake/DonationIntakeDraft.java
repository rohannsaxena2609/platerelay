package dev.platerelay.intake;

import dev.platerelay.donation.FoodUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DonationIntakeDraft(@NotBlank @Size(max = 160) String title,
                                  @NotBlank @Size(max = 1000) String description,
                                  @NotNull @Positive Integer quantity,
                                  @NotNull FoodUnit unit) {}
