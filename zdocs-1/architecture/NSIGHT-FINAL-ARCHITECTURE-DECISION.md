# NSIGHT 최종 아키텍처 결정서

| 항목        | 내용                                          |
| ----------- | --------------------------------------------- |
| 문서명      | NSIGHT Final Architecture Decision (FAD)      |
| 대상 시스템 | NSIGHT TCF Framework (`nsight-tcf-framework`) |
| 확정일      | 2026-06-28                                    |
| 상태        | **확정 (Approved)**                           |
| 관련 브랜치 | `f-20250628-routing-session`                  |

---

## 결정 요약

| No  | 확정 항목                | 한 줄 결정                                                                     |
| :-: | ------------------------ | ------------------------------------------------------------------------------ |
|  1  | 전체 요청 흐름           | 채널 → **tcf-uj** → **tcf-gateway** → 업무 WAR/OM, 표준 JSON POST `/online`    |
|  2  | Gateway 위치·역할        | **단일 진입 API Gateway** — 라우팅·세션 관문·프록시 로그, 세션 **비소유**      |
|  3  | 세션 관리                | **SESSIONDB** 공유 (`SPRING_SESSION` + `TCF_USER_SESSION`), Gateway 4단계 검증 |
|  4  | Tomcat 배포              | **업무그룹(MSA-A/B/GATEWAY)별 WAR 분리**, ztomcat/K8s Service 단위             |
|  5  | serviceId / businessCode | **businessCode=배포·라우팅**, **serviceId=TCF Handler 라우팅**                 |
|  6  | OM 관리업무              | **운영 메타·세션·거래로그·배치·파일·대시보드** — 업무 거래 아님                |
|  7  | DB 사용 기준             | **RDW/ADW=업무 데이터**, **SESSIONDB=세션**, **LOGDB=거래·감사 이력**          |

---

## 1. 전체 요청 흐름

### 1.1 확정 원칙

- 모든 **온라인 거래**는 `POST` + `application/json` + `StandardRequest` 형식이다.
- 채널(UI)은 업무 WAS에 **직접 붙지 않고** Gateway를 **반드시 경유**한다.
- 성공/실패 판단은 HTTP Status가 아니라 **`result.resultCode == "S0000"`** 이다.
- Gateway·OM·업무 WAR 모두 동일 TCF 파이프라인(`STF → Dispatcher → Handler → ETF`)을 사용한다.

### 1.2 End-to-End (로컬 bootRun 기준)

```text
┌──────────┐   Cookie(JSESSIONID)   ┌──────────┐   Cookie 유지    ┌─────────────┐
│ 브라우저  │ ────────────────────► │ tcf-uj   │ ───────────────► │ tcf-gateway │
│          │  POST /api/relay/     │  :8102   │ POST /{code}/    │   :8100     │
│          │  {code}/online        │          │ online           │             │
└──────────┘                       └──────────┘                  └──────┬──────┘
                                                                          │
                    TCF_GATEWAY_ROUTE (ENV + BUSINESS_CODE)               │
                                                                          ▼
                                                               ┌──────────────────┐
                                                               │ downstream WAS   │
                                                               │ sv :8086/sv/online│
                                                               │ om :8097/om/online│
                                                               └────────┬─────────┘
                                                                        │
                                                                        ▼
                                                               TCF.process()
                                                               Handler → Facade → Service
```

### 1.3 Tomcat 통합 (운영 유사, :8080)

```text
브라우저 → /uj/api/relay/{code}/online
        → /gw/{code}/online          (tcf-gateway, gw.war)
        → /{code}/online             (업무 WAR context)
```

### 1.4 Gateway 미경유 예외 (확정)

| 경로                                    | 이유                                       |
| --------------------------------------- | ------------------------------------------ |
| `tcf-uj` → `tcf-om` `/api/updownload/*` | 대용량 파일·멀티파트 — Gateway 프록시 제외 |
| `actuator/health`                       | 헬스체크 — Gateway·WAR 각자                |

### 1.5 Gateway 내부 처리 (확정)

```text
XxxProxyController
  → BusinessRouteService
    → GRF.forwardOnline
      → GSF.preProcess
          ① TCF_GATEWAY_ROUTE 조회
          ② GatewayAuthenticationService (Bearer → JWT / 없음 → SESSIONDB 4단계)
          ③ GatewaySessionRequestEnricher
      → GatewayRouteDispatcher (RestClient + Cookie·Authorization)
      → GEF (응답·OM 로그인 시 TCF_USER_SESSION 등록)
      → GatewayTransactionLogRecorder (TCF_GATEWAY_TX_LOG)
```

