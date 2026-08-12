package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OntologyControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void catalog_listsMgcoa9001() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/ontology/catalog", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("mgcoa9001");
        assertThat(response.getBody()).contains("mgcoa9001S0");
    }

    @Test
    void service_returnsFourAxes() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/service/mgcoa9001S0", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("architecture");
        assertThat(response.getBody()).contains("development");
        assertThat(response.getBody()).contains("data");
        assertThat(response.getBody()).contains("operations");
        assertThat(response.getBody()).contains("TB_MG_TX_CONTROL");
        assertThat(response.getBody()).contains("TransactionTemplate");
    }
}
