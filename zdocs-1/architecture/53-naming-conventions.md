# 53. 명명규칙 정의서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 53 |
| 제목 | NSIGHT TCF Naming Conventions |
| 상위 문서 | [architecture.md](architecture.md) |
| 구현 네이밍 | [06-naming.md](06-naming.md) — Java 계층·SQL ID 상세 |
| 관련 문서 | [01-application-layer.md](01-application-layer.md), [02-junmun.md](02-junmun.md), [05-exception.md](05-exception.md), [19-tcf-table.md](19-tcf-table.md), [39-header-transaction-control.md](39-header-transaction-control.md), [51-api-gateway.md](51-api-gateway.md) |
| 상세 매뉴얼 | [znsight-man/명명규칙-00-목차.md](../../znsight-man/명명규칙-00-목차.md) (21개 주제) |

---

## 1. 목적

NSIGHT TCF의 명명규칙은 **이름 하나로 업무 경계·배포 단위·실행 경로·DB·로그·오류를 추적**하기 위한 표준이다.

| 목표 | 설명 |
|------|------|
| 추적성 | 로그·거래로그만 보고 업무·거래·Handler·SQL 위치 파악 |
| 일관성 | 업무코드 → Context → WAR → Package → serviceId가 한 선으로 연결 |
| 확장성 | 신규 업무·거래·모듈도 동일 패턴으로 추가 |
| 운영 연계 | OM Service Catalog·거래통제·오류코드 마스터와 정합 |

### 1.1 이름 추적 체인

```text
업무코드 (SV, OM, …)
  ↓
Context Path (/sv) · WAR (sv.war) · Gradle Module (sv-service)
  ↓
Java Package (com.nh.nsight.marketing.sv)
  ↓
serviceId (SV.Customer.selectSummary / OM.User.inquiry)
  ↓
Handler → Facade → Service → Rule → DAO → Mapper
  ↓
Mapper XML · SQL ID
  ↓
DB Table/Column · transactionCode · errorCode · 거래로그/감사로그
```

### 1.2 식별자 역할 분리

| 식별자 | 역할 | 예 |
|--------|------|-----|
| `businessCode` | 업무·배포·URL·Gateway 라우팅 | `SV`, `OM` |
| `serviceId` | TCF Dispatcher → Handler 실행 키 | `SV.Sample.inquiry`, `OM.User.save` |
| `transactionCode` | 거래로그·감사·거래통제·화면 설계 ID | `SV-INQ-0001`, `OM-ADM-0001` |

> URL(`/sv/online`)은 **진입 경로**이고, 실제 실행 대상은 **Header.serviceId**이다.  
> 상세: [NSIGHT-FINAL-ARCHITECTURE-DECISION.md](NSIGHT-FINAL-ARCHITECTURE-DECISION.md) §5

---

## 2. 최상위 원칙

| No | 원칙 | 기준 |
|----|------|------|
| 1 | 업무코드 우선 | 모든 이름은 `SV`, `OM` 등 업무코드에서 출발 |
| 2 | 하나의 이름 체계 | 동일 업무를 여러 이름으로 부르지 않음 |
| 3 | 실행 식별자 분리 | URL ≠ serviceId |
| 4 | 계층 책임 명확화 | Handler/Facade/Service/Rule/DAO/Mapper 접미사 고정 |
| 5 | 공통·업무 분리 | `tcf-*` vs `{code}-service` |
| 6 | 표기 통일 | Java PascalCase, DB UPPER_SNAKE, JSON lowerCamelCase |
| 7 | 운영 추적 | 로그·SQL ID·오류코드에서 serviceId 역추적 가능 |
| 8 | 약어 통제 | `Svc`, `Mgr`, `upd01` 등 의미 불명 축약 금지 |
| 9 | 중복 의미 금지 | 표준 동사·Prefix만 사용 |
| 10 | 확장 가능 | 17업무·신규 플랫폼 모듈도 동일 규칙 적용 |

