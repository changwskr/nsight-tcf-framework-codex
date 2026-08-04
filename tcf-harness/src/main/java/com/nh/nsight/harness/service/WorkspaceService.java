package com.nh.nsight.harness.service;

import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.storage.JsonStateRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkspaceService {
    private final Path repositoryRoot;
    private final JsonStateRepository stateRepository;

    public WorkspaceService(Path repositoryRoot, JsonStateRepository stateRepository) {
        this.repositoryRoot = repositoryRoot;
        this.stateRepository = stateRepository;
    }

    public WorkItemState initialize(String workItemId, String title, String branch) throws IOException {
        if (stateRepository.exists(workItemId)) {
            throw new IllegalStateException("Work item already exists: " + workItemId);
        }
        WorkItemState state = WorkItemState.create(workItemId, title, repositoryRoot.toAbsolutePath().normalize().toString(), branch);
        Path workItemDirectory = repositoryRoot.resolve("docs/work-items").resolve(workItemId);
        Files.createDirectories(workItemDirectory.resolve("test-evidence/logs"));
        Files.createDirectories(repositoryRoot.resolve(".harness/work").resolve(workItemId));
        stateRepository.save(state);
        writeReadme(state, workItemDirectory);
        return state;
    }

    public Path writeReadme(WorkItemState state, Path directory) throws IOException {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(state.workItemId()).append(" — ").append(state.title()).append("\n\n");
        md.append("- 현재 단계: `").append(state.currentStage()).append("`\n");
        md.append("- 작업 브랜치: `").append(state.branch()).append("`\n\n");
        md.append("## 단계 상태\n\n| 단계 | 상태 | 산출물 |\n|---|---|---|\n");
        for (Stage stage : Stage.values()) {
            md.append("| ").append(stage).append(" | ").append(state.stage(stage).status()).append(" | [")
                    .append(stage.artifactFileName()).append("](./").append(stage.artifactFileName()).append(") |\n");
        }
        Path readme = directory.resolve("README.md");
        Files.writeString(readme, md.toString(), StandardCharsets.UTF_8);
        return readme;
    }
}
