package dev.platerelay.intake;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/donations/intake")
public class DonationIntakeController {
    private final DonationIntakeSuggestionService service;

    public DonationIntakeController(DonationIntakeSuggestionService service) {
        this.service = service;
    }

    @PostMapping("/suggestions")
    public DonationIntakeSuggestion suggest(@Valid @RequestBody DonationIntakeRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        String donorId = jwt.getClaimAsString("donor_id");
        if (donorId == null || donorId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token has no donor identity.");
        }
        return service.suggest(donorId, request.description());
    }
}
