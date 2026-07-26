# Gateway 라우팅 명명규칙

> **NSIGHT TCF 명명규칙 상세** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

Gateway 라우팅 명명규칙 설계기준
## 1. 도입 전 안내말

Gateway 라우팅 명명규칙은 단순히 URL 경로를 정하는 기준이 아니다.NSIGHT TCF에서는 Gateway 라우팅명이 다음 항목과 연결된다.
```text
businessCode
↓
Gateway RouteId
↓
Target Group
↓
Target URL
↓
ServiceId
↓
TransactionCode
↓
Gateway Relay Log
↓
```

거래로그 / 장애추적 / 운영관리

NSIGHT Gateway는 Apache, L4, GSLB를 대체하는 인프라 장비가 아니라, Apache 뒤쪽에서 업무코드 기반 라우팅, 세션/JWT 확인, Header 검증, ServiceId 검증, 거래통제, Downstream 업무 WAS Relay를 담당하는 Application Gateway로 정의한다.또한 Gateway 라우팅은 businessCode 기준으로 대상 업무 WAR를 결정하고, ENV_CODE + BUSINESS_CODE 기준으로 local/dev/prd 환경별 대상 URL을 다르게 관리하는 구조가 적합하다.

## 2. Gateway 라우팅 명명 최상위 원칙

| 원칙 | 설계 기준 |
| --- | --- |
| 업무코드 우선 | 모든 Route는 SV, CM, OM, MG 등 업무코드를 기준으로 식별한다 |
| 환경코드 포함 | LOCAL, DEV, STG, PRD 환경을 Route에 포함한다 |
| 논리/물리 분리 | 논리 Route와 실제 Target 서버를 분리한다 |
| URL과 업무코드 일치 | /sv/online은 businessCode = SV와 일치해야 한다 |
| RouteId 표준화 | RouteId만 보고 환경, 업무, 목적을 알 수 있어야 한다 |
| TargetId 표준화 | TargetId만 보고 대상 업무, 서버, Port를 알 수 있어야 한다 |
| ServiceId 검증 연계 | Gateway는 Route만 하지 않고 ServiceId와 businessCode 정합성을 확인한다 |
| 운영 추적 가능 | Gateway 로그에 routeId, targetId, businessCode, serviceId, guid를 남긴다 |
| 미등록 Route 차단 | 라우팅 테이블에 없는 업무코드는 기본 차단한다 |
| 운영 변경관리 | Route 변경은 OM 또는 배포관리 기준으로 이력 관리한다 |

## 3. Gateway 라우팅 기본 구조

사용자 / WebTopSuite / tcf-ui
```text
        ↓
Apache
        ↓
tcf-gateway
        ↓
TCF_GATEWAY_ROUTE
        ↓
TCF_GATEWAY_ROUTE_TARGET
        ↓

```

업무 WAR / tcf-om
```text
        ↓
```

TCF.process()

예시는 다음과 같다.
```text
POST /gw/SV/online
```

Header.businessCode = SV
```text
Header.serviceId    = SV.Customer.selectSummary
↓
ROUTE_ID = GW-PRD-SV-ONLINE
↓
TARGET_GROUP_ID = TG-PRD-SV-01
↓
```

TARGET_URL = http://msa-b-service:9090/sv/online

## 4. Gateway URL 명명규칙

### 4.1 외부 Gateway 진입 URL

/gw/{businessCode}/online

또는 API Prefix를 명확히 둘 경우 다음을 사용한다.
/api/tcf/{businessCode}/online

| 구분 | 표준 URL | 설명 |
| --- | --- | --- |
| SV 업무 | /gw/SV/online | SV 업무 온라인 거래 |
| CM 업무 | /gw/CM/online | 캠페인 업무 온라인 거래 |
| OM 업무 | /gw/OM/online | 운영관리 업무 온라인 거래 |
| MG 업무 | /gw/MG/online | 메시지 업무 온라인 거래 |
| UD 업무 | /gw/UD/online | 파일 업다운로드 업무 |

