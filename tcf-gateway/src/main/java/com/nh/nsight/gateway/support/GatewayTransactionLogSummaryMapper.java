package com.nh.nsight.gateway.support;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;

public final class GatewayTransactionLogSummaryMapper {
    private static final RowMapper<Map<String, Object>> INSTANCE = (rs, rowNum) -> mapRow(rs);

    private GatewayTransactionLogSummaryMapper() {
    }

    public static RowMapper<Map<String, Object>> instance() {
        return INSTANCE;
    }

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("totalCount", rs.getLong(1));
        row.put("successCount", rs.getLong(2));
        row.put("errorCount", rs.getLong(3));
        row.put("timeoutCount", rs.getLong(4));
        row.put("avgElapsedMs", rs.getDouble(5));
        return row;
    }
}