---

## 2. Gateway 위치와 역할

### 2.1 확정 위치

| 환경         | Gateway                              | 비고                              |
| ------------ | ------------------------------------ | --------------------------------- |
| 로컬 bootRun | `http://localhost:8100`              | `tcf-gateway`                     |
| Tomcat       | `http://{host}:8080/gw`              | `gw.war`                          |
| DEV/PRD K8s  | `msa-*-service` 앞단 Ingress/Service | `TCF_GATEWAY_ROUTE`로 Target 결정 |

Gateway는 **업무 WAR와 분리된 독립 프로세스(WAR)** 이다.

### 2.2 Gateway가 하는 일 (In Scope)

| 역할                 | 구현                                                                        |
| -------------------- | --------------------------------------------------------------------------- |
| **라우팅**           | `TCF_GATEWAY_ROUTE` — `ENV_CODE` + `BUSINESS_CODE` → Target URL             |
| **세션 관문**        | SESSIONDB 조회·검증·Cookie 전달·header 보정                                 |
| **JWT 관문**         | `Authorization: Bearer` → tcf-jwt JWKS 검증 (선택, `auth.jwt.enabled`)      |
| **프록시**           | RestClient POST, Connect/Read Timeout 적용                                  |
| **관리**             | `/admin/routes.html`, `/admin/transaction-log.html`, `/admin/sessions.html` |
| **Gateway 거래로그** | `TCF_GATEWAY_TX_LOG` — 관문 통과 이력                                       |

### 2.3 Gateway가 하지 않는 일 (Out of Scope)

| 항목                                 | 담당                                         |
| ------------------------------------ | -------------------------------------------- |
| HttpSession/JSESSIONID **발급·소유** | OM·업무 WAS (Spring Session)                 |
| JWT **발급**                         | tcf-jwt                                      |
| TCF Handler 실행                     | downstream WAS                               |
| 업무 DB(RDW/ADW) 접근                | downstream WAS                               |
| 파일 업·다운로드                     | tcf-om 직접                                  |

### 2.4 설계 원칙 (확정)

> **Gateway = SESSIONDB·JWT 기반 관문.**  
> 세션을 소유하지 않고, 쿠키 또는 Bearer를 검증·전달·통제만 수행한다.

---

## 3. 세션 관리 방식

### 3.1 확정 모델 — 3계층

