# 01. 어플리케이션 계층 구조

| 항목 | 내용 |
|------|------|
| 문서 번호 | 01 |
| 제목 | Application Layer Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 대상 | 업무 서비스(`*-service`), `tcf-om` 개발자 |

---

## 1. 개요

NSIGHT TCF Framework의 **어플리케이션 계층**은 TCF 엔진(`tcf-core`)이 제공하는 공통 파이프라인 **이후**에 위치하는 업무 코드 영역이다.

```text
[프레임워크 영역 — tcf-web / tcf-core]
  HTTP 진입 → STF → Dispatcher → ETF
        │
        ▼
[어플리케이션 계층 — 업무 모듈]
  entry/handler → entry/facade → application/service → application/rule → persistence/dao|mapper
```

어플리케이션 계층의 목표:

1. **거래 단위 진입점 통일** — `serviceId`당 Handler 1개
2. **책임 분리** — 오케스트레이션(Facade), 도메인(Service), 검증(Rule), 영속(DAO/Mapper)
3. **프레임워크 비침투** — 업무 로직은 `StandardRequest` / `TransactionContext` 계약만 의존

---

## 2. 계층 전체 구조

### 2.1 수직 계층 다이어그램

```text
┌─────────────────────────────────────────────────────────────┐
│ Presentation (선택)                                         │
│  entry/web — OnlineTransactionController, TcfGateway, REST  │
│  tcf-ui / tcf-uj Relay (브라우저 → gateway 또는 업무 WAR)    │
└────────────────────────────┬────────────────────────────────┘
                             │ StandardRequest JSON
┌────────────────────────────▼────────────────────────────────┐
│ Framework Pipeline (tcf-core + tcf-web)                       │
│  STF → TransactionDispatcher → ETF                          │
└────────────────────────────┬────────────────────────────────┘
                             │ serviceId 라우팅
┌────────────────────────────▼────────────────────────────────┐
│ entry/handler    거래 어댑터 — serviceId 등록, Facade 위임   │
├─────────────────────────────────────────────────────────────┤
│ entry/facade     유스케이스 오케스트레이션, 트랜잭션 경계     │
├─────────────────────────────────────────────────────────────┤
│ application/service   도메인 로직, 결과 조립                  │
├─────────────────────────────────────────────────────────────┤
│ application/rule      입력·업무 규칙 검증 (BusinessException) │
├─────────────────────────────────────────────────────────────┤
│ persistence/dao       영속 접근 추상화 (Repository 역할)      │
├─────────────────────────────────────────────────────────────┤
│ persistence/mapper    MyBatis SQL 매핑                        │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 패키지 구조 (업무 모듈 표준 — 6계층)

업무 WAR(`sv-service` 등), `tcf-om`, `tcf-jwt`, `tcf-gateway`는 동일한 **6계층** 패키지 규칙을 따른다.

```text
com.nh.nsight.marketing.{code}   (또는 com.nh.nsight.auth.jwt, com.nh.nsight.gateway)
├── Nsight{Xxx}Application.java   # extends com.nh.nsight.tcf.web.support.NsightWarBootstrap
├── application/
│   ├── service/       업무 Service
│   ├── rule/          업무 Rule
│   └── scheduler/     @Scheduled (해당 시)
├── client/            외부 WAS·API Client (해당 시)
├── config/            모듈 전용 Spring 설정
├── entry/
│   ├── handler/       TransactionHandler 구현체
│   ├── facade/        유스케이스 Facade
│   └── web/           REST Controller·Filter (해당 시)
├── persistence/
│   ├── dao/           DAO (@Repository)
│   └── mapper/        MyBatis Mapper 인터페이스
└── support/           마이그레이션·헬퍼·상수

src/main/resources/
└── mapper/{code}/   MyBatis XML (예: mapper/sv/SvSampleMapper.xml)
```

| 패키지 | Spring 스테레오타입 | tcf-om | *-service |
|--------|---------------------|--------|-----------|
| `entry.handler` | `@Component` | 40+ Handler | 샘플 1~N Handler |
| `entry.facade` | `@Service` | 도메인별 Facade | 샘플 Facade |
| `entry.web` | `@RestController` | `OmUpdownloadFileController` | — |
| `application.service` | `@Service` | 도메인별 Service | 샘플 Service |
| `application.rule` | `@Component` | `OmOperationRule` 등 | `SvSampleRule` 등 |
| `application.scheduler` | `@Component` | `OmSessionCleanupScheduler` | `EbEventPublishScheduler` |
| `persistence.dao` | `@Repository` | `OmOperationDao` 등 | `SvSampleDao` 등 |
| `persistence.mapper` | `@Mapper` (MyBatis) | `OmOperationMapper` | `SvSampleMapper` |

---

## 3. 계층별 책임

### 3.1 Handler — 거래 어댑터

| 항목 | 설명 |
|------|------|
| **역할** | `serviceId`와 TCF 파이프라인을 연결하는 **유일한 업무 진입점** |
| **인터페이스** | `com.nh.nsight.tcf.core.support.transaction.TransactionHandler` |
| **등록** | Spring `@Component` → `TransactionDispatcher`가 기동 시 `serviceId` 맵에 등록 |
| **금지** | SQL 직접 호출, 복잡한 비즈니스 로직, 트랜잭션 어노테이션 |

Handler는 **얇게(thin)** 유지한다. `request.getBody()`와 `TransactionContext`를 Facade에 넘기는 것이 주 역할이다.

핸들러는 **도메인(application Service)당 1개**를 원칙으로 하며, `serviceIds()`로 담당 거래를 선언하고 `doHandle` 안에서 `serviceId`로 분기한다.

```java
@Component
public class SvSampleHandler implements TransactionHandler {
    private static final String INQUIRY = "SV.Sample.inquiry";

