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
public class OmDeployService {
    private final OmOperationRule operationRule;
    private final OmDeployApprovalRule deployRule;
    private final OmDeployDao dao;
    private final OmChangeRecorder recorder;
    private final OmCicdClientService cicdClient;
    private final OmBuildService buildService;

    public OmDeployService(OmOperationRule operationRule, OmDeployApprovalRule deployRule,
                           OmDeployDao dao, OmChangeRecorder recorder,
                           OmCicdClientService cicdClient, OmBuildService buildService) {
        this.operationRule = operationRule;
        this.deployRule = deployRule;
        this.dao = dao;
        this.recorder = recorder;
        this.cicdClient = cicdClient;
        this.buildService = buildService;
    }

    public Map<String, Object> deployRequest(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        operationRule.requireReason(body, "requestReason");
        deployRule.validateEnvCode(OmBodySupport.stringValue(body, "envCode"));
        String businessCode = OmBodySupport.stringValue(body, "businessCode");
        String moduleName = OmBodySupport.stringValue(body, "moduleName");
        OmCicdClientService.ModuleSpec spec = cicdClient.resolveModule(businessCode, moduleName);

        String requestId = "DRQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = DateTimeUtil.nowKst();
        Map<String, Object> row = new HashMap<>();
        row.put("deployRequestId", requestId);
        row.put("requestType", "DEPLOY");
        row.put("envCode", OmBodySupport.stringValue(body, "envCode").toUpperCase());
        row.put("businessCode", businessCode == null ? spec.contextPath().toUpperCase() : businessCode.toUpperCase());
        row.put("moduleName", spec.moduleName());
        row.put("branchName", OmBodySupport.stringValue(body, "branchName"));
        row.put("commitId", OmBodySupport.stringValue(body, "commitId"));
        row.put("artifactName", spec.deployWarName());
        row.put("status", "REQUESTED");
        row.put("requestUserId", context.getHeader().getUserId());
        row.put("requestTime", now);
        dao.insertDeployRequest(row);

        String reason = OmBodySupport.stringValue(body, "requestReason");
        recorder.recordAdminAudit(context, "DEPLOY_REQUEST", "배포 요청 등록", reason, "REQUESTED");

        Map<String, Object> result = baseResult("배포 요청 등록");
        result.put("deployRequestId", requestId);
        result.put("status", "REQUESTED");
        result.put("moduleName", spec.moduleName());
        return result;
    }

    public Map<String, Object> approve(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        operationRule.requireField(body, "deployRequestId");
        operationRule.requireReason(body, "approveReason");
        Map<String, Object> request = requireRequest(body);
        deployRule.requireStatus(request, "REQUESTED");

        String now = DateTimeUtil.nowKst();
        Map<String, Object> patch = new HashMap<>();
        patch.put("deployRequestId", request.get("deployRequestId"));
        patch.put("status", "APPROVED");
        patch.put("approveUserId", context.getHeader().getUserId());
        patch.put("approveTime", now);
        dao.updateDeployRequest(patch);

        String reason = OmBodySupport.stringValue(body, "approveReason");
        recorder.recordAdminAudit(context, "DEPLOY_APPROVE", "배포 승인", reason, "APPROVED");

        Map<String, Object> result = baseResult("배포 승인");
        result.put("deployRequestId", request.get("deployRequestId"));
        result.put("status", "APPROVED");
        result.put("approveUserId", context.getHeader().getUserId());
        result.put("approveTime", now);
        return result;
    }

    public Map<String, Object> execute(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        operationRule.requireField(body, "deployRequestId");
        operationRule.requireReason(body, "executeReason");
        Map<String, Object> request = requireRequest(body);
        deployRule.requireAnyStatus(request, "APPROVED", "FAIL");

        String requestType = String.valueOf(request.get("requestType"));
        String reason = OmBodySupport.stringValue(body, "executeReason");
        return switch (requestType.toUpperCase()) {
            case "BUILD" -> buildService.executeBuild(request, context, reason);
            case "DEPLOY" -> executeDeploy(request, context, reason);
            case "ROLLBACK" -> executeDeploy(request, context, reason);
            default -> throw new BusinessException("E-OM-BIZ-0003", "지원하지 않는 requestType: " + requestType);
        };
    }

    public Map<String, Object> history(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>(body == null ? Map.of() : body);
        operationRule.normalizePaging(criteria);

        Map<String, Object> result = baseResult("배포 이력");
        result.put("requests", dao.searchDeployRequests(criteria));
        result.put("requestTotalCount", dao.countDeployRequests(criteria));
        result.put("histories", dao.searchDeployHistories(criteria));
        result.put("historyTotalCount", dao.countDeployHistories(criteria));
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("deployStatus", dao.selectDeployStatusByBusinessCode(
                OmBodySupport.stringValue(body, "businessCode")));
        return result;
    }

    public Map<String, Object> logInquiry(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        operationRule.requireField(body, "deployRequestId");
        Map<String, Object> request = requireRequest(body);
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("deployRequestId", request.get("deployRequestId"));
        criteria.put("pageNo", 1);
        criteria.put("pageSize", 20);
        criteria.put("offset", 0);
        List<Map<String, Object>> histories = dao.searchDeployHistories(criteria);

        Map<String, Object> result = baseResult("배포 로그");
        result.put("request", request);
        result.put("histories", histories);
        result.put("logText", request.get("errorMessage"));
        return result;
    }

