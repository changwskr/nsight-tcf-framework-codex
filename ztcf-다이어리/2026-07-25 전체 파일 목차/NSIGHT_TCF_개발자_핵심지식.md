# NSIGHT TCF 개발자 핵심 지식

> 작성 기준: `nsight-tcf-framework`의 Markdown, Gradle 설정, Java/XML/YAML 및 스크립트  
> 제외 범위: `.doc`, `.docx`, `.docm`, `.dot`, `.dotx` 등 Word 파일은 열거나 내용 추출하지 않음  
> 우선순위: 현재 소스·루트 빌드 설정 > 루트 README > 세부 Markdown 설계 문서 > 과거·샘플 문서  
> 기준일: 2026-07-25

## 1. 먼저 기억할 10가지

1. NSIGHT는 단순한 Spring Boot REST 애플리케이션이 아니라 **ServiceId 중심의 거래 처리 프레임워크**다.
2. 외부 요청은 `header + body` 표준 전문으로 들어오며, 실행할 업무는 URL이 아니라 `header.serviceId`로 결정된다.
3. 공통 처리 흐름은 `STF → Dispatcher/Handler → ETF`다.
4. 업무 개발의 기본 호출 구조는 `Handler → Facade → Service → Rule → DAO/Mapper`다.
5. Handler는 요청 변환과 위임만 담당하도록 얇게 유지한다.
6. 업무 WAR끼리 Java 의존성을 직접 연결하지 않는다. 타 업무 호출은 표준 전문 기반의 연동 Client를 사용한다.
7. ServiceId, 거래코드, Timeout, 권한, 거래통제, 오류코드는 코드만 작성해서 끝나지 않으며 OM 기준정보와 함께 관리한다.
8. Timeout은 온라인 전체, 트랜잭션, DB Query의 여러 계층에 일관되게 적용한다.
9. 장애 추적의 기본 키는 `GUID/TraceId + ServiceId + TransactionCode + SQL ID`다.
10. 운영 반영물은 모듈별 임의 파일 교체가 아니라 검증된 WAR 단위로 배포한다.

## 2. 시스템과 요청 처리 구조

대표적인 운영 요청 경로는 다음과 같다.

```text
사용자·외부 API
  → GSLB / L4
  → Apache HTTPD
  → Gateway 또는 업무 WAR
  → POST /{업무코드}/online
  → TCF.process()
      → STF.preProcess()
      → Timeout Executor
      → TransactionDispatcher
      → TransactionHandler
      → Facade → Service → Rule → DAO/Mapper
      → ETF.success / businessFail / systemError
  → StandardResponse
```

공통 프레임워크가 담당하는 항목:

- Header 필수값 검증
- GUID와 TraceId 생성·전파
- 세션, 인증, 권한 확인
- 멱등성 및 중복 요청 통제
- 등록 거래와 차단 상태 확인
- ServiceId별 Timeout 정책 적용
- 거래로그의 `PROCESSING → SUCCESS/FAIL` 상태 관리
- 성공·업무 오류·시스템 오류를 표준 응답으로 조립
- 감사로그와 메트릭 기록

업무 개발자가 담당하는 항목:

- 요청 DTO 변환과 입력값 검증
- 유스케이스 및 업무 규칙 구현
- DB 조회·변경과 외부/타 업무 연동
- 업무 오류코드 사용
- 단위·통합·거래 테스트
- ServiceId와 OM 기준정보의 정합성 확보

## 3. 현재 모듈 구성

현재 `settings.gradle`과 루트 README 기준 주요 모듈은 다음과 같다.

