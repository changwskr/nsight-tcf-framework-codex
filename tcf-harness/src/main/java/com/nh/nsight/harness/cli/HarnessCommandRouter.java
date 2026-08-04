package com.nh.nsight.harness.cli;

import com.nh.nsight.harness.agent.AgentConfig;
import com.nh.nsight.harness.agent.AgentConfigLoader;
import com.nh.nsight.harness.agent.AgentExecutionResult;
import com.nh.nsight.harness.agent.GenericCommandAdapter;
import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.StageStatus;
import com.nh.nsight.harness.domain.TestCommand;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.git.BranchNameFactory;
import com.nh.nsight.harness.json.SimpleJson;
import com.nh.nsight.harness.prompt.StagePromptContract;
import com.nh.nsight.harness.service.RequirementQuestion;
import com.nh.nsight.harness.storage.JsonStateRepository;
import com.nh.nsight.harness.testexec.RepairAction;
import com.nh.nsight.harness.testexec.TestRunSummary;
import com.nh.nsight.harness.util.SensitiveDataGuard;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class HarnessCommandRouter {
    private final PrintStream out;
    private final PrintStream err;

    public HarnessCommandRouter(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public int run(String[] rawArgs) {
        try {
            Arguments args = Arguments.parse(rawArgs);
            String command = args.positional(0, "help");
            Path repo = Path.of(args.option("repo", ".")).toAbsolutePath().normalize();
            HarnessRuntime runtime = HarnessRuntime.create(repo);
            return switch (command) {
                case "help", "--help", "-h" -> help();
                case "init" -> init(runtime, args);
                case "requirement" -> requirement(runtime, args);
                case "approve" -> approve(runtime, args);
                case "review" -> review(runtime, args);
                case "analyze" -> agentStage(runtime, args, Stage.ANALYSIS);
                case "design" -> agentStage(runtime, args, Stage.DESIGN);
                case "implement" -> agentStage(runtime, args, Stage.IMPLEMENTATION);
                case "test" -> test(runtime, args);
                case "status" -> status(runtime, args);
                case "close" -> agentStage(runtime, args, Stage.CLOSE);
                default -> throw new IllegalArgumentException("Unknown command: " + command);
            };
        } catch (Exception e) {
            err.println("ERROR: " + e.getMessage());
            return 2;
        }
    }

    private int help() {
        out.println("""
                NSIGHT Development Harness

                Commands:
                  init --id <ID> --title <TITLE> [--repo <PATH>] [--skip-branch]
                  requirement next --id <ID> [--repo <PATH>]
                  requirement answer --id <ID> --question <REQ-QNN> --text <ANSWER> [--repo <PATH>]
                  review --id <ID> --stage <STAGE> [--repo <PATH>]
                  approve --id <ID> --stage <STAGE> --decision <APPROVED|REVISION_REQUIRED|REJECTED> [--comment <TEXT>]
                  analyze|design|implement|close --id <ID> [--repo <PATH>]
                  test detect --id <ID> [--repo <PATH>]
                  test approve-command --id <ID> --command-id <ID> --command <TEXT> [--timeout <SECONDS>]
                  test run --id <ID> [--repo <PATH>]
                  status --id <ID> [--repo <PATH>]
                """);
        return 0;
    }

    private int init(HarnessRuntime runtime, Arguments args) throws Exception {
        String id = args.require("id");
        String title = args.require("title");
        if (runtime.stateRepository().exists(id)) {
            throw new IllegalStateException("Work item already exists: " + id);
        }
        String branch = args.option("branch", BranchNameFactory.create(id, title));
        if (!args.flag("skip-branch")) {
            if (!runtime.gitService().isRepository()) {
                throw new IllegalStateException("Target directory is not a Git repository: " + runtime.repositoryRoot());
            }
            runtime.gitService().createAndSwitchBranch(branch);
        }
        runtime.scaffoldService().initializeConfigIfMissing();
        WorkItemState state = runtime.workspaceService().initialize(id, title, branch);
        runtime.auditLogWriter().append(id, "WORK_ITEM_INITIALIZED", "user", Map.of("title", title, "branch", branch));
        out.println("Initialized " + state.workItemId());
        out.println("Branch: " + state.branch());
        out.println("Next: requirement next --id " + id);
        return 0;
    }

    private int requirement(HarnessRuntime runtime, Arguments args) throws Exception {
        String action = args.positional(1, "next");
        String id = args.require("id");
        if ("next".equals(action)) {
            RequirementQuestion question = runtime.requirementService().nextQuestion(id).orElse(null);
            if (question == null) {
                runtime.gateService().review(id, Stage.REQUIREMENT);
                out.println("All 12 answers are complete. REQUIREMENT moved to REVIEW.");
            } else {
                out.println(question.id() + " | " + question.title());
                out.println(question.question());
            }
            return 0;
        }
        if ("answer".equals(action)) {
            String questionId = args.require("question");
            String text = args.require("text");
            runtime.requirementService().answer(id, questionId, text);
            runtime.auditLogWriter().append(id, "REQUIREMENT_ANSWERED", "user", Map.of("questionId", questionId));
            out.println("Saved answer for " + questionId);
            runtime.requirementService().nextQuestion(id).ifPresentOrElse(
                    next -> out.println("Next: " + next.id() + " | " + next.question()),
                    () -> out.println("All answers complete. Run: requirement next --id " + id));
            return 0;
        }
        throw new IllegalArgumentException("Unknown requirement action: " + action);
    }


    private int review(HarnessRuntime runtime, Arguments args) throws Exception {
        String id = args.require("id");
        Stage stage = Stage.valueOf(args.require("stage").toUpperCase());
        WorkItemState beforeReview = runtime.stateRepository().load(id);
        runtime.artifactValidator().validateForReview(beforeReview, stage);
        WorkItemState state = runtime.gateService().review(id, stage);
        refreshReadme(runtime, state);
        runtime.auditLogWriter().append(id, "STAGE_SUBMITTED_FOR_REVIEW", "user", Map.of("stage", stage.name()));
        out.println(stage + " -> REVIEW");
        return 0;
    }

    private int approve(HarnessRuntime runtime, Arguments args) throws Exception {
        String id = args.require("id");
        Stage stage = Stage.valueOf(args.require("stage").toUpperCase());
        StageStatus decision = StageStatus.valueOf(args.require("decision").toUpperCase());
        String comment = args.option("comment", "");
        WorkItemState state = runtime.gateService().decide(id, stage, decision, comment);
        refreshReadme(runtime, state);
        runtime.auditLogWriter().append(id, "GATE_DECISION", "user",
                Map.of("stage", stage.name(), "decision", decision.name(), "comment", comment));
        out.println(stage + " -> " + state.stage(stage).status());
        return 0;
    }

    private int agentStage(HarnessRuntime runtime, Arguments args, Stage stage) throws Exception {
        String id = args.require("id");
        AgentExecutionResult result = runtime.lifecycleService().runStage(id, stage);
        refreshReadme(runtime, runtime.stateRepository().load(id));
        runtime.auditLogWriter().append(id, "AGENT_STAGE_EXECUTED", "system",
                Map.of("stage", stage.name(), "executed", result.executed(), "success", result.success(), "message", result.message()));
        out.println(result.message());
        if (!result.executed()) {
            out.println("Prompt contract generated under .harness/work/" + id + "/" + stage.name().toLowerCase());
            return 0;
        }
        return result.success() ? 0 : 1;
    }

    private int test(HarnessRuntime runtime, Arguments args) throws Exception {
        String action = args.positional(1, "detect");
        String id = args.require("id");
        JsonStateRepository repository = runtime.stateRepository();
        if ("detect".equals(action)) {
            List<TestCommand> candidates = runtime.testCommandDetector().detect(runtime.repositoryRoot());
            for (TestCommand candidate : candidates) {
                out.println(candidate.id() + " | " + candidate.command() + " | timeout=" + candidate.timeoutSeconds());
            }
            if (candidates.isEmpty()) out.println("No test command candidate detected.");
            return 0;
        }
        if ("approve-command".equals(action)) {
            WorkItemState state = repository.load(id);
            String approvedCommand = args.require("command");
            SensitiveDataGuard.assertSafeCommand(approvedCommand);
            TestCommand command = new TestCommand(
                    args.require("command-id"),
                    approvedCommand,
                    true,
                    Integer.parseInt(args.option("timeout", "900")));
            state.addTestCommand(command);
            repository.save(state);
            runtime.auditLogWriter().append(id, "TEST_COMMAND_APPROVED", "user", command.toMap());
            out.println("Approved test command: " + command.id());
            return 0;
        }
        if ("run".equals(action)) {
            runtime.gateService().startStage(id, Stage.TEST);
            AgentConfig config = new AgentConfigLoader(runtime.repositoryRoot()).load();
            GenericCommandAdapter adapter = new GenericCommandAdapter(runtime.processRunner());
            RepairAction repair = (attempt, partial) -> {
                if (!config.enabled()) return false;
                StagePromptContract contract = runtime.lifecycleService().createRepairContract(id);
                Path failedSummary = contract.directory().resolve("failed-test-summary.json");
                Files.writeString(failedSummary,
                        SimpleJson.stringify(Map.of(
                                "attempt", attempt,
                                "verdict", partial.verdict(),
                                "results", partial.results().stream().map(result -> Map.of(
                                        "commandId", result.commandId(),
                                        "command", result.command(),
                                        "exitCode", result.exitCode(),
                                        "stdoutFile", result.stdoutFile().toString(),
                                        "stderrFile", result.stderrFile().toString()
                                )).toList()
                        )) + System.lineSeparator(), StandardCharsets.UTF_8);
                Files.writeString(
                        contract.request().promptFile(),
                        System.lineSeparator() + "## 현재 재시도 컨텍스트" + System.lineSeparator()
                                + "- 실패 회차: " + attempt + System.lineSeparator()
                                + "- 실패 요약: `" + failedSummary + "`" + System.lineSeparator()
                                + "- 위 파일과 연결된 stdout/stderr 로그를 먼저 읽고 최소 수정만 수행한다."
                                + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.APPEND
                );
                return adapter.execute(config, contract.request()).success();
            };
            TestRunSummary summary = runtime.testExecutionService().execute(id, 3, repair);
            WorkItemState updated;
            if (summary.success()) {
                updated = runtime.gateService().review(id, Stage.TEST);
            } else {
                updated = runtime.gateService().markNeedsHumanReview(id, Stage.TEST, summary.verdict());
            }
            refreshReadme(runtime, updated);
            out.println("Test verdict: " + summary.verdict());
            return summary.success() ? 0 : 1;
        }
        throw new IllegalArgumentException("Unknown test action: " + action);
    }

    private int status(HarnessRuntime runtime, Arguments args) throws Exception {
        String id = args.require("id");
        WorkItemState state = runtime.stateRepository().load(id);
        out.println(state.workItemId() + " | " + state.title());
        out.println("Repository: " + state.repositoryRoot());
        out.println("Branch: " + state.branch());
        out.println("Current stage: " + state.currentStage());
        for (Stage stage : Stage.values()) {
            out.printf("%-15s %s%n", stage, state.stage(stage).status());
        }
        return 0;
    }
    private void refreshReadme(HarnessRuntime runtime, WorkItemState state) throws Exception {
        Path directory = runtime.repositoryRoot().resolve("docs/work-items").resolve(state.workItemId());
        runtime.workspaceService().writeReadme(state, directory);
    }

}
