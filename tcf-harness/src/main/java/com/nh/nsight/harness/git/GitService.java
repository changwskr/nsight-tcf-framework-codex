package com.nh.nsight.harness.git;

import com.nh.nsight.harness.util.ProcessResult;
import com.nh.nsight.harness.util.ProcessRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GitService {
    private final Path repositoryRoot;
    private final ProcessRunner runner;

    public GitService(Path repositoryRoot, ProcessRunner runner) {
        this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
        this.runner = runner;
    }

    public boolean isRepository() throws IOException, InterruptedException {
        return run("rev-parse", "--is-inside-work-tree").success();
    }

    public boolean isClean() throws IOException, InterruptedException {
        ProcessResult result = requireSuccess("status", "--porcelain=v1", "--untracked-files=all");
        return result.stdout().isBlank();
    }

    public String currentBranch() throws IOException, InterruptedException {
        return requireSuccess("branch", "--show-current").stdout().trim();
    }

    public String headSha() throws IOException, InterruptedException {
        return requireSuccess("rev-parse", "HEAD").stdout().trim();
    }

    public boolean branchExists(String branch) throws IOException, InterruptedException {
        validateBranchName(branch);
        return run("show-ref", "--verify", "--quiet", "refs/heads/" + branch).success();
    }

    public void createAndSwitchBranch(String branch) throws IOException, InterruptedException {
        validateBranchName(branch);
        if (!isClean()) {
            throw new IllegalStateException("Git working tree is not clean: " + repositoryRoot);
        }
        if (branchExists(branch)) {
            throw new IllegalStateException("Git branch already exists: " + branch);
        }
        requireSuccess("switch", "-c", branch);
    }

    public List<String> changedFiles() throws IOException, InterruptedException {
        String text = requireSuccess("status", "--porcelain=v1", "--untracked-files=all").stdout();
        List<String> files = new ArrayList<>();
        for (String line : text.lines().toList()) {
            if (line.length() >= 4) {
                files.add(line.substring(3).trim());
            }
        }
        return files;
    }

    public Path writeDiff(Path target) throws IOException, InterruptedException {
        ProcessResult tracked = requireSuccess("diff", "--binary", "--no-ext-diff");
        ProcessResult staged = requireSuccess("diff", "--binary", "--cached", "--no-ext-diff");
        Files.createDirectories(target.getParent());
        Files.writeString(target, tracked.stdout() + staged.stdout(), StandardCharsets.UTF_8);
        return target;
    }

    private ProcessResult run(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(Arrays.asList(args));
        return runner.run(command, repositoryRoot, 60);
    }

    private ProcessResult requireSuccess(String... args) throws IOException, InterruptedException {
        ProcessResult result = run(args);
        if (!result.success()) {
            throw new IllegalStateException("Git command failed: " + result.command() + "\n" + result.stderr());
        }
        return result;
    }

    private void validateBranchName(String branch) throws IOException, InterruptedException {
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("Branch must not be blank");
        }
        ProcessResult result = runner.run(List.of("git", "check-ref-format", "--branch", branch), repositoryRoot, 10);
        if (!result.success()) {
            throw new IllegalArgumentException("Invalid Git branch name: " + branch);
        }
    }
}
