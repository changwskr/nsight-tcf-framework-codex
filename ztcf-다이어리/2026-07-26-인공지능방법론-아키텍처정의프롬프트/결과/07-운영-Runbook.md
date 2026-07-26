# 운영·Runbook

근거: A12 — **OM Gap Explicit**

## 관측

| 항목 | 내용 |
| --- | --- |
| Health | Actuator |
| 로그 | 거래로그 · 감사로그 |
| OM | Catalog / Timeout — 실등록 Gap |
| APM | 있으면 보강 · 없으면 미확정 |

## 추적 키

`guid` · `traceId` · `serviceId` · `userId`

## Runbook (최소)

1. 증상 확인  
2. 추적 키로 거래/감사 로그 조회  
3. OM Timeout·Catalog 여부 확인  
4. 조치 / 롤백 / escalation  

운영 반영: **AA 승인 후** (A16)
