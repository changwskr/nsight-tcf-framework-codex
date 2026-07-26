# NSIGHT TCF Framework - 애플리케이션 구성도

## 📋 애플리케이션 구성도 개요

**NSIGHT TCF Framework**의 애플리케이션 구성도는 **Spring Boot 기반의 공통 거래 처리 프레임워크(TCF)** 와 **업무별 마이크로서비스(WAR)** 구조로 설계되었습니다.  
화면(`tcf-ui`/`tcf-uj`) → Gateway(`tcf-gateway`) → 업무 WAR → TCF 6계층(Handler~Mapper) → DB 순으로 요청이 처리되며, 각 모듈은 독립적으로 개발·배포·운영이 가능하도록 구성되어 있습니다.

핵심 원칙:

- 업무 개발자는 **업무 기능**을 구현하고, TCF는 **거래 공통 절차**(인증 연계, ServiceId 라우팅, Timeout, 표준 응답, 거래로그)를 책임진다.
- Gateway는 `TCF_GATEWAY_ROUTE` 기준으로 `ENV_CODE + BUSINESS_CODE` 라우팅만 수행한다.
- 업무 내부는 **Handler → Facade → Service → Rule → DAO → Mapper** 6계층을 표준으로 한다.

---

## 🏗️ 전체 애플리케이션 구성도

### 📊 애플리케이션 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           NSIGHT TCF Framework                                  │
│                              애플리케이션 구성도                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│  🌐 클라이언트 레이어 (Client Layer)                                             │
│  ├── 웹 브라우저 (tcf-ui 정적 UI / Relay)                                       │
│  ├── 통합 시험 UI (tcf-uj)                                                      │
│  ├── OM 관리 콘솔 (tcf-om / tcf-ui /om)                                        │
│  └── Gateway 라우트 관리 화면 (tcf-gateway admin)                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│  🔒 보안·게이트웨이 레이어 (Security / Gateway Layer)                           │
│  ├── tcf-gateway (업무코드별 Relay / TCF_GATEWAY_ROUTE)                         │
│  ├── tcf-jwt (JWT 발급·검증)                                                    │
│  ├── 세션·헤더 규칙 (Gateway Session Header Rules)                              │
│  └── 보안 정책 / Timeout 정책 (OM·TCF Core)                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│  🎯 프레젠테이션 레이어 (Presentation Layer)                                    │
│  ├── OnlineTransactionController (공통 온라인 진입)                             │
│  ├── ProxyController (Gateway 업무코드별)                                       │
│  ├── UI Relay / 정적 리소스 (tcf-ui static HTML/JS/CSS)                          │
│  └── Admin REST API (routes, OM, JWT 관리)                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│  🏦 업무 서비스 레이어 (Business Service Layer / WAR)                           │
│  ├── 고객계: IC / PC / BC / MS                                                  │
│  ├── 마케팅계: SV / PD / EB / CM                                                │
│  ├── 실시간·이벤트: EP / BP                                                     │
│  ├── 지원·운영: SS / MG / OM / OC / UD                                          │
│  └── 공통·인증: CC / JWT                                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│  🔄 TCF 비즈니스 로직 레이어 (TCF 6계층)                                        │
│  ├── Handler (ServiceId 진입)                                                   │
│  ├── Facade (유스케이스 조합)                                                   │
│  ├── Service (업무 처리)                                                        │
│  ├── Rule (업무 규칙·검증)                                                      │
│  └── STF / ETF (표준 요청 검사·응답·거래로그)                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│  📊 데이터 액세스 레이어 (Data Access Layer)                                    │
│  ├── DAO (Data Access Objects)                                                  │
│  ├── MyBatis Mapper (+ XML)                                                     │
│  ├── tcf-cache (캐시 지원)                                                      │
│  └── Persistence DTO / Row                                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│  🗄️ 데이터 레이어 (Data Layer)                                                 │
│  ├── Oracle (운영/개발 DB)                                                      │
│  ├── H2 (로컬·테스트 / gateway-route, txlog)                                    │
│  ├── 거래로그 저장소 (nsight-txlog)                                             │
│  └── 파일·업다운로드 저장소 (data/updownload)                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│  🔗 통합 레이어 (Integration Layer)                                             │
│  ├── tcf-eai / Integration Client (업무 간 HTTP 연동)                           │
│  ├── EpOnlineClient 등 서비스 간 Client                                         │
│  ├── Outbox / Scheduler 이벤트 발행 (EB→EP 등)                                  │
│  └── Gateway Relay (GEF)                                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│  🛠️ 인프라 레이어 (Infrastructure Layer)                                       │
│  ├── Spring Boot Embedded Tomcat / 외부 Tomcat (WAR)                            │
│  ├── Docker (tcf-docker / *-service/scripts/docker)                             │
│  ├── Kubernetes (tcf-k8s)                                                       │
│  └── CI/CD 설정 (tcf-cicd: local / dev / prod)                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🌐 클라이언트 레이어 (Client Layer)

