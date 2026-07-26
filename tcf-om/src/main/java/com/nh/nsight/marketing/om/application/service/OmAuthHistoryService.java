package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OmAuthHistoryService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmAuthHistoryService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>(OmBodySupport.searchCriteria(body));
        rule.normalizePaging(criteria);
        putIfPresent(body, criteria, "targetType", "changedBy");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "권한이력 조회");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", dao.countAuthHistories(criteria));
        result.put("rows", dao.searchAuthHistories(criteria));
        return result;
    }

    public Map<String, Object> deleteAll(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireReason(body, "deleteReason");
        if (!"DELETE_ALL".equals(OmBodySupport.stringValue(body, "confirmCode"))) {
            throw new BusinessException("E-OM-VAL-0002", "confirmCode=DELETE_ALL 이 필요합니다.");
        }

        int deletedCount = dao.deleteAllAuthHistories();
        String reason = OmBodySupport.stringValue(body, "deleteReason");
        recorder.recordAdminAudit(context, "AUTH_HISTORY_DELETE_ALL", "권한이력 전체 삭제", reason, "SUCCESS");
        recorder.recordAuthHistory(context, "AUTH_HISTORY", "OM_AUTH_HISTORY", "all", "deleted:" + deletedCount, reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "권한이력 전체 삭제");
        result.put("deletedCount", deletedCount);
        result.put("message", "권한이력을 초기화했습니다.");
        return result;
    }

    private void putIfPresent(Map<String, Object> body, Map<String, Object> criteria, String... keys) {
        for (String key : keys) {
            String value = OmBodySupport.stringValue(body, key);
            if (value != null) {
                criteria.put(key, value);
            }
        }
    }
}


