package nhnis.ontology.knowledge;

import java.util.List;
import java.util.Map;

/**
 * Indexed Architecture Knowledge document (exearchidoc).
 */
public final class KnowledgeDocument {

    private final String id;
    private final String fileName;
    private final String relativePath;
    private final String title;
    private final String category;
    private final String content;
    private final List<String> headings;
    private final int lineCount;
    private final long bytes;

    public KnowledgeDocument(
            String id,
            String fileName,
            String relativePath,
            String title,
            String category,
            String content,
            List<String> headings,
            int lineCount,
            long bytes) {
        this.id = id;
        this.fileName = fileName;
        this.relativePath = relativePath;
        this.title = title;
        this.category = category;
        this.content = content;
        this.headings = headings == null ? List.of() : List.copyOf(headings);
        this.lineCount = lineCount;
        this.bytes = bytes;
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getContent() {
        return content;
    }

    public List<String> getHeadings() {
        return headings;
    }

    public int getLineCount() {
        return lineCount;
    }

    public long getBytes() {
        return bytes;
    }

    public Map<String, Object> toSummary() {
        return Map.of(
                "id", id,
                "fileName", fileName,
                "relativePath", relativePath,
                "title", title,
                "category", category,
                "lineCount", lineCount,
                "bytes", bytes,
                "headingCount", headings.size(),
                "headings", headings.size() > 12 ? headings.subList(0, 12) : headings);
    }

    public Map<String, Object> toDetail() {
        java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>(toSummary());
        m.put("content", content);
        m.put("headings", headings);
        return m;
    }
}