### 🖥️ 웹 브라우저 / tcf-ui
- **기술**: HTML5, CSS3, JavaScript (정적 UI), Spring Boot Relay
- **포트(local)**: `8099`
- **기능**:
  - 업무 화면 제공 (`/sv`, `/eb`, `/om` 등)
  - Gateway·업무 WAR로 요청 Relay
  - 공통 스크립트·허브 UI (`static/_shared`)

### 🧪 통합 시험 UI / tcf-uj
- **기술**: Spring Boot, 정적 HTML, sample-requests JSON
- **기능**:
  - 업무별 샘플 거래 호출
  - ServiceId·요청 바디 시험
  - 업다운로드·다중 거래 시나리오 검증

### 🖥️ OM 관리 콘솔
- **기술**: `tcf-om` + `tcf-ui` `/om/admin`
- **포트(local OM)**: `8097`
- **기능**:
  - 운영관리·거래통제·오류코드
  - 인증 이력·Timeout 정책
  - 파일 업다운로드(UD) 관리

### 🛠️ Gateway 라우트 관리
- **기술**: `tcf-gateway` Admin UI / REST
- **기능**:
  - `TCF_GATEWAY_ROUTE` 조회·등록
  - ENV_CODE(LOCAL/DEV/PRD)별 Target URL 관리
  - 헬스체크·Timeout 설정 확인

---

## 🔒 보안·게이트웨이 레이어 (Security / Gateway Layer)

### 🌐 tcf-gateway
- **기술**: Spring Boot, RestClient Relay, H2/Oracle 라우트 저장
- **기능**:
  - `POST /{businessCode}/online` 프록시
  - `TCF_GATEWAY_ROUTE`로 Target URL 조립  
    `TARGET_BASE_URL + CONTEXT_PATH + ONLINE_PATH`
  - Connect/Read Timeout 적용
  - 미등록 업무코드 → HTTP 404 + 안내 JSON

### 🔐 tcf-jwt
- **기술**: JWT, Spring Boot
- **포트(local)**: `8110`
- **기능**:
  - 토큰 발급·검증
  - Gateway/업무와 연동되는 인증 경계
  - 보안 정책 화면·API 지원

### 🛡️ 세션·권한·정책
- **기술**: Gateway Session Header Rules, OM Timeout/Auth Policy
- **기능**:
  - 요청 헤더·세션 규칙 적용
  - 역할/권한 기반 접근 통제(운영 정책)
  - Timeout·거래통제 정책 반영

---

## 🎯 프레젠테이션 레이어 (Presentation Layer)

### 🌐 공통 온라인 컨트롤러
- **기술**: Spring MVC (`tcf-web` / 업무 WAR)
- **구성**:
  ```
  OnlineTransactionController          # /{ctx}/online 공통 진입
  STF (Standard Transaction Framework) # 요청 검사·거래 시작
  ETF                                  # 표준 응답·거래로그 마무리
  ```

