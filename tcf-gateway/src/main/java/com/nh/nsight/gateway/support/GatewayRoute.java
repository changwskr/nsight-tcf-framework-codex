package com.nh.nsight.gateway.support;

import org.springframework.util.StringUtils;

public record GatewayRoute(
        String routeId,
        String envCode,
        String routeGroupCode,
        String routeGroupName,
        String businessCode,
        String businessName,
        String targetBaseUrl,
        String contextPath,
        String onlinePath,
        String healthCheckPath,
        int connectTimeoutMs,
        int readTimeoutMs,
        String useYn,
        Integer sortOrder,
        String description
) {
    public String targetUrl() {
        String base = trimTrailingSlash(targetBaseUrl);
        String context = normalizePath(contextPath);
        String online = normalizePath(onlinePath);
        if (!StringUtils.hasText(context)) {
            return base + online;
        }
        return base + context + online;
    }

    public boolean active() {
        return "Y".equalsIgnoreCase(useYn);
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String normalizePath(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.startsWith("/") ? value : "/" + value;
    }
}
