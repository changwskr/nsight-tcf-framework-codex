package com.nh.nsight.gateway.application.service;

import com.nh.nsight.gateway.config.GatewayProperties;
import com.nh.nsight.gateway.application.rule.GatewayAuthException;
import com.nh.nsight.gateway.application.rule.GatewayAuthExemptions;
import com.nh.nsight.gateway.persistence.dao.SpringSessionDao;
import com.nh.nsight.gateway.support.GatewaySessionContext;
import com.nh.nsight.gateway.support.SessionStatus;
import com.nh.nsight.gateway.support.SessionType;
import com.nh.nsight.gateway.support.UserSession;
import com.nh.nsight.gateway.support.GatewayCookieParser;
import com.nh.nsight.gateway.support.GatewayRequestUserReader;
import com.nh.nsight.gateway.application.rule.GatewaySessionHeaderRules;
import com.nh.nsight.gateway.support.GatewaySessionIdResolver;
import com.nh.nsight.gateway.support.GatewayProxyTrace;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Gateway 세션 관문 — SESSIONDB(SPRING_SESSION + TCF_USER_SESSION)만 검증합니다.
 * Gateway는 HttpSession을 생성·보유하지 않습니다.
 */
@Service
public class GatewaySessionValidationService {
    private static final String PHASE = "GatewaySessionValidationService.validate";

    private final GatewayProperties properties;
    private final SpringSessionDao springSessionDao;
    private final UserSessionService userSessionService;
    private final GatewayRequestUserReader requestUserReader;

    public GatewaySessionValidationService(GatewayProperties properties,
                                           SpringSessionDao springSessionDao,
                                           UserSessionService userSessionService,
                                           GatewayRequestUserReader requestUserReader) {
        this.properties = properties;
        this.springSessionDao = springSessionDao;
        this.userSessionService = userSessionService;
        this.requestUserReader = requestUserReader;
    }

    public GatewaySessionContext validate(String businessCode, String cookieHeader, String requestBody) {
        GatewayProxyTrace.start(PHASE);
        try {
            if (!properties.getAuth().isLoginRequired()) {
                GatewayProxyTrace.log(PHASE, "login check skipped");
                return null;
            }
            String serviceId = requestUserReader.serviceId(requestBody).orElse(null);
            if (GatewayAuthExemptions.isLoginExempt(serviceId)) {
                GatewayProxyTrace.log(PHASE, "login exempt serviceId=" + serviceId);
                return null;
            }

            GatewayProperties.SessionValidation validation = properties.getAuth().getSessionValidation();
            Optional<String> cookieSessionId = GatewayCookieParser.sessionId(cookieHeader);
            String sessionId = cookieSessionId.orElse(null);

            validateStage1(cookieSessionId);
            validateStage2(validation, sessionId);
            UserSession userSession = validateStage3(validation, businessCode, sessionId, requestBody);
            validateStage4(validation, requestBody, userSession, sessionId);

            if (validation.isUserSessionCheck() && StringUtils.hasText(sessionId)) {
                userSessionService.touchLastAccess(sessionId);
            }
            return toContext(sessionId, userSession, requestBody);
        } finally {
            GatewayProxyTrace.end(PHASE);
        }
    }

    private GatewaySessionContext toContext(String sessionId, UserSession userSession, String requestBody) {
        if (userSession != null) {
            return new GatewaySessionContext(
                    sessionId,
                    userSession.userId(),
                    userSession.branchId(),
                    userSession.channelId() != null ? userSession.channelId()
                            : requestUserReader.channelId(requestBody).orElse(null));
        }
        if (!StringUtils.hasText(sessionId) || !springSessionDao.isAvailable()) {
            return null;
        }
        return springSessionDao.findBySessionId(sessionId)
                .map(row -> new GatewaySessionContext(
                        row.sessionId(),
                        row.userId(),
                        null,
                        requestUserReader.channelId(requestBody).orElse(null)))
                .orElse(null);
    }

    private void validateStage1(Optional<String> sessionId) {
        GatewayProxyTrace.log(PHASE, "stage1 cookieCheck");
        if (sessionId.isPresent()) {
            GatewayProxyTrace.log(PHASE, "stage1 pass sessionId=" + sessionId.get());
            return;
        }
        throw new GatewayAuthException(401, "세션 쿠키(JSESSIONID/NSIGHTSID)가 없습니다.");
    }