### 🔌 Gateway Proxy Controllers
- **기술**: Spring Web
- **구성**:
  ```
  tcf-gateway/.../entry/web/
  ├── CcProxyController.java           # /cc/online
  ├── IcProxyController.java           # /ic/online
  ├── SvProxyController.java           # /sv/online
  ├── EbProxyController.java           # /eb/online
  ├── ...ProxyController.java          # 업무코드별
  └── JwtProxyController.java          # JWT 연계
  ```

### 📁 정적 UI 리소스 (tcf-ui)
- **기술**: HTML / CSS / JS
- **구성**:
  ```
  tcf-ui/src/main/resources/static/
  ├── sv/                              # Single View
  ├── eb/                              # EBM
  ├── pc/                              # Private Customer
  ├── om/admin/                        # 운영관리 화면
  ├── oc/                              # 용량·환경
  ├── jwt/admin/                       # JWT 관리
  └── _shared/                         # 공통 스크립트
  ```

### 🔌 Admin REST API
- **기술**: Spring Web
- **예시**:
  - Gateway: `/api/admin/routes`
  - OM / JWT 관리 API
  - Actuator Health: `/actuator/health`

---

## 🏦 업무 서비스 레이어 (Business Service Layer)

LOCAL bootRun 포트 기준 (`BusinessModuleDefinitions` / Gateway 라우팅 테이블):

| 코드 | 모듈 | 그룹 | Port | 모듈 경로 |
|------|------|------|------|-----------|
| CC | Common | 공통 | 8081 | (공통) |
| IC | Integration Customer | 고객 | 8082 | `ic-service` |
| PC | Private Customer | 고객 | 8083 | `pc-service` |
| BC | Business Customer | 고객 | 8084 | (연계) |
| MS | Mini Single View | 고객 | 8085 | `ms-service` |
| SV | Single View | 마케팅 | 8086 | `sv-service` |
| PD | Product | 마케팅 | 8087 | `pd-service` |
| CM | Campaign | 마케팅 | 8088 | (연계) |
| EB | EBM | 마케팅 | 8089 | `eb-service` |
| EP | Event Processing | 실시간 | 8090 | `ep-service` |
| BP | Behavior Processing | 실시간 | 8091 | (연계) |
| BD | Behavior Data | 데이터 | 8092 | (연계) |
| SS | Sales Support | 지원 | 8093 | `ss-service` |
| OC | Operation Capacity | 운영 | 8094 | `tcf-oc` |
| MG | Message | 지원 | 8096 | `mg-service` |
| OM / UD | Operation / UpDownload | 운영·공통 | 8097 | `tcf-om` / `om-service` |
| JWT | JWT Auth | 인증 | 8110 | `tcf-jwt` |
| UI | tcf-ui | 화면 | 8099 | `tcf-ui` |

### 📌 예시: EB (EBM) 서비스
- **기술**: Spring Boot WAR, TCF 6계층, MyBatis, Scheduler
- **구성**:
  ```
  com.nh.nsight.marketing.eb/
  ├── entry/handler/                   # EbEventHandler, EbUserHandler, ...
  ├── entry/facade/                    # EbEventFacade, EbUserFacade, ...
  ├── application/service/             # EbEventService, EbUserService, ...
  ├── application/rule/                # EbEventRule, EbUserRule, ...
  ├── persistence/dao|mapper/          # DAO + MyBatis Mapper
  ├── client/                          # EpOnlineClient (EP 연동)
  └── application/scheduler/           # Outbox 이벤트 발행
  ```

### 📌 예시: SV (Single View) 서비스
- **기술**: Spring Boot WAR, TCF, Integration Client(IC)
- **구성**:
  ```
  com.nh.nsight.marketing.sv/
  ├── entry/controller|handler|facade
  ├── application/service|rule
  ├── persistence/dao|mapper
  └── client/IcIntegrationClient       # IC 연계
  ```

