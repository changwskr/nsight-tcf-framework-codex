package com.nh.nsight.harness.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WorkItemState {
    private final String workItemId;
    private final String title;
    private final String repositoryRoot;
    private final String branch;
    private Stage currentStage;
    private final String createdAt;
    private String updatedAt;
    private final EnumMap<Stage, StageState> stages;
    private final List<RequirementAnswer> requirementAnswers;
    private final List<TestCommand> testCommands;
    private final Map<String, String> metadata;

    private WorkItemState(
            String workItemId,
            String title,
            String repositoryRoot,
            String branch,
            Stage currentStage,
            String createdAt,
            String updatedAt,
            EnumMap<Stage, StageState> stages,
            List<RequirementAnswer> requirementAnswers,
            List<TestCommand> testCommands,
            Map<String, String> metadata
    ) {
        this.workItemId = Objects.requireNonNull(workItemId);
        this.title = Objects.requireNonNull(title);
        this.repositoryRoot = Objects.requireNonNull(repositoryRoot);
        this.branch = Objects.requireNonNull(branch);
        this.currentStage = Objects.requireNonNull(currentStage);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.stages = stages;
        this.requirementAnswers = requirementAnswers;
        this.testCommands = testCommands;
        this.metadata = metadata;
    }

    public static WorkItemState create(String workItemId, String title, String repositoryRoot, String branch) {
        String now = OffsetDateTime.now().toString();
        EnumMap<Stage, StageState> states = new EnumMap<>(Stage.class);
        for (Stage stage : Stage.values()) {
            states.put(stage, StageState.initial("docs/work-items/" + workItemId + "/" + stage.artifactFileName()));
        }
        states.put(Stage.REQUIREMENT, states.get(Stage.REQUIREMENT).transition(StageStatus.IN_PROGRESS, "system", "work item initialized"));
        return new WorkItemState(workItemId, title, repositoryRoot, branch, Stage.REQUIREMENT, now, now,
                states, new ArrayList<>(), new ArrayList<>(), new LinkedHashMap<>());
    }

    public String workItemId() { return workItemId; }
    public String title() { return title; }
    public String repositoryRoot() { return repositoryRoot; }
    public String branch() { return branch; }
    public Stage currentStage() { return currentStage; }
    public String createdAt() { return createdAt; }
    public String updatedAt() { return updatedAt; }
    public StageState stage(Stage stage) { return stages.get(stage); }
    public Map<Stage, StageState> stages() { return Map.copyOf(stages); }
    public List<RequirementAnswer> requirementAnswers() { return List.copyOf(requirementAnswers); }
    public List<TestCommand> testCommands() { return List.copyOf(testCommands); }
    public Map<String, String> metadata() { return Map.copyOf(metadata); }

    public void updateStage(Stage stage, StageState state) {
        stages.put(stage, state);
        currentStage = stage;
        touch();
    }

    public void addOrReplaceRequirementAnswer(RequirementAnswer answer) {
        requirementAnswers.removeIf(existing -> existing.questionId().equals(answer.questionId()));
        requirementAnswers.add(answer);
        touch();
    }

    public void addTestCommand(TestCommand command) {
        testCommands.removeIf(existing -> existing.id().equals(command.id()));
        testCommands.add(command);
        touch();
    }

    public void putMetadata(String key, String value) {
        metadata.put(key, value);
        touch();
    }

    private void touch() {
        updatedAt = OffsetDateTime.now().toString();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("workItemId", workItemId);
        map.put("title", title);
        map.put("repositoryRoot", repositoryRoot);
        map.put("branch", branch);
        map.put("currentStage", currentStage.name());
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);

        Map<String, Object> stageMap = new LinkedHashMap<>();
        for (Stage stage : Stage.values()) {
            stageMap.put(stage.name(), stages.get(stage).toMap());
        }
        map.put("stages", stageMap);

        map.put("requirementAnswers", requirementAnswers.stream().map(RequirementAnswer::toMap).toList());
        map.put("testCommands", testCommands.stream().map(TestCommand::toMap).toList());
        map.put("metadata", new LinkedHashMap<>(metadata));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static WorkItemState fromMap(Map<String, Object> map) {
        EnumMap<Stage, StageState> stages = new EnumMap<>(Stage.class);
        Map<String, Object> stageMap = (Map<String, Object>) map.getOrDefault("stages", Map.of());
        for (Stage stage : Stage.values()) {
            Object raw = stageMap.get(stage.name());
            if (raw instanceof Map<?, ?> rawMap) {
                stages.put(stage, StageState.fromMap((Map<String, Object>) rawMap));
            } else {
                stages.put(stage, StageState.initial(""));
            }
        }

        List<RequirementAnswer> answers = new ArrayList<>();
        Object rawAnswers = map.get("requirementAnswers");
        if (rawAnswers instanceof List<?> list) {
            for (Object raw : list) {
                if (raw instanceof Map<?, ?> rawMap) {
                    answers.add(RequirementAnswer.fromMap((Map<String, Object>) rawMap));
                }
            }
        }

        List<TestCommand> commands = new ArrayList<>();
        Object rawCommands = map.get("testCommands");
        if (rawCommands instanceof List<?> list) {
            for (Object raw : list) {
                if (raw instanceof Map<?, ?> rawMap) {
                    commands.add(TestCommand.fromMap((Map<String, Object>) rawMap));
                }
            }
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        Object rawMetadata = map.get("metadata");
        if (rawMetadata instanceof Map<?, ?> rawMap) {
            rawMap.forEach((key, value) -> metadata.put(String.valueOf(key), String.valueOf(value)));
        }

        return new WorkItemState(
                String.valueOf(map.get("workItemId")),
                String.valueOf(map.get("title")),
                String.valueOf(map.get("repositoryRoot")),
                String.valueOf(map.get("branch")),
                Stage.valueOf(String.valueOf(map.getOrDefault("currentStage", Stage.REQUIREMENT.name()))),
                String.valueOf(map.getOrDefault("createdAt", "")),
                String.valueOf(map.getOrDefault("updatedAt", "")),
                stages,
                answers,
                commands,
                metadata
        );
    }
}
