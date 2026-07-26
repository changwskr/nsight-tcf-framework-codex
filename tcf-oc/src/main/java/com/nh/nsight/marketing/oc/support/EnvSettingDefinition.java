package com.nh.nsight.marketing.oc.support;

import com.nh.nsight.marketing.oc.application.dto.env.SettingMatchStatus;

import java.util.function.Function;

public record EnvSettingDefinition(
        String key,
        String categoryId,
        String categoryTitle,
        String categoryDescription,
        String label,
        String layer,
        String guideValue,
        String source,
        String propertyKey,
        MatchType matchType,
        String guideMin,
        String guideMax,
        String note
) {

    public enum MatchType {
        EXACT,
        RANGE,
        MAX,
        MIN,
        INFO_ONLY
    }

    public SettingMatchStatus evaluate(String actual) {
        if (matchType == MatchType.INFO_ONLY || actual == null || actual.isBlank()) {
            return SettingMatchStatus.INFO;
        }
        String a = actual.trim();
        String g = guideValue == null ? "" : guideValue.trim();

        return switch (matchType) {
            case EXACT -> equalsNormalized(a, g) ? SettingMatchStatus.MATCH : SettingMatchStatus.WARN;
            case RANGE -> inNumericRange(a, guideMin, guideMax) ? SettingMatchStatus.MATCH : SettingMatchStatus.WARN;
            case MAX -> compareNumeric(a, guideMax) <= 0 ? SettingMatchStatus.MATCH : SettingMatchStatus.WARN;
            case MIN -> compareNumeric(a, guideMin) >= 0 ? SettingMatchStatus.MATCH : SettingMatchStatus.WARN;
            case INFO_ONLY -> SettingMatchStatus.INFO;
        };
    }

    private static boolean equalsNormalized(String actual, String guide) {
        return normalize(actual).equals(normalize(guide));
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", "")
                .replace("m", "min")
                .toLowerCase();
    }

    private static boolean inNumericRange(String actual, String min, String max) {
        double v = parseNumber(actual);
        double lo = parseNumber(min);
        double hi = parseNumber(max);
        return v >= lo && v <= hi;
    }

    private static int compareNumeric(String actual, String bound) {
        return Double.compare(parseNumber(actual), parseNumber(bound));
    }

    private static double parseNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return Double.NaN;
        }
        String s = raw.trim().toLowerCase();
        if (s.endsWith("ms")) {
            return Double.parseDouble(s.replace("ms", "").trim());
        }
        if (s.endsWith("s") && !s.endsWith("ms")) {
            return Double.parseDouble(s.replace("s", "").trim()) * (s.contains("min") ? 60 : 1);
        }
        if (s.endsWith("m")) {
            return Double.parseDouble(s.replace("m", "").trim());
        }
        if (s.endsWith("min")) {
            return Double.parseDouble(s.replace("min", "").trim());
        }
        if (s.endsWith("h")) {
            return Double.parseDouble(s.replace("h", "").trim());
        }
        return Double.parseDouble(s.replaceAll("[^0-9.]", ""));
    }

    public static Function<EnvSettingDefinition, String> propertyResolver() {
        return EnvSettingDefinition::propertyKey;
    }
}
