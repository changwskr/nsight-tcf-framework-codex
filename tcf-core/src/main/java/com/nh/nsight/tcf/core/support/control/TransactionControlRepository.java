package com.nh.nsight.tcf.core.support.control;

import java.util.Optional;

public interface TransactionControlRepository {

    boolean isGlobalUnblockActive();

    Optional<TransactionControlRule> findRule(TransactionControlHeader header);
}
