package nhnis.ontology.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nhnis.ontology.design.DesignWizardService;
import nhnis.ontology.query.OntologyQueryService;
import nhnis.ontology.recommend.DesignRecommendationService;
import nhnis.ontology.validate.ArchitectureRuleValidator;

/**
 * Ontology 1.0 query / impact / validate APIs.
 */
@RestController
@RequestMapping("/api/ontology")
public class OntologyCoreQueryController {

    private final OntologyQueryService queryService;
    private final ArchitectureRuleValidator ruleValidator;
    private final DesignRecommendationService designRecommendationService;
    private final DesignWizardService designWizardService;

    public OntologyCoreQueryController(
            OntologyQueryService queryService,
            ArchitectureRuleValidator ruleValidator,
            DesignRecommendationService designRecommendationService,
            DesignWizardService designWizardService) {
        this.queryService = queryService;
        this.ruleValidator = ruleValidator;
        this.designRecommendationService = designRecommendationService;
        this.designWizardService = designWizardService;
    }

    @GetMapping("/query/service/{serviceId}/structure")
    public ResponseEntity<?> serviceStructure(@PathVariable String serviceId) {
        return okOrBad(() -> queryService.serviceStructure(serviceId));
    }

    @GetMapping("/query/program/{programId}/services")
    public ResponseEntity<?> programServices(@PathVariable String programId) {
        return okOrBad(() -> queryService.programServices(programId));
    }

    @GetMapping("/query/handler/{handler}/services")
    public ResponseEntity<?> handlerServices(@PathVariable String handler) {
        return okOrBad(() -> queryService.handlerServices(handler));
    }

    @GetMapping("/query/table/{table}/services")
    public ResponseEntity<?> tableServices(@PathVariable String table) {
        return okOrBad(() -> queryService.tableServices(table));
    }

    @GetMapping("/query/service/{serviceId}/tables")
    public ResponseEntity<?> serviceTables(@PathVariable String serviceId) {
        return okOrBad(() -> queryService.serviceTables(serviceId));
    }

    @GetMapping("/query/business/{businessCode}/tree")
    public ResponseEntity<?> businessTree(@PathVariable String businessCode) {
        return okOrBad(() -> queryService.businessTree(businessCode));
    }

    @GetMapping("/query/runtime/tx-chain")
    public ResponseEntity<?> runtimeTxChain() {
        return okOrBad(queryService::runtimeTxChain);
    }

    @GetMapping("/impact/table/{tableName}")
    public ResponseEntity<?> impactTable(@PathVariable String tableName) {
        return okOrBad(() -> queryService.impactByTable(tableName));
    }

    @GetMapping("/validate/rules")
    public Map<String, Object> validateRules() {
        return ruleValidator.validateAll();
    }

    @GetMapping("/validate/service/{serviceId}")
    public ResponseEntity<?> validateService(@PathVariable String serviceId) {
        return ResponseEntity.ok(ruleValidator.validateService(serviceId));
    }

    @PostMapping("/validate/design-baseline")
    public Map<String, Object> validateDesignBaseline(
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> body) {
        return ruleValidator.validateDesignBaseline(body == null ? Map.of() : body);
    }

    @PostMapping("/validate/design")
    public Map<String, Object> validateDesign(
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> payload = body == null ? Map.of() : body;
        // Wizard design body → DesignWizard gate; legacy baseline map → ruleValidator
        if (payload.containsKey("serviceIdDesign")
                || payload.containsKey("dataDesign")
                || payload.containsKey("application")
                || "DESIGN_WIZARD".equals(String.valueOf(payload.get("scope")))) {
            return designWizardService.validateDesign(payload);
        }
        Map<String, String> flat = new java.util.LinkedHashMap<>();
        payload.forEach((k, v) -> {
            if (v != null) {
                flat.put(k, String.valueOf(v));
            }
        });
        return ruleValidator.validateDesignBaseline(flat);
    }

    @PostMapping("/design/recommend")
    public Map<String, Object> designRecommend(
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> body) {
        return designRecommendationService.recommend(body == null ? Map.of() : body);
    }

    private ResponseEntity<?> okOrBad(SupplierEx supplier) {
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
