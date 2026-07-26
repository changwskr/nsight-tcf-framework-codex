package com.nh.nsight.marketing.eb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.eb.event-publish")
public class EbEventPublishProperties {
    private boolean enabled = true;
    private long fixedDelayMs = 60_000;
    private int batchSize = 50;
    private String epOnlineUrl = "http://127.0.0.1:8090/ep/online";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getEpOnlineUrl() {
        return epOnlineUrl;
    }

    public void setEpOnlineUrl(String epOnlineUrl) {
        this.epOnlineUrl = epOnlineUrl;
    }
}
