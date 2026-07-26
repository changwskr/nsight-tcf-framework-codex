package com.nh.nsight.tcf.core.support.message.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** TCF 표준 전문 구조(세그먼트) 메타 — StandardRequest/StandardResponse 분할 단위. */
public record TcfMessageStructDefinition(
        String structCode,
        String businessCode,
        String serviceId,
        String transactionCode,
        String messageType,
        String segmentType,
        String structName,
        String description,
        String sourceClass,
        List<TcfMessageFieldDefinition> fields) {

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("structCode", structCode);
        row.put("businessCode", businessCode);
        row.put("serviceId", serviceId);
        row.put("transactionCode", transactionCode);
        row.put("messageType", messageType);
        row.put("segmentType", segmentType);
        row.put("structName", structName);
        row.put("description", description);
        row.put("sourceClass", sourceClass);
        row.put("frameworkTemplate", true);
        row.put("fields", fields.stream().map(TcfMessageFieldDefinition::toMap).toList());
        return row;
    }
}
