package nhnis.ontology.validate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.ontology.domain.concept.ConceptIds;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.relation.OntologyRelation;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.store.OntologyStore;
import nhnis.ontology.support.ServiceIdParser;

/**
 * Architecture rule validation (Ontology 1.0 RULE-001 ~ RULE-006).
 * SERVICE and DESIGN scopes must never recursively call each other for the same unresolved id.
 */
@Service
public class ArchitectureRuleValidator {

    private final OntologyStore store;

    public ArchitectureRuleValidator(OntologyStore store) {
        this.store = store;
    }

    public Map<String, Object> validateAll() {
        List<Map<String, Object>> findings = new ArrayList<>();
        findings.addAll(rule001());
        findings.addAll(rule002());
        findings.addAll(rule003());
        findings.addAll(rule004());
        findings.addAll(rule005());
        findings.addAll(rule006());
        return summarize("ALL", findings);
    }

    /**
     * ServiceId 단위 검증 — 해당 Service와 연결된 Program/Handler/Service/DAO 이웃만.
     */
    public Map<String, Object> validateService(String serviceId) {
        if (serviceId == null || serviceId.isBlank() || "UNRESOLVED".equalsIgnoreCase(serviceId.trim())) {
            return validateDesignBaseline(Map.of("serviceId", "UNRESOLVED"));
        }
        String canonical;
        try {
            canonical = ServiceIdParser.canonical(serviceId);
        } catch (IllegalArgumentException e) {
            List<Map<String, Object>> findings = List.of(
                    finding("RULE-001", "FAIL", serviceId, "ServiceId format invalid", Map.of("serviceId", serviceId), null));
            return summarize("SERVICE", findings);
        }
        OntologyConcept sid = store.findConcept(canonical)
                .or(() -> store.findConcept(ConceptIds.service(canonical)))
                .orElse(null);
        if (sid == null) {
            List<Map<String, Object>> findings = List.of(
                    finding("RULE-001", "PASS", canonical, "ServiceId format OK", Map.of("serviceId", canonical), null),
                    finding("SERVICE_LOOKUP", "NOT_FOUND", canonical,
                            "ServiceId not registered in Ontology Graph",
                            Map.of("serviceId", canonical, "reason", "not_in_graph"), null));
            Map<String, Object> out = summarize("SERVICE", findings);
            out.put("status", "NOT_FOUND");
            out.put("note", "Do not recurse into Design Baseline for unregistered ServiceId");
            return out;
        }

        List<Map<String, Object>> findings = new ArrayList<>();
        findings.addAll(rule001For(sid));
        findings.addAll(rule002For(sid));

        List<OntologyConcept> handlers = store.findRelations(sid.getId(), RelationType.HANDLED_BY).stream()
                .map(r -> store.findConcept(r.getToId()).orElse(null))
                .filter(c -> c != null)
                .toList();
        for (OntologyConcept h : handlers) {
            findings.addAll(rule003For(h));
        }
        if (handlers.isEmpty()) {
            findings.add(finding("RULE-003", "NOT_APPLICABLE", canonical,
                    "No Handler linked — RULE-003 skipped", Map.of("serviceId", canonical), null));
        }

        List<OntologyConcept> programs = store.findRelations(sid.getId(), RelationType.BELONGS_TO_PROGRAM).stream()
                .map(r -> store.findConcept(r.getToId()).orElse(null))
                .filter(c -> c != null)
                .toList();
        for (OntologyConcept p : programs) {
            findings.addAll(rule004For(p));
        }

        for (OntologyRelation rel : store.findRelations(sid.getId(), RelationType.HANDLED_BY)) {
            walkComponentsForRules(rel.getToId(), findings, 0);
        }
        return summarize("SERVICE", findings);
    }

