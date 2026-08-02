# TCF Harness World

TCF Harness World는 Codex에서 에이전트 팀과 프로젝트 스킬을 설계하고 검증하기 위한 하네스 패키지입니다. 프로젝트 지침은 `AGENTS.md`, 재사용 워크플로우는 `SKILL.md`, 역할별 책임은 `agents/`에서 관리합니다.

## 구성

- `AGENTS.md`: 이 패키지 범위의 Codex 작업 규칙
- `agents/`: Analyst, Builder, QA 역할 계약
- `skills/harness/SKILL.md`: 하네스 설계 메타 스킬
- `skills/harness/orchestrator-sample/`: Analyst → Builder → QA 샘플
- `docs/quickstart.md`: 프로젝트 로컬 사용과 선택적 설치
- `docs/migration-from-claude.md`: 원본에서 바뀐 실행 계약
- `scripts/verify-codex-harness.ps1`: Windows 검증 진입점
- `scripts/verify-codex-harness.sh`: POSIX 검증 진입점

## 처리 흐름

```text
사용자 요청
  → Analyst: 요구사항·제약·완료 기준
  → Builder: 승인된 분석을 구현 산출물로 변환
  → QA: 요구사항 추적성과 검증 증거 확인
  → 통과 또는 제한된 수정 요청
```

역할 파일은 프롬프트 계약입니다. 실제 작업 배정과 결과 회수는 Codex의 협업 도구로 수행하며, 역할 간 전달은 명시적인 작업 메시지와 파일 산출물을 사용합니다.

## 빠른 확인

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\tests\test-verifier.ps1 -Mode All
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\scripts\verify-codex-harness.ps1
```

오프라인 샘플은 외부 모델 호출 없이 역할 간 산출물 전달을 재현합니다.

```powershell
.\tcf-harness-world\skills\harness\orchestrator-sample\scripts\run-simulation.ps1
```

자세한 사용법은 [빠른 시작](docs/quickstart.md), 전환 경계는 [마이그레이션 설명](docs/migration-from-claude.md)을 참고합니다.

라이선스: Apache 2.0
