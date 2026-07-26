# ARC05 - NSIGHT TCF 네이밍(명명규칙) 전체 정리

## 1. 문서 개요

### 1.1 목적
- NSIGHT TCF Framework의 **명명규칙 전체 맵**을 한 문서로 제공
- 업무코드 → Context/WAR/Package → ServiceId → 6계층 클래스 → Mapper/SQL → DB → 오류코드/로그까지 **하나의 연결선**으로 정리
- 개발·운영·장애 추적 시 “이름만으로” 업무·거래·위치를 추정할 수 있게 함

### 1.2 범위
| 영역 | 내용 |
|------|------|
| 업무·모듈 | 업무코드, Context, WAR, Gradle Module, Package Root |
| 도메인 | 업무 내 책임영역(Domain) 명명·세구분·ServiceId 연결 |
| 패키지 | **Package Root** 명명, **하위 패키지(Layer/SubArea/Domain)** 구성 |
| 거래 식별 | ServiceId, TransactionCode, Header 식별자 |
| Java 프로그래밍 | Class·Interface·Enum·Method·Field·Constant·DTO·Exception·지역변수 |
| MyBatis/DB | **테이블·컬럼·PK/UK/FK/Index**, Mapper XML, SQL ID, Row 매핑 |
| 화면·운영 | ScreenId, MenuId, 오류코드, Cache, Batch, Gateway |
| 배포 | Docker 이미지/컨테이너, K8s 리소스명 |

### 1.3 대상 독자
- 업무 개발자 / 프레임워크 개발자
- 아키텍트 / OM·운영 담당
- 코드리뷰·품질 Gate 담당

### 1.4 관련 문서
| 문서 | 내용 |
|------|------|
| ARC02~04 | 애플리케이션·비즈니스·통합 아키텍처 |
| `znsight-man/명명규칙-*.md` | 명명규칙 상세(01~21) |
| 제11부 집필본 | 명명·패키지·화면·ServiceId·DB 표준 |
| `README.md` | ServiceId·6계층 요약 |

---

## 2. 네이밍 연결 개요

### 2.1 핵심 연결선

```
업무코드 (SV)
   ↓
Context /sv  ·  WAR sv.war  ·  Module sv-service  ·  Package ...marketing.sv
   ↓
ServiceId          SV.Customer.selectSummary
TransactionCode    SV-INQ-0001
   ↓
Handler            SvCustomerHandler
Facade / Service / Rule / DAO / Mapper
   ↓
Mapper XML · SQL ID · Table SV_CUSTOMER_SUMMARY
   ↓
거래로그(GUID/TraceId/ServiceId) · 오류코드 E-SV-BIZ-0001
```

### 2.2 최상위 원칙

| 원칙 | 기준 | 예시 |
|------|------|------|
| 업무코드 우선 | 이름은 업무코드에서 파생 | SV → /sv, sv.war, Sv* |
| Context/WAR/Package 일치 | 한 업무코드 = 한 Context·WAR·Package | `/eb`, `eb.war`, `.eb` |
| 실행 기준은 ServiceId | URL이 아니라 `header.serviceId`로 Handler 결정 | `EB.User.inquiry` |
| Java Class PascalCase | 역할 Suffix 명확 | `EbEventHandler` |
| Method/Field camelCase | 행위·필드 소문자 시작 | `selectSummary()` |
| Package lowercase | 전부 소문자 | `com.nh.nsight.marketing.eb` |
| DB UPPER_SNAKE | 테이블·컬럼·인덱스 | `EB_EVENT`, `EVENT_STATUS` |
| 오류코드 추적 가능 | DOMAIN·CATEGORY·일련번호 | `E-EB-BIZ-0001` |
| 약어 통제 | 표준사전만 사용 | Svc/Mgr/Proc 지양 |
| 운영 추적성 | 로그만으로 업무·거래 식별 | GUID + ServiceId + TxCode |

### 2.3 식별자 역할 구분

| 식별자 | 역할 | 예시 |
|--------|------|------|
| businessCode | 어느 업무 WAR인가 | `EB` |
| serviceId | 어느 Handler/기능인가 | `EB.Event.inquiry` |
| transactionCode | 로그·통제·감사상 어떤 거래인가 | `EB-INQ-0001` |
| guid / traceId | 거래·구간 추적 | (헤더 발급) |
| screenId / menuId | 화면·메뉴 권한·감사 | `EB-EVT-0001` |
| errorCode | 상세 오류 원인 | `E-EB-BIZ-0001` |

---

## 3. 업무코드 · Context · WAR · Module · Package

### 3.1 변환 규칙 (업무코드 확정 시 자동 결정)

| 대상 | 변환 | 예 (EB) |
|------|------|---------|
| 업무코드 | 영문 대문자 2자리 | `EB` |
| Context | `/` + 소문자 | `/eb` |
| Online URL | `/{code}/online` | `/eb/online` |
| WAR | 소문자 + `.war` | `eb.war` |
| Gradle Module | 소문자 + `-service` | `eb-service` |
| Java Prefix | PascalCase | `Eb` |
| Package | `com.nh.nsight.marketing.{소문자}` | `...marketing.eb` |
| ServiceId Prefix | 대문자 + `.` | `EB.` |

**예외**

| 모듈 | Module | Context / WAR | Package |
|------|--------|---------------|---------|
| OM | `tcf-om` (권장) | `/om`, `om.war` | `...marketing.om` |
| OC | `tcf-oc` | `/oc` | `...marketing.oc` 등 |
| Gateway | `tcf-gateway` | `/gw` 등 | `com.nh.nsight.gateway` |
| JWT | `tcf-jwt` | `/jwt` | `com.nh.nsight.auth.jwt` |
| UI / UJ | `tcf-ui` / `tcf-uj` | `/ui`, `/uj` | `com.nh.nsight.tcf.ui` / `.uj` |

### 3.2 업무코드 표준표

| 코드 | 명칭 | Context | Module | Port(local) |
|------|------|---------|--------|-------------|
| CC | Common | /cc | (확장) | 8081 |
| IC | Integration Customer | /ic | ic-service | 8082 |
| PC | Private Customer | /pc | pc-service | 8083 |
| BC | Business Customer | /bc | (확장) | 8084 |
| MS | Mini Single View | /ms | ms-service | 8085 |
| SV | Single View | /sv | sv-service | 8086 |
| PD | Product | /pd | pd-service | 8087 |
| CM | Campaign | /cm | (확장) | 8088 |
| EB | EBM | /eb | eb-service | 8089 |
| EP | Event Processing | /ep | ep-service | 8090 |
| BP | Behavior Processing | /bp | (확장) | 8091 |
| BD | Behavior Data | /bd | (확장) | 8092 |
| SS | Sales Support | /ss | ss-service | 8093 |
| OC | Operation Capacity | /oc | tcf-oc | 8094 |
| MG | Message | /mg | mg-service | 8096 |
| OM | Operation Management | /om | tcf-om | 8097 |
| UD | UpDownload | /om (내장) | tcf-om | 8097 |
| JWT | JWT Auth | /jwt | tcf-jwt | 8110 |

> MG = Message. Management 의미로 MG를 쓰지 않는다 (운영은 OM).

