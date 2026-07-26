package com.nh.nsight.marketing.oc.application.dto.env;

import java.util.List;

public record StackLayerView(
        int order,
        String layerId,
        String layerName,
        String description,
        boolean layerValid,
        List<StackSettingRow> settings
) {
}
