package com.nh.nsight.tcf.uj.entry.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * OM Admin HTML 응답에 캐시 방지 메타·bootstrap 스크립트를 주입합니다.
 * 브라우저에 캐시된 구버전 HTML/JS로 인한 OmAdmin API 누락 오류를 방지합니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class OmAdminHtmlInjectFilter extends OncePerRequestFilter {

    private static final String NO_CACHE_META =
            "<meta http-equiv=\"Cache-Control\" content=\"no-cache, no-store, must-revalidate\">"
                    + "<meta http-equiv=\"Pragma\" content=\"no-cache\">"
                    + "<meta http-equiv=\"Expires\" content=\"0\">";

    private static final String BOOTSTRAP_SCRIPT =
            "<script src=\"%s/_shared/om-admin-bootstrap.js?v=20260626a\"></script>";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isOmAdminScript(uri)) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            filterChain.doFilter(request, response);
            return;
        }
        if (!isOmAdminHtml(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapped);

        byte[] body = wrapped.getContentAsByteArray();
        if (body.length == 0 || !isHtml(wrapped)) {
            wrapped.copyBodyToResponse();
            return;
        }

        String contextPath = request.getContextPath();
        String prefix = contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)
                ? ""
                : (contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath);

        String html = new String(body, StandardCharsets.UTF_8);
        if (!html.contains("om-admin-bootstrap.js")) {
            html = html.replaceFirst("(?i)(<head[^>]*>)", "$1" + NO_CACHE_META);
            html = html.replaceFirst(
                    "(?i)<script[^>]+/_shared/om-admin\\.js",
                    BOOTSTRAP_SCRIPT.formatted(prefix) + "<script src=\"" + prefix + "/_shared/om-admin.js");
        }

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        byte[] rewritten = html.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(rewritten.length);
        response.getOutputStream().write(rewritten);
    }

    private boolean isOmAdminScript(String uri) {
        return uri != null && uri.startsWith("/_shared/om-admin") && uri.endsWith(".js");
    }

    private boolean isOmAdminHtml(String uri) {
        return uri != null && uri.contains("/om/admin/") && uri.endsWith(".html");
    }

    private boolean isHtml(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.toLowerCase().contains("text/html");
    }
}
