package com.nh.nsight.tcf.core.support.idempotency;

import com.nh.nsight.tcf.core.support.message.StandardHeader;

public interface IdempotencyChecker {
    void checkAndMarkProcessing(StandardHeader header);
    void markSuccess(StandardHeader header);
    void markFail(StandardHeader header);
}
