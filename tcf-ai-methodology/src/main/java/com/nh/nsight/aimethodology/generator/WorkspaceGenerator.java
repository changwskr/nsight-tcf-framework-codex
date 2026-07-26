package com.nh.nsight.aimethodology.generator;

import com.nh.nsight.aimethodology.model.BusinessModel;
import com.nh.nsight.aimethodology.model.ValidationIssue;
import com.nh.nsight.aimethodology.validation.ModelValidator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

/**
 * 워크스페이스 전체 산출물 생성 및 ZIP 패키징.
 */
@Service
public class WorkspaceGenerator {

    private final ModelValidator validator;

    public WorkspaceGenerator(ModelValidator validator) {
        this.validator = validator;
    }

    public Map<String, String> generateWorkspace(List<BusinessModel> models) {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("생성할 모델이 없습니다.");
        }

        List<ValidationIssue> issues = new ArrayList<>();
        for (BusinessModel model : models) {
            issues.addAll(validator.validateModel(model));
        }
        issues.addAll(validator.validateWorkspace(models));
        if (validator.hasErrors(issues)) {
            String messages = issues.stream()
                    .filter(item -> "ERROR".equals(item.getLevel()))
                    .map(ValidationIssue::getMessage)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("");
            throw new IllegalArgumentException("모델 검증 실패: " + messages);
        }

        Map<String, String> artifacts = new LinkedHashMap<>();
        Map<String, List<BusinessModel>> groups = new TreeMap<>();
        for (BusinessModel model : models) {
            String profile = model.getPackageProfile() == null || model.getPackageProfile().isBlank()
                    ? "CURRENT_SOURCE" : model.getPackageProfile();
            String key = model.getBusinessCode() + "\0" + model.getDomainCode() + "\0" + profile;
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(model);
        }

        for (List<BusinessModel> group : groups.values()) {
            group.sort(Comparator.comparing(BusinessModel::getServiceId,
                    Comparator.nullsLast(String::compareTo)));
            artifacts.putAll(DomainArtifactGenerator.generateDomainClasses(group));
            BusinessModel first = group.get(0);
            Map.Entry<String, String> test = DocArtifactGenerator.generateRuleTest(first);
            artifacts.put(test.getKey(), test.getValue());
        }

        Set<String> seenTables = new HashSet<>();
        for (BusinessModel model : models) {
            put(artifacts, DtoArtifactGenerator.generateRequestDto(model));
            put(artifacts, DtoArtifactGenerator.generateResponseDto(model));
            if ("SELECT_ONE".equals(model.getOperation()) || "SELECT_LIST".equals(model.getOperation())) {
                put(artifacts, DtoArtifactGenerator.generateCriteriaDto(model));
                put(artifacts, DtoArtifactGenerator.generateRowDto(model));
            }
            if (model.getTableName() != null && seenTables.add(model.getTableName())) {
                put(artifacts, DocArtifactGenerator.generateDdl(model));
            }
            try {
                put(artifacts, DocArtifactGenerator.generateOmCatalog(model));
                put(artifacts, DocArtifactGenerator.generateHttpRequest(model));
                put(artifacts, DocArtifactGenerator.generateScreenDefinition(model));
                put(artifacts, DocArtifactGenerator.generateTransactionDefinition(model));
            } catch (Exception ex) {
                throw new IllegalStateException("문서 산출물 생성 실패: " + model.getServiceId(), ex);
            }
        }

        put(artifacts, DocArtifactGenerator.generateTraceabilityCsv(models));
        put(artifacts, DocArtifactGenerator.generateQualityGate(models));
        try {
            put(artifacts, DocArtifactGenerator.generateManifest(models, artifacts));
        } catch (Exception ex) {
            throw new IllegalStateException("manifest 생성 실패", ex);
        }
        return artifacts;
    }

    public byte[] artifactsToZip(Map<String, String> artifacts) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ZipOutputStream archive = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
                List<String> paths = artifacts.keySet().stream().sorted().toList();
                for (String path : paths) {
                    String content = artifacts.get(path);
                    ZipEntry entry = new ZipEntry(path);
                    archive.putNextEntry(entry);
                    byte[] bytes = path.endsWith(".csv")
                            ? (content == null ? new byte[0] : ("\uFEFF" + content).getBytes(StandardCharsets.UTF_8))
                            : (content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
                    archive.write(bytes);
                    archive.closeEntry();
                }
            }
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("ZIP 생성 실패", ex);
        }
    }

    private static void put(Map<String, String> artifacts, Map.Entry<String, String> entry) {
        artifacts.put(entry.getKey(), entry.getValue());
    }
}
