package com.nh.nsight.tcf.eai.exception;

import java.util.Map;

/**
 * 대상 서비스가 <b>업무 실패</b>(resultCode != 성공)를 반환한 경우의 연동 예외.
 *
 * <p>대상 서비스의 resultCode/메시지를 그대로 담아 전파하여, 호출 측이
 * 필요 시 자신의 업무 오류코드로 재변환할 수 있게 한다.
 */
public class IntegrationBusinessException extends IntegrationException {

    private final Map<String, Object> responseBody;

    public IntegrationBusinessException(String targetServiceId, String targetResultCode,
                                        String message, Map<String, Object> responseBody) {
        super(targetResultCode, targetServiceId, message);
        this.responseBody = responseBody;
    }

    /** 대상 서비스가 내려준 업무 resultCode. */
    public String getTargetResultCode() {
        return getErrorCode();
    }

    public Map<String, Object> getResponseBody() {
        return responseBody;
    }
}