---

## 3. 표기 규칙 요약

| 대상 | 규칙 | 예 |
|------|------|-----|
| 업무코드 | 대문자 2자리 | `SV`, `OM`, `MG` |
| Context Path | `/` + 소문자 업무코드 | `/sv`, `/om` |
| WAR | 소문자 + `.war` | `sv.war`, `om.war` |
| Gradle 업무 모듈 | 소문자 + `-service` | `sv-service` |
| 플랫폼 모듈 | `tcf-{역할}` | `tcf-om`, `tcf-gateway`, `tcf-eai` |
| Java Package | `com.nh.nsight.marketing.{bc}` | `.sv`, `.om` |
| Java Class | PascalCase + 역할 접미사 | `SvCustomerFacade` |
| Method/Field | lowerCamelCase | `selectSummary`, `userId` |
| JSON Header | lowerCamelCase | `serviceId`, `transactionCode` |
| DB Table/Column | UPPER_SNAKE_CASE | `OM_USER`, `SERVICE_ID` |
| HTML/JS | kebab-case | `transaction-log.html`, `om-admin.js` |
| 상수 | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |

---

## 4. 업무코드 · Context · WAR · Package

### 4.1 변환 규칙

업무코드 `SV`가 정해지면 아래가 **자동 결정**된다.

| 항목 | 규칙 | SV 예 |
|------|------|-------|
| Context | `/{bc.lower}` | `/sv` |
| WAR | `{bc.lower}.war` | `sv.war` |
| Gradle Module | `{bc.lower}-service` | `sv-service` |
| Java Prefix | PascalCase | `Sv` |
| Package | `com.nh.nsight.marketing.{bc.lower}` | `.sv` |
| serviceId Prefix | 대문자 그대로 | `SV.` |

**예외:** 운영관리는 `tcf-om` → `om.war`, Package `.om`, Prefix `Om`.

### 4.2 업무코드 표준 (17 + OM)

| 코드 | 업무명 | Context | Module (현재) | 비고 |
|------|--------|---------|---------------|------|
| CC | Common | /cc | (확장 예정) | |
| IC | Integration Customer | /ic | ic-service | 구현 |
| PC | Private Customer | /pc | pc-service | 구현 |
| BC | Business Customer | /bc | (확장 예정) | |
| MS | Mini Single View | /ms | ms-service | 구현 |
| SV | Single View | /sv | sv-service | 구현 |
| PD | Product | /pd | pd-service | 구현 |
| CM | Campaign | /cm | (확장 예정) | |
| EB | EBM | /eb | eb-service | 구현 |
| EP | Event Processing | /ep | ep-service | 구현 |
| BP | Behavior Processing | /bp | (확장 예정) | |
| BD | Behavior Data | /bd | (확장 예정) | |
| SS | Sales Support | /ss | ss-service | 구현 |
| CS | Customer Support | /cs | (확장 예정) | |
| CT | Customer Targeting | /ct | (확장 예정) | |
| MG | Message | /mg | mg-service | **Message** (Management 아님) |
| OM | Operation Management | /om | tcf-om | 플랫폼 WAR |

저장소 현재 구현: **9개 업무 WAR** + `tcf-om`. 포트·Context: [architecture.md](architecture.md) §13.

### 4.3 플랫폼 모듈 명명

| 모듈 | 역할 |
|------|------|
| `tcf-util` | Spring 비의존 유틸 |
| `tcf-core` | STF/TCF/ETF, Dispatcher |
| `tcf-web` | `/online`, WAR Bootstrap |
| `tcf-cache` | EhCache / Spring Cache |
| `tcf-eai` | 업무 간 HTTP/JSON Client |
| `tcf-om` | OM Admin + UD |
| `tcf-batch` | 대시보드 수집·배치 |
| `tcf-ui` / `tcf-uj` | 테스트 UI · Relay |
| `tcf-gateway` | API Gateway |
| `tcf-jwt` | JWT 발급·JWKS |
| `tcf-cicd` | 환경 yml·CI/CD |
| `tcf-scripts` | 빌드·실행 래퍼 |

