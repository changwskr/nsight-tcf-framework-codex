package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OntologyQueryControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void meta_listsConceptAndRelationTypes() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/ontology/v1/meta", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("conceptTypes", "relationTypes", "counts");
    }

    @Test
    void serviceId_parsesMgcoa8888S0() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/api/ontology/v1/service-id/mgcoa8888S0", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("programId")).isEqualTo("mgcoa8888");
        assertThat(response.getBody().get("operationType")).isEqualTo("S");
        assertThat(response.getBody().get("registered")).isEqualTo(true);
    }

    @Test
    void chain_returnsClassificationAndCallChain() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/v1/chain/mgcoa8888S0", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("HAS_BUSINESS");
        assertThat(response.getBody()).contains("PROVIDES_SERVICE");
        assertThat(response.getBody()).contains("HANDLED_BY");
        assertThat(response.getBody()).contains("TB_FW_IMAGE_LOG");
        assertThat(response.getBody()).contains("mgcoa8888Handler");
    }

    @Test
    void impact_table_listsServiceIds() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/v1/impact/table/TB_FW_IMAGE_LOG", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("mgcoa8888S0");
        assertThat(response.getBody()).containsAnyOf("mgcoa8888-ORA.xml", "mgcoa8888S0_S0", "affectedMappers");
    }

    @Test
    void concept_program() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/v1/concept/mgcoa8888", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("PROGRAM");
        assertThat(response.getBody()).contains("PROVIDES_SERVICE");
    }
}
