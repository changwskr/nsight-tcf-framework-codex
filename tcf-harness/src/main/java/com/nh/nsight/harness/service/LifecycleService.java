package com.nh.nsight.harness.service;

import com.nh.nsight.harness.agent.AgentCommandAdapter;
import com.nh.nsight.harness.agent.AgentConfig;
import com.nh.nsight.harness.agent.AgentConfigLoader;
import com.nh.nsight.harness.agent.AgentExecutionResult;
import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.prompt.PromptService;
import com.nh.nsight.harness.prompt.StagePromptContract;
import com.nh.nsight.harness.storage.JsonStateRepository;

import java.io.IOException;
import java.nio.file.Path;

public final class LifecycleService {
    private final Path repositoryRoot;
    private final JsonStateRepository repository;
    private final GateService gateService;
    private final ArtifactValidator artifactValidator;
    private final PromptService promptService;
    private final AgentConfigLoader configLoader;
    private final AgentCommandAdapter adapter;

    public LifecycleService(
            Path repositoryRoot,
            JsonStateRepository repository,
            GateService gateService,
            ArtifactValidator artifactValidator,
            PromptService promptService,
            AgentConfigLoader configLoader,
            AgentCommandAdapter adapter
    ) {
        this.repositoryRoot = repositoryRoot;
        this.repository = repository;
        this.gateService = gateService;
        this.artifactValidator = artifactValidator;
        this.promptService = promptService;
        this.configLoader = configLoader;
        this.adapter = adapter;
    }

    public AgentExecutionResult runStage(String workItemId, Stage stage) throws IOException, InterruptedException {
        WorkItemState state = gateService.startStage(workItemId, stage);
        StagePromptContract contract = promptService.createContract(state, stage);
        AgentConfig config = configLoader.load();
        AgentExecutionResult result = adapter.execute(config, contract.request());
        if (!result.executed()) {
            return result;
        }
        if (!result.success()) {
            gateService.markNeedsHumanReview(workItemId, stage, "Agent execution failed: exitCode=" + result.exitCode());
            return result;
        }
        try {
            artifactValidator.validateForReview(repository.load(workItemId), stage);
        } catch (IllegalStateException missingArtifact) {
            gateService.markNeedsHumanReview(workItemId, stage, missingArtifact.getMessage());
            return new AgentExecutionResult(true, false, result.exitCode(), result.timedOut(), result.duration(),
                    result.command(), "Agent completed but required artifact validation failed: " + missingArtifact.getMessage());
        }
        gateService.review(workItemId, stage);
        return result;
    }

    public StagePromptContract createRepairContract(String workItemId) throws IOException {
        return promptService.createContract(repository.load(workItemId), Stage.TEST);
    }
}
