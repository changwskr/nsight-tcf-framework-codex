package nhnis.mg.jw.a.dto;

/**
 * JWT 로그아웃 결과 (mgjwa1000D1).
 */
public class mgjwa1000D1DTOout {

    private String businessCode;
    private Boolean loggedOut;

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public Boolean getLoggedOut() {
        return loggedOut;
    }

    public void setLoggedOut(Boolean loggedOut) {
        this.loggedOut = loggedOut;
    }
}
