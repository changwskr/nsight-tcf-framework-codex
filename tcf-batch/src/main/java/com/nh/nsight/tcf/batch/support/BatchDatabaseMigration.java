package com.nh.nsight.tcf.batch.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class BatchDatabaseMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BatchDatabaseMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public BatchDatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureApStatusTable();
        ensureDbStatusTable();
        ensureSessionStatusTable();
        ensureDeployStatusTable();
        ensureBatchHistoryTable();
        removeEmptySessionStatus();
        removeDuplicateOmSessionStatus();
        removeEmptyDeployStatus();
        removeEmptyDbStatus();
        removeObsoleteDashboardRows();
    }

    private void removeObsoleteDashboardRows() {
        if (tableExists("OM_AP_STATUS")) {
            jdbcTemplate.update("""
                    DELETE FROM OM_AP_STATUS
                     WHERE AP_ID IN (
                           'bc-ap', 'ic-ap', 'pc-ap', 'cc-ap', 'ms-ap', 'sv-ap', 'pd-ap', 'cm-ap',
                           'eb-ap', 'ep-ap', 'bp-ap', 'bd-ap', 'ss-ap', 'cs-ap', 'ct-ap', 'mg-ap'
                     )
                    """);
        }
        if (tableExists("OM_DB_STATUS")) {
            jdbcTemplate.update("""
                    DELETE FROM OM_DB_STATUS
                     WHERE DB_ID IN (
                           'LOGDB', 'CC-DS', 'BC-DS', 'IC-DS', 'PC-DS', 'MS-DS', 'SV-DS', 'PD-DS',
                           'CM-DS', 'EB-DS', 'EP-DS', 'BP-DS', 'BD-DS', 'SS-DS', 'CS-DS', 'CT-DS', 'MG-DS'
                     )
                    """);
        }
        if (tableExists("OM_SESSION_STATUS")) {
            jdbcTemplate.update("""
                    DELETE FROM OM_SESSION_STATUS
                     WHERE SCOPE_ID IN (
                           'SV-AP', 'BC-AP', 'IC-AP', 'PC-AP', 'CC-AP', 'MS-AP', 'PD-AP', 'CM-AP',
                           'EB-AP', 'EP-AP', 'BP-AP', 'BD-AP', 'SS-AP', 'CS-AP', 'CT-AP', 'MG-AP'
                     )
                    """);
        }
        if (tableExists("OM_DEPLOY_STATUS")) {
            jdbcTemplate.update("""
                    DELETE FROM OM_DEPLOY_STATUS
                     WHERE BUSINESS_CODE IN (
                           'CC', 'BC', 'IC', 'PC', 'MS', 'SV', 'PD', 'CM', 'EB', 'EP',
                           'BP', 'BD', 'SS', 'CS', 'CT', 'MG'
                     )
                    """);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = UPPER(?)",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private void removeEmptyDbStatus() {
        if (!tableExists("OM_DB_STATUS")) {
            log.info("Skip OM_DB_STATUS cleanup — table not found yet");
            return;
        }
        jdbcTemplate.update("""
                DELETE FROM OM_DB_STATUS
                 WHERE DB_ID IN ('RDW', 'ADW', 'SESSIONDB')
                """);
    }

    private void removeEmptyDeployStatus() {
        if (!tableExists("OM_DEPLOY_STATUS")) {
            return;
        }
        jdbcTemplate.update("""
                DELETE FROM OM_DEPLOY_STATUS
                 WHERE BUSINESS_CODE = 'ET'
                """);
    }

    private void removeDuplicateOmSessionStatus() {
        if (!tableExists("OM_SESSION_STATUS")) {
            return;
        }
        jdbcTemplate.update("""
                DELETE FROM OM_SESSION_STATUS
                 WHERE SCOPE_ID = 'OM-AP'
                """);
    }

    private void removeEmptySessionStatus() {
        if (!tableExists("OM_SESSION_STATUS")) {
            return;
        }
        jdbcTemplate.update("""
                DELETE FROM OM_SESSION_STATUS
                 WHERE SCOPE_ID IN ('legacy-session')
                """);
    }

    private void ensureApStatusTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_AP_STATUS (
                    AP_ID VARCHAR(50) NOT NULL,
                    AP_NAME VARCHAR(100),
                    HEALTH_STATUS VARCHAR(20),
                    CPU_USAGE_PCT DECIMAL(5,2),
                    HEAP_USAGE_PCT DECIMAL(5,2),
                    THREAD_COUNT INT,
                    CHECKED_AT VARCHAR(40),
                    PRIMARY KEY (AP_ID)
                )
                """);
        log.info("OM_AP_STATUS table ensured");
    }

    private void ensureDbStatusTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_DB_STATUS (
                    DB_ID VARCHAR(50) NOT NULL,
                    DB_NAME VARCHAR(100),
                    HEALTH_STATUS VARCHAR(20),
                    POOL_USAGE_PCT DECIMAL(5,2),
                    CHECKED_AT VARCHAR(40),
                    PRIMARY KEY (DB_ID)
                )
                """);
        log.info("OM_DB_STATUS table ensured");
    }

    private void ensureDeployStatusTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_DEPLOY_STATUS (
                    BUSINESS_CODE VARCHAR(10) NOT NULL,
                    WAR_NAME VARCHAR(50),
                    WAR_VERSION VARCHAR(50),
                    DEPLOYED_AT VARCHAR(40),
                    HEALTH_STATUS VARCHAR(20),
                    PRIMARY KEY (BUSINESS_CODE)
                )
                """);
        log.info("OM_DEPLOY_STATUS table ensured");
    }

    private void ensureSessionStatusTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_SESSION_STATUS (
                    SCOPE_ID VARCHAR(50) NOT NULL,
                    SCOPE_NAME VARCHAR(100),
                    ACTIVE_COUNT INT,
                    EXPIRED_COUNT INT,
                    TOTAL_COUNT INT,
                    UNIQUE_USER_COUNT INT,
                    HEALTH_STATUS VARCHAR(20),
                    CHECKED_AT VARCHAR(40),
                    PRIMARY KEY (SCOPE_ID)
                )
                """);
        log.info("OM_SESSION_STATUS table ensured");
    }

    private void ensureBatchHistoryTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_BATCH_HISTORY (
                    HISTORY_ID VARCHAR(64) NOT NULL,
                    JOB_ID VARCHAR(50) NOT NULL,
                    RUN_TIME VARCHAR(40) NOT NULL,
                    RUN_STATUS VARCHAR(20),
                    DURATION_MS BIGINT,
                    RESULT_MESSAGE VARCHAR(500),
                    PRIMARY KEY (HISTORY_ID)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS IDX_OM_BATCH_HIST ON OM_BATCH_HISTORY (JOB_ID, RUN_TIME DESC)
                """);
        log.info("OM_BATCH_HISTORY table ensured");
    }
}
