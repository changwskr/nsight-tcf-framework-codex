package nhnis.ontology.loader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.store.OntologyStore;

@SpringBootTest
class YamlGraphLoaderIT {

    @Autowired
    private OntologyStore store;

    @Test
    void bootstraps_all_curated_mappings_into_graph() {
        assertThat(store.findConceptsByType(ConceptType.PROGRAM))
                .extracting(c -> c.getName())
                .contains("mgcoa8888", "mgcoa9000", "mgcoa9001", "mgcoa5530", "mgcoa9999");

        assertThat(store.findConcept("mgcoa9001S0")).isPresent();
        assertThat(store.findConcept("TB_MG_TX_CONTROL")).isPresent();
        assertThat(store.findConcept("mgcoa9001Handler")).isPresent();
    }
}
