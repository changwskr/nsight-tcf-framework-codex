# NSIGHT TCF 명명규칙 표준화 설계

## 1. 목적

NSIGHT TCF Framework의 신규 코드와 프롬프트 기반 CRUD 생성에 적용할 명명규칙을 확정한다. 이름만 보고 업무 경계, Domain, 계층 책임, 실행 경로, SQL과 운영 식별자를 추적할 수 있어야 한다.

기존 공개 계약과 DB 객체는 일괄 변경하지 않는다. 신규 코드는 새 표준을 적용하고, 기존 코드는 기능 변경 범위에서 점진적으로 정리한다.

## 2. 최상위 원칙

1. 모든 이름은 업무코드에서 출발한다.
2. 업무코드, 모듈, Context, WAR, Package, ServiceId, Java 클래스, DB와 운영 식별자가 하나의 체계로 연결되어야 한다.
3. URL은 진입 경로이고 실제 거래 실행 식별자는 `serviceId`다.
4. Java 클래스 이름에 Handler, Facade, Service, Rule, Dao, Mapper 등 계층 책임을 명시한다.
5. 공통 모듈은 `tcf-*`, 업무 모듈은 `{bc}-service`로 구분한다.
6. Java, JSON, DB, URL과 파일의 표기 방식을 일관되게 사용한다.
7. ServiceId에서 Handler, SQL, 거래코드와 오류코드까지 검색·추적할 수 있어야 한다.
8. 표준 기술 약어 외에 의미가 불분명한 축약을 사용하지 않는다.
9. 같은 의미에 여러 이름을 사용하지 않는다.
10. 신규 업무와 거래에도 같은 규칙으로 확장할 수 있어야 한다.

## 3. 적용과 호환성

| 대상 | 정책 |
| --- | --- |
| 신규 코드 | 새 명명규칙 필수 적용 |
| 기존 코드 기능 변경 | 변경 범위 안에서 점진 적용 |
| 기존 공개 ServiceId | 호환성 검토 없이 변경 금지 |
| DB 객체·컬럼 | Migration과 Rollback 없이 이름 변경 금지 |
| 내부 클래스·메서드 | 호출부와 테스트를 함께 수정할 수 있을 때 정리 |
| 레거시 예외 | 예외 원장에 현재 이름, 사유와 대체 표준 기록 |

기존 이름을 새 표준 이름으로 자동 해석하거나 자동 변경하지 않는다. 공개 계약, Catalog, UI, 샘플 요청과 외부 호출 사용처를 조사한 뒤 변경한다.

## 4. 표기 규칙

| 대상 | 규칙 | 예 |
| --- | --- | --- |
| Java 클래스 | PascalCase | `SvCustomerService` |
| Java 메서드·필드 | lowerCamelCase | `selectCustomerList`, `customerId` |
| Java 상수 | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |
| JSON 필드 | lowerCamelCase | `serviceId`, `businessCode` |
| DB 테이블·컬럼 | UPPER_SNAKE_CASE | `SV_CUSTOMER`, `CUSTOMER_ID` |
| HTML·JS·CSS | kebab-case | `customer-contact.html` |
| 패키지 | 소문자 | `com.nh.nsight.marketing.sv` |
| 업무코드 | 대문자 | `SV`, `OM` |
| Java 업무 Prefix | 업무코드의 PascalCase 표현 | `SV` → `Sv` |

## 5. 업무코드 파생 규칙

업무코드 `SV`의 파생 이름은 다음과 같다.

```text
업무코드       SV
Gradle 모듈    sv-service
Context Path   /sv
WAR            sv.war
Root Package   com.nh.nsight.marketing.sv
Java Prefix    Sv
DB Prefix      SV_
ServiceId      SV.
오류코드       E-SV-
```

플랫폼·공통 모듈은 `tcf-core`, `tcf-web`, `tcf-eai`, `tcf-cache`, `tcf-gateway`, `tcf-jwt`처럼 `tcf-*`를 사용한다.

## 6. ServiceId

### 6.1 형식

```text
{BC}.{Domain}.{action}
```

- `BC`: 대문자 업무코드
- `Domain`: PascalCase 단수 업무 개념
- `action`: lowerCamelCase 동사

### 6.2 표준 CRUD action

