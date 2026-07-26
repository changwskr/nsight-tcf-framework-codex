package com.nh.nsight.gateway.support;

import com.nh.nsight.gateway.support.GatewayBusinessModules.Module;

public record RouteContext(
        Module module,
        String targetUrl,
        String enrichedBody,
        long startedAtMillis,
        int connectTimeoutMs,
        int readTimeoutMs,
        String authorizationHeader
) {
    private static final String PHASE = "RouteContext";

    public RouteContext {
        GatewayProxyTrace.start(PHASE);
        GatewayProxyTrace.log(PHASE, "module=" + module.code()
                + " targetUrl=" + targetUrl
                + " connectTimeoutMs=" + connectTimeoutMs
                + " readTimeoutMs=" + readTimeoutMs
                + " startedAtMillis=" + startedAtMillis);
        GatewayProxyTrace.end(PHASE);
    }
}
