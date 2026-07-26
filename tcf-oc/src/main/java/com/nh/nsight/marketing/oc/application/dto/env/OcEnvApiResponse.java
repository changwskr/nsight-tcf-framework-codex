package com.nh.nsight.marketing.oc.application.dto.env;

/**
 * ENV API 응답 — tcf-ui traceenvironment.js 호환 형식.
 */
public class OcEnvApiResponse<T> {

    private ErrorBlock error;
    private BodyBlock<T> body;

    public static <T> OcEnvApiResponse<T> success(String code, String operation, T data) {
        OcEnvApiResponse<T> response = new OcEnvApiResponse<>();
        response.error = new ErrorBlock("SUCCESS", operation, code);
        response.body = new BodyBlock<>(data);
        return response;
    }

    public static <T> OcEnvApiResponse<T> fail(String code, String message) {
        OcEnvApiResponse<T> response = new OcEnvApiResponse<>();
        response.error = new ErrorBlock("FAIL", message, code);
        return response;
    }

    public ErrorBlock getError() {
        return error;
    }

    public void setError(ErrorBlock error) {
        this.error = error;
    }

    public BodyBlock<T> getBody() {
        return body;
    }

    public void setBody(BodyBlock<T> body) {
        this.body = body;
    }

    public record ErrorBlock(String resultCode, String resultMessage, String detailMessage) {
    }

    public record BodyBlock<T>(T response) {
    }
}
