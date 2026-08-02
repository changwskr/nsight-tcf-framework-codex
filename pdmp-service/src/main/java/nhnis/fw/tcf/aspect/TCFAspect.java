package nhnis.fw.tcf.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import nhnis.fw.exception.BizException;
import nhnis.fw.tcf.TcfTransaction;
import nhnis.fw.tcf.context.TcfContext;
import nhnis.fw.tcf.context.TcfContextHolder;
import nhnis.fw.tcf.dto.StandardRequestDto;
import nhnis.fw.tcf.dto.StandardResponseDto;
import nhnis.fw.tcf.etf.ETF;
import nhnis.fw.tcf.stf.STF;

/**
 * {@link TcfTransaction} 거래의 선후처리를 엮는다.
 *
 * <p>
 * 이 클래스는 배선만 담당하고 판단은 {@link STF}와 {@link ETF}에 있다. 그래야 선후처리 로직을
 * AOP 없이 단위 테스트할 수 있다.
 *
 * <p>
 * 대상 메서드는 {@code StandardResponseDto}를 반환해야 한다. CGLIB 프록시는 선언된 반환 타입을
 * 유지하므로, 어드바이스가 다른 타입을 돌려주면 ClassCastException이 난다. 실패 전문도 같은 자리로
 * 나가야 하니 반환 타입이 봉투로 고정되어야 한다.
 */
@Aspect
@Component
public class TCFAspect {

    private final STF stf;
    private final ETF etf;

    public TCFAspect(STF stf, ETF etf) {
        this.stf = stf;
        this.etf = etf;
    }

    @Around("@annotation(transaction)")
    public Object aroundTransaction(ProceedingJoinPoint joinPoint, TcfTransaction transaction) throws Throwable {
        String transactionResult = "UNKNOWN";
        TcfContext context = null;
        System.out.println("=========[TCFAspect][aroundTransaction][START] "
                + "serviceId=" + transaction.serviceId()
                + " transactionCode=" + transaction.transactionCode()
                + " processingType=" + transaction.processingType()
                + " serviceName=" + transaction.serviceName());
        try {
            context = stf.preProcess(findRequest(joinPoint), transaction);
            Object result = joinPoint.proceed();
            transactionResult = "SUCCESS";
            return etf.success(context, asStandardResponse(result));
        } catch (BizException e) {
            transactionResult = "BUSINESS_FAIL";
            return etf.businessFail(context, e);
        } catch (Exception e) {
            transactionResult = "SYSTEM_ERROR";
            return etf.systemError(context, e);
        } finally {
            System.out.println("=========[TCFAspect][aroundTransaction][END] "
                    + "serviceId=" + (context == null ? "UNKNOWN" : context.serviceId())
                    + " result=" + transactionResult);
            TcfContextHolder.clear();
        }
    }

    /** 전문 봉투를 쓰지 않는 거래도 있으므로 없으면 null을 준다. */
    private StandardRequestDto<?> findRequest(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof StandardRequestDto<?> request) {
                return request;
            }
        }
        return null;
    }

    private StandardResponseDto<?> asStandardResponse(Object returned) {
        if (returned == null) {
            return StandardResponseDto.of(null);
        }
        if (returned instanceof StandardResponseDto<?> response) {
            return response;
        }
        throw new IllegalStateException(
                "@TcfTransaction 메서드는 StandardResponseDto를 반환해야 한다: " + returned.getClass().getName());
    }
}
