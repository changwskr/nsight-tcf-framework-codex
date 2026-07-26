package com.nh.nsight.tcf.batch.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.batch.deploy-status")
public class DeployStatusBatchProperties {
    private String jobId = "BAT-BATCH-004";
    private String cron = "55 */5 * * * *";
    private String defaultVersion = "1.0.0-SNAPSHOT";
    private List<DeployTargetProperties> targets = new ArrayList<>();

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

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public List<DeployTargetProperties> getTargets() {
        return targets;
    }

    public void setTargets(List<DeployTargetProperties> targets) {
        this.targets = targets;
    }

    public static class DeployTargetProperties {
        private String businessCode;
        private String warName;
        private String defaultVersion;
        private String baseUrl;
        private String sourceType = "actuator";
        private boolean enabled = true;

        public String getBusinessCode() {
            return businessCode;
        }

        public void setBusinessCode(String businessCode) {
            this.businessCode = businessCode;
        }

        public String getWarName() {
            return warName;
        }

        public void setWarName(String warName) {
            this.warName = warName;
        }

        public String getDefaultVersion() {
            return defaultVersion;
        }

        public void setDefaultVersion(String defaultVersion) {
            this.defaultVersion = defaultVersion;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
