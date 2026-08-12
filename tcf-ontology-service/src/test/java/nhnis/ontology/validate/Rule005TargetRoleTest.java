package nhnis.ontology.validate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import nhnis.ontology.domain.Provenance;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.relation.GraphType;
import nhnis.ontology.domain.relation.OntologyRelation;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.store.OntologyStore;

class Rule005TargetRoleTest {

    @Test
    void service_calls_service_only_fails_rule005() {
        OntologyStore store = new OntologyStore();
        Provenance p = Provenance.yamlMapping("test");
        OntologyConcept service = OntologyConcept.builder()
                .id("component:SvcA")
                .type(ConceptType.COMPONENT)
                .name("SvcA")
                .attributes(Map.of("role", "SERVICE"))
                .provenance(p)
                .build();
        OntologyConcept other = OntologyConcept.builder()
                .id("component:SvcB")
                .type(ConceptType.COMPONENT)
                .name("SvcB")
                .attributes(Map.of("role", "SERVICE"))
                .provenance(p)
                .build();
        store.putConcept(service);
        store.putConcept(other);
        store.putRelation(OntologyRelation.builder()
                .fromId(service.getId())
                .predicate(RelationType.CALLS)
                .toId(other.getId())
                .graphType(GraphType.DESIGN)
                .provenance(p)
                .build());

        ArchitectureRuleValidator validator = new ArchitectureRuleValidator(store);
        List<Map<String, Object>> findings = validator.rule005();
        assertThat(findings).isNotEmpty();
        assertThat(findings.get(0).get("verdict")).isEqualTo("FAIL");
    }

    @Test
    void service_calls_dao_passes_rule005() {
        OntologyStore store = new OntologyStore();
        Provenance p = Provenance.yamlMapping("test");
        OntologyConcept service = OntologyConcept.builder()
                .id("component:SvcA")
                .type(ConceptType.COMPONENT)
                .name("SvcA")
                .attributes(Map.of("role", "SERVICE"))
                .provenance(p)
                .build();
        OntologyConcept dao = OntologyConcept.builder()
                .id("component:DaoA")
                .type(ConceptType.COMPONENT)
                .name("DaoA")
                .attributes(Map.of("role", "DAO"))
                .provenance(p)
                .build();
        store.putConcept(service);
        store.putConcept(dao);
        store.putRelation(OntologyRelation.builder()
                .fromId(service.getId())
                .predicate(RelationType.USES)
                .toId(dao.getId())
                .graphType(GraphType.DESIGN)
                .provenance(p)
                .build());

        ArchitectureRuleValidator validator = new ArchitectureRuleValidator(store);
        List<Map<String, Object>> findings = validator.rule005();
        assertThat(findings.get(0).get("verdict")).isEqualTo("PASS");
    }
}
