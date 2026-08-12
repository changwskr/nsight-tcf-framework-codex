package nhnis.mg.jw.a.dto;

/**
 * SSO 연계 JWT 발급 입력 (mgjwa1000C1). tcf-om 등 내부 서비스만 호출한다.
 */
public class mgjwa1000C1DTOin {

    private String userId;
    private String userName;
    private String branchId;
    private String authGroupId;
    private String authGroupName;
    private String issuer;
    private String ssoSubject;
    private String ssoAssertionId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getAuthGroupId() {
        return authGroupId;
    }

    public void setAuthGroupId(String authGroupId) {
        this.authGroupId = authGroupId;
    }

    public String getAuthGroupName() {
        return authGroupName;
    }

    public void setAuthGroupName(String authGroupName) {
        this.authGroupName = authGroupName;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

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
