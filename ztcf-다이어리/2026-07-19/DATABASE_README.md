# NSIGHT TCF — 데이터베이스 초기화 및 가이드

## 개요

NSIGHT TCF Framework는 **업무 WAR별 데이터소스**와 **공통 저장소(거래로그·Gateway 라우트·세션)** 를 분리합니다.

| 구분 | local | dev | prod |
|------|-------|-----|------|
| 업무 DB | H2 in-memory (`MODE=Oracle`) | H2 TCP 또는 Oracle | Oracle |
| 거래로그 | H2 file (`./data/nsight-txlog`) | H2 TCP / Oracle | Oracle/H2 설정 |
| Gateway 라우트 | H2 + `schema.sql`/`data.sql` | 동일 또는 Oracle DDL | Oracle |

초기화 방식:

1. Spring SQL Init: `classpath:schema.sql` (+ `data.sql`)
2. 모듈별 `ApplicationRunner` 마이그레이션 (예: `EbDatabaseMigration`)
3. Oracle 수동 DDL: `tcf-gateway/sql/oracle/*.sql`

---

## 환경별 JDBC (예시)

### EB (`eb-service`) — local

```yaml
# application-local.yml
spring:
  sql.init.mode: always
  sql.init.schema-locations: classpath:schema.sql
  datasource:
    url: jdbc:h2:mem:nsight_eb;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false
  h2.console.enabled: true

nsight.tcf.transaction-log-datasource:
  url: jdbc:h2:file:./data/nsight-txlog/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false
  username: sa
  password:
```

### Gateway (`tcf-gateway`) — local

- 앱 포트: `8100`
- 라우트/세션/게이트웨이 로그: `schema.sql` + `data.sql`
- Oracle 운영 DDL: `tcf-gateway/sql/oracle/`

### EP (`ep-service`)

- `schema.sql`: `EP_USER_EVENT`
- `data.sql`: 시드 이벤트 3건

---

## 스키마 맵 (주요 모듈)

### 1. tcf-gateway

| 테이블 | 용도 |
|--------|------|
| `TCF_GATEWAY_ROUTE` | ENV + BUSINESS_CODE 라우팅 |
| `TCF_USER_SESSION` | 사용자 세션 |
| `TCF_GATEWAY_TX_LOG` | Gateway 중계 거래로그 |

```sql
-- 핵심: TCF_GATEWAY_ROUTE (local H2 / Oracle 동일 개념)
CREATE TABLE IF NOT EXISTS TCF_GATEWAY_ROUTE (
    ROUTE_ID             VARCHAR(60)  NOT NULL,
    ENV_CODE             VARCHAR(20)  NOT NULL,  -- LOCAL / DEV / PRD
    ROUTE_GROUP_CODE     VARCHAR(30)  NOT NULL,
    ROUTE_GROUP_NAME     VARCHAR(100) NOT NULL,
    BUSINESS_CODE        VARCHAR(10)  NOT NULL,
    BUSINESS_NAME        VARCHAR(100) NOT NULL,
    TARGET_BASE_URL      VARCHAR(300) NOT NULL,
    CONTEXT_PATH         VARCHAR(100) NOT NULL,
    ONLINE_PATH          VARCHAR(100) DEFAULT '/online' NOT NULL,
    HEALTH_CHECK_PATH    VARCHAR(200),
    CONNECT_TIMEOUT_MS   INT DEFAULT 3000 NOT NULL,
    READ_TIMEOUT_MS      INT DEFAULT 5000 NOT NULL,
    USE_YN               CHAR(1) DEFAULT 'Y' NOT NULL,
    SORT_ORDER           INT,
    DESCRIPTION          VARCHAR(500),
    CREATED_AT           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UPDATED_AT           TIMESTAMP,
    CONSTRAINT PK_TCF_GATEWAY_ROUTE PRIMARY KEY (ROUTE_ID),
    CONSTRAINT UK_TCF_GATEWAY_ROUTE_01 UNIQUE (ENV_CODE, BUSINESS_CODE)
);
```

Oracle 스크립트:

- `tcf-gateway/sql/oracle/TCF_GATEWAY_ROUTE.sql`
- `tcf-gateway/sql/oracle/TCF_GATEWAY_ROUTE_DATA.sql`
- `tcf-gateway/sql/oracle/TCF_USER_SESSION.sql`
- `tcf-gateway/sql/oracle/TCF_GATEWAY_TX_LOG.sql`

### 2. eb-service

| 테이블 | 용도 |
|--------|------|
| `EB_USER` | EB 사용자 |
| `EB_EVENT` | 이벤트·Outbox 상태 (`EVENT_STATUS`) |
| `EB_SYSTEM_TX` | 시스템 거래 현황(화면용) |