운영 표준은 대문자 업무코드 사용을 권장한다. 단, Apache 또는 Spring Routing 설정상 소문자를 사용하는 경우에도 내부 Header의 businessCode는 대문자 기준으로 통일한다.

### 4.2 Downstream 업무 URL

Gateway가 호출하는 내부 업무 URL은 다음 형식을 사용한다.
{targetBaseUrl}/{businessContext}/online

| 업무 | Target Base URL | Context | 최종 URL |
| --- | --- | --- | --- |
| CC | http://msa-a-service:8080 | /cc | http://msa-a-service:8080/cc/online |
| IC | http://msa-a-service:8080 | /ic | http://msa-a-service:8080/ic/online |
| PC | http://msa-a-service:8080 | /pc | http://msa-a-service:8080/pc/online |
| SV | http://msa-b-service:9090 | /sv | http://msa-b-service:9090/sv/online |
| CM | http://msa-c-service:9090 | /cm | http://msa-c-service:9090/cm/online |
| OM | http://om-service:8080 | /om | http://om-service:8080/om/online |

여러 업무 WAR가 같은 Tomcat에 배포되는 경우에도 업무코드별 CONTEXT_PATH만 다르게 관리하면 된다. 기존 Gateway 설계에서도 여러 업무코드가 같은 Tomcat Base URL을 공유하고, 업무코드별 Context만 다르게 라우팅하는 구조를 허용한다.

## 5. RouteId 명명규칙

### 5.1 기본 형식

GW-{ENV_CODE}-{BUSINESS_CODE}-{ROUTE_TYPE}

| 구성요소 | 설명 | 예시 |
| --- | --- | --- |
| GW | Gateway Route 식별 Prefix | GW |
| ENV_CODE | 환경코드 | LOCAL, DEV, STG, PRD |
| BUSINESS_CODE | 업무코드 | SV, CM, OM |
| ROUTE_TYPE | 라우팅 유형 | ONLINE, FILE, BATCH, HEALTH |
| 예시: | RouteId | 설명 |
| GW-LOCAL-SV-ONLINE | Local 환경 SV 온라인 라우팅 | GW-DEV-SV-ONLINE |
| Dev 환경 SV 온라인 라우팅 | GW-STG-CM-ONLINE | Stage 환경 CM 온라인 라우팅 |
| GW-PRD-OM-ONLINE | 운영 환경 OM 온라인 라우팅 | GW-PRD-UD-FILE |
| 운영 환경 UD 파일 라우팅 | GW-PRD-SV-HEALTH | 운영 환경 SV Health Check 라우팅 |

## 6. RouteType 표준

RouteType
| 의미 | 사용 예시 | ONLINE | 온라인 전문 거래 |
| --- | --- | --- | --- |
| /gw/SV/online | FILE | 파일 업로드/다운로드 | /gw/UD/files |
| BATCH | 배치 수동 실행 또는 상태 조회 | /gw/BT/online | HEALTH |
| 업무 WAS Health Check | /gw/SV/health | ADMIN | 운영관리 전용 라우팅 |
| /gw/OM/online | AUTH | 인증/SSO/JWT 라우팅 | /gw/JWT/online |

RELAY
외부 연계 Relay
/gw/IF/relay

## 7. TargetGroupId 명명규칙

Gateway Route는 논리 라우팅이고, 실제 호출 대상은 Target Group으로 관리한다.
TG-{ENV_CODE}-{BUSINESS_CODE}-{GROUP_NO}

| TargetGroupId | 설명 |
| --- | --- |
| TG-PRD-SV-01 | 운영 SV 1번 Target Group |
| TG-PRD-CM-01 | 운영 CM 1번 Target Group |
| TG-DEV-OM-01 | 개발 OM 1번 Target Group |
| TG-LOCAL-SV-01 | Local SV Target Group |
동일 업무가 A/B 그룹으로 나누어 운영될 경우 다음처럼 확장한다.

