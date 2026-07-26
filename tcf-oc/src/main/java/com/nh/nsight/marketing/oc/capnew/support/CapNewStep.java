package com.nh.nsight.marketing.oc.capnew.support;

public enum CapNewStep {
    BASIC(1, "프로젝트 기본정보"),
    USER_SESSION(2, "사용자·세션 조건"),
    TPS(3, "동시요청·TPS 시나리오"),
    VM(4, "업무복잡도·CPU·VM"),
    AP_DR(5, "AP 대수·센터·DR"),
    WAS(6, "WAS Thread·JVM"),
    DB_POOL(7, "DB Pool·DB Session"),
    SUMMARY(8, "종합 결과·비교·확정");

    private final int number;
    private final String label;

    CapNewStep(int number, String label) {
        this.number = number;
        this.label = label;
    }

    public int getNumber() {
        return number;
    }

    public String getLabel() {
        return label;
    }

    public static CapNewStep resolve(int stepNumber) {
        for (CapNewStep step : values()) {
            if (step.number == stepNumber) {
                return step;
            }
        }
        throw new CapNewBizException("유효하지 않은 단계입니다: " + stepNumber);
    }
}
