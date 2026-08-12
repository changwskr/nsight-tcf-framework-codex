package nhnis.ontology.impact;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import nhnis.ontology.prompt.PromptContextExporter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImpactAndPromptIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ImpactAnalyzer impactAnalyzer;

    @Autowired
    private PromptContextExporter promptExporter;

    @Test
    void impact_byTable_and_serviceId() {
        Map<String, Object> byTable = impactAnalyzer.analyze("TB_MG_TX_CONTROL");
        assertThat(byTable.get("status")).isEqualTo("OK");
        assertThat(byTable.get("programId")).isEqualTo("mgcoa9001");
        assertThat(byTable.get("blastRadius")).isInstanceOf(Map.class);

        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/impact?from=mgcoa9001S0", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("blastRadius");
        assertThat(response.getBody()).contains("PROVIDES_SERVICE");
        assertThat(response.getBody()).contains("HANDLED_BY");
        assertThat(response.getBody()).contains("mgcoa9001Handler");
    }

    @Test
    void path_and_recommend() {
        ResponseEntity<String> path = restTemplate.getForEntity(
                "/api/ontology/path?system=MG&business=CO&function=A&program=mgcoa8888S0",
                String.class);
        assertThat(path.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(path.getBody()).contains("mgcoa8888");
        assertThat(path.getBody()).contains("PROVIDES_SERVICE");

        ResponseEntity<String> recommend = restTemplate.getForEntity(
                "/api/ontology/recommend?system=MG&business=CO&function=A&intent=crud",
                String.class);
        assertThat(recommend.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recommend.getBody())
                .as("body=%s", recommend.getBody())
                .contains("recommendations");
        assertThat(recommend.getBody()).containsAnyOf("mgcoa9000", "mgcoa9001");
    }

    @Test
    void prompt_markdown_containsContracts() {
        String md = promptExporter.asMarkdown("mgcoa9001");
        assertThat(md).contains("Ontology Prompt Context");
        assertThat(md).contains("TB_MG_TX_CONTROL");
        assertThat(md).contains("R-HANDLER-NO-DAO");
        assertThat(md).contains("mgcoa9001S0");

        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/prompt/mgcoa9001.md", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("packageRoot");
    }
}