| TG-{ENV_CODE}-{BUSINESS_CODE}-{GROUP_CODE} | |

| TargetGroupId | 설명 |
| --- | --- |
| TG-PRD-SV-A | 운영 SV A그룹 |
| TG-PRD-SV-B | 운영 SV B그룹 |
| TG-PRD-CM-A | 운영 CM A그룹 |
| TG-PRD-CM-B | 운영 CM B그룹 |

## 8. TargetId 명명규칙

### 8.1 기본 형식

TGT-{ENV_CODE}-{BUSINESS_CODE}-{NODE_ID}-{PORT}

| 구성요소 | 설명 | 예시 |
| --- | --- | --- |
| TGT | Target 식별 Prefix | TGT |
| ENV_CODE | 환경코드 | PRD |
| BUSINESS_CODE | 업무코드 | SV |
| NODE_ID | 서버 또는 서비스 식별자 | AP01, MSA-B-01 |
| PORT | Port | 9090 |
| 예시: | TargetId | Target URL |
| TGT-PRD-SV-MSA-B-01-9090 | http://msa-b-01:9090/sv/online | TGT-PRD-SV-MSA-B-02-9090 |
| http://msa-b-02:9090/sv/online | TGT-PRD-OM-AP01-8080 | http://om-ap01:8080/om/online |
| TGT-DEV-SV-LOCAL-9090 | http://localhost:9090/sv/online |  |

## 9. 환경코드 명명규칙

ENV_CODE
| 의미 | 설명 | LOCAL | 로컬 개발 |
| --- | --- | --- | --- |
| 개발자 PC 또는 local bootRun | DEV | 개발 환경 | 개발 서버 |
| STG | 스테이징 | 운영 반영 전 검증 | PRD |
| 운영 | 운영 서비스 | DR | 재해복구 |
| DR 센터 또는 DR 대상 | TEST | 테스트 | 자동화 테스트 또는 성능 테스트 |

환경별 라우팅은 ENV_CODE + BUSINESS_CODE를 기준으로 같은 업무코드가 환경별로 다른 Target으로 가도록 설계한다.

## 10. 업무코드와 Context 명명규칙

| 업무코드 | Context | WAR |
| --- | --- | --- |
| Gateway URL | Downstream URL | CC |
| /cc | cc.war | /gw/CC/online |
| /cc/online | IC | /ic |
| ic.war | /gw/IC/online | /ic/online |
| PC | /pc | pc.war |
| /gw/PC/online | /pc/online | SV |
| /sv | sv.war | /gw/SV/online |
| /sv/online | CM | /cm |
| cm.war | /gw/CM/online | /cm/online |
| MG | /mg | mg.war |
| /gw/MG/online | /mg/online | OM |
| /om | om.war | /gw/OM/online |
| /om/online | UD | /ud |
| ud 또는 om.war 내장 | /gw/UD/files | /ud/files |
| BT | /bt | tcf-batch |
| /gw/BT/online | /bt/online |  |

## 11. Gateway Route Table 명명규칙

Gateway 라우팅 기준 테이블은 다음 두 개로 분리한다.
TCF_GATEWAY_ROUTE
= 업무코드별 논리 라우팅 정책

TCF_GATEWAY_ROUTE_TARGET
= Route별 실제 대상 서버 목록

기존 설계에서도 업무코드별 라우팅은 TCF_GATEWAY_ROUTE, 실제 Target 서버 목록은 TCF_GATEWAY_ROUTE_TARGET으로 분리하는 것을 권장한다.

## 12. TCF_GATEWAY_ROUTE 컬럼 명명규칙

