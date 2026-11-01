package dev.platerelay.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.platerelay.donation.Donation;
import dev.platerelay.donation.DonationRepository;
import dev.platerelay.donation.DonationRequest;
import dev.platerelay.donation.DonationResponse;
import dev.platerelay.donation.DonationService;
import dev.platerelay.donation.DonationStatus;
import dev.platerelay.donation.FoodUnit;
import dev.platerelay.recipient.RecipientRequest;
import dev.platerelay.recipient.RecipientService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class ClaimFlowIntegrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18-alpine"))
            .withDatabaseName("platerelay_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private ClaimService claims;
    @Autowired private DonationService donations;
    @Autowired private RecipientService recipients;
    @Autowired private DonationRepository donationRepository;
    @Autowired private JdbcTemplate jdbc;

    private String recipientId;

    @BeforeEach
    void createRecipient() {
        recipientId = "test-fridge-" + UUID.randomUUID();
        recipients.register(new RecipientRequest(recipientId, "Integration Test Fridge",
                12.973, 77.6, 30, Set.of("VEGAN")));
    }

    @Test
    void concurrentClaimsAllowOnlyOneReservation() throws Exception {
        DonationResponse donation = createDonation("Concurrency test");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<ClaimResponse>> attempts = List.of(
                    pool.submit(() -> concurrentClaim(donation.id(), ready, start)),
                    pool.submit(() -> concurrentClaim(donation.id(), ready, start)));
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successful = 0;
            int rejected = 0;
            for (Future<ClaimResponse> attempt : attempts) {
                try {
                    assertThat(attempt.get().status()).isEqualTo(ClaimStatus.RESERVED);
                    successful++;
                } catch (ExecutionException exception) {
                    assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
                    rejected++;
                }
            }

            assertThat(successful).isEqualTo(1);
            assertThat(rejected).isEqualTo(1);
            assertThat(donationRepository.findById(donation.id()).orElseThrow().getStatus())
                    .isEqualTo(DonationStatus.RESERVED);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void idempotentRetryReturnsOriginalClaim() {
        DonationResponse donation = createDonation("Idempotency test");
        String key = UUID.randomUUID().toString();

        ClaimResponse first = claims.claim(donation.id(), new ClaimRequest(recipientId), key, recipientId);
        ClaimResponse retry = claims.claim(donation.id(), new ClaimRequest(recipientId), key, recipientId);

        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(jdbc.queryForObject("select count(*) from claims where donation_id = ?", Integer.class,
                donation.id())).isEqualTo(1);
    }

    @Test
    void releaseRestoresDonationAvailabilityAndCompletionClosesTheFlow() {
        DonationResponse releaseDonation = createDonation("Release test");
        ClaimResponse releaseClaim = claims.claim(releaseDonation.id(), new ClaimRequest(recipientId), null,
                recipientId);

        ClaimResponse released = claims.release(releaseClaim.id(), recipientId);

        assertThat(released.status()).isEqualTo(ClaimStatus.RELEASED);
        assertThat(donationRepository.findById(releaseDonation.id()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.AVAILABLE);

        DonationResponse completionDonation = createDonation("Completion test");
        ClaimResponse completionClaim = claims.claim(completionDonation.id(), new ClaimRequest(recipientId), null,
                recipientId);
        ClaimResponse completed = claims.complete(completionClaim.id(), recipientId);

        assertThat(completed.status()).isEqualTo(ClaimStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();
        assertThat(donationRepository.findById(completionDonation.id()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.COMPLETED);
    }

    @Test
    void recipientCannotClaimForAnotherLocation() {
        DonationResponse donation = createDonation("Ownership test");

        assertThatThrownBy(() -> claims.claim(donation.id(), new ClaimRequest("another-fridge"), null,
                recipientId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void staleReservationIsReleasedByExpirySweep() {
        DonationResponse donation = createDonation("Stale claim test");
        ClaimResponse claim = claims.claim(donation.id(), new ClaimRequest(recipientId), null, recipientId);
        jdbc.update("update claims set reserved_until = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(10)), claim.id());

        assertThat(claims.expireStaleReservations()).isEqualTo(1);
        assertThat(donationRepository.findById(donation.id()).orElseThrow().getStatus())
                .isEqualTo(DonationStatus.AVAILABLE);
    }

    @Test
    void domainEventsAreWrittenWithBusinessChangesAndImpactLedgerDeduplicates() {
        DonationResponse donation = createDonation("Outbox and ledger test");

        assertThat(jdbc.queryForObject("select count(*) from event_outbox where aggregate_id = ? "
                + "and event_type = 'donation.published'", Integer.class, donation.id().toString())).isEqualTo(1);

        UUID eventId = UUID.randomUUID();
        var occurredAt = java.sql.Timestamp.from(Instant.now());
        String insert = "insert into impact_events(event_id,event_type,aggregate_id,occurred_at) "
                + "values (?,?,?,?) on conflict (event_id) do nothing";
        assertThat(jdbc.update(insert, eventId, "claim.completed", donation.id().toString(), occurredAt)).isEqualTo(1);
        assertThat(jdbc.update(insert, eventId, "claim.completed", donation.id().toString(), occurredAt)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from impact_events where event_id = ?", Integer.class,
                eventId)).isEqualTo(1);
    }


    private ClaimResponse concurrentClaim(UUID donationId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        return claims.claim(donationId, new ClaimRequest(recipientId), UUID.randomUUID().toString(), recipientId);
    }

    private DonationResponse createDonation(String title) {
        DonationRequest request = new DonationRequest("market-17", title, "Test donation", 6, FoodUnit.BOX,
                Instant.now().plusSeconds(7200),
                new DonationRequest.PickupLocation(12.9716, 77.5946), Set.of("VEGAN"));
        return donations.create(request, "market-17");
    }
}
