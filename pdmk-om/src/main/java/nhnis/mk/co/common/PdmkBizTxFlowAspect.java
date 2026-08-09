package nhnis.mk.co.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.log.PdmkTxFlowLog;

/**
 * 업무 Controller / Service / DAO 메서드 시작·종료 TX-FLOW 로그.
 *
 * <p>{@link BizPrePostAspect}({@code @Order(100)}) 보다 바깥에서 감싸도록 {@code @Order(50)}.
 */
@Aspect
@Component
@Order(50)
@ConditionalOnProperty(name = "nhnis.fw.commons.legacy-web.enabled", havingValue = "true", matchIfMissing = true)
public class PdmkBizTxFlowAspect {

    @Pointcut("execution(* nhnis.mk.co..controller..*(..))"
            + " || execution(* nhnis.mk.co..service..*(..))"
            + " || execution(* nhnis.mk.co..dao..*(..))")
    public void bizFlow() {
        // pointcut
    }

    @Around("bizFlow()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        return PdmkTxFlowLog.around(PdmkBizTxFlowAspect.class, pjp);
    }
}
