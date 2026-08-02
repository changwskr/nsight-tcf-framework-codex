# 02 — Requirements Traceability

기준본: [00-CRUD-Prompt-Architecture.md](./00-CRUD-Prompt-Architecture.md)  
요구 ID는 본 패키지(프롬프트 아키텍처) 범위다. 업무 CRUD 기능 요구(예: AV.CustomerContact)는 결과폴더 C17을 사용한다.

상태: `Satisfied` = 구현·문서·실행으로 확인됨 · `Partial` = 일부 · `Open` = 미완

---

## 추적 매트릭스

| ID | 요구 | 기준본 | 구현/문서 | 검증 | 상태 |
| --- | --- | --- | --- | --- | --- |
| R01 | 신규 자유 입력과 기존 결과폴더 모두 처리 | §7 | MASTER INPUT 유형 A/C | 결과-1 재개 실행 | Satisfied |
| R02 | Codex·Cursor에서 전용 도구명 없이 실행 | §3 | MASTER/STANDALONE 텍스트 프롬프트 | 붙여넣기 실행 | Satisfied |
| R03 | 저장소 연동형·독립형이 동일 상태·Gate·완료 | §6, §9 | MASTER·STANDALONE | 문서 대조 | Satisfied |
| R04 | 저장소에서 확인 가능한 값 불필요 질문 금지 | §6 DISCOVERY | MASTER 질문 규칙 | 결과-1 DESIGN 생략 | Satisfied |
| R05 | C14 승인 전 업무 소스 수정 금지 | §6.1 | MASTER Hard Gate | Gate 전 소스 미수정 | Satisfied |
| R06 | Gate 승인 후 목록~검증 자동 | §6, ADR-004 | MASTER | 승인 후 C17/C18·test | Satisfied |
| R07 | 요청하지 않은 CRUD·선택 산출물 금지 | §7 | MASTER 범위 규칙 | OM 미포함 승인 | Satisfied |
| R08 | 미커밋·보호 경로 보존 | §8 | WORKTREE_CONFLICT | help 등 미침범 | Satisfied |
| R09 | 실행/미검증 구분 보고 | §9 | REPORT·C16 | 실거래 미검증 Explicit | Satisfied |
| R10 | C00~C18·C15 관계 README 설명 | §5, §10 | [../README.md](../README.md) | 링크 존재 | Satisfied |
| R11 | As-Is/To-Be/전환이 문서화됨 | §2, §10 | 00 본문 | 본 패키지 | Satisfied |
| R12 | 상태별 입출력·전이·실패 정의 | §6, §8 | 00·03 | Gate 체크 | Satisfied |
| R13 | 주요 결정이 ADR에 연결 | — | 01 ADR-001~006 | 링크 | Satisfied |
| R14 | Runbook으로 중단 재개 가능 | — | 03 | 시나리오 정의 | Satisfied |
| R15 | Architecture Gate로 전후 판정 | — | 04 | 체크리스트 | Satisfied |
| R16 | 확인 안 된 사실 ≠ 확정값 | §8 | 출처 표기 규칙 | CONDITIONAL Explicit | Satisfied |
| R17 | Framework 전체 아키텍처 미복제 | §1 | 00 참조만 | — | Satisfied |
| R18 | UTF-8 MD·상대 링크 | 스펙 §16 | architecture/* | 수동 링크 검토 | Partial |
| R19 | C15 제거 여부 결정 | §10 | ADR-003 보류 | — | Open |
| R20 | 중복 규칙 SoT 링크 정리 | §10 단계7 | 후속 | — | Open |

---

## 구성요소 매핑

| 구성요소 | 관련 요구 |
| --- | --- |
| MASTER | R01~R09, R12 |
| STANDALONE | R02, R03, ADR-005 |
| C15 호환 | R10, R19 |
| 원장·C00~C18 | R01, R04, R10 |
| architecture/* | R11~R18 |
| README | R10 |

---

## 대표 시나리오 ↔ 요구

| 시나리오 | 요구 | 증적 |
| --- | --- | --- |
| 결과-1 MASTER 재개 | R01, R04, R05 | Gate 보고 |
| CONDITIONAL + OM 미포함 승인 | R06, R07, R09 | C17/C18, test PASS |
| LN/AV 충돌 해소(이전) | R08, R16, DESIGN_CONFLICT | 원장 AV 정합 |
