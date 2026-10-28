package dev.platerelay.recipient;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recipients")
public class RecipientController {
    private final RecipientService service;

    public RecipientController(RecipientService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<RecipientResponse> register(@Valid @RequestBody RecipientRequest request) {
        RecipientResponse created = service.register(request);
        return ResponseEntity.created(URI.create("/api/v1/recipients/" + created.id())).body(created);
    }

    @GetMapping
    List<RecipientResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    RecipientResponse get(@PathVariable String id) {
        return service.get(id);
    }
}
