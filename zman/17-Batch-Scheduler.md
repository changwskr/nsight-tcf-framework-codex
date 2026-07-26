# 17장. Batch / Scheduler 구조 — 설명

## 설계서 절 목차

17.1~17.2 개요 · 17.3~17.4 Scheduler vs Batch · 17.5~17.6 Job · 17.7~17.9 실행·Lock · 17.10~17.22 OM·이력·운영

---

## 핵심 결론

| | 역할 |
|---|------|
| **Scheduler** | **언제** |
| **Batch** | **무엇을** |
| **OM** | 등록·실행·중지·이력·재처리 |

---

## 전체 구조 (17.3)

```
운영자 → tcf-ui (OM.Batch.*)
→ OM_BATCH_JOB / OM_BATCH_SCHEDULE
→ tcf-batch (Launcher, Lock, Executor)
→ DAO → OMDB/LOGDB/SESSIONDB
→ OM Dashboard
```

## tcf-batch Job (17.6)

| Job | 적재 테이블 |
|-----|-------------|
| BAT-BATCH-001 | OM_AP_STATUS |
| BAT-BATCH-002 | OM_DB_STATUS |
| BAT-BATCH-003 | OM_SESSION_STATUS |
| BAT-BATCH-004 | OM_DEPLOY_STATUS |

## Batch 대상 확장 (17.5)

- TX Log 아카이브  
- UNKNOWN 재처리  
- 파일 보관기간 정리  
- ADW 집계  

## 실행 방식 (17.7)

| | 설명 |
|---|------|
| Cron | OM_BATCH_SCHEDULE |
| 수동 | OM Admin — **감사로그 필수** |
| 재실행 | 실패 Job 재처리 |

## 중복 실행 방지 (17.9)

```
Trigger → OM_BATCH_LOCK → Lock 획득 → Job → Lock 해제
```

다중 인스턴스 **DB Lock** 필수.

## Batch vs 온라인

- Handler 경유 ❌  
- DAO/Mapper **동일 표준**, Query Timeout 적용  

## Gap·보완

현재: `@Scheduled` + 수동 API  
목표: DB Job/스케줄, 이력, Lock, 재처리 **운영 완성** (24장 P2)

## 코드베이스

- `tcf-batch/` — 8098  
- `OmBatchHandler` — tcf-om  
- `zdoc/배치관리.md`, `zdoc/스케줄러.md`

## 이전 · 다음

← [16장 Cache](./16-Cache구조.md) · [18장 파일](./18-파일업다운로드.md) →