    /**
     * Design-Time Baseline — 아직 없는 Handler/DAO를 무조건 FAIL하지 않음.
     * Never calls validateService for ids that are absent from the graph (avoids recursion).
     */
    public Map<String, Object> validateDesignBaseline(Map<String, String> baseline) {
        String serviceId = baseline == null ? null : baseline.get("serviceId");
        List<Map<String, Object>> findings = new ArrayList<>();
        if (serviceId == null || serviceId.isBlank() || "UNRESOLVED".equalsIgnoreCase(serviceId)) {
            findings.add(finding("RULE-001", "UNRESOLVED", "ServiceId",
                    "Design baseline ServiceId not allocated yet", Map.of("serviceId", "UNRESOLVED"), null));
            findings.add(finding("RULE-002", "NOT_YET_IMPLEMENTED", "Handler",
                    "Handler not implemented for new design", Map.of(), null));
            findings.add(finding("RULE-003", "NOT_YET_IMPLEMENTED", "Handler-ServiceId",
                    "Ontology Handler-ServiceId relation check deferred until implementation", Map.of(), null));
            findings.add(finding("RULE-004", "UNRESOLVED", "Program",
                    "ProgramId UNRESOLVED in design baseline", Map.of("programId", "UNRESOLVED"), null));
            findings.add(finding("RULE-005", "NOT_YET_IMPLEMENTED", "Service",
                    "Service→DAO dependency deferred", Map.of(), null));
            findings.add(finding("RULE-006", "NOT_YET_IMPLEMENTED", "DAO",
                    "DAO→Mapper dependency deferred", Map.of(), null));
            Map<String, Object> out = summarize("DESIGN_BASELINE", findings);
            out.put("note", "Design-time: NOT_YET_IMPLEMENTED/UNRESOLVED are not treated as FAIL");
            return out;
        }

        String canonical;
        try {
            canonical = ServiceIdParser.canonical(serviceId);
        } catch (IllegalArgumentException e) {
            findings.add(finding("RULE-001", "FAIL", serviceId, "ServiceId format invalid", Map.of("serviceId", serviceId), null));
            return summarize("DESIGN_BASELINE", findings);
        }

        OntologyConcept sid = store.findConcept(canonical)
                .or(() -> store.findConcept(ConceptIds.service(canonical)))
                .orElse(null);
        if (sid == null) {
            findings.add(finding("RULE-001", "PASS", canonical, "ServiceId format OK", Map.of("serviceId", canonical), null));
            findings.add(finding("RULE-002", "NOT_YET_IMPLEMENTED", canonical,
                    "Allocated ServiceId not yet in Ontology Graph", Map.of("serviceId", canonical), null));
            findings.add(finding("RULE-003", "NOT_YET_IMPLEMENTED", "Handler-ServiceId",
                    "Implementation registration deferred", Map.of("serviceId", canonical), null));
            findings.add(finding("RULE-004", "NOT_YET_IMPLEMENTED", "Program",
                    "Program linkage deferred until seed", Map.of("serviceId", canonical), null));
            findings.add(finding("RULE-005", "NOT_YET_IMPLEMENTED", "Service",
                    "Service→DAO dependency deferred", Map.of(), null));
            findings.add(finding("RULE-006", "NOT_YET_IMPLEMENTED", "DAO",
                    "DAO→Mapper dependency deferred", Map.of(), null));
            Map<String, Object> out = summarize("DESIGN_BASELINE", findings);
            out.put("note", "Allocated but unimplemented ServiceId — Design-Time NOT_YET_IMPLEMENTED");
            return out;
        }

        // Existing ServiceId used as design reference — run SERVICE rules explicitly (no recursion back).
        Map<String, Object> serviceResult = validateService(canonical);
        serviceResult.put("scope", "DESIGN_BASELINE");
        serviceResult.put("note", "Reference ServiceId exists in graph — SERVICE rules applied as design baseline reference");
        serviceResult.put("referenceServiceValidation", true);
        return serviceResult;
    }

    private void walkComponentsForRules(String componentId, List<Map<String, Object>> findings, int depth) {
        if (depth > 8) {
            return;
        }
        OntologyConcept c = store.findConcept(componentId).orElse(null);
        if (c == null || c.getType() != ConceptType.COMPONENT) {
            return;
        }
        Object role = c.attr("role");
        if ("SERVICE".equals(role)) {
            findings.addAll(rule005For(c));
        }
        if ("DAO".equals(role)) {
            findings.addAll(rule006For(c));
        }
        for (OntologyRelation rel : store.findRelationsFrom(c.getId())) {
            if (rel.getPredicate() == RelationType.CALLS || rel.getPredicate() == RelationType.USES
                    || rel.getPredicate() == RelationType.EXECUTES) {
                walkComponentsForRules(rel.getToId(), findings, depth + 1);
            }
        }
    }

