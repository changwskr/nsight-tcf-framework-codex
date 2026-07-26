
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제12부. 초보 개발자 실전 프로젝트와 종합 예제

## 1. 도입 전 안내말

앞의 제1부부터 제11부까지는 NSIGHT TCF 개발에 필요한 개별 주제를 단계별로 배웠습니다.

```
TCF 거래 흐름

개발표준과 ServiceId

계층별 구현

CRUD

오류·Timeout·중복방지

JWT·Gateway 보안

업무 간 연계

Cache·파일·Batch

테스트·배포

OM 운영관리

명명규칙과 추적성
```

이제 실제 프로젝트에서 초보 개발자가 가장 많이 마주치는 상황을 생각해 보겠습니다.

업무팀이 다음 자료만 전달합니다.

```
화면 이미지

화면 항목 목록

버튼과 이벤트

사용할 테이블

테이블 컬럼

간단한 업무 설명
```

그리고 다음과 같이 요청합니다.

```
“이 화면을 개발해 주세요.”

“조회·등록·수정·삭제가 되면 됩니다.”

“기존 TCF 구조에 맞춰 만들어 주세요.”
```

초보 개발자는 다음 질문부터 생깁니다.

```
어느 업무코드를 사용해야 하는가?

화면 ID는 어떻게 정하는가?

ServiceId는 몇 개가 필요한가?

거래코드는 어떻게 나누는가?

Controller를 새로 만들어야 하는가?

Handler와 Facade는 무엇을 작성해야 하는가?

DTO는 몇 종류가 필요한가?

조회와 저장 SQL은 어떻게 나누는가?

삭제는 실제 DELETE를 해야 하는가?

사용자 ID는 화면에서 받아도 되는가?

Timeout과 권한은 어디에 등록하는가?

테스트는 어디까지 해야 하는가?

OM에는 무엇을 등록해야 하는가?
```

제12부에서는 이 질문에 하나씩 답하면서 실제 개발절차를 완성합니다.

이번 실전 프로젝트의 주제는 다음과 같습니다.

## 고객 상담이력 관리

업무코드:

```
CT
= Contact
```

도메인:

```
Contact
= 고객 상담·접촉이력
```

주요 기능:

```
고객 상담이력 목록조회

고객 상담이력 상세조회

고객 상담이력 등록

고객 상담이력 변경

고객 상담이력 사용중지
```

이번 부의 핵심 원칙은 다음과 같습니다.

```
코드부터 작성하지 않는다.

화면 이벤트와 업무기능을 먼저 분리한다.

하나의 ServiceId에는 하나의 책임을 부여한다.

사용자 정보는 화면이 아닌 인증 Context를 사용한다.

DB 변경은 데이터 소유 업무에서 수행한다.

정상 흐름과 오류 흐름을 함께 구현한다.

개발 완료는 OM 등록과 운영 인수까지 포함한다.
```

## 2. 제12부 개요

### 2.1 목적

제12부의 목적은 화면과 테이블 정보만 주어진 상태에서 NSIGHT TCF 표준에 맞는 업무 기능을 처음부터 끝까지 설계하고 구현하는 전체 절차를 익히는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

- 화면과 테이블 정보를 분석하여 업무 범위를 정의한다.
- 화면 이벤트를 독립적인 ServiceId로 분리한다.
- 화면 ID·이벤트 ID·거래코드를 발급한다.
- 요구사항과 추적성 매트릭스를 작성한다.
- 패키지와 프로그램 골격을 구성한다.
- Request·Response·Command·Query·Data DTO를 구분한다.
- Validation과 업무 Rule을 구현한다.
- Handler·Facade·Service 책임을 분리한다.
- DAO·Mapper·SQL을 구현한다.
- 트랜잭션·Rollback·동시성 제어를 적용한다.
- 인증·권한·데이터권한을 적용한다.
- 오류·Timeout·중복요청을 처리한다.
- 단위·Mapper·거래 테스트를 작성한다.
- OM 기준정보와 운영항목을 등록한다.
- 빌드·배포·Smoke Test·운영인수를 수행한다.
- AI 개발도구를 사용할 때 입력해야 할 설계정보를 정리한다.

### 2.2 적용범위

| 영역 | 주요 내용 |
| --- | --- |
| 요구사항 | 화면·테이블·업무규칙 분석 |
| 식별체계 | 화면 ID·이벤트 ID·ServiceId |
| 산출물 | 화면·거래·프로그램·SQL 설계 |
| 구현 | Handler부터 Mapper까지 |
| 데이터 | Master·History·상태관리 |
| 보안 | 인증·기능권한·데이터권한 |
| 안정성 | 오류·Timeout·Idempotency |
| 테스트 | 단위·통합·거래 |
| 운영 | OM·모니터링·장애대응 |
| 배포 | Build·WAR·CI/CD |
| 품질 | Architecture Gate·체크리스트 |

### 2.3 대상 독자

- 처음으로 TCF 업무기능을 개발하는 개발자
- 화면과 DB 정보만 전달받은 개발자
- 기존 프로젝트 구조를 분석하기 어려운 개발자
- CRUD는 가능하지만 설계서 작성이 어려운 개발자
- AI 코딩도구로 소스를 만들려는 개발자
- 요구사항부터 운영까지 전체 흐름을 이해하려는 개발자
- 개발 산출물과 실제 소스를 연결해야 하는 개발자

### 2.4 선행조건

다음 내용을 알고 있어야 합니다.

```
공통 온라인 Endpoint는 업무코드 경로를 사용한다.

기능은 Header의 ServiceId로 선택한다.

Handler는 Facade를 호출한다.

Facade는 트랜잭션 경계가 된다.

Service는 업무 흐름을 담당한다.

Rule은 검증과 계산을 담당한다.

DAO와 Mapper는 데이터 접근을 담당한다.

STF는 인증·권한·통제·Timeout을 처리한다.

ETF는 표준 응답과 종료처리를 수행한다.
```

### 2.5 실전 프로젝트 요약

| 항목 | 내용 |
| --- | --- |
| 업무 | 고객 상담이력 |
| 업무코드 | CT |
| 도메인 | Contact |
| WAR | ct-service |
| Context Path | /ct |
| 화면 ID | CT-CNT-0001 |
| 화면명 | 고객 상담이력 관리 |
| 기본 Endpoint | POST /ct/online |
| 대상 고객 | 고객번호 기준 |
| 주요 테이블 | CT_CONTACT_MASTER |
| 이력 테이블 | CT_CONTACT_HISTORY |
| 목록 최대건수 | 페이지당 최대 100건 |
| 기본 Timeout | 조회 3초, 변경 5초 |
| 권한 | 조회·등록·변경 분리 |
| 삭제방식 | 물리 삭제가 아닌 사용중지 |
| 동시성 | Version 기반 낙관적 잠금 |

## 제97장. 화면과 테이블 정보 분석하기

### 97.1 업무팀에서 받은 화면 정보

화면명:

```
고객 상담이력 관리
```

화면 항목:

| 영역 | 항목 | 설명 |
| --- | --- | --- |
| 검색 | 고객번호 | 필수 |
| 검색 | 상담구분 | 선택 |
| 검색 | 상담일자 From | 선택 |
| 검색 | 상담일자 To | 선택 |
| 목록 | 상담일시 | 목록 표시 |
| 목록 | 상담구분 | 코드명 표시 |
| 목록 | 상담제목 | 목록 표시 |
| 목록 | 상담직원 | 사용자명 표시 |
| 상세 | 상담내용 | 상세 입력 |
| 상세 | 처리결과 | 상세 입력 |
| 상세 | 공개여부 | 지점 공유 여부 |
| 상세 | 상태 | 정상·사용중지 |
| 제어 | 조회 | 목록조회 |
| 제어 | 신규 | 입력영역 초기화 |
| 제어 | 저장 | 등록 또는 변경 |
| 제어 | 사용중지 | 기존 상담 비활성화 |

### 97.2 업무팀에서 받은 테이블 정보

#### CT_CONTACT_MASTER

| 컬럼 | 유형 | 설명 |
| --- | --- | --- |
| CONTACT_ID | VARCHAR2 | 상담이력 ID |
| CUSTOMER_NO | VARCHAR2 | 고객번호 |
| CONTACT_TYPE_CD | VARCHAR2 | 상담구분 |
| CONTACT_TITLE | VARCHAR2 | 상담제목 |
| CONTACT_CONTENT | CLOB | 상담내용 |
| PROCESS_RESULT | VARCHAR2 | 처리결과 |
| OPEN_YN | CHAR | 공개 여부 |
| STATUS_CD | VARCHAR2 | 상태 |
| CONTACT_DTM | TIMESTAMP | 상담일시 |
| BRANCH_ID | VARCHAR2 | 등록지점 |
| VERSION_NO | NUMBER | Version |
| CREATE_USER_ID | VARCHAR2 | 등록자 |
| CREATE_DTM | TIMESTAMP | 등록일시 |
| UPDATE_USER_ID | VARCHAR2 | 수정자 |
| UPDATE_DTM | TIMESTAMP | 수정일시 |

#### CT_CONTACT_HISTORY

| 컬럼 | 설명 |
| --- | --- |
| HISTORY_ID | 이력 식별자 |
| CONTACT_ID | 상담이력 ID |
| CHANGE_TYPE_CD | CREATE·UPDATE·DEACTIVATE |
| BEFORE_DATA | 변경 전 데이터 |
| AFTER_DATA | 변경 후 데이터 |
| CHANGE_USER_ID | 변경 사용자 |
| CHANGE_DTM | 변경일시 |
| TRACE_ID | 거래 추적 ID |

### 97.3 처음 확인할 질문

코드를 작성하기 전에 업무팀과 다음 내용을 확인해야 합니다.

```
고객번호는 어떤 형식인가?

사용자가 다른 지점 고객을 조회할 수 있는가?

상담내용 최대길이는 얼마인가?

공개여부가 N이면 누가 조회할 수 있는가?

등록 후 상담일시를 수정할 수 있는가?

사용중지 후 다시 활성화할 수 있는가?

같은 상담을 중복 등록해도 되는가?

목록 조회기간은 최대 몇 개월인가?

상담내용 전체가 목록에 필요한가?

등록·변경 시 이력을 모두 남겨야 하는가?
```

### 97.4 확정한 업무규칙

이번 실전 사례에서는 다음과 같이 확정합니다.

```
고객번호는 필수다.

목록 조회기간은 최대 3개월이다.

기본 조회기간은 최근 1개월이다.

목록에는 상담내용 전문을 포함하지 않는다.

상세조회에서 상담내용을 조회한다.

상담제목은 최대 100자다.

상담내용은 최대 4,000자다.

처리결과는 최대 1,000자다.

공개여부 N은 등록지점 사용자만 조회한다.

공개여부 Y는 조회권한이 있는 모든 지점이 조회할 수 있다.

사용중지는 논리 삭제다.

변경과 사용중지는 Version을 확인한다.

모든 등록·변경·사용중지는 이력 테이블에 기록한다.
```

