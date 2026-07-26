# 06. 네이밍 표준

| 항목 | 내용 |
|------|------|
| 문서 번호 | 06 |
| 제목 | Naming Convention Standard (구현 네이밍) |
| 상위 문서 | [architecture.md](architecture.md) |
| **명명규칙 정의서** | [53-naming-conventions.md](53-naming-conventions.md) — 업무코드·serviceId·DB·Gateway 등 전체 표준 |
| 관련 문서 | [01-application-layer.md](01-application-layer.md), [02-junmun.md](02-junmun.md), [03-transaction.md](03-transaction.md), [05-exception.md](05-exception.md) |
| 대상 | `*-service`, `tcf-om`, `tcf-ui` 개발자 |

---

## 1. 개요

NSIGHT TCF Framework의 네이밍은 **거래 식별자(`serviceId`)를 중심**으로 계층별 클래스·패키지·SQL ID가 1:1로 추적되도록 설계한다.

| 목표 | 설명 |
|------|------|
| 일관성 | 코드만 보고 역할(Controller/Facade/Service 등)을 즉시 파악 |
| 추적성 | `serviceId` → Handler → Facade → Service → DAO → Mapper SQL까지 단방향 추적 |
| 검색성 | IDE 검색(`OM.ErrorCode.update` 등)으로 관련 파일을 빠르게 발견 |
| 확장성 | 업무코드(OM/SV/CC...)가 늘어도 동일 패턴 유지 |

---

## 2. 공통 원칙

### 2.1 표기 원칙

| 항목 | 규칙 |
|------|------|
| 패키지/경로 | 소문자 `lower_snake` 또는 `lower` (`handler`, `mapper/sv`) |
| 클래스/인터페이스 | `PascalCase` (`SvSampleService`) |
| 메서드/필드 | `camelCase` (`inquiry`, `saveUser`) |
| 상수 | `UPPER_SNAKE_CASE` (`DEFAULT_PAGE_SIZE`) |
| 약어 | 도메인 약어는 대문자 2자리 유지 (`OM`, `SV`, `UD`) |
| 접두/접미 | 역할 접미사 고정 (`Controller`, `Facade`, `Service`, `Rule`, `Dao`, `Mapper`) |

### 2.2 금지 규칙

| 금지 | 이유 |
|------|------|
| 의미 없는 `Common`, `Util2`, `EtcService` | 책임이 모호해 유지보수 어려움 |
| 역할 접미사 생략 (`OmUser` 등) | 계층 식별 불가 |
| 계층 혼합 명명 (`OmUserDaoService`) | 단일 책임 위반 |
| 한 클래스에 복수 도메인 접두 (`OmBatchUserService`) | 응집도 저하 |

---

## 3. 패키지 네이밍

### 3.1 루트 패키지

업무 모듈 표준:

```text
com.nh.nsight.marketing.{businessCodeLower}
```

예:

- `com.nh.nsight.marketing.sv`
- `com.nh.nsight.marketing.om`
- `com.nh.nsight.marketing.cc`

### 3.2 계층 패키지 (6계층)

| 패키지 | 역할 | 예 |
|--------|------|----|
| `entry.web` | HTTP API 진입점 (표준/비표준) | `OmUpdownloadFileController`, `TcfApiController` |
| `entry.handler` | TCF 거래 진입점 (`serviceIds`, 도메인당 1개) | `OmErrorCodeHandler` |
| `entry.facade` | 트랜잭션 경계·유스케이스 조합 | `OmErrorCodeFacade` |
| `application.service` | 도메인 처리 | `OmErrorCodeService` |
| `application.rule` | 검증 규칙 | `OmOperationRule` |
| `application.scheduler` | `@Scheduled` 배치 | `EbEventPublishScheduler` |
| `client` | 외부 WAS·API Client | `EpOnlineClient`, `GatewayRouteDispatcher` |
| `persistence.dao` | 영속 접근 추상화 | `OmOperationDao` |
| `persistence.mapper` | MyBatis 매퍼 인터페이스 | `OmOperationMapper` |
| `config` | 설정 클래스 | `OmUpdownloadConfiguration` |
| `support` | 보조 기능(마이그레이션/헬퍼) | `OmDatabaseMigration` |

