package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.timeout.TcfServiceTimeoutConstants;
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
public class OmTimeoutPolicyService {
    private static final String[] KEY_FIELDS = {"serviceId", "transactionCode", "businessCode"};

    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmTimeoutPolicyService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        copyIfPresent(body, criteria, "serviceId", "transactionCode", "businessCode", "useYn", "pageNo", "pageSize");
        rule.normalizePaging(criteria);

        List<Map<String, Object>> rows = dao.searchTimeoutPolicies(criteria);
        int totalCount = dao.countTimeoutPolicies(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "Timeout 정책 관리");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        requireKey(body);
        rule.requireReason(body, "changeReason");
        Map<String, Object> row = toRow(body);
        if (dao.selectTimeoutPolicyByKey(row) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 Timeout 정책입니다.");
        }
        dao.insertTimeoutPolicy(row);
        recordChange(context, body, row, null);
        return savedResult("Timeout 정책 등록", row);
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        requireKey(body);
        rule.requireReason(body, "changeReason");
        Map<String, Object> key = keyRow(body);
        Map<String, Object> before = dao.selectTimeoutPolicyByKey(key);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0004", "Timeout 정책을 찾을 수 없습니다.");
        }
        Map<String, Object> row = toRow(body);
        dao.updateTimeoutPolicy(row);
        recordChange(context, body, row, before);
        return savedResult("Timeout 정책 수정", row);
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        requireKey(body);
        rule.requireReason(body, "changeReason");
        Map<String, Object> key = keyRow(body);
        Map<String, Object> before = dao.selectTimeoutPolicyByKey(key);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0004", "Timeout 정책을 찾을 수 없습니다.");
        }
        dao.deleteTimeoutPolicy(key);
        recorder.recordAuthHistory(context, "TIMEOUT_POLICY", keyLabel(key),
                String.valueOf(before), "deleted", OmBodySupport.stringValue(body, "changeReason"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "Timeout 정책 삭제");
        result.put("deleted", true);
        result.put("serviceId", key.get("serviceId"));
        return result;
    }

    private Map<String, Object> savedResult(String screen, Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        result.put("saved", true);
        result.put("serviceId", row.get("serviceId"));
        result.put("transactionCode", row.get("transactionCode"));
        result.put("businessCode", row.get("businessCode"));
        return result;
    }

    private void recordChange(TransactionContext context, Map<String, Object> body,
                              Map<String, Object> after, Map<String, Object> before) {
        recorder.recordAuthHistory(context, "TIMEOUT_POLICY", keyLabel(after),
                before != null ? String.valueOf(before) : null,
                String.valueOf(after), OmBodySupport.stringValue(body, "changeReason"));
    }

    private static String keyLabel(Map<String, Object> row) {
        return row.get("serviceId") + "|" + row.get("transactionCode") + "|" + row.get("businessCode");
    }

    private static Map<String, Object> keyRow(Map<String, Object> body) {
        Map<String, Object> key = new LinkedHashMap<>();
        key.put("serviceId", OmBodySupport.stringValue(body, "serviceId"));
        key.put("transactionCode", OmBodySupport.stringValue(body, "transactionCode"));
        key.put("businessCode", OmBodySupport.stringValue(body, "businessCode"));
        return key;
    }

    private static void requireKey(Map<String, Object> body) {
        for (String field : KEY_FIELDS) {
            if (!StringUtils.hasText(OmBodySupport.stringValue(body, field))) {
                throw new BusinessException("E-OM-VAL-0001", field + " is required");
            }
        }
    }

    private static Map<String, Object> toRow(Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("serviceId", OmBodySupport.stringValue(body, "serviceId"));
        row.put("transactionCode", OmBodySupport.stringValue(body, "transactionCode"));
        row.put("businessCode", OmBodySupport.stringValue(body, "businessCode"));
        row.put("serviceName", defaultString(OmBodySupport.stringValue(body, "serviceName"), "-"));
        row.put("onlineTimeoutSec", intValue(body, "onlineTimeoutSec", TcfServiceTimeoutConstants.DEFAULT_ONLINE_TIMEOUT_SEC));
        row.put("txTimeoutSec", intValue(body, "txTimeoutSec", TcfServiceTimeoutConstants.DEFAULT_TX_TIMEOUT_SEC));
        row.put("dbQueryTimeoutSec", intValue(body, "dbQueryTimeoutSec", TcfServiceTimeoutConstants.DEFAULT_DB_QUERY_TIMEOUT_SEC));
        row.put("externalConnectTimeoutMs", intValue(body, "externalConnectTimeoutMs", TcfServiceTimeoutConstants.DEFAULT_EXTERNAL_CONNECT_TIMEOUT_MS));
        row.put("externalReadTimeoutMs", intValue(body, "externalReadTimeoutMs", TcfServiceTimeoutConstants.DEFAULT_EXTERNAL_READ_TIMEOUT_MS));
        row.put("timeoutAction", defaultString(OmBodySupport.stringValue(body, "timeoutAction"), TcfServiceTimeoutConstants.TIMEOUT_ACTION_FAIL));
        row.put("useYn", defaultString(OmBodySupport.stringValue(body, "useYn"), "Y"));
        row.put("description", OmBodySupport.stringValue(body, "description"));
        return row;
    }

    private static int intValue(Map<String, Object> body, String key, int defaultValue) {
        String raw = OmBodySupport.stringValue(body, key);
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private void copyIfPresent(Map<String, Object> body, Map<String, Object> criteria, String... keys) {
        for (String key : keys) {
            String value = OmBodySupport.stringValue(body, key);
            if (value != null) {
                criteria.put(key, value);
            }
        }
    }
}