    private List<Map<String, Object>> rule001For(OntologyConcept c) {
        String sid = c.getName();
        boolean ok = ServiceIdParser.isValid(sid);
        return List.of(finding("RULE-001", ok ? "PASS" : "FAIL", sid,
                ok ? "ServiceId format OK" : "ServiceId must be 11-char PDMG format",
                Map.of("serviceId", sid),
                c.getProvenance() == null ? null : c.getProvenance().toMap()));
    }

    private List<Map<String, Object>> rule002For(OntologyConcept c) {
        boolean ok = !store.findRelations(c.getId(), RelationType.HANDLED_BY).isEmpty();
        return List.of(finding("RULE-002", ok ? "PASS" : "FAIL", c.getName(),
                ok ? "HANDLED_BY present" : "ServiceId has no HANDLED_BY Handler",
                Map.of("serviceConceptId", c.getId()),
                c.getProvenance() == null ? null : c.getProvenance().toMap()));
    }

    private List<Map<String, Object>> rule003For(OntologyConcept handler) {
        List<String> linked = store.findRelationsTo(handler.getId()).stream()
                .filter(r -> r.getPredicate() == RelationType.HANDLED_BY)
                .map(r -> store.findConcept(r.getFromId()).map(OntologyConcept::getName).orElse(""))
                .filter(s -> !s.isBlank())
                .toList();
        Object programId = handler.attr("programId");
        boolean ok = !linked.isEmpty()
                && linked.stream().allMatch(sid -> programId == null || sid.startsWith(String.valueOf(programId)));
        return List.of(finding("RULE-003", ok ? "PASS" : "FAIL", handler.getName(),
                ok ? "Ontology Handler-ServiceId Relation Consistency OK"
                        : "Ontology Handler-ServiceId relation mismatch (not Java serviceIds() source check)",
                Map.of(
                        "linkedServiceIds", linked,
                        "programId", programId == null ? "" : programId,
                        "checkType", "ONTOLOGY_RELATION_ONLY",
                        "sourceBacked", false),
                handler.getProvenance() == null ? null : handler.getProvenance().toMap()));
    }

    private List<Map<String, Object>> rule004For(OntologyConcept program) {
        boolean ok = !store.findRelations(program.getId(), RelationType.PROVIDES_SERVICE).isEmpty();
        return List.of(finding("RULE-004", ok ? "PASS" : "FAIL", program.getName(),
                ok ? "Program provides ServiceId" : "Program has no PROVIDES_SERVICE",
                Map.of("programConceptId", program.getId()),
                program.getProvenance() == null ? null : program.getProvenance().toMap()));
    }

    private List<Map<String, Object>> rule005For(OntologyConcept service) {
        boolean ok = store.findRelationsFrom(service.getId()).stream()
                .filter(r -> r.getPredicate() == RelationType.USES || r.getPredicate() == RelationType.CALLS)
                .anyMatch(r -> isDaoOrClientTarget(r.getToId()));
        return List.of(finding("RULE-005", ok ? "PASS" : "FAIL", service.getName(),
                ok ? "Service has USES/CALLS to DAO/CLIENT"
                        : "Service missing USES/CALLS dependency to DAO/CLIENT role",
                Map.of("serviceConceptId", service.getId(), "requiredTargetRoles", List.of("DAO", "CLIENT")),
                service.getProvenance() == null ? null : service.getProvenance().toMap()));
    }

    private boolean isDaoOrClientTarget(String toId) {
        OntologyConcept target = store.findConcept(toId).orElse(null);
        if (target == null) {
            return false;
        }
        Object role = target.attr("role");
        return "DAO".equals(role) || "CLIENT".equals(role);
    }

    private List<Map<String, Object>> rule006For(OntologyConcept dao) {
        boolean ok = store.findRelations(dao.getId(), RelationType.EXECUTES).stream()
                .anyMatch(r -> store.findConcept(r.getToId())
                        .map(c -> c.getType() == ConceptType.MAPPER || c.getType() == ConceptType.SQL_ID)
                        .orElse(false));
        return List.of(finding("RULE-006", ok ? "PASS" : "FAIL", dao.getName(),
                ok ? "DAO EXECUTES Mapper/SqlId" : "DAO missing Mapper/SqlId EXECUTES relation",
                Map.of("daoConceptId", dao.getId()),
                dao.getProvenance() == null ? null : dao.getProvenance().toMap()));
    }

