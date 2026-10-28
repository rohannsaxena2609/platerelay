package dev.platerelay.matching;

import dev.platerelay.donation.DonationResponse;
import java.util.List;

public record MatchResponse(DonationResponse donation, double distanceKm, long minutesUntilExpiry,
                            int score, List<String> reasons) {}
