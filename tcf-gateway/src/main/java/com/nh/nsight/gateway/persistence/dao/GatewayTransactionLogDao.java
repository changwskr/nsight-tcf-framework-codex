package com.nh.nsight.gateway.persistence.dao;

import com.nh.nsight.gateway.support.GatewayTransactionLogEntry;
import com.nh.nsight.gateway.support.GatewayTransactionLogRowMapper;
import com.nh.nsight.gateway.support.GatewayTransactionLogSummaryMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class GatewayTransactionLogDao {
    private final JdbcTemplate jdbcTemplate;

    public GatewayTransactionLogDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(GatewayTransactionLogEntry entry) {
        jdbcTemplate.update("""
                INSERT INTO TCF_GATEWAY_TX_LOG (
                    LOG_ID, TX_TIME, ENV_CODE, BUSINESS_CODE, SERVICE_ID, TRANSACTION_CODE,
                    GUID, TRACE_ID, USER_ID, BRANCH_ID, SESSION_ID, TARGET_URL,
                    HTTP_STATUS, RESULT_STATUS, RESULT_CODE, ERROR_CODE, ELAPSED_TIME_MS, PHASE
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                entry.logId(), entry.txTime(), entry.envCode(), entry.businessCode(),
                entry.serviceId(), entry.transactionCode(), entry.guid(), entry.traceId(),
                entry.userId(), entry.branchId(), entry.sessionId(), entry.targetUrl(),
                entry.httpStatus(), entry.resultStatus(), entry.resultCode(), entry.errorCode(),
                entry.elapsedTimeMs(), entry.phase());
    }

    public List<Map<String, Object>> search(Map<String, Object> criteria) {
        WhereClause where = buildWhere(criteria);
        int offset = intValue(criteria.get("offset"));
        int pageSize = intValue(criteria.get("pageSize"));
        String sql = """
                SELECT LOG_ID AS logId, TX_TIME AS txTime, ENV_CODE AS envCode,
                       BUSINESS_CODE AS businessCode, SERVICE_ID AS serviceId,
                       TRANSACTION_CODE AS transactionCode, GUID AS guid, TRACE_ID AS traceId,
                       USER_ID AS userId, BRANCH_ID AS branchId, SESSION_ID AS sessionId,
                       TARGET_URL AS targetUrl, HTTP_STATUS AS httpStatus,
                       RESULT_STATUS AS resultStatus, RESULT_CODE AS resultCode,
                       ERROR_CODE AS errorCode, ELAPSED_TIME_MS AS elapsedTimeMs, PHASE AS phase
                  FROM TCF_GATEWAY_TX_LOG
                """ + where.sql + """
                 ORDER BY TX_TIME DESC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        List<Object> params = new ArrayList<>(where.params);
        params.add(offset);
        params.add(pageSize);
        return jdbcTemplate.query(sql, GatewayTransactionLogRowMapper.instance(), params.toArray());
    }

    public int count(Map<String, Object> criteria) {
        WhereClause where = buildWhere(criteria);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM TCF_GATEWAY_TX_LOG" + where.sql,
                Integer.class,
                where.params.toArray());
        return count == null ? 0 : count;
    }

    public Map<String, Object> summarize(Map<String, Object> criteria) {
        WhereClause where = buildWhere(criteria);
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT COUNT(*) AS totalCount,
                       SUM(CASE WHEN RESULT_STATUS = 'SUCCESS' THEN 1 ELSE 0 END) AS successCount,
                       SUM(CASE WHEN RESULT_STATUS = 'FAIL' OR RESULT_STATUS = 'ERROR' THEN 1 ELSE 0 END) AS errorCount,
                       SUM(CASE WHEN ELAPSED_TIME_MS >= 5000 THEN 1 ELSE 0 END) AS timeoutCount,
                       COALESCE(AVG(ELAPSED_TIME_MS), 0) AS avgElapsedMs
                  FROM TCF_GATEWAY_TX_LOG
                """ + where.sql, GatewayTransactionLogSummaryMapper.instance(), where.params.toArray());
        if (rows.isEmpty()) {
            return Map.of();
        }
        return rows.get(0);
    }

    public int deleteAll() {
        return jdbcTemplate.update("DELETE FROM TCF_GATEWAY_TX_LOG");
    }

    private WhereClause buildWhere(Map<String, Object> criteria) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendEq(sql, params, criteria, "businessCode", "BUSINESS_CODE");
        appendEq(sql, params, criteria, "envCode", "ENV_CODE");
        appendEq(sql, params, criteria, "guid", "GUID");
        appendEq(sql, params, criteria, "traceId", "TRACE_ID");
        appendLike(sql, params, criteria, "serviceId", "SERVICE_ID");
        appendEq(sql, params, criteria, "transactionCode", "TRANSACTION_CODE");
        appendEq(sql, params, criteria, "userId", "USER_ID");
        appendEq(sql, params, criteria, "branchId", "BRANCH_ID");
        appendEq(sql, params, criteria, "resultStatus", "RESULT_STATUS");
        appendLike(sql, params, criteria, "errorCode", "ERROR_CODE");
        appendGte(sql, params, criteria, "fromDate", "TX_TIME");
        appendLte(sql, params, criteria, "toDate", "TX_TIME");
        return new WhereClause(sql.toString(), params);
    }

    private void appendEq(StringBuilder sql, List<Object> params, Map<String, Object> criteria,
                          String key, String column) {
        String value = stringValue(criteria.get(key));
        if (StringUtils.hasText(value)) {
            sql.append(" AND ").append(column).append(" = ? ");
            params.add(value);
        }
    }

    private void appendLike(StringBuilder sql, List<Object> params, Map<String, Object> criteria,
                            String key, String column) {
        String value = stringValue(criteria.get(key));
        if (StringUtils.hasText(value)) {
            sql.append(" AND ").append(column).append(" LIKE '%' || ? || '%' ");
            params.add(value);
        }
    }

    private void appendGte(StringBuilder sql, List<Object> params, Map<String, Object> criteria,
                           String key, String column) {
        String value = stringValue(criteria.get(key));
        if (StringUtils.hasText(value)) {
            sql.append(" AND ").append(column).append(" >= ? ");
            params.add(value);
        }
    }

    private void appendLte(StringBuilder sql, List<Object> params, Map<String, Object> criteria,
                           String key, String column) {
        String value = stringValue(criteria.get(key));
        if (StringUtils.hasText(value)) {
            sql.append(" AND ").append(column).append(" <= ? ");
            params.add(value);
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record WhereClause(String sql, List<Object> params) {
    }
}
