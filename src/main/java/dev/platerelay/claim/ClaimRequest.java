package dev.platerelay.claim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimRequest(@NotBlank @Size(max = 120) String recipientId) {}