### 97.5 화면 기능 분해

화면 버튼 하나가 반드시 ServiceId 하나를 의미하지는 않습니다.

이번 화면의 업무기능을 다음과 같이 분리합니다.

```
목록조회

상세조회

등록

변경

사용중지
```

신규 버튼은 화면 입력영역을 초기화할 뿐 서버 호출이 필요하지 않습니다.

### 97.6 데이터 소유권

```
CT 업무
= 상담이력 Master와 History의 소유 업무
```

고객 기본정보는 IC 업무가 소유한다고 가정합니다.

따라서 고객명 확인이 필요하면 다음과 같이 연계합니다.

```
CT
→ IC.Customer.selectBasic
```

CT가 IC_CUSTOMER_MASTER를 직접 조회하지 않습니다.

### 제97장 요약

```
화면에 버튼이 있다고 바로 코드를 작성하지 않는다.

화면 항목·테이블·업무규칙·권한·상태를 먼저 확인한다.

화면 기능을 독립 업무기능으로 분리하고
데이터 소유 업무를 확정한다.
```

## 제98장. 요구사항과 추적성 식별자 정의하기

### 98.1 요구사항 ID

| 요구사항 ID | 내용 |
| --- | --- |
| REQ-CT-0001 | 고객 상담이력 목록조회 |
| REQ-CT-0002 | 고객 상담이력 상세조회 |
| REQ-CT-0003 | 고객 상담이력 등록 |
| REQ-CT-0004 | 고객 상담이력 변경 |
| REQ-CT-0005 | 고객 상담이력 사용중지 |
| REQ-CT-0006 | 상담이력 변경이력 저장 |
| REQ-CT-0007 | 지점별 데이터권한 적용 |

### 98.2 화면 ID

```
CT-CNT-0001
```

구성:

| 구분 | 값 |
| --- | --- |
| 업무코드 | CT |
| 업무세구분 | CNT |
| 일련번호 | 0001 |

### 98.3 이벤트 ID

| 이벤트 ID | 이벤트 |
| --- | --- |
| CT-CNT-0001-E01 | 상담이력 목록조회 |
| CT-CNT-0001-E02 | 상담이력 상세조회 |
| CT-CNT-0001-E03 | 상담이력 등록 |
| CT-CNT-0001-E04 | 상담이력 변경 |
| CT-CNT-0001-E05 | 상담이력 사용중지 |

### 98.4 ServiceId

| 기능 | ServiceId |
| --- | --- |
| 목록조회 | CT.Contact.selectList |
| 상세조회 | CT.Contact.selectDetail |
| 등록 | CT.Contact.create |
| 변경 | CT.Contact.update |
| 사용중지 | CT.Contact.deactivate |

금지:

```
CT.Contact.manage
```

Body의 actionType으로 조회·등록·변경을 모두 처리하지 않습니다.

### 98.5 거래코드

| ServiceId | 거래코드 |
| --- | --- |
| CT.Contact.selectList | CT-INQ-0001 |
| CT.Contact.selectDetail | CT-INQ-0002 |
| CT.Contact.create | CT-REG-0001 |
| CT.Contact.update | CT-UPD-0001 |
| CT.Contact.deactivate | CT-UPD-0002 |

### 98.6 권한코드

| 기능 | 권한코드 |
| --- | --- |
| 목록·상세조회 | CT_CONTACT_VIEW |
| 등록 | CT_CONTACT_CREATE |
| 변경 | CT_CONTACT_UPDATE |
| 사용중지 | CT_CONTACT_DEACTIVATE |

### 98.7 Timeout

| ServiceId | Timeout |
| --- | --- |
| 목록조회 | 3,000ms |
| 상세조회 | 3,000ms |
| 등록 | 5,000ms |
| 변경 | 5,000ms |
| 사용중지 | 5,000ms |

### 98.8 추적성 매트릭스

| 요구사항 | 화면 이벤트 | ServiceId | 거래코드 | 권한 |
| --- | --- | --- | --- | --- |
| REQ-CT-0001 | E01 | CT.Contact.selectList | CT-INQ-0001 | CT_CONTACT_VIEW |
| REQ-CT-0002 | E02 | CT.Contact.selectDetail | CT-INQ-0002 | CT_CONTACT_VIEW |
| REQ-CT-0003 | E03 | CT.Contact.create | CT-REG-0001 | CT_CONTACT_CREATE |
| REQ-CT-0004 | E04 | CT.Contact.update | CT-UPD-0001 | CT_CONTACT_UPDATE |
| REQ-CT-0005 | E05 | CT.Contact.deactivate | CT-UPD-0002 | CT_CONTACT_DEACTIVATE |

### 제98장 요약

```
요구사항·화면 이벤트·ServiceId·거래코드·권한을
코드 작성 전에 연결한다.

ServiceId는 기능별로 분리하고
조회·등록·변경·사용중지를 각각 관리한다.
```

## 제99장. 설계서와 프로그램 목록 작성하기

### 99.1 필요한 산출물

이번 기능에 필요한 기본 산출물은 다음과 같습니다.

```
화면 설계서

거래 설계서

프로그램 설계서

SQL 설계서

DB 설계서

테스트 시나리오

OM 등록대장

추적성 매트릭스
```

### 99.2 프로그램 목록

| 계층 | 프로그램 |
| --- | --- |
| Handler | CtContactHandler |
| Facade | CtContactFacade |
| Service | CtContactService |
| Rule | CtContactRule |
| DAO | CtContactDao |
| Mapper | CtContactMapper |
| IC Client | IcCustomerClient |
| Request DTO | 기능별 Request |
| Response DTO | 기능별 Response |
| Command | Create·Update Command |
| Query | List·Detail Query |
| Data | Master·List·Detail Data |

### 99.3 SQL 목록

| SQL ID | Mapper 메서드 | 기능 |
| --- | --- | --- |
| CT-CNT-SEL-001 | selectContactList | 목록조회 |
| CT-CNT-SEL-002 | countContactList | 목록건수 |
| CT-CNT-SEL-003 | selectContactDetail | 상세조회 |
| CT-CNT-INS-001 | insertContact | 상담 등록 |
| CT-CNT-UPD-001 | updateContact | 상담 변경 |
| CT-CNT-UPD-002 | deactivateContact | 사용중지 |
| CT-CNT-INS-002 | insertContactHistory | 변경이력 저장 |

### 99.4 정상 처리흐름

```
화면
→ POST /ct/online
→ OnlineTransactionController
→ TCF.process()
→ STF
→ CtContactHandler
→ CtContactFacade
→ CtContactService
→ CtContactRule
→ CtContactDao
→ CtContactMapper
→ DB
→ ETF
→ 표준 응답
```

### 99.5 오류 처리흐름

```
Validation 실패
→ BusinessException
→ DB 미실행
→ ETF 업무오류 응답

권한 실패
→ STF 차단
→ Handler 미실행

Version 불일치
→ 영향건수 0
→ 동시성 오류

SQL 오류
→ SystemException
→ 트랜잭션 Rollback
→ ETF 시스템오류 응답

Timeout
→ TIMEOUT 상태
→ 실제 DB 반영 여부 확인
```

### 제99장 요약

```
설계서는 소스 작성 후 만드는 문서가 아니다.

구현할 계층·DTO·SQL·DB·오류 흐름을
먼저 목록으로 정리한 뒤 코드를 작성한다.
```

## 제100장. 프로젝트와 패키지 골격 만들기

### 100.1 대상 모듈

```
ct-service
```

배포 결과:

```
ct-service.war
```

Context Path:

```
/ct
```

### 100.2 패키지 구조

```
com.nh.nsight.marketing.ct.contact
├─ handler
├─ facade
├─ service
├─ rule
├─ dao
├─ mapper
├─ client
├─ dto
│  ├─ request
│  ├─ response
│  ├─ command
│  ├─ query
│  └─ data
└─ config
```

### 100.3 파일 구조 예

```
ct-service
└─ src
   ├─ main
   │  ├─ java
   │  │  └─ com.nh.nsight.marketing.ct.contact
   │  │     ├─ handler
   │  │     │  └─ CtContactHandler.java
   │  │     ├─ facade
   │  │     │  └─ CtContactFacade.java
   │  │     ├─ service
   │  │     │  └─ CtContactService.java
   │  │     ├─ rule
   │  │     │  └─ CtContactRule.java
   │  │     ├─ dao
   │  │     │  └─ CtContactDao.java
   │  │     └─ mapper
   │  │        └─ CtContactMapper.java
   │  └─ resources
   │     └─ mapper
   │        └─ ct
   │           └─ contact
   │              └─ CtContactMapper.xml
   └─ test
      └─ java
```

### 100.4 기존 구조를 먼저 확인한다

신규 파일을 만들기 전에 기존 소스에서 다음을 찾습니다.

```
기존 Handler Interface

Facade Annotation

TransactionContext 사용법

BusinessException 생성방식

Mapper Scan 범위

공통 Response 구조

ServiceId 등록방식

테스트 Base Class
```

기존 Framework 계약을 확인하지 않고 새로운 공통구조를 임의로 만들지 않습니다.

### 100.5 Gradle 의존성 확인

개념 예:

```
dependencies {
    implementation project(":tcf-core")
    implementation project(":tcf-web")
    implementation project(":tcf-jwt")
    implementation project(":tcf-util")

    implementation "org.mybatis.spring.boot:mybatis-spring-boot-starter"
    implementation "org.springframework.boot:spring-boot-starter-validation"

    testImplementation "org.springframework.boot:spring-boot-starter-test"
}
```

실제 버전과 모듈명은 프로젝트 기준을 사용합니다.

### 100.6 잘못된 골격

```
controller
service
repository
entity
```

일반 Spring 예제를 그대로 복사해 TCF의 Handler·Facade·Rule 구조를 무시하면 안 됩니다.

### 제100장 요약

```
업무코드·도메인·계층 구조에 맞춰 Package를 만든다.

기존 TCF Interface와 공통 예외·Context 사용법을 먼저 확인한다.

일반 Spring 예제를 그대로 적용하지 않는다.
```

## 제101장. 요청·응답 DTO 설계하기

### 101.1 목록조회 Request

```
public record ContactListRequest(

    @NotBlank
    String customerNo,

    String contactTypeCode,

    @Pattern(regexp = "\\d{8}")
    String fromDate,

    @Pattern(regexp = "\\d{8}")
    String toDate,

    @Min(1)
    Integer pageNumber,

    @Min(1)
    @Max(100)
    Integer pageSize

) {
}
```

### 101.2 목록조회 Response

```
public record ContactListResponse(
    List<ContactListItem> items,
    int pageNumber,
    int pageSize,
    long totalCount
) {
}
public record ContactListItem(
    String contactId,
    String contactTypeCode,
    String contactTypeName,
    String contactTitle,
    LocalDateTime contactDtm,
    String createUserName,
    String branchId,
    String statusCode,
    long versionNo
) {
}
```

