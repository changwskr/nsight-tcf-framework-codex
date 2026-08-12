package nhnis.fw.tcf.stf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.log.PdmgTxFlowLog;
import nhnis.fw.commons.txcontrol.MgTxControlService;
import nhnis.fw.tcf.core.context.TransactionContext;

/**
 * PDMG STF (Start-of-Transaction Framework).
 *
 * <p>TCF ON 경로에서 Handler 진입 전 공통 선처리를 수행한다.
 * 현재는 {@code TB_MG_TX_CONTROL} 거래통제 강제를 담당한다.
 *
 * <pre>
 * Filter → 시스템선처리(Interceptor) → Controller → TcfFacade
 *   → stf.preProcess → OnlineTimeoutExecutor → Handler → …
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class stf {

    private static final Logger log = LoggerFactory.getLogger(stf.class);

    private final MgTxControlService mgTxControlService;

    public stf(MgTxControlService mgTxControlService) {
        this.mgTxControlService = mgTxControlService;
    }

    /**
     * 거래 시작 공통 선처리.
     *
     * @param context 온라인 거래 컨텍스트 (헤더 포함)
     */
    public void preProcess(TransactionContext context) {
        PdmgTxFlowLog.enter(log, stf.class, "preProcess");
        try {
            preProcessInternal(context);
        } finally {
            PdmgTxFlowLog.leave(log, stf.class, "preProcess");
        }
    }

    private void preProcessInternal(TransactionContext context) {
        log.info("========================= [stf.preProcess] 시작");
        try {
            hdr_nhnis header = context == null ? null : context.getHeader();
            String serviceId = context == null ? null : context.getServiceId();
            log.info("[stf] transaction control check serviceId={}", serviceId);
            mgTxControlService.check(header);
            log.info("[stf] transaction control allowed serviceId={}", serviceId);
        } finally {
            log.info("========================= [stf.preProcess] 종료");
        }
    }
}
