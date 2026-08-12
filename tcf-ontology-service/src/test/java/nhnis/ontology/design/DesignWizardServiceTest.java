package nhnis.ontology.design;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DesignWizardServiceTest {

    @Autowired
    DesignWizardService wizard;

    @Test
    void serviceId_query_maps_to_S_and_unused_program_is_available() {
        Map<String, Object> res = wizard.validateServiceId(Map.of(
                "system", "MG",
                "business", "CO",
                "function", "A",
                "programNo", "7000",
                "transactionType", "QUERY",
                "sequence", "0"));
        assertThat(res.get("operation")).isEqualTo("S");
        assertThat(res.get("programId")).isEqualTo("mgcoa7000");
        assertThat(res.get("serviceId")).isEqualTo("mgcoa7000S0");
        assertThat(res.get("available")).isEqualTo(true);
        assertThat(res.get("status")).isEqualTo("PROPOSED");
    }

    @Test
    void serviceId_duplicate_existing_program_fails() {
        Map<String, Object> res = wizard.validateServiceId(Map.of(
                "system", "MG",
                "business", "CO",
                "function", "A",
                "programNo", "5530",
                "transactionType", "QUERY",
                "sequence", "0"));
        assertThat(res.get("available")).isEqualTo(false);
        assertThat(String.valueOf(res.get("status"))).isEqualTo("REJECTED");
    }

    @Test
    void programs_list_for_mg_co_a_includes_used_and_proposed() {
        Map<String, Object> res = wizard.listPrograms("MG", "CO", "A");
        assertThat(res.get("usedProgramNos")).asList().contains("5530");
        assertThat(res.get("proposedProgramNos")).asList().isNotEmpty();
        assertThat(res.get("programs")).asList().isNotEmpty();
    }

    @Test
    void tables_search_returns_ontology_tables_only() {
        Map<String, Object> res = wizard.searchTables("CO", "A", "TB_MK", null);
        assertThat(res.get("count")).isInstanceOf(Number.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) res.get("tables");
        assertThat(tables).isNotEmpty();
        assertThat(tables.get(0)).containsKey("tableName");
    }

    @Test
    void application_and_policy_and_export_golden_path() {
        Map<String, Object> sid = wizard.validateServiceId(Map.of(
                "system", "mg",
                "business", "co",
                "function", "a",
                "programNo", "7011",
                "transactionType", "QUERY",
                "sequence", "0"));
        Map<String, Object> app = wizard.proposeApplication(Map.of(
                "programId", sid.get("programId"),
                "serviceId", sid.get("serviceId")));
        assertThat(app.get("components")).isInstanceOf(Map.class);
        Map<?, ?> comps = (Map<?, ?>) app.get("components");
        assertThat(comps.get("handler")).isEqualTo("mgcoa7011Handler");

        Map<String, Object> policy = wizard.proposePolicy(Map.of(
                "paging", "YES",
                "timeoutPolicy", "DEFAULT",
                "personalData", "UNKNOWN"));
        assertThat(policy.get("paging")).isInstanceOf(Map.class);

        Map<String, Object> design = Map.of(
                "requirement", Map.of("title", "마케팅희망고객 조회"),
                "classification", Map.of("system", "MG", "business", "CO", "function", "A"),
                "serviceIdDesign", sid,
                "dataDesign", Map.of(
                        "selectedTables", List.of(Map.of(
                                "tableName", "TB_MK_CO_A_5530",
                                "accessType", "READ",
                                "primaryKey", "L5101,L5103",
                                "selectColumns", List.of("L5101", "L5103"))),
                        "joins", List.of()),
                "application", app,
                "policy", policy);

        Map<String, Object> gate = wizard.validateDesign(design);
        assertThat(gate.get("status")).isIn("PASS", "PASS_WITH_UNRESOLVED");

        Map<String, Object> exp = wizard.export(design, "markdown");
        String md = String.valueOf(exp.get("markdown"));
        assertThat(md).contains("mgcoa7011S0");
        assertThat(md).contains("TB_MK_CO_A_5530");
        assertThat(md).doesNotContain("undefined");
        assertThat(md).doesNotContain("[object Object]");
    }
}
