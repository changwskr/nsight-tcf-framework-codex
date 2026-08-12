package nhnis.mg.jw.a.dto;

import java.util.Map;

/**
 * 보안정책 조회 결과 (mgjwa1004S0).
 *
 * <p>{@code policy} 는 DB/런타임 정책 행(Map)을 그대로 담아 pdmg-ui
 * {@code security-policy.html} 이 참조하는 필드명(issuer, audience,
 * accessTokenValidMinutes, refreshTokenValidHours, algorithm, clockSkewSeconds,
 * denylistCheckEnabled, refreshTokenRotationEnabled, updatedAt, updatedBy)을 그대로 유지한다.
 */
public class mgjwa1004S0DTOout {

    private String businessCode;
    private String screen;
    private Map<String, Object> policy;

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public String getScreen() {
        return screen;
    }

    public void setScreen(String screen) {
        this.screen = screen;
    }

    public Map<String, Object> getPolicy() {
        return policy;
    }

    public void setPolicy(Map<String, Object> policy) {
        this.policy = policy;
    }
}
