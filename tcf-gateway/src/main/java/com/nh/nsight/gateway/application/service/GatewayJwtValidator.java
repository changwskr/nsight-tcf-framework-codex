package com.nh.nsight.gateway.application.service;

import com.nh.nsight.gateway.application.rule.GatewayAuthException;
import com.nh.nsight.gateway.config.GatewayProperties;
import com.nh.nsight.gateway.support.GatewayProxyTrace;
import com.nh.nsight.gateway.support.GatewayRequestUserReader;
import com.nh.nsight.gateway.support.GatewaySessionContext;
import com.nh.nsight.gateway.application.rule.GatewaySessionHeaderRules;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "nsight.gateway.auth.jwt", name = "enabled", havingValue = "true")
public class GatewayJwtValidator {
    private static final String PHASE = "GatewayJwtValidator.validate";
    private static final String JWT_LOG = "******* [GW-JWT] ";

    private final GatewayProperties properties;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;
    private final GatewayRequestUserReader requestUserReader;

    public GatewayJwtValidator(GatewayProperties properties,
            ObjectProvider<JwtDecoder> jwtDecoderProvider,
            GatewayRequestUserReader requestUserReader) {
        this.properties = properties;
        this.jwtDecoderProvider = jwtDecoderProvider;
        this.requestUserReader = requestUserReader;
    }

    public boolean hasBearerToken(String authorizationHeader) {
        return extractBearerToken(authorizationHeader) != null;
    }

    public GatewaySessionContext validate(String authorizationHeader, String requestBody) {
        GatewayProxyTrace.start(PHASE);
        try {
            String token = extractBearerToken(authorizationHeader);
            System.out.println(JWT_LOG + "validate start token=" + maskToken(token));
            if (!StringUtils.hasText(token)) {
                System.out.println(JWT_LOG + "validate fail: Bearer token empty");
                throw new GatewayAuthException(401, "Authorization Bearer 토큰이 없습니다.");
            }
            JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
            if (jwtDecoder == null) {
                System.out.println(JWT_LOG + "validate fail: JwtDecoder not configured");
                throw new GatewayAuthException(503, "JWT 검증기가 구성되지 않았습니다.");
            }
            Jwt jwt;
            try {
                jwt = jwtDecoder.decode(token);
            } catch (JwtException e) {
                System.out.println(JWT_LOG + "validate fail: decode error " + e.getMessage());
                throw new GatewayAuthException(401, "JWT 토큰이 유효하지 않습니다.");
            }
            String userId = claimAsString(jwt, "userId");
            if (!StringUtils.hasText(userId)) {
                userId = jwt.getSubject();
            }
            if (!StringUtils.hasText(userId)) {
                System.out.println(JWT_LOG + "validate fail: userId missing in claims");
                throw new GatewayAuthException(401, "JWT에 사용자 정보가 없습니다.");
            }
            String branchId = claimAsString(jwt, "branchId");
            String channelId = claimAsString(jwt, "channelId");
            validateHeaderUser(requestBody, userId);
            String sessionId = StringUtils.hasText(jwt.getId()) ? jwt.getId() : null;
            GatewayProxyTrace.log(PHASE, "pass userId=" + userId + " jti=" + sessionId);
            System.out.println(JWT_LOG + "validate pass userId=" + userId
                    + " branchId=" + branchId
                    + " channelId=" + channelId
                    + " jti=" + sessionId
                    + " iss=" + claimAsString(jwt, "iss")
                    + " aud=" + jwt.getClaim("aud"));
            return new GatewaySessionContext(sessionId, userId, branchId, channelId);
        } finally {
            GatewayProxyTrace.end(PHASE);
        }
    }

    private void validateHeaderUser(String requestBody, String jwtUserId) {
        GatewayProperties.Jwt jwtProps = properties.getAuth().getJwt();
        if (!jwtProps.isHeaderUserStrict()) {
            return;
        }
        Optional<String> headerUserId = requestUserReader.userId(requestBody);
        if (headerUserId.isEmpty() || GatewaySessionHeaderRules.isPlaceholderUserId(headerUserId.get())) {
            return;
        }
        if (!headerUserId.get().equalsIgnoreCase(jwtUserId.trim())) {
            throw new GatewayAuthException(401, "요청 Header 사용자와 JWT 사용자가 일치하지 않습니다.");
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }
        GatewayProperties.Jwt jwt = properties.getAuth().getJwt();
        String prefix = jwt.getTokenPrefix() + " ";
        String trimmed = authorizationHeader.trim();
        if (!trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        String token = trimmed.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "-";
        }
        if (token.length() <= 16) {
            return token.substring(0, Math.min(4, token.length())) + "…";
        }
        return token.substring(0, 8) + "…" + token.substring(token.length() - 6);
    }

    private static String claimAsString(Jwt jwt, String claimName) {
        Object value = jwt.getClaim(claimName);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
