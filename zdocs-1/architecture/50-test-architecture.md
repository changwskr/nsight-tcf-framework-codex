# 50. 테스트 아키텍처 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 50 |
| 제목 | Test Architecture Design |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [14-online-arc.md](14-online-arc.md), [05-exception.md](05-exception.md), [37-transaction-log.md](37-transaction-log.md), [49-release-strategy.md](49-release-strategy.md) |
| 상세 매뉴얼 | [54-단위-테스트-기준.md](../../znsight-man/54-단위-테스트-기준.md) ~ [60-장애-테스트-기준.md](../../znsight-man/60-장애-테스트-기준.md), [78-테스트-코드-샘플.md](../../znsight-man/78-테스트-코드-샘플.md) |
| 대상 | 프레임워크·업무 개발자, QA, 릴리즈 담당자 |

---

## 1. 문서 목적

본 문서는 NSIGHT TCF의 **테스트 피라미드·계층·도구·환경·품질 Gate**를 아키텍처 관점에서 정의한다.

| 구분 | 담당 문서 |
|------|-----------|
| 계층별 테스트 상세 | [54](54-단위-테스트-기준.md)~[60](60-장애-테스트-기준.md) (znsight-man) |
| TCF 거래·Smoke | [56-TCF-거래-테스트-기준.md](../../znsight-man/56-TCF-거래-테스트-기준.md) |
| 릴리즈 Gate | [49-release-strategy.md](49-release-strategy.md) |
| **테스트 아키텍처 (본 문서)** | 피라미드·범위·도구·CI 연동 |

핵심 문장:

> NSIGHT TCF 테스트는 **serviceId 단위 거래**가 중심이다. 단위 테스트는 계층 책임을, 통합·Smoke는 **TCF.process() End-to-End**와 `TCF_TX_LOG` 정합을 검증한다.

---

## 2. 테스트 피라미드

```text
                    ┌─────────────┐
                    │  Smoke/E2E  │  배포 후·릴리즈 전 (대표 serviceId)
                    ├─────────────┤
                    │  통합/거래   │  MockMvc, RestAssured, tcf-ui
                    ├─────────────┤
                    │ Mapper/SQL  │  H2 Test DB, SQL 검증
                    ├─────────────┤
                    │  단위 테스트 │  JUnit5 + Mockito (다수)
                    └─────────────┘
```

| 층 | 비율(목표) | 실행 시점 |
|----|------------|-----------|
| 단위 | 70%+ | 개발 중·MR·CI |
| Mapper/SQL | 10% | Mapper 변경 시 |
| 통합·거래 | 15% | 기능 완료·develop merge |
| Smoke/E2E | 5% | ztomcat·스테이징·배포 후 |

---

## 3. 테스트 유형 정의

### 3.1 유형 매트릭스

| 유형 | 목적 | Spring Context | DB | HTTP |
|------|------|:--------------:|:--:|:----:|
| **단위** | Rule·Validator·Policy | ✕ | Mock | ✕ |
| **Handler 단위** | Facade 위임·body 변환 | ✕ | Mock | ✕ |
| **Mapper** | SQL·파라미터 매핑 | △ | H2 | ✕ |
| **통합** | WAR 내 E2E | ● | H2 file/mem | MockMvc |
| **TCF 거래** | serviceId 1건 정합 | ● | H2 | POST `/online` |
| **연동** | tcf-eai IC↔SV | ● | H2 | RestClient |
| **Smoke** | 대표 거래 생존 | ● | dev/stg | curl·UI |
| **성능** | TPS·P95 | ● | perf DB | 부하 도구 |
| **보안** | 인증·권한·통제 | ● | test | OWASP 시나리오 |
| **장애** | Timeout·롤백 | ● | 장애 주입 | Chaos (목표) |

### 3.2 TCF 거래 테스트 vs 통합 테스트

| 구분 | 통합 테스트 | TCF 거래 테스트 |
|------|-------------|-----------------|
| 질문 | 업무 기능이 맞는가? | TCF 표준 거래로 정상인가? |
| 단위 | 기능·API | **serviceId 1건** |
| 검증 | body·DB 결과 | `resultCode`, `guid`, `TCF_TX_LOG` |
| 도구 | MockMvc | + OM 거래로그·tcf-ui |

상세: [56-TCF-거래-테스트-기준.md](../../znsight-man/56-TCF-거래-테스트-기준.md)

---

## 4. 계층별 테스트 책임

