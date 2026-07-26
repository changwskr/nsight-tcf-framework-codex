
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제2부. 개발표준과 거래 식별 이해하기

## 1. 도입 전 안내말

제1부에서는 사용자의 요청이 다음 경로로 처리된다는 것을 배웠습니다.

```
화면
→ Gateway
→ 업무 WAR
→ OnlineTransactionController
→ TCF
→ STF
→ Dispatcher
→ Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ ETF
→ 표준 응답
```

제2부에서는 이 처리 흐름에 필요한 이름과 형식을 배웁니다.

처음 개발 프로젝트에 참여하면 다음과 같은 질문을 받게 됩니다.

```
이 화면의 화면 ID는 무엇인가요?

조회 버튼의 이벤트 ID는 무엇인가요?

어느 업무 WAR에서 개발하나요?

ServiceId는 어떻게 정하나요?

거래코드는 ServiceId와 무엇이 다른가요?

요청 JSON의 Header에는 무엇을 넣나요?

신규 ServiceId는 OM에 등록했나요?
```

이 질문들은 프로그램을 작성하기 전에 반드시 답해야 합니다.

표준 없이 개발자가 각자 이름을 정하면 다음과 같은 문제가 생깁니다.

```
화면에서는 CustomerSearch라고 부름

ServiceId는 SV.Customer.getInfo

Java 메서드는 findCust()

Mapper ID는 selectUser

테이블은 SV_CUST_BASE

운영 화면에는 고객통합조회로 등록
```

모든 프로그램이 같은 기능을 처리하지만 이름이 서로 다릅니다.

이런 구조에서는 문제가 발생했을 때 다음 내용을 연결하기 어렵습니다.

```
어느 화면에서 호출했는가?
어느 ServiceId가 실행되었는가?
어느 Handler가 처리했는가?
어느 SQL이 실행되었는가?
어느 테이블에 접근했는가?
운영자가 어느 거래를 차단해야 하는가?
```

따라서 NSIGHT TCF에서는 다음 식별자를 하나의 연결 구조로 관리합니다.

```
업무코드
→ 화면 ID
→ 이벤트 ID
→ ServiceId
→ 거래코드
→ Handler
→ 프로그램
→ SQL ID
→ DB 객체
→ OM 기준정보
→ 거래로그
```

표준 개발·운영 가이드에서는 표준 전문을 StandardRequest와 StandardResponse로 구분하고, ServiceId는 Handler를 선택하는 실행 식별자로, 거래코드는 감사·통제·통계 식별자로 정의합니다.

## 2. 제2부 개요

### 2.1 목적

제2부의 목적은 초보 개발자가 하나의 화면 기능을 다음과 같은 거래 정의로 바꿀 수 있도록 하는 것입니다.

```
요구사항
“고객번호로 고객 요약정보를 조회한다.”

       ↓

업무코드
SV

       ↓

화면 ID
SV-CUS-0001

       ↓

이벤트 ID
SV-CUS-0001-E01

       ↓

ServiceId
SV.Customer.selectSummary

       ↓

거래코드
SV-INQ-0001

       ↓

표준 요청
Header + Body

       ↓

표준 응답
Header + Result + Body + Error
```

제2부를 학습한 뒤에는 다음 작업을 수행할 수 있어야 합니다.

- 기능이 속한 업무코드를 선택한다.
- 화면과 이벤트를 식별한다.
- ServiceId를 표준 형식으로 작성한다.
- 거래코드를 ServiceId와 구분하여 작성한다.
- 표준 요청·응답 JSON을 작성한다.
- ServiceId와 Handler를 연결한다.
- OM 등록에 필요한 정보를 정리한다.
- 화면부터 SQL까지 추적관계를 설명한다.

### 2.2 적용범위

| 구분 | 학습 내용 |
| --- | --- |
| 개발표준 | 표준이 필요한 이유 |
| 업무 식별 | 업무코드, WAR, Context Path |
| 화면 식별 | 화면 ID, 이벤트 ID, 기능 ID |
| 거래 식별 | ServiceId와 거래코드 |
| 표준 전문 | Header, Body, Result, Error |
| 추적 식별 | GUID, TraceId, RequestId |
| 운영 등록 | OM Service Catalog, Timeout, 거래통제 |
| 추적성 | 화면부터 SQL·DB까지의 연결 |

### 2.3 대상 독자

- 제1부의 TCF 처리 흐름을 이해한 초보 개발자
- 화면 요구사항을 서버 거래로 변환해야 하는 개발자
- ServiceId와 거래코드가 혼동되는 개발자
- 표준 JSON 전문을 처음 작성하는 개발자
- 화면·프로그램·SQL 설계서를 작성해야 하는 개발자
- 신규 거래를 OM에 등록해야 하는 개발자

### 2.4 선행조건

다음 내용을 이해하고 있어야 합니다.

```
TCF는 모든 온라인 거래의 공통 실행 엔진이다.

Dispatcher는 ServiceId로 Handler를 찾는다.

업무 프로그램은
Handler → Facade → Service → Rule → DAO → Mapper
구조를 따른다.

ETF는 표준 응답을 생성한다.
```

### 2.5 주요 용어

| 용어 | 쉬운 설명 |
| --- | --- |
| 개발표준 | 모든 개발자가 같은 방식으로 개발하기 위한 규칙 |
| 업무코드 | 어느 업무 영역인지 나타내는 코드 |
| 화면 ID | 화면을 구분하는 식별자 |
| 이벤트 ID | 화면에서 발생하는 동작을 구분하는 식별자 |
| 기능 ID | 사용자가 수행하는 논리 기능을 구분하는 식별자 |
| ServiceId | 실행할 Handler와 업무 기능을 찾는 이름 |
| 거래코드 | 거래통제·감사·통계에 사용하는 코드 |
| 표준 전문 | 시스템 간 요청과 응답의 공통 JSON 구조 |
| OM Catalog | 운영 대상 ServiceId의 기준정보 |
| Processing Type | 조회·등록·변경·삭제 등 처리 유형 |
| Idempotency Key | 같은 변경 요청의 중복 실행을 방지하는 키 |
| Contract Version | 요청·응답 계약의 버전 |

## 제5장. 개발표준은 왜 필요한가요?

### 5.1 프로그램이 동작하면 끝이 아닌 이유

초보 개발자는 프로그램이 정상적으로 동작하면 개발이 끝났다고 생각하기 쉽습니다.

하지만 기업 시스템에서는 다음 조건도 만족해야 합니다.

```
다른 개발자가 이해할 수 있는가?

운영자가 거래를 찾을 수 있는가?

장애 발생 시 원인을 추적할 수 있는가?

보안 담당자가 권한을 검증할 수 있는가?

테스트팀이 어떤 기능을 시험해야 하는지 알 수 있는가?

변경 시 영향받는 화면과 SQL을 찾을 수 있는가?
```

프로그램이 동작하는 것은 최소 조건입니다.

```
좋은 기업 시스템

= 기능 동작
+ 표준 준수
+ 보안
+ 운영 가능성
+ 장애 추적성
+ 변경 가능성
```

### 5.2 교통법규 비유

개발표준은 교통법규와 비슷합니다.

자동차가 움직인다고 해서 아무 방향으로 운전해도 되는 것은 아닙니다.

```
신호등
차선
속도 제한
도로 표지판
차량 번호
교통 기록
```

이 기준이 있어야 많은 차량이 안전하게 움직일 수 있습니다.

개발도 마찬가지입니다.

