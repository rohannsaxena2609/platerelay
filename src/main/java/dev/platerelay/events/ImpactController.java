package dev.platerelay.events;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/impact")
public class ImpactController {
    private final ImpactSummaryService service;

    public ImpactController(ImpactSummaryService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ImpactSummary summary() {
        return service.summary();
    }
}