> `entry.web`는 TCF 표준 거래(`POST /online`)보다 비표준 REST(파일 업로드, UI Relay API)에서 주로 사용한다.  
> 프레임워크 공통 HTTP: `tcf-web`의 `entry.web.OnlineTransactionController`, `entry.facade.TcfGateway`

---

## 4. 계층별 클래스 네이밍

### 4.1 Controller

```text
{Domain}{Resource}{Action?}Controller
```

예:

- `OnlineTransactionController` (프레임워크 공통)
- `TcfApiController` (UI Relay API)
- `OmUpdownloadFileController` (UD REST)

규칙:

- REST 자원 중심이면 `Action` 생략 (`FileController`)
- 동일 자원 내 동작이 많으면 메서드명으로 구분 (`upload`, `list`, `download`)

### 4.2 Handler

```text
{Business}{Domain}Handler
```

예:

- `SvSampleHandler`
- `OmErrorCodeHandler`
- `OmTransactionLogHandler`

규칙:

- **핸들러는 도메인(application Service)당 1개**를 원칙으로 한다 (`{Business}{Domain}Handler`)
- 한 도메인의 여러 거래(`inquiry/save/update/delete/execute`)는 `serviceIds()`로 선언하고 `doHandle` 내부에서 `serviceId` 기준으로 분기한다
- 단일 거래만 담당하는 경우 `serviceId()` 하나만 재정의해도 된다 (하위호환)

### 4.3 Facade

```text
{Business}{Domain}Facade
```

예:

- `SvSampleFacade`
- `OmSampleFacade`
- `OmErrorCodeFacade`

규칙:

- 유스케이스 경계 단위로 분리 (`User`, `Batch`, `ErrorCode`)
- 클래스명에 `Transactional` 같은 기술 용어를 넣지 않는다

### 4.4 Service

```text
{Business}{Domain}Service
```

예:

- `SvSampleService`
- `OmBatchService`
- `OmAuthService`

규칙:

- 핵심 도메인 명사 사용 (`Catalog`, `Session`, `Cache`, `Auth`)
- 지나치게 포괄적인 `CommonService` 금지

### 4.5 Rule

```text
{Business}{Domain?}Rule
```

예:

- `SvSampleRule`
- `OmOperationRule`

규칙:

- 도메인 전역 검증은 `OperationRule`
- 특정 도메인 검증은 `UserRule`, `BatchRule`처럼 명시

### 4.6 DAO

```text
{Business}{Domain}Dao
```

예:

- `SvSampleDao`
- `OmOperationDao`

규칙:

- DAO는 Mapper 묶음 단위 이름과 맞춘다 (`OperationDao` ↔ `OperationMapper`)
- 저장소 기술명이 바뀌어도(`JPA/JDBC`) 도메인 명칭은 유지

### 4.7 Mapper

```text
{Business}{Domain}Mapper
```

예:

- `SvSampleMapper`
- `OmOperationMapper`

규칙:

- 인터페이스명과 XML 파일명은 동일하게 유지
- Mapper 메서드명은 SQL ID와 동일하게 맞춘다

---

## 5. 메서드 네이밍

### 5.1 권장 액션 어휘

| 용도 | 권장 메서드명 |
|------|---------------|
| 단건 조회 | `detail`, `get`, `findBy...` |
| 목록 조회 | `inquiry`, `list`, `search` |
| 등록 | `save`, `create`, `insert` |
| 수정 | `update`, `modify` |
| 삭제/비활성 | `delete`, `remove`, `disable` |
| 실행형 | `execute`, `run` |

### 5.2 계층별 메서드 패턴

