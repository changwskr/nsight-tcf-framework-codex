---
id: tcf-ui-overview
title: TCF UI 개요
section: start
order: 1
---

# TCF UI 개요

**TCF UI**(`tcf-ui`, 포트 **8099**)는 WebTopSuite 없이 브라우저에서 표준 HTTP/JSON 전문을 작성·전송·응답을 확인하는 **테스트·Relay UI**입니다.

## 역할

| 역할 | 설명 |
|------|------|
| Relay | 브라우저 → 업무 WAS / tcf-om / tcf-oc API 전달 |
| 테스트 화면 | 업무코드별 단일·다중 거래 JSON 편집 |
| OC·ENV | 용량 산정·환경설정 점검 포털 |
| OM Admin | 운영관리 화면 호스팅 |

## 배포 모드

| 모드 | URL 예시 | 비고 |
|------|----------|------|
| bootRun | `http://localhost:8099/index.html` | 루트 context `/` |
| Tomcat WAR | `http://host:port/ui/index.html` | context `/ui`, `ui-context.js` 자동 보정 |

## 주요 진입점

- [업무 모듈](/index.html#moduleSection) — SV, EB, IC 등
- [OC 포털](/oc/index.html) — 용량·ENV
- [HELP](/help.html) — 이 문서 모음
- [OM Admin](/om/admin/login.html) — 운영관리 (tcf-om 8097 Relay)

## 기동

```bash
gradle :tcf-ui:bootRun
```

테스트 대상 업무 WAS(예: `sv-service` 8086)가 **먼저** 기동되어 있어야 Relay가 성공합니다.

## 관련 문서

- 저장소 `tcf-ui/README.md`
- [Relay 사용법](?doc=relay-usage)
- [로컬 기동·포트](?doc=local-ports)