```
업무코드
ServiceId
거래코드
패키지 구조
계층 구조
오류코드
로그 형식
표준 요청·응답
```

이 기준이 있어야 많은 개발자가 만든 프로그램이 하나의 시스템처럼 동작합니다.

### 5.3 NSIGHT 개발표준의 주요 영역

| 영역 | 대표 표준 |
| --- | --- |
| 업무 | 업무코드, 업무세구분코드 |
| 화면 | 화면 ID, 이벤트 ID, 메뉴 ID |
| 거래 | ServiceId, 거래코드 |
| 프로그램 | Handler, Facade, Service, Rule, DAO, Mapper |
| Java | 클래스명, 메서드명, DTO명 |
| 패키지 | 업무코드 → 도메인 → 계층 |
| 데이터 | 테이블, 컬럼, 인덱스, 제약조건 |
| SQL | Mapper Namespace, SQL ID |
| 오류 | 공통·프레임워크·업무 오류코드 |
| 로그 | GUID, TraceId, ServiceId, SQL ID |
| 운영 | OM Catalog, 거래통제, Timeout |
| 테스트 | 화면·거래·프로그램·SQL 테스트 연결 |

### 5.4 표준이 없을 때 생기는 문제

#### 사례 1: 이름이 서로 다르다

```
화면명: 고객 종합 조회
ServiceId: SV.Customer.get
메서드명: searchCustData()
SQL ID: selectUserInfo
테이블: SV_CUST_SUM
```

같은 고객 기능이지만 Customer, Cust, User가 혼용됩니다.

#### 사례 2: 계층을 건너뛴다

```
Handler
→ Mapper
```

처음에는 코드가 짧지만 업무 규칙, 트랜잭션, 테스트 책임이 불명확해집니다.

#### 사례 3: 운영 등록이 없다

```
Handler는 존재함
OM Service Catalog에는 없음
Timeout 정책 없음
거래통제 정책 없음
```

개발환경에서는 동작할 수 있지만 운영 통제가 불가능합니다.

#### 사례 4: 로그를 연결할 수 없다

```
화면 오류 발생
→ ServiceId 기록 없음
→ TraceId 없음
→ 어느 SQL인지 확인 불가
```

### 5.5 표준 적용의 핵심 원칙

```
하나의 업무 용어를
화면·ServiceId·Java·SQL·DB에서
가능한 한 같은 의미로 사용한다.
```

예:

| 구분 | 권장 표현 |
| --- | --- |
| 도메인 | Customer |
| ServiceId | SV.Customer.selectSummary |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService |
| DAO | SvCustomerDao |
| Mapper | SvCustomerMapper |
| SQL ID | selectCustomerSummary |
| 테이블 | SV_CUSTOMER_SUMMARY |

### 5.6 정상 예시

```
화면
SV-CUS-0001 고객 요약 조회

이벤트
SV-CUS-0001-E01 조회 버튼 클릭

ServiceId
SV.Customer.selectSummary

거래코드
SV-INQ-0001

Handler
SvCustomerHandler

SQL ID
SvCustomerMapper.selectCustomerSummary

테이블
SV_CUSTOMER_SUMMARY
```

### 5.7 금지 예시

```
화면
screen01

ServiceId
getData

거래코드
001

Handler
CommonHandler

Service
TestService

SQL ID
query1

테이블
TMP_TABLE
```

금지 이유:

- 업무 영역을 식별할 수 없음
- 기능 의미가 없음
- 운영 추적이 어려움
- 중복 이름이 발생하기 쉬움
- 변경 책임자를 찾기 어려움

### 제5장 요약

```
개발표준은 코드를 예쁘게 만드는 규칙이 아니다.

설계·코드·운영·테스트를
서로 연결하기 위한 공통 언어다.
```

## 제6장. 업무코드와 업무 경계

### 6.1 업무코드란 무엇인가요?

업무코드는 기능이 어느 업무 영역에 속하는지를 나타냅니다.

예:

| 코드 | 업무 영역 예 |
| --- | --- |
| CC | 공통 |
| IC | 통합 고객 |
| PC | 개인 고객 |
| BC | 기업 고객 |
| SV | Single View |
| PD | 상품 |
| CM | 캠페인 |
| EB | EBM |
| EP | 이벤트 처리 |
| SS | 영업 지원 |
| MG | 관리·메시지 |
| OM | 운영관리 |

업무코드는 단순 분류값이 아닙니다.

다음 항목을 연결하는 기준입니다.

```
업무코드
→ 업무 WAR
→ Context Path
→ Java 패키지
→ ServiceId
→ 거래코드
→ DB 객체
→ 로그
→ 담당 조직
```

### 6.2 업무코드와 WAR

예를 들어 업무코드가 SV이면 다음과 같이 연결할 수 있습니다.

```
업무코드
SV

업무 WAR
sv-service.war

Context Path
/sv

Endpoint
POST /sv/online

Java 패키지
com.nh.nsight.marketing.sv

DB Prefix
SV_
```

| 구분 | 값 |
| --- | --- |
| 업무코드 | SV |
| WAR | sv-service |
| Context | /sv |
| ServiceId Prefix | SV. |
| 거래코드 Prefix | SV- |
| 패키지 | .sv. |
| DB 객체 | SV_ |

### 6.3 업무코드가 일치해야 하는 이유

다음 요청을 생각해 봅시다.

```
URL: /sv/online
Header businessCode: CM
ServiceId: IC.Customer.selectSummary
```

세 값이 서로 다릅니다.

```
URL은 SV 업무
Header는 CM 업무
ServiceId는 IC 업무
```

이 요청을 허용하면 다음 문제가 생깁니다.

- 잘못된 WAR에서 Handler를 찾음
- 권한과 거래통제 기준이 달라짐
- 로그의 업무 분류가 잘못됨
- 운영자가 잘못된 거래를 차단할 수 있음
- 고의적인 Header 변조일 수 있음
따라서 STF는 업무코드 정합성을 검사해야 합니다.

```
URL 업무코드
= Header businessCode
= ServiceId 업무코드
= 대상 WAR 업무코드
```

### 6.4 업무세구분코드

업무가 크면 업무코드 아래에 업무세구분을 둘 수 있습니다.

예:

```
SV
├─ CUS : 고객
├─ PRD : 상품
├─ ACT : 활동
└─ STA : 통계
```

화면 ID 예:

```
SV-CUS-0001
SV-PRD-0001
SV-ACT-0001
```

업무세구분코드는 화면과 산출물을 분류하는 데 유용합니다.

그러나 ServiceId의 도메인명과 반드시 같은 형식일 필요는 없습니다.

```
화면 ID
SV-CUS-0001

ServiceId
SV.Customer.selectSummary
```

CUS는 관리용 코드이고 Customer는 개발자가 이해할 수 있는 도메인명입니다.

### 6.5 업무코드 선택 방법

다음 질문을 순서대로 확인합니다.

```
1. 이 기능의 업무 책임자는 누구인가?

2. 어느 업무 데이터가 중심인가?

3. 어느 WAR가 해당 기능을 배포하는가?

4. 어느 도메인이 데이터를 변경할 권한을 가지는가?

5. 장애 발생 시 어느 조직이 대응하는가?
```

예:

```
고객 Single View 요약 조회
→ SV

캠페인 생성
→ CM

운영 거래중지 설정
→ OM
```

### 6.6 다른 업무의 데이터를 조회하는 경우

화면이 여러 업무의 데이터를 함께 보여준다고 해서 아무 업무코드나 선택하면 안 됩니다.

