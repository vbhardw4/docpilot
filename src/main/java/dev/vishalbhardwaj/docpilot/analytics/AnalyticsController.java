package dev.vishalbhardwaj.docpilot.analytics;

import dev.vishalbhardwaj.docpilot.analytics.dto.AnalyticsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** Dashboard numbers: answer/escalation rates, latency, feedback, top questions. */
    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return analyticsService.snapshot();
    }
}
