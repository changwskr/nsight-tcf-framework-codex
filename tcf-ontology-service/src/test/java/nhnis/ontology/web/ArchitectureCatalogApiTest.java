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
class ArchitectureCatalogApiTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void concepts_api_returns_all_architecture_objects() {
        ResponseEntity<Map> res = rest.getForEntity("/api/ontology/v1/concepts", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("totalConcepts")).isEqualTo(101);
        assertThat(res.getBody().get("count")).isEqualTo(101);
        assertThat(res.getBody().get("objects")).isInstanceOf(List.class);
        assertThat((List<?>) res.getBody().get("objects")).hasSize(101);
        assertThat(res.getBody().get("byType")).isInstanceOf(Map.class);
    }

    @Test
    void concepts_api_filters_by_type() {
        ResponseEntity<Map> res = rest.getForEntity("/api/ontology/v1/concepts?type=PROGRAM", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("totalConcepts")).isEqualTo(101);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> objects = (List<Map<String, Object>>) res.getBody().get("objects");
        assertThat(objects).isNotEmpty();
        assertThat(objects).allMatch(o -> "PROGRAM".equals(o.get("type")));
    }

    @Test
    void workbench_has_catalog_route() {
        ResponseEntity<String> index = rest.getForEntity("/workbench/index.html", String.class);
        assertThat(index.getBody()).contains("Architecture Catalog");
        assertThat(index.getBody()).contains("#/catalog");

        ResponseEntity<String> app = rest.getForEntity("/workbench/js/app.js", String.class);
        assertThat(app.getBody()).contains("renderCatalog");
        assertThat(app.getBody()).contains("OntologyApi.concepts");
    }
}
