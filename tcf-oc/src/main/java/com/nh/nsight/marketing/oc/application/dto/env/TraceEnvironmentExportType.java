package com.nh.nsight.marketing.oc.application.dto.env;

public enum TraceEnvironmentExportType {
    ENV003,
    ENV004,
    CHECK,
    RULE_CHECK;

    public static TraceEnvironmentExportType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("exportType이 필요합니다.");
        }
        String normalized = raw.trim().toUpperCase().replace('-', '_');
        try {
            return TraceEnvironmentExportType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("지원하지 않는 exportType: " + raw);
        }
    }
}
