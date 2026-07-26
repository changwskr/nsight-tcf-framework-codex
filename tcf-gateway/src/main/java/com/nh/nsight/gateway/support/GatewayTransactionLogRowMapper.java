package com.nh.nsight.gateway.support;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;

public final class GatewayTransactionLogRowMapper {
    private static final RowMapper<Map<String, Object>> INSTANCE = (rs, rowNum) -> mapRow(rs);

    private GatewayTransactionLogRowMapper() {
    }

    public static RowMapper<Map<String, Object>> instance() {
        return INSTANCE;
    }

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("logId", rs.getString(1));
        row.put("txTime", rs.getString(2));
        row.put("envCode", rs.getString(3));
        row.put("businessCode", rs.getString(4));
        row.put("serviceId", rs.getString(5));
        row.put("transactionCode", rs.getString(6));
        row.put("guid", rs.getString(7));
        row.put("traceId", rs.getString(8));
        row.put("userId", rs.getString(9));
        row.put("branchId", rs.getString(10));
        row.put("sessionId", rs.getString(11));
        row.put("targetUrl", rs.getString(12));
        row.put("httpStatus", rs.getObject(13) == null ? null : rs.getInt(13));
        row.put("resultStatus", rs.getString(14));
        row.put("resultCode", rs.getString(15));
        row.put("errorCode", rs.getString(16));
        row.put("elapsedTimeMs", rs.getObject(17) == null ? null : rs.getLong(17));
        row.put("phase", rs.getString(18));
        return row;
    }
}
