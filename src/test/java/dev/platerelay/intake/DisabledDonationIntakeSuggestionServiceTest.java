package dev.platerelay.intake;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DisabledDonationIntakeSuggestionServiceTest {
    @Test
    void failsClearlyWhenOptionalAiFeatureIsDisabled() {
        assertThatThrownBy(() -> new DisabledDonationIntakeSuggestionService()
                .suggest("market-17", "boxes of fresh produce"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }
}
