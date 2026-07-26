package com.nh.nsight.auth.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.security.jwt")
public class JwtSecurityProperties {
    private String issuer = "NSIGHT-AUTH";
    private String audience = "NSIGHT-MP";
    private int accessTokenValidMinutes = 15;
    private int refreshTokenValidHours = 8;
    private int clockSkewSeconds = 60;
    private String algorithm = "RS256";
    private String issuerUri;
    private String jwkSetUri;
    private boolean denylistCheckEnabled = true;
    private boolean refreshTokenRotationEnabled = true;
    private String headerName = "Authorization";
    private String tokenPrefix = "Bearer";
    private boolean cookieModeEnabled;
    private String accessTokenCookieName = "NSIGHT_AT";
    private String refreshTokenCookieName = "NSIGHT_RT";
    private boolean schemaAutoInit = true;

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public int getAccessTokenValidMinutes() { return accessTokenValidMinutes; }
    public void setAccessTokenValidMinutes(int accessTokenValidMinutes) { this.accessTokenValidMinutes = accessTokenValidMinutes; }
    public int getRefreshTokenValidHours() { return refreshTokenValidHours; }
    public void setRefreshTokenValidHours(int refreshTokenValidHours) { this.refreshTokenValidHours = refreshTokenValidHours; }
    public int getClockSkewSeconds() { return clockSkewSeconds; }
    public void setClockSkewSeconds(int clockSkewSeconds) { this.clockSkewSeconds = clockSkewSeconds; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getIssuerUri() { return issuerUri; }
    public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }
    public String getJwkSetUri() { return jwkSetUri; }
    public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }
    public boolean isDenylistCheckEnabled() { return denylistCheckEnabled; }
    public void setDenylistCheckEnabled(boolean denylistCheckEnabled) { this.denylistCheckEnabled = denylistCheckEnabled; }
    public boolean isRefreshTokenRotationEnabled() { return refreshTokenRotationEnabled; }
    public void setRefreshTokenRotationEnabled(boolean refreshTokenRotationEnabled) { this.refreshTokenRotationEnabled = refreshTokenRotationEnabled; }
    public String getHeaderName() { return headerName; }
    public void setHeaderName(String headerName) { this.headerName = headerName; }
    public String getTokenPrefix() { return tokenPrefix; }
    public void setTokenPrefix(String tokenPrefix) { this.tokenPrefix = tokenPrefix; }
    public boolean isCookieModeEnabled() { return cookieModeEnabled; }
    public void setCookieModeEnabled(boolean cookieModeEnabled) { this.cookieModeEnabled = cookieModeEnabled; }
    public String getAccessTokenCookieName() { return accessTokenCookieName; }
    public void setAccessTokenCookieName(String accessTokenCookieName) { this.accessTokenCookieName = accessTokenCookieName; }
    public String getRefreshTokenCookieName() { return refreshTokenCookieName; }
    public void setRefreshTokenCookieName(String refreshTokenCookieName) { this.refreshTokenCookieName = refreshTokenCookieName; }
    public boolean isSchemaAutoInit() { return schemaAutoInit; }
    public void setSchemaAutoInit(boolean schemaAutoInit) { this.schemaAutoInit = schemaAutoInit; }
}
