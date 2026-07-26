package com.nh.nsight.tcf.core.support.runtime.model;

public enum RuntimeTransactionStep {
    STF,
    WAIT_HANDLER,
    HANDLER,
    FACADE,
    SERVICE,
    RULE,
    WAIT_DB_CONNECTION,
    EXECUTING_SQL,
    WAIT_EXTERNAL,
    ETF,
    COMPLETED
}
