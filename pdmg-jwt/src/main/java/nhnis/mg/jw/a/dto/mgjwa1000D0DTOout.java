package nhnis.mg.jw.a.dto;

/**
 * Access Token 폐기 결과 (mgjwa1000D0).
 */
public class mgjwa1000D0DTOout {

    private String businessCode;
    private Boolean revoked;
    private String reason;

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