```sql
CREATE TABLE IF NOT EXISTS EB_USER (
    USER_ID     VARCHAR(50) PRIMARY KEY,
    USER_NAME   VARCHAR(100),
    BRANCH_ID   VARCHAR(20),
    CREATED_AT  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS EB_EVENT (
    EVENT_ID      VARCHAR(50) PRIMARY KEY,
    USER_ID       VARCHAR(50) NOT NULL,
    EVENT_TYPE    VARCHAR(30) NOT NULL,
    EVENT_STATUS  VARCHAR(20) NOT NULL,
    RETRY_COUNT   INT DEFAULT 0,
    CREATED_AT    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    SENT_AT       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS EB_SYSTEM_TX (
    TX_SEQ_NO      VARCHAR(40) PRIMARY KEY,
    TX_DATE        VARCHAR(10) NOT NULL,
    SCREEN_ID      VARCHAR(20),
    SERVICE_ID     VARCHAR(80),
    GLOBAL_ID      VARCHAR(64),
    REQUEST_AT     TIMESTAMP NOT NULL,
    RESPONSE_AT    TIMESTAMP,
    ELAPSED_SEC    INT,
    INPUT_CONTENT  VARCHAR(1000),
    EMP_NO         VARCHAR(20),
    BRANCH_CODE    VARCHAR(20),
    TERMINAL_IP    VARCHAR(40),
    TX_TYPE        VARCHAR(20)
);
```

- 스키마: `eb-service/src/main/resources/schema.sql`
- 추가 초기화: `EbDatabaseMigration` (`ApplicationRunner`)

### 3. ep-service

| 테이블 | 용도 |
|--------|------|
| `EP_USER_EVENT` | EB 등에서 수신한 사용자 이벤트 |

