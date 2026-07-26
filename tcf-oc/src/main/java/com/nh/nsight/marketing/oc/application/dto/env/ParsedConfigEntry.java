package com.nh.nsight.marketing.oc.application.dto.env;

public record ParsedConfigEntry(
        String fileName,
        String configKey,
        String configValue,
        String normalizedKey,
        int sourceLine
) {
}
