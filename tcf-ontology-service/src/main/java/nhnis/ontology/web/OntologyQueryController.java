package nhnis.ontology.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nhnis.ontology.query.OntologyQueryService;

/**
 * Ontology 1.0 Concept/Relation query API.
 */
@RestController
@RequestMapping("/api/ontology/v1")
public class OntologyQueryController {

    private final OntologyQueryService queryService;

    public OntologyQueryController(OntologyQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        return queryService.meta();
    }

    @GetMapping("/concepts")
    public Map<String, Object> concepts(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String type,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q) {
        return queryService.listConcepts(type, q);
    }

    @GetMapping("/snapshot")
    public Map<String, Object> snapshot() {
        return queryService.snapshot();
    }

    @GetMapping("/concept/{id:.+}")
    public ResponseEntity<?> concept(@PathVariable String id) {
        try {
            return ResponseEntity.ok(queryService.getConcept(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/service-id/{serviceId}")
    public ResponseEntity<?> parseServiceId(@PathVariable String serviceId) {
        try {
            return ResponseEntity.ok(queryService.parseServiceId(serviceId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/chain/{serviceId}")
    public ResponseEntity<?> chain(@PathVariable String serviceId) {
        try {
            return ResponseEntity.ok(queryService.chainForService(serviceId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/impact/table/{table}")
    public Map<String, Object> impactTable(@PathVariable String table) {
        return queryService.impactByTable(table);
    }
}
