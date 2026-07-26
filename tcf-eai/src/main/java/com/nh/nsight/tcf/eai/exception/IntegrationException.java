package com.nh.nsight.tcf.eai.exception;

/**
 * 서비스 간 연동 기술 오류의 기본 예외.
 *
 * <p>대상 서버 다운, 전문 형식 오류 등 <b>시스템성</b> 연동 오류를 표현한다.
 * 호출 측 업무 계층은 필요 시 이 예외를 잡아 자신의 업무 오류로 변환할 수 있다.
 */
public class IntegrationException extends RuntimeException {

    private final String errorCode;
    private final String targetServiceId;

    public IntegrationException(String errorCode, String targetServiceId, String message) {
        super(message);
        this.errorCode = errorCode;
        this.targetServiceId = targetServiceId;
    }

    public IntegrationException(String errorCode, String targetServiceId, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.targetServiceId = targetServiceId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTargetServiceId() {
        return targetServiceId;
    }
}
