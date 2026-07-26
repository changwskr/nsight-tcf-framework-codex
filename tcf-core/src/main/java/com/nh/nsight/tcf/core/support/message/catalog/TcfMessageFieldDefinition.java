package com.nh.nsight.tcf.core.support.message.catalog;

import java.util.LinkedHashMap;
import java.util.Map;

/** TCF 표준 전문 필드 메타 — OM 전문구조·문서·검증의 공통 정의 단위. */
public record TcfMessageFieldDefinition(
        String fieldKey,
        String fieldLabel,
        String dataType,
        String requiredYn,
        Integer maxLength,
        String defaultValue,
        String sampleValue,
        String validationRule,
        String description,
        int sortOrder) {

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("fieldKey", fieldKey);
        row.put("fieldLabel", fieldLabel);
        row.put("dataType", dataType);
        row.put("requiredYn", requiredYn);
        row.put("maxLength", maxLength);
        row.put("defaultValue", defaultValue);
        row.put("sampleValue", sampleValue);
        row.put("validationRule", validationRule);
        row.put("description", description);
        row.put("sortOrder", sortOrder);
        return row;
    }
}
