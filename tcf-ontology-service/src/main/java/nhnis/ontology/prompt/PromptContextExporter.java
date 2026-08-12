package nhnis.ontology.prompt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import nhnis.ontology.ontology.OntologyRegistry;

/**
 * 32.범용CRUD프롬프트에 붙일 온톨로지 컨텍스트를 생성한다.
 */
@Service
public class PromptContextExporter {

    private final OntologyRegistry registry;

    public PromptContextExporter(OntologyRegistry registry) {
        this.registry = registry;
    }

    public Map<String, Object> asJson(String programOrServiceId) {
        Map<String, Object> program = resolve(programOrServiceId)
                .orElseThrow(() -> new IllegalArgumentException("mapping not found: " + programOrServiceId));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("source", "tcf-ontology-service");
        out.put("forPrompt", "pdmg-service/docs/32.범용CRUD프롬프트.md");
        out.put("programId", program.get("programId"));
        out.put("title", program.get("title"));
        out.put("classification", Map.of(
                "majorGroup", program.get("majorGroup"),
                "businessCode", program.get("businessCode"),
                "functionCode", program.get("functionCode"),
                "functionName", program.get("functionName"),
                "packageRoot", program.get("packageRoot")));
        out.put("architectureContract", summarizeRuntime());
        out.put("componentBoundaries", registry.rulesBundle().get("rules"));
        out.put("development", program.get("development"));
        out.put("services", program.get("services"));
        out.put("data", program.get("data"));
        out.put("operations", program.get("operations"));
        out.put("namingShape", registry.shapesBundle());
        return out;
    }

    public String asMarkdown(String programOrServiceId) {
        Map<String, Object> ctx = asJson(programOrServiceId);
        StringBuilder md = new StringBuilder();
        md.append("# Ontology Prompt Context — ").append(ctx.get("programId")).append('\n');
        md.append('\n');
        md.append("> 이 블록은 `tcf-ontology-service`가 생성한 지식 허브 컨텍스트다.\n");
        md.append("> `32.범용CRUD프롬프트.md` 요구사항 작성·구현 시 **추정 금지**, 아래 계약을 우선한다.\n");
        md.append('\n');
        md.append("## 프로그램\n\n");
        md.append("- programId: `").append(ctx.get("programId")).append("`\n");
        md.append("- title: ").append(ctx.get("title")).append('\n');
        @SuppressWarnings("unchecked")
        Map<String, Object> classification = (Map<String, Object>) ctx.get("classification");
        md.append("- 분류: ")
                .append(classification.get("majorGroup")).append('/')
                .append(classification.get("businessCode")).append('/')
                .append(classification.get("functionCode"))
                .append(" (").append(classification.get("functionName")).append(")\n");
        md.append("- packageRoot: `").append(classification.get("packageRoot")).append("`\n");
        md.append('\n');

        md.append("## 아키텍처 계약\n\n");
        md.append("```text\n");
        md.append(ctx.get("architectureContract")).append('\n');
        md.append("```\n\n");

        md.append("## 컴포넌트 경계 규칙\n\n");
        if (ctx.get("componentBoundaries") instanceof List<?> rules) {
            for (Object rule : rules) {
                if (rule instanceof Map<?, ?> r) {
                    md.append("- **").append(r.get("id")).append("** (").append(r.get("severity"))
                            .append("): ").append(r.get("statement")).append('\n');
                }
            }
        }
        md.append('\n');

        md.append("## 개발 매핑\n\n");
        if (ctx.get("development") instanceof Map<?, ?> dev) {
            for (String key : List.of("handler", "facade", "controller", "service", "dao")) {
                if (dev.get(key) != null) {
                    md.append("- ").append(key).append(": `").append(dev.get(key)).append("`\n");
                }
            }
        }
        md.append('\n');

        md.append("## 서비스 ID\n\n");
        if (ctx.get("services") instanceof List<?> services) {
            md.append("| serviceId | op | method | sqlIds |\n");
            md.append("|---|---|---|---|\n");
            for (Object item : services) {
                if (item instanceof Map<?, ?> svc) {
                    md.append("| `").append(svc.get("serviceId")).append("` | ")
                            .append(svc.get("op")).append(" | `")
                            .append(svc.get("method")).append("` | ")
                            .append(svc.get("sqlIds")).append(" |\n");
                }
            }
        }
        md.append('\n');

        md.append("## 데이터\n\n");
        if (ctx.get("data") instanceof Map<?, ?> data) {
            md.append("- table: `").append(data.get("table")).append("`\n");
            md.append("- pk: `").append(data.get("pk")).append("`\n");
            md.append("- mapperXml: `").append(data.get("mapperXml")).append("`\n");
            md.append("- deleteMode: ").append(data.get("deleteMode")).append('\n');
        }
        md.append('\n');

        md.append("## 운영\n\n");
        if (ctx.get("operations") instanceof Map<?, ?> ops) {
            md.append("- uiRoute: `").append(ops.get("uiRoute")).append("`\n");
            md.append("- exceptionCodes: ").append(ops.get("exceptionCodes")).append('\n');
            md.append("- envelope success/error: ").append(ops.get("envelope")).append('\n');
            md.append("- samples: ").append(ops.get("samples")).append('\n');
        }
        md.append('\n');

        md.append("## 프롬프트 사용법\n\n");
        md.append("1. 위 컨텍스트를 CRUD 프롬프트 앞에 붙인다.\n");
        md.append("2. 미정 항목만 질문하고, 여기 있는 serviceId/table/FQCN은 재추정하지 않는다.\n");
        md.append("3. 구현 전 impact API로 변경 파일 목록을 확인한다: ")
                .append("`GET /api/ontology/impact?from=")
                .append(ctx.get("programId")).append("`\n");
        return md.toString();
    }

    private Optional<Map<String, Object>> resolve(String id) {
        Optional<Map<String, Object>> byService = registry.findByServiceId(id);
        if (byService.isPresent()) {
            return byService;
        }
        return registry.findProgram(id);
    }

    private String summarizeRuntime() {
        Map<String, Object> runtime = registry.runtimeBundle();
        List<String> lines = new ArrayList<>();
        Object stepsObj = runtime.get("steps");
        if (stepsObj instanceof List<?> steps && !steps.isEmpty()) {
            List<Map<?, ?>> ordered = new ArrayList<>();
            for (Object item : steps) {
                if (item instanceof Map<?, ?> m) {
                    ordered.add(m);
                }
            }
            ordered.sort((a, b) -> Integer.compare(asInt(a.get("seq")), asInt(b.get("seq"))));
            StringBuilder request = new StringBuilder("RequestThread:");
            StringBuilder worker = new StringBuilder("Worker:");
            for (Map<?, ?> step : ordered) {
                String id = String.valueOf(step.get("id"));
                Object threadObj = step.get("thread");
                String thread = threadObj == null ? "" : String.valueOf(threadObj);
                String tx = step.get("tx") == null ? "" : ("/" + step.get("tx"));
                if (thread.startsWith("request")) {
                    request.append(" → ").append(id).append(tx);
                } else {
                    worker.append(" → ").append(id).append(tx);
                }
            }
            lines.add(request.toString());
            lines.add(worker.toString());
            lines.add("source: ontology/technical/tx-runtime.yml steps (dynamic)");
        } else {
            lines.add("UNRESOLVED: tx-runtime.yml steps missing");
        }
        if (runtime.get("outcomes") instanceof Map<?, ?> outcomes) {
            lines.add("outcomes: " + outcomes);
        }
        return String.join("\n", lines);
    }

    private static int asInt(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }
}
