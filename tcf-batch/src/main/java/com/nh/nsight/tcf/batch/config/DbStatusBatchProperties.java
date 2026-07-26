package com.nh.nsight.tcf.batch.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.batch.db-status")
public class DbStatusBatchProperties {
    private String jobId = "BAT-BATCH-002";
    private String cron = "30 */5 * * * *";
    private List<DbTargetProperties> targets = new ArrayList<>();

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

    public List<DbTargetProperties> getTargets() {
        return targets;
    }

    public void setTargets(List<DbTargetProperties> targets) {
        this.targets = targets;
    }

    public static class DbTargetProperties {
        private String dbId;
        private String dbName;
        /** actuator | jdbc */
        private String sourceType = "actuator";
        private String baseUrl;
        private String jdbcUrl;
        private String jdbcUser = "sa";
        private String jdbcPassword = "";
        private boolean enabled = true;

        public String getDbId() {
            return dbId;
        }

        public void setDbId(String dbId) {
            this.dbId = dbId;
        }

        public String getDbName() {
            return dbName;
        }

        public void setDbName(String dbName) {
            this.dbName = dbName;
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

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getJdbcUser() {
            return jdbcUser;
        }

        public void setJdbcUser(String jdbcUser) {
            this.jdbcUser = jdbcUser;
        }

        public String getJdbcPassword() {
            return jdbcPassword;
        }

        public void setJdbcPassword(String jdbcPassword) {
            this.jdbcPassword = jdbcPassword;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
