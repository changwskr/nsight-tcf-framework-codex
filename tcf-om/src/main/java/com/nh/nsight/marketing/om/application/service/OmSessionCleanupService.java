package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OmSessionCleanupService {
    private static final Logger log = LoggerFactory.getLogger(OmSessionCleanupService.class);

    public static final String JOB_ID = "BAT-OM-002";

    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;
    private final TransactionTemplate transactionTemplate;

    public OmSessionCleanupService(OmOperationDao dao, OmChangeRecorder recorder,
                                   PlatformTransactionManager transactionManager) {
        this.dao = dao;
        this.recorder = recorder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout(10);
    }

    public Optional<OmSessionCleanupResult> runScheduled() {
        Map<String, Object> job = dao.selectBatchJobById(JOB_ID);
        if (!isJobEnabled(job)) {
            return Optional.empty();
        }
        OmSessionCleanupResult result = transactionTemplate.execute(status -> doCleanup(false, null, null));
        if (result.deletedCount() > 0) {
            log.info("Session cleanup scheduled run. expired={}, deleted={}, active={}, elapsedMs={}",
                    result.expiredCount(), result.deletedCount(), result.activeCount(), result.durationMs());
        } else {
            log.debug("Session cleanup scheduled run. expired={}, active={}, elapsedMs={}",
                    result.expiredCount(), result.activeCount(), result.durationMs());
        }
        return Optional.of(result);
    }

    public OmSessionCleanupResult runManual(TransactionContext context, String reason) {
        OmSessionCleanupResult result = transactionTemplate.execute(status -> doCleanup(true, context, reason));
        log.info("Session cleanup manual run. expired={}, deleted={}, active={}, elapsedMs={}",
                result.expiredCount(), result.deletedCount(), result.activeCount(), result.durationMs());
        return result;
    }

    private OmSessionCleanupResult doCleanup(boolean manualRun, TransactionContext context, String reason) {
        long start = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        int expired = dao.countExpiredSpringSessions(now);
        int deleted = expired > 0 ? dao.deleteExpiredSpringSessions(now) : 0;
        int active = dao.countActiveSpringSessions(now);
        long durationMs = System.currentTimeMillis() - start;

        OmSessionCleanupResult result = new OmSessionCleanupResult(expired, deleted, active, durationMs);
        if (manualRun || deleted > 0) {
            recordHistory(result, manualRun, reason);
        }
        if (manualRun && context != null) {
            String auditReason = reason == null ? "세션 정리 배치 수동 실행" : reason;
            recorder.recordAdminAudit(context, "BATCH_EXECUTE", "세션 정리 배치", auditReason, "SUCCESS");
            recorder.recordAuthHistory(context, "BATCH", JOB_ID, "scheduled", "manual-execute", auditReason);
        }
        return result;
    }

    private void recordHistory(OmSessionCleanupResult result, boolean manualRun, String reason) {
        Map<String, Object> history = new HashMap<>();
        history.put("historyId", "BH-" + UUID.randomUUID());
        history.put("jobId", JOB_ID);
        history.put("runTime", DateTimeUtil.nowKst());
        history.put("runStatus", "SUCCESS");
        history.put("durationMs", result.durationMs());
        String trigger = manualRun ? "수동" : "스케줄";
        String message = trigger + " 만료 세션 정리: 만료 " + result.expiredCount()
                + "건, 삭제 " + result.deletedCount() + "건, 활성 " + result.activeCount() + "건";
        if (manualRun && reason != null && !reason.isBlank()) {
            message += " (사유: " + reason + ")";
        }
        history.put("resultMessage", message);
        dao.insertBatchHistory(history);
    }

    private boolean isJobEnabled(Map<String, Object> job) {
        return job != null && "Y".equalsIgnoreCase(String.valueOf(job.get("useYn")));
    }

    public record OmSessionCleanupResult(int expiredCount, int deletedCount, int activeCount, long durationMs) {
    }
}
