package nhnis.ontology.impact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import nhnis.ontology.graph.OntologyGraphService;
import nhnis.ontology.ontology.OntologyRegistry;

@Service
public class ImpactAnalyzer {

    private final OntologyRegistry registry;
    private final OntologyGraphService graphService;

    public ImpactAnalyzer(OntologyRegistry registry, OntologyGraphService graphService) {
        this.registry = registry;
        this.graphService = graphService;
    }

    /**
     * from 예: serviceId, programId, FQCN, table, ui path, sqlId
     */
    public Map<String, Object> analyze(String from) {
        String needle = from == null ? "" : from.trim();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", needle);

        Optional<Map<String, Object>> matched = resolveProgram(needle);
        if (matched.isEmpty()) {
            result.put("status", "NOT_FOUND");
            result.put("message", "No seeded mapping matched: " + needle);
            result.put("candidates", suggest(needle));
            return result;
        }

        Map<String, Object> program = matched.get();
        String programId = String.valueOf(program.get("programId"));
        String matchType = detectMatchType(needle, program);

        result.put("status", "OK");
        result.put("matchType", matchType);
        result.put("programId", programId);
        result.put("title", program.get("title"));

        Map<String, Object> graph = graphService.buildProgramGraph(program);
        result.put("path", graph.get("path"));
        result.put("pathLabel", graph.get("pathLabel"));
        result.put("nodes", graph.get("nodes"));
        result.put("edges", graph.get("edges"));
        result.put("relationVocabulary", "ontology/core/relations.yml");

        Map<String, Object> blastRadius = new LinkedHashMap<>();
        blastRadius.put("architecture", List.of(
                "Worker TransactionTemplate scope",
                "Facade @Transactional REQUIRED join",
                "BizPrePostAspect on Service"));
        blastRadius.put("development", collectDevFiles(program));
        blastRadius.put("data", collectDataFiles(program));
        blastRadius.put("operations", collectOpsFiles(program));
        result.put("blastRadius", blastRadius);
        result.put("rules", registry.rulesBundle().get("rules"));
        result.put("runtimeRef", program.get("architecture"));
        return result;
    }

