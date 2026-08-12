package nhnis.ontology.facade;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.graph.OntologyGraphService;
import nhnis.ontology.impact.ImpactAnalyzer;
import nhnis.ontology.ontology.OntologyRegistry;
import nhnis.ontology.query.OntologyQueryService;
import nhnis.ontology.store.OntologyStore;
import nhnis.ontology.support.ServiceIdParser;

/**
 * Single lookup facade (W10): YAML Hub + Concept/Relation Graph.
 * Graph is preferred for structure/impact paths; Registry supplies document/4-axis detail.
 */
@Service
public class OntologyFacade {

    private final OntologyRegistry registry;
    private final OntologyQueryService queryService;
    private final OntologyStore store;
    private final ImpactAnalyzer impactAnalyzer;
    private final OntologyGraphService graphService;

    public OntologyFacade(
            OntologyRegistry registry,
            OntologyQueryService queryService,
            OntologyStore store,
            ImpactAnalyzer impactAnalyzer,
            OntologyGraphService graphService) {
        this.registry = registry;
        this.queryService = queryService;
        this.store = store;
        this.impactAnalyzer = impactAnalyzer;
        this.graphService = graphService;
    }

    public Map<String, Object> catalog() {
        Map<String, Object> out = new LinkedHashMap<>(registry.catalog());
        out.put("graph", Map.of(
                "conceptCount", store.allConcepts().size(),
                "relationCount", store.allRelations().size(),
                "programsInGraph", store.findConceptsByType(ConceptType.PROGRAM).size(),
                "servicesInGraph", store.findConceptsByType(ConceptType.SERVICE_ID).size(),
                "runtimeComponents", store.findConceptsByType(ConceptType.RUNTIME_COMPONENT).size(),
                "byType", queryService.listConcepts(null, null).get("byType")));
        out.put("unified", true);
        return out;
    }

    public Optional<Map<String, Object>> program(String programId) {
        Optional<Map<String, Object>> yaml = registry.findProgram(programId);
        if (yaml.isEmpty() && store.findConcept(programId).isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        yaml.ifPresent(doc -> out.putAll(doc));
        try {
            out.put("graph", queryService.programServices(programId));
        } catch (IllegalArgumentException ignored) {
            out.put("graph", Map.of("status", "NOT_IN_GRAPH", "programId", programId));
        }
        out.put("sources", sources(yaml.isPresent(), store.findConcept(programId).isPresent()));
        return Optional.of(out);
    }

    public Optional<Map<String, Object>> service(String serviceId) {
        Map<String, Object> fourAxis = registry.fourAxisByServiceId(serviceId);
        Map<String, Object> graph = null;
        try {
            if (ServiceIdParser.isValid(serviceId)) {
                graph = queryService.serviceStructure(serviceId);
            }
        } catch (IllegalArgumentException ignored) {
            // not in graph
        }
        if (fourAxis.isEmpty() && graph == null) {
            return Optional.empty();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        if (!fourAxis.isEmpty()) {
            out.putAll(fourAxis);
        } else {
            out.put("serviceId", serviceId);
        }
        if (graph != null) {
            out.put("graph", graph);
        }
        out.put("sources", sources(!fourAxis.isEmpty(), graph != null));
        return Optional.of(out);
    }

    /**
     * Unified impact: legacy blastRadius (YAML) + graph reverse paths when applicable.
     */
    public Map<String, Object> impact(String from) {
        String needle = from == null ? "" : from.trim();
        Map<String, Object> legacy = impactAnalyzer.analyze(needle);

        Map<String, Object> out = new LinkedHashMap<>(legacy);
        out.put("unified", true);

        // Table → graph impact
        if (looksLikeTable(needle)) {
            try {
                out.put("graphImpact", queryService.impactByTable(needle));
            } catch (IllegalArgumentException e) {
                out.put("graphImpact", Map.of("status", "NOT_IN_GRAPH", "error", e.getMessage()));
            }
            return out;
        }

        // ServiceId → structure + tables
        if (ServiceIdParser.isValid(needle)) {
            try {
                out.put("graphStructure", queryService.serviceStructure(needle));
                out.put("graphTables", queryService.serviceTables(needle));
            } catch (IllegalArgumentException e) {
                out.put("graphStructure", Map.of("status", "NOT_IN_GRAPH", "error", e.getMessage()));
            }
        }

        // Program → services in graph
        if ("OK".equals(legacy.get("status")) && legacy.get("programId") != null) {
            try {
                out.put("graphProgram", queryService.programServices(String.valueOf(legacy.get("programId"))));
            } catch (IllegalArgumentException ignored) {
                // optional
            }
        }
        return out;
    }

    public Map<String, Object> path(String system, String business, String function, String program) {
        Map<String, Object> legacy = graphService.pathQuery(system, business, function, program);
        Map<String, Object> out = new LinkedHashMap<>(legacy);
        out.put("unified", true);
        try {
            out.put("graphBusinessTree", queryService.businessTree(business));
        } catch (IllegalArgumentException e) {
            out.put("graphBusinessTree", Map.of("status", "NOT_IN_GRAPH", "error", e.getMessage()));
        }
        if (program != null && ServiceIdParser.isValid(program)) {
            try {
                out.put("graphStructure", queryService.serviceStructure(program));
            } catch (IllegalArgumentException ignored) {
                // optional
            }
        } else if (program != null && !program.isBlank()) {
            try {
                out.put("graphProgram", queryService.programServices(program));
            } catch (IllegalArgumentException ignored) {
                // optional
            }
        }
        return out;
    }

    public Map<String, Object> runtimeTxChain() {
        Map<String, Object> out = new LinkedHashMap<>(queryService.runtimeTxChain());
        out.put("unified", true);
        out.put("yamlRuntime", registry.runtimeBundle());
        return out;
    }

    public Map<String, Object> consistency() {
        int yamlPrograms = registry.listPrograms().size();
        int graphPrograms = store.findConceptsByType(ConceptType.PROGRAM).size();
        int yamlServices = ((java.util.List<?>) registry.catalog().get("services")).size();
        int graphServices = store.findConceptsByType(ConceptType.SERVICE_ID).size();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("yamlPrograms", yamlPrograms);
        out.put("graphPrograms", graphPrograms);
        out.put("yamlServices", yamlServices);
        out.put("graphServices", graphServices);
        out.put("programsAligned", yamlPrograms == graphPrograms);
        out.put("servicesAligned", yamlServices == graphServices);
        out.put("status", (yamlPrograms == graphPrograms && yamlServices == graphServices) ? "ALIGNED" : "DRIFT");
        return out;
    }

    private static boolean looksLikeTable(String needle) {
        String u = needle.toUpperCase(Locale.ROOT);
        return u.startsWith("TB_") || u.startsWith("TABLE:");
    }

    private static Map<String, Object> sources(boolean yaml, boolean graph) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("yaml", yaml);
        s.put("graph", graph);
        return s;
    }
}