    private final SvSampleFacade facade;

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request,
                           TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "SvSampleHandler 미지원 serviceId: " + serviceId);
        };
    }
}
```

**serviceId 명명 규칙**

```text
{BusinessCode}.{Domain}.{action}
```

전체 명명 표준: [53-naming-conventions.md](../architecture/53-naming-conventions.md)

예) `SV.Sample.inquiry`, `OM.User.save`, `OM.Dashboard.inquiry`

- `BusinessCode`: CC, SV, OM, UD …
- `Domain`: 업무 도메인·엔티티
- `action`: inquiry, save, update, delete, execute …

`TransactionDispatcher`는 **동일 serviceId 중복 등록 시 기동 실패**한다.

---

### 3.2 Facade — 유스케이스 오케스트레이션

| 항목 | 설명 |
|------|------|
| **역할** | 하나의 거래(유스케이스) 흐름 조율 |
| **트랜잭션** | `@Transactional` 경계를 **Facade**에 둔다 |
| **호출** | Service(들), 필요 시 Support |
| **반환** | Handler → ETF로 전달될 `Map` 또는 DTO |

```java
@Service
public class SvSampleFacade {
    private final SvSampleService service;

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }
}
```

**Facade 배치 기준 (tcf-om)**

| Facade | 담당 유스케이스 예 |
|--------|-------------------|
| `OmAuthFacade` | 로그인·로그아웃 |
| `OmUserFacade` | 사용자 CRUD |
| `OmDashboardFacade` | 운영 대시보드 조회 |
| `OmCommonCodeFacade` | 공통코드 CRUD |
| `OmBatchFacade` | 배치 Job 실행 |

복수 Service를 엮거나 외부 연동(tcf-batch HTTP 호출)이 필요하면 Facade에서 조합한다.

---

### 3.3 Service — 도메인 로직

| 항목 | 설명 |
|------|------|
| **역할** | 실질적인 업무 처리·데이터 조립 |
| **호출** | Rule(검증) → DAO(조회/저장) |
| **컨텍스트** | `TransactionContext`에서 guid, userId, serviceId 참조 가능 |
| **예외** | `BusinessException` — ETF가 표준 오류 응답으로 변환 |

```java
@Service
public class SvSampleService {
    private final SvSampleRule rule;
    private final SvSampleDao dao;

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateInquiry(body);
        Map<String, Object> data = dao.selectSample(body);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "SV");
        result.put("serviceId", context.getHeader().getServiceId());
        result.put("guid", context.getHeader().getGuid());
        result.put("data", data);
        return result;
    }
}
```

**Service vs Facade**

| 구분 | Facade | Service |
|------|--------|---------|
| 트랜잭션 | `@Transactional` 선언 | 트랜잭션 어노테이션 없음(기본) |
| 범위 | 유스케이스 1건 | 도메인 연산 |
| 재사용 | Handler 전용 조합 | Facade·다른 Service에서 재사용 가능 |

---

### 3.4 Rule — 업무 규칙·입력 검증

| 항목 | 설명 |
|------|------|
| **역할** | Body·조건값 검증, 업무 불가 판단 |
| **예외** | `BusinessException` + `ErrorCode` |
| **위치** | Service 호출 **전** 검증 |

```java
@Component
public class SvSampleRule {
    public void validateInquiry(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 비어 있습니다.");
        }
    }
}
```

**Rule vs STF 검증**

| 계층 | 검증 대상 |
|------|-----------|
| STF (`tcf-core`) | Header 필수값, 세션·권한, 멱등성, GUID |
| Rule (어플리케이션) | Body 필드, 업무 제약, 도메인 규칙 |

Header 수준 오류는 STF, Body·도메인 오류는 Rule/Service에서 처리한다.

---

### 3.5 DAO — 영속 접근

| 항목 | 설명 |
|------|------|
| **역할** | DB 접근 캡슐화 (Repository 패턴) |
| **구현** | MyBatis Mapper 위임, 또는 단순 JDBC/인메모리 |
| **반환** | `Map`, `List<Map>`, primitive wrapper |

```java
@Repository
public class OmOperationDao {
    private final OmOperationMapper mapper;

