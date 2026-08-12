package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServiceStructureQueryTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void structure_of_mgcoa8888S0() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/ontology/query/service/mgcoa8888S0/structure", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("HANDLED_BY");
        assertThat(response.getBody()).contains("mgcoa8888Handler");
        assertThat(response.getBody()).contains("mgcoa8888Facade");
        assertThat(response.getBody()).contains("TB_FW_IMAGE_LOG");
        assertThat(response.getBody()).contains("provenance");
    }
}
