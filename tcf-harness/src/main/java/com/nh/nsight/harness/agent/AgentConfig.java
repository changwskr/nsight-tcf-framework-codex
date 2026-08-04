package com.nh.nsight.harness.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AgentConfig(
        boolean enabled,
        List<String> command,
        int timeoutSeconds,
        Map<String, String> environment
) {
    public AgentConfig {
        command = command == null ? List.of() : List.copyOf(command);
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        if (timeoutSeconds <= 0) timeoutSeconds = 1800;
    }

    public static AgentConfig disabled() {
        return new AgentConfig(false, List.of(), 1800, Map.of());
    }

    @SuppressWarnings("unchecked")
    public static AgentConfig fromMap(Map<String, Object> map) {
        Object enabled = map.getOrDefault("enabled", Boolean.FALSE);
        List<String> command = new ArrayList<>();
        Object rawCommand = map.get("command");
        if (rawCommand instanceof List<?> list) {
            for (Object item : list) command.add(String.valueOf(item));
        }
        int timeout = map.get("timeoutSeconds") instanceof Number number ? number.intValue() : 1800;
        Map<String, String> environment = new LinkedHashMap<>();
        Object rawEnvironment = map.get("environment");
        if (rawEnvironment instanceof Map<?, ?> envMap) {
            envMap.forEach((key, value) -> environment.put(String.valueOf(key), String.valueOf(value)));
        }
        return new AgentConfig(
                enabled instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(enabled)),
                command,
                timeout,
                environment
        );
    }
}
