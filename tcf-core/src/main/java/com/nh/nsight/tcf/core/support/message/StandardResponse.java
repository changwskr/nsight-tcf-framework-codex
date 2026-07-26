package com.nh.nsight.tcf.core.support.message;

import java.io.Serializable;

public class StandardResponse<T> implements Serializable {
    private StandardHeader header;
    private Result result;
    private T body;

    public static <T> StandardResponse<T> success(StandardHeader header, T body) {
        StandardResponse<T> response = new StandardResponse<>();
        response.header = header;
        response.result = Result.success();
        response.body = body;
        return response;
    }

    public static <T> StandardResponse<T> fail(StandardHeader header, String errorCode, String message, String detail) {
        StandardResponse<T> response = new StandardResponse<>();
        response.header = header;
        response.result = Result.fail(errorCode, message, detail);
        return response;
    }

    public StandardHeader getHeader() { return header; }
    public void setHeader(StandardHeader header) { this.header = header; }
    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }
    public T getBody() { return body; }
    public void setBody(T body) { this.body = body; }
}
