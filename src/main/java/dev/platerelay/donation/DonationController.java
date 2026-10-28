package dev.platerelay.donation;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/donations")
public class DonationController {
    private final DonationService service;

    public DonationController(DonationService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<DonationResponse> create(@Valid @RequestBody DonationRequest request,
                                            @AuthenticationPrincipal Jwt jwt) {
        DonationResponse created = service.create(request, jwt.getClaimAsString("donor_id"));
        return ResponseEntity.created(URI.create("/api/v1/donations/" + created.id())).body(created);
    }

    @GetMapping
    List<DonationResponse> browse() {
        return service.browse();
    }

    @GetMapping("/{id}")
    DonationResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    DonationResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.cancel(id, jwt.getClaimAsString("donor_id"));
    }
}
