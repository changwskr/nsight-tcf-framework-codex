package com.nh.nsight.tcf.core.support.runtime.model;

public record SlowTransactionInfo(
        String guid,
        String businessCode,
        String serviceId,
        long startTimeMillis,
        long endTimeMillis,
        long elapsedMillis,
        RuntimeTransactionStep lastStep,
        String lastSqlId,
        long recordedAtMillis) {
}
