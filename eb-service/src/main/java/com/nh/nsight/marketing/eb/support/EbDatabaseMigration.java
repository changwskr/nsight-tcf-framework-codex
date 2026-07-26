package com.nh.nsight.marketing.eb.support;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
public class EbDatabaseMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(EbDatabaseMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public EbDatabaseMigration(@Qualifier("dataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS EB_USER (
                    USER_ID     VARCHAR(50) PRIMARY KEY,
                    USER_NAME   VARCHAR(100),
                    BRANCH_ID   VARCHAR(20),
                    CREATED_AT  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS EB_EVENT (
                    EVENT_ID      VARCHAR(50) PRIMARY KEY,
                    USER_ID       VARCHAR(50) NOT NULL,
                    EVENT_TYPE    VARCHAR(30) NOT NULL,
                    EVENT_STATUS  VARCHAR(20) NOT NULL,
                    RETRY_COUNT   INT DEFAULT 0,
                    CREATED_AT    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    SENT_AT       TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS IX_EB_EVENT_STATUS ON EB_EVENT (EVENT_STATUS, CREATED_AT)
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS EB_SYSTEM_TX (
                    TX_SEQ_NO      VARCHAR(40) PRIMARY KEY,
                    TX_DATE        VARCHAR(10) NOT NULL,
                    SCREEN_ID      VARCHAR(20),
                    SERVICE_ID     VARCHAR(80),
                    GLOBAL_ID      VARCHAR(64),
                    REQUEST_AT     TIMESTAMP NOT NULL,
                    RESPONSE_AT    TIMESTAMP,
                    ELAPSED_SEC    INT,
                    INPUT_CONTENT  VARCHAR(1000),
                    EMP_NO         VARCHAR(20),
                    BRANCH_CODE    VARCHAR(20),
                    TERMINAL_IP    VARCHAR(40),
                    TX_TYPE        VARCHAR(20)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS IX_EB_SYSTEM_TX_REQ ON EB_SYSTEM_TX (REQUEST_AT, TX_TYPE)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS IX_EB_SYSTEM_TX_SVC ON EB_SYSTEM_TX (SERVICE_ID, SCREEN_ID)
                """);
        seedSystemTxSamples();
        log.info("EB schema migration applied.");
    }

    private void seedSystemTxSamples() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM EB_SYSTEM_TX", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        insertSample(
                "202603050900001", "2026-03-05", "19410", "EB.SystemTx.inquiry", "GID-19410-0001",
                "2026-03-05T09:12:01", "2026-03-05T09:12:03", 2,
                "{\"customerId\":\"C001\"}", "E1001", "001", "10.10.1.21", "NORMAL");
        insertSample(
                "202603051015002", "2026-03-05", "19101", "EB.User.inquiry", "GID-19410-0002",
                "2026-03-05T10:15:44", "2026-03-05T10:15:45", 1,
                "{\"userId\":\"U001\"}", "E1002", "002", "10.10.1.33", "NORMAL");
        insertSample(
                "202603051430003", "2026-03-05", "19410", "EB.Event.inquiry", "GID-19410-0003",
                "2026-03-05T14:30:10", "2026-03-05T14:30:18", 8,
                "{\"eventStatus\":\"FAIL\"}", "E1001", "001", "10.10.1.21", "ERROR");
        insertSample(
                "202603052200004", "2026-03-05", "19200", "EB.Batch.inquiry", "GID-19410-0004",
                "2026-03-05T22:00:01", "2026-03-05T22:00:02", 1,
                "{}", "E1099", "900", "10.20.0.5", "NORMAL");
        insertSample(
                "202603061100005", "2026-03-06", "19410", "EB.SystemTx.inquiry", "GID-19410-0005",
                "2026-03-06T11:00:05", "2026-03-06T11:00:12", 7,
                "{\"txType\":\"ERROR\"}", "E1003", "010", "10.10.2.8", "ERROR");
        log.info("EB_SYSTEM_TX sample rows seeded for screen 19410 (5 rows).");
    }

    private void insertSample(
            String txSeqNo,
            String txDate,
            String screenId,
            String serviceId,
            String globalId,
            String requestAt,
            String responseAt,
            int elapsedSec,
            String inputContent,
            String empNo,
            String branchCode,
            String terminalIp,
            String txType) {
        jdbcTemplate.update("""
                INSERT INTO EB_SYSTEM_TX (
                    TX_SEQ_NO, TX_DATE, SCREEN_ID, SERVICE_ID, GLOBAL_ID,
                    REQUEST_AT, RESPONSE_AT, ELAPSED_SEC, INPUT_CONTENT,
                    EMP_NO, BRANCH_CODE, TERMINAL_IP, TX_TYPE
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                txSeqNo,
                txDate,
                screenId,
                serviceId,
                globalId,
                Timestamp.valueOf(LocalDateTime.parse(requestAt)),
                Timestamp.valueOf(LocalDateTime.parse(responseAt)),
                elapsedSec,
                inputContent,
                empNo,
                branchCode,
                terminalIp,
                txType);
    }
}
