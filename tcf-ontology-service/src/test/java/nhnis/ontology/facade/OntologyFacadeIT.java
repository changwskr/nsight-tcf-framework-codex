package nhnis.ontology.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OntologyFacadeIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OntologyFacade facade;

    @Test
    void consistency_yaml_and_graph_aligned() {
        Map<String, Object> c = facade.consistency();
        assertThat(c.get("status")).isEqualTo("ALIGNED");
        assertThat(c.get("programsAligned")).isEqualTo(true);
        assertThat(c.get("servicesAligned")).isEqualTo(true);
    }

    @Test
    void service_includes_yaml_and_graph() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/service/mgcoa8888S0", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("architecture");
        assertThat(response.getBody()).contains("\"graph\"");
        assertThat(response.getBody()).contains("HANDLED_BY");
        assertThat(response.getBody()).contains("\"sources\"");
    }

    @Test
    void catalog_includes_graph_counts() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/ontology/catalog", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("graph");
        assertThat(response.getBody()).contains("unified");
    }

    @Test
    void consistency_endpoint() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/consistency", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("ALIGNED");
    }

    @Test
    void impact_table_includes_graphImpact() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/ontology/impact?from=TB_FW_IMAGE_LOG", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("graphImpact");
        assertThat(response.getBody()).contains("mgcoa8888S0");
    }
}
