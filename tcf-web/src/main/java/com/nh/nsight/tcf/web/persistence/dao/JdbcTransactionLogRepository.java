package com.nh.nsight.tcf.web.persistence.dao;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.logging.TransactionLogRecord;
import com.nh.nsight.tcf.core.support.logging.TransactionLogRepository;
import java.sql.PreparedStatement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * 프레임워크 공통 트랜잭션 로그 테이블({@link com.nh.nsight.tcf.core.support.logging.TcfTransactionLogConstants#TABLE_NAME})에 INSERT.
 * 업무 DataSource가 {@code auto-commit: false}여도 로그만 즉시 커밋한다.
 */
public class JdbcTransactionLogRepository implements TransactionLogRepository {

    private static final String INSERT_SQL_TEMPLATE = """
            INSERT INTO %s (
                LOG_ID, TX_TIME, BUSINESS_CODE, SERVICE_ID, TRANSACTION_CODE,
                GUID, TRACE_ID, USER_ID, BRANCH_ID,
                RESULT_STATUS, RESULT_CODE, ERROR_CODE, ELAPSED_TIME_MS
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final String insertSql;

    public JdbcTransactionLogRepository(JdbcTemplate jdbcTemplate, TcfProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.insertSql = INSERT_SQL_TEMPLATE.formatted(validateTableName(properties.getTransactionLogTableName()));
    }

    @Override
    public void save(TransactionLogRecord record) {
        jdbcTemplate.execute((java.sql.Connection connection) -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(true);
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    bind(ps, record);
                    ps.executeUpdate();
                }
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
            return null;
        });
    }

    private void bind(PreparedStatement ps, TransactionLogRecord record) throws java.sql.SQLException {
        ps.setString(1, record.logId());
        ps.setString(2, record.txTime());
        ps.setString(3, record.businessCode());
        ps.setString(4, record.serviceId());
        ps.setString(5, record.transactionCode());
        ps.setString(6, record.guid());
        ps.setString(7, record.traceId());
        ps.setString(8, record.userId());
        ps.setString(9, record.branchId());
        ps.setString(10, record.resultStatus());
        ps.setString(11, record.resultCode());
        ps.setString(12, record.errorCode());
        ps.setLong(13, record.elapsedTimeMs());
    }

    static String validateTableName(String tableName) {
        if (!StringUtils.hasText(tableName) || !tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid transaction log table name: " + tableName);
        }
        return tableName.toUpperCase();
    }
}
