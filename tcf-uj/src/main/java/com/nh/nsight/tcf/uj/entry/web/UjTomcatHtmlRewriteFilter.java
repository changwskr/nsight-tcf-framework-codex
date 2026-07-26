package com.nh.nsight.tcf.uj.entry.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * uj.war (/uj) 배포 시 HTML 내 절대 경로에 context 접두사를 붙이고 uj-context.js를 주입합니다.
 */
@Component
public class UjTomcatHtmlRewriteFilter extends OncePerRequestFilter {

    private static final String UI_CONTEXT_SCRIPT = "<script src=\"%s/_shared/uj-context.js\"></script>";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)) {
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

        String prefix = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
        String html = new String(body, StandardCharsets.UTF_8);
        html = html.replace("href=\"/", "href=\"" + prefix + "/");
        html = html.replace("src=\"/", "src=\"" + prefix + "/");
        html = html.replace("content=\"0; url=/", "content=\"0; url=" + prefix + "/");
        html = html.replace("url(/", "url(" + prefix + "/");
        html = html.replace("url='/", "url='" + prefix + "/");
        html = html.replace("url=\"/", "url=\"" + prefix + "/");
        if (!html.contains("uj-context.js")) {
            html = html.replaceFirst("(?i)(<head[^>]*>)", "$1" + UI_CONTEXT_SCRIPT.formatted(prefix));
        }

        byte[] rewritten = html.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(rewritten.length);
        response.getOutputStream().write(rewritten);
    }

    private boolean isHtml(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.toLowerCase().contains("text/html");
    }
}
