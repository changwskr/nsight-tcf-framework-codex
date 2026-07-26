package com.nh.nsight.tcf.web.entry.web;

import com.nh.nsight.tcf.core.support.security.AuthenticationContext;
import com.nh.nsight.tcf.core.support.security.AuthenticationContextHolder;
import com.nh.nsight.tcf.web.config.TcfWebJwtProperties;
import com.nh.nsight.tcf.web.support.AuthenticatedUserContext;
import com.nh.nsight.tcf.web.support.AuthenticatedUserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(prefix = "nsight.tcf.web.jwt", name = "enabled", havingValue = "true")
public class TcfJwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String REQ_ATTR_AUTH_CONTEXT = "nsight.authenticatedUserContext";
    private static final String LOG_PREFIX = "******* [TCF-WEB-JWT] ";

    private final TcfWebJwtProperties jwtProperties;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;

    public TcfJwtAuthenticationFilter(
            TcfWebJwtProperties jwtProperties,
            ObjectProvider<JwtDecoder> jwtDecoderProvider) {
        this.jwtProperties = jwtProperties;
        this.jwtDecoderProvider = jwtDecoderProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        if (uri.startsWith("/actuator") || uri.endsWith("/health") || uri.startsWith("/internal/runtime")) {
            return true;
        }
        return !uri.endsWith("/online");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(jwtProperties.getHeaderName());
        String token = extractBearerToken(authorization);
        if (!StringUtils.hasText(token)) {
            if (jwtProperties.isRequiredForOnline()) {
                unauthorized(response, "E-JWT-AUTH-0001", "Authorization Bearer 토큰이 없습니다.");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder == null) {
            unauthorized(response, "E-JWT-JWKS-0001", "JWT 검증기가 구성되지 않았습니다.");
            return;
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String userId = claimAsString(jwt, "userId");
            if (!StringUtils.hasText(userId)) {
                userId = jwt.getSubject();
            }
            if (!StringUtils.hasText(userId)) {
                unauthorized(response, "E-JWT-AUTH-0008", "JWT에 사용자 식별 claim이 없습니다.");
                return;
            }
            AuthenticatedUserContext context = new AuthenticatedUserContext(
                    userId,
                    claimAsString(jwt, "branchId"),
                    claimAsString(jwt, "channelId"),
                    StringUtils.hasText(jwt.getId()) ? jwt.getId() : null,
                    jwt);
            AuthenticationContext coreContext = new AuthenticationContext(
                    context.userId(),
                    context.branchId(),
                    context.channelId(),
                    context.jti());
            request.setAttribute(REQ_ATTR_AUTH_CONTEXT, context);
            AuthenticatedUserContextHolder.set(context);
            AuthenticationContextHolder.set(coreContext);
            System.out.println(LOG_PREFIX + "validate pass userId=" + userId
                    + " branchId=" + context.branchId()
                    + " channelId=" + context.channelId()
                    + " jti=" + context.jti());
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            System.out.println(LOG_PREFIX + "validate fail: " + e.getMessage());
            unauthorized(response, "E-JWT-AUTH-0004", "JWT 토큰이 유효하지 않습니다.");
        } finally {
            AuthenticatedUserContextHolder.clear();
            AuthenticationContextHolder.clear();
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }
        String prefix = jwtProperties.getTokenPrefix() + " ";
        String trimmed = authorizationHeader.trim();
        if (!trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        String token = trimmed.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static String claimAsString(Jwt jwt, String claimName) {
        Object value = jwt.getClaim(claimName);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private void unauthorized(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"errorCode\":\"" + code + "\",\"message\":\"" + message + "\"}";
        response.getWriter().write(body);
    }
}
