package nhnis.ontology.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.ontology.config.OntologyProperties;
import nhnis.ontology.evidence.OntologyEvidenceMerger;
import nhnis.ontology.facade.OntologyFacade;
import nhnis.ontology.loader.OntologyGraphBootstrap;
import nhnis.ontology.ontology.OntologyRegistry;
import nhnis.ontology.prompt.PromptContextExporter;
import nhnis.ontology.recommend.RecommendService;
import nhnis.ontology.scan.InventorySnapshot;
import nhnis.ontology.scan.PdmgInventoryScanner;
import nhnis.ontology.seed.MappingSeedGenerator;
import nhnis.ontology.store.OntologyStore;
import nhnis.ontology.validate.OntologyValidator;

@RestController
@RequestMapping("/api/ontology")
public class OntologyController {

    private final OntologyRegistry registry;
    private final OntologyFacade facade;
    private final PdmgInventoryScanner scanner;
    private final OntologyValidator validator;
    private final PromptContextExporter promptExporter;
    private final RecommendService recommendService;
    private final MappingSeedGenerator seedGenerator;
    private final OntologyProperties properties;
    private final ObjectMapper objectMapper;
    private final OntologyStore store;
    private final OntologyGraphBootstrap graphBootstrap;
    private final OntologyEvidenceMerger evidenceMerger;

