package com.nh.nsight.gateway.persistence.dao;

import com.nh.nsight.gateway.support.GatewayRoute;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class GatewayRouteDao {
    private static final String SELECT_COLUMNS = """
            ROUTE_ID, ENV_CODE, ROUTE_GROUP_CODE, ROUTE_GROUP_NAME, BUSINESS_CODE, BUSINESS_NAME,
            TARGET_BASE_URL, CONTEXT_PATH, ONLINE_PATH, HEALTH_CHECK_PATH,
            CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, USE_YN, SORT_ORDER, DESCRIPTION
            """;

    private final JdbcTemplate jdbcTemplate;

    public GatewayRouteDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<GatewayRoute> findActive(String envCode, String businessCode) {
        String sql = """
                SELECT %s FROM TCF_GATEWAY_ROUTE
                WHERE ENV_CODE = ? AND BUSINESS_CODE = ? AND USE_YN = 'Y'
                """.formatted(SELECT_COLUMNS);
        List<GatewayRoute> rows = jdbcTemplate.query(sql, ROW_MAPPER, envCode, businessCode.toUpperCase());
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<GatewayRoute> findByEnv(String envCode) {
        String sql = """
                SELECT %s FROM TCF_GATEWAY_ROUTE
                WHERE ENV_CODE = ?
                ORDER BY SORT_ORDER, BUSINESS_CODE
                """.formatted(SELECT_COLUMNS);
        return jdbcTemplate.query(sql, ROW_MAPPER, envCode);
    }

    public Optional<GatewayRoute> findById(String routeId) {
        String sql = "SELECT %s FROM TCF_GATEWAY_ROUTE WHERE ROUTE_ID = ?".formatted(SELECT_COLUMNS);
        List<GatewayRoute> rows = jdbcTemplate.query(sql, ROW_MAPPER, routeId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public int insert(GatewayRoute route) {
        return jdbcTemplate.update("""
                INSERT INTO TCF_GATEWAY_ROUTE (
                    ROUTE_ID, ENV_CODE, ROUTE_GROUP_CODE, ROUTE_GROUP_NAME, BUSINESS_CODE, BUSINESS_NAME,
                    TARGET_BASE_URL, CONTEXT_PATH, ONLINE_PATH, HEALTH_CHECK_PATH,
                    CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, USE_YN, SORT_ORDER, DESCRIPTION,
                    CREATED_AT, UPDATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                route.routeId(), route.envCode(), route.routeGroupCode(), route.routeGroupName(),
                route.businessCode(), route.businessName(), route.targetBaseUrl(), toDbContextPath(route.contextPath()),
                route.onlinePath(), route.healthCheckPath(), route.connectTimeoutMs(), route.readTimeoutMs(),
                route.useYn(), route.sortOrder(), route.description());
    }

    public int update(GatewayRoute route) {
        return jdbcTemplate.update("""
                UPDATE TCF_GATEWAY_ROUTE SET
                    ENV_CODE = ?, ROUTE_GROUP_CODE = ?, ROUTE_GROUP_NAME = ?,
                    BUSINESS_CODE = ?, BUSINESS_NAME = ?, TARGET_BASE_URL = ?,
                    CONTEXT_PATH = ?, ONLINE_PATH = ?, HEALTH_CHECK_PATH = ?,
                    CONNECT_TIMEOUT_MS = ?, READ_TIMEOUT_MS = ?, USE_YN = ?,
                    SORT_ORDER = ?, DESCRIPTION = ?, UPDATED_AT = CURRENT_TIMESTAMP
                WHERE ROUTE_ID = ?
                """,
                route.envCode(), route.routeGroupCode(), route.routeGroupName(),
                route.businessCode(), route.businessName(), route.targetBaseUrl(),
                toDbContextPath(route.contextPath()), route.onlinePath(), route.healthCheckPath(),
                route.connectTimeoutMs(), route.readTimeoutMs(), route.useYn(),
                route.sortOrder(), route.description(), route.routeId());
    }

    public int delete(String routeId) {
        return jdbcTemplate.update("DELETE FROM TCF_GATEWAY_ROUTE WHERE ROUTE_ID = ?", routeId);
    }

    private static final RowMapper<GatewayRoute> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

    private static GatewayRoute mapRow(ResultSet rs) throws SQLException {
        return new GatewayRoute(
                rs.getString("ROUTE_ID"),
                rs.getString("ENV_CODE"),
                rs.getString("ROUTE_GROUP_CODE"),
                rs.getString("ROUTE_GROUP_NAME"),
                rs.getString("BUSINESS_CODE"),
                rs.getString("BUSINESS_NAME"),
                rs.getString("TARGET_BASE_URL"),
                fromDbContextPath(rs.getString("CONTEXT_PATH")),
                rs.getString("ONLINE_PATH"),
                rs.getString("HEALTH_CHECK_PATH"),
                rs.getInt("CONNECT_TIMEOUT_MS"),
                rs.getInt("READ_TIMEOUT_MS"),
                rs.getString("USE_YN"),
                (Integer) rs.getObject("SORT_ORDER"),
                rs.getString("DESCRIPTION")
        );
    }

    /** H2 Oracle 모드에서는 빈 문자열이 NULL로 저장되므로 공백 한 칸을 사용한다. */
    private static String toDbContextPath(String value) {
        return org.springframework.util.StringUtils.hasText(value) ? value : " ";
    }

    private static String fromDbContextPath(String value) {
        return org.springframework.util.StringUtils.hasText(value) ? value : "";
    }
}