    public Map<String, Object> healthCheck(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        operationRule.requireField(body, "businessCode");
        OmCicdClientService.ModuleSpec spec = cicdClient.resolveModule(
                OmBodySupport.stringValue(body, "businessCode"), null);
        Map<String, Object> health = cicdClient.healthCheck(spec.contextPath());
        Map<String, Object> result = baseResult("Health Check");
        result.put("businessCode", OmBodySupport.stringValue(body, "businessCode").toUpperCase());
        result.putAll(health);
        result.put("deployStatus", dao.selectDeployStatusByBusinessCode(
                OmBodySupport.stringValue(body, "businessCode").toUpperCase()));
        return result;
    }

    public Map<String, Object> deleteAll(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        operationRule.requireReason(body, "resetReason");
        if (!"DELETE_ALL".equals(OmBodySupport.stringValue(body, "confirmCode"))) {
            throw new BusinessException("E-OM-VAL-0002", "confirmCode=DELETE_ALL 이 필요합니다.");
        }

        int deletedHistoryCount = dao.deleteAllDeployHistories();
        int deletedRequestCount = dao.deleteAllDeployRequests();
        String reason = OmBodySupport.stringValue(body, "resetReason");
        recorder.recordAdminAudit(context, "DEPLOY_DELETE_ALL", "배포 요청·이력 초기화", reason, "SUCCESS");
        recorder.recordAuthHistory(context, "DEPLOY", "OM_DEPLOY", "all",
                "requests:" + deletedRequestCount + ",histories:" + deletedHistoryCount, reason);

        Map<String, Object> result = baseResult("배포 데이터 초기화");
        result.put("deletedRequestCount", deletedRequestCount);
        result.put("deletedHistoryCount", deletedHistoryCount);
        result.put("message", "배포 요청·이력을 초기화했습니다.");
        return result;
    }

    private Map<String, Object> executeDeploy(Map<String, Object> request, TransactionContext context, String reason) {
        String requestId = String.valueOf(request.get("deployRequestId"));
        OmCicdClientService.ModuleSpec spec = cicdClient.resolveModule(
                String.valueOf(request.get("businessCode")),
                String.valueOf(request.get("moduleName")));
        Map<String, Object> before = dao.selectDeployStatusByBusinessCode(String.valueOf(request.get("businessCode")));
        String start = DateTimeUtil.nowKst();
        updateRunning(requestId, start);

        Map<String, Object> deployResult;
        String status;
        String error = null;
        Map<String, Object> health = null;
        try {
            deployResult = cicdClient.deployWar(spec);
            health = cicdClient.healthCheck(spec.contextPath());
            boolean up = "UP".equals(health.get("healthCheckResult"));
            status = up ? "SUCCESS" : "FAIL";
            if (!up) {
                error = "Health check DOWN";
            }
        } catch (BusinessException e) {
            status = "FAIL";
            error = e.getMessage();
            deployResult = Map.of("success", false, "message", e.getMessage());
        }

        String end = DateTimeUtil.nowKst();
        updateFinished(requestId, status, end, error);

        Map<String, Object> history = new HashMap<>();
        history.put("deployHistoryId", "DPH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        history.put("deployRequestId", requestId);
        history.put("envCode", request.get("envCode"));
        history.put("targetServer", "local-ztomcat");
        history.put("contextPath", "/" + spec.contextPath());
        history.put("beforeVersion", before == null ? null : before.get("warVersion"));
        history.put("afterVersion", spec.deployWarName());
        history.put("resultCode", "SUCCESS".equals(status) ? "OK" : "FAIL");
        history.put("resultMessage", "SUCCESS".equals(status) ? "Tomcat 배포 완료" : error);
        history.put("healthCheckUrl", health == null ? null : health.get("healthCheckUrl"));
        history.put("healthCheckResult", health == null ? null : health.get("healthCheckResult"));
        history.put("createdTime", end);
        dao.insertDeployHistory(history);

        recorder.recordAdminAudit(context, "DEPLOY_EXECUTE", "배포 실행", reason, status);

        Map<String, Object> result = baseResult("배포 실행");
        result.put("deployRequestId", requestId);
        result.put("status", status);
        result.put("deployResult", deployResult);
        result.put("healthCheck", health);
        return result;
    }

    private Map<String, Object> requireRequest(Map<String, Object> body) {
        Map<String, Object> request = dao.selectDeployRequestById(OmBodySupport.stringValue(body, "deployRequestId"));
        if (request == null) {
            throw new BusinessException("E-OM-BIZ-0003", "배포 요청을 찾을 수 없습니다.");
        }
        return request;
    }

    private void updateRunning(String requestId, String start) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("deployRequestId", requestId);
        patch.put("status", "RUNNING");
        patch.put("startTime", start);
        dao.updateDeployRequest(patch);
    }

    private void updateFinished(String requestId, String status, String end, String error) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("deployRequestId", requestId);
        patch.put("status", status);
        patch.put("endTime", end);
        if (error != null) {
            patch.put("errorMessage", error);
        }
        dao.updateDeployRequest(patch);
    }

    private Map<String, Object> baseResult(String screen) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        return result;
    }
}