```text
┌─────────────────────────────────────────────────────────────┐
│ ① SPRING_SESSION (SESSIONDB)                                 │
│    Spring Session JDBC — 실제 세션 저장 (OM 로그인 등)        │
├─────────────────────────────────────────────────────────────┤
│ ② TCF_USER_SESSION (Gateway DB)                              │
│    사용자 세션 레지스트리 — Gateway 관문용                    │
├─────────────────────────────────────────────────────────────┤
│ ③ header.userId (전문 Header)                                │
│    거래 요청 사용자 — Gateway가 SESSIONDB 기준 보정           │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Gateway 4단계 검증 (확정)

| 단계 | 검증 내용                               |           필수           |
| :--: | --------------------------------------- | :----------------------: |
|  1   | Cookie `JSESSIONID` / `NSIGHTSID` 존재  |            ●             |
|  2   | `SPRING_SESSION` 존재·미만료            |         ○ (권장)         |
|  3   | `TCF_USER_SESSION` STATUS=ACTIVE·미만료 |         ○ (권장)         |
|  4   | `header.userId` vs SESSIONDB userId     | ○ (strict=false 시 보정) |

면제: `OM.Auth.login`, `OM.Auth.logout`, `OM.Auth.session`, JWT Auth 계열.

### 3.3 생명주기 (확정)

| 이벤트            | SPRING_SESSION          | TCF_USER_SESSION                           |
| ----------------- | ----------------------- | ------------------------------------------ |
| OM 로그인 성공    | 행 생성/갱신            | Gateway `GEF` → **INSERT** (OM.Auth.login) |
| 업무 거래         | LAST_ACCESS 갱신        | `touchLastAccess`                          |
| OM 로그아웃       | **DELETE** (invalidate) | 배치 ~10초 내 **EXPIRED**                  |
| Gateway 강제 종료 | (변경 없음)             | `FORCED_LOGOUT`                            |
| SPRING 만료·삭제  | OM 배치 DELETE          | Gateway sync → **EXPIRED**                 |

### 3.4 Gateway 자체 세션 (확정)

- `server.servlet.session.tracking-modes: []` — Gateway **JSESSIONID 미발급**
- Spring Security: **STATELESS**

---

## 4. 업무그룹별 Tomcat 배포 구조

### 4.1 업무그룹 정의 (확정)

| 업무그룹    | 코드      | 포함 BUSINESS_CODE      | 배포 단위 (운영)             |
| ----------- | --------- | ----------------------- | ---------------------------- |
| **MSA-A**   | `MSA-A`   | CC, IC, PC              | `msa-a-service` (Tomcat/K8s) |
| **MSA-B**   | `MSA-B`   | BC, MS, SV, PD          | `msa-b-service`              |
| **GATEWAY** | `GATEWAY` | OM, EB, EP, MG, SS, JWT | `gateway-service` + OM WAR   |

Gateway 라우팅 테이블(`TCF_GATEWAY_ROUTE`)의 `ROUTE_GROUP_CODE`와 동일 체계.

### 4.2 ztomcat Context 맵 (로컬 통합, 확정)

| Context                    | WAR                | 업무그룹        |
| -------------------------- | ------------------ | --------------- |
| `/cc`, `/ic`, `/pc`        | cc, ic, pc.war     | MSA-A           |
| `/bc`, `/ms`, `/sv`, `/pd` | bc, ms, sv, pd.war | MSA-B           |
| `/eb`, `/ep`, `/mg`, `/ss` | eb … ss.war        | GATEWAY         |
| `/om`                      | om.war             | GATEWAY (OM)    |
| `/batch`                   | batch.war          | 플랫폼          |
| `/uj`                      | uj.war             | 채널 UI         |
| `/gw`                      | gw.war             | **API Gateway** |

### 4.3 bootRun 포트 (개발, 확정)

| 구분        | 포트               |
| ----------- | ------------------ |
| 업무 WAR    | 8081~8096 (업무별) |
| tcf-om      | 8097               |
| tcf-batch   | 8098               |
| tcf-gateway | **8100**           |
| tcf-uj      | **8102**           |

### 4.4 배포 원칙 (확정)

1. **업무 WAR**는 업무그룹 Tomcat에 co-deploy 가능 (context path `/sv` 등으로 분리).
2. **tcf-gateway**는 업무 Tomcat과 **분리 배포** (단일 `/gw` 진입).
3. **tcf-uj**는 채널 전용 — Relay만 수행, TCF Handler 없음.
4. Profile: bootRun=`local`, ztomcat=`dev`, 운용=`prod`.

---

## 5. ServiceId / businessCode 역할 분리

### 5.1 확정 정의

| 식별자              | 레벨          | 역할                                               | 예                  |
| ------------------- | ------------- | -------------------------------------------------- | ------------------- |
| **businessCode**    | 업무·배포·URL | WAR/context 식별, Gateway 라우팅, DB BUSINESS_CODE | `SV`, `OM`, `CC`    |
| **serviceId**       | 거래·Handler  | TCF Dispatcher 라우팅 키, OM_SERVICE_CATALOG 등록  | `SV.Sample.inquiry` |
| **transactionCode** | 화면·업무     | 화면/거래 설계 ID, 감사·통제                       | `SV-INQ-0001`       |

### 5.2 라우팅 책임 분리 (확정)

```text
URL path / Gateway     → businessCode   (TCF_GATEWAY_ROUTE.BUSINESS_CODE)
TCF Dispatcher         → serviceId      (Handler.serviceIds())
OM·통제·카탈로그      → serviceId + transactionCode
```

### 5.3 serviceId 명명 (확정)

```text
{BusinessCode}.{Domain}.{action}

예) SV.Sample.inquiry
    OM.TransactionLog.inquiry
    OM.Auth.login
