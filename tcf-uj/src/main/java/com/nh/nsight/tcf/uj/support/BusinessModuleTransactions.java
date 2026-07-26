package com.nh.nsight.tcf.uj.support;

import java.util.List;

public record BusinessModuleTransactions(
        String code,
        String name,
        String group,
        String contextPath,
        int localPort,
        List<BusinessTransactionInfo> transactions
) {
}
