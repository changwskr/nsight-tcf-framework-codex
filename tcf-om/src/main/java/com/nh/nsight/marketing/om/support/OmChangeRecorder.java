package com.nh.nsight.marketing.om.support;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.util.DateTimeUtil;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OmChangeRecorder {
    private final OmOperationDao dao;

    public OmChangeRecorder(OmOperationDao dao) {
        this.dao = dao;
    }

    public void recordAuthHistory(TransactionContext context, String targetType, String targetId,
                                  String beforeValue, String afterValue, String changeReason) {
        Map<String, Object> row = new HashMap<>();
        row.put("historyId", "AH-" + UUID.randomUUID());
        row.put("changedAt", DateTimeUtil.nowKst());
        row.put("changedBy", context.getHeader().getUserId());
        row.put("targetType", targetType);
        row.put("targetId", targetId);
        row.put("beforeValue", truncate(beforeValue));
        row.put("afterValue", truncate(afterValue));
        row.put("changeReason", changeReason);
        dao.insertAuthHistory(row);
    }

    public void recordAdminAudit(TransactionContext context, String functionId, String functionName,
                                 String reason, String resultStatus) {
        Map<String, Object> row = new HashMap<>();
        row.put("auditId", "AUD-" + UUID.randomUUID());
        row.put("auditTime", DateTimeUtil.nowKst());
        row.put("userId", context.getHeader().getUserId());
        row.put("branchId", context.getHeader().getBranchId());
        row.put("customerNo", null);
        row.put("functionId", functionId);
        row.put("functionName", functionName);
        row.put("inquiryReason", reason);
        row.put("resultStatus", resultStatus);
        row.put("clientIp", context.getHeader().getClientIp());
        dao.insertAuditLog(row);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 950 ? value.substring(0, 950) + "..." : value;
    }
}
