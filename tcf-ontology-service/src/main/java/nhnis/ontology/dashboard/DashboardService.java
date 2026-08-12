package nhnis.ontology.dashboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import nhnis.ontology.design.DesignWizardService;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.relation.OntologyRelation;
import nhnis.ontology.ontology.OntologyRegistry;
import nhnis.ontology.query.OntologyQueryService;
import nhnis.ontology.store.OntologyStore;
import nhnis.ontology.validate.ArchitectureRuleValidator;

/**
 * Dashboard detail payloads for Concepts / Relations / Programs / ServiceIds / Designs / Rules FAIL.
 */
@Service
public class DashboardService {

    private final OntologyStore store;
    private final OntologyRegistry registry;
    private final OntologyQueryService queryService;
    private final ArchitectureRuleValidator ruleValidator;
    private final DesignWizardService designWizard;

    public DashboardService(
            OntologyStore store,
            OntologyRegistry registry,
            OntologyQueryService queryService,
            ArchitectureRuleValidator ruleValidator,
            DesignWizardService designWizard) {
        this.store = store;
        this.registry = registry;
        this.queryService = queryService;
        this.ruleValidator = ruleValidator;
        this.designWizard = designWizard;
    }

    public Map<String, Object> summary() {
        Map<String, Object> rules = ruleValidator.validateAll();
        long fail = countVerdict(rules, "FAIL");
        long pass = countVerdict(rules, "PASS");
        List<Map<String, Object>> designArtifacts = designWizard.listCompletedDesignArtifacts();
        long designSessions = designArtifacts.stream().filter(a -> "SESSION".equals(a.get("kind"))).count();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("concepts", store.allConcepts().size());
        out.put("relations", store.allRelations().size());
        out.put("programs", store.findConceptsByType(ConceptType.PROGRAM).size());
        out.put("serviceIds", store.findConceptsByType(ConceptType.SERVICE_ID).size());
        out.put("designSessions", designSessions);
        out.put("designArtifacts", designArtifacts.size());
        out.put("rulesFail", fail);
        out.put("rulesPass", pass);
        out.put("byType", queryService.listConcepts(null, null).get("byType"));
        out.put("byPredicate", countByPredicate());
        return out;
    }

