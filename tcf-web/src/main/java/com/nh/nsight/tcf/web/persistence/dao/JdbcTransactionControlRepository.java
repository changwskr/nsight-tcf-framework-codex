package com.nh.nsight.tcf.web.persistence.dao;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.control.TransactionControlExemptions;
import com.nh.nsight.tcf.core.support.control.TransactionControlHeader;
import com.nh.nsight.tcf.core.support.control.TransactionControlRepository;
import com.nh.nsight.tcf.core.support.control.TransactionControlRule;
import com.nh.nsight.tcf.core.support.control.TcfTransactionControlConstants;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

public class JdbcTransactionControlRepository implements TransactionControlRepository {

    private static final String GLOBAL_UNBLOCK_SQL_TEMPLATE = """
            SELECT BLOCK_YN
              FROM %s
             WHERE CONTROL_TYPE = ?
               AND BLOCK_YN = ?
             FETCH FIRST 1 ROW ONLY
            """;

    /** 통제유형 + 해당 필드값만 일치하면 차단 (와일드카드 컬럼 불필요) */
    private static final String FIND_BLOCKING_RULE_SQL_TEMPLATE = """
            SELECT CONTROL_TYPE, BLOCK_YN
              FROM %s
             WHERE BLOCK_YN = 'Y'
               AND (
                    CONTROL_TYPE = 'GLOBAL'
                 OR (CONTROL_TYPE = 'BUSINESS' AND UPPER(BUSINESS_CODE) = UPPER(?))
                 OR (CONTROL_TYPE = 'SERVICE' AND SERVICE_ID = ?)
                 OR (CONTROL_TYPE = 'CHANNEL' AND CHANNEL_ID = ?)
                 OR (CONTROL_TYPE = 'BRANCH' AND BRANCH_ID = ?)
                 OR (CONTROL_TYPE = 'USER' AND USER_ID = ?)
                 OR (CONTROL_TYPE = 'IP' AND SERVICE_NAME = ? AND ? <> '')
                 OR (CONTROL_TYPE = 'FULL'
                     AND SERVICE_ID = ? AND TRANSACTION_CODE = ? AND UPPER(BUSINESS_CODE) = UPPER(?)
                     AND SERVICE_NAME = ? AND USER_ID = ? AND CHANNEL_ID = ? AND BRANCH_ID = ?)
               )
             FETCH FIRST 1 ROW ONLY
            """;

    private final JdbcTemplate jdbcTemplate;
    private final String globalUnblockSql;
    private final String findBlockingRuleSql;

    public JdbcTransactionControlRepository(JdbcTemplate jdbcTemplate, TcfProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        String tableName = validateTableName(properties.getTransactionControlTableName());
        this.globalUnblockSql = GLOBAL_UNBLOCK_SQL_TEMPLATE.formatted(tableName);
        this.findBlockingRuleSql = FIND_BLOCKING_RULE_SQL_TEMPLATE.formatted(tableName);
    }

    @Override
    public boolean isGlobalUnblockActive() {
        List<String> blockFlags = jdbcTemplate.query(
                globalUnblockSql,
                (rs, rowNum) -> rs.getString("BLOCK_YN"),
                TcfTransactionControlConstants.CONTROL_TYPE_GLOBAL,
                TcfTransactionControlConstants.BLOCK_NO);
        return !blockFlags.isEmpty();
    }

    @Override
    public Optional<TransactionControlRule> findRule(TransactionControlHeader header) {
        String serviceId = nvl(header.getServiceId());
        if (TransactionControlExemptions.isExempt(serviceId)) {
            return Optional.empty();
        }
        String transactionCode = nvl(header.getTransactionCode());
        String businessCode = nvl(header.getBusinessCode());
        String serviceName = nvl(header.getServiceName());
        String userId = nvl(header.getUser());
        String channelId = nvl(header.getChannelId());
        String branchId = nvl(header.getBranch());
        String clientIp = nvl(header.getClientIp());

        List<TransactionControlRule> rules = jdbcTemplate.query(
                findBlockingRuleSql,
                (rs, rowNum) -> new TransactionControlRule(
                        rs.getString("CONTROL_TYPE"),
                        rs.getString("BLOCK_YN")),
                businessCode,
                serviceId,
                channelId,
                branchId,
                userId,
                clientIp, clientIp,
                serviceId, transactionCode, businessCode, serviceName, userId, channelId, branchId);

        if (rules.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rules.get(0));
    }

    private static String nvl(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    static String validateTableName(String tableName) {
        if (!StringUtils.hasText(tableName) || !tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid transaction control table name: " + tableName);
        }
        return tableName.toUpperCase();
    }
}
