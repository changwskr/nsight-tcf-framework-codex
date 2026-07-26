package com.nh.nsight.tcf.core.support.logging;

/**
 * STF/ETF 거래 종료 시 DB에 적재하는 트랜잭션 로그 레코드.
 */
public record TransactionLogRecord(
        String logId,
        String txTime,
        String businessCode,
        String serviceId,
        String transactionCode,
        String guid,
        String traceId,
        String userId,
        String branchId,
        String resultStatus,
        String resultCode,
        String errorCode,
        long elapsedTimeMs) {
}
