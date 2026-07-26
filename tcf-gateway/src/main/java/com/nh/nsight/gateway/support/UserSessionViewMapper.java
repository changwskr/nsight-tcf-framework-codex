package com.nh.nsight.gateway.support;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;

public final class UserSessionViewMapper {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private UserSessionViewMapper() {
    }

    public static Map<String, Object> toView(UserSession session) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sessionId", session.sessionId());
        row.put("userId", session.userId());
        row.put("userName", session.userName());
        row.put("branchId", session.branchId());
        row.put("channelId", session.channelId());
        row.put("authGroupId", session.authGroupId());
        row.put("sessionType", session.sessionType() == null ? null : session.sessionType().name());
        row.put("loginTime", formatInstant(session.loginTime()));
        row.put("lastAccessTime", formatInstant(session.lastAccessTime()));
        row.put("absoluteExpireTime", formatInstant(session.absoluteExpireTime()));
        row.put("clientIp", session.clientIp());
        row.put("userAgent", session.userAgent());
        row.put("centerId", session.centerId());
        row.put("wasId", session.wasId());
        row.put("status", session.status() == null ? null : session.status().name());
        row.put("logoutTime", formatInstant(session.logoutTime()));
        row.put("logoutReason", session.logoutReason());
        row.put("activeYn", session.isActive(Instant.now()) ? "Y" : "N");
        return row;
    }

    public static RowMapper<UserSession> entityMapper() {
        return (rs, rowNum) -> new UserSession(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getString(6),
                SessionType.fromDb(rs.getString(7)),
                toInstant(rs.getTimestamp(8)),
                toInstant(rs.getTimestamp(9)),
                toInstant(rs.getTimestamp(10)),
                rs.getString(11),
                rs.getString(12),
                rs.getString(13),
                rs.getString(14),
                SessionStatus.fromDb(rs.getString(15)),
                toInstant(rs.getTimestamp(16)),
                rs.getString(17));
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO.format(instant.atZone(KST));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
