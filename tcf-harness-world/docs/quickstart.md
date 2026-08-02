# TCF Harness World 빠른 시작

## 프로젝트 로컬 사용

저장소 루트에서 Codex를 시작하면 루트 지침과 `tcf-harness-world/AGENTS.md`가 함께 적용됩니다. 하네스 작업을 요청할 때는 `tcf-harness-world/skills/harness/SKILL.md`를 사용하고, 역할 계약은 `tcf-harness-world/agents/`에서 읽습니다.

1. 구조를 검증합니다.

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\scripts\verify-codex-harness.ps1
   ```

2. 샘플 흐름을 실행합니다.

   ```powershell
   .\tcf-harness-world\skills\harness\orchestrator-sample\scripts\run-simulation.ps1
   ```

3. `_workspace/` 산출물과 `_runs/orchestrator-simulation.log`의 QA 결과를 확인합니다.

## 재사용 가능한 스킬로 설치

다른 프로젝트에서 사용하려면 `skills/harness` 디렉터리를 Codex 사용자 스킬 디렉터리 아래의 `harness`로 복사합니다. 사용자 홈을 자동으로 변경하지 않으므로 설치 대상은 직접 선택해야 합니다.

```powershell
$destination = Join-Path $env:CODEX_HOME 'skills\harness'
Copy-Item -LiteralPath .\tcf-harness-world\skills\harness -Destination $destination -Recurse
```

복사 전에 동일 이름의 스킬이 있는지 확인하고 기존 사용자 파일을 덮어쓰지 마십시오.

## 실제 멀티에이전트 실행

- 독립적이고 경계가 명확한 작업만 `spawn_agent`로 배정합니다.
- 진행 중인 에이전트에는 `send_message`, 완료된 에이전트의 후속 작업에는 `followup_task`를 사용합니다.
- `wait_agent`로 결과를 받은 후 QA 계약에 따라 검증합니다.
- 병렬 실행은 사용자 또는 상위 지침이 허용한 경우에만 사용합니다.
