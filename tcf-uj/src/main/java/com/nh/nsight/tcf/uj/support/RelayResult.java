package com.nh.nsight.tcf.uj.support;

import java.util.List;

public record RelayResult(
        String businessCode,
        String targetUrl,
        int httpStatus,
        long elapsedMs,
        String responseBody,
        List<String> setCookies
) {
    public RelayResult(String businessCode, String targetUrl, int httpStatus, long elapsedMs, String responseBody) {
        this(businessCode, targetUrl, httpStatus, elapsedMs, responseBody, List.of());
    }
}
