package dev.platerelay.recipient;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "recipients")
public class Recipient {
    @Id
    @Column(length = 120)
    private String id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "daily_capacity", nullable = false)
    private int dailyCapacity;

    @ElementCollection
    @CollectionTable(name = "recipient_dietary_tags", joinColumns = @JoinColumn(name = "recipient_id"))
    @Column(name = "tag", nullable = false, length = 40)
    private Set<String> dietaryTags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Recipient() {}

    public Recipient(String id, String name, double latitude, double longitude, int dailyCapacity,
                     Set<String> dietaryTags, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dailyCapacity = dailyCapacity;
        this.dietaryTags = new LinkedHashSet<>(dietaryTags);
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getDailyCapacity() { return dailyCapacity; }
    public Set<String> getDietaryTags() { return Set.copyOf(dietaryTags); }
    public Instant getCreatedAt() { return createdAt; }
}
