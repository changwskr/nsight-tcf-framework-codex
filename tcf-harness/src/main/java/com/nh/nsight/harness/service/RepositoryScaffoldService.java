package com.nh.nsight.harness.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RepositoryScaffoldService {
    private final Path repositoryRoot;

    public RepositoryScaffoldService(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    public void initializeConfigIfMissing() throws IOException {
        Path config = repositoryRoot.resolve(".harness/config/harness-config.json");
        Files.createDirectories(config.getParent());
        if (!Files.exists(config)) {
            Files.writeString(config, """
                    {
                      "agent": {
                        "enabled": false,
                        "command": [
                          "davis-coder",
                          "run",
                          "--prompt-file",
                          "${promptFile}",
                          "--output-file",
                          "${resultFile}"
                        ],
                        "timeoutSeconds": 1800,
                        "environment": {}
                      },
                      "test": {
                        "maxAttempts": 3
                      }
                    }
                    """, StandardCharsets.UTF_8);
        }
        Path gitignore = repositoryRoot.resolve(".gitignore");
        String marker = "# development-harness-runtime";
        String entries = marker + System.lineSeparator()
                + ".harness/work/*/*/stdout.log" + System.lineSeparator()
                + ".harness/work/*/*/stderr.log" + System.lineSeparator();
        if (!Files.exists(gitignore)) {
            Files.writeString(gitignore, entries, StandardCharsets.UTF_8);
        } else {
            String current = Files.readString(gitignore, StandardCharsets.UTF_8);
            if (!current.contains(marker)) {
                Files.writeString(gitignore, current + (current.endsWith(System.lineSeparator()) ? "" : System.lineSeparator()) + entries,
                        StandardCharsets.UTF_8);
            }
        }
    }
}
