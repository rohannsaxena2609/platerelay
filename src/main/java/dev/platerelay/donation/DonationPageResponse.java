package dev.platerelay.donation;

import java.util.List;

public record DonationPageResponse(List<DonationResponse> content, int page, int size,
                                  long totalElements, int totalPages, boolean hasNext) {}
