package com.nh.nsight.marketing.oc.application.dto.env;

import java.util.List;

public record CapacityDesignView(
        String scenarioId,
        CapacityPlannerResult planner,
        List<StackLayerView> stackLayers,
        List<LayerGridRow> layerGrid,
        JvmSizingRecommendation jvmSizing,
        boolean stackValid,
        int activeResponseTimeoutSec,
        int activeSessionMinutes
) {
}
