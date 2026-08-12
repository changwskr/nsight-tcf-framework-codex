package nhnis.ontology.knowledge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import nhnis.ontology.config.OntologyProperties;

/**
 * Loads and searches Architecture Knowledge corpus under docs/knowledge/exearchidoc.
 */
@Service
public class ArchitectureKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureKnowledgeService.class);
    private static final Pattern HEADING = Pattern.compile("^#{1,3}\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern CATEGORY = Pattern.compile("^(\\d{2})\\.");

    private final OntologyProperties properties;
    private final ConcurrentHashMap<String, KnowledgeDocument> byId = new ConcurrentHashMap<>();
    private volatile String loadedFrom = "UNRESOLVED";

    public ArchitectureKnowledgeService(OntologyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    public synchronized Map<String, Object> reload() {
        byId.clear();
        int fromFs = loadFromFileSystem();
        int fromCp = 0;
        if (fromFs == 0) {
            fromCp = loadFromClasspath();
        }
        loadedFrom = fromFs > 0 ? "filesystem:" + resolveFsRoot() : (fromCp > 0 ? "classpath" : "EMPTY");
        log.info("Architecture knowledge loaded: docs={}, from={}", byId.size(), loadedFrom);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("count", byId.size());
        out.put("loadedFrom", loadedFrom);
        out.put("knowledgePath", properties.getKnowledgePath());
        return out;
    }

    public Map<String, Object> catalog(String category, String keyword) {
        String cat = category == null ? "" : category.trim();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> items = byId.values().stream()
                .sorted(Comparator.comparing(KnowledgeDocument::getFileName, String.CASE_INSENSITIVE_ORDER))
                .filter(d -> cat.isBlank() || cat.equalsIgnoreCase(d.getCategory()) || "ALL".equalsIgnoreCase(cat))
                .filter(d -> {
                    if (kw.isBlank()) {
                        return true;
                    }
                    String hay = (d.getFileName() + " " + d.getTitle() + " " + String.join(" ", d.getHeadings()))
                            .toLowerCase(Locale.ROOT);
                    return hay.contains(kw);
                })
                .map(KnowledgeDocument::toSummary)
                .collect(Collectors.toList());

        Map<String, Long> byCategory = byId.values().stream()
                .collect(Collectors.groupingBy(KnowledgeDocument::getCategory, LinkedHashMap::new, Collectors.counting()));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("source", "docs/knowledge/exearchidoc");
        out.put("loadedFrom", loadedFrom);
        out.put("total", byId.size());
        out.put("count", items.size());
        out.put("byCategory", byCategory);
        out.put("documents", items);
        out.put("note", "Architecture Knowledge corpus for QnA / browsing. Not auto-merged into Ontology Graph.");
        return out;
    }

    public Optional<KnowledgeDocument> find(String idOrFileName) {
        if (idOrFileName == null || idOrFileName.isBlank()) {
            return Optional.empty();
        }
        String key = idOrFileName.trim();
        KnowledgeDocument direct = byId.get(key);
        if (direct != null) {
            return Optional.of(direct);
        }
        return byId.values().stream()
                .filter(d -> d.getFileName().equalsIgnoreCase(key)
                        || d.getId().equalsIgnoreCase(key)
                        || d.getRelativePath().equalsIgnoreCase(key))
                .findFirst();
    }

    public Map<String, Object> getDocument(String idOrFileName) {
        KnowledgeDocument doc = find(idOrFileName)
                .orElseThrow(() -> new IllegalArgumentException("Knowledge document not found: " + idOrFileName));
        return doc.toDetail();
    }

    public List<ScoredDocument> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return List.of();
        }
        int top = Math.max(1, Math.min(limit <= 0 ? 5 : limit, 20));
        List<ScoredDocument> scored = new ArrayList<>();
        for (KnowledgeDocument doc : byId.values()) {
            double score = score(doc, tokens, query);
            if (score > 0) {
                scored.add(new ScoredDocument(doc, score, extractSnippets(doc, tokens, 3)));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredDocument::score).reversed()
                .thenComparing(s -> s.document().getFileName()));
        if (scored.size() > top) {
            return scored.subList(0, top);
        }
        return scored;
    }

    public List<KnowledgeDocument> all() {
        return byId.values().stream()
                .sorted(Comparator.comparing(KnowledgeDocument::getFileName))
                .toList();
    }

    static List<String> tokenize(String text) {
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^0-9a-zA-Z가-힣._\\-]+", " ")
                .trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String t : normalized.split("\\s+")) {
            if (t.length() >= 2) {
                out.add(t);
            }
        }
        // also keep meaningful short tokens
        for (String t : List.of("tx", "db", "ui", "fw", "pk", "dto", "dao", "sql", "tcf")) {
            if (normalized.contains(t) && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private double score(KnowledgeDocument doc, List<String> tokens, String rawQuery) {
        String title = doc.getTitle().toLowerCase(Locale.ROOT);
        String file = doc.getFileName().toLowerCase(Locale.ROOT);
        String body = doc.getContent().toLowerCase(Locale.ROOT);
        double s = 0;
        String q = rawQuery.toLowerCase(Locale.ROOT);
        if (title.contains(q) || file.contains(q)) {
            s += 20;
        }
        for (String token : tokens) {
            if (file.contains(token)) {
                s += 8;
            }
            if (title.contains(token)) {
                s += 6;
            }
            for (String h : doc.getHeadings()) {
                if (h.toLowerCase(Locale.ROOT).contains(token)) {
                    s += 3;
                }
            }
            s += Math.min(12, countOccurrences(body, token) * 1.2);
        }
        // Prefer canonical docs over "-1" duplicates when scores are close.
        if (file.endsWith("-1.md")) {
            s *= 0.92;
        }
        return s;
    }

    private static int countOccurrences(String hay, String needle) {
        if (needle.isBlank() || hay.isBlank()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = hay.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
            if (count > 40) {
                break;
            }
        }
        return count;
    }

    private static List<String> extractSnippets(KnowledgeDocument doc, List<String> tokens, int max) {
        String[] paras = doc.getContent().split("\\n\\s*\\n");
        List<ScoredSnippet> hits = new ArrayList<>();
        for (String para : paras) {
            String p = para.trim();
            if (p.length() < 40) {
                continue;
            }
            String lower = p.toLowerCase(Locale.ROOT);
            int hit = 0;
            for (String t : tokens) {
                if (lower.contains(t)) {
                    hit++;
                }
            }
            if (hit > 0) {
                String clipped = p.length() > 420 ? p.substring(0, 420) + "…" : p;
                hits.add(new ScoredSnippet(hit, clipped));
            }
        }
        hits.sort(Comparator.comparingInt(ScoredSnippet::score).reversed());
        return hits.stream().limit(max).map(ScoredSnippet::text).toList();
    }

    private int loadFromFileSystem() {
        Path root = resolveFsRoot();
        if (root == null || !Files.isDirectory(root)) {
            return 0;
        }
        int n = 0;
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> files = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .sorted()
                    .toList();
            for (Path file : files) {
                putDocument(root.relativize(file).toString().replace('\\', '/'),
                        Files.readString(file, StandardCharsets.UTF_8),
                        Files.size(file));
                n++;
            }
        } catch (IOException e) {
            log.warn("Failed loading knowledge from filesystem {}: {}", root, e.getMessage());
            return 0;
        }
        loadedFrom = "filesystem:" + root.toAbsolutePath().normalize();
        return n;
    }

    private int loadFromClasspath() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:docs/knowledge/exearchidoc/**/*.md");
            int n = 0;
            for (Resource resource : resources) {
                if (!resource.exists() || !resource.isReadable()) {
                    continue;
                }
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                putDocument(filename, content, content.getBytes(StandardCharsets.UTF_8).length);
                n++;
            }
            return n;
        } catch (IOException e) {
            log.warn("Failed loading knowledge from classpath: {}", e.getMessage());
            return 0;
        }
    }

    private Path resolveFsRoot() {
        String configured = properties.getKnowledgePath();
        Path p = Path.of(configured == null ? "docs/knowledge/exearchidoc" : configured);
        if (Files.isDirectory(p)) {
            return p.toAbsolutePath().normalize();
        }
        Path alt = Path.of("tcf-ontology-service").resolve(p);
        if (Files.isDirectory(alt)) {
            return alt.toAbsolutePath().normalize();
        }
        return p.toAbsolutePath().normalize();
    }

    private void putDocument(String relativePath, String content, long bytes) {
        String fileName = Path.of(relativePath).getFileName().toString();
        String id = fileName.replace(' ', '_');
        String title = extractTitle(content, fileName);
        String category = extractCategory(fileName);
        List<String> headings = extractHeadings(content);
        int lines = content.isEmpty() ? 0 : content.split("\\R", -1).length;
        byId.put(id, new KnowledgeDocument(
                id,
                fileName,
                "docs/knowledge/exearchidoc/" + relativePath.replace('\\', '/'),
                title,
                category,
                content,
                headings,
                lines,
                bytes));
    }

    private static String extractTitle(String content, String fileName) {
        Matcher m = HEADING.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }
        String base = fileName;
        if (base.toLowerCase(Locale.ROOT).endsWith(".md")) {
            base = base.substring(0, base.length() - 3);
        }
        return base;
    }

    private static String extractCategory(String fileName) {
        Matcher m = CATEGORY.matcher(fileName);
        if (m.find()) {
            return m.group(1);
        }
        return "MISC";
    }

    private static List<String> extractHeadings(String content) {
        List<String> out = new ArrayList<>();
        Matcher m = HEADING.matcher(content);
        while (m.find()) {
            out.add(m.group(1).trim());
            if (out.size() >= 40) {
                break;
            }
        }
        return out;
    }

    public record ScoredDocument(KnowledgeDocument document, double score, List<String> snippets) {
    }

    private record ScoredSnippet(int score, String text) {
    }
}
