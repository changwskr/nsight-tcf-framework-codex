package nhnis.fw.exception;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import nhnis.fw.tcf.TcfMdcKeys;
import nhnis.fw.tcf.dto.StandardResponseDto;
import nhnis.fw.tcf.etf.ETF;

/**
 * AOP 밖에서 새어 나온 예외를 표준 실패 전문으로 바꾼다.
 *
 * <p>
 * Jackson 역직렬화 실패처럼 컨트롤러에 닿기 전에 끝난 요청이 여기로 온다. 거래 컨텍스트가 없으므로
 * 응답 Header는 비지만, 전문 포맷은 ETF를 거쳐 AOP 경로와 동일하게 유지한다.
 *
 * <p>
 * HTTP 상태는 성공·실패 모두 200이며 판별은 {@code result.resultCode}로 한다. TCF 표준 계약이다.
 *
 * <p>PDMK commons 경로({@code nhnis.fw.tcf.enabled=false})에서는 등록하지 않는다.
 */
@RestControllerAdvice
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class GlobalExceptionHandler {

    private final ETF etf;

    public GlobalExceptionHandler(ETF etf) {
        this.etf = etf;
    }

    @ExceptionHandler(BizException.class)
    public StandardResponseDto<Object> handleBizException(BizException e) {
        System.out.println("=========[GlobalExceptionHandler][handleBizException][START] "
                + "errorCode=" + e.getCode()
                + " exception=" + e.getClass().getSimpleName());
        MDC.put(TcfMdcKeys.ERR_CODE, e.getCode());
        try {
            return etf.businessFail(null, e);
        } finally {
            System.out.println("=========[GlobalExceptionHandler][handleBizException][END] "
                    + "errorCode=" + e.getCode());
            MDC.remove(TcfMdcKeys.ERR_CODE);
        }
    }

    @ExceptionHandler(Exception.class)
    public StandardResponseDto<Object> handleException(Exception e) {
        System.out.println("=========[GlobalExceptionHandler][handleException][START] "
                + "exception=" + e.getClass().getSimpleName());
        MDC.put(TcfMdcKeys.ERR_CODE, ExceptionCodeProperties.DEFAULT_CODE);
        try {
            return etf.systemError(null, e);
        } finally {
            System.out.println("=========[GlobalExceptionHandler][handleException][END] "
                    + "exception=" + e.getClass().getSimpleName());
            MDC.remove(TcfMdcKeys.ERR_CODE);
        }
    }
}
