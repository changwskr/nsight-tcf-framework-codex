package com.nh.nsight.marketing.oc.application.dto.env;

import java.util.List;

public record EnvSettingCategoryView(
        String id,
        String title,
        String description,
        List<EnvSettingItemView> items
) {
}
