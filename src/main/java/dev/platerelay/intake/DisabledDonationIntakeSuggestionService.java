package dev.platerelay.intake;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "platerelay.ai.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledDonationIntakeSuggestionService implements DonationIntakeSuggestionService {
    @Override
    public DonationIntakeSuggestion suggest(String donorId, String description) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI intake is disabled. Set PLATERELAY_AI_ENABLED=true and OPENAI_API_KEY to enable it.");
    }
}
