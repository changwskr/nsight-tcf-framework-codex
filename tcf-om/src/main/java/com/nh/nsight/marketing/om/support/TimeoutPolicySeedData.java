package com.nh.nsight.marketing.om.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/** OM_SERVICE_CATALOG 기준 기본 Timeout 정책 MERGE */
public final class TimeoutPolicySeedData {
    private static final Logger log = LoggerFactory.getLogger(TimeoutPolicySeedData.class);

    private TimeoutPolicySeedData() {}

    public static void mergeAll(JdbcTemplate jdbcTemplate) {
        ensureTable(jdbcTemplate);
        int merged = jdbcTemplate.update("""
                MERGE INTO TCF_SERVICE_TIMEOUT_POLICY (
                    SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE, SERVICE_NAME,
                    ONLINE_TIMEOUT_SEC, TX_TIMEOUT_SEC, DB_QUERY_TIMEOUT_SEC,
                    EXTERNAL_CONNECT_TIMEOUT_MS, EXTERNAL_READ_TIMEOUT_MS,
                    TIMEOUT_ACTION, USE_YN, DESCRIPTION, CREATED_AT
                )
                KEY (SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE)
                SELECT c.SERVICE_ID,
                       c.TRANSACTION_CODE,
                       c.BUSINESS_CODE,
                       COALESCE(c.DESCRIPTION, c.SERVICE_ID),
                       COALESCE(NULLIF(c.TIMEOUT_SEC, 0),
                                CASE WHEN UPPER(c.PROCESSING_TYPE) IN ('EXECUTE','CREATE','UPDATE','DELETE') THEN 10 ELSE 5 END),
                       5,
                       CASE WHEN UPPER(c.PROCESSING_TYPE) = 'EXECUTE' THEN 3 ELSE 3 END,
                       3000,
                       CASE WHEN UPPER(c.PROCESSING_TYPE) IN ('EXECUTE','CREATE','UPDATE','DELETE') THEN 5000 ELSE 5000 END,
                       CASE
                           WHEN UPPER(c.PROCESSING_TYPE) IN ('CREATE','UPDATE','DELETE') THEN 'UNKNOWN'
                           WHEN UPPER(c.PROCESSING_TYPE) = 'EXECUTE' THEN 'STATUS_CHECK'
                           ELSE 'FAIL'
                       END,
                       'Y',
                       c.DESCRIPTION,
                       CURRENT_TIMESTAMP
                  FROM OM_SERVICE_CATALOG c
                 WHERE COALESCE(c.USE_YN, 'Y') = 'Y'
                   AND c.SERVICE_ID IS NOT NULL
                   AND c.TRANSACTION_CODE IS NOT NULL
                   AND c.BUSINESS_CODE IS NOT NULL
                """);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM TCF_SERVICE_TIMEOUT_POLICY WHERE USE_YN = 'Y'",
                Integer.class);
        log.info("Timeout policy seed merged from catalog (touched={}, active={})", merged, count);
    }

    static void ensureTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS TCF_SERVICE_TIMEOUT_POLICY (
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
                """);
    }
}
