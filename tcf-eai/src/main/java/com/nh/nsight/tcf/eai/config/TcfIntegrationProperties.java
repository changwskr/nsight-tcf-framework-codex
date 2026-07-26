package com.nh.nsight.tcf.eai.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 서비스 간 연동 설정 (prefix: {@code nsight.integration}).
 *
 * <pre>
 * nsight:
 *   integration:
 *     default-timeout-ms: 3000
 *     services:
 *       SV:
 *         base-url: http://127.0.0.1:8086
 *         context-path: /sv
 *         online-path: /online
 *         connect-timeout-ms: 1000
 *         read-timeout-ms: 3000
 * </pre>
 */
@ConfigurationProperties(prefix = "nsight.integration")
public class TcfIntegrationProperties {

    /** 서비스별 read-timeout 미지정 시 기본값(ms). */
    private long defaultTimeoutMs = 3000;

    /** 대상 업무코드 → 연동 대상 설정. key 는 businessCode(예: SV). */
    private Map<String, ServiceEndpoint> services = new LinkedHashMap<>();

    public long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public void setDefaultTimeoutMs(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public Map<String, ServiceEndpoint> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceEndpoint> services) {
        this.services = services;
    }

    /** businessCode 로 대상 endpoint 조회 (대소문자 무시). */
    public ServiceEndpoint findEndpoint(String businessCode) {
        if (businessCode == null) {
            return null;
        }
        ServiceEndpoint endpoint = services.get(businessCode);
        if (endpoint != null) {
            return endpoint;
        }
        return services.get(businessCode.toUpperCase());
    }

    public static class ServiceEndpoint {
        private String baseUrl;
        private String contextPath = "";
        private String onlinePath = "/online";
        private Long connectTimeoutMs;
        private Long readTimeoutMs;

        /** base-url + context-path + online-path 조립. 예: http://127.0.0.1:8086/sv/online */
        public String resolveOnlineUrl() {
            String base = trimTrailingSlash(baseUrl);
            String ctx = normalizeSegment(contextPath);
            String online = normalizeSegment(onlinePath);
            return base + ctx + online;
        }

        private static String trimTrailingSlash(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            String v = value.trim();
            return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
        }

        private static String normalizeSegment(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            String v = value.trim();
            if (!v.startsWith("/")) {
                v = "/" + v;
            }
            if (v.endsWith("/")) {
                v = v.substring(0, v.length() - 1);
            }
            return v;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getContextPath() {
            return contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }

        public String getOnlinePath() {
            return onlinePath;
        }

        public void setOnlinePath(String onlinePath) {
            this.onlinePath = onlinePath;
        }

        public Long getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(Long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public Long getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(Long readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }
}
