package nhnis.fw.tcf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nhnis.fw.tcf.dto.ProcessingType;

/**
 * 표준 전문 거래 선언. 이 애노테이션이 붙은 컨트롤러 메서드가 STF/ETF의 대상이 된다.
 *
 * <p>여기 적은 값은 거래 정의이며, 클라이언트가 Header에 보내지 않은 항목을 STF가 이 값으로 채운다.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TcfTransaction {

    /** 거래 식별자. 예: MP.SalesTip.list */
    String serviceId();

    /** 화면·거래 코드. 예: MP-INQ-0001 */
    String transactionCode();

    /** 처리 유형 */
    ProcessingType processingType();

    /** 거래명. 로그와 응답 Header에 실린다. */
    String serviceName() default "";

    /** 업무 코드. 비우면 serviceId의 첫 토큰을 쓴다. */
    String businessCode() default "";
}
