package com.nh.nsight.marketing.oc.capnew.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioTemplateCDTO;
import com.nh.nsight.marketing.oc.capnew.persistence.dao.CapNewScenarioTemplateDao;
import com.nh.nsight.marketing.oc.capnew.persistence.dto.CapNewScenarioTemplateRow;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CapNewScenarioTemplateService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final CapNewScenarioTemplateDao templateDao;
    private final ObjectMapper objectMapper;

    public CapNewScenarioTemplateService(
            CapNewScenarioTemplateDao templateDao,
            ObjectMapper objectMapper) {
        this.templateDao = templateDao;
        this.objectMapper = objectMapper;
    }

    public List<CapNewScenarioTemplateCDTO> listTemplates() {
        return templateDao.findAllEnabled().stream()
                .map(this::toSummary)
                .toList();
    }

    public CapNewScenarioTemplateCDTO getTemplate(String code) {
        return toSummary(requireTemplate(code));
    }

    public Map<String, Object> getSeedPayload(String code) {
        CapNewScenarioTemplateRow row = requireTemplate(code);
        if (!"Y".equalsIgnoreCase(text(row.getEnabled()))) {
            throw new CapNewBizException("비활성화된 시나리오 템플릿입니다: " + code);
        }
        Map<String, Object> seed = readSeedPayload(row.getSeedPayload());
        seed.put("_templateCode", row.getTemplateCode());
        seed.put("_templateLabel", row.getLabel());
        return seed;
    }

    private CapNewScenarioTemplateRow requireTemplate(String code) {
        if (!StringUtils.hasText(code)) {
            throw new CapNewBizException("템플릿 코드가 필요합니다.");
        }
        CapNewScenarioTemplateRow row = templateDao.findByCode(code.trim().toUpperCase(Locale.ROOT));
        if (row == null) {
            throw new CapNewBizException("알 수 없는 시나리오 템플릿입니다: " + code);
        }
        return row;
    }

    private CapNewScenarioTemplateCDTO toSummary(CapNewScenarioTemplateRow row) {
        CapNewScenarioTemplateCDTO dto = new CapNewScenarioTemplateCDTO();
        dto.setCode(row.getTemplateCode());
        dto.setLabel(row.getLabel());
        dto.setDescription(row.getDescription());
        dto.setPurpose(row.getPurpose());
        dto.setTargetEnv(row.getTargetEnv());
        dto.setVmProfileCode(row.getVmProfileCode());
        dto.setTotalUsers(row.getTotalUsers());
        dto.setDesignPeakTps(row.getDesignPeakTps());
        dto.setDeploymentAp(row.getDeploymentAp());
        dto.setMaxThreads(row.getMaxThreads());
        dto.setPoolPerVm(row.getPoolPerVm());
        return dto;
    }

    private Map<String, Object> readSeedPayload(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new CapNewBizException("템플릿 seed payload JSON을 읽을 수 없습니다.");
        }
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
