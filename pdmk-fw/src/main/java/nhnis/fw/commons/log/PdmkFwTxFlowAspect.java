package nhnis.fw.commons.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * FW 공통 컴포넌트(이미지로그 등) 메서드 시작/종료 TX-FLOW 로그.
 *
 * <p>Filter/Interceptor/ResponseBodyAdvice 는 컨테이너가 직접 호출하므로
 * 각 클래스에서 {@link PdmkTxFlowLog} 를 수동으로 남긴다.
 */
@Aspect
@Component
@Order(20)
@ConditionalOnProperty(name = "nhnis.fw.commons.legacy-web.enabled", havingValue = "true", matchIfMissing = true)
public class PdmkFwTxFlowAspect {

    @Around("execution(* nhnis.fw.commons.imagelog.ImageLogHandler.*(..))")
    public Object aroundImageLog(ProceedingJoinPoint pjp) throws Throwable {
        return PdmkTxFlowLog.around(PdmkFwTxFlowAspect.class, pjp);
    }
}
