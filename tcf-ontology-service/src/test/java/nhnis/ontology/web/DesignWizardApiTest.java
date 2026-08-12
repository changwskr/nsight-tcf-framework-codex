package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DesignWizardApiTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void service_id_validate_and_programs_and_tables_apis() {
        ResponseEntity<Map> sid = rest.postForEntity(
                "/api/ontology/design/service-id/validate",
                Map.of(
                        "system", "MG",
                        "business", "CO",
                        "function", "A",
                        "programNo", "7020",
                        "transactionType", "CREATE",
                        "sequence", "0"),
                Map.class);
        assertThat(sid.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sid.getBody().get("operation")).isEqualTo("C");
        assertThat(sid.getBody().get("serviceId")).isEqualTo("mgcoa7020C0");
        assertThat(sid.getBody().get("available")).isEqualTo(true);

        ResponseEntity<Map> programs = rest.getForEntity(
                "/api/ontology/design/programs?system=MG&business=CO&function=A", Map.class);
        assertThat(programs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(programs.getBody().get("programs")).isInstanceOf(List.class);

        ResponseEntity<Map> tables = rest.getForEntity(
                "/api/ontology/design/tables?business=CO&function=A&keyword=TB_", Map.class);
        assertThat(tables.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tables.getBody().get("tables")).isInstanceOf(List.class);
    }

    @Test
    void session_application_policy_validate_export() {
        ResponseEntity<Map> session = rest.postForEntity("/api/ontology/design/session", Map.of("title", "t"), Map.class);
        assertThat(session.getStatusCode()).isEqualTo(HttpStatus.OK);
        String sessionId = String.valueOf(session.getBody().get("sessionId"));

        Map<String, Object> sidBody = Map.of(
                "system", "MG",
                "business", "CO",
                "function", "A",
                "programNo", "7030",
                "transactionType", "QUERY",
                "sequence", "0");
        Map sid = rest.postForEntity("/api/ontology/design/service-id/validate", sidBody, Map.class).getBody();

        Map app = rest.postForEntity(
                "/api/ontology/design/application",
                Map.of("programId", sid.get("programId"), "serviceId", sid.get("serviceId")),
                Map.class).getBody();
        Map policy = rest.postForEntity(
                "/api/ontology/design/policy",
                Map.of("paging", "YES", "timeoutPolicy", "DEFAULT"),
                Map.class).getBody();

        Map<String, Object> design = Map.of(
                "requirement", Map.of("title", "wizard-api"),
                "classification", Map.of("system", "MG", "business", "CO", "function", "A"),
                "serviceIdDesign", sid,
                "dataDesign", Map.of(
                        "selectedTables", List.of(Map.of(
                                "tableName", "TB_MK_CO_A_5530",
                                "accessType", "READ",
                                "primaryKey", "L5101")),
                        "joins", List.of()),
                "application", app,
                "policy", policy);

        rest.exchange(
                "/api/ontology/design/session/" + sessionId,
                HttpMethod.PUT,
                new HttpEntity<>(design),
                Map.class);

        ResponseEntity<Map> gate = rest.postForEntity("/api/ontology/validate/design", design, Map.class);
        assertThat(gate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(gate.getBody().get("scope")).isEqualTo("DESIGN_WIZARD");

        ResponseEntity<Map> exp = rest.getForEntity(
                "/api/ontology/design/export/" + sessionId + "?format=markdown", Map.class);
        assertThat(exp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(String.valueOf(exp.getBody().get("markdown"))).contains("mgcoa7030S0");
    }

    @Test
    void table_proposal_validate_create_get() {
        Map<String, Object> column = Map.of(
                "logicalName", "고객번호",
                "physicalName", "CUST_NO",
                "dataType", "VARCHAR2",
                "length", "20",
                "primaryKey", true,
                "nullable", false,
                "personalData", "UNRESOLVED");
        Map<String, Object> body = Map.of(
                "logicalName", "고객 AI 추천 결과 관리",
                "physicalName", "TB_MK_CO_A_AI_RECOMMEND_API",
                "schema", "RDW",
                "system", "MG",
                "business", "CO",
                "function", "A",
                "tableType", "MASTER",
                "description", "API test proposal",
                "hasPersonalData", "UNRESOLVED",
                "columns", List.of(column));

        ResponseEntity<Map> validated = rest.postForEntity(
                "/api/ontology/design/table-proposal/validate", body, Map.class);
        assertThat(validated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(validated.getBody().get("status")).isIn("PASS", "PASS_WITH_UNRESOLVED");

        ResponseEntity<Map> created = rest.postForEntity(
                "/api/ontology/design/table-proposal", body, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody().get("accepted")).isEqualTo(true);
        assertThat(created.getBody().get("proposalStatus")).isEqualTo("PROPOSED");
        String id = String.valueOf(created.getBody().get("proposalId"));

        ResponseEntity<Map> got = rest.getForEntity(
                "/api/ontology/design/table-proposal/" + id, Map.class);
        assertThat(got.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(got.getBody().get("status")).isEqualTo("PROPOSED");
        assertThat(got.getBody().get("physicalName")).isEqualTo("TB_MK_CO_A_AI_RECOMMEND_API");
    }
}
