package com.nh.nsight.gateway.application.rule;

public class GatewayAuthException extends RuntimeException {
    private final int httpStatus;

    public GatewayAuthException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
