---
name: tcf-harness-orchestrator-sample
description: "tcf-harness-world의 Analyst → Builder → QA 산출물 전달과 검증 흐름을 학습하거나 오프라인으로 점검할 때 사용합니다."
---

# TCF Harness Orchestrator Sample

## 흐름

1. `agents/analyst.md` 계약으로 `_workspace/analysis-summary.md`를 만든다.
2. `agents/builder.md` 계약으로 분석을 읽고 `_workspace/poc-plan.md`를 만든다.
3. `agents/qa.md` 계약으로 두 파일이 존재하고 비어 있지 않은지 확인한다.
4. 결과를 `_runs/orchestrator-simulation.log`에 QA PASS 또는 QA FAIL로 기록한다.

## 오프라인 실행

```powershell
cd tcf-harness-world/skills/harness/orchestrator-sample/scripts
.\run-simulation.ps1
```

이 스크립트는 실제 에이전트를 생성하지 않는다. 실제 Codex 실행은 [도구 매핑](../references/codex-tool-mapping.md)에 따라 권한이 허용된 경우에만 수행한다.
