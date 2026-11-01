package dev.platerelay.events;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImpactSummaryService {
    private final ImpactEventRepository events;

    public ImpactSummaryService(ImpactEventRepository events) {
        this.events = events;
    }

    @Transactional(readOnly = true)
    public ImpactSummary summary() {
        return new ImpactSummary(
                events.countByEventType("donation.published"),
                events.countByEventType("donation.cancelled"),
                events.countByEventType("donation.expired"),
                events.countByEventType("claim.reserved"),
                events.countByEventType("claim.completed"),
                events.countByEventType("claim.released"),
                events.countByEventType("claim.expired"));
    }
}
