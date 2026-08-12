package nhnis.ontology.validate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nhnis.ontology.seed.Mgcoa8888OntologySeed;
import nhnis.ontology.store.OntologyStore;

class ArchitectureRuleValidationTest {

    private ArchitectureRuleValidator validator;

    @BeforeEach
    void setUp() {
        OntologyStore store = new OntologyStore();
        Mgcoa8888OntologySeed.seed(store);
        validator = new ArchitectureRuleValidator(store);
    }

    @Test
    void golden_graph_passes_rules() {
        var result = validator.validateAll();
        assertThat(result.get("status")).isEqualTo("PASS");
        assertThat(result.get("failCount")).isEqualTo(0L);
    }
}