| 용도 | action | 예 |
| --- | --- | --- |
| 목록 조회 | `selectList` | `SV.Customer.selectList` |
| 상세 조회 | `selectDetail` | `SV.Customer.selectDetail` |
| 등록 | `create` | `SV.Customer.create` |
| 수정 | `update` | `SV.Customer.update` |
| 삭제 | `delete` | `SV.Customer.delete` |

CRUD로 표현할 수 없는 업무 행위는 명확한 Domain 동사를 사용한다.

```text
MG.Message.send
OM.Deploy.approve
OM.Batch.execute
```

기존 `inquiry`, `detail`, `save`, `register`, `list`는 레거시 호환 action으로 유지하되 신규 CRUD에는 사용하지 않는다.

### 6.3 정합성

- `header.businessCode`는 ServiceId의 BC와 같아야 한다.
- ServiceId Domain은 Handler가 소유하는 Domain과 같아야 한다.
- Handler·Facade·Service 메서드는 ServiceId action 어휘를 유지한다.
- ServiceId 변경 시 Handler, Catalog, UI, 샘플 요청과 도움말 사용처를 함께 확인한다.

## 7. Domain과 Handler 경계

Domain은 화면, 조회 결과 또는 개별 action이 아니라 거래를 소유하는 안정적인 업무 개념이다.

```text
{Bc}{Domain}Handler
```

좋은 예:

```text
SvCustomerHandler
AvCustomerContactHandler
OmServiceCatalogHandler
MgMessageHandler
```

금지 예:

```text
SvCustomerSummaryHandler
SvCustomerSelectListHandler
UpdateCustomerHandler
```

단, `CustomerContact`, `ServiceCatalog`, `TransactionLog`처럼 독립된 데이터 소유권, 수명주기, 권한 또는 업무 규칙을 가진 복합 개념은 하나의 Domain으로 허용한다.

Domain 판정 기준:

- 같은 PK와 수명주기를 공유하면 하나의 Domain이다.
- 독립된 권한, 테이블 소유권 또는 업무 규칙이 있으면 별도 Domain이다.
- 화면 탭, 검색 결과와 요약 Projection은 별도 Domain이 아니다.
- Domain은 단수 명사를 사용한다.
- `Management`, `Process`, `Common`, `Operation`처럼 범위가 불분명한 이름을 신규 Domain에 사용하지 않는다.

도메인당 Handler 하나를 사용하며 동일 Domain의 여러 ServiceId는 `serviceIds()`와 Handler 분기로 처리한다.

## 8. 계층 클래스

```text
{Bc}{Domain}Handler
{Bc}{Domain}Facade
{Bc}{Domain}Service
{Bc}{Domain}Rule
{Bc}{Domain}Dao
{Bc}{Domain}Mapper
```

예:

```text
SvCustomerHandler
SvCustomerFacade
SvCustomerService
SvCustomerRule
SvCustomerDao
SvCustomerMapper
```

역할이 불명확한 `Manager`, `Processor`, `Common`, `Util`, `Impl` 접미사를 신규 업무 클래스에 사용하지 않는다. 실제 대체 구현이 여러 개일 때만 기술 또는 전략 이름으로 구현체를 구분한다.

## 9. DTO

### 9.1 내부 업무 DTO

업무코드는 모듈과 패키지에서 이미 표현되므로 내부 DTO 클래스에는 BC Prefix를 반복하지 않는다.

```text
{Domain}{Purpose}{Role}
```

예:

```text
CustomerSelectListRequest
CustomerSelectListResponse
CustomerSelectDetailRequest
CustomerSelectDetailResponse
CustomerCreateRequest
CustomerUpdateRequest
CustomerSearchCriteria
CustomerRow
```

### 9.2 외부·타 업무 연동 DTO

호출 대상 계약을 명확하게 표현하기 위해 대상 업무 또는 시스템 Prefix를 허용한다.

```text
SvCustomerSummaryResult
EaiMessageSendRequest
```

### 9.3 역할 접미사

| 역할 | 접미사 |
| --- | --- |
| 거래 입력 | `Request` |
| 거래 출력 | `Response` |
| 조회 조건 | `Criteria` |
| DB 조회 결과 | `Row` |
| 외부 연동 결과 | `Result` |
| 내부 값 객체 | `Value` 또는 구체적인 Domain 명사 |

책임이 불명확한 `Dto`, `Data`, `Info`, `Model`, `Param`을 신규 DTO 접미사로 사용하지 않는다.

