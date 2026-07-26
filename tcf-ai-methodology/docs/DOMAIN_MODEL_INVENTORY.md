# NSIGHT 업무모델 인벤토리 (코드 분석 기반)

생성일: 2026-07-25 · 총 **41**건 · Model Studio **v0.2.0**

출처: `*-service` / `tcf-om` Handler의 ServiceId + `schema.sql` 컬럼.  
시드 파일: `src/main/resources/data/models-seed.json`

## 계층 안내

```text
businessCode (업무 WAR)
  └── domainCode (Handler 단위)
        └── ServiceId (거래)
```

| 용어 | 설명 |
|------|------|
| businessCode | SV, IC, EB, EP, OM … — Gradle 모듈·contextPath |
| domainCode | Customer, User … — UpperCamelCase, Handler/클래스명 기준 |
| domainName | 한글 표시명 (문서·UI) |
| ServiceId | `{BC}.{Domain}.{action}` |

한 업무코드 아래에 도메인이 여러 개 있습니다.  
동일 문자열 `Customer`라도 `SV`와 `IC`에서는 서로 다른 도메인입니다.

## 업무코드별 요약

| BC | 모듈 | 건수 | 대표 ServiceId |
|----|------|------|----------------|
| SV | sv-service | 3 | `SV.Customer.selectSummary`, `SV.Sample.inquiry`, `SV.Integration.icSample` |
| IC | ic-service | 2 | `IC.Customer.inquiry`, `IC.Sample.inquiry` |
| EB | eb-service | 6 | `EB.User.inquiry`, `EB.User.create`, `EB.Event.inquiry` |
| EP | ep-service | 3 | `EP.UserEvent.inquiry`, `EP.UserEvent.receive`, `EP.Sample.inquiry` |
| PC | pc-service | 1 | `PC.Sample.inquiry` |
| MS | ms-service | 1 | `MS.Sample.inquiry` |
| PD | pd-service | 1 | `PD.Sample.inquiry` |
| SS | ss-service | 1 | `SS.Sample.inquiry` |
| MG | mg-service | 1 | `MG.Sample.inquiry` |
| OM | tcf-om | 22 | `OM.User.*`, `OM.Menu.inquiry`, `OM.ServiceCatalog.inquiry` … |

## 전체 ServiceId