### 3.3 공통 Gradle 모듈명

| Module | 역할 |
|--------|------|
| tcf-util / tcf-core / tcf-web / tcf-eai / tcf-cache | 프레임워크 |
| tcf-om / tcf-oc / tcf-batch / tcf-ui / tcf-uj | 운영·UI |
| tcf-gateway / tcf-jwt | 관문·인증 |
| tcf-help / tcf-scripts / tcf-cicd / tcf-docker / tcf-k8s | 문서·스크립트·배포 |

---

## 3.4 도메인(Domain) 명명규칙

> **점검 결과:** ARC05에 도메인 전용 절은 없었고, ServiceId·클래스의 **「업무대상」** 으로만 산재해 있었다.  
> 아래는 제11부(87.4~87.7) · `명명규칙-07-ServiceId` · 본 레포(EB 등) 기준으로 **도메인 명명**을 독립 정리한 것이다.

### 3.4.1 도메인이란
- **업무코드(SV, EB…)** = WAR / Context / Package 경계
- **도메인(Domain)** = 그 업무 **안**에서 동일한 책임·규칙을 갖는 개념 영역(업무대상)
- ServiceId 두 번째 요소, Handler/Facade/Service 접미사, DTO·테이블·화면세구분과 연결

```
업무코드 EB
├── User          ← 도메인
├── Event
├── Sample
├── SystemTx
└── Batch
```

### 3.4.2 도메인 이름 규칙

| 규칙 | 기준 | 예 |
|------|------|-----|
| 표기 | **명사형 단수** 영문 PascalCase | `Customer`, `Event`, `Campaign` |
| 의미 | **처리행위가 아닌 업무대상** | `Approval` (O) / `DoApproval` (X) |
| 길이 | 짧고 명확, 합성어 최소화 | `UserEvent` (복합 개념일 때만) |
| 복수형 금지 | 단수 유지 | `Customers` (X) → `Customer` |
| 동사·처리형 금지 | 행위는 ServiceId 3번째에 | `CampaignProcessing` (X) |
| 모호한 포괄명 금지 | | `CustomerDataManagement` (X) |
| 약어 | 표준 사전만 (세구분과 매핑) | `SystemTx` (시스템거래, 합의된 약어) |

**권장:** `Customer`, `Product`, `Campaign`, `Event`, `User`, `Message`, `Target`, `Approval`  
**금지:** `Customers`, `DoApproval`, `CampaignProcessing`, `Data`, `Info`, `Manager`

### 3.4.3 표기 위치별 변환

| 위치 | 표기 | 예 (Event 도메인) |
|------|------|-------------------|
| ServiceId | PascalCase | `EB.Event.inquiry` |
| Java Class | PascalCase (Prefix 뒤) | `EbEventHandler`, `EbEventService` |
| Java package 하위 | lowercase | `…application.dto.event`, `…persistence.dto.event` |
| 테이블 | UPPER_SNAKE + 업무 Prefix | `EB_EVENT` |
| 화면 세구분(약어) | 대문자 2~3자 | `EVT` ↔ Event |
| SQL 관리 ID(선택) | 세구분 약어 | `EB-EVT-SEL-001` |

### 3.4.4 업무세구분코드 ↔ 도메인

화면 ID·관리대장용 짧은 코드. **가능하면 도메인과 1:1 대응.**

| 업무 | 세구분 | 코드 | 도메인 |
|------|--------|------|--------|
| SV | 고객 | CUS | Customer |
| SV | 상품 | PRD | Product |
| SV | 활동 | ACT | Activity |
| CM | 캠페인 | CAM | Campaign |
| CM | 대상자 | TGT | Target |
| CM | 승인 | APR | Approval |
| EB | 이벤트 | EVT | Event |
| EB | 사용자 | USR | User |
| OM | 서비스관리 | SVC | ServiceCatalog |
| OM | 거래로그 | TXL | TransactionLog |
| OM | Batch | BAT | Batch |

화면 그룹과 앱 도메인이 항상 동일할 필요는 없으나, **추적성을 위해 매핑표를 유지**한다.

### 3.4.5 업무별 도메인 예시 (본 레포·표준)

| 업무 | 도메인 예 | ServiceId 예 |
|------|-----------|--------------|
| SV | Customer, Product | `SV.Customer.selectSummary` |
| EB | User, Event, Sample, SystemTx, Batch | `EB.Event.inquiry`, `EB.User.create` |
| EP | UserEvent, Sample | `EP.UserEvent.receive` |
| IC | Customer | `IC.Customer.…` |
| OM | User, ServiceCatalog, ErrorCode, Dashboard | `OM.User.inquiry` |
| MG | Message | `MG.Message.send` |
| JWT | Auth / Token | `JWT.Auth.issue` (관례) |

### 3.4.6 Handler · 패키지와의 관계
- **도메인당 Handler 1개** 권장 → `EbEventHandler`가 `EB.Event.*` 담당
- DTO 패키지: `application.dto.{domainLower}` / `persistence.dto.{domainLower}`
- 새 기능 추가 시: **도메인 확정 → ServiceId → Handler/DTO/Table** 순서로 이름 맞춤

### 3.4.7 도메인 체크리스트
- [ ] 명사 단수 PascalCase인가?
- [ ] 행위(동사)가 도메인명에 섞이지 않았는가?
- [ ] ServiceId 2번째 = 도메인명과 동일한가?
- [ ] Handler/Service/Mapper에 동일 도메인 토큰이 들어가는가?
- [ ] 테이블 `{CODE}_{DOMAIN_SNAKE}` 와 대응되는가?
- [ ] 화면 세구분 약어 매핑이 있는가?

---

## 4. Package · Java 프로그래밍 네이밍

### 4.1 패키지(Package) 네이밍

> `znsight-man/명명규칙-06-Package` + 본 레포 실제 소스 기준.

---

#### 4.1.1 Package Root 네이밍

**Package Root** = 모듈(또는 업무)의 Java 패키지 **시작점**.  
그 아래에만 계층(`entry`/`application`/…)·도메인 하위 패키지를 둔다.

##### (1) Root 세그먼트 분해

```
com . nh . nsight . marketing . eb
│     │     │         │          │
│     │     │         │          └─ 업무 Package (업무코드 소문자)
│     │     │         └─ 영역 Root (마케팅 플랫폼)
│     │     └─ 제품/플랫폼 Root (NSIGHT)
│     └─ 조직
└─ 역도메인 (관례)
```

| 세그먼트 | 명칭 | 규칙 | 예 |
|----------|------|------|-----|
| `com` | 역도메인 TLD | 고정 lowercase | `com` |
| `nh` | 조직(Organization) | 고정 | `nh` |
| `nsight` | 제품/플랫폼 Root | 고정 | `nsight` |
| `marketing` | **영역(Area) Root** | 업무 WAR 공통 | `marketing` |
| `{code}` | **업무 Package Root 접미** | 업무코드 소문자 2자리 | `eb`, `sv` |
| `tcf` | TCF 프레임워크 영역 | 공통 모듈 | `tcf.core`, `tcf.web` |
| `gateway` / `auth` | 관문·인증 영역 | 업무와 분리 | `gateway`, `auth.jwt` |

