package nhnis.ontology.store;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.relation.GraphType;
import nhnis.ontology.domain.relation.OntologyRelation;
import nhnis.ontology.domain.relation.RelationType;

/**
 * In-memory Concept / Relation store (Ontology 1.0).
 */
@Component
public class OntologyStore {

    private final Map<String, OntologyConcept> concepts = new ConcurrentHashMap<>();
    private final Map<String, OntologyRelation> relations = new ConcurrentHashMap<>();
    /** alias (short name / serviceId / programId) → stable concept id */
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    public void putConcept(OntologyConcept concept) {
        concepts.put(concept.getId(), concept);
        aliases.put(concept.getId().toLowerCase(Locale.ROOT), concept.getId());
        aliases.put(concept.getName().toLowerCase(Locale.ROOT), concept.getId());
        Object shortId = concept.attr("shortId");
        if (shortId != null) {
            aliases.put(String.valueOf(shortId).toLowerCase(Locale.ROOT), concept.getId());
        }
        Object fullServiceId = concept.attr("fullServiceId");
        if (fullServiceId != null) {
            aliases.put(String.valueOf(fullServiceId).toLowerCase(Locale.ROOT), concept.getId());
        }
        Object programId = concept.attr("programId");
        if (programId != null && concept.getType() == ConceptType.PROGRAM) {
            aliases.put(String.valueOf(programId).toLowerCase(Locale.ROOT), concept.getId());
        }
        Object className = concept.attr("className");
        if (className != null) {
            aliases.put(String.valueOf(className).toLowerCase(Locale.ROOT), concept.getId());
            int idx = String.valueOf(className).lastIndexOf('.');
            if (idx > 0) {
                aliases.put(String.valueOf(className).substring(idx + 1).toLowerCase(Locale.ROOT), concept.getId());
            }
        }
        // Only TABLE concepts own the physical table-name alias.
        // Column.tableName must not pollute generic alias lookup (impact resolve bug).
        Object tableName = concept.attr("tableName");
        if (tableName != null && concept.getType() == ConceptType.TABLE) {
            aliases.put(String.valueOf(tableName).toLowerCase(Locale.ROOT), concept.getId());
        }
    }

    public void putRelation(OntologyRelation relation) {
        relations.put(relation.edgeKey(), relation);
    }

    public Optional<OntologyConcept> findConcept(String idOrAlias) {
        if (idOrAlias == null || idOrAlias.isBlank()) {
            return Optional.empty();
        }
        OntologyConcept direct = concepts.get(idOrAlias);
        if (direct != null) {
            return Optional.of(direct);
        }
        String resolved = aliases.get(idOrAlias.toLowerCase(Locale.ROOT));
        if (resolved != null) {
            return Optional.ofNullable(concepts.get(resolved));
        }
        return Optional.empty();
    }