상세: [48-multi-module-dependencies.md](48-multi-module-dependencies.md), [명명규칙-05-모듈-설계기준.md](../../znsight-man/명명규칙-05-모듈-설계기준.md)

---

## 5. Package · Java Class

### 5.1 6계층 Package

```text
com.nh.nsight.marketing.{bc}
├── entry/handler|facade|web
├── application/service|rule|scheduler
├── persistence/dao|mapper
├── client
├── config
└── support
```

플랫폼 모듈(`tcf-core` 등)은 `com.nh.nsight.tcf.{module}.support.*` — [architecture.md](architecture.md) §4.2.

### 5.2 Class 접미사

| 계층 | 패턴 | 예 |
|------|------|-----|
| Handler | `{Bc}{Domain}Handler` | `OmUserHandler`, `SvSampleHandler` |
| Facade | `{Bc}{Domain}Facade` | `OmServiceCatalogFacade` |
| Service | `{Bc}{Domain}Service` | `SvCustomerService` |
| Rule | `{Bc}{Domain}Rule` | `SvCustomerGradeRule` |
| DAO | `{Bc}{Domain}Dao` | `OmOperationDao` |
| Mapper | `{Bc}{Domain}Mapper` | `SvCustomerMapper` |
| Controller | `{Domain}{Resource?}Controller` | `OmUpdownloadFileController` |

**Handler 원칙:** 도메인(application Service)당 **1개**. 여러 거래는 `serviceIds()` + `doHandle` 분기.

구현 상세·금지 규칙: [06-naming.md](06-naming.md) §3~5.

---

## 6. serviceId

### 6.1 표준 형식

```text
{BusinessCode}.{Domain}.{action}
```

| 파트 | 규칙 | 예 |
|------|------|-----|
| BusinessCode | 대문자 2자리 | `SV`, `OM` |
| Domain | PascalCase 도메인·엔티티 | `Customer`, `ServiceCatalog`, `Auth` |
| action | lowerCamelCase 동사 | `inquiry`, `selectSummary`, `save` |

### 6.2 action 표준 (두 계층)

| 구분 | 권장 action | 사용처 | 예 |
|------|-------------|--------|-----|
| **플랫폼·OM** | 짧은 CRUD 동사 | tcf-om Handler | `inquiry`, `detail`, `save`, `update`, `delete`, `execute`, `login` |
| **업무 도메인** | 의미 있는 조회·처리 동사 | *-service Handler | `selectSummary`, `selectList`, `create`, `send`, `retry` |

코드베이스 기준 **OM은 짧은 동사**를 사용한다 (`OM.User.inquiry`, `OM.ServiceCatalog.save`).  
업무 WAR는 **도메인 의미가 드러나는 동사**를 사용한다 (`SV.Customer.selectSummary`).

신규 거래는 **해당 업무·도메인 내에서 한 스타일만** 유지한다.

### 6.3 정합성 규칙

1. `header.businessCode` = serviceId Prefix (`SV` → `SV.*`)
2. URL Context와 businessCode 일치 (`/sv` → `SV`)
3. OM Service Catalog에 등록 후 운영 반영
4. 미등록 serviceId → `E-COM-DISP-0001` (Dispatcher)
5. 운영 반영 후 serviceId 변경 **금지**

### 6.4 추적 매핑 예

```text
serviceId : OM.ErrorCode.update
  → Handler : OmErrorCodeHandler
  → Facade  : OmErrorCodeFacade.update(...)
  → Service : OmErrorCodeService.update(...)
  → DAO/Mapper/SQL : updateErrorCode
  → transactionCode : OM-ERR-0004 (카탈로그 등록)
  → errorCode : E-OM-BIZ-0002 (업무 실패 시)
```

