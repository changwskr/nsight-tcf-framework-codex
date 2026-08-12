package nhnis.mg.jw.a.dto;

/**
 * Access Token 폐기 입력 (mgjwa1000D0).
 */
public class mgjwa1000D0DTOin {

    private String accessToken;
    private String jti;
    private String reason;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
