package nhnis.ontology.ontology;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nhnis.ontology.config.OntologyProperties;

@Slf4j
@Service
public class OntologyRegistry {

    private final OntologyProperties properties;
    private final Map<String, Object> bundles = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> programById = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> serviceIndex = new ConcurrentHashMap<>();

    public OntologyRegistry(OntologyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void load() throws IOException {
        reload();
    }

    public synchronized void reload() throws IOException {
        bundles.clear();
        programById.clear();
        serviceIndex.clear();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String pattern = "classpath*:ontology/**/*.yml";
        Resource[] resources = resolver.getResources(pattern);
        Yaml yaml = new Yaml();

        for (Resource resource : resources) {
            if (!resource.isReadable()) {
                continue;
            }
            String filename = resource.getFilename();
            if (filename == null) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                Object loaded = yaml.load(in);
                if (!(loaded instanceof Map<?, ?> map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> doc = (Map<String, Object>) map;
                String key = relativeOntologyKey(resource);
                if (key.contains("/_generated/") || key.startsWith("_generated/")) {
                    continue;
                }
                bundles.put(key, doc);

                if (key.startsWith("mappings/") && doc.get("programId") != null) {
                    indexProgram(doc);
                }
            }
        }
        log.info("Ontology loaded: {} bundles, {} programs, {} services (basePath={})",
                bundles.size(), programById.size(), serviceIndex.size(), properties.getBasePath());
    }

    private void indexProgram(Map<String, Object> doc) {
        String programId = String.valueOf(doc.get("programId"));
        programById.put(programId, doc);
        Object servicesObj = doc.get("services");
        if (!(servicesObj instanceof List<?> services)) {
            return;
        }
        for (Object item : services) {
            if (item instanceof Map<?, ?> svc) {
                Object sid = svc.get("serviceId");
                if (sid != null) {
                    serviceIndex.put(String.valueOf(sid), doc);
                }
            }
        }
    }

    private static String relativeOntologyKey(Resource resource) throws IOException {
        String url = resource.getURL().toString().replace('\\', '/');
        int idx = url.lastIndexOf("/ontology/");
        if (idx >= 0) {
            return url.substring(idx + "/ontology/".length());
        }
        return resource.getFilename();
    }

    public Map<String, Object> catalog() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bundleCount", bundles.size());
        out.put("bundles", new ArrayList<>(bundles.keySet()).stream().sorted().toList());
        out.put("programs", new ArrayList<>(programById.keySet()).stream().sorted().toList());
        out.put("services", new ArrayList<>(serviceIndex.keySet()).stream().sorted().toList());
        out.put("version", firstPresent(
                bundles,
                "versions/v0.2.0.yml",
                "versions/v0.1.0.yml"));
        return out;
    }

    private static Object firstPresent(Map<String, Object> bundles, String... keys) {
        for (String key : keys) {
            Object doc = bundles.get(key);
            if (doc instanceof Map<?, ?> map && map.get("version") != null) {
                return map.get("version");
            }
        }
        return "unknown";
    }

    public Optional<Map<String, Object>> findProgram(String programId) {
        return Optional.ofNullable(programById.get(programId));
    }

    public Optional<Map<String, Object>> findByServiceId(String serviceId) {
        return Optional.ofNullable(serviceIndex.get(serviceId));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fourAxisByServiceId(String serviceId) {
        Map<String, Object> program = serviceIndex.get(serviceId);
        if (program == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> serviceDetail = null;
        Object servicesObj = program.get("services");
        if (servicesObj instanceof List<?> services) {
            for (Object item : services) {
                if (item instanceof Map<?, ?> svc
                        && serviceId.equals(String.valueOf(svc.get("serviceId")))) {
                    serviceDetail = (Map<String, Object>) svc;
                    break;
                }
            }
        }

        Map<String, Object> architecture = new LinkedHashMap<>();
        architecture.put("runtime", bundles.get("technical/tx-runtime.yml"));
        architecture.put("programNotes", program.get("architecture"));
        architecture.put("rules", bundles.get("rules/component-boundaries.yml"));

        Map<String, Object> development = new LinkedHashMap<>();
        development.put("programId", program.get("programId"));
        development.put("packageRoot", program.get("packageRoot"));
        Map<String, Object> components = new LinkedHashMap<>();
        if (program.get("development") instanceof Map<?, ?> dev) {
            components.put("handler", dev.get("handler"));
            components.put("facade", dev.get("facade"));
            components.put("controller", dev.get("controller"));
            components.put("service", dev.get("service"));
            components.put("dao", dev.get("dao"));
        }
        development.put("components", components);
        development.put("service", serviceDetail);
        development.put("core", bundles.get("core/concepts.yml"));
        Map<String, Object> classification = new LinkedHashMap<>();
        classification.put("majorGroup", program.get("majorGroup"));
        classification.put("businessCode", program.get("businessCode"));
        classification.put("functionCode", program.get("functionCode"));
        classification.put("functionName", program.get("functionName"));
        development.put("classification", classification);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mapping", program.get("data"));
        data.put("sqlIds", serviceDetail != null ? serviceDetail.get("sqlIds") : List.of());

        Map<String, Object> operations = new LinkedHashMap<>();
        operations.put("mapping", program.get("operations"));
        operations.put("shapes", bundles.get("shapes/service-id.yml"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("serviceId", serviceId);
        out.put("programId", program.get("programId"));
        out.put("title", program.get("title"));
        out.put("architecture", architecture);
        out.put("development", development);
        out.put("data", data);
        out.put("operations", operations);
        return out;
    }

    public Map<String, Object> getBundle(String relativePath) {
        Object doc = bundles.get(relativePath);
        if (doc instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return typed;
        }
        return Collections.emptyMap();
    }

    public List<Map<String, Object>> listPrograms() {
        return programById.values().stream()
                .sorted((a, b) -> String.valueOf(a.get("programId"))
                        .compareTo(String.valueOf(b.get("programId"))))
                .toList();
    }

    public Map<String, Object> runtimeBundle() {
        return getBundle("technical/tx-runtime.yml");
    }

    public Map<String, Object> rulesBundle() {
        return getBundle("rules/component-boundaries.yml");
    }

    public Map<String, Object> shapesBundle() {
        return getBundle("shapes/service-id.yml");
    }

    public Map<String, Object> coreBundle() {
        return getBundle("core/concepts.yml");
    }
}
