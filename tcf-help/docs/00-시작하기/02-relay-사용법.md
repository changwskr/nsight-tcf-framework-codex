---
id: relay-usage
title: Relay 사용법
section: start
order: 2
---

# Relay 사용법

TCF UI는 업무 서버에 **직접 HTTP POST**를 중계합니다. 브라우저는 CORS·인증 이슈를 피하기 위해 UI 서버의 Relay API를 호출합니다.

## 엔드포인트

| 용도 | 메서드 | 경로 |
|------|--------|------|
| 업무 온라인 | POST | `/api/relay/{업무코드}/online` |
| OM | POST | `/api/relay/om/online` |
| OC ENV | GET/POST | `/api/oc/env/...` |
| OC CAP | POST | `/api/oc/capacity/...` |

`{업무코드}` 예: `sv`, `eb`, `ic`, `om` (소문자)

## 요청 헤더 (공통)

- `X-GUID` — 요청 추적 ID (UUID)
- `X-USER-ID` — 테스트 사용자 ID
- `Content-Type: application/json`

## 응답 구조

표준 응답은 `body` + `error`(또는 `result`) 영역을 가집니다.

```json
{
  "error": { "resultCode": "SUCCESS" },
  "body": { "response": { } }
}
```

실패 시 **HELP** 버튼으로 [오류 상세](/help.html#errors)에 기록됩니다.

## Gateway 경유 (tcf-uj)

운영 유사 경로는 `tcf-uj`(8102) + `tcf-gateway`(8100)를 사용합니다. 로컬 단순 테스트는 **tcf-ui Relay**가 일반적입니다.

## 관련

- [오류 응답 구조](?doc=error-response)
- [Relay 실패 대응](?doc=relay-failure)
