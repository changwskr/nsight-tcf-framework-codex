package nhnis.mg.co.a.entry.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.log.PdmgTxFlowLog;

/**
 * 업무 Handler / Facade / Controller / Service / DAO 메서드 시작·종료 TX-FLOW 로그.
 *
 * <p>{@link BizPrePostAspect}({@code @Order(100)}) 보다 바깥에서 감싸도록 {@code @Order(50)}.
 */
@Aspect
@Component
@Order(50)
@ConditionalOnProperty(name = "nhnis.fw.commons.legacy-web.enabled", havingValue = "true", matchIfMissing = true)
public class PdmgBizTxFlowAspect {

    @Pointcut("execution(* nhnis.mg.co.a.entry.handler..*(..))"
            + " || execution(* nhnis.mg.co.a.application.facade..*(..))"
            + " || execution(* nhnis.mg.co.a.application.controller..*(..))"
            + " || execution(* nhnis.mg.co.a.application.service..*(..))"
            + " || execution(* nhnis.mg.co.a.persistence.dao..*(..))")
    public void bizFlow() {
        // pointcut
    }

    @Around("bizFlow()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        return PdmgTxFlowLog.around(PdmgBizTxFlowAspect.class, pjp);
    }
}
