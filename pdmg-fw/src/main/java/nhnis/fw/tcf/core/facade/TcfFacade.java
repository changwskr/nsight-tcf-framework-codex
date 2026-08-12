package nhnis.fw.tcf.core.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.dispatch.TransactionDispatcher;
import nhnis.fw.tcf.timeout.OnlineTimeoutExecutor;

/**
 * TCF Core Facade.
 *
 * <p>tcf-core 의 {@code TCF.process} 역할을 PDMG 에 맞게 단순화한다.
 * 시스템 선후처리(Filter / ServicePreventionInterceptor / ResponseBodyAdvice)는
 * 그대로 유지하고, 여기서는 Controller → Handler → 업무 Facade 연결만 담당한다.
 *
 * <p>{@link OnlineTimeoutExecutor} 가 활성이면 Dispatcher 이하를 Worker + TX 로 감싼다.
 *
 * <pre>
 * Filter → 시스템선처리 → Controller → TcfFacade → OnlineTimeoutExecutor
 *   → Handler → 업무Facade → 업무선처리 → Service → DAO → 업무후처리 → 시스템후처리
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class TcfFacade {

    private static final Logger log = LoggerFactory.getLogger(TcfFacade.class);

    private final TransactionDispatcher dispatcher;
    private final OnlineTimeoutExecutor onlineTimeoutExecutor;

    public TcfFacade(TransactionDispatcher dispatcher, OnlineTimeoutExecutor onlineTimeoutExecutor) {
        log.info("========================= [TcfFacade.<init>] 시작");
        try {
            this.dispatcher = dispatcher;
            this.onlineTimeoutExecutor = onlineTimeoutExecutor;
        } finally {
            log.info("========================= [TcfFacade.<init>] 종료");
        }
    }

    /**
     * @param serviceId 거래 식별자 (예: mgcoa5530S0)
     * @param dtoBody   요청 전문 {@code dto} 노드
     * @return 업무 응답 DTO
     */
    public Object process(String serviceId, Object dtoBody) throws Exception {
        log.info("========================= [TcfFacade.process] 시작");
        try {
            log.debug("[TcfFacade] process start serviceId={}", serviceId);
            TransactionContext context = TransactionContext.fromCurrent(serviceId);
            Object result = onlineTimeoutExecutor.execute(
                    () -> dispatcher.dispatch(serviceId, dtoBody, context));
            log.debug("[TcfFacade] process end serviceId={} elapsedMs={}", serviceId, context.elapsedMs());
            return result;
        } finally {
            log.info("========================= [TcfFacade.process] 종료");
        }
    }
}
