package com.nh.nsight.tcf.core.support.error;

public class SystemException extends RuntimeException {
    private final String errorCode;

    public SystemException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
