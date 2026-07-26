package com.nh.nsight.marketing.oc.support;

import com.nh.nsight.marketing.oc.application.dto.env.JvmSizingRecommendation;

/**
 * JVM Heap·GC 권장 — NSIGHT 용량산정 문서(2026-05-31) 및 IaaS 코어당 8GB RAM 원칙.
 * <p>입력한 VM 코어·RAM에 따라 산정합니다. Heap은 RAM 전체를 쓰지 않으며, 코어당 8GB가 아닌
 * 8CORE/32GB(온라인 Scale-Out 표준)는 별도 앵커를 사용합니다.</p>
 * <ul>
 *   <li>코어당 8GB RAM ({@value #GB_PER_CORE_IAAS}GB/Core): 일반 Heap Xms ≈ 1.5×Core, Xmx ≈ 1.75×Core (32C는 32~48GB 상한)</li>
 *   <li>SingleView Heap 상한: RAM 앵커 보간 (32→14, 64→28, 128→40, 256→64 GB)</li>
 * </ul>
 */
public final class JvmSizingGuide {

    /** IaaS VM 메모리 배분: 1 Core 당 8GB RAM (8C→64G, 16C→128G, 32C→256G). */
    public static final int GB_PER_CORE_IAAS = 8;

    /** 8CORE/32GB 온라인 표준 — 코어당 4GB RAM, Heap 12~14GB. */
    private static final int HEAP_8C32G_MIN = 12;
    private static final int HEAP_8C32G_MAX = 14;
    private static final int HEAP_SV_8C32G = 14;

    private JvmSizingGuide() {
    }

    public static JvmSizingRecommendation recommend(VmProfile vm) {
        return recommend(vm.getCores(), vm.getMemoryGb(), vm.getId());
    }

    /**
     * @param cores      vCPU
     * @param memoryGb   VM RAM(GB). 0 이하이면 cores × {@link #GB_PER_CORE_IAAS}
     * @param profileLabel 화면 표시용 (null 가능)
     */
    public static JvmSizingRecommendation recommend(int cores, int memoryGb, String profileLabel) {
        int safeCores = Math.max(1, cores);
        int safeRam = memoryGb > 0 ? memoryGb : safeCores * GB_PER_CORE_IAAS;
        HeapRange general = computeHeapGeneral(safeCores, safeRam);
        int svMaxGb = computeHeapSingleViewMaxGb(safeCores, safeRam);
        String id = profileLabel != null && !profileLabel.isBlank()
                ? profileLabel
                : safeCores + "CORE-" + safeRam + "GB";
        String layout = describeMemoryLayout(safeCores, safeRam);
        String heapRatioNote = "VM RAM " + safeRam + "GB (" + layout + ") — 일반 Heap "
                + percent(general.minGb(), safeRam) + "~" + percent(general.maxGb(), safeRam)
                + "%, SingleView ≤ " + percent(svMaxGb, safeRam) + "%";
        return new JvmSizingRecommendation(
                id,
                safeCores,
                safeRam,
                general.minGb(),
                general.maxGb(),
                svMaxGb,
                "G1GC",
                200,
                "512k",
                metaspaceSizeMb(safeRam),
                maxMetaspaceSizeMb(safeRam),
                heapRatioNote,
                exampleGeneral(general),
                exampleSingleView(svMaxGb, metaspaceSizeMb(safeRam), maxMetaspaceSizeMb(safeRam)),
                sizingNote(safeCores, safeRam, layout)
        );
    }

    public static JvmSizingRecommendation recommend(int cores, int memoryGb) {
        return recommend(cores, memoryGb, null);
    }

    /**
     * 코어당 8GB RAM 여부 (±1GB 허용).
     */
    public static boolean isEightGbPerCore(int cores, int memoryGb) {
        if (cores <= 0 || memoryGb <= 0) {
            return false;
        }
        int expected = cores * GB_PER_CORE_IAAS;
        return Math.abs(memoryGb - expected) <= 1;
    }