## 10. 계층별 메서드

| 계층 | 규칙 | 예 |
| --- | --- | --- |
| Handler | 프레임워크 계약 | `serviceIds`, `doHandle` |
| Facade | ServiceId action과 동일 | `selectList`, `create`, `approve` |
| Service | ServiceId action 또는 명확한 유스케이스 동사 | `selectDetail`, `create`, `send` |
| Rule | 검증·필수조건·정규화 의도 | `validateCreate`, `requireCustomer`, `normalizeName` |
| DAO | 영속 동작 + Domain | `selectCustomerList`, `insertCustomer` |
| Mapper | SQL 동작 + Domain | `selectCustomerList`, `insertCustomer` |

추적 예:

```text
SV.Customer.create
→ SvCustomerHandler
→ SvCustomerFacade.create(...)
→ SvCustomerService.create(...)
→ SvCustomerRule.validateCreate(...)
→ SvCustomerDao.insertCustomer(...)
→ SvCustomerMapper.insertCustomer(...)
→ SQL id="insertCustomer"
```

추가 규칙:

- Boolean 반환 메서드는 `is*`, `has*`, `can*`을 허용한다.
- Boolean 필드는 `enabled`, `active`, `deleted`처럼 상태를 표현한다.
- Collection 필드는 `customers`, `errorCodes`처럼 복수형을 사용한다.
- `process`, `handleData`, `doWork`, `executeLogic`처럼 대상과 결과가 불명확한 동사를 사용하지 않는다.

## 11. DAO와 Mapper

신규 코드는 Domain별 DAO·Mapper 한 쌍을 사용한다.

```text
{Bc}{Domain}Dao
{Bc}{Domain}Mapper
mapper/{bc}/{Bc}{Domain}Mapper.xml
```

예:

```text
SvCustomerDao
SvCustomerMapper
mapper/sv/SvCustomerMapper.xml
```

규칙:

- Mapper 인터페이스명과 XML 파일명을 동일하게 유지한다.
- Mapper 메서드명과 XML SQL ID를 동일하게 유지한다.
- `select*`, `search*`, `count*`, `insert*`, `update*`, `merge*`, `delete*`, `disable*`를 사용한다.
- `updErrCd`, `sel01` 같은 축약 SQL ID를 사용하지 않는다.
- `OperationMapper`, `CommonMapper`, `ManagementDao`, `MasterMapper`처럼 여러 Domain을 불명확하게 묶지 않는다.

여러 Domain을 조합하는 읽기 전용 Projection 또는 프레임워크 기술 경계에는 의미가 명확한 예외 이름을 허용한다.

```text
OmDashboardQueryMapper
TcfTransactionLogRepository
```

기존 `OmOperationMapper` 같은 대형 Mapper는 즉시 변경하지 않고 기능 변경 시 Domain Mapper로 점진 분리한다.

## 12. DB 객체와 필드

업무 DB 객체는 다음 형식을 사용한다.

```text
{BC}_{DOMAIN}_{PURPOSE}
```

예:

```text
SV_CUSTOMER
SV_CUSTOMER_CONTACT
SV_CUSTOMER_HISTORY
OM_SERVICE_CATALOG
TCF_TRANSACTION_LOG
```

용도 접미사는 실제 성격이 있을 때만 사용한다.

| 접미사 | 의미 |
| --- | --- |
| `_MASTER` | 기준정보 또는 마스터 |
| `_DETAIL` | 부모에 종속된 상세 |
| `_HISTORY` | 변경·처리 이력 |
| `_LOG` | 실행 로그 |
| `_MAP` | 다대다 관계 |
| `_POLICY` | 정책 |
| `_CONTROL` | 통제 기준 |

DB 컬럼과 Java 필드는 다음처럼 대응한다.

```text
CUSTOMER_ID   ↔ customerId
CUSTOMER_NAME ↔ customerName
CREATED_AT    ↔ createdAt
UPDATED_AT    ↔ updatedAt
ACTIVE_YN     ↔ active
```

공통 감사 컬럼은 `CREATED_AT`, `CREATED_BY`, `UPDATED_AT`, `UPDATED_BY`를 사용한다. 레거시 컬럼을 Java 표준명으로 표현할 때는 Mapper 매핑을 명시한다.

## 13. 운영 식별자

### 13.1 transactionCode

