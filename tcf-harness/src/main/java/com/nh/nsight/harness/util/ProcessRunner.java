package com.nh.nsight.harness.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class ProcessRunner {
    public ProcessResult run(List<String> command, Path workingDirectory, int timeoutSeconds) throws IOException, InterruptedException {
        return run(command, workingDirectory, timeoutSeconds, Map.of());
    }

    public ProcessResult run(
            List<String> command,
            Path workingDirectory,
            int timeoutSeconds,
            Map<String, String> environment
    ) throws IOException, InterruptedException {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Command must not be empty");
        }
        if (workingDirectory == null || !Files.isDirectory(workingDirectory)) {
            throw new IllegalArgumentException("Working directory does not exist: " + workingDirectory);
        }
        Instant started = Instant.now();
        ProcessBuilder builder = new ProcessBuilder(new ArrayList<>(command));
        builder.directory(workingDirectory.toFile());
        builder.environment().putAll(environment);
        Process process = builder.start();

        StreamCollector stdout = new StreamCollector(process.getInputStream(), StandardCharsets.UTF_8);
        StreamCollector stderr = new StreamCollector(process.getErrorStream(), StandardCharsets.UTF_8);
        Thread stdoutThread = Thread.startVirtualThread(stdout);
        Thread stderrThread = Thread.startVirtualThread(stderr);

        boolean completed = process.waitFor(Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
        if (!completed) {
            process.destroy();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        }
        stdoutThread.join();
        stderrThread.join();
        int exitCode = completed ? process.exitValue() : 124;
        return new ProcessResult(
                List.copyOf(command),
                exitCode,
                !completed,
                Duration.between(started, Instant.now()),
                stdout.content(),
                stderr.content()
        );
    }

    public ProcessResult runShell(String command, Path workingDirectory, int timeoutSeconds) throws IOException, InterruptedException {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> shell = os.contains("win")
                ? List.of("cmd.exe", "/d", "/s", "/c", command)
                : List.of("sh", "-lc", command);
        return run(shell, workingDirectory, timeoutSeconds);
    }
}