    private void validateStage2(GatewayProperties.SessionValidation validation, String sessionId) {
        if (!validation.isSpringSessionCheck()) {
            GatewayProxyTrace.log(PHASE, "stage2 skipped");
            return;
        }
        GatewayProxyTrace.log(PHASE, "stage2 springSessionCheck sessionId=" + sessionId
                + " candidates=" + GatewaySessionIdResolver.lookupCandidates(sessionId));
        if (!StringUtils.hasText(sessionId)) {
            throw new GatewayAuthException(401, "세션 ID를 확인할 수 없습니다.");
        }
        if (!springSessionDao.isAvailable()) {
            GatewayProxyTrace.log(PHASE, "stage2 skipped session datasource not configured");
            return;
        }
        long now = System.currentTimeMillis();
        if (!springSessionDao.isActive(sessionId, now)) {
            throw new GatewayAuthException(401, "SPRING_SESSION에 유효한 세션이 없습니다.");
        }
        GatewayProxyTrace.log(PHASE, "stage2 pass");
    }

    private UserSession validateStage3(GatewayProperties.SessionValidation validation,
                                       String businessCode,
                                       String sessionId,
                                       String requestBody) {
        if (!validation.isUserSessionCheck()) {
            GatewayProxyTrace.log(PHASE, "stage3 skipped");
            return null;
        }
        GatewayProxyTrace.log(PHASE, "stage3 userSessionCheck sessionId=" + sessionId);
        if (!StringUtils.hasText(sessionId)) {
            throw new GatewayAuthException(401, "세션 ID를 확인할 수 없습니다.");
        }
        SessionType sessionType = SessionType.fromBusinessCode(businessCode);
        UserSession userSession = userSessionService.findOrSync(sessionId, sessionType, requestBody)
                .orElseThrow(() -> new GatewayAuthException(401, "TCF_USER_SESSION에 세션 정보가 없습니다."));
        Instant now = Instant.now();
        if (userSession.status() == SessionStatus.FORCED_LOGOUT) {
            throw new GatewayAuthException(401, "강제 로그아웃된 세션입니다.");
        }
        if (userSession.status() == SessionStatus.EXPIRED || userSession.status() == SessionStatus.LOGGED_OUT) {
            throw new GatewayAuthException(401, "만료되거나 로그아웃된 세션입니다.");
        }
        if (!userSession.isActive(now)) {
            throw new GatewayAuthException(401, "세션이 만료되었습니다.");
        }
        GatewayProxyTrace.log(PHASE, "stage3 pass userId=" + userSession.userId() + " status=" + userSession.status());
        return userSession;
    }

    private void validateStage4(GatewayProperties.SessionValidation validation,
                                String requestBody,
                                UserSession userSession,
                                String sessionId) {
        if (!validation.isHeaderUserCheck()) {
            GatewayProxyTrace.log(PHASE, "stage4 skipped");
            return;
        }
        Optional<String> headerUserId = requestUserReader.userId(requestBody);
        if (headerUserId.isEmpty() || GatewaySessionHeaderRules.isPlaceholderUserId(headerUserId.get())) {
            GatewayProxyTrace.log(PHASE, "stage4 skipped header.userId placeholder");
            return;
        }
        String sessionUserId = resolveSessionUserId(userSession, sessionId);
        if (!StringUtils.hasText(sessionUserId)) {
            GatewayProxyTrace.log(PHASE, "stage4 skipped session user unknown");
            return;
        }
        GatewayProxyTrace.log(PHASE, "stage4 headerUserCheck headerUserId=" + headerUserId.get()
                + " sessionUserId=" + sessionUserId);
        if (headerUserId.get().equalsIgnoreCase(sessionUserId.trim())) {
            GatewayProxyTrace.log(PHASE, "stage4 pass");
            return;
        }
        if (validation.isHeaderUserStrict()) {
            throw new GatewayAuthException(401, "요청 Header 사용자와 세션 사용자가 일치하지 않습니다.");
        }
        GatewayProxyTrace.log(PHASE, "stage4 mismatch will be corrected by session enricher");
    }

    private String resolveSessionUserId(UserSession userSession, String sessionId) {
        if (userSession != null && StringUtils.hasText(userSession.userId())) {
            return userSession.userId();
        }
        if (StringUtils.hasText(sessionId) && springSessionDao.isAvailable()) {
            return springSessionDao.findBySessionId(sessionId)
                    .map(row -> row.userId())
                    .orElse(null);
        }
        return null;
    }
}
