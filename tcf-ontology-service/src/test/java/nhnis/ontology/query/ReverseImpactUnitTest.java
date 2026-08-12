package nhnis.ontology.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nhnis.ontology.seed.Mgcoa8888OntologySeed;
import nhnis.ontology.store.OntologyStore;

class ReverseImpactUnitTest {

    private OntologyStore store;
    private OntologyQueryService query;

    @BeforeEach
    void setUp() {
        store = new OntologyStore();
        Mgcoa8888OntologySeed.seed(store);
        query = new OntologyQueryService(store);
    }

    @Test
    void impact_finds_serviceIds() {
        Map<String, Object> impact = query.impactByTable("TB_FW_IMAGE_LOG");
        @SuppressWarnings("unchecked")
        List<String> serviceIds = (List<String>) impact.get("affectedServiceIds");
        assertThat(serviceIds).as("impact=%s", impact).contains("mgcoa8888S0", "mgcoa8888D0");
        @SuppressWarnings("unchecked")
        List<String> handlers = (List<String>) impact.get("affectedHandlers");
        @SuppressWarnings("unchecked")
        List<String> programs = (List<String>) impact.get("affectedPrograms");
        @SuppressWarnings("unchecked")
        List<String> businesses = (List<String>) impact.get("affectedBusinesses");
        assertThat(handlers).contains("mgcoa8888Handler");
        assertThat(programs).contains("mgcoa8888");
        @SuppressWarnings("unchecked")
        List<String> functions = (List<String>) impact.get("affectedFunctions");
        assertThat(functions).isNotEmpty();
        assertThat(businesses).contains("CO");
        @SuppressWarnings("unchecked")
        List<String> systems = (List<String>) impact.get("affectedSystems");
        assertThat(systems).contains("MG");
        assertThat(String.valueOf(((Map<?, ?>) impact.get("table")).get("type"))).isEqualTo("TABLE");
        assertThat(impact.get("pathStatus")).isIn("COMPLETE", "PARTIAL");
    }

    @Test
    void impact_paths_never_invent_handledBy_to_table() {
        Map<String, Object> impact = query.impactByTable("TB_FW_IMAGE_LOG");
        @SuppressWarnings("unchecked")
        List<List<Map<String, Object>>> paths = (List<List<Map<String, Object>>>) impact.get("paths");
        assertThat(paths).isNotEmpty();
        for (List<Map<String, Object>> path : paths) {
            for (Map<String, Object> step : path) {
                if (!"HANDLED_BY".equals(String.valueOf(step.get("predicate")))) {
                    continue;
                }
                String to = String.valueOf(step.get("to"));
                assertThat(to)
                        .as("invented edge: %s", step)
                        .doesNotContain("table:");
                store.findConcept(to).ifPresent(c ->
                        assertThat(c.getType().name())
                                .as("HANDLED_BY target must not be TABLE: %s", step)
                                .isNotEqualTo("TABLE"));
            }
        }
    }

    @Test
    void table_services_finds_serviceIds() {
        Map<String, Object> body = query.tableServices("TB_FW_IMAGE_LOG");
        @SuppressWarnings("unchecked")
        List<String> serviceIds = (List<String>) body.get("serviceIds");
        assertThat(serviceIds).as("body=%s", body).contains("mgcoa8888S0");
    }
}
