package com.nh.nsight.harness.service;

import com.nh.nsight.harness.domain.RequirementAnswer;
import com.nh.nsight.harness.domain.Stage;
import com.nh.nsight.harness.domain.StageStatus;
import com.nh.nsight.harness.domain.WorkItemState;
import com.nh.nsight.harness.storage.JsonStateRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public final class RequirementService {
    private static final List<RequirementQuestion> QUESTIONS = List.of(
            new RequirementQuestion("REQ-Q01", "업무 요청 제목", "개발할 업무 또는 기능의 제목은 무엇입니까?"),
            new RequirementQuestion("REQ-Q02", "개발 목적과 배경", "현재 문제와 개발이 필요한 이유는 무엇입니까?"),
            new RequirementQuestion("REQ-Q03", "대상 사용자", "이 기능을 사용하는 사용자와 조직은 누구입니까?"),
            new RequirementQuestion("REQ-Q04", "업무 시작 조건", "업무가 시작되는 화면 이벤트 또는 선행 조건은 무엇입니까?"),
            new RequirementQuestion("REQ-Q05", "입력정보", "사용자 또는 연계 시스템이 제공해야 하는 입력정보는 무엇입니까?"),
            new RequirementQuestion("REQ-Q06", "처리 절차", "정상 업무 처리 절차를 순서대로 설명해 주세요."),
            new RequirementQuestion("REQ-Q07", "업무 규칙", "반드시 적용해야 하는 업무 규칙과 검증 조건은 무엇입니까?"),
            new RequirementQuestion("REQ-Q08", "출력정보", "정상 처리 후 화면 또는 연계 시스템에 반환할 정보는 무엇입니까?"),
            new RequirementQuestion("REQ-Q09", "오류·예외 조건", "처리를 중단하거나 별도로 안내해야 하는 오류·예외 조건은 무엇입니까?"),
            new RequirementQuestion("REQ-Q10", "데이터·외부 연계", "사용할 테이블, 파일, 메시지 또는 외부 시스템은 무엇입니까?"),
            new RequirementQuestion("REQ-Q11", "권한·보안·감사", "권한, 개인정보, 마스킹, 암호화 및 감사 요구사항은 무엇입니까?"),
            new RequirementQuestion("REQ-Q12", "인수조건·완료기준", "어떤 결과와 테스트가 충족되어야 완료로 승인할 수 있습니까?")
    );

    private final JsonStateRepository repository;
    private final Path repositoryRoot;

    public RequirementService(JsonStateRepository repository, Path repositoryRoot) {
        this.repository = repository;
        this.repositoryRoot = repositoryRoot;
    }

    public List<RequirementQuestion> questions() {
        return QUESTIONS;
    }

    public Optional<RequirementQuestion> nextQuestion(String workItemId) throws IOException {
        WorkItemState state = repository.load(workItemId);
        return QUESTIONS.stream()
                .filter(question -> state.requirementAnswers().stream()
                        .noneMatch(answer -> answer.questionId().equals(question.id())))
                .findFirst();
    }

    public void answer(String workItemId, String questionId, String answer) throws IOException {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("Requirement answer must not be blank");
        }
        RequirementQuestion question = QUESTIONS.stream()
                .filter(candidate -> candidate.id().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown requirement question: " + questionId));
        WorkItemState state = repository.load(workItemId);
        StageStatus requirementStatus = state.stage(Stage.REQUIREMENT).status();
        if (requirementStatus != StageStatus.IN_PROGRESS && requirementStatus != StageStatus.REVISION_REQUIRED) {
            throw new IllegalStateException("Requirement answers can be changed only in IN_PROGRESS or REVISION_REQUIRED: "
                    + requirementStatus);
        }
        state.addOrReplaceRequirementAnswer(new RequirementAnswer(
                question.id(), question.question(), answer.trim(), OffsetDateTime.now().toString()));
        repository.save(state);
        writeRequirementDocument(state);
    }

    public Path writeRequirementDocument(WorkItemState state) throws IOException {
        Path workItemDirectory = repositoryRoot.resolve("docs/work-items").resolve(state.workItemId());
        Files.createDirectories(workItemDirectory);
        StringBuilder md = new StringBuilder();
        md.append("# ").append(state.workItemId()).append(" — ").append(state.title()).append(" 요건 정의서\n\n");

        md.append("## 1. 도입 전 안내말\n\n");
        md.append("본 문서는 CLI 하네스의 고정 12문항 답변을 기준으로 생성된 요건 기준선이다. ")
                .append("미응답 항목은 `미확정`으로 표시하며 에이전트가 임의로 보완하지 않는다.\n\n");

        md.append("## 2. 문서 개요\n\n");
        md.append("### 2.1 목적\n\n").append(valueFor(state, "REQ-Q02")).append("\n\n");
        md.append("### 2.2 적용범위\n\n").append(valueFor(state, "REQ-Q01")).append("\n\n");
        md.append("### 2.3 대상 독자\n\n").append(valueFor(state, "REQ-Q03")).append("\n\n");
        md.append("### 2.4 선행조건\n\n").append(valueFor(state, "REQ-Q04")).append("\n\n");
        md.append("### 2.5 용어 정의\n\n");
        md.append("요건 분석 과정에서 별도 정의가 필요한 업무 용어는 분석서에서 식별하고 사용자 승인을 받는다.\n\n");

        md.append("## 3. 본문\n\n");
        md.append("### 3.1 문제 정의 및 개발 배경\n\n").append(valueFor(state, "REQ-Q02")).append("\n\n");
        md.append("### 3.2 대상 사용자와 권한 범위\n\n").append(valueFor(state, "REQ-Q03")).append("\n\n");
        md.append("### 3.3 업무 시작 조건\n\n").append(valueFor(state, "REQ-Q04")).append("\n\n");
        md.append("### 3.4 입력 요구사항\n\n").append(valueFor(state, "REQ-Q05")).append("\n\n");
        md.append("### 3.5 정상 처리 절차\n\n").append(valueFor(state, "REQ-Q06")).append("\n\n");
        md.append("### 3.6 업무 규칙\n\n").append(valueFor(state, "REQ-Q07")).append("\n\n");
        md.append("### 3.7 출력 요구사항\n\n").append(valueFor(state, "REQ-Q08")).append("\n\n");
        md.append("### 3.8 오류·예외 요구사항\n\n").append(valueFor(state, "REQ-Q09")).append("\n\n");
        md.append("### 3.9 데이터·외부 연계 요구사항\n\n").append(valueFor(state, "REQ-Q10")).append("\n\n");
        md.append("### 3.10 보안·개인정보·감사 요구사항\n\n").append(valueFor(state, "REQ-Q11")).append("\n\n");
        md.append("### 3.11 성능·운영 요구사항\n\n");
        md.append("별도 수치가 제시되지 않은 경우 `미확정`으로 유지하고 분석 단계에서 질문한다.\n\n");
        md.append("### 3.12 인수조건과 완료기준\n\n").append(valueFor(state, "REQ-Q12")).append("\n\n");
        md.append("### 3.13 제외범위와 미결사항\n\n");
        md.append("12문항 답변에서 명시되지 않은 기능·데이터·연계·운영 항목은 자동으로 범위에 포함하지 않는다.\n\n");

        md.append("## 4. 요건 추적 원장\n\n");
        md.append("| 요건 ID | 질문 영역 | 상태 |\n|---|---|---|\n");
        for (RequirementQuestion question : QUESTIONS) {
            boolean answered = state.requirementAnswers().stream()
                    .anyMatch(candidate -> candidate.questionId().equals(question.id()));
            md.append("| `").append(question.id()).append("` | ")
                    .append(question.title()).append(" | ")
                    .append(answered ? "확정 입력" : "미확정").append(" |\n");
        }
        md.append("\n");

        md.append("## 5. 승인 및 변경 통제\n\n");
        md.append("- 12개 필수 질문에 모두 답변한 뒤 REVIEW 상태로 전환한다.\n");
        md.append("- 사용자 승인 전에는 분석 단계로 진행하지 않는다.\n");
        md.append("- 승인 이후 변경은 `REVISION_REQUIRED` 결정과 재승인을 거친다.\n\n");

        md.append("## 6. 시사점\n\n");
        md.append("요건은 분석·설계·구현·테스트 단계의 읽기 전용 기준선이며, 후속 단계는 누락사항을 임의로 추정하지 않는다.\n\n");

        md.append("## 7. 마무리말\n\n");
        md.append("미확정 사항과 상충하는 답변은 분석 단계의 질문과 위험 원장으로 이관하고 사용자 결정을 받는다.\n");

        Path target = workItemDirectory.resolve("requirement.md");
        Files.writeString(target, md.toString(), StandardCharsets.UTF_8);
        return target;
    }

    private String valueFor(WorkItemState state, String questionId) {
        return state.requirementAnswers().stream()
                .filter(candidate -> candidate.questionId().equals(questionId))
                .map(RequirementAnswer::answer)
                .findFirst()
                .orElse("미확정");
    }

}