    private Map<String, Object> summarize(String scope, List<Map<String, Object>> findings) {
        long fails = findings.stream().filter(f -> "FAIL".equals(f.get("verdict"))).count();
        long unresolved = findings.stream().filter(f -> "UNRESOLVED".equals(f.get("verdict"))).count();
        long notYet = findings.stream().filter(f -> "NOT_YET_IMPLEMENTED".equals(f.get("verdict"))).count();
        long notFound = findings.stream().filter(f -> "NOT_FOUND".equals(f.get("verdict"))).count();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scope", scope);
        out.put("gateFamily", "ALL".equals(scope) || "SERVICE".equals(scope) || "DESIGN_BASELINE".equals(scope)
                ? "ONTOLOGY_INTEGRITY_GATE"
                : "ONTOLOGY_INTEGRITY_GATE");
        out.put("gateNote", "Structural ontology integrity only — not full Application Architecture Conformance (R-* YAML rules)");
        if (fails > 0) {
            out.put("status", "FAIL");
        } else if ("DESIGN_BASELINE".equals(scope) && (unresolved > 0 || notYet > 0)) {
            out.put("status", "PASS_WITH_UNRESOLVED");
        } else if (notFound > 0 && "SERVICE".equals(scope)) {
            out.put("status", "NOT_FOUND");
        } else {
            out.put("status", "PASS");
        }
        out.put("failCount", fails);
        out.put("unresolvedCount", unresolved);
        out.put("notYetImplementedCount", notYet);
        out.put("notFoundCount", notFound);
        out.put("findings", findings);
        return out;
    }

    /** RULE-001 ServiceId는 11자리 형식이어야 한다. */
    public List<Map<String, Object>> rule001() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OntologyConcept c : store.findConceptsByType(ConceptType.SERVICE_ID)) {
            out.addAll(rule001For(c));
        }
        return out;
    }

    /** RULE-002 모든 ServiceId는 최소 하나의 Handler와 연결되어야 한다. */
    public List<Map<String, Object>> rule002() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OntologyConcept c : store.findConceptsByType(ConceptType.SERVICE_ID)) {
            out.addAll(rule002For(c));
        }
        return out;
    }

    /**
     * RULE-003 Ontology HANDLED_BY reverse set consistency with Handler programId.
     * Does not inspect Java handler.serviceIds() source registration.
     */
    public List<Map<String, Object>> rule003() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OntologyConcept handler : store.findConceptsByType(ConceptType.COMPONENT)) {
            if (!"HANDLER".equals(handler.attr("role"))) {
                continue;
            }
            out.addAll(rule003For(handler));
        }
        return out;
    }

    /** RULE-004 Program은 최소 하나 이상의 ServiceId를 가져야 한다. */
    public List<Map<String, Object>> rule004() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OntologyConcept program : store.findConceptsByType(ConceptType.PROGRAM)) {
            out.addAll(rule004For(program));
        }
        return out;
    }

    /** RULE-005 Service는 DAO 또는 Client 중 최소 하나의 하위 의존성을 가져야 한다. */
    public List<Map<String, Object>> rule005() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OntologyConcept service : store.findConceptsByType(ConceptType.COMPONENT)) {
            if (!"SERVICE".equals(service.attr("role"))) {
                continue;
            }
            out.addAll(rule005For(service));
        }
        return out;
    }

    /** RULE-006 DAO를 통해 DB 접근 시 Mapper 또는 SqlId 관계가 존재해야 한다. */
    public List<Map<String, Object>> rule006() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OntologyConcept dao : store.findConceptsByType(ConceptType.COMPONENT)) {
            if (!"DAO".equals(dao.attr("role"))) {
                continue;
            }
            out.addAll(rule006For(dao));
        }
        return out;
    }

    private static Map<String, Object> finding(
            String ruleId,
            String verdict,
            String target,
            String message,
            Map<String, Object> evidence,
            Map<String, Object> source) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ruleId", ruleId);
        m.put("verdict", verdict);
        m.put("target", target);
        m.put("message", message);
        m.put("evidence", evidence);
        if (source != null) {
            m.put("source", source);
        }
        return m;
    }
}