목록에는 상담내용 전문을 넣지 않습니다.

### 101.3 상세조회 Request

```
public record ContactDetailRequest(

    @NotBlank
    String contactId

) {
}
```

### 101.4 상세조회 Response

```
public record ContactDetailResponse(
    String contactId,
    String customerNo,
    String customerName,
    String contactTypeCode,
    String contactTitle,
    String contactContent,
    String processResult,
    String openYn,
    String statusCode,
    LocalDateTime contactDtm,
    String branchId,
    String createUserId,
    LocalDateTime createDtm,
    String updateUserId,
    LocalDateTime updateDtm,
    long versionNo
) {
}
```

### 101.5 등록 Request

```
public record ContactCreateRequest(

    @NotBlank
    String customerNo,

    @NotBlank
    String contactTypeCode,

    @NotBlank
    @Size(max = 100)
    String contactTitle,

    @NotBlank
    @Size(max = 4000)
    String contactContent,

    @Size(max = 1000)
    String processResult,

    @Pattern(regexp = "Y|N")
    String openYn,

    @NotNull
    LocalDateTime contactDtm

) {
}
```

### 101.6 변경 Request

```
public record ContactUpdateRequest(

    @NotBlank
    String contactId,

    @NotBlank
    String contactTypeCode,

    @NotBlank
    @Size(max = 100)
    String contactTitle,

    @NotBlank
    @Size(max = 4000)
    String contactContent,

    @Size(max = 1000)
    String processResult,

    @Pattern(regexp = "Y|N")
    String openYn,

    @NotNull
    Long versionNo

) {
}
```

### 101.7 사용중지 Request

```
public record ContactDeactivateRequest(

    @NotBlank
    String contactId,

    @NotNull
    Long versionNo,

    @NotBlank
    @Size(max = 200)
    String reason

) {
}
```

### 101.8 Command 분리

화면 Request를 DB에 그대로 전달하지 않습니다.

```
public record ContactCreateCommand(
    String contactId,
    String customerNo,
    String contactTypeCode,
    String contactTitle,
    String contactContent,
    String processResult,
    String openYn,
    LocalDateTime contactDtm,
    String branchId,
    String createUserId,
    String traceId
) {
}
```

branchId, createUserId, traceId는 화면이 아닌 TransactionContext에서 설정합니다.

### 101.9 화면에서 받으면 안 되는 값

```
createUserId

updateUserId

branchId

권한코드

statusCode 초기값

TraceId

ServiceId
```

표준 Header 일부는 화면이 보낼 수 있지만 최종 신뢰값은 인증·거래 Context에서 가져옵니다.

### 제101장 요약

```
기능별 Request와 Response를 분리한다.

목록과 상세 응답을 분리하여 불필요한 대용량 데이터를 줄인다.

사용자·지점·Trace 정보는
화면이 아니라 TransactionContext에서 가져온다.
```

## 제102장. Validation과 업무 Rule 구현하기

### 102.1 Validation 두 단계

```
DTO Validation
= 형식·필수·길이

Rule Validation
= 업무 의미·상태·권한 범위
```

### 102.2 목록조회 Rule

```
@Component
public class CtContactRule {

    private static final int MAX_SEARCH_MONTHS = 3;

    public ContactSearchPeriod normalizeAndValidatePeriod(
            String fromDate,
            String toDate,
            LocalDate baseDate) {

        LocalDate normalizedTo =
            toDate == null
                ? baseDate
                : parseDate(toDate);

        LocalDate normalizedFrom =
            fromDate == null
                ? normalizedTo.minusMonths(1)
                : parseDate(fromDate);

        if (normalizedFrom.isAfter(normalizedTo)) {
            throw new BusinessException(
                "E-CT-CNT-0001",
                "조회 시작일은 종료일보다 늦을 수 없습니다."
            );
        }

        if (normalizedFrom.plusMonths(
                MAX_SEARCH_MONTHS
            ).isBefore(normalizedTo)) {

            throw new BusinessException(
                "E-CT-CNT-0002",
                "조회기간은 최대 3개월입니다."
            );
        }

        return new ContactSearchPeriod(
            normalizedFrom,
            normalizedTo
        );
    }
}
```

### 102.3 등록 Rule

```
public void validateCreate(
        ContactCreateRequest request,
        CustomerBasicInfo customer) {

    if (!customer.active()) {
        throw new BusinessException(
            "E-CT-CNT-0003",
            "사용 가능한 고객이 아닙니다."
        );
    }

    if (request.contactDtm()
            .isAfter(LocalDateTime.now())) {

        throw new BusinessException(
            "E-CT-CNT-0004",
            "미래의 상담일시는 등록할 수 없습니다."
        );
    }
}
```

실제 코드에서는 Clock을 주입해 테스트 가능하게 구성합니다.

### 102.4 변경 가능 상태

```
public void validateUpdatable(
        ContactDetailData current,
        TransactionContext context) {

    if (!"ACTIVE".equals(current.statusCode())) {
        throw new BusinessException(
            "E-CT-CNT-0005",
            "사용중지된 상담이력은 변경할 수 없습니다."
        );
    }

    if (!current.branchId().equals(
            context.getBranchId())) {

        throw new BusinessException(
            "E-CT-CNT-0006",
            "다른 지점에서 등록한 비공개 상담은 변경할 수 없습니다."
        );
    }
}
```

### 102.5 공개여부 Rule

```
OPEN_YN = Y
→ 조회권한이 있는 사용자 조회 가능

OPEN_YN = N
→ 동일 지점 사용자만 조회 가능
```

조회 SQL과 Service Rule에서 일관되게 적용합니다.

### 102.6 금지 Rule

```
if ("Y".equals(request.adminYn())) {
    // 모든 데이터 허용
}
```

화면에서 전달한 관리자 여부를 신뢰하면 안 됩니다.

### 제102장 요약

```
DTO Validation은 형식을 검사하고,
Rule은 실제 업무 가능 여부를 판단한다.

상태·지점·고객 유효성·조회기간은 Rule에서 검증한다.

화면이 전달한 권한정보를 신뢰하지 않는다.
```

## 제103장. Handler 구현하기

### 103.1 Handler의 책임

```
ServiceId 등록

ServiceId 분기

Body를 Request DTO로 변환

Facade 호출

응답 반환
```

Handler가 SQL과 복잡한 업무규칙을 처리하면 안 됩니다.

### 103.2 ServiceId 상수

```
public final class CtContactServiceIds {

    public static final String SELECT_LIST =
        "CT.Contact.selectList";

    public static final String SELECT_DETAIL =
        "CT.Contact.selectDetail";

    public static final String CREATE =
        "CT.Contact.create";

    public static final String UPDATE =
        "CT.Contact.update";

    public static final String DEACTIVATE =
        "CT.Contact.deactivate";

    private CtContactServiceIds() {
    }
}
```

### 103.3 Handler 예

```
@Component
@RequiredArgsConstructor
public class CtContactHandler
        implements TransactionHandler {

    private final CtContactFacade contactFacade;
    private final RequestBodyConverter bodyConverter;

    @Override
    public Set<String> serviceIds() {
        return Set.of(
            CtContactServiceIds.SELECT_LIST,
            CtContactServiceIds.SELECT_DETAIL,
            CtContactServiceIds.CREATE,
            CtContactServiceIds.UPDATE,
            CtContactServiceIds.DEACTIVATE
        );
    }

    @Override
    public Object handle(
            String serviceId,
            Object body,
            TransactionContext context) {

        return switch (serviceId) {

            case CtContactServiceIds.SELECT_LIST ->
                contactFacade.selectList(
                    bodyConverter.convert(
                        body,
                        ContactListRequest.class
                    ),
                    context
                );

            case CtContactServiceIds.SELECT_DETAIL ->
                contactFacade.selectDetail(
                    bodyConverter.convert(
                        body,
                        ContactDetailRequest.class
                    ),
                    context
                );

            case CtContactServiceIds.CREATE ->
                contactFacade.create(
                    bodyConverter.convert(
                        body,
                        ContactCreateRequest.class
                    ),
                    context
                );

            case CtContactServiceIds.UPDATE ->
                contactFacade.update(
                    bodyConverter.convert(
                        body,
                        ContactUpdateRequest.class
                    ),
                    context
                );

            case CtContactServiceIds.DEACTIVATE ->
                contactFacade.deactivate(
                    bodyConverter.convert(
                        body,
                        ContactDeactivateRequest.class
                    ),
                    context
                );

            default ->
                throw new ServiceNotFoundException(
                    serviceId
                );
        };
    }
}
```

실제 Handler Interface와 메서드 Signature는 Framework 기준을 사용합니다.

### 103.4 Handler 금지 예

```
case CREATE -> {
    validateCustomer(body);
    mapper.insertContact(body);
    mapper.insertHistory(body);
    return response;
}
```

Handler 안에서 Rule과 Mapper를 직접 호출하면 계층 책임이 무너집니다.

### 제103장 요약

```
Handler는 ServiceId와 Facade를 연결한다.

업무 검증·SQL·트랜잭션은 Handler에서 처리하지 않는다.

등록된 ServiceId 외 요청은 명확하게 거부한다.
```

## 제104장. Facade와 Service 구현하기

### 104.1 Facade의 책임

```
유스케이스 시작점

트랜잭션 경계

여러 Service 조정

결과 반환
```

### 104.2 Facade 예

```
@Component
@RequiredArgsConstructor
public class CtContactFacade {

    private final CtContactService contactService;

    @Transactional(readOnly = true)
    public ContactListResponse selectList(
            ContactListRequest request,
            TransactionContext context) {

        return contactService.selectList(
            request,
            context
        );
    }

    @Transactional(readOnly = true)
    public ContactDetailResponse selectDetail(
            ContactDetailRequest request,
            TransactionContext context) {

        return contactService.selectDetail(
            request,
            context
        );
    }

    @Transactional
    public ContactCreateResponse create(
            ContactCreateRequest request,
            TransactionContext context) {

        return contactService.create(
            request,
            context
        );
    }

    @Transactional
    public ContactUpdateResponse update(
            ContactUpdateRequest request,
            TransactionContext context) {

        return contactService.update(
            request,
            context
        );
    }

    @Transactional
    public void deactivate(
            ContactDeactivateRequest request,
            TransactionContext context) {

        contactService.deactivate(
            request,
            context
        );
    }
}
```

### 104.3 목록조회 Service

