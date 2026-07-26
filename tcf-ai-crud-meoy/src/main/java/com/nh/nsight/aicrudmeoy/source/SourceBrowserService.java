package com.nh.nsight.aicrudmeoy.source;

import com.nh.nsight.aicrudmeoy.config.CrudMeoyProperties;
import com.nh.nsight.aicrudmeoy.store.CrudSessionEntity;
import com.nh.nsight.aicrudmeoy.store.CrudSessionRepository;
import com.nh.nsight.aicrudmeoy.store.LedgerEntryRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 세션의 업무코드(BC)·도메인코드를 기준으로 nsight-tcf-framework 저장소 안의
 * 관련 소스(서비스 모듈, tcf-ui 화면, 샘플 요청)를 찾아서 읽기 전용으로 제공한다.
 * C15~C18 단계에서 "기준소스/생성소스 확인" 용도로 사용한다.
 */
@Service
public class SourceBrowserService {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "java", "xml", "yml", "yaml", "sql", "json", "html", "js", "css",
            "md", "properties", "http", "txt", "gradle", "bat", "sh", "py");
    private static final long MAX_FILE_BYTES = 512 * 1024;
    private static final int MAX_FILES = 400;

    private final CrudSessionRepository sessions;
    private final LedgerEntryRepository ledger;
    private final Path repoRoot;

    public SourceBrowserService(
            CrudSessionRepository sessions,
            LedgerEntryRepository ledger,
            CrudMeoyProperties properties) {
        this.sessions = sessions;
        this.ledger = ledger;
        this.repoRoot = resolveRepoRoot(properties.getRepoRoot());
    }

    /** 설정값이 없으면 작업 디렉터리에서 settings.gradle 이 있는 상위 폴더를 저장소 루트로 삼는다. */
    private static Path resolveRepoRoot(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }
        Path dir = Paths.get("").toAbsolutePath().normalize();
        for (Path p = dir; p != null; p = p.getParent()) {
            if (Files.exists(p.resolve("settings.gradle"))) {
                return p;
            }
        }
        return dir;
    }

    public Path getRepoRoot() {
        return repoRoot;
    }

    public Map<String, Object> listSources(String sessionId) {
        CrudSessionEntity session = sessions.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다: " + sessionId));

        String bc = firstNonBlank(session.getBusinessCode(), ledgerValue(sessionId, "c01.businessCode"));
        String domain = firstNonBlank(session.getDomainCode(), ledgerValue(sessionId, "c01.domainCode"));
        // 대상 모듈은 항상 C01 업무코드 기준. c00.baseModule은 "복사 기준(참조)"일 뿐이라 대상과 혼동하지 않는다.
        String targetModule = bc == null ? null : bc.toLowerCase(Locale.ROOT) + "-service";
        String baseModule = normalizeModuleName(ledgerValue(sessionId, "c00.baseModule"));
        if (baseModule != null && targetModule != null && baseModule.equalsIgnoreCase(targetModule)) {
            baseModule = null; // 대상과 같으면 참조 카테고리 중복 표시 안 함
        }

        List<Map<String, Object>> files = new ArrayList<>();
        if (targetModule != null) {
            Path moduleSrc = repoRoot.resolve(targetModule).resolve("src");
            collectByDomain(moduleSrc, "대상 모듈 (" + targetModule + ")", domain, files);
        }
        if (bc != null) {
            String bcLower = bc.toLowerCase(Locale.ROOT);
            collectAll(repoRoot.resolve("tcf-ui/src/main/resources/static").resolve(bcLower),
                    "화면 (tcf-ui/static/" + bcLower + ")", files);
            collectSampleRequests(bcLower, domain, files);
        }
        if (baseModule != null) {
            Path baseSrc = repoRoot.resolve(baseModule).resolve("src");
            collectByDomain(baseSrc, "기준 모듈 참조 (c00.baseModule=" + baseModule + ")", domain, files);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("businessCode", bc);
        result.put("domainCode", domain);
        result.put("module", targetModule);
        result.put("baseModule", baseModule);
        result.put("repoRoot", repoRoot.toString());
        result.put("count", files.size());
        result.put("files", files);
        if (bc == null && domain == null) {
            result.put("note", "C01(업무코드/도메인코드)을 먼저 입력하면 관련 소스를 찾을 수 있습니다.");
        } else if (files.isEmpty()) {
            result.put("note", "일치하는 소스가 없습니다. 아직 구현 전이거나(도메인코드=" + domain
                    + "), 대상 모듈(" + targetModule + ")이 존재하지 않습니다."
                    + (baseModule != null ? " 하단 기준 모듈(" + baseModule + ") 참조를 확인하세요." : ""));
        } else if (baseModule != null) {
            long targetDomainCount = files.stream()
                    .filter(f -> String.valueOf(f.get("category")).startsWith("대상 모듈"))
                    .count();
            if (targetDomainCount == 0) {
                result.put("note", "대상 모듈(" + targetModule + ")에는 아직 도메인(" + domain
                        + ") 소스가 없습니다. 화면/샘플은 업무코드(" + bc + ") 기준이며, "
                        + "아래 '기준 모듈 참조'는 C00의 " + baseModule + " 입니다.");
            }
        }
        return result;
    }

    /** "ln-service", "2) ln-service ..." 같은 값에서 모듈명만 추출 */
    private static String normalizeModuleName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        // "... ln-service" 또는 "ln-service" 형태에서 xxx-service 토큰 추출
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)\\b([a-z0-9]+-service)\\b")
                .matcher(t);
        if (m.find()) {
            return m.group(1).toLowerCase(Locale.ROOT);
        }
        if (t.matches("(?i)[a-z0-9]+-service")) {
            return t.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    public Map<String, Object> readSource(String relPath) {
        if (relPath == null || relPath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "path 파라미터가 필요합니다.");
        }
        Path target = repoRoot.resolve(relPath).normalize();
        if (!target.startsWith(repoRoot) || target.toString().contains(".git")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "저장소 밖의 경로는 조회할 수 없습니다: " + relPath);
        }
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다: " + relPath);
        }
        try {
            long size = Files.size(target);
            boolean truncated = size > MAX_FILE_BYTES;
            byte[] bytes = truncated
                    ? readHead(target)
                    : Files.readAllBytes(target);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("path", toRelative(target));
            result.put("size", size);
            result.put("truncated", truncated);
            result.put("content", new String(bytes, StandardCharsets.UTF_8));
            return result;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽지 못했습니다: " + e.getMessage());
        }
    }

    private byte[] readHead(Path target) throws IOException {
        try (var in = Files.newInputStream(target)) {
            return in.readNBytes((int) MAX_FILE_BYTES);
        }
    }

    /** 모듈 src 아래에서 파일명이 도메인코드(CamelCase 또는 kebab-case)를 포함하는 텍스트 파일을 모은다. */
    private void collectByDomain(Path base, String category, String domain, List<Map<String, Object>> out) {
        if (!Files.isDirectory(base)) {
            return;
        }
        String domainLower = domain == null ? null : domain.toLowerCase(Locale.ROOT);
        String domainKebab = domain == null ? null : toKebab(domain);
        try (Stream<Path> walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile)
                    .filter(this::isTextFile)
                    .filter(p -> {
                        if (domainLower == null) {
                            return true;
                        }
                        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.contains(domainLower) || name.contains(domainKebab);
                    })
                    .sorted()
                    .limit(MAX_FILES)
                    .forEach(p -> out.add(fileInfo(p, category)));
        } catch (IOException ignored) {
            // 스캔 실패 시 해당 카테고리만 비워 둔다.
        }
    }

    private void collectAll(Path base, String category, List<Map<String, Object>> out) {
        collectByDomain(base, category, null, out);
    }

    private void collectSampleRequests(String bcLower, String domain, List<Map<String, Object>> out) {
        Path base = repoRoot.resolve("tcf-ui/src/main/resources/sample-requests");
        if (!Files.isDirectory(base)) {
            return;
        }
        String domainKebab = domain == null ? null : toKebab(domain);
        try (Stream<Path> walk = Files.list(base)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (!name.startsWith(bcLower + "-")) {
                            return false;
                        }
                        return domainKebab == null || name.contains(domainKebab);
                    })
                    .sorted()
                    .forEach(p -> out.add(fileInfo(p, "샘플 요청 (sample-requests)")));
        } catch (IOException ignored) {
        }
    }

    private Map<String, Object> fileInfo(Path p, String category) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("path", toRelative(p));
        info.put("name", p.getFileName().toString());
        info.put("category", category);
        try {
            info.put("size", Files.size(p));
        } catch (IOException e) {
            info.put("size", -1L);
        }
        return info;
    }

    private String toRelative(Path p) {
        return repoRoot.relativize(p).toString().replace('\\', '/');
    }

    private boolean isTextFile(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return TEXT_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** CustomerContact -> customer-contact */
    private static String toKebab(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }

    private String ledgerValue(String sessionId, String key) {
        return ledger.findBySessionIdAndEntryKey(sessionId, key)
                .map(e -> blankToNull(e.getValue()))
                .orElse(null);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
