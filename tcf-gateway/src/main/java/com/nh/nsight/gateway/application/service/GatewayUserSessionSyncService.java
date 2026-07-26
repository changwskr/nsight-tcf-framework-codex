package com.nh.nsight.gateway.application.service;

import com.nh.nsight.gateway.config.GatewayProperties;
import com.nh.nsight.gateway.persistence.dao.SpringSessionDao;
import com.nh.nsight.gateway.persistence.dao.UserSessionDao;
import com.nh.nsight.gateway.support.SpringSessionRow;
import com.nh.nsight.gateway.support.UserSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SPRING_SESSION 기준으로 TCF_USER_SESSION ACTIVE 행을 동기화·만료 처리합니다.
 */
@Service
public class GatewayUserSessionSyncService {
    private static final Logger log = LoggerFactory.getLogger(GatewayUserSessionSyncService.class);

    static final String REASON_SPRING_NOT_FOUND = "SPRING_SESSION not found";
    static final String REASON_SPRING_EXPIRED = "SPRING_SESSION expired";
    static final String REASON_ABSOLUTE_EXPIRED = "ABSOLUTE_EXPIRE_TIME elapsed";

    private final GatewayProperties properties;
    private final UserSessionDao userSessionDao;
    private final SpringSessionDao springSessionDao;

    public GatewayUserSessionSyncService(GatewayProperties properties,
                                         UserSessionDao userSessionDao,
                                         SpringSessionDao springSessionDao) {
        this.properties = properties;
        this.userSessionDao = userSessionDao;
        this.springSessionDao = springSessionDao;
    }

    public SyncResult syncActiveSessions() {
        if (!properties.getUserSessionSync().isEnabled()) {
            return SyncResult.skipped();
        }

        long started = System.currentTimeMillis();
        List<UserSession> activeSessions = userSessionDao.findAllActive();
        long now = System.currentTimeMillis();
        Instant nowInstant = Instant.ofEpochMilli(now);

        int scanned = activeSessions.size();
        int expired = 0;
        int synced = 0;

        boolean springAvailable = springSessionDao.isAvailable();
        for (UserSession session : activeSessions) {
            if (springAvailable) {
                Optional<SpringSessionRow> spring = springSessionDao.findBySessionId(session.sessionId());
                if (spring.isEmpty()) {
                    expired += expire(session.sessionId(), REASON_SPRING_NOT_FOUND);
                    continue;
                }
                SpringSessionRow row = spring.get();
                if (!row.isActive(now)) {
                    expired += expire(session.sessionId(), REASON_SPRING_EXPIRED);
                    continue;
                }
                synced += userSessionDao.syncActiveFromSpring(
                        session.sessionId(),
                        Instant.ofEpochMilli(row.lastAccessTime()),
                        row.expiryInstant());
                continue;
            }
            if (!session.isActive(nowInstant)) {
                expired += expire(session.sessionId(), REASON_ABSOLUTE_EXPIRED);
            }
        }

        SyncResult result = new SyncResult(scanned, expired, synced, System.currentTimeMillis() - started);
        if (expired > 0 || log.isDebugEnabled()) {
            log.info("TCF_USER_SESSION sync scanned={} expired={} synced={} elapsedMs={}",
                    result.scanned(), result.expired(), result.synced(), result.elapsedMs());
        }
        return result;
    }

    private int expire(String sessionId, String reason) {
        int updated = userSessionDao.markExpired(sessionId, reason);
        if (updated > 0) {
            log.debug("TCF_USER_SESSION expired sessionId={} reason={}", sessionId, reason);
        }
        return updated;
    }

    public record SyncResult(int scanned, int expired, int synced, long elapsedMs) {
        static SyncResult skipped() {
            return new SyncResult(0, 0, 0, 0);
        }
    }
}