### 📌 공통 프레임워크 모듈
| 모듈 | 역할 |
|------|------|
| `tcf-core` | 거래 런타임, Timeout, 멱등, 로깅 모델 |
| `tcf-web` | Online 컨트롤러·웹 자동구성 |
| `tcf-util` | 공통 유틸·헤더 규칙 |
| `tcf-eai` | 통합 연동 설정·클라이언트 |
| `tcf-cache` | 캐시 지원 |
| `tcf-batch` | 배치·대시보드 수집 |
| `tcf-help` | 도움말/문서 export |

---

## 🔄 TCF 비즈니스 로직 레이어 (TCF 6계층)

### Handler
- **역할**: ServiceId로 실행 기능 진입
- **예**: `EbEventHandler`, `SvSampleHandler`

### Facade
- **역할**: 유스케이스 단위 조합, 트랜잭션 경계
- **예**: `EbEventFacade`, `EbUserFacade`

### Service
- **역할**: 업무 처리 본체
- **예**: `EbEventService`, `EbEventPublishService`

### Rule
- **역할**: 업무 규칙·입력 검증
- **예**: `EbEventRule`, `EbUserRule`

### STF / ETF
- **역할**:
  - STF: 요청 검사, GUID/TraceId, 거래 시작
  - ETF: 표준 응답, 거래로그 마무리, 오류 매핑

### 이벤트·스케줄 처리
- **기술**: Spring Scheduler, Outbox 패턴
- **예**: EB 이벤트 → EP `/ep/online` 발행

---

## 📊 데이터 액세스 레이어 (Data Access Layer)

### DAO
- **기술**: DAO Pattern + MyBatis
- **구성 예 (EB)**:
  ```
  persistence/dao/
  ├── EbEventDao.java
  ├── EbUserDao.java
  ├── EbSampleDao.java
  └── EbSystemTxDao.java
  ```

### MyBatis Mapper
- **기술**: MyBatis + XML
- **구성 예**:
  ```
  persistence/mapper/ + resources/mapper/eb/
  ├── EbEventMapper.java / EbEventMapper.xml
  ├── EbUserMapper.java / EbUserMapper.xml
  └── EbSampleMapper.java / EbSampleMapper.xml
  ```

### Persistence DTO / Row
- **구성 예**:
  ```
  persistence/dto/
  ├── event/EventRow, EventInsertRow, EventStatusCountRow
  ├── user/UserRow, UserInsertRow
  └── sample/SampleRow
  ```

### 캐시
- **기술**: `tcf-cache`, Spring Cache (환경별)
- **용도**: 공통코드·라우트·세션성 데이터 캐싱

---

## 🗄️ 데이터 레이어 (Data Layer)

### 관계형 DB
- **기술**: Oracle (dev/prod), H2 (local/test)
- **주요 테이블 예**:
  ```
  TCF_GATEWAY_ROUTE                    # Gateway 라우팅
  (업무별) EB_* / SV_* / EP_* ...      # 업무 테이블
  (OM) 오류코드·Timeout·인증 이력 등
  ```

### 인메모리 / 로컬 파일 DB
- **기술**: H2 file/mem
- **용도**:
  - 로컬 Gateway 라우트 (`data/gateway-route*.db`)
  - 로컬 거래로그 (`nsight-txlog`)
  - 단위·통합 테스트

### 파일 저장소
- **경로 예**: `data/updownload/`
- **용도**: 업다운로드 바이너리, 배치/파일 연계

---

## 🔗 통합 레이어 (Integration Layer)

### 업무 간 HTTP 연동
- **기술**: `tcf-eai`, RestClient/WebClient 스타일 Client
- **구성 예**:
  ```
  sv-service → IcIntegrationClient → IC
  eb-service → EpOnlineClient      → EP
  tcf-gateway → 각 업무 /{code}/online Relay
  ```