| 구분 | 모듈 | 책임 |
|---|---|---|
| 공통 유틸 | `tcf-util` | 문자열, 날짜, 마스킹 등 최소 의존 유틸 |
| 거래 엔진 | `tcf-core` | 표준 전문, STF/TCF/ETF, Dispatcher, 거래통제, Timeout, 예외 |
| Web 공통 | `tcf-web` | `/online`, Controller, Filter, Web 예외 처리, WAR bootstrap |
| 업무 간 연동 | `tcf-eai` | 표준 전문 기반 HTTP/JSON 호출 |
| Cache | `tcf-cache` | 공통코드·정책 등 공통 Cache |
| 운영관리 | `tcf-om` | 사용자·권한·ServiceId·거래통제·Timeout·오류코드·파일 |
| 용량/환경 | `tcf-oc` | 용량 산정과 통합 환경설정 |
| Gateway/JWT | `tcf-gateway`, `tcf-jwt` | 라우팅·인증 관문, 토큰 발급·JWKS |
| Batch/UI | `tcf-batch`, `tcf-ui`, `tcf-uj` | 수집·배치, 관리/거래 테스트 UI |
| 업무 WAR | `ic/pc/ms/sv/pd/eb/ep/ss/mg-service` | 업무별 Handler 이하 계층 |
| 로컬 통합 | `ztomcat` | 여러 WAR를 8080에서 통합 실행 |

`om-service`는 레거시로 문서화되어 있으며 신규 개발·배포는 `tcf-om`을 기준으로 한다.

### 의존성 원칙

개념적인 의존 방향은 아래와 같다.

```text
tcf-util
  → tcf-core
    → tcf-web
      → 선택 공통 모듈(tcf-cache, tcf-eai)
        → 업무 WAR / OM / Batch / Gateway / JWT
```

금지해야 할 대표 패턴:

- `tcf-core → sv-service`처럼 공통 모듈이 업무 구현을 참조
- `ic-service → sv-service`처럼 업무 WAR끼리 직접 참조
- 업무 서비스가 Gateway 또는 JWT 구현 클래스를 직접 참조
- `tcf-util`에 Spring Web, DB 또는 업무 규칙 의존성을 추가

## 4. 업무 애플리케이션 6계층

현재 소스의 기본 패키지 루트는 일반적으로 다음 형태다.

```text
com.nh.nsight.marketing.{업무코드소문자}
├─ entry/
│  ├─ handler/       ServiceId의 실행 진입점
│  ├─ facade/        유스케이스 조립, 트랜잭션 경계
│  └─ web/           별도 REST 진입점이 필요한 경우
├─ application/
│  ├─ service/       업무 처리 절차
│  ├─ rule/          검증, 판단, 계산
│  └─ scheduler/     스케줄 업무가 있는 경우
├─ persistence/
│  ├─ dao/           Mapper 호출 캡슐화
│  └─ mapper/        MyBatis Mapper
├─ client/           타 업무·외부 API Client
├─ config/           Spring 설정과 Properties
└─ support/          업무 내부 상수·도우미
```

| 계층 | 해야 하는 일 | 넣지 말아야 할 일 |
|---|---|---|
| Handler | ServiceId 등록, Body 변환, Facade 호출 | SQL, 복잡한 규칙, 임의 응답 전문 조립 |
| Facade | 유스케이스 조립, 트랜잭션 경계 | SQL 직접 실행 |
| Service | 처리 순서 제어, Rule/DAO/Client 호출 | HTTP 응답 생성 |
| Rule | 업무 검증·판단·계산 | Mapper 직접 호출 남발 |
| DAO | Mapper 호출과 영속성 예외 경계 | 업무 판단 |
| Mapper/XML | SQL 실행과 결과 매핑 | 화면 로직, 복잡한 업무 정책 |

Handler는 도메인당 한 개를 두고 `serviceIds()`로 관련 거래를 묶을 수 있다. 실제 분기는 명시적으로 처리하고, Handler가 거대한 업무 클래스가 되지 않도록 한다.

## 5. 식별자와 명명 규칙

### 업무코드, Context, WAR

| 항목 | 규칙 | 예 |
|---|---|---|
| 업무코드 | 대문자 2~3자리 | `SV`, `IC`, `OM` |
| Context | 업무코드 소문자 | `/sv`, `/ic`, `/om` |
| WAR | Context와 같은 이름 | `sv.war`, `ic.war`, `om.war` |
| Endpoint | 업무 Context 아래 공통 진입점 | `POST /sv/online` |

### ServiceId

형식:

```text
{BusinessCode}.{Domain}.{action}
```

예:

```text
SV.Customer.selectSummary
OM.User.inquiry
MG.Message.send
```

원칙:

