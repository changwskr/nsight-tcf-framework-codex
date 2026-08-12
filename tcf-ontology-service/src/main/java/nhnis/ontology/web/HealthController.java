package nhnis.ontology.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @Value("${nhnis.ontology.product-version:0.1.0-RC1}")
    private String productVersion;

    @Value("${nhnis.ontology.schema-version:1.0}")
    private String ontologySchemaVersion;

    @Value("${nhnis.ontology.knowledge-snapshot:2026.08.10.03}")
    private String knowledgeSnapshot;

    @GetMapping({"/", "/health"})
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "tcf-ontology-service");
        body.put("status", "UP");
        body.put("workbench", "workbench/index.html");
        body.put("productVersion", productVersion);
        body.put("ontologySchemaVersion", ontologySchemaVersion);
        body.put("knowledgeSnapshot", knowledgeSnapshot);
        body.put("apiVersion", "v1");
        return body;
    }
}
