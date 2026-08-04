# Quickstart — Local Manual Setup (claude-independent)

이 문서는 `claude` CLI가 없는 환경에서 `harness-service`의 스킬과 템플릿을 로컬에서 읽고 테스트하는 간이 가이드를 제공합니다. 실제 플러그인 설치 없이도 스킬을 편집하고, 에이전트 정의를 수동으로 만들어 오케스트레이터 실행을 시뮬레이션할 수 있습니다.

Prerequisites:

- 로컬 터미널 (PowerShell, bash 등)
- 편집 가능한 텍스트 에디터

Steps:

1. 복사: `skills/harness` 디렉터리를 로컬 `.claude/skills/harness`로 복사합니다.

```powershell
Copy-Item -Path .\skills\harness -Destination .\.claude\skills\harness -Recurse -Force
```

2. 에이전트 정의 생성: `.claude/agents/`를 만들고 `analyst.md`, `builder.md`, `qa.md` 최소 3개 파일을 생성합니다. (예시는 `harness-service`의 `skills/harness/references`를 참고)

3. 오케스트레이터 시뮬레이션: 수동으로 `._workspace/`를 만들고, `analyst`가 생성할 파일을 흉내낸 뒤 `builder`→`qa` 순으로 파일을 생성하면서 워크플로우를 시뮬레이션합니다.

4. 확인: `_workspace/`와 `.claude/skills/harness`의 내용을 검토해 오케스트레이터 템플릿과 일치하는지 확인합니다.

Notes:

- 이 방식은 실제 Agent Teams 실행을 대체하지 않습니다. 대신 스킬·에이전트 정의를 오프라인으로 편집·검토하고, 다른 LLM 런타임으로 포팅할 때 유용합니다.