상세: [명명규칙-07-ServiceId.md](../../znsight-man/명명규칙-07-ServiceId.md), [03-transaction.md](03-transaction.md)

---

## 7. transactionCode (거래코드)

### 7.1 형식

```text
{BC}-{TYPE}-{NNNN}
```

| 파트 | 규칙 | 예 |
|------|------|-----|
| BC | 업무코드 | `SV`, `OM` |
| TYPE | 2~4자 대문자 (거래 유형) | `INQ`, `REG`, `UPD`, `DEL`, `EXE`, `ADM`, `ERR` |
| NNNN | 4자리 순번 | `0001` |

예: `SV-INQ-0001`, `OM-ADM-0001`, `MG-SND-0001`

### 7.2 serviceId와의 관계

| 구분 | serviceId | transactionCode |
|------|-----------|-----------------|
| 목적 | Handler 실행 식별 | 거래로그·감사·통제 식별 |
| 관계 | 1 serviceId : N transactionCode 가능 | 카탈로그 대표 거래코드 1:1 권장 |

상세: [명명규칙-08-거래코드.md](../../znsight-man/명명규칙-08-거래코드.md), [17-거래코드-설계.md](../../znsight-man/17-거래코드-설계.md)

---

## 8. errorCode (오류코드)

### 8.1 형식

```text
E-{영역}-{분류}-{NNNN}
```

| 영역 | 분류 | 예 |
|------|------|-----|
| `COM` | `VALID`, `DISP`, `AUTH`, `SYS`, `IDEMP`, `BIZ` | `E-COM-VALID-0001` |
| `OM`, `SV`, `UD` … | `BIZ`, `VAL`, `DB`, `AUTH` | `E-OM-BIZ-0002` |

프레임워크 공통: `com.nh.nsight.tcf.core.support.error.ErrorCode`

상세: [05-exception.md](05-exception.md), [명명규칙-14-오류코드.md](../../znsight-man/명명규칙-14-오류코드.md)

---

## 9. DB 객체

### 9.1 Prefix

| Prefix | 영역 | 예 |
|--------|------|-----|
| `TCF_` | TCF 실행·통제·거래로그 | `TCF_TX_LOG`, `TCF_TRANSACTION_CONTROL` |
| `OM_` | 운영 마스터·이력 | `OM_USER`, `OM_SERVICE_CATALOG` |
| `TCF_GATEWAY_` | Gateway | `TCF_GATEWAY_ROUTE` |
| `TCF_JWT_` | JWT | `TCF_JWT_TOKEN` |
| `SPRING_SESSION` | 세션 JDBC | `SPRING_SESSION` |
| `UD_` | 파일 메타 | `UD_FILE_META` |
| `{BC}_` | 업무 데이터 | `SV_CUSTOMER_SUMMARY`, `CM_CAMPAIGN_MASTER` |

### 9.2 Table 명명

```text
{Prefix}_{대상}_{성격}
```

성격: `_MASTER`, `_DETAIL`, `_HISTORY`, `_LOG`, `_MAP`

MyBatis: `map-underscore-to-camel-case` — `CUSTOMER_NO` → `customerNo`

전체 테이블: [19-tcf-table.md](19-tcf-table.md), [명명규칙-13-DB-객체.md](../../znsight-man/명명규칙-13-DB-객체.md)

---

## 10. MyBatis · SQL ID

| 구분 | 규칙 | 예 |
|------|------|-----|
| XML 경로 | `mapper/{bc}/{Bc}{Domain}Mapper.xml` | `mapper/om/OmOperationMapper.xml` |
| SQL ID | lowerCamelCase, 도메인+동사 | `searchErrorCodes`, `updateErrorCode` |
| 원칙 | **Mapper method name == SQL id** | 필수 |

접두: `select*`, `search*`, `count*`, `insert*`, `update*`, `merge*`, `delete*` / `disable*`

상세: [06-naming.md](06-naming.md) §7, [26-mybatis.md](26-mybatis.md), [명명규칙-12-MyBatis-Mapper-SQL.md](../../znsight-man/명명규칙-12-MyBatis-Mapper-SQL.md)

