package nhnis.ontology.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nhnis.ontology.dashboard.DashboardService;

/**
 * Dashboard summary + detail APIs for Architect Home stats.
 */
@RestController
@RequestMapping("/api/ontology/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    public Map<String, Object> summary() {
        return dashboard.summary();
    }

    @GetMapping("/{view}")
    public ResponseEntity<?> detail(
            @PathVariable String view,
            @RequestParam(required = false) String q) {
        try {
            return ResponseEntity.ok(dashboard.detail(view, q));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
