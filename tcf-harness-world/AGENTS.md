# TCF Harness World Agent Instructions

## 적용 범위

이 파일은 `tcf-harness-world/` 전체에 적용된다. 저장소 루트 `AGENTS.md`를 함께 따른다.

## 작업 원칙

- 하네스 정의는 `skills/`, 역할 계약은 `agents/`, 사용 설명은 `docs/`에 둔다.
- 역할 간 입력과 출력은 파일 경로와 완료 조건으로 명시한다.
- 독립 작업만 병렬화하며 사용자 또는 상위 지침이 허용하지 않으면 에이전트를 생성하지 않는다.
- 기존 사용자 산출물을 보존하고 생성 파일은 임시 파일에서 완성한 뒤 이동한다.

## 역할 흐름

1. Analyst가 요구사항, 제약, 위험, 완료 기준을 작성한다.
2. Builder가 승인된 분석을 입력으로 구현 산출물을 만든다.
3. QA가 요구사항 추적성, 파일 구조, 실행 결과를 검증한다.
4. 실패 시 원인과 담당 역할을 명시해 제한된 수정만 요청한다.

## 검증

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\test-verifier.ps1 -Mode All
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-codex-harness.ps1
```