예:

```
SV 고객 종합 화면
├─ 고객 기본정보
├─ 보유상품
├─ 상담이력
└─ 캠페인 반응
```

화면을 소유하는 업무가 SV라면 대표 거래는 SV 업무에서 시작할 수 있습니다.

그러나 다른 업무의 데이터 변경은 소유 업무를 통해 처리해야 합니다.

```
조회 조합
= SV Facade가 공개 계약을 통해 조합 가능

다른 업무 데이터 변경
= 반드시 데이터 소유 업무의 ServiceId 호출
```

다른 업무 WAR의 DAO나 테이블을 임의로 직접 변경해서는 안 됩니다.

### 6.7 정상 예시

```
업무 기능
고객 요약 조회

업무코드
SV

WAR
sv-service

Context
/sv

ServiceId
SV.Customer.selectSummary

거래코드
SV-INQ-0001

패키지
com.nh.nsight.marketing.sv.customer
```

### 6.8 금지 예시

```
업무코드: SV
WAR: customer-service
Context: /customer
ServiceId: IC.Customer.selectSummary
거래코드: CM-INQ-0001
패키지: com.nh.nsight.common.customer
```

하나의 기능이 여러 업무 식별자로 표현되어 있으므로 금지합니다.

### 제6장 요약

```
업무코드는 단순한 두 자리 문자가 아니다.

WAR, Context, ServiceId, 거래코드,
패키지, DB, 로그, 책임 조직을
연결하는 최상위 식별자다.
```

## 제7장. ServiceId 설계하기

### 7.1 ServiceId란 무엇인가요?

ServiceId는 TCF가 실행할 업무 기능을 식별하는 이름입니다.

Dispatcher는 ServiceId를 이용해 Handler를 찾습니다.

```
SV.Customer.selectSummary
        ↓
TransactionDispatcher
        ↓
SvCustomerHandler
```

ServiceId는 다음 질문에 답합니다.

```
어느 업무에서
어느 도메인의
어떤 행위를 실행하는가?
```

### 7.2 표준 형식

권장 형식은 다음과 같습니다.

```
{업무코드}.{도메인}.{행위}
```

예:

```
SV.Customer.selectSummary
CM.Campaign.create
PD.Product.selectDetail
OM.ServiceCatalog.updateStatus
```

ServiceId와 거래코드의 표준 형식은 각각 {업무코드}.{도메인}.{행위}와 {업무코드}-{처리유형}-{일련번호}로 구분합니다.

### 7.3 구성요소

#### 업무코드

```
SV
CM
PD
OM
```

#### 도메인

```
Customer
Campaign
Product
ServiceCatalog
```

#### 행위

```
selectSummary
selectDetail
create
update
delete
approve
cancel
execute
download
```

### 7.4 ServiceId 해석하기

```
SV.Customer.selectSummary
```

| 구성 | 값 | 의미 |
| --- | --- | --- |
| 업무코드 | SV | Single View 업무 |
| 도메인 | Customer | 고객 영역 |
| 행위 | selectSummary | 요약정보 조회 |

```
CM.Campaign.approve
```

| 구성 | 값 | 의미 |
| --- | --- | --- |
| 업무코드 | CM | 캠페인 업무 |
| 도메인 | Campaign | 캠페인 영역 |
| 행위 | approve | 캠페인 승인 |

### 7.5 행위어 선택 기준

| 처리 | 권장 행위어 | 예 |
| --- | --- | --- |
| 단건 조회 | selectDetail | 상품 상세 조회 |
| 요약 조회 | selectSummary | 고객 요약 조회 |
| 목록 조회 | selectList | 캠페인 목록 조회 |
| 등록 | create | 캠페인 생성 |
| 변경 | update | 고객 연락처 변경 |
| 삭제 | delete | 임시정보 삭제 |
| 승인 | approve | 캠페인 승인 |
| 취소 | cancel | 예약 취소 |
| 실행 | execute | 캠페인 실행 |
| 다운로드 | download | 결과 파일 다운로드 |
| 검증 | validate | 조건 유효성 검증 |
| 재처리 | retry | 실패 메시지 재처리 |

### 7.6 피해야 할 행위어

다음 이름은 의미가 모호합니다.

```
get
set
process
handle
run
doWork
executeTask
manage
data
info
```

예:

```
금지
SV.Customer.get
SV.Customer.process
SV.Customer.doWork
```

수정:

```
권장
SV.Customer.selectSummary
SV.Customer.updateContact
SV.Customer.calculateGrade
```

### 7.7 조회와 변경을 분리해야 하는 이유

다음 ServiceId는 여러 행위를 포함하고 있습니다.

```
SV.Customer.manage
```

이 하나의 ServiceId에서 조회, 등록, 변경, 삭제를 모두 수행하면 다음 문제가 생깁니다.

- 권한을 세분화하기 어려움
- 거래통제가 어려움
- Timeout 기준이 달라짐
- 감사 대상 구분이 어려움
- 장애 통계가 모호해짐
- 테스트 범위가 커짐
따라서 다음과 같이 분리합니다.

```
SV.Customer.selectDetail
SV.Customer.create
SV.Customer.update
SV.Customer.delete
```

### 7.8 화면 하나와 ServiceId의 관계

화면 하나가 반드시 ServiceId 하나만 사용하는 것은 아닙니다.

```
고객 종합 화면
├─ 고객 요약 조회
├─ 상품 목록 조회
├─ 상담이력 조회
└─ 메모 등록
```

각 기능은 별도의 ServiceId를 사용할 수 있습니다.

```
SV.Customer.selectSummary
SV.Product.selectList
CT.Contact.selectHistory
SV.CustomerMemo.create
```

반대로 하나의 ServiceId를 여러 화면에서 재사용할 수도 있습니다.

```
SV.Customer.selectSummary

사용 화면
├─ 고객 종합 화면
├─ 캠페인 대상자 화면
└─ 상담 고객 팝업
```

### 7.9 ServiceId와 Handler

최신 권장 구조에서는 도메인 단위 Handler가 여러 ServiceId를 등록할 수 있습니다.

```
@Component
@RequiredArgsConstructor
public class SvCustomerHandler implements TransactionHandler {

    private final SvCustomerFacade customerFacade;

    @Override
    public Set<String> serviceIds() {
        return Set.of(
            "SV.Customer.selectSummary",
            "SV.Customer.selectDetail",
            "SV.Customer.updateContact"
        );
    }
}
```

Handler는 전달받은 ServiceId에 따라 Facade 메서드를 선택합니다.

```
@Override
public Object handle(
        StandardRequest<?> request,
        TransactionContext context) {

    return switch (context.getServiceId()) {
        case "SV.Customer.selectSummary" ->
            customerFacade.selectSummary(
                request.bodyAs(CustomerSummaryRequest.class),
                context
            );

        case "SV.Customer.selectDetail" ->
            customerFacade.selectDetail(
                request.bodyAs(CustomerDetailRequest.class),
                context
            );

        case "SV.Customer.updateContact" ->
            customerFacade.updateContact(
                request.bodyAs(CustomerContactUpdateRequest.class),
                context
            );

        default ->
            throw new ServiceNotFoundException(
                context.getServiceId()
            );
    };
}
```

### 7.10 ServiceId 중복

두 Handler가 같은 ServiceId를 등록하면 안 됩니다.

```
SvCustomerHandler
→ SV.Customer.selectSummary

CommonCustomerHandler
→ SV.Customer.selectSummary
```

