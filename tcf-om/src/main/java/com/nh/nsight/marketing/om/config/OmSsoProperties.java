package com.nh.nsight.marketing.om.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.om.sso")
public class OmSsoProperties {
    private boolean mockEnabled = true;
    private String jwtServiceUrl = "http://127.0.0.1:8110";
    private String internalSharedSecret = "nsight-local-jwt-internal-secret";
    private String internalServiceName = "tcf-om";

    public boolean isMockEnabled() { return mockEnabled; }
    public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }
    public String getJwtServiceUrl() { return jwtServiceUrl; }
    public void setJwtServiceUrl(String jwtServiceUrl) { this.jwtServiceUrl = jwtServiceUrl; }
    public String getInternalSharedSecret() { return internalSharedSecret; }
    public void setInternalSharedSecret(String internalSharedSecret) { this.internalSharedSecret = internalSharedSecret; }
    public String getInternalServiceName() { return internalServiceName; }
    public void setInternalServiceName(String internalServiceName) { this.internalServiceName = internalServiceName; }
}
