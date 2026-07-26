package com.nh.nsight.tcf.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.tcf.web.jwt")
public class TcfWebJwtProperties {
    private boolean enabled = false;
    private String jwkSetUri;
    private String issuer = "NSIGHT-AUTH";
    private String audience = "NSIGHT-MP";
    private String headerName = "Authorization";
    private String tokenPrefix = "Bearer";
    private boolean requiredForOnline = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public boolean isRequiredForOnline() {
        return requiredForOnline;
    }

    public void setRequiredForOnline(boolean requiredForOnline) {
        this.requiredForOnline = requiredForOnline;
    }
}
