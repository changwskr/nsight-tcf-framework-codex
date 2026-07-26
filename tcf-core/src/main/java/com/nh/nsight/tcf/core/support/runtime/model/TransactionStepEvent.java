package com.nh.nsight.tcf.core.support.runtime.model;

public record TransactionStepEvent(
        String stepKey,
        String label,
        long timestampMillis,
        Long durationMs,
        boolean highlight) {

    public TransactionStepEvent withDuration(long durationMs) {
        return new TransactionStepEvent(stepKey, label, timestampMillis, durationMs, highlight);
    }

    public TransactionStepEvent withHighlight(boolean highlight) {
        return new TransactionStepEvent(stepKey, label, timestampMillis, durationMs, highlight);
    }
}
