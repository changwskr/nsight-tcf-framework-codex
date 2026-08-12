package nhnis.ontology.loader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import nhnis.ontology.domain.concept.ConceptIds;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.seed.Mgcoa8888OntologySeed;
import nhnis.ontology.store.OntologyStore;

class CompositePkAndMultiTableLoaderTest {

    @Test
    void mgcoa5530_composite_pk_creates_separate_columns() {
        OntologyStore store = loadMapping("ontology/mappings/mgcoa5530.yml");
        String tableId = ConceptIds.table("RDW", "TB_MK_CO_A_5530");
        assertThat(store.findConcept(tableId)).isPresent();
        assertThat(store.findConcept(ConceptIds.column("RDW", "TB_MK_CO_A_5530", "L5101"))).isPresent();
        assertThat(store.findConcept(ConceptIds.column("RDW", "TB_MK_CO_A_5530", "L5103"))).isPresent();
        assertThat(store.findConceptsByType(ConceptType.COLUMN).stream()
                .map(c -> c.getName())
                .noneMatch(n -> n.startsWith("["))).isTrue();
        assertThat(store.findRelations(tableId, RelationType.HAS_COLUMN)).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void mgcoa9999_composite_pk_no_serialized_list_column() {
        OntologyStore store = loadMapping("ontology/mappings/mgcoa9999.yml");
        assertThat(store.findConceptsByType(ConceptType.COLUMN).stream()
                .map(c -> c.getName())
                .noneMatch(n -> n.startsWith("["))).isTrue();
        assertThat(store.findConcept(ConceptIds.column("RDW", "TB_CR_AH_SALES_TIP_RACT", "TRT_BRC"))).isPresent();
    }

    @Test
    void normalizeStringList_supports_scalar_and_list() {
        assertThat(YamlGraphLoader.normalizeStringList("GUID")).containsExactly("GUID");
        assertThat(YamlGraphLoader.normalizeStringList(List.of("L5101", "L5103")))
                .containsExactly("L5101", "L5103");
        assertThat(YamlGraphLoader.normalizeStringList("[L5101, L5103]")).isEmpty();
    }

    @Test
    void mgcoa8888_scalar_pk_still_works() {
        OntologyStore store = new OntologyStore();
        Mgcoa8888OntologySeed.seed(store);
        assertThat(store.findConcept(ConceptIds.column("RDW", "TB_FW_IMAGE_LOG", "GUID"))).isPresent();
    }

    @Test
    void synthetic_multi_table_maps_both_tables_without_sql_table_invention() {
        OntologyStore store = new OntologyStore();
        YamlGraphLoader loader = new YamlGraphLoader(store);
        Map<String, Object> doc = Map.of(
                "programId", "mgcoa7701",
                "majorGroup", "MG",
                "businessCode", "CO",
                "functionCode", "A",
                "packageRoot", "nhnis.mg.co.a",
                "development", Map.of(
                        "handler", "nhnis.mg.co.a.entry.handler.mgcoa7701Handler",
                        "facade", "nhnis.mg.co.a.application.facade.mgcoa7701Facade",
                        "controller", "nhnis.mg.co.a.application.controller.mgcoa7701Controller",
                        "service", "nhnis.mg.co.a.application.service.mgcoa7701Service",
                        "dao", "nhnis.mg.co.a.persistence.dao.mgcoa7701DAO"),
                "data", Map.of(
                        "mapperXml", "rdw.mg.co.a/mgcoa7701-ORA.xml",
                        "namespace", "nhnis.mg.co.a.persistence.dao.mgcoa7701DAO",
                        "tables", List.of("TB_A_7701", "TB_B_7701"),
                        "pk", List.of("ID")),
                "services", List.of(Map.of(
                        "serviceId", "mgcoa7701S0",
                        "op", "S",
                        "method", "mgcoa7701S0",
                        "sqlIds", List.of("mgcoa7701S0_S0"))));
        loader.loadProgramMapping(doc, "synthetic/mgcoa7701.yml");

        String mapperId = ConceptIds.mapper("rdw.mg.co.a/mgcoa7701-ORA.xml");
        String tableA = ConceptIds.table("RDW", "TB_A_7701");
        String tableB = ConceptIds.table("RDW", "TB_B_7701");
        assertThat(store.findConcept(tableA)).isPresent();
        assertThat(store.findConcept(tableB)).isPresent();
        assertThat(store.findRelations(mapperId, RelationType.ACCESSES)).hasSize(2);
        String sqlId = ConceptIds.sql("mgcoa7701S0_S0");
        assertThat(store.findRelations(sqlId, RelationType.ACCESSES)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static OntologyStore loadMapping(String classpath) {
        OntologyStore store = new OntologyStore();
        YamlGraphLoader loader = new YamlGraphLoader(store);
        try (InputStream in = CompositePkAndMultiTableLoaderTest.class.getClassLoader().getResourceAsStream(classpath)) {
            assertThat(in).as(classpath).isNotNull();
            Object loaded = new Yaml().load(in);
            loader.loadProgramMapping((Map<String, Object>) loaded, classpath);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return store;
    }
}
