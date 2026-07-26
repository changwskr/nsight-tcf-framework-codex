package com.nh.nsight.marketing.oc.application.dto.env;

/** 설계서 ENV-004 계층별 설정 점검 Grid 행. */
public record LayerGridRow(
        String layer,
        String settingLabel,
        String propertyKey,
        String recommendedValue,
        String currentValue,
        String status,
        String statusLabel,
        String reason,
        String configLocation,
        String settingExample,
        String actionGuide
) {
}
