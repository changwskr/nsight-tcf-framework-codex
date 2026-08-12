package nhnis.ontology.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import nhnis.ontology.domain.concept.ConceptIds;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.concept.ServiceIdParts;
import nhnis.ontology.domain.relation.GraphType;
import nhnis.ontology.domain.relation.OntologyRelation;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.loader.TxRuntimeGraphLoader;
import nhnis.ontology.store.OntologyStore;
import nhnis.ontology.support.ServiceIdParser;

@Service
public class OntologyQueryService {

    private static final List<RelationType> STRUCTURE_PREDICATES = List.of(
            RelationType.HANDLED_BY,
            RelationType.CALLS,
            RelationType.USES,
            RelationType.EXECUTES,
            RelationType.ACCESSES,
            RelationType.HAS_COLUMN);

    private static final List<RelationType> IMPACT_REVERSE = List.of(
            RelationType.ACCESSES,
            RelationType.EXECUTES,
            RelationType.USES,
            RelationType.CALLS,
            RelationType.HANDLED_BY,
            RelationType.PROVIDES_SERVICE,
            RelationType.BELONGS_TO_PROGRAM,
            RelationType.HAS_PROGRAM,
            RelationType.HAS_FUNCTION,
            RelationType.HAS_BUSINESS);

    private final OntologyStore store;

    public OntologyQueryService(OntologyStore store) {
        this.store = store;
    }

    public Map<String, Object> getConcept(String id) {
        OntologyConcept concept = store.findConcept(id)
                .orElseThrow(() -> new IllegalArgumentException("Concept not found: " + id));
        Map<String, Object> out = new LinkedHashMap<>(concept.toMap());
        out.put("outgoing", store.findRelationsFrom(concept.getId()).stream().map(OntologyRelation::toMap).toList());
        out.put("incoming", store.findRelationsTo(concept.getId()).stream().map(OntologyRelation::toMap).toList());
        return out;
    }

    public Map<String, Object> parseServiceId(String serviceId) {
        ServiceIdParts parts = ServiceIdParser.parse(serviceId);
        Map<String, Object> out = new LinkedHashMap<>(parts.toAttributeMap());
        out.put("canonical", ServiceIdParser.canonical(serviceId));
        out.put("conceptId", ConceptIds.service(parts.getFullServiceId()));
        out.put("registered", store.findConcept(ConceptIds.service(parts.getFullServiceId())).isPresent());
        return out;
    }

    public Map<String, Object> serviceStructure(String serviceId) {
        String canonical = ServiceIdParser.canonical(serviceId);
        OntologyConcept service = store.findConcept(canonical)
                .or(() -> store.findConcept(ConceptIds.service(canonical)))
                .orElseThrow(() -> new IllegalArgumentException("ServiceId not found: " + serviceId));

        List<Map<String, Object>> designSteps = store.traverse(
                service.getId(), 10, STRUCTURE_PREDICATES, GraphType.DESIGN);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("serviceId", canonical);
        out.put("concept", service.toMap());
        out.put("classification", classificationPath(canonical));
        out.put("structure", designSteps);
        out.put("summary", summarizeStructure(service.getId()));
        out.put("tables", collectTargets(designSteps, ConceptType.TABLE));
        out.put("sqlIds", collectTargets(designSteps, ConceptType.SQL_ID));
        out.put("mappers", collectTargets(designSteps, ConceptType.MAPPER));
        return out;
    }

