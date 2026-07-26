package com.nh.nsight.aicrudmeoy.web;

import com.nh.nsight.aicrudmeoy.catalog.PromptCatalogService;
import com.nh.nsight.aicrudmeoy.config.CrudMeoyProperties;
import com.nh.nsight.aicrudmeoy.domain.BusinessModuleLedger;
import com.nh.nsight.aicrudmeoy.domain.DomainLedgerService;
import com.nh.nsight.aicrudmeoy.service.CrudSessionService;
import com.nh.nsight.aicrudmeoy.service.SampleSessionSeeder;
import com.nh.nsight.aicrudmeoy.source.SourceBrowserService;
import com.nh.nsight.aicrudmeoy.store.CrudSessionEntity;
import com.nh.nsight.aicrudmeoy.store.CrudStepSessionEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CrudMeoyController {

    private final PromptCatalogService catalog;
    private final CrudSessionService sessions;
    private final DomainLedgerService domainLedger;
    private final SampleSessionSeeder sampleSeeder;
    private final SourceBrowserService sourceBrowser;
    private final CrudMeoyProperties properties;

    public CrudMeoyController(
            PromptCatalogService catalog,
            CrudSessionService sessions,
            DomainLedgerService domainLedger,
            SampleSessionSeeder sampleSeeder,
            SourceBrowserService sourceBrowser,
            CrudMeoyProperties properties) {
        this.catalog = catalog;
        this.sessions = sessions;
        this.domainLedger = domainLedger;
        this.sampleSeeder = sampleSeeder;
        this.sourceBrowser = sourceBrowser;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("application", "NSIGHT CRUD Meoy");
        body.put("version", properties.getVersion());
        body.put("sessionCount", sessions.list().size());
        body.put("domainCount", domainLedger.getRoot().getDomainCount());
        return body;
    }

    @GetMapping("/domains/summary")
    public Map<String, Object> domainSummary() {
        return domainLedger.summary();
    }

    @GetMapping("/domains")
    public Map<String, Object> domains(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "businessCode", required = false) String businessCode) {
        return domainLedger.search(q, group, status, businessCode);
    }

    @GetMapping("/domains/{businessCode}")
    public BusinessModuleLedger domainModule(@PathVariable String businessCode) {
        return domainLedger.requireModule(businessCode);
    }

    @GetMapping("/steps")
    public Map<String, Object> steps() {
        return Map.of(
                "version", catalog.getCatalog().getVersion(),
                "sourceNote", catalog.getCatalog().getSourceNote(),
                "masterId", catalog.getCatalog().getMasterId(),
                "steps", catalog.listSteps());
    }

    @GetMapping("/steps/{id}")
    public Map<String, Object> step(@PathVariable String id) {
        return catalog.stepPayload(id);
    }

    @GetMapping("/sessions")
    public Map<String, Object> listSessions() {
        return Map.of("sessions", sessions.list());
    }

    @PostMapping("/sessions")
    public ResponseEntity<CrudSessionEntity> create(@RequestBody(required = false) Map<String, Object> body) {
        String name = body == null ? null : String.valueOf(body.getOrDefault("name", ""));
        return ResponseEntity.status(HttpStatus.CREATED).body(sessions.create(name));
    }

    @GetMapping("/sessions/{id}")
    public Map<String, Object> getSession(@PathVariable String id) {
        return sessions.detail(id);
    }

    @PutMapping("/sessions/{id}")
    public CrudSessionEntity updateSession(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return sessions.rename(id, String.valueOf(body.getOrDefault("name", "")));
    }

    @DeleteMapping("/sessions/{id}")
    public Map<String, Object> deleteSession(@PathVariable String id) {
        sessions.delete(id);
        return Map.of("deleted", true);
    }

    @PostMapping("/sessions/{id}/clone-as-template")
    public ResponseEntity<CrudSessionEntity> cloneAsTemplate(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body) {
        String name = body == null ? null : String.valueOf(body.getOrDefault("name", ""));
        if ("null".equals(name)) {
            name = null;
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(sessions.cloneAsTemplate(id, name));
    }

    @PostMapping("/sessions/{id}/answers")
    public Map<String, Object> answer(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return sessions.saveAnswer(id, body);
    }

    @PostMapping("/sessions/{id}/steps/{stepId}/complete")
    public Map<String, Object> complete(@PathVariable String id, @PathVariable String stepId) {
        return sessions.completeStep(id, stepId);
    }

    @PostMapping("/sessions/{id}/gate")
    public CrudSessionEntity gate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return sessions.applyGate(id, body);
    }

    @PostMapping("/sessions/{id}/move/{stepId}")
    public CrudSessionEntity move(@PathVariable String id, @PathVariable String stepId) {
        return sessions.moveTo(id, stepId);
    }

    @GetMapping("/sessions/{id}/ledger")
    public Map<String, Object> ledger(@PathVariable String id) {
        return Map.of("ledger", sessions.ledger(id));
    }

    @GetMapping("/sessions/{id}/export.zip")
    public ResponseEntity<byte[]> export(@PathVariable String id) {
        byte[] zip = sessions.exportZip(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"crud-meoy-" + id + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    @GetMapping("/sessions/{id}/sources")
    public Map<String, Object> sources(@PathVariable String id) {
        return sourceBrowser.listSources(id);
    }

    @GetMapping("/sources/content")
    public Map<String, Object> sourceContent(@RequestParam("path") String path) {
        return sourceBrowser.readSource(path);
    }

    @GetMapping("/step-sessions")
    public Map<String, Object> stepSessions(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "stepId", required = false) String stepId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "businessCode", required = false) String businessCode) {
        return sessions.searchStepSessions(q, sessionId, stepId, status, businessCode);
    }

    @GetMapping("/step-sessions/{id}")
    public CrudStepSessionEntity stepSession(@PathVariable Long id) {
        return sessions.getStepSession(id);
    }

    @PostMapping("/samples/ln-customer-contact")
    public Map<String, Object> createSample() {
        String id = sampleSeeder.seedLnCustomerContactSample();
        return Map.of("seeded", true, "sessionId", id);
    }
}