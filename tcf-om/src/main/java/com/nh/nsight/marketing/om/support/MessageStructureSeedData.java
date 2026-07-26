package com.nh.nsight.marketing.om.support;

import com.nh.nsight.tcf.core.support.message.catalog.TcfMessageFieldDefinition;
import com.nh.nsight.tcf.core.support.message.catalog.TcfMessageStructDefinition;
import com.nh.nsight.tcf.core.support.message.catalog.TcfStandardMessageCatalog;
import org.springframework.jdbc.core.JdbcTemplate;

/** OM_MESSAGE_STRUCT / OM_MESSAGE_FIELD 샘플 시드 — 프레임워크 구조는 tcf-core catalog 기준. */
final class MessageStructureSeedData {
    private MessageStructureSeedData() {
    }

    static void mergeAll(JdbcTemplate jdbcTemplate) {
        mergeFromCatalog(jdbcTemplate, "MSG-STD-001", TcfStandardMessageCatalog.STRUCT_STD_REQ_HEADER);

        jdbcTemplate.update("""
                MERGE INTO OM_MESSAGE_STRUCT (
                    STRUCT_ID, STRUCT_CODE, BUSINESS_CODE, SERVICE_ID, TRANSACTION_CODE,
                    MESSAGE_TYPE, SEGMENT_TYPE, STRUCT_NAME, DESCRIPTION, SAMPLE_JSON, USE_YN
                ) KEY (STRUCT_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "MSG-SV-001",
                "SV-SAMPLE-REQ-BODY",
                "SV",
                "SV.Sample.inquiry",
                "SV-INQ-0001",
                "REQUEST",
                "BODY",
                "SV 샘플 조회 요청 Body",
                "tcf-ui sample-requests/sv-sample-inquiry.json body 영역",
                "{\"sampleKey\":\"SV-SAMPLE\",\"baseDate\":\"20260614\"}",
                "Y");

        mergeField(jdbcTemplate, "FLD-SV-001", "MSG-SV-001", "sampleKey", "샘플키", "STRING", "Y", 50,
                null, "SV-SAMPLE", null, null, 1);
        mergeField(jdbcTemplate, "FLD-SV-002", "MSG-SV-001", "baseDate", "기준일자", "STRING", "N", 8,
                null, "20260614", "yyyyMMdd", null, 2);
    }

    private static void mergeFromCatalog(JdbcTemplate jdbcTemplate, String structId, String structCode) {
        TcfMessageStructDefinition definition = TcfStandardMessageCatalog.findByStructCode(structCode)
                .orElseThrow(() -> new IllegalStateException("Catalog struct not found: " + structCode));

        jdbcTemplate.update("""
                MERGE INTO OM_MESSAGE_STRUCT (
                    STRUCT_ID, STRUCT_CODE, BUSINESS_CODE, SERVICE_ID, TRANSACTION_CODE,
                    MESSAGE_TYPE, SEGMENT_TYPE, STRUCT_NAME, DESCRIPTION, SAMPLE_JSON, USE_YN
                ) KEY (STRUCT_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                structId,
                definition.structCode(),
                definition.businessCode(),
                definition.serviceId(),
                definition.transactionCode(),
                definition.messageType(),
                definition.segmentType(),
                definition.structName(),
                definition.description(),
                null,
                "Y");

        jdbcTemplate.update("DELETE FROM OM_MESSAGE_FIELD WHERE STRUCT_ID = ?", structId);
        for (TcfMessageFieldDefinition field : definition.fields()) {
            mergeField(
                    jdbcTemplate,
                    "FLD-" + structId + "-" + field.fieldKey(),
                    structId,
                    field.fieldKey(),
                    field.fieldLabel(),
                    field.dataType(),
                    field.requiredYn(),
                    field.maxLength() != null ? field.maxLength() : 0,
                    field.defaultValue(),
                    field.sampleValue(),
                    field.validationRule(),
                    field.description(),
                    field.sortOrder());
        }
    }

    private static void mergeField(
            JdbcTemplate jdbcTemplate,
            String fieldId,
            String structId,
            String fieldKey,
            String fieldLabel,
            String dataType,
            String requiredYn,
            int maxLength,
            String defaultValue,
            String sampleValue,
            String validationRule,
            String description,
            int sortOrder) {
        jdbcTemplate.update("""
                MERGE INTO OM_MESSAGE_FIELD (
                    FIELD_ID, STRUCT_ID, FIELD_KEY, FIELD_LABEL, DATA_TYPE, REQUIRED_YN,
                    MAX_LENGTH, DEFAULT_VALUE, SAMPLE_VALUE, VALIDATION_RULE, DESCRIPTION, SORT_ORDER
                ) KEY (FIELD_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                fieldId, structId, fieldKey, fieldLabel, dataType, requiredYn,
                maxLength, defaultValue, sampleValue, validationRule, description, sortOrder);
    }
}