- BusinessCode는 대문자다.
- Domain은 명확한 영문 명사와 PascalCase를 사용한다.
- action은 동사로 시작하는 camelCase다.
- `get`, `proc`, `manage`처럼 의미가 불명확한 단어와 불필요한 약어를 피한다.
- ServiceId는 코드의 `TransactionHandler.serviceIds()`와 OM Service Catalog에 모두 일치해야 한다.

### 거래코드

형식:

```text
{업무코드}-{처리유형}-{4자리 일련번호}
```

| 유형 | 의미 | 예 |
|---|---|---|
| `INQ` | 조회 | `SV-INQ-0001` |
| `REG` | 등록 | `OM-REG-0001` |
| `UPD` | 수정 | `OM-UPD-0001` |
| `DEL` | 삭제 | `OM-DEL-0001` |
| `EXE` | 실행 | `BT-EXE-0001` |
| `UPL` / `DWN` | 업로드 / 다운로드 | `UD-UPL-0001` |
| `APR` | 승인 | `OM-APR-0001` |
| `SND` | 발송 | `MG-SND-0001` |

ServiceId는 “무엇을 실행할지”, 거래코드는 “운영에서 어떤 거래로 추적·통제할지”를 나타낸다. 둘을 같은 개념으로 취급하지 않는다.

### 클래스와 SQL

| 대상 | 규칙 예 |
|---|---|
| Handler | `SvCustomerHandler` 또는 행위를 포함한 명확한 Handler명 |
| Facade | `SvCustomerFacade` |
| Service | `SvCustomerService` |
| Rule | `SvCustomerRule` |
| DAO | `SvCustomerDao` |
| Mapper | `SvCustomerMapper` |
| Request/Response | `SvCustomerSummaryRequest`, `SvCustomerSummaryResponse` |
| Query/Command/Result | 용도별 별도 타입 |
| SQL ID | `selectCustomerSummary`, `countCustomerList`, `insertCustomer` |

`Manager`, `Processor`, `CommonController`, `ServiceImpl2`처럼 책임이나 업무 경계가 드러나지 않는 이름을 피한다.

## 6. 표준 전문

요청의 기본 모양은 다음과 같다.

```json
{
  "header": {
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "businessCode": "SV",
    "guid": "end-to-end-correlation-id",
    "traceId": "internal-trace-id",
    "channelId": "WEB"
  },
  "body": {
    "customerNo": "..."
  }
}
```

응답은 ETF가 표준 Header, Result, Body 구조로 조립한다. 업무 코드에서 임의 JSON 응답 구조를 만들지 않는다.

중요 원칙:

- Header는 라우팅·통제·추적용이고 Body는 업무 데이터용이다.
- 클라이언트가 보낸 사용자·권한 관련 값을 그대로 신뢰하지 않는다.
- GUID와 TraceId는 모든 하위 호출과 로그에 전파한다.
- Request DTO, Response DTO, DB Result를 분리해 DB 컬럼이나 민감정보가 그대로 외부에 노출되지 않게 한다.
- 실제 필수 Header 필드는 현재 `StandardHeaderValidator`와 환경 정책을 최종 기준으로 확인한다.

## 7. 새 거래 구현 순서

1. 업무코드, ServiceId, 거래코드를 먼저 확정한다.
2. Request/Response DTO와 검증 규칙을 정의한다.
3. Handler의 `serviceIds()`에 ServiceId를 등록한다.
4. Handler에서는 Body를 DTO로 변환하고 Facade만 호출한다.
5. Facade에 트랜잭션 경계와 Timeout을 설정한다.
6. Service에서 업무 처리 흐름을 조립한다.
7. Rule에서 필수값, 형식, 상태 전이, 업무 조건을 검증한다.
8. DAO/Mapper와 Mapper XML에 SQL을 구현한다.
9. 업무 예외에 표준 오류코드를 연결한다.
10. OM에 Service Catalog, 거래통제, Timeout, 권한, 오류코드를 등록한다.
11. 정상·검증 실패·업무 실패·DB 실패·Timeout 테스트를 작성한다.
12. 로컬 `bootRun`과 통합 Tomcat 환경에서 표준 전문으로 Smoke Test한다.

최소 정합성:

```text
요청 header.serviceId
  = Handler.serviceIds()
  = OM_SERVICE_CATALOG.SERVICE_ID

요청 header.transactionCode
  = Service Catalog의 거래코드
  = 거래통제·로그·감사 기준의 거래코드
```

