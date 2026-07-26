package com.nh.nsight.tcf.ui.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.tcf.ui.support.BusinessModuleDefinitions;
import com.nh.nsight.tcf.ui.support.BusinessModuleDefinitions.ModuleDefinition;
import com.nh.nsight.tcf.ui.support.BusinessModuleInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BusinessModuleCatalog {
    private final List<BusinessModuleInfo> modules;

    public BusinessModuleCatalog(ObjectMapper objectMapper) {
        this.modules = loadModules(objectMapper);
    }

    public List<BusinessModuleInfo> findAll() {
        return modules;
    }

    public BusinessModuleInfo findByCode(String code) {
        return modules.stream()
                .filter(module -> module.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 업무코드입니다: " + code));
    }

    private List<BusinessModuleInfo> loadModules(ObjectMapper objectMapper) {
        List<BusinessModuleInfo> loaded = new ArrayList<>();
        for (ModuleDefinition definition : BusinessModuleDefinitions.ALL) {
            String code = definition.code();
            String fileName = "sample-requests/" + code.toLowerCase() + "-sample-inquiry.json";
            Map<String, Object> sampleRequest = readSample(objectMapper, fileName);
            @SuppressWarnings("unchecked")
            Map<String, Object> header = (Map<String, Object>) sampleRequest.get("header");
            loaded.add(new BusinessModuleInfo(
                    code,
                    definition.name(),
                    definition.group(),
                    "/" + code.toLowerCase(),
                    definition.localPort(),
                    String.valueOf(header.get("serviceId")),
                    String.valueOf(header.get("transactionCode")),
                    sampleRequest
            ));
        }
        return List.copyOf(loaded);
    }

    private Map<String, Object> readSample(ObjectMapper objectMapper, String fileName) {
        ClassPathResource resource = new ClassPathResource(fileName);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("샘플 전문을 읽을 수 없습니다: " + fileName, e);
        }
    }
}
