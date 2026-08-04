package com.nh.nsight.harness.storage;

import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.json.SimpleJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public final class JsonStateRepository {
    private final Path harnessRoot;
    private final Path stateDirectory;

    public JsonStateRepository(Path repositoryRoot) {
        this.harnessRoot = repositoryRoot.resolve(".harness");
        this.stateDirectory = harnessRoot.resolve("state");
    }

    public Path harnessRoot() {
        return harnessRoot;
    }

    public boolean exists(String workItemId) {
        return Files.exists(pathFor(workItemId));
    }

    public void save(WorkItemState state) throws IOException {
        Files.createDirectories(stateDirectory);
        Path target = pathFor(state.workItemId());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, SimpleJson.stringify(state.toMap()) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailure) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SuppressWarnings("unchecked")
    public WorkItemState load(String workItemId) throws IOException {
        Path path = pathFor(workItemId);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Unknown work item: " + workItemId);
        }
        Object parsed = SimpleJson.parse(Files.readString(path, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalStateException("State root must be a JSON object: " + path);
        }
        return WorkItemState.fromMap((Map<String, Object>) map);
    }

    public Path pathFor(String workItemId) {
        validateWorkItemId(workItemId);
        return stateDirectory.resolve(workItemId + ".json");
    }

    private static void validateWorkItemId(String workItemId) {
        if (workItemId == null || !workItemId.matches("[A-Z][A-Z0-9-]{2,63}")) {
            throw new IllegalArgumentException("Invalid workItemId: " + workItemId);
        }
    }
}
