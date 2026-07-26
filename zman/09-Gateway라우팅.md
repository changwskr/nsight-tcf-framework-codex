# 9장. Gateway 라우팅 구조 — 설명

## 설계서 절 목차

9.1~9.5 개요·흐름 · 9.6~9.8 라우팅 테이블 · 9.9~9.10 환경·업무그룹 · 9.11~9.12 세션·검증 · 9.13~9.14 알고리즘·Target · 9.15~9.17 로그·Timeout·오류 · 9.18~9.21 Apache·운영

---

## 핵심 결론

> Gateway = **businessCode → 대상 WAR Relay**. 업무 실행은 대상 WAR **TCF + serviceId**.

Gateway ≠ L4/GSLB/Apache.

---

## 전체 라우팅 (9.3)

```
사용자 → Apache → tcf-gateway
  1. Path 업무코드 (SV)
  2. JSON header.businessCode
  3. Path ↔ Header 일치
  4. JSESSIONID / Bearer JWT
  5. serviceId 형식·Prefix
  6. 거래통제 1차
  7. TCF_GATEWAY_ROUTE
  8. Target 선택 (ACTIVE/UP)
  9. HTTP/JSON Relay
 10. 응답 + TCF_GATEWAY_TX_LOG
→ /sv/online → TCF.process()
```

## URL 표준 (9.4)

| 호출 | URL | 대상 |
|------|-----|------|
| 외부/화면 | `POST /gw/SV/online` | Gateway |
| Relay | `POST /sv/online` | sv-service |
| 개발 직접 | `POST /sv/online` | bootRun 8086 |

## 라우팅 테이블 (9.6~9.8)

**TCF_GATEWAY_ROUTE**

- ENV_CODE (LOCAL/DEV/PRD)
- BUSINESS_CODE
- ROUTE_TYPE, CONTEXT_PATH, ONLINE_PATH

**TCF_GATEWAY_ROUTE_TARGET**

- TARGET_BASE_URL, WEIGHT, STATUS (ACTIVE/UP/DOWN)

## Target 선택 (9.14)

- Round Robin / Weight
- Health Check 연동
- 장애 Target 제외

## Gateway vs Dispatcher

| | Gateway | Dispatcher |
|---|---------|------------|
| Key | businessCode | serviceId |
| 위치 | Apache 뒤 | WAR 내부 STF 뒤 |

## Gateway 세션·JWT (9.11)

- **Bearer 있음:** JWT JWKS 검증 (`GatewayJwtValidator`, issuer/audience)
- **Bearer 없음:** SESSIONDB 4단계 (`GatewaySessionValidationService`)
- Cookie/Authorization **그대로 Relay**
- 정밀 검증: 업무 WAR STF

## Gateway 로그 (9.15)

`TCF_GATEWAY_TX_LOG` — Gateway 구간 GUID, Target, elapsed

## Gateway 오류 (9.17)

- Path/Header businessCode 불일치
- Route 미등록, Target DOWN
- Proxy Timeout

## Apache 역할 분리 (9.18)

Apache: SSL, VIP Proxy, Sticky  
Gateway: **업무·ServiceId 1차 통제**

## 코드베이스

- `tcf-gateway/` — bootRun 8100
- `UserSessionService`, `GatewaySessionRequestEnricher`
- `tcf-uj` — Gateway 경유 UI 8102

## 운영 체크리스트 (9.20)

- [ ] ENV별 Route 등록  
- [ ] Target Health  
- [ ] Path/Header 불일치 모니터링  
- [ ] Gateway TX Log ↔ TCF TX Log GUID 연계  

## 관련 문서

- `zdoc/세션관리.md` · [10-세션](./10-세션관리.md)

## 이전 · 다음

← [8장 Handler](./08-업무Handler개발.md) · [10장 세션](./10-세션관리.md) →
