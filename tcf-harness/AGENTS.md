# AGENTS.md — 저장소 작업 지도

이 파일은 백과사전이 아니라 에이전트가 필요한 기준으로 이동하기 위한 짧은 지도다.

## 반드시 읽는 순서

1. 현재 작업의 `docs/work-items/{workItemId}/README.md`
2. 현재 단계의 승인된 선행 산출물
3. [아키텍처 지도](./ARCHITECTURE.md)
4. [마스터 하네스 프롬프트](./harness/prompts/MASTER-HARNESS.md)
5. 현재 단계 프롬프트

## 공통 불변조건

- 기준 브랜치 직접 수정 금지
- 자동 Push·Merge 금지
- 승인된 선행 산출물 임의 변경 금지
- 테스트 삭제·비활성화·조건 완화 금지
- 비밀번호·토큰·Private Key·개인정보 원문 기록 금지
- 현재 작업과 관계없는 리팩터링 금지
- 성공 증적이 없으면 완료 보고 금지

## 주요 위치

| 위치 | 역할 |
|---|---|
| `harness/prompts/` | 공통·단계별 에이전트 지시 |
| `harness/templates/` | 산출물 기본 골격 |
| `harness/schemas/` | 상태·실행계약 검증 스키마 |
| `docs/work-items/` | 작업별 요건부터 종료까지의 기록 |
| `.harness/state/` | 현재 상태 JSON |
| `.harness/audit/` | 변경·승인 감사 JSONL |
| `.harness/work/` | 프롬프트·컨텍스트·실행로그 |

## 구현 기준

- Java 21
- Spring Boot `CommandLineRunner`
- 의존성 없는 코어 도메인 우선
- 외부 프로세스는 `ProcessBuilder`와 Timeout으로 실행
- 파일 저장은 임시파일 작성 후 교체
- 단계 변경은 `GateService`를 통해서만 수행

## 검증

```bash
./gradlew test
./gradlew check
sh scripts/offline-smoke-test.sh
```
