package dev.platerelay.intake;

public interface DonationIntakeSuggestionService {
    DonationIntakeSuggestion suggest(String donorId, String description);
}
