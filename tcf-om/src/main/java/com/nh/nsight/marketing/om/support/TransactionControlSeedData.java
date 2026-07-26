package com.nh.nsight.marketing.om.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/** 거래통제 규칙 — 자동 시드 없음. OM 화면에서만 등록한다. */
public final class TransactionControlSeedData {
    private static final Logger log = LoggerFactory.getLogger(TransactionControlSeedData.class);
    private static final String MIGRATION_GROUP = "OM_MIGRATION";
    private static final String CLEAR_ALL_CODE = "TX_CONTROL_CLEAR_ALL";

    private TransactionControlSeedData() {}

    public static void mergeAll(JdbcTemplate jdbcTemplate) {
        clearAllRulesOnce(jdbcTemplate);
    }

    /** 기존 시드·수동 등록 규칙을 1회 삭제 (재기동마다 반복하지 않음) */
    static void clearAllRulesOnce(JdbcTemplate jdbcTemplate) {
        if (isClearAllDone(jdbcTemplate)) {
            return;
        }
        int removed = jdbcTemplate.update("DELETE FROM TCF_TRANSACTION_CONTROL");
        markClearAllDone(jdbcTemplate);
        log.info("Cleared all TCF_TRANSACTION_CONTROL rules (removed={})", removed);
    }

    private static boolean isClearAllDone(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM OM_COMMON_CODE
                 WHERE CODE_GROUP = ?
                   AND CODE = ?
                   AND USE_YN = 'Y'
                """, Integer.class, MIGRATION_GROUP, CLEAR_ALL_CODE);
        return count != null && count > 0;
    }

    private static void markClearAllDone(JdbcTemplate jdbcTemplate) {
        String now = java.time.OffsetDateTime.now().toString();
        jdbcTemplate.update("""
                MERGE INTO OM_COMMON_CODE (CODE_GROUP, CODE, CODE_NAME, SORT_ORDER, USE_YN,
                                           DESCRIPTION, CREATED_AT, UPDATED_AT)
                KEY (CODE_GROUP, CODE)
                VALUES (?, ?, ?, 1, 'Y', ?, ?, ?)
                """,
                MIGRATION_GROUP,
                CLEAR_ALL_CODE,
                "거래통제 전체 초기화",
                "TCF_TRANSACTION_CONTROL 전체 삭제 1회 수행 완료",
                now,
                now);
    }
}
