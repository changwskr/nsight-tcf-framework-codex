<p align="center">
  <img src="harness_banner.png" alt="Harness Banner" width="600">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-1.2.0-brightgreen.svg" alt="Version">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License"></a>
  <img src="https://img.shields.io/badge/Patterns-6_Architectures-orange.svg" alt="6 Architecture Patterns">
  <img src="https://img.shields.io/badge/Mode-Agent_Teams-green.svg" alt="Agent Teams">
</p>

# Harness Service World — Team-Architecture Factory (claude-independent)

한국어 문서 포함. 이 복제본은 `claude` CLI나 `.claude-plugin` 매니페스트에 의존하지 않도록 정리되었습니다.

Harness는 에이전트 팀 설계와 스킬 작성을 위한 패턴과 템플릿을 제공합니다. 이 저장소는 원본 `harness-service`의 문서와 스킬 레퍼런스를 그대로 포함하되, Claude 전용 설치 지침 및 플러그인 매니페스트는 제거했습니다.

## 주요 내용

- `skills/harness/` — 메인 `SKILL.md`와 레퍼런스 템플릿 (오케스트레이터·패턴·QA 가이드 등)
- `docs/quickstart.md` — Claude 없이 로컬에서 하네스 스킬/에이전트를 시험해볼 수 있는 수동 설치 가이드
- README는 Claude 전용 배지·설치 방법을 제거한 버전입니다

## 목적

이 복제본은 다음을 위해 적합합니다:

- Claude CLI가 없는 환경에서 하네스 스킬·템플릿을 읽고 편집하려는 경우
- 조직 내부에서 수동으로 스킬을 배포하거나 테스트하려는 경우
- 다른 에이전트 런타임(대체 LLM/플랫폼)으로 포팅하기 전 레퍼런스를 확보하려는 경우

## 수동 설치(예시)

1. 이 프로젝트의 `skills/harness` 디렉터리를 로컬 사용자 디렉터리의 스킬 저장소로 복사합니다 (대체 런타임에 맞게 경로를 조정하세요):

```powershell
# Windows PowerShell 예시
Copy-Item -Path .\skills\harness -Destination $env:USERPROFILE\.claude\skills\harness -Recurse -Force
```

또는 프로젝트 내부에서 테스트하려면 로컬 `.claude/skills/harness` 경로로 복사하세요:

```powershell
Copy-Item -Path .\skills\harness -Destination .\.claude\skills\harness -Recurse -Force
```

2. 에이전트 정의는 수동으로 `.claude/agents/`에 작성합니다. 기본적으로 다음과 같은 최소 에이전트를 만들어두면 테스트에 유용합니다:

- `analyst.md`, `builder.md`, `qa.md` — 각 파일에 역할·입출력 규약을 간단히 기술

3. 생성된 디렉터리를 확인합니다:

```powershell
Get-ChildItem -Path .\.claude\skills\harness
Get-ChildItem -Path .\.claude\agents
```

## Quickstart (claude 없이 수동 테스트)

- `docs/quickstart.md`에 수동 설치 및 간이 실행 절차를 정리했습니다. 로컬에서 에이전트-스킬 워크플로우를 시뮬레이트하려면 해당 문서를 참조하세요.

## 참고

이 저장소는 원본 `harness-service`의 문서·레퍼런스를 그대로 보존합니다. Claude 전용 매니페스트(`.claude-plugin` 등)와 CLI 설치 지침은 의도적으로 제외되어 있습니다.

라이선스: Apache 2.0