| ServiceId | 처리 | 테이블 | 화면ID |
|-----------|------|--------|--------|
| `SV.Customer.selectSummary` | SELECT_ONE | `SV_CUSTOMER` | SV-CUS-0001 |
| `SV.Sample.inquiry` | SELECT_LIST | `SV_SAMPLE` | SV-SMP-0001 |
| `SV.Integration.icSample` | SELECT_ONE | `SV_INTEGRATION_LOG` | SV-INT-0001 |
| `IC.Customer.inquiry` | SELECT_LIST | `IC_CUSTOMER` | IC-CUS-0001 |
| `IC.Sample.inquiry` | SELECT_LIST | `IC_SAMPLE` | IC-SMP-0001 |
| `EB.User.inquiry` | SELECT_LIST | `EB_USER` | EB-USR-0001 |
| `EB.User.create` | INSERT | `EB_USER` | EB-USR-0002 |
| `EB.Event.inquiry` | SELECT_LIST | `EB_EVENT` | EB-EVT-0001 |
| `EB.Batch.inquiry` | SELECT_ONE | `EB_EVENT` | EB-BAT-0001 |
| `EB.SystemTx.inquiry` | SELECT_LIST | `EB_SYSTEM_TX` | EB-STX-0001 |
| `EB.Sample.inquiry` | SELECT_LIST | `EB_SAMPLE` | EB-SMP-0001 |
| `EP.UserEvent.inquiry` | SELECT_LIST | `EP_USER_EVENT` | EP-UEV-0001 |
| `EP.UserEvent.receive` | INSERT | `EP_USER_EVENT` | EP-UEV-0002 |
| `EP.Sample.inquiry` | SELECT_LIST | `EP_SAMPLE` | EP-SMP-0001 |
| `PC.Sample.inquiry` | SELECT_LIST | `PC_SAMPLE` | PC-SMP-0001 |
| `MS.Sample.inquiry` | SELECT_LIST | `MS_SAMPLE` | MS-SMP-0001 |
| `PD.Sample.inquiry` | SELECT_LIST | `PD_SAMPLE` | PD-SMP-0001 |
| `SS.Sample.inquiry` | SELECT_LIST | `SS_SAMPLE` | SS-SMP-0001 |
| `MG.Sample.inquiry` | SELECT_LIST | `MG_SAMPLE` | MG-SMP-0001 |
| `OM.User.inquiry` | SELECT_LIST | `OM_USER` | OM-USR-0001 |
| `OM.User.detail` | SELECT_ONE | `OM_USER` | OM-USR-0002 |
| `OM.User.save` | INSERT | `OM_USER` | OM-USR-0003 |
| `OM.User.update` | UPDATE | `OM_USER` | OM-USR-0004 |
| `OM.User.delete` | DELETE | `OM_USER` | OM-USR-0005 |
| `OM.Menu.inquiry` | SELECT_LIST | `OM_MENU` | OM-MNU-0001 |
| `OM.AuthGroup.inquiry` | SELECT_LIST | `OM_AUTH_GROUP` | OM-AGR-0001 |
| `OM.ServiceCatalog.inquiry` | SELECT_LIST | `OM_SERVICE_CATALOG` | OM-SVC-0001 |
| `OM.CommonCode.inquiry` | SELECT_LIST | `OM_COMMON_CODE` | OM-COD-0001 |
| `OM.ErrorCode.inquiry` | SELECT_LIST | `OM_ERROR_CODE` | OM-ERR-0001 |
| `OM.TransactionLog.inquiry` | SELECT_LIST | `TCF_TX_LOG` | OM-TXL-0001 |
| `OM.AuditLog.inquiry` | SELECT_LIST | `OM_AUDIT_LOG` | OM-AUD-0001 |
| `OM.Batch.inquiry` | SELECT_LIST | `OM_BATCH_JOB` | OM-BAT-0001 |
| `OM.Session.inquiry` | SELECT_LIST | `SPRING_SESSION` | OM-SES-0001 |
| `OM.Auth.login` | INSERT | `OM_USER` | OM-ATH-0001 |
| `OM.Auth.session` | SELECT_ONE | `SPRING_SESSION` | OM-ATH-0002 |
| `OM.Dashboard.inquiry` | SELECT_LIST | `OM_AP_STATUS` | OM-DSH-0001 |
| `OM.SystemConfig.inquiry` | SELECT_LIST | `OM_SYSTEM_CONFIG` | OM-CFG-0001 |
| `OM.FunctionAuth.inquiry` | SELECT_LIST | `OM_FUNCTION_AUTH` | OM-FNA-0001 |
| `OM.HealthCheck.inquiry` | SELECT_LIST | `OM_AP_STATUS` | OM-HLT-0001 |
| `OM.Cache.inquiry` | SELECT_LIST | `OM_CACHE_STATUS` | OM-CCH-0001 |
| `OM.Sample.inquiry` | SELECT_LIST | `OM_SAMPLE` | OM-SMP-0001 |

## 재생성·DB 반영

```bash
# 시드 JSON 재생성
node tcf-ai-methodology/generate-domain-models.js

# 앱 기동 후 DB 교체 적재
curl -X POST http://127.0.0.1:8787/api/models/reseed
```

PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8787/api/models/reseed
```

## 관련 문서

- [README.md](README.md) — 도구 사용
- [SOURCE_ALIGNMENT.md](SOURCE_ALIGNMENT.md) — 생성 템플릿·소스 계약
- [../README.md](../README.md) — 모듈 개요
