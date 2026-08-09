# 04. PDMP 실행 워크플로

대상 하네스: `tcf-harness-exe-pdmp`  
대상 코드: `pdmp-service` (하네스에서 `../pdmp-service`)

## 1. 사전 조건

- JDK / `JAVA_HOME` / PATH 준비 (스크립트가 JDK를 설치하지 않음)
- `pdmp-service`에서 `gradlew.bat` 동작
- 사용자 작업 트리 보존: `git status --short` 확인 후 무관 파일 커밋·리셋 금지

## 2. 표준 순서 (AGENTS.md)

```text
1. analysis
2. user approval          ← 구현 전 필수
3. implementation plan
4. implementation         ← TDD: RED → 최소 구현 → GREEN
5. security review        ← auth/SQL/secret/PII/CORS 등 해당 시
6. QA                     ← focused test → gradlew test/war
```

승인 없이 가면 안 되는 변경:

- public API
- 스키마
- 보안 경계
- `@TcfTransaction` 메타데이터
- 삭제(soft/hard) 정책

## 3. Claude `/harness` 사용

1. Claude Code에서 작업 루트를 `tcf-harness-exe-pdmp` (또는 저장소 루트 + 하네스 인식)로
2. `/harness` 실행 후 요청 설명
3. 옵션이 제시되면 **번호로 선택** (예: 안정화 vs 신규 CRUD)
4. phase 디렉터리와 step 파일이 생성·갱신되는지 확인

### 신규 CRUD를 시킬 때 필수 정보

| 항목 | 예 |
|------|-----|
| 프로그램/클래스 prefix | `mpcoa7777` |
| serviceId / transactionCode | 승인된 값 |
| 테이블·키 | 명시 |
| 삭제 의미 | soft delete / hard delete |
| TCF 헤더·처리유형 | 명시 |
| 보안 | JWT 필요 여부, 공개 경로 |

정보가 없으면 **추론하지 말고 질문**.

## 4. phases 산출물 규약

```text
phases/
  index.json                          # phase 목록
  N-<slug>/
    index.json                        # step 목록·상태
    step0.md …                        # 분석·계획·구현 기록
    security-review.md                # 해당 시
    qa-report.md                      # 명령·exit code·결과
```

각 step/QA에는 최소한:

- 실행 명령
- exit code
- 핵심 출력(민감정보 제외)
- 남은 리스크 (예: Oracle 미검증)

## 5. TDD 최소 루프

```text
1) 가장 좁은 테스트 작성
2) 실행 → RED 기록
3) 승인된 최소 변경만 구현
4) 동일 테스트 GREEN 기록
5) 범위 허용 시 gradlew test / war
```

## 6. 보안 체크리스트 (요약)

- JWT 보호 경로가 열리지 않았는가
- SQL 값 바인딩 (문자열 연결 금지)
- 로그에 credential / token / 개인정보 없음
- CORS·에러 메시지 과다 노출 없음
- 시크릿은 환경변수/플레이스홀더, 기본값은 로컬 전용임을 명시

## 7. H2 vs Oracle

| 환경 | 의미 |
|------|------|
| H2 테스트 GREEN | 로컬/CI 단위 증거 |
| Oracle | 승인된 Oracle 환경 없이 **미검증**으로 명시 |

QA 보고에 둘을 섞어 “프로덕션 검증 완료”로 쓰지 않는다.

## 8. 자주 쓰는 명령

```powershell
# 대상 프로젝트
cd c:\Programming(23-08-15)\nsight-tcf-framework\pdmp-service
.\gradlew.bat test
.\gradlew.bat war

# 특정 테스트
.\gradlew.bat test --tests "*mpcoa8888*"
.\gradlew.bat test --tests "*SecurityConfigTest*"
```

## 9. 계약 하네스와의 역할 분담

| 작업 | 패키지 |
|------|--------|
| 구현·빌드 고침 | `tcf-harness-exe-pdmp` |
| 계약 문서·역할 정의 갱신 | `tcf-harness-pdmp` (구현 후 동기화) |
| 다이어리 지식화 | `ztcf-다이어리/2026-08-05-하네스-집필` (본 문서) |
