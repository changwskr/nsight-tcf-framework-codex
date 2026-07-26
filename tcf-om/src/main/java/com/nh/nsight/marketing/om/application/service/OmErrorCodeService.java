package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
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

@Service
public class OmErrorCodeService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmErrorCodeService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        copyIfPresent(body, criteria, "errorCode", "errorCategory", "useYn", "pageNo", "pageSize");
        rule.normalizePaging(criteria);

        List<Map<String, Object>> rows = dao.searchErrorCodes(criteria);
        int totalCount = dao.countErrorCodes(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "오류코드 / 메시지 관리");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> detail(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "errorCode");

        String errorCode = OmBodySupport.stringValue(body, "errorCode");
        Map<String, Object> row = dao.selectErrorCodeByCode(errorCode);
        if (row == null) {
            throw new BusinessException("E-OM-BIZ-0002", "오류코드를 찾을 수 없습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "오류코드 상세");
        result.put("row", row);
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "errorCode");
        rule.requireField(body, "errorCategory");
        rule.requireReason(body, "changeReason");

        String errorCode = OmBodySupport.stringValue(body, "errorCode");
        if (dao.selectErrorCodeByCode(errorCode) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 오류코드입니다.");
        }

        Map<String, Object> row = toRow(body);
        dao.insertErrorCode(row);
        recorder.recordAuthHistory(context, "ERROR_CODE", errorCode,
                null, String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("오류코드 등록", errorCode, "REGISTER");
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "errorCode");
        rule.requireField(body, "errorCategory");
        rule.requireReason(body, "changeReason");

        String errorCode = OmBodySupport.stringValue(body, "errorCode");
        Map<String, Object> before = dao.selectErrorCodeByCode(errorCode);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "수정할 오류코드를 찾을 수 없습니다.");
        }

        Map<String, Object> row = toRow(body);
        dao.updateErrorCode(row);
        recorder.recordAuthHistory(context, "ERROR_CODE", errorCode,
                String.valueOf(before), String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("오류코드 수정", errorCode, "UPDATE");
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "errorCode");
        rule.requireReason(body, "changeReason");

        String errorCode = OmBodySupport.stringValue(body, "errorCode");
        Map<String, Object> before = dao.selectErrorCodeByCode(errorCode);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 오류코드를 찾을 수 없습니다.");
        }

        Map<String, Object> key = new HashMap<>();
        key.put("errorCode", errorCode);
        int updated = dao.disableErrorCode(key);
        if (updated == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 오류코드를 찾을 수 없습니다.");
        }

        recorder.recordAuthHistory(context, "ERROR_CODE", errorCode,
                String.valueOf(before), "USE_YN=N", OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "오류코드 삭제");
        result.put("deleted", true);
        result.put("errorCode", errorCode);
        return result;
    }

    private Map<String, Object> toRow(Map<String, Object> body) {
        Map<String, Object> row = new HashMap<>();
        row.put("errorCode", OmBodySupport.stringValue(body, "errorCode"));
        row.put("errorCategory", OmBodySupport.stringValue(body, "errorCategory"));
        row.put("userMessage", OmBodySupport.stringValue(body, "userMessage"));
        row.put("operatorMessage", OmBodySupport.stringValue(body, "operatorMessage"));
        row.put("actionGuide", OmBodySupport.stringValue(body, "actionGuide"));
        row.put("notifyTarget", OmBodySupport.stringValue(body, "notifyTarget"));
        row.put("useYn", OmBodySupport.stringValue(body, "useYn") != null ? body.get("useYn") : "Y");
        return row;
    }

    private Map<String, Object> savedResult(String screen, String errorCode, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        result.put("saved", true);
        result.put("mode", mode);
        result.put("errorCode", errorCode);
        return result;
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
