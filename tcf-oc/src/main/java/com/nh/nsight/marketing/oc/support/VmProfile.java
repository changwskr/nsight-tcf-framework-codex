package com.nh.nsight.marketing.oc.support;



import java.util.Arrays;

import java.util.Optional;



/**

 * VM 사양 — 용량산정 문서(2026-05-31) · §4 Tomcat·Hikari 기준 (tcf-oc support).

 * <ul>

 *   <li>VM TPS(산정): Core × Core당 TPS(30/35/40, TPMC 연동)</li>

 *   <li>VM TPS(문서 1차): {@link #guideNominalTps()} — 250/500/500/1,000</li>

 *   <li>Tomcat·Hikari: {@link #getTomcatHikariSpec()}</li>

 * </ul>

 */

public enum VmProfile {

    /** 소형 AP · PoC — 2 vCPU / 16GB. */
    CORE2_16(
            "2CORE-16GB", 2, 16, 60, 72, 80, 150, 200, 60,
            th(150, 200, 25, 40, 50, 100, 4_000, 6_000, 20, 30, 20, 28, 60,
                    "소형 VM · Thread·DB Session 상한 엄격 검증")
    ),

    /** 소형 Scale-Out — 4 vCPU / 32GB. */
    CORE4_32(
            "4CORE-32GB", 4, 32, 120, 144, 160, 280, 350, 120,
            th(280, 350, 50, 75, 100, 175, 6_000, 8_000, 35, 45, 30, 40, 120,
                    "소형·개발계 · 운영 전 성능시험 필수")
    ),

    /** 온라인 AP Scale-Out — 8 vCPU / 32GB, VM당 250 TPS. */

    CORE8_32(

            "8CORE-32GB", 8, 32, 240, 250, 320, 400, 500, 250,

            th(400, 500, 100, 150, 200, 400, 8_000, 12_000, 80, 100, 70, 80, 250,

                    "Scale-Out · DB Session 검증 필요")

    ),

    CORE8_64(

            "8CORE-64GB", 8, 64, 240, 280, 320, 400, 500, 250,

            th(400, 500, 100, 150, 200, 400, 8_000, 12_000, 80, 100, 70, 80, 250,

                    "DB Session 검증 필요")

    ),

    /** §4 — 16CORE/64GB. */

    CORE16_64(

            "16CORE-64GB", 16, 64, 480, 560, 640, 800, 1_000, 500,

            th(800, 1_000, 150, 200, 300, 500, 12_000, 16_000, 80, 100, 70, 80, 500,

                    "DB Session 검증 필요")

    ),

    /** §4 — 16CORE/128GB (Thread·Pool은 64GB와 동일). */

    CORE16_128(

            "16CORE-128GB", 16, 128, 480, 560, 640, 800, 1_000, 500,

            th(800, 1_000, 150, 200, 300, 500, 12_000, 16_000, 80, 100, 70, 80, 500,

                    "DB Session 검증 필요")

    ),

    /** §4 — 32CORE/256GB, 성능시험 1차 Tomcat 1,200~1,500. */

    CORE32_256(

            "32CORE-256GB", 32, 256, 960, 1_120, 1_280, 1_200, 1_500, 1_000,

            th(1_200, 1_500, 200, 300, 500, 800, 20_000, 30_000, 120, 150, 150, 180, 1_000,

                    "DB Session 총량 검증 없이는 적용 금지")

    );



    private final String id;

    private final int cores;

    private final int memoryGb;

    private final int vmTpsAt30;

    private final int vmTpsAt35;

    private final int vmTpsAt40;

    private final int tomcatThreadsRef;

    private final int tomcatThreadsMaxRef;

    private final int guideNominalTps;

    private final TomcatHikariSizingGuide.ProfileSpec tomcatHikariSpec;



    VmProfile(

            String id,

            int cores,

            int memoryGb,

            int vmTpsAt30,

            int vmTpsAt35,

            int vmTpsAt40,

            int tomcatThreadsRef,

            int tomcatThreadsMaxRef,

            int guideNominalTps,

            TomcatHikariSizingGuide.ProfileSpec tomcatHikariSpec

    ) {

        this.id = id;

        this.cores = cores;

        this.memoryGb = memoryGb;

        this.vmTpsAt30 = vmTpsAt30;

        this.vmTpsAt35 = vmTpsAt35;

        this.vmTpsAt40 = vmTpsAt40;

        this.tomcatThreadsRef = tomcatThreadsRef;

        this.tomcatThreadsMaxRef = tomcatThreadsMaxRef;

        this.guideNominalTps = guideNominalTps;

        this.tomcatHikariSpec = tomcatHikariSpec;

    }



