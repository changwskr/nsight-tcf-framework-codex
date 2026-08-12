package nhnis.ontology.validate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nhnis.ontology.seed.Mgcoa8888OntologySeed;
import nhnis.ontology.store.OntologyStore;

class ArchitectureRuleScopedValidationTest {

    private ArchitectureRuleValidator validator;

    @BeforeEach
    void setUp() {
        OntologyStore store = new OntologyStore();
        Mgcoa8888OntologySeed.seed(store);
        validator = new ArchitectureRuleValidator(store);
    }

    @Test
    void validate_service_scopes_to_mgcoa8888S0() {
        Map<String, Object> result = validator.validateService("mgcoa8888S0");
        assertThat(result.get("scope")).isEqualTo("SERVICE");
        assertThat(result.get("status")).isEqualTo("PASS");
        assertThat(((Number) result.get("failCount")).longValue()).isZero();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) result.get("findings");
        assertThat(findings).isNotEmpty();
        assertThat(findings).allMatch(f -> f.get("verdict") != null);
    }

    @Test
    void validate_design_baseline_unresolved_is_not_fail() {
        Map<String, Object> result = validator.validateDesignBaseline(Map.of("serviceId", "UNRESOLVED"));
        assertThat(result.get("scope")).isEqualTo("DESIGN_BASELINE");
        assertThat(result.get("status")).isEqualTo("PASS_WITH_UNRESOLVED");
        assertThat(((Number) result.get("failCount")).longValue()).isZero();
        assertThat(((Number) result.get("unresolvedCount")).longValue()).isGreaterThan(0L);
        assertThat(((Number) result.get("notYetImplementedCount")).longValue()).isGreaterThan(0L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) result.get("findings");
        assertThat(findings).anyMatch(f -> "UNRESOLVED".equals(f.get("verdict")));
        assertThat(findings).anyMatch(f -> "NOT_YET_IMPLEMENTED".equals(f.get("verdict")));
        assertThat(findings).noneMatch(f -> "FAIL".equals(f.get("verdict")));
    }

    @Test
    void validate_service_unregistered_does_not_recurse() {
        Map<String, Object> result = validator.validateService("mgcoa7777S0");
        assertThat(result.get("scope")).isEqualTo("SERVICE");
        assertThat(result.get("status")).isEqualTo("NOT_FOUND");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) result.get("findings");
        assertThat(findings).anyMatch(f -> "NOT_FOUND".equals(f.get("verdict")));
    }

    @Test
    void validate_design_allocated_but_missing_is_not_yet_implemented() {
        Map<String, Object> result = validator.validateDesignBaseline(Map.of("serviceId", "mgcoa7777S0"));
        assertThat(result.get("scope")).isEqualTo("DESIGN_BASELINE");
        assertThat(result.get("status")).isEqualTo("PASS_WITH_UNRESOLVED");
        assertThat(((Number) result.get("failCount")).longValue()).isZero();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) result.get("findings");
        assertThat(findings).anyMatch(f -> "NOT_YET_IMPLEMENTED".equals(f.get("verdict")));
    }
}