## 8. 데이터 접근과 트랜잭션

### DTO와 Mapper

- Request, Response, Query, Command, Result를 목적에 맞게 분리한다.
- Mapper Interface와 XML namespace를 정확히 맞춘다.
- 복잡한 결과는 명시적 `resultMap`을 사용한다.
- 온라인 목록은 Paging과 최대 `pageSize`를 강제한다.
- 운영 온라인 SQL의 무제한 조회와 의도하지 않은 Full Scan을 금지한다.
- SQL ID, ServiceId, GUID가 함께 추적될 수 있게 로그 문맥을 유지한다.
- 실시간 운영 조회와 분석·대량 조회 DB의 용도를 구분한다.

### 트랜잭션

- 일반적으로 Facade를 트랜잭션 경계로 둔다.
- 조회 거래는 `readOnly = true`를 사용한다.
- 여러 DB나 외부 시스템을 하나의 로컬 트랜잭션처럼 오해하지 않는다.
- 외부 호출을 DB 트랜잭션 안에 오래 포함하지 않는다.
- 재시도는 멱등성이 보장된 조회나 명시적으로 설계된 요청에만 제한한다.
- 등록·변경 거래는 중복 요청 키와 처리 결과 조회 방식을 설계한다.

### Timeout

현재 구현은 정책의 세 계층을 사용한다.

| 계층 | 정책 | 적용 지점 |
|---|---|---|
| Online | `ONLINE_TIMEOUT_SEC` | Dispatcher 실행을 감싸는 Timeout Executor |
| Transaction | `TX_TIMEOUT_SEC` | Spring `@Transactional` AOP |
| DB | `DB_QUERY_TIMEOUT_SEC` | MyBatis Query Timeout |

운영 인프라까지 포함한 기본 방향은 다음과 같다.

```text
DB Query < Transaction < Online < Apache/Client < L4 Idle
```

숫자를 문서 예시에서 복사하지 말고 Service Catalog와 환경별 정책의 현재값을 확인한다. Timeout 발생 후 결과가 불명확할 수 있는 변경 거래는 자동 재시도보다 상태 조회와 멱등성 처리를 우선한다.

## 9. 오류, 로그, 보안

### 오류 처리

오류코드 형식:

```text
E-{DOMAIN}-{CATEGORY}-{NNNN}
```

예:

```text
E-TCF-HDR-0001
E-TCF-SVC-0001
E-SV-BIZ-0001
E-TCF-DB-0001
E-TCF-TIME-0001
E-TCF-SYS-9999
```

권장 구분:

- 입력 검증 오류: 필수값·길이·형식·코드값
- 업무 오류: 조회 결과 없음, 허용되지 않은 상태 전이 등
- 인증/권한 오류: 로그인·토큰·기능·데이터 권한
- 시스템 오류: DB, 연동, Timeout, 미처리 예외

업무 계층은 의미 있는 예외를 발생시키고 ETF가 표준 응답으로 변환한다. StackTrace, SQL, 내부 서버명, 개인정보를 사용자 메시지에 노출하지 않는다. 신규 오류코드는 OM에 등록하고, 폐기 시 재사용하지 않는다.

### 로그와 추적

거래를 찾을 때 최소한 다음 값을 함께 사용한다.

```text
GUID / TraceId
ServiceId
TransactionCode
BusinessCode
User / Branch / ChannelId
SQL ID
Elapsed Time
Result Code
```

비밀번호, 토큰, 주민등록번호, 계좌번호, 전체 요청 Body를 평문 로그로 남기지 않는다. 운영 문제를 `System.out`으로 추적하지 말고 구조화된 애플리케이션·거래·감사 로그를 사용한다.

### 세션과 권한

- 로컬/운영 구성은 Spring Session JDBC와 `SPRING_SESSION`을 사용할 수 있다.
- Apache sticky session은 `JSESSIONID`의 route와 Tomcat `jvmRoute`의 일치를 전제로 한다.
- DeltaManager 세션 복제는 센터 내부로 한정되며 센터 간 장애 전환은 재로그인이 필요할 수 있다.
- 세션 객체는 직렬화 가능해야 하며 대용량 DTO와 민감 원문 저장을 피한다.
- 인증 성공과 업무 데이터 접근 권한은 별도 문제다. 메뉴·기능·데이터 권한을 업무 실행 전에 확인한다.

