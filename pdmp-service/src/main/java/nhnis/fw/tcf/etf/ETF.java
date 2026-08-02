package nhnis.fw.tcf.etf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import nhnis.fw.exception.BizException;
import nhnis.fw.exception.ExceptionCodeProperties;
import nhnis.fw.tcf.TcfMdcKeys;
import nhnis.fw.tcf.TcfProperties;
import nhnis.fw.tcf.context.TcfContext;
import nhnis.fw.tcf.dto.Result;
import nhnis.fw.tcf.dto.StandardHeaderDto;
import nhnis.fw.tcf.dto.StandardResponseDto;

/**
 * 시스템 후처리. 거래 종료를 판정해 기록하고 표준 응답 전문을 완성한다.
 *
 * <p>
 * 실패 전문은 AOP 말고도 Jackson 역직렬화 실패, 인증 실패처럼 AOP가 볼 수 없는 지점에서도
 * 만들어져야 한다. 포맷이 갈라지지 않도록 조립 책임을 이 클래스로 모았고, 그래서
 * {@code GlobalExceptionHandler}와 인증 EntryPoint도 이 빈을 주입받아 쓴다.
 */
@Component
public class ETF {

    private static final Logger log = LoggerFactory.getLogger(ETF.class);

    private final TcfProperties properties;
    private final ExceptionCodeProperties exceptionCodes;

    public ETF(TcfProperties properties, ExceptionCodeProperties exceptionCodes) {
        this.properties = properties;
        this.exceptionCodes = exceptionCodes;
    }

    /** 컨트롤러가 Body만 채워 돌려준 응답에 Header와 Result를 채운다. */
    public <T> StandardResponseDto<T> success(TcfContext context, StandardResponseDto<T> handlerResult) {
        System.out.println("=========[ETF][success][START] "
                + "serviceId=" + serviceIdOf(context));
        StandardResponseDto<T> response = handlerResult == null ? StandardResponseDto.of(null) : handlerResult;
        response.setHeader(responseHeaderOf(context));
        response.setResult(Result.success());
        end(context, Result.SUCCESS_CODE, null);
        System.out.println("=========[ETF][success][END] "
                + "serviceId=" + serviceIdOf(context)
                + " resultCode=" + Result.SUCCESS_CODE);
        return response;
    }

    /** 업무 예외를 실패 전문으로 바꾼다. */
    public StandardResponseDto<Object> businessFail(TcfContext context, BizException e) {
        System.out.println("=========[ETF][businessFail][START] "
                + "serviceId=" + serviceIdOf(context)
                + " errorCode=" + e.getCode());
        String message = exceptionCodes.message(e.getCode(), e.getArgs());
        end(context, Result.FAIL_CODE, e.getCode());
        System.out.println("=========[ETF][businessFail][END] "
                + "serviceId=" + serviceIdOf(context)
                + " resultCode=" + Result.FAIL_CODE
                + " errorCode=" + e.getCode());
        return StandardResponseDto.fail(responseHeaderOf(context), e.getCode(), message, null);
    }

    /** 예기치 않은 예외를 실패 전문으로 바꾼다. */
    public StandardResponseDto<Object> systemError(TcfContext context, Exception e) {
        System.out.println("=========[ETF][systemError][START] "
                + "serviceId=" + serviceIdOf(context)
                + " exception=" + e.getClass().getSimpleName());
        String code = ExceptionCodeProperties.DEFAULT_CODE;
        log.error("[ETF] 시스템 오류 serviceId={}", serviceIdOf(context), e);
        end(context, Result.FAIL_CODE, code);
        System.out.println("=========[ETF][systemError][END] "
                + "serviceId=" + serviceIdOf(context)
                + " resultCode=" + Result.FAIL_CODE
                + " errorCode=" + code);
        return StandardResponseDto.fail(responseHeaderOf(context), code, exceptionCodes.message(code),
                e.getClass().getSimpleName());
    }

    /** 거래 종료 기록. errorCode는 성공일 때 null이다. */
    public void end(TcfContext context, String resultCode, String errorCode) {
        System.out.println("=========[ETF][end][START] "
                + "serviceId=" + serviceIdOf(context)
                + " resultCode=" + resultCode
                + " errorCode=" + (errorCode == null ? "-" : errorCode));
        if (context == null) {
            // STF 이전이나 AOP 밖에서 끝난 거래. 소요시간은 알 수 없어도 실패는 남긴다.
            if (!Result.SUCCESS_CODE.equals(resultCode)) {
                log.warn("[ETF] 거래 실패 (컨텍스트 없음) resultCode={} errorCode={}", resultCode, errorCode);
            }
            System.out.println("=========[ETF][end][END] "
                    + "serviceId=UNKNOWN resultCode=" + resultCode + " errorCode="
                    + (errorCode == null ? "-" : errorCode));
            return;
        }
        long elapsedMs = context.elapsedMs();
        long threshold = properties.getSlowTransactionMs();
        if (threshold > 0 && elapsedMs >= threshold) {
            log.warn("[ETF] 지연 거래 serviceId={} resultCode={} business={}ms threshold={}ms",
                    context.serviceId(), resultCode, elapsedMs, threshold);
        }
        if (Result.SUCCESS_CODE.equals(resultCode)) {
            log.info("[ETF] 거래 종료 serviceId={} resultCode={} business={}ms",
                    context.serviceId(), resultCode, elapsedMs);
            System.out.println("=========[ETF][end][END] "
                    + "serviceId=" + context.serviceId()
                    + " resultCode=" + resultCode + " business=" + elapsedMs + "ms");
            return;
        }
        log.warn("[ETF] 거래 실패 serviceId={} resultCode={} errorCode={} business={}ms",
                context.serviceId(), resultCode, errorCode, elapsedMs);
        System.out.println("=========[ETF][end][END] "
                + "serviceId=" + context.serviceId()
                + " resultCode=" + resultCode + " errorCode=" + errorCode + " business=" + elapsedMs + "ms");
    }

    /** 거래 컨텍스트가 없는 지점(인증 실패, 역직렬화 실패)에서 쓰는 실패 전문. */
    public StandardResponseDto<Object> fail(String errorCode, Object... messageArgs) {
        System.out.println("=========[ETF][fail][START] "
                + "errorCode=" + errorCode);
        StandardResponseDto<Object> response = StandardResponseDto.fail(responseHeaderOf(null), errorCode,
                exceptionCodes.message(errorCode, messageArgs), null);
        System.out.println("=========[ETF][fail][END] "
                + "errorCode=" + errorCode);
        return response;
    }

    /**
     * 응답 Header는 클라이언트가 보낸 원본에 서버가 채번한 guid/traceId만 얹은 것이다.
     *
     * <p>
     * STF가 끝나기 전에 실패하면 컨텍스트가 없지만, 그때도 추적 ID는 돌려줘야 클라이언트가
     * 장애 문의 시 로그를 찾을 수 있다. 필터가 MDC에 심어 둔 값으로 최소 Header를 만든다.
     */
    private StandardHeaderDto responseHeaderOf(TcfContext context) {
        if (context != null) {
            return context.getClientHeader();
        }
        StandardHeaderDto header = new StandardHeaderDto();
        header.setGuid(MDC.get(TcfMdcKeys.GUID));
        header.setTraceId(MDC.get(TcfMdcKeys.TRACE_ID));
        return header;
    }

    private String serviceIdOf(TcfContext context) {
        return context == null ? "UNKNOWN" : context.serviceId();
    }
}
