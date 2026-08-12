package nhnis.mg.jw.a.dto;

/**
 * 보안정책 수정 입력 (mgjwa1004U0).
 *
 * <p>{@code denylistCheckEnabled}/{@code refreshTokenRotationEnabled} 는 pdmg-ui 가
 * {@code "Y"/"N"} 문자열로 보내므로 String 으로 받는다.
 */
public class mgjwa1004U0DTOin {

    private String issuer;
    private String audience;
    private Integer accessTokenValidMinutes;
    private Integer refreshTokenValidHours;
    private String algorithm;
    private Integer clockSkewSeconds;
    private String denylistCheckEnabled;
    private String refreshTokenRotationEnabled;

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

    public Integer getAccessTokenValidMinutes() {
        return accessTokenValidMinutes;
    }

    public void setAccessTokenValidMinutes(Integer accessTokenValidMinutes) {
        this.accessTokenValidMinutes = accessTokenValidMinutes;
    }

    public Integer getRefreshTokenValidHours() {
        return refreshTokenValidHours;
    }

    public void setRefreshTokenValidHours(Integer refreshTokenValidHours) {
        this.refreshTokenValidHours = refreshTokenValidHours;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(Integer clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public String getDenylistCheckEnabled() {
        return denylistCheckEnabled;
    }

    public void setDenylistCheckEnabled(String denylistCheckEnabled) {
        this.denylistCheckEnabled = denylistCheckEnabled;
    }

    public String getRefreshTokenRotationEnabled() {
        return refreshTokenRotationEnabled;
    }

    public void setRefreshTokenRotationEnabled(String refreshTokenRotationEnabled) {
        this.refreshTokenRotationEnabled = refreshTokenRotationEnabled;
    }
}