```
@Service
@RequiredArgsConstructor
public class CtContactService {

    private final CtContactRule contactRule;
    private final CtContactDao contactDao;
    private final IcCustomerClient customerClient;
    private final Clock clock;

    public ContactListResponse selectList(
            ContactListRequest request,
            TransactionContext context) {

        ContactSearchPeriod period =
            contactRule.normalizeAndValidatePeriod(
                request.fromDate(),
                request.toDate(),
                LocalDate.now(clock)
            );

        int pageNumber =
            request.pageNumber() == null
                ? 1
                : request.pageNumber();

        int pageSize =
            request.pageSize() == null
                ? 20
                : request.pageSize();

        ContactListQuery query =
            new ContactListQuery(
                request.customerNo(),
                request.contactTypeCode(),
                period.fromDate(),
                period.toDate(),
                pageNumber,
                pageSize,
                context.getBranchId(),
                context.getUserId()
            );

        long totalCount =
            contactDao.countContactList(query);

        List<ContactListData> data =
            totalCount == 0
                ? List.of()
                : contactDao.selectContactList(query);

        return ContactListResponseMapper.map(
            data,
            pageNumber,
            pageSize,
            totalCount
        );
    }
}
```

### 104.4 상세조회 Service

```
public ContactDetailResponse selectDetail(
        ContactDetailRequest request,
        TransactionContext context) {

    ContactDetailData current =
        contactDao.selectContactDetail(
            request.contactId()
        );

    if (current == null) {
        throw new BusinessException(
            "E-CT-CNT-0007",
            "상담이력을 찾을 수 없습니다."
        );
    }

    contactRule.validateReadable(
        current,
        context
    );

    CustomerBasicInfo customer =
        customerClient.selectBasic(
            current.customerNo(),
            context
        );

    return ContactDetailResponseMapper.map(
        current,
        customer
    );
}
```

### 104.5 등록 Service

```
public ContactCreateResponse create(
        ContactCreateRequest request,
        TransactionContext context) {

    CustomerBasicInfo customer =
        customerClient.selectBasic(
            request.customerNo(),
            context
        );

    contactRule.validateCreate(
        request,
        customer
    );

    String contactId =
        contactIdGenerator.generate();

    ContactCreateCommand command =
        new ContactCreateCommand(
            contactId,
            request.customerNo(),
            request.contactTypeCode(),
            request.contactTitle(),
            request.contactContent(),
            request.processResult(),
            defaultOpenYn(request.openYn()),
            request.contactDtm(),
            context.getBranchId(),
            context.getUserId(),
            context.getTraceId()
        );

    int inserted =
        contactDao.insertContact(command);

    if (inserted != 1) {
        throw new SystemException(
            "E-CT-DB-0001",
            "상담이력 등록 결과가 올바르지 않습니다."
        );
    }

    contactDao.insertCreateHistory(command);

    return new ContactCreateResponse(
        contactId,
        1L
    );
}
```

### 104.6 변경 Service

```
public ContactUpdateResponse update(
        ContactUpdateRequest request,
        TransactionContext context) {

    ContactDetailData current =
        requireContact(request.contactId());

    contactRule.validateUpdatable(
        current,
        context
    );

    ContactUpdateCommand command =
        ContactUpdateCommand.from(
            request,
            context
        );

    int updated =
        contactDao.updateContact(command);

    if (updated == 0) {
        throw new BusinessException(
            "E-CT-CNT-0008",
            "다른 사용자가 먼저 변경했습니다. 다시 조회해 주세요."
        );
    }

    ContactDetailData changed =
        requireContact(request.contactId());

    contactDao.insertUpdateHistory(
        current,
        changed,
        context
    );

    return new ContactUpdateResponse(
        request.contactId(),
        changed.versionNo()
    );
}
```

### 104.7 사용중지 Service

```
public void deactivate(
        ContactDeactivateRequest request,
        TransactionContext context) {

    ContactDetailData current =
        requireContact(request.contactId());

    contactRule.validateDeactivatable(
        current,
        context
    );

    int updated =
        contactDao.deactivateContact(
            new ContactDeactivateCommand(
                request.contactId(),
                request.versionNo(),
                request.reason(),
                context.getUserId(),
                context.getTraceId()
            )
        );

    if (updated == 0) {
        throw new BusinessException(
            "E-CT-CNT-0008",
            "다른 사용자가 먼저 변경했습니다. 다시 조회해 주세요."
        );
    }

    contactDao.insertDeactivateHistory(
        current,
        request.reason(),
        context
    );
}
```

### 104.8 외부 호출과 트랜잭션

고객 기본정보 조회는 등록 트랜잭션 시작 전 또는 짧은 범위에서 처리하는 것이 바람직합니다.

```
외부 호출 장시간 대기
+ DB Transaction 유지
→ DB Connection과 Lock 장기 점유
```

실제 Facade 트랜잭션 시작위치는 Framework와 호출구조에 따라 조정합니다.

### 제104장 요약

```
Facade는 유스케이스와 트랜잭션 경계를 담당한다.

Service는 고객확인·Rule·DAO 호출 순서를 조정한다.

변경은 현재 데이터 조회, 상태검증, Version 확인,
변경, 이력 저장 순서로 처리한다.
```

## 제105장. DAO·Mapper·SQL 구현하기

### 105.1 DAO

```
@Repository
@RequiredArgsConstructor
public class CtContactDao {

    private final CtContactMapper contactMapper;

    public List<ContactListData> selectContactList(
            ContactListQuery query) {

        return contactMapper.selectContactList(
            query
        );
    }

    public long countContactList(
            ContactListQuery query) {

        return contactMapper.countContactList(
            query
        );
    }

    public ContactDetailData selectContactDetail(
            String contactId) {

        return contactMapper.selectContactDetail(
            contactId
        );
    }

    public int insertContact(
            ContactCreateCommand command) {

        return contactMapper.insertContact(
            command
        );
    }

    public int updateContact(
            ContactUpdateCommand command) {

        return contactMapper.updateContact(
            command
        );
    }

    public int deactivateContact(
            ContactDeactivateCommand command) {

        return contactMapper.deactivateContact(
            command
        );
    }
}
```

### 105.2 Mapper Interface

```
@Mapper
public interface CtContactMapper {

    List<ContactListData> selectContactList(
        ContactListQuery query
    );

    long countContactList(
        ContactListQuery query
    );

    ContactDetailData selectContactDetail(
        String contactId
    );

    int insertContact(
        ContactCreateCommand command
    );

    int updateContact(
        ContactUpdateCommand command
    );

    int deactivateContact(
        ContactDeactivateCommand command
    );

    int insertContactHistory(
        ContactHistoryCommand command
    );
}
```

### 105.3 목록조회 SQL

```
<select
    id="selectContactList"
    resultType="com.nh.nsight.marketing.ct.contact.dto.data.ContactListData">

    SELECT CONTACT_ID,
           CONTACT_TYPE_CD,
           CONTACT_TITLE,
           CONTACT_DTM,
           CREATE_USER_ID,
           BRANCH_ID,
           STATUS_CD,
           VERSION_NO
      FROM CT_CONTACT_MASTER
     WHERE CUSTOMER_NO = #{customerNo}
       AND CONTACT_DTM >= #{fromDtm}
       AND CONTACT_DTM <  #{toDtmExclusive}
       AND STATUS_CD = 'ACTIVE'
       AND (
              OPEN_YN = 'Y'
           OR BRANCH_ID = #{branchId}
       )

    <if test="contactTypeCode != null
              and contactTypeCode != ''">
       AND CONTACT_TYPE_CD =
           #{contactTypeCode}
    </if>

     ORDER BY CONTACT_DTM DESC,
              CONTACT_ID DESC

     OFFSET #{offset} ROWS
     FETCH NEXT #{pageSize} ROWS ONLY
</select>
```

대량 데이터에서는 Keyset Paging을 검토합니다.

### 105.4 목록 Count SQL

```
<select
    id="countContactList"
    resultType="long">

    SELECT COUNT(*)
      FROM CT_CONTACT_MASTER
     WHERE CUSTOMER_NO = #{customerNo}
       AND CONTACT_DTM >= #{fromDtm}
       AND CONTACT_DTM <  #{toDtmExclusive}
       AND STATUS_CD = 'ACTIVE'
       AND (
              OPEN_YN = 'Y'
           OR BRANCH_ID = #{branchId}
       )

    <if test="contactTypeCode != null
              and contactTypeCode != ''">
       AND CONTACT_TYPE_CD =
           #{contactTypeCode}
    </if>
</select>
```

목록 SQL과 Count SQL의 조건이 다르면 페이지 수가 잘못 표시됩니다.

### 105.5 등록 SQL

```
<insert id="insertContact">

    INSERT INTO CT_CONTACT_MASTER
    (
        CONTACT_ID,
        CUSTOMER_NO,
        CONTACT_TYPE_CD,
        CONTACT_TITLE,
        CONTACT_CONTENT,
        PROCESS_RESULT,
        OPEN_YN,
        STATUS_CD,
        CONTACT_DTM,
        BRANCH_ID,
        VERSION_NO,
        CREATE_USER_ID,
        CREATE_DTM,
        UPDATE_USER_ID,
        UPDATE_DTM
    )
    VALUES
    (
        #{contactId},
        #{customerNo},
        #{contactTypeCode},
        #{contactTitle},
        #{contactContent},
        #{processResult},
        #{openYn},
        'ACTIVE',
        #{contactDtm},
        #{branchId},
        1,
        #{createUserId},
        CURRENT_TIMESTAMP,
        #{createUserId},
        CURRENT_TIMESTAMP
    )
</insert>
```

### 105.6 변경 SQL

```
<update id="updateContact">

    UPDATE CT_CONTACT_MASTER
       SET CONTACT_TYPE_CD = #{contactTypeCode},
           CONTACT_TITLE   = #{contactTitle},
           CONTACT_CONTENT = #{contactContent},
           PROCESS_RESULT  = #{processResult},
           OPEN_YN         = #{openYn},
           VERSION_NO      = VERSION_NO + 1,
           UPDATE_USER_ID  = #{updateUserId},
           UPDATE_DTM      = CURRENT_TIMESTAMP
     WHERE CONTACT_ID      = #{contactId}
       AND VERSION_NO      = #{versionNo}
       AND STATUS_CD       = 'ACTIVE'
</update>
```

### 105.7 사용중지 SQL

```
<update id="deactivateContact">

    UPDATE CT_CONTACT_MASTER
       SET STATUS_CD      = 'INACTIVE',
           VERSION_NO     = VERSION_NO + 1,
           UPDATE_USER_ID = #{updateUserId},
           UPDATE_DTM     = CURRENT_TIMESTAMP
     WHERE CONTACT_ID     = #{contactId}
       AND VERSION_NO     = #{versionNo}
       AND STATUS_CD      = 'ACTIVE'
</update>
```

### 105.8 이력 SQL

```
<insert id="insertContactHistory">

    INSERT INTO CT_CONTACT_HISTORY
    (
        HISTORY_ID,
        CONTACT_ID,
        CHANGE_TYPE_CD,
        BEFORE_DATA,
        AFTER_DATA,
        CHANGE_USER_ID,
        CHANGE_DTM,
        TRACE_ID
    )
    VALUES
    (
        #{historyId},
        #{contactId},
        #{changeTypeCode},
        #{beforeData},
        #{afterData},
        #{changeUserId},
        CURRENT_TIMESTAMP,
        #{traceId}
    )
</insert>
```

