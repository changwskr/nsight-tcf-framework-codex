---
name: harness-sample
description: "샘플 하네스 스킬 — 도메인 문장을 받아 간단한 에이전트 팀을 생성합니다."
---

# Harness Sample Skill

이 스킬은 Quickstart 시연용으로 생성된 샘플 스킬입니다. 도메인 문장을 입력받아 `.claude/agents/`에 정의된 에이전트를 호출하고, `_workspace/`에 산출물을 생성합니다.

Usage:

1. 도메인 문장 입력
2. `analyst` 에이전트가 분석 결과를 `_workspace/{workId}/analysis.md`로 출력

Example trigger: `build a harness for a fintech risk-assessment team`