```text
[채널/UI]     Smoke, E2E (tcf-ui Relay)
     ↓
[Controller]  MockMvc POST /online
     ↓
[TCF]         STF·Dispatcher·ETF — core 단위 + 통합
     ↓
[Handler]     Mockito → Facade 위임
     ↓
[Facade]      @Transactional 경계 (통합)
     ↓
[Service/Rule] 단위 — BusinessException 분기
     ↓
[DAO/Mapper]  Mapper test (H2)
     ↓
[Client/EAI]  tcf-eai mock HTTP 또는 IC+SV bootRun
```

| 계층 | 단위 검증 | 통합 검증 |
|------|-----------|-----------|
| Handler | body→Facade 호출 | serviceId 라우팅 |
| Rule | 입력·상태·예외 | — |
| Service | Mock DAO | DB 결과 |
| STF/ETF | Validator 단위 | Header·TX_LOG |
| Dispatcher | 등록 테이블 | 미등록 `E-COM-DISP-0001` |

---

## 5. 기술 스택

| 항목 | 선택 |
|------|------|
| 프레임워크 | **JUnit 5** (`useJUnitPlatform` — 루트 `build.gradle`) |
| Mock | **Mockito** |
| Assertion | **AssertJ** / JUnit Assertions |
| Spring | `spring-boot-starter-test` (전 모듈) |
| HTTP | **MockMvc** (`@AutoConfigureMockMvc`) |
| API (목표) | RestAssured |
| DB | H2 (`MODE=Oracle`), `transactionLogJdbcTemplate` |

### 5.1 코드베이스 테스트 위치 (현행)

| 모듈 | 테스트 수 | 대표 클래스 |
|------|-----------|-------------|
| `tcf-core` | 9+ | `TransactionControlTest`, `AuthenticationContextValidatorTest`, `TransactionLogServiceTest` |
| `tcf-web` | 4 | `PolicyDrivenQueryTimeoutInterceptorTest`, `JdbcTransactionControlRepositoryTest` |
| `tcf-cache` | 1 | `TcfCacheConfigurationTest` |
| `sv-service` | 1 | `SvTransactionLogIntegrationTest` |
| 업무 WAR | 확대 필요 | Handler·Service 샘플 ([78](78-테스트-코드-샘플.md)) |

실행:

```bash
gradle :tcf-core:test
gradle :tcf-web:test
gradle :sv-service:test
gradle test   # 전 모듈 (bootRun 없는 lib + test 있는 WAR)
```

---

## 6. 표준 시나리오 (TCF 거래)

### 6.1 필수 시나리오 ID

| ID | 시나리오 | 기대 |
|----|----------|------|
| TX-001 | 정상 INQUIRY | `resultCode=S0000`, TX_LOG +1 |
| TX-002 | Header 누락 | `E-TCF-HDR-*` |
| TX-003 | 미등록 serviceId | `E-COM-DISP-0001` |
| TX-004 | 거래통제 미등록 | `E-TCF-CTL-*` |
| TX-005 | BusinessException | `resultCode=E0001`, errorCode 유지 |
| TX-006 | Timeout (정책) | `E-TIMEOUT-*` |
| TX-007 | 멱등 중복 | Idempotency 거부 |
| TX-008 | 감사 대상 | `audit.log` / `OM_AUDIT_LOG` |
| TX-009 | JWT Bearer (해당 WAR) | 2차 STF 정합 |
| TX-010 | 서비스 연동 | 동일 `guid`, 양쪽 TX_LOG |

### 6.2 샘플 전문 (SoT)

| 경로 | 용도 |
|------|------|
| `tcf-ui/src/main/resources/sample-requests/*.json` | UI 거래 테스트·Smoke |
| `sv-service/src/test/resources/sv-sample-inquiry.json` | MockMvc 통합 |
| `docs/sample-requests/` | 문서·curl 예제 |

샘플은 `OM_SERVICE_CATALOG`·Handler와 **동기** 유지 ([47-data-governance.md](47-data-governance.md)).

---

## 7. 테스트 환경

### 7.1 프로파일

| 프로파일 | 용도 | DB |
|----------|------|-----|
| `local` | bootRun·단위·MockMvc | H2 mem + txlog file |
| `dev` | ztomcat 통합 | `tcf-cicd/dev` |
| `stg` | 통합·성능 (목표) | 운영 유사 |
| `prod` | **Smoke만** — 직접 통합 테스트 금지 | 운영 |

bootRun 테스트 공통: `nsight.txlog.path=./data/nsight-txlog` (루트 `build.gradle`).

### 7.2 ztomcat 검증

```bash
cd ztomcat
./deploy-wars.sh all
./verify-deploy.sh    # 12 context health
```

Health 4단계 (릴리즈): Liveness → Readiness → Deep → **Smoke serviceId** ([49-release-strategy.md](49-release-strategy.md)).