이 경우 어떤 Handler를 실행해야 할지 알 수 없습니다.

권장 처리:

```
애플리케이션 기동 시
중복 ServiceId 발견
→ 서버 기동 실패
```

운영 중 임의의 Handler를 선택하는 것보다 기동 단계에서 문제를 발견하는 것이 안전합니다.

### 7.11 ServiceId 변경 위험

기존 ServiceId를 단순히 이름이 마음에 들지 않는다는 이유로 변경하면 안 됩니다.

```
기존
SV.Customer.selectSummary

변경
SV.Customer.getOverview
```

영향 범위:

```
화면 이벤트
Gateway Route
Handler Registry
OM Service Catalog
거래통제
Timeout
권한
감사로그
테스트
운영 대시보드
연계 시스템
```

ServiceId는 외부 계약과 운영 식별자의 성격을 가지므로 변경 전에 영향 분석이 필요합니다.

### 7.12 정상 예시

| 업무 기능 | ServiceId |
| --- | --- |
| 고객 요약 조회 | SV.Customer.selectSummary |
| 고객 상세 조회 | SV.Customer.selectDetail |
| 고객 연락처 변경 | SV.Customer.updateContact |
| 캠페인 생성 | CM.Campaign.create |
| 캠페인 승인 | CM.Campaign.approve |
| 상품 목록 조회 | PD.Product.selectList |
| 서비스 운영중지 | OM.ServiceCatalog.suspend |

### 7.13 금지 예시

| 금지 ServiceId | 문제 |
| --- | --- |
| customerSearch | 업무코드·도메인 구분 없음 |
| SV.getData | 도메인과 행위가 모호함 |
| SV.Customer.doWork | 실제 업무 의미 없음 |
| SV.CUST.001 | 사람이 이해하기 어려움 |
| SV.Customer.selectAndUpdate | 조회와 변경 책임 혼합 |
| COMMON.Customer.select | 소유 업무가 불분명 |
| SV.Customer.v2 | 행위가 아닌 버전이 이름에 포함 |

### 7.14 ServiceId 체크리스트

| 점검 질문 | 확인 |
| --- | --- |
| 업무코드가 올바른가? | □ |
| 실제 업무 도메인을 표현하는가? | □ |
| 행위가 명확한 동사인가? | □ |
| 조회와 변경이 분리되어 있는가? | □ |
| 기존 ServiceId와 중복되지 않는가? | □ |
| 담당 Handler가 정의되어 있는가? | □ |
| OM Catalog 등록 대상인가? | □ |
| Timeout 정책이 정의되어 있는가? | □ |
| 화면 이벤트와 연결되어 있는가? | □ |
| 테스트케이스와 연결되어 있는가? | □ |

### 제7장 요약

```
ServiceId
= 실행할 업무 기능을 찾는 논리적 주소

형식
= {업무코드}.{도메인}.{행위}

Dispatcher는 ServiceId로 Handler를 찾는다.
```

## 제8장. 거래코드 이해하기

### 8.1 거래코드는 무엇인가요?

거래코드는 거래를 운영 관점에서 분류하기 위한 코드입니다.

ServiceId가 프로그램 실행을 위한 이름이라면, 거래코드는 다음 목적으로 사용됩니다.

```
거래통제
감사
통계
권한
모니터링
업무 유형 분류
운영 보고
```

### 8.2 ServiceId와 거래코드 차이

| 구분 | ServiceId | 거래코드 |
| --- | --- | --- |
| 예 | SV.Customer.selectSummary | SV-INQ-0001 |
| 주목적 | Handler 라우팅 | 통제·감사·통계 |
| 사람이 읽기 | 업무 의미 중심 | 분류·관리 중심 |
| 사용 위치 | Dispatcher | STF, ETF, OM, 로그 |
| 변경 영향 | 프로그램 계약 | 운영 정책과 통계 |
| 필수 여부 | 온라인 업무 필수 | 프로젝트 표준에 따라 필수 |

쉽게 비유하면 다음과 같습니다.

```
ServiceId
= 어느 업무 창구로 갈 것인가

거래코드
= 이 업무가 어떤 종류의 거래인가
```

### 8.3 표준 형식

권장 형식:

```
{업무코드}-{처리유형}-{일련번호}
```

예:

```
SV-INQ-0001
SV-REG-0001
SV-UPD-0001
SV-DEL-0001
CM-APR-0001
CM-EXE-0001
```

### 8.4 처리유형 예시

| 코드 | 의미 | 대표 행위 |
| --- | --- | --- |
| INQ | 조회 | select |
| REG | 등록 | create |
| UPD | 변경 | update |
| DEL | 삭제 | delete |
| APR | 승인 | approve |
| CNL | 취소 | cancel |
| EXE | 실행 | execute |
| DWN | 다운로드 | download |
| UPL | 업로드 | upload |
| VAL | 검증 | validate |
| RPR | 재처리 | retry |

처리유형 코드는 프로젝트 표준 코드사전에서 관리해야 하며 개발자가 임의로 추가하지 않습니다.

### 8.5 거래코드 예시

| 업무 기능 | ServiceId | 거래코드 |
| --- | --- | --- |
| 고객 요약 조회 | SV.Customer.selectSummary | SV-INQ-0001 |
| 고객 상세 조회 | SV.Customer.selectDetail | SV-INQ-0002 |
| 연락처 변경 | SV.Customer.updateContact | SV-UPD-0001 |
| 캠페인 생성 | CM.Campaign.create | CM-REG-0001 |
| 캠페인 승인 | CM.Campaign.approve | CM-APR-0001 |
| 캠페인 실행 | CM.Campaign.execute | CM-EXE-0001 |
| 결과 다운로드 | CM.Campaign.downloadResult | CM-DWN-0001 |

### 8.6 ServiceId와 거래코드를 분리하는 이유

다음과 같은 운영 요구가 있을 수 있습니다.

```
모든 다운로드 거래를 일시 중지한다.

모든 변경 거래에 강화된 감사로그를 적용한다.

조회 거래와 변경 거래의 통계를 분리한다.

승인 거래는 특정 사용자만 허용한다.
```

거래코드에 처리유형이 포함되어 있으면 운영 정책을 분류하기 쉽습니다.

```
*-DWN-*
→ 다운로드 통제

*-UPD-*
→ 변경 거래 감사

*-APR-*
→ 승인 권한 강화
```

### 8.7 ServiceId 하나와 거래코드 하나

일반적으로 하나의 ServiceId에는 하나의 대표 거래코드를 연결하는 것이 이해하기 쉽습니다.

```
SV.Customer.selectSummary
↔ SV-INQ-0001
```

그러나 조직이나 채널에 따라 동일 ServiceId를 서로 다른 운영 분류로 관리해야 하는 특별한 경우가 있을 수 있습니다.

이 경우에도 개발자가 임의로 결정하지 않고 아키텍처팀과 OM 운영 기준을 따라야 합니다.

### 8.8 거래코드로 Handler를 찾으면 안 되는 이유

Dispatcher의 라우팅 키는 ServiceId입니다.

```
권장
ServiceId → Handler

금지
거래코드 → Handler
```

거래코드는 운영 분류용이므로 나중에 운영 정책이 변경될 수 있습니다.

거래코드 변경이 프로그램 라우팅 변경으로 이어지면 책임이 섞입니다.

### 8.9 정상 예시

