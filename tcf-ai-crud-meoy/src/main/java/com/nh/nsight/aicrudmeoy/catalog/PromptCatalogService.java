package com.nh.nsight.aicrudmeoy.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.aicrudmeoy.config.CrudMeoyProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromptCatalogService {

    private final CrudMeoyProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private CatalogRoot catalog;

    public PromptCatalogService(
            CrudMeoyProperties properties,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() throws IOException {
        Resource resource = resourceLoader.getResource(properties.getCatalogResource());
        try (InputStream in = resource.getInputStream()) {
            catalog = objectMapper.readValue(in, CatalogRoot.class);
        }
    }

    public CatalogRoot getCatalog() {
        return catalog;
    }

    public List<StepDefinition> listSteps() {
        return catalog.getSteps();
    }

    public Optional<StepDefinition> findStep(String id) {
        return catalog.getSteps().stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    public StepDefinition requireStep(String id) {
        return findStep(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "단계를 찾을 수 없습니다: " + id));
    }

    public String loadPromptMarkdown(String stepId) {
        StepDefinition step = requireStep(stepId);
        String location = properties.getPromptLocation();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        Resource resource = resourceLoader.getResource(location + step.getPromptFile());
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "프롬프트 파일을 읽을 수 없습니다: " + step.getPromptFile(), ex);
        }
    }

    public Map<String, Object> stepPayload(String stepId) {
        StepDefinition step = requireStep(stepId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("step", step);
        body.put("markdown", loadPromptMarkdown(stepId));
        return body;
    }
}
