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
public class OmTransactionLogService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmTransactionLogService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>(OmBodySupport.searchCriteria(body));
        rule.normalizePaging(criteria);
        copyIfPresent(body, criteria, "businessCode", "guid", "traceId", "serviceId", "transactionCode",
                "userId", "branchId", "resultStatus", "errorCode", "fromDate", "toDate");

        List<Map<String, Object>> rows = dao.searchTransactionLogs(criteria);
        int totalCount = dao.countTransactionLogs(criteria);
        Map<String, Object> summary = dao.summarizeTransactionLogs(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "거래로그 조회");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("summary", summary);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> deleteAll(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireReason(body, "deleteReason");
        if (!"DELETE_ALL".equals(OmBodySupport.stringValue(body, "confirmCode"))) {
            throw new BusinessException("E-OM-VAL-0002", "confirmCode=DELETE_ALL 이 필요합니다.");
        }

        int deletedCount = dao.deleteAllTransactionLogs();
        String reason = OmBodySupport.stringValue(body, "deleteReason");
        recorder.recordAdminAudit(context, "TX_LOG_DELETE_ALL", "거래로그 전체 삭제", reason, "SUCCESS");
        recorder.recordAuthHistory(context, "TX_LOG", "TCF_TX_LOG", "all", "deleted:" + deletedCount, reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "거래로그 전체 삭제");
        result.put("deletedCount", deletedCount);
        return result;
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


