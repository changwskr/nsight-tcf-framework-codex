---
name: orchestrator-sample
description: "샘플 오케스트레이터 — Analyst → Builder → QA 순으로 작업을 배포하고 결과를 통합합니다. 이 버전은 로컬 시뮬레이션 스크립트를 포함합니다."
---

# Orchestrator Sample (Runnable Simulation)

이 오케스트레이터는 간단한 워크플로우(Analyst → Builder → QA)를 시뮬레이션하는 예시입니다. 실제 `claude` 실행 없이 스크립트로 동작을 흉내낼 수 있도록 `scripts/run-simulation.*`와 샘플 로그를 제공합니다.

구성:

- `scripts/run-simulation.bat` — Windows용 시뮬레이션 스크립트
- `scripts/run-simulation.sh` — POSIX(shell)용 시뮬레이션 스크립트
- `../../../../_runs/orchestrator_simulation.log` — 실행 결과 샘플 로그

사용 방법 (Windows PowerShell):

1. `cd harness-service-world/skills/harness/orchestrator-sample/scripts`
2. `./run-simulation.bat`
3. `type ..\..\..\..\_runs\orchestrator_simulation.log` 로 결과 확인

POSIX 사용 예 (bash):

```bash
cd harness-service-world/skills/harness/orchestrator-sample/scripts
./run-simulation.sh
cat ../../../../_runs/orchestrator_simulation.log
```

설명: 스크립트는 간단한 텍스트 로그를 생성하여 Analyst가 분석을 생성하고 Builder가 산출물을 만들며 QA가 검증하는 순서를 보여줍니다. 이는 교육·문서·로컬 디버그용 시뮬레이션입니다.