### 7.3 tcf-ui 거래 테스트

- `online-multi.js` — 다중 serviceId POST
- `POST /api/relay/{code}/online` — Relay 경로 Smoke
- `POST /api/multi/relay/{code}/online` — 배치 호출

---

## 8. Mapper·SQL 테스트

| 항목 | 기준 |
|------|------|
| 대상 | `mapper/{code}/*.xml` |
| DB | H2 `MODE=Oracle` — 업무 WAR primary DS |
| 검증 | SQL id, 파라미터, 결과 Map |
| Timeout | `PolicyDrivenQueryTimeoutInterceptor` (통합) |
| 실행계획 | 운영 DB EXPLAIN (목표) |

상세: [57-MyBatis-SQL-테스트-기준.md](../../znsight-man/57-MyBatis-SQL-테스트-기준.md)

---

## 9. 연동·보안·성능·장애

| 영역 | 문서 | 아키텍처 요약 |
|------|------|---------------|
| 서비스 연동 | [46-service-integration-contract.md](46-service-integration-contract.md) | ic+sv bootRun E2E, `IntegrationBusinessException` |
| 보안 | [58-보안-테스트-기준.md](../../znsight-man/58-보안-테스트-기준.md) | 세션·JWT·거래통제·SQL Injection |
| 성능 | [59-성능-테스트-기준.md](../../znsight-man/59-성능-테스트-기준.md) | TPS 720, P95 3초 목표 |
| 장애 | [60-장애-테스트-기준.md](../../znsight-man/60-장애-테스트-기준.md) | TXLOG 장애 시 거래 지속 등 |

---

## 10. CI/CD 품질 Gate

### 10.1 MR (develop)

```text
gradle compileJava → :tcf-core:test → :tcf-web:test
  → [changed-modules]:test
  → (목표) SonarQube quality gate
```

### 10.2 릴리즈 (release/*)

```text
gradle buildZtomcatWars
  → verify-deploy (health)
  → Smoke: 대표 serviceId per 업무코드
  → (목표) 보안 스캔
```

### 10.3 배포 후

- Smoke 1건/업무코드
- `TX_START`/`TX_END` 쌍 ([44-observability.md](44-observability.md))
- 실패 시 롤백 — [67-롤백-절차.md](../../znsight-man/67-롤백-절차.md)

---

## 11. 커버리지·품질 목표

| 항목 | 목표 (설계) |
|------|-------------|
| `tcf-core` line coverage | 70%+ |
| 신규 Handler | 단위 + 거래 1건 |
| 신규 serviceId | sample-requests JSON + Smoke |
| Mapper 변경 | SQL 테스트 또는 통합 |
| 회귀 | develop merge 전 affected module test |

SonarQube·JaCoCo — CI 목표 ([65-CICD-파이프라인-기준.md](../../znsight-man/65-CICD-파이프라인-기준.md)).

---

## 12. 신규 serviceId 테스트 체크리스트

- [ ] Handler 단위 (Mockito)
- [ ] Rule 오류 분기 (`BusinessException`)
- [ ] `sample-requests/{code}-*.json`
- [ ] MockMvc 또는 curl `POST /{code}/online`
- [ ] `resultCode=S0000` (정상)
- [ ] `TCF_TX_LOG` row (`guid`, `SERVICE_ID`)
- [ ] 거래통제·Catalog OM 등록과 정합
- [ ] (연동 시) tcf-eai + 피호출 Smoke
- [ ] MR CI 통과

---

## 13. 현행 vs 목표 (Gap)

| 항목 | 현행 | 목표 |
|------|------|------|
| 업무 WAR 테스트 | sv 1건 위주 | 업무코드별 Handler+거래 |
| RestAssured | 문서만 | CI 통합 시나리오 |
| Testcontainers | 미사용 | Oracle 호환 통합 (선택) |
| SonarQube | 목표 | MR 필수 Gate |
| 자동 Smoke | 수동·UI | 파이프라인 post-deploy |
| 성능 | 수동 | stg 정기 부하 |

---

## 14. 관련 소스

| 경로 | 설명 |
|------|------|
| `tcf-core/src/test/...` | 프레임워크 단위 |
| `tcf-web/src/test/...` | Web·Policy |
| `sv-service/src/test/.../SvTransactionLogIntegrationTest.java` | MockMvc+TX_LOG |
| `tcf-ui/.../sample-requests/` | Smoke JSON |
| `ztomcat/verify-deploy.*` | Health Smoke |
| `build.gradle` `subprojects` | JUnit Platform |

---

← [49-release-strategy.md](49-release-strategy.md) · [51-api-gateway.md](51-api-gateway.md) →
