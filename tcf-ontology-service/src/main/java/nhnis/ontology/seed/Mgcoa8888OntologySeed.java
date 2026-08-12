package nhnis.ontology.seed;

import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import nhnis.ontology.loader.YamlGraphLoader;
import nhnis.ontology.store.OntologyStore;

/**
 * Golden sample helper for mgcoa8888.
 * Production boot uses {@link nhnis.ontology.loader.OntologyGraphBootstrap}.
 * Unit tests may call {@link #seed(OntologyStore)} to load only this mapping.
 */
public final class Mgcoa8888OntologySeed {

    public static final String MAPPING_YAML = "ontology/mappings/mgcoa8888.yml";
    public static final String SERVICE_ID_S0 = "mgcoa8888S0";

    private Mgcoa8888OntologySeed() {
    }

    /**
     * Load curated mgcoa8888.yml into the given store (test helper).
     */
    @SuppressWarnings("unchecked")
    public static void seed(OntologyStore store) {
        YamlGraphLoader loader = new YamlGraphLoader(store);
        try (InputStream in = Mgcoa8888OntologySeed.class.getClassLoader().getResourceAsStream(MAPPING_YAML)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + MAPPING_YAML);
            }
            Object loaded = new Yaml().load(in);
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new IllegalStateException("Invalid mapping YAML: " + MAPPING_YAML);
            }
            loader.loadProgramMapping((Map<String, Object>) map, MAPPING_YAML);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to seed mgcoa8888 from YAML", e);
        }
    }
}