```
ServiceId
CM.Campaign.approve

거래코드
CM-APR-0001

처리유형
APPROVAL

권한
CM_CAMPAIGN_APPROVE

감사 대상
Y

Timeout
5000ms
```

### 8.10 금지 예시

```
ServiceId
CM.Campaign.approve

거래코드
SV-INQ-0001
```

문제:

- 업무코드 불일치
- 처리유형 불일치
- 잘못된 감사·통계 분류
- 거래통제 오류 가능

### 제8장 요약

```
ServiceId
= 실행 식별자

거래코드
= 운영 분류 식별자

두 값은 연결되지만
같은 목적으로 사용하지 않는다.
```

## 제9장. 표준 요청 전문

### 9.1 전문이란 무엇인가요?

전문은 시스템 사이에 주고받는 데이터의 약속된 형식입니다.

NSIGHT 온라인 거래의 요청은 보통 다음 구조를 사용합니다.

```
StandardRequest
├─ Header
└─ Body
```

요청 예:

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEBTOP",
    "userId": "user01",
    "branchId": "001234",
    "guid": "",
    "traceId": "",
    "requestDtm": "20260717103000123"
  },
  "body": {
    "customerNo": "CUST000001",
    "baseDate": "20260717"
  }
}
```

표준 요청 Header에는 업무코드, ServiceId, 거래코드, 처리유형, 채널, 사용자, 지점, GUID와 TraceId 등이 포함됩니다.

### 9.2 Header와 Body의 차이

```
Header
= 모든 거래가 공통으로 사용하는 정보

Body
= 해당 업무에서만 사용하는 정보
```

예:

| 구분 | 고객 조회 | 캠페인 생성 |
| --- | --- | --- |
| 업무코드 | 공통 Header | 공통 Header |
| ServiceId | 공통 Header | 공통 Header |
| 사용자 ID | 공통 Header | 공통 Header |
| 고객번호 | Body | 해당 없음 |
| 캠페인명 | 해당 없음 | Body |
| 시작일 | 해당 없음 | Body |

### 9.3 Header 주요 항목

| 필드 | 설명 | 작성 주체 |
| --- | --- | --- |
| businessCode | 업무코드 | UI·Gateway |
| serviceId | 실행할 기능 | UI 이벤트 |
| transactionCode | 운영 거래 분류 | UI 이벤트·설정 |
| processingType | 조회·등록·변경 유형 | UI·설정 |
| channelId | 요청 채널 | 채널·Gateway |
| userId | 사용자 ID | 인증 Context 우선 |
| branchId | 소속 지점 | 인증 Context 우선 |
| guid | 전역 거래 추적번호 | Gateway·TCF |
| traceId | 내부 로그 추적번호 | Gateway·TCF |
| requestDtm | 요청 시각 | UI·Gateway |
| idempotencyKey | 중복방지 키 | 변경 요청 클라이언트 |
| contractVersion | 전문 버전 | 클라이언트·서버 |

### 9.4 클라이언트 값을 모두 신뢰하면 안 된다

클라이언트는 다음 값을 변경해서 요청할 수 있습니다.

```
{
  "userId": "admin",
  "branchId": "999999"
}
```

따라서 신뢰 기준은 다음과 같습니다.

```
JWT·인증 Context
= 검증된 사용자 정보

요청 Header
= 비교 또는 보정 대상
```

엄격 모드 예:

```
JWT userId = user01
Header userId = admin

→ 인증정보 불일치
→ 거래 차단
```

### 9.5 GUID와 TraceId

#### GUID

여러 시스템을 지나는 전체 거래를 연결합니다.

```
화면
→ Gateway
→ SV
→ IC
→ 외부 시스템

GUID는 동일하게 유지
```

#### TraceId

애플리케이션 로그와 하위 호출을 연결합니다.

```
GUID
= 전체 거래 묶음

TraceId
= 실행 경로와 로그 묶음
```

프로젝트의 추적 체계에 따라 하위 호출마다 SpanId나 새 TraceId를 추가로 사용할 수도 있습니다.

### 9.6 RequestDtm

요청 시각은 다음 용도로 사용됩니다.

- 오래된 요청 차단
- 재전송 요청 판단
- 거래 순서 분석
- 장애 시각 추적
- 감사 증적
권장 형식 예:

```
yyyyMMddHHmmssSSS
```

예:

```
20260717103000123
```

### 9.7 Idempotency Key

등록·변경 거래는 사용자가 버튼을 두 번 클릭해도 한 번만 처리되어야 할 수 있습니다.

```
첫 번째 요청
idempotencyKey = ABC123
→ 등록 성공

두 번째 요청
idempotencyKey = ABC123
→ 기존 처리결과 반환 또는 중복 차단
```

조회 거래에서는 일반적으로 필수가 아니지만 다음 거래에서는 중요합니다.

- 등록
- 변경
- 승인
- 메시지 발송
- 파일 생성
- 예약
- 외부기관 요청

### 9.8 Body 설계 원칙

#### 필요한 필드만 보낸다

금지:

```
{
  "customer": {
    "customerNo": "CUST001",
    "customerName": "홍길동",
    "residentNo": "...",
    "phoneNo": "...",
    "address": "...",
    "grade": "...",
    "allData": "..."
  }
}
```

고객번호만 필요한 거래라면 다음과 같이 보냅니다.

```
{
  "customerNo": "CUST001"
}
```

#### 화면 객체명을 그대로 사용하지 않는다

금지:

```
{
  "edtCustNo": "CUST001",
  "grdSearchType": "01"
}
```

권장:

```
{
  "customerNo": "CUST001",
  "searchType": "01"
}
```

서버 전문은 화면 컴포넌트가 아니라 업무 의미를 표현해야 합니다.

### 9.9 Body Validation

검증은 크게 두 종류로 나뉩니다.

#### 형식 검증

```
필수 여부
문자열 길이
숫자 형식
날짜 형식
허용 코드
```

예:

```
public record CustomerSummaryRequest(

    @NotBlank
    @Size(max = 20)
    String customerNo,

    @Pattern(regexp = "\\d{8}")
    String baseDate

) {
}
```

#### 업무 검증

```
고객이 존재하는가?
조회 가능한 상태인가?
사용자에게 조회 권한이 있는가?
기준일이 업무 허용 범위인가?
```

업무 검증은 Service와 Rule에서 수행합니다.

### 9.10 정상 요청 예시

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEBTOP",
    "userId": "user01",
    "branchId": "001234",
    "guid": "",
    "traceId": "",
    "requestDtm": "20260717103000123",
    "contractVersion": "1.0"
  },
  "body": {
    "customerNo": "CUST000001",
    "baseDate": "20260717"
  }
}
```

### 9.11 금지 요청 예시

```
{
  "service": "customer",
  "action": "get",
  "user": "admin",
  "data": {
    "value1": "CUST000001"
  }
}
```

문제:

- 표준 Header 없음
- 업무코드 없음
- 정식 ServiceId 없음
- 거래코드 없음
- 사용자 위변조 위험
- Body 필드 의미 불명확
- 추적정보 없음

### 제9장 요약

```
표준 요청
= Header + Body

Header
= 거래 공통정보

Body
= 업무 데이터

사용자·지점은
검증된 인증 Context를 우선한다.
```

## 제10장. 표준 응답 전문

### 10.1 응답 구조

표준 응답은 다음 구조를 사용합니다.

```
StandardResponse
├─ Header
├─ Result
├─ Body
└─ Error
```