```text
{BC}-{TYPE}-{NNNN}
```

표준 TYPE:

| TYPE | 의미 |
| --- | --- |
| `INQ` | 조회 |
| `REG` | 등록 |
| `UPD` | 수정 |
| `DEL` | 삭제 |
| `EXE` | 실행 |
| `ADM` | 관리 |

### 13.2 errorCode

```text
E-{AREA}-{CATEGORY}-{NNNN}
```

표준 Category:

| Category | 의미 |
| --- | --- |
| `VALID` | 입력·형식 검증 |
| `BIZ` | 업무 규칙 |
| `AUTH` | 인증·권한 |
| `DB` | 데이터 접근 |
| `EXT` | 외부 연동 |
| `SYS` | 시스템 |

### 13.3 화면과 리소스

```text
화면번호      {BC}{NNNN}
HTML/JS/CSS   kebab-case
샘플 요청     {bc}-{domain-kebab}-{action}.json
```

예:

```text
SV0101
customer-contact-list.html
customer-contact.js
sv-customer-contact-select-list.json
```

ServiceId, transactionCode, 화면번호와 errorCode는 목적을 혼합하지 않는다.

## 14. 패키지와 설정

업무 Root Package:

```text
com.nh.nsight.marketing.{bc}
```

표준 하위 패키지:

```text
entry.handler
entry.facade
entry.web
application.service
application.rule
application.dto.{domain}
client
config
persistence.dao
persistence.mapper
support
```

패키지에는 소문자만 사용한다. `common`, `misc`, `etc`, `temp` 패키지를 신규 생성하지 않는다. `support`에 업무 흐름이나 DB 접근을 넣지 않는다.

Spring 설정 키는 kebab-case, 환경변수는 UPPER_SNAKE_CASE를 사용한다.

```yaml
nsight:
  tcf:
    transaction-control-enabled: true
```

```text
NSIGHT_TCF_TRANSACTION_CONTROL_ENABLED
```

## 15. 공식 기준 우선순위

```text
AGENTS.md
→ zdocs-1/architecture/53-naming-conventions.md
→ 영역별 상세 명명 문서
→ 대상 모듈 README
→ 실제 공개 계약과 호환성 제약
→ 유사 코드 패턴
```

`53-naming-conventions.md`를 명명규칙 SoT로 사용하고 `06-naming.md`는 Java·Mapper 구현 지침으로 유지한다. 구형 문서와 충돌하면 최신 SoT를 적용하되 공개 계약을 자동 변경하지 않는다.

## 16. 검증 기준

신규 CRUD 생성과 기능 변경 시 다음을 검사한다.

1. 업무코드, 모듈, Context, WAR와 Package 일치
2. ServiceId의 `{BC}.{Domain}.{action}` 형식
3. 신규 CRUD action의 표준 어휘 사용
4. 도메인당 Handler 하나 원칙
5. 계층 클래스의 BC·Domain 정합성
6. DTO의 Request·Response·Criteria·Row 책임 분리
7. DAO·Mapper·XML의 Domain 정합성
8. Mapper method와 SQL ID 일치
9. DB Prefix와 Java 필드 매핑
10. transactionCode, errorCode와 화면번호 형식
11. Catalog, UI, 샘플 요청과 도움말 사용처 정합성
12. 레거시 예외의 원장 기록

## 17. 금지 이름

다음처럼 업무, Domain, 역할 또는 데이터 책임을 알 수 없는 이름을 신규 코드에 사용하지 않는다.

```text
CommonService
CustomerManager
OperationProcessor
Util2
upd01
selData
Dto
Info
Param
Temp
Misc
```

## 18. 수용 기준

1. ServiceId CRUD action이 단일 어휘로 정의된다.
2. Domain과 Handler 경계를 저장소 조사 없이도 같은 기준으로 판정할 수 있다.
3. 업무 내부 DTO와 외부 연동 DTO의 Prefix 규칙이 구분된다.
4. 계층별 클래스와 메서드 이름으로 책임을 식별할 수 있다.
5. Domain별 DAO·Mapper 구조와 허용 예외가 명확하다.
6. DB, 거래코드, 오류코드, 화면과 리소스 규칙이 구분된다.
7. 신규 적용과 레거시 호환 정책이 함께 정의된다.
8. 자동 생성 프롬프트가 검증 가능한 체크리스트를 제공받는다.
