package nhnis.ontology.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BusinessTreeQueryTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void business_tree_co() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/ontology/query/business/CO/tree", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"name\":\"A\"");
        assertThat(response.getBody()).contains("mgcoa8888");
        assertThat(response.getBody()).contains("mgcoa8888S0");
    }
}
