package dev.platerelay.events;

public record ImpactSummary(long donationsPublished, long donationsCancelled, long donationsExpired,
                            long claimsReserved, long pickupsCompleted, long claimsReleased,
                            long claimsExpired) {}
