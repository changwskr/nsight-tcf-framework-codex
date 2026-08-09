package nhnis.mg.entry.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.log.PdmkMessagePrinter;
import nhnis.fw.commons.log.PdmkTxLog;

/**
 * PDMG 업무 공통 선/후처리 Aspect.
 *
 * <p>패키지·클래스명을 운영 로그와 동일하게 {@code nhnis.mg.entry.aspect.BizPrePostAspect} 로 둔다.
 * {@code log.info} 는 이 클래스에서 직접 호출해 {@code %C.%M} 위치가 맞도록 한다.
 */
@Aspect
@Component
@Order(100)
@ConditionalOnProperty(name = "nhnis.fw.commons.legacy-web.enabled", havingValue = "true", matchIfMissing = true)
public class BizPrePostAspect {

    private static final Logger log = LoggerFactory.getLogger(BizPrePostAspect.class);

    @Pointcut("execution(* nhnis.mg.entry.controller..*(..))")
    public void mgCoControllers() {
        // pointcut
    }

    @Before("mgCoControllers()")
    public void before(JoinPoint joinPoint) {
        log.info(PdmkTxLog.bizPreStart());
        logBizPreProgress(joinPoint);
        log.info(PdmkTxLog.bizPreEnd());
        log.info(PdmkTxLog.bizProcessStart());
    }

    @AfterReturning(pointcut = "mgCoControllers()", returning = "result")
    public void after(JoinPoint joinPoint, Object result) {
        log.info(PdmkTxLog.bizProcessEnd());
        log.info(PdmkTxLog.bizPostStart());
        if (log.isInfoEnabled()) {
            log.info(PdmkTxLog.bizResponseMessage());
            log.info(PdmkMessagePrinter.businessDto(result));
        }
        log.info(PdmkTxLog.bizPostEnd());
    }

    /** 업무 선처리 구간 진행 로그 (GUID / 서비스 / 메서드 / BRC). */
    private void logBizPreProgress(JoinPoint joinPoint) {
        ServiceContext ctx = ServiceContextHolder.getInstance();
        String guid = ctx != null ? ctx.getGuid() : null;
        String serviceId = null;
        if (ctx != null && ctx.getHeader() != null && ctx.getHeader().getSys_comm() != null) {
            serviceId = ctx.getHeader().getSys_comm().getRms_svc_c();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        log.info(PdmkTxLog.bizPreProgress("GUID: " + guid));
        log.info(PdmkTxLog.bizPreProgress("서비스: " + serviceId));
        log.info(PdmkTxLog.bizPreProgress("호출: " + method));
        log.info(PdmkTxLog.bizPreProgress("BRC: " + extractBrc(joinPoint.getArgs())));
    }

    private Object extractBrc(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            for (String name : new String[] {"getTrtBrc", "getBrc", "getBRC", "getL5104"}) {
                try {
                    Method method = arg.getClass().getMethod(name);
                    return method.invoke(arg);
                } catch (ReflectiveOperationException ignored) {
                    // try next
                }
            }
        }
        return null;
    }
}
