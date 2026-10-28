package dev.platerelay.matching;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.util.List;
import java.util.Set;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@Validated
@RestController
@RequestMapping("/api/v1/matches")
public class MatchingController {
    private final MatchingService service;

    public MatchingController(MatchingService service) {
        this.service = service;
    }

    @GetMapping
    List<MatchResponse> matches(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @RequestParam(defaultValue = "10") @DecimalMin("0.1") @DecimalMax("50") double radiusKm,
            @RequestParam(required = false) Set<String> dietaryTags) {
        return service.findMatches(latitude, longitude, radiusKm,
                dietaryTags == null ? Set.of() : dietaryTags);
    }

    @GetMapping("/recipients/{recipientId}")
    List<MatchResponse> matchesForRecipient(@PathVariable String recipientId,
            @RequestParam(defaultValue = "10") @DecimalMin("0.1") @DecimalMax("50") double radiusKm,
            @AuthenticationPrincipal Jwt jwt) {
        if (!recipientId.equals(jwt.getClaimAsString("recipient_id"))) {
            throw new AccessDeniedException("A recipient can view matches only for its own location.");
        }
        return service.findMatchesForRecipient(recipientId, radiusKm);
    }
}
