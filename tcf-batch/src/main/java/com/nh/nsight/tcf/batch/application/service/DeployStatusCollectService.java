package com.nh.nsight.tcf.batch.application.service;

import com.nh.nsight.tcf.batch.client.DeployMetricsClient;
import com.nh.nsight.tcf.batch.client.DeployMetricsClient.DeployProbe;
import com.nh.nsight.tcf.batch.config.DeployStatusBatchProperties;
import com.nh.nsight.tcf.batch.config.DeployStatusBatchProperties.DeployTargetProperties;
import com.nh.nsight.tcf.batch.support.model.DeployStatusCollectResult;
import com.nh.nsight.tcf.batch.support.model.DeployStatusSnapshot;
import com.nh.nsight.tcf.batch.persistence.repository.OmDashboardStatusRepository;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeployStatusCollectService {
    private static final Logger log = LoggerFactory.getLogger(DeployStatusCollectService.class);

    private final DeployStatusBatchProperties properties;
    private final DeployMetricsClient metricsClient;
    private final OmDashboardStatusRepository repository;

    public DeployStatusCollectService(DeployStatusBatchProperties properties, DeployMetricsClient metricsClient,
                                      OmDashboardStatusRepository repository) {
        this.properties = properties;
        this.metricsClient = metricsClient;
        this.repository = repository;
    }

    public DeployStatusCollectResult collectAndPersist() {
        long start = System.currentTimeMillis();
        String runTime = DateTimeUtil.nowKst();
        List<DeployTargetProperties> targets = properties.getTargets().stream()
                .filter(DeployTargetProperties::isEnabled)
                .toList();

        List<DeployStatusSnapshot> snapshots = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (DeployTargetProperties target : targets) {
            DeployStatusSnapshot snapshot = collectOne(target, runTime);
            snapshots.add(snapshot);
            if (snapshot.reachable()) {
                repository.upsertDeploy(snapshot);
                successCount++;
            } else {
                repository.upsertDeploy(snapshot);
                failCount++;
            }
            log.info("Deploy status collected businessCode={} health={} version={} deployedAt={}",
                    snapshot.businessCode(), snapshot.healthStatus(), snapshot.warVersion(), snapshot.deployedAt());
        }

        pruneStaleTargets(targets);

        long durationMs = System.currentTimeMillis() - start;
        String runStatus = failCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAIL" : "PARTIAL");
        String message = "배포 현황 %d건 수집 (기동 %d, 미기동 %d)".formatted(
                targets.size(), successCount, failCount);

        repository.insertBatchHistory(properties.getJobId(), runTime, runStatus, durationMs, message);

        return new DeployStatusCollectResult(
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

    private DeployStatusSnapshot collectOne(DeployTargetProperties target, String checkedAt) {
        try {
            DeployProbe probe = metricsClient.collect(target, properties.getDefaultVersion());
            String deployedAt = StringUtils.hasText(probe.deployedAt()) ? probe.deployedAt() : "-";
            String version = StringUtils.hasText(probe.warVersion())
                    ? probe.warVersion() : properties.getDefaultVersion();
            return new DeployStatusSnapshot(
                    target.getBusinessCode(),
                    target.getWarName(),
                    version,
                    deployedAt,
                    probe.healthStatus(),
                    checkedAt,
                    probe.reachable(),
                    probe.detailMessage()
            );
        } catch (Exception e) {
            log.warn("Deploy status collect failed businessCode={} cause={}",
                    target.getBusinessCode(), e.getMessage());
            return failedSnapshot(target, checkedAt, e.getMessage());
        }
    }

    private DeployStatusSnapshot failedSnapshot(DeployTargetProperties target, String checkedAt, String detail) {
        return new DeployStatusSnapshot(
                target.getBusinessCode(),
                target.getWarName(),
                properties.getDefaultVersion(),
                "-",
                "DOWN",
                checkedAt,
                false,
                detail
        );
    }

    private void pruneStaleTargets(List<DeployTargetProperties> enabledTargets) {
        List<String> keepCodes = enabledTargets.stream().map(DeployTargetProperties::getBusinessCode).toList();
        repository.retainOnlyDeployBusinessCodes(keepCodes);
    }
}