정상 응답:

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "guid": "7d82f3b0-0001",
    "traceId": "01JABC0001"
  },
  "result": {
    "resultStatus": "SUCCESS",
    "resultCode": "S0000",
    "message": "정상 처리되었습니다."
  },
  "body": {
    "customerNo": "CUST000001",
    "customerName": "홍길동",
    "customerGrade": "VIP"
  },
  "error": null
}
```

오류 응답:

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "guid": "7d82f3b0-0002",
    "traceId": "01JABC0002"
  },
  "result": {
    "resultStatus": "FAIL",
    "resultCode": "E-SV-CUST-0001",
    "message": "고객을 찾을 수 없습니다."
  },
  "body": null,
  "error": {
    "errorCode": "E-SV-CUST-0001",
    "errorType": "BUSINESS",
    "detailMessage": null
  }
}
```

표준 응답은 Header, Result, Body와 Error를 분리하여 정상 결과와 오류 정보를 일관되게 표현합니다.

### 10.2 Header Echo

응답 Header는 요청 거래를 식별할 수 있는 정보를 반환합니다.

```
요청 ServiceId
→ 응답 ServiceId

요청 또는 생성 GUID
→ 응답 GUID

생성 TraceId
→ 응답 TraceId
```

화면에서 오류가 발생했을 때 GUID나 TraceId를 운영자에게 전달하면 로그를 찾기 쉽습니다.

### 10.3 Result

Result는 거래의 최종 처리상태를 나타냅니다.

| 필드 | 설명 |
| --- | --- |
| resultStatus | 성공·실패 상태 |
| resultCode | 표준 결과코드 |
| message | 사용자 또는 호출자용 메시지 |

예:

```
{
  "resultStatus": "SUCCESS",
  "resultCode": "S0000",
  "message": "정상 처리되었습니다."
}
```

### 10.4 Body

Body는 정상적인 업무 결과를 담습니다.

```
{
  "customerNo": "CUST000001",
  "customerName": "홍길동",
  "customerGrade": "VIP"
}
```

업무 오류가 발생한 경우 Body는 일반적으로 null입니다.

부분 성공을 허용하는 거래는 별도의 계약을 명확하게 정의해야 합니다.

### 10.5 Error

Error는 오류 분석에 필요한 구조화된 정보를 담습니다.

| 필드 | 설명 |
| --- | --- |
| errorCode | 표준 오류코드 |
| errorType | BUSINESS, SYSTEM, TIMEOUT 등 |
| detailMessage | 제한된 상세정보 |
| fieldErrors | 입력 필드 오류 |
| retryable | 재시도 가능 여부 |

보안상 다음 정보는 응답으로 노출하면 안 됩니다.

```
Java 클래스 전체 Stack Trace
SQL 원문
DB 계정
서버 경로
Private Key
JWT 원문
내부 IP
개인정보 원문
```

### 10.6 성공·업무 오류·시스템 오류

#### 성공

```
resultStatus = SUCCESS
resultCode = S0000
body = 업무 결과
error = null
```

#### 업무 오류

```
resultStatus = FAIL
errorType = BUSINESS
```

예:

- 고객이 존재하지 않음
- 캠페인 기간 오류
- 승인할 수 없는 상태
- 조회 권한 없음

#### 시스템 오류

```
resultStatus = ERROR
errorType = SYSTEM
```

예:

- DB 연결 실패
- 프로그램 오류
- 설정 누락
- 외부 시스템 접속 실패

#### Timeout

```
resultStatus = ERROR
errorType = TIMEOUT
```

### 10.7 화면 처리 기준

화면은 메시지 문자열만 보고 성공 여부를 판단하면 안 됩니다.

금지:

```
if (response.result.message === "정상 처리되었습니다.") {
    // 성공
}
```

권장:

```
if (response.result.resultStatus === "SUCCESS") {
    renderCustomer(response.body);
} else {
    showError(
        response.result.resultCode,
        response.result.message,
        response.header.traceId
    );
}
```

### 10.8 HTTP 상태와 업무 결과

HTTP 상태와 업무 결과코드는 목적이 다릅니다.

```
HTTP 상태
= 통신·인증·서버 처리 상태

업무 결과코드
= 업무 처리 결과
```

예:

| 상황 | HTTP 예 | 업무 결과 |
| --- | --- | --- |
| 정상 조회 | 200 | S0000 |
| 고객 없음 | 200 또는 정책값 | 업무 오류코드 |
| 인증 실패 | 401 | 인증 오류 |
| 권한 없음 | 403 | 권한 오류 |
| 잘못된 요청 | 400 | 전문 검증 오류 |
| 서버 장애 | 500 | 시스템 오류 |
| Gateway Timeout | 504 | Timeout 오류 |

정확한 HTTP 정책은 프로젝트의 오류처리 표준을 따릅니다.

### 10.9 금지 응답 예시

```
{
  "success": false,
  "msg": "에러",
  "data": null,
  "exception": "java.sql.SQLException at ..."
}
```

문제:

- 표준 Result 구조 없음
- 오류코드 없음
- 추적정보 없음
- 내부 예외 노출
- 호출자가 오류 종류를 판단하기 어려움

### 제10장 요약

```
표준 응답
= Header + Result + Body + Error

화면은 resultStatus와 resultCode로 판단한다.

내부 Stack Trace와 SQL은
사용자 응답에 노출하지 않는다.
```

## 제11장. 화면 이벤트부터 OM까지 연결하기

### 11.1 거래는 화면 버튼에서 시작된다

사용자가 화면에서 버튼을 클릭하면 이벤트가 발생합니다.

```
화면
SV-CUS-0001 고객 종합정보 조회

버튼
btnSearch

이벤트
SV-CUS-0001-E01

기능
고객 요약 조회
```

이 이벤트가 표준 요청을 생성합니다.

```
SV-CUS-0001-E01
→ SV.Customer.selectSummary
→ SV-INQ-0001
```

### 11.2 전체 추적 구조

```
화면 ID
SV-CUS-0001

    ↓

이벤트 ID
SV-CUS-0001-E01

    ↓

ServiceId
SV.Customer.selectSummary

    ↓

거래코드
SV-INQ-0001

    ↓

Handler
SvCustomerHandler

    ↓

Facade
SvCustomerFacade.selectSummary()

    ↓

Service
SvCustomerService.selectSummary()

    ↓

DAO
SvCustomerDao.selectCustomerSummary()

    ↓

Mapper SQL
SvCustomerMapper.selectCustomerSummary

    ↓

DB 객체
SV_CUSTOMER_SUMMARY

    ↓

OM Catalog
Timeout·권한·거래통제·감사
```

거래설계서는 화면이 아니라 하나의 ServiceId를 중심으로 관리하며, 화면 이벤트·프로그램·SQL·DB·운영정책을 End-to-End로 연결해야 합니다.

### 11.3 OM 등록정보

신규 ServiceId를 개발하면 다음 정보를 OM에 등록합니다.

| 항목 | 예시 |
| --- | --- |
| ServiceId | SV.Customer.selectSummary |
| 서비스명 | 고객 요약정보 조회 |
| 업무코드 | SV |
| 거래코드 | SV-INQ-0001 |
| Handler | SvCustomerHandler |
| 처리유형 | INQUIRY |
| Timeout | 3000ms |
| 인증 필요 | Y |
| 권한코드 | SV_CUSTOMER_VIEW |
| 감사 대상 | Y |
| 중요 거래 | Y |
| 운영 상태 | ACTIVE |
| 허용 채널 | WEBTOP |
| 중복방지 | N |
| 계약 버전 | 1.0 |