    /**
     * Type-specific resolve: generic alias is accepted only when the hit matches {@code type};
     * otherwise scans concepts of that type by id/name/tableName.
     */
    public Optional<OntologyConcept> findConceptOfType(String idOrAlias, ConceptType type) {
        if (idOrAlias == null || idOrAlias.isBlank() || type == null) {
            return Optional.empty();
        }
        OntologyConcept direct = concepts.get(idOrAlias);
        if (direct != null && direct.getType() == type) {
            return Optional.of(direct);
        }
        String key = idOrAlias.toLowerCase(Locale.ROOT);
        String resolved = aliases.get(key);
        if (resolved != null) {
            OntologyConcept viaAlias = concepts.get(resolved);
            if (viaAlias != null && viaAlias.getType() == type) {
                return Optional.of(viaAlias);
            }
        }
        return concepts.values().stream()
                .filter(c -> c.getType() == type)
                .filter(c -> c.getId().equalsIgnoreCase(idOrAlias)
                        || c.getName().equalsIgnoreCase(idOrAlias)
                        || key.equals(String.valueOf(c.attr("tableName") == null ? "" : c.attr("tableName"))
                                .toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    public String resolveId(String idOrAlias) {
        return findConcept(idOrAlias).map(OntologyConcept::getId).orElse(idOrAlias);
    }

    public Collection<OntologyConcept> allConcepts() {
        return Collections.unmodifiableCollection(concepts.values());
    }

    public List<OntologyConcept> findConceptsByType(ConceptType type) {
        return concepts.values().stream()
                .filter(c -> c.getType() == type)
                .collect(Collectors.toList());
    }

    public Collection<OntologyRelation> allRelations() {
        return Collections.unmodifiableCollection(relations.values());
    }

    public List<OntologyRelation> findRelationsFrom(String fromIdOrAlias) {
        String fromId = resolveId(fromIdOrAlias);
        return relations.values().stream()
                .filter(r -> r.getFromId().equals(fromId))
                .collect(Collectors.toList());
    }

    public List<OntologyRelation> findRelationsTo(String toIdOrAlias) {
        String toId = resolveId(toIdOrAlias);
        return relations.values().stream()
                .filter(r -> r.getToId().equals(toId))
                .collect(Collectors.toList());
    }

    public List<OntologyRelation> findRelations(String fromIdOrAlias, RelationType predicate) {
        String fromId = resolveId(fromIdOrAlias);
        RelationType canonical = predicate.canonical();
        return relations.values().stream()
                .filter(r -> r.getFromId().equals(fromId) && r.getPredicate() == canonical)
                .collect(Collectors.toList());
    }

    public List<OntologyRelation> findByPredicate(RelationType predicate) {
        RelationType canonical = predicate.canonical();
        return relations.values().stream()
                .filter(r -> r.getPredicate() == canonical)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> traverse(
            String startIdOrAlias,
            int maxDepth,
            List<RelationType> predicates,
            GraphType graphType) {
        List<Map<String, Object>> path = new ArrayList<>();
        walk(resolveId(startIdOrAlias), maxDepth, predicates == null ? List.of() : predicates, graphType, path, 0);
        return path;
    }

    private void walk(
            String currentId,
            int maxDepth,
            List<RelationType> predicates,
            GraphType graphType,
            List<Map<String, Object>> path,
            int depth) {
        if (depth >= maxDepth) {
            return;
        }
        for (OntologyRelation rel : findRelationsFrom(currentId)) {
            if (graphType != null && rel.getGraphType() != graphType) {
                continue;
            }
            if (!predicates.isEmpty() && !predicates.contains(rel.getPredicate())) {
                continue;
            }
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("depth", depth + 1);
            step.put("from", currentId);
            step.put("predicate", rel.getPredicate().name());
            step.put("to", rel.getToId());
            step.put("graphType", rel.getGraphType().name());
            findConcept(rel.getToId()).ifPresent(c -> {
                step.put("toType", c.getType().name());
                step.put("toName", c.getName());
            });
            if (rel.getProvenance() != null) {
                step.put("provenance", rel.getProvenance().toMap());
            }
            path.add(step);
            walk(rel.getToId(), maxDepth, predicates, graphType, path, depth + 1);
        }
    }

    public List<List<Map<String, Object>>> reversePaths(
            String startIdOrAlias,
            int maxDepth,
            List<RelationType> predicates) {
        return reversePaths(startIdOrAlias, maxDepth, predicates, null);
    }

    /**
     * Reverse BFS from start following incoming edges. Returns one path per leaf ancestor branch.
     */
    public List<List<Map<String, Object>>> reversePaths(
            String startIdOrAlias,
            int maxDepth,
            List<RelationType> predicates,
            GraphType graphType) {
        String start = resolveId(startIdOrAlias);
        List<List<Map<String, Object>>> results = new ArrayList<>();
        Deque<Frame> queue = new ArrayDeque<>();
        queue.add(new Frame(start, List.of(), 0));
        Set<String> visitedEdgeAtDepth = new HashSet<>();

        while (!queue.isEmpty()) {
            Frame frame = queue.poll();
            if (frame.depth >= maxDepth) {
                if (!frame.path.isEmpty()) {
                    results.add(frame.path);
                }
                continue;
            }
            List<OntologyRelation> incoming = findRelationsTo(frame.nodeId).stream()
                    .filter(r -> predicates == null || predicates.isEmpty() || predicates.contains(r.getPredicate()))
                    .filter(r -> graphType == null || r.getGraphType() == graphType)
                    .collect(Collectors.toList());
            if (incoming.isEmpty()) {
                if (!frame.path.isEmpty()) {
                    results.add(frame.path);
                }
                continue;
            }
            for (OntologyRelation rel : incoming) {
                String edgeVisit = frame.nodeId + "<-" + rel.edgeKey();
                if (!visitedEdgeAtDepth.add(edgeVisit)) {
                    continue;
                }
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("to", frame.nodeId);
                step.put("predicate", rel.getPredicate().name());
                step.put("from", rel.getFromId());
                step.put("graphType", rel.getGraphType().name());
                findConcept(rel.getFromId()).ifPresent(c -> {
                    step.put("fromType", c.getType().name());
                    step.put("fromName", c.getName());
                });
                List<Map<String, Object>> nextPath = new ArrayList<>(frame.path);
                nextPath.add(step);
                queue.add(new Frame(rel.getFromId(), nextPath, frame.depth + 1));
            }
        }
        return results;
    }

    private static final class Frame {
        private final String nodeId;
        private final List<Map<String, Object>> path;
        private final int depth;

        private Frame(String nodeId, List<Map<String, Object>> path, int depth) {
            this.nodeId = nodeId;
            this.path = path;
            this.depth = depth;
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("conceptCount", concepts.size());
        out.put("relationCount", relations.size());
        out.put("concepts", concepts.values().stream().map(OntologyConcept::toMap).collect(Collectors.toList()));
        out.put("relations", relations.values().stream().map(OntologyRelation::toMap).collect(Collectors.toList()));
        return out;
    }

    public void clear() {
        concepts.clear();
        relations.clear();
        aliases.clear();
    }

    /**
     * Atomic swap helper: replace live contents from a fully loaded temporary store.
     */
    public synchronized void replaceFrom(OntologyStore other) {
        if (other == null || other == this) {
            return;
        }
        Map<String, OntologyConcept> nextConcepts = new LinkedHashMap<>();
        Map<String, OntologyRelation> nextRelations = new LinkedHashMap<>();
        for (OntologyConcept c : other.allConcepts()) {
            nextConcepts.put(c.getId(), c);
        }
        for (OntologyRelation r : other.allRelations()) {
            nextRelations.put(r.edgeKey(), r);
        }
        concepts.clear();
        relations.clear();
        aliases.clear();
        nextConcepts.values().forEach(this::putConcept);
        nextRelations.values().forEach(this::putRelation);
    }
}
