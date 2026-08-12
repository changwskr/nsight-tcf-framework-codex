package nhnis.mg.jw.a.dto;

/**
 * SSO 연계 JWT 발급 결과 (mgjwa1000C1). 공통 토큰 필드는 {@link mgjwa1000C0DTOout} 과 동일하다.
 */
public class mgjwa1000C1DTOout extends mgjwa1000C0DTOout {

    private String ssoSubject;
    private String ssoAssertionId;

    public String getSsoSubject() {
        return ssoSubject;
    }

    public void setSsoSubject(String ssoSubject) {
        this.ssoSubject = ssoSubject;
    }

    public String getSsoAssertionId() {
        return ssoAssertionId;
    }

    public void setSsoAssertionId(String ssoAssertionId) {
        this.ssoAssertionId = ssoAssertionId;
    }
}