    private Optional<Map<String, Object>> resolveProgram(String needle) {
        if (needle.isEmpty()) {
            return Optional.empty();
        }
        Optional<Map<String, Object>> byService = registry.findByServiceId(needle);
        if (byService.isPresent()) {
            return byService;
        }
        Optional<Map<String, Object>> byProgram = registry.findProgram(needle);
        if (byProgram.isPresent()) {
            return byProgram;
        }

        String lower = needle.toLowerCase(Locale.ROOT);
        for (Map<String, Object> program : registry.listPrograms()) {
            if (containsIgnoreCase(String.valueOf(program.get("programId")), lower)) {
                return Optional.of(program);
            }
            if (program.get("development") instanceof Map<?, ?> dev) {
                for (Object v : List.of(dev.get("handler"), dev.get("facade"), dev.get("controller"),
                        dev.get("service"), dev.get("dao"))) {
                    if (v != null && containsIgnoreCase(String.valueOf(v), lower)) {
                        return Optional.of(program);
                    }
                }
            }
            if (program.get("data") instanceof Map<?, ?> data) {
                if (data.get("table") != null && containsIgnoreCase(String.valueOf(data.get("table")), lower)) {
                    return Optional.of(program);
                }
                if (data.get("mapperXml") != null
                        && containsIgnoreCase(String.valueOf(data.get("mapperXml")), lower)) {
                    return Optional.of(program);
                }
            }
            if (program.get("operations") instanceof Map<?, ?> ops
                    && ops.get("uiRoute") != null
                    && containsIgnoreCase(String.valueOf(ops.get("uiRoute")), lower)) {
                return Optional.of(program);
            }
            if (program.get("services") instanceof List<?> services) {
                for (Object item : services) {
                    if (item instanceof Map<?, ?> svc) {
                        if (svc.get("sqlIds") instanceof List<?> sqlIds) {
                            for (Object sqlId : sqlIds) {
                                if (containsIgnoreCase(String.valueOf(sqlId), lower)) {
                                    return Optional.of(program);
                                }
                            }
                        }
                        if (svc.get("dtoIn") != null && containsIgnoreCase(String.valueOf(svc.get("dtoIn")), lower)) {
                            return Optional.of(program);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private List<String> suggest(String needle) {
        String lower = needle.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Map<String, Object> program : registry.listPrograms()) {
            out.add(String.valueOf(program.get("programId")));
            if (program.get("services") instanceof List<?> services) {
                for (Object item : services) {
                    if (item instanceof Map<?, ?> svc && svc.get("serviceId") != null) {
                        String sid = String.valueOf(svc.get("serviceId"));
                        if (sid.contains(lower) || lower.contains(String.valueOf(program.get("programId")))) {
                            out.add(sid);
                        }
                    }
                }
            }
        }
        return out.stream().distinct().limit(20).toList();
    }

    private static String detectMatchType(String needle, Map<String, Object> program) {
        if (needle.equals(String.valueOf(program.get("programId")))) {
            return "programId";
        }
        if (program.get("services") instanceof List<?> services) {
            for (Object item : services) {
                if (item instanceof Map<?, ?> svc && needle.equals(String.valueOf(svc.get("serviceId")))) {
                    return "serviceId";
                }
            }
        }
        if (program.get("data") instanceof Map<?, ?> data
                && needle.equalsIgnoreCase(String.valueOf(data.get("table")))) {
            return "table";
        }
        if (needle.contains(".") && needle.startsWith("nhnis.")) {
            return "javaClass";
        }
        if (needle.contains("/")) {
            return "path";
        }
        return "fuzzy";
    }

    private static List<String> collectDevFiles(Map<String, Object> program) {
        List<String> files = new ArrayList<>();
        if (program.get("development") instanceof Map<?, ?> dev) {
            for (String key : List.of("handler", "facade", "controller", "service", "dao")) {
                if (dev.get(key) != null) {
                    files.add(toJavaPath(String.valueOf(dev.get(key))));
                }
            }
        }
        if (program.get("services") instanceof List<?> services) {
            for (Object item : services) {
                if (item instanceof Map<?, ?> svc) {
                    if (svc.get("dtoIn") != null) {
                        files.add(toJavaPath(String.valueOf(svc.get("dtoIn"))));
                    }
                    if (svc.get("dtoOut") != null) {
                        files.add(toJavaPath(String.valueOf(svc.get("dtoOut"))));
                    }
                }
            }
        }
        return files;
    }

    private static List<String> collectDataFiles(Map<String, Object> program) {
        List<String> files = new ArrayList<>();
        if (program.get("data") instanceof Map<?, ?> data) {
            if (data.get("mapperXml") != null) {
                files.add("pdmg-service/src/main/resources/" + data.get("mapperXml"));
            }
            if (data.get("table") != null) {
                files.add("table:" + data.get("table"));
            }
        }
        return files;
    }

    private static List<String> collectOpsFiles(Map<String, Object> program) {
        List<String> files = new ArrayList<>();
        if (program.get("operations") instanceof Map<?, ?> ops) {
            if (ops.get("uiRoute") != null) {
                files.add("pdmg-ui/src/main/resources/static" + ops.get("uiRoute"));
            }
            if (ops.get("samples") instanceof List<?> samples) {
                for (Object s : samples) {
                    files.add("pdmg-ui/src/main/resources/" + s);
                }
            }
        }
        return files;
    }

    private static String toJavaPath(String fqcn) {
        return "pdmg-service/src/main/java/" + fqcn.replace('.', '/') + ".java";
    }

    private static boolean containsIgnoreCase(String value, String needleLower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needleLower);
    }
}
