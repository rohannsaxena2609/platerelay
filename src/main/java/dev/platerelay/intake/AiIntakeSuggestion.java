package dev.platerelay.intake;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_intake_suggestions")
public class AiIntakeSuggestion {
    @Id
    private UUID id;
    @Column(name = "donor_id", nullable = false, length = 120)
    private String donorId;
    @Column(name = "input_text", nullable = false, length = 4000)
    private String inputText;
    @Column(name = "suggested_payload", nullable = false, columnDefinition = "text")
    private String suggestedPayload;
    @Column(name = "model_id", nullable = false, length = 120)
    private String modelId;
    @Column(name = "prompt_version", nullable = false, length = 40)
    private String promptVersion;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "donation_id")
    private UUID donationId;

    protected AiIntakeSuggestion() {}

    public AiIntakeSuggestion(UUID id, String donorId, String inputText, String suggestedPayload,
                              String modelId, String promptVersion, String status, Instant createdAt) {
        this.id = id;
        this.donorId = donorId;
        this.inputText = inputText;
        this.suggestedPayload = suggestedPayload;
        this.modelId = modelId;
        this.promptVersion = promptVersion;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void review(String decision, Instant reviewedAt, UUID donationId) {
        if (!"PENDING_REVIEW".equals(status)) {
            throw new IllegalStateException("Suggestion has already been reviewed.");
        }
        this.status = decision;
        this.reviewedAt = reviewedAt;
        this.donationId = donationId;
    }

    public UUID getId() { return id; }
    public String getDonorId() { return donorId; }
    public String getSuggestedPayload() { return suggestedPayload; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public UUID getDonationId() { return donationId; }
}