| 컬럼 | 설명 | 예시 |
| --- | --- | --- |
| ROUTE_ID | Route 식별자 | GW-PRD-SV-ONLINE |
| ENV_CODE | 환경코드 | PRD |
| BUSINESS_CODE | 업무코드 | SV |
| ROUTE_TYPE | Route 유형 | ONLINE |
| SOURCE_PATH | Gateway 수신 Path | /gw/SV/online |
| TARGET_GROUP_ID | Target Group ID | TG-PRD-SV-01 |
| TARGET_CONTEXT_PATH | 대상 Context | /sv |
| TARGET_ONLINE_PATH | 대상 Online Path | /sv/online |
| AUTH_REQUIRED_YN | 인증 필요 여부 | Y |
| SESSION_REQUIRED_YN | 세션 필요 여부 | Y |
| JWT_REQUIRED_YN | JWT 필요 여부 | N |
| HEADER_CHECK_YN | Header 검증 여부 | Y |
| SERVICE_CHECK_YN | ServiceId 검증 여부 | Y |
| USE_YN | 사용 여부 | Y |

## 13. TCF_GATEWAY_ROUTE_TARGET 컬럼 명명규칙

| 컬럼 | 설명 | 예시 |
| --- | --- | --- |
| TARGET_ID | Target 식별자 | TGT-PRD-SV-MSA-B-01-9090 |
| ROUTE_ID | Route ID | GW-PRD-SV-ONLINE |
| TARGET_GROUP_ID | Target Group ID | TG-PRD-SV-01 |
| TARGET_NAME | Target 명 | SV 업무 WAS 1호기 |
| TARGET_BASE_URL | Target Base URL | http://msa-b-01:9090 |
| TARGET_CONTEXT_PATH | Context Path | /sv |
| TARGET_FULL_URL | 최종 호출 URL | http://msa-b-01:9090/sv/online |
| WEIGHT | 부하분산 가중치 | 1 |
| PRIORITY | 우선순위 | 1 |
| HEALTH_STATUS | Health 상태 | UP, DOWN |
| ACTIVE_YN | 활성 여부 | Y |
| USE_YN | 사용 여부 | Y |

## 14. Gateway 라우팅 검증 기준

Gateway는 단순히 URL만 보고 전달하지 않는다.다음 항목을 순서대로 검증한다.
## 1. Source Path 업무코드 추출

   /gw/SV/online → SV

## 2. Header businessCode 확인

   header.businessCode = SV

## 3. Path 업무코드와 Header 업무코드 일치 확인

## 4. TCF_GATEWAY_ROUTE 조회

   ENV_CODE + BUSINESS_CODE + ROUTE_TYPE

## 5. Route 사용 여부 확인

   USE_YN = Y

## 6. Target Group 조회

## 7. Active Target 선택

   ACTIVE_YN = Y
   HEALTH_STATUS = UP

## 8. ServiceId 업무코드 검증

   serviceId Prefix = businessCode

## 9. Downstream URL 조립

## 10. Gateway Relay Log 기록

## 15. ServiceId와 Gateway Route 정합성 기준

| 검증 항목 | 기준 |
| --- | --- |
| 차단 예시 | Path 업무코드 |
| /gw/SV/online | /gw/SV/online |
| Header 업무코드 | businessCode = SV |
| businessCode = OM이면 차단 | ServiceId Prefix |
| SV.Customer.selectSummary | OM.User.delete이면 차단 |
| TransactionCode Prefix | SV-INQ-0001 |
| OM-DEL-0001이면 차단 | Route 업무코드 |
| TCF_GATEWAY_ROUTE.BUSINESS_CODE = SV | Route 미등록 시 차단 |
| Target Context | /sv |
| /om이면 차단 |  |

## 16. Gateway 오류코드 명명규칙

Gateway 오류코드는 다음 형식을 사용한다.
E-GW-{CATEGORY}-{NNNN}

