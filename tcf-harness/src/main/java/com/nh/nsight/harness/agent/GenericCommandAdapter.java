package com.nh.nsight.harness.agent;

import com.nh.nsight.harness.json.SimpleJson;
import com.nh.nsight.harness.util.ProcessResult;
import com.nh.nsight.harness.util.ProcessRunner;
import com.nh.nsight.harness.util.SensitiveDataGuard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GenericCommandAdapter implements AgentCommandAdapter {
    private final ProcessRunner runner;

    public GenericCommandAdapter(ProcessRunner runner) {
        this.runner = runner;
    }

    @Override
    public AgentExecutionResult execute(AgentConfig config, AgentExecutionRequest request) throws IOException, InterruptedException {
        if (!config.enabled()) {
            return AgentExecutionResult.skipped("Agent execution is disabled. Prompt contract was generated only.");
        }
        if (config.command().isEmpty()) {
            throw new IllegalStateException("Agent command is empty");
        }
        Files.createDirectories(request.stdoutFile().getParent());
        Map<String, String> values = new LinkedHashMap<>();
        values.put("workItemId", request.workItemId());
        values.put("stage", request.stage());
        values.put("workingDirectory", request.workingDirectory().toString());
        values.put("promptFile", request.promptFile().toString());
        values.put("contextFile", request.contextFile().toString());
        values.put("resultFile", request.resultFile().toString());
        values.put("executionFile", request.executionFile().toString());
        if (request.placeholders() != null) values.putAll(request.placeholders());

        List<String> command = new ArrayList<>();
        for (String token : config.command()) {
            command.add(replacePlaceholders(token, values));
        }
        SensitiveDataGuard.assertSafeCommand(String.join(" ", command));
        String startedAt = OffsetDateTime.now().toString();
        ProcessResult result = runner.run(command, request.workingDirectory(), config.timeoutSeconds(), config.environment());
        String endedAt = OffsetDateTime.now().toString();
        Files.writeString(request.stdoutFile(), SensitiveDataGuard.redact(result.stdout()), StandardCharsets.UTF_8);
        Files.writeString(request.stderrFile(), SensitiveDataGuard.redact(result.stderr()), StandardCharsets.UTF_8);

        Map<String, Object> execution = new LinkedHashMap<>();
        execution.put("workItemId", request.workItemId());
        execution.put("stage", request.stage());
        execution.put("startedAt", startedAt);
        execution.put("endedAt", endedAt);
        execution.put("command", SensitiveDataGuard.redact(command));
        execution.put("workingDirectory", request.workingDirectory().toString());
        execution.put("exitCode", result.exitCode());
        execution.put("timedOut", result.timedOut());
        execution.put("durationMillis", result.duration().toMillis());
        execution.put("success", result.success());
        Files.writeString(request.executionFile(), SimpleJson.stringify(execution) + System.lineSeparator(), StandardCharsets.UTF_8);

        return new AgentExecutionResult(true, result.success(), result.exitCode(), result.timedOut(),
                result.duration(), SensitiveDataGuard.redact(command), result.success() ? "Agent completed" : "Agent failed");
    }

    private static String replacePlaceholders(String text, Map<String, String> values) {
        String replaced = text;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            replaced = replaced.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return replaced;
    }
}
