package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DashboardDetailApiTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void summary_and_detail_views() {
        ResponseEntity<Map> summary = rest.getForEntity("/api/ontology/dashboard", Map.class);
        assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(summary.getBody().get("concepts")).isEqualTo(101);
        assertThat(summary.getBody().get("relations")).isEqualTo(153);
        assertThat(summary.getBody().get("programs")).isEqualTo(5);
        assertThat(summary.getBody().get("serviceIds")).isEqualTo(12);
        assertThat(summary.getBody()).containsKey("designSessions");

        assertDetail("concepts", 101);
        assertDetail("relations", 153);

        ResponseEntity<Map> programs = rest.getForEntity("/api/ontology/dashboard/programs", Map.class);
        assertThat(programs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) programs.getBody().get("ontologyCount")).longValue()).isEqualTo(5L);

        ResponseEntity<Map> services = rest.getForEntity("/api/ontology/dashboard/services", Map.class);
        assertThat(services.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) services.getBody().get("ontologyCount")).longValue()).isEqualTo(12L);

        ResponseEntity<Map> designs = rest.getForEntity("/api/ontology/dashboard/designs", Map.class);
        assertThat(designs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(designs.getBody().get("view")).isEqualTo("designs");

        ResponseEntity<Map> rules = rest.getForEntity("/api/ontology/dashboard/rules-fail", Map.class);
        assertThat(rules.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rules.getBody().get("view")).isEqualTo("rules-fail");
        assertThat(rules.getBody().get("items")).isInstanceOf(List.class);
    }

    @Test
    void workbench_dashboard_route_exists() {
        ResponseEntity<String> app = rest.getForEntity("/workbench/js/app.js", String.class);
        assertThat(app.getBody()).contains("renderDashboard");
        assertThat(app.getBody()).contains("dashboard?view=concepts");
        assertThat(app.getBody()).contains("dashboard?view=relations");
        assertThat(app.getBody()).contains("dashboard?view=programs");
        assertThat(app.getBody()).contains("dashboard?view=services");
        assertThat(app.getBody()).contains("dashboard?view=designs");
        assertThat(app.getBody()).contains("dashboard?view=rules-fail");
        assertThat(app.getBody()).contains("Designs (PROPOSED)");
        assertThat(app.getBody()).contains("wb-stat--link");
    }

    @Test
    void completed_design_appears_in_dashboard_designs_and_services() {
        ResponseEntity<Map> session = rest.postForEntity(
                "/api/ontology/design/session",
                Map.of("title", "통합고객 관리"),
                Map.class);
        assertThat(session.getStatusCode()).isEqualTo(HttpStatus.OK);
        String sessionId = String.valueOf(session.getBody().get("sessionId"));

        Map<String, Object> design = Map.of(
                "requirement", Map.of("title", "통합고객 관리"),
                "classification", Map.of(
                        "system", "MG",
                        "business", "IC",
                        "function", "B",
                        "packageRoot", "nhnis.mg.ic.b"),
                "serviceIdDesign", Map.of(
                        "programId", "mgicb7000",
                        "serviceId", "mgicb7000S0",
                        "operation", "S",
                        "available", true,
                        "status", "PROPOSED",
                        "confirmed", true),
                "dataDesign", Map.of(
                        "selectedTables", List.of(),
                        "tableProposals", List.of(Map.of(
                                "mode", "NEW_TABLE_PROPOSAL",
                                "status", "PROPOSED",
                                "physicalName", "TB_MG_IC_7000",
                                "logicalName", "TB_MG_IC_7000",
                                "columns", List.of(Map.of(
                                        "logicalName", "고객명",
                                        "physicalName", "NAME",
                                        "dataType", "VARCHAR2",
                                        "length", "20",
                                        "primaryKey", true,
                                        "nullable", false))))),
                "application", Map.of("components", Map.of("handler", "mgicb7000Handler")),
                "policy", Map.of("paging", Map.of("enabled", Map.of("value", "YES"))),
                "gate", Map.of(
                        "scope", "DESIGN_WIZARD",
                        "status", "PASS_WITH_UNRESOLVED",
                        "failCount", 0,
                        "unresolvedCount", 1,
                        "findings", List.of()));

        ResponseEntity<Map> completed = rest.postForEntity(
                "/api/ontology/design/session/" + sessionId + "/complete",
                design,
                Map.class);
        assertThat(completed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completed.getBody().get("status")).isEqualTo("COMPLETED");

        ResponseEntity<Map> designs = rest.getForEntity(
                "/api/ontology/dashboard/designs?q=mgicb7000", Map.class);
        assertThat(designs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(designs.getBody().get("view")).isEqualTo("designs");
        assertThat((List<?>) designs.getBody().get("items")).isNotEmpty();

        ResponseEntity<Map> services = rest.getForEntity(
                "/api/ontology/dashboard/services?q=mgicb7000S0", Map.class);
        assertThat(services.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> svcItems = (List<Map<String, Object>>) services.getBody().get("items");
        assertThat(svcItems.stream().anyMatch(m ->
                "mgicb7000S0".equals(String.valueOf(m.get("serviceId")))
                        && "PROPOSED".equals(String.valueOf(m.get("verificationStatus"))))).isTrue();
    }

    private void assertDetail(String view, int expectedTotal) {
        ResponseEntity<Map> res = rest.getForEntity("/api/ontology/dashboard/" + view, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("total")).isEqualTo(expectedTotal);
        assertThat(res.getBody().get("items")).isInstanceOf(List.class);
        assertThat((List<?>) res.getBody().get("items")).hasSize(expectedTotal);
    }
}
