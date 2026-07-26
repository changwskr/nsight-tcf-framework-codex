package com.nh.nsight.tcf.web.persistence.dao;

import com.nh.nsight.tcf.core.config.TcfProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class TimeoutPolicySchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(TimeoutPolicySchemaInitializer.class);

    private static final String CREATE_TABLE_TEMPLATE = """
            CREATE TABLE IF NOT EXISTS %s (
                SERVICE_ID VARCHAR(100) NOT NULL,
                TRANSACTION_CODE VARCHAR(50) NOT NULL,
                BUSINESS_CODE VARCHAR(10) NOT NULL,
                SERVICE_NAME VARCHAR(200) NOT NULL,
                ONLINE_TIMEOUT_SEC INT NOT NULL DEFAULT 5,
                TX_TIMEOUT_SEC INT NOT NULL DEFAULT 5,
                DB_QUERY_TIMEOUT_SEC INT NOT NULL DEFAULT 3,
                EXTERNAL_CONNECT_TIMEOUT_MS INT NOT NULL DEFAULT 3000,
                EXTERNAL_READ_TIMEOUT_MS INT NOT NULL DEFAULT 5000,
                TIMEOUT_ACTION VARCHAR(30) NOT NULL DEFAULT 'FAIL',
                USE_YN CHAR(1) NOT NULL DEFAULT 'Y',
                DESCRIPTION VARCHAR(500),
                CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UPDATED_AT TIMESTAMP,
                PRIMARY KEY (SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE)
            )
            """;

    private static final String CREATE_SVC_INDEX_TEMPLATE = """
            CREATE INDEX IF NOT EXISTS IDX_%s_SVC ON %s (BUSINESS_CODE, SERVICE_ID)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TcfProperties properties;

    public TimeoutPolicySchemaInitializer(JdbcTemplate jdbcTemplate, TcfProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        String tableName = JdbcTimeoutPolicyRepository.validateTableName(properties.getTimeoutPolicyTableName());
        jdbcTemplate.execute(CREATE_TABLE_TEMPLATE.formatted(tableName));
        jdbcTemplate.execute(CREATE_SVC_INDEX_TEMPLATE.formatted(tableName, tableName));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE USE_YN = 'Y'",
                Integer.class);
        log.info("Timeout policy table ready: {} (activePolicies={})", tableName, count);
    }
}
