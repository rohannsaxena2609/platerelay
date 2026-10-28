package dev.platerelay.claim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClaimExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(ClaimExpiryJob.class);
    private final ClaimService service;

    public ClaimExpiryJob(ClaimService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${platerelay.expiry.scan-delay:60000}")
    public void expireStaleReservations() {
        int expired = service.expireStaleReservations();
        if (expired > 0) {
            log.info("Released {} stale reservations", expired);
        }
    }
}
