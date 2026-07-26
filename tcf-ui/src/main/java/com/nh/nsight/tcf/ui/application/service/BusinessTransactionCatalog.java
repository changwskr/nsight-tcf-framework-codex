package com.nh.nsight.tcf.ui.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.tcf.ui.support.BusinessModuleDefinitions;
import com.nh.nsight.tcf.ui.support.BusinessModuleDefinitions.ModuleDefinition;
import com.nh.nsight.tcf.ui.support.BusinessModuleTransactions;
import com.nh.nsight.tcf.ui.support.BusinessTransactionInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BusinessTransactionCatalog {
    private final List<BusinessModuleTransactions> modules;
    private final ObjectMapper objectMapper;

    public BusinessTransactionCatalog(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.modules = loadModules();
    }

    public List<BusinessModuleTransactions> findAll() {
        return modules;
    }

    public BusinessModuleTransactions findByCode(String code) {
        return modules.stream()
                .filter(module -> module.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 업무코드입니다: " + code));
    }

    public BusinessTransactionInfo findTransaction(String code, String transactionId) {
        BusinessModuleTransactions module = findByCode(code);
        return module.transactions().stream()
                .filter(tx -> tx.id().equals(transactionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 거래입니다: " + transactionId));
    }

    private List<BusinessModuleTransactions> loadModules() {
        List<BusinessModuleTransactions> loaded = new ArrayList<>();
        for (ModuleDefinition definition : BusinessModuleDefinitions.ALL) {
            String code = definition.code();
            loaded.add(new BusinessModuleTransactions(
                    code,
                    definition.name(),
                    definition.group(),
                    "/" + code.toLowerCase(),
                    definition.localPort(),
                    loadTransactions(code)
            ));
        }
        return List.copyOf(loaded);
    }

    private List<BusinessTransactionInfo> loadTransactions(String code) {
        String manifestFile = "sample-requests/" + code.toLowerCase() + "-transactions.json";
        ClassPathResource manifestResource = new ClassPathResource(manifestFile);
        if (manifestResource.exists()) {
            return loadFromManifest(code, manifestResource);
        }
        return loadDefaultTransactions(code);
    }

    private List<BusinessTransactionInfo> loadFromManifest(String code, ClassPathResource manifestResource) {
        try (InputStream inputStream = manifestResource.getInputStream()) {
            List<Map<String, Object>> entries = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            List<BusinessTransactionInfo> transactions = new ArrayList<>();
            for (Map<String, Object> entry : entries) {
                String label = String.valueOf(entry.get("label"));
                Map<String, Object> sampleRequest;
                if (entry.containsKey("sampleRequest")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> inline = (Map<String, Object>) entry.get("sampleRequest");
                    sampleRequest = inline;
                } else {
                    String sampleFile = String.valueOf(entry.get("sampleFile"));
                    sampleRequest = readSample("sample-requests/" + sampleFile);
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> header = (Map<String, Object>) sampleRequest.get("header");
                String serviceId = String.valueOf(header.get("serviceId"));
                String transactionCode = String.valueOf(header.get("transactionCode"));
                String processingType = String.valueOf(header.get("processingType"));
                transactions.add(new BusinessTransactionInfo(
                        buildId(serviceId, transactionCode),
                        label,
                        serviceId,
                        transactionCode,
                        processingType,
                        sampleRequest
                ));
            }
            return List.copyOf(transactions);
        } catch (IOException e) {
            throw new IllegalStateException("거래 목록을 읽을 수 없습니다: " + code, e);
        }
    }

    private List<BusinessTransactionInfo> loadDefaultTransactions(String code) {
        Map<String, Object> inquiry = readSample("sample-requests/" + code.toLowerCase() + "-sample-inquiry.json");
        List<BusinessTransactionInfo> transactions = new ArrayList<>();
        transactions.add(toTransactionInfo("조회", inquiry));

        Map<String, Object> create = copySample(inquiry);
        patchHeader(create, code + ".Sample.register", code + "-REG-0001", "CREATE");
        transactions.add(toTransactionInfo("등록", create));

        Map<String, Object> update = copySample(inquiry);
        patchHeader(update, code + ".Sample.update", code + "-UPD-0001", "UPDATE");
        transactions.add(toTransactionInfo("수정", update));

        return List.copyOf(transactions);
    }

    private BusinessTransactionInfo toTransactionInfo(String label, Map<String, Object> sampleRequest) {
        @SuppressWarnings("unchecked")
        Map<String, Object> header = (Map<String, Object>) sampleRequest.get("header");
        String serviceId = String.valueOf(header.get("serviceId"));
        String transactionCode = String.valueOf(header.get("transactionCode"));
        String processingType = String.valueOf(header.get("processingType"));
        return new BusinessTransactionInfo(
                buildId(serviceId, transactionCode),
                label,
                serviceId,
                transactionCode,
                processingType,
                sampleRequest
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> copySample(Map<String, Object> source) {
        return objectMapper.convertValue(source, Map.class);
    }

    @SuppressWarnings("unchecked")
    private void patchHeader(Map<String, Object> sampleRequest, String serviceId, String transactionCode, String processingType) {
        Map<String, Object> header = (Map<String, Object>) sampleRequest.get("header");
        header.put("serviceId", serviceId);
        header.put("transactionCode", transactionCode);
        header.put("processingType", processingType);
    }

    private String buildId(String serviceId, String transactionCode) {
        return serviceId + "::" + transactionCode;
    }

    private Map<String, Object> readSample(String fileName) {
        ClassPathResource resource = new ClassPathResource(fileName);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("샘플 전문을 읽을 수 없습니다: " + fileName, e);
        }
    }
}
