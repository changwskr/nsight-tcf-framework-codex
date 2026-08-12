package nhnis.ontology.design;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TableProposalServiceTest {

    @Autowired
    TableProposalService service;

    @Autowired
    DesignWizardService wizard;

    private Map<String, Object> goldenProposal() {
        Map<String, Object> colPk = new LinkedHashMap<>();
        colPk.put("logicalName", "고객번호");
        colPk.put("physicalName", "CUST_NO");
        colPk.put("dataType", "VARCHAR2");
        colPk.put("length", "20");
        colPk.put("primaryKey", true);
        colPk.put("nullable", false);
        colPk.put("personalData", "UNRESOLVED");
        colPk.put("encryption", "UNRESOLVED");
        colPk.put("masking", "UNRESOLVED");

        Map<String, Object> colScore = new LinkedHashMap<>();
        colScore.put("logicalName", "추천점수");
        colScore.put("physicalName", "RECOMMEND_SCORE");
        colScore.put("dataType", "NUMBER");
        colScore.put("precision", "10");
        colScore.put("scale", "2");
        colScore.put("primaryKey", false);
        colScore.put("nullable", true);
        colScore.put("personalData", "UNRESOLVED");
        colScore.put("encryption", "UNRESOLVED");
        colScore.put("masking", "UNRESOLVED");

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("logicalName", "고객 AI 추천 결과 관리");
        p.put("physicalName", "TB_MK_CO_A_AI_RECOMMEND");
        p.put("schema", "RDW");
        p.put("system", "MG");
        p.put("business", "CO");
        p.put("function", "A");
        p.put("tableType", "MASTER");
        p.put("description", "고객별 AI 추천 결과 및 추천점수를 저장한다.");
        p.put("accessType", "CREATE");
        p.put("hasPersonalData", "UNRESOLVED");
        p.put("columns", List.of(colPk, colScore));
        return p;
    }

    @Test
    void validate_fails_when_no_columns() {
        Map<String, Object> raw = goldenProposal();
        raw.put("columns", List.of());
        Map<String, Object> res = service.validate(raw);
        assertThat(res.get("status")).isEqualTo("FAIL");
        assertThat(String.valueOf(res.get("findings"))).contains("AT_LEAST_ONE_COLUMN_REQUIRED");
    }

    @Test
    void create_stays_proposed_never_verified() {
        Map<String, Object> res = service.create(goldenProposal());
        assertThat(res.get("accepted")).isEqualTo(true);
        assertThat(res.get("proposalStatus")).isEqualTo("PROPOSED");
        @SuppressWarnings("unchecked")
        Map<String, Object> proposal = (Map<String, Object>) res.get("proposal");
        assertThat(proposal.get("status")).isEqualTo("PROPOSED");
        assertThat(proposal.get("mode")).isEqualTo("NEW_TABLE_PROPOSAL");
        assertThat(proposal.get("verificationStatus")).isEqualTo("PROPOSED");
        assertThat(proposal.get("primaryKey")).isEqualTo(List.of("CUST_NO"));
        assertThat(proposal.get("ontologyStatus")).isEqualTo("PROPOSED");
    }

    @Test
    void composite_pk_from_column_flags() {
        Map<String, Object> raw = goldenProposal();
        raw.put("physicalName", "TB_MK_CO_A_AI_RECOMMEND_COMPOSITE");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cols = (List<Map<String, Object>>) raw.get("columns");
        cols.get(1).put("primaryKey", true);
        cols.get(1).put("nullable", false);
        Map<String, Object> res = service.validate(raw);
        assertThat(res.get("status")).isIn("PASS", "PASS_WITH_UNRESOLVED");
        @SuppressWarnings("unchecked")
        Map<String, Object> proposal = (Map<String, Object>) res.get("proposal");
        assertThat(proposal.get("primaryKey")).isEqualTo(List.of("CUST_NO", "RECOMMEND_SCORE"));
    }

    @Test
    void design_gate_and_export_include_table_proposal() {
        Map<String, Object> body = goldenProposal();
        body.put("physicalName", "TB_MK_CO_A_AI_RECOMMEND_GATE");
        Map<String, Object> created = service.create(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> proposal = (Map<String, Object>) created.get("proposal");

        Map<String, Object> design = new LinkedHashMap<>();
        design.put("requirement", Map.of("title", "고객별 AI 추천 결과 저장"));
        design.put("classification", Map.of("system", "MG", "business", "CO", "function", "A"));
        design.put("serviceIdDesign", Map.of(
                "serviceId", "mgcoa7099S0",
                "programId", "mgcoa7099",
                "available", true));
        design.put("dataDesign", Map.of(
                "selectedTables", List.of(),
                "tableProposals", List.of(proposal),
                "tableUnresolved", false));
        design.put("application", Map.of("components", Map.of("handler", "mgcoa7099Handler")));
        design.put("policy", Map.of("paging", "UNRESOLVED"));

        Map<String, Object> gate = wizard.validateDesign(design);
        assertThat(gate.get("scope")).isEqualTo("DESIGN_WIZARD");
        assertThat(String.valueOf(gate.get("findings"))).contains("DATA-TBL-PROPOSAL");
        assertThat(String.valueOf(gate.get("findings"))).doesNotContain("must not be VERIFIED");

        Map<String, Object> exp = wizard.export(design, "markdown");
        assertThat(String.valueOf(exp.get("markdown"))).contains("NEW_TABLE_PROPOSAL");
        assertThat(String.valueOf(exp.get("markdown"))).contains("TB_MK_CO_A_AI_RECOMMEND_GATE");
    }
}
