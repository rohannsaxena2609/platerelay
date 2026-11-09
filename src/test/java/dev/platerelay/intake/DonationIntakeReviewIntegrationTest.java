package dev.platerelay.intake;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.platerelay.donation.DonationRequest;
import dev.platerelay.donation.DonationStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class DonationIntakeReviewIntegrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18-alpine"))
            .withDatabaseName("platerelay_intake_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private DonationIntakeReviewService reviewService;
    @Autowired private AiIntakeSuggestionRepository suggestions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MockMvc mockMvc;

    @Test
    void approvalPersistsDonationReviewAndOutboxAtomicallyAndRetryDoesNotDuplicate() throws Exception {
        String donorId = "integration-donor-" + UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        var draft = new DonationIntakeDraft("Fresh vegetables", "Twelve boxes of fresh vegetables", 12,
                dev.platerelay.donation.FoodUnit.BOX);
        suggestions.save(new AiIntakeSuggestion(suggestionId, donorId,
                "12 boxes of fresh vegetables", objectMapper.writeValueAsString(draft),
                "test-model", "donation-intake-v1", "PENDING_REVIEW", Instant.now()));
        var request = new DonationIntakeReviewRequest(DonationIntakeReviewRequest.Decision.APPROVE,
                Instant.now().plusSeconds(3600), new DonationRequest.PickupLocation(12.973, 77.6), Set.of("VEGAN"));

        DonationIntakeReviewResponse first = reviewService.review(suggestionId, donorId, request);
        DonationIntakeReviewResponse retry = reviewService.review(suggestionId, donorId, request);

        assertThat(first.status()).isEqualTo("APPROVED");
        assertThat(first.donation()).isNotNull();
        assertThat(first.donation().status()).isEqualTo(DonationStatus.AVAILABLE);
        assertThat(retry.donationId()).isEqualTo(first.donationId());
        assertThat(suggestions.findById(suggestionId).orElseThrow().getStatus()).isEqualTo("APPROVED");
        assertThat(suggestions.findById(suggestionId).orElseThrow().getDonationId()).isEqualTo(first.donationId());
        assertThat(jdbc.queryForObject("select count(*) from donations where id = ?", Integer.class,
                first.donationId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from event_outbox where aggregate_id = ? "
                + "and event_type = 'donation.published'", Integer.class,
                first.donationId().toString())).isEqualTo(1);
    }

    @Test
    void rejectionPersistsWithoutCreatingDonation() throws Exception {
        String donorId = "integration-donor-" + UUID.randomUUID();
        UUID suggestionId = UUID.randomUUID();
        var draft = new DonationIntakeDraft("Surplus meals", "Prepared meals", 10,
                dev.platerelay.donation.FoodUnit.MEAL);
        suggestions.save(new AiIntakeSuggestion(suggestionId, donorId,
                "10 prepared meals", objectMapper.writeValueAsString(draft),
                "test-model", "donation-intake-v1", "PENDING_REVIEW", Instant.now()));

        DonationIntakeReviewResponse rejected = reviewService.review(suggestionId, donorId,
                new DonationIntakeReviewRequest(DonationIntakeReviewRequest.Decision.REJECT,
                        null, null, null));

        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(rejected.donationId()).isNull();
        assertThat(suggestions.findById(suggestionId).orElseThrow().getReviewedAt()).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from donations where donor_id = ?", Integer.class,
                donorId)).isZero();
    }

    @Test
    void donorRoleCannotReviewAnotherDonorsSuggestionThroughHttp() throws Exception {
        String ownerId = "suggestion-owner-" + UUID.randomUUID();
        UUID suggestionId = savePendingSuggestion(ownerId);

        mockMvc.perform(post("/api/v1/donations/intake/suggestions/{id}/review", suggestionId)
                        .with(jwt().jwt(token -> token.claim("donor_id", "different-donor"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DONOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\"}"))
                .andExpect(status().isForbidden());

        assertThat(suggestions.findById(suggestionId).orElseThrow().getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(jdbc.queryForObject("select count(*) from donations where donor_id = ?", Integer.class,
                ownerId)).isZero();
    }

    @Test
    void recipientRoleCannotCallDonorReviewEndpointThroughHttp() throws Exception {
        String donorId = "suggestion-owner-" + UUID.randomUUID();
        UUID suggestionId = savePendingSuggestion(donorId);

        mockMvc.perform(post("/api/v1/donations/intake/suggestions/{id}/review", suggestionId)
                        .with(jwt().jwt(token -> token.claim("donor_id", donorId))
                                .authorities(new SimpleGrantedAuthority("ROLE_RECIPIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\"}"))
                .andExpect(status().isForbidden());

        assertThat(suggestions.findById(suggestionId).orElseThrow().getStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void unauthenticatedRequestCannotReachDonorReviewEndpoint() throws Exception {
        UUID suggestionId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/donations/intake/suggestions/{id}/review", suggestionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\"}"))
                .andExpect(status().isUnauthorized());
    }

    private UUID savePendingSuggestion(String donorId) throws Exception {
        UUID suggestionId = UUID.randomUUID();
        var draft = new DonationIntakeDraft("Fresh vegetables", "Vegetables for pickup", 4,
                dev.platerelay.donation.FoodUnit.BOX);
        suggestions.save(new AiIntakeSuggestion(suggestionId, donorId,
                "Four boxes of vegetables", objectMapper.writeValueAsString(draft),
                "test-model", "donation-intake-v1", "PENDING_REVIEW", Instant.now()));
        return suggestionId;
    }
}
