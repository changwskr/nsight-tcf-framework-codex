package com.nh.nsight.harness.testexec;

import com.nh.nsight.harness.domain.TestCommand;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.git.GitService;
import com.nh.nsight.harness.json.SimpleJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TestEvidenceWriter {
    private final Path repositoryRoot;
    private final GitService gitService;

    public TestEvidenceWriter(Path repositoryRoot, GitService gitService) {
        this.repositoryRoot = repositoryRoot;
        this.gitService = gitService;
    }

    public Path evidenceDirectory(String workItemId) throws IOException {
        Path directory = repositoryRoot.resolve("docs/work-items").resolve(workItemId).resolve("test-evidence");
        Files.createDirectories(directory.resolve("logs"));
        Files.createDirectories(directory.resolve("retry-history"));
        return directory;
    }

    public void writeEnvironment(WorkItemState state) throws IOException {
        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("capturedAt", OffsetDateTime.now().toString());
        environment.put("osName", System.getProperty("os.name"));
        environment.put("osVersion", System.getProperty("os.version"));
        environment.put("osArch", System.getProperty("os.arch"));
        environment.put("javaVersion", System.getProperty("java.version"));
        environment.put("javaVendor", System.getProperty("java.vendor"));
        environment.put("repositoryRoot", repositoryRoot.toAbsolutePath().normalize().toString());
        environment.put("branch", state.branch());
        try {
            environment.put("commitSha", gitService.headSha());
        } catch (Exception e) {
            environment.put("commitSha", "UNAVAILABLE: " + e.getMessage());
        }
        Files.writeString(evidenceDirectory(state.workItemId()).resolve("environment.json"),
                SimpleJson.stringify(environment) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    public void writeCommands(WorkItemState state) throws IOException {
        List<Object> commands = new ArrayList<>();
        for (TestCommand command : state.testCommands()) commands.add(command.toMap());
        Files.writeString(evidenceDirectory(state.workItemId()).resolve("commands.json"),
                SimpleJson.stringify(commands) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    public void writeSummary(WorkItemState state, TestRunSummary summary) throws IOException {
        Path directory = evidenceDirectory(state.workItemId());
        StringBuilder md = new StringBuilder();
        md.append("# ").append(state.workItemId()).append(" 테스트 증적 요약\n\n");
        md.append("## 1. 도입 전 안내말\n\n");
        md.append("본 문서는 사용자가 승인한 테스트 명령의 실행 결과와 자동 수정 반복 이력을 보존한다.\n\n");
        md.append("## 2. 실행 결과\n\n");
        md.append("| 항목 | 값 |\n|---|---|\n");
        md.append("| 최종 판정 | `").append(summary.verdict()).append("` |\n");
        md.append("| 전체 성공 | ").append(summary.success()).append(" |\n");
        md.append("| 수행 회차 | ").append(summary.attempts()).append(" |\n");
        md.append("| 작업 브랜치 | `").append(state.branch()).append("` |\n\n");
        md.append("## 3. 명령별 결과\n\n");
        md.append("| 회차 | ID | 명령 | 결과 | 종료코드 | Timeout | 소요(ms) |\n|---:|---|---|---|---:|---|---:|\n");
        for (TestAttemptResult result : summary.results()) {
            md.append("| ").append(result.attempt()).append(" | ").append(result.commandId()).append(" | `")
                    .append(result.command().replace("|", "\\|"))
                    .append("` | ").append(result.success() ? "PASS" : "FAIL")
                    .append(" | ").append(result.exitCode())
                    .append(" | ").append(result.timedOut())
                    .append(" | ").append(result.duration().toMillis()).append(" |\n");
        }
        md.append("\n## 4. 자동 수정 통제\n\n");
        md.append("- 최대 반복 횟수: 3회\n");
        md.append("- 테스트 삭제·비활성화·검증 축소 금지\n");
        md.append("- 승인 설계 변경 시 사람 검토로 전환\n\n");
        md.append("## 5. 최종 승인\n\n테스트 결과는 사용자 수동 승인 전까지 완료로 간주하지 않는다.\n");
        Files.writeString(directory.resolve("test-summary.md"), md.toString(), StandardCharsets.UTF_8);
        try {
            gitService.writeDiff(directory.resolve("git-diff.patch"));
            Files.writeString(directory.resolve("changed-files.json"),
                    SimpleJson.stringify(gitService.changedFiles()) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Files.writeString(directory.resolve("git-diff.patch"), "Git diff unavailable: " + e.getMessage(), StandardCharsets.UTF_8);
        }
    }
}
