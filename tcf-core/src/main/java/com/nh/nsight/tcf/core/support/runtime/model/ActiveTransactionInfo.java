package com.nh.nsight.tcf.core.support.runtime.model;

public record ActiveTransactionInfo(
        String guid,
        String traceId,
        String businessCode,
        String serviceId,
        String transactionCode,
        String threadName,
        long threadId,
        long startTimeMillis,
        RuntimeTransactionStep currentStep,
        String currentSqlId,
        long dbWaitStartMillis,
        String externalSystemCode) {

    public long elapsedMillis() {
        return Math.max(0, System.currentTimeMillis() - startTimeMillis);
    }

    public ActiveTransactionInfo withStep(RuntimeTransactionStep step) {
        return new ActiveTransactionInfo(guid, traceId, businessCode, serviceId, transactionCode,
                threadName, threadId, startTimeMillis, step, currentSqlId, dbWaitStartMillis,
                externalSystemCode);
    }

    public ActiveTransactionInfo withSqlId(String sqlId) {
        return new ActiveTransactionInfo(guid, traceId, businessCode, serviceId, transactionCode,
                threadName, threadId, startTimeMillis, currentStep, sqlId, dbWaitStartMillis,
                externalSystemCode);
    }

    public ActiveTransactionInfo withDbWaitStart(long dbWaitStartMillis) {
        return new ActiveTransactionInfo(guid, traceId, businessCode, serviceId, transactionCode,
                threadName, threadId, startTimeMillis, RuntimeTransactionStep.WAIT_DB_CONNECTION,
                currentSqlId, dbWaitStartMillis, externalSystemCode);
    }

    public ActiveTransactionInfo withExternalSystem(String externalSystemCode) {
        return new ActiveTransactionInfo(guid, traceId, businessCode, serviceId, transactionCode,
                threadName, threadId, startTimeMillis, RuntimeTransactionStep.WAIT_EXTERNAL,
                currentSqlId, dbWaitStartMillis, externalSystemCode);
    }
}
