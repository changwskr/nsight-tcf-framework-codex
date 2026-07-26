package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.util.GuidGenerator;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import com.nh.nsight.tcf.core.support.message.catalog.TcfStandardMessageCatalog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OmMessageStructureService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmMessageStructureService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        copyIfPresent(body, criteria, "structCode", "businessCode", "serviceId", "messageType", "segmentType", "useYn",
                "pageNo", "pageSize");
        rule.normalizePaging(criteria);

        List<Map<String, Object>> rows = dao.searchMessageStructs(criteria);
        int totalCount = dao.countMessageStructs(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "전문구조 관리");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> detail(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "structId");

        String structId = OmBodySupport.stringValue(body, "structId");
        Map<String, Object> row = dao.selectMessageStructById(structId);
        if (row == null) {
            throw new BusinessException("E-OM-BIZ-0002", "전문구조를 찾을 수 없습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "전문구조 상세");
        result.put("row", row);
        result.put("fields", dao.searchMessageFieldsByStructId(structId));
        return result;
    }

    public Map<String, Object> frameworkInquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "TCF 표준 전문 템플릿");
        result.put("source", "tcf-core:TcfStandardMessageCatalog");
        result.put("templates", TcfStandardMessageCatalog.templatesAsMaps());
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "structCode");
        rule.requireField(body, "messageType");
        rule.requireField(body, "segmentType");
        rule.requireField(body, "structName");
        rule.requireReason(body, "changeReason");

        String structCode = OmBodySupport.stringValue(body, "structCode");
        if (dao.selectMessageStructByCode(structCode) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 전문구조 코드입니다.");
        }

        String structId = GuidGenerator.newGuid();
        Map<String, Object> row = toStructRow(body, structId);
        dao.insertMessageStruct(row);
        replaceFields(structId, extractFields(body));

        recorder.recordAuthHistory(context, "MESSAGE_STRUCT", structId,
                null, String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("전문구조 등록", structId, structCode, "REGISTER");
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "structId");
        rule.requireField(body, "structName");
        rule.requireField(body, "messageType");
        rule.requireField(body, "segmentType");
        rule.requireReason(body, "changeReason");

        String structId = OmBodySupport.stringValue(body, "structId");
        Map<String, Object> before = dao.selectMessageStructById(structId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "수정할 전문구조를 찾을 수 없습니다.");
        }

        Map<String, Object> row = toStructRow(body, structId);
        row.put("structCode", before.get("structCode"));
        dao.updateMessageStruct(row);
        replaceFields(structId, extractFields(body));

        recorder.recordAuthHistory(context, "MESSAGE_STRUCT", structId,
                String.valueOf(before), String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("전문구조 수정", structId, String.valueOf(before.get("structCode")), "UPDATE");
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "structId");
        rule.requireReason(body, "changeReason");

        String structId = OmBodySupport.stringValue(body, "structId");
        Map<String, Object> before = dao.selectMessageStructById(structId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 전문구조를 찾을 수 없습니다.");
        }

        Map<String, Object> key = new HashMap<>();
        key.put("structId", structId);
        int updated = dao.disableMessageStruct(key);
        if (updated == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 전문구조를 찾을 수 없습니다.");
        }

        recorder.recordAuthHistory(context, "MESSAGE_STRUCT", structId,
                String.valueOf(before), "USE_YN=N", OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "전문구조 삭제");
        result.put("deleted", true);
        result.put("structId", structId);
        result.put("structCode", before.get("structCode"));
        return result;
    }

    private void replaceFields(String structId, List<Map<String, Object>> fields) {
        dao.deleteMessageFieldsByStructId(structId);
        if (fields == null || fields.isEmpty()) {
            return;
        }
        int order = 0;
        for (Map<String, Object> field : fields) {
            String fieldKey = OmBodySupport.stringValue(field, "fieldKey");
            if (fieldKey == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("fieldId", GuidGenerator.newGuid());
            row.put("structId", structId);
            row.put("fieldKey", fieldKey);
            row.put("fieldLabel", OmBodySupport.stringValue(field, "fieldLabel"));
            row.put("dataType", defaultString(field, "dataType", "STRING"));
            row.put("requiredYn", defaultString(field, "requiredYn", "N"));
            row.put("maxLength", field.get("maxLength"));
            row.put("defaultValue", OmBodySupport.stringValue(field, "defaultValue"));
            row.put("sampleValue", OmBodySupport.stringValue(field, "sampleValue"));
            row.put("validationRule", OmBodySupport.stringValue(field, "validationRule"));
            row.put("description", OmBodySupport.stringValue(field, "description"));
            row.put("sortOrder", field.containsKey("sortOrder") ? field.get("sortOrder") : ++order);
            dao.insertMessageField(row);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFields(Map<String, Object> body) {
        Object raw = body.get("fields");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> fields = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> field = new LinkedHashMap<>();
                map.forEach((k, v) -> field.put(String.valueOf(k), v));
                fields.add(field);
            }
        }
        return fields;
    }

    private Map<String, Object> toStructRow(Map<String, Object> body, String structId) {
        Map<String, Object> row = new HashMap<>();
        row.put("structId", structId);
        row.put("structCode", OmBodySupport.stringValue(body, "structCode"));
        row.put("businessCode", OmBodySupport.stringValue(body, "businessCode"));
        row.put("serviceId", OmBodySupport.stringValue(body, "serviceId"));
        row.put("transactionCode", OmBodySupport.stringValue(body, "transactionCode"));
        row.put("messageType", OmBodySupport.stringValue(body, "messageType"));
        row.put("segmentType", OmBodySupport.stringValue(body, "segmentType"));
        row.put("structName", OmBodySupport.stringValue(body, "structName"));
        row.put("description", OmBodySupport.stringValue(body, "description"));
        row.put("sampleJson", OmBodySupport.stringValue(body, "sampleJson"));
        row.put("useYn", OmBodySupport.stringValue(body, "useYn") != null ? body.get("useYn") : "Y");
        return row;
    }

    private Map<String, Object> savedResult(String screen, String structId, String structCode, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        result.put("saved", true);
        result.put("mode", mode);
        result.put("structId", structId);
        result.put("structCode", structCode);
        return result;
    }

    private String defaultString(Map<String, Object> body, String key, String defaultValue) {
        String value = OmBodySupport.stringValue(body, key);
        return value != null ? value : defaultValue;
    }

    private void copyIfPresent(Map<String, Object> body, Map<String, Object> criteria, String... keys) {
        if (body == null) {
            return;
        }
        for (String key : keys) {
            if (!body.containsKey(key) || body.get(key) == null) {
                continue;
            }
            if ("pageNo".equals(key) || "pageSize".equals(key)) {
                criteria.put(key, body.get(key));
                continue;
            }
            String value = OmBodySupport.stringValue(body, key);
            if (value != null) {
                criteria.put(key, value);
            }
        }
    }
}