    public OntologyController(
            OntologyRegistry registry,
            OntologyFacade facade,
            PdmgInventoryScanner scanner,
            OntologyValidator validator,
            PromptContextExporter promptExporter,
            RecommendService recommendService,
            MappingSeedGenerator seedGenerator,
            OntologyProperties properties,
            ObjectMapper objectMapper,
            OntologyStore store,
            OntologyGraphBootstrap graphBootstrap,
            OntologyEvidenceMerger evidenceMerger) {
        this.registry = registry;
        this.facade = facade;
        this.scanner = scanner;
        this.validator = validator;
        this.promptExporter = promptExporter;
        this.recommendService = recommendService;
        this.seedGenerator = seedGenerator;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.store = store;
        this.graphBootstrap = graphBootstrap;
        this.evidenceMerger = evidenceMerger;
    }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        return facade.catalog();
    }

    @GetMapping("/program/{programId}")
    public ResponseEntity<?> program(@PathVariable String programId) {
        return facade.program(programId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "program not found", "programId", programId)));
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<?> service(@PathVariable String serviceId) {
        return facade.service(serviceId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "service not found", "serviceId", serviceId)));
    }

    @GetMapping("/impact")
    public ResponseEntity<?> impact(@RequestParam("from") String from) {
        Map<String, Object> body = facade.impact(from);
        if ("NOT_FOUND".equals(body.get("status")) && body.get("graphImpact") == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        // table-only in graph: promote status
        if ("NOT_FOUND".equals(body.get("status")) && body.get("graphImpact") instanceof Map<?, ?> gi
                && gi.get("table") != null) {
            body.put("status", "OK");
            body.put("matchType", "table-graph");
        }
        if ("NOT_FOUND".equals(body.get("status"))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        return ResponseEntity.ok(body);
    }

    /**
     * MG → CO → A → program/service 계층 조회 (YAML path + Graph tree)
     */
    @GetMapping("/path")
    public ResponseEntity<?> path(
            @RequestParam String system,
            @RequestParam String business,
            @RequestParam String function,
            @RequestParam(required = false) String program) {
        Map<String, Object> body = facade.path(system, business, function, program);
        if ("NOT_FOUND".equals(body.get("status"))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/consistency")
    public Map<String, Object> consistency() {
        return facade.consistency();
    }

    @GetMapping("/runtime/tx-chain")
    public Map<String, Object> runtimeTxChain() {
        return facade.runtimeTxChain();
    }

    @GetMapping("/meta-model")
    public Map<String, Object> metaModel() {
        return registry.getBundle("core/meta-model.yml");
    }

    @GetMapping("/relations")
    public Map<String, Object> relations() {
        return registry.getBundle("core/relations.yml");
    }

    @PostMapping("/recommend")
    public Map<String, Object> recommend(@RequestBody(required = false) Map<String, String> body) {
        return recommendService.recommend(body == null ? Map.of() : body);
    }

    @GetMapping("/recommend")
    public Map<String, Object> recommendGet(
            @RequestParam(defaultValue = "MG") String system,
            @RequestParam(defaultValue = "CO") String business,
            @RequestParam(defaultValue = "A") String function,
            @RequestParam(defaultValue = "crud") String intent,
            @RequestParam(required = false) String like,
            @RequestParam(required = false) String dbAccess,
            @RequestParam(required = false) String paging,
            @RequestParam(required = false) String channel) {
        Map<String, String> req = new java.util.LinkedHashMap<>();
        req.put("system", system);
        req.put("business", business);
        req.put("function", function);
        req.put("intent", intent);
        if (like != null) {
            req.put("like", like);
        }
        if (dbAccess != null) {
            req.put("dbAccess", dbAccess);
        }
        if (paging != null) {
            req.put("paging", paging);
        }
        if (channel != null) {
            req.put("channel", channel);
        }
        return recommendService.recommend(req);
    }

    @GetMapping(value = "/prompt/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> promptJson(@PathVariable String id) {
        try {
            return ResponseEntity.ok(promptExporter.asJson(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping(value = "/prompt/{id}.md", produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<?> promptMarkdown(@PathVariable String id) {
        try {
            return ResponseEntity.ok()
                    .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                    .body(promptExporter.asMarkdown(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("error: " + ex.getMessage());
        }
    }

    @GetMapping("/bundle/{*path}")
    public ResponseEntity<?> bundle(@PathVariable("path") String path) {
        String key = path.startsWith("/") ? path.substring(1) : path;
        Map<String, Object> doc = registry.getBundle(key);
        if (doc.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "bundle not found", "path", key));
        }
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/reload")
    public Map<String, Object> reload() throws IOException {
        if (!properties.isAdminMutationsEnabled()) {
            return Map.of(
                    "status", "FORBIDDEN",
                    "error", "Admin mutations disabled (ontology.admin-mutations-enabled=false)");
        }
        registry.reload();
        int programs = graphBootstrap.reloadAtomic();
        Map<String, Object> out = new LinkedHashMap<>(registry.catalog());
        out.put("reload", Map.of(
                "registry", "OK",
                "graphStore", "OK",
                "mode", "ATOMIC_SWAP",
                "programsReloaded", programs,
                "conceptCount", store.allConcepts().size(),
                "relationCount", store.allRelations().size()));
        return out;
    }

    @PostMapping("/evidence/upgrade")
    public Map<String, Object> upgradeEvidence() {
        if (!properties.isAdminMutationsEnabled()) {
            return Map.of("status", "FORBIDDEN", "error", "Admin mutations disabled");
        }
        return evidenceMerger.upgradeFromFilesystem();
    }

    @PostMapping("/import/pdmg")
    public Map<String, Object> importPdmg() throws IOException {
        if (!properties.isAdminMutationsEnabled()) {
            return Map.of("status", "FORBIDDEN", "error", "Admin mutations disabled");
        }
        InventorySnapshot snapshot = scanner.scan();
        Path out = scanner.writeYaml(snapshot);
        return Map.of(
                "status", "OK",
                "output", out.toAbsolutePath().toString(),
                "programCount", snapshot.getPrograms().size(),
                "uiRouteCount", snapshot.getUiRoutes().size(),
                "notes", snapshot.getNotes());
    }

    @PostMapping("/seed/pdmg")
    public Map<String, Object> seedPdmg(
            @RequestParam(name = "overwrite", defaultValue = "false") boolean overwrite) throws IOException {
        if (!properties.isAdminMutationsEnabled()) {
            return Map.of("status", "FORBIDDEN", "error", "Admin mutations disabled");
        }
        InventorySnapshot snapshot = scanner.scan();
        scanner.writeYaml(snapshot);
        MappingSeedGenerator.SeedReport report = seedGenerator.generate(snapshot, overwrite);
        return report.toMap();
    }

    @PostMapping("/validate/pdmg")
    public ResponseEntity<Map<String, Object>> validatePdmg() throws Exception {
        Map<String, Object> report = validator.scanAndValidate();
        writeReport(report);
        HttpStatus status = "FAIL".equals(report.get("status"))
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(report);
    }

    @GetMapping("/inventory/pdmg")
    public InventorySnapshot inventoryPdmg() throws IOException {
        return scanner.scan();
    }

    private void writeReport(Map<String, Object> report) throws IOException {
        Path out = Path.of(System.getProperty("user.dir"))
                .resolve(properties.getScan().getReportOutput())
                .normalize();
        Files.createDirectories(out.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), report);
    }
}
