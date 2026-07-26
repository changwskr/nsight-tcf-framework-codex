package com.nh.nsight.marketing.oc.application.dto.env;

public record StackSettingRow(
        String settingLabel,
        String propertyKey,
        String configFile,
        String actualValue,
        String recommendedValue,
        String status,
        String statusLabel,
        String reason,
        String settingExample,
        String actionGuide,
        String note
) {
}
