package com.nh.nsight.tcf.batch.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.batch.ap-status")
public class ApStatusBatchProperties {
    private String jobId = "BAT-BATCH-001";
    private String cron = "0 */5 * * * *";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private List<ApTargetProperties> targets = new ArrayList<>();

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
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

    public List<ApTargetProperties> getTargets() {
        return targets;
    }

    public void setTargets(List<ApTargetProperties> targets) {
        this.targets = targets;
    }

    public static class ApTargetProperties {
        private String apId;
        private String apName;
        private String baseUrl;
        private boolean enabled = true;

        public String getApId() {
            return apId;
        }

        public void setApId(String apId) {
            this.apId = apId;
        }

        public String getApName() {
            return apName;
        }

        public void setApName(String apName) {
            this.apName = apName;
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