### 105.9 인덱스 검토

목록조회 조건:

```
CUSTOMER_NO

CONTACT_DTM

STATUS_CD

BRANCH_ID

CONTACT_TYPE_CD
```

후보 인덱스 예:

```
IX_CT_CONTACT_MASTER_01
(CUSTOMER_NO, STATUS_CD, CONTACT_DTM DESC)
```

인덱스는 SQL 조건과 데이터 분포를 DBA와 함께 검토합니다.

### 105.10 금지 SQL

```
SELECT *
FROM CT_CONTACT_MASTER
UPDATE CT_CONTACT_MASTER
SET STATUS_CD = 'INACTIVE'
WHERE CONTACT_ID = #{contactId}
```

Version과 현재 상태 확인이 없습니다.

### 제105장 요약

```
Mapper 메서드와 XML ID를 일치시킨다.

목록과 Count SQL 조건을 동일하게 유지한다.

변경 SQL에는 Version과 현재 상태 조건을 포함한다.

데이터 변경과 이력 저장은 같은 트랜잭션에서 처리한다.
```

## 제106장. 인증·권한·데이터권한 적용하기

### 106.1 인증 사용자

```
JWT sub
→ AuthenticationContext.userId
→ TransactionContext.userId
```

Request Body의 userId를 사용하지 않습니다.

### 106.2 기능권한

STF에서 ServiceId별 권한을 확인합니다.

```
CT.Contact.selectList
→ CT_CONTACT_VIEW

CT.Contact.create
→ CT_CONTACT_CREATE
```

권한이 없으면 Handler가 실행되지 않습니다.

### 106.3 데이터권한

조회권한이 있어도 모든 상담이력을 조회할 수 있는 것은 아닙니다.

```
OPEN_YN = Y
→ 권한 사용자 조회 가능

OPEN_YN = N
→ 등록지점 사용자만 조회 가능
```

SQL과 Rule에서 검증합니다.

### 106.4 변경권한

다음 조건을 모두 만족해야 합니다.

```
CT_CONTACT_UPDATE 권한 보유

상담상태 ACTIVE

등록지점과 사용자 지점 일치

Version 일치
```

관리자 예외가 있다면 별도 권한코드를 사용합니다.

```
CT_CONTACT_UPDATE_ALL_BRANCH
```

### 106.5 Header 정합성

```
Header userId
= AuthenticationContext userId

Header branchId
= AuthenticationContext branchId
```

불일치는 위변조 가능성으로 차단하거나 인증 Context 값으로 보정하되 보안로그를 남깁니다.

### 106.6 감사대상

다음 작업은 감사로그를 남깁니다.

```
상담내용 상세조회

상담 등록

상담 변경

상담 사용중지

대량 다운로드
```

고객 상담내용은 개인정보나 민감한 영업정보를 포함할 수 있습니다.

### 제106장 요약

```
기능권한은 STF에서 확인하고,
데이터권한은 Service·Rule·SQL에서 확인한다.

사용자와 지점은 인증 Context를 신뢰한다.

상담 상세조회와 변경은 감사대상으로 관리한다.
```

## 제107장. 오류·Timeout·중복·동시성 처리

### 107.1 오류코드 목록

| 오류코드 | 의미 |
| --- | --- |
| E-CT-CNT-0001 | 조회 시작일 오류 |
| E-CT-CNT-0002 | 조회기간 초과 |
| E-CT-CNT-0003 | 사용 불가 고객 |
| E-CT-CNT-0004 | 미래 상담일시 |
| E-CT-CNT-0005 | 변경 불가 상태 |
| E-CT-CNT-0006 | 지점 데이터권한 없음 |
| E-CT-CNT-0007 | 상담이력 없음 |
| E-CT-CNT-0008 | 동시성 충돌 |
| E-CT-DB-0001 | DB 처리결과 이상 |
| E-TCF-TIME-0001 | 거래 Timeout |

### 107.2 Timeout 계층

```
UI
약 8초

Gateway
약 7초

TCF 변경 ServiceId
5초

DB Query
2~3초

IC 고객조회 Client
700ms
```

하위 Timeout이 상위보다 길지 않도록 합니다.

### 107.3 등록 Idempotency

사용자가 저장 버튼을 두 번 클릭할 수 있습니다.

표준 Header:

```
{
  "idempotencyKey": "CT-USER01-20260717-0001"
}
```

처리:

```
동일 Key 최초 요청
→ 등록 처리
→ 결과 저장

동일 Key 재요청
→ 기존 결과 반환
```

### 107.4 업무 중복

같은 고객에게 같은 시각과 제목으로 상담이 등록되었더라도 실제로 다른 상담일 수 있습니다.

따라서 업무 중복 조건은 업무팀과 확정해야 합니다.

이번 사례에서는 자동 중복차단보다 Idempotency Key를 사용합니다.

### 107.5 동시성 충돌

사용자 A와 B가 같은 상담이력을 조회합니다.

```
Version = 3
```

사용자 A가 먼저 변경합니다.

```
Version = 4
```

사용자 B의 Update 조건:

```
WHERE VERSION_NO = 3
```

영향 건수는 0입니다.

```
다른 사용자가 먼저 변경했습니다.
다시 조회해 주세요.
```

### 107.6 Timeout 후 상태

등록 SQL이 Commit되었지만 응답이 늦어 Timeout될 가능성을 고려합니다.

```
화면 Timeout
→ 같은 Idempotency Key로 결과조회
→ 실제 등록 여부 확인
```

무조건 신규 Key로 재등록하면 중복 상담이 생성될 수 있습니다.

### 107.7 Rollback

Master 등록 성공 후 History 저장이 실패하면 전체를 Rollback합니다.

```
Master INSERT
→ 성공

History INSERT
→ 실패

Facade Transaction
→ 전체 Rollback
```

### 제107장 요약

```
업무 오류와 시스템 오류를 코드로 구분한다.

등록은 Idempotency Key로 중복요청을 방지한다.

변경은 Version으로 동시성을 제어한다.

Timeout 후에는 실제 DB 반영 여부를 확인한다.
```

## 제108장. 표준 요청과 응답 예제

### 108.1 목록조회 요청

```
{
  "header": {
    "businessCode": "CT",
    "serviceId": "CT.Contact.selectList",
    "transactionCode": "CT-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEB"
  },
  "body": {
    "customerNo": "CUST000001",
    "contactTypeCode": "CONSULT",
    "fromDate": "20260701",
    "toDate": "20260717",
    "pageNumber": 1,
    "pageSize": 20
  }
}
```

### 108.2 목록조회 응답

```
{
  "result": {
    "resultStatus": "SUCCESS",
    "resultCode": "S0000"
  },
  "body": {
    "items": [
      {
        "contactId": "CT202607170001",
        "contactTypeCode": "CONSULT",
        "contactTypeName": "일반상담",
        "contactTitle": "상품 문의",
        "contactDtm": "2026-07-17T09:30:00",
        "createUserName": "홍길동",
        "branchId": "001234",
        "statusCode": "ACTIVE",
        "versionNo": 1
      }
    ],
    "pageNumber": 1,
    "pageSize": 20,
    "totalCount": 1
  }
}
```

### 108.3 등록 요청

```
{
  "header": {
    "businessCode": "CT",
    "serviceId": "CT.Contact.create",
    "transactionCode": "CT-REG-0001",
    "processingType": "REGISTRATION",
    "channelId": "WEB",
    "idempotencyKey": "CT-USER01-20260717-0001"
  },
  "body": {
    "customerNo": "CUST000001",
    "contactTypeCode": "CONSULT",
    "contactTitle": "예금상품 문의",
    "contactContent": "고객이 예금상품의 금리를 문의함",
    "processResult": "상품 설명 후 안내자료 제공",
    "openYn": "N",
    "contactDtm": "2026-07-17T09:30:00"
  }
}
```

### 108.4 등록 응답

```
{
  "result": {
    "resultStatus": "SUCCESS",
    "resultCode": "S0000"
  },
  "body": {
    "contactId": "CT202607170001",
    "versionNo": 1
  }
}
```

### 108.5 동시성 오류 응답

```
{
  "result": {
    "resultStatus": "FAIL",
    "resultCode": "E-CT-CNT-0008",
    "message": "다른 사용자가 먼저 변경했습니다. 다시 조회해 주세요."
  },
  "body": null,
  "error": {
    "errorType": "BUSINESS",
    "retryable": false
  }
}
```

## 제109장. 테스트 작성하기

### 109.1 Rule 테스트

```
class CtContactRuleTest {

    private final Clock clock =
        Clock.fixed(
            Instant.parse("2026-07-17T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
        );

    private final CtContactRule rule =
        new CtContactRule(clock);

    @Test
    void shouldRejectSearchPeriodOverThreeMonths() {
        assertThatThrownBy(
            () -> rule.normalizeAndValidatePeriod(
                "20260101",
                "20260717",
                LocalDate.of(2026, 7, 17)
            )
        )
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo("E-CT-CNT-0002");
    }
}
```

### 109.2 Service 등록 테스트

```
@ExtendWith(MockitoExtension.class)
class CtContactServiceTest {

    @Mock
    private CtContactDao contactDao;

    @Mock
    private IcCustomerClient customerClient;

    @Mock
    private CtContactRule contactRule;

    @Mock
    private ContactIdGenerator idGenerator;

    @InjectMocks
    private CtContactService contactService;

    @Test
    void shouldCreateContactAndHistory() {

        given(customerClient.selectBasic(
            anyString(),
            any()
        )).willReturn(
            new CustomerBasicInfo(
                "CUST000001",
                "홍길동",
                true
            )
        );

        given(idGenerator.generate())
            .willReturn("CT202607170001");

        given(contactDao.insertContact(any()))
            .willReturn(1);

        ContactCreateResponse response =
            contactService.create(
                createRequest(),
                transactionContext()
            );

        assertThat(response.contactId())
            .isEqualTo("CT202607170001");

        then(contactDao)
            .should()
            .insertCreateHistory(any());
    }
}
```

### 109.3 동시성 테스트

```
@Test
void shouldRejectUpdateWhenVersionChanged() {

    given(contactDao.selectContactDetail(
        "CT001"
    )).willReturn(
        contactDataWithVersion(4)
    );

    given(contactDao.updateContact(any()))
        .willReturn(0);

    assertThatThrownBy(
        () -> contactService.update(
            updateRequestWithVersion(3),
            transactionContext()
        )
    )
    .isInstanceOf(BusinessException.class)
    .extracting("errorCode")
    .isEqualTo("E-CT-CNT-0008");
}
```

### 109.4 Mapper 테스트

검증내용:

```
목록조건

지점 공개조건

날짜 경계

정렬순서

Count 일치

Version Update

사용중지 상태

History 저장
```

### 109.5 거래 테스트

