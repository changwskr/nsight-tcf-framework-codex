package com.nh.nsight.aimethodology.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.aimethodology.config.ModelStudioProperties;
import com.nh.nsight.aimethodology.generator.WorkspaceGenerator;
import com.nh.nsight.aimethodology.model.BusinessModel;
import com.nh.nsight.aimethodology.model.ValidationIssue;
import com.nh.nsight.aimethodology.store.ModelStore;
import com.nh.nsight.aimethodology.validation.ModelValidator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ModelStudioController {

    private final ModelStore store;
    private final ModelValidator validator;
    private final WorkspaceGenerator generator;
    private final ModelStudioProperties properties;
    private final ObjectMapper objectMapper;

    public ModelStudioController(
            ModelStore store,
            ModelValidator validator,
            WorkspaceGenerator generator,
            ModelStudioProperties properties,
            ObjectMapper objectMapper) {
        this.store = store;
        this.validator = validator;
        this.generator = generator;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("application", "NSIGHT Model Studio");
        body.put("version", properties.getVersion());
        body.put("runtime", "Spring Boot");
        body.put("storage", "database");
        body.put("modelCount", store.count());
        return body;
    }

    @GetMapping("/models")
    public Map<String, Object> listModels(@RequestParam(value = "q", required = false) String query) {
        return Map.of("models", store.search(query));
    }

    @GetMapping("/models/{id}")
    public BusinessModel getModel(@PathVariable String id) {
        return store.get(id).orElseThrow(() -> notFound("모델을 찾을 수 없습니다."));
    }

    @GetMapping("/sample")
    public BusinessModel sample() {
        try {
            return store.loadSample();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "샘플 모델을 읽을 수 없습니다.", ex);
        }
    }

    @PostMapping("/models")
    public ResponseEntity<BusinessModel> create(@RequestBody BusinessModel model) {
        return ResponseEntity.status(HttpStatus.CREATED).body(store.save(model));
    }

    @PutMapping("/models/{id}")
    public BusinessModel update(@PathVariable String id, @RequestBody BusinessModel model) {
        model.setId(id);
        return store.save(model);
    }

    @DeleteMapping("/models/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        if (!store.delete(id)) {
            throw notFound("삭제할 모델을 찾을 수 없습니다.");
        }
        return Map.of("deleted", true);
    }

    @PostMapping("/models/{id}/duplicate")
    public ResponseEntity<BusinessModel> duplicate(@PathVariable String id) {
        BusinessModel duplicated = store.duplicate(id)
                .orElseThrow(() -> notFound("복제할 모델을 찾을 수 없습니다."));
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicated);
    }

    @PostMapping("/models/reseed")
    public Map<String, Object> reseed() {
        try {
            List<BusinessModel> models = store.reseedFromClasspath();
            return Map.of("reseeded", true, "count", models.size(), "models", models);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "시드 재적재에 실패했습니다.", ex);
        }
    }

    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody Map<String, Object> payload) {
        return issueResponse(validator.validateModel(extractModel(payload)));
    }

    @PostMapping("/validate-workspace")
    public Map<String, Object> validateWorkspace(@RequestBody Map<String, Object> payload) {
        List<BusinessModel> models = extractModels(payload);
        if (models.isEmpty()) {
            models = store.list();
        }
        List<ValidationIssue> issues = new ArrayList<>();
        for (BusinessModel model : models) {
            issues.addAll(validator.validateModel(model));
        }
        issues.addAll(validator.validateWorkspace(models));
        return issueResponse(issues);
    }

    @PostMapping("/preview")
    public Map<String, Object> preview(@RequestBody Map<String, Object> payload) {
        List<BusinessModel> models = extractModels(payload);
        Map<String, String> artifacts = generator.generateWorkspace(models);
        List<String> paths = artifacts.keySet().stream().sorted().toList();
        Object pathObj = payload.get("path");
        String selected = pathObj == null ? null : String.valueOf(pathObj);
        if (selected != null && !selected.isBlank()) {
            if (!artifacts.containsKey(selected)) {
                throw notFound("미리보기 파일을 찾을 수 없습니다.");
            }
            return Map.of("path", selected, "content", artifacts.get(selected), "paths", paths);
        }
        String defaultPath = paths.stream()
                .filter(p -> p.endsWith("Handler.java"))
                .findFirst()
                .orElse(paths.get(0));
        return Map.of("path", defaultPath, "content", artifacts.get(defaultPath), "paths", paths);
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@RequestBody Map<String, Object> payload) {
        List<BusinessModel> models = extractModels(payload);
        Map<String, String> artifacts = generator.generateWorkspace(models);
        byte[] zip = generator.artifactsToZip(artifacts);
        Object filenameObj = payload.get("filename");
        String filename = filenameObj == null ? "nsight-generated-workspace.zip" : String.valueOf(filenameObj);
        filename = filename.chars()
                .filter(ch -> Character.isLetterOrDigit(ch) || "-_.".indexOf(ch) >= 0)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (filename.isBlank()) {
            filename = "nsight-generated-workspace.zip";
        }
        return zipResponse(zip, filename);
    }

    @PostMapping("/generate-saved")
    public ResponseEntity<byte[]> generateSaved(@RequestBody(required = false) Map<String, Object> payload) {
        List<BusinessModel> models = store.list();
        if (payload != null && payload.get("ids") instanceof List<?> ids && !ids.isEmpty()) {
            Set<String> selected = ids.stream().map(String::valueOf).collect(Collectors.toSet());
            models = models.stream().filter(m -> selected.contains(m.getId())).toList();
        }
        Map<String, String> artifacts = generator.generateWorkspace(models);
        return zipResponse(generator.artifactsToZip(artifacts), "nsight-saved-models.zip");
    }

    private Map<String, Object> issueResponse(List<ValidationIssue> issues) {
        long errors = issues.stream().filter(i -> "ERROR".equals(i.getLevel())).count();
        long warnings = issues.stream().filter(i -> "WARNING".equals(i.getLevel())).count();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("issues", issues);
        body.put("errorCount", errors);
        body.put("warningCount", warnings);
        return body;
    }

    private ResponseEntity<byte[]> zipResponse(byte[] zip, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    private BusinessModel extractModel(Map<String, Object> payload) {
        Object model = payload.get("model");
        if (model instanceof Map<?, ?>) {
            return objectMapper.convertValue(model, BusinessModel.class);
        }
        return objectMapper.convertValue(payload, BusinessModel.class);
    }

    private List<BusinessModel> extractModels(Map<String, Object> payload) {
        Object models = payload.get("models");
        if (models instanceof List<?> list) {
            return list.stream()
                    .map(item -> objectMapper.convertValue(item, BusinessModel.class))
                    .toList();
        }
        Object model = payload.get("model");
        if (model != null) {
            return List.of(objectMapper.convertValue(model, BusinessModel.class));
        }
        if (payload.containsKey("serviceId") || payload.containsKey("businessCode")) {
            return List.of(objectMapper.convertValue(payload, BusinessModel.class));
        }
        return List.of();
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