| 계층 | 패턴 | 예 |
|------|------|----|
| Handler | `doHandle(...)` 고정 | `doHandle(request, context)` |
| Facade | 유스케이스 동사 | `inquiry(...)`, `save(...)` |
| Service | 도메인 동사 | `searchErrorCodes(...)`, `deleteSession(...)` |
| Rule | `validate*`, `require*`, `normalize*` | `validateInquiry`, `requireField` |
| DAO | `select*`, `insert*`, `update*`, `merge*` | `selectErrorCodeByCode` |
| Mapper | DAO와 동일 | `searchErrorCodes`, `disableErrorCode` |

---

## 6. `serviceId` 네이밍 표준

> 전체 정의·업무/OM action 구분·체크리스트: [53-naming-conventions.md](53-naming-conventions.md) §6

### 6.1 형식

```text
{BC}.{Domain}.{action}
```

| 파트 | 규칙 | 예 |
|------|------|----|
| `{BC}` | 대문자 업무코드 | `OM`, `SV`, `UD` |
| `{Domain}` | PascalCase 도메인 | `ErrorCode`, `TransactionLog`, `Auth` |
| `{action}` | lowerCamelCase 동사 | `inquiry`, `save`, `selectSummary` (업무 도메인) |

**OM·플랫폼:** `inquiry`, `detail`, `save`, `update`, `delete`, `execute`  
**업무 WAR:** `selectSummary`, `selectList`, `create`, `send` 등 도메인 의미 동사 — 해당 업무 내 일관 유지

예:

- `OM.ErrorCode.inquiry`
- `OM.ErrorCode.update`
- `SV.Sample.inquiry`

### 6.2 매핑 원칙

```text
serviceId: OM.ErrorCode.update
  → Handler: OmErrorCodeHandler (serviceIds에 OM.ErrorCode.* 포함, update로 분기)
  → Facade : OmErrorCodeFacade.update(...)
  → Service: OmErrorCodeService.update(...)
  → DAO    : OmOperationDao.updateErrorCode(...)
  → Mapper : OmOperationMapper.updateErrorCode(...)
  → SQL ID : updateErrorCode
```

---

## 7. SQL ID / Mapper XML 네이밍

### 7.1 파일 경로

```text
src/main/resources/mapper/{bc}/{Business}{Domain}Mapper.xml
```

예:

- `mapper/sv/SvSampleMapper.xml`
- `mapper/om/OmOperationMapper.xml`

### 7.2 SQL ID 규칙

| 구분 | 규칙 | 예 |
|------|------|----|
| 조회(단건) | `select{Domain}{ByCondition}` | `selectErrorCodeByCode` |
| 조회(목록) | `search{DomainPlural}` 또는 `select{Domain}List` | `searchErrorCodes` |
| 건수 | `count{DomainPlural}` | `countErrorCodes` |
| 등록 | `insert{Domain}` | `insertErrorCode` |
| 수정 | `update{Domain}` | `updateErrorCode` |
| 병합 | `merge{Domain}` | `mergeErrorCode` |
| 삭제/비활성 | `delete{Domain}` / `disable{Domain}` | `disableErrorCode` |

### 7.3 Mapper 메서드와 SQL ID 일치

Mapper 인터페이스:

```java
int updateErrorCode(Map<String, Object> params);
```

XML:

```xml
<update id="updateErrorCode">
  <!-- SQL -->
</update>
```

원칙:

- `Mapper method name == SQL id`를 강제한다
- ID 축약(`updErrCd`) 금지, 도메인 명확성 우선

---

## 8. 트랜잭션 코드(`transactionCode`) 네이밍

형식:

```text
{BC}-{ACTION3}-{NNNN}
```

예:

- `OM-ERR-0001` (오류코드 조회)
- `OM-ERR-0004` (오류코드 수정)
- `SV-INQ-0001` (샘플 조회)

권장:

