package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkbenchStaticUiTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void workbench_index_is_served() {
        ResponseEntity<String> response = restTemplate.getForEntity("/workbench/index.html", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("NSIGHT Architect Workbench");
        assertThat(response.getBody()).contains("Architecture Catalog");
        assertThat(response.getBody()).contains("Architecture QnA");
        assertThat(response.getBody()).contains("Architecture Knowledge");
        assertThat(response.getBody()).contains("Architecture Search");
        assertThat(response.getBody()).contains("Architecture Design");
        assertThat(response.getBody()).contains("./js/design.js");
        assertThat(response.getBody()).contains("./js/table-proposal.js");
        assertThat(response.getBody()).contains("./js/app.js");
    }

    @Test
    void workbench_assets_are_served() {
        assertThat(restTemplate.getForEntity("/workbench/js/api.js", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.getForEntity("/workbench/js/design.js", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.getForEntity("/workbench/js/table-proposal.js", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.getForEntity("/workbench/js/markdown.js", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.getForEntity("/workbench/css/workbench.css", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void workbench_app_js_has_design_route_and_meta() {
        ResponseEntity<String> app = restTemplate.getForEntity("/workbench/js/app.js", String.class);
        assertThat(app.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(app.getBody()).contains("catalog:");
        assertThat(app.getBody()).contains("renderCatalog");
        assertThat(app.getBody()).contains("design:");
        assertThat(app.getBody()).contains("Architecture Design");
        assertThat(app.getBody()).contains("DesignAssistant.render");
        assertThat(app.getBody()).contains("safe === \"design\"");
        // Program 9-char vs ServiceId 11-char AUTO detect
        assertThat(app.getBody()).contains("[a-z]{2}[a-z]{2}[a-z]\\d{4}[SCUDAR]");
        assertThat(app.getBody()).contains("return \"PROGRAM\"");

        ResponseEntity<String> design = restTemplate.getForEntity("/workbench/js/design.js", String.class);
        assertThat(design.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(design.getBody()).contains("DesignAssistant");
        assertThat(design.getBody()).contains("designServiceIdValidate");
        assertThat(design.getBody()).contains("classificationScheme");
        assertThat(design.getBody()).contains("NSIGHT 애플리케이션 코드 분류표");
        assertThat(design.getBody()).contains("packageRoot");
        assertThat(design.getBody()).contains("STEP 3 · ServiceId Design");
        assertThat(design.getBody()).contains("STEP 4 · Data / Table Design");
        assertThat(design.getBody()).contains("NEW_TABLE_PROPOSAL");
        assertThat(design.getBody()).contains("tableProposals");
        assertThat(design.getBody()).contains("+ 신규 Table 설계");
        assertThat(design.getBody()).contains("TableProposalWizard");
        assertThat(design.getBody()).contains("designValidate");

        ResponseEntity<String> ntp = restTemplate.getForEntity("/workbench/js/table-proposal.js", String.class);
        assertThat(ntp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ntp.getBody()).contains("TableProposalWizard");
        assertThat(ntp.getBody()).contains("4-3 Column Design");
        assertThat(ntp.getBody()).contains("PROPOSED");
    }

    @Test
    void classification_bundle_is_available() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/bundle/business/classification.yml", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Market Group Platform");
        assertThat(response.getBody()).contains("\"code\":\"CO\"");
        assertThat(response.getBody()).contains("통합고객");
    }
}
