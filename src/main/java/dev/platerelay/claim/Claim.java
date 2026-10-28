package dev.platerelay.claim;

import dev.platerelay.donation.Donation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claims")
public class Claim {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_id", nullable = false)
    private Donation donation;

    @Column(name = "recipient_id", nullable = false, length = 120)
    private String recipientId;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "reserved_until", nullable = false)
    private Instant reservedUntil;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private long version;

    protected Claim() {}

    public Claim(Donation donation, String recipientId, String idempotencyKey,
                 Instant reservedAt, Instant reservedUntil) {
        this.id = UUID.randomUUID();
        this.donation = donation;
        this.recipientId = recipientId;
        this.idempotencyKey = idempotencyKey;
        this.status = ClaimStatus.RESERVED;
        this.reservedAt = reservedAt;
        this.reservedUntil = reservedUntil;
    }

    public void complete(Instant now) {
        requireReserved();
        if (!reservedUntil.isAfter(now)) {
            throw new IllegalStateException("The reservation window has ended.");
        }
        status = ClaimStatus.COMPLETED;
        completedAt = now;
    }

    public void release() {
        requireReserved();
        status = ClaimStatus.RELEASED;
    }

    public void expire() {
        requireReserved();
        status = ClaimStatus.EXPIRED;
    }

    private void requireReserved() {
        if (status != ClaimStatus.RESERVED) {
            throw new IllegalStateException("Claim is " + status + " and cannot be changed.");
        }
    }

    public UUID getId() { return id; }
    public Donation getDonation() { return donation; }
    public String getRecipientId() { return recipientId; }
    public ClaimStatus getStatus() { return status; }
    public Instant getReservedAt() { return reservedAt; }
    public Instant getReservedUntil() { return reservedUntil; }
    public Instant getCompletedAt() { return completedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
