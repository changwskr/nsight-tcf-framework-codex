package nhnis.ontology.validate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import nhnis.ontology.domain.Provenance;
import nhnis.ontology.domain.concept.ConceptIds;
import nhnis.ontology.domain.concept.ConceptType;
import nhnis.ontology.domain.concept.OntologyConcept;
import nhnis.ontology.domain.relation.GraphType;
import nhnis.ontology.domain.relation.OntologyRelation;
import nhnis.ontology.domain.relation.RelationType;
import nhnis.ontology.store.OntologyStore;

/**
 * Verification-only: intentional broken graphs must FAIL rules (check report §6).
 */
class ArchitectureRuleNegativeCasesTest {

    private final Provenance p = Provenance.yamlMapping("test://negative");

    @Test
    void case1_serviceId_without_handler_fails_rule002() {
        OntologyStore store = new OntologyStore();
        put(store, ConceptIds.service("mgcoa8888S0"), ConceptType.SERVICE_ID, "mgcoa8888S0");
        ArchitectureRuleValidator v = new ArchitectureRuleValidator(store);
        assertThat(v.rule002()).anyMatch(f -> "FAIL".equals(f.get("verdict")) && "RULE-002".equals(f.get("ruleId")));
    }

    @Test
    void case2_handler_without_service_relation_fails_rule003() {
        OntologyStore store = new OntologyStore();
        String handler = ConceptIds.component("nhnis.mg.co.a.entry.handler.orphanHandler");
        put(store, handler, ConceptType.COMPONENT, "orphanHandler", Map.of("role", "HANDLER", "programId", "mgcoa9999"));
        ArchitectureRuleValidator v = new ArchitectureRuleValidator(store);
        assertThat(v.rule003()).anyMatch(f -> "FAIL".equals(f.get("verdict")) && "RULE-003".equals(f.get("ruleId")));
    }

    @Test
    void case3_program_without_service_fails_rule004() {
        OntologyStore store = new OntologyStore();
        put(store, ConceptIds.program("MG", "CO", "A", "7777"), ConceptType.PROGRAM, "mgcoa7777");
        ArchitectureRuleValidator v = new ArchitectureRuleValidator(store);
        assertThat(v.rule004()).anyMatch(f -> "FAIL".equals(f.get("verdict")) && "RULE-004".equals(f.get("ruleId")));
    }

    @Test
    void case4_service_without_dao_fails_rule005() {
        OntologyStore store = new OntologyStore();
        String service = ConceptIds.component("nhnis.x.ServiceAlone");
        put(store, service, ConceptType.COMPONENT, "ServiceAlone", Map.of("role", "SERVICE"));
        ArchitectureRuleValidator v = new ArchitectureRuleValidator(store);
        assertThat(v.rule005()).anyMatch(f -> "FAIL".equals(f.get("verdict")) && "RULE-005".equals(f.get("ruleId")));
    }

    @Test
    void case5_dao_without_mapper_fails_rule006() {
        OntologyStore store = new OntologyStore();
        String dao = ConceptIds.component("nhnis.x.DaoAlone");
        put(store, dao, ConceptType.COMPONENT, "DaoAlone", Map.of("role", "DAO"));
        ArchitectureRuleValidator v = new ArchitectureRuleValidator(store);
        assertThat(v.rule006()).anyMatch(f -> "FAIL".equals(f.get("verdict")) && "RULE-006".equals(f.get("ruleId")));
    }

    @Test
    void validateAll_broken_graph_status_fail_and_failCount_positive() {
        OntologyStore store = new OntologyStore();
        // Intentionally broken: ServiceId without HANDLED_BY Handler
        put(store, ConceptIds.service("mgcoa8888S0"), ConceptType.SERVICE_ID, "mgcoa8888S0");
        put(store, ConceptIds.program("MG", "CO", "A", "8888"), ConceptType.PROGRAM, "mgcoa8888");
        // Program without PROVIDES_SERVICE also breaks RULE-004
        Map<String, Object> all = new ArchitectureRuleValidator(store).validateAll();

        assertThat(all.get("status")).as("validateAll=%s", all).isEqualTo("FAIL");
        assertThat(((Number) all.get("failCount")).longValue()).isGreaterThan(0L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) all.get("findings");
        long verdictFails = findings.stream().filter(f -> "FAIL".equals(f.get("verdict"))).count();
        assertThat(((Number) all.get("failCount")).longValue()).isEqualTo(verdictFails);
        assertThat(findings).anyMatch(f -> "FAIL".equals(f.get("verdict")) && "RULE-002".equals(f.get("ruleId")));
    }

    @Test
    void findings_include_ruleId_and_evidence() {
        OntologyStore store = new OntologyStore();
        put(store, ConceptIds.service("mgcoa8888S0"), ConceptType.SERVICE_ID, "mgcoa8888S0");
        Map<String, Object> all = new ArchitectureRuleValidator(store).validateAll();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) all.get("findings");
        assertThat(findings).isNotEmpty();
        assertThat(findings.get(0)).containsKeys("ruleId", "verdict", "target", "message", "evidence");
    }

    private void put(OntologyStore store, String id, ConceptType type, String name) {
        put(store, id, type, name, Map.of());
    }

    private void put(OntologyStore store, String id, ConceptType type, String name, Map<String, Object> attrs) {
        store.putConcept(OntologyConcept.builder()
                .id(id).type(type).name(name).attributes(attrs).provenance(p).build());
    }
}
