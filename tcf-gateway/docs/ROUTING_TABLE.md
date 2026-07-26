# Gateway 라우팅 테이블 설계 (TCF_GATEWAY_ROUTE)

## 개요

Gateway는 현재 실행 환경(`ENV_CODE`)과 업무코드(`BUSINESS_CODE`)로 `TCF_GATEWAY_ROUTE`를 조회하고,
Target URL을 아래 공식으로 조립해 downstream WAR로 Relay합니다.

```text
TARGET_URL = TARGET_BASE_URL + CONTEXT_PATH + ONLINE_PATH
```

## 처리 흐름

```text
tcf-gateway
   ↓
현재 실행 모드 확인 (application-{profile}.yml → nsight.gateway.env-code)
LOCAL / DEV / PRD
   ↓
businessCode 기준 조회
   ↓
TCF_GATEWAY_ROUTE
   ↓
TARGET_BASE_URL + CONTEXT_PATH + ONLINE_PATH 조립
   ↓
GEF Relay
```

## Oracle DDL

```sql
CREATE TABLE TCF_GATEWAY_ROUTE (
    ROUTE_ID             VARCHAR2(60)  NOT NULL,
    ENV_CODE             VARCHAR2(20)  NOT NULL,
    ROUTE_GROUP_CODE     VARCHAR2(30)  NOT NULL,
    ROUTE_GROUP_NAME     VARCHAR2(100) NOT NULL,
    BUSINESS_CODE        VARCHAR2(10)  NOT NULL,
    BUSINESS_NAME        VARCHAR2(100) NOT NULL,
    TARGET_BASE_URL      VARCHAR2(300) NOT NULL,
    CONTEXT_PATH         VARCHAR2(100) NOT NULL,
    ONLINE_PATH          VARCHAR2(100) DEFAULT '/online' NOT NULL,
    HEALTH_CHECK_PATH    VARCHAR2(200),
    CONNECT_TIMEOUT_MS   NUMBER(10) DEFAULT 3000 NOT NULL,
    READ_TIMEOUT_MS      NUMBER(10) DEFAULT 5000 NOT NULL,
    USE_YN               CHAR(1) DEFAULT 'Y' NOT NULL,
    SORT_ORDER           NUMBER(5),
    DESCRIPTION          VARCHAR2(500),
    CREATED_AT           DATE DEFAULT SYSDATE NOT NULL,
    UPDATED_AT           DATE,

    CONSTRAINT PK_TCF_GATEWAY_ROUTE
        PRIMARY KEY (ROUTE_ID),

    CONSTRAINT UK_TCF_GATEWAY_ROUTE_01
        UNIQUE (ENV_CODE, BUSINESS_CODE),

    CONSTRAINT CK_TCF_GATEWAY_ROUTE_ENV
        CHECK (ENV_CODE IN ('LOCAL', 'DEV', 'PRD')),

    CONSTRAINT CK_TCF_GATEWAY_ROUTE_USE
        CHECK (USE_YN IN ('Y', 'N'))
);

CREATE INDEX IX_TCF_GATEWAY_ROUTE_01
ON TCF_GATEWAY_ROUTE (ENV_CODE, BUSINESS_CODE, USE_YN);

CREATE INDEX IX_TCF_GATEWAY_ROUTE_02
ON TCF_GATEWAY_ROUTE (ENV_CODE, ROUTE_GROUP_CODE, USE_YN);
```

## Gateway 조회 SQL

```sql
SELECT
    ROUTE_ID,
    ENV_CODE,
    ROUTE_GROUP_CODE,
    ROUTE_GROUP_NAME,
    BUSINESS_CODE,
    BUSINESS_NAME,
    TARGET_BASE_URL,
    CONTEXT_PATH,
    ONLINE_PATH,
    TARGET_BASE_URL || CONTEXT_PATH || ONLINE_PATH AS TARGET_URL,
    HEALTH_CHECK_PATH,
    CONNECT_TIMEOUT_MS,
    READ_TIMEOUT_MS
FROM TCF_GATEWAY_ROUTE
WHERE ENV_CODE = :envCode
  AND BUSINESS_CODE = :businessCode
  AND USE_YN = 'Y';
```

