# tcf-web — TCF HTTP 레이어

`TCF.process()`를 HTTP 엔드포인트로 노출하고, REST 어댑터·필터·전역 예외 처리·거래로그 DB 연동·WAR 배포 부트스트랩을 제공합니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-web` |
| 패키지 | `com.nh.nsight.tcf.web` |
| 산출물 | JAR (라이브러리) |

## 주요 구성

| 구성요소 | 설명 |
|----------|------|
| `OnlineTransactionController` | `POST /online`, `POST /{businessCode}/online` |
| `TcfGateway` | REST·multipart 등 비표준 진입점 → `TCF.process()` 위임 |
| `GuidMdcCleanupFilter` | 요청 종료 시 MDC·Context 정리 |
| `GlobalStandardExceptionHandler` | 표준 오류 응답 변환 |
| `TcfJwtAuthenticationFilter` | `/online` 요청 Bearer JWT 검증 (옵션) |
| `TcfAutoConfiguration` | Spring Boot 자동 구성 |
| `TcfPrimaryDataSourceAutoConfiguration` | 다중 DS 환경 기본 DataSource |
| `TcfTransactionLogDataSourceConfiguration` | H2 기반 공유 거래로그 DB |
| `TcfTransactionControlConfiguration` | 거래통제 JDBC·스키마 자동 생성 |
| `TcfTimeoutPolicyConfiguration` | Timeout 정책 JDBC·스키마 자동 생성 |
| `TcfTimeoutTransactionManagementConfiguration` | `@Transactional` TX timeout AOP |
| `TcfOnlineTimeoutConfiguration` | 온라인 timeout 워커 스레드 풀 |
| `PolicyDrivenQueryTimeoutInterceptor` | MyBatis statement timeout |
| `TcfDataSourceLifecycleConfiguration` | HikariCP 종료·풀 재사용 |
| `NsightWarBootstrap` | WAR(Tomcat) 배포 시 context path·프로파일 초기화 (`com.nh.nsight.tcf.web.support`) |

## 패키지 구조

```text
com.nh.nsight.tcf.web
├── config/                  TcfAutoConfiguration, DataSource·Timeout·TransactionControl
├── entry/
│   ├── web/                 OnlineTransactionController, GlobalStandardExceptionHandler, GuidMdcCleanupFilter
│   └── facade/              TcfGateway (REST·multipart → TCF.process)
├── application/rule/        PolicyDrivenTransactionExecutor, PolicyDrivenTransactionAttributeSource
├── persistence/
│   ├── dao/                 JdbcTransactionLogRepository, JdbcTimeoutPolicyRepository, JdbcTransactionControlRepository
│   └── mapper/              MyBatis mapper 패키지
└── support/                 NsightWarBootstrap, PolicyDrivenQueryTimeoutInterceptor, MappedStatementSupport
```

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/online` | 표준 JSON 거래 (header.serviceId 필수) |
| POST | `/{businessCode}/online` | 업무코드 경로 기반 거래 |

Tomcat 예: `POST http://localhost:8080/sv/online`

## JWT 필터 (옵션)

`nsight.tcf.web.jwt.enabled=true` 일 때 `/online`, `/{businessCode}/online` 요청에 대해 Bearer JWT를 검증합니다.

```yaml
nsight:
  tcf:
    web:
      jwt:
        enabled: true
        jwk-set-uri: http://127.0.0.1:8110/.well-known/jwks.json
        issuer: NSIGHT-AUTH
        audience: NSIGHT-MP
        header-name: Authorization
        token-prefix: Bearer
        required-for-online: true
```

- 검증 통과 시 `AuthenticatedUserContext`를 request attribute와 ThreadLocal holder에 저장
- `AuthenticationContextHolder`(tcf-core)에도 동일 claim을 저장하여 STF 2차 검증에 사용
- 검증 실패 시 401 JSON(`errorCode`, `message`) 반환

STF는 `AuthenticationContextValidator`가 JWT claim과 전문 Header(`userId`, `branchId`, `channelId`) 정합성을 확인합니다.

## TcfGateway 사용 예

업무 모듈의 REST Controller가 서비스를 직접 호출하지 않고 TCF 파이프라인을 거치도록 할 때 사용합니다.

```java
StandardResponse<Object> response = tcfGateway.invoke(
    TcfInvokeRequest.builder("UD.File.list", "UD-LST-0001", "INQUIRY")
        .body(body)
        .userId(userId)
        .clientIp(clientIp)
        .build()
);
```

## 거래로그 DB

bootRun·Tomcat 공통으로 프로젝트 `data/nsight-txlog/` H2 파일을 공유합니다.

- 시스템 프로퍼티: `nsight.txlog.path`
- bootRun: Gradle `bootRun`이 자동 설정
- ztomcat: `ztomcat/conf/setenv.bat`의 `-Dnsight.txlog.path`

## dev / prod 프로파일 (Tomcat WAR)

- `application-dev.yml` — ztomcat·개발 서버 (`dev` 프로파일)
- `application-prod.yml` — 운영 (`prod` 프로파일, `NSIGHT_GATEWAY_BASE_URL`)

상세: [docs/architecture/25-env-profile.md](../zdocs-1/architecture/25-env-profile.md)

## 의존 관계

```text
tcf-util → tcf-core → tcf-web → *-service / tcf-om / tcf-batch
```

## 빌드

```bash
gradle :tcf-web:build
```
