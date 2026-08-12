package nhnis.ontology.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DesignRecommendationServiceTest {

    @Autowired
    private DesignRecommendationService design;

    @Test
    void delete_request_selects_D_service_for_mgcoa8888() {
        Map<String, Object> out = design.recommend(Map.of(
                "system", "MG",
                "business", "CO",
                "function", "A",
                "transactionType", "DELETE"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) out.get("candidates");
        assertThat(candidates).isNotEmpty();
        Map<String, Object> mgcoa8888 = candidates.stream()
                .filter(c -> "mgcoa8888".equals(String.valueOf(c.get("programId"))))
                .findFirst()
                .orElseThrow();
        assertThat(mgcoa8888.get("operationMatch")).isEqualTo(true);
        assertThat(String.valueOf(mgcoa8888.get("serviceId"))).isEqualTo("mgcoa8888D0");
    }

    @Test
    void update_does_not_map_to_query_intent_silently_using_S() {
        Map<String, Object> out = design.recommend(Map.of(
                "business", "CO",
                "function", "A",
                "transactionType", "UPDATE"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) out.get("candidates");
        // mgcoa9000 has U0 — if present prefer operationMatch
        candidates.stream()
                .filter(c -> "mgcoa9000".equals(String.valueOf(c.get("programId"))))
                .findFirst()
                .ifPresent(c -> {
                    assertThat(c.get("operationMatch")).isEqualTo(true);
                    assertThat(String.valueOf(c.get("serviceId"))).contains("U0");
                });
    }

    @Test
    void create_request_selects_C_service_when_available() {
        Map<String, Object> out = design.recommend(Map.of(
                "system", "MG",
                "business", "CO",
                "function", "A",
                "transactionType", "CREATE"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) out.get("candidates");
        assertThat(candidates).isNotEmpty();
        candidates.stream()
                .filter(c -> "mgcoa9000".equals(String.valueOf(c.get("programId"))))
                .findFirst()
                .ifPresent(c -> {
                    assertThat(c.get("operationMatch")).isEqualTo(true);
                    assertThat(String.valueOf(c.get("serviceId"))).isEqualTo("mgcoa9000C0");
                });
    }

    @Test
    void paging_property_is_unresolved_not_verified() {
        Map<String, Object> out = design.recommend(Map.of(
                "business", "CO",
                "function", "A",
                "transactionType", "QUERY",
                "paging", "YES"));
        @SuppressWarnings("unchecked")
        Map<String, Object> pattern = (Map<String, Object>) out.get("pattern");
        assertThat(pattern.get("status")).isIn("DERIVED_PATTERN", "UNRESOLVED_PATTERN");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> props = (List<Map<String, Object>>) pattern.get("properties");
        Map<String, Object> paging = props.stream()
                .filter(p -> "paging".equals(p.get("property")))
                .findFirst()
                .orElseThrow();
        assertThat(paging.get("status")).isEqualTo("UNRESOLVED");
    }

    @Test
    void mixed_and_report_without_A_or_R_are_operation_no_match() {
        Map<String, Object> mixed = design.recommend(Map.of(
                "business", "CO",
                "function", "A",
                "transactionType", "MIXED"));
        assertThat(mixed.get("status")).isEqualTo("OPERATION_NO_MATCH");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mixedCandidates = (List<Map<String, Object>>) mixed.get("candidates");
        assertThat(mixedCandidates).isNotEmpty();
        assertThat(mixedCandidates).allMatch(c -> Boolean.FALSE.equals(c.get("operationMatch")));

        Map<String, Object> report = design.recommend(Map.of(
                "business", "CO",
                "function", "A",
                "transactionType", "REPORT"));
        assertThat(report.get("status")).isEqualTo("OPERATION_NO_MATCH");
    }

    @Test
    void baseline_export_contract_has_no_raw_object_fields() {
        Map<String, Object> out = design.recommend(Map.of(
                "business", "CO",
                "function", "A",
                "transactionType", "QUERY",
                "paging", "YES"));
        @SuppressWarnings("unchecked")
        Map<String, Object> baseline = (Map<String, Object>) out.get("baseline");
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) baseline.get("message");
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = (Map<String, Object>) message.get("envelope");
        assertThat(envelope.get("value")).isNotNull();
        assertThat(envelope.get("status")).isEqualTo("UNRESOLVED");
        @SuppressWarnings("unchecked")
        Map<String, Object> paging = (Map<String, Object>) baseline.get("paging");
        assertThat(paging.get("value")).isEqualTo("REQUESTED_YES");
        assertThat(paging.get("status")).isEqualTo("UNRESOLVED");
        @SuppressWarnings("unchecked")
        Map<String, Object> tx = (Map<String, Object>) baseline.get("transaction");
        assertThat(tx.get("status")).isEqualTo("UNRESOLVED");
        String rendered = String.valueOf(envelope.get("value")) + paging.get("value") + tx.get("value");
        assertThat(rendered).doesNotContain("undefined", "[object Object]");
    }
}
