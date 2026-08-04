package com.nh.nsight.harness.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public record TestCommand(String id, String command, boolean approved, int timeoutSeconds) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("command", command);
        map.put("approved", approved);
        map.put("timeoutSeconds", timeoutSeconds);
        return map;
    }

    public static TestCommand fromMap(Map<String, Object> map) {
        Object approvedValue = map.get("approved");
        Object timeoutValue = map.get("timeoutSeconds");
        return new TestCommand(
                String.valueOf(map.getOrDefault("id", "")),
                String.valueOf(map.getOrDefault("command", "")),
                approvedValue instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(approvedValue)),
                timeoutValue instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(timeoutValue))
        );
    }
}
