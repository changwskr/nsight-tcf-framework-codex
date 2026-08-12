package nhnis.fw.commons.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.spi.LocationAwareLogger;

/**
 * 거래 흐름(Filter → Interceptor → Aspect → Controller/Service/DAO) 참여 메서드의
 * 시작/종료 로그.
 *
 * <p>Log4j {@code %C.%M} 이 헬퍼({@code PdmgTxFlowLog})가 아니라
 * 실제 참여 클래스·메서드로 나오도록 위치를 보정한다.
 *
 * <pre>
 * [TX-FLOW][시작] DefaultFilter.doFilter
 * [TX-FLOW][종료] DefaultFilter.doFilter
 * </pre>
 */
public final class PdmgTxFlowLog {

    /** LocationAwareLogger 가 이 클래스 프레임을 건너뛰게 하는 FQCN. */
    private static final String FQCN = PdmgTxFlowLog.class.getName();

    private PdmgTxFlowLog() {
    }

    public static String start(Class<?> type, String method) {
        return "[TX-FLOW][시작] " + simpleName(type) + "." + method;
    }

    public static String end(Class<?> type, String method) {
        return "[TX-FLOW][종료] " + simpleName(type) + "." + method;
    }

    public static String start(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return start(signature.getDeclaringType(), signature.getName());
    }

    public static String end(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return end(signature.getDeclaringType(), signature.getName());
    }

    /**
     * Aspect {@code @Around} 용.
     * AOP 호출 스택에는 대상 메서드가 없으므로 Log4j {@code withLocation} 으로
     * 대상 클래스·메서드를 {@code %C.%M} 에 명시한다.
     */
    public static Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Class<?> type = signature.getDeclaringType();
        String method = signature.getName();

        infoAt(type, method, start(type, method));
        try {
            return pjp.proceed();
        } finally {
            infoAt(type, method, end(type, method));
        }
    }

    /** 호환용. {@link #around(ProceedingJoinPoint)} 와 동일. */
    public static Object around(Class<?> aspectClass, ProceedingJoinPoint pjp) throws Throwable {
        return around(pjp);
    }

    /** @deprecated Aspect 에서는 {@link #around(Class, ProceedingJoinPoint)} 사용. */
    @Deprecated
    public static Object around(Logger log, Class<?> type, String method, ProceedingJoinPoint pjp)
            throws Throwable {
        infoAt(type, method, start(type, method));
        try {
            return pjp.proceed();
        } finally {
            infoAt(type, method, end(type, method));
        }
    }

    /**
     * Filter/Interceptor 등 직접 호출부용.
     * {@link LocationAwareLogger} 로 헬퍼 프레임을 건너뛰어 호출부 {@code %C.%M} 을 남긴다.
     */
    public static void enter(Logger log, Class<?> type, String method) {
        infoFromCaller(log, start(type, method));
    }

    public static void leave(Logger log, Class<?> type, String method) {
        infoFromCaller(log, end(type, method));
    }

    private static void infoFromCaller(Logger log, String message) {
        if (log == null || !log.isInfoEnabled()) {
            return;
        }
        if (log instanceof LocationAwareLogger locationAware) {
            locationAware.log(
                    null,
                    FQCN,
                    LocationAwareLogger.INFO_INT,
                    message,
                    null,
                    null);
            return;
        }
        log.info(message);
    }

    /** AOP 대상 클래스·메서드를 Log4j location 으로 강제 지정. */
    private static void infoAt(Class<?> type, String method, String message) {
        if (type == null) {
            return;
        }
        ExtendedLogger logger = (ExtendedLogger) LogManager.getLogger(type);
        if (!logger.isInfoEnabled()) {
            return;
        }
        StackTraceElement location = new StackTraceElement(
                type.getName(),
                method,
                type.getSimpleName() + ".java",
                -1);
        logger.atLevel(Level.INFO).withLocation(location).log(message);
    }

    private static String simpleName(Class<?> type) {
        return type == null ? "?" : type.getSimpleName();
    }
}
