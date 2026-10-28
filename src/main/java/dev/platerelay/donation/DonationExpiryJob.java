package dev.platerelay.donation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DonationExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(DonationExpiryJob.class);
    private final DonationService service;

    public DonationExpiryJob(DonationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${platerelay.expiry.scan-delay:60000}")
    public void expireAvailableDonations() {
        int expired = service.expireAvailableDonations();
        if (expired > 0) {
            log.info("Expired {} unclaimed donations", expired);
        }
    }
}
