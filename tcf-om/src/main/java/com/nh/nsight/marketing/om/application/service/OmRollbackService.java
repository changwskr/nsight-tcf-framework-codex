package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.marketing.om.client.OmCicdClientService;
import com.nh.nsight.marketing.om.persistence.dao.OmDeployDao;
import com.nh.nsight.marketing.om.application.rule.OmDeployApprovalRule;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OmRollbackService {
    private final OmOperationRule operationRule;
    private final OmDeployApprovalRule deployRule;
    private final OmDeployDao dao;
    private final OmChangeRecorder recorder;
    private final OmCicdClientService cicdClient;

    public OmRollbackService(OmOperationRule operationRule, OmDeployApprovalRule deployRule,
                             OmDeployDao dao, OmChangeRecorder recorder, OmCicdClientService cicdClient) {
        this.operationRule = operationRule;
        this.deployRule = deployRule;
        this.dao = dao;
        this.recorder = recorder;
        this.cicdClient = cicdClient;
    }

    public Map<String, Object> rollbackRequest(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        operationRule.requireField(body, "deployRequestId");
        operationRule.requireReason(body, "requestReason");
        Map<String, Object> source = dao.selectDeployRequestById(OmBodySupport.stringValue(body, "deployRequestId"));
        if (source == null) {
            throw new BusinessException("E-OM-BIZ-0003", "원본 배포 요청을 찾을 수 없습니다.");
        }
        deployRule.validateEnvCode(OmBodySupport.stringValue(body, "envCode") == null
                ? String.valueOf(source.get("envCode")) : OmBodySupport.stringValue(body, "envCode"));

        String requestId = "DRQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = DateTimeUtil.nowKst();
        Map<String, Object> row = new HashMap<>();
        row.put("deployRequestId", requestId);
        row.put("requestType", "ROLLBACK");
        row.put("envCode", OmBodySupport.stringValue(body, "envCode") == null
                ? source.get("envCode") : OmBodySupport.stringValue(body, "envCode").toUpperCase());
        row.put("businessCode", source.get("businessCode"));
        row.put("moduleName", source.get("moduleName"));
        row.put("artifactName", source.get("artifactName"));
        row.put("status", "REQUESTED");
        row.put("requestUserId", context.getHeader().getUserId());
        row.put("requestTime", now);
        dao.insertDeployRequest(row);

        String reason = OmBodySupport.stringValue(body, "requestReason");
        recorder.recordAdminAudit(context, "DEPLOY_ROLLBACK_REQUEST", "롤백 요청 등록", reason, "REQUESTED");
        recorder.recordAuthHistory(context, "DEPLOY", requestId,
                String.valueOf(source.get("deployRequestId")), "ROLLBACK/REQUESTED", reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "배포관리");
        result.put("deployRequestId", requestId);
        result.put("sourceDeployRequestId", source.get("deployRequestId"));
        result.put("status", "REQUESTED");
        result.put("moduleName", source.get("moduleName"));
        return result;
    }
}