    /**
     * 일반 AP Heap (GB). 코어당 8GB일 때: min=max(12, round(core×1.5)), max=max(min+2, round(core×1.75));
     * 32Core는 문서 하한 32GB·상한 48GB.
     */
    public static HeapRange computeHeapGeneral(int cores, int memoryGb) {
        int safeCores = Math.max(1, cores);
        int safeRam = memoryGb > 0 ? memoryGb : safeCores * GB_PER_CORE_IAAS;

        if (safeCores <= 8 && safeRam <= 40) {
            return new HeapRange(HEAP_8C32G_MIN, HEAP_8C32G_MAX);
        }
        if (isEightGbPerCore(safeCores, safeRam)) {
            int minGb = Math.max(12, (int) Math.round(safeCores * 1.5));
            int maxGb = Math.max(minGb + 2, (int) Math.round(safeCores * 1.75));
            if (safeCores >= 32) {
                minGb = Nsight32Core256GbGuide.JVM_HEAP_GENERAL_GB_MIN;
                maxGb = Math.min(
                        Nsight32Core256GbGuide.JVM_HEAP_GENERAL_GB_MAX,
                        Math.max(minGb, maxGb));
            }
            return new HeapRange(minGb, maxGb);
        }
        return interpolateGeneralHeap(safeRam);
    }

    /** SingleView Heap Xmx 상한(GB) — RAM 앵커 선형 보간. */
    public static int computeHeapSingleViewMaxGb(int cores, int memoryGb) {
        int safeCores = Math.max(1, cores);
        int safeRam = memoryGb > 0 ? memoryGb : safeCores * GB_PER_CORE_IAAS;
        if (safeCores <= 8 && safeRam <= 40) {
            return HEAP_SV_8C32G;
        }
        return interpolateSvHeap(safeRam);
    }

    private static HeapRange interpolateGeneralHeap(int memoryGb) {
        int minGb = piecewise(memoryGb, 32, 64, 128, 256, 12, 24, 32, 32);
        int maxGb = piecewise(memoryGb, 32, 64, 128, 256, 14, 28, 40, 48);
        return new HeapRange(Math.min(minGb, maxGb), maxGb);
    }

    private static int interpolateSvHeap(int memoryGb) {
        return piecewise(memoryGb, 32, 64, 128, 256, 14, 28, 40, 64);
    }

    private static int piecewise(int ram, int r0, int r1, int r2, int r3, int v0, int v1, int v2, int v3) {
        if (ram <= r0) {
            return v0;
        }
        if (ram <= r1) {
            return v0 + (v1 - v0) * (ram - r0) / (r1 - r0);
        }
        if (ram <= r2) {
            return v1 + (v2 - v1) * (ram - r1) / (r2 - r1);
        }
        if (ram <= r3) {
            return v2 + (v3 - v2) * (ram - r2) / (r3 - r2);
        }
        return v3;
    }

    private static int metaspaceSizeMb(int memoryGb) {
        return memoryGb >= 128 ? 512 : 256;
    }

    private static int maxMetaspaceSizeMb(int memoryGb) {
        return memoryGb >= 128 ? 2048 : 512;
    }

    private static String describeMemoryLayout(int cores, int memoryGb) {
        if (cores <= 8 && memoryGb <= 40) {
            return "8CORE/32GB 표준 · Scale-Out";
        }
        if (isEightGbPerCore(cores, memoryGb)) {
            return "코어당 " + GB_PER_CORE_IAAS + "GB RAM";
        }
        return String.format("%.1fGB/Core", memoryGb / (double) cores);
    }

    private static String sizingNote(int cores, int memoryGb, String layout) {
        if (cores >= 32 && isEightGbPerCore(cores, memoryGb)) {
            return "32Core·256GB — 특수 고부하 검토 VM. Heap 32~48GB, 256GB 전체 Heap 사용 금지";
        }
        if (isEightGbPerCore(cores, memoryGb)) {
            return "코어당 8GB RAM — Heap=Core×(1.5~1.75)GB 권장. Thread·Pool 상향 시 GC·Metaspace 동시 검토";
        }
        return layout + " — RAM 비율 비표준, 문서 앵커 보간 적용";
    }

    private static String exampleGeneral(HeapRange range) {
        return "-Xms" + range.minGb() + "g -Xmx" + range.maxGb() + "g -Xss512k "
                + "-XX:+UseG1GC -XX:MaxGCPauseMillis=200 "
                + "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump "
                + "-Xlog:gc*:file=/logs/gc/ap-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M";
    }

    private static String exampleSingleView(int svMaxGb, int metaMb, int metaMaxMb) {
        int xms = Math.max(12, Math.min(32, svMaxGb - 8));
        return "-Xms" + xms + "g -Xmx" + svMaxGb + "g -Xss512k "
                + "-XX:+UseG1GC -XX:MaxGCPauseMillis=200 "
                + "-XX:MetaspaceSize=" + metaMb + "m -XX:MaxMetaspaceSize=" + metaMaxMb + "m "
                + "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump";
    }

