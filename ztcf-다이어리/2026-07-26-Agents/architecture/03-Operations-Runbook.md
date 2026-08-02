# 03 — Operations Runbook

기준본: [00-CRUD-Prompt-Architecture.md](./00-CRUD-Prompt-Architecture.md)  
실행 진입: [../prompts/MASTER-CRUD-DEVELOPER.md](../prompts/MASTER-CRUD-DEVELOPER.md)

각 시나리오: 선행조건 → 절차 → 성공 조건 → 실패 시 재개.

---

## R0. 공통 체크포인트 (재개 키)

상태 보고에 항상 남긴다.

```text
현재상태 / 결과폴더 / Gate판정 / 승인여부 /
확정값요약 / 가정 / 충돌 / OpenIssue /
보호경로 / 다음전이조건
```

원장 경로: `결과*/_확정정보원장.md`  
명령: `현황` 으로 요약 요청.

---

## R1. 신규 CRUD 작업 시작

**선행:** 저장소 체크아웃, MASTER 파일 존재

1. MASTER 붙여넣기 블록을 채팅에 붙인다.
2. `CRUD 개발 시작: <요구>` 또는 REQUEST TEMPLATE을 채운다.
3. INPUT→DISCOVERY→DESIGN→GATE까지 진행한다.
4. Gate 보고를 확인한다. FAIL이면 설계 보완.

**성공:** GATE에 도달, 업무 소스 미수정  
**실패:** DISCOVERY 근거 부족 → 모듈/BC를 명시하고 `현황` 후 DESIGN 재개

---

## R2. 기존 C00~C14 결과에서 재개

**선행:** 결과폴더에 원장·C14 존재

1. MASTER 붙이기
2. `기존 결과로 개발: <결과폴더>`
3. 원장·C14·소스 정합만 검사 (DESIGN 생략 가능)
4. Gate 제시

**성공:** CONDITIONAL/PASS Gate 보고  
**충돌:** `DESIGN_CONFLICT` 표 → 사용자 선택 후 원장 정합 → Gate 재제시

---

## R3. CONDITIONAL Gate 승인

**선행:** Gate = CONDITIONAL, Open Issue Explicit

1. Open Issue 포함 여부 결정 (예: OM 포함/미포함)
2. `Gate 승인` 또는 `Gate 승인 (OM 포함)`
3. IMPLEMENT→VERIFY→TRACE→REPORT 자동

**성공:** REPORT와 검증 명령 결과  
**주의:** CONDITIONAL이어도 승인 없으면 소스 수정 금지

---

## R4. 보호 경로 추가

1. `보호: <경로>` (여러 번 가능)
2. IMPLEMENT 시 해당 경로 신규/수정 금지

**성공:** 파일 구분표에 `보호`  
**충돌:** 반드시 수정해야 하면 `WORKTREE_CONFLICT`로 중단·판단 요청

---

## R5. 컴파일·테스트 실패 복구

**오류:** `IMPLEMENTATION_FAILURE`

1. 실패 명령·로그 요약
2. 구현 결함이면 요청 범위 안 수정
3. 동일 검증 재실행
4. 범위 초과면 중단·사용자 판단

**성공:** 동일 명령 PASS  
**기존 실패:** 요청 변경과 무관함을 REPORT에 구분

---

## R6. 외부 DB 없이 가능한 검증

1. `:module:test` / `compileJava` 우선
2. H2·로컬 schema가 있으면 단위·구조까지
3. `/online`·운영 DB는 미검증 Explicit

**성공:** 단위/컴파일 증적 + 미검증 목록  
예: `.\gradlew.bat :av-service:test`

---

## R7. Codex ↔ Cursor 작업 인계

**선행:** 원장·최근 상태 보고 존재

인계 메시지 최소 세트:

```text
결과폴더=
현재상태=
Gate= / 승인여부=
보호경로=
OpenIssue=
다음에 할 일=
금지(미커밋 덮지 말 것)=
```

수신측: MASTER 붙이기 → `현황` → 상태부터 재개  
**성공:** 동일 결과폴더·상태에서 연속 진행

---

## R8. 대화 중단 후 재개

1. 결과폴더 원장·C14~C18 최신본 확인
2. MASTER + `기존 결과로 개발: …` 또는 `현황`
3. GATE 이전이면 설계만, 승인이면 VERIFY/TRACE부터

**성공:** 체크포인트와 원장이 일치

---

## R9. C15 호환 실행

1. [C15-실행프롬프트.md](../prompts/C15-실행프롬프트.md) 붙이기
2. 결과폴더 지정
3. 파일 목록 → `승인` (MASTER의 Gate 승인과 다름)

**성공:** C15 규칙으로 생성  
**주의:** MASTER와 승인 모델이 다름 ([ADR-003](./01-Architecture-Decision-Records.md))

---

## R10. 작업 취소와 생성·수정 파일 식별

1. `중단` — 요약 후 종료
2. `git status` / Diff로 이번 작업 파일 식별
3. 미커밋 사용자 파일과 구분
4. 되돌리기는 사용자 명시 요청 시에만

**성공:** 생성·수정·보호·스킵 목록이 REPORT에 남음

---

## 오류 코드 빠른 대응

| 코드 | 즉시 행동 |
| --- | --- |
| DESIGN_CONFLICT | 충돌표 → 선택 → 원장 수정 → Gate 재개 |
| WORKTREE_CONFLICT | 수정 중단 → 보호 또는 사용자 정리 |
| IMPLEMENTATION_FAILURE | R5 |
| ENVIRONMENT_FAILURE | 명령·원인·미검증 기록 → R6 범위로 축소 |
