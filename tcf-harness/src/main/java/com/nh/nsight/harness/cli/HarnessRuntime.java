package com.nh.nsight.harness.cli;

import com.nh.nsight.harness.agent.AgentConfigLoader;
import com.nh.nsight.harness.agent.GenericCommandAdapter;
import com.nh.nsight.harness.git.GitService;
import com.nh.nsight.harness.prompt.PromptService;
import com.nh.nsight.harness.service.ArtifactValidator;
import com.nh.nsight.harness.service.GateService;
import com.nh.nsight.harness.service.LifecycleService;
import com.nh.nsight.harness.service.RepositoryScaffoldService;
import com.nh.nsight.harness.service.RequirementService;
import com.nh.nsight.harness.service.WorkspaceService;
import com.nh.nsight.harness.storage.AuditLogWriter;
import com.nh.nsight.harness.storage.JsonStateRepository;
import com.nh.nsight.harness.testexec.TestCommandDetector;
import com.nh.nsight.harness.testexec.TestEvidenceWriter;
import com.nh.nsight.harness.testexec.TestExecutionService;
import com.nh.nsight.harness.util.ProcessRunner;

import java.nio.file.Path;

public record HarnessRuntime(
        Path repositoryRoot,
        JsonStateRepository stateRepository,
        AuditLogWriter auditLogWriter,
        ProcessRunner processRunner,
        GitService gitService,
        WorkspaceService workspaceService,
        RequirementService requirementService,
        GateService gateService,
        ArtifactValidator artifactValidator,
        RepositoryScaffoldService scaffoldService,
        LifecycleService lifecycleService,
        TestCommandDetector testCommandDetector,
        TestExecutionService testExecutionService
) {
    public static HarnessRuntime create(Path repositoryRoot) {
        Path normalized = repositoryRoot.toAbsolutePath().normalize();
        JsonStateRepository stateRepository = new JsonStateRepository(normalized);
        AuditLogWriter audit = new AuditLogWriter(normalized);
        ProcessRunner runner = new ProcessRunner();
        GitService git = new GitService(normalized, runner);
        GateService gates = new GateService(stateRepository);
        ArtifactValidator artifacts = new ArtifactValidator(normalized);
        PromptService prompts = new PromptService(normalized);
        GenericCommandAdapter adapter = new GenericCommandAdapter(runner);
        LifecycleService lifecycle = new LifecycleService(
                normalized, stateRepository, gates, artifacts, prompts, new AgentConfigLoader(normalized), adapter);
        TestEvidenceWriter evidence = new TestEvidenceWriter(normalized, git);
        return new HarnessRuntime(
                normalized,
                stateRepository,
                audit,
                runner,
                git,
                new WorkspaceService(normalized, stateRepository),
                new RequirementService(stateRepository, normalized),
                gates,
                artifacts,
                new RepositoryScaffoldService(normalized),
                lifecycle,
                new TestCommandDetector(),
                new TestExecutionService(normalized, stateRepository, runner, evidence)
        );
    }
}
