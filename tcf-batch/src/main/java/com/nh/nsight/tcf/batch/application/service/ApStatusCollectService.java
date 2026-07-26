package com.nh.nsight.tcf.batch.application.service;

import com.nh.nsight.tcf.batch.client.ApMetricsClient;
import com.nh.nsight.tcf.batch.config.ApStatusBatchProperties;
import com.nh.nsight.tcf.batch.config.ApStatusBatchProperties.ApTargetProperties;
import com.nh.nsight.tcf.batch.support.model.ApStatusCollectResult;
import com.nh.nsight.tcf.batch.support.model.ApStatusSnapshot;
import com.nh.nsight.tcf.batch.persistence.repository.OmDashboardStatusRepository;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApStatusCollectService {
    private static final Logger log = LoggerFactory.getLogger(ApStatusCollectService.class);

    private final ApStatusBatchProperties properties;
    private final ApMetricsClient metricsClient;
    private final OmDashboardStatusRepository repository;

    public ApStatusCollectService(ApStatusBatchProperties properties, ApMetricsClient metricsClient,
                                  OmDashboardStatusRepository repository) {
        this.properties = properties;
        this.metricsClient = metricsClient;
        this.repository = repository;
    }

    public ApStatusCollectResult collectAndPersist() {
        long start = System.currentTimeMillis();
        String runTime = DateTimeUtil.nowKst();
        List<ApTargetProperties> targets = properties.getTargets().stream()
                .filter(ApTargetProperties::isEnabled)
                .toList();

        List<ApStatusSnapshot> snapshots = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (ApTargetProperties target : targets) {
            ApStatusSnapshot snapshot = collectOne(target, runTime);
            snapshots.add(snapshot);
            if (snapshot.reachable()) {
                repository.upsertAp(snapshot);
                successCount++;
            } else {
                repository.upsertAp(snapshot);
                failCount++;
            }
            log.info("AP status collected apId={} health={} cpu={}% heap={}% threads={}",
                    snapshot.apId(), snapshot.healthStatus(), snapshot.cpuUsagePct(),
                    snapshot.heapUsagePct(), snapshot.threadCount());
        }

        pruneStaleTargets(targets);

        long durationMs = System.currentTimeMillis() - start;
        String runStatus = failCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAIL" : "PARTIAL");
        String message = "AP 상태 %d건 수집 (성공 %d, 실패 %d)".formatted(targets.size(), successCount, failCount);

        repository.insertBatchHistory(properties.getJobId(), runTime, runStatus, durationMs, message);

        return new ApStatusCollectResult(
                properties.getJobId(),
                runTime,
                runStatus,
                durationMs,
                targets.size(),
                successCount,
                failCount,
                snapshots,
                message
        );
    }

    private ApStatusSnapshot collectOne(ApTargetProperties target, String checkedAt) {
        try {
            boolean reachable = metricsClient.isReachable(target);
            if (!reachable) {
                return unreachableSnapshot(target, checkedAt, "Actuator health DOWN 또는 연결 실패");
            }

            double cpu = round1(metricsClient.cpuUsagePct(target));
            double heap = round1(metricsClient.heapUsagePct(target));
            int threads = metricsClient.threadCount(target);
            String health = resolveHealthStatus(cpu, heap);

            return new ApStatusSnapshot(
                    target.getApId(),
                    target.getApName(),
                    health,
                    cpu,
                    heap,
                    threads,
                    checkedAt,
                    true,
                    "OK"
            );
        } catch (Exception e) {
            log.warn("AP status collect failed apId={} url={} cause={}",
                    target.getApId(), target.getBaseUrl(), e.getMessage());
            return unreachableSnapshot(target, checkedAt, e.getMessage());
        }
    }

    private ApStatusSnapshot unreachableSnapshot(ApTargetProperties target, String checkedAt, String detail) {
        return new ApStatusSnapshot(
                target.getApId(),
                target.getApName(),
                "FAIL",
                0,
                0,
                0,
                checkedAt,
                false,
                detail
        );
    }

    private String resolveHealthStatus(double cpuPct, double heapPct) {
        if (cpuPct >= 95 || heapPct >= 95) {
            return "FAIL";
        }
        if (cpuPct >= 85 || heapPct >= 85) {
            return "WARN";
        }
        return "NORMAL";
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private void pruneStaleTargets(List<ApTargetProperties> enabledTargets) {
        List<String> keepIds = enabledTargets.stream().map(ApTargetProperties::getApId).toList();
        repository.retainOnlyApIds(keepIds);
    }
}
