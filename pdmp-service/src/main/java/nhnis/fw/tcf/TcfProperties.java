package nhnis.fw.tcf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code nsight.tcf.*} 프레임워크 설정.
 */
@ConfigurationProperties("nsight.tcf")
public class TcfProperties {

    /** 이 시간을 넘긴 거래는 WARN으로 남긴다. 0 이하면 감시하지 않는다. */
    private long slowTransactionMs = 3000L;

    public long getSlowTransactionMs() {
        return slowTransactionMs;
    }

    public void setSlowTransactionMs(long slowTransactionMs) {
        this.slowTransactionMs = slowTransactionMs;
    }
}
