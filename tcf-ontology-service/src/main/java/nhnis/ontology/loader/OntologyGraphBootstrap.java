package nhnis.ontology.loader;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import nhnis.ontology.ontology.OntologyRegistry;
import nhnis.ontology.store.OntologyStore;

/**
 * Bootstraps OntologyStore from curated YAML mappings (W7).
 * Skips _generated (already filtered by OntologyRegistry).
 */
@Component
@Order(1)
public class OntologyGraphBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OntologyGraphBootstrap.class);

    private final OntologyRegistry registry;
    private final YamlGraphLoader loader;
    private final TxRuntimeGraphLoader txRuntimeLoader;
    private final OntologyStore store;

    public OntologyGraphBootstrap(
            OntologyRegistry registry,
            YamlGraphLoader loader,
            TxRuntimeGraphLoader txRuntimeLoader,
            OntologyStore store) {
        this.registry = registry;
        this.loader = loader;
        this.txRuntimeLoader = txRuntimeLoader;
        this.store = store;
    }

    @Override
    public void run(ApplicationArguments args) {
        loadAll();
    }

    public int loadAll() {
        return loadInto(store, loader, txRuntimeLoader);
    }

    /**
     * Reload into a temporary store then atomically swap — avoids empty/partial live graph on failure.
     */
    public int reloadAtomic() {
        OntologyStore temp = new OntologyStore();
        YamlGraphLoader tempLoader = new YamlGraphLoader(temp);
        TxRuntimeGraphLoader tempTx = new TxRuntimeGraphLoader(temp);
        int programs = loadInto(temp, tempLoader, tempTx);
        store.replaceFrom(temp);
        log.info("Atomic reload complete: programs={}, concepts={}, relations={}",
                programs, store.allConcepts().size(), store.allRelations().size());
        return programs;
    }

    private int loadInto(OntologyStore target, YamlGraphLoader graphLoader, TxRuntimeGraphLoader runtimeLoader) {
        int programs = 0;
        int services = 0;
        for (Map<String, Object> doc : registry.listPrograms()) {
            String programId = String.valueOf(doc.get("programId"));
            String sourcePath = "ontology/mappings/" + programId + ".yml";
            services += graphLoader.loadProgramMapping(doc, sourcePath);
            programs++;
        }
        int runtimeSteps = runtimeLoader.load(registry.runtimeBundle());
        log.info("YAML→Graph loaded: programs={}, services={}, runtimeSteps={}, concepts={}, relations={}",
                programs, services, runtimeSteps, target.allConcepts().size(), target.allRelations().size());
        return programs;
    }
}