##### (2) 용어 정리

| 용어 | 의미 | 예 |
|------|------|-----|
| **NSIGHT Base** | `com.nh.nsight` | 전 모듈 공통 접두 |
| **영역 Root (Area Root)** | Base + 영역 | `com.nh.nsight.marketing` |
| **업무 Package Root** | 영역 Root + 업무코드 | `com.nh.nsight.marketing.eb` |
| **프레임워크 Package Root** | Base + `tcf.{module}` | `com.nh.nsight.tcf.core` |
| **특수 Package Root** | Base + 전용 경로 | `com.nh.nsight.gateway`, `com.nh.nsight.auth.jwt` |

##### (3) Package Root 표준표 (모듈별)

| 구분 | Package Root | Gradle Module | 비고 |
|------|--------------|---------------|------|
| 업무 WAR | `com.nh.nsight.marketing.{code}` | `{code}-service` | IC/PC/MS/SV/PD/EB/EP/SS/MG… |
| OM | `com.nh.nsight.marketing.om` | `tcf-om` | 영역은 marketing 유지 |
| OC | `com.nh.nsight.marketing.oc` | `tcf-oc` | |
| TCF Util | `com.nh.nsight.tcf.util` | `tcf-util` | |
| TCF Core | `com.nh.nsight.tcf.core` | `tcf-core` | |
| TCF Web | `com.nh.nsight.tcf.web` | `tcf-web` | |
| TCF Cache | `com.nh.nsight.tcf.cache` | `tcf-cache` | |
| TCF EAI | `com.nh.nsight.tcf.eai` | `tcf-eai` | |
| TCF UI | `com.nh.nsight.tcf.ui` | `tcf-ui` | |
| TCF UJ | `com.nh.nsight.tcf.uj` | `tcf-uj` | |
| TCF Batch | `com.nh.nsight.tcf.batch` | `tcf-batch` | |
| Gateway | `com.nh.nsight.gateway` | `tcf-gateway` | 본 레포 (가이드 `tcf.gateway`와 상이) |
| JWT | `com.nh.nsight.auth.jwt` | `tcf-jwt` | 본 레포 (가이드 `tcf.jwt`와 상이) |

##### (4) Package Root 명명 규칙·금지

| 규칙 | 내용 |
|------|------|
| lowercase only | `Marketing`, `EB` 세그먼트 금지 |
| 업무 Root = 영역 + 코드 | `com.nh.nsight.eb` (영역 생략) 금지 |
| 한 모듈 한 Root | 모듈 안에서 Root를 섞지 않음 |
| 공통 코드를 marketing에 넣지 않음 | 공통은 `tcf.*` |
| 타 업무 Root 직접 참조 금지 | HTTP Client / EAI만 |

**변환:** 업무코드 `EB` → 업무 Package Root `com.nh.nsight.marketing.eb`

---

#### 4.1.2 하위 패키지 구성 네이밍

업무 Package Root **아래**를 계층·세부·도메인으로 구성한다.

##### (1) 전체 형식

```
{PackageRoot}.{layer}.{subArea}[.{domainLower}][.{more}]
```

예:
```
com.nh.nsight.marketing.eb.entry.handler
com.nh.nsight.marketing.eb.application.service
com.nh.nsight.marketing.eb.application.dto.event
com.nh.nsight.marketing.eb.persistence.mapper
com.nh.nsight.marketing.eb.persistence.dto.event
com.nh.nsight.marketing.eb.client.dto.ep
```

| 세그먼트 | 명칭 | 허용값 | 예 |
|----------|------|--------|-----|
| `{layer}` | 계층(Layer) | `entry`, `application`, `persistence`, `client`, `config`, `support` | `application` |
| `{subArea}` | 세부 책임(SubArea) | layer별 표준 목록 (아래) | `handler`, `dto` |
| `{domainLower}` | 도메인 소문자 | 도메인 PascalCase → lowercase | `event`, `user` |

##### (2) 계층(Layer) 표준 이름

| Layer | 의미 | 하위(subArea) |
|-------|------|----------------|
| `entry` | 진입점 | `handler`, `facade`, `web` |
| `application` | 업무 로직 | `service`, `rule`, `dto`, `scheduler` |
| `persistence` | 영속화 | `dao`, `mapper`, `dto` |
| `client` | 외부/타업무 호출 | (root에 Client) / `dto` |
| `config` | Spring 설정 | (보통 평탄) |
| `support` | 상수·Enum·헬퍼 | (평탄 또는 주제별) |

##### (3) SubArea 표준 이름 · 클래스 대응

| 전체 Package (Root 이하) | SubArea | 두는 클래스 |
|--------------------------|---------|-------------|
| `entry.handler` | handler | `{Prefix}{Domain}Handler` |
| `entry.facade` | facade | `{Prefix}{Domain}Facade` |
| `entry.web` | web | `*Controller`, Filter |
| `application.service` | service | `{Prefix}{Domain}Service` |
| `application.rule` | rule | `{Prefix}{Domain}Rule` |
| `application.dto.{domain}` | dto + domain | `*Request`, `*Response`, `*Criteria` |
| `application.scheduler` | scheduler | `*Scheduler` |
| `persistence.dao` | dao | `{Prefix}{Domain}Dao` |
| `persistence.mapper` | mapper | `{Prefix}{Domain}Mapper` |
| `persistence.dto.{domain}` | dto + domain | `*Row`, `*InsertRow` |
| `client` | — | `*Client` |
| `client.dto.{peer}` | dto + 상대업무 | 연계 payload |
| `config` | — | `*Config`, `*Properties` |
| `support` | — | Constants, Enum, Migration |

##### (4) 하위 패키지 트리 (표준)

```
{PackageRoot} = com.nh.nsight.marketing.{code}
│
├── entry/
│   ├── handler/                 # ServiceId 진입
│   ├── facade/                  # 유스케이스·TX
│   └── web/                     # 부가 REST (선택)
│
├── application/
│   ├── service/
│   ├── rule/
│   ├── dto/
│   │   └── {domain}/            # event, user, … (lowercase)
│   └── scheduler/               # 선택
│
├── persistence/
│   ├── dao/
│   ├── mapper/
│   └── dto/
│       └── {domain}/            # Row 전용
│
├── client/
│   └── dto/
│       └── {peerCode}/          # ep, ic, … 상대 업무
│
├── config/
└── support/
```

**리소스(비-Java) 경로 명명**

| 종류 | 경로 | 예 |
|------|------|-----|
| Mapper XML | `classpath:mapper/{code}/` | `mapper/eb/EbEventMapper.xml` |
| 정적 UI | `static/{code}/` | `static/eb/…` (tcf-ui) |

##### (5) Layer / SubArea 책임 · 금지

