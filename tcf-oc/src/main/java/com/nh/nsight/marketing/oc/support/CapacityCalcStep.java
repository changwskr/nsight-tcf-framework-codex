package com.nh.nsight.marketing.oc.support;

/**
 * 용량산정 단계 — CAP-020(TPS) ~ CAP-050(DB Pool).
 */
public enum CapacityCalcStep {

    CAP_020("020", "CAP-020 TPS"),
    CAP_030("030", "CAP-030 AP"),
    CAP_040("040", "CAP-040 WAS"),
    CAP_050("050", "CAP-050 DB Pool"),
    ALL("ALL", "전체");

    private final String code;
    private final String label;

    CapacityCalcStep(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean includesAp() {
        return this != CAP_020;
    }

    public boolean includesWas() {
        return this == CAP_040 || this == CAP_050 || this == ALL;
    }

    public boolean includesDb() {
        return this == CAP_050 || this == ALL;
    }

    public static CapacityCalcStep resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        String normalized = raw.trim().toUpperCase().replace("CAP-", "").replace("CAP", "");
        for (CapacityCalcStep step : values()) {
            if (step.code.equals(normalized) || step.name().equals(normalized)) {
                return step;
            }
        }
        throw new OcCapacityBizException("지원하지 않는 산정 단계입니다: " + raw
                + " (020, 030, 040, 050, ALL)");
    }
}
