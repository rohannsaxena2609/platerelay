package dev.platerelay.intake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.platerelay.donation.DonationRequest;
import dev.platerelay.donation.DonationResponse;
import dev.platerelay.donation.DonationService;
import dev.platerelay.donation.DonationStatus;
import dev.platerelay.donation.FoodUnit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DonationIntakeReviewServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-09T06:00:00Z");
    private static final String DONOR_ID = "donor-org-17";
    private static final UUID SUGGESTION_ID = UUID.randomUUID();

    @Mock private AiIntakeSuggestionRepository suggestions;
    @Mock private DonationService donations;

    private DonationIntakeReviewService service;
    private AiIntakeSuggestion suggestion;
    private DonationResponse donation;

    @BeforeEach
    void setUp() throws Exception {
        service = new DonationIntakeReviewService(suggestions, donations, new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        suggestion = new AiIntakeSuggestion(SUGGESTION_ID, DONOR_ID, "12 boxes of vegetables",
                "{\"title\":\"Fresh vegetables\",\"description\":\"Fresh vegetables for pickup\","
                        + "\"quantity\":12,\"unit\":\"BOX\"}",
                "test-model", "donation-intake-v1", "PENDING_REVIEW", NOW.minusSeconds(30));
        donation = new DonationResponse(UUID.randomUUID(), DONOR_ID, "Fresh vegetables",
                "Fresh vegetables for pickup", 12, FoodUnit.BOX, NOW.plusSeconds(3600),
                12.973, 77.6, Set.of("VEGAN"), DonationStatus.AVAILABLE, NOW);
        when(suggestions.findByIdForUpdate(SUGGESTION_ID)).thenReturn(Optional.of(suggestion));
    }

    @Test
    void approvalPublishesDonationAndReviewIsIdempotent() {
        when(suggestions.save(any(AiIntakeSuggestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(donations.create(any(DonationRequest.class), eq(DONOR_ID))).thenReturn(donation);
        DonationIntakeReviewRequest request = new DonationIntakeReviewRequest(
                DonationIntakeReviewRequest.Decision.APPROVE, NOW.plusSeconds(3600),
                new DonationRequest.PickupLocation(12.973, 77.6), Set.of("VEGAN"));

        DonationIntakeReviewResponse approved = service.review(SUGGESTION_ID, DONOR_ID, request);

        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(approved.donationId()).isEqualTo(donation.id());
        assertThat(approved.donation().status()).isEqualTo(DonationStatus.AVAILABLE);
        assertThat(approved.reviewedAt()).isEqualTo(NOW);
        ArgumentCaptor<DonationRequest> donationRequest = ArgumentCaptor.forClass(DonationRequest.class);
        verify(donations).create(donationRequest.capture(), eq(DONOR_ID));
        assertThat(donationRequest.getValue().title()).isEqualTo("Fresh vegetables");
        assertThat(donationRequest.getValue().quantity()).isEqualTo(12);

        when(donations.get(donation.id())).thenReturn(donation);
        DonationIntakeReviewResponse retried = service.review(SUGGESTION_ID, DONOR_ID, request);

        assertThat(retried.donationId()).isEqualTo(donation.id());
        verify(donations).create(any(DonationRequest.class), eq(DONOR_ID));
    }

    @Test
    void rejectionMarksSuggestionWithoutPublishingDonation() {
        when(suggestions.save(any(AiIntakeSuggestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DonationIntakeReviewRequest request = new DonationIntakeReviewRequest(
                DonationIntakeReviewRequest.Decision.REJECT, null, null, null);

        DonationIntakeReviewResponse rejected = service.review(SUGGESTION_ID, DONOR_ID, request);

        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(rejected.donationId()).isNull();
        assertThat(rejected.reviewedAt()).isEqualTo(NOW);
        verify(donations, never()).create(any(), any());
    }

    @Test
    void donorCannotReviewAnotherDonorsSuggestion() {
        assertThatThrownBy(() -> service.review(SUGGESTION_ID, "somebody-else",
                new DonationIntakeReviewRequest(DonationIntakeReviewRequest.Decision.REJECT,
                        null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(suggestions, never()).save(any());
        verify(donations, never()).create(any(), any());
    }

    @Test
    void approvalRequiresPickupAndExpiryAndOppositeDecisionConflicts() {
        var incomplete = new DonationIntakeReviewRequest(
                DonationIntakeReviewRequest.Decision.APPROVE, null, null, null);

        assertThatThrownBy(() -> service.review(SUGGESTION_ID, DONOR_ID, incomplete))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(suggestion.getStatus()).isEqualTo("PENDING_REVIEW");

        suggestion.review("REJECTED", NOW, null);
        assertThatThrownBy(() -> service.review(SUGGESTION_ID, DONOR_ID,
                new DonationIntakeReviewRequest(DonationIntakeReviewRequest.Decision.APPROVE,
                        NOW.plusSeconds(3600), new DonationRequest.PickupLocation(12.973, 77.6), null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