    private static TomcatHikariSizingGuide.ProfileSpec th(

            int tomcatMin,

            int tomcatMax,

            int minSpareMin,

            int minSpareMax,

            int acceptMin,

            int acceptMax,

            int maxConnMin,

            int maxConnMax,

            int hikariGenMin,

            int hikariGenMax,

            int hikariSvMin,

            int hikariSvMax,

            int guideTps,

            String caution

    ) {

        return new TomcatHikariSizingGuide.ProfileSpec(

                tomcatMin, tomcatMax, minSpareMin, minSpareMax,

                acceptMin, acceptMax, maxConnMin, maxConnMax,

                hikariGenMin, hikariGenMax, hikariSvMin, hikariSvMax,

                guideTps,

                TomcatWasSizingGuide.CONNECTION_TIMEOUT_SEC,

                TomcatWasSizingGuide.KEEP_ALIVE_TIMEOUT_SEC,

                TomcatWasSizingGuide.MAX_KEEP_ALIVE_REQUESTS,

                TomcatWasSizingGuide.THREAD_STACK_XSS,

                caution

        );

    }



    public String getId() {

        return id;

    }



    public int getCores() {

        return cores;

    }



    public int getMemoryGb() {

        return memoryGb;

    }



    public int getVmTpsAt30() {

        return vmTpsAt30;

    }



    public int getVmTpsAt35() {

        return vmTpsAt35;

    }



    public int getVmTpsAt40() {

        return vmTpsAt40;

    }



    public int getGuideNominalTps() {

        return guideNominalTps;

    }



    public TomcatHikariSizingGuide.ProfileSpec getTomcatHikariSpec() {

        return tomcatHikariSpec;

    }



    public double memoryGbPerCore() {

        return cores > 0 ? (double) memoryGb / cores : 0;

    }



    public boolean isEightGbPerCoreLayout() {

        return JvmSizingGuide.isEightGbPerCore(cores, memoryGb);

    }



    public int vmTpsForBase(int tpsPerCoreBase) {

        if (tpsPerCoreBase <= 30) {

            return vmTpsAt30;

        }

        if (tpsPerCoreBase >= 40) {

            return vmTpsAt40;

        }

        return vmTpsAt35;

    }



    public VmTpsRange vmTpsRangeForTpmc(int tpmcPerTps) {

        var core = NsightCapacityDerivation.coreTpsFromTpmc(tpmcPerTps);

        return new VmTpsRange(

                cores * core.tpsPerCoreMin(),

                cores * core.tpsPerCoreBase(),

                cores * core.tpsPerCoreMax()

        );

    }



    public record VmTpsRange(int min, int base, int max) {

    }



    public int getTomcatThreadsRef() {

        return tomcatThreadsRef;

    }



    public int getTomcatThreadsMaxRef() {

        return tomcatThreadsMaxRef;

    }



    public String tomcatThreadsRangeLabel() {

        return tomcatHikariSpec.tomcatMaxThreadsRange();

    }



    public static String tomcatThreadsRangeLabel(int cores, int memoryGb) {

        return findNearestForSizing(cores, memoryGb).tomcatThreadsRangeLabel();

    }



    private static VmProfile findNearestForSizing(int cores, int memoryGb) {

        int safeCores = Math.max(1, cores);

        int safeRam = memoryGb > 0 ? memoryGb : safeCores * JvmSizingGuide.GB_PER_CORE_IAAS;

        return nearest(safeCores, safeRam);

    }



    /** @deprecated use {@link #getVmTpsAt35()} */

    public int getVmMaxTps() {

        return vmTpsAt40;

    }



    public static Optional<VmProfile> find(String id) {

        if (id == null || id.isBlank()) {

            return Optional.empty();

        }

        String norm = id.trim().toUpperCase().replace(" ", "").replace("/", "-");

        return Arrays.stream(values())

                .filter(v -> v.id.equalsIgnoreCase(id.trim())

                        || v.id.replace("-", "").equalsIgnoreCase(norm.replace("-", "")))

                .findFirst();

    }



    public static String normalizeProfileId(String id) {

        return find(id).map(VmProfile::getId).orElse(null);

    }



    public static VmProfile nearest(int cores, int memoryGb) {

        int safeCores = Math.max(1, cores);

        int safeRam = memoryGb > 0 ? memoryGb : safeCores * JvmSizingGuide.GB_PER_CORE_IAAS;

        VmProfile best = CORE8_32;

        long bestDiff = Long.MAX_VALUE;

        for (VmProfile p : values()) {

            long diff = Math.abs(p.cores - safeCores) * 1000L + Math.abs(p.memoryGb - safeRam);

            if (diff < bestDiff) {

                bestDiff = diff;

                best = p;

            }

        }

        return best;

    }



    public static VmProfile defaultProfile() {

        return CORE8_32;

    }

}