## 10. 빌드와 실행

### 현재 기준 기술 버전

루트 `build.gradle` 기준:

| 항목 | 현재값 |
|---|---|
| Java toolchain | JDK 21 |
| Spring Boot BOM | 3.3.5 |
| H2 | 2.2.224 |
| Test | JUnit Platform |
| 기본 bootRun profile | `local` |

과거 문서의 “JDK 17 이상”은 일반 가이드이므로 현재 저장소 실행에는 JDK 21을 사용한다.

### 자주 쓰는 명령

Windows에서는 저장소에 Gradle Wrapper가 있으므로 다음처럼 실행하는 것이 안전하다.

```powershell
.\gradlew.bat :sv-service:test
.\gradlew.bat :sv-service:bootRun
.\gradlew.bat :sv-service:bootWar
.\gradlew.bat buildBusinessWars
.\gradlew.bat buildZtomcatWars
```

대표 실행:

| 모드 | 용도 | 예 |
|---|---|---|
| `bootRun` | 단일 모듈 빠른 개발 | SV `http://localhost:8086/sv/online` |
| `ztomcat` | 여러 WAR 통합 검증 | `http://localhost:8080/sv/online` |

루트 집계 태스크의 현재 의미:

- `buildBusinessWars`: 업무 WAR 묶음
- `buildZtomcatWars`: 업무 WAR에 Batch, UI, UJ, JWT, Gateway 등을 더한 통합 Tomcat용 묶음

정확한 WAR 수는 문서에 고정 숫자로 의존하지 말고 `build.gradle`의 태스크 의존성과 실제 산출물을 확인한다.

## 11. 테스트 전략

새 거래의 최소 테스트 세트:

| 구분 | 검증 내용 |
|---|---|
| 단위 | Rule의 필수값·형식·상태 조건, Service 분기 |
| Handler | ServiceId 등록, Body 변환, Facade 위임 |
| Mapper | namespace, SQL ID, 파라미터, 결과 매핑, Paging |
| 통합 | Spring Bean 등록, 트랜잭션, DB 연결, 표준 예외 변환 |
| 거래 | 실제 `POST /{업무}/online`의 정상·실패 응답 |
| 통제 | 미등록/차단 ServiceId, 권한 없음 |
| 안정성 | DB·외부 연동 지연, Timeout, 중복 요청 |
| 보안 | 민감정보 마스킹, 권한 우회, 입력 변조 |
| 배포 | WAR Context, Health, 주요 Smoke Test |

공통 모듈 변경 시 해당 모듈 테스트만 통과했다고 끝내지 않는다.

- `tcf-util`, `tcf-core`, `tcf-web`: 전체 또는 주요 업무 거래 회귀 테스트
- `tcf-gateway`, `tcf-jwt`: 인증·라우팅·주요 업무 Smoke Test
- Mapper 변경: 해당 업무 Mapper 및 거래 통합 테스트
- 환경설정 변경: local/dev/prod 차이와 기동 검증

## 12. 장애 분석 순서

1. 환경(local/dev/prod)과 실행 방식(bootRun/Tomcat)을 확정한다.
2. 실패 모듈, URL, ServiceId, 거래코드를 확인한다.
3. GUID/TraceId로 Gateway, TCF, 업무, SQL 로그를 연결한다.
4. 실패 지점을 `STF → Dispatcher → Handler → 업무 계층 → ETF` 중 하나로 좁힌다.
5. 최근 WAR, 설정, DB 스키마, OM 기준정보, Cache 변경을 우선 확인한다.
6. `ServiceId 없음`이면 요청 Header, Handler Bean, `serviceIds()`, Package Scan, OM Catalog 순서로 본다.
7. SQL 오류이면 Mapper namespace/SQL ID, 파라미터, DB 연결, Query Timeout을 확인한다.
8. Timeout이면 전체 지연시간과 함께 DB Pool, 느린 SQL, 외부 호출, 각 계층의 제한시간을 비교한다.
9. 기준정보 수정 후 Cache reload/evict 여부를 확인한다.
10. 운영에서는 원인 분석보다 안전한 복구와 Rollback 판단을 우선하고 서버 파일을 직접 교체하지 않는다.

