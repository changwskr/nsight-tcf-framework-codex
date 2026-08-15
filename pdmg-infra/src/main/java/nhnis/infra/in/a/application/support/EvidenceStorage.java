package nhnis.infra.in.a.application.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Gate Evidence 실파일 저장소 — data/evidence/{evidenceId}/{safeFileName}
 */
@Component
public class EvidenceStorage {
    private static final Set<String> ALLOWED_EXT = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "txt", "csv", "xlsx", "xls", "doc", "docx", "md");
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private final Path root;

    public EvidenceStorage() throws IOException {
        this.root = Paths.get(System.getProperty("user.dir"), "data", "evidence").toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Path root() {
        return root;
    }

    public Stored store(String evidenceId, String originalFileName, byte[] content) throws IOException {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("EMPTY_FILE");
        }
        if (content.length > MAX_BYTES) {
            throw new IllegalArgumentException("FILE_TOO_LARGE_MAX_2MB");
        }
        String safeId = sanitizeId(evidenceId);
        String safeName = sanitizeFileName(originalFileName);
        String ext = extension(safeName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("UNSUPPORTED_EXT:" + ext);
        }
        Path dir = root.resolve(safeId).normalize();
        if (!dir.startsWith(root)) {
            throw new IllegalArgumentException("INVALID_PATH");
        }
        Files.createDirectories(dir);
        Path file = dir.resolve(safeName).normalize();
        if (!file.startsWith(dir)) {
            throw new IllegalArgumentException("INVALID_PATH");
        }
        Files.write(file, content);
        String uri = "/evidence/" + safeId + "/" + safeName;
        return new Stored(safeName, uri, file.toAbsolutePath().toString(), content.length);
    }

    public boolean existsUri(String fileUri) {
        if (fileUri == null || !fileUri.startsWith("/evidence/")) {
            return false;
        }
        String rel = fileUri.substring("/evidence/".length());
        Path p = root.resolve(rel).normalize();
        return p.startsWith(root) && Files.isRegularFile(p);
    }

    private static String sanitizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("INVALID_EVIDENCE_ID");
        }
        String s = id.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        if (s.isEmpty()) {
            throw new IllegalArgumentException("INVALID_EVIDENCE_ID");
        }
        return s;
    }

    private static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("INVALID_FILE_NAME");
        }
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^A-Za-z0-9._\\-가-힣]", "_");
        if (base.isBlank() || ".".equals(base) || "..".equals(base)) {
            throw new IllegalArgumentException("INVALID_FILE_NAME");
        }
        if (base.length() > 120) {
            String ext = extension(base);
            base = base.substring(0, Math.min(100, base.length())) + (ext.isEmpty() ? "" : "." + ext);
        }
        return base;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record Stored(String fileName, String fileUri, String absolutePath, long sizeBytes) {}
}