```
@SpringBootTest
@AutoConfigureMockMvc
class CtContactCreateTransactionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateContactThroughTcf()
            throws Exception {

        mockMvc.perform(
            post("/ct/online")
                .header(
                    "Authorization",
                    "Bearer " + testToken()
                )
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .content(createRequestJson())
        )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.result.resultStatus")
                .value("SUCCESS")
        )
        .andExpect(
            jsonPath("$.body.contactId")
                .exists()
        );
    }
}
```

### 109.6 필수 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| CT-001 | 정상 목록조회 | 목록 반환 |
| CT-002 | 결과 없음 | 빈 목록 성공 |
| CT-003 | 조회기간 초과 | 업무 오류 |
| CT-004 | 비공개 타 지점 조회 | 권한 오류 |
| CT-005 | 정상 상세조회 | 상세 반환 |
| CT-006 | 상담이력 없음 | 업무 오류 |
| CT-007 | 정상 등록 | Master·History 저장 |
| CT-008 | 고객 없음 | 업무 오류 |
| CT-009 | 중복 요청 | 기존 결과 반환 |
| CT-010 | History 실패 | 전체 Rollback |
| CT-011 | 정상 변경 | Version 증가 |
| CT-012 | Version 충돌 | 동시성 오류 |
| CT-013 | 사용중지 자료 변경 | 업무 오류 |
| CT-014 | 정상 사용중지 | 상태 INACTIVE |
| CT-015 | 권한 없음 | Handler 미실행 |
| CT-016 | ServiceId 통제 | Handler 미실행 |
| CT-017 | SQL Timeout | TIMEOUT 응답 |
| CT-018 | Header 사용자 위조 | 보안 오류 |
| CT-019 | JWT 만료 | 인증 오류 |
| CT-020 | Slow SQL | OM 추적 가능 |

### 제109장 요약

```
Rule·Service·Mapper·TCF 거래 테스트를 나누어 작성한다.

정상 흐름뿐 아니라 권한·Rollback·동시성·Timeout을 검증한다.

테스트는 화면 요구사항과 추적성 ID에 연결한다.
```

## 제110장. OM 등록과 운영 준비

### 110.1 Service Catalog 등록

| ServiceId | 상태 | Timeout | 권한 |
| --- | --- | --- | --- |
| CT.Contact.selectList | ACTIVE | 3,000ms | CT_CONTACT_VIEW |
| CT.Contact.selectDetail | ACTIVE | 3,000ms | CT_CONTACT_VIEW |
| CT.Contact.create | ACTIVE | 5,000ms | CT_CONTACT_CREATE |
| CT.Contact.update | ACTIVE | 5,000ms | CT_CONTACT_UPDATE |
| CT.Contact.deactivate | ACTIVE | 5,000ms | CT_CONTACT_DEACTIVATE |

### 110.2 운영 Metric

ServiceId별로 다음을 수집합니다.

```
호출 건수

성공률

업무 오류율

시스템 오류율

Timeout율

평균 응답시간

p95·p99

DB Connection 대기시간

Mapper·SQL 수행시간
```

### 110.3 Slow SQL 연결

```
CT.Contact.selectList
→ CtContactMapper.selectContactList
→ CT-CNT-SEL-001
```

OM에서 ServiceId에서 SQL까지 이동할 수 있어야 합니다.

### 110.4 거래통제

장애 시 다음과 같이 통제할 수 있습니다.

```
목록조회
ACTIVE

상세조회
ACTIVE

등록
SUSPENDED

변경
SUSPENDED

사용중지
SUSPENDED
```

조회는 유지하고 변경만 중지할 수 있습니다.

### 110.5 운영 Runbook

#### 목록조회 지연

```
ServiceId p95 확인

CT WAR Thread 확인

CT DB Pool Pending 확인

CT-CNT-SEL-001 수행시간 확인

조회기간·결과건수 확인

인덱스·실행계획 확인
```

#### 등록 Timeout

```
Idempotency Key 확인

Master 저장 여부 확인

History 저장 여부 확인

Transaction 상태 확인

재등록 전 기존 결과조회
```

#### Version 충돌 급증

```
화면이 최신 Version을 전달하는지 확인

동일 상담 동시 편집 여부 확인

Cache된 상세데이터 사용 여부 확인

변경 후 목록·상세 재조회 여부 확인
```

### 110.6 로그 마스킹

금지:

```
상담내용 전체 로그

고객번호 원문 과다기록

JWT 원문

Request 전문 전체
```

권장:

```
contactId

ServiceId

TraceId

customerNo 마스킹

오류코드

처리시간
```

### 제110장 요약

```
개발된 ServiceId는 OM Catalog에 등록해야 한다.

운영자는 ServiceId·Mapper·SQL을 연결해 조회할 수 있어야 한다.

장애 시 조회와 변경 거래를 분리 통제할 수 있어야 한다.
```

## 제111장. 빌드·배포·Smoke Test

### 111.1 로컬 Build

```
./gradlew :ct-service:clean :ct-service:build
```

확인:

```
Compile 성공

단위 테스트 성공

Mapper XML 포함

Architecture Test 성공

WAR 생성
```

### 111.2 WAR 확인

```
ct-service/build/libs/ct-service-1.0.0.war
```

WAR 내부:

```
WEB-INF/classes
→ CtContactHandler.class

WEB-INF/classes/mapper/ct/contact
→ CtContactMapper.xml

WEB-INF/lib
→ tcf-core 등 공통 JAR
```

### 111.3 배포 전 확인

```
OM Service Catalog 등록

권한코드 등록

DB 테이블·인덱스 반영

환경설정 확인

IC Client Route 확인

Rollback WAR 확보

Smoke Test 데이터 준비
```

### 111.4 배포 흐름

```
대상 서버 Drain

기존 WAR 백업

신규 ct-service WAR 배포

Tomcat 기동

Readiness 확인

대표 거래 Smoke Test

Metric 확인

트래픽 복귀
```

### 111.5 Smoke Test

#### 조회

```
CT.Contact.selectList
→ 정상 목록 또는 빈 목록
```

#### 등록

운영 전용 테스트 고객과 테스트 상담구분을 사용합니다.

```
등록 성공

Master 확인

History 확인

거래로그 확인
```

#### 변경·사용중지

```
Version 증가 확인

이력 저장 확인

상태 변경 확인
```

테스트 데이터는 종료 후 정책에 따라 제거하거나 사용중지합니다.

### 111.6 배포 성공 기준

```
ct-service Health UP

5개 ServiceId Catalog 정상

Smoke Test 성공

시스템 오류 0건

DB Pool Pending 0

Slow SQL 없음

로그에 개인정보 원문 없음

서버별 WAR Version 동일
```

### 111.7 Rollback

```
신규 WAR 트래픽 제외

이전 WAR 복구

기동

Health Check

목록조회 Smoke Test

트래픽 복귀
```

DB Schema가 하위 호환되는지 배포 전에 확인합니다.

### 제111장 요약

```
배포 전 코드·DB·OM·권한·Route를 함께 확인한다.

WAR 복사만으로 배포가 완료되는 것은 아니다.

Health와 대표 ServiceId Smoke Test가 성공해야 한다.
```

## 제112장. 화면과 테이블 정보만 있을 때의 표준 개발절차

### 112.1 1단계: 자료 수집

```
화면 이미지

화면 항목

버튼과 이벤트

테이블 정의

코드값

업무 설명

권한

예상 건수

응답시간 요구
```

### 112.2 2단계: 질문 목록 작성

```
필수값은 무엇인가?

조회범위는 어디까지인가?

상태전이는 무엇인가?

삭제는 물리인가 논리인가?

중복기준은 무엇인가?

다른 업무 데이터가 필요한가?

이력을 남겨야 하는가?

동시성 제어가 필요한가?

개인정보가 포함되는가?
```

### 112.3 3단계: 업무 경계 확정

```
소유 업무코드

도메인

대상 WAR

소유 테이블

타 업무 연계

운영 담당자
```

### 112.4 4단계: 식별자 발급

```
요구사항 ID

화면 ID

이벤트 ID

ServiceId

거래코드

권한코드

SQL ID

오류코드
```

### 112.5 5단계: 추적성 작성

```
화면 이벤트
→ ServiceId
→ Handler
→ Facade
→ Service
→ Mapper
→ SQL
→ Table
```

### 112.6 6단계: DTO 설계

```
Request

Response

Command

Query

Data

Result
```

화면 DTO와 DB DTO를 분리합니다.

### 112.7 7단계: 업무 Rule 설계

```
필수·형식

상태

권한

날짜

금액

중복

동시성

이력
```

### 112.8 8단계: 정상·오류 흐름 작성

```
정상 흐름

Validation 오류

권한 오류

데이터 없음

동시성 충돌

SQL 오류

Timeout

Rollback
```

### 112.9 9단계: 계층별 구현

```
Handler

Facade

Service

Rule

DAO

Mapper
```

### 112.10 10단계: 테스트

```
Rule 단위 테스트

Service 테스트

Mapper 테스트

거래 테스트

권한 테스트

Timeout 테스트

Rollback 테스트
```

### 112.11 11단계: OM 등록

```
Service Catalog

Timeout

권한

거래통제

오류코드

Metric

운영 담당자
```

### 112.12 12단계: 배포·운영 인수

```
Build

품질 Gate

WAR

DB Script

OM 기준정보

Smoke Test

모니터링

Rollback

운영 Runbook
```

### 제112장 요약

```
화면과 테이블만 받아도 개발은 가능하다.

단, 화면을 바로 코드로 바꾸는 것이 아니라
업무경계·Rule·식별자·오류·운영요건을 먼저 정리해야 한다.
```

## 제113장. AI 개발도구를 활용하는 방법

### 113.1 AI에게 화면과 테이블만 주면 충분할까요?

AI는 코드를 만들 수 있지만 업무 의미와 프로젝트 표준을 자동으로 완벽하게 판단하지는 못합니다.

잘못된 입력:

```
이 화면과 테이블로 CRUD 만들어 줘.
```

결과 위험:

```
일반 Controller 생성

TCF Handler 누락

Session에서 사용자 조회

다른 업무 테이블 직접 접근

물리 DELETE 사용

Timeout·권한·OM 누락

테스트 없는 코드
```

### 113.2 AI 입력에 포함할 정보

```
업무코드와 도메인

화면 ID와 이벤트 ID

ServiceId와 거래코드

패키지 표준

계층별 책임

Request·Response 항목

업무 Rule

테이블과 컬럼

트랜잭션 범위

오류코드

권한

Timeout

테스트 요구사항

금지사항
```

### 113.3 AI 요청 예

