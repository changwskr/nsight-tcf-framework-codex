package com.nh.nsight.gateway.persistence.dao;

import com.nh.nsight.gateway.support.SessionStatus;
import com.nh.nsight.gateway.support.SessionType;
import com.nh.nsight.gateway.support.UserSession;
import com.nh.nsight.gateway.support.UserSessionViewMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserSessionDao {
    private static final String SELECT_COLUMNS = """
            SESSION_ID, USER_ID, USER_NAME, BRANCH_ID, CHANNEL_ID, AUTH_GROUP_ID, SESSION_TYPE,
            LOGIN_TIME, LAST_ACCESS_TIME, ABSOLUTE_EXPIRE_TIME, CLIENT_IP, USER_AGENT,
            CENTER_ID, WAS_ID, STATUS, LOGOUT_TIME, LOGOUT_REASON
            """;

    private final JdbcTemplate jdbcTemplate;

    public UserSessionDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserSession> findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT %s FROM TCF_USER_SESSION WHERE SESSION_ID = ?".formatted(SELECT_COLUMNS);
        List<UserSession> rows = jdbcTemplate.query(sql, ROW_MAPPER, sessionId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public void upsert(UserSession session) {
        if (findBySessionId(session.sessionId()).isPresent()) {
            jdbcTemplate.update("""
                    UPDATE TCF_USER_SESSION SET
                        USER_ID = ?, USER_NAME = ?, BRANCH_ID = ?, CHANNEL_ID = ?, AUTH_GROUP_ID = ?,
                        SESSION_TYPE = ?, LOGIN_TIME = ?, LAST_ACCESS_TIME = ?, ABSOLUTE_EXPIRE_TIME = ?,
                        CLIENT_IP = ?, USER_AGENT = ?, CENTER_ID = ?, WAS_ID = ?, STATUS = ?,
                        LOGOUT_TIME = ?, LOGOUT_REASON = ?, UPDATED_AT = CURRENT_TIMESTAMP
                    WHERE SESSION_ID = ?
                    """,
                    session.userId(), session.userName(), session.branchId(), session.channelId(),
                    session.authGroupId(), session.sessionType().name(),
                    timestamp(session.loginTime()), timestamp(session.lastAccessTime()),
                    timestamp(session.absoluteExpireTime()), session.clientIp(), session.userAgent(),
                    session.centerId(), session.wasId(), session.status().name(),
                    timestamp(session.logoutTime()), session.logoutReason(), session.sessionId());
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO TCF_USER_SESSION (
                    SESSION_ID, USER_ID, USER_NAME, BRANCH_ID, CHANNEL_ID, AUTH_GROUP_ID, SESSION_TYPE,
                    LOGIN_TIME, LAST_ACCESS_TIME, ABSOLUTE_EXPIRE_TIME, CLIENT_IP, USER_AGENT,
                    CENTER_ID, WAS_ID, STATUS, LOGOUT_TIME, LOGOUT_REASON, CREATED_AT, UPDATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                session.sessionId(), session.userId(), session.userName(), session.branchId(),
                session.channelId(), session.authGroupId(), session.sessionType().name(),
                timestamp(session.loginTime()), timestamp(session.lastAccessTime()),
                timestamp(session.absoluteExpireTime()), session.clientIp(), session.userAgent(),
                session.centerId(), session.wasId(), session.status().name(),
                timestamp(session.logoutTime()), session.logoutReason());
    }

    public void touchLastAccess(String sessionId, Instant lastAccessTime) {
        jdbcTemplate.update("""
                UPDATE TCF_USER_SESSION
                   SET LAST_ACCESS_TIME = ?, UPDATED_AT = CURRENT_TIMESTAMP
                 WHERE SESSION_ID = ?
                """, timestamp(lastAccessTime), sessionId);
    }

    public List<UserSession> search(String userId, boolean activeOnly, int offset, int pageSize) {
        WhereClause where = buildSearchWhere(userId, activeOnly);
        String sql = """
                SELECT SESSION_ID, USER_ID, USER_NAME, BRANCH_ID, CHANNEL_ID, AUTH_GROUP_ID, SESSION_TYPE,
                       LOGIN_TIME, LAST_ACCESS_TIME, ABSOLUTE_EXPIRE_TIME, CLIENT_IP, USER_AGENT,
                       CENTER_ID, WAS_ID, STATUS, LOGOUT_TIME, LOGOUT_REASON
                  FROM TCF_USER_SESSION
                """ + where.sql + """
                 ORDER BY LAST_ACCESS_TIME DESC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        List<Object> params = new ArrayList<>(where.params);
        params.add(offset);
        params.add(pageSize);
        return jdbcTemplate.query(sql, UserSessionViewMapper.entityMapper(), params.toArray());
    }

    public int count(String userId, boolean activeOnly) {
        WhereClause where = buildSearchWhere(userId, activeOnly);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM TCF_USER_SESSION" + where.sql,
                Integer.class,
                where.params.toArray());
        return count == null ? 0 : count;
    }

    public int countActive() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM TCF_USER_SESSION
                 WHERE STATUS = 'ACTIVE' AND ABSOLUTE_EXPIRE_TIME > CURRENT_TIMESTAMP
                """, Integer.class);
        return count == null ? 0 : count;
    }

    public int forceLogout(String sessionId, String reason) {
        return jdbcTemplate.update("""
                UPDATE TCF_USER_SESSION
                   SET STATUS = 'FORCED_LOGOUT',
                       LOGOUT_TIME = CURRENT_TIMESTAMP,
                       LOGOUT_REASON = ?,
                       UPDATED_AT = CURRENT_TIMESTAMP
                 WHERE SESSION_ID = ?
                   AND STATUS = 'ACTIVE'
                """, reason, sessionId);
    }

    public List<UserSession> findAllActive() {
        String sql = """
                SELECT SESSION_ID, USER_ID, USER_NAME, BRANCH_ID, CHANNEL_ID, AUTH_GROUP_ID, SESSION_TYPE,
                       LOGIN_TIME, LAST_ACCESS_TIME, ABSOLUTE_EXPIRE_TIME, CLIENT_IP, USER_AGENT,
                       CENTER_ID, WAS_ID, STATUS, LOGOUT_TIME, LOGOUT_REASON
                  FROM TCF_USER_SESSION
                 WHERE STATUS = 'ACTIVE'
                 ORDER BY LAST_ACCESS_TIME DESC
                """;
        return jdbcTemplate.query(sql, UserSessionViewMapper.entityMapper());
    }

    public int markExpired(String sessionId, String reason) {
        return jdbcTemplate.update("""
                UPDATE TCF_USER_SESSION
                   SET STATUS = 'EXPIRED',
                       LOGOUT_TIME = CURRENT_TIMESTAMP,
                       LOGOUT_REASON = ?,
                       UPDATED_AT = CURRENT_TIMESTAMP
                 WHERE SESSION_ID = ?
                   AND STATUS = 'ACTIVE'
                """, reason, sessionId);
    }

    public int syncActiveFromSpring(String sessionId, Instant lastAccessTime, Instant absoluteExpireTime) {
        return jdbcTemplate.update("""
                UPDATE TCF_USER_SESSION
                   SET LAST_ACCESS_TIME = ?,
                       ABSOLUTE_EXPIRE_TIME = ?,
                       UPDATED_AT = CURRENT_TIMESTAMP
                 WHERE SESSION_ID = ?
                   AND STATUS = 'ACTIVE'
                """, timestamp(lastAccessTime), timestamp(absoluteExpireTime), sessionId);
    }

    private WhereClause buildSearchWhere(String userId, boolean activeOnly) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (userId != null && !userId.isBlank()) {
            sql.append(" AND USER_ID = ? ");
            params.add(userId.trim());
        }
        if (activeOnly) {
            sql.append(" AND STATUS = 'ACTIVE' AND ABSOLUTE_EXPIRE_TIME > CURRENT_TIMESTAMP ");
        }
        return new WhereClause(sql.toString(), params);
    }

    private record WhereClause(String sql, List<Object> params) {
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static final RowMapper<UserSession> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

    private static UserSession mapRow(ResultSet rs) throws SQLException {
        return new UserSession(
                rs.getString("SESSION_ID"),
                rs.getString("USER_ID"),
                rs.getString("USER_NAME"),
                rs.getString("BRANCH_ID"),
                rs.getString("CHANNEL_ID"),
                rs.getString("AUTH_GROUP_ID"),
                SessionType.fromDb(rs.getString("SESSION_TYPE")),
                toInstant(rs.getTimestamp("LOGIN_TIME")),
                toInstant(rs.getTimestamp("LAST_ACCESS_TIME")),
                toInstant(rs.getTimestamp("ABSOLUTE_EXPIRE_TIME")),
                rs.getString("CLIENT_IP"),
                rs.getString("USER_AGENT"),
                rs.getString("CENTER_ID"),
                rs.getString("WAS_ID"),
                SessionStatus.fromDb(rs.getString("STATUS")),
                toInstant(rs.getTimestamp("LOGOUT_TIME")),
                rs.getString("LOGOUT_REASON"));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
