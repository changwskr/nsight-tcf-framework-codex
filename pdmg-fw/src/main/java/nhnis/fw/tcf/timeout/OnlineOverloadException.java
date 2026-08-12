package nhnis.fw.tcf.timeout;

/**
 * 온라인 Worker Pool / 대기 큐 포화.
 */
public class OnlineOverloadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String serviceId;
    private final String guid;
    private final int active;
    private final int poolSize;
    private final int queueSize;

    public OnlineOverloadException(String serviceId, String guid, int active, int poolSize, int queueSize) {
        super("온라인 거래 처리 요청이 일시적으로 많습니다.");
        this.serviceId = serviceId;
        this.guid = guid;
        this.active = active;
        this.poolSize = poolSize;
        this.queueSize = queueSize;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getGuid() {
        return guid;
    }

    public int getActive() {
        return active;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getQueueSize() {
        return queueSize;
    }
}