TCF 적용을 위해서는 표준 전문과 ServiceId뿐 아니라 Handler, OM Catalog, 거래통제, Timeout, 인증 문맥과 거래로그 구조가 함께 준비되어야 합니다.

### 11.4 코드만 작성하면 안 되는 이유

다음 상태를 생각해 봅시다.

```
Handler 개발 완료
Service 개발 완료
Mapper 개발 완료
단위 테스트 성공
```

하지만 OM에 ServiceId가 등록되어 있지 않습니다.

가능한 결과:

```
미등록 ServiceId로 실행 차단
Timeout 기본값 잘못 적용
권한 확인 불가
감사 대상 누락
운영 중 거래중지 불가
대시보드 분류 불가
```

따라서 신규 거래 완료 기준은 다음과 같습니다.

```
코드
+ 설계서
+ OM 기준정보
+ 테스트
+ 로그 확인
+ 배포 설정
```

### 11.5 화면 이벤트–ServiceId 매트릭스

| 화면 ID | 이벤트 ID | 기능 | ServiceId | 거래코드 |
| --- | --- | --- | --- | --- |
| SV-CUS-0001 | E01 | 고객 요약 조회 | SV.Customer.selectSummary | SV-INQ-0001 |
| SV-CUS-0001 | E02 | 고객 상세 조회 | SV.Customer.selectDetail | SV-INQ-0002 |
| SV-CUS-0001 | E03 | 연락처 변경 | SV.Customer.updateContact | SV-UPD-0001 |
| SV-CUS-0001 | E04 | 상품목록 조회 | SV.Product.selectList | SV-INQ-0010 |

### 11.6 프로그램 추적 매트릭스

| ServiceId | Handler | Facade | Service | DAO | Mapper |
| --- | --- | --- | --- | --- | --- |
| SV.Customer.selectSummary | SvCustomerHandler | selectSummary | selectSummary | selectCustomerSummary | selectCustomerSummary |
| SV.Customer.selectDetail | SvCustomerHandler | selectDetail | selectDetail | selectCustomerDetail | selectCustomerDetail |
| SV.Customer.updateContact | SvCustomerHandler | updateContact | updateContact | updateContact | updateContact |

### 11.7 SQL·DB 추적 매트릭스

| ServiceId | Mapper ID | SQL 유형 | DB 객체 |
| --- | --- | --- | --- |
| SV.Customer.selectSummary | SvCustomerMapper.selectCustomerSummary | SELECT | SV_CUSTOMER_SUMMARY |
| SV.Customer.selectDetail | SvCustomerMapper.selectCustomerDetail | SELECT | SV_CUSTOMER_DETAIL |
| SV.Customer.updateContact | SvCustomerMapper.updateCustomerContact | UPDATE | SV_CUSTOMER_CONTACT |

### 11.8 운영 추적 예시

운영자가 다음 로그를 발견했다고 가정합니다.

```
traceId=T202607170001
serviceId=SV.Customer.selectSummary
resultCode=E-SV-CUST-0002
```

추적 순서:

```
ServiceId 확인
→ OM Catalog 확인
→ Handler 확인
→ 프로그램 설계서 확인
→ Mapper ID 확인
→ SQL 수행시간 확인
→ DB 상태 확인
→ 영향 화면 확인
```

역방향 추적도 가능합니다.

```
SV_CUSTOMER_SUMMARY 테이블 변경
→ 사용하는 Mapper 확인
→ DAO 확인
→ ServiceId 확인
→ 영향 화면 확인
→ 회귀 테스트 선정
```

### 11.9 변경 영향 분석

ServiceId나 SQL을 변경할 때 다음 항목을 확인합니다.

| 변경 대상 | 영향 확인 |
| --- | --- |
| 화면 이벤트 | ServiceId 요청값 |
| ServiceId | Handler, OM, 권한, Timeout |
| 거래코드 | 거래통제, 감사, 통계 |
| DTO | UI, Handler, 계약 테스트 |
| Handler | ServiceId Registry |
| Mapper | DAO, SQL, DB |
| 테이블 | Mapper, ServiceId, 화면 |
| Timeout | 사용자 응답, Thread, DB Pool |
| 권한코드 | 사용자·역할·메뉴 |
| 계약 버전 | 연계 시스템 호환성 |

### 제11장 요약

```
하나의 거래는 코드 파일 하나가 아니다.

화면 이벤트
→ ServiceId
→ 거래코드
→ Handler
→ 프로그램
→ SQL
→ DB
→ OM
→ 로그
→ 테스트

전체가 연결되어야 한다.
```

## 3. 제2부 종합 실습

### 3.1 실습 요구사항

```
캠페인 관리 화면에서 사용자가
캠페인명, 시작일, 종료일을 입력하고
‘등록’ 버튼을 누르면 신규 캠페인을 생성한다.
```

### 3.2 1단계: 업무 식별

| 항목 | 정의 |
| --- | --- |
| 업무코드 | CM |
| 업무도메인 | Campaign |
| 업무 WAR | cm-service |
| Context Path | /cm |
| Endpoint | POST /cm/online |

### 3.3 2단계: 화면 식별

| 항목 | 정의 |
| --- | --- |
| 화면 ID | CM-CAM-0001 |
| 화면명 | 캠페인 등록 |
| 이벤트 ID | CM-CAM-0001-E01 |
| 이벤트 | 등록 버튼 클릭 |
| 기능 ID | CM-CAM-0001-F01 |

### 3.4 3단계: 거래 식별

| 항목 | 정의 |
| --- | --- |
| ServiceId | CM.Campaign.create |
| 거래코드 | CM-REG-0001 |
| 처리유형 | REGISTRATION |
| 권한코드 | CM_CAMPAIGN_CREATE |
| Timeout | 5000ms |
| 중복방지 | Y |
| 감사 대상 | Y |

### 3.5 4단계: 요청 전문

```
{
  "header": {
    "businessCode": "CM",
    "serviceId": "CM.Campaign.create",
    "transactionCode": "CM-REG-0001",
    "processingType": "REGISTRATION",
    "channelId": "WEBTOP",
    "userId": "user01",
    "branchId": "001234",
    "guid": "",
    "traceId": "",
    "requestDtm": "20260717110000123",
    "idempotencyKey": "CM-20260717-USER01-0001",
    "contractVersion": "1.0"
  },
  "body": {
    "campaignName": "우수고객 여름 캠페인",
    "startDate": "20260801",
    "endDate": "20260831"
  }
}
```

### 3.6 5단계: 응답 전문

```
{
  "header": {
    "businessCode": "CM",
    "serviceId": "CM.Campaign.create",
    "transactionCode": "CM-REG-0001",
    "guid": "G202607170001",
    "traceId": "T202607170001"
  },
  "result": {
    "resultStatus": "SUCCESS",
    "resultCode": "S0000",
    "message": "캠페인이 등록되었습니다."
  },
  "body": {
    "campaignId": "CMP202600001",
    "campaignStatus": "DRAFT"
  },
  "error": null
}
```

### 3.7 6단계: 프로그램

| 계층 | 프로그램 |
| --- | --- |
| Handler | CmCampaignHandler |
| Facade | CmCampaignFacade.createCampaign() |
| Service | CmCampaignService.createCampaign() |
| Rule | CmCampaignRule.validateCreate() |
| DAO | CmCampaignDao.insertCampaign() |
| Mapper | CmCampaignMapper.insertCampaign() |
| 테이블 | CM_CAMPAIGN_MASTER |

