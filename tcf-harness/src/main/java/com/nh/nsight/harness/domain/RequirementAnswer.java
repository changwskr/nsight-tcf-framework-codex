package com.nh.nsight.harness.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public record RequirementAnswer(String questionId, String question, String answer, String answeredAt) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("questionId", questionId);
        map.put("question", question);
        map.put("answer", answer);
        map.put("answeredAt", answeredAt);
        return map;
    }

    public static RequirementAnswer fromMap(Map<String, Object> map) {
        return new RequirementAnswer(
                String.valueOf(map.getOrDefault("questionId", "")),
                String.valueOf(map.getOrDefault("question", "")),
                String.valueOf(map.getOrDefault("answer", "")),
                String.valueOf(map.getOrDefault("answeredAt", ""))
        );
    }
}
