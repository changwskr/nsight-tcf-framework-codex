---
id: om-admin
title: OM 운영관리
section: modules
order: 11
---

# OM 운영관리

**OM Admin**은 tcf-om(8097) API를 tcf-ui Relay로 호출하는 운영 포털입니다.

## 접속

- bootRun: `http://localhost:8099/om/admin/login.html`
- 테스트 계정 예: `admin01` / `nsight01!` (환경에 따라 다름)

## 주요 기능

- Service Catalog · 거래통제
- Timeout·권한·메뉴
- 런타임 진단·배포·캐시
- 대시보드 (tcf-batch 연동)

## 사전 조건

- `tcf-om:bootRun` (8097)
- `tcf-ui:bootRun` (8099)

SSO/JWT 연동 시 `tcf-jwt`(8110) 추가 기동이 필요할 수 있습니다.

## 관련

- `zguide/tcf-om-개발가이드.md`
- `znsight-man/83-OM-런타임-진단-활용가이드.md`
