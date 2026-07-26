package com.nh.nsight.tcf.ui.support;

import java.util.Map;

public record BusinessModuleInfo(
        String code,
        String name,
        String group,
        String contextPath,
        int localPort,
        String serviceId,
        String transactionCode,
        Map<String, Object> sampleRequest
) {
}
