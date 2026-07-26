package com.nh.nsight.tcf.eai.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 서비스 간 연동 호출 결과 모델.
 *
 * <p>대상 서비스의 표준 응답 전문({@code result}, {@code body})을 호출자 관점으로 정규화한 결과.
 */
public class IntegrationCallResult {

    private final boolean success;
    private final String resultCode;
    private final String message;
    private final Map<String, Object> body;
    private final long elapsedMs;

    public IntegrationCallResult(boolean success, String resultCode, String message,
                                 Map<String, Object> body, long elapsedMs) {
        this.success = success;
        this.resultCode = resultCode;
        this.message = message;
        this.body = body != null ? body : new LinkedHashMap<>();
        this.elapsedMs = elapsedMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    /** 대상 서비스 응답 body (Map). */
    public Map<String, Object> getBody() {
        return body;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }
}