### Gateway Relay (GEF)
- **기술**: GatewayRouteDispatcher + RestClient
- **흐름**:
  ```
  UI/Client → tcf-gateway → TCF_GATEWAY_ROUTE 조회
           → Target URL 조립 → 업무 WAR /online
  ```

### 이벤트·스케줄
- **기술**: Spring `@Scheduled`, Outbox
- **예**: EB Event Publish → EP Online

---

## 🛠️ 인프라 레이어 (Infrastructure Layer)

### 애플리케이션 런타임
- **기술**: Spring Boot Embedded Tomcat, 외부 Apache Tomcat (WAR 배포)
- **기능**: 온라인 거래 처리, Actuator Health

### Docker
- **위치**:
  - `*-service/scripts/docker/` (예: eb, sv, tcf-ui)
  - `tcf-docker/<module>/` (중앙 디렉터리)
- **기능**: 이미지 빌드/실행, 포트 매핑(`0.0.0.0:HOST:CONTAINER`)

### Kubernetes
- **위치**: `tcf-k8s/scripts/deploy.sh`, `eb-service/scripts/k8s/`
- **기능**: Namespace `nsight`, Deployment/Service/ConfigMap/Secret, Ingress

### CI/CD 설정
- **위치**: `tcf-cicd/{local,dev,prod}/spring/...`
- **기능**: 환경별 `application-*.yml` 관리

### 모니터링
- **기술**: Spring Actuator Health, OM 관측성, 거래로그(GUID/TraceId/ServiceId)
- **기능**: 헬스체크, 거래 추적, 장애 진단

---

## 🔄 컴포넌트 간 상호작용

### 📡 컴포넌트 상호작용 다이어그램

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  tcf-ui /   │    │ tcf-gateway │    │  업무 WAR   │
│  tcf-uj     │───▶│  (+ JWT)    │───▶│ /xx/online  │
└─────────────┘    └─────────────┘    └─────────────┘
                           │                   │
                           ▼                   ▼
                   ┌─────────────┐    ┌─────────────┐
                   │ TCF_GATEWAY │    │ OnlineTxn   │
                   │   _ROUTE    │    │ Controller  │
                   └─────────────┘    └─────────────┘
                                              │
                                              ▼
                   ┌─────────────────────────────────┐
                   │ Handler→Facade→Service→Rule     │
                   │            →DAO→Mapper          │
                   └─────────────────────────────────┘
                                              │
                                              ▼
                                       ┌─────────────┐
                                       │ Oracle / H2 │
                                       └─────────────┘
```

### 🔄 데이터 플로우

#### 1. 온라인 거래 요청 플로우
```
1. 브라우저(tcf-ui) 요청
2. (선택) Gateway → JWT/세션 검증
3. Gateway → TCF_GATEWAY_ROUTE로 Target 조립
4. 업무 WAR OnlineTransactionController 진입
5. STF → ServiceId로 Handler 선택
6. Handler → Facade → Service → Rule → DAO → Mapper
7. ETF → 표준 응답 + 거래로그
8. 클라이언트 응답
```

#### 2. 업무 간 연계 플로우 (예: EB → EP)
```
1. EB 거래/스케줄러가 Outbox 이벤트 적재
2. EbEventPublishService 가 EP Online 호출
3. EP Handler/Service 가 이벤트 처리
4. 결과/상태 EB 측에 반영(또는 로그)
```

#### 3. 인증 처리 플로우
```
1. 로그인/토큰 요청 → tcf-jwt
2. JWT 발급
3. 이후 요청 → Gateway/업무에서 토큰·세션 검증
4. 유효 시 업무 라우팅 허용
```

---

## 📊 배포 구성도

### 🐳 컨테이너 배포 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                 NSIGHT TCF Docker Architecture                  │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │  nsight-ui  │  │  nsight-sv  │  │  nsight-eb  │  ...        │
│  │   :8099     │  │   :8086     │  │   :8089     │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│         │                │                │                    │
│         └────────────────┼────────────────┘                    │
│                          ▼                                     │
│                 host.docker.internal                           │
│            (Gateway / 타 업무 / DB)                            │
└─────────────────────────────────────────────────────────────────┘
```

