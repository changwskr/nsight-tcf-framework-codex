package nhnis.ontology.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nhnis.ontology.domain.concept.ConceptIds;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.seed.Mgcoa8888OntologySeed;

class Mgcoa8888GoldenGraphTest {

    private OntologyStore store;

    @BeforeEach
    void setUp() {
        store = new OntologyStore();
        Mgcoa8888OntologySeed.seed(store);
    }

    @Test
    void golden_classification_and_call_chain() {
        assertThat(store.findConcept(ConceptIds.system("MG"))).isPresent();
        assertThat(store.findConcept("CO")).get().extracting(c -> c.getType()).isEqualTo(ConceptType.BUSINESS);
        assertThat(store.findConcept("mgcoa8888")).get().extracting(c -> c.getType()).isEqualTo(ConceptType.PROGRAM);
        assertThat(store.findConcept("mgcoa8888S0")).get().extracting(c -> c.getType()).isEqualTo(ConceptType.SERVICE_ID);

        assertThat(store.findRelations(ConceptIds.system("MG"), RelationType.HAS_BUSINESS)).isNotEmpty();
        assertThat(store.findRelations(ConceptIds.business("MG", "CO"), RelationType.HAS_FUNCTION)).isNotEmpty();
        assertThat(store.findRelations(ConceptIds.function("MG", "CO", "A"), RelationType.HAS_PROGRAM)).isNotEmpty();
        assertThat(store.findRelations(ConceptIds.program("MG", "CO", "A", "8888"), RelationType.PROVIDES_SERVICE))
                .anyMatch(r -> r.getToId().equals(ConceptIds.service("mgcoa8888S0")));

        var path = store.traverse(ConceptIds.service("mgcoa8888S0"), 10, null, null);
        assertThat(path.stream().map(s -> s.get("to").toString()))
                .anyMatch(id -> id.contains("mgcoa8888Handler"))
                .anyMatch(id -> id.contains("mgcoa8888Facade"))
                .anyMatch(id -> id.contains("mgcoa8888Service"))
                .anyMatch(id -> id.contains("mgcoa8888DAO"))
                .anyMatch(id -> id.contains("mgcoa8888-ORA.xml") || id.contains("mgcoa8888S0_S0"))
                .anyMatch(id -> id.contains("TB_FW_IMAGE_LOG"));
    }

    @Test
    void provenance_present_on_concepts() {
        assertThat(store.findConcept("mgcoa8888S0")).get()
                .extracting(c -> c.getProvenance().getSourcePath())
                .isEqualTo(Mgcoa8888OntologySeed.MAPPING_YAML);
    }
}
