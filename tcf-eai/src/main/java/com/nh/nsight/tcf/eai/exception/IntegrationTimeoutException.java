package com.nh.nsight.tcf.eai.exception;

/**
 * 대상 서비스 응답 Timeout 연동 예외 ({@link IntegrationErrorCode#TIMEOUT}).
 */
public class IntegrationTimeoutException extends IntegrationException {

    public IntegrationTimeoutException(String targetServiceId, String message, Throwable cause) {
        super(IntegrationErrorCode.TIMEOUT, targetServiceId, message, cause);
    }
}
