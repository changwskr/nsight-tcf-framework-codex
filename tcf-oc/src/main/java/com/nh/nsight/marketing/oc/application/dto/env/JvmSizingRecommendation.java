package com.nh.nsight.marketing.oc.application.dto.env;

/** ENV-004 JVM Heap·GC 사이징 권장 (VM 프로파일 연동). */
public record JvmSizingRecommendation(
        String vmProfileId,
        int vmCores,
        int vmMemoryGb,
        int heapGeneralMinGb,
        int heapGeneralMaxGb,
        int heapSingleViewMaxGb,
        String gcAlgorithm,
        int maxGcPauseMillis,
        String threadStackSize,
        int metaspaceSizeMb,
        int maxMetaspaceSizeMb,
        String heapRatioNote,
        String exampleOptsGeneral,
        String exampleOptsSingleView,
        String sizingNote
) {
}
