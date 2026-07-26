package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.client.OmCicdClientService;
import com.nh.nsight.marketing.om.persistence.dao.OmDeployDao;
import com.nh.nsight.marketing.om.application.rule.OmDeployApprovalRule;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OmBuildService {
    private final OmOperationRule operationRule;
    private final OmDeployApprovalRule deployRule;
    private final OmDeployDao dao;
    private final OmChangeRecorder recorder;
    private final OmCicdClientService cicdClient;

    public OmBuildService(OmOperationRule operationRule, OmDeployApprovalRule deployRule,
            OmDeployDao dao, OmChangeRecorder recorder, OmCicdClientService cicdClient) {
        this.operationRule = operationRule;
        this.deployRule = deployRule;
        this.dao = dao;
        this.recorder = recorder;
        this.cicdClient = cicdClient;
    }

    public Map<String, Object> buildRequest(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        operationRule.requireReason(body, "requestReason");
        deployRule.validateEnvCode(OmBodySupport.stringValue(body, "envCode"));
        String businessCode = OmBodySupport.stringValue(body, "businessCode");
        String moduleName = OmBodySupport.stringValue(body, "moduleName");
        OmCicdClientService.ModuleSpec spec = cicdClient.resolveModule(businessCode, moduleName);

        String requestId = "DRQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = DateTimeUtil.nowKst();
        String userId = context.getHeader().getUserId();

        Map<String, Object> row = new HashMap<>();
        row.put("deployRequestId", requestId);
        row.put("requestType", "BUILD");
        row.put("envCode", OmBodySupport.stringValue(body, "envCode").toUpperCase());
        row.put("businessCode", businessCode == null ? spec.contextPath().toUpperCase() : businessCode.toUpperCase());
        row.put("moduleName", spec.moduleName());
        row.put("branchName", OmBodySupport.stringValue(body, "branchName"));
        row.put("commitId", OmBodySupport.stringValue(body, "commitId"));
        row.put("gradleTask", OmBodySupport.stringValue(body, "gradleTask"));
        row.put("artifactName", spec.warFileName());
        row.put("status", "REQUESTED");
        row.put("requestUserId", userId);
        row.put("requestTime", now);
        dao.insertDeployRequest(row);

        String reason = OmBodySupport.stringValue(body, "requestReason");
        recorder.recordAdminAudit(context, "DEPLOY_BUILD_REQUEST", "빌드 요청 등록", reason, "REQUESTED");
        recorder.recordAuthHistory(context, "DEPLOY", requestId, "-", "BUILD/REQUESTED", reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "배포관리");
        result.put("deployRequestId", requestId);
        result.put("status", "REQUESTED");
        result.put("moduleName", spec.moduleName());
        result.put("artifactName", spec.warFileName());
        return result;
    }

    public Map<String, Object> buildStatus(Map<String, Object> body, TransactionContext context) {
        operationRule.validateOperation(context);
        operationRule.requireField(body, "deployRequestId");
        Map<String, Object> request = dao.selectDeployRequestById(OmBodySupport.stringValue(body, "deployRequestId"));
        if (request == null) {
            return emptyStatus();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "배포관리");
        result.put("request", request);
        return result;
    }

    Map<String, Object> executeBuild(Map<String, Object> request, TransactionContext context, String reason) {
        String requestId = String.valueOf(request.get("deployRequestId"));
        String moduleName = String.valueOf(request.get("moduleName"));
        String gradleTask = request.get("gradleTask") == null ? null : String.valueOf(request.get("gradleTask"));
        String start = DateTimeUtil.nowKst();
        updateStatus(requestId, "RUNNING", start, null, null);

        Map<String, Object> buildResult = cicdClient.runGradleBuild(moduleName, gradleTask);
        boolean success = Boolean.TRUE.equals(buildResult.get("success"));
        String end = DateTimeUtil.nowKst();
        String status = success ? "SUCCESS" : "FAIL";
        String error = success ? null : String.valueOf(buildResult.get("output"));
        updateStatus(requestId, status, start, end, error);

        insertHistory(request, start, success ? "OK" : "FAIL",
                success ? "Gradle 빌드 성공" : "Gradle 빌드 실패", null, null);

        recorder.recordAdminAudit(context, "DEPLOY_BUILD_EXECUTE", "빌드 실행", reason, status);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "배포관리");
        result.put("deployRequestId", requestId);
        result.put("status", status);
        result.put("buildResult", buildResult);
        return result;
    }

    private void updateStatus(String requestId, String status, String start, String end, String error) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("deployRequestId", requestId);
        patch.put("status", status);
        if (start != null) {
            patch.put("startTime", start);
        }
        if (end != null) {
            patch.put("endTime", end);
        }
        if (error != null) {
            patch.put("errorMessage", error);
        }
        dao.updateDeployRequest(patch);
    }

    private void insertHistory(Map<String, Object> request, String createdTime, String resultCode,
            String resultMessage, String healthUrl, String healthResult) {
        Map<String, Object> history = new HashMap<>();
        history.put("deployHistoryId", "DPH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        history.put("deployRequestId", request.get("deployRequestId"));
        history.put("envCode", request.get("envCode"));
        history.put("targetServer", "local-ztomcat");
        history.put("contextPath", "/" + cicdClient.resolveModule(
                String.valueOf(request.get("businessCode")),
                String.valueOf(request.get("moduleName"))).contextPath());
        history.put("beforeVersion", null);
        history.put("afterVersion", request.get("artifactName"));
        history.put("resultCode", resultCode);
        history.put("resultMessage", resultMessage);
        history.put("healthCheckUrl", healthUrl);
        history.put("healthCheckResult", healthResult);
        history.put("createdTime", createdTime);
        dao.insertDeployHistory(history);
    }

    private Map<String, Object> emptyStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "배포관리");
        result.put("request", null);
        return result;
    }
}
