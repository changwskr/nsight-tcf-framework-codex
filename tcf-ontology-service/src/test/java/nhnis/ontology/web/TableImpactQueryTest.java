package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TableImpactQueryTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void impact_table_includes_paths_and_layers() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/ontology/impact/table/TB_FW_IMAGE_LOG", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("affectedServiceIds");
        assertThat(response.getBody()).contains("mgcoa8888S0");
        assertThat(response.getBody()).contains("affectedHandlers");
        assertThat(response.getBody()).contains("mgcoa8888Handler");
        assertThat(response.getBody()).contains("affectedPrograms");
        assertThat(response.getBody()).contains("mgcoa8888");
        assertThat(response.getBody()).contains("affectedBusinesses");
        assertThat(response.getBody()).contains("\"CO\"");
        assertThat(response.getBody()).contains("affectedFunctions");
        assertThat(response.getBody()).contains("affectedSystems");
        assertThat(response.getBody()).contains("\"MG\"");
        assertThat(response.getBody()).contains("\"type\":\"TABLE\"");
        assertThat(response.getBody()).contains("paths");
    }
}
