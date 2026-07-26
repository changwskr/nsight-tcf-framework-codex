package com.nh.nsight.marketing.oc.application.dto.env;

public record EnvSettingItemView(
        String key,
        String label,
        String guideValue,
        String actualValue,
        String source,
        String layer,
        SettingMatchStatus status,
        String note
) {
}
