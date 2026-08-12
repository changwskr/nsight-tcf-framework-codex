package nhnis.mg.jw.a.dto;

/**
 * 토큰 강제폐기 입력 (mgjwa1001D0).
 */
public class mgjwa1001D0DTOin {

    private String jti;
    private String reason;

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
