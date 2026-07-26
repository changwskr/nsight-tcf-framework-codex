package com.nh.nsight.auth.jwt.config;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 런타임 JWT 보안 정책 (yml 초기값 + DB 갱신). */
@Component
public class JwtRuntimePolicy {
    private final JwtSecurityProperties defaults;
    private volatile String issuer;
    private volatile String audience;
    private volatile int accessTokenValidMinutes;
    private volatile int refreshTokenValidHours;
    private volatile String algorithm;
    private volatile int clockSkewSeconds;
    private volatile boolean denylistCheckEnabled;
    private volatile boolean refreshTokenRotationEnabled;

    public JwtRuntimePolicy(JwtSecurityProperties defaults) {
        this.defaults = defaults;
    }

    @PostConstruct
    void initFromDefaults() {
        apply(defaults.getIssuer(), defaults.getAudience(),
                defaults.getAccessTokenValidMinutes(), defaults.getRefreshTokenValidHours(),
                defaults.getAlgorithm(), defaults.getClockSkewSeconds(),
                defaults.isDenylistCheckEnabled(), defaults.isRefreshTokenRotationEnabled());
    }

    public void apply(String issuer, String audience, int accessMinutes, int refreshHours,
                      String algorithm, int clockSkew, boolean denylistCheck, boolean refreshRotation) {
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenValidMinutes = accessMinutes;
        this.refreshTokenValidHours = refreshHours;
        this.algorithm = algorithm;
        this.clockSkewSeconds = clockSkew;
        this.denylistCheckEnabled = denylistCheck;
        this.refreshTokenRotationEnabled = refreshRotation;
    }

    public void applyFromRow(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return;
        }
        apply(
                string(row, "issuer", defaults.getIssuer()),
                string(row, "audience", defaults.getAudience()),
                number(row, "accessTokenValidMinutes", defaults.getAccessTokenValidMinutes()),
                number(row, "refreshTokenValidHours", defaults.getRefreshTokenValidHours()),
                string(row, "algorithm", defaults.getAlgorithm()),
                number(row, "clockSkewSeconds", defaults.getClockSkewSeconds()),
                yn(row, "denylistCheckEnabled", defaults.isDenylistCheckEnabled()),
                yn(row, "refreshTokenRotationEnabled", defaults.isRefreshTokenRotationEnabled())
        );
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "policyId", "DEFAULT",
                "issuer", getIssuer(),
                "audience", getAudience(),
                "accessTokenValidMinutes", getAccessTokenValidMinutes(),
                "refreshTokenValidHours", getRefreshTokenValidHours(),
                "algorithm", getAlgorithm(),
                "clockSkewSeconds", getClockSkewSeconds(),
                "denylistCheckEnabled", isDenylistCheckEnabled() ? "Y" : "N",
                "refreshTokenRotationEnabled", isRefreshTokenRotationEnabled() ? "Y" : "N"
        );
    }

    public String getIssuer() { return issuer; }
    public String getAudience() { return audience; }
    public int getAccessTokenValidMinutes() { return accessTokenValidMinutes; }
    public int getRefreshTokenValidHours() { return refreshTokenValidHours; }
    public String getAlgorithm() { return algorithm; }
    public int getClockSkewSeconds() { return clockSkewSeconds; }
    public boolean isDenylistCheckEnabled() { return denylistCheckEnabled; }
    public boolean isRefreshTokenRotationEnabled() { return refreshTokenRotationEnabled; }

    private static String string(Map<String, Object> row, String key, String fallback) {
        Object v = row.get(key);
        if (v == null) {
            String upper = key.toUpperCase();
            v = row.get(upper);
        }
        return v == null ? fallback : String.valueOf(v);
    }

    private static int number(Map<String, Object> row, String key, int fallback) {
        Object v = row.get(key);
        if (v == null) {
            v = row.get(key.toUpperCase());
        }
        if (v == null) {
            return fallback;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean yn(Map<String, Object> row, String key, boolean fallback) {
        Object v = row.get(key);
        if (v == null) {
            v = row.get(key.toUpperCase());
        }
        if (v == null) {
            return fallback;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return "Y".equalsIgnoreCase(String.valueOf(v)) || "true".equalsIgnoreCase(String.valueOf(v));
    }
}
