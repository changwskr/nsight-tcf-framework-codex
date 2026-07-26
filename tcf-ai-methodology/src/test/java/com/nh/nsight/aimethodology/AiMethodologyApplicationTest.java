package com.nh.nsight.aimethodology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.aimethodology.generator.WorkspaceGenerator;
import com.nh.nsight.aimethodology.model.BusinessModel;
import com.nh.nsight.aimethodology.model.ValidationIssue;
import com.nh.nsight.aimethodology.store.ModelStore;
import com.nh.nsight.aimethodology.validation.ModelValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AiMethodologyApplicationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelValidator validator;

    @Autowired
    private WorkspaceGenerator generator;

    @Autowired
    private ModelStore modelStore;

    @Test
    void sampleModelValidatesAndGeneratesHandler() throws Exception {
        BusinessModel sample = objectMapper.readValue(
                new ClassPathResource("data/sample_model.json").getInputStream(),
                BusinessModel.class);
        assertFalse(validator.hasErrors(validator.validateModel(sample)));

        Map<String, String> artifacts = generator.generateWorkspace(List.of(sample));
        assertTrue(artifacts.keySet().stream().anyMatch(p -> p.endsWith("SvCustomerHandler.java")));
        assertTrue(artifacts.containsKey("manifest.json"));
        assertTrue(generator.artifactsToZip(artifacts).length > 100);
    }

    @Test
    void businessModelsPersistInDatabase() {
        assertTrue(modelStore.count() > 0);
        BusinessModel first = modelStore.list().get(0);
        assertTrue(modelStore.get(first.getId()).isPresent());
    }

    @Test
    void seedModelsValidateWithoutErrors() throws Exception {
        @SuppressWarnings("unchecked")
        List<BusinessModel> seed = objectMapper.readValue(
                new ClassPathResource("data/models-seed.json").getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, BusinessModel.class));
        assertTrue(seed.size() >= 30);
        List<ValidationIssue> issues = new ArrayList<>();
        for (BusinessModel model : seed) {
            issues.addAll(validator.validateModel(model));
        }
        issues.addAll(validator.validateWorkspace(seed));
        List<ValidationIssue> errors = issues.stream()
                .filter(i -> "ERROR".equalsIgnoreCase(i.getLevel()))
                .toList();
        assertTrue(errors.isEmpty(), () -> "seed validation errors: " + errors.stream()
                .map(i -> i.getCode() + " " + i.getPath() + " " + i.getMessage())
                .reduce((a, b) -> a + " | " + b)
                .orElse(""));
    }

    @Test
    void duplicateServiceIdKeepsThreeSegments() {
        assertEquals(
                "SV.Customer.selectSummaryCopy",
                ModelStore.copyServiceId("SV.Customer.selectSummary"));
        assertTrue(ModelStore.copyScreenId("SV-CUS-0001", "SV").matches("^SV-CUS-\\d{4}$"));
        assertTrue(ModelStore.copyTransactionCode("SV-INQ-0001", "SV", "SELECT_ONE")
                .matches("^SV-INQ-\\d{4}$"));
    }
}
