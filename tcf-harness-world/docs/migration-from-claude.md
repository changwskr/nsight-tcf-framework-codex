# Claude 기반 원본에서 Codex로 전환

이 문서는 전환 이력을 설명하기 때문에 과거 제품명과 경로를 제한적으로 기록합니다. 실행 문서와 스킬은 아래의 Codex 계약만 사용합니다.

| 원본 요소 | Codex 대응 |
|---|---|
| `CLAUDE.md` | `AGENTS.md` |
| `.claude/agents/` | `agents/` 역할 계약 |
| `.claude/skills/` | `skills/`의 Codex `SKILL.md` |
| Claude Agent Teams | Codex 협업 도구 |
| `.claude-plugin` | 제거 |

변환은 단순 문자열 치환이 아닙니다. 원본의 역할 분리와 산출물 전달 원칙을 유지하되, 실제 배정은 `spawn_agent`, 메시지는 `send_message` 또는 `followup_task`, 결과 대기는 `wait_agent`로 표현합니다.

이 패키지는 Claude CLI나 Anthropic API를 설치·호출하지 않습니다. 향후 원본 변경을 가져올 때에도 실행 파일을 직접 복사하지 않고 역할, 입력, 출력, 실패 계약을 다시 검토합니다.