## 13. 개발 완료 체크리스트

### 설계·등록

- [ ] 업무코드, Context, WAR, Package가 일치한다.
- [ ] ServiceId와 거래코드가 명명 규칙을 따른다.
- [ ] Handler `serviceIds()`와 OM Service Catalog가 일치한다.
- [ ] 거래통제, Timeout, 권한, 오류코드 기준정보가 준비됐다.

### 구현

- [ ] Handler가 얇고 계층 책임이 분리됐다.
- [ ] Request/Response/DB Result가 분리됐다.
- [ ] 입력값과 업무 규칙을 Rule 등 적절한 계층에서 검증한다.
- [ ] Facade의 트랜잭션 속성과 Timeout이 거래 성격에 맞다.
- [ ] DAO에는 업무 판단이 없고 Mapper SQL은 제한 조회를 사용한다.
- [ ] 타 업무 호출은 직접 Java 의존이 아닌 표준 연동을 사용한다.
- [ ] 변경 거래의 멱등성과 Timeout 후 결과 확인 방법이 있다.

### 품질·운영

- [ ] 정상, 검증 실패, 업무 실패, DB 실패, Timeout을 테스트했다.
- [ ] 로그에 GUID, ServiceId, 거래코드, SQL ID가 연결된다.
- [ ] 개인정보, 인증정보, 내부 예외 상세가 노출되지 않는다.
- [ ] 모듈 테스트와 표준 전문 Smoke Test가 통과한다.
- [ ] WAR명과 Context를 통합 Tomcat에서 확인했다.
- [ ] 배포, Health Check, Rollback 절차와 담당자가 정해졌다.

## 14. 문서 간 차이가 있을 때

자료에는 작성 시점과 목적이 다른 집필본, 입문서, 구축 문서, 환경설정 예시가 함께 있어 다음 항목이 서로 다를 수 있다.

- JDK 17 권장 표기와 현재 JDK 21 toolchain
- 9개 업무 WAR와 확장 업무/관리 WAR를 포함한 다른 개수
- 샘플 ServiceId·거래코드 일련번호
- `com.nh.nsight.{업무}`와 현재 소스의 `com.nh.nsight.marketing.{업무}`
- `bootRun`, 단일 Tomcat, Apache/Tomcat Cluster 등 실행 토폴로지
- 예시 Timeout과 환경별 실제 정책값

판단 순서:

1. 현재 브랜치의 `settings.gradle`, 루트/모듈 `build.gradle`
2. 현재 Java 소스, Mapper XML, `application-*.yml`
3. 루트 README와 모듈 README
4. OM의 현재 기준정보와 환경별 배포 설정
5. 설계·가이드 Markdown의 원칙
6. 샘플·집필본의 예시 값

예시 숫자나 계정·주소를 운영값으로 간주하지 말고 배포 대상 환경의 승인된 설정을 확인한다.

## 15. 이번 요약에 사용한 비 Word 자료

핵심 근거는 다음 범주에서 교차 확인했다.

- `README.md`, `settings.gradle`, `build.gradle`
- `ztcf-집필본-md`: 개발 입문과 역추적·영향도 분석
- `zarchitecture`: 전체, TCF, 6계층, 보안, 연동, 배포 아키텍처
- `zdocs-1`, `zdocs-2`: TCF 구현 구조와 개별 개발 표준
- `zguide`, `zman`, `znsight-man`: 업무별 가이드와 개발 매뉴얼
- `ztcfbook`, `ztcfbook-h`, `ztcfbook-m`: 명명, 전문, Mapper, 오류코드 부록
- `ztcf-book-capacity-md`: 용량과 설정 연결 원칙
- `znsight-config-info`, `znsight-config-value-word`, `ztcf-engine-config-info`: 비 Word 환경·배포 설정
- `ztcf-다이어리`: 최근 아키텍처·실행 메모
- 현재 `tcf-core` 및 `sv-service` Java 소스의 Dispatcher, Handler, Facade 구현

Word 파일만 존재하거나 비 Word 자료가 없는 폴더는 내용 요약에서 제외했다.
