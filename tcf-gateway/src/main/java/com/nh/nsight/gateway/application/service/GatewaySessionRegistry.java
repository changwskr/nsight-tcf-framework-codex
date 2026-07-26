package com.nh.nsight.gateway.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.gateway.support.OmLoginSnapshot;
import com.nh.nsight.gateway.support.GatewayCookieParser;
import com.nh.nsight.gateway.support.GatewayRequestUserReader;
import com.nh.nsight.gateway.support.GatewayProxyTrace;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Gateway는 세션을 소유하지 않습니다.
 * OM 로그인 성공 시 downstream Set-Cookie·응답 body의 SESSIONDB 세션 ID로 {@code TCF_USER_SESSION}만 등록합니다.
 */
@Component
public class GatewaySessionRegistry {
    public static final String OM_AUTH_LOGIN = "OM.Auth.login";

    private final ObjectMapper objectMapper;
    private final GatewayRequestUserReader requestUserReader;
    private final UserSessionService userSessionService;

    public GatewaySessionRegistry(ObjectMapper objectMapper,
                                  GatewayRequestUserReader requestUserReader,
                                  UserSessionService userSessionService) {
        this.objectMapper = objectMapper;
        this.requestUserReader = requestUserReader;
        this.userSessionService = userSessionService;
    }

    public Optional<OmLoginSnapshot> tryRegisterOmLogin(String requestBody,
                                                        String responseBody,
                                                        List<String> setCookies) {
        if (!StringUtils.hasText(requestBody) || !StringUtils.hasText(responseBody)) {
            return Optional.empty();
        }
        if (!OM_AUTH_LOGIN.equals(requestUserReader.serviceId(requestBody).orElse(null))) {
            return Optional.empty();
        }
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
            if (!isSuccess(response)) {
                return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.get("body");
            if (body == null || !Boolean.TRUE.equals(body.get("loggedIn"))) {
                return Optional.empty();
            }
            OmLoginSnapshot snapshot = toSnapshot(body, setCookies);
            if (!StringUtils.hasText(snapshot.sessionId()) || !StringUtils.hasText(snapshot.userId())) {
                GatewayProxyTrace.log("GatewaySessionRegistry.tryRegisterOmLogin",
                        "skip sessionId or userId missing");
                return Optional.empty();
            }
            userSessionService.registerLogin(snapshot, requestBody);
            logRegistered("GatewaySessionRegistry.tryRegisterOmLogin", snapshot);
            return Optional.of(snapshot);
        } catch (Exception e) {
            GatewayProxyTrace.log("GatewaySessionRegistry.tryRegisterOmLogin",
                    "skip reason=" + e.getClass().getSimpleName() + " message=" + e.getMessage());
            return Optional.empty();
        }
    }

    private OmLoginSnapshot toSnapshot(Map<String, Object> body, List<String> setCookies) {
        String sessionId = stringValue(body, "sessionId");
        if (!StringUtils.hasText(sessionId)) {
            sessionId = GatewayCookieParser.sessionIdFromSetCookies(setCookies).orElse(null);
        }
        return new OmLoginSnapshot(
                sessionId,
                stringValue(body, "userId"),
                stringValue(body, "userName"),
                stringValue(body, "branchId"),
                stringValue(body, "authGroupId"),
                stringValue(body, "authGroupName"));
    }

    private void logRegistered(String phase, OmLoginSnapshot snapshot) {
        GatewayProxyTrace.log(phase, "sessionDbRegistered sessionId=" + snapshot.sessionId()
                + " userId=" + snapshot.userId()
                + " userName=" + snapshot.userName()
                + " branchId=" + snapshot.branchId()
                + " authGroupId=" + snapshot.authGroupId());
    }

    private boolean isSuccess(Map<String, Object> response) {
        Object result = response.get("result");
        if (!(result instanceof Map<?, ?> resultMap)) {
            return false;
        }
        return "S0000".equals(String.valueOf(resultMap.get("resultCode")));
    }

    private String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
