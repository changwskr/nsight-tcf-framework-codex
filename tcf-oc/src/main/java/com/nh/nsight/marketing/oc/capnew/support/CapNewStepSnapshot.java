package com.nh.nsight.marketing.oc.capnew.support;

import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCascadeImpactCDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * STEP 저장 전·후 핵심 산정 지표 스냅샷·차이 (설계서 §13).
 */
public final class CapNewStepSnapshot {

    private static final List<MetricDef> METRICS = List.of(
            new MetricDef("totalUsers", "전체 사용자", 2),
            new MetricDef("designedSessions", "설계 세션", 2),
            new MetricDef("designPeakTps", "설계 피크 TPS", 4),
            new MetricDef("vmProfileCode", "VM Profile", 4),
            new MetricDef("vmAdjustedTps", "VM 보정 TPS", 4),
            new MetricDef("baselineTotalAp", "배포 AP", 5),
            new MetricDef("baselineApPerCenter", "센터당 AP", 5),
            new MetricDef("targetTps", "목표 TPS", 6),
            new MetricDef("deploymentAp", "배포 AP(산정)", 6),
            new MetricDef("apTps", "AP당 TPS", 6),
            new MetricDef("recommendedMaxThreads", "maxThreads", 6),
            new MetricDef("poolPerVm", "Pool/VM", 7),
            new MetricDef("totalDbSessions", "DB Session", 7),
            new MetricDef("warPoolTotalSessions", "WAR Pool 합계", 7),
            new MetricDef("overallJudgment", "종합 판정", 8));

    private CapNewStepSnapshot() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> extract(Map<String, Object> payload) {
        Map<String, String> snap = new LinkedHashMap<>();
        if (payload == null) {
            return snap;
        }
        for (MetricDef def : METRICS) {
            Map<String, Object> step = map(payload.get("step" + def.step()));
            String value = text(step.get(def.field()));
            if ("overallJudgment".equals(def.field())) {
                Map<String, Object> step8 = map(payload.get("step8"));
                Map<String, Object> headline = map(step8.get("headline"));
                value = text(headline.get("overallJudgment"));
            }
            if (!value.isEmpty()) {
                snap.put(def.field(), value);
            }
        }
        return snap;
    }

    public static List<CapNewCascadeImpactCDTO.ChangeItem> diff(
            Map<String, String> before,
            Map<String, String> after) {
        List<CapNewCascadeImpactCDTO.ChangeItem> changes = new ArrayList<>();
        for (MetricDef def : METRICS) {
            String prev = before.get(def.field());
            String next = after.get(def.field());
            if (Objects.equals(prev, next)) {
                continue;
            }
            if (prev == null && next == null) {
                continue;
            }
            CapNewCascadeImpactCDTO.ChangeItem item = new CapNewCascadeImpactCDTO.ChangeItem();
            item.setFieldId(def.field());
            item.setLabel(def.label());
            item.setBeforeValue(prev == null ? "-" : prev);
            item.setAfterValue(next == null ? "-" : next);
            item.setAffectedStep(def.step());
            changes.add(item);
        }
        return changes;
    }

    public static String formatPrimaryChange(int sourceStep, List<CapNewCascadeImpactCDTO.ChangeItem> changes) {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        return switch (sourceStep) {
            case 2 -> findChange(changes, "totalUsers", "designedSessions");
            case 3 -> findChange(changes, "designPeakTps");
            case 4 -> findChange(changes, "vmProfileCode", "vmAdjustedTps");
            case 5 -> findChange(changes, "baselineTotalAp", "baselineApPerCenter");
            case 6 -> findChange(changes, "recommendedMaxThreads", "apTps");
            case 7 -> findChange(changes, "poolPerVm", "totalDbSessions", "warPoolTotalSessions");
            default -> changes.get(0).getLabel() + " " + changes.get(0).getBeforeValue()
                    + " → " + changes.get(0).getAfterValue();
        };
    }

    private static String findChange(List<CapNewCascadeImpactCDTO.ChangeItem> changes, String... fieldIds) {
        for (String fieldId : fieldIds) {
            for (CapNewCascadeImpactCDTO.ChangeItem item : changes) {
                if (fieldId.equals(item.getFieldId())) {
                    return item.getLabel() + " " + item.getBeforeValue() + " → " + item.getAfterValue();
                }
            }
        }
        CapNewCascadeImpactCDTO.ChangeItem first = changes.get(0);
        return first.getLabel() + " " + first.getBeforeValue() + " → " + first.getAfterValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static String text(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private record MetricDef(String field, String label, int step) {
    }
}
