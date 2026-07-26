package com.nh.nsight.auth.jwt.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.security.jwt.internal")
public class JwtInternalCallProperties {
    private String sharedSecret = "nsight-local-jwt-internal-secret";
    private String allowedService = "tcf-om";
    private int timestampSkewSeconds = 180;
    private List<String> allowedIps = new ArrayList<>(List.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1"));

    public String getSharedSecret() { return sharedSecret; }
    public void setSharedSecret(String sharedSecret) { this.sharedSecret = sharedSecret; }
    public String getAllowedService() { return allowedService; }
    public void setAllowedService(String allowedService) { this.allowedService = allowedService; }
    public int getTimestampSkewSeconds() { return timestampSkewSeconds; }
    public void setTimestampSkewSeconds(int timestampSkewSeconds) { this.timestampSkewSeconds = timestampSkewSeconds; }
    public List<String> getAllowedIps() { return allowedIps; }
    public void setAllowedIps(List<String> allowedIps) { this.allowedIps = allowedIps; }
}
