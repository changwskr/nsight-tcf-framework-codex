package com.nh.nsight.tcf.batch.application.service;

import com.nh.nsight.tcf.batch.client.SessionMetricsClient;
import com.nh.nsight.tcf.batch.client.SessionMetricsClient.SessionCounts;
import com.nh.nsight.tcf.batch.config.SessionStatusBatchProperties;
import com.nh.nsight.tcf.batch.config.SessionStatusBatchProperties.SessionTargetProperties;
import com.nh.nsight.tcf.batch.support.model.SessionStatusCollectResult;
import com.nh.nsight.tcf.batch.support.model.SessionStatusSnapshot;
import com.nh.nsight.tcf.batch.persistence.repository.OmDashboardStatusRepository;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SessionStatusCollectService {
    private static final Logger log = LoggerFactory.getLogger(SessionStatusCollectService.class);

    private final SessionStatusBatchProperties properties;
    private final SessionMetricsClient metricsClient;
    private final OmDashboardStatusRepository repository;

    public SessionStatusCollectService(SessionStatusBatchProperties properties, SessionMetricsClient metricsClient,
                                       OmDashboardStatusRepository repository) {
        this.properties = properties;
        this.metricsClient = metricsClient;
        this.repository = repository;
    }

    public SessionStatusCollectResult collectAndPersist() {
        long start = System.currentTimeMillis();
        String runTime = DateTimeUtil.nowKst();
        long now = System.currentTimeMillis();
        List<SessionTargetProperties> targets = properties.getTargets().stream()
                .filter(SessionTargetProperties::isEnabled)
                .toList();

        List<SessionStatusSnapshot> snapshots = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        int totalActive = 0;
        int totalExpired = 0;
        int totalUniqueUsers = 0;

        for (SessionTargetProperties target : targets) {
            SessionStatusSnapshot snapshot = collectOne(target, runTime, now);
            snapshots.add(snapshot);
            if (snapshot.reachable()) {
                repository.upsertSession(snapshot);
                successCount++;
                totalActive += snapshot.activeCount();
                totalExpired += snapshot.expiredCount();
                totalUniqueUsers += snapshot.uniqueUserCount();
            } else {
                repository.upsertSession(snapshot);
                failCount++;
            }
            log.info("Session status collected scopeId={} active={} expired={} users={}",
                    snapshot.scopeId(), snapshot.activeCount(), snapshot.expiredCount(), snapshot.uniqueUserCount());
        }

        pruneStaleTargets(targets);

        long durationMs = System.currentTimeMillis() - start;
        String runStatus = failCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAIL" : "PARTIAL");
        String message = "세션 현황 %d건 수집 (활성 %d, 만료 %d, 사용자 %d)".formatted(
                targets.size(), totalActive, totalExpired, totalUniqueUsers);

        repository.insertBatchHistory(properties.getJobId(), runTime, runStatus, durationMs, message);

        return new SessionStatusCollectResult(
                properties.getJobId(),
                runTime,
                runStatus,
                durationMs,
                targets.size(),
                successCount,
                failCount,
                totalActive,
                totalExpired,
                totalUniqueUsers,
                snapshots,
                message
        );
    }

    private SessionStatusSnapshot collectOne(SessionTargetProperties target, String checkedAt, long now) {
        try {
            SessionCounts counts;
            if ("actuator".equalsIgnoreCase(target.getSourceType())) {
                if (!StringUtils.hasText(target.getBaseUrl())) {
                    return failedSnapshot(target, checkedAt, "baseUrl이 설정되지 않았습니다.");
                }
                counts = metricsClient.collectActuatorSessionCounts(target);
            } else {
                counts = metricsClient.collectSpringSessionCounts(now);
            }
            String health = metricsClient.resolveHealthStatus(counts, properties.getWarnActiveThreshold());
            return new SessionStatusSnapshot(
                    target.getScopeId(),
                    target.getScopeName(),
                    counts.activeCount(),
                    counts.expiredCount(),
                    counts.totalCount(),
                    counts.uniqueUserCount(),
                    health,
                    checkedAt,
                    counts.reachable(),
                    counts.reachable() ? "OK" : "수집 실패"
            );
        } catch (Exception e) {
            log.warn("Session status collect failed scopeId={} cause={}", target.getScopeId(), e.getMessage());
            return failedSnapshot(target, checkedAt, e.getMessage());
        }
    }

    private SessionStatusSnapshot failedSnapshot(SessionTargetProperties target, String checkedAt, String detail) {
        return new SessionStatusSnapshot(
                target.getScopeId(),
                target.getScopeName(),
                0,
                0,
                0,
                0,
                "FAIL",
                checkedAt,
                false,
                detail
        );
    }

    private void pruneStaleTargets(List<SessionTargetProperties> enabledTargets) {
        List<String> keepIds = enabledTargets.stream().map(SessionTargetProperties::getScopeId).toList();
        repository.retainOnlySessionScopeIds(keepIds);
    }
}
