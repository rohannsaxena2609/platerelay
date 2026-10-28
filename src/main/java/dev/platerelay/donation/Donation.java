package dev.platerelay.donation;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "donations")
public class Donation {
    @Id
    private UUID id;

    @Column(name = "donor_id", nullable = false, length = 120)
    private String donorId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodUnit unit;

    @Column(name = "available_until", nullable = false)
    private Instant availableUntil;

    @Column(name = "pickup_latitude", nullable = false)
    private double latitude;

    @Column(name = "pickup_longitude", nullable = false)
    private double longitude;

    @ElementCollection
    @BatchSize(size = 64)
    @CollectionTable(name = "donation_dietary_tags", joinColumns = @JoinColumn(name = "donation_id"))
    @Column(name = "tag", nullable = false, length = 40)
    private Set<String> dietaryTags = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Donation() {}

    public Donation(String donorId, String title, String description, int quantity, FoodUnit unit,
                    Instant availableUntil, double latitude, double longitude, Set<String> dietaryTags,
                    Instant createdAt) {
        this.id = UUID.randomUUID();
        this.donorId = donorId;
        this.title = title;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.availableUntil = availableUntil;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dietaryTags = new LinkedHashSet<>(dietaryTags);
        this.status = DonationStatus.AVAILABLE;
        this.createdAt = createdAt;
    }

    public void reserve() {
        transition(DonationStatus.AVAILABLE, DonationStatus.RESERVED);
    }

    public void release(Instant now) {
        transition(DonationStatus.RESERVED,
                availableUntil.isAfter(now) ? DonationStatus.AVAILABLE : DonationStatus.EXPIRED);
    }

    public void complete() {
        transition(DonationStatus.RESERVED, DonationStatus.COMPLETED);
    }

    public void cancel() {
        if (status != DonationStatus.AVAILABLE) {
            throw new IllegalStateException("Only an available donation can be cancelled.");
        }
        status = DonationStatus.CANCELLED;
    }

    public void expire() {
        if (status == DonationStatus.AVAILABLE) {
            status = DonationStatus.EXPIRED;
        }
    }

    private void transition(DonationStatus expected, DonationStatus next) {
        if (status != expected) {
            throw new IllegalStateException("Donation is " + status + " and cannot transition to " + next + ".");
        }
        status = next;
    }

    public UUID getId() { return id; }
    public String getDonorId() { return donorId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public FoodUnit getUnit() { return unit; }
    public Instant getAvailableUntil() { return availableUntil; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Set<String> getDietaryTags() { return Set.copyOf(dietaryTags); }
    public DonationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
