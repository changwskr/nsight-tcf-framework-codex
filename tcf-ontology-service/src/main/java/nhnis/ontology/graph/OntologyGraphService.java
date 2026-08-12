package nhnis.ontology.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import nhnis.ontology.model.MetaTypes;
import nhnis.ontology.model.RelationPredicates;
import nhnis.ontology.ontology.OntologyRegistry;

@Service
public class OntologyGraphService {

    private final OntologyRegistry registry;

    public OntologyGraphService(OntologyRegistry registry) {
        this.registry = registry;
    }

    public Map<String, Object> buildProgramGraph(Map<String, Object> program) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();

        String programId = String.valueOf(program.get("programId"));
        String systemId = String.valueOf(program.getOrDefault("majorGroup", "MG"));
        String businessId = String.valueOf(program.getOrDefault("businessCode", ""));
        String functionId = String.valueOf(program.getOrDefault("functionCode", ""));

        addNode(nodes, MetaTypes.SYSTEM, systemId, Map.of("name", "Market Group Platform"));
        addNode(nodes, MetaTypes.BUSINESS, businessId, Map.of("name", program.get("businessCode")));
        addNode(nodes, MetaTypes.FUNCTION, functionId, Map.of(
                "name", program.getOrDefault("functionName", functionId)));
        addNode(nodes, MetaTypes.PROGRAM, programId, Map.of(
                "title", program.get("title"),
                "packageRoot", program.get("packageRoot")));

        addEdge(edges, edgeKeys, systemId, businessId, RelationPredicates.HAS_BUSINESS);
        addEdge(edges, edgeKeys, businessId, functionId, RelationPredicates.HAS_FUNCTION);
        addEdge(edges, edgeKeys, functionId, programId, RelationPredicates.HAS_PROGRAM);

        if (program.get("development") instanceof Map<?, ?> dev) {
            linkClass(nodes, edges, edgeKeys, programId, RelationPredicates.HANDLED_BY,
                    MetaTypes.JAVA_CLASS, "handler", dev.get("handler"));
            linkClass(nodes, edges, edgeKeys, programId, RelationPredicates.ORCHESTRATED_BY,
                    MetaTypes.JAVA_CLASS, "facade", dev.get("facade"));
            linkClass(nodes, edges, edgeKeys, programId, RelationPredicates.IMPLEMENTED_BY,
                    MetaTypes.JAVA_CLASS, "controller", dev.get("controller"));
            linkClass(nodes, edges, edgeKeys, programId, RelationPredicates.IMPLEMENTED_BY,
                    MetaTypes.JAVA_CLASS, "service", dev.get("service"));
            linkClass(nodes, edges, edgeKeys, programId, RelationPredicates.IMPLEMENTED_BY,
                    MetaTypes.JAVA_CLASS, "dao", dev.get("dao"));
        }

        if (program.get("data") instanceof Map<?, ?> data) {
            Object table = data.get("table");
            if (table instanceof List<?> list) {
                for (Object t : list) {
                    addNode(nodes, MetaTypes.TABLE, String.valueOf(t), Map.of("pk", data.get("pk")));
                    addEdge(edges, edgeKeys, programId, String.valueOf(t), RelationPredicates.PERSISTS_TO);
                }
            } else if (table != null) {
                addNode(nodes, MetaTypes.TABLE, String.valueOf(table), Map.of("pk", data.get("pk")));
                addEdge(edges, edgeKeys, programId, String.valueOf(table), RelationPredicates.PERSISTS_TO);
            }
            if (data.get("mapperXml") != null) {
                addNode(nodes, MetaTypes.MAPPER_XML, String.valueOf(data.get("mapperXml")), Map.of());
                addEdge(edges, edgeKeys, programId, String.valueOf(data.get("mapperXml")),
                        RelationPredicates.MAPPED_BY);
            }
        }

        if (program.get("operations") instanceof Map<?, ?> ops) {
            if (ops.get("uiRoute") != null) {
                addNode(nodes, MetaTypes.UI_ROUTE, String.valueOf(ops.get("uiRoute")), Map.of());
                addEdge(edges, edgeKeys, programId, String.valueOf(ops.get("uiRoute")),
                        RelationPredicates.EXPOSED_AT);
            }
            if (ops.get("exceptionCodes") instanceof List<?> codes) {
                for (Object code : codes) {
                    addNode(nodes, MetaTypes.EXCEPTION_CODE, String.valueOf(code), Map.of());
                    addEdge(edges, edgeKeys, programId, String.valueOf(code),
                            RelationPredicates.USES_EXCEPTION);
                }
            }
            if (ops.get("configKeys") instanceof List<?> keys) {
                for (Object key : keys) {
                    addNode(nodes, MetaTypes.CONFIG_KEY, String.valueOf(key), Map.of());
                    addEdge(edges, edgeKeys, programId, String.valueOf(key),
                            RelationPredicates.CONFIGURED_BY);
                }
            }
        }

