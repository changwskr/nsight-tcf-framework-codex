package nhnis.ontology.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nhnis.ontology.knowledge.ArchitectureKnowledgeService;
import nhnis.ontology.knowledge.ArchitectureQnAService;

/**
 * Architecture Knowledge browser + QnA APIs (exearchidoc corpus).
 */
@RestController
@RequestMapping("/api/ontology")
public class ArchitectureKnowledgeController {

    private final ArchitectureKnowledgeService knowledgeService;
    private final ArchitectureQnAService qnaService;

    public ArchitectureKnowledgeController(
            ArchitectureKnowledgeService knowledgeService,
            ArchitectureQnAService qnaService) {
        this.knowledgeService = knowledgeService;
        this.qnaService = qnaService;
    }

    @GetMapping("/knowledge")
    public Map<String, Object> catalog(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {
        return knowledgeService.catalog(category, q);
    }

    @GetMapping("/knowledge/doc/{id:.+}")
    public ResponseEntity<?> document(@PathVariable String id) {
        try {
            return ResponseEntity.ok(knowledgeService.getDocument(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/knowledge/search")
    public Map<String, Object> search(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "8") int limit) {
        var hits = knowledgeService.search(q, limit);
        return Map.of(
                "query", q,
                "count", hits.size(),
                "hits", hits.stream().map(h -> Map.of(
                        "score", h.score(),
                        "document", h.document().toSummary(),
                        "snippets", h.snippets())).toList());
    }

    @PostMapping("/knowledge/reload")
    public Map<String, Object> reload() {
        return knowledgeService.reload();
    }

    @PostMapping("/qna/ask")
    public ResponseEntity<?> ask(@RequestBody(required = false) Map<String, Object> body) {
        try {
            Map<String, Object> req = body == null ? Map.of() : body;
            String question = req.get("question") == null ? String.valueOf(req.getOrDefault("q", ""))
                    : String.valueOf(req.get("question"));
            Integer topK = null;
            if (req.get("topK") instanceof Number n) {
                topK = n.intValue();
            }
            return ResponseEntity.ok(qnaService.ask(question, topK));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/qna/ask")
    public ResponseEntity<?> askGet(
            @RequestParam String q,
            @RequestParam(required = false) Integer topK) {
        try {
            return ResponseEntity.ok(qnaService.ask(q, topK));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
