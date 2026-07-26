package com.nh.nsight.tcf.web.persistence.dao;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.timeout.TimeoutPolicy;
import com.nh.nsight.tcf.core.support.timeout.TimeoutPolicyRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

public class JdbcTimeoutPolicyRepository implements TimeoutPolicyRepository {

    private static final String FIND_POLICY_SQL_TEMPLATE = """
            SELECT SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE, SERVICE_NAME,
                   ONLINE_TIMEOUT_SEC, TX_TIMEOUT_SEC, DB_QUERY_TIMEOUT_SEC,
                   EXTERNAL_CONNECT_TIMEOUT_MS, EXTERNAL_READ_TIMEOUT_MS,
                   TIMEOUT_ACTION, USE_YN, DESCRIPTION
              FROM %s
             WHERE SERVICE_ID = ?
               AND TRANSACTION_CODE = ?
               AND UPPER(BUSINESS_CODE) = UPPER(?)
               AND USE_YN = 'Y'
             FETCH FIRST 1 ROW ONLY
            """;

    private final JdbcTemplate jdbcTemplate;
    private final String findPolicySql;

    public JdbcTimeoutPolicyRepository(JdbcTemplate jdbcTemplate, TcfProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        String tableName = validateTableName(properties.getTimeoutPolicyTableName());
        this.findPolicySql = FIND_POLICY_SQL_TEMPLATE.formatted(tableName);
    }

    @Override
    public Optional<TimeoutPolicy> findPolicy(String serviceId, String transactionCode, String businessCode) {
        if (!StringUtils.hasText(serviceId) || !StringUtils.hasText(transactionCode)
                || !StringUtils.hasText(businessCode)) {
            return Optional.empty();
        }
        List<TimeoutPolicy> policies = jdbcTemplate.query(
                findPolicySql,
                this::mapRow,
                serviceId.trim(),
                transactionCode.trim(),
                businessCode.trim());
        if (policies.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(policies.get(0));
    }

    private TimeoutPolicy mapRow(ResultSet rs, int rowNum) throws SQLException {
        TimeoutPolicy policy = new TimeoutPolicy();
        policy.setServiceId(rs.getString("SERVICE_ID"));
        policy.setTransactionCode(rs.getString("TRANSACTION_CODE"));
        policy.setBusinessCode(rs.getString("BUSINESS_CODE"));
        policy.setServiceName(rs.getString("SERVICE_NAME"));
        policy.setOnlineTimeoutSec(rs.getInt("ONLINE_TIMEOUT_SEC"));
        policy.setTxTimeoutSec(rs.getInt("TX_TIMEOUT_SEC"));
        policy.setDbQueryTimeoutSec(rs.getInt("DB_QUERY_TIMEOUT_SEC"));
        policy.setExternalConnectTimeoutMs(rs.getInt("EXTERNAL_CONNECT_TIMEOUT_MS"));
        policy.setExternalReadTimeoutMs(rs.getInt("EXTERNAL_READ_TIMEOUT_MS"));
        policy.setTimeoutAction(rs.getString("TIMEOUT_ACTION"));
        policy.setUseYn(rs.getString("USE_YN"));
        policy.setDescription(rs.getString("DESCRIPTION"));
        return policy;
    }

    static String validateTableName(String tableName) {
        if (!StringUtils.hasText(tableName) || !tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid timeout policy table name: " + tableName);
        }
        return tableName.toUpperCase();
    }
}
