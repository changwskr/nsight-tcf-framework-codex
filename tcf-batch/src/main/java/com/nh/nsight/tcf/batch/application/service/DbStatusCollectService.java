package com.nh.nsight.tcf.batch.application.service;

import com.nh.nsight.tcf.batch.client.DbMetricsClient;
import com.nh.nsight.tcf.batch.config.DbStatusBatchProperties;
import com.nh.nsight.tcf.batch.config.DbStatusBatchProperties.DbTargetProperties;
import com.nh.nsight.tcf.batch.support.model.DbStatusCollectResult;
import com.nh.nsight.tcf.batch.support.model.DbStatusSnapshot;
import com.nh.nsight.tcf.batch.persistence.repository.OmDashboardStatusRepository;
import com.nh.nsight.tcf.util.DateTimeUtil;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DbStatusCollectService {
    private static final Logger log = LoggerFactory.getLogger(DbStatusCollectService.class);

    private final DbStatusBatchProperties properties;
    private final DbMetricsClient metricsClient;
    private final OmDashboardStatusRepository repository;
    private final DataSource dataSource;

    public DbStatusCollectService(DbStatusBatchProperties properties, DbMetricsClient metricsClient,
                                  OmDashboardStatusRepository repository, DataSource dataSource) {
        this.properties = properties;
        this.metricsClient = metricsClient;
        this.repository = repository;
        this.dataSource = dataSource;
    }

    public DbStatusCollectResult collectAndPersist() {
        long start = System.currentTimeMillis();
        String runTime = DateTimeUtil.nowKst();
        List<DbTargetProperties> targets = properties.getTargets().stream()
                .filter(DbTargetProperties::isEnabled)
                .toList();

        List<DbStatusSnapshot> snapshots = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (DbTargetProperties target : targets) {
            DbStatusSnapshot snapshot = collectOne(target, runTime);
            snapshots.add(snapshot);
            if (snapshot.reachable()) {
                repository.upsertDb(snapshot);
                successCount++;
            } else {
                repository.upsertDb(snapshot);
                failCount++;
            }
            log.info("DB status collected dbId={} health={} pool={}%",
                    snapshot.dbId(), snapshot.healthStatus(), snapshot.poolUsagePct());
        }

        pruneStaleTargets(targets);

        long durationMs = System.currentTimeMillis() - start;
        String runStatus = failCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAIL" : "PARTIAL");
        String message = "DB 상태 %d건 수집 (성공 %d, 실패 %d)".formatted(targets.size(), successCount, failCount);

        repository.insertBatchHistory(properties.getJobId(), runTime, runStatus, durationMs, message);

        return new DbStatusCollectResult(
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

    private DbStatusSnapshot collectOne(DbTargetProperties target, String checkedAt) {
        try {
            if ("jdbc".equalsIgnoreCase(target.getSourceType())) {
                return collectJdbc(target, checkedAt);
            }
            return collectActuator(target, checkedAt);
        } catch (Exception e) {
            log.warn("DB status collect failed dbId={} cause={}", target.getDbId(), e.getMessage());
            return failedSnapshot(target, checkedAt, e.getMessage());
        }
    }

    private DbStatusSnapshot collectJdbc(DbTargetProperties target, String checkedAt) {
        if (!StringUtils.hasText(target.getJdbcUrl())) {
            return failedSnapshot(target, checkedAt, "jdbcUrl이 설정되지 않았습니다.");
        }
        boolean reachable = metricsClient.pingJdbc(target);
        double poolPct = reachable ? resolveLocalPoolUsagePct() : 0;
        String health = metricsClient.resolveHealthStatus(poolPct, reachable);
        return new DbStatusSnapshot(
                target.getDbId(),
                target.getDbName(),
                health,
                round1(poolPct),
                checkedAt,
                reachable,
                reachable ? "JDBC ping OK" : "JDBC ping FAIL"
        );
    }

    private double resolveLocalPoolUsagePct() {
        if (dataSource instanceof HikariDataSource hikari) {
            int max = hikari.getMaximumPoolSize();
            HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
            if (pool != null && max > 0) {
                return pool.getActiveConnections() * 100.0 / max;
            }
        }
        return 0;
    }

    private DbStatusSnapshot collectActuator(DbTargetProperties target, String checkedAt) {
        if (!StringUtils.hasText(target.getBaseUrl())) {
            return failedSnapshot(target, checkedAt, "baseUrl이 설정되지 않았습니다.");
        }
        String componentHealth = metricsClient.dbHealthFromActuator(target.getBaseUrl());
        double poolPct = round1(metricsClient.poolUsagePctFromActuator(target.getBaseUrl()));
        boolean reachable = !"FAIL".equals(componentHealth);
        String health = metricsClient.resolveHealthStatus(poolPct, reachable);
        if ("FAIL".equals(componentHealth)) {
            health = "FAIL";
        } else if ("WARN".equals(componentHealth) && "NORMAL".equals(health)) {
            health = "WARN";
        }
        return new DbStatusSnapshot(
                target.getDbId(),
                target.getDbName(),
                health,
                poolPct,
                checkedAt,
                reachable,
                reachable ? "Actuator OK" : "Actuator DB DOWN"
        );
    }

    private DbStatusSnapshot failedSnapshot(DbTargetProperties target, String checkedAt, String detail) {
        return new DbStatusSnapshot(
                target.getDbId(),
                target.getDbName(),
                "FAIL",
                0,
                checkedAt,
                false,
                detail
        );
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private void pruneStaleTargets(List<DbTargetProperties> enabledTargets) {
        List<String> keepIds = enabledTargets.stream().map(DbTargetProperties::getDbId).toList();
        repository.retainOnlyDbIds(keepIds);
    }
}
