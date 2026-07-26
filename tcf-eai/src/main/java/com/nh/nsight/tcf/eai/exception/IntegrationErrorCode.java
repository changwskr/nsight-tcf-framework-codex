package com.nh.nsight.tcf.eai.exception;

/**
 * 서비스 간 연동(EAI) 표준 오류코드.
 *
 * <p>연동 계층에서 발생하는 기술 오류를 업무 오류와 분리하여 표준 코드로 표현한다.
 * 대상 서비스의 <b>업무 오류</b>는 대상 resultCode 를 그대로 전파한다.
 */
public final class IntegrationErrorCode {

    /** 대상 응답 Timeout (read/connect timeout). */
    public static final String TIMEOUT = "E-TCF-IF-0001";

    /** 대상 서버 연결 불가/다운 등 시스템 오류. */
    public static final String SYSTEM = "E-TCF-IF-0002";

    /** 응답 전문 파싱 오류(형식 불일치). */
    public static final String MESSAGE = "E-TCF-MSG-0001";

    private IntegrationErrorCode() {
    }
}