    public List<Map<String, Object>> selectApStatus() {
        return mapper.selectApStatus();
    }
}
```

DAO는 **비즈니스 판단을 하지 않는다**. 조건 Map 조립·Mapper 호출·결과 반환만 담당한다.

---

### 3.6 Mapper — SQL 매핑

| 항목 | 설명 |
|------|------|
| **역할** | MyBatis SQL 정의 |
| **인터페이스** | `mapper/{Domain}Mapper.java` |
| **XML** | `resources/mapper/{code}/{Domain}Mapper.xml` |

```java
public interface OmOperationMapper {
    List<Map<String, Object>> selectApStatus();
}
```

복잡한 조회·MERGE·페이징은 XML에 두고, Java에서는 메서드 시그니처만 유지한다.

---

## 4. 프레임워크 경계와의 관계

어플리케이션 계층은 아래 프레임워크 컴포넌트를 **호출하지 않고**, Dispatcher가 Handler를 호출하는 **피호출 구조**이다.

```text
OnlineTransactionController
        │
        ▼
      TCF.process()
        │
   ┌────┴────┐
   ▼         ▼
  STF    Dispatcher ──→ Handler (어플리케이션)
   │         │
   └────┬────┘
        ▼
       ETF  ←── Handler 반환값을 StandardResponse로 조립
```

### 4.1 TransactionContext

Handler·Facade·Service 전 구간에서 전달되는 요청 컨텍스트.

| 제공 정보 | 용도 |
|-----------|------|
| `StandardHeader` | serviceId, userId, guid, businessCode |
| 거래 메타 | 감사·로그 연계 |

STF가 생성·등록하고, TCF `finally`에서 `TransactionContextHolder.clear()`로 정리한다.

### 4.2 TcfGateway — 비표준 진입점

`POST /online`이 아닌 REST(파일 업로드 등)도 TCF 파이프라인을 태우려면 `TcfGateway.invoke()`를 사용한다.

```java
tcfGateway.invoke(
    TcfInvokeRequest.builder("UD.File.list", "UD-LST-0001", "INQUIRY")
        .body(body)
        .userId(userId)
        .clientIp(clientIp)
        .build()
);
```

> `tcf-om`의 `/ud/files` REST Controller는 multipart 특성상 **직접 Service 호출** 패턴도 사용한다.  
> 표준 JSON 거래는 Handler 경로를 우선한다.

---

## 5. 모듈 유형별 적용

### 5.1 업무 서비스 (`ic-service` … `mg-service`, 현재 9개)

```text
표준 6계층: Handler → Facade → Service → Rule → DAO → Mapper
```

- 거래 1건당 Handler 1개 (샘플: `SV.Sample.inquiry`)
- 업무별 독립 WAR, 동일 패키지 규칙
- `tcf-web` + `tcf-core` 의존

### 5.2 운영 모듈 (`tcf-om`)

```text
Handler (40+) → Facade (도메인별) → Service → Rule/DAO/Mapper
         │
         ├─ support/   DB 마이그레이션, 배치 원격 호출, 런타임 헬스
         ├─ config/    Spring Session, 비밀번호 인코더
         └─ updownload/  UD 파일 REST (Controller → Service)
```

- OM Admin 전 기능이 `OM.*` serviceId로 TCF 거래화
- `OM_SERVICE_CATALOG` 테이블에 Handler 클래스·권한·타임아웃 등록
- EhCache(`tcf-cache`)는 Service 계층에서 `@Cacheable` 사용

### 5.3 UI Relay (`tcf-ui`) — 프레젠테이션

어플리케이션 6계층과 **별도**이다.

```text
브라우저 → om-admin.js / TcfApiController → HTTP Relay → 업무 WAR /online
```

- 비즈니스 로직 없음, 경로·context 보정(`ui-context.js`)만 담당

### 5.4 수집 배치 (`tcf-batch`) — 인프라 어플리케이션

Handler 패턴 대신 **Job + CollectService** 구조.

```text
Scheduler / REST /jobs/{name}/run
  → ApStatusCollectService 등
  → Actuator Client / JDBC
  → OmDashboardStatusRepository (MERGE)
