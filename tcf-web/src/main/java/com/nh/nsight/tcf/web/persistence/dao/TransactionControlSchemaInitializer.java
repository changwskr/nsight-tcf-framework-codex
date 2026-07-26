package com.nh.nsight.tcf.web.persistence.dao;

import com.nh.nsight.tcf.core.config.TcfProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class TransactionControlSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(TransactionControlSchemaInitializer.class);

    private static final String CREATE_TABLE_TEMPLATE = """
            CREATE TABLE IF NOT EXISTS %s (
                SERVICE_ID VARCHAR(100) NOT NULL,
                TRANSACTION_CODE VARCHAR(50) NOT NULL,
                BUSINESS_CODE VARCHAR(10) NOT NULL,
                SERVICE_NAME VARCHAR(200) NOT NULL,
                USER_ID VARCHAR(50) NOT NULL,
                CHANNEL_ID VARCHAR(30) NOT NULL,
                BRANCH_ID VARCHAR(30) NOT NULL,
                CONTROL_TYPE VARCHAR(20) NOT NULL DEFAULT 'FULL',
                BLOCK_YN CHAR(1) NOT NULL DEFAULT 'Y',
                PRIMARY KEY (
                    SERVICE_ID,
                    TRANSACTION_CODE,
                    BUSINESS_CODE,
                    SERVICE_NAME,
                    USER_ID,
                    CHANNEL_ID,
                    BRANCH_ID
                )
            )
            """;

    private static final String CREATE_USER_INDEX_TEMPLATE = """
            CREATE INDEX IF NOT EXISTS IDX_%s_USER ON %s (USER_ID, CHANNEL_ID, BRANCH_ID)
            """;

    private static final String CREATE_SVC_INDEX_TEMPLATE = """
            CREATE INDEX IF NOT EXISTS IDX_%s_SVC ON %s (BUSINESS_CODE, SERVICE_ID, TRANSACTION_CODE)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TcfProperties properties;

    public TransactionControlSchemaInitializer(JdbcTemplate jdbcTemplate, TcfProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        String tableName = JdbcTransactionControlRepository
                .validateTableName(properties.getTransactionControlTableName());
        dropLegacyTableIfNeeded(tableName);
        jdbcTemplate.execute(CREATE_TABLE_TEMPLATE.formatted(tableName));
        ensureBlockColumns(tableName);
        jdbcTemplate.execute(CREATE_USER_INDEX_TEMPLATE.formatted(tableName, tableName));
        jdbcTemplate.execute(CREATE_SVC_INDEX_TEMPLATE.formatted(tableName, tableName));
        Integer blockingRules = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE BLOCK_YN = 'Y'",
                Integer.class);
        log.info("Transaction control blocklist table ready: {} (blockingRules={})", tableName, blockingRules);
    }

    private void dropLegacyTableIfNeeded(String tableName) {
        Integer legacyColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE TABLE_NAME = ?
                   AND COLUMN_NAME = 'TX_ID'
                """, Integer.class, tableName);
        if (legacyColumnCount != null && legacyColumnCount > 0) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
            log.info("Dropped legacy transaction control table: {}", tableName);
        }
    }

    private void ensureBlockColumns(String tableName) {
        Integer controlTypeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE TABLE_NAME = ?
                   AND COLUMN_NAME = 'CONTROL_TYPE'
                """, Integer.class, tableName);
        if (controlTypeCount == null || controlTypeCount == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE %s ADD COLUMN IF NOT EXISTS CONTROL_TYPE VARCHAR(20) NOT NULL DEFAULT 'FULL'
                    """.formatted(tableName));
            jdbcTemplate.execute("""
                    ALTER TABLE %s ADD COLUMN IF NOT EXISTS BLOCK_YN CHAR(1) NOT NULL DEFAULT 'N'
                    """.formatted(tableName));
            jdbcTemplate.update("""
                    UPDATE %s SET BLOCK_YN = 'N' WHERE BLOCK_YN IS NULL
                    """.formatted(tableName));
            log.info("Added CONTROL_TYPE/BLOCK_YN columns to {}", tableName);
        }
    }
}