    private static int percent(int gb, int ramGb) {
        if (ramGb <= 0) {
            return 0;
        }
        return Math.round(100f * gb / ramGb);
    }

    public static String heapGeneralRangeLabel(int cores, int memoryGb) {
        HeapRange r = computeHeapGeneral(cores, memoryGb);
        return r.minGb() + "~" + r.maxGb() + " GB";
    }

    public static String heapGeneralRangeLabel(VmProfile vm) {
        return heapGeneralRangeLabel(vm.getCores(), vm.getMemoryGb());
    }

    public static String heapSingleViewLabel(int cores, int memoryGb) {
        return "≤" + computeHeapSingleViewMaxGb(cores, memoryGb) + " GB";
    }

    public static String heapSingleViewLabel(VmProfile vm) {
        return heapSingleViewLabel(vm.getCores(), vm.getMemoryGb());
    }

    /** ENV-003 VM당 JVM Heap 산출식 (단계별). */
    public static String buildDerivationFormula(int cores, int memoryGb, String profileLabel) {
        int safeCores = Math.max(1, cores);
        int safeRam = memoryGb > 0 ? memoryGb : safeCores * GB_PER_CORE_IAAS;
        HeapRange general = computeHeapGeneral(safeCores, safeRam);
        int svMaxGb = computeHeapSingleViewMaxGb(safeCores, safeRam);
        String id = profileLabel != null && !profileLabel.isBlank()
                ? profileLabel
                : safeCores + "CORE-" + safeRam + "GB";
        String layout = describeMemoryLayout(safeCores, safeRam);

        if (safeCores <= 8 && safeRam <= 40) {
            return String.join("\n",
                    "① VM = " + id + " · " + safeCores + " vCPU / " + safeRam + " GB RAM",
                    "② RAM 배분 = " + layout,
                    "③ 일반 Heap = 8CORE/32GB §4 앵커 (고정 12~14 GB, 코어×1.5 산식 미적용)",
                    "④ SV Xmx ≤ " + svMaxGb + " GB",
                    "⑤ VM당 JVM Heap = " + general.minGb() + "~" + general.maxGb() + " GB",
                    "⑥ RSS = Heap + Metaspace + (Tomcat Thread×Xss) — 256GB 전체 Heap 사용 금지");
        }
        if (isEightGbPerCore(safeCores, safeRam)) {
            int rawXms = Math.max(12, (int) Math.round(safeCores * 1.5));
            int rawXmx = Math.max(rawXms + 2, (int) Math.round(safeCores * 1.75));
            String step32 = safeCores >= 32
                    ? " → 32Core 문서 하한·상한 적용 후 Xms=" + general.minGb() + "GB, Xmx=" + general.maxGb() + "GB"
                    : "";
            return String.join("\n",
                    "① VM = " + id + " · " + safeCores + " vCPU / " + safeRam + " GB RAM",
                    "② RAM 배분 = " + layout + " (IaaS 코어당 " + GB_PER_CORE_IAAS + "GB)",
                    "③ Xms = max(12, round(" + safeCores + "×1.5)) = " + rawXms + " GB" + step32,
                    "④ Xmx = max(Xms+2, round(" + safeCores + "×1.75)) = " + rawXmx + " GB"
                            + (safeCores >= 32 ? " → 상한 " + Nsight32Core256GbGuide.JVM_HEAP_GENERAL_GB_MAX + "GB" : ""),
                    "⑤ VM당 JVM Heap = " + general.minGb() + "~" + general.maxGb() + " GB",
                    "⑥ SV Xmx ≤ " + svMaxGb + " GB (RAM 앵커 보간)",
                    "⑦ Thread·Pool 상향 시 GC·Metaspace·RSS 동시 검토");
        }
        return String.join("\n",
                "① VM = " + id + " · " + safeCores + " vCPU / " + safeRam + " GB RAM",
                "② RAM 배분 = " + layout + " (코어당 8GB 아님 → 문서 앵커 보간)",
                "③ 일반 Heap = RAM " + safeRam + "GB 앵커 보간 → " + general.minGb() + "~" + general.maxGb() + " GB",
                "④ SV Xmx ≤ " + svMaxGb + " GB",
                "⑤ VM당 JVM Heap = " + general.minGb() + "~" + general.maxGb() + " GB",
                "⑥ 256GB VM 전체를 Heap으로 쓰지 않음");
    }

    public record HeapRange(int minGb, int maxGb) {
    }
}