| Package | 역할 | 금지 |
|---------|------|------|
| `entry.handler` | ServiceId 진입, Body→DTO | SQL·복잡 업무 로직 |
| `entry.facade` | 유스케이스 조립, `@Transactional` | Mapper 직접 호출 지양 |
| `entry.web` | 부가 REST/Filter | 온라인 거래 Controller 남발 |
| `application.service` | 업무 처리 순서 | SQL 작성 |
| `application.rule` | 검증·판단 | DB 접근 |
| `application.scheduler` | 스케줄 | 공통 배치 중복 |
| `application.dto.*` | 외부/업무 DTO | persistence Row와 혼용 |
| `persistence.dao` | Mapper 캡슐화 | 업무 판단 |
| `persistence.mapper` | MyBatis API | 업무 로직 |
| `persistence.dto.*` | DB Row | 화면 Response 직접 노출 |
| `client` | 타 업무 HTTP | 타 WAR Java 직접 참조 |
| `config` | Spring 설정 | 업무 로직 |
| `support` | 상수·Enum·헬퍼 | 핵심 로직 은닉용 잡동사니화 |

##### (6) 의존 방향 (하위 패키지)

```
entry.handler
  → entry.facade
    → application.service
      → application.rule
      → persistence.dao → persistence.mapper
  → application.dto.* / client (필요 시)
```

- `persistence.*` → `application.*` / `entry.*` **금지**
- `marketing.sv` → `marketing.ic` **직접 import 금지** (Client만)

##### (7) EB 실제 하위 패키지 예시

```
com.nh.nsight.marketing.eb                    ← 업무 Package Root
├── entry.handler                             EbEventHandler, EbUserHandler
├── entry.facade                              EbEventFacade
├── application.service                       EbEventService, EbEventPublishService
├── application.rule                          EbEventRule
├── application.dto.event                     EventInquiryRequest, …
├── application.dto.user                      UserCreateRequest, …
├── persistence.dao                           EbEventDao
├── persistence.mapper                        EbEventMapper
├── persistence.dto.event                     EventRow, EventStatusCountRow
├── client                                    EpOnlineClient
├── client.dto.ep                             EpUserEventPayload
├── config                                    EbEventPublishProperties
└── support                                   EbEventStatus, …
```

##### (8) Package Root · 하위 구성 체크리스트
- [ ] Root가 표준표의 한 줄과 일치하는가? (`…marketing.eb` 등)
- [ ] Root 아래 첫 세그먼트가 `entry|application|persistence|client|config|support`인가?
- [ ] DTO는 `application.dto.{domain}` / Row는 `persistence.dto.{domain}`인가?
- [ ] domain 세그먼트는 **lowercase**인가?
- [ ] Mapper XML이 `mapper/{code}/` 인가?
- [ ] 타 업무 Package를 compile 의존하지 않는가?

---

#### 4.1.3 요약 한눈에

| 단계 | 이름 | 예 |
|------|------|-----|
| 1 | NSIGHT Base | `com.nh.nsight` |
| 2 | 영역 Root | `com.nh.nsight.marketing` |
| 3 | 업무 Package Root | `com.nh.nsight.marketing.eb` |
| 4 | Layer | `…eb.application` |
| 5 | SubArea | `…application.dto` |
| 6 | Domain | `…dto.event` |

```
com.nh.nsight.marketing.{code}/
├── entry/{handler|facade|web}
├── application/{service|rule|dto/{domain}|scheduler}
├── persistence/{dao|mapper|dto/{domain}}
├── client[/dto/{peer}]
├── config
└── support
```

---

### 4.2 Java 식별자 표기법 요약

