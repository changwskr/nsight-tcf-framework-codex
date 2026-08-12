package nhnis.ontology.recommend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.ontology.domain.Provenance;
import nhnis.ontology.domain.concept.ConceptIds;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.graph.OntologyGraphService;
import nhnis.ontology.ontology.OntologyRegistry;
import nhnis.ontology.prompt.PromptContextExporter;
import nhnis.ontology.query.OntologyQueryService;
import nhnis.ontology.store.OntologyStore;
import nhnis.ontology.validate.ArchitectureRuleValidator;

@Service
public class RecommendService {

    private final OntologyRegistry registry;
    private final OntologyGraphService graphService;
    private final PromptContextExporter promptExporter;
    private final OntologyStore store;
    private final OntologyQueryService queryService;
    private final ArchitectureRuleValidator ruleValidator;

    public RecommendService(
            OntologyRegistry registry,
            OntologyGraphService graphService,
            PromptContextExporter promptExporter,
            OntologyStore store,
            OntologyQueryService queryService,
            ArchitectureRuleValidator ruleValidator) {
        this.registry = registry;
        this.graphService = graphService;
        this.promptExporter = promptExporter;
        this.store = store;
        this.queryService = queryService;
        this.ruleValidator = ruleValidator;
    }

    public Map<String, Object> recommend(Map<String, String> request) {
        String system = defaultVal(request.get("system"), "MG");
        String business = defaultVal(request.get("business"), "CO");
        String function = defaultVal(request.get("function"), "A");
        String intent = defaultVal(request.get("intent"), "crud");
        String like = request.get("like");
        String dbAccess = defaultVal(request.get("dbAccess"), request.get("db"), "YES");
        String paging = defaultVal(request.get("paging"), "UNKNOWN");
        String channel = defaultVal(request.get("channel"), "UNKNOWN");
        List<String> requestedTables = splitCsv(
                firstNonBlank(request.get("referenceTables"), request.get("preferredTables"), request.get("tables")));
        List<String> requestedFields = splitCsv(
                firstNonBlank(request.get("keyFields"), request.get("fields")));

        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Map<String, Object> program : registry.listPrograms()) {
            List<String> matched = new ArrayList<>();
            List<String> unmatched = new ArrayList<>();
            int score = 0;

            if (equalsIgnore(system, program.get("majorGroup"))) {
                score += 20;
                matched.add("system");
            } else {
                unmatched.add("system");
            }
            if (equalsIgnore(business, program.get("businessCode"))) {
                score += 30;
                matched.add("business");
            } else {
                unmatched.add("business");
            }
            if (equalsIgnore(function, program.get("functionCode"))) {
                score += 20;
                matched.add("function");
            } else {
                unmatched.add("function");
            }

            score += intentScore(intent, program, matched, unmatched);

            String programId = String.valueOf(program.get("programId"));
            OntologyConcept programConcept = null;
            try {
                programConcept = store.findConcept(programId)
                        .or(() -> store.findConcept(ConceptIds.programFromShortId(programId)))
                        .orElse(null);
            } catch (IllegalArgumentException ignored) {
                programConcept = store.findConcept(programId).orElse(null);
            }
            if (programConcept != null) {
                score += 10;
                matched.add("programGraph");
            } else {
                unmatched.add("programGraph");
            }

            String primarySid = primaryServiceId(program, intent);
            String requestedOp = request.get("op");
            if (requestedOp != null && !requestedOp.isBlank()) {
                String matchedSid = serviceIdForOp(program, requestedOp);
                if (matchedSid != null) {
                    primarySid = matchedSid;
                }
            }
            final String resolvedSid = primarySid;
            OntologyConcept sidConcept = resolvedSid == null ? null
                    : store.findConcept(resolvedSid).or(() -> store.findConcept(ConceptIds.service(resolvedSid))).orElse(null);
            boolean hasHandler = false;
            boolean hasTable = false;
            boolean hasMapper = false;
            Map<String, Object> structure = null;
            if (sidConcept != null) {
                score += 10;
                matched.add("serviceId");
                hasHandler = !store.findRelations(sidConcept.getId(), RelationType.HANDLED_BY).isEmpty();
                if (hasHandler) {
                    score += 10;
                    matched.add("componentRelation");
                } else {
                    unmatched.add("componentRelation");
                }
                try {
                    structure = queryService.serviceStructure(resolvedSid);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> tables = (List<Map<String, Object>>) structure.get("tables");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> mappers = (List<Map<String, Object>>) structure.get("mappers");
                    hasTable = tables != null && !tables.isEmpty();
                    hasMapper = mappers != null && !mappers.isEmpty();
                    if (hasMapper) {
                        score += 5;
                        matched.add("mapper");
                    } else {
                        unmatched.add("mapper");
                    }
                    if (hasTable) {
                        score += 5;
                        matched.add("table");
                    } else {
                        unmatched.add("table");
                    }
                    score += tableFieldScore(
                            requestedTables,
                            requestedFields,
                            program,
                            tables,
                            matched,
                            unmatched);
                } catch (RuntimeException ex) {
                    unmatched.add("structure");
                    score += tableFieldScore(
                            requestedTables,
                            requestedFields,
                            program,
                            null,
                            matched,
                            unmatched);
                }
            } else {
                unmatched.add("serviceId");
                unmatched.add("componentRelation");
                score += tableFieldScore(
                        requestedTables,
                        requestedFields,
                        program,
                        null,
                        matched,
                        unmatched);
            }

            if ("YES".equalsIgnoreCase(dbAccess)) {
                if (hasTable || hasMapper || program.get("data") != null) {
                    score += 5;
                    matched.add("dbAccess");
                } else {
                    unmatched.add("dbAccess");
                }
            }

            // Paging / transaction metadata often absent in YAML → do not invent match
            if ("YES".equalsIgnoreCase(paging) || "NO".equalsIgnoreCase(paging)) {
                unmatched.add("pagingMetadata");
            }
            if (!"UNKNOWN".equalsIgnoreCase(channel)) {
                unmatched.add("channelMetadata");
            }
            unmatched.add("transactionMetadata");

            Map<String, Object> ruleSnap = null;
            if (resolvedSid != null && sidConcept != null) {
                ruleSnap = ruleValidator.validateService(resolvedSid);
                Object st = ruleSnap.get("status");
                if ("PASS".equals(st)) {
                    score += 10;
                    matched.add("architectureRule");
                } else {
                    unmatched.add("architectureRule");
                }
            } else {
                unmatched.add("architectureRule");
            }

            if (like != null && !like.isBlank()) {
                if (programId.equalsIgnoreCase(like) || containsService(program, like)) {
                    score += 50;
                    matched.add("explicitLike");
                }
            }
            if (score <= 0) {
                continue;
            }

            Map<String, Object> evidence = buildEvidence(program, resolvedSid, sidConcept, programConcept);
            String confidence = confidenceOf(score, matched, unmatched);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("candidateId", programId);
            row.put("candidateType", "PROGRAM");
            row.put("programId", programId);
            row.put("title", program.get("title"));
            row.put("primaryServiceId", resolvedSid);
            row.put("confidence", confidence);
            row.put("score", score); // internal ordinal only — not a fake percentage
            row.put("matchedAttributes", matched);
            row.put("unmatchedAttributes", unmatched);
            row.put("evidence", evidence);
            row.put("provenance", evidence.get("provenance"));
            row.put("status", "OK");
            row.put("structureSummary", structure == null ? List.of() : structure.get("summary"));
            row.put("ruleStatus", ruleSnap == null ? "UNRESOLVED" : ruleSnap.get("status"));
            row.put("reuse", Map.of(
                    "packageRoot", program.get("packageRoot"),
                    "development", program.get("development"),
                    "data", program.get("data"),
                    "services", program.get("services"),
                    "path", graphService.buildProgramGraph(program).get("pathLabel")));
            candidates.add(row);
        }
        candidates.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));

        // Prefer operation-matching candidates before Top-N cut (P1-07).
        String requestedOp = request.get("op");
        if (requestedOp != null && !requestedOp.isBlank()) {
            List<Map<String, Object>> matching = new ArrayList<>();
            List<Map<String, Object>> rest = new ArrayList<>();
            for (Map<String, Object> row : candidates) {
                Object reuse = row.get("reuse");
                boolean opHit = false;
                if (reuse instanceof Map<?, ?> r && r.get("services") instanceof List<?> services) {
                    for (Object item : services) {
                        if (item instanceof Map<?, ?> svc
                                && requestedOp.equalsIgnoreCase(String.valueOf(svc.get("op")))) {
                            opHit = true;
                            break;
                        }
                    }
                }
                if (opHit) {
                    matching.add(row);
                } else {
                    rest.add(row);
                }
            }
            candidates = new ArrayList<>(matching);
            candidates.addAll(rest);
        }

        int topK = 5;
        try {
            if (request.get("topK") != null) {
                topK = Math.max(1, Math.min(50, Integer.parseInt(request.get("topK"))));
            }
        } catch (NumberFormatException ignored) {
            topK = 5;
        }
        if (candidates.size() > topK) {
            candidates = new ArrayList<>(candidates.subList(0, topK));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", candidates.isEmpty() ? "NO_MATCH" : "OK");
        Map<String, Object> reqOut = new LinkedHashMap<>();
        reqOut.put("system", system);
        reqOut.put("business", business);
        reqOut.put("function", function);
        reqOut.put("intent", intent);
        reqOut.put("dbAccess", dbAccess);
        reqOut.put("paging", paging);
        reqOut.put("channel", channel);
        if (like != null && !like.isBlank()) {
            reqOut.put("like", like);
        }
        out.put("request", reqOut);
        out.put("recommendations", candidates);
        out.put("candidates", candidates);
        if (!candidates.isEmpty()) {
            String top = String.valueOf(candidates.get(0).get("programId"));
            out.put("topProgramId", top);
            out.put("promptMarkdown", promptExporter.asMarkdown(top));
            out.put("nextSteps", List.of(
                    "Reuse package/component pattern from topProgramId",
                    "Allocate new identifier (4 digits) under same MG/CO/A axis",
                    "Run seedPdmg after implementation, then validatePdmg",
                    "Attach promptMarkdown to 32.범용CRUD프롬프트"));
        }
        out.put("metaModel", "ontology/core/meta-model.yml");
        out.put("relations", "ontology/core/relations.yml");
        return out;
    }

    private Map<String, Object> buildEvidence(
            Map<String, Object> program,
            String primarySid,
            OntologyConcept sidConcept,
            OntologyConcept programConcept) {
        Map<String, Object> ev = new LinkedHashMap<>();
        String yamlPath = "ontology/mappings/" + program.get("programId") + ".yml";
        Provenance yaml = Provenance.yamlMapping(yamlPath);
        Map<String, Object> prov = yaml.toMap();
        if (sidConcept != null && sidConcept.getProvenance() != null) {
            prov = sidConcept.getProvenance().toMap();
        } else if (programConcept != null && programConcept.getProvenance() != null) {
            prov = programConcept.getProvenance().toMap();
        }
        ev.put("sourceType", prov.getOrDefault("sourceType", "YAML_MAPPING"));
        ev.put("sourcePath", prov.getOrDefault("sourcePath", yamlPath));
        ev.put("verificationStatus", prov.getOrDefault("verificationStatus", "DISCOVERED"));
        ev.put("discoveredBy", prov.getOrDefault("discoveredBy", "YamlGraphLoader"));
        ev.put("referenceProgram", program.get("programId"));
        ev.put("referenceServiceId", primarySid);
        ev.put("provenance", prov);
        return ev;
    }

    private static String confidenceOf(int score, List<String> matched, List<String> unmatched) {
        if (score >= 90 && matched.contains("componentRelation") && matched.contains("serviceId")) {
            return "HIGH";
        }
        if (score >= 55) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static String serviceIdForOp(Map<String, Object> program, String op) {
        if (!(program.get("services") instanceof List<?> services)) {
            return null;
        }
        for (Object item : services) {
            if (item instanceof Map<?, ?> svc
                    && op.equalsIgnoreCase(String.valueOf(svc.get("op")))) {
                return String.valueOf(svc.get("serviceId"));
            }
        }
        return null;
    }

    private static String primaryServiceId(Map<String, Object> program, String intent) {
        if (!(program.get("services") instanceof List<?> services) || services.isEmpty()) {
            return null;
        }
        String preferOp = preferOpForIntent(intent);
        for (Object item : services) {
            if (item instanceof Map<?, ?> svc && preferOp.equalsIgnoreCase(String.valueOf(svc.get("op")))) {
                return String.valueOf(svc.get("serviceId"));
            }
        }
        // Do not silently fall back to S when a specific operation was requested.
        if (!"S".equals(preferOp) && !"A".equals(preferOp)) {
            return null;
        }
        Object first = services.get(0);
        if (first instanceof Map<?, ?> svc) {
            return String.valueOf(svc.get("serviceId"));
        }
        return null;
    }

    private static String preferOpForIntent(String intent) {
        String i = intent == null ? "query" : intent.toLowerCase(Locale.ROOT);
        return switch (i) {
            case "delete", "d" -> "D";
            case "create", "c" -> "C";
            case "update", "u" -> "U";
            case "report", "r" -> "R";
            case "mixed", "a" -> "A";
            case "crud" -> "S"; // mixed CRUD catalog browse defaults to query sample
            default -> "S";
        };
    }

    private static int intentScore(
            String intent, Map<String, Object> program, List<String> matched, List<String> unmatched) {
        if (!(program.get("services") instanceof List<?> services)) {
            unmatched.add("operationType");
            return 0;
        }
        boolean hasS = false;
        boolean hasC = false;
        boolean hasU = false;
        boolean hasD = false;
        for (Object item : services) {
            if (item instanceof Map<?, ?> svc && svc.get("op") != null) {
                switch (String.valueOf(svc.get("op"))) {
                    case "S" -> hasS = true;
                    case "C" -> hasC = true;
                    case "U" -> hasU = true;
                    case "D" -> hasD = true;
                    default -> {
                    }
                }
            }
        }
        String i = intent.toLowerCase(Locale.ROOT);
        if ("crud".equals(i) && hasS && hasC && hasU && hasD) {
            matched.add("operationType");
            return 25;
        }
        if (("query".equals(i) || "read".equals(i)) && hasS) {
            matched.add("operationType");
            return hasC || hasU ? 15 : 25;
        }
        if (("create".equals(i) || "c".equals(i)) && hasC) {
            matched.add("operationType");
            return 25;
        }
        if (("update".equals(i) || "u".equals(i)) && hasU) {
            matched.add("operationType");
            return 25;
        }
        if (("delete".equals(i) || "d".equals(i)) && hasD) {
            matched.add("operationType");
            return 25;
        }
        if (("report".equals(i) || "r".equals(i))) {
            // REPORT maps to op R; do not treat S presence as operationMatch evidence.
            unmatched.add("operationType");
            return hasS ? 5 : 0;
        }
        unmatched.add("operationType");
        return 0;
    }

    private static int tableFieldScore(
            List<String> requestedTables,
            List<String> requestedFields,
            Map<String, Object> program,
            List<Map<String, Object>> structureTables,
            List<String> matched,
            List<String> unmatched) {
        int score = 0;
        List<String> programTables = collectProgramTables(program, structureTables);
        List<String> programFields = collectProgramFields(program);

        if (!requestedTables.isEmpty()) {
            boolean hit = requestedTables.stream().anyMatch(req ->
                    programTables.stream().anyMatch(pt -> pt.equalsIgnoreCase(req)));
            if (hit) {
                score += 40;
                matched.add("requestedTable");
            } else {
                unmatched.add("requestedTable");
            }
        }
        if (!requestedFields.isEmpty()) {
            boolean hit = requestedFields.stream().anyMatch(req ->
                    programFields.stream().anyMatch(pf -> pf.equalsIgnoreCase(req)));
            if (hit) {
                score += 20;
                matched.add("requestedField");
            } else {
                unmatched.add("requestedField");
            }
        }
        return score;
    }

    private static List<String> collectProgramTables(
            Map<String, Object> program, List<Map<String, Object>> structureTables) {
        List<String> out = new ArrayList<>();
        Object data = program.get("data");
        if (data instanceof Map<?, ?> d) {
            Object tables = d.get("tables");
            if (tables instanceof List<?> list) {
                for (Object t : list) {
                    if (t != null && !String.valueOf(t).isBlank()) {
                        out.add(String.valueOf(t).trim());
                    }
                }
            }
            Object table = d.get("table");
            if (table != null && !String.valueOf(table).isBlank()) {
                out.add(String.valueOf(table).trim());
            }
        }
        if (structureTables != null) {
            for (Map<String, Object> t : structureTables) {
                Object name = t.get("name");
                if (name == null) {
                    name = t.get("tableName");
                }
                if (name != null && !String.valueOf(name).isBlank()) {
                    out.add(String.valueOf(name).trim());
                }
            }
        }
        return out;
    }

    private static List<String> collectProgramFields(Map<String, Object> program) {
        List<String> out = new ArrayList<>();
        Object data = program.get("data");
        if (data instanceof Map<?, ?> d) {
            Object pk = d.get("pk");
            if (pk instanceof List<?> list) {
                for (Object c : list) {
                    if (c != null && !String.valueOf(c).isBlank()) {
                        out.add(String.valueOf(c).trim());
                    }
                }
            } else if (pk != null && !String.valueOf(pk).isBlank()) {
                out.add(String.valueOf(pk).trim());
            }
        }
        return out;
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[,;\\s]+")) {
            if (!part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static boolean containsService(Map<String, Object> program, String serviceId) {
        if (!(program.get("services") instanceof List<?> services)) {
            return false;
        }
        for (Object item : services) {
            if (item instanceof Map<?, ?> svc
                    && serviceId.equalsIgnoreCase(String.valueOf(svc.get("serviceId")))) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalsIgnore(String expected, Object actual) {
        return expected != null && actual != null
                && expected.equalsIgnoreCase(String.valueOf(actual));
    }

    private static String defaultVal(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String defaultVal(String value, String alt, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return defaultVal(alt, fallback);
    }
}
