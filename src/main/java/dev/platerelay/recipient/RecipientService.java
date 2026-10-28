package dev.platerelay.recipient;

import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipientService {
    private final RecipientRepository recipients;
    private final Clock clock;

    public RecipientService(RecipientRepository recipients, Clock clock) {
        this.recipients = recipients;
        this.clock = clock;
    }

    @Transactional
    public RecipientResponse register(RecipientRequest request) {
        Recipient recipient = new Recipient(request.id(), request.name(), request.latitude(), request.longitude(),
                request.dailyCapacity(), request.dietaryTags() == null ? java.util.Set.of() : request.dietaryTags(),
                clock.instant());
        return RecipientResponse.from(recipients.save(recipient));
    }

    @Transactional(readOnly = true)
    public List<RecipientResponse> list() {
        return recipients.findAll().stream().map(RecipientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RecipientResponse get(String id) {
        return recipients.findById(id).map(RecipientResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Recipient " + id + " was not found."));
    }
}
