package com.nh.nsight.gateway.support;

import org.springframework.http.ResponseEntity;

public final class ProxyResponseSupport {
    private ProxyResponseSupport() {
    }

    public static ResponseEntity<String> toResponse(RouteResult result) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(result.httpStatus());
        if (result.setCookies() != null) {
            for (String setCookie : result.setCookies()) {
                builder.header("Set-Cookie", setCookie);
            }
        }
        return builder.body(result.responseBody());
    }
}
