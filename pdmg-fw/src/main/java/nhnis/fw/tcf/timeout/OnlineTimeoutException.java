package nhnis.fw.tcf.timeout;

/**
 * 온라인 거래 공통 타임아웃.
 *
 * <p>요청·응답 전문, Token, 개인정보는 담지 않는다.
 */
public class OnlineTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long timeoutMs;
    private final long elapsedMs;
    private final String serviceId;
    private final String guid;

    public OnlineTimeoutException(long timeoutMs, long elapsedMs, String serviceId, String guid) {
        super("온라인 거래 처리시간을 초과했습니다.");
        this.timeoutMs = timeoutMs;
        this.elapsedMs = elapsedMs;
        this.serviceId = serviceId;
        this.guid = guid;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getGuid() {
        return guid;
    }
}