        if (program.get("services") instanceof List<?> services) {
            for (Object item : services) {
                if (!(item instanceof Map<?, ?> svc) || svc.get("serviceId") == null) {
                    continue;
                }
                String serviceId = String.valueOf(svc.get("serviceId"));
                addNode(nodes, MetaTypes.SERVICE_ID, serviceId, Map.of("op", svc.get("op")));
                addEdge(edges, edgeKeys, programId, serviceId, RelationPredicates.PROVIDES_SERVICE);
                addEdge(edges, edgeKeys, serviceId, programId, RelationPredicates.BELONGS_TO_PROGRAM);

                if (program.get("development") instanceof Map<?, ?> dev
                        && dev.get("handler") != null) {
                    addEdge(edges, edgeKeys, serviceId, String.valueOf(dev.get("handler")),
                            RelationPredicates.HANDLED_BY);
                }
                if (svc.get("dtoIn") != null) {
                    addNode(nodes, MetaTypes.JAVA_CLASS, String.valueOf(svc.get("dtoIn")),
                            Map.of("role", "dto"));
                    addEdge(edges, edgeKeys, serviceId, String.valueOf(svc.get("dtoIn")),
                            RelationPredicates.USES_DTO);
                }
                if (svc.get("dtoOut") != null) {
                    addNode(nodes, MetaTypes.JAVA_CLASS, String.valueOf(svc.get("dtoOut")),
                            Map.of("role", "dto"));
                    addEdge(edges, edgeKeys, serviceId, String.valueOf(svc.get("dtoOut")),
                            RelationPredicates.USES_DTO);
                }
                if (svc.get("sqlIds") instanceof List<?> sqlIds) {
                    for (Object sqlId : sqlIds) {
                        addNode(nodes, MetaTypes.SQL_ID, String.valueOf(sqlId), Map.of());
                        addEdge(edges, edgeKeys, serviceId, String.valueOf(sqlId),
                                RelationPredicates.EXECUTES);
                    }
                }
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("path", List.of(systemId, businessId, functionId, programId));
        out.put("pathLabel", systemId + " → " + businessId + " → " + functionId + " → " + programId);
        out.put("nodes", nodes);
        out.put("edges", edges);
        return out;
    }

    public Map<String, Object> pathQuery(String system, String business, String function, String programOrService) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", Map.of(
                "system", system,
                "business", business,
                "function", function,
                "tail", programOrService));

        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> program : registry.listPrograms()) {
            if (!equalsIgnore(system, program.get("majorGroup"))) {
                continue;
            }
            if (!equalsIgnore(business, program.get("businessCode"))) {
                continue;
            }
            if (!equalsIgnore(function, program.get("functionCode"))) {
                continue;
            }
            String programId = String.valueOf(program.get("programId"));
            if (programOrService == null || programOrService.isBlank()
                    || programId.equalsIgnoreCase(programOrService)
                    || hasService(program, programOrService)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("programId", programId);
                row.put("title", program.get("title"));
                row.put("graph", buildProgramGraph(program));
                if (programOrService != null && looksLikeServiceId(programOrService)) {
                    row.put("serviceId", programOrService);
                    row.put("fourAxis", registry.fourAxisByServiceId(programOrService));
                }
                matched.add(row);
            }
        }
        result.put("status", matched.isEmpty() ? "NOT_FOUND" : "OK");
        result.put("matches", matched);
        result.put("example", "MG → CO → A → mgcoa8888S0");
        return result;
    }

    private static boolean hasService(Map<String, Object> program, String serviceId) {
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

    private static boolean looksLikeServiceId(String value) {
        return value != null && value.matches("^[a-z]{2}[a-z]{2}[a-z][0-9]{4}[SCUDAR][0-9A-Z]$");
    }

    private static boolean equalsIgnore(String expected, Object actual) {
        return expected != null && actual != null
                && expected.equalsIgnoreCase(String.valueOf(actual));
    }

    private static void linkClass(
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges,
            Set<String> edgeKeys,
            String fromProgram,
            String predicate,
            String type,
            String role,
            Object fqcn) {
        if (fqcn == null) {
            return;
        }
        String id = String.valueOf(fqcn);
        addNode(nodes, type, id, Map.of("role", role));
        addEdge(edges, edgeKeys, fromProgram, id, predicate);
    }

    private static void addNode(List<Map<String, Object>> nodes, String type, String id, Map<String, ?> attrs) {
        if (id == null || id.isBlank() || "null".equals(id)) {
            return;
        }
        boolean exists = nodes.stream().anyMatch(n -> id.equals(n.get("id")));
        if (exists) {
            return;
        }
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", type);
        node.put("id", id);
        attrs.forEach((k, v) -> {
            if (v != null) {
                node.put(k, v);
            }
        });
        nodes.add(node);
    }

    private static void addEdge(
            List<Map<String, Object>> edges,
            Set<String> edgeKeys,
            String from,
            String to,
            String predicate) {
        if (from == null || to == null || "null".equals(from) || "null".equals(to)) {
            return;
        }
        String key = from + "|" + predicate + "|" + to;
        if (!edgeKeys.add(key)) {
            return;
        }
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("from", from);
        edge.put("predicate", predicate);
        edge.put("to", to);
        edges.add(edge);
    }
}