```

OM 대시보드 조회(`OM.Dashboard.inquiry`)는 tcf-om Service가 위 테이블을 **읽기만** 한다.

---

## 6. 호출 규칙 (의존성)

### 6.1 허용 호출 방향

```text
Handler     → Facade
Facade      → Service, Support
Service     → Rule, DAO, 다른 Service(주의)
DAO         → Mapper
Rule        → (없음 — 순수 검증)
```

### 6.2 금지 사항

| 금지 | 이유 |
|------|------|
| Handler → DAO 직접 | 계층 우회, 테스트·트랜잭션 경계 붕괴 |
| Rule → DAO | 검증·조회 혼재 |
| Mapper → Service | 역방향 의존 |
| Service → Handler | 순환 참조 |
| DAO 간 직접 호출 | Service에서 조합 |

### 6.3 트랜잭션 가이드

| 처리 유형 | 권장 위치 | 어노테이션 예 |
|-----------|-----------|---------------|
| 조회 | Facade | `@Transactional(readOnly = true, timeout = 5)` |
| 등록·수정·삭제 | Facade | `@Transactional(timeout = 10)` |
| 다단계 유스케이스 | Facade 하나에서 묶기 | 단일 `@Transactional` |

타임아웃은 `OM_SERVICE_CATALOG.TIMEOUT_SEC`와 맞춘다.

---

## 7. 요청·응답 데이터 흐름

```text
1. Client POST /sv/online + StandardRequest JSON
2. OnlineTransactionController — businessCode·clientIp 보정
3. TCF.process()
4. STF — Header 검증, Context 생성
5. Dispatcher — "SV.Sample.inquiry" → SvSampleHandler
6. Handler — facade.inquiry(body, context)
7. Facade — @Transactional, service.inquiry()
8. Service — rule.validate → dao.select → result Map 조립
9. ETF — StandardResponse { header, result, body } 생성
10. Client — HTTP 200 + resultCode로 성공/실패 판별
```

**Body 관례**

- 요청 Body: `Map<String, Object>` — 화면·채널별 유연한 필드
- 응답 Body: Service가 조립한 `Map` — 화면 계약은 API 문서·샘플 JSON으로 관리  
  (`tcf-ui/src/main/resources/sample-requests/`)

---

## 8. 신규 거래 추가 절차

1. **serviceId 정의** — `{BC}.{Domain}.{action}`
2. **Handler** — 도메인 핸들러에 `serviceIds()` 등록 + `serviceId` 분기 → Facade 위임 (새 도메인일 때만 신규 생성)
3. **Facade** — `@Transactional` 유스케이스 메서드
4. **Service** — 도메인 로직
5. **Rule** — 입력 검증 (필요 시)
6. **DAO / Mapper / XML** — DB 연동 (필요 시)
7. **카탈로그 등록** (tcf-om) — `OM_SERVICE_CATALOG` 또는 `data.sql` MERGE
8. **샘플 JSON** — `tcf-ui/.../sample-requests/` 추가
9. **검증** — `curl POST /{code}/online` 또는 tcf-ui 거래 화면

---

## 9. 참고 소스 (학습 순서)

| 순서 | 모듈 | 파일 |
|------|------|------|
| 1 | tcf-core | `processor/TCF.java`, `dispatch/TransactionDispatcher.java` |
| 2 | tcf-web | `entry/web/OnlineTransactionController.java` |
| 3 | sv-service | `entry/handler/SvSampleHandler.java` |
| 4 | sv-service | `entry/facade/SvSampleFacade.java` → `application/service/SvSampleService.java` |
| 5 | sv-service | `application/rule/SvSampleRule.java`, `persistence/dao/SvSampleDao.java` |
| 6 | tcf-om | `entry/handler/OmUserHandler.java` (도메인 CRUD 패턴) |
| 7 | tcf-om | `entry/handler/OmDashboardHandler.java` (조회·집계) |

---

## 10. 관련 문서

| 문서 | 설명 |
|------|------|
| [architecture.md](architecture.md) | 전체 아키텍처 정의서 |
| [02-junmun.md](02-junmun.md) | 표준 전문(준문) 구조 |
| [03-transaction.md](03-transaction.md) | 트랜잭션 처리 아키텍처 |
| [04-messaging.md](04-messaging.md) | 메시지 처리·릴레이 |
| [05-exception.md](05-exception.md) | 예외 처리 표준 |
| [29-facade.md](29-facade.md) | Web→TCF→Facade→Service→DAO 소스 가이드 |
| [TCF_FRAMEWORK_GUIDE.md](../TCF_FRAMEWORK_GUIDE.md) | Handler 개발 가이드 |
| [TCF_MODULE_RESTRUCTURE.md](../TCF_MODULE_RESTRUCTURE.md) | 모듈·패키지 변경 이력 |
| [SOURCE_INDEX.md](../SOURCE_INDEX.md) | 소스 위치 인덱스 |

---

## 11. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 |
| 2026-06 | 6계층 패키지(entry/application/persistence) 반영 |
