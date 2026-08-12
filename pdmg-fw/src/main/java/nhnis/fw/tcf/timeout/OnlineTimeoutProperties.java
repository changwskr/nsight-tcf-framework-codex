package nhnis.fw.tcf.timeout;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;

/**
 * 온라인 거래 공통 타임아웃 설정.
 *
 * <pre>
 * nhnis.fw.timeout.enabled=true
 * nhnis.fw.timeout.milliseconds=5000
 * nhnis.fw.timeout.pool-size=20
 * nhnis.fw.timeout.queue-capacity=100
 * </pre>
 */
@ConfigurationProperties(prefix = "nhnis.fw.timeout")
public class OnlineTimeoutProperties {

    private boolean enabled = false;

    private long milliseconds = 5000L;

    private int poolSize = 20;

    private int queueCapacity = 100;

    @PostConstruct
    void validate() {
        if (milliseconds < 1) {
            throw new IllegalStateException("nhnis.fw.timeout.milliseconds must be >= 1");
        }
        if (poolSize < 1) {
            throw new IllegalStateException("nhnis.fw.timeout.pool-size must be >= 1");
        }
        if (queueCapacity < 0) {
            throw new IllegalStateException("nhnis.fw.timeout.queue-capacity must be >= 0");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    public void setMilliseconds(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}
