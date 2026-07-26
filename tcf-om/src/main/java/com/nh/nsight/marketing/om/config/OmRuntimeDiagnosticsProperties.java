package com.nh.nsight.marketing.om.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.om.runtime-diagnostics")
public class OmRuntimeDiagnosticsProperties {
    private boolean enabled = true;
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 8000;
    /** 동일 Tomcat에 동시 HTTP 조회 수 (과부하·Read timed out 방지) */
    private int maxParallelRequests = 4;
    private final List<Target> targets = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxParallelRequests() {
        return maxParallelRequests;
    }

    public void setMaxParallelRequests(int maxParallelRequests) {
        this.maxParallelRequests = maxParallelRequests;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public static class Target {
        private String businessCode;
        private String baseUrl;
        private boolean enabled = true;

        public String getBusinessCode() {
            return businessCode;
        }

        public void setBusinessCode(String businessCode) {
            this.businessCode = businessCode;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
