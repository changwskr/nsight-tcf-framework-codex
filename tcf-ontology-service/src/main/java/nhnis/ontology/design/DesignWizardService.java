package nhnis.ontology.design;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.ontology.OntologyRegistry;
import nhnis.ontology.query.OntologyQueryService;
import nhnis.ontology.recommend.DesignRecommendationService;
import nhnis.ontology.store.OntologyStore;
import nhnis.ontology.support.ServiceIdParser;

/**
 * Architecture Design Wizard use cases (ServiceId / Data / App / Policy / Gate / Export).
 */
@Service
public class DesignWizardService {

    private final OntologyRegistry registry;
    private final OntologyStore store;
    private final OntologyQueryService queryService;
    private final DesignRecommendationService recommendationService;
    private final ConcurrentHashMap<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    public DesignWizardService(
            OntologyRegistry registry,
            OntologyStore store,
            OntologyQueryService queryService,
            DesignRecommendationService recommendationService) {
        this.registry = registry;
        this.store = store;
        this.queryService = queryService;
        this.recommendationService = recommendationService;
    }

    public Map<String, Object> createSession(Map<String, Object> seed) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("sessionId", id);
        session.put("status", "DRAFT");
        session.put("step", 1);
        session.put("requirement", seed == null ? Map.of() : new LinkedHashMap<>(seed));
        session.put("classification", Map.of());
        session.put("serviceIdDesign", Map.of());
        session.put("dataDesign", Map.of());
        session.put("application", Map.of());
        session.put("policy", Map.of());
        session.put("gate", Map.of());
        sessions.put(id, session);
        return session;
    }

    public Map<String, Object> getSession(String sessionId) {
        Map<String, Object> s = sessions.get(sessionId);
        if (s == null) {
            throw new IllegalArgumentException("session not found: " + sessionId);
        }
        return s;
    }

    public Map<String, Object> patchSession(String sessionId, Map<String, Object> patch) {
        Map<String, Object> s = getSession(sessionId);
        if (patch == null) {
            return s;
        }
        patch.forEach((k, v) -> {
            if (!"sessionId".equals(k) && v != null) {
                s.put(k, v);
            }
        });
        if (!"COMPLETED".equals(String.valueOf(s.get("status")))) {
            s.put("status", "IN_PROGRESS");
        }
        s.put("updatedAt", java.time.Instant.now().toString());
        return s;
    }

    public Map<String, Object> completeSession(String sessionId, Map<String, Object> body) {
        Map<String, Object> s = patchSession(sessionId, body);
        Map<?, ?> gate = asMap(s.get("gate"));
        Object gateStatusObj = gate.get("status");
        String gateStatus = gateStatusObj == null ? "" : String.valueOf(gateStatusObj);
        if (gate.isEmpty() || gateStatus.isBlank() || "null".equalsIgnoreCase(gateStatus)) {
            Map<String, Object> validated = validateDesign(s);
            s.put("gate", validated);
            gateStatus = String.valueOf(validated.getOrDefault("status", ""));
        }
        if ("FAIL".equalsIgnoreCase(gateStatus)) {
            throw new IllegalStateException("Gate FAIL — cannot complete design session");
        }
        s.put("status", "COMPLETED");
        s.put("step", 7);
        s.put("completedAt", java.time.Instant.now().toString());
        return s;
    }

    public Map<String, Object> listSessions() {
        List<Map<String, Object>> items = sessions.values().stream()
                .map(this::sessionSummary)
                .sorted(Comparator
                        .comparing((Map<String, Object> m) -> String.valueOf(m.getOrDefault("completedAt", "")))
                        .reversed()
                        .thenComparing(m -> String.valueOf(m.get("sessionId"))))
                .collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", items.size());
        out.put("completedCount", items.stream().filter(m -> "COMPLETED".equals(m.get("status"))).count());
        out.put("sessions", items);
        return out;
    }

    /** Completed wizard designs for Dashboard (not Ontology VERIFIED). */
    public List<Map<String, Object>> listCompletedDesignArtifacts() {
        List<Map<String, Object>> artifacts = new ArrayList<>();
        for (Map<String, Object> session : sessions.values()) {
            if (!"COMPLETED".equals(String.valueOf(session.get("status")))) {
                continue;
            }
            artifacts.addAll(artifactsFromSession(session));
        }
        return artifacts;
    }

    private Map<String, Object> sessionSummary(Map<String, Object> session) {
        Map<?, ?> req = asMap(session.get("requirement"));
        Map<?, ?> cls = asMap(session.get("classification"));
        Map<?, ?> sid = asMap(session.get("serviceIdDesign"));
        Map<?, ?> data = asMap(session.get("dataDesign"));
        Map<?, ?> gate = asMap(session.get("gate"));
        List<?> proposals = data.get("tableProposals") instanceof List<?> pl ? pl : List.of();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sessionId", session.get("sessionId"));
        row.put("status", session.getOrDefault("status", "DRAFT"));
        row.put("step", session.get("step"));
        row.put("title", nullTo(req.get("title"), req.get("requirement"), "(untitled)"));
        row.put("system", cls.get("system"));
        row.put("business", cls.get("business"));
        row.put("function", cls.get("function"));
        row.put("programId", sid.get("programId"));
        row.put("serviceId", sid.get("serviceId"));
        row.put("gateStatus", gate.get("status"));
        row.put("tableProposalCount", proposals.size());
        row.put("completedAt", session.get("completedAt"));
        row.put("updatedAt", session.get("updatedAt"));
        row.put("source", "DESIGN_WIZARD");
        row.put("verificationStatus", "PROPOSED");
        return row;
    }

    private List<Map<String, Object>> artifactsFromSession(Map<String, Object> session) {
        List<Map<String, Object>> out = new ArrayList<>();
        String sessionId = String.valueOf(session.get("sessionId"));
        Map<?, ?> req = asMap(session.get("requirement"));
        Map<?, ?> cls = asMap(session.get("classification"));
        Map<?, ?> sid = asMap(session.get("serviceIdDesign"));
        Map<?, ?> app = asMap(session.get("application"));
        Map<?, ?> data = asMap(session.get("dataDesign"));
        Map<?, ?> gate = asMap(session.get("gate"));
        String title = String.valueOf(nullTo(req.get("title"), "(untitled)"));

        Map<String, Object> sessionRow = sessionSummary(session);
        sessionRow.put("kind", "SESSION");
        sessionRow.put("name", title);
        out.add(sessionRow);

        Object programId = sid.get("programId");
        if (programId != null && !String.valueOf(programId).isBlank()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("kind", "PROGRAM");
            p.put("id", "design:" + sessionId + ":program:" + programId);
            p.put("name", programId);
            p.put("programId", programId);
            p.put("title", title);
            p.put("businessCode", cls.get("business"));
            p.put("functionCode", cls.get("function"));
            p.put("majorGroup", cls.get("system"));
            p.put("packageRoot", cls.get("packageRoot"));
            p.put("serviceId", sid.get("serviceId"));
            p.put("services", sid.get("serviceId") == null ? List.of() : List.of(String.valueOf(sid.get("serviceId"))));
            p.put("serviceCount", sid.get("serviceId") == null ? 0 : 1);
            p.put("table", firstProposedTableName(data));
            p.put("sessionId", sessionId);
            p.put("source", "DESIGN_WIZARD");
            p.put("verificationStatus", "PROPOSED");
            p.put("gateStatus", gate.get("status"));
            out.add(p);
        }

        Object serviceId = sid.get("serviceId");
        if (serviceId != null && !String.valueOf(serviceId).isBlank()) {
            Map<?, ?> comps = asMap(app.get("components"));
            if (comps.isEmpty()) {
                comps = asMap(sid.get("components"));
            }
            Map<String, Object> svc = new LinkedHashMap<>();
            svc.put("kind", "SERVICE_ID");
            svc.put("id", "design:" + sessionId + ":service:" + serviceId);
            svc.put("name", serviceId);
            svc.put("serviceId", serviceId);
            svc.put("programId", programId);
            svc.put("op", sid.get("operation"));
            svc.put("sequence", sid.get("sequence"));
            svc.put("handler", comps.get("handler"));
            svc.put("handlers", comps.get("handler") == null ? List.of() : List.of(String.valueOf(comps.get("handler"))));
            svc.put("programs", programId == null ? List.of() : List.of(String.valueOf(programId)));
            svc.put("programTitle", title);
            svc.put("packageRoot", cls.get("packageRoot"));
            svc.put("sessionId", sessionId);
            svc.put("source", "DESIGN_WIZARD");
            svc.put("verificationStatus", "PROPOSED");
            svc.put("gateStatus", gate.get("status"));
            out.add(svc);
        }

        if (data.get("tableProposals") instanceof List<?> proposals) {
            for (Object item : proposals) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> t = new LinkedHashMap<>();
                raw.forEach((k, v) -> t.put(String.valueOf(k), v));
                String physical = String.valueOf(t.getOrDefault("physicalName", t.get("logicalName")));
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("kind", "TABLE_PROPOSAL");
                row.put("id", "design:" + sessionId + ":table:" + physical);
                row.put("name", physical);
                row.put("tableName", physical);
                row.put("logicalName", t.get("logicalName"));
                row.put("physicalName", physical);
                row.put("columnCount", t.get("columns") instanceof List<?> c ? c.size() : 0);
                row.put("primaryKey", t.get("primaryKey"));
                row.put("accessType", t.get("accessType"));
                row.put("sessionId", sessionId);
                row.put("serviceId", serviceId);
                row.put("source", "DESIGN_WIZARD");
                row.put("mode", "NEW_TABLE_PROPOSAL");
                row.put("verificationStatus", "PROPOSED");
                row.put("status", t.getOrDefault("status", "PROPOSED"));
                row.put("proposal", t);
                out.add(row);
            }
        }
        return out;
    }

    private static String firstProposedTableName(Map<?, ?> data) {
        if (data.get("tableProposals") instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> m) {
            Object n = m.get("physicalName");
            return n == null ? "UNRESOLVED" : String.valueOf(n);
        }
        if (data.get("selectedTables") instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> m) {
            Object n = m.get("tableName");
            return n == null ? "UNRESOLVED" : String.valueOf(n);
        }
        return "UNRESOLVED";
    }

    public Map<String, Object> listPrograms(String system, String business, String function) {
        String sys = defaultVal(system, "MG").toUpperCase(Locale.ROOT);
        String biz = defaultVal(business, "CO").toUpperCase(Locale.ROOT);
        String fn = defaultVal(function, "A").toUpperCase(Locale.ROOT);
        List<Map<String, Object>> programs = new ArrayList<>();
        List<String> usedNos = new ArrayList<>();
        for (Map<String, Object> p : registry.listPrograms()) {
            if (!equalsIgnore(sys, p.get("majorGroup"))
                    || !equalsIgnore(biz, p.get("businessCode"))
                    || !equalsIgnore(fn, p.get("functionCode"))) {
                continue;
            }
            String programId = String.valueOf(p.get("programId"));
            List<Map<String, Object>> services = new ArrayList<>();
            if (p.get("services") instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> svc) {
                        Map<String, Object> svcRow = new LinkedHashMap<>();
                        svcRow.put("serviceId", String.valueOf(svc.get("serviceId")));
                        svcRow.put("op", String.valueOf(svc.get("op")));
                        Object method = svc.get("method");
                        svcRow.put("method", method == null ? "" : String.valueOf(method));
                        services.add(svcRow);
                    }
                }
            }
            if (programId.length() >= 9) {
                usedNos.add(programId.substring(5, 9));
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("programId", programId);
            row.put("title", p.get("title"));
            row.put("packageRoot", p.get("packageRoot"));
            row.put("services", services);
            Object data = p.get("data");
            if (data instanceof Map<?, ?> d) {
                row.put("table", d.get("table"));
                row.put("tables", d.get("tables"));
                row.put("pk", d.get("pk"));
            }
            programs.add(row);
        }
        programs.sort(Comparator.comparing(m -> String.valueOf(m.get("programId"))));
        List<String> proposed = proposeProgramNos(usedNos);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("system", sys);
        out.put("business", biz);
        out.put("function", fn);
        out.put("programs", programs);
        out.put("usedProgramNos", usedNos.stream().distinct().sorted().toList());
        out.put("proposedProgramNos", proposed);
        out.put("note", "Program No is PROPOSED only — Architect confirms");
        return out;
    }

    public Map<String, Object> validateServiceId(Map<String, Object> raw) {
        String system = defaultVal(str(raw.get("system")), "MG").toLowerCase(Locale.ROOT);
        String business = defaultVal(str(raw.get("business")), "CO").toLowerCase(Locale.ROOT);
        String function = defaultVal(str(raw.get("function")), "A").toLowerCase(Locale.ROOT);
        String programNo = defaultVal(str(raw.get("programNo")), "").trim();
        String tx = defaultVal(str(raw.get("transactionType")), "QUERY").toUpperCase(Locale.ROOT);
        String sequence = defaultVal(str(raw.get("sequence")), "0").toUpperCase(Locale.ROOT);
        String op = DesignRecommendationService.operationCode(tx);

        List<Map<String, Object>> findings = new ArrayList<>();
        boolean available = true;

        if (!programNo.matches("\\d{4}")) {
            findings.add(finding("SID-005", "FAIL", "programNo", "Program No must be 4 digits"));
            available = false;
        } else {
            findings.add(finding("SID-005", "PASS", "programNo", "Program No format OK"));
        }

        String programId = system + business + function + programNo;
        String serviceId = programId + op + sequence;

        if (ServiceIdParser.isValid(serviceId)) {
            findings.add(finding("SID-001", "PASS", serviceId, "ServiceId format OK"));
        } else {
            findings.add(finding("SID-001", "FAIL", serviceId, "ServiceId format invalid"));
            available = false;
        }

        boolean programExists = registry.findProgram(programId).isPresent()
                || store.findConcept(programId).isPresent();
        if (programExists) {
            findings.add(finding("SID-006", "FAIL", programId, "Program ID already used"));
            available = false;
        } else {
            findings.add(finding("SID-006", "PASS", programId, "Program ID unused"));
        }

        boolean serviceExists = registry.findByServiceId(serviceId).isPresent()
                || store.findConcept(serviceId).isPresent()
                || store.findConcept("service:" + serviceId).isPresent();
        if (serviceExists) {
            findings.add(finding("SID-008", "FAIL", serviceId, "ServiceId already used"));
            available = false;
        } else {
            findings.add(finding("SID-008", "PASS", serviceId, "ServiceId unused"));
        }

        findings.add(finding("SID-007", "PASS", op, "Transaction " + tx + " → Operation " + op));
        findings.add(finding("SID-002", "PASS", system, "System code present"));
        findings.add(finding("SID-003", "PASS", business, "Business code present"));
        findings.add(finding("SID-004", "PASS", function, "Function code present"));
        findings.add(finding("SID-009", "PASS", programId + "/" + serviceId, "Program-ServiceId consistency"));
        findings.add(finding("SID-010", "PASS", sequence, "Sequence accepted (Architect confirms uniqueness in program)"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("programId", programId);
        out.put("serviceId", serviceId);
        out.put("operation", op);
        out.put("sequence", sequence);
        out.put("transactionType", tx);
        out.put("available", available);
        out.put("findings", findings);
        out.put("status", available ? "PROPOSED" : "REJECTED");
        out.put("packageRoot", "nhnis." + system + "." + business + "." + function);
        out.put("components", proposeComponentNames(programId));
        return out;
    }

    public Map<String, Object> searchTables(
            String business, String function, String keyword, String referenceServiceId) {
        String kw = keyword == null ? "" : keyword.trim().toUpperCase(Locale.ROOT);
        List<Map<String, Object>> tables = new ArrayList<>();

        if (referenceServiceId != null && !referenceServiceId.isBlank()) {
            try {
                Map<String, Object> structure = queryService.serviceStructure(referenceServiceId);
                Object t = structure.get("tables");
                if (t instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> m && m.get("name") != null) {
                            String name = String.valueOf(m.get("name"));
                            if (kw.isBlank() || name.toUpperCase(Locale.ROOT).contains(kw)) {
                                tables.add(tableSummary(name, "REFERENCE_SERVICE", referenceServiceId));
                            }
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // continue with catalog tables
            }
        }

        for (OntologyConcept c : store.findConceptsByType(ConceptType.TABLE)) {
            String name = c.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            if (!kw.isBlank() && !name.toUpperCase(Locale.ROOT).contains(kw)) {
                continue;
            }
            boolean already = tables.stream().anyMatch(t -> name.equalsIgnoreCase(String.valueOf(t.get("tableName"))));
            if (already) {
                continue;
            }
            // optional business/function filter via used programs is soft: include all matching keyword
            tables.add(tableSummary(name, "ONTOLOGY", null));
        }

        // soft filter by business/function programs' tables when no keyword
        if (kw.isBlank() && (business != null || function != null)) {
            Map<String, Object> prog = listPrograms("MG", business, function);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> programs = (List<Map<String, Object>>) prog.get("programs");
            for (Map<String, Object> p : programs) {
                addTableFromProgram(tables, p);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tables", tables);
        out.put("count", tables.size());
        out.put("note", "Only Ontology-known tables. Missing tables → NEW_TABLE_PROPOSAL in UI");
        return out;
    }

    public Map<String, Object> tableDetail(String tableName) {
        OntologyConcept table = store.findConceptOfType(tableName, ConceptType.TABLE)
                .orElseThrow(() -> new IllegalArgumentException("table not in Ontology: " + tableName));
        List<Map<String, Object>> columns = new ArrayList<>();
        for (var rel : store.findRelations(table.getId(), RelationType.HAS_COLUMN)) {
            store.findConcept(rel.getToId()).ifPresent(col -> {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("column", col.getName());
                c.put("id", col.getId());
                c.put("personalData", col.attr("personalData") == null ? "UNRESOLVED" : col.attr("personalData"));
                c.put("encryption", col.attr("encryption") == null ? "UNRESOLVED" : col.attr("encryption"));
                c.put("masking", col.attr("masking") == null ? "UNRESOLVED" : col.attr("masking"));
                c.put("evidence", col.getProvenance() == null ? Map.of() : col.getProvenance().toMap());
                columns.add(c);
            });
        }
        List<Map<String, Object>> usedBy = new ArrayList<>();
        for (Map<String, Object> p : registry.listPrograms()) {
            Object data = p.get("data");
            if (!(data instanceof Map<?, ?> d)) {
                continue;
            }
            boolean hit = false;
            if (tableName.equalsIgnoreCase(String.valueOf(d.get("table")))) {
                hit = true;
            }
            if (d.get("tables") instanceof List<?> list) {
                for (Object t : list) {
                    if (tableName.equalsIgnoreCase(String.valueOf(t))) {
                        hit = true;
                    }
                }
            }
            if (hit) {
                usedBy.add(Map.of(
                        "programId", String.valueOf(p.get("programId")),
                        "title", String.valueOf(p.getOrDefault("title", ""))));
            }
        }
        Object pk = table.attr("pk");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("table", Map.of(
                "tableName", table.getName(),
                "id", table.getId(),
                "pk", pk == null ? "UNRESOLVED" : pk,
                "provenance", table.getProvenance() == null ? Map.of() : table.getProvenance().toMap()));
        out.put("columns", columns);
        out.put("usedBy", usedBy);
        out.put("evidence", table.getProvenance() == null ? List.of() : List.of(table.getProvenance().toMap()));
        return out;
    }

    public Map<String, Object> proposeApplication(Map<String, Object> raw) {
        String programId = defaultVal(str(raw.get("programId")), "");
        String serviceId = defaultVal(str(raw.get("serviceId")), "");
        String referenceServiceId = defaultVal(str(raw.get("referenceServiceId")), "");
        if (programId.isBlank() && serviceId.length() >= 9) {
            programId = serviceId.substring(0, 9);
        }
        Map<String, Object> components = proposeComponentNames(programId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("programId", programId);
        out.put("serviceId", serviceId);
        out.put("referenceServiceId", referenceServiceId);
        out.put("packageRoot", packageRootFromProgram(programId));
        out.put("components", components);
        out.put("rule", Map.of(
                "needed", "UNKNOWN",
                "name", programId.isBlank() ? "UNRESOLVED" : programId + "Rule",
                "status", "NOT_APPLICABLE"));
        out.put("layers", List.of(
                components.get("handler"),
                components.get("facade"),
                components.get("service"),
                components.get("dao"),
                components.get("mapper")));
        out.put("status", "PROPOSED");
        out.put("note", "Naming follows PDMG convention — Architect confirms");
        return out;
    }

    public Map<String, Object> proposePolicy(Map<String, Object> raw) {
        String paging = defaultVal(str(raw.get("paging")), "UNKNOWN").toUpperCase(Locale.ROOT);
        String timeout = defaultVal(str(raw.get("timeoutPolicy")), "DEFAULT");
        String personalData = defaultVal(str(raw.get("personalData")), "UNKNOWN");
        String pagingKey = defaultVal(str(raw.get("pagingKey")), "UNRESOLVED");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", Map.of(
                "request", evidence("hdr_nhnis + dto", "UNRESOLVED"),
                "success", evidence("hdr_nhnis + dto", "UNRESOLVED"),
                "failure", evidence("hdr_nhnis + result", "UNRESOLVED")));
        out.put("transaction", Map.of(
                "tcf", evidence("ON", "DERIVED"),
                "owner", evidence("TimeoutExecutor", "UNRESOLVED")));
        out.put("timeout", evidence(timeout, "REQUESTED"));
        out.put("paging", Map.of(
                "enabled", evidence(paging, "REQUESTED"),
                "type", evidence("UNRESOLVED", "UNRESOLVED"),
                "key", evidence(pagingKey, "UNRESOLVED".equals(pagingKey) ? "UNRESOLVED" : "REQUESTED")));
        out.put("security", Map.of(
                "personalData", evidence(personalData, "REQUESTED"),
                "masking", evidence("UNRESOLVED", "UNRESOLVED"),
                "encryption", evidence("UNRESOLVED", "UNRESOLVED")));
        out.put("logging", evidence("UNRESOLVED", "UNRESOLVED"));
        out.put("audit", evidence("UNRESOLVED", "UNRESOLVED"));
        out.put("status", "PROPOSED");
        return out;
    }

    public Map<String, Object> validateDesign(Map<String, Object> design) {
        List<Map<String, Object>> findings = new ArrayList<>();
        long fail = 0;
        long unresolved = 0;

        Object sid = deep(design, "serviceIdDesign", "serviceId");
        Object available = deep(design, "serviceIdDesign", "available");
        if (sid == null || String.valueOf(sid).isBlank() || "UNRESOLVED".equalsIgnoreCase(String.valueOf(sid))) {
            findings.add(finding("SID-001", "UNRESOLVED", "serviceId", "ServiceId not confirmed"));
            unresolved++;
        } else if (Boolean.FALSE.equals(available)) {
            findings.add(finding("SID-008", "FAIL", String.valueOf(sid), "ServiceId not available"));
            fail++;
        } else {
            findings.add(finding("SID-001", "PASS", String.valueOf(sid), "ServiceId designed"));
        }

        Object tables = deep(design, "dataDesign", "selectedTables");
        Object proposals = deep(design, "dataDesign", "tableProposals");
        boolean hasSelected = tables instanceof List<?> list && !list.isEmpty();
        boolean hasProposal = proposals instanceof List<?> pl && !pl.isEmpty();
        boolean unresolvedKeep = Boolean.TRUE.equals(deep(design, "dataDesign", "tableUnresolved"));

        if (!hasSelected && !hasProposal && !unresolvedKeep) {
            // legacy string newTableProposal
            Object legacy = deep(design, "dataDesign", "newTableProposal");
            if (legacy != null && !String.valueOf(legacy).isBlank()) {
                hasProposal = true;
            }
        }

        if (!hasSelected && !hasProposal) {
            if (unresolvedKeep) {
                findings.add(finding("DATA-001", "UNRESOLVED", "tables", "Table kept as UNRESOLVED"));
                unresolved++;
            } else {
                findings.add(finding("DATA-001", "UNRESOLVED", "tables", "No table selected / no NEW_TABLE_PROPOSAL"));
                unresolved++;
            }
        } else {
            if (hasSelected) {
                findings.add(finding("DATA-001", "PASS", "tables", "Tables selected"));
                @SuppressWarnings("unchecked")
                List<?> list = (List<?>) tables;
                for (Object t : list) {
                    if (t instanceof Map<?, ?> m) {
                        if (m.get("accessType") == null || String.valueOf(m.get("accessType")).isBlank()) {
                            findings.add(finding("DATA-002", "UNRESOLVED", String.valueOf(m.get("tableName")),
                                    "Access Type missing"));
                            unresolved++;
                        }
                        if (m.get("primaryKey") == null || "UNRESOLVED".equalsIgnoreCase(String.valueOf(m.get("primaryKey")))) {
                            findings.add(finding("DATA-003", "UNRESOLVED", String.valueOf(m.get("tableName")),
                                    "PK unresolved"));
                            unresolved++;
                        }
                    }
                }
            }
            if (hasProposal && proposals instanceof List<?> pl) {
                for (Object p : pl) {
                    if (p instanceof Map<?, ?> m) {
                        Object statusObj = m.get("status");
                        String status = statusObj == null ? "PROPOSED" : String.valueOf(statusObj);
                        Object nameObj = m.get("physicalName");
                        if (nameObj == null) {
                            nameObj = m.get("tableName");
                        }
                        String name = nameObj == null ? "proposal" : String.valueOf(nameObj);
                        if ("VERIFIED".equalsIgnoreCase(status)) {
                            findings.add(finding("DATA-TBL-PROPOSAL", "FAIL", name,
                                    "NEW_TABLE_PROPOSAL must not be VERIFIED"));
                            fail++;
                        } else {
                            findings.add(finding("DATA-TBL-PROPOSAL", "PASS_WITH_UNRESOLVED", name,
                                    "NEW_TABLE_PROPOSAL status=" + status));
                            unresolved++;
                        }
                        Object cols = m.get("columns");
                        if (!(cols instanceof List<?> cl) || cl.isEmpty()) {
                            findings.add(finding("DATA-COL-001", "FAIL", name, "AT_LEAST_ONE_COLUMN_REQUIRED"));
                            fail++;
                        }
                    }
                }
            } else if (hasProposal) {
                findings.add(finding("DATA-TBL-PROPOSAL", "PASS_WITH_UNRESOLVED", "proposal",
                        "Legacy NEW_TABLE_PROPOSAL present"));
                unresolved++;
            }
        }

        Object comps = deep(design, "application", "components");
        if (!(comps instanceof Map<?, ?>)) {
            findings.add(finding("APP-001", "UNRESOLVED", "components", "Application components not designed"));
            unresolved++;
        } else {
            findings.add(finding("APP-001", "PASS", "components", "Component naming proposed"));
        }

        Object policy = design.get("policy");
        if (!(policy instanceof Map<?, ?>)) {
            findings.add(finding("RUN-001", "UNRESOLVED", "policy", "Runtime policy not designed"));
            unresolved++;
        } else {
            findings.add(finding("RUN-001", "PASS", "policy", "Runtime policy present (may contain UNRESOLVED)"));
            unresolved += countUnresolvedStrings(policy);
        }

        String status = fail > 0 ? "FAIL" : (unresolved > 0 ? "PASS_WITH_UNRESOLVED" : "PASS");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scope", "DESIGN_WIZARD");
        out.put("status", status);
        out.put("failCount", fail);
        out.put("unresolvedCount", unresolved);
        out.put("findings", findings);
        return out;
    }

    public Map<String, Object> export(Map<String, Object> design, String format) {
        Map<String, Object> gate = validateDesign(design);
        String md = toMarkdown(design, gate);
        if (md.contains("undefined") || md.contains("[object Object]") || md.contains("NaN")) {
            throw new IllegalStateException("Export contains forbidden tokens");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("format", format == null ? "markdown" : format);
        out.put("gate", gate);
        out.put("markdown", md);
        out.put("json", design);
        return out;
    }

    public Map<String, Object> recommendAssist(Map<String, Object> requirement) {
        return recommendationService.recommend(requirement);
    }

    private String toMarkdown(Map<String, Object> design, Map<String, Object> gate) {
        StringBuilder md = new StringBuilder();
        md.append("# NSIGHT Development Context\n\n");
        md.append("> Architecture Design Wizard export. Ontology 근거와 Architect 확정값만 사용.\n\n");

        Map<?, ?> req = asMap(design.get("requirement"));
        md.append("## Requirement\n");
        md.append(nullTo(req.get("title"), req.get("requirement"), "(none)")).append("\n\n");

        Map<?, ?> cls = asMap(design.get("classification"));
        md.append("## Business Classification\n");
        md.append("System: ").append(nullTo(cls.get("system"), "MG")).append('\n');
        md.append("Business: ").append(nullTo(cls.get("business"), "")).append('\n');
        md.append("Function: ").append(nullTo(cls.get("function"), "")).append("\n\n");

        Map<?, ?> sid = asMap(design.get("serviceIdDesign"));
        md.append("## Program\n").append(nullTo(sid.get("programId"), "UNRESOLVED")).append("\n\n");
        md.append("## ServiceId\n").append(nullTo(sid.get("serviceId"), "UNRESOLVED")).append('\n');
        md.append("Operation: ").append(nullTo(sid.get("transactionType"), "")).append(" / ")
                .append(nullTo(sid.get("operation"), "")).append("\n\n");

        md.append("## Data Design\n\n");
        Map<?, ?> data = asMap(design.get("dataDesign"));
        Object selected = data.get("selectedTables");
        if (selected instanceof List<?> list && !list.isEmpty()) {
            for (Object t : list) {
                if (t instanceof Map<?, ?> m) {
                    md.append("### ").append(nullTo(m.get("tableName"), "UNRESOLVED")).append('\n');
                    md.append("Access: ").append(nullTo(m.get("accessType"), "UNRESOLVED")).append('\n');
                    md.append("PK: ").append(nullTo(m.get("primaryKey"), "UNRESOLVED")).append('\n');
                    Object cols = m.get("selectColumns");
                    if (cols instanceof List<?> cl && !cl.isEmpty()) {
                        md.append("Columns:\n");
                        for (Object c : cl) {
                            md.append("- ").append(c).append('\n');
                        }
                    }
                    md.append('\n');
                }
            }
        }
        Object proposals = data.get("tableProposals");
        if (proposals instanceof List<?> pl && !pl.isEmpty()) {
            for (Object p : pl) {
                if (p instanceof Map<?, ?> m) {
                    md.append("### NEW_TABLE_PROPOSAL\n");
                    md.append("Status: ").append(nullTo(m.get("status"), "PROPOSED")).append('\n');
                    md.append("Logical: ").append(nullTo(m.get("logicalName"), "")).append('\n');
                    md.append("Physical: ").append(nullTo(m.get("physicalName"), "")).append('\n');
                    md.append("Schema: ").append(nullTo(m.get("schema"), "RDW")).append('\n');
                    md.append("Access: ").append(nullTo(m.get("accessType"), "UNRESOLVED")).append('\n');
                    Object pk = m.get("primaryKey");
                    if (pk instanceof List<?> pkl) {
                        md.append("PK:\n");
                        for (Object c : pkl) {
                            md.append("- ").append(c).append('\n');
                        }
                    }
                    Object cols = m.get("columns");
                    if (cols instanceof List<?> cl) {
                        md.append("Columns:\n");
                        for (Object c : cl) {
                            if (c instanceof Map<?, ?> cm) {
                                md.append("- ").append(nullTo(cm.get("physicalName"), "?"))
                                        .append(" ").append(nullTo(cm.get("dataType"), ""))
                                        .append(" personalData=").append(nullTo(cm.get("personalData"), "UNRESOLVED"))
                                        .append('\n');
                            }
                        }
                    }
                    md.append('\n');
                }
            }
        } else if (!(selected instanceof List<?> sel) || sel.isEmpty()) {
            Object legacy = data.get("newTableProposal");
            if (legacy != null && !String.valueOf(legacy).isBlank()) {
                md.append("### NEW_TABLE_PROPOSAL\n").append(legacy).append("\n\n");
            } else if (Boolean.TRUE.equals(data.get("tableUnresolved"))) {
                md.append("UNRESOLVED (Architect chose to keep unresolved)\n\n");
            } else {
                md.append("UNRESOLVED\n\n");
            }
        }
        Object joins = data.get("joins");
        if (joins instanceof List<?> jl && !jl.isEmpty()) {
            md.append("### Join\n");
            for (Object j : jl) {
                md.append("- ").append(j).append('\n');
            }
            md.append('\n');
        }

        Map<?, ?> policy = asMap(design.get("policy"));
        Map<?, ?> paging = asMap(policy.get("paging"));
        md.append("## Paging\n");
        md.append("Enabled: ").append(valOf(paging.get("enabled"))).append('\n');
        md.append("Key: ").append(valOf(paging.get("key"))).append('\n');
        md.append("Type: ").append(valOf(paging.get("type"))).append("\n\n");

        Map<?, ?> app = asMap(design.get("application"));
        md.append("## Application Architecture\n");
        Object layers = app.get("layers");
        if (layers instanceof List<?> ll) {
            md.append(ll.stream().map(String::valueOf).collect(Collectors.joining("\n→ "))).append("\n\n");
        } else {
            md.append("UNRESOLVED\n\n");
        }

        md.append("## Runtime\n");
        Map<?, ?> tx = asMap(policy.get("transaction"));
        md.append("TCF: ").append(valOf(tx.get("tcf"))).append('\n');
        md.append("Timeout: ").append(valOf(policy.get("timeout"))).append("\n\n");

        md.append("## Reference\n").append(nullTo(sid.get("referenceServiceId"), data.get("referenceServiceId"), "(none)"))
                .append("\n\n");

        md.append("## Gate\n").append(gate.get("status")).append(" (fail=")
                .append(gate.get("failCount")).append(", unresolved=")
                .append(gate.get("unresolvedCount")).append(")\n\n");

        md.append("## Unresolved\n");
        if (gate.get("findings") instanceof List<?> findings) {
            for (Object f : findings) {
                if (f instanceof Map<?, ?> m && "UNRESOLVED".equals(m.get("verdict"))) {
                    md.append("- ").append(m.get("ruleId")).append(": ").append(m.get("message")).append('\n');
                }
            }
        }
        return md.toString();
    }

    private static Map<String, Object> proposeComponentNames(String programId) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (programId == null || programId.isBlank()) {
            m.put("handler", "UNRESOLVED");
            m.put("facade", "UNRESOLVED");
            m.put("service", "UNRESOLVED");
            m.put("dao", "UNRESOLVED");
            m.put("mapper", "UNRESOLVED");
            return m;
        }
        m.put("handler", programId + "Handler");
        m.put("facade", programId + "Facade");
        m.put("service", programId + "Service");
        m.put("dao", programId + "DAO");
        m.put("mapper", programId + "Mapper");
        return m;
    }

    private static String packageRootFromProgram(String programId) {
        if (programId == null || programId.length() < 5) {
            return "UNRESOLVED";
        }
        String p = programId.toLowerCase(Locale.ROOT);
        return "nhnis." + p.substring(0, 2) + "." + p.substring(2, 4) + "." + p.substring(4, 5);
    }

    private static List<String> proposeProgramNos(List<String> used) {
        List<String> out = new ArrayList<>();
        for (int i = 7000; i <= 7999 && out.size() < 5; i++) {
            String no = String.format("%04d", i);
            if (!used.contains(no)) {
                out.add(no);
            }
        }
        return out;
    }

    private Map<String, Object> tableSummary(String name, String source, String refServiceId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tableName", name);
        m.put("source", source);
        if (refServiceId != null) {
            m.put("referenceServiceId", refServiceId);
        }
        store.findConceptOfType(name, ConceptType.TABLE).ifPresent(c -> {
            m.put("pk", c.attr("pk") == null ? "UNRESOLVED" : c.attr("pk"));
            m.put("conceptId", c.getId());
        });
        return m;
    }

    private void addTableFromProgram(List<Map<String, Object>> tables, Map<String, Object> p) {
        Object table = p.get("table");
        if (table != null && !String.valueOf(table).isBlank()) {
            String name = String.valueOf(table);
            boolean already = tables.stream().anyMatch(t -> name.equalsIgnoreCase(String.valueOf(t.get("tableName"))));
            if (!already) {
                tables.add(tableSummary(name, "PROGRAM_AXIS", null));
            }
        }
        if (p.get("tables") instanceof List<?> list) {
            for (Object t : list) {
                if (t == null) {
                    continue;
                }
                String name = String.valueOf(t);
                boolean already = tables.stream().anyMatch(x -> name.equalsIgnoreCase(String.valueOf(x.get("tableName"))));
                if (!already) {
                    tables.add(tableSummary(name, "PROGRAM_AXIS", null));
                }
            }
        }
    }

    private static Map<String, Object> finding(String ruleId, String verdict, String target, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ruleId", ruleId);
        m.put("verdict", verdict);
        m.put("target", target);
        m.put("message", message);
        return m;
    }

    private static Map<String, Object> evidence(Object value, String status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value == null ? "UNRESOLVED" : value);
        m.put("status", status);
        return m;
    }

    private static Object deep(Map<String, Object> root, String a, String b) {
        Object x = root.get(a);
        if (x instanceof Map<?, ?> m) {
            return m.get(b);
        }
        return null;
    }

    private static long countUnresolvedStrings(Object node) {
        if (node == null) {
            return 0;
        }
        if (node instanceof Map<?, ?> m) {
            long n = 0;
            for (Object v : m.values()) {
                n += countUnresolvedStrings(v);
            }
            return n;
        }
        if (node instanceof List<?> list) {
            long n = 0;
            for (Object v : list) {
                n += countUnresolvedStrings(v);
            }
            return n;
        }
        return "UNRESOLVED".equalsIgnoreCase(String.valueOf(node)) ? 1 : 0;
    }

    private static Map<?, ?> asMap(Object o) {
        return o instanceof Map<?, ?> m ? m : Map.of();
    }

    private static String valOf(Object o) {
        if (o instanceof Map<?, ?> m && m.get("value") != null) {
            return String.valueOf(m.get("value"));
        }
        return o == null ? "UNRESOLVED" : String.valueOf(o);
    }

    private static String nullTo(Object... vals) {
        for (Object v : vals) {
            if (v != null && !String.valueOf(v).isBlank() && !"null".equalsIgnoreCase(String.valueOf(v))) {
                return String.valueOf(v);
            }
        }
        return "UNRESOLVED";
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String defaultVal(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static boolean equalsIgnore(String expected, Object actual) {
        return expected != null && actual != null && expected.equalsIgnoreCase(String.valueOf(actual));
    }
}