---

## 11. Header 항목

| 항목 | JSON/Java | DB 컬럼 | 용도 |
|------|-----------|---------|------|
| `serviceId` | lowerCamelCase | `SERVICE_ID` | Dispatcher |
| `transactionCode` | lowerCamelCase | `TRANSACTION_CODE` | 거래로그·통제 |
| `businessCode` | lowerCamelCase | `BUSINESS_CODE` | Context·라우팅 |
| `userId`, `channelId`, `branchId` | lowerCamelCase | `USER_ID` … | 권한·통제 |
| `guid`, `traceId` | lowerCamelCase | `GUID`, `TRACE_ID` | 상관 추적 |

거래통제 Allow-List 7필드: [39-header-transaction-control.md](39-header-transaction-control.md), [40-header-7-transaction-control.md](40-header-7-transaction-control.md)

상세: [02-junmun.md](02-junmun.md), [명명규칙-21-Header-항목.md](../../znsight-man/명명규칙-21-Header-항목.md)

---

## 12. Gateway · Batch · Cache

### 12.1 Gateway

| 항목 | 규칙 | 예 |
|------|------|-----|
| RouteId | `GW-{ENV}-{BC}-ONLINE` | `GW-PRD-SV-ONLINE` |
| TargetGroup | `TG-{ENV}-{BC}-{NN}` | `TG-PRD-SV-01` |
| DB | `TCF_GATEWAY_ROUTE` | ENV + BUSINESS_CODE |

상세: [51-api-gateway.md](51-api-gateway.md), [명명규칙-18-Gateway-라우팅.md](../../znsight-man/명명규칙-18-Gateway-라우팅.md)

### 12.2 Batch / Scheduler

| 항목 | 규칙 | 예 |
|------|------|-----|
| Job ID | `BAT-{BC}-{DOMAIN}-{NNN}` | `BAT-BATCH-001` (tcf-batch 수집) |
| OM ServiceId | `OM.Batch.execute` | 수동 실행 |
| 실행 이력 | `OM_BATCH_HISTORY` | tcf-batch → OM 테이블 |

상세: [13-batch.md](13-batch.md), [명명규칙-19-Batch-Scheduler.md](../../znsight-man/명명규칙-19-Batch-Scheduler.md)

### 12.3 Cache

| 항목 | 규칙 | 예 |
|------|------|-----|
| Cache Name | lowerCamelCase | `commonCode`, `serviceCatalog` |
| Cache Key | `DOMAIN:TYPE:KEY` | `CC:CODE:CHANNEL_ID` |
| OM API | `OM.Cache.inquiry` / `delete` | tcf-om |

업무 데이터·PII **캐시 금지**. 상세: [12-cache.md](12-cache.md), [명명규칙-20-Cache.md](../../znsight-man/명명규칙-20-Cache.md)

---

## 13. UI · 화면 · 리소스

| 항목 | 규칙 | 예 |
|------|------|-----|
| 화면번호 | `{BC}{NNNN}` | `OM0101` |
| HTML | kebab-case | `service-catalog.html` |
| JS | kebab-case | `om-admin.js` |
| 샘플 JSON | `{bc}-{domain}-{action}.json` | `sv-sample-inquiry.json` |
| 화면 ↔ serviceId | OM 카탈로그·메뉴에 매핑 | `OM0101` → `OM.User.inquiry` |

상세: [명명규칙-15-화면번호.md](../../znsight-man/명명규칙-15-화면번호.md), [명명규칙-17-화면-ServiceId-연결.md](../../znsight-man/명명규칙-17-화면-ServiceId-연결.md)

---

## 14. DTO · Method · Field

| 구분 | 규칙 |
|------|------|
| Request/Response DTO | `{Bc}{Domain}{Purpose}Request` / `Response` |
| Body Map 키 | lowerCamelCase, Header 항목과 중복 금지 |
| Boolean | `is*` / `has*` 지양 → `enabled`, `active` |
| Collection | 복수형 (`errorCodes`, `userList`) |

