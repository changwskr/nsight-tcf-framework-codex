package com.nh.nsight.harness.prompt;

import com.nh.nsight.harness.agent.AgentExecutionRequest;
import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.json.SimpleJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PromptService {
    private final Path repositoryRoot;

    public PromptService(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    public StagePromptContract createContract(WorkItemState state, Stage stage) throws IOException {
        Path directory = repositoryRoot.resolve(".harness/work")
                .resolve(state.workItemId())
                .resolve(stage.name().toLowerCase());
        Files.createDirectories(directory);
        Path promptFile = directory.resolve("prompt.md");
        Path contextFile = directory.resolve("context.json");
        Path resultFile = directory.resolve("result.md");
        Path executionFile = directory.resolve("execution.json");
        Path stdoutFile = directory.resolve("stdout.log");
        Path stderrFile = directory.resolve("stderr.log");

        String master = readRequired(repositoryRoot.resolve("harness/prompts/MASTER-HARNESS.md"));
        String stagePrompt = readRequired(repositoryRoot.resolve("harness/prompts/" + promptFileName(stage)));
        String rendered = (master + "\n\n---\n\n" + stagePrompt)
                .replace("${workItemId}", state.workItemId())
                .replace("${title}", state.title())
                .replace("${repositoryRoot}", state.repositoryRoot())
                .replace("${branch}", state.branch())
                .replace("${stage}", stage.name())
                .replace("${workItemDirectory}", repositoryRoot.resolve("docs/work-items").resolve(state.workItemId()).toString());
        Files.writeString(promptFile, rendered, StandardCharsets.UTF_8);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("workItem", state.toMap());
        context.put("stage", stage.name());
        context.put("repositoryRoot", repositoryRoot.toAbsolutePath().normalize().toString());
        context.put("workItemDirectory", repositoryRoot.resolve("docs/work-items").resolve(state.workItemId()).toString());
        context.put("promptFile", promptFile.toString());
        context.put("resultFile", resultFile.toString());
        Files.writeString(contextFile, SimpleJson.stringify(context) + System.lineSeparator(), StandardCharsets.UTF_8);

        if (!Files.exists(resultFile)) {
            Files.writeString(resultFile, "# Agent Result\n\n상태: NOT_EXECUTED\n", StandardCharsets.UTF_8);
        }
        AgentExecutionRequest request = new AgentExecutionRequest(
                state.workItemId(), stage.name(), repositoryRoot, promptFile, contextFile, resultFile,
                executionFile, stdoutFile, stderrFile, Map.of());
        return new StagePromptContract(directory, request);
    }

    private String readRequired(Path path) throws IOException {
        if (Files.exists(path)) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        String resourceName = "/harness/prompts/" + path.getFileName();
        try (InputStream input = PromptService.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Required prompt file not found in repository or classpath: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String promptFileName(Stage stage) {
        return switch (stage) {
            case REQUIREMENT -> "01-REQUIREMENT.md";
            case ANALYSIS -> "02-ANALYSIS.md";
            case DESIGN -> "03-DESIGN.md";
            case IMPLEMENTATION -> "04-IMPLEMENTATION.md";
            case TEST -> "05-TEST.md";
            case CLOSE -> "06-CLOSE.md";
        };
    }
}
