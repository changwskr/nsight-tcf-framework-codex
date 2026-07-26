package com.nh.nsight.gateway.support;

public record GatewayTransactionLogEntry(
        String logId,
        String txTime,
        String envCode,
        String businessCode,
        String serviceId,
        String transactionCode,
        String guid,
        String traceId,
        String userId,
        String branchId,
        String sessionId,
        String targetUrl,
        int httpStatus,
        String resultStatus,
        String resultCode,
        String errorCode,
        long elapsedTimeMs,
        String phase
) {
}
