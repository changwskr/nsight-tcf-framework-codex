package com.nh.nsight.gateway.support;

public class GatewayRouteNotFoundException extends RuntimeException {
    private final String envCode;
    private final String businessCode;

    public GatewayRouteNotFoundException(String envCode, String businessCode) {
        super("라우팅 테이블에 등록되지 않은 업무입니다. envCode=" + envCode + ", businessCode=" + businessCode);
        this.envCode = envCode;
        this.businessCode = businessCode;
    }

    public String envCode() {
        return envCode;
    }

    public String businessCode() {
        return businessCode;
    }
}
