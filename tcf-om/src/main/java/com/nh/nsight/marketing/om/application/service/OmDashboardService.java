package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.util.DateTimeUtil;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.client.OmBatchRemoteClient;
import com.nh.nsight.marketing.om.support.OmDashboardSnapshotTx;
import com.nh.nsight.marketing.om.support.OmDashboardSnapshotTx.SnapshotPurgeResult;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OmDashboardService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int RECENT_WINDOW_MINUTES = 15;
    private static final int RECENT_TOP_LIMIT = 10;

    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmDashboardSnapshotTx snapshotTx;
    private final OmBatchRemoteClient batchRemoteClient;
    private final String batchServiceUrl;

    public OmDashboardService(OmOperationRule rule, OmOperationDao dao, OmDashboardSnapshotTx snapshotTx,
                              OmBatchRemoteClient batchRemoteClient,
                              @Value("${nsight.om.batch-service-url:http://127.0.0.1:8098/batch}") String batchServiceUrl) {
        this.rule = rule;
        this.dao = dao;
        this.snapshotTx = snapshotTx;
        this.batchRemoteClient = batchRemoteClient;
        this.batchServiceUrl = batchServiceUrl;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        String baseDate = OmBodySupport.stringValue(body, "baseDate");
        if (baseDate == null) {
            baseDate = DateTimeUtil.todayKst();
            if (baseDate.length() == 8) {
                baseDate = baseDate.substring(0, 4) + "-" + baseDate.substring(4, 6) + "-" + baseDate.substring(6, 8);
            }
        }

        Map<String, Object> txSummary = dao.selectTxSummary(baseDate);
        long total = toLong(txSummary.get("totalCount"));
        long errors = toLong(txSummary.get("errorCount"));
        double errorRate = total == 0 ? 0.0 : (errors * 100.0 / total);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "운영 대시보드");
        result.put("baseDate", baseDate);
        result.put("overallStatus", errors > 0 ? "WARN" : "NORMAL");
        result.put("errorRatePct", Math.round(errorRate * 10) / 10.0);
        result.put("timeoutCount", txSummary.get("timeoutCount"));
        result.put("avgElapsedMs", txSummary.get("avgElapsedMs"));
        List<Map<String, Object>> apStatus = dao.selectApStatus();
        List<Map<String, Object>> dbStatus = dao.selectDbStatus();
        List<Map<String, Object>> deployStatus = dao.selectDeployStatus();
        List<Map<String, Object>> sessionStatus = dao.selectSessionStatus();
        String batchLastCollectedAt = resolveLatestCollectedAt(apStatus, dbStatus, sessionStatus);
        result.put("apStatus", apStatus);
        result.put("dbStatus", dbStatus);
        result.put("deployStatus", deployStatus);
        result.put("sessionStatus", sessionStatus);
        result.put("batchServiceUrl", batchServiceUrl);
        result.put("batchLastCollectedAt", batchLastCollectedAt);
        result.put("batchDataStale", isBatchDataStale(batchLastCollectedAt));
        int activeFromBatch = dao.sumSessionStatusActiveCount();
        result.put("activeSessionCount", sessionStatus.isEmpty()
                ? dao.selectActiveSessionCount() : activeFromBatch);
        result.put("expiredSessionCount", dao.sumSessionStatusExpiredCount());
        result.put("uniqueUserCount", dao.sumSessionStatusUniqueUsers());
        String toTime = DateTimeUtil.nowKst();
        String fromTime = OffsetDateTime.now(KST).minusMinutes(RECENT_WINDOW_MINUTES).toString();
        result.put("recentWindowMinutes", RECENT_WINDOW_MINUTES);
        result.put("errorTop", dao.selectErrorTop(fromTime, toTime, RECENT_TOP_LIMIT));
        result.put("slowTransactionFromTime", fromTime);
        result.put("slowTransactionToTime", toTime);
        result.put("slowTransactionTop", dao.selectSlowTransactionsTop(fromTime, toTime, RECENT_TOP_LIMIT));
        result.put("transactionSummary", txSummary);
        return result;
    }

    public Map<String, Object> reset(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireReason(body, "resetReason");
        if (!"RESET_DASHBOARD".equals(OmBodySupport.stringValue(body, "confirmCode"))) {
            throw new BusinessException("E-OM-VAL-0002", "confirmCode=RESET_DASHBOARD 이 필요합니다.");
        }

        String reason = OmBodySupport.stringValue(body, "resetReason");
        SnapshotPurgeResult purge = snapshotTx.purgeAll();

        List<Map<String, Object>> collectResults = new ArrayList<>();
        collectResults.add(batchRemoteClient.runApStatusCollect());
        collectResults.add(batchRemoteClient.runDbStatusCollect());
        collectResults.add(batchRemoteClient.runSessionStatusCollect());
        collectResults.add(batchRemoteClient.runDeployStatusCollect());

        snapshotTx.recordResetAudit(context, purge, reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "대시보드 DB 초기화");
        result.put("deletedApCount", purge.deletedAp());
        result.put("deletedDbCount", purge.deletedDb());
        result.put("deletedSessionCount", purge.deletedSession());
        result.put("deletedDeployCount", purge.deletedDeploy());
        result.put("collectResults", collectResults);
        result.put("message", "대시보드 스냅샷 테이블을 초기화하고 tcf-batch 수집을 재실행했습니다.");
        return result;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String resolveLatestCollectedAt(List<Map<String, Object>> apStatus,
                                          List<Map<String, Object>> dbStatus,
                                          List<Map<String, Object>> sessionStatus) {
        OffsetDateTime latest = null;
        latest = maxCheckedAt(latest, apStatus, "checkedAt");
        latest = maxCheckedAt(latest, dbStatus, "checkedAt");
        latest = maxCheckedAt(latest, sessionStatus, "checkedAt");
        return latest == null ? null : latest.toString();
    }

    private OffsetDateTime maxCheckedAt(OffsetDateTime latest, List<Map<String, Object>> rows, String field) {
        for (Map<String, Object> row : rows) {
            Object raw = row.get(field);
            if (raw == null) {
                continue;
            }
            String value = String.valueOf(raw);
            if (value.isBlank() || "-".equals(value)) {
                continue;
            }
            try {
                OffsetDateTime parsed = OffsetDateTime.parse(value);
                if (latest == null || parsed.isAfter(latest)) {
                    latest = parsed;
                }
            } catch (Exception ignored) {
                // skip unparsable timestamps
            }
        }
        return latest;
    }

    private boolean isBatchDataStale(String batchLastCollectedAt) {
        if (batchLastCollectedAt == null || batchLastCollectedAt.isBlank()) {
            return true;
        }
        try {
            OffsetDateTime collectedAt = OffsetDateTime.parse(batchLastCollectedAt);
            return collectedAt.isBefore(OffsetDateTime.now(KST).minusMinutes(6));
        } catch (Exception e) {
            return true;
        }
    }
}


