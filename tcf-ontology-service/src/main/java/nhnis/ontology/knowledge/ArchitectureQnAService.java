package nhnis.ontology.knowledge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import nhnis.ontology.facade.OntologyFacade;
import nhnis.ontology.query.OntologyQueryService;
import nhnis.ontology.support.ServiceIdParser;

/**
 * Architecture QnA grounded on exearchidoc corpus (+ optional Ontology hints).
 * Extractive retrieval answer — no external LLM required.
 */
@Service
public class ArchitectureQnAService {

    private static final Pattern SERVICE_ID = Pattern.compile(
            "\\b([a-z]{2}[a-z]{2}[a-z]\\d{4}[SCUDAR][0-9A-Z])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRAM_ID = Pattern.compile(
            "\\b([a-z]{2}[a-z]{2}[a-z]\\d{4})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLE = Pattern.compile("\\b(TB_[A-Z0-9_]+)\\b");

    private final ArchitectureKnowledgeService knowledgeService;
    private final OntologyFacade ontologyFacade;
    private final OntologyQueryService queryService;

    public ArchitectureQnAService(
            ArchitectureKnowledgeService knowledgeService,
            OntologyFacade ontologyFacade,
            OntologyQueryService queryService) {
        this.knowledgeService = knowledgeService;
        this.ontologyFacade = ontologyFacade;
        this.queryService = queryService;
    }

    public Map<String, Object> ask(String question, Integer topK) {
        String q = question == null ? "" : question.trim();
        if (q.isBlank()) {
            throw new IllegalArgumentException("question is required");
        }
        int k = topK == null || topK <= 0 ? 5 : Math.min(topK, 10);
        List<ArchitectureKnowledgeService.ScoredDocument> hits = knowledgeService.search(q, k);

        List<Map<String, Object>> references = new ArrayList<>();
        StringBuilder answer = new StringBuilder();
        answer.append("질문: ").append(q).append("\n\n");

        if (hits.isEmpty()) {
            answer.append("관련 Architecture Knowledge 문서를 찾지 못했습니다.\n");
            answer.append("99. Architecture Knowledge에서 문서를 직접 조회하거나, 키워드를 바꿔 다시 질문해 주세요.\n");
        } else {
            answer.append("exearchidoc 근거 기반 요약 (추출형 QnA):\n\n");
            int i = 1;
            for (ArchitectureKnowledgeService.ScoredDocument hit : hits) {
                KnowledgeDocument doc = hit.document();
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("rank", i);
                ref.put("score", Math.round(hit.score() * 10.0) / 10.0);
                ref.put("id", doc.getId());
                ref.put("fileName", doc.getFileName());
                ref.put("title", doc.getTitle());
                ref.put("relativePath", doc.getRelativePath());
                ref.put("snippets", hit.snippets());
                references.add(ref);

                answer.append(i).append(") ").append(doc.getTitle())
                        .append(" (`").append(doc.getFileName()).append("`)\n");
                if (hit.snippets().isEmpty()) {
                    answer.append("   - (본문 매칭 스니펫 없음, 제목/파일명 매칭)\n");
                } else {
                    for (String snip : hit.snippets()) {
                        String oneLine = snip.replace('\n', ' ').replaceAll("\\s+", " ").trim();
                        if (oneLine.length() > 280) {
                            oneLine = oneLine.substring(0, 280) + "…";
                        }
                        answer.append("   - ").append(oneLine).append('\n');
                    }
                }
                answer.append('\n');
                i++;
            }
            answer.append("주의: 본 답변은 LLM 생성이 아니라 문서 검색·발췌입니다. 원문은 99. Architecture Knowledge에서 확인하세요.\n");
        }

        Map<String, Object> ontologyHints = ontologyHints(q);
        if (!ontologyHints.isEmpty()) {
            answer.append('\n').append("Ontology 연계 힌트:\n");
            ontologyHints.forEach((k2, v) -> answer.append("- ").append(k2).append(": ").append(v).append('\n'));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("question", q);
        out.put("mode", "EXTRACTIVE_RETRIEVAL");
        out.put("corpus", "docs/knowledge/exearchidoc");
        out.put("answer", answer.toString());
        out.put("references", references);
        out.put("referenceCount", references.size());
        out.put("ontologyHints", ontologyHints);
        out.put("status", hits.isEmpty() ? "NO_HIT" : "OK");
        return out;
    }

    private Map<String, Object> ontologyHints(String question) {
        Map<String, Object> hints = new LinkedHashMap<>();
        Matcher sid = SERVICE_ID.matcher(question);
        if (sid.find()) {
            String serviceId = sid.group(1);
            try {
                if (ServiceIdParser.isValid(serviceId)) {
                    hints.put("serviceId", ServiceIdParser.canonical(serviceId));
                    ontologyFacade.service(serviceId).ifPresent(s -> {
                        hints.put("programId", s.get("programId"));
                        hints.put("serviceLookup", "FOUND");
                    });
                    try {
                        Map<String, Object> structure = queryService.serviceStructure(serviceId);
                        hints.put("structureSummary", structure.get("summary"));
                    } catch (RuntimeException ignored) {
                        // optional
                    }
                }
            } catch (RuntimeException ignored) {
                hints.put("serviceId", serviceId);
            }
        }
        Matcher pid = PROGRAM_ID.matcher(question);
        if (pid.find() && !hints.containsKey("programId")) {
            String programId = pid.group(1).toLowerCase(Locale.ROOT);
            ontologyFacade.program(programId).ifPresent(p -> {
                hints.put("programId", programId);
                hints.put("programTitle", p.get("title"));
            });
        }
        Matcher table = TABLE.matcher(question);
        if (table.find()) {
            String t = table.group(1);
            hints.put("table", t);
            try {
                hints.put("tableImpact", queryService.impactByTable(t).get("affectedServiceIds"));
            } catch (RuntimeException ignored) {
                // optional
            }
        }
        return hints;
    }
}