```

### 5.4 혼동 방지 규칙 (확정)

- Gateway URL `/sv/online`의 `sv` ≠ serviceId. **businessCode**이다.
- 동일 businessCode WAR 안에 **수백 개 serviceId** Handler가 존재할 수 있다.
- `header.businessCode`와 URL path businessCode는 STF/Controller에서 **일치**시킨다.

---

## 6. OM 관리업무 범위

### 6.1 OM(tcf-om)의 확정 역할

**운영·관리(Operations Management)** 전담. **고객/계좌 등 핵심 업무 거래는 SV·CC 등 업무 WAR**가 담당.

### 6.2 OM In Scope (확정)

| 영역                | serviceId 예                               | 설명                            |
| ------------------- | ------------------------------------------ | ------------------------------- |
| **인증**            | `OM.Auth.login/logout/session`             | OM Admin 로그인·세션            |
| **대시보드**        | `OM.Dashboard.inquiry`                     | AP/DB/세션/배포 상태            |
| **거래로그**        | `OM.TransactionLog.inquiry`                | `TCF_TX_LOG` 통합 조회          |
| **세션 관리**       | `OM.Session.inquiry`                       | `SPRING_SESSION` 조회·강제 종료 |
| **사용자·권한**     | `OM.User.*`, `OM.AuthGroup.*`, `OM.Menu.*` | 운영자·메뉴·권한                |
| **서비스 카탈로그** | `OM.ServiceCatalog.*`                      | serviceId 레지스트리            |
| **배치 관리**       | `OM.Batch.*`                               | tcf-batch Job 수동 실행         |
| **시스템 설정**     | `OM.SystemConfig.inquiry`                  | 환경·게이트웨이 설정 조회       |
| **거래 통제**       | `OM.TransactionControl.*`                  | 헤더 기반 거래 통제 정책        |
| **파일(UD)**        | REST `/updownload/*`                       | 업·다운로드, `UD_FILE_META`     |
| **감사**            | `OM.FileDownload.*`, Admin Audit           | 다운로드·관리 감사              |

### 6.3 OM Out of Scope (확정)

| 항목                | 담당            |
| ------------------- | --------------- |
| 채널 UI·Relay       | tcf-uj          |
| API Gateway·라우팅  | tcf-gateway     |
| AP/DB/세션 **수집** | tcf-batch       |
| 업무 도메인 거래    | `*-service` WAR |

### 6.4 OM vs Gateway 관리 화면 (확정)

| 화면             | URL                                 | DB                             |
| ---------------- | ----------------------------------- | ------------------------------ |
| OM 세션          | `/uj/om/admin/session.html`         | `SPRING_SESSION`               |
| Gateway 세션     | `/admin/sessions.html` (:8100)      | `TCF_USER_SESSION`             |
| OM 거래로그      | `/uj/om/admin/transaction-log.html` | `TCF_TX_LOG` (업무 downstream) |
| Gateway 거래로og | `/admin/transaction-log.html`       | `TCF_GATEWAY_TX_LOG` (관문)    |

---

## 7. RDW / ADW / SESSIONDB / LOGDB 사용 기준

### 7.1 논리 DB 4분류 (확정)

| 논리 ID       | 명칭                      | 용도                                        | 물리 구현 (현재/목표)                                              |
| ------------- | ------------------------- | ------------------------------------------- | ------------------------------------------------------------------ |
| **RDW**       | Read Data Warehouse       | 업무 **조회** — 고객·계좌·상품 등 OLTP Read | 업무 WAR Primary DS → Oracle/H2 (업무별)                           |
| **ADW**       | Analytical Data Warehouse | **분석·집계** — 대량 조회·통계              | Oracle ADW / 별도 분석 DB                                          |
| **SESSIONDB** | Session Database          | **인증 세션** 공유                          | `SPRING_SESSION` (tcf-om H2/Oracle) + `TCF_USER_SESSION` (Gateway) |
| **LOGDB**     | Log Database              | **거래·감사 이력**                          | `TCF_TX_LOG`, `TCF_GATEWAY_TX_LOG`, `OM_*_AUDIT`                   |

### 7.2 접근 주체 (확정)

```text
                    RDW/ADW          SESSIONDB         LOGDB
업무 WAR (*-service)   ● read/write      ○ (간접)         ● write (TCF_TX_LOG)
tcf-om                 ○                 ● read/write     ● read/write
tcf-gateway            ○                 ● read only      ● write (GW tx log)
tcf-batch              ● ping/monitor    ● aggregate      ○
tcf-uj                 ○                 ○ (cookie만)     ○
```

### 7.3 RDW (확정)

- **목적**: 온라인 거래 Handler → Service → DAO/MyBatis의 **업무 데이터** CRUD.
- **현재**: 샘플 WAR는 in-memory H2, DAO 주석 `RDW/ADW mapper hook`.
- **목표**: 운영 시 Oracle RDW 스키마, 업무 DS URL 분리.
- **Timeout**: RDW Query 기본 **3초** (41-service-timeout-policy).

### 7.4 ADW (확정)

- **목적**: 리포트·대시보드·배치 분석 등 **Analytical** workload.
- **현재**: 샘플 단계 — 업무 WAR에서 RDW와 동일 hook.
- **원칙**: 온라인 실시간 경로와 **분리**; 필요 시 Service에서 ADW DS만 참조.

### 7.5 SESSIONDB (확정)

| 테이블                      | 소유                         | 설명                      |
| --------------------------- | ---------------------------- | ------------------------- |
| `SPRING_SESSION`            | tcf-om (Spring Session JDBC) | 실제 HttpSession 직렬화   |
| `SPRING_SESSION_ATTRIBUTES` | ↑                            | 세션 속성                 |
| `TCF_USER_SESSION`          | tcf-gateway                  | Gateway 사용자 레지스트리 |

- **공유**: 로컬 `./data/nsight-txlog/nsight_om` (OM·Gateway session-datasource).
- **운영**: Oracle SESSIONDB 단일 인스턴스, OM·Gateway·(향후) 업무 WAS 공유.

### 7.6 LOGDB (확정)

| 테이블                 | 작성 주체       | 내용                   |
| ---------------------- | --------------- | ---------------------- |
| `TCF_TX_LOG`           | 각 WAR TCF ETF  | 업무 Handler 거래 이력 |
| `TCF_GATEWAY_TX_LOG`   | tcf-gateway GRF | 관문 프록시 이력       |
| `OM_FILE_DOWNLOAD_LOG` | tcf-om          | 파일 다운로드 감사     |
| `OM_ADMIN_AUDIT` 등    | tcf-om          | 관리操作 감사          |

- **로컬**: `nsight.txlog.path` / `transactionLogDataSource` — OM과 업무 WAR **동일 file DB** 공유 → OM에서 통합 조회.
- **OM 대시보드 `LOGDB`**: tcf-batch가 JDBC ping으로 **연결 상태**만 수집 (`OM_DB_STATUS`).

### 7.7 DB 선택 의사결정 트리 (확정)

```text
데이터를 저장/조회하는가?
  ├─ 세션·로그인?        → SESSIONDB
  ├─ 거래 추적·감사?     → LOGDB
  ├─ 실시간 업무 조회/변경? → RDW
  └─ 분석·통계·대량?     → ADW
```

---

## 부록 A. 환경별 URL (확정)

| 구분    | bootRun                                          | Tomcat :8080                         |
| ------- | ------------------------------------------------ | ------------------------------------ |
| UI      | `http://localhost:8102`                          | `http://localhost:8080/uj`           |
| Gateway | `http://localhost:8100`                          | `http://localhost:8080/gw`           |
| OM API  | `http://localhost:8097/om/online`                | `http://localhost:8080/om/online`    |
| SV API  | `http://localhost:8100/sv/online` (Gateway 경유) | `http://localhost:8080/gw/sv/online` |

---

## 부록 B. 관련 문서

| 문서            | 경로                                                       |
| --------------- | ---------------------------------------------------------- |
| 아키텍처 정의서 | [architecture.md](architecture.md)                         |
| 온라인 아키텍처 | [14-online-arc.md](14-online-arc.md)                       |
| 세션            | [10-session.md](10-session.md)                             |
| TCF 테이블      | [19-tcf-table.md](19-tcf-table.md)                         |
| Gateway README  | [../../tcf-gateway/README.md](../../tcf-gateway/README.md) |
| tcf-uj README   | [../../tcf-uj/README.md](../../tcf-uj/README.md)           |

---

## 부록 C. 변경 이력

| 일자       | 내용                                                                                   |
| ---------- | -------------------------------------------------------------------------------------- |
| 2026-06-28 | 최초 확정 — Gateway SESSIONDB 관문, TCF_USER_SESSION, GW 거래로그, 세션 sync 배치 반영 |
