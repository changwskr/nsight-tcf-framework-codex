package nhnis.mg.jw.a.dto;

/**
 * JWT 로그아웃 입력 (mgjwa1000D1).
 */
public class mgjwa1000D1DTOin {

    private String accessToken;
    private String refreshToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