## 환경별 설정 (application.yml)

| Profile | env-code | route-cache |
|---------|----------|-------------|
| local   | LOCAL    | disabled    |
| dev     | DEV      | 30s TTL     |
| prod    | PRD      | 60s TTL     |

운영 환경에서는 설정 파일을 서버에서 직접 수정하지 않고 Git/CI-CD 기준으로 반영합니다.

## 관리 화면

- URL: `http://localhost:8100/admin/routes.html`
- REST API: `/api/admin/routes`

## 프록시 엔드포인트

| 업무코드 | Gateway 경로 | 컨트롤러 |
|----------|--------------|----------|
| CC | `POST /cc/online` | `CcProxyController` |
| BC | `POST /bc/online` | `BcProxyController` |
| IC, PC, MS, SV, PD, … | `POST /{code}/online` | `*ProxyController` |

## Oracle 시드 데이터

- `sql/oracle/TCF_GATEWAY_ROUTE_DATA.sql` — LOCAL / DEV / PRD 기본 라우팅 39건
- H2 로컬: `src/main/resources/data.sql` (기동 시 자동 MERGE)

## 운영 변경 원칙

| 구분 | 권장 기준 |
|------|-----------|
| 테이블 | `TCF_GATEWAY_ROUTE` 1개 |
| 환경 구분 | `ENV_CODE` = LOCAL, DEV, PRD |
| 라우팅 기준 | `ENV_CODE + BUSINESS_CODE` |
| Target 조립 | `TARGET_BASE_URL + CONTEXT_PATH + ONLINE_PATH` |
| 운영 변경 | DB 직접 수정 금지, 관리화면 또는 승인된 배포 절차 |
| 화면 표시 | PRD → "운용" |

## Timeout 적용

| 컬럼 | Relay 동작 |
|------|------------|
| `CONNECT_TIMEOUT_MS` | downstream TCP 연결 대기 (기본 3000ms) |
| `READ_TIMEOUT_MS` | downstream 응답 수신 대기 (기본 5000ms) |

`GatewayRouteDispatcher`가 `RouteContext`의 timeout으로 `RestClient`를 생성해 요청마다 적용합니다.

## 라우팅 원칙

Gateway는 **TCF_GATEWAY_ROUTE만** 사용합니다. `deployment-mode`, catalog, query 파라미터 fallback은 없습니다.
미등록 업무코드는 **HTTP 404**와 함께 라우팅 등록 안내 JSON을 반환합니다.

## LOCAL bootRun 포트 기준

| 업무 | 포트 | Target URL (LOCAL) |
|------|------|------------------|
| CC | 8081 | `http://127.0.0.1:8081/cc/online` |
| IC | 8082 | `http://127.0.0.1:8082/ic/online` |
| PC | 8083 | `http://127.0.0.1:8083/pc/online` |
| BC | 8084 | `http://127.0.0.1:8084/bc/online` |
| MS | 8085 | `http://127.0.0.1:8085/ms/online` |
| SV | 8086 | `http://127.0.0.1:8086/sv/online` |
| PD | 8087 | `http://127.0.0.1:8087/pd/online` |
| EB | 8089 | `http://127.0.0.1:8089/eb/online` |
| EP | 8090 | `http://127.0.0.1:8090/ep/online` |
| SS | 8093 | `http://127.0.0.1:8093/ss/online` |
| MG | 8096 | `http://127.0.0.1:8096/mg/online` |
| OM | 8097 | `http://127.0.0.1:8097/om/online` |
| JWT | 8110 | `http://127.0.0.1:8110/online` |

출처: 각 `*-service/application-local.yml`, `tcf-om`, `tcf-jwt`, `BusinessModuleDefinitions`.
