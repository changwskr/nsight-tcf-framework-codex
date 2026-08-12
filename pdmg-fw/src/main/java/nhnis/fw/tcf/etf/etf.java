package nhnis.fw.tcf.etf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.log.PdmgTxFlowLog;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.timeout.OnlineTimeoutException;
import nhnis.fw.tcf.timeout.OnlineTimeoutProperties;

/**
 * PDMG ETF (End-of-Transaction Framework).
 *
 * <p>TCF ON 경로에서 Handler 종료 후 공통 후처리를 수행한다.
 * 거래 시작시각 기준 serviceId 타임아웃 인터벌을 점검한다.
 *
 * <pre>
 * … → Handler → etf.postProcess(checkTimeoutInterval) → …
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class etf {

    private static final Logger log = LoggerFactory.getLogger(etf.class);

    private final OnlineTimeoutProperties timeoutProperties;

    public etf(OnlineTimeoutProperties timeoutProperties) {
        this.timeoutProperties = timeoutProperties;
    }

    /**
     * 거래 종료 공통 후처리.
     *
     * @param context 온라인 거래 컨텍스트
     */
    public void postProcess(TransactionContext context) {
        PdmgTxFlowLog.enter(log, etf.class, "postProcess");
        try {
            postProcessInternal(context);
        } finally {
            PdmgTxFlowLog.leave(log, etf.class, "postProcess");
        }
    }

    private void postProcessInternal(TransactionContext context) {
        log.info("========================= [etf.postProcess] 시작");
        try {
            checkTimeoutInterval(context);
        } finally {
            log.info("========================= [etf.postProcess] 종료");
        }
    }

    /**
     * 트랜잭션 시작시각을 기준으로 serviceId 타임아웃(ms)과 현재 시각 경과분을 비교한다.
     * 경과 시간이 허용 인터벌을 초과하면 {@link OnlineTimeoutException} 을 던진다.
     *
     * @param context 거래 컨텍스트 (시작시각·serviceId·guid 포함)
     */
    public void checkTimeoutInterval(TransactionContext context) {
        if (context == null) {
            return;
        }
        if (timeoutProperties == null || !timeoutProperties.isEnabled()) {
            log.debug("[etf] timeout interval check skipped (disabled)");
            return;
        }

        String serviceId = context.getServiceId();
        long timeoutMs = timeoutProperties.resolveMilliseconds(serviceId);
        long elapsedMs = context.elapsedMsSinceStart();
        String guid = context.getGuid();

        log.info("[etf] timeout interval check serviceId={} timeoutMs={} elapsedMs={} guid={}",
                serviceId, timeoutMs, elapsedMs, guid);

        if (elapsedMs > timeoutMs) {
            log.warn("[etf] timeout interval exceeded serviceId={} timeoutMs={} elapsedMs={} guid={}",
                    serviceId, timeoutMs, elapsedMs, guid);
            throw new OnlineTimeoutException(timeoutMs, elapsedMs, serviceId, guid);
        }
    }
}
