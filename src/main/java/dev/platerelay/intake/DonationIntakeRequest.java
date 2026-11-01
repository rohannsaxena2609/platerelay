package dev.platerelay.intake;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DonationIntakeRequest(@NotBlank @Size(max = 4000) String description) {}
