package com.nh.nsight.harness.storage;

import com.nh.nsight.harness.json.SimpleJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AuditLogWriter {
    private final Path auditDirectory;

    public AuditLogWriter(Path repositoryRoot) {
        this.auditDirectory = repositoryRoot.resolve(".harness/audit");
    }

    public void append(String workItemId, String event, String actor, Map<String, Object> details) throws IOException {
        Files.createDirectories(auditDirectory);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("timestamp", OffsetDateTime.now().toString());
        record.put("workItemId", workItemId);
        record.put("event", event);
        record.put("actor", actor == null ? "" : actor);
        record.put("details", details == null ? Map.of() : details);
        Files.writeString(
                auditDirectory.resolve(workItemId + ".jsonl"),
                SimpleJson.stringify(record).replace("\r", "").replace("\n", "") + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }
}
