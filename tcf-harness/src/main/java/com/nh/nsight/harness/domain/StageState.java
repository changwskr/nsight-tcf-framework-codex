package com.nh.nsight.harness.domain;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record StageState(
        StageStatus status,
        String artifact,
        String approvedBy,
        String approvedAt,
        String comment,
        String updatedAt
) {
    public static StageState initial(String artifact) {
        return new StageState(StageStatus.NOT_STARTED, artifact, "", "", "", OffsetDateTime.now().toString());
    }

    public StageState transition(StageStatus nextStatus, String actor, String transitionComment) {
        String approvedActor = nextStatus == StageStatus.APPROVED ? actor : approvedBy;
        String approvalTime = nextStatus == StageStatus.APPROVED ? OffsetDateTime.now().toString() : approvedAt;
        return new StageState(nextStatus, artifact, nullToEmpty(approvedActor), nullToEmpty(approvalTime),
                nullToEmpty(transitionComment), OffsetDateTime.now().toString());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status.name());
        map.put("artifact", artifact);
        map.put("approvedBy", approvedBy);
        map.put("approvedAt", approvedAt);
        map.put("comment", comment);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public static StageState fromMap(Map<String, Object> map) {
        return new StageState(
                StageStatus.valueOf(string(map.get("status"), StageStatus.NOT_STARTED.name())),
                string(map.get("artifact"), ""),
                string(map.get("approvedBy"), ""),
                string(map.get("approvedAt"), ""),
                string(map.get("comment"), ""),
                string(map.get("updatedAt"), "")
        );
    }

    private static String string(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
