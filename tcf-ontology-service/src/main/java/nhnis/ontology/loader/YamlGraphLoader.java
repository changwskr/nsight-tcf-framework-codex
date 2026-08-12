package nhnis.ontology.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import nhnis.ontology.domain.Provenance;
import nhnis.ontology.domain.concept.ConceptIds;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.concept.ServiceIdParts;
import nhnis.ontology.domain.relation.GraphType;
import nhnis.ontology.domain.relation.OntologyRelation;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.store.OntologyStore;
import nhnis.ontology.support.ServiceIdParser;

/**
 * Loads ProgramMapping YAML documents into Concept/Relation DESIGN graph.
 * YAML remains Source of Truth; this converts curated mappings → OntologyStore.
 */
@Component
public class YamlGraphLoader {

    private static final Logger log = LoggerFactory.getLogger(YamlGraphLoader.class);
    private static final String DEFAULT_SCHEMA = "RDW";

    private final OntologyStore store;

    public YamlGraphLoader(OntologyStore store) {
        this.store = store;
    }

    /**
     * Load one ProgramMapping document (ontology/mappings/*.yml content).
     *
     * @return number of relations added for this program (approx)
     */
    @SuppressWarnings("unchecked")
    public int loadProgramMapping(Map<String, Object> doc, String sourcePath) {
        if (doc == null || doc.get("programId") == null) {
            return 0;
        }
        String programShortId = String.valueOf(doc.get("programId"));
        String major = str(doc.get("majorGroup"), "MG");
        String business = str(doc.get("businessCode"), "CO");
        String function = str(doc.get("functionCode"), "A");
        String functionName = str(doc.get("functionName"), function);
        String identifier = str(doc.get("identifier"), extractIdentifier(programShortId));
        String title = str(doc.get("title"), programShortId);
        String packageRoot = str(doc.get("packageRoot"), "");

        Provenance yaml = Provenance.yamlMapping(sourcePath == null ? "ontology/mappings/" + programShortId + ".yml" : sourcePath);

        String systemId = ConceptIds.system(major);
        String businessId = ConceptIds.business(major, business);
        String functionId = ConceptIds.function(major, business, function);
        String programId = ConceptIds.program(major, business, function, identifier);

        putIfAbsent(systemId, ConceptType.SYSTEM, major.toUpperCase(Locale.ROOT), "System " + major, yaml,
                attrs("shortId", major.toUpperCase(Locale.ROOT), "code", major.toUpperCase(Locale.ROOT)));
        putIfAbsent(businessId, ConceptType.BUSINESS, business.toUpperCase(Locale.ROOT), "Business " + business, yaml,
                attrs("shortId", business.toUpperCase(Locale.ROOT), "code", business.toUpperCase(Locale.ROOT), "parent", major));
        putIfAbsent(functionId, ConceptType.FUNCTION, function.toUpperCase(Locale.ROOT), functionName, yaml,
                attrs("shortId", function.toUpperCase(Locale.ROOT), "code", function.toUpperCase(Locale.ROOT), "parent", business));
        put(programId, ConceptType.PROGRAM, programShortId, title, yaml, attrs(
                "shortId", programShortId,
                "programId", programShortId,
                "packageRoot", packageRoot,
                "identifier", identifier,
                "title", title));

        link(systemId, RelationType.HAS_BUSINESS, businessId, yaml);
        link(businessId, RelationType.HAS_FUNCTION, functionId, yaml);
        link(functionId, RelationType.HAS_PROGRAM, programId, yaml);

        Map<String, Object> development = map(doc.get("development"));
        Map<String, Object> data = map(doc.get("data"));

        String handlerFqcn = str(development.get("handler"), null);
        String facadeFqcn = str(development.get("facade"), null);
        String serviceFqcn = str(development.get("service"), null);
        String daoFqcn = str(development.get("dao"), null);
        String controllerFqcn = str(development.get("controller"), null);

        String handlerId = putComponent(handlerFqcn, "HANDLER", programShortId, yaml);
        String facadeId = putComponent(facadeFqcn, "FACADE", programShortId, yaml);
        String serviceId = putComponent(serviceFqcn, "SERVICE", programShortId, yaml);
        String daoId = putComponent(daoFqcn, "DAO", programShortId, yaml);
        putComponent(controllerFqcn, "CONTROLLER", programShortId, yaml);

        String mapperXml = str(data.get("mapperXml"), null);
        String namespace = str(data.get("namespace"), daoFqcn);
        List<String> tableNames = normalizeStringList(firstPresent(data, "tables", "table"));
        List<String> pkColumns = normalizeStringList(data.get("pk"));
        String deleteMode = str(data.get("deleteMode"), null);

        String mapperId = null;
        if (mapperXml != null) {
            mapperId = ConceptIds.mapper(mapperXml);
            String mapperName = mapperXml.contains("/") ? mapperXml.substring(mapperXml.lastIndexOf('/') + 1) : mapperXml;
            put(mapperId, ConceptType.MAPPER, mapperName, "Mapper " + mapperName, yaml, attrs(
                    "mapperXml", mapperXml,
                    "namespace", namespace,
                    "programId", programShortId));
        }

        List<String> tableIds = new ArrayList<>();
        for (String tableName : tableNames) {
            if (tableName == null || tableName.isBlank() || looksLikeSerializedList(tableName)) {
                log.warn("Skip invalid table name in {}: {}", programShortId, tableName);
                continue;
            }
            String tableId = ConceptIds.table(DEFAULT_SCHEMA, tableName);
            put(tableId, ConceptType.TABLE, tableName, "Table " + tableName, yaml, attrs(
                    "tableName", tableName,
                    "schema", DEFAULT_SCHEMA,
                    "pkColumns", pkColumns,
                    "deleteMode", deleteMode));
            for (String pkCol : pkColumns) {
                if (pkCol == null || pkCol.isBlank() || looksLikeSerializedList(pkCol)) {
                    continue;
                }
                String columnId = ConceptIds.column(DEFAULT_SCHEMA, tableName, pkCol);
                put(columnId, ConceptType.COLUMN, pkCol, "PK " + pkCol, yaml, attrs(
                        "tableName", tableName,
                        "columnName", pkCol,
                        "pk", true));
                link(tableId, RelationType.HAS_COLUMN, columnId, yaml);
            }
            if (mapperId != null) {
                link(mapperId, RelationType.ACCESSES, tableId, yaml);
            }
            tableIds.add(tableId);
        }

        if (handlerId != null && facadeId != null) {
            link(handlerId, RelationType.CALLS, facadeId, yaml);
        }
        if (facadeId != null && serviceId != null) {
            link(facadeId, RelationType.CALLS, serviceId, yaml);
        }
        if (serviceId != null && daoId != null) {
            link(serviceId, RelationType.USES, daoId, yaml);
        }
        if (daoId != null && mapperId != null) {
            link(daoId, RelationType.EXECUTES, mapperId, yaml);
        }

        Object servicesObj = doc.get("services");
        int serviceCount = 0;
        if (servicesObj instanceof List<?> services) {
            for (Object item : services) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> svc = (Map<String, Object>) raw;
                String sid = str(svc.get("serviceId"), null);
                if (sid == null || !ServiceIdParser.isValid(sid)) {
                    log.warn("Skip invalid serviceId in {}: {}", programShortId, sid);
                    continue;
                }
                loadService(sid, programId, handlerId, daoId, mapperId, tableIds, svc, yaml);
                serviceCount++;
            }
        }