```sql
CREATE TABLE IF NOT EXISTS EP_USER_EVENT (
    EVENT_ID     VARCHAR(50) PRIMARY KEY,
    USER_ID      VARCHAR(50) NOT NULL,
    EVENT_TYPE   VARCHAR(30) NOT NULL,
    RECEIVED_AT  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

시드 (`data.sql`):

| EVENT_ID | USER_ID | EVENT_TYPE |
|----------|---------|------------|
| EVT-SEED-001 | U001 | USER_CREATED |
| EVT-SEED-002 | U002 | USER_UPDATED |
| EVT-SEED-003 | U001 | USER_LOGIN |

### 4. tcf-om / 거래로그

| 테이블(대표) | 용도 |
|--------------|------|
| `TCF_TX_LOG` | 온라인 거래로그 |
| `TCF_TRANSACTION_CONTROL` | 거래 통제 |
| `OM_USER` / `OM_AUTH_GROUP` / `OM_MENU` | 운영 사용자·권한·메뉴 |
| `OM_AUDIT_LOG` | 감사 로그 |
| `OM_*_STATUS` | AP/DB/세션/배포 상태 |

- 스키마: `tcf-om/src/main/resources/schema.sql`
- 시드: `tcf-om/src/main/resources/data.sql`
- 파일 DB 경로(예): `./data/nsight-txlog/nsight_om`

### 5. tcf-jwt

| 테이블(대표) | 용도 |
|--------------|------|
| `TCF_JWT_TOKEN` / `TCF_REFRESH_TOKEN` | 토큰 |
| `TCF_TOKEN_DENYLIST` | 폐기 토큰 |
| `TCF_JWT_LOGIN_HISTORY` | 로그인 이력 |
| `TCF_JWT_SECURITY_POLICY` | 보안 정책 |
| `OM_USER` / `OM_AUTH_GROUP` | 인증 사용자·그룹 |

- `tcf-jwt/src/main/resources/schema.sql`, `data.sql`

### 6. tcf-oc

용량·시나리오 관련 테이블 (`CP_HELLO_LOG`, `CAP_NEW_*`, `TB_CAPACITY_ACCOUNT` 등)

- `tcf-oc/src/main/resources/schema.sql`, `data.sql`

### 7. sv-service 등

- 업무별 `schema.sql` / `data.sql`이 있으면 동일 패턴으로 기동 시 초기화
- MyBatis XML: `src/main/resources/mapper/**`

---

## 설정 파일 위치

| 파일 | 역할 |
|------|------|
| `*/src/main/resources/schema.sql` | 테이블·인덱스 생성 |
| `*/src/main/resources/data.sql` | 시드 데이터 |
| `*/src/main/resources/application-local.yml` | local JDBC·H2 콘솔 |
| `*/src/main/resources/application-dev.yml` | dev JDBC |
| `*/src/main/resources/application-prod.yml` | prod Oracle URL (환경변수) |
| `tcf-cicd/{local,dev,prod}/spring/...` | 배포용 yml |
| `tcf-gateway/sql/oracle/*.sql` | Oracle DDL/DML |

`spring.sql.init.mode=always` (local 등)일 때 기동마다 schema(+data)가 적용됩니다.  
H2 mem DB는 프로세스 종료 시 소멸합니다.

---

## H2 콘솔 접속

모듈별로 `spring.h2.console.enabled=true`인 경우:

| 모듈 | 앱 URL 예 | JDBC URL 예 |
|------|-----------|-------------|
| eb-service | `http://localhost:8089/h2-console` | `jdbc:h2:mem:nsight_eb;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false` |
| ep-service | `http://localhost:8090/h2-console` | (해당 모듈 application-local.yml 참고) |
| tcf-gateway | `http://localhost:8100/h2-console` | (gateway local yml 참고) |

공통:

- Username: `sa`
- Password: (비움)

거래로그 파일 DB:

```
jdbc:h2:file:./data/nsight-txlog/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false
```

로컬 파일 예:

- `data/nsight-txlog/` (또는 모듈 cwd 기준 상대 경로)
- `data/gateway-route*.db` (Gateway H2 파일 사용 시)

---

## 기동 및 확인

### 1. 로컬 기동 (예: EB)

```bash
# 레포 루트
./gradlew :eb-service:bootRun
# 또는 Windows 스크립트
eb-service\scripts\run-local.bat
```

### 2. Health

```bash
curl http://localhost:8089/actuator/health
curl http://localhost:8090/actuator/health
curl http://localhost:8100/actuator/health
```

### 3. 온라인 거래로 데이터 확인

```bash
# EP 시드 조회는 업무 ServiceId에 따름 — tcf-uj sample-requests 사용 권장
# Gateway 라우트 관리
# http://localhost:8100/admin/routes.html
```

### 4. Docker

컨테이너에서도 동일 프로필/`SERVER_PORT`를 쓰며, 볼륨으로 txlog·data를 마운트합니다.

```bat
eb-service\scripts\docker\docker-run.bat -d
sv-service\scripts\docker\docker-run.bat -d
```

---

## 데이터 흐름 (EB → EP)

```
EB_EVENT (EVENT_STATUS) 등록
    → Scheduler / Outbox Publish
    → EP /ep/online
    → EP_USER_EVENT 적재
    → EB_EVENT.SENT_AT / STATUS 갱신
```

거래 추적은 **GUID / TraceId / ServiceId** 를 Gateway·업무·OM 거래로그에서 동일 키로 조회합니다.

---

## 스키마·데이터 수정 방법

1. **테이블 구조**: 해당 모듈 `schema.sql` 수정 → 재기동  
   - Oracle은 `tcf-gateway/sql/oracle/` 등 DDL을 별도 반영
2. **시드 데이터**: `data.sql` 수정 → 재기동 (mem DB는 항상 재적용)
3. **런타임**: H2 콘솔 또는 업무 Online API
4. **EB 추가 마이그레이션**: `EbDatabaseMigration` 로직 확인

PRD Gateway 라우트는 DB 직접 수정 대신 **관리화면 또는 승인된 배포**를 권장합니다.  
(`ROUTING_TABLE.md` 운영 원칙)

---

## 문제 해결

### 테이블이 없음
- `schema.sql` 경로·`spring.sql.init.mode` 확인
- 프로필이 `local`인지 확인 (`SPRING_PROFILES_ACTIVE`)
- H2 콘솔에서 `SHOW TABLES` 확인

### 시드가 안 들어감
- `data.sql` 존재 여부 (EB는 data.sql 없이 Migration 사용 가능)
- FK/UNIQUE 위반 로그 확인
- mem DB면 재기동으로 초기화

### 거래로그 DB 잠금 / 접속 실패
- `AUTO_SERVER=TRUE` 와 다중 프로세스 동시 접속 확인
- `./data/nsight-txlog` 경로·권한 확인
- 다른 모듈이 동일 파일 DB를 쓰는지 확인

### Oracle 전환 실패
- `NSIGHT_*_DB_URL` / Driver / 계정 Secret 확인
- Oracle DDL과 H2 `MODE=Oracle` 스키마 차이 점검
- `tcf-cicd/prod/spring/...` 설정과 일치 여부 확인

---

## 관련 문서

| 문서 | 내용 |
|------|------|
| ARC02 | 애플리케이션 구성도 (데이터 레이어) |
| ARC03 | 비즈니스 개괄 (도메인·데이터 흐름) |
| ARC04 | 통합 아키텍처 DA |
| `tcf-gateway/docs/ROUTING_TABLE.md` | 라우트 DDL·시드 |

---

**문서**: DATABASE_README  
**버전**: 1.0.0  
**작성일**: 2026-07-19  
**대상**: NSIGHT TCF Framework (`nsight-tcf-framework`)