    public Map<String, Object> detail(String view, String keyword) {
        String v = view == null ? "concepts" : view.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "relations", "relation" -> relationsDetail(keyword);
            case "programs", "program" -> programsDetail(keyword);
            case "services", "serviceids", "service-ids", "service_id" -> servicesDetail(keyword);
            case "designs", "design", "proposals", "wizard" -> designsDetail(keyword);
            case "rules-fail", "rules", "rulesfail", "fail" -> rulesFailDetail(keyword);
            case "concepts", "concept" -> conceptsDetail(keyword);
            default -> throw new IllegalArgumentException(
                    "Unknown dashboard view: " + view
                            + " (concepts|relations|programs|services|designs|rules-fail)");
        };
    }

    private Map<String, Object> conceptsDetail(String keyword) {
        Map<String, Object> list = queryService.listConcepts(null, keyword);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("view", "concepts");
        out.put("title", "Concepts Detail");
        out.put("total", list.get("totalConcepts"));
        out.put("count", list.get("count"));
        out.put("byType", list.get("byType"));
        out.put("items", list.get("objects"));
        out.put("columns", List.of("type", "name", "id", "verificationStatus", "outgoingCount", "incomingCount"));
        out.put("note", "Ontology VERIFIED/loaded concepts only. Wizard PROPOSED → Designs tab");
        return out;
    }

    private Map<String, Object> relationsDetail(String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> items = store.allRelations().stream()
                .sorted(Comparator
                        .comparing((OntologyRelation r) -> r.getPredicate().name())
                        .thenComparing(OntologyRelation::getFromId)
                        .thenComparing(OntologyRelation::getToId))
                .map(this::relationRow)
                .filter(row -> {
                    if (kw.isBlank()) {
                        return true;
                    }
                    String hay = String.join(" ",
                            String.valueOf(row.get("from")),
                            String.valueOf(row.get("predicate")),
                            String.valueOf(row.get("to")),
                            String.valueOf(row.get("graphType")),
                            String.valueOf(row.get("fromName")),
                            String.valueOf(row.get("toName"))).toLowerCase(Locale.ROOT);
                    return hay.contains(kw);
                })
                .collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("view", "relations");
        out.put("title", "Relations Detail");
        out.put("total", store.allRelations().size());
        out.put("count", items.size());
        out.put("byPredicate", countByPredicate());
        out.put("items", items);
        out.put("columns", List.of("predicate", "fromName", "toName", "graphType", "verificationStatus"));
        return out;
    }

    private Map<String, Object> programsDetail(String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> items = new ArrayList<>();
        for (OntologyConcept c : store.findConceptsByType(ConceptType.PROGRAM)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("programId", c.getName());
            row.put("type", "PROGRAM");
            row.put("source", "ONTOLOGY");
            registry.findProgram(c.getName()).ifPresentOrElse(doc -> {
                row.put("title", doc.get("title"));
                row.put("majorGroup", doc.get("majorGroup"));
                row.put("businessCode", doc.get("businessCode"));
                row.put("functionCode", doc.get("functionCode"));
                row.put("packageRoot", doc.get("packageRoot"));
                Object data = doc.get("data");
                if (data instanceof Map<?, ?> d) {
                    row.put("table", d.get("table"));
                    row.put("tables", d.get("tables"));
                    row.put("pk", d.get("pk"));
                }
                List<String> serviceIds = new ArrayList<>();
                if (doc.get("services") instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> svc && svc.get("serviceId") != null) {
                            serviceIds.add(String.valueOf(svc.get("serviceId")));
                        }
                    }
                }
                row.put("services", serviceIds);
                row.put("serviceCount", serviceIds.size());
            }, () -> {
                row.put("title", c.getDescription());
                row.put("services", List.of());
                row.put("serviceCount", 0);
            });
            if (c.getProvenance() != null) {
                row.put("verificationStatus", c.getProvenance().getVerificationStatus() == null
                        ? "UNRESOLVED"
                        : c.getProvenance().getVerificationStatus().name());
                row.put("sourcePath", c.getProvenance().getSourcePath());
            } else {
                row.put("verificationStatus", "UNRESOLVED");
            }
            if (matches(kw, row.get("programId"), row.get("title"), row.get("table"), row.get("packageRoot"))) {
                items.add(row);
            }
        }

        for (Map<String, Object> artifact : designWizard.listCompletedDesignArtifacts()) {
            if (!"PROGRAM".equals(artifact.get("kind"))) {
                continue;
            }
            String programId = String.valueOf(artifact.get("programId"));
            boolean exists = items.stream().anyMatch(r -> programId.equalsIgnoreCase(String.valueOf(r.get("programId"))));
            if (exists) {
                continue;
            }
            if (matches(kw, artifact.get("programId"), artifact.get("title"), artifact.get("table"), artifact.get("sessionId"))) {
                items.add(artifact);
            }
        }
        items.sort(Comparator.comparing(m -> String.valueOf(m.get("programId"))));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("view", "programs");
        out.put("title", "Programs Detail");
        out.put("total", items.size());
        out.put("ontologyCount", items.stream().filter(i -> !"DESIGN_WIZARD".equals(i.get("source"))).count());
        out.put("proposedCount", items.stream().filter(i -> "DESIGN_WIZARD".equals(i.get("source"))).count());
        out.put("count", items.size());
        out.put("items", items);
        out.put("columns", List.of("programId", "title", "businessCode", "functionCode", "table", "serviceCount", "verificationStatus"));
        out.put("note", "Ontology + Completed Design Wizard PROPOSED programs");
        return out;
    }

    private Map<String, Object> servicesDetail(String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> items = new ArrayList<>();
        for (OntologyConcept c : store.findConceptsByType(ConceptType.SERVICE_ID)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("serviceId", c.getName());
            row.put("type", "SERVICE_ID");
            row.put("source", "ONTOLOGY");
            row.put("op", c.attr("op"));
            row.put("sequence", c.attr("sequence"));
            List<String> programs = store.findRelations(c.getId(),
                            nhnis.ontology.domain.relation.RelationType.BELONGS_TO_PROGRAM)
                    .stream()
                    .map(r -> store.findConcept(r.getToId()).map(OntologyConcept::getName).orElse(r.getToId()))
                    .toList();
            List<String> handlers = store.findRelations(c.getId(),
                            nhnis.ontology.domain.relation.RelationType.HANDLED_BY)
                    .stream()
                    .map(r -> store.findConcept(r.getToId()).map(OntologyConcept::getName).orElse(r.getToId()))
                    .toList();
            row.put("programs", programs);
            row.put("programId", programs.isEmpty() ? "UNRESOLVED" : programs.get(0));
            row.put("handlers", handlers);
            row.put("handler", handlers.isEmpty() ? "UNRESOLVED" : handlers.get(0));
            registry.findByServiceId(c.getName()).ifPresent(doc -> {
                row.put("programTitle", doc.get("title"));
                row.put("packageRoot", doc.get("packageRoot"));
            });
            if (c.getProvenance() != null) {
                row.put("verificationStatus", c.getProvenance().getVerificationStatus() == null
                        ? "UNRESOLVED"
                        : c.getProvenance().getVerificationStatus().name());
            } else {
                row.put("verificationStatus", "UNRESOLVED");
            }
            if (matches(kw, row.get("serviceId"), row.get("programId"), row.get("handler"), row.get("programTitle"))) {
                items.add(row);
            }
        }

        for (Map<String, Object> artifact : designWizard.listCompletedDesignArtifacts()) {
            if (!"SERVICE_ID".equals(artifact.get("kind"))) {
                continue;
            }
            String serviceId = String.valueOf(artifact.get("serviceId"));
            boolean exists = items.stream().anyMatch(r -> serviceId.equalsIgnoreCase(String.valueOf(r.get("serviceId"))));
            if (exists) {
                continue;
            }
            if (matches(kw, artifact.get("serviceId"), artifact.get("programId"), artifact.get("handler"), artifact.get("sessionId"))) {
                items.add(artifact);
            }
        }
        items.sort(Comparator.comparing(m -> String.valueOf(m.get("serviceId"))));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("view", "services");
        out.put("title", "ServiceIds Detail");
        out.put("total", items.size());
        out.put("ontologyCount", items.stream().filter(i -> !"DESIGN_WIZARD".equals(i.get("source"))).count());
        out.put("proposedCount", items.stream().filter(i -> "DESIGN_WIZARD".equals(i.get("source"))).count());
        out.put("count", items.size());
        out.put("items", items);
        out.put("columns", List.of("serviceId", "op", "programId", "handler", "verificationStatus"));
        out.put("note", "Ontology + Completed Design Wizard PROPOSED serviceIds");
        return out;
    }

    private Map<String, Object> designsDetail(String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> items = designWizard.listCompletedDesignArtifacts().stream()
                .filter(row -> matches(kw,
                        row.get("kind"),
                        row.get("name"),
                        row.get("serviceId"),
                        row.get("programId"),
                        row.get("tableName"),
                        row.get("physicalName"),
                        row.get("title"),
                        row.get("sessionId")))
                .sorted(Comparator
                        .comparing((Map<String, Object> m) -> String.valueOf(m.getOrDefault("sessionId", "")))
                        .thenComparing(m -> String.valueOf(m.getOrDefault("kind", ""))))
                .collect(Collectors.toList());

        long sessions = items.stream().filter(i -> "SESSION".equals(i.get("kind"))).count();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("view", "designs");
        out.put("title", "Design Wizard · Completed (PROPOSED)");
        out.put("total", items.size());
        out.put("count", items.size());
        out.put("sessionCount", sessions);
        out.put("items", items);
        out.put("columns", List.of("kind", "name", "serviceId", "programId", "verificationStatus", "gateStatus", "sessionId"));
        out.put("note", "Done 저장분. Ontology VERIFIED가 아님 — PROPOSED only");
        return out;
    }

    private Map<String, Object> rulesFailDetail(String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Map<String, Object> all = ruleValidator.validateAll();
        List<Map<String, Object>> fails = new ArrayList<>();
        List<Map<String, Object>> others = new ArrayList<>();
        if (all.get("findings") instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> f = new LinkedHashMap<>();
                raw.forEach((k, v) -> f.put(String.valueOf(k), v));
                String verdict = String.valueOf(f.getOrDefault("verdict", "")).toUpperCase(Locale.ROOT);
                String hay = String.join(" ",
                        String.valueOf(f.get("ruleId")),
                        String.valueOf(f.get("target")),
                        String.valueOf(f.get("message")),
                        verdict).toLowerCase(Locale.ROOT);
                if (!kw.isBlank() && !hay.contains(kw)) {
                    continue;
                }
                if ("FAIL".equals(verdict) || "FAILED".equals(verdict)) {
                    fails.add(f);
                } else {
                    others.add(f);
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("view", "rules-fail");
        out.put("title", "Rules FAIL Detail");
        out.put("total", all.get("failCount"));
        out.put("count", fails.size());
        out.put("status", all.get("status"));
        out.put("failCount", fails.size());
        out.put("passCount", countVerdict(all, "PASS"));
        out.put("items", fails);
        out.put("otherFindings", others);
        out.put("columns", List.of("ruleId", "target", "message", "verdict"));
        return out;
    }

    private Map<String, Object> relationRow(OntologyRelation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("from", r.getFromId());
        m.put("predicate", r.getPredicate().name());
        m.put("to", r.getToId());
        m.put("graphType", r.getGraphType() == null ? "DESIGN" : r.getGraphType().name());
        m.put("fromName", store.findConcept(r.getFromId()).map(OntologyConcept::getName).orElse(r.getFromId()));
        m.put("toName", store.findConcept(r.getToId()).map(OntologyConcept::getName).orElse(r.getToId()));
        m.put("fromType", store.findConcept(r.getFromId()).map(c -> c.getType().name()).orElse("UNRESOLVED"));
        m.put("toType", store.findConcept(r.getToId()).map(c -> c.getType().name()).orElse("UNRESOLVED"));
        if (r.getProvenance() != null && r.getProvenance().getVerificationStatus() != null) {
            m.put("verificationStatus", r.getProvenance().getVerificationStatus().name());
        } else {
            m.put("verificationStatus", "UNRESOLVED");
        }
        return m;
    }

    private Map<String, Long> countByPredicate() {
        Map<String, Long> by = new LinkedHashMap<>();
        for (OntologyRelation r : store.allRelations()) {
            by.merge(r.getPredicate().name(), 1L, Long::sum);
        }
        return by;
    }

    private static boolean matches(String kw, Object... parts) {
        if (kw == null || kw.isBlank()) {
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (p != null) {
                sb.append(' ').append(p);
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT).contains(kw);
    }

    private static long countVerdict(Map<String, Object> rules, String verdict) {
        if (!(rules.get("findings") instanceof List<?> list)) {
            if ("FAIL".equals(verdict) && rules.get("failCount") instanceof Number n) {
                return n.longValue();
            }
            return 0;
        }
        long n = 0;
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Object verdictObj = m.get("verdict");
                if (verdictObj == null) {
                    verdictObj = m.get("status");
                }
                String v = String.valueOf(verdictObj == null ? "" : verdictObj).toUpperCase(Locale.ROOT);
                if (verdict.equals(v) || (verdict.equals("FAIL") && "FAILED".equals(v))) {
                    n++;
                }
            }
        }
        return n;
    }
}