| 종류 | 표기 | 예 |
|------|------|-----|
| Class / Interface / Enum / Record | PascalCase | `EbEventService`, `EventStatus` |
| Method | lowerCamelCase, **동사+목적어** | `selectEventList()` |
| Field / 지역변수 / 파라미터 | lowerCamelCase | `eventId`, `userName` |
| Constant (`static final`) | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE`, `INQUIRY` |
| Package | 전부 lowercase | `com.nh.nsight.marketing.eb` |
| Type Parameter | 대문자 1자 또는 PascalCase | `T`, `R`, `E` |
| Boolean method | `is` / `has` / `can` / `should` | `isActive()`, `hasPermission()` |
| Boolean field | 긍정형 형용사/명사 | `active`, `enabled` (not `isActive` 필드 권장) |

### 4.3 Class 명명 — 형식과 계층 접미사

**표준 형식:** `{업무Prefix}{업무대상}{행위/세부?}{역할}`

| 구성 | 설명 | 예 |
|------|------|-----|
| 업무Prefix | 업무코드 PascalCase | `Eb`, `Sv`, `Om` |
| 업무대상 | 도메인 | `Event`, `User`, `Customer` |
| 행위/세부 | 필요 시 | `Summary`, `Publish`, `Inquiry` |
| 역할 | 계층·기술 접미사 | `Handler`, `Service`, `Dao` |

| 역할 | 표준 형식 | 예 (EB/SV) |
|------|-----------|------------|
| Handler | `{Prefix}{Domain}Handler` | `EbEventHandler` |
| Facade | `{Prefix}{Domain}Facade` | `EbEventFacade` |
| Service | `{Prefix}{Domain}Service` | `EbEventService`, `EbEventPublishService` |
| Rule | `{Prefix}{Domain}Rule` | `EbEventRule` |
| DAO | `{Prefix}{Domain}Dao` | `EbEventDao` |
| Mapper | `{Prefix}{Domain}Mapper` | `EbEventMapper` |
| Client | `{Target}Client` | `EpOnlineClient`, `IcIntegrationClient` |
| Config | `{Prefix}{대상}Config` | `EbSchedulerConfiguration` |
| Properties | `{Prefix}{대상}Properties` | `EbEventPublishProperties` |
| Validator | `{Prefix}{대상}Validator` | `SvCustomerValidator` |
| Exception | `{Prefix}{대상}Exception` | `EbBusinessException` |
| Constants | `{Prefix}Constants` | `EbConstants` |
| Scheduler | `{Prefix}{목적}Scheduler` | `EbEventPublishScheduler` |
| Application | `Nsight{Code}ServiceApplication` | `NsightEbServiceApplication` |
| Controller | `{Prefix}{대상}Controller` | `SvOnlineTransactionController` (또는 공통 Online) |

**Handler 운용 기준 (본 레포)**
- **도메인당 Handler 1개** 권장 → `serviceIds()`에 `EB.Event.*` 등 등록
- 가이드 문서의 `{Prefix}{Domain}{행위}Handler` (ServiceId 1:1)도 가능하나, switch가 길어지면 분리

### 4.4 Interface · Enum · Record · Annotation

| 종류 | 규칙 | 예 |
|------|------|-----|
| Interface | PascalCase, 형용사/`able` 또는 역할명. `I` 접두 지양 | `TransactionHandler`, `Readable` |
| Enum | PascalCase 타입 + UPPER_SNAKE 상수 | `enum EbEventStatus { PENDING, SENT, FAILED }` |
| Record | PascalCase, 불변 DTO에 사용 가능 | `ModuleDefinition` |
| Annotation | PascalCase | `@Service`, 커스텀 `@OnlineTx` |
| 구현체 | `Impl` 남용 금지. 필요 시 의미명 | `JdbcEbEventDao` (기술 구분 시) |

### 4.5 Method 명명

**형식:** lowerCamelCase + **동사로 시작** + 목적어

| 처리 | 표준 동사 | 예 |
|------|-----------|-----|
| 단건/상세 조회 | `select` / `selectDetail` | `selectCustomerDetail()` |
| 목록 | `selectList` | `selectEventList()` |
| 요약 | `selectSummary` | `selectCustomerSummary()` |
| 검색 | `search` | `searchCustomers()` |
| 건수 | `count` | `countEventByStatus()` |
| 존재 | `exists` | `existsByUserId()` |
| 등록(업무) | `create` | `createUser()` |
| Insert(DB) | `insert` | `insertEvent()` |
| 수정 | `update` | `updateEventStatus()` |
| 삭제 | `delete` | `deleteByEventId()` |
| 저장(업서트) | `save` | `saveServiceCatalog()` |
| 검증 | `validate` | `validateEventRequest()` |
| 변환 | `to` / `from` | `toCommand()`, `fromRow()` |
| 조립 | `build` | `buildResponse()` |
| 발급/검증(토큰) | `issue` / `verify` | `issueAccessToken()` |
| 발송/실행/재시도 | `send` / `execute` / `retry` | `sendMessage()`, `retryFailed()` |
| 파일 | `upload` / `download` | `uploadFile()` |

**계층별 Method**

| 계층 | 권장 | 금지/지양 |
|------|------|-----------|
| Handler | `handle`, `serviceIds`, Body→DTO 변환 | SQL성 `select*`/`insert*` 직접 |
| Facade | ServiceId와 맞는 `select*`/`create*` 유스케이스 | DB 접근 |
| Service | 업무 동사 `select*`/`create*`/`publish*` | |
| Rule | `validate*`, `check*`, `assert*` | 영속화 |
| DAO/Mapper | `select*`/`insert*`/`update*`/`delete*` — **SQL ID와 동일** | 모호한 `process` |

**지양:** `process()`, `doWork()`, `execute()` 단독, `proc`/`chk`/`sel`/`upd` 약어

### 4.6 Field · 지역변수 · 파라미터 · Constant

| 종류 | 규칙 | 예 |
|------|------|-----|
| Field | 명사, lowerCamelCase | `eventId`, `retryCount`, `createdAt` |
| Collection | 복수형 또는 `*List`/`*Map` | `events`, `eventList`, `statusCountMap` |
| Boolean field | 긍정형 | `enabled`, `deleted` |
| 파라미터 | 의미 있는 이름 (1글자 금지, loop `i` 제외) | `eventInquiryRequest` |
| 지역변수 | Field와 동일 규칙, 축약 최소화 | `publishedCount` |
| Constant | UPPER_SNAKE | `MAX_BATCH_SIZE`, `DEFAULT_PAGE_SIZE` |
| ServiceId 상수 | UPPER_SNAKE 또는 도메인 상수 | `INQUIRY = "EB.Event.inquiry"` |

Header/전문 필드와 맞출 때: `serviceId`, `transactionCode`, `businessCode`, `guid`, `traceId` (camelCase)

### 4.7 DTO 명명 (Request / Response / Command / Result / Criteria / Row)

| 유형 | 형식 | 사용 위치 | 예 |
|------|------|-----------|-----|
| Request | `{Domain}{행위}Request` | 외부 Body | `EventInquiryRequest`, `UserCreateRequest` |
| Response | `{Domain}{행위}Response` | 외부 Body | `EventInquiryResponse` |
| Command | `{Domain}{행위}Command` | Facade→Service 내부 | `EventPublishCommand` |
| Result | `{Domain}{행위}Result` | Service/DAO 결과 | `CustomerSummaryResult` |
| Criteria | `{Domain}SearchCriteria` | 검색 조건 | `EventSearchCriteria` |
| Query | `{Domain}{행위}Query` | Mapper 조회조건 | `EventStatusQuery` |
| Item | `{Domain}Item` | 목록 행 | `EventItem` |
| Row | `{Domain}Row` / `InsertRow` | persistence 매핑 | `EventRow`, `UserInsertRow` |
| Page | `PageRequest` / `PageResponse` | 페이징 | (공통) |
| Summary | `{Domain}StatusSummary` 등 | 집계 | `EventStatusSummary` |

**흐름**

```
StandardRequest<EventInquiryRequest>
  → Handler
  → (Command)
  → Service / Rule / Dao
  → EventRow / Result
  → EventInquiryResponse
  → StandardResponse<EventInquiryResponse>
```

원칙: **외부 DTO(Request/Response)** 와 **내부(Command/Result/Row)** 를 섞지 않는다.

### 4.8 Exception · Logging · 테스트 클래스

| 종류 | 규칙 | 예 |
|------|------|-----|
| Business Exception | `{Prefix}{의미}Exception` | `EbBusinessException` |
| 메시지 | 사용자/운영 분리, 오류코드와 매핑 | `E-EB-BIZ-0001` |
| Logger | `private static final Logger log` | `LoggerFactory.getLogger(EbEventService.class)` |
| Test class | `{대상}Test` / `{대상}IT` | `EbEventServiceTest` |
| Test method | `should{결과}_when{조건}` 또는 `test{행위}` | `shouldReturnEmpty_whenNoEvents()` |

### 4.9 Java 프로그래밍 네이밍 체크리스트
- [ ] Class = PascalCase + 업무 Prefix + 역할 Suffix
- [ ] Method = 동사+목적어, Mapper method = SQL id
- [ ] Field/변수 = camelCase, Constant = UPPER_SNAKE
- [ ] DTO 유형(Request/Response/Command/Row) 분리
- [ ] Boolean은 `is/has/can` 메서드, 긍정형 필드
- [ ] `Impl`/`Mgr`/`Svc`/`process` 남용 없음
- [ ] ServiceId 상수와 Handler `serviceIds()` 일치

### 4.10 EB 코드 대응 예시

| Java | 이름 |
|------|------|
| Handler | `EbEventHandler`, `EbUserHandler` |
| ServiceId 상수 | `INQUIRY = "EB.Event.inquiry"` |
| Facade/Service/Rule | `EbEventFacade` / `EbEventService` / `EbEventRule` |
| DAO/Mapper | `EbEventDao` / `EbEventMapper` |
| DTO | `EventInquiryRequest`, `EventSearchCriteria` |
| Row | `EventRow`, `EventStatusCountRow` |
| Client | `EpOnlineClient` |
| Properties | `EbEventPublishProperties` |

---

## 5. ServiceId · TransactionCode · Header

### 5.1 ServiceId

**형식:** `{업무코드}.{도메인}.{처리행위}`  
(※ 여기서 **도메인 = 업무대상**. 상세는 **3.4 도메인 명명규칙**)

| 구성 | 표기 | 예 |
|------|------|-----|
| 업무코드 | 대문자 2자리 | `EB` |
| 도메인(업무대상) | PascalCase 명사 단수 | `Event`, `User`, `SystemTx` |
| 처리행위 | lowerCamelCase | `inquiry`, `create`, `receive` |

**예시**

| ServiceId | 의미 |
|-----------|------|
| `SV.Customer.selectSummary` | SV 고객 요약 조회 |
| `EB.User.create` / `EB.User.inquiry` | EB 사용자 |
| `EB.Event.inquiry` | EB 이벤트 조회 |
| `EP.UserEvent.receive` | EP 이벤트 수신 |
| `EP.Sample.inquiry` | EP 샘플 |
| `OM.User.inquiry` | OM 사용자 |
| `OM.Dashboard.inquiry` | OM 대시보드 |

**원칙**
1. Context `/eb` 요청이면 ServiceId는 `EB.`로 시작
2. `businessCode`와 Prefix 일치
3. OM Service Catalog 등록 필수 (미등록 실행 금지 권장)
4. 운영 반영 후 변경 금지

### 5.2 TransactionCode

**형식:** `{업무코드}-{거래유형}-{NNNN}`

| 거래유형 | 의미 |
|----------|------|
| INQ | 조회 |
| REG | 등록 |
| UPD | 수정 |
| DEL | 삭제 |
| EXE / PRC | 실행·처리 |
| SND | 발송 |
| ADM | 관리 |

예: `SV-INQ-0001`, `EB-REG-0001`, `EP-EXE-0001`, `OM-ADM-0001`

### 5.3 Header 핵심 항목 (명명)

| 필드 | 용도 |
|------|------|
| businessCode | 업무 |
| serviceId | Handler |
| transactionCode | 거래 식별 |
| guid | 전구간 추적 |
| traceId | 구간/스팬 추적 |
| screenId / menuId | 화면·메뉴 |
| userId / branchId / sessionId | 주체·세션 |

---

## 6. 테이블 · MyBatis · DB 객체 네이밍

> `znsight-man/명명규칙-13-DB-객체` + 본 레포 schema (`EB_*`, `TCF_GATEWAY_*` 등) 기준.

### 6.1 연결선

```
업무코드 / 도메인
  → ServiceId
  → DAO / Mapper
  → Mapper XML / SQL ID
  → Table / Column / Index / Constraint
  → 거래로그 · 감사로그
