package com.nh.nsight.marketing.oc.application.dto.env;

import java.util.List;

public record TimeoutMapView(
        String runId,
        String chainRuleId,
        boolean chainValid,
        String chainSummary,
        List<TimeoutMapNode> nodes
) {
}
