package nhnis.fw.tcf.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code jwt.*} 설정. 프로파일별로 application.yml에 정의한다.
 */
@ConfigurationProperties("jwt")
public class JwtProperties {

    /** HMAC 서명 키. 최소 256bit(32자) 이상이어야 한다. */
    private String secret;

    /** 액세스 토큰 유효시간(ms). 발급 기능을 붙일 때 사용한다. */
    private long accessTokenExpiration = 180_000L;

    /** false면 토큰을 검증하지 않는다. 조회 전용 단계에서 끄고 쓸 수 있다. */
    private boolean enabled = false;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
