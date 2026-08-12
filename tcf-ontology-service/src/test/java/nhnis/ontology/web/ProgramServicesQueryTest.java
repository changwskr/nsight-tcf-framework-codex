package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProgramServicesQueryTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void program_services_include_S0_and_D0() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/ontology/query/program/mgcoa8888/services", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("mgcoa8888S0");
        assertThat(response.getBody()).contains("mgcoa8888D0");
    }
}
