package com.nh.nsight.tcf.batch.persistence.repository;

import com.nh.nsight.tcf.batch.support.model.ApStatusSnapshot;
import com.nh.nsight.tcf.batch.support.model.DbStatusSnapshot;
import com.nh.nsight.tcf.batch.support.model.DeployStatusSnapshot;
import com.nh.nsight.tcf.batch.support.model.SessionStatusSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class OmDashboardStatusRepository {
    private final JdbcTemplate jdbcTemplate;

    public OmDashboardStatusRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertAp(ApStatusSnapshot snapshot) {
        jdbcTemplate.update("""
                MERGE INTO OM_AP_STATUS (AP_ID, AP_NAME, HEALTH_STATUS, CPU_USAGE_PCT, HEAP_USAGE_PCT,
                                         THREAD_COUNT, CHECKED_AT)
                KEY (AP_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                snapshot.apId(),
                snapshot.apName(),
                snapshot.healthStatus(),
                snapshot.cpuUsagePct(),
                snapshot.heapUsagePct(),
                snapshot.threadCount(),
                snapshot.checkedAt());
    }

    public void deleteAp(String apId) {
        jdbcTemplate.update("DELETE FROM OM_AP_STATUS WHERE AP_ID = ?", apId);
    }

    public void upsertDb(DbStatusSnapshot snapshot) {
        jdbcTemplate.update("""
                MERGE INTO OM_DB_STATUS (DB_ID, DB_NAME, HEALTH_STATUS, POOL_USAGE_PCT, CHECKED_AT)
                KEY (DB_ID)
                VALUES (?, ?, ?, ?, ?)
                """,
                snapshot.dbId(),
                snapshot.dbName(),
                snapshot.healthStatus(),
                snapshot.poolUsagePct(),
                snapshot.checkedAt());
    }

    public void deleteDb(String dbId) {
        jdbcTemplate.update("DELETE FROM OM_DB_STATUS WHERE DB_ID = ?", dbId);
    }

    public void upsertSession(SessionStatusSnapshot snapshot) {
        jdbcTemplate.update("""
                MERGE INTO OM_SESSION_STATUS (SCOPE_ID, SCOPE_NAME, ACTIVE_COUNT, EXPIRED_COUNT,
                                              TOTAL_COUNT, UNIQUE_USER_COUNT, HEALTH_STATUS, CHECKED_AT)
                KEY (SCOPE_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                snapshot.scopeId(),
                snapshot.scopeName(),
                snapshot.activeCount(),
                snapshot.expiredCount(),
                snapshot.totalCount(),
                snapshot.uniqueUserCount(),
                snapshot.healthStatus(),
                snapshot.checkedAt());
    }

    public void deleteSession(String scopeId) {
        jdbcTemplate.update("DELETE FROM OM_SESSION_STATUS WHERE SCOPE_ID = ?", scopeId);
    }

    public void upsertDeploy(DeployStatusSnapshot snapshot) {
        jdbcTemplate.update("""
                MERGE INTO OM_DEPLOY_STATUS (BUSINESS_CODE, WAR_NAME, WAR_VERSION, DEPLOYED_AT, HEALTH_STATUS)
                KEY (BUSINESS_CODE)
                VALUES (?, ?, ?, ?, ?)
                """,
                snapshot.businessCode(),
                snapshot.warName(),
                snapshot.warVersion(),
                snapshot.deployedAt(),
                snapshot.healthStatus());
    }

    public void deleteDeploy(String businessCode) {
        jdbcTemplate.update("DELETE FROM OM_DEPLOY_STATUS WHERE BUSINESS_CODE = ?", businessCode);
    }

    public void retainOnlyApIds(Collection<String> keepIds) {
        deleteNotIn("OM_AP_STATUS", "AP_ID", keepIds);
    }

    public void retainOnlyDbIds(Collection<String> keepIds) {
        deleteNotIn("OM_DB_STATUS", "DB_ID", keepIds);
    }

    public void retainOnlySessionScopeIds(Collection<String> keepIds) {
        deleteNotIn("OM_SESSION_STATUS", "SCOPE_ID", keepIds);
    }

    public void retainOnlyDeployBusinessCodes(Collection<String> keepIds) {
        deleteNotIn("OM_DEPLOY_STATUS", "BUSINESS_CODE", keepIds);
    }

    private void deleteNotIn(String table, String column, Collection<String> keepIds) {
        List<String> ids = keepIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            jdbcTemplate.update("DELETE FROM " + table);
            return;
        }
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        jdbcTemplate.update(
                "DELETE FROM " + table + " WHERE " + column + " NOT IN (" + placeholders + ")",
                ids.toArray());
    }

    public void insertBatchHistory(String jobId, String runTime, String runStatus, long durationMs, String message) {
        jdbcTemplate.update("""
                INSERT INTO OM_BATCH_HISTORY (HISTORY_ID, JOB_ID, RUN_TIME, RUN_STATUS, DURATION_MS, RESULT_MESSAGE)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "BH-" + UUID.randomUUID(),
                jobId,
                runTime,
                runStatus,
                durationMs,
                message);
    }
}