| 오류코드 | 의미 |
| --- | --- |
| E-GW-ROUTE-0001 | Route 미등록 |
| E-GW-ROUTE-0002 | Route 사용 중지 |
| E-GW-ROUTE-0003 | Path 업무코드와 Header 업무코드 불일치 |
| E-GW-TGT-0001 | Target 없음 |
| E-GW-TGT-0002 | Target Down |
| E-GW-TGT-0003 | Target 선택 실패 |
| E-GW-SVC-0001 | ServiceId 업무코드 불일치 |
| E-GW-TIME-0001 | Downstream Timeout |
| E-GW-IF-0001 | Downstream 호출 실패 |
| E-GW-AUTH-0001 | Gateway 인증 실패 |

## 17. Gateway 로그 명명규칙

### 17.1 Gateway 로그 파일명

nsight-{envCode}-gw-{logType}.log

예시:

| 로그 유형 | 파일명 | Gateway App Log |
| --- | --- | --- |
| nsight-prd-gw-app.log | Gateway Route Log | nsight-prd-gw-route.log |
| Gateway Interface Log | nsight-prd-gw-if.log | Gateway Error Log |
| nsight-prd-gw-error.log | Gateway Audit Log | nsight-prd-gw-audit.log |

### 17.2 Gateway 이벤트 코드

| 이벤트 코드 | 의미 |
| --- | --- |
| GW-ROUTE-START | Gateway 라우팅 시작 |
| GW-ROUTE-END | Gateway 라우팅 종료 |
| GW-ROUTE-FAIL | Gateway 라우팅 실패 |
| GW-TARGET-SELECT | Target 선택 |
| GW-TARGET-DOWN | Target Down |
| GW-RELAY-REQUEST | Downstream 요청 |
| GW-RELAY-RESPONSE | Downstream 응답 |
| GW-RELAY-TIMEOUT | Downstream Timeout |

## 18. Gateway Relay Log 필드

| 필드 | 설명 |
| --- | --- |
| 예시 | GUID |
| 거래 고유 ID | 7f9c... |
| TRACE_ID | Trace ID |
| TRC202607050001 | ROUTE_ID |
| Route ID | GW-PRD-SV-ONLINE |
| TARGET_GROUP_ID | Target Group |
| TG-PRD-SV-01 | TARGET_ID |
| Target ID | TGT-PRD-SV-MSA-B-01-9090 |
| BUSINESS_CODE | 업무코드 |
| SV | SERVICE_ID |
| ServiceId | SV.Customer.selectSummary |
| TRANSACTION_CODE | 거래코드 |
| SV-INQ-0001 | SOURCE_PATH |
| Gateway 요청 Path | /gw/SV/online |
| TARGET_URL | 호출 대상 URL |
| 마스킹 또는 내부 로그 한정 | HTTP_STATUS |
| HTTP 응답코드 | 200, 500 |
| ELAPSED_MS | Relay 소요시간 |
| 120 | RESULT_STATUS |
| 결과 상태 | SUCCESS, FAIL, TIMEOUT |
| ERROR_CODE | 오류코드 |
| E-GW-TIME-0001 |  |

## 19. Health Check Route 명명규칙

Health Check Route는 일반 업무 Route와 분리한다.
GW-{ENV_CODE}-{BUSINESS_CODE}-HEALTH

| RouteId | Health URL |
| --- | --- |
| GW-PRD-SV-HEALTH | http://msa-b-01:9090/sv/actuator/health |
| GW-PRD-CM-HEALTH | http://msa-c-01:9090/cm/actuator/health |
| GW-PRD-OM-HEALTH | http://om-ap01:8080/om/actuator/health |
Health Check Target은 다음 명명규칙을 사용한다.

| HC-{ENV_CODE}-{BUSINESS_CODE}-{NODE_ID} | |

예시:
HC-PRD-SV-MSA-B-01
HC-PRD-SV-MSA-B-02
HC-PRD-OM-AP01

## 20. DDL 예시

