package com.nh.nsight.tcf.core.support.control;

public record TransactionControlRule(String controlType, String blockYn) {

    public boolean isBlocking() {
        return "Y".equalsIgnoreCase(blockYn);
    }
}
