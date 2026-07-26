package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OmHealthCheckService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;

    public OmHealthCheckService(OmOperationRule rule, OmOperationDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        String checkedAt = DateTimeUtil.nowKst();

        List<Map<String, Object>> apStatus = dao.selectApStatus();
        List<Map<String, Object>> dbStatus = dao.selectDbStatus();
        List<Map<String, Object>> deployStatus = dao.selectDeployStatus();
        String overall = resolveOverall(apStatus, dbStatus, deployStatus);
        long now = System.currentTimeMillis();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "Health Check / 배포 조회");
        result.put("overallStatus", overall);
        result.put("checkedAt", checkedAt);
        result.put("apStatus", apStatus);
        result.put("dbStatus", dbStatus);
        result.put("deployStatus", deployStatus);
        result.put("activeSessionCount", dao.countActiveSpringSessions(now));
        result.put("totalSessionCount", dao.selectActiveSessionCount());
        return result;
    }

    private String resolveOverall(List<Map<String, Object>> ap, List<Map<String, Object>> db,
                                  List<Map<String, Object>> deploy) {
        boolean warn = containsStatus(ap, "WARN") || containsStatus(db, "WARN");
        boolean fail = containsStatus(ap, "FAIL", "DOWN", "ERROR") || containsStatus(db, "FAIL", "DOWN", "ERROR")
                || containsStatus(deploy, "DOWN", "FAIL");
        if (fail) {
            return "CRITICAL";
        }
        if (warn) {
            return "WARN";
        }
        return "NORMAL";
    }

    private boolean containsStatus(List<Map<String, Object>> rows, String... statuses) {
        for (Map<String, Object> row : rows) {
            Object value = row.get("healthStatus");
            if (value == null) {
                continue;
            }
            String status = String.valueOf(value).toUpperCase();
            for (String s : statuses) {
                if (status.contains(s)) {
                    return true;
                }
            }
        }
        return false;
    }
}
