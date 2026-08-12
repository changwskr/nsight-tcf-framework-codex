package nhnis.mg.ui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 전문 테스트 대상(pdmg-service) 접속 정보.
 *
 * <p>화면에서 대상 URL을 직접 바꿔가며 시험할 수 있고, 여기 값은 초기값으로만 쓰인다.
 */
@ConfigurationProperties("pdmg.ui")
public class PdmgUiProperties {

    /** pdmg-service 기본 주소. */
    private String targetBaseUrl = "http://localhost:8080";

    /** 브라우저 fetch Abort 타임아웃(ms). pdmg-service OnlineTimeout과 별개. */
    private int timeoutMs = 10000;

    public String getTargetBaseUrl() {
        return targetBaseUrl;
    }

    public void setTargetBaseUrl(String targetBaseUrl) {
        this.targetBaseUrl = targetBaseUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