CREATE TABLE TCF_GATEWAY_ROUTE (
    ROUTE_ID              VARCHAR2(50)  NOT NULL,
    ENV_CODE              VARCHAR2(10)  NOT NULL,
    BUSINESS_CODE          VARCHAR2(10)  NOT NULL,
    ROUTE_TYPE             VARCHAR2(20)  NOT NULL,
    SOURCE_PATH            VARCHAR2(200) NOT NULL,
    TARGET_GROUP_ID        VARCHAR2(50)  NOT NULL,
    TARGET_CONTEXT_PATH    VARCHAR2(50)  NOT NULL,
    TARGET_ONLINE_PATH     VARCHAR2(100) NOT NULL,
    AUTH_REQUIRED_YN       CHAR(1) DEFAULT 'Y' NOT NULL,
    SESSION_REQUIRED_YN    CHAR(1) DEFAULT 'Y' NOT NULL,
    JWT_REQUIRED_YN        CHAR(1) DEFAULT 'N' NOT NULL,
    HEADER_CHECK_YN        CHAR(1) DEFAULT 'Y' NOT NULL,
    SERVICE_CHECK_YN       CHAR(1) DEFAULT 'Y' NOT NULL,
    USE_YN                 CHAR(1) DEFAULT 'Y' NOT NULL,
    DESCRIPTION            VARCHAR2(500),
    CREATED_BY             VARCHAR2(50) NOT NULL,
    CREATED_DTM            DATE DEFAULT SYSDATE NOT NULL,
    UPDATED_BY             VARCHAR2(50),
    UPDATED_DTM            DATE,
    CONSTRAINT PK_TCF_GATEWAY_ROUTE
        PRIMARY KEY (ROUTE_ID),
    CONSTRAINT UK_TCF_GATEWAY_ROUTE_01
        UNIQUE (ENV_CODE, BUSINESS_CODE, ROUTE_TYPE)
);

CREATE TABLE TCF_GATEWAY_ROUTE_TARGET (
    TARGET_ID              VARCHAR2(80)  NOT NULL,
    ROUTE_ID               VARCHAR2(50)  NOT NULL,
    TARGET_GROUP_ID        VARCHAR2(50)  NOT NULL,
    TARGET_NAME            VARCHAR2(100) NOT NULL,
    TARGET_BASE_URL        VARCHAR2(300) NOT NULL,
    TARGET_CONTEXT_PATH    VARCHAR2(50)  NOT NULL,
    TARGET_FULL_URL        VARCHAR2(400) NOT NULL,
    WEIGHT                 NUMBER(5) DEFAULT 1 NOT NULL,
    PRIORITY               NUMBER(5) DEFAULT 1 NOT NULL,
    HEALTH_STATUS          VARCHAR2(20) DEFAULT 'UNKNOWN' NOT NULL,
    ACTIVE_YN              CHAR(1) DEFAULT 'Y' NOT NULL,
    USE_YN                 CHAR(1) DEFAULT 'Y' NOT NULL,
    CREATED_BY             VARCHAR2(50) NOT NULL,
    CREATED_DTM            DATE DEFAULT SYSDATE NOT NULL,
    UPDATED_BY             VARCHAR2(50),
    UPDATED_DTM            DATE,
    CONSTRAINT PK_TCF_GATEWAY_ROUTE_TARGET
        PRIMARY KEY (TARGET_ID)
);

## 21. 명명 예시 종합

| 구분 | 표준명 | 설명 |
| --- | --- | --- |
| Gateway URL | /gw/SV/online | SV 업무 Gateway 진입점 |
| Downstream URL | /sv/online | SV 업무 WAR 진입점 |
| RouteId | GW-PRD-SV-ONLINE | 운영 SV 온라인 Route |
| TargetGroupId | TG-PRD-SV-01 | 운영 SV Target Group |
| TargetId | TGT-PRD-SV-MSA-B-01-9090 | SV 1번 Target |
| Health RouteId | GW-PRD-SV-HEALTH | SV Health Check Route |
| ErrorCode | E-GW-ROUTE-0001 | Gateway Route 미등록 |
| EventCode | GW-ROUTE-START | 라우팅 시작 이벤트 |
| Log File | nsight-prd-gw-route.log | Gateway Route 로그 |
| DB Table | TCF_GATEWAY_ROUTE | Gateway 논리 Route 테이블 |
| DB Table | TCF_GATEWAY_ROUTE_TARGET | Gateway Target 테이블 |

