package nhnis.ontology.loader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import nhnis.ontology.domain.concept.ConceptIds;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.relation.GraphType;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.ontology.OntologyRegistry;
import nhnis.ontology.store.OntologyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TxRuntimeGraphLoaderIT {

    @Autowired
    private OntologyStore store;

    @Autowired
    private OntologyRegistry registry;

    @Test
    void design_and_runtime_graphs_are_separated() {
        assertThat(store.findConcept("mgcoa8888S0")).isPresent();
        assertThat(store.findConcept(ConceptIds.runtime("DefaultFilter"))).isPresent();
        assertThat(store.findConcept(ConceptIds.runtime("DefaultFilter")).get().getType())
                .isEqualTo(ConceptType.RUNTIME_COMPONENT);

        boolean designCall = store.findByPredicate(RelationType.CALLS).stream()
                .anyMatch(r -> r.getGraphType() == GraphType.DESIGN);
        boolean runtimeFlow = store.findByPredicate(RelationType.FLOWS_TO).stream()
                .anyMatch(r -> r.getGraphType() == GraphType.RUNTIME);
        assertThat(designCall).isTrue();
        assertThat(runtimeFlow).isTrue();

        assertThat(registry.runtimeBundle()).isNotEmpty();
    }
}
