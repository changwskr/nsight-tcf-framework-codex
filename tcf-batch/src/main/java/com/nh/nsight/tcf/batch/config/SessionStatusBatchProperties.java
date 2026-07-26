package com.nh.nsight.tcf.batch.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.batch.session-status")
public class SessionStatusBatchProperties {
    private String jobId = "BAT-BATCH-003";
    private String cron = "45 */5 * * * *";
    private int warnActiveThreshold = 200;
    private List<SessionTargetProperties> targets = new ArrayList<>();

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

    public int getWarnActiveThreshold() {
        return warnActiveThreshold;
    }

    public void setWarnActiveThreshold(int warnActiveThreshold) {
        this.warnActiveThreshold = warnActiveThreshold;
    }

    public List<SessionTargetProperties> getTargets() {
        return targets;
    }

    public void setTargets(List<SessionTargetProperties> targets) {
        this.targets = targets;
    }

    public static class SessionTargetProperties {
        private String scopeId;
        private String scopeName;
        /** spring-session | actuator */
        private String sourceType = "spring-session";
        private String baseUrl;
        private String metricName = "tomcat.sessions.active.current";
        private boolean enabled = true;

        public String getScopeId() {
            return scopeId;
        }

        public void setScopeId(String scopeId) {
            this.scopeId = scopeId;
        }

        public String getScopeName() {
            return scopeName;
        }

        public void setScopeName(String scopeName) {
            this.scopeName = scopeName;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getMetricName() {
            return metricName;
        }

        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
