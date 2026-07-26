---
id: relay-failure
title: Relay 실패 대응
section: errors
order: 31
---

# Relay 실패 대응

Relay 호출이 실패할 때 점검 순서입니다.

## 1. 대상 서버 기동

| 증상 | 확인 |
|------|------|
| Connection refused | 업무 WAS bootRun 여부, 포트 |
| 404 | context-path (`/sv`, `/om`) |
| 502/504 | 타임아웃·방화벽 |

## 2. URL·업무코드

- Relay: `/api/relay/sv/online` → `sv-service` 8086
- OM: `/api/relay/om/online` → `tcf-om` 8097
- OC: `/api/oc/env/...` → `tcf-oc` 8094

## 3. serviceId·Catalog

- `header.serviceId` 미등록 → OM Catalog 등록 필요
- 거래통제·권한 오류 → OM Admin에서 정책 확인

## 4. HELP 오류 상세

`targetUrl`, `httpStatus`, `traceId`를 확인하고 WAS 로그와 대조합니다.

## 5. WAR 배포 모드

bootRun은 루트 context, ztomcat은 `/ui`, `/sv` 등 nested context — URL 접두사 불일치가 흔한 원인입니다.

## 관련

- [로컬 기동·포트](?doc=local-ports)
- [오류 응답 구조](?doc=error-response)
