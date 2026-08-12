package nhnis.ontology.recommend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import nhnis.ontology.query.OntologyQueryService;
import nhnis.ontology.validate.ArchitectureRuleValidator;

/**
 * Backend Design Recommendation Use Case — Browser must not invent architecture facts.
 */
@Service
public class DesignRecommendationService {

    private final RecommendService recommendService;
    private final ArchitectureRuleValidator ruleValidator;
    private final OntologyQueryService queryService;

    public DesignRecommendationService(
            RecommendService recommendService,
            ArchitectureRuleValidator ruleValidator,
            OntologyQueryService queryService) {
        this.recommendService = recommendService;
        this.ruleValidator = ruleValidator;
        this.queryService = queryService;
    }

    public Map<String, Object> recommend(Map<String, Object> raw) {
        Map<String, String> flat = flatten(raw);
        String tx = defaultVal(flat.get("transactionType"), flat.get("intent"), "QUERY").toUpperCase(Locale.ROOT);
        String op = operationCode(tx);
        flat.put("intent", intentForRecommend(tx));
        flat.put("op", op);
        flat.putIfAbsent("topK", "20");

        Map<String, Object> base = recommendService.recommend(flat);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recs = (List<Map<String, Object>>) base.getOrDefault("recommendations", List.of());

        List<Map<String, Object>> candidates = new ArrayList<>();
        List<String> matchedServiceIds = new ArrayList<>();
        for (Map<String, Object> row : recs) {
            Map<String, Object> c = new LinkedHashMap<>(row);
            String matchedSid = serviceForOp(row, op);
            boolean opMatch = matchedSid != null;
            c.put("serviceId", matchedSid);
            c.put("primaryServiceId", matchedSid);
            c.put("operationMatch", opMatch);
            c.put("requestedOperation", op);
            if (!opMatch) {
                c.put("confidence", "LOW");
                c.put("status", "OPERATION_UNRESOLVED");
                @SuppressWarnings("unchecked")
                List<String> unmatched = new ArrayList<>((List<String>) c.getOrDefault("unmatchedAttributes", List.of()));
                unmatched.add("operationMatch");
                c.put("unmatchedAttributes", unmatched);
            } else {
                matchedServiceIds.add(matchedSid);
            }
            candidates.add(c);
        }
        candidates.sort((a, b) -> {
            boolean am = Boolean.TRUE.equals(a.get("operationMatch"));
            boolean bm = Boolean.TRUE.equals(b.get("operationMatch"));
            if (am != bm) {
                return am ? -1 : 1;
            }
            return Integer.compare((int) b.getOrDefault("score", 0), (int) a.getOrDefault("score", 0));
        });
        int topK = parseTopK(flat.get("topK"), 5);
        if (candidates.size() > topK) {
            candidates = new ArrayList<>(candidates.subList(0, topK));
            matchedServiceIds = candidates.stream()
                    .filter(c -> Boolean.TRUE.equals(c.get("operationMatch")))
                    .map(c -> Objects.toString(c.get("serviceId"), null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        boolean anyOpMatch = candidates.stream().anyMatch(c -> Boolean.TRUE.equals(c.get("operationMatch")));
        String status;
        if (candidates.isEmpty()) {
            status = "NO_MATCH";
        } else if (!anyOpMatch) {
            status = "OPERATION_NO_MATCH";
        } else {
            status = "OK";
        }

        Map<String, Object> pattern = buildPattern(tx, flat, matchedServiceIds);
        Map<String, Object> baseline = buildBaseline(flat, candidates, pattern, matchedServiceIds);
        Map<String, Object> designGate = ruleValidator.validateDesignBaseline(Map.of(
                "serviceId", "UNRESOLVED",
                "transactionType", tx));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("requestId", UUID.randomUUID().toString());
        out.put("requirement", flat);
        out.put("candidates", candidates);
        out.put("matchingCandidateCount", matchedServiceIds.size());
        out.put("pattern", pattern);
        out.put("baseline", baseline);
        out.put("designGate", designGate);
        out.put("status", status);
        out.put("legacyRecommend", Map.of(
                "status", base.get("status"),
                "topProgramId", base.get("topProgramId")));
        return out;
    }

    private Map<String, Object> buildPattern(String tx, Map<String, String> req, List<String> matchedServiceIds) {
        String name = switch (tx) {
            case "DELETE" -> "ONLINE_DELETE";
            case "CREATE" -> "ONLINE_CREATE";
            case "UPDATE" -> "ONLINE_UPDATE";
            case "MIXED", "CRUD" -> "ONLINE_MIXED";
            case "REPORT" -> "ONLINE_REPORT";
            default -> "ONLINE_QUERY";
        };
        boolean pagingReq = "YES".equalsIgnoreCase(req.get("paging")) || "true".equalsIgnoreCase(req.get("paging"));
        if (pagingReq && "QUERY".equals(tx)) {
            name = "ONLINE_PAGING_QUERY";
        }

        StructureEvidence structure = resolveStructureEvidence(matchedServiceIds);

        List<Map<String, Object>> properties = new ArrayList<>();
        properties.add(prop("structure", structure.value(), structure.status(), structure.note()));
        properties.add(prop("messageEnvelope", "UNRESOLVED", "UNRESOLVED",
                "operations.envelope not present on recommend reuse payload"));
        properties.add(prop("transaction", "UNRESOLVED", "UNRESOLVED",
                "No verified transaction metadata on candidates"));
        properties.add(prop("paging",
                pagingReq ? "REQUESTED_YES" : "REQUESTED_NO_OR_UNKNOWN",
                "UNRESOLVED",
                "User requirement only — not treated as candidate evidence"));
        properties.add(prop("timeout",
                defaultVal(req.get("timeoutPolicy"), "DEFAULT"),
                "UNRESOLVED",
                "Requirement input — not Ontology-verified"));

        Map<String, Object> pattern = new LinkedHashMap<>();
        pattern.put("name", name);
        pattern.put("status", "UNRESOLVED".equals(structure.status()) ? "UNRESOLVED_PATTERN" : "DERIVED_PATTERN");
        pattern.put("derivedFrom", matchedServiceIds);
        pattern.put("candidatePaths", structure.paths());
        pattern.put("structureCommon", structure.value());
        pattern.put("properties", properties);
        pattern.put("note", "ArchitecturePattern registry 없음 — candidate serviceStructure 공통 경로에서 DERIVED");
        return pattern;
    }

    private Map<String, Object> buildBaseline(
            Map<String, String> req,
            List<Map<String, Object>> candidates,
            Map<String, Object> pattern,
            List<String> matchedServiceIds) {
        List<String> unresolved = new ArrayList<>(List.of(
                "ProgramId", "ServiceId", "NewTable", "NewColumn", "PersonalDataPolicy"));
        boolean pagingReq = "YES".equalsIgnoreCase(req.get("paging")) || "true".equalsIgnoreCase(req.get("paging"));
        if (pagingReq) {
            unresolved.add("PagingStrategy");
        }

        Map<String, Object> classification = new LinkedHashMap<>();
        classification.put("system", defaultVal(req.get("system"), "MG"));
        classification.put("majorGroupName", defaultVal(req.get("majorGroupName"), "Market Group Platform"));
        classification.put("business", defaultVal(req.get("business"), "CO"));
        classification.put("businessName", defaultVal(req.get("businessName"), req.get("business")));
        classification.put("businessEn", defaultVal(req.get("businessEn"), ""));
        classification.put("function", defaultVal(req.get("function"), "A"));
        classification.put("functionName", defaultVal(req.get("functionName"), req.get("function")));
        classification.put("functionDesc", defaultVal(req.get("functionDesc"), ""));
        classification.put("packageRoot", defaultVal(req.get("packageRoot"),
                "nhnis." + defaultVal(req.get("system"), "MG").toLowerCase(Locale.ROOT)
                        + "." + defaultVal(req.get("business"), "CO").toLowerCase(Locale.ROOT)
                        + "." + defaultVal(req.get("function"), "A").toLowerCase(Locale.ROOT)));
        classification.put("programPrefix", defaultVal(req.get("programPrefix"), ""));
        classification.put("programId", "UNRESOLVED");
        classification.put("serviceId", "UNRESOLVED");
        classification.put("source", defaultVal(req.get("classificationSource"),
                "pdmg-service/docs/00.NSIGHT 애플리케이션 코드 분류표.md"));

        StructureEvidence structure = resolveStructureEvidence(matchedServiceIds);

        List<String> requestedTables = splitReqList(req.get("referenceTables"), req.get("preferredTables"), req.get("tables"));
        List<String> requestedFields = splitReqList(req.get("keyFields"), req.get("fields"));
        String newTableNeeded = defaultVal(req.get("newTableNeeded"), "UNKNOWN").toUpperCase(Locale.ROOT);
        String newTableName = defaultVal(req.get("newTableName"), "");
        String dataMode = defaultVal(req.get("dataMode"), "REUSE").toUpperCase(Locale.ROOT);

        if ("YES".equals(newTableNeeded) || "NEW".equals(dataMode)) {
            unresolved.add("NewTableDDL");
            unresolved.add("NewMapperSql");
        }
        if ("EXTEND".equals(dataMode) || !requestedFields.isEmpty()) {
            unresolved.add("ColumnDesignConfirmation");
        }

        List<String> candidateTables = collectCandidateTables(candidates);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", dataMode);
        data.put("requestedReferenceTables", requestedTables.isEmpty()
                ? evidenceField("NONE", "UNRESOLVED", List.of())
                : evidenceField(requestedTables, "REQUESTED", List.of("requirement.referenceTables")));
        data.put("requestedFields", requestedFields.isEmpty()
                ? evidenceField("NONE", "UNRESOLVED", List.of())
                : evidenceField(requestedFields, "REQUESTED", List.of("requirement.keyFields")));
        data.put("candidateTables", candidateTables.isEmpty()
                ? evidenceField("UNRESOLVED", "UNRESOLVED", List.of())
                : evidenceField(candidateTables, "DISCOVERED", List.of("candidate.reuse.data / serviceStructure")));
        data.put("newTableNeeded", evidenceField(newTableNeeded, "REQUESTED", List.of()));
        if ("YES".equals(newTableNeeded) || "NEW".equals(dataMode)) {
            data.put("newTable", evidenceField(
                    newTableName.isBlank() ? "UNRESOLVED" : newTableName,
                    newTableName.isBlank() ? "UNRESOLVED" : "REQUESTED",
                    List.of("requirement.newTableName — not Ontology VERIFIED")));
        } else {
            data.put("newTable", evidenceField("NOT_REQUIRED", "REQUESTED", List.of()));
            unresolved.remove("NewTable");
        }
        data.put("note", "요구 Table은 검색 힌트. Ontology에 없는 신규 Table은 UNRESOLVED로 유지");

        Map<String, Object> baseline = new LinkedHashMap<>();
        baseline.put("classification", classification);
        baseline.put("application", Map.of(
                "layers", structure.value(),
                "layersStatus", structure.status(),
                "referenceServiceIds", matchedServiceIds));
        baseline.put("message", Map.of(
                "envelope", evidenceField("UNRESOLVED", "UNRESOLVED", List.of()),
                "detail", evidenceField("UNRESOLVED", "UNRESOLVED", List.of())));
        baseline.put("transaction", evidenceField("UNRESOLVED", "UNRESOLVED", List.of()));
        baseline.put("paging", evidenceField(
                pagingReq ? "REQUESTED_YES" : "REQUESTED_NO_OR_UNKNOWN",
                "UNRESOLVED",
                List.of()));
        baseline.put("data", data);
        baseline.put("security", Map.of("personalData", defaultVal(req.get("personalData"), "UNKNOWN")));
        baseline.put("unresolved", unresolved);
        baseline.put("pattern", pattern);
        baseline.put("prohibited", List.of(
                "Invent JPA/Spring patterns not in Ontology",
                "Treat requirement paging as candidate VERIFIED paging",
                "Select wrong operation ServiceId silently",
                "Hardcode Handler→…→Table without candidate serviceStructure evidence",
                "Treat requirement.newTableName as Ontology VERIFIED table"));
        return baseline;
    }

    private static List<String> collectCandidateTables(List<Map<String, Object>> candidates) {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> c : candidates) {
            if (!Boolean.TRUE.equals(c.get("operationMatch"))) {
                continue;
            }
            Object reuse = c.get("reuse");
            if (!(reuse instanceof Map<?, ?> r)) {
                continue;
            }
            Object data = r.get("data");
            if (!(data instanceof Map<?, ?> d)) {
                continue;
            }
            Object tables = d.get("tables");
            if (tables instanceof List<?> list) {
                for (Object t : list) {
                    String name = String.valueOf(t);
                    if (!name.isBlank() && !out.contains(name)) {
                        out.add(name);
                    }
                }
            }
            Object table = d.get("table");
            if (table != null) {
                String name = String.valueOf(table);
                if (!name.isBlank() && !out.contains(name)) {
                    out.add(name);
                }
            }
        }
        return out;
    }

    private static List<String> splitReqList(String... values) {
        for (String raw : values) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            List<String> out = new ArrayList<>();
            for (String part : raw.split("[,;]+")) {
                if (!part.isBlank()) {
                    out.add(part.trim());
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
        }
        return List.of();
    }

    private StructureEvidence resolveStructureEvidence(List<String> matchedServiceIds) {
        List<List<String>> paths = new ArrayList<>();
        for (String sid : matchedServiceIds) {
            try {
                Map<String, Object> st = queryService.serviceStructure(sid);
                Object summary = st.get("summary");
                if (summary instanceof List<?> list && !list.isEmpty()) {
                    paths.add(list.stream().map(String::valueOf).toList());
                }
            } catch (RuntimeException ignored) {
                // leave path absent
            }
        }
        if (paths.isEmpty()) {
            return new StructureEvidence("UNRESOLVED", "UNRESOLVED",
                    "No candidate serviceStructure summary available", List.of());
        }
        String common = commonPath(paths);
        if (common == null || common.isBlank() || "UNRESOLVED".equals(common)) {
            return new StructureEvidence("UNRESOLVED", "UNRESOLVED",
                    "Candidates have no shared structure path", paths);
        }
        boolean layered = common.contains("Handler") && common.contains("Facade")
                && common.contains("Service") && common.contains("DAO");
        return new StructureEvidence(
                common,
                layered ? "DERIVED" : "PARTIAL",
                layered ? "Common layered path derived from candidate serviceStructure"
                        : "Partial common path from candidate serviceStructure",
                paths);
    }

    private static String commonPath(List<List<String>> paths) {
        if (paths.isEmpty()) {
            return "UNRESOLVED";
        }
        List<String> first = paths.get(0);
        List<String> common = new ArrayList<>();
        for (int i = 0; i < first.size(); i++) {
            String token = first.get(i);
            int idx = i;
            boolean all = paths.stream().allMatch(p -> p.size() > idx && Objects.equals(p.get(idx), token));
            if (!all) {
                break;
            }
            common.add(token);
        }
        if (common.isEmpty()) {
            // fallback: use first path when no prefix commonality
            return String.join(" → ", first);
        }
        return String.join(" → ", common);
    }

    private static Map<String, Object> evidenceField(Object value, String status, List<?> evidence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value == null ? "UNRESOLVED" : value);
        m.put("status", status);
        m.put("evidence", evidence == null ? List.of() : evidence);
        return m;
    }

    private static Map<String, Object> prop(String property, Object value, String status, String note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("property", property);
        m.put("value", value == null ? "UNRESOLVED" : value);
        m.put("status", status);
        m.put("note", note);
        return m;
    }

    private static String serviceForOp(Map<String, Object> row, String op) {
        Object reuse = row.get("reuse");
        if (!(reuse instanceof Map<?, ?> r) || !(r.get("services") instanceof List<?> services)) {
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

    public static String operationCode(String transactionType) {
        return switch (transactionType.toUpperCase(Locale.ROOT)) {
            case "CREATE", "C" -> "C";
            case "UPDATE", "U" -> "U";
            case "DELETE", "D" -> "D";
            case "MIXED", "CRUD", "A" -> "A";
            case "REPORT", "R" -> "R";
            default -> "S";
        };
    }

    private static String intentForRecommend(String tx) {
        return switch (tx.toUpperCase(Locale.ROOT)) {
            case "DELETE", "D" -> "delete";
            case "CREATE", "C" -> "create";
            case "UPDATE", "U" -> "update";
            case "MIXED", "CRUD", "A" -> "crud";
            case "REPORT", "R" -> "report";
            default -> "query";
        };
    }

    private static Map<String, String> flatten(Map<String, Object> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        raw.forEach((k, v) -> {
            if (v != null) {
                out.put(k, String.valueOf(v));
            }
        });
        if (out.containsKey("businessCode") && !out.containsKey("business")) {
            out.put("business", out.get("businessCode"));
        }
        if (out.containsKey("functionCode") && !out.containsKey("function")) {
            out.put("function", out.get("functionCode"));
        }
        return out;
    }

    private static int parseTopK(String raw, int fallback) {
        try {
            int v = Integer.parseInt(raw);
            return v > 0 ? Math.min(v, 50) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String defaultVal(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static String defaultVal(String v, String alt, String fallback) {
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        return defaultVal(alt, fallback);
    }

    private record StructureEvidence(String value, String status, String note, List<List<String>> paths) {
    }
}