        log.debug("Loaded mapping graph: program={}, services={}, tables={}", programShortId, serviceCount, tableIds.size());
        return serviceCount;
    }

    @SuppressWarnings("unchecked")
    private void loadService(
            String serviceId,
            String programConceptId,
            String handlerId,
            String daoId,
            String mapperId,
            List<String> tableIds,
            Map<String, Object> svc,
            Provenance yaml) {
        ServiceIdParts parts = ServiceIdParser.parse(serviceId);
        String conceptId = ConceptIds.service(parts.getFullServiceId());
        Map<String, Object> attrs = new LinkedHashMap<>(parts.toAttributeMap());
        attrs.put("shortId", parts.getFullServiceId());
        if (svc.get("method") != null) {
            attrs.put("method", svc.get("method"));
        }
        if (svc.get("op") != null) {
            attrs.put("op", svc.get("op"));
        }
        put(conceptId, ConceptType.SERVICE_ID, parts.getFullServiceId(),
                "ServiceId " + parts.getFullServiceId(), yaml, attrs);

        link(programConceptId, RelationType.PROVIDES_SERVICE, conceptId, yaml);
        link(conceptId, RelationType.BELONGS_TO_PROGRAM, programConceptId, yaml);
        if (handlerId != null) {
            link(conceptId, RelationType.HANDLED_BY, handlerId, yaml);
        }

        Object sqlIdsObj = svc.get("sqlIds");
        if (sqlIdsObj instanceof List<?> sqlIds) {
            for (Object sql : sqlIds) {
                if (sql == null) {
                    continue;
                }
                String sqlId = String.valueOf(sql);
                String sqlConceptId = ConceptIds.sql(sqlId);
                put(sqlConceptId, ConceptType.SQL_ID, sqlId, "SQL " + sqlId, yaml, attrs(
                        "sqlId", sqlId,
                        "serviceId", parts.getFullServiceId()));
                if (daoId != null) {
                    link(daoId, RelationType.EXECUTES, sqlConceptId, yaml);
                }
                // Only link SqlId→Table when exactly one table is known (avoid inventing multi links)
                if (tableIds != null && tableIds.size() == 1) {
                    link(sqlConceptId, RelationType.ACCESSES, tableIds.get(0), yaml);
                }
            }
        }
    }

    private String putComponent(String fqcn, String role, String programShortId, Provenance yaml) {
        if (fqcn == null || fqcn.isBlank()) {
            return null;
        }
        String id = ConceptIds.component(fqcn);
        String simple = fqcn.contains(".") ? fqcn.substring(fqcn.lastIndexOf('.') + 1) : fqcn;
        Provenance src = Provenance.sourceCode(guessJavaPath(fqcn));
        put(id, ConceptType.COMPONENT, simple, role + " " + simple, src, attrs(
                "role", role,
                "className", fqcn,
                "programId", programShortId));
        return id;
    }

    private static String guessJavaPath(String fqcn) {
        return "pdmg-service/src/main/java/" + fqcn.replace('.', '/') + ".java";
    }

    private void putIfAbsent(
            String id,
            ConceptType type,
            String name,
            String description,
            Provenance provenance,
            Map<String, Object> attrs) {
        if (store.findConcept(id).isPresent()) {
            return;
        }
        put(id, type, name, description, provenance, attrs);
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
        if (from == null || to == null) {
            return;
        }
        store.putRelation(OntologyRelation.builder()
                .fromId(from)
                .predicate(predicate)
                .toId(to)
                .graphType(GraphType.DESIGN)
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

    /** Scalar or list YAML values → trimmed non-blank strings (never String.valueOf(List)). */
    static List<String> normalizeStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value == null) {
            return out;
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item == null) {
                    continue;
                }
                String s = String.valueOf(item).trim();
                if (!s.isEmpty() && !"null".equals(s) && !looksLikeSerializedList(s)) {
                    out.add(s);
                }
            }
            return out;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equals(s) || looksLikeSerializedList(s)) {
            return out;
        }
        out.add(s);
        return out;
    }

    private static Object firstPresent(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object v = data.get(key);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static boolean looksLikeSerializedList(String s) {
        return s.startsWith("[") && s.endsWith("]");
    }

    private static String extractIdentifier(String programId) {
        if (programId != null && programId.length() >= 9) {
            return programId.substring(5, 9);
        }
        return "0000";
    }
}
