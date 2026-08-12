package nhnis.ontology.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import nhnis.ontology.domain.Provenance;
import nhnis.ontology.domain.concept.ConceptIds;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.relation.GraphType;
import nhnis.ontology.domain.relation.OntologyRelation;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.store.OntologyStore;

/**
 * Loads ontology/technical/tx-runtime.yml into RUNTIME Concept/Relation graph (W8).
 * DESIGN call chains and RUNTIME pipeline are kept separate via GraphType.
 */
@Component
public class TxRuntimeGraphLoader {

    private static final Logger log = LoggerFactory.getLogger(TxRuntimeGraphLoader.class);
    public static final String SOURCE = "ontology/technical/tx-runtime.yml";
    public static final String UOW_NAME = "rdw-TransactionTemplate";

    private final OntologyStore store;

    public TxRuntimeGraphLoader(OntologyStore store) {
        this.store = store;
    }

    @SuppressWarnings("unchecked")
    public int load(Map<String, Object> doc) {
        if (doc == null || doc.isEmpty()) {
            log.warn("tx-runtime.yml empty — RUNTIME graph skipped");
            return 0;
        }
        Provenance prov = Provenance.builder()
                .sourceType(Provenance.SourceType.YAML_MAPPING)
                .sourceSystem("pdmg-fw")
                .sourcePath(SOURCE)
                .sourceDocument(str(doc.get("sourceDoc"), SOURCE))
                .discoveredBy("TxRuntimeGraphLoader")
                .extractedAt(java.time.Instant.now())
                .verificationStatus(Provenance.VerificationStatus.VERIFIED)
                .build();

        String uowId = ConceptIds.unitOfWork(UOW_NAME);
        Map<String, Object> txBoundary = map(doc.get("txBoundary"));
        put(uowId, ConceptType.RUNTIME_COMPONENT, UOW_NAME,
                "Business DB UnitOfWork / TransactionTemplate",
                prov, attrs(
                        "role", "UNIT_OF_WORK",
                        "owner", txBoundary.get("owner"),
                        "includes", txBoundary.get("includes"),
                        "excludes", txBoundary.get("excludes")));

        Map<String, Object> threads = map(doc.get("threads"));
        String requestThread = ConceptIds.runtimeThread("request");
        String workerThread = ConceptIds.runtimeThread("worker");
        putThread(requestThread, "requestThread", map(threads.get("requestThread")), prov);
        putThread(workerThread, "workerThread", map(threads.get("workerThread")), prov);

        List<Map<String, Object>> steps = new ArrayList<>();
        Object stepsObj = doc.get("steps");
        if (stepsObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    steps.add((Map<String, Object>) m);
                }
            }
        }

        List<String> stepIds = new ArrayList<>();
        for (Map<String, Object> step : steps) {
            String id = str(step.get("id"), null);
            if (id == null) {
                continue;
            }
            String conceptId = ConceptIds.runtime(id);
            stepIds.add(conceptId);
            Integer seq = step.get("seq") instanceof Number n ? n.intValue() : null;
            String thread = str(step.get("thread"), "");
            put(conceptId, ConceptType.RUNTIME_COMPONENT, id, "Runtime step " + id, prov, attrs(
                    "role", "RUNTIME_STEP",
                    "seq", seq,
                    "thread", thread,
                    "tx", step.get("tx"),
                    "configKey", step.get("configKey"),
                    "notes", step.get("notes"),
                    "defaults", step.get("defaults")));

            if (thread.contains("worker")) {
                link(conceptId, RelationType.RUNS_ON_THREAD, workerThread, prov);
            } else if (thread.contains("request")) {
                link(conceptId, RelationType.RUNS_ON_THREAD, requestThread, prov);
            }

            // TX participation
            Object tx = step.get("tx");
            if (tx != null) {
                String txVal = String.valueOf(tx);
                if (txVal.contains("inside") || "commit-or-rollback".equals(txVal)) {
                    link(conceptId, RelationType.PARTICIPATES_IN_TRANSACTION, uowId, prov);
                }
            }
        }

        // Sequential FLOW
        for (int i = 0; i + 1 < stepIds.size(); i++) {
            link(stepIds.get(i), RelationType.FLOWS_TO, stepIds.get(i + 1), prov);
        }

        // Named RUNTIME semantics from architecture docs
        String timeoutExec = ConceptIds.runtime("OnlineTimeoutExecutor");
        String dispatcher = ConceptIds.runtime("TransactionDispatcher");
        String handler = ConceptIds.runtime("TransactionHandler");
        String facade = ConceptIds.runtime("Facade");

        if (store.findConcept(timeoutExec).isPresent()) {
            link(timeoutExec, RelationType.STARTS_TRANSACTION, uowId, prov);
        }
        if (store.findConcept(dispatcher).isPresent() && store.findConcept(handler).isPresent()) {
            link(dispatcher, RelationType.DISPATCHES_TO, handler, prov);
        }
        // Business Handler is DESIGN; RUNTIME dispatcher hands off to TransactionHandler then Facade
        if (store.findConcept(handler).isPresent() && store.findConcept(facade).isPresent()) {
            link(handler, RelationType.DISPATCHES_TO, facade, prov);
        }

        Map<String, Object> outcomes = map(doc.get("outcomes"));
        if (!outcomes.isEmpty()) {
            String outcomeId = ConceptIds.runtime("outcomes");
            put(outcomeId, ConceptType.RUNTIME_COMPONENT, "outcomes", "TX runtime outcomes", prov,
                    new LinkedHashMap<>(outcomes));
        }

        log.info("RUNTIME graph loaded from {}: steps={}, uow={}", SOURCE, stepIds.size(), UOW_NAME);
        return stepIds.size();
    }

    private void putThread(String id, String name, Map<String, Object> attrs, Provenance prov) {
        Map<String, Object> a = new LinkedHashMap<>(attrs);
        a.put("role", "THREAD");
        put(id, ConceptType.RUNTIME_COMPONENT, name, "Thread " + name, prov, a);
    }

    private void put(
            String id,
            ConceptType type,
            String name,
            String description,
            Provenance provenance,
            Map<String, Object> attrs) {
        store.putConcept(OntologyConcept.builder()
                .id(id)
                .type(type)
                .name(name)
                .description(description)
                .attributes(attrs)
                .version("1.0")
                .status(OntologyConcept.Status.ACTIVE)
                .provenance(provenance)
                .build());
    }

    private void link(String from, RelationType predicate, String to, Provenance provenance) {
        store.putRelation(OntologyRelation.builder()
                .fromId(from)
                .predicate(predicate)
                .toId(to)
                .graphType(GraphType.RUNTIME)
                .version("1.0")
                .status(OntologyRelation.Status.ACTIVE)
                .provenance(provenance)
                .build());
    }

    private static Map<String, Object> attrs(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private static String str(Object o, String defaultValue) {
        if (o == null) {
            return defaultValue;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() || "null".equals(s) ? defaultValue : s;
    }
}
