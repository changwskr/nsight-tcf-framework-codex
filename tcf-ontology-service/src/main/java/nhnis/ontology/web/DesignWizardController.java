package nhnis.ontology.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nhnis.ontology.design.DesignWizardService;
import nhnis.ontology.design.TableProposalService;

/**
 * Architecture Design Wizard APIs (ServiceId / Data / App / Policy / Export).
 */
@RestController
@RequestMapping("/api/ontology/design")
public class DesignWizardController {

    private final DesignWizardService wizard;
    private final TableProposalService tableProposalService;

    public DesignWizardController(DesignWizardService wizard, TableProposalService tableProposalService) {
        this.wizard = wizard;
        this.tableProposalService = tableProposalService;
    }

    @PostMapping("/session")
    public Map<String, Object> createSession(@RequestBody(required = false) Map<String, Object> body) {
        return wizard.createSession(body);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        return okOrNotFound(() -> wizard.getSession(sessionId));
    }

    @PutMapping("/session/{sessionId}")
    public ResponseEntity<?> patchSession(
            @PathVariable String sessionId,
            @RequestBody(required = false) Map<String, Object> body) {
        return okOrNotFound(() -> wizard.patchSession(sessionId, body));
    }

    @GetMapping("/sessions")
    public Map<String, Object> listSessions() {
        return wizard.listSessions();
    }

    @PostMapping("/session/{sessionId}/complete")
    public ResponseEntity<?> completeSession(
            @PathVariable String sessionId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            return ResponseEntity.ok(wizard.completeSession(sessionId, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/service-id/validate")
    public Map<String, Object> validateServiceId(@RequestBody(required = false) Map<String, Object> body) {
        return wizard.validateServiceId(body == null ? Map.of() : body);
    }

    @GetMapping("/programs")
    public Map<String, Object> listPrograms(
            @RequestParam(required = false) String system,
            @RequestParam(required = false) String business,
            @RequestParam(required = false) String function,
            @RequestParam(required = false) String functionCode) {
        String fn = (functionCode != null && !functionCode.isBlank()) ? functionCode : function;
        return wizard.listPrograms(system, business, fn);
    }

    @GetMapping("/tables")
    public Map<String, Object> searchTables(
            @RequestParam(required = false) String business,
            @RequestParam(required = false) String function,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String referenceServiceId) {
        return wizard.searchTables(business, function, keyword, referenceServiceId);
    }

    @GetMapping("/table/{tableName}")
    public ResponseEntity<?> tableDetail(@PathVariable String tableName) {
        return okOrNotFound(() -> wizard.tableDetail(tableName));
    }

    @GetMapping("/table/{tableName}/columns")
    public ResponseEntity<?> tableColumns(@PathVariable String tableName) {
        return okOrNotFound(() -> {
            Map<String, Object> detail = wizard.tableDetail(tableName);
            return Map.of(
                    "tableName", tableName,
                    "columns", detail.get("columns"),
                    "pk", ((Map<?, ?>) detail.get("table")).get("pk"));
        });
    }

    @PostMapping("/application")
    public Map<String, Object> proposeApplication(@RequestBody(required = false) Map<String, Object> body) {
        return wizard.proposeApplication(body == null ? Map.of() : body);
    }

    @PostMapping("/policy")
    public Map<String, Object> proposePolicy(@RequestBody(required = false) Map<String, Object> body) {
        return wizard.proposePolicy(body == null ? Map.of() : body);
    }

    @PostMapping("/table-proposal/validate")
    public Map<String, Object> validateTableProposal(@RequestBody(required = false) Map<String, Object> body) {
        return tableProposalService.validate(body == null ? Map.of() : body);
    }

    @PostMapping("/table-proposal")
    public Map<String, Object> createTableProposal(@RequestBody(required = false) Map<String, Object> body) {
        return tableProposalService.create(body == null ? Map.of() : body);
    }

    @GetMapping("/table-proposal/{proposalId}")
    public ResponseEntity<?> getTableProposal(@PathVariable String proposalId) {
        return okOrNotFound(() -> tableProposalService.get(proposalId));
    }

    @PutMapping("/table-proposal/{proposalId}")
    public ResponseEntity<?> updateTableProposal(
            @PathVariable String proposalId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            return ResponseEntity.ok(tableProposalService.update(proposalId, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public Map<String, Object> validateDesign(@RequestBody(required = false) Map<String, Object> body) {
        return wizard.validateDesign(body == null ? Map.of() : body);
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportBody(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(defaultValue = "markdown") String format) {
        try {
            return ResponseEntity.ok(wizard.export(body == null ? Map.of() : body, format));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/export/{sessionId}")
    public ResponseEntity<?> exportSession(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "markdown") String format) {
        try {
            Map<String, Object> session = wizard.getSession(sessionId);
            return ResponseEntity.ok(wizard.export(session, format));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<?> okOrNotFound(SupplierEx supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @FunctionalInterface
    private interface SupplierEx {
        Map<String, Object> get();
    }
}