    public Map<String, Object> programServices(String programId) {
        OntologyConcept program = store.findConcept(programId)
                .or(() -> store.findConcept(ConceptIds.programFromShortId(normalizeProgram(programId))))
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + programId));
        List<Map<String, Object>> services = store.findRelations(program.getId(), RelationType.PROVIDES_SERVICE)
                .stream()
                .map(r -> store.findConcept(r.getToId()).map(OntologyConcept::toMap).orElse(Map.of("id", r.getToId())))
                .collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("program", program.toMap());
        out.put("services", services);
        return out;
    }

    public Map<String, Object> handlerServices(String handler) {
        OntologyConcept component = store.findConcept(handler)
                .or(() -> store.findConcept(ConceptIds.component(handler)))
                .orElseThrow(() -> new IllegalArgumentException("Handler not found: " + handler));
        List<Map<String, Object>> services = store.findRelationsTo(component.getId()).stream()
                .filter(r -> r.getPredicate() == RelationType.HANDLED_BY)
                .map(r -> store.findConcept(r.getFromId()).map(OntologyConcept::toMap).orElse(Map.of("id", r.getFromId())))
                .collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("handler", component.toMap());
        out.put("services", services);
        return out;
    }

    public Map<String, Object> tableServices(String table) {
        Map<String, Object> impact = impactByTable(table);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("table", impact.get("table"));
        out.put("serviceIds", impact.get("affectedServiceIds"));
        out.put("paths", impact.get("paths"));
        out.put("affectedHandlers", impact.get("affectedHandlers"));
        out.put("affectedPrograms", impact.get("affectedPrograms"));
        out.put("affectedFunctions", impact.get("affectedFunctions"));
        out.put("affectedBusinesses", impact.get("affectedBusinesses"));
        out.put("affectedSystems", impact.get("affectedSystems"));
        return out;
    }

    public Map<String, Object> serviceTables(String serviceId) {
        Map<String, Object> structure = serviceStructure(serviceId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("serviceId", structure.get("serviceId"));
        out.put("tables", structure.get("tables"));
        out.put("sqlIds", structure.get("sqlIds"));
        out.put("mappers", structure.get("mappers"));
        out.put("paths", structure.get("structure"));
        return out;
    }

    public Map<String, Object> businessTree(String businessCode) {
        OntologyConcept business = store.findConcept(businessCode)
                .or(() -> store.findConcept(ConceptIds.business("MG", businessCode)))
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessCode));

        List<Map<String, Object>> functions = new ArrayList<>();
        for (OntologyRelation fRel : store.findRelations(business.getId(), RelationType.HAS_FUNCTION)) {
            OntologyConcept function = store.findConcept(fRel.getToId()).orElse(null);
            if (function == null) {
                continue;
            }
            List<Map<String, Object>> programs = new ArrayList<>();
            for (OntologyRelation pRel : store.findRelations(function.getId(), RelationType.HAS_PROGRAM)) {
                OntologyConcept program = store.findConcept(pRel.getToId()).orElse(null);
                if (program == null) {
                    continue;
                }
                List<Map<String, Object>> services = store.findRelations(program.getId(), RelationType.PROVIDES_SERVICE)
                        .stream()
                        .map(r -> store.findConcept(r.getToId()).map(OntologyConcept::toMap).orElse(Map.of("id", r.getToId())))
                        .collect(Collectors.toList());
                Map<String, Object> pNode = new LinkedHashMap<>(program.toMap());
                pNode.put("services", services);
                programs.add(pNode);
            }
            Map<String, Object> fNode = new LinkedHashMap<>(function.toMap());
            fNode.put("programs", programs);
            functions.add(fNode);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("business", business.toMap());
        out.put("functions", functions);
        return out;
    }

    public Map<String, Object> impactByTable(String tableName) {
        OntologyConcept table = resolveTable(tableName);
        List<List<Map<String, Object>>> paths = store.reversePaths(
                table.getId(), 16, IMPACT_REVERSE, GraphType.DESIGN);

        Set<String> sqlIds = new LinkedHashSet<>();
        Set<String> mappers = new LinkedHashSet<>();
        Set<String> daos = new LinkedHashSet<>();
        Set<String> services = new LinkedHashSet<>();
        Set<String> facades = new LinkedHashSet<>();
        Set<String> handlers = new LinkedHashSet<>();
        Set<String> serviceIds = new LinkedHashSet<>();
        Set<String> programs = new LinkedHashSet<>();
        Set<String> functions = new LinkedHashSet<>();
        Set<String> businesses = new LinkedHashSet<>();
        Set<String> systems = new LinkedHashSet<>();

        for (List<Map<String, Object>> path : paths) {
            for (Map<String, Object> step : path) {
                collectImpactNode(String.valueOf(step.get("from")),
                        sqlIds, mappers, daos, services, facades, handlers, serviceIds,
                        programs, functions, businesses, systems);
                collectImpactNode(String.valueOf(step.get("to")),
                        sqlIds, mappers, daos, services, facades, handlers, serviceIds,
                        programs, functions, businesses, systems);
            }
        }

        // ServiceIds that reach this table (forward DESIGN)
        for (OntologyConcept sid : store.findConceptsByType(ConceptType.SERVICE_ID)) {
            boolean reaches = store.traverse(sid.getId(), 10, STRUCTURE_PREDICATES, GraphType.DESIGN).stream()
                    .anyMatch(s -> table.getId().equals(s.get("to")));
            if (reaches) {
                serviceIds.add(sid.getName());
            }
        }

        // Enrich all layers from each affected ServiceId (implements §11 impact response)
        for (String sidName : new ArrayList<>(serviceIds)) {
            enrichLayersFromServiceId(sidName,
                    sqlIds, mappers, daos, services, facades, handlers, programs, functions, businesses, systems);
        }

        if (!serviceIds.isEmpty()) {
            paths = ensureEndToEndPaths(paths, table, serviceIds);
        }

        boolean pathReachesSystem = paths.stream()
                .flatMap(List::stream)
                .anyMatch(s -> ConceptType.SYSTEM.name().equals(String.valueOf(s.get("fromType"))));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("table", table.toMap());
        out.put("affectedSqlIds", new ArrayList<>(sqlIds));
        out.put("affectedMappers", new ArrayList<>(mappers));
        out.put("affectedDaos", new ArrayList<>(daos));
        out.put("affectedServices", new ArrayList<>(services));
        out.put("affectedFacades", new ArrayList<>(facades));
        out.put("affectedHandlers", new ArrayList<>(handlers));
        out.put("affectedServiceIds", new ArrayList<>(serviceIds));
        out.put("affectedPrograms", new ArrayList<>(programs));
        out.put("affectedFunctions", new ArrayList<>(functions));
        out.put("affectedBusinesses", new ArrayList<>(businesses));
        out.put("affectedSystems", new ArrayList<>(systems));
        out.put("paths", paths);
        out.put("pathStatus", pathReachesSystem ? "COMPLETE" : (paths.isEmpty() ? "UNRESOLVED" : "PARTIAL"));
        return out;
    }

    private void enrichLayersFromServiceId(
            String serviceIdName,
            Set<String> sqlIds,
            Set<String> mappers,
            Set<String> daos,
            Set<String> services,
            Set<String> facades,
            Set<String> handlers,
            Set<String> programs,
            Set<String> functions,
            Set<String> businesses,
            Set<String> systems) {
        OntologyConcept sid = store.findConcept(serviceIdName)
                .or(() -> store.findConcept(ConceptIds.service(serviceIdName)))
                .orElse(null);
        if (sid == null) {
            return;
        }

        for (OntologyRelation rel : store.findRelations(sid.getId(), RelationType.HANDLED_BY)) {
            store.findConcept(rel.getToId()).ifPresent(c -> handlers.add(c.getName()));
        }
        for (OntologyRelation rel : store.findRelations(sid.getId(), RelationType.BELONGS_TO_PROGRAM)) {
            store.findConcept(rel.getToId()).ifPresent(program -> {
                programs.add(program.getName());
                collectClassificationAncestors(program.getId(), functions, businesses, systems);
            });
        }

        for (Map<String, Object> step : store.traverse(sid.getId(), 10, STRUCTURE_PREDICATES, GraphType.DESIGN)) {
            collectImpactNode(String.valueOf(step.get("to")),
                    sqlIds, mappers, daos, services, facades, handlers, new LinkedHashSet<>(),
                    programs, functions, businesses, systems);
        }
    }

    /** Program ← Function ← Business ← System */
    private void collectClassificationAncestors(
            String programId,
            Set<String> functions,
            Set<String> businesses,
            Set<String> systems) {
        for (OntologyRelation in : store.findRelationsTo(programId)) {
            if (in.getPredicate() != RelationType.HAS_PROGRAM) {
                continue;
            }
            store.findConcept(in.getFromId()).ifPresent(function -> {
                functions.add(function.getName());
                for (OntologyRelation bin : store.findRelationsTo(function.getId())) {
                    if (bin.getPredicate() != RelationType.HAS_FUNCTION) {
                        continue;
                    }
                    store.findConcept(bin.getFromId()).ifPresent(business -> {
                        businesses.add(business.getName());
                        for (OntologyRelation sin : store.findRelationsTo(business.getId())) {
                            if (sin.getPredicate() == RelationType.HAS_BUSINESS) {
                                store.findConcept(sin.getFromId()).ifPresent(sys -> systems.add(sys.getName()));
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * Keep reverse-discovered paths; if none reach System, synthesize end-to-end proof paths.
     */
    private List<List<Map<String, Object>>> ensureEndToEndPaths(
            List<List<Map<String, Object>>> paths,
            OntologyConcept table,
            Set<String> serviceIds) {
        boolean hasSystem = paths.stream()
                .flatMap(List::stream)
                .anyMatch(s -> ConceptType.SYSTEM.name().equals(String.valueOf(s.get("fromType"))));
        if (hasSystem) {
            return paths;
        }
        List<List<Map<String, Object>>> synthesized = synthesizeImpactPaths(table, serviceIds);
        if (synthesized.isEmpty()) {
            return paths;
        }
        List<List<Map<String, Object>>> merged = new ArrayList<>(paths);
        merged.addAll(synthesized);
        return merged;
    }

    /**
     * Rebuild reverse path from real DESIGN structure edges only.
     * Never invent relations that do not exist in OntologyStore
     * (e.g. ServiceId -HANDLED_BY→ Table is forbidden).
     */
    private List<List<Map<String, Object>>> synthesizeImpactPaths(OntologyConcept table, Set<String> serviceIds) {
        List<List<Map<String, Object>>> out = new ArrayList<>();
        for (String sidName : serviceIds) {
            OntologyConcept sid = store.findConcept(sidName)
                    .or(() -> store.findConcept(ConceptIds.service(sidName)))
                    .orElse(null);
            if (sid == null) {
                continue;
            }
            List<Map<String, Object>> forward = store.traverse(sid.getId(), 10, STRUCTURE_PREDICATES, GraphType.DESIGN);
            boolean touchesTable = forward.stream().anyMatch(s -> table.getId().equals(s.get("to")));
            if (!touchesTable) {
                continue;
            }
            List<Map<String, Object>> reverse = new ArrayList<>();
            for (int i = forward.size() - 1; i >= 0; i--) {
                Map<String, Object> step = forward.get(i);
                Map<String, Object> rev = new LinkedHashMap<>();
                rev.put("to", step.get("to"));
                rev.put("predicate", step.get("predicate"));
                rev.put("from", step.get("from"));
                rev.put("graphType", step.get("graphType"));
                rev.put("note", "replay-from-design-structure");
                store.findConcept(String.valueOf(step.get("from"))).ifPresent(c -> {
                    rev.put("fromType", c.getType().name());
                    rev.put("fromName", c.getName());
                });
                reverse.add(rev);
            }
            prependClassificationReverse(reverse, sid);
            if (!reverse.isEmpty()) {
                out.add(reverse);
            }
        }
        return out;
    }

    /** Prepend System→Business→Function→Program→ServiceId as reverse steps onto path. */
    private void prependClassificationReverse(List<Map<String, Object>> reverse, OntologyConcept sid) {
        List<Map<String, Object>> classification = new ArrayList<>();
        OntologyConcept program = store.findRelations(sid.getId(), RelationType.BELONGS_TO_PROGRAM).stream()
                .map(r -> store.findConcept(r.getToId()).orElse(null))
                .filter(c -> c != null)
                .findFirst()
                .orElse(null);
        if (program == null) {
            return;
        }
        classification.add(edgeStep(sid.getId(), RelationType.BELONGS_TO_PROGRAM, program.getId()));

        OntologyConcept function = store.findRelationsTo(program.getId()).stream()
                .filter(r -> r.getPredicate() == RelationType.HAS_PROGRAM)
                .map(r -> store.findConcept(r.getFromId()).orElse(null))
                .filter(c -> c != null)
                .findFirst()
                .orElse(null);
        if (function != null) {
            classification.add(edgeStep(program.getId(), RelationType.HAS_PROGRAM, function.getId()));
            OntologyConcept business = store.findRelationsTo(function.getId()).stream()
                    .filter(r -> r.getPredicate() == RelationType.HAS_FUNCTION)
                    .map(r -> store.findConcept(r.getFromId()).orElse(null))
                    .filter(c -> c != null)
                    .findFirst()
                    .orElse(null);
            if (business != null) {
                classification.add(edgeStep(function.getId(), RelationType.HAS_FUNCTION, business.getId()));
                OntologyConcept system = store.findRelationsTo(business.getId()).stream()
                        .filter(r -> r.getPredicate() == RelationType.HAS_BUSINESS)
                        .map(r -> store.findConcept(r.getFromId()).orElse(null))
                        .filter(c -> c != null)
                        .findFirst()
                        .orElse(null);
                if (system != null) {
                    classification.add(edgeStep(business.getId(), RelationType.HAS_BUSINESS, system.getId()));
                }
            }
        }
        // reversePaths convention: from = ancestor, to = descendant
        reverse.addAll(classification);
    }

    private Map<String, Object> edgeStep(String downstreamId, RelationType predicate, String upstreamId) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("to", downstreamId);
        step.put("predicate", predicate.name());
        step.put("from", upstreamId);
        step.put("graphType", GraphType.DESIGN.name());
        store.findConcept(upstreamId).ifPresent(c -> {
            step.put("fromType", c.getType().name());
            step.put("fromName", c.getName());
        });
        step.put("note", "classification-ancestor");
        return step;
    }

    public Map<String, Object> chainForService(String serviceId) {
        return serviceStructure(serviceId);
    }

    /**
     * RUNTIME TX pipeline: DefaultFilter → … → Response/Interceptor.
     */
    public Map<String, Object> runtimeTxChain() {
        String start = ConceptIds.runtime("DefaultFilter");
        if (store.findConcept(start).isEmpty()) {
            throw new IllegalArgumentException("RUNTIME graph not loaded (missing DefaultFilter)");
        }
        List<Map<String, Object>> flow = store.traverse(
                start, 20, List.of(RelationType.FLOWS_TO), GraphType.RUNTIME);

        List<String> summary = new ArrayList<>();
        summary.add("DefaultFilter");
        for (Map<String, Object> step : flow) {
            Object name = step.get("toName");
            if (name != null) {
                summary.add(String.valueOf(name));
            }
        }

        String uow = ConceptIds.unitOfWork(TxRuntimeGraphLoader.UOW_NAME);
        List<Map<String, Object>> txLinks = store.findRelationsTo(uow).stream()
                .filter(r -> r.getGraphType() == GraphType.RUNTIME)
                .map(OntologyRelation::toMap)
                .collect(Collectors.toList());

        List<Map<String, Object>> dispatches = store.findByPredicate(RelationType.DISPATCHES_TO).stream()
                .filter(r -> r.getGraphType() == GraphType.RUNTIME)
                .map(OntologyRelation::toMap)
                .collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("graphType", GraphType.RUNTIME.name());
        out.put("source", TxRuntimeGraphLoader.SOURCE);
        out.put("summary", summary);
        out.put("flow", flow);
        out.put("unitOfWork", store.findConcept(uow).map(OntologyConcept::toMap).orElse(Map.of()));
        out.put("transactionLinks", txLinks);
        out.put("dispatches", dispatches);
        out.put("note", "DESIGN Handler→Facade CALLS is separate from this RUNTIME pipeline");
        return out;
    }

    public Map<String, Object> meta() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("conceptTypes", List.of(ConceptType.values()));
        out.put("relationTypes", List.of(RelationType.values()));
        out.put("graphTypes", List.of(GraphType.values()));
        out.put("counts", Map.of(
                "concepts", store.allConcepts().size(),
                "relations", store.allRelations().size()));
        out.put("byType", countByType());
        return out;
    }

    /**
     * Architecture object catalog for Workbench (all Ontology concepts).
     */
    public Map<String, Object> listConcepts(String typeFilter, String keyword) {
        String type = typeFilter == null ? "" : typeFilter.trim().toUpperCase(Locale.ROOT);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        List<Map<String, Object>> objects = store.allConcepts().stream()
                .sorted(Comparator
                        .comparing((OntologyConcept c) -> c.getType().name())
                        .thenComparing(OntologyConcept::getName, String.CASE_INSENSITIVE_ORDER))
                .filter(c -> type.isBlank() || type.equals("ALL") || c.getType().name().equals(type))
                .filter(c -> {
                    if (kw.isBlank()) {
                        return true;
                    }
                    String hay = (c.getId() + " " + c.getName() + " " + c.getDescription()).toLowerCase(Locale.ROOT);
                    return hay.contains(kw);
                })
                .map(this::architectureObjectSummary)
                .collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalConcepts", store.allConcepts().size());
        out.put("totalRelations", store.allRelations().size());
        out.put("byType", countByType());
        out.put("filter", Map.of(
                "type", type.isBlank() ? "ALL" : type,
                "keyword", keyword == null ? "" : keyword));
        out.put("count", objects.size());
        out.put("objects", objects);
        out.put("note", "Architecture objects = Ontology concept graph nodes (programs/services/components/tables/runtime…)");
        return out;
    }

    public Map<String, Object> snapshot() {
        return store.snapshot();
    }

    private Map<String, Long> countByType() {
        Map<String, Long> byType = new LinkedHashMap<>();
        for (ConceptType t : ConceptType.values()) {
            byType.put(t.name(), 0L);
        }
        for (OntologyConcept c : store.allConcepts()) {
            byType.merge(c.getType().name(), 1L, Long::sum);
        }
        return byType;
    }

    private Map<String, Object> architectureObjectSummary(OntologyConcept c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("type", c.getType().name());
        m.put("name", c.getName());
        m.put("status", c.getStatus() == null ? "ACTIVE" : c.getStatus().name());
        m.put("description", c.getDescription());
        if (c.attr("role") != null) {
            m.put("role", c.attr("role"));
        }
        if (c.attr("pk") != null) {
            m.put("pk", c.attr("pk"));
        }
        if (c.attr("op") != null) {
            m.put("op", c.attr("op"));
        }
        if (c.getProvenance() != null) {
            m.put("provenance", c.getProvenance().toMap());
            m.put("verificationStatus",
                    c.getProvenance().getVerificationStatus() == null
                            ? "UNRESOLVED"
                            : c.getProvenance().getVerificationStatus().name());
            m.put("sourcePath", c.getProvenance().getSourcePath());
        } else {
            m.put("verificationStatus", "UNRESOLVED");
        }
        m.put("outgoingCount", store.findRelationsFrom(c.getId()).size());
        m.put("incomingCount", store.findRelationsTo(c.getId()).size());
        return m;
    }

    private void collectImpactNode(
            String id,
            Set<String> sqlIds,
            Set<String> mappers,
            Set<String> daos,
            Set<String> services,
            Set<String> facades,
            Set<String> handlers,
            Set<String> serviceIds,
            Set<String> programs,
            Set<String> functions,
            Set<String> businesses,
            Set<String> systems) {
        store.findConcept(id).ifPresent(c -> {
            switch (c.getType()) {
                case SQL_ID -> sqlIds.add(c.getName());
                case MAPPER -> mappers.add(c.getName());
                case COMPONENT -> {
                    Object role = c.attr("role");
                    if ("DAO".equals(role)) {
                        daos.add(c.getName());
                    } else if ("SERVICE".equals(role)) {
                        services.add(c.getName());
                    } else if ("FACADE".equals(role)) {
                        facades.add(c.getName());
                    } else if ("HANDLER".equals(role)) {
                        handlers.add(c.getName());
                    }
                }
                case SERVICE_ID -> serviceIds.add(c.getName());
                case PROGRAM -> programs.add(c.getName());
                case FUNCTION -> functions.add(c.getName());
                case BUSINESS -> businesses.add(c.getName());
                case SYSTEM -> systems.add(c.getName());
                default -> {
                }
            }
        });
    }

    private OntologyConcept resolveTable(String table) {
        String raw = table.trim();
        return store.findConceptOfType(raw, ConceptType.TABLE)
                .or(() -> store.findConceptOfType(ConceptIds.table("RDW", raw), ConceptType.TABLE))
                .or(() -> {
                    if (raw.contains(":")) {
                        String[] parts = raw.split(":");
                        if (parts.length >= 2) {
                            return store.findConceptOfType(
                                    ConceptIds.table(parts[parts.length - 2], parts[parts.length - 1]),
                                    ConceptType.TABLE);
                        }
                    }
                    return Optional.empty();
                })
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + table));
    }

    private List<Map<String, Object>> classificationPath(String serviceId) {
        ServiceIdParts parts = ServiceIdParser.parse(serviceId);
        List<Map<String, Object>> list = new ArrayList<>();
        addStep(list, ConceptIds.system(parts.getGroupCode()), RelationType.HAS_BUSINESS,
                ConceptIds.business(parts.getGroupCode(), parts.getBusinessCode()), ConceptType.BUSINESS);
        addStep(list, ConceptIds.business(parts.getGroupCode(), parts.getBusinessCode()), RelationType.HAS_FUNCTION,
                ConceptIds.function(parts.getGroupCode(), parts.getBusinessCode(), parts.getFunctionCode()),
                ConceptType.FUNCTION);
        addStep(list,
                ConceptIds.function(parts.getGroupCode(), parts.getBusinessCode(), parts.getFunctionCode()),
                RelationType.HAS_PROGRAM,
                ConceptIds.program(parts.getGroupCode(), parts.getBusinessCode(), parts.getFunctionCode(),
                        parts.getProgramNo()),
                ConceptType.PROGRAM);
        addStep(list,
                ConceptIds.program(parts.getGroupCode(), parts.getBusinessCode(), parts.getFunctionCode(),
                        parts.getProgramNo()),
                RelationType.PROVIDES_SERVICE,
                ConceptIds.service(parts.getFullServiceId()),
                ConceptType.SERVICE_ID);
        return list;
    }

    private List<String> summarizeStructure(String serviceConceptId) {
        List<String> summary = new ArrayList<>();
        store.findConcept(serviceConceptId).ifPresent(c -> summary.add(c.getName()));
        String current = serviceConceptId;
        for (int i = 0; i < 10; i++) {
            var next = store.findRelationsFrom(current).stream()
                    .filter(r -> r.getGraphType() == GraphType.DESIGN)
                    .filter(r -> STRUCTURE_PREDICATES.contains(r.getPredicate()))
                    .sorted((a, b) -> {
                        int p = a.getPredicate().name().compareTo(b.getPredicate().name());
                        return p != 0 ? p : a.getToId().compareTo(b.getToId());
                    })
                    .findFirst();
            if (next.isEmpty()) {
                break;
            }
            current = next.get().getToId();
            store.findConcept(current).ifPresent(c -> summary.add(c.getName()));
        }
        return summary;
    }

    private List<Map<String, Object>> collectTargets(List<Map<String, Object>> steps, ConceptType type) {
        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> step : steps) {
            String to = String.valueOf(step.get("to"));
            store.findConcept(to).ifPresent(c -> {
                if (c.getType() == type && seen.add(c.getId())) {
                    out.add(c.toMap());
                }
            });
        }
        return out;
    }

    private void addStep(
            List<Map<String, Object>> list,
            String from,
            RelationType predicate,
            String to,
            ConceptType toType) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("from", from);
        step.put("predicate", predicate.name());
        step.put("to", to);
        step.put("toType", toType.name());
        boolean exists = store.findRelations(from, predicate).stream().anyMatch(r -> to.equals(r.getToId()));
        step.put("relationStatus", exists ? "PRESENT" : "INFERRED");
        step.put("verificationStatus", "DISCOVERED");
        step.put("origin", exists ? "ONTOLOGY_RELATION" : "SERVICEID_PARSE");
        step.put("note", exists
                ? "PRESENT means graph edge exists; not Source VERIFIED"
                : "INFERRED from ServiceId parse path");
        list.add(step);
    }

    private static String normalizeProgram(String programId) {
        return programId.toLowerCase(Locale.ROOT).replace("program:", "");
    }
}
