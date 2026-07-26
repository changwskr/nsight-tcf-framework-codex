package com.nh.nsight.gateway.application.service;

import com.nh.nsight.gateway.persistence.dao.SpringSessionDao;

import com.nh.nsight.gateway.persistence.dao.UserSessionDao;

import com.nh.nsight.gateway.support.OmLoginSnapshot;

import com.nh.nsight.gateway.support.SessionStatus;

import com.nh.nsight.gateway.support.SessionType;

import com.nh.nsight.gateway.support.SpringSessionRow;

import com.nh.nsight.gateway.support.UserSession;

import com.nh.nsight.gateway.support.GatewayRequestUserReader;
import com.nh.nsight.gateway.support.GatewaySessionIdResolver;

import com.nh.nsight.gateway.support.GatewayProxyTrace;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import org.springframework.web.context.request.RequestContextHolder;

import org.springframework.web.context.request.ServletRequestAttributes;



@Service

public class UserSessionService {

    private final UserSessionDao userSessionDao;

    private final SpringSessionDao springSessionDao;

    private final GatewayRequestUserReader requestUserReader;

    private final String wasId;



    public UserSessionService(UserSessionDao userSessionDao,

                              SpringSessionDao springSessionDao,

                              GatewayRequestUserReader requestUserReader,

                              @Value("${spring.application.name:nsight-gateway}") String applicationName) {

        this.userSessionDao = userSessionDao;

        this.springSessionDao = springSessionDao;

        this.requestUserReader = requestUserReader;

        this.wasId = applicationName;

    }



    /** OM 로그인 성공 시 SESSIONDB(TCF_USER_SESSION)에만 등록 — Gateway HttpSession 미사용 */

    public void registerLogin(OmLoginSnapshot login, String requestBody) {

        if (!StringUtils.hasText(login.sessionId()) || !StringUtils.hasText(login.userId())) {

            return;

        }

        Instant now = Instant.now();

        Instant expireTime = springSessionDao.findBySessionId(login.sessionId())

                .map(SpringSessionRow::expiryInstant)

                .orElse(now.plusSeconds(3600));

        HttpServletRequest request = currentRequest().orElse(null);

        UserSession session = new UserSession(

                login.sessionId(),

                login.userId(),

                login.userName(),

                login.branchId(),

                requestUserReader.channelId(requestBody).orElse(null),

                login.authGroupId(),

                SessionType.OM,

                now,

                now,

                expireTime,

                clientIp(request),

                userAgent(request),

                requestUserReader.centerId(requestBody).orElse(null),

                wasId,

                SessionStatus.ACTIVE,

                null,

                null);

        userSessionDao.upsert(session);

        GatewayProxyTrace.log("UserSessionService.registerLogin",

                "sessionId=" + login.sessionId() + " userId=" + login.userId());

    }



    public Optional<UserSession> findOrSync(String sessionId, SessionType sessionType, String requestBody) {
        for (String candidate : GatewaySessionIdResolver.lookupCandidates(sessionId)) {
            Optional<UserSession> existing = userSessionDao.findBySessionId(candidate);
            if (existing.isPresent()) {
                return existing;
            }
        }
        return springSessionDao.findBySessionId(sessionId)
                .map(row -> syncFromSpringSession(row, sessionType, requestBody));
    }

    public void touchLastAccess(String sessionId) {
        for (String candidate : GatewaySessionIdResolver.lookupCandidates(sessionId)) {
            if (userSessionDao.findBySessionId(candidate).isPresent()) {
                userSessionDao.touchLastAccess(candidate, Instant.now());
                return;
            }
        }
    }



    private UserSession syncFromSpringSession(SpringSessionRow row, SessionType sessionType, String requestBody) {

        HttpServletRequest request = currentRequest().orElse(null);

        UserSession session = new UserSession(

                row.sessionId(),

                row.userId(),

                null,

                null,

                requestUserReader.channelId(requestBody).orElse(null),

                null,

                sessionType,

                Instant.ofEpochMilli(row.creationTime()),

                Instant.ofEpochMilli(row.lastAccessTime()),

                row.expiryInstant(),

                clientIp(request),

                userAgent(request),

                requestUserReader.centerId(requestBody).orElse(null),

                wasId,

                SessionStatus.ACTIVE,

                null,

                null);

        userSessionDao.upsert(session);

        GatewayProxyTrace.log("UserSessionService.syncFromSpringSession",

                "sessionId=" + row.sessionId() + " userId=" + row.userId());

        return session;

    }



    private Optional<HttpServletRequest> currentRequest() {

        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {

            return Optional.empty();

        }

        return Optional.ofNullable(attrs.getRequest());

    }



    private String clientIp(HttpServletRequest request) {

        if (request == null) {

            return null;

        }

        String forwarded = request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(forwarded)) {

            return forwarded.split(",")[0].trim();

        }

        return request.getRemoteAddr();

    }



    private String userAgent(HttpServletRequest request) {

        return request == null ? null : request.getHeader("User-Agent");

    }

}


