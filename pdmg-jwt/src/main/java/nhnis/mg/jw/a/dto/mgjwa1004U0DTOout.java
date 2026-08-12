package nhnis.mg.jw.a.dto;

import java.util.Map;

/**
 * 보안정책 수정 결과 (mgjwa1004U0).
 */
public class mgjwa1004U0DTOout {

    private String businessCode;
    private String screen;
    private Boolean updated;
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

    public Boolean getUpdated() {
        return updated;
    }

    public void setUpdated(Boolean updated) {
        this.updated = updated;
    }

    public Map<String, Object> getPolicy() {
        return policy;
    }

    public void setPolicy(Map<String, Object> policy) {
        this.policy = policy;
    }
}
