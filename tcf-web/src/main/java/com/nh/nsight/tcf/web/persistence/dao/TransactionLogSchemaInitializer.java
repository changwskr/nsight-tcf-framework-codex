package com.nh.nsight.tcf.web.persistence.dao;

import com.nh.nsight.tcf.core.config.TcfProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 트랜잭션 로그 테이블이 없을 때 H2 등 로컬 DB에 자동 생성한다.
 */
public class TransactionLogSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(TransactionLogSchemaInitializer.class);

    private static final String CREATE_TABLE_TEMPLATE = """
            CREATE TABLE IF NOT EXISTS %s (
                LOG_ID VARCHAR(64) NOT NULL,
                TX_TIME VARCHAR(40) NOT NULL,
                BUSINESS_CODE VARCHAR(10),
                SERVICE_ID VARCHAR(100),
                TRANSACTION_CODE VARCHAR(50),
                GUID VARCHAR(64) NOT NULL,
                TRACE_ID VARCHAR(64),
                USER_ID VARCHAR(50),
                BRANCH_ID VARCHAR(20),
                RESULT_STATUS VARCHAR(20),
                RESULT_CODE VARCHAR(20),
                ERROR_CODE VARCHAR(50),
                ELAPSED_TIME_MS BIGINT,
                PRIMARY KEY (LOG_ID)
            )
            """;

    private static final String CREATE_GUID_INDEX_TEMPLATE = """
            CREATE INDEX IF NOT EXISTS IDX_%s_GUID ON %s (GUID, TX_TIME DESC)
            """;

    private static final String CREATE_SVC_INDEX_TEMPLATE = """
            CREATE INDEX IF NOT EXISTS IDX_%s_SVC ON %s (SERVICE_ID, TX_TIME DESC)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TcfProperties properties;

    public TransactionLogSchemaInitializer(JdbcTemplate jdbcTemplate, TcfProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        String tableName = JdbcTransactionLogRepository.validateTableName(properties.getTransactionLogTableName());
        jdbcTemplate.execute(CREATE_TABLE_TEMPLATE.formatted(tableName));
        jdbcTemplate.execute(CREATE_GUID_INDEX_TEMPLATE.formatted(tableName, tableName));
        jdbcTemplate.execute(CREATE_SVC_INDEX_TEMPLATE.formatted(tableName, tableName));
        log.info("Transaction log table ready: {}", tableName);
    }
}
