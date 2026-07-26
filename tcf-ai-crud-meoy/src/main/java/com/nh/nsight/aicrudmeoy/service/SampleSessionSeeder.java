package com.nh.nsight.aicrudmeoy.service;

import com.nh.nsight.aicrudmeoy.store.CrudSessionEntity;
import com.nh.nsight.aicrudmeoy.store.CrudSessionRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * LN.CustomerContact 조회 시범 CRUD 대화 샘플을 DB에 적재한다.
 */
@Component
public class SampleSessionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleSessionSeeder.class);

    private final CrudSessionRepository sessions;
    private final CrudSessionService sessionService;

    public SampleSessionSeeder(CrudSessionRepository sessions, CrudSessionService sessionService) {
        this.sessions = sessions;
        this.sessionService = sessionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (sessions.existsBySampleFlagTrue()) {
            log.info("CRUD Meoy sample session already present — skip seed");
            return;
        }
        String sessionId = seedLnCustomerContactSample();
        log.info("CRUD Meoy sample session seeded: {}", sessionId);
    }

    @Transactional
    public String seedLnCustomerContactSample() {
        CrudSessionEntity session = sessionService.create("[샘플] LN.CustomerContact 조회");
        session.setSampleFlag(true);
        session.setBusinessCode("LN");
        session.setDomainCode("CustomerContact");
        sessions.save(session);
        String id = session.getId();

        // C-MASTER ~ C14 (조건부 통과) — 시범 조회 CRUD
        answerAndComplete(id, "C-MASTER", Map.of("master_ack", "1"));
        answerAndComplete(id, "C00", Map.of(
                "dev_mode", "2",
                "base_module", "ln-service",
                "run_mode", "1"));
        answerAndComplete(id, "C01", Map.of(
                "business_code", "LN",
                "domain_code", "CustomerContact",
                "domain_name", "고객연락처"));
        answerAndComplete(id, "C02", Map.of(
                "crud_scope", "2",
                "service_ids", "LN.CustomerContact.selectList, LN.CustomerContact.selectDetail"));
        answerAndComplete(id, "C03", Map.of(
                "screen_id", "/ln/contact-list.html",
                "events", "조회→selectList, 행클릭→selectDetail"));
        answerAndComplete(id, "C04", Map.of(
                "table_info", "1",
                "table_name", "LN_CUSTOMER_CONTACT",
                "pk", "CONTACT_ID"));
        answerAndComplete(id, "C05", Map.of("rules", "없음(조회만)"));
        answerAndComplete(id, "C06", Map.of(
                "service_id_final", "LN.CustomerContact.selectList, LN.CustomerContact.selectDetail",
                "tx_code", "미확정"));
        answerAndComplete(id, "C07", Map.of(
                "dto_note", "selectList(req:keyword)→list / selectDetail(req:contactId)→detail"));
        answerAndComplete(id, "C08", Map.of(
                "package_layout", "1",
                "handler_name", "LnCustomerContactHandler"));
        answerAndComplete(id, "C09", Map.of("mapper_ids", "selectList, selectDetail"));
        answerAndComplete(id, "C10", Map.of(
                "tx_policy", "1",
                "timeout_sec", "5"));
        answerAndComplete(id, "C11", Map.of(
                "pii", "1",
                "pii_cols", "CONTACT_VALUE"));
        answerAndComplete(id, "C12", Map.of("test_scope", "1"));
        answerAndComplete(id, "C13", Map.of("om_plan", "3"));
        answerAndComplete(id, "C14", Map.of(
                "gate_verdict", "CONDITIONAL",
                "gate_note", "OM 실등록·포트/거래코드 공식채번 Gap Explicit"));

        // C15 이후 IN_PROGRESS 일부
        sessionService.saveAnswer(id, answer("C15", "impl_plan", "2"));
        sessionService.saveAnswer(id, answer("C15", "impl_note", "ln-service CustomerContact 구현됨"));
        sessionService.completeStep(id, "C15");
        sessionService.saveAnswer(id, answer("C16", "verify_level", "1"));
        sessionService.saveAnswer(id, answer("C16", "verify_result", "미실행(샘플)"));

        return id;
    }

    private void answerAndComplete(String sessionId, String stepId, Map<String, String> answers) {
        for (Map.Entry<String, String> e : answers.entrySet()) {
            sessionService.saveAnswer(sessionId, answer(stepId, e.getKey(), e.getValue()));
        }
        sessionService.completeStep(sessionId, stepId);
    }

    private static Map<String, Object> answer(String stepId, String questionId, String value) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stepId", stepId);
        body.put("questionId", questionId);
        body.put("answer", value);
        return body;
    }
}
