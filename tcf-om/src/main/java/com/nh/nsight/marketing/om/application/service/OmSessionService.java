package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmAuthSessionSupport;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OmSessionService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmAuthSessionSupport sessionSupport;
    private final OmChangeRecorder recorder;

    public OmSessionService(OmOperationRule rule, OmOperationDao dao, OmAuthSessionSupport sessionSupport,
                            OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.sessionSupport = sessionSupport;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);

        Map<String, Object> criteria = new HashMap<>();
        putIfPresent(body, criteria, "userId");
        String activeOnly = OmBodySupport.stringValue(body, "activeOnly");
        criteria.put("activeOnly", "Y".equalsIgnoreCase(activeOnly));
        long now = System.currentTimeMillis();
        criteria.put("now", now);
        rule.normalizePaging(criteria);

        List<Map<String, Object>> rows = dao.searchSpringSessions(criteria);
        int totalCount = dao.countSpringSessions(criteria);
        int activeCount = dao.countActiveSpringSessions(now);

        Map<String, Object> currentSession = new LinkedHashMap<>(
                sessionSupport.toSessionBody(sessionSupport.currentUser().orElse(null),
                        sessionSupport.currentUser().isPresent()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "세션 관리");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("activeCount", activeCount);
        result.put("currentSession", currentSession);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "sessionId");
        rule.requireReason(body, "deleteReason");

        String sessionId = OmBodySupport.stringValue(body, "sessionId");
        Map<String, Object> target = dao.selectSpringSessionById(sessionId);
        if (target == null) {
            throw new BusinessException("E-OM-BIZ-0002", "종료할 세션을 찾을 수 없습니다.");
        }

        int deleted = dao.deleteSpringSession(sessionId);
        if (deleted == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "종료할 세션을 찾을 수 없습니다.");
        }

        sessionSupport.currentSessionId()
                .filter(sessionId::equals)
                .ifPresent(id -> sessionSupport.invalidateSession());

        String reason = OmBodySupport.stringValue(body, "deleteReason");
        recorder.recordAdminAudit(context, "SESSION_DELETE", "세션 강제 종료", reason, "SUCCESS");
        recorder.recordAuthHistory(context, "SESSION", sessionId,
                String.valueOf(target), "deleted", reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "세션 강제 종료");
        result.put("deleted", true);
        result.put("sessionId", sessionId);
        result.put("userId", target.get("userId"));
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