### ☸️ Kubernetes 배포 구성

```
Namespace: nsight
├── Deployment/Service : nsight-ui, nsight-sv, nsight-eb, ...
├── ConfigMap          : nsight-*-config (SPRING_PROFILES_ACTIVE 등)
├── Secret             : nsight-*-secret
├── Ingress            : tcf-k8s/templates/ingress.yaml
└── deploy.sh          : environment + commit-sha 이미지 롤아웃
```

환경 매핑 (`tcf-k8s/scripts/deploy.sh`):

| environment  | Spring profile | replicas |
|--------------|----------------|----------|
| development  | local          | 1        |
| staging      | dev            | 2        |
| production   | prod           | 3        |

---

## 📈 확장성 및 성능

### 수평 확장
- 업무 WAR·Gateway·UI 인스턴스 다중 기동
- K8s replicas / Docker 스케일 아웃
- Gateway 라우트 캐시(TTL: DEV 30s / PRD 60s)

### 수직 확장
- JVM Heap / `JAVA_OPTS` (`MaxRAMPercentage` 등)
- DB 커넥션 풀(Hikari) 조정
- Connect/Read Timeout 업무별 튜닝

### 성능 최적화
- MyBatis SQL·인덱스 최적화
- `tcf-cache` 활용
- 비동기 Scheduler/Outbox로 연계 부하 분리
- 거래로그 경로·보관 정책 관리

---

## 🔒 보안 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                     NSIGHT Security Architecture                │
├─────────────────────────────────────────────────────────────────┤
│  네트워크/인프라                                                │
│  ├── Ingress / TLS (환경별)                                     │
│  ├── Namespace 격리 (nsight)                                    │
│  └── Secret / ConfigMap 분리                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│  애플리케이션                                                   │
│  ├── tcf-jwt 토큰 발급·검증                                     │
│  ├── Gateway 세션·헤더 규칙                                     │
│  ├── OM 인증 이력·Timeout 정책                                  │
│  └── 입력 검증 (Rule 계층)                                      │
├─────────────────────────────────────────────────────────────────┤
│  데이터                                                         │
│  ├── DB 계정 Secret 분리                                        │
│  ├── 거래로그 GUID/TraceId 추적                                 │
│  └── 업다운로드 파일 경로 통제                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📊 모니터링 및 운영

### 모니터링
1. **시스템**: Actuator `/actuator/health`, 컨테이너/K8s 상태
2. **애플리케이션**: 응답시간, 오류율, Timeout
3. **비즈니스**: 거래량(ServiceId), GUID/TraceId 추적, OM 대시보드

### 운영 도구
| 구분 | 구성 |
|------|------|
| 로그 | TCF 거래로그 + 애플리케이션 로그 |
| 배포 | Gradle WAR / Docker / `tcf-k8s/scripts/deploy.sh` |
| 설정 | `tcf-cicd/{local,dev,prod}` |
| 시험 | `tcf-uj` sample-requests, 로컬 bootRun |

---

## 📎 관련 산출물

| 문서/경로 | 내용 |
|-----------|------|
| `tcf-gateway/docs/ROUTING_TABLE.md` | Gateway 라우팅 설계 |
| `ztcf-집필본-md` 제1부 | TCF 구조·6계층 이해 |
| `ztcf-집필본-md` 제6부 | 인증·세션·JWT·Gateway |
| `tcf-docker/` | 모듈별 Docker 디렉터리 |
| `tcf-k8s/` | K8s 배포 스크립트·Ingress |

---

*문서 ID: ARC02*  
*마지막 업데이트: 2026-07-19*  
*버전: 1.0.0*  
*대상: NSIGHT TCF Framework (`nsight-tcf-framework`)*
