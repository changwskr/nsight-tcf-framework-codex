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
class ArchitectureKnowledgeQnAApiTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void knowledge_catalog_loads_exearchidoc() {
        ResponseEntity<Map> res = rest.getForEntity("/api/ontology/knowledge", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) res.getBody().get("total")).intValue()).isGreaterThanOrEqualTo(80);
        assertThat(res.getBody().get("documents")).isInstanceOf(List.class);
        assertThat(String.valueOf(res.getBody().get("source"))).contains("exearchidoc");
    }

    @Test
    void knowledge_document_and_search() {
        ResponseEntity<Map> catalog = rest.getForEntity("/api/ontology/knowledge?q=Timeout", Map.class);
        assertThat(catalog.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) catalog.getBody().get("count")).intValue()).isGreaterThan(0);

        ResponseEntity<Map> search = rest.getForEntity(
                "/api/ontology/knowledge/search?q=Filter%20Interceptor&limit=5", Map.class);
        assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) search.getBody().get("count")).intValue()).isGreaterThan(0);
    }

    @Test
    void qna_ask_uses_exearchidoc_references() {
        ResponseEntity<Map> res = rest.postForEntity(
                "/api/ontology/qna/ask",
                Map.of("question", "TimeoutExecutor와 Transaction 경계는 무엇인가?", "topK", 5),
                Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("status")).isEqualTo("OK");
        assertThat(res.getBody().get("mode")).isEqualTo("EXTRACTIVE_RETRIEVAL");
        assertThat(String.valueOf(res.getBody().get("corpus"))).contains("exearchidoc");
        assertThat(((Number) res.getBody().get("referenceCount")).intValue()).isGreaterThan(0);
        assertThat(String.valueOf(res.getBody().get("answer"))).contains("exearchidoc");
    }

    @Test
    void workbench_menu_has_qna_and_knowledge() {
        ResponseEntity<String> index = rest.getForEntity("/workbench/index.html", String.class);
        assertThat(index.getBody()).contains("Architecture QnA");
        assertThat(index.getBody()).contains("Architecture Knowledge");
        assertThat(index.getBody()).contains("#/qna");
        assertThat(index.getBody()).contains("#/knowledge");

        ResponseEntity<String> app = rest.getForEntity("/workbench/js/app.js", String.class);
        assertThat(app.getBody()).contains("renderQnA");
        assertThat(app.getBody()).contains("renderKnowledge");
        assertThat(app.getBody()).contains("WorkbenchMarkdown");
        assertThat(app.getBody()).contains("qnaAsk");
        assertThat(rest.getForEntity("/workbench/js/markdown.js", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
