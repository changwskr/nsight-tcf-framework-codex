package com.nh.nsight.marketing.oc.support;

/**
 * WAS / Tomcat 설정 — 32CORE·256GB (용량산정 문서).
 * <p>16CORE의 단순 2배가 아니라 VM TPS·평균 응답시간·운영 여유율로 Busy Thread를 산정합니다.</p>
 * <p>{@code Busy ≈ TPS × 응답(초) × 여유율} — 1,000 TPS × 1.0~1.2s × 1.2 → 약 1,200~1,440</p>
 */
public final class TomcatWasSizingGuide {

    public static final double AVG_RESPONSE_SEC_MIN = 1.0;
    public static final double AVG_RESPONSE_SEC_MAX = 1.2;
    public static final double OPERATIONAL_MARGIN = 1.2;

    public static final int MAX_KEEP_ALIVE_REQUESTS = 100;
    public static final String THREAD_STACK_XSS = "512k";
    public static final int CONNECTION_TIMEOUT_SEC = 8;
    public static final int KEEP_ALIVE_TIMEOUT_SEC = 60;

    public static final String OPERATIONAL_NOTE =
            "성능시험 1차 — 운영 시 Busy Thread·CPU·Hikari Pending·DB SQL Time으로 보정";
    public static final String PERF_TEST_NOTE_32C =
            "성능시험에서 maxThreads 1,200 vs 1,500 두 값 비교 권장";

    private TomcatWasSizingGuide() {
    }

    /** Busy Thread ≈ ceil(TPS × 평균응답(초) × 여유율). */
    public static int busyThreads(int vmTps, double avgResponseSec, double margin) {
        return (int) Math.ceil(vmTps * avgResponseSec * margin);
    }

    public static int busyThreadsLow(int vmTps) {
        return busyThreads(vmTps, AVG_RESPONSE_SEC_MIN, OPERATIONAL_MARGIN);
    }

    public static int busyThreadsHigh(int vmTps) {
        return busyThreads(vmTps, AVG_RESPONSE_SEC_MAX, OPERATIONAL_MARGIN);
    }

    public static String busyThreadFormula(int vmTps) {
        return "Busy≈" + vmTps + " TPS×(1.0~1.2s)×1.2 → "
                + busyThreadsLow(vmTps) + "~" + busyThreadsHigh(vmTps);
    }

    public static String maxThreadsRationale(VmProfile profile) {
        TomcatHikariSizingGuide.ProfileSpec spec = profile.getTomcatHikariSpec();
        String base = "maxThreads " + spec.tomcatMaxThreadsRange()
                + " (보수 " + spec.tomcatMaxThreadsMin() + ")";
        if (profile == VmProfile.CORE32_256) {
            return base + " · " + busyThreadFormula(profile.getGuideNominalTps()) + " · " + PERF_TEST_NOTE_32C;
        }
        return base + " · " + busyThreadFormula(profile.getGuideNominalTps());
    }

    /** ENV-003 VM당 WAS Threads(maxThreads) 산출식 (단계별). */
    public static String buildDerivationFormula(VmProfile profile) {
        TomcatHikariSizingGuide.ProfileSpec spec = profile.getTomcatHikariSpec();
        int tps = profile.getGuideNominalTps();
        int busyLow = busyThreadsLow(tps);
        int busyHigh = busyThreadsHigh(tps);
        String step7 = profile == VmProfile.CORE32_256 ? PERF_TEST_NOTE_32C : OPERATIONAL_NOTE;
        return String.join("\n",
                "① 프로파일 = " + profile.getId() + " · §4 기준 VM TPS = " + tps,
                "② Busy Thread = ceil(TPS × 평균응답(1.0~1.2s) × 운영여유율 1.2)",
                "③ Busy_low = ceil(" + tps + " × 1.0 × 1.2) = " + busyLow,
                "④ Busy_high = ceil(" + tps + " × 1.2 × 1.2) = " + busyHigh,
                "⑤ maxThreads = §4 " + spec.tomcatMaxThreadsRange()
                        + " (보수 " + spec.tomcatMaxThreadsMin() + ", Busy " + busyLow + "~" + busyHigh + " 반영)",
                "⑥ 16CORE 단순 2배 금지 · minSpare " + spec.minSpareThreadsRange()
                        + " · accept " + spec.acceptCountRange(),
                "⑦ " + step7);
    }
}
