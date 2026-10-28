package dev.platerelay.claim;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ClaimController {
    private final ClaimService service;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    @PostMapping("/donations/{donationId}/claims")
    ResponseEntity<ClaimResponse> claim(@PathVariable UUID donationId,
                                       @Valid @RequestBody ClaimRequest request,
                                       @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                       @AuthenticationPrincipal Jwt jwt) {
        ClaimResponse response = service.claim(donationId, request, idempotencyKey,
                jwt.getClaimAsString("recipient_id"));
        return ResponseEntity.created(URI.create("/api/v1/claims/" + response.id())).body(response);
    }

    @PostMapping("/claims/{claimId}/complete")
    ClaimResponse complete(@PathVariable UUID claimId, @AuthenticationPrincipal Jwt jwt) {
        return service.complete(claimId, jwt.getClaimAsString("recipient_id"));
    }

    @PostMapping("/claims/{claimId}/release")
    ClaimResponse release(@PathVariable UUID claimId, @AuthenticationPrincipal Jwt jwt) {
        return service.release(claimId, jwt.getClaimAsString("recipient_id"));
    }
}
