package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.util.DateTimeUtil;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.client.OmBatchRemoteClient;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OmBatchService {
    public static final String AP_STATUS_JOB_ID = "BAT-BATCH-001";
    public static final String DB_STATUS_JOB_ID = "BAT-BATCH-002";
    public static final String SESSION_STATUS_JOB_ID = "BAT-BATCH-003";
    public static final String DEPLOY_STATUS_JOB_ID = "BAT-BATCH-004";

    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;
    private final OmSessionCleanupService sessionCleanupService;
    private final OmBatchRemoteClient batchRemoteClient;

    public OmBatchService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder,
                          OmSessionCleanupService sessionCleanupService,
                          OmBatchRemoteClient batchRemoteClient) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
        this.sessionCleanupService = sessionCleanupService;
        this.batchRemoteClient = batchRemoteClient;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> jobCriteria = new HashMap<>();
        putIfPresent(body, jobCriteria, "businessCode", "useYn");

        Map<String, Object> historyCriteria = new HashMap<>(body == null ? Map.of() : body);
        rule.normalizePaging(historyCriteria);
        putIfPresent(body, historyCriteria, "jobId", "runStatus");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "배치 / 스케줄 관리");
        result.put("jobs", dao.searchBatchJobs(jobCriteria));
        result.put("histories", dao.searchBatchHistories(historyCriteria));
        result.put("pageNo", historyCriteria.get("pageNo"));
        result.put("pageSize", historyCriteria.get("pageSize"));
        result.put("totalCount", dao.countBatchHistories(historyCriteria));
        return result;
    }

    public Map<String, Object> execute(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "jobId");
        rule.requireReason(body, "executeReason");

        String jobId = OmBodySupport.stringValue(body, "jobId");
        Map<String, Object> job = dao.selectBatchJobById(jobId);
        if (job == null || !"Y".equalsIgnoreCase(String.valueOf(job.get("useYn")))) {
            throw new BusinessException("E-OM-BIZ-0003", "실행 가능한 배치 Job이 없습니다: " + jobId);
        }

        String reason = OmBodySupport.stringValue(body, "executeReason");
        if (OmSessionCleanupService.JOB_ID.equals(jobId)) {
            OmSessionCleanupService.OmSessionCleanupResult cleanup = sessionCleanupService.runManual(context, reason);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("businessCode", "OM");
            result.put("screen", "배치 재실행");
            result.put("executed", true);
            result.put("jobId", jobId);
            result.put("runStatus", "SUCCESS");
            result.put("expiredCount", cleanup.expiredCount());
            result.put("deletedCount", cleanup.deletedCount());
            result.put("activeCount", cleanup.activeCount());
            result.put("durationMs", cleanup.durationMs());
            return result;
        }
        if (AP_STATUS_JOB_ID.equals(jobId)) {
            Map<String, Object> remote = batchRemoteClient.runApStatusCollect();
            recorder.recordAdminAudit(context, "BATCH_EXECUTE", "배치 재실행", reason,
                    String.valueOf(remote.getOrDefault("runStatus", "SUCCESS")));
            recorder.recordAuthHistory(context, "BATCH", jobId, "scheduled", "manual-execute", reason);
            return batchRemoteClient.toExecuteResult(jobId, remote);
        }
        if (DB_STATUS_JOB_ID.equals(jobId)) {
            Map<String, Object> remote = batchRemoteClient.runDbStatusCollect();
            recorder.recordAdminAudit(context, "BATCH_EXECUTE", "배치 재실행", reason,
                    String.valueOf(remote.getOrDefault("runStatus", "SUCCESS")));
            recorder.recordAuthHistory(context, "BATCH", jobId, "scheduled", "manual-execute", reason);
            return batchRemoteClient.toExecuteResult(jobId, remote);
        }
        if (SESSION_STATUS_JOB_ID.equals(jobId)) {
            Map<String, Object> remote = batchRemoteClient.runSessionStatusCollect();
            recorder.recordAdminAudit(context, "BATCH_EXECUTE", "배치 재실행", reason,
                    String.valueOf(remote.getOrDefault("runStatus", "SUCCESS")));
            recorder.recordAuthHistory(context, "BATCH", jobId, "scheduled", "manual-execute", reason);
            return batchRemoteClient.toExecuteResult(jobId, remote);
        }
        if (DEPLOY_STATUS_JOB_ID.equals(jobId)) {
            Map<String, Object> remote = batchRemoteClient.runDeployStatusCollect();
            recorder.recordAdminAudit(context, "BATCH_EXECUTE", "배치 재실행", reason,
                    String.valueOf(remote.getOrDefault("runStatus", "SUCCESS")));
            recorder.recordAuthHistory(context, "BATCH", jobId, "scheduled", "manual-execute", reason);
            return batchRemoteClient.toExecuteResult(jobId, remote);
        }

        long start = System.currentTimeMillis();
        String runStatus = "SUCCESS";
        String message = "수동 재실행 완료: " + job.get("jobName");

        Map<String, Object> history = new HashMap<>();
        history.put("historyId", "BH-" + UUID.randomUUID());
        history.put("jobId", jobId);
        history.put("runTime", DateTimeUtil.nowKst());
        history.put("runStatus", runStatus);
        history.put("durationMs", System.currentTimeMillis() - start + 120);
        history.put("resultMessage", message + " (사유: " + reason + ")");
        dao.insertBatchHistory(history);

        recorder.recordAdminAudit(context, "BATCH_EXECUTE", "배치 재실행", reason, runStatus);
        recorder.recordAuthHistory(context, "BATCH", jobId, "scheduled", "manual-execute", reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "배치 재실행");
        result.put("executed", true);
        result.put("jobId", jobId);
        result.put("historyId", history.get("historyId"));
        result.put("runStatus", runStatus);
        return result;
    }

    public Map<String, Object> deleteAllHistories(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireReason(body, "deleteReason");
        if (!"DELETE_ALL".equals(OmBodySupport.stringValue(body, "confirmCode"))) {
            throw new BusinessException("E-OM-VAL-0002", "confirmCode=DELETE_ALL 이 필요합니다.");
        }

        int deletedCount = dao.deleteAllBatchHistories();
        String reason = OmBodySupport.stringValue(body, "deleteReason");
        recorder.recordAdminAudit(context, "BATCH_HISTORY_DELETE_ALL", "배치 실행이력 전체 삭제", reason, "SUCCESS");
        recorder.recordAuthHistory(context, "BATCH", "OM_BATCH_HISTORY", "all", "deleted:" + deletedCount, reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "배치 실행이력 전체 삭제");
        result.put("deletedCount", deletedCount);
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


