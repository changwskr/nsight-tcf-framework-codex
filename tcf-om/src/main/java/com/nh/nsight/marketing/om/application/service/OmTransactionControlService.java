package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.control.TcfTransactionControlConstants;
import com.nh.nsight.tcf.core.support.control.TransactionControlRowSupport;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OmTransactionControlService {
    private static final String[] KEY_FIELDS = {
            "serviceId", "transactionCode", "businessCode", "serviceName", "userId", "channelId", "branchId"
    };

    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmTransactionControlService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        copyIfPresent(body, criteria, "controlType", "blockYn", "pageNo", "pageSize");
        if (body != null && StringUtils.hasText(OmBodySupport.stringValue(body, "targetValue"))) {
            criteria.put("targetKeyword", OmBodySupport.stringValue(body, "targetValue"));
        }
        rule.normalizePaging(criteria);

        List<Map<String, Object>> rows = dao.searchTransactionControls(criteria);
        rows.forEach(this::enrichRow);
        int totalCount = dao.countTransactionControls(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "거래통제 관리");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "controlType");
        rule.requireField(body, "blockYn");
        rule.requireReason(body, "changeReason");

        Map<String, Object> row = toRow(body);
        if (dao.selectTransactionControlByKey(row) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 거래통제 규칙입니다.");
        }

        dao.insertTransactionControl(row);
        recorder.recordAuthHistory(context, "TRANSACTION_CONTROL", keyLabel(row),
                null, String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "거래통제 등록");
        result.put("saved", true);
        result.put("controlType", row.get("controlType"));
        result.put("targetValue", TransactionControlRowSupport.extractTarget(
                String.valueOf(row.get("controlType")), row));
        return result;
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        requireStorageKeys(body);
        rule.requireField(body, "controlType");
        rule.requireField(body, "blockYn");
        rule.requireReason(body, "changeReason");

        Map<String, Object> keyRow = keyRow(body);
        Map<String, Object> before = dao.selectTransactionControlByKey(keyRow);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "수정할 거래통제 규칙을 찾을 수 없습니다.");
        }

        Map<String, Object> row = new HashMap<>(keyRow);
        row.put("controlType", OmBodySupport.stringValue(body, "controlType"));
        row.put("blockYn", OmBodySupport.stringValue(body, "blockYn").toUpperCase());
        dao.updateTransactionControl(row);
        recorder.recordAuthHistory(context, "TRANSACTION_CONTROL", keyLabel(row),
                String.valueOf(before), String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "거래통제 수정");
        result.put("updated", true);
        return result;
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        requireStorageKeys(body);
        rule.requireReason(body, "changeReason");

        Map<String, Object> keyRow = keyRow(body);
        Map<String, Object> before = dao.selectTransactionControlByKey(keyRow);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 거래통제 규칙을 찾을 수 없습니다.");
        }

        int deleted = dao.deleteTransactionControl(keyRow);
        if (deleted == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 거래통제 규칙을 찾을 수 없습니다.");
        }

        recorder.recordAuthHistory(context, "TRANSACTION_CONTROL", keyLabel(keyRow),
                String.valueOf(before), "deleted", OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "거래통제 삭제");
        result.put("deleted", true);
        return result;
    }

    private Map<String, Object> toRow(Map<String, Object> body) {
        String controlType = OmBodySupport.stringValue(body, "controlType");
        String targetValue = OmBodySupport.stringValue(body, "targetValue");
        if (!TcfTransactionControlConstants.CONTROL_TYPE_GLOBAL.equalsIgnoreCase(controlType)) {
            rule.requireField(body, "targetValue");
        }
        String blockYn = OmBodySupport.stringValue(body, "blockYn");
        Map<String, String> storage = TransactionControlRowSupport.toStorageRow(
                controlType,
                TcfTransactionControlConstants.CONTROL_TYPE_GLOBAL.equalsIgnoreCase(controlType)
                        ? TcfTransactionControlConstants.GLOBAL_WILDCARD
                        : targetValue,
                blockYn);
        return new HashMap<>(storage);
    }

    private Map<String, Object> keyRow(Map<String, Object> body) {
        Map<String, Object> key = new HashMap<>();
        for (String field : KEY_FIELDS) {
            key.put(field, OmBodySupport.stringValue(body, field));
        }
        return key;
    }

    private void requireStorageKeys(Map<String, Object> body) {
        for (String field : KEY_FIELDS) {
            rule.requireField(body, field);
        }
    }

    private void enrichRow(Map<String, Object> row) {
        String controlType = OmBodySupport.stringValue(row, "controlType");
        row.put("targetValue", TransactionControlRowSupport.extractTarget(controlType, row));
    }

    private String keyLabel(Map<String, Object> row) {
        return row.get("controlType") + "|" + TransactionControlRowSupport.extractTarget(
                String.valueOf(row.get("controlType")), row);
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