```

### 6.2 테이블(Table) 네이밍

#### 6.2.1 최상위 원칙

| 원칙 | 기준 | 예 |
|------|------|-----|
| UPPER_SNAKE_CASE | 전부 대문자, 단어는 `_` | `EB_EVENT`, `EVENT_STATUS` |
| Prefix로 영역 식별 | 업무/공통/운영 Prefix 필수 | `EB_`, `TCF_`, `OM_` |
| 도메인과 대응 | 업무대상(Domain)이 이름에 드러남 | `Event` → `EB_EVENT` |
| 의미 우선 | 과도한 축약 금지 | `CUSTOMER_NO` (O) / `CUST`만 (X) |
| Java 매핑 가능 | underscore → camelCase | `EVENT_STATUS` → `eventStatus` |
| 임시·개인명 금지 | | `TMP1`, `TEST_TABLE`, `KIM_*` (X) |

#### 6.2.2 테이블 Prefix 표준

| Prefix | 영역 | 예 |
|--------|------|-----|
| `TCF_` | TCF 실행·통제·거래로그 | `TCF_TX_LOG`, `TCF_TRANSACTION_CONTROL`, `TCF_SERVICE_TIMEOUT_POLICY` |
| `TCF_GATEWAY_` | Gateway | `TCF_GATEWAY_ROUTE`, `TCF_GATEWAY_TX_LOG` |
| `TCF_JWT_` / `JWT_` | JWT | `TCF_JWT_TOKEN`, `TCF_REFRESH_TOKEN` |
| `TCF_USER_SESSION` / `SPRING_SESSION` | 세션 | `TCF_USER_SESSION` |
| `OM_` | 운영관리 | `OM_USER`, `OM_ERROR_CODE`, `OM_SERVICE_CATALOG`, `OM_MENU` |
| `UD_` | 업다운로드 | `UD_FILE_META` |
| `BAT_` / `OM_BATCH_` | 배치 | `OM_BATCH_JOB` |
| `{업무코드}_` | 업무 데이터 | `EB_EVENT`, `EP_USER_EVENT`, `SV_CUSTOMER_SUMMARY`, `IC_…` |

#### 6.2.3 테이블명 형식

**기본:** `{Prefix}_{업무대상}_{데이터성격?}`

| 성격 Suffix | 용도 | 예 |
|-------------|------|-----|
| (없음) / 단순명 | 핵심 엔티티 | `EB_USER`, `EB_EVENT`, `OM_USER` |
| `_MASTER` | 마스터 | `CM_CAMPAIGN_MASTER` |
| `_DETAIL` | 상세 | `CM_CAMPAIGN_DETAIL` |
| `_HISTORY` | 이력 | `OM_DEPLOY_HISTORY` |
| `_LOG` | 로그 | `TCF_TX_LOG`, `MG_MESSAGE_SEND_LOG` |
| `_MAP` / `_MAPPING` | 매핑 | `CM_CAMPAIGN_CHANNEL_MAP` |
| `_CONTROL` / `_POLICY` | 통제·정책 | `TCF_TRANSACTION_CONTROL` |
| `_STATUS` | 상태 스냅샷 | `OM_AP_STATUS` (운영) |

**업무 테이블 변환**

| 업무 | 도메인 | 테이블 |
|------|--------|--------|
| EB | User | `EB_USER` |
| EB | Event | `EB_EVENT` |
| EB | SystemTx | `EB_SYSTEM_TX` |
| EP | UserEvent | `EP_USER_EVENT` |
| SV | Customer Summary | `SV_CUSTOMER_SUMMARY` |
| Gateway | Route | `TCF_GATEWAY_ROUTE` |

#### 6.2.4 본 레포·표준 주요 테이블

| 목적 | 테이블명 |
|------|----------|
| Gateway 라우팅 | `TCF_GATEWAY_ROUTE` |
| Gateway 중계 로그 | `TCF_GATEWAY_TX_LOG` |
| 사용자 세션 | `TCF_USER_SESSION` |
| 거래로그 | `TCF_TX_LOG` (모듈별 명칭 변형 가능) |
| 거래통제 | `TCF_TRANSACTION_CONTROL` |
| Timeout 정책 | `TCF_SERVICE_TIMEOUT_POLICY` |
| ServiceId 카탈로그 | `OM_SERVICE_CATALOG` |
| 오류코드 | `OM_ERROR_CODE` |
| OM 사용자/권한/메뉴 | `OM_USER`, `OM_AUTH_GROUP`, `OM_MENU` |
| EB 이벤트/사용자 | `EB_EVENT`, `EB_USER`, `EB_SYSTEM_TX` |
| EP 수신 이벤트 | `EP_USER_EVENT` |

---

### 6.3 컬럼(Column) 네이밍

#### 6.3.1 Suffix 표준

| Suffix | 의미 | 예 |
|--------|------|-----|
| `_ID` | 식별자 | `USER_ID`, `EVENT_ID`, `ROUTE_ID` |
| `_CODE` | 코드 | `BUSINESS_CODE`, `ENV_CODE` |
| `_NAME` | 명칭 | `USER_NAME`, `BUSINESS_NAME` |
| `_YN` | Y/N 여부 | `USE_YN`, `DEL_YN` |
| `_STATUS` | 상태 | `EVENT_STATUS`, `RESULT_STATUS` |
| `_TYPE` | 유형 | `EVENT_TYPE`, `SESSION_TYPE` |
| `_DTM` / `_AT` | 일시 | `CREATED_AT`, `CREATED_DTM` (레포는 `_AT`/`TIMESTAMP` 혼용) |
| `_DATE` | 일자 | `TX_DATE`, `BASE_DATE` |
| `_TIME` | 시각 | `TX_TIME`, `LOGIN_TIME` |
| `_CNT` / `_COUNT` | 건수 | `RETRY_COUNT`, `TOTAL_CNT` |
| `_SEQ` / `_NO` | 순번/번호 | `TX_SEQ_NO`, `SORT_ORDER` |
| `_AMT` | 금액 | `BALANCE_AMT` |
| `_MS` | 밀리초 | `ELAPSED_TIME_MS`, `CONNECT_TIMEOUT_MS` |
| `_URL` / `_PATH` | URL/경로 | `TARGET_BASE_URL`, `CONTEXT_PATH` |

#### 6.3.2 공통·추적 컬럼

| 컬럼 | 용도 |
|------|------|
| `CREATED_AT` / `CREATED_DTM`, `CREATED_BY` | 등록 |
| `UPDATED_AT` / `UPDATED_DTM`, `UPDATED_BY` | 수정 |
| `USE_YN`, `DEL_YN` | 사용·논리삭제 |
| `SORT_ORDER` | 정렬 |
| `DESCRIPTION` / `REMARK` | 설명 |
| `GUID`, `TRACE_ID` | 거래 추적 |
| `SERVICE_ID`, `TRANSACTION_CODE`, `BUSINESS_CODE` | 거래 식별 |
| `USER_ID`, `BRANCH_ID`, `SESSION_ID` | 주체·세션 |

#### 6.3.3 Java / MyBatis 매핑

| DB 컬럼 | Java field (Row/DTO) |
|---------|----------------------|
| `EVENT_STATUS` | `eventStatus` |
| `RETRY_COUNT` | `retryCount` |
| `CREATED_AT` | `createdAt` |
| `SERVICE_ID` | `serviceId` |

설정: `map-underscore-to-camel-case: true` (관례)

---

### 6.4 PK · UK · FK · Index · Check · Sequence

| 객체 | 형식 | 예 |
|------|------|-----|
| Primary Key | `PK_{TABLE}` | `PK_TCF_GATEWAY_ROUTE`, `PK_EB_EVENT` |
| Unique Key | `UK_{TABLE}_{NN}` | `UK_TCF_GATEWAY_ROUTE_01` (ENV+BUSINESS) |
| Foreign Key | `FK_{자식}_{부모약어}_{NN}` | `FK_OM_FUNCTION_AUTH_MENU_01` |
| Index | `IX_{TABLE}_{NN}` 또는 `IDX_{TABLE}_{컬럼약어}` | `IX_EB_EVENT_STATUS`, `IDX_TCF_TX_LOG_GUID` |
| Check | `CK_{TABLE}_{COLUMN}` | `CK_TCF_GATEWAY_ROUTE_ENV` |
| Sequence | `SEQ_{TABLE}` | `SEQ_OM_AUDIT_LOG` |

**인덱스 권장**
- 거래로그: `GUID`, `SERVICE_ID` + 시간, `USER_ID` + 시간
- Gateway 라우트: `(ENV_CODE, BUSINESS_CODE, USE_YN)`
- 업무 상태 조회: `(EVENT_STATUS, CREATED_AT)` 등 실제 WHERE 기준

**FK**
- 기준정보·운영 테이블: 물리 FK 권장
- 대량 로그/이력: 논리 FK(컬럼만) 허용 가능

---

### 6.5 Mapper · XML · SQL ID (테이블과 연결)

| 항목 | 규칙 | 예 |
|------|------|-----|
| Mapper 인터페이스 | `{Prefix}{Domain}Mapper` | `EbEventMapper` → `EB_EVENT` |
| XML 경로 | `mapper/{code}/` | `mapper/eb/EbEventMapper.xml` |
| XML namespace | Mapper FQCN | |
| method ≈ SQL id | 동일 | `selectEventList` |
| SQL 관리 ID(선택) | `{업무}-{세구분}-{유형}-{NNNN}` | `EB-EVT-SEL-001` |

SQL 유형 약어 예: `SEL` 조회, `INS` 등록, `UPD` 수정, `DEL` 삭제

---

### 6.6 persistence Row · 테이블 대응

| Java | Package | 테이블 |
|------|---------|--------|
| `EventRow` | `…persistence.dto.event` | `EB_EVENT` |
| `EventInsertRow` | 동일 | `EB_EVENT` insert용 |
| `UserRow` | `…persistence.dto.user` | `EB_USER` |
| `EventStatusCountRow` | 집계 결과 DTO | (조인/그룹 결과, 물리 테이블 아님) |

원칙: **테이블 1 : Mapper 1(도메인)** 권장. 과도하게 커지면 도메인 분리.

---

### 6.7 테이블 네이밍 체크리스트
- [ ] UPPER_SNAKE + 표준 Prefix인가?
- [ ] 도메인/업무코드와 이름이 대응되는가?
- [ ] PK/UK/Index 명이 형식에 맞는가?
- [ ] 여부 컬럼은 `_YN`, 식별자는 `_ID`인가?
- [ ] GUID/SERVICE_ID 등 추적 컬럼이 로그성 테이블에 있는가?
- [ ] Java Row field와 camelCase 매핑이 되는가?
- [ ] `TMP_*` / 개인명 테이블이 없는가?

---

## 7. 화면 · UI · 이벤트

### 7.1 화면 ID

**권장 형식:** `{업무코드}-{세구분}-{NNNN}`  
예: `SV-CUS-0001`, `EB-EVT-0001`, `OM-DSH-0001`

### 7.2 화면 이벤트 ID

**형식:** `{화면ID}-E{NN}`  
예: `SV-CUS-0001-E01` (조회 버튼)

### 7.3 정적 화면 경로 (tcf-ui)

```
tcf-ui/src/main/resources/static/
├── {code}/                 # sv/, eb/, pc/ …
├── om/admin/               # OM 관리
├── jwt/admin/
└── _shared/
```

파일: `kebab-case` 또는 도메인 의미명 (`login.html`, `dashboard.html`)

### 7.4 화면 ↔ ServiceId
- 화면 1개 : 대표 ServiceId 1개 이상
- OM 매핑 테이블(예: `OM_SCREEN_SERVICE_MAP`)로 관리 권장

---

## 8. 오류코드 · 로그 · Cache · Batch · Gateway

### 8.1 오류코드

**형식:** `E-{DOMAIN}-{CATEGORY}-{NNNN}`

| DOMAIN 예 | CATEGORY 예 |
|-----------|-------------|
| TCF, GW, JWT, OM, UD, BT | HDR, VAL, AUTHN, AUTHZ, BIZ, DB, TIME, IF, FILE, SVC, CTL |
| SV, EB, EP, … (업무) | BIZ, VAL, DB |

예: `E-TCF-HDR-0001`, `E-SV-BIZ-0001`, `E-GW-IF-0001`, `E-JWT-AUTHN-0001`

- 저장: `OM_ERROR_CODE`
- 변환: ETF에서 표준 오류코드로 일원화
- `resultCode`(응답 결과) ≠ `errorCode`(상세 원인)

### 8.2 로그 식별 키 (필수 남김)
`guid`, `traceId`, `businessCode`, `serviceId`, `transactionCode`, `userId` (가능 시)

### 8.3 Cache
- 이름: `{영역}:{대상}` 또는 업무 Prefix  
  예: `om:commonCode`, `tcf:serviceCatalog`

### 8.4 Batch / Scheduler
- Job/Bean: `{Prefix}{목적}Scheduler` / `{Prefix}{목적}Job`  
  예: `EbEventPublishScheduler`
- ServiceId(배치 거래): `EB.Batch.inquiry` 등

### 8.5 Gateway
- 테이블: `TCF_GATEWAY_ROUTE`
- 키: `ENV_CODE` + `BUSINESS_CODE`
- ENV: `LOCAL` / `DEV` / `PRD`
- Target: `TARGET_BASE_URL + CONTEXT_PATH + ONLINE_PATH`

---

## 9. Docker · Kubernetes · 환경 변수 명명

### 9.1 Docker

| 항목 | 형식 | 예 |
|------|------|-----|
| Image | `nsight-{short}:tag` | `nsight-eb:local`, `nsight-ui:local` |
| Container | `nsight-{short}` | `nsight-sv` |
| Volume | `nsight-{short}-data` | `nsight-eb-data` |
| Compose name | `nsight-{short}` | `nsight-ui` |
| GHCR(배포) | `ghcr.io/.../nsight-tcf-framework/{module}:{sha}` | `.../eb-service:abc1234` |

### 9.2 Kubernetes (`nsight` namespace)

| 리소스 | 형식 | 예 |
|--------|------|-----|
| Deployment/Service | `nsight-{short}` | `nsight-eb` |
| ConfigMap | `nsight-{short}-config` | `nsight-eb-config` |
| Secret | `nsight-{short}-secret` | `nsight-eb-secret` |
| Label app | `nsight-{short}` | `app: nsight-eb` |
| Container | `{module}` | `eb-service`, `tcf-ui` |

### 9.3 환경 변수
- Spring: `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`
- NSIGHT: `NSIGHT_{영역}_{항목}`  
  예: `NSIGHT_EB_DB_URL`, `NSIGHT_TXLOG_PATH`, `NSIGHT_INTEGRATION_SERVICES_IC_BASE_URL`

### 9.4 Profile
| Profile | 용도 |
|---------|------|
| local | 로컬 H2 / bootRun |
| dev | 개발 |
| prod | 운영 |

---

## 10. End-to-End 네이밍 예시

### 10.1 EB 이벤트 조회

| 층 | 이름 |
|----|------|
| 업무코드 | `EB` |
| URL | `POST /eb/online` |
| ServiceId | `EB.Event.inquiry` |
| TxCode | `EB-INQ-0001` |
| Handler | `EbEventHandler` |
| Facade/Service/Rule | `EbEventFacade` / `EbEventService` / `EbEventRule` |
| DAO/Mapper | `EbEventDao` / `EbEventMapper` |
| Table | `EB_EVENT` |
| 오류(업무) | `E-EB-BIZ-0001` |

### 10.2 SV → IC 연계 조회

| 층 | 이름 |
|----|------|
| 진입 | `POST /sv/online`, `SV.Customer.selectSummary` |
| Client | `IcIntegrationClient` → `POST /ic/online` |
| IC ServiceId | `IC.Customer.…` |
| 추적 | 동일 `guid` / 연계 `traceId` |

### 10.3 EB → EP 이벤트 발행

| 층 | 이름 |
|----|------|
| Outbox | `EB_EVENT.EVENT_STATUS` |
| Scheduler | `EbEventPublishScheduler` / `EbEventPublishService` |
| Client | `EpOnlineClient` |
| EP | `EP.UserEvent.receive` → `EP_USER_EVENT` |

### 10.4 추적 경로 (장애 시)

```
화면 / UI
  → screenId + ServiceId
  → Gateway (BUSINESS_CODE, TARGET_URL)
  → WAR /online
  → Handler (serviceId)
  → Mapper/SQL/Table
  → TxLog(guid, traceId) / errorCode