## 22. 금지 규칙

| 금지 예시 | 문제 | 표준 예시 |
| --- | --- | --- |
| /service1/online | 업무코드 식별 불가 | /gw/SV/online |
| /gateway/customer | ServiceId 방식과 불일치 | /gw/SV/online |
| ROUTE001 | 환경·업무 식별 불가 | GW-PRD-SV-ONLINE |
| SV_TARGET1 | 환경·서버·Port 식별 부족 | TGT-PRD-SV-MSA-B-01-9090 |
| http://server1:8080 직접 하드코딩 | 운영 변경관리 불가 | TCF_GATEWAY_ROUTE_TARGET 기준 |
| Path는 SV, Header는 OM | Header 위변조 가능 | 즉시 차단 |
| 운영 Route에 TEST Target 연결 | 운영 장애 위험 | ENV_CODE 기준 분리 |
| Gateway에서 모든 업무를 단일 Target으로 전달 | 장애격리 불가 | 업무코드별 Target Group 분리 |

## 23. 검토 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| RouteId가 GW-{ENV_CODE}-{BUSINESS_CODE}-{ROUTE_TYPE} 형식인가? | □ |
| TargetGroupId가 TG-{ENV_CODE}-{BUSINESS_CODE}-{GROUP_NO} 형식인가? | □ |
| TargetId가 환경, 업무, 서버, Port를 식별할 수 있는가? | □ |
| Gateway URL의 업무코드와 Header businessCode가 일치하는가? | □ |
| ServiceId Prefix와 businessCode가 일치하는가? | □ |
| TransactionCode Prefix와 businessCode가 일치하는가? | □ |
| local/dev/stg/prd Route가 분리되어 있는가? | □ |
| 운영 Route가 개발 Target을 바라보지 않는가? | □ |
| Target Health 상태가 Route 선택에 반영되는가? | □ |
| Gateway Relay Log에 routeId, targetId, guid, serviceId가 남는가? | □ |
| 미등록 Route 요청이 차단되는가? | □ |
| Route 변경 이력이 OM 또는 배포관리에서 관리되는가? | □ |

## 24. 마무리말

Gateway 라우팅 명명규칙의 핵심은 다음과 같다.
Gateway Route는 업무코드를 기준으로 찾고,
Target은 환경과 서버를 기준으로 선택하며,
ServiceId는 실제 실행 거래를 검증한다.

따라서 NSIGHT Gateway 라우팅은 다음 네 가지 이름을 반드시 표준화해야 한다.
ROUTE_ID        = GW-{ENV_CODE}-{BUSINESS_CODE}-{ROUTE_TYPE}
TARGET_GROUP_ID = TG-{ENV_CODE}-{BUSINESS_CODE}-{GROUP_NO}
TARGET_ID       = TGT-{ENV_CODE}-{BUSINESS_CODE}-{NODE_ID}-{PORT}
SOURCE_PATH     = /gw/{BUSINESS_CODE}/online

이 기준을 적용하면 운영자는 Gateway 로그의 routeId와 targetId만 보고도 어느 환경의 어느 업무가 어느 WAS로 라우팅되었는지 즉시 추적할 수 있다.

---

## 관련 Manual 장

- [22장](./22-Online-Endpoint-기준.md)

## 원본

- [`znsight-guide-word`](../znsight-guide-word/) — `명명규칙 상세 (18).docx`
