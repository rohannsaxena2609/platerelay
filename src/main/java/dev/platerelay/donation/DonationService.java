package dev.platerelay.donation;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonationService {
    private final DonationRepository donations;
    private final Clock clock;

    public DonationService(DonationRepository donations, Clock clock) {
        this.donations = donations;
        this.clock = clock;
    }

    @Transactional
    public DonationResponse create(DonationRequest request, String actorDonorId) {
        if (actorDonorId == null || !actorDonorId.equals(request.donorId())) {
            throw new AccessDeniedException("A donor can publish donations only for its own organization.");
        }
        Donation donation = new Donation(request.donorId(), request.title(), request.description(),
                request.quantity(), request.unit(), request.availableUntil(), request.pickup().latitude(),
                request.pickup().longitude(), request.dietaryTags() == null ? java.util.Set.of() : request.dietaryTags(),
                clock.instant());
        return DonationResponse.from(donations.save(donation));
    }

    @Transactional(readOnly = true)
    public DonationResponse get(UUID id) {
        return DonationResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public DonationPageResponse browse(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.asc("availableUntil"), Sort.Order.asc("id")));
        var results = donations.findByStatusAndAvailableUntilAfter(DonationStatus.AVAILABLE, clock.instant(), pageable);
        return new DonationPageResponse(results.getContent().stream().map(DonationResponse::from).toList(),
                results.getNumber(), results.getSize(), results.getTotalElements(), results.getTotalPages(),
                results.hasNext());
    }

    @Transactional
    public DonationResponse cancel(UUID id, String actorDonorId) {
        Donation donation = lock(id);
        if (actorDonorId == null || !actorDonorId.equals(donation.getDonorId())) {
            throw new AccessDeniedException("A donor can cancel only its own donation.");
        }
        donation.cancel();
        return DonationResponse.from(donation);
    }

    @Transactional
    public int expireAvailableDonations() {
        List<Donation> stale = donations.findByStatusAndAvailableUntilBefore(DonationStatus.AVAILABLE,
                clock.instant());
        stale.forEach(Donation::expire);
        return stale.size();
    }

    Donation find(UUID id) {
        return donations.findById(id).orElseThrow(() -> new EntityNotFoundException("Donation " + id + " was not found."));
    }

    public Donation lock(UUID id) {
        return donations.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Donation " + id + " was not found."));
    }

    public Instant now() {
        return clock.instant();
    }
}
