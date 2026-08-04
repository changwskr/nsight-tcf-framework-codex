package com.nh.nsight.harness.agent;

import com.nh.nsight.harness.json.SimpleJson;
import com.nh.nsight.harness.util.SensitiveDataGuard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class AgentConfigLoader {
    private final Path configPath;

    public AgentConfigLoader(Path repositoryRoot) {
        this.configPath = repositoryRoot.resolve(".harness/config/harness-config.json");
    }

    @SuppressWarnings("unchecked")
    public AgentConfig load() throws IOException {
        if (!Files.exists(configPath)) {
            return AgentConfig.disabled();
        }
        Object parsed = SimpleJson.parse(Files.readString(configPath, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IllegalStateException("Harness config must be a JSON object: " + configPath);
        }
        Object rawAgent = root.get("agent");
        if (!(rawAgent instanceof Map<?, ?> agentMap)) {
            return AgentConfig.disabled();
        }
        AgentConfig config = AgentConfig.fromMap((Map<String, Object>) agentMap);
        config.environment().forEach((key, value) -> {
            if (SensitiveDataGuard.isSensitiveKey(key) && value != null && !value.isBlank()) {
                throw new IllegalStateException("Do not store credential values in harness-config.json: " + key);
            }
        });
        return config;
    }
}
