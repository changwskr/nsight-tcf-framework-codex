package com.nh.nsight.gateway.persistence.dao;

import com.nh.nsight.gateway.support.SpringSessionRow;
import com.nh.nsight.gateway.support.GatewaySessionIdResolver;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SpringSessionDao {
    private static final String SELECT_SQL = """
            SELECT SESSION_ID, PRINCIPAL_NAME, CREATION_TIME, LAST_ACCESS_TIME,
                   MAX_INACTIVE_INTERVAL, EXPIRY_TIME
              FROM SPRING_SESSION
             WHERE SESSION_ID = ?
            """;

    private final JdbcTemplate sessionJdbcTemplate;

    public SpringSessionDao(@Autowired(required = false) @Qualifier("sessionJdbcTemplate") JdbcTemplate sessionJdbcTemplate) {
        this.sessionJdbcTemplate = sessionJdbcTemplate;
    }

    public boolean isAvailable() {
        return sessionJdbcTemplate != null;
    }

    public Optional<SpringSessionRow> findBySessionId(String sessionId) {
        if (sessionJdbcTemplate == null || sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        for (String candidate : GatewaySessionIdResolver.lookupCandidates(sessionId)) {
            Optional<SpringSessionRow> found = queryBySessionId(candidate);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public boolean isActive(String sessionId, long nowEpochMillis) {
        return findBySessionId(sessionId)
                .map(row -> row.isActive(nowEpochMillis))
                .orElse(false);
    }

    private Optional<SpringSessionRow> queryBySessionId(String sessionId) {
        List<SpringSessionRow> rows = sessionJdbcTemplate.query(SELECT_SQL, (rs, rowNum) -> new SpringSessionRow(
                rs.getString("SESSION_ID"),
                rs.getString("PRINCIPAL_NAME"),
                rs.getLong("CREATION_TIME"),
                rs.getLong("LAST_ACCESS_TIME"),
                rs.getInt("MAX_INACTIVE_INTERVAL"),
                rs.getLong("EXPIRY_TIME")), sessionId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
