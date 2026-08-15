package nhnis.infra.in.a.entry.aspect;

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
import nhnis.fw.commons.log.PdmgMessagePrinter;
import nhnis.fw.commons.log.PdmgTxLog;
import nhnis.infra.in.a.application.rule.InfraAuthRule;
import nhnis.infra.in.a.application.support.ValidationResult;

/**
 * Infra 업무 공통 선/후처리 Aspect.
 */
@Aspect
@Component
@Order(100)
@ConditionalOnProperty(name = "nhnis.fw.commons.legacy-web.enabled", havingValue = "true", matchIfMissing = true)
public class BizPrePostAspect {

    private static final Logger log = LoggerFactory.getLogger(BizPrePostAspect.class);
    private final InfraAuthRule authRule;

    public BizPrePostAspect(InfraAuthRule authRule) {
        this.authRule = authRule;
    }

    @Pointcut("execution(public * nhnis.infra.in.a.application.service..*(..))")
    public void infraBizBoundary() {
        // pointcut
    }

    @Before("infraBizBoundary()")
    public void before(JoinPoint joinPoint) {
        log.info(PdmgTxLog.bizPreStart());
        ServiceContext ctx = ServiceContextHolder.getInstance();
        String guid = ctx != null ? ctx.getGuid() : null;
        String serviceId = null;
        if (ctx != null && ctx.getHeader() != null && ctx.getHeader().getSys_comm() != null) {
            serviceId = ctx.getHeader().getSys_comm().getRms_svc_c();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        log.info(PdmgTxLog.bizPreProgress("GUID: " + guid));
        log.info(PdmgTxLog.bizPreProgress("Service: " + serviceId));
        log.info(PdmgTxLog.bizPreProgress("Method: " + method));
        ValidationResult auth = authRule.evaluate(serviceId);
        for (String w : auth.softWarnings()) {
            log.info(PdmgTxLog.bizPreProgress("RACI Soft: " + w));
        }
        if (auth.hasHard()) {
            log.warn(PdmgTxLog.bizPreProgress("RACI HARD deny: " + auth.firstHard().orElseThrow().formatted()));
        }
        log.info(PdmgTxLog.bizPreEnd());
        log.info(PdmgTxLog.bizProcessStart());
    }

    @AfterReturning(pointcut = "infraBizBoundary()", returning = "result")
    public void after(JoinPoint joinPoint, Object result) {
        log.info(PdmgTxLog.bizProcessEnd());
        log.info(PdmgTxLog.bizPostStart());
        if (log.isInfoEnabled()) {
            log.info(PdmgTxLog.bizResponseMessage());
            log.info(PdmgMessagePrinter.businessDto(result));
        }
        log.info(PdmgTxLog.bizPostEnd());
    }
}