상세: [명명규칙-09~11](../../znsight-man/명명규칙-09-Java-Class.md)

---

## 15. 신규 거래 체크리스트

1. 업무코드 → Context/WAR/Module/Package 확정
2. `serviceId` = `{BC}.{Domain}.{action}` 정의
3. `transactionCode` = `{BC}-{TYPE}-{NNNN}` 카탈로그 등록
4. Handler(도메인 1개) → Facade → Service → Rule → DAO → Mapper 생성
5. Mapper method = SQL ID
6. DB Table/Column Prefix·명명 검토
7. `errorCode` 사전 정의 (`E-{BC}-*`)
8. OM Service Catalog·거래통제·Timeout 정책 등록
9. 샘플 JSON·화면번호·메뉴 연결
10. Gateway Route(해당 시)·Cache 대상 여부 검토

---

## 16. 상세 매뉴얼 인덱스 (znsight-man)

| No | 주제 | 문서 |
|----|------|------|
| 1 | 총정리 | [명명규칙-01-총정리.md](../../znsight-man/명명규칙-01-총정리.md) |
| 2 | 최상위 원칙 | [명명규칙-02-최상위-원칙.md](../../znsight-man/명명규칙-02-최상위-원칙.md) |
| 3 | 업무코드·Context·WAR | [명명규칙-03-업무코드-Context-WAR-Package.md](../../znsight-man/명명규칙-03-업무코드-Context-WAR-Package.md) |
| 4 | 업무코드 표준표 | [명명규칙-04-업무코드-표준표.md](../../znsight-man/명명규칙-04-업무코드-표준표.md) |
| 5 | 모듈 설계 | [명명규칙-05-모듈-설계기준.md](../../znsight-man/명명규칙-05-모듈-설계기준.md) |
| 6 | Package | [명명규칙-06-Package.md](../../znsight-man/명명규칙-06-Package.md) |
| 7 | ServiceId | [명명규칙-07-ServiceId.md](../../znsight-man/명명규칙-07-ServiceId.md) |
| 8 | 거래코드 | [명명규칙-08-거래코드.md](../../znsight-man/명명규칙-08-거래코드.md) |
| 9~11 | Java Class/Method/DTO | [09](../../znsight-man/명명규칙-09-Java-Class.md) · [10](../../znsight-man/명명규칙-10-Java-Method-Field.md) · [11](../../znsight-man/명명규칙-11-Java-DTO.md) |
| 12 | MyBatis/SQL | [명명규칙-12-MyBatis-Mapper-SQL.md](../../znsight-man/명명규칙-12-MyBatis-Mapper-SQL.md) |
| 13 | DB 객체 | [명명규칙-13-DB-객체.md](../../znsight-man/명명규칙-13-DB-객체.md) |
| 14 | 오류코드 | [명명규칙-14-오류코드.md](../../znsight-man/명명규칙-14-오류코드.md) |
| 15~17 | 화면·로그·연결 | [15](../../znsight-man/명명규칙-15-화면번호.md) · [16](../../znsight-man/명명규칙-16-로그-감사로그.md) · [17](../../znsight-man/명명규칙-17-화면-ServiceId-연결.md) |
| 18~21 | Gateway·Batch·Cache·Header | [18](../../znsight-man/명명규칙-18-Gateway-라우팅.md) · [19](../../znsight-man/명명규칙-19-Batch-Scheduler.md) · [20](../../znsight-man/명명규칙-20-Cache.md) · [21](../../znsight-man/명명규칙-21-Header-항목.md) |

---

## 17. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-07 | 최초 작성 — znsight-man 명명규칙 21주제를 아키텍처 단일 정의서로 통합·인덱스 |

---

← [52-om-operations.md](52-om-operations.md) · [06-naming.md](06-naming.md) →
