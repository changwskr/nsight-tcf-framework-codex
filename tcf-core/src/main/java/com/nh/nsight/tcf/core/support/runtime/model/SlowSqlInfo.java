package com.nh.nsight.tcf.core.support.runtime.model;

public record SlowSqlInfo(
        String mapperId,
        String sqlId,
        String serviceId,
        long startTimeMillis,
        long endTimeMillis,
        long elapsedMillis,
        boolean success,
        long affectedRows,
        long recordedAtMillis) {
}