```
NSIGHT TCF 표준에 맞춰 고객 상담이력 등록 거래를 구현한다.

업무코드: CT
도메인: Contact
ServiceId: CT.Contact.create
거래코드: CT-REG-0001
Endpoint: POST /ct/online

구조:
Handler → Facade → Service → Rule → DAO → Mapper

규칙:
- Handler는 Facade만 호출한다.
- Facade가 트랜잭션 경계다.
- 사용자와 지점은 TransactionContext에서 가져온다.
- CT_CONTACT_MASTER와 CT_CONTACT_HISTORY를 같은 트랜잭션으로 저장한다.
- History 실패 시 전체 Rollback한다.
- Idempotency Key를 사용한다.
- 실제 DELETE를 사용하지 않는다.
- BusinessException과 SystemException을 구분한다.

산출물:
Request/Response DTO
Command
Handler
Facade
Service
Rule
DAO
Mapper Interface
Mapper XML
JUnit 테스트
```

### 113.4 AI 생성결과 검토

```
ServiceId가 정확한가?

패키지가 표준인가?

Handler가 Mapper를 호출하지 않는가?

사용자 ID를 Request에서 받지 않는가?

SQL에 #{ } Binding을 사용하는가?

Version 조건이 있는가?

영향 건수를 확인하는가?

History가 같은 트랜잭션인가?

오류코드가 표준인가?

테스트가 오류 흐름을 포함하는가?
```

### 113.5 AI가 잘하는 일

```
프로그램 골격 생성

DTO 생성

반복 Mapper 코드 생성

테스트 초안 생성

설계서 표 변환

추적성 대장 초안

명명규칙 검사

누락항목 점검
```

### 113.6 사람이 판단해야 할 일

```
업무 경계

데이터 소유권

상태전이

권한

중복기준

트랜잭션 경계

보상처리

Timeout

개인정보

운영 위험

최종 승인
```

### 제113장 요약

```
AI는 설계정보가 구체적일수록 정확한 코드를 만든다.

화면과 테이블만 전달하지 말고
업무코드·ServiceId·Rule·권한·Timeout·금지사항을 함께 준다.

AI 생성코드는 반드시 Architecture Gate와 테스트로 검증한다.
```

## 3. 목표 아키텍처

```
[업무 요구사항]
       │
       ▼
[화면·테이블 분석]
       │
       ▼
[업무코드·도메인 확정]
       │
       ▼
[식별자 발급]
 화면 ID
 이벤트 ID
 ServiceId
 거래코드
 권한코드
       │
       ▼
[추적성 매트릭스]
       │
       ▼
[DTO·Rule 설계]
       │
       ▼
[TCF 구현]
 Handler
 Facade
 Service
 Rule
 DAO
 Mapper
       │
       ▼
[DB]
 Master
 History
 Index
       │
       ▼
[자동 테스트]
 Unit
 Mapper
 Transaction
 Security
 Timeout
       │
       ▼
[OM 등록]
 Catalog
 Timeout
 Authority
 Control
 Metric
       │
       ▼
[CI/CD]
 Build
 WAR
 Deploy
 Smoke Test
       │
       ▼
[운영]
 Trace
 SQL
 Alert
 Runbook
```

## 4. 표준 형식 종합

### 4.1 기능 식별자

| 항목 | 값 |
| --- | --- |
| 업무코드 | CT |
| 도메인 | Contact |
| 화면 ID | CT-CNT-0001 |
| 목록 이벤트 | CT-CNT-0001-E01 |
| 목록 ServiceId | CT.Contact.selectList |
| 목록 거래코드 | CT-INQ-0001 |
| 목록 권한 | CT_CONTACT_VIEW |
| 등록 ServiceId | CT.Contact.create |
| 등록 거래코드 | CT-REG-0001 |
| 등록 권한 | CT_CONTACT_CREATE |

### 4.2 프로그램 식별자

| 계층 | 프로그램 |
| --- | --- |
| Handler | CtContactHandler |
| Facade | CtContactFacade |
| Service | CtContactService |
| Rule | CtContactRule |
| DAO | CtContactDao |
| Mapper | CtContactMapper |
| IC Client | IcCustomerClient |
| Mapper XML | CtContactMapper.xml |

### 4.3 데이터 식별자

| 유형 | 이름 |
| --- | --- |
| Master | CT_CONTACT_MASTER |
| History | CT_CONTACT_HISTORY |
| 목록 SQL | CT-CNT-SEL-001 |
| 상세 SQL | CT-CNT-SEL-003 |
| 등록 SQL | CT-CNT-INS-001 |
| 변경 SQL | CT-CNT-UPD-001 |
| 사용중지 SQL | CT-CNT-UPD-002 |

## 5. 구성요소 및 속성

| 구성요소 | 주요 속성 |
| --- | --- |
| 화면 | ID·이벤트·입력항목 |
| Service Catalog | ServiceId·Timeout·권한 |
| Handler | ServiceId 분기 |
| Facade | 트랜잭션 |
| Service | 업무 흐름 |
| Rule | 업무 검증 |
| DAO | 데이터 접근 |
| Mapper | SQL 실행 |
| Master | 현재 상태 |
| History | 변경 이력 |
| Client | 타 업무 호출 |
| Test | 정상·오류·Rollback |
| OM | 통제·관측 |
| CI/CD | 자동검증·배포 |

## 6. 책임 경계와 RACI

| 활동 | 업무팀 | AA | UI | DEV | DA·DBA | FW | QA | OM·OPS |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 요구사항 | A/R | C | C | C | I | I | C | I |
| 업무 Rule | A | C | C | R/C | C | I | C | I |
| 화면 ID | C | C | A/R | C | I | I | C | I |
| ServiceId | C | A | C | R | I | C | C | C |
| DB 설계 | C | C | I | C | A/R | I | C | I |
| 프로그램 구현 | I | C | C | A/R | C | C | C | I |
| TCF 공통기능 | I | C | I | C | I | A/R | C | C |
| 테스트 | C | C | C | R | C | C | A/R | I |
| OM 등록 | C | C | I | R/C | I | C | C | A |
| 배포 | I | C | I | C | C | C | C | A/R |
| 운영 인수 | C | C | I | C | C | C | C | A/R |

## 7. 정상 처리 흐름

### 7.1 등록 거래

```
1. 사용자가 상담정보 입력

2. 화면이 CT.Contact.create 호출

3. JWT 인증

4. STF 권한·거래통제·Timeout 확인

5. Handler가 Create Request 변환

6. Facade가 트랜잭션 시작

7. Service가 고객정보 조회

8. Rule이 고객·날짜·입력 검증

9. 상담 ID 생성

10. Master INSERT

11. History INSERT

12. Commit

13. ETF 거래로그 종료

14. 상담 ID와 Version 응답
```

### 7.2 변경 거래

```
1. 상담 상세조회

2. 화면이 Version 보관

3. 사용자가 내용 변경

4. CT.Contact.update 호출

5. 현재 상담정보 조회

6. 상태·지점 권한 확인

7. Version 조건 Update

8. 영향 건수 1 확인

9. 변경 전후 이력 저장

10. Commit

11. 신규 Version 응답
```

## 8. 오류·Timeout·장애 흐름

### 8.1 Validation 오류

```
제목 100자 초과
→ DTO Validation 실패
→ Handler 업무 실행 안 함
→ 업무 오류 응답
```

### 8.2 고객 연계 오류

```
IC.Customer.selectBasic Timeout
→ CT 등록 중단
→ DB Transaction 시작 전이면 DB 영향 없음
→ 외부연계 오류 응답
```

### 8.3 History 저장 실패

```
Master INSERT 성공
→ History INSERT 실패
→ 전체 Rollback
→ 시스템 오류
```

### 8.4 Version 충돌

```
Update 영향 건수 0
→ 동시성 업무 오류
→ 사용자가 재조회
```

### 8.5 등록 Timeout

```
응답 Timeout
→ Idempotency 상태 조회
→ 동일 Key 결과 확인
→ 무조건 신규 등록 금지
```

## 9. 정상 예시

```
화면
CT-CNT-0001

이벤트
CT-CNT-0001-E03

ServiceId
CT.Contact.create

사용자
user01

지점
001234

고객
CUST000001

권한
CT_CONTACT_CREATE

결과
Master 1건
History 1건
Version 1

거래상태
SUCCESS

처리시간
320ms
```

## 10. 금지 예시

### 10.1 새로운 Controller 생성

```
@PostMapping("/contact/create")
```

공통 TCF Endpoint와 ServiceId 구조를 우회합니다.

### 10.2 Handler에서 SQL 실행

```
handler -> mapper.insertContact()
```

### 10.3 화면 사용자 ID 신뢰

```
command.setCreateUserId(
    request.userId()
);
```

### 10.4 타 업무 DB 직접 조회

```
SELECT *
FROM IC_CUSTOMER_MASTER
```

### 10.5 실제 DELETE

```
DELETE FROM CT_CONTACT_MASTER
```

감사와 이력 요구를 무시합니다.

### 10.6 Version 없는 변경

```
WHERE CONTACT_ID = #{contactId}
```

### 10.7 등록 후 History 별도 비동기 저장

감사상 필수 이력이 유실될 수 있습니다.

### 10.8 테스트 없이 OM 등록

기능 검증 전 운영에 노출합니다.

## 11. 연계 규칙

```
CT 화면
→ CT ServiceId

CT Service
→ IC Customer Client

CT Service
→ CT DAO

CT DAO
→ CT Mapper

CT Mapper
→ CT 소유 테이블

CT Service Catalog
→ OM

CT Metric
→ OM Dashboard

CT Release
→ CI/CD
```

타 업무 연계 시 유지항목:

```
GUID

Parent TraceId

Child TraceId

원 사용자

호출 ServiceId

대상 ServiceId
```

## 12. 데이터 및 상태관리

### 12.1 상담 상태

```
ACTIVE
→ INACTIVE
```

재활성화 요구가 추가되면 별도 ServiceId를 설계합니다.

```
CT.Contact.activate
```

### 12.2 Version 상태

```
등록
Version 1

변경
Version 2

사용중지
Version 3
```

### 12.3 이력 유형

```
CREATE

UPDATE

DEACTIVATE
```

### 12.4 Idempotency 상태

```
PROCESSING
SUCCESS
FAIL
TIMEOUT
UNKNOWN
```

## 13. 성능·용량·확장성

| 영역 | 기준 |
| --- | --- |
| 목록 조회기간 | 최대 3개월 |
| 페이지 크기 | 최대 100 |
| 목록 상담내용 | 미포함 |
| 상세 내용 | 단건 조회 |
| Count SQL | 조건 일치 |
| Index | 고객번호·상태·상담일시 |
| 외부 Client | 700ms 이내 |
| 조회 Timeout | 3초 |
| 변경 Timeout | 5초 |
| History | 보관기간 적용 |
| 대량 Export | Batch Job 분리 |
| Cache | 상담 원문 Cache 비권장 |

## 14. 보안·개인정보·감사

```
상담내용은 민감한 업무정보로 취급한다.

상세조회 권한을 검증한다.

비공개 상담은 지점범위를 적용한다.

고객번호와 상담내용을 로그에 원문 저장하지 않는다.

등록·변경·사용중지를 감사한다.

대량 Export는 별도 승인과 파일보관정책을 적용한다.

화면 사용자 ID보다 인증 Context를 신뢰한다.
```

