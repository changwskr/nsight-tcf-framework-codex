package com.nh.nsight.gateway.support;

import java.util.List;

public record RouteResult(
        String targetUrl,
        int httpStatus,
        long elapsedMs,
        String responseBody,
        List<String> setCookies) {
    private static final String PHASE = "RouteResult";

    public RouteResult {
        GatewayProxyTrace.start(PHASE);
        GatewayProxyTrace.log(PHASE, "targetUrl=" + targetUrl
                + " httpStatus=" + httpStatus
                + " elapsedMs=" + elapsedMs
                + " setCookies=" + (setCookies == null ? 0 : setCookies.size()));
        GatewayProxyTrace.end(PHASE);
    }
}