```

---

## 11. 체크리스트 · 금지어 · 결론

### 11.1 신규 거래 추가 체크리스트
- [ ] 도메인 = 명사 단수 PascalCase, ServiceId 2번째와 일치 (3.4)
- [ ] 업무코드·Context·Module·Package Root 일치
- [ ] Package 계층(`entry`/`application`/`persistence`)·의존 방향 준수
- [ ] ServiceId = `{CODE}.{Domain}.{action}` + OM 등록
- [ ] TransactionCode = `{CODE}-{TYPE}-{NNNN}` 매핑
- [ ] Handler 1개/도메인 + `serviceIds()` 등록
- [ ] Java Class/Method/Field/DTO 표기법 준수 (4장)
- [ ] Facade/Service/Rule/DAO/Mapper 접미사 준수
- [ ] 테이블 Prefix·UPPER_SNAKE·도메인 대응 (6장)
- [ ] 컬럼 Suffix(`_ID`/`_YN`/`_STATUS`…)·PK/Index 명 규칙
- [ ] Mapper method명 = SQL id, XML은 `mapper/{code}/`
- [ ] Row ↔ 테이블 매핑 (persistence.dto)
- [ ] 오류코드 `E-{DOMAIN}-{CATEGORY}-{NNNN}` (필요 시 OM 등록)
- [ ] sample-requests / 화면 이벤트 ID 갱신

### 11.2 금지어·지양
| 지양 | 이유 |
|------|------|
| `Controller1`, `TestService`, `CommonUtil2` | 업무·역할 불명 |
| `process`, `execute`, `doWork` 단독 | 행위 불명 |
| `proc`, `chk`, `sel`, `upd`, `Svc`, `Mgr` | 약어 비표준 |
| `I` 접두 Interface (`IService`) | Java 관례상 불필요 |
| `*Impl` 남발 | 구현 의미 모호 |
| Handler에서 `select*`/`insert*` | 계층 책임 위반 |
| Request와 Row를 같은 타입으로 혼용 | 외부/내부 DTO 혼선 |
| `TB001`, `screen01` | 의미 없는 번호 |
| Context와 다른 ServiceId Prefix | `/sv`인데 `EB.` |
| 운영 중 ServiceId/TxCode 임의 변경 | 로그·통제 단절 |

### 11.3 결론
- 명명규칙은 코딩 스타일이 아니라 **업무 의미를 코드·운영에 전달하는 공통 언어**다.
- 이름만으로 업무·기능·계층·테이블·오류 위치를 추정할 수 있어야 한다.
- 상세 조항은 `znsight-man/명명규칙-01`~`21` 및 제11부 집필본을 따른다.

---

**문서 ID**: ARC05  
**문서 버전**: 1.0  
**작성일**: 2026-07-19  
**대상**: NSIGHT TCF Framework (`nsight-tcf-framework`)  
**관련**: ARC02 / ARC03 / ARC04 · `znsight-man/명명규칙-*` · 제11부 명명규칙