## 15. 운영·모니터링·장애 대응

운영자는 다음을 확인할 수 있어야 합니다.

```
CT ServiceId별 호출량

목록 p95

등록 실패율

Version 충돌 건수

IC 고객조회 Timeout

CT DB Pool Pending

CT-CNT-SEL-001 Slow SQL

상담 등록 Rollback

History 저장 실패
```

## 16. 자동검증 및 품질 Gate

| Gate | 검증 |
| --- | --- |
| 화면 | ID·이벤트 등록 |
| ServiceId | 형식·중복·Handler |
| 거래코드 | Registry 등록 |
| 권한 | ServiceId 연결 |
| 패키지 | CT·Contact·계층 |
| Handler | Mapper 참조 금지 |
| Rule | DAO 참조 금지 |
| Mapper | Interface·XML 일치 |
| SQL | SELECT * 금지 |
| Update | Version 조건 |
| 사용자 | Request userId 사용 금지 |
| 이력 | 변경 거래 History |
| 테스트 | 정상·오류·Rollback |
| OM | Timeout·담당자 |
| 배포 | Smoke Test·Rollback |

## 17. 종합 테스트 시나리오

| ID | 구분 | 시나리오 |
| --- | --- | --- |
| E2E-001 | 조회 | 정상 목록조회 |
| E2E-002 | 조회 | 빈 결과 |
| E2E-003 | 조회 | 최대기간 초과 |
| E2E-004 | 조회 | 비공개 타 지점 차단 |
| E2E-005 | 상세 | 정상 상세조회 |
| E2E-006 | 상세 | 없는 상담 ID |
| E2E-007 | 등록 | 정상 등록 |
| E2E-008 | 등록 | 고객 미존재 |
| E2E-009 | 등록 | 미래 상담일시 |
| E2E-010 | 등록 | 동일 Idempotency 재요청 |
| E2E-011 | 등록 | History 실패 Rollback |
| E2E-012 | 변경 | 정상 변경 |
| E2E-013 | 변경 | Version 충돌 |
| E2E-014 | 변경 | 타 지점 변경 차단 |
| E2E-015 | 중지 | 정상 사용중지 |
| E2E-016 | 중지 | 이미 중지된 자료 |
| E2E-017 | 인증 | JWT 없음 |
| E2E-018 | 인증 | JWT 위조 |
| E2E-019 | 권한 | 기능권한 없음 |
| E2E-020 | 통제 | ServiceId SUSPENDED |
| E2E-021 | Timeout | SQL Timeout |
| E2E-022 | 연계 | IC Timeout |
| E2E-023 | 추적 | TraceId로 SQL 조회 |
| E2E-024 | 배포 | Smoke Test |
| E2E-025 | 복구 | 이전 WAR Rollback |

## 18. 제12부 체크리스트

### 18.1 요구사항

| 점검 항목 | 확인 |
| --- | --- |
| 화면 항목을 분석했는가? | □ |
| 버튼별 서버 호출 여부를 구분했는가? | □ |
| 업무규칙을 질문으로 정리했는가? | □ |
| 데이터 소유 업무를 확정했는가? | □ |
| 개인정보 여부를 확인했는가? | □ |
| 예상 건수와 성능을 확인했는가? | □ |

### 18.2 식별자·설계

| 점검 항목 | 확인 |
| --- | --- |
| 요구사항 ID가 있는가? | □ |
| 화면 ID가 발급되었는가? | □ |
| 이벤트 ID가 있는가? | □ |
| ServiceId가 기능별로 분리되었는가? | □ |
| 거래코드가 있는가? | □ |
| 권한코드가 있는가? | □ |
| 추적성 매트릭스가 있는가? | □ |

### 18.3 구현

| 점검 항목 | 확인 |
| --- | --- |
| Package가 표준인가? | □ |
| Handler가 Facade만 호출하는가? | □ |
| Facade가 트랜잭션 경계인가? | □ |
| Service가 업무 흐름을 담당하는가? | □ |
| Rule이 업무 검증을 담당하는가? | □ |
| DAO·Mapper 책임이 분리되는가? | □ |
| Request와 DB DTO가 분리되는가? | □ |

### 18.4 데이터

| 점검 항목 | 확인 |
| --- | --- |
| Master와 History가 정의되었는가? | □ |
| 목록과 상세 SQL이 분리되는가? | □ |
| 목록에 대용량 CLOB을 제외했는가? | □ |
| Version 조건이 있는가? | □ |
| 상태조건이 있는가? | □ |
| Index를 검토했는가? | □ |
| 영향 건수를 확인하는가? | □ |

### 18.5 보안·안정성

| 점검 항목 | 확인 |
| --- | --- |
| 사용자정보를 Context에서 가져오는가? | □ |
| 기능권한을 STF에서 확인하는가? | □ |
| 데이터권한을 적용하는가? | □ |
| Idempotency를 적용하는가? | □ |
| Timeout 계층이 정합적인가? | □ |
| 오류코드가 표준인가? | □ |
| 민감정보가 로그에 노출되지 않는가? | □ |

### 18.6 테스트·운영

| 점검 항목 | 확인 |
| --- | --- |
| Rule 테스트가 있는가? | □ |
| Mapper 테스트가 있는가? | □ |
| 거래 테스트가 있는가? | □ |
| Rollback 테스트가 있는가? | □ |
| 동시성 테스트가 있는가? | □ |
| OM Catalog가 등록되었는가? | □ |
| Metric과 Runbook이 있는가? | □ |
| Smoke Test와 Rollback 절차가 있는가? | □ |

## 19. 변경·호환성·폐기 관리

### 19.1 신규 필드 추가

예:

```
상담 중요도
CONTACT_PRIORITY_CD
```

변경 대상:

```
화면

Request·Response

Command·Data

DB Column

Mapper SQL

Validation

테스트

파일 Export

추적성 문서
```

### 19.2 신규 기능 추가

상담 재활성화 요구:

```
CT.Contact.activate
```

기존 deactivate에 actionType=ACTIVATE를 추가하지 않습니다.

신규 권한과 거래코드를 부여합니다.

### 19.3 화면 변경

화면 표시명 변경은 화면 ID를 유지할 수 있습니다.

기능 의미가 완전히 달라지면 신규 화면 ID를 검토합니다.

### 19.4 테이블 변경

컬럼 Rename은 구·신 코드 호환성을 고려해 단계적으로 진행합니다.

```
신규 컬럼 추가

병행 기록

데이터 이관

신규 코드 전환

기존 컬럼 폐기
```

### 19.5 ServiceId 폐기

```
ACTIVE
→ DEPRECATED
→ 호출자 전환
→ DISABLED
→ 코드·OM·테스트 제거
```

## 20. 시사점

### 20.1 핵심 아키텍처 판단

제12부의 핵심은 다음과 같습니다.

```
화면과 테이블을 받는다
→ 바로 CRUD 코드를 만든다
```

가 아닙니다.

```
화면과 테이블을 받는다
→ 업무경계와 Rule을 정한다
→ 식별자와 추적성을 만든다
→ 계층별 책임으로 구현한다
→ 테스트와 OM까지 완성한다
```

입니다.

### 20.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 화면만 보고 코드 작성 | 업무규칙 누락 |
| ServiceId 범용화 | 통제·권한 불가 |
| 사용자 ID Request 사용 | 위변조 |
| 타 업무 DB 직접 조회 | 소유권 붕괴 |
| Request DTO 전체 재사용 | 정보노출·결합 |
| 물리 삭제 | 감사·복구 불가 |
| Version 미적용 | 변경 덮어쓰기 |
| History 누락 | 감사 불가 |
| 목록 CLOB 조회 | 성능 저하 |
| 테스트 정상 흐름만 작성 | 오류 장애 |
| OM 등록 누락 | 운영 통제 불가 |
| AI 코드 무검증 | 표준 위반 확산 |

### 20.3 우선 보완 과제

```
1. 화면·테이블 분석 질문표 표준화

2. ServiceId 분해 가이드

3. CRUD 설계 템플릿

4. Package·DTO 자동 생성 템플릿

5. 공통 Handler 골격

6. Version·History 표준

7. 권한·Context 검증 공통화

8. Mapper·SQL 품질 Gate

9. 거래 테스트 Base 구축

10. OM 등록 자동검증

11. 배포 Smoke Test 자동화

12. AI 개발 프롬프트 표준화
```

### 20.4 중장기 발전 방향

```
수작업 설계서
→ 화면·테이블 Metadata 기반 초안 생성

수동 식별자 발급
→ Registry 자동 발급

개발자별 코드 작성
→ TCF 표준 Code Generator

수동 추적성
→ Source·OM·DB 자동 연결

수동 테스트
→ ServiceId별 테스트 자동 생성

수동 OM 등록
→ Source Scan 기반 등록 후보

AI 단순 코드 생성
→ 설계·검증·테스트 통합 개발지원
```

자동 생성은 업무 판단을 대신하는 기능이 아니라 표준 반복작업을 줄이는 도구로 사용해야 합니다.

## 21. 마무리말

제12부에서 가장 중요하게 기억해야 할 전체 개발절차는 다음과 같습니다.

```
화면과 테이블 수령

→ 업무 질문

→ 업무코드와 도메인

→ 화면 ID와 이벤트 ID

→ ServiceId와 거래코드

→ 추적성 매트릭스

→ Request·Response DTO

→ Handler

→ Facade

→ Service

→ Rule

→ DAO·Mapper·SQL

→ 트랜잭션·이력·동시성

→ 인증·권한·Timeout

→ 테스트

→ OM 등록

→ Build·배포

→ Smoke Test

→ 운영 인수
```

초보 개발자가 새로운 화면 개발을 시작하기 전에 마지막으로 확인해야 할 질문은 다음과 같습니다.

```
이 기능은 어느 업무가 소유하는가?

화면 버튼 중 서버 호출이 필요한 것은 무엇인가?

ServiceId를 기능별로 분리했는가?

거래코드와 권한이 연결되었는가?

화면 Request에 사용자정보를 넣고 있지는 않은가?

목록과 상세 DTO가 분리되어 있는가?

업무 Rule은 어느 계층에 있는가?

다른 업무의 테이블을 직접 조회하고 있지는 않은가?

변경에 Version 조건이 있는가?

Master와 History가 같은 트랜잭션인가?

Timeout 후 실제 반영 여부를 확인할 수 있는가?

정상·오류·Rollback 테스트가 있는가?

OM에서 ServiceId와 SQL을 추적할 수 있는가?

운영 배포 후 Smoke Test가 가능한가?

문서와 실제 Source가 동일한가?
```

이 질문에 답할 수 있다면 화면과 테이블 정보만 받은 초보 개발자도 NSIGHT TCF의 표준 구조에 맞춰 요구사항 분석부터 구현·테스트·배포·운영까지 하나의 완전한 업무기능을 완성할 수 있습니다.

