package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.application.dto.env.ParsedConfigEntry;

import java.util.List;
import java.util.Locale;

public final class ConfigFileResolver {

    private ConfigFileResolver() {
    }

    public static String resolveFile(List<ParsedConfigEntry> entries, String propertyKey, String guideSource) {
        if (propertyKey == null || propertyKey.isBlank()) {
            return defaultFileFromGuideSource(guideSource);
        }
        if (entries != null) {
            String norm = propertyKey.toLowerCase(Locale.ROOT);
            for (ParsedConfigEntry entry : entries) {
                if (norm.equals(entry.normalizedKey())
                        || norm.equals(entry.configKey())
                        || entry.configKey().equals(propertyKey)) {
                    return entry.fileName();
                }
            }
        }
        return defaultFileFromGuideSource(guideSource);
    }

    public static String defaultFileFromGuideSource(String guideSource) {
        if (guideSource == null || guideSource.isBlank()) {
            return "—";
        }
        int sep = guideSource.indexOf('·');
        return (sep > 0 ? guideSource.substring(0, sep) : guideSource).trim();
    }
}
