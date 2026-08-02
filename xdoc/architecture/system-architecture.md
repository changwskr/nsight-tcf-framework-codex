# 시스템 아키텍처

## 모듈 계층

```text
Foundation
  tcf-util
    └─ tcf-core
         └─ tcf-web
              ├─ *-service
              ├─ tcf-om
              ├─ tcf-batch
              ├─ tcf-ui / tcf-uj
              ├─ tcf-gateway
              └─ tcf-jwt

Optional capabilities
  tcf-cache, tcf-eai, tcf-help

Development tools
  tcf-ai-methodology, tcf-ai-crud-meoy
```

### 기반 모듈

| 모듈 | 책임 |
|---|---|
| `tcf-util` | Spring 비의존 공통 유틸리티와 상수 |
| `tcf-core` | 표준 전문, STF/TCF/ETF, Dispatcher와 공통 거래 정책 |
| `tcf-web` | HTTP 진입점, 자동 설정, DataSource와 MyBatis 통합 |
| `tcf-eai` | 서비스 간 표준 HTTP/JSON 연동 |
| `tcf-cache` | EhCache와 Spring Cache 지원 |

### 플랫폼 및 업무 모듈

| 구분 | 모듈 |
|---|---|
| 업무 WAR | `av-service`, `ln-service`, `ic-service` 등 `*-service` |
| 운영 관리 | `tcf-om`, `tcf-oc`, `tcf-batch` |
| 채널 | `tcf-ui`, `tcf-uj`, `tcf-gateway` |
| 인증 | `tcf-jwt` |
| 도움말 | `tcf-help` |
| 통합 컨테이너 | `ztomcat` |

## 온라인 거래 흐름

```text
Client
  → POST /online 또는 /{businessCode}/online
  → OnlineTransactionController / TcfGateway
  → TCF.process
      → STF.preProcess
          → Header validation
          → GUID / Trace ID 생성
          → Session / Authentication / Authorization
          → Transaction control
          → Timeout policy
          → Idempotency
          → Transaction log START
      → OnlineTransactionTimeoutExecutor
          → TransactionDispatcher
              → TransactionHandler
                  → Facade → Service → Rule → DAO/Mapper
      → ETF
          → Success / Business Fail / System Error
          → Transaction log END
          → Audit / Metric / Idempotency 상태 갱신
  → StandardResponse
```

`TCF.process()`의 `finally` 구간은 Transaction, Authentication, Timeout Context와 MDC를 정리한다. ThreadLocal 기반 컨텍스트를 추가할 때 동일한 정리 보장이 필요하다.

## 업무 애플리케이션 계층

```text
entry/handler
  → entry/facade
    → application/service
      → application/rule
        → persistence/dao 또는 persistence/mapper
```

- Handler는 `serviceId` 분기와 요청 전달만 담당한다.
- Facade는 DTO 변환, 유스케이스 조정과 트랜잭션 경계를 담당한다.
- Service는 업무 흐름을 구성한다.
- Rule은 검증과 계산 같은 업무 규칙을 캡슐화한다.
- DAO/Mapper는 영속성 세부사항을 담당한다.
- 외부 시스템 호출은 `client` 계층과 `tcf-eai`를 사용한다.

## 데이터와 정책 저장소

| 관심사 | 대표 저장소/테이블 |
|---|---|
| 거래 통제 | `TCF_TRANSACTION_CONTROL` |
| 타임아웃 정책 | `TCF_SERVICE_TIMEOUT_POLICY` |
| 거래 로그 | `TCF_TRANSACTION_LOG` |
| 세션 | `SPRING_SESSION` |
| 서비스 카탈로그 | `OM_SERVICE_CATALOG` |
| 오류 코드 | `OM_ERROR_CODE` |

로컬 환경은 H2 파일 DB를 사용한다. 운영 환경에서는 DataSource 분리 여부와 트랜잭션 경계를 환경 설정으로 명시해야 한다.

## 실행 및 배포 모델

### 독립 실행

각 업무 모듈은 `bootRun`으로 별도 포트에서 실행할 수 있다.

```powershell
.\gradlew.bat :sv-service:bootRun
```

### 통합 배포

`buildZtomcatWars`가 통합 대상 WAR를 생성하고 `ztomcat`이 8080 포트에서 여러 Context를 제공한다.

```powershell
.\gradlew.bat buildZtomcatWars
```

## 아키텍처 변경 규칙

- 기반 모듈은 상위 업무 모듈에 의존하지 않는다.
- 공통 기능을 특정 업무 서비스에 복제하지 않는다.
- 모듈 간 순환 의존성을 허용하지 않는다.
- 표준 전문 또는 Handler 계약 변경은 전체 업무 WAR 영향도를 검토한다.
- 설계 문서와 코드가 불일치하면 코드 동작을 확인하고 문서를 함께 정정한다.

