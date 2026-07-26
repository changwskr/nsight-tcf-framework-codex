package com.nh.nsight.marketing.oc.support;

/**
 * VM당 Hikari DB Pool — §4 Tomcat·Hikari 기준표(프로파일 앵커).
 * <ul>
 *   <li>16CORE/64·128GB: 일반 80~100, SV 70~80</li>
 *   <li>32CORE/256GB: 일반 120~150, SV 150~180</li>
 *   <li>합계 = 권장 VM × VM당 Pool · Pool ≤ Tomcat maxThreads · DB Session 검증</li>
 * </ul>
 */
public final class DbPoolSizingGuide {

    /** Tomcat maxThreads 대비 Pool 참고 비율 (§4 프로파일 앵커와 정합). */
    public static final double POOL_TO_THREAD_RATIO = 0.10;

    private DbPoolSizingGuide() {
    }

    public record Recommendation(
            int recommendedGeneral,
            int minGeneral,
            int maxGeneral,
            int minSingleView,
            int maxSingleView,
            int tomcatThreadsMaxRef,
            int poolCandidateFromThreads,
            String rangeLabel,
            String formulaSummary,
            String derivationFormula,
            String sizingNote
    ) {
    }

    public static Recommendation recommend(VmProfile profile, int cores, int memoryGb) {
        VmProfile p = profile != null ? profile : VmProfile.nearest(cores, memoryGb);
        TomcatHikariSizingGuide.ProfileSpec spec = p.getTomcatHikariSpec();
        int tomcatMax = spec.tomcatMaxThreadsMax();
        int candidate = poolCandidateFromThreads(tomcatMax);
        int clampedInRange = clampToProfileRange(candidate, spec.hikariGeneralMin(), spec.hikariGeneralMax());
        int recommended = spec.hikariGeneralRecommended();
        String range = spec.hikariGeneralRange();
        String derivation = buildDerivation(p, spec, tomcatMax, candidate, clampedInRange, recommended);
        String formula = "VM당 Pool = " + recommended
                + " · " + spec.hikariGeneralFormula(p)
                + " · 합계 = 권장VM × " + recommended;
        String note = spec.cautionNote() + " · Pool ≤ maxThreads " + tomcatMax;
        return new Recommendation(
                recommended,
                spec.hikariGeneralMin(),
                spec.hikariGeneralMax(),
                spec.hikariSingleViewMin(),
                spec.hikariSingleViewMax(),
                tomcatMax,
                candidate,
                range,
                formula,
                derivation,
                note
        );
    }

    /** floor(maxThreads × 10%) — §4 앵커 산출 참고값. */
    public static int poolCandidateFromThreads(int tomcatMaxThreads) {
        return (int) Math.floor(tomcatMaxThreads * POOL_TO_THREAD_RATIO);
    }

    static int clampToProfileRange(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    static String buildDerivation(
            VmProfile profile,
            TomcatHikariSizingGuide.ProfileSpec spec,
            int tomcatMax,
            int candidate,
            int clampedInRange,
            int recommended
    ) {
        return String.join("\n",
                "① 프로파일 = " + profile.getId() + " · §4 Hikari 일반 " + spec.hikariGeneralRange(),
                "② Tomcat maxThreads 권장 상한 = " + tomcatMax + " → Pool ≤ maxThreads (업무 Thread 수 초과 금지)",
                "③ 참고 = floor(" + tomcatMax + " × " + (int) (POOL_TO_THREAD_RATIO * 100) + "%) = " + candidate,
                "④ §4 범위 적용 = clamp(" + candidate + ", " + spec.hikariGeneralMin() + ", "
                        + spec.hikariGeneralMax() + ") = " + clampedInRange,
                "⑤ VM당 DB Pool 권장 = " + recommended + " (§4 범위 상한 · 성능시험·DB Session 검증 후 조정)",
                "⑥ SV AP = " + spec.hikariSingleViewRange() + " · 합계 = 권장 VM × " + recommended + " ≤ DB Session 한도");
    }

    public static Recommendation recommend(int cores, int memoryGb) {
        return recommend(VmProfile.nearest(cores, memoryGb), cores, memoryGb);
    }
}
