package com.nh.nsight.aicrudmeoy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.aicrudmeoy.catalog.PromptCatalogService;
import com.nh.nsight.aicrudmeoy.catalog.QuestionDefinition;
import com.nh.nsight.aicrudmeoy.catalog.StepDefinition;
import com.nh.nsight.aicrudmeoy.store.CrudSessionEntity;
import com.nh.nsight.aicrudmeoy.store.CrudSessionRepository;
import com.nh.nsight.aicrudmeoy.store.CrudStepSessionEntity;
import com.nh.nsight.aicrudmeoy.store.CrudStepSessionRepository;
import com.nh.nsight.aicrudmeoy.store.LedgerEntryEntity;
import com.nh.nsight.aicrudmeoy.store.LedgerEntryRepository;
import com.nh.nsight.aicrudmeoy.store.StepAnswerEntity;
import com.nh.nsight.aicrudmeoy.store.StepAnswerRepository;
import com.nh.nsight.aicrudmeoy.store.StepResultEntity;
import com.nh.nsight.aicrudmeoy.store.StepResultRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CrudSessionService {

    private static final Set<String> GATE_UNLOCK = Set.of("PASS", "CONDITIONAL");

    private final CrudSessionRepository sessions;
    private final StepAnswerRepository answers;
    private final LedgerEntryRepository ledger;
    private final StepResultRepository results;
    private final CrudStepSessionRepository stepSessions;
    private final PromptCatalogService catalog;
    private final ObjectMapper objectMapper;

    public CrudSessionService(
            CrudSessionRepository sessions,
            StepAnswerRepository answers,
            LedgerEntryRepository ledger,
            StepResultRepository results,
            CrudStepSessionRepository stepSessions,
            PromptCatalogService catalog,
            ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.answers = answers;
        this.ledger = ledger;
        this.results = results;
        this.stepSessions = stepSessions;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CrudSessionEntity> list() {
        return sessions.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public CrudSessionEntity get(String id) {
        return sessions.findById(id).orElseThrow(() -> notFound("세션을 찾을 수 없습니다."));
    }

    @Transactional
    public CrudSessionEntity create(String name) {
        Instant now = Instant.now();
        CrudSessionEntity entity = new CrudSessionEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName((name == null || name.isBlank()) ? "CRUD 세션" : name.trim());
        entity.setCurrentStepId("C-MASTER");
        entity.setGateStatus("NONE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return sessions.save(entity);
    }

    @Transactional
    public CrudSessionEntity rename(String id, String name) {
        CrudSessionEntity entity = get(id);
        if (name != null && !name.isBlank()) {
            entity.setName(name.trim());
        }
        entity.setUpdatedAt(Instant.now());
        return sessions.save(entity);
    }

    /**
     * 선택 세션(샘플 포함)을 템플릿으로 복제한다. sampleFlag는 false로 둔다.
     */
    @Transactional
    public CrudSessionEntity cloneAsTemplate(String sourceId, String newName) {
        CrudSessionEntity source = get(sourceId);
        Instant now = Instant.now();
        CrudSessionEntity copy = new CrudSessionEntity();
        copy.setId(UUID.randomUUID().toString().replace("-", ""));
        String baseName = (newName == null || newName.isBlank())
                ? stripSamplePrefix(source.getName()) + " (템플릿 복제)"
                : newName.trim();
        copy.setName(baseName);
        copy.setCurrentStepId(source.getCurrentStepId());
        copy.setGateStatus(source.getGateStatus());
        copy.setGateNote(source.getGateNote());
        copy.setBusinessCode(source.getBusinessCode());
        copy.setDomainCode(source.getDomainCode());
        copy.setSampleFlag(false);
        copy.setCreatedAt(now);
        copy.setUpdatedAt(now);
        sessions.save(copy);

        String newId = copy.getId();
        for (StepAnswerEntity a : answers.findBySessionIdOrderByStepIdAscQuestionIdAsc(sourceId)) {
            StepAnswerEntity na = new StepAnswerEntity();
            na.setSessionId(newId);
            na.setStepId(a.getStepId());
            na.setQuestionId(a.getQuestionId());
            na.setAnswerJson(a.getAnswerJson());
            na.setNote(a.getNote());
            na.setUpdatedAt(now);
            answers.save(na);
        }
        for (LedgerEntryEntity e : ledger.findBySessionIdOrderByEntryKeyAsc(sourceId)) {
            LedgerEntryEntity ne = new LedgerEntryEntity();
            ne.setSessionId(newId);
            ne.setEntryKey(e.getEntryKey());
            ne.setValue(e.getValue());
            ne.setSourceStepId(e.getSourceStepId());
            ne.setUpdatedAt(now);
            ledger.save(ne);
        }
        for (StepResultEntity r : results.findBySessionId(sourceId)) {
            StepResultEntity nr = new StepResultEntity();
            nr.setSessionId(newId);
            nr.setStepId(r.getStepId());
            nr.setStatus(r.getStatus());
            nr.setSummaryMd(r.getSummaryMd());
            nr.setConfirmedAt(r.getConfirmedAt() == null ? null : now);
            results.save(nr);
        }
        for (CrudStepSessionEntity s : stepSessions.findBySessionIdOrderByStepOrderAscStepIdAsc(sourceId)) {
            CrudStepSessionEntity ns = new CrudStepSessionEntity();
            ns.setSessionId(newId);
            ns.setSessionName(copy.getName());
            ns.setStepId(s.getStepId());
            ns.setStepTitle(s.getStepTitle());
            ns.setStepOrder(s.getStepOrder());
            ns.setStatus(s.getStatus());
            ns.setBusinessCode(copy.getBusinessCode());
            ns.setDomainCode(copy.getDomainCode());
            ns.setAnswersJson(s.getAnswersJson());
            ns.setSummaryMd(s.getSummaryMd());
            ns.setCreatedAt(now);
            ns.setUpdatedAt(now);
            ns.setConfirmedAt(s.getConfirmedAt() == null ? null : now);
            stepSessions.save(ns);
        }
        return copy;
    }

    private static String stripSamplePrefix(String name) {
        if (name == null) {
            return "CRUD 세션";
        }
        return name.replaceFirst("^\\[샘플\\]\\s*", "").trim();
    }

    @Transactional
    public void delete(String id) {
        if (!sessions.existsById(id)) {
            throw notFound("삭제할 세션이 없습니다.");
        }
        answers.findBySessionIdOrderByStepIdAscQuestionIdAsc(id).forEach(answers::delete);
        ledger.findBySessionIdOrderByEntryKeyAsc(id).forEach(ledger::delete);
        results.findBySessionId(id).forEach(results::delete);
        stepSessions.deleteBySessionId(id);
        sessions.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(String id) {
        CrudSessionEntity session = get(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("session", session);
        body.put("answers", answers.findBySessionIdOrderByStepIdAscQuestionIdAsc(id));
        body.put("ledger", ledger.findBySessionIdOrderByEntryKeyAsc(id));
        body.put("results", results.findBySessionId(id));
        body.put("stepSessions", stepSessions.findBySessionIdOrderByStepOrderAscStepIdAsc(id));
        body.put("unlocked", isUnlocked(session));
        return body;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchStepSessions(
            String q, String sessionId, String stepId, String status, String businessCode) {
        List<CrudStepSessionEntity> rows = stepSessions.search(
                blankToNull(q),
                blankToNull(sessionId),
                blankToNull(stepId),
                blankToNull(status),
                blankToNull(businessCode));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", rows.size());
        body.put("rows", rows);
        return body;
    }

    @Transactional(readOnly = true)
    public CrudStepSessionEntity getStepSession(Long id) {
        return stepSessions.findById(id)
                .orElseThrow(() -> notFound("단계 세션을 찾을 수 없습니다."));
    }

    @Transactional
    public Map<String, Object> saveAnswer(String sessionId, Map<String, Object> payload) {
        CrudSessionEntity session = get(sessionId);
        String stepId = stringVal(payload.get("stepId"));
        String questionId = stringVal(payload.get("questionId"));
        Object answer = payload.get("answer");
        String note = stringVal(payload.get("note"));
        if (stepId.isBlank() || questionId.isBlank()) {
            throw badRequest("stepId와 questionId는 필수입니다.");
        }
        StepDefinition step = catalog.requireStep(stepId);
        assertStepAccessible(session, step);

        Instant now = Instant.now();
        StepAnswerEntity entity = answers.findBySessionIdAndStepIdAndQuestionId(sessionId, stepId, questionId)
                .orElseGet(StepAnswerEntity::new);
        entity.setSessionId(sessionId);
        entity.setStepId(stepId);
        entity.setQuestionId(questionId);
        entity.setAnswerJson(toJson(answer));
        entity.setNote(note.isBlank() ? null : note);
        entity.setUpdatedAt(now);
        answers.save(entity);

        QuestionDefinition question = step.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElse(null);
        if (question != null && question.getLedgerKey() != null && !question.getLedgerKey().isBlank()) {
            upsertLedger(sessionId, question.getLedgerKey(), displayAnswer(answer, question), stepId, now);
            syncBusinessDomainFromLedger(session);
        }

        // C04: 테이블 정보를 알고 있으면 전략 질문은 건너뛰고 tableMode=직접입력으로 원장 고정
        if ("C04".equals(stepId) && "table_info".equals(questionId) && "1".equals(stringVal(answer))) {
            StepAnswerEntity modeAns = answers.findBySessionIdAndStepIdAndQuestionId(sessionId, stepId, "table_mode")
                    .orElseGet(StepAnswerEntity::new);
            modeAns.setSessionId(sessionId);
            modeAns.setStepId(stepId);
            modeAns.setQuestionId("table_mode");
            modeAns.setAnswerJson(toJson("0"));
            modeAns.setNote("table_info=1 → 직접입력(전략 생략)");
            modeAns.setUpdatedAt(now);
            answers.save(modeAns);
            upsertLedger(sessionId, "c04.tableMode", "0 — 직접입력(정보확정)", stepId, now);
        }

        if (step.isGate() && "gate_verdict".equals(questionId)) {
            String verdict = stringVal(answer).toUpperCase();
            session.setGateStatus(verdict.isBlank() ? "NONE" : verdict);
        }

        session.setCurrentStepId(stepId);
        session.setUpdatedAt(now);
        sessions.save(session);
        upsertStepSession(session, step, "IN_PROGRESS", null, now);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("answer", entity);
        body.put("session", session);
        body.put("unlocked", isUnlocked(session));
        return body;
    }

    @Transactional
    public Map<String, Object> completeStep(String sessionId, String stepId) {
        CrudSessionEntity session = get(sessionId);
        StepDefinition step = catalog.requireStep(stepId);
        assertStepAccessible(session, step);

        List<StepAnswerEntity> stepAnswers = answers.findBySessionIdAndStepId(sessionId, stepId);
        List<QuestionDefinition> required = visibleQuestions(sessionId, step);
        long answeredRequired = required.stream()
                .filter(q -> stepAnswers.stream().anyMatch(a -> q.getId().equals(a.getQuestionId())))
                .count();
        if (answeredRequired < required.size()) {
            throw badRequest("모든 질문에 답한 뒤 단계를 완료하세요. ("
                    + answeredRequired + "/" + required.size() + ")");
        }

        Instant now = Instant.now();
        String summary = buildSummaryMd(session, step, stepAnswers);
        StepResultEntity result = results.findBySessionIdAndStepId(sessionId, stepId)
                .orElseGet(StepResultEntity::new);
        result.setSessionId(sessionId);
        result.setStepId(stepId);
        result.setStatus("DONE");
        result.setSummaryMd(summary);
        result.setConfirmedAt(now);
        results.save(result);
        upsertStepSession(session, step, "DONE", summary, now);

        if (step.isGate()) {
            String verdict = session.getGateStatus();
            StepAnswerEntity noteAns = stepAnswers.stream()
                    .filter(a -> "gate_note".equals(a.getQuestionId()))
                    .findFirst()
                    .orElse(null);
            if (noteAns != null) {
                session.setGateNote(stripJson(noteAns.getAnswerJson()));
            }
            if (!GATE_UNLOCK.contains(verdict) && !"STOP".equals(verdict) && !"HOLD".equals(verdict)) {
                throw badRequest("Gate 판정(통과/조건부/보완/중단)이 필요합니다.");
            }
        }

        if (step.getNextId() != null) {
            StepDefinition next = catalog.requireStep(step.getNextId());
            if (!next.isRequiresGate() || isUnlocked(session)) {
                session.setCurrentStepId(step.getNextId());
            }
        }
        session.setUpdatedAt(now);
        sessions.save(session);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result", result);
        body.put("session", session);
        body.put("nextId", step.getNextId());
        body.put("unlocked", isUnlocked(session));
        return body;
    }

    @Transactional
    public CrudSessionEntity applyGate(String sessionId, Map<String, Object> payload) {
        CrudSessionEntity session = get(sessionId);
        String verdict = stringVal(payload.get("verdict")).toUpperCase();
        if (!Set.of("PASS", "CONDITIONAL", "HOLD", "STOP", "NONE").contains(verdict)) {
            throw badRequest("verdict는 PASS|CONDITIONAL|HOLD|STOP|NONE 중 하나여야 합니다.");
        }
        session.setGateStatus(verdict);
        session.setGateNote(stringVal(payload.get("note")));
        session.setUpdatedAt(Instant.now());
        return sessions.save(session);
    }

    @Transactional
    public CrudSessionEntity moveTo(String sessionId, String stepId) {
        CrudSessionEntity session = get(sessionId);
        StepDefinition step = catalog.requireStep(stepId);
        assertStepAccessible(session, step);
        session.setCurrentStepId(stepId);
        session.setUpdatedAt(Instant.now());
        return sessions.save(session);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryEntity> ledger(String sessionId) {
        get(sessionId);
        return ledger.findBySessionIdOrderByEntryKeyAsc(sessionId);
    }

    @Transactional(readOnly = true)
    public byte[] exportZip(String sessionId) {
        CrudSessionEntity session = get(sessionId);
        List<StepResultEntity> stepResults = results.findBySessionId(sessionId);
        List<LedgerEntryEntity> entries = ledger.findBySessionIdOrderByEntryKeyAsc(sessionId);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            put(zos, "결과/_확정정보원장.md", buildLedgerMd(session, entries));
            for (StepResultEntity r : stepResults) {
                StepDefinition step = catalog.findStep(r.getStepId()).orElse(null);
                String path = step != null && step.getResultFile() != null
                        ? step.getResultFile()
                        : "결과/" + r.getStepId() + ".md";
                put(zos, path, r.getSummaryMd() == null ? "" : r.getSummaryMd());
            }
            put(zos, "결과/README.md",
                    "# CRUD Meoy Export\n\nsession=" + session.getId() + "\nname=" + session.getName() + "\n");
            zos.finish();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Export 실패", ex);
        }
    }

    private void upsertStepSession(
            CrudSessionEntity session,
            StepDefinition step,
            String status,
            String summaryMd,
            Instant now) {
        CrudStepSessionEntity entity = stepSessions
                .findBySessionIdAndStepId(session.getId(), step.getId())
                .orElseGet(CrudStepSessionEntity::new);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setSessionId(session.getId());
        entity.setSessionName(session.getName());
        entity.setStepId(step.getId());
        entity.setStepTitle(step.getTitle());
        entity.setStepOrder(step.getOrder());
        entity.setStatus(status);
        entity.setBusinessCode(session.getBusinessCode());
        entity.setDomainCode(session.getDomainCode());
        entity.setAnswersJson(toJson(buildAnswersSnapshot(session.getId(), step.getId())));
        if (summaryMd != null) {
            entity.setSummaryMd(summaryMd);
        }
        entity.setUpdatedAt(now);
        if ("DONE".equals(status)) {
            entity.setConfirmedAt(now);
        }
        stepSessions.save(entity);
    }

    private List<Map<String, Object>> buildAnswersSnapshot(String sessionId, String stepId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (StepAnswerEntity a : answers.findBySessionIdAndStepId(sessionId, stepId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("questionId", a.getQuestionId());
            row.put("answer", parseJson(a.getAnswerJson()));
            row.put("note", a.getNote());
            list.add(row);
        }
        return list;
    }

    private void syncBusinessDomainFromLedger(CrudSessionEntity session) {
        ledger.findBySessionIdAndEntryKey(session.getId(), "c01.businessCode")
                .ifPresent(e -> session.setBusinessCode(e.getValue()));
        ledger.findBySessionIdAndEntryKey(session.getId(), "c01.domainCode")
                .ifPresent(e -> session.setDomainCode(e.getValue()));
    }

    private void assertStepAccessible(CrudSessionEntity session, StepDefinition step) {
        if (step.isRequiresGate() && !isUnlocked(session)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "C14 Gate 통과/조건부 통과 후에만 진행할 수 있습니다. 현재=" + session.getGateStatus());
        }
    }

    private boolean isUnlocked(CrudSessionEntity session) {
        return GATE_UNLOCK.contains(session.getGateStatus());
    }

    private void upsertLedger(String sessionId, String key, String value, String stepId, Instant now) {
        LedgerEntryEntity entry = ledger.findBySessionIdAndEntryKey(sessionId, key)
                .orElseGet(LedgerEntryEntity::new);
        entry.setSessionId(sessionId);
        entry.setEntryKey(key);
        entry.setValue(value == null ? "" : value);
        entry.setSourceStepId(stepId);
        entry.setUpdatedAt(now);
        ledger.save(entry);
    }

    private String buildSummaryMd(CrudSessionEntity session, StepDefinition step, List<StepAnswerEntity> stepAnswers) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(step.getId()).append(" — ").append(step.getTitle()).append(" (확정)\n\n");
        sb.append("| 구분 | 내용 |\n| --- | --- |\n");
        sb.append("| 단계 | ").append(step.getId()).append(" |\n");
        sb.append("| 세션 | ").append(session.getName()).append(" |\n");
        sb.append("| 상태 | DONE |\n");
        if (step.isGate()) {
            sb.append("| Gate | ").append(session.getGateStatus()).append(" |\n");
        }
        sb.append("\n## 확정 답변\n\n");
        for (QuestionDefinition q : visibleQuestions(session.getId(), step)) {
            StepAnswerEntity a = stepAnswers.stream()
                    .filter(x -> x.getQuestionId().equals(q.getId()))
                    .findFirst()
                    .orElse(null);
            sb.append("### ").append(q.getText()).append("\n\n");
            sb.append("- 답: ").append(a == null ? "(없음)" : displayAnswer(parseJson(a.getAnswerJson()), q)).append("\n\n");
        }
        sb.append("## Gate 체크\n\n");
        for (String check : step.getGateChecks()) {
            sb.append("- [x] ").append(check).append("\n");
        }
        return sb.toString();
    }

    private String buildLedgerMd(CrudSessionEntity session, List<LedgerEntryEntity> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 확정정보 원장\n\n");
        sb.append("| 구분 | 값 | 근거 |\n| --- | --- | --- |\n");
        sb.append("| 세션 | ").append(session.getName()).append(" | — |\n");
        sb.append("| Gate | ").append(session.getGateStatus()).append(" | C14 |\n");
        sb.append("| 현재단계 | ").append(session.getCurrentStepId()).append(" | — |\n");
        for (LedgerEntryEntity e : entries) {
            sb.append("| ").append(e.getEntryKey()).append(" | ")
                    .append(escapePipe(e.getValue())).append(" | ")
                    .append(e.getSourceStepId() == null ? "" : e.getSourceStepId())
                    .append(" |\n");
        }
        return sb.toString();
    }

    private void put(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private List<QuestionDefinition> visibleQuestions(String sessionId, StepDefinition step) {
        List<StepAnswerEntity> stepAnswers = answers.findBySessionIdAndStepId(sessionId, step.getId());
        return step.getQuestions().stream()
                .filter(q -> isQuestionVisible(q, stepAnswers))
                .toList();
    }

    private boolean isQuestionVisible(QuestionDefinition q, List<StepAnswerEntity> stepAnswers) {
        if (q.getShowWhen() == null
                || q.getShowWhen().getQuestionId() == null
                || q.getShowWhen().getQuestionId().isBlank()) {
            return true;
        }
        String expected = q.getShowWhen().getEquals() == null ? "" : q.getShowWhen().getEquals().trim();
        return stepAnswers.stream()
                .filter(a -> q.getShowWhen().getQuestionId().equals(a.getQuestionId()))
                .map(a -> stringVal(parseJson(a.getAnswerJson())).trim())
                .anyMatch(v -> v.equals(expected)
                        || v.startsWith(expected + " ")
                        || v.startsWith(expected + " —")
                        || v.startsWith(expected + "—"));
    }

    private String displayAnswer(Object answer, QuestionDefinition question) {
        String raw = stringVal(answer);
        if (question.getOptions() != null) {
            return question.getOptions().stream()
                    .filter(o -> o.getValue().equals(raw))
                    .map(o -> o.getValue() + " — " + o.getLabel())
                    .findFirst()
                    .orElse(raw);
        }
        return raw;
    }

    private Object parseJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ex) {
            return json;
        }
    }

    private String stripJson(String json) {
        Object v = parseJson(json);
        return stringVal(v);
    }

    private String toJson(Object answer) {
        try {
            return objectMapper.writeValueAsString(answer);
        } catch (JsonProcessingException ex) {
            throw badRequest("답변 JSON 직렬화 실패");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String stringVal(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String escapePipe(String s) {
        return s == null ? "" : s.replace("|", "\\|").replace("\n", " ");
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
