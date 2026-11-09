package dev.platerelay.intake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.platerelay.donation.DonationRequest;
import dev.platerelay.donation.DonationResponse;
import dev.platerelay.donation.DonationService;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DonationIntakeReviewService {
    private final AiIntakeSuggestionRepository suggestions;
    private final DonationService donations;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DonationIntakeReviewService(AiIntakeSuggestionRepository suggestions, DonationService donations,
            ObjectMapper objectMapper, Clock clock) {
        this.suggestions = suggestions;
        this.donations = donations;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public DonationIntakeReviewResponse review(UUID suggestionId, String actorDonorId,
            DonationIntakeReviewRequest request) {
        AiIntakeSuggestion suggestion = suggestions.findByIdForUpdate(suggestionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Donation intake suggestion " + suggestionId + " was not found."));
        if (actorDonorId == null || !actorDonorId.equals(suggestion.getDonorId())) {
            throw new AccessDeniedException("A donor can review only its own intake suggestions.");
        }

        String requestedStatus = request.decision() == DonationIntakeReviewRequest.Decision.APPROVE
                ? "APPROVED" : "REJECTED";
        if (!"PENDING_REVIEW".equals(suggestion.getStatus())) {
            if (requestedStatus.equals(suggestion.getStatus())) {
                return response(suggestion);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Suggestion has already been reviewed as " + suggestion.getStatus() + ".");
        }

        if (request.decision() == DonationIntakeReviewRequest.Decision.REJECT) {
            suggestion.review("REJECTED", clock.instant(), null);
            suggestions.save(suggestion);
            return response(suggestion);
        }

        if (request.availableUntil() == null || request.pickup() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Approval requires a future availableUntil and pickup location.");
        }
        DonationIntakeDraft draft = readDraft(suggestion);
        DonationRequest donationRequest = new DonationRequest(suggestion.getDonorId(), draft.title(),
                draft.description(), draft.quantity(), draft.unit(), request.availableUntil(), request.pickup(),
                request.dietaryTags() == null ? Set.of() : request.dietaryTags());
        DonationResponse donation = donations.create(donationRequest, actorDonorId);
        suggestion.review("APPROVED", clock.instant(), donation.id());
        suggestions.save(suggestion);
        return new DonationIntakeReviewResponse(suggestion.getId(), suggestion.getStatus(), donation.id(), donation,
                suggestion.getReviewedAt());
    }

    private DonationIntakeDraft readDraft(AiIntakeSuggestion suggestion) {
        try {
            return objectMapper.readValue(suggestion.getSuggestedPayload(), DonationIntakeDraft.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Saved AI donation draft could not be read.", exception);
        }
    }

    private DonationIntakeReviewResponse response(AiIntakeSuggestion suggestion) {
        DonationResponse donation = suggestion.getDonationId() == null ? null : donations.get(suggestion.getDonationId());
        return new DonationIntakeReviewResponse(suggestion.getId(), suggestion.getStatus(), suggestion.getDonationId(),
                donation, suggestion.getReviewedAt());
    }
}