- `ACTION3`는 도메인/동작이 식별되게 유지 (`INQ`, `ERR`, `BAT`, `AUT`)
- `serviceId`와 1:1 또는 N:1 대응은 가능하나, 카탈로그 기준으로 유일성 보장

---

## 9. 예외 코드(`errorCode`) 네이밍

형식:

```text
E-{BC}-{CATEGORY}-{NNNN}
```

| 파트 | 의미 | 예 |
|------|------|----|
| `E` | Error prefix | 고정 |
| `{BC}` | COM/OM/SV/UD 등 | `COM`, `OM`, `UD` |
| `{CATEGORY}` | `VALID`, `BIZ`, `AUTH`, `SYS`, `DB` | `BIZ` |
| `{NNNN}` | 4자리 일련번호 | `0001` |

예:

- `E-COM-VALID-0001`
- `E-COM-SYS-0001`
- `E-OM-BIZ-0002`
- `E-UD-SYS-0001`

---

## 10. 프론트/리소스 네이밍

| 항목 | 규칙 | 예 |
|------|------|----|
| HTML | kebab-case | `transaction-log.html`, `message-composer.html` |
| JS 모듈 | kebab-case | `om-admin.js`, `online-multi.js` |
| API Path | 소문자 + 하이픈/슬래시 | `/api/relay/{code}/online` |
| 샘플 JSON | `{bc}-{domain}-{action}.json` | `sv-sample-inquiry.json` |

---

## 11. 신규 거래 추가 체크리스트

1. `serviceId`를 `{BC}.{Domain}.{action}` 형식으로 정의
2. Handler 클래스명 `{Business}{Domain}Handler` 생성 (도메인당 1개)
3. Facade/Service/DAO/Mapper를 동일 도메인 이름으로 생성
4. Mapper 메서드명과 SQL ID를 정확히 일치
5. `transactionCode`를 카탈로그 규칙에 맞게 등록
6. 실패 케이스 `errorCode`(`E-{BC}-...`)를 사전 정의
7. 샘플 JSON 파일명도 동일 도메인·액션으로 맞춤

---

## 12. 좋은 예 / 나쁜 예

### 12.1 좋은 예

```text
serviceId: OM.ServiceCatalog.update
handler  : OmServiceCatalogHandler (도메인당 1개, update로 분기)
facade   : OmServiceCatalogFacade
service  : OmServiceCatalogService
dao      : OmOperationDao.updateServiceCatalog(...)
mapper   : OmOperationMapper.updateServiceCatalog(...)
sql id   : updateServiceCatalog
```

### 12.2 나쁜 예

```text
serviceId: om_updateSvc
handler  : UpdateHandler
service  : CommonService
dao      : OmDao
mapper   : opMapper
sql id   : upd01
```

문제점: 계층 추적 불가, 도메인 불명확, 검색성 저하.

---

## 13. 참고 소스

| 파일 | 설명 |
|------|------|
| `docs/architecture/01-application-layer.md` | 계층 구조·역할 |
| `tcf-core/.../dispatch/TransactionDispatcher.java` | `serviceId` 라우팅 |
| `tcf-om/.../entry/handler/*` | Handler 네이밍 실사례 |
| `tcf-om/.../application/service/OmErrorCodeService.java` | Service 네이밍 실사례 |
| `tcf-om/.../persistence/dao/OmOperationDao.java` | DAO 메서드 네이밍 |
| `tcf-om/.../persistence/mapper/OmOperationMapper.java` | Mapper 메서드 네이밍 |
| `tcf-ui/src/main/resources/sample-requests/` | 샘플 JSON 파일명 규칙 |

---

## 14. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — 계층/SQL/serviceId/errorCode 네이밍 표준화 |
| 2026-06 | 6계층 패키지(entry/application/persistence/client) 반영 |
| 2026-07 | [53-naming-conventions.md](53-naming-conventions.md) 연계, serviceId action 이중 계층 명시 |

---

← [53-naming-conventions.md](53-naming-conventions.md) · [07-DAO.md](07-DAO.md) →