### 3.8 7단계: 테스트

| 번호 | 조건 | 예상 결과 |
| --- | --- | --- |
| 1 | 정상 입력 | 캠페인 등록 성공 |
| 2 | 캠페인명 없음 | 필수값 오류 |
| 3 | 시작일이 종료일 이후 | 업무 규칙 오류 |
| 4 | 등록 권한 없음 | 권한 오류 |
| 5 | 같은 Idempotency Key 재전송 | 중복 차단 |
| 6 | 거래통제 DENY | Handler 미실행 |
| 7 | DB 오류 | 시스템 오류·Rollback |
| 8 | 5초 초과 | Timeout |
| 9 | ServiceId 오타 | 미등록 ServiceId 오류 |
| 10 | 업무코드 불일치 | Header 검증 오류 |

## 4. 제2부 체크리스트

### 4.1 업무 식별

| 점검 항목 | 확인 |
| --- | --- |
| 업무코드가 공식 코드인가? | □ |
| 업무 WAR와 Context Path가 일치하는가? | □ |
| 데이터 소유 업무가 명확한가? | □ |
| 담당 조직이 명확한가? | □ |

### 4.2 ServiceId

| 점검 항목 | 확인 |
| --- | --- |
| {업무코드}.{도메인}.{행위} 형식인가? | □ |
| 행위가 구체적인가? | □ |
| 기존 ServiceId와 중복되지 않는가? | □ |
| 조회·등록·변경이 분리되어 있는가? | □ |
| Handler에 등록되어 있는가? | □ |

### 4.3 거래코드

| 점검 항목 | 확인 |
| --- | --- |
| 업무코드가 ServiceId와 일치하는가? | □ |
| 처리유형이 실제 행위와 일치하는가? | □ |
| 표준 처리유형 코드를 사용했는가? | □ |
| OM 정책과 연결되어 있는가? | □ |
| 감사·통계 분류가 적절한가? | □ |

### 4.4 요청 전문

| 점검 항목 | 확인 |
| --- | --- |
| Header와 Body가 분리되어 있는가? | □ |
| 필수 Header가 존재하는가? | □ |
| 사용자·지점정보를 인증 Context와 검증하는가? | □ |
| 화면 컴포넌트명이 Body에 포함되지 않았는가? | □ |
| 필요한 업무 데이터만 전송하는가? | □ |
| 변경 거래에 Idempotency Key가 필요한가? | □ |

### 4.5 응답 전문

| 점검 항목 | 확인 |
| --- | --- |
| Header, Result, Body, Error 구조인가? | □ |
| 성공·업무오류·시스템오류를 구분하는가? | □ |
| GUID와 TraceId를 반환하는가? | □ |
| 내부 Stack Trace가 노출되지 않는가? | □ |
| 개인정보가 마스킹되는가? | □ |
| 화면이 resultStatus와 resultCode를 사용하는가? | □ |

### 4.6 운영 등록

| 점검 항목 | 확인 |
| --- | --- |
| OM Service Catalog에 등록되었는가? | □ |
| 거래통제 정책이 등록되었는가? | □ |
| Timeout 정책이 등록되었는가? | □ |
| 권한코드가 연결되었는가? | □ |
| 감사 대상 여부가 정의되었는가? | □ |
| 허용 채널이 정의되었는가? | □ |
| 운영 상태가 정의되었는가? | □ |

### 4.7 추적성

| 점검 항목 | 확인 |
| --- | --- |
| 화면 이벤트와 ServiceId가 연결되는가? | □ |
| ServiceId와 Handler가 연결되는가? | □ |
| Handler와 Facade 메서드가 연결되는가? | □ |
| DAO와 Mapper ID가 연결되는가? | □ |
| Mapper와 DB 객체가 연결되는가? | □ |
| ServiceId와 테스트케이스가 연결되는가? | □ |
| 로그에서 GUID·TraceId로 추적 가능한가? | □ |

## 5. 시사점

### 5.1 핵심 아키텍처 판단

NSIGHT TCF 개발의 시작점은 Java 클래스가 아닙니다.

```
업무 요구사항
→ 업무코드
→ 화면 이벤트
→ ServiceId
→ 거래코드
→ 표준 전문
→ Handler
→ 프로그램과 SQL
→ OM 운영정책
```

이 순서로 정의해야 개발과 운영이 일치합니다.

### 5.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 업무코드 불일치 | 잘못된 WAR·권한·로그 분류 |
| 모호한 ServiceId | Handler와 기능 책임 불명확 |
| 거래코드 혼용 | 감사·통계·통제 오류 |
| 비표준 요청 | STF 검증과 연계 호환성 저하 |
| 사용자 Header 신뢰 | 사용자·지점 위변조 |
| 오류 형식 불일치 | 화면·운영 처리 복잡 |
| OM 미등록 | 운영 통제 불가 |
| 추적성 누락 | 변경 영향과 장애 원인 확인 불가 |
| 기존 ServiceId 임의 변경 | 화면·연계·운영 호환성 손상 |
| 화면과 DB 직접 연결 | 업무 계층과 보안 통제 우회 |

### 5.3 우선 보완 과제

초보 개발자는 다음 항목을 반복 연습해야 합니다.

```
1. 요구사항에서 업무코드 찾기
2. 화면 이벤트 정의하기
3. ServiceId 작성하기
4. 거래코드 작성하기
5. 요청 Header 작성하기
6. Body DTO 작성하기
7. 표준 응답 이해하기
8. OM 등록표 작성하기
9. 전체 추적 매트릭스 작성하기
```

### 5.4 중장기 발전 방향

제2부의 식별체계는 이후 모든 개발 단계의 기준이 됩니다.

```
ServiceId
→ Handler 개발
→ DTO와 Validation
→ Facade 트랜잭션
→ Service와 Rule
→ DAO·Mapper·SQL
→ 오류코드
→ 권한
→ Timeout
→ 거래로그
→ 테스트
→ 운영 모니터링
```

따라서 ServiceId와 표준 전문을 잘못 설계하면 이후 프로그램 전체를 수정해야 할 수 있습니다.

```
코드를 먼저 만들고
나중에 ServiceId를 붙이는 방식보다

거래를 먼저 설계하고
코드를 연결하는 방식이 안전하다.
```

## 6. 마무리말

제2부에서 가장 중요하게 기억해야 할 내용은 다음과 같습니다.

```
업무코드
= 어느 업무가 책임지는가

ServiceId
= 어떤 업무 기능을 실행하는가

거래코드
= 어떤 유형의 거래로 통제·감사할 것인가

표준 요청
= Header + Body

표준 응답
= Header + Result + Body + Error
```

하나의 거래는 다음 전체 연결관계를 가져야 합니다.

```
화면
→ 이벤트
→ ServiceId
→ 거래코드
→ Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ SQL
→ DB
→ OM
→ 로그
→ 테스트
```

초보 개발자가 신규 기능을 받았을 때 가장 먼저 해야 하는 질문은 다음과 같습니다.

```
“어느 클래스부터 만들까요?”
```

가 아닙니다.

다음 질문부터 시작해야 합니다.

```
이 기능은 어느 업무가 책임지는가?

사용자는 어느 화면 이벤트에서 실행하는가?

ServiceId와 거래코드는 무엇인가?

요청과 응답 계약은 무엇인가?

어떤 권한·Timeout·감사 정책이 필요한가?
```

이 질문에 답할 수 있다면 프로그램 개발을 시작할 준비가 된 것입니다.

