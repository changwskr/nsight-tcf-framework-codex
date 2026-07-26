<!-- source: ztcf-집필본/NSIGHT TCF 개발 입문서 - 제7부. 업무 간 연계와 외부 시스템 호출_재구성확장판_v3.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

나는 제7부. 업무 간 연계와 외부 시스템 호출의 개념을 코드·설정·로그·산출물 중 어디에서 확인할 수 있는가? 문제가 발생했을 때 어느 경계부터 조사해야 하는가?

읽기 전 질문

① 장의 학습 목표를 먼저 읽습니다. ② 기존 설명과 예제를 따라갑니다. ③ 실무 적용 절차를 자신의 프로젝트에 대입합니다. ④ 완료 기준으로 이해 여부를 확인합니다.

권장 학습법

인증·인가, 시스템 간 계약, 캐시·배치 등 분산 환경의 경계를 이해합니다.

이 부의 학습 좌표

RESTRUCTURED & EXPANDED EDITION

3단계 · 보안과 연계

**초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서**

# **제7부. 업무 간 연계와 외부 시스템 호출**

# **1\. 도입 전 안내말**

제6부에서는 사용자가 SSO로 인증된 뒤 JWT가 발급되고, Gateway와 업무 WAR가 토큰을 검증하여 신뢰할 수 있는 AuthenticationContext와 TransactionContext를 만드는 과정을 배웠습니다.

제7부에서는 하나의 업무 거래가 다른 업무 또는 외부 시스템의 기능을 필요로 할 때 어떻게 연계해야 하는지를 다룹니다.

다음 화면을 생각해 봅시다.

고객 종합정보 화면
├─ 고객 기본정보
├─ 보유상품
├─ 최근 상담이력
├─ 캠페인 참여이력
└─ 고객 행동정보

이 화면을 처리하는 SV 업무가 모든 데이터를 직접 소유하고 있지는 않습니다.

고객 기본정보
→ IC 업무 소유

상품정보
→ PD 업무 소유

상담이력
→ CT 업무 소유

캠페인정보
→ CM 업무 소유

행동정보
→ BD·EP 업무 소유

초보 개발자는 필요한 데이터가 보이면 다음처럼 구현하기 쉽습니다.

@Autowired
private IcCustomerMapper icCustomerMapper;

@Autowired
private PdProductMapper pdProductMapper;

@Autowired
private CmCampaignMapper cmCampaignMapper;

그리고 다른 업무의 테이블과 Mapper를 직접 호출합니다.

이 방식은 처음에는 빠르게 보일 수 있지만 다음 문제가 발생합니다.

다른 업무의 내부 테이블 구조에 직접 의존한다.

데이터 소유 업무의 검증 규칙을 우회한다.

어느 업무가 장애 책임을 가지는지 불명확하다.

테이블 변경 시 여러 WAR를 동시에 수정해야 한다.

권한·감사·거래통제·Timeout이 적용되지 않는다.

다른 업무 WAR의 독립 배포가 어려워진다.

업무 간 연계의 핵심 원칙은 다음과 같습니다.

다른 업무의 데이터를 사용하더라도
그 데이터의 소유권까지 가져오는 것은 아니다.

다른 업무의 기능은
공개된 ServiceId 또는 연계 계약을 통해 호출한다.

상대 업무의 Mapper와 테이블을
임의로 직접 변경하지 않는다.

연계는 단순히 “호출이 되는가”만 확인해서는 안 됩니다.

누가 누구를 호출하는가?

어느 업무가 최종 결과를 책임지는가?

동기 호출인가, 비동기 처리인가?

하위 시스템이 느리면 얼마나 기다리는가?

실패했을 때 재시도해도 안전한가?

일부 처리만 성공하면 어떻게 복구하는가?

사용자와 TraceId가 끝까지 전달되는가?

연계 전문이 변경되면 누가 영향받는가?

제7부에서는 이러한 질문에 답할 수 있도록 업무 내부 호출부터 외부기관 연계까지 단계별로 설명합니다.

# **2\. 제7부 개요**

## **2.1 목적**

제7부의 목적은 초보 개발자가 연계 대상을 구분하고, 업무 책임 경계를 훼손하지 않으면서 안전한 호출 구조를 설계하고 구현하도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

1.  같은 도메인 내부 호출과 다른 업무 호출을 구분한다.
2.  같은 WAR 내부 호출과 다른 WAR 원격 호출을 구분한다.
3.  다른 업무의 DAO와 Mapper를 직접 호출하면 안 되는 이유를 설명한다.
4.  ServiceId 기반 업무 연계 계약을 설계한다.
5.  공통 Client와 업무별 Client의 역할을 구분한다.
6.  GUID·TraceId·사용자 문맥을 하위 호출에 전달한다.
7.  동기·비동기 연계 중 적절한 방식을 선택한다.
8.  외부 시스템 오류를 내부 표준 오류로 변환한다.
9.  Timeout·재시도·Circuit Breaker를 계층적으로 적용한다.
10.  분산 트랜잭션의 한계를 이해한다.
11.  보상처리·Outbox·상태기반 재처리를 설계한다.
12.  연계 계약 테스트와 장애 테스트를 작성한다.
13.  운영자가 호출자와 피호출자를 추적할 수 있도록 로그를 설계한다.

## **2.2 적용범위**

| **영역** | **주요 내용** |
| --- | --- |
| 동일 도메인 | Service·Rule·DAO 내부 호출 |
| 동일 WAR | 다른 도메인 Application Contract |
| 다른 WAR | ServiceId 기반 TCF 호출 |
| Gateway 경유 | 내부 Route와 인증문맥 |
| EAI | 외부기관·레거시 연계 |
| 동기 연계 | 즉시 응답이 필요한 호출 |
| 비동기 연계 | Queue·Event·Batch 처리 |
| 표준 전문 | Header·Body·Result·Error |
| Context 전파 | GUID·TraceId·사용자·채널 |
| 장애 제어 | Timeout·Retry·Circuit Breaker |
| 데이터 정합성 | 보상·Outbox·재처리 |
| 운영 | 연계로그·Metric·통제 |
| 품질 | 계약·통합·장애 테스트 |

## **2.3 대상 독자**

-   다른 업무 데이터를 조회해야 하는 초보 개발자
-   업무 WAR 간 호출을 처음 구현하는 개발자
-   공통 HTTP Client와 업무 Client의 차이가 궁금한 개발자
-   외부기관 응답코드를 내부 오류코드로 변환해야 하는 개발자
-   분산 트랜잭션과 보상처리가 어려운 개발자
-   동기 호출과 비동기 이벤트 중 선택해야 하는 개발자
-   재시도와 Circuit Breaker를 처음 적용하는 개발자
-   연계 장애의 원인을 추적해야 하는 개발자

## **2.4 선행조건**

다음 내용을 이해하고 있어야 합니다.

ServiceId는 업무 기능의 실행 식별자다.

Handler는 Facade를 호출한다.

Facade는 유스케이스와 트랜잭션 경계다.

JWT와 AuthenticationContext는
검증된 사용자 신원을 전달한다.

GUID와 TraceId는 거래 흐름을 추적한다.

Timeout 이후에는 실제 반영 여부를 확인해야 한다.

## **2.5 주요 용어**

| **용어** | **쉬운 설명** |
| --- | --- |
| 호출자 | 다른 기능을 요청하는 시스템 |
| 피호출자 | 요청을 받아 기능을 실행하는 시스템 |
| 업무 소유권 | 데이터를 생성·변경할 책임이 있는 업무 |
| Application Contract | 다른 도메인에 공개하는 애플리케이션 계층 계약 |
| Remote Client | 다른 서버·WAR를 호출하는 Client |
| EAI | 시스템 간 연계를 중계·표준화하는 구조 |
| 동기 호출 | 호출 후 결과가 올 때까지 기다리는 방식 |
| 비동기 호출 | 요청을 전달한 뒤 결과를 기다리지 않는 방식 |
| Event | 업무상 발생한 사실을 알리는 메시지 |
| Command | 특정 작업을 수행하라고 요청하는 메시지 |
| Contract | 요청·응답 구조와 오류·버전을 포함한 약속 |
| Circuit Breaker | 하위 장애 시 반복 호출을 자동 차단하는 기능 |
| Bulkhead | 장애와 자원을 업무별로 격리하는 방식 |
| Compensation | 이미 완료된 작업을 업무적으로 되돌리는 처리 |
| Outbox | DB 변경과 이벤트 발행을 안전하게 연결하는 패턴 |
| Saga | 여러 시스템의 로컬 트랜잭션과 보상을 연결하는 방식 |
| DLQ | 반복 실패 메시지를 별도로 보관하는 Queue |
| Correlation | 여러 시스템의 로그와 거래를 연결하는 것 |

# **제45장. 업무 연계의 기본 원칙**

학습 목표 | 45장. 업무 연계의 기본 원칙의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 계약과 결합도: 호출자는 공개 계약에만 의존하고 상대 시스템의 내부 테이블이나 구현 세부사항을 전제로 삼지 않아야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **45.1 연계가 필요한 이유**

하나의 업무 시스템이 모든 데이터를 소유하는 경우는 드뭅니다.

예를 들어 고객 종합정보 조회는 여러 업무의 데이터를 조합해야 합니다.

SV.Customer.selectOverview
├─ IC.Customer.selectBasic
├─ PD.CustomerProduct.selectList
├─ CT.Contact.selectRecentList
└─ CM.Campaign.selectParticipationList

SV는 조합 결과를 화면에 제공하지만 각 원천 데이터의 관리 책임까지 가지는 것은 아닙니다.

## **45.2 데이터 소유권과 화면 소유권**

화면을 소유하는 업무와 데이터를 소유하는 업무는 다를 수 있습니다.

| **구분** | **예** |
| --- | --- |
| 화면 소유 | SV 고객 종합화면 |
| 고객정보 소유 | IC |
| 상품정보 소유 | PD |
| 캠페인정보 소유 | CM |
| 상담정보 소유 | CT |

화면 소유 업무는 화면 전체 응답을 책임집니다.

데이터 소유 업무는 자신의 데이터와 업무 규칙을 책임집니다.

## **45.3 다른 업무 테이블 직접 조회 문제**

다음 SQL을 SV 업무에서 직접 작성했다고 가정합니다.

SELECT CUSTOMER\_NAME,
CUSTOMER\_STATUS
FROM IC\_CUSTOMER\_MASTER
WHERE CUSTOMER\_NO = #{customerNo}

처음에는 정상 조회될 수 있습니다.

하지만 다음 문제가 숨어 있습니다.

IC 업무의 고객상태 해석 규칙을 SV가 모른다.

IC 테이블 컬럼 변경이 SV 장애로 이어진다.

IC의 개인정보 권한검증을 우회한다.

IC가 조회 결과를 Cache하거나 가공해도 사용할 수 없다.

어느 업무가 SQL 성능을 책임지는지 불명확하다.

따라서 원칙적으로 다른 업무 데이터는 소유 업무의 공개 계약을 통해 사용합니다.

## **45.4 연계 수준 분류**

연계 대상을 다음과 같이 구분할 수 있습니다.

1\. 같은 도메인 내부
2\. 같은 WAR의 다른 도메인
3\. 다른 업무 WAR
4\. 사내 외부 시스템
5\. 외부기관
6\. 비동기 Event·Batch

각 수준에 따라 호출 방법과 통제 강도가 달라집니다.

## **45.5 호출 방법 선택표**

| **호출 대상** | **권장 방식** |
| --- | --- |
| 같은 Service 내부 | 메서드 호출 |
| 같은 도메인 | Service·Rule·DAO 호출 |
| 같은 WAR의 다른 도메인 | 공개 Application Contract |
| 다른 WAR | ServiceId 기반 원격 호출 |
| 레거시 내부 시스템 | EAI·표준 Client |
| 외부기관 | EAI·전용 Adapter |
| 대량 후속처리 | Event·Queue·Batch |

## **45.6 공개 계약과 내부 구현**

다른 도메인에 공개하는 계약은 내부 구현과 분리해야 합니다.

공개 계약
CustomerInquiryContract

내부 구현
IcCustomerService
IcCustomerRule
IcCustomerDao
IcCustomerMapper

호출자는 공개 계약만 알아야 합니다.

호출자가 내부 DAO와 Mapper를 알아야 한다면 업무 경계가 무너진 것입니다.

## **45.7 조회와 변경의 연계 기준**

### **다른 업무 조회**

공개 조회 ServiceId 또는 Contract를 사용할 수 있습니다.

SV
→ IC.Customer.selectBasic

### **다른 업무 변경**

반드시 데이터 소유 업무의 변경 ServiceId를 호출합니다.

SV
→ IC.Customer.updateContact

금지:

SV DAO
→ IC\_CUSTOMER\_CONTACT 직접 UPDATE

변경에는 소유 업무의 상태검증, 권한, 감사, 이력, 동시성 정책이 적용되어야 하기 때문입니다.

## **45.8 연계 순환 의존**

다음 구조는 위험합니다.

SV → IC
IC → SV

동일 거래에서 서로 호출하면 순환 호출이 발생할 수 있습니다.

SV.Customer.selectOverview
→ IC.Customer.selectBasic
→ SV.Customer.selectOverview
→ 반복

방지 원칙:

-   호출 방향을 아키텍처에서 정의
-   상위 조합 업무를 별도로 정의
-   공통 도메인 계약 분리
-   Runtime 순환 감지
-   최대 호출 깊이 제한

## **45.9 호출 책임**

호출자는 다음을 책임집니다.

정확한 요청 계약 생성
Timeout 설정
오류 변환
재시도 판단
결과 검증
최종 사용자 응답

피호출자는 다음을 책임집니다.

자신의 업무 규칙
권한과 데이터 범위
데이터 정합성
표준 응답
계약 호환성
운영 상태

## **제45장 요약**

학습 목표 | 45장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

화면 소유와 데이터 소유는 다를 수 있다.

다른 업무 데이터는
소유 업무의 공개 계약으로 사용한다.

다른 업무의 DAO·Mapper·테이블을
임의로 직접 변경하지 않는다.

# **제46장. 같은 WAR 내부의 도메인 연계**

학습 목표 | 46장. 같은 WAR 내부의 도메인 연계의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 계약과 결합도: 호출자는 공개 계약에만 의존하고 상대 시스템의 내부 테이블이나 구현 세부사항을 전제로 삼지 않아야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **46.1 같은 WAR라고 직접 호출해도 될까요?**

같은 WAR에 여러 도메인이 함께 있을 수 있습니다.

sv-service
├─ customer
├─ product
├─ activity
└─ dashboard

같은 프로세스 안에 있다고 해서 다른 도메인의 내부 클래스를 자유롭게 호출하면 안 됩니다.

금지:

private final SvProductDao productDao;

customer 도메인 Service가 product 도메인의 DAO를 직접 호출하면 두 도메인의 데이터 접근 책임이 결합됩니다.

## **46.2 Application Contract**

다른 도메인에 제공할 기능을 별도 Contract로 정의할 수 있습니다.

public interface CustomerProductInquiryContract {

List<CustomerProductSummary> selectProducts(
String customerNo,
TransactionContext context
);
}

구현:

@Component
@RequiredArgsConstructor
public class CustomerProductInquiryContractImpl
implements CustomerProductInquiryContract {

private final SvProductService productService;

@Override
public List<CustomerProductSummary> selectProducts(
String customerNo,
TransactionContext context) {

return productService.selectCustomerProducts(
customerNo,
context
);
}
}

호출자:

@Service
@RequiredArgsConstructor
public class SvCustomerOverviewService {

private final CustomerProductInquiryContract
productInquiryContract;

public CustomerOverviewResponse selectOverview(
CustomerOverviewRequest request,
TransactionContext context) {

List<CustomerProductSummary> products =
productInquiryContract.selectProducts(
request.customerNo(),
context
);

return CustomerOverviewResponse.of(products);
}
}

## **46.3 Contract가 필요한 이유**

Contract는 다음을 보호합니다.

호출자는 공개 메서드와 DTO만 안다.

피호출 도메인은 내부 Service·DAO를 변경할 수 있다.

내부 테이블과 Mapper가 외부에 노출되지 않는다.

호출 가능한 기능이 명확해진다.

계약 테스트를 작성할 수 있다.

## **46.4 내부 DTO와 공개 DTO 분리**

피호출 도메인의 DB DTO를 그대로 반환하면 안 됩니다.

금지:

CustomerProductData selectProducts(...)

권장:

List<CustomerProductSummary> selectProducts(...)

공개 DTO에는 호출자에게 필요한 정보만 포함합니다.

## **46.5 트랜잭션 경계**

같은 WAR 내부 호출은 하나의 Spring 트랜잭션에 포함될 수 있습니다.

예:

Facade 트랜잭션 시작
→ Customer Service
→ Product Contract
→ Product Service
→ 동일 DB
→ Commit

하지만 도메인 간 책임은 여전히 분리해야 합니다.

또한 서로 다른 DataSource를 사용하면 하나의 로컬 트랜잭션으로 묶이지 않을 수 있습니다.

## **46.6 조회 조합과 변경 조합**

조회 조합:

Customer Overview
→ Customer Contract 조회
→ Product Contract 조회
→ Activity Contract 조회
→ 결과 조합

변경 조합:

Customer 변경
→ Product 상태 변경

변경 조합은 더 신중해야 합니다.

다음 사항을 확인합니다.

-   하나의 트랜잭션인가?
-   서로 다른 DB인가?
-   일부 실패 시 Rollback 가능한가?
-   도메인 상태전이 순서가 올바른가?
-   보상처리가 필요한가?

## **46.7 도메인 간 호출 규칙**

Handler
→ 자기 도메인 Facade

Facade
→ 자기 도메인 Service
→ 필요한 공개 Contract

Service
→ 다른 도메인 DAO 직접 호출 금지

## **46.8 순환 Contract 방지**

CustomerContract
→ ProductContract

ProductContract
→ CustomerContract

상호 호출이 필요한 경우 다음을 검토합니다.

-   상위 Orchestration 도메인
-   공통 조회모델
-   Event 기반 동기화
-   호출 방향 재설계

## **46.9 정상 예시**

SvCustomerOverviewFacade
→ SvCustomerOverviewService
→ CustomerBasicInquiryContract
→ CustomerProductInquiryContract
→ CustomerActivityInquiryContract
→ 결과 조합

## **46.10 금지 예시**

SvCustomerService
→ SvProductMapper
→ SvActivityDao
→ 공통 DB 테이블 직접 Join

단일 SQL Join이 꼭 필요한 대량 분석 조회라면 별도의 Read Model과 데이터 소유·운영 책임을 설계해야 합니다.

## **제46장 요약**

학습 목표 | 46장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

같은 WAR 안에서도 도메인 경계를 유지한다.

다른 도메인은
내부 DAO가 아니라 공개 Contract로 호출한다.

공개 DTO와 내부 DB DTO를 분리한다.

# **제47장. 다른 업무 WAR 호출하기**

학습 목표 | 47장. 다른 업무 WAR 호출하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 계약과 결합도: 호출자는 공개 계약에만 의존하고 상대 시스템의 내부 테이블이나 구현 세부사항을 전제로 삼지 않아야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **47.1 다른 WAR 호출이 필요한 경우**

예:

SV 고객 종합조회
→ IC 고객 기본정보 필요

업무 WAR가 분리되어 있다면 로컬 Java 메서드로 호출할 수 없습니다.

sv-service.war
→ HTTP·TCF 연계
→ ic-service.war

## **47.2 ServiceId 기반 호출**

피호출 업무가 공개 ServiceId를 제공합니다.

IC.Customer.selectBasic

호출자는 표준 요청을 생성합니다.

{
"header": {
"businessCode": "IC",
"serviceId": "IC.Customer.selectBasic",
"transactionCode": "IC-INQ-0001",
"processingType": "INQUIRY",
"channelId": "INTERNAL",
"guid": "G202607170001",
"traceId": "T202607170002"
},
"body": {
"customerNo": "CUST000001"
}
}

## **47.3 원 거래와 하위 거래**

상위 거래:

SV.Customer.selectOverview

하위 거래:

IC.Customer.selectBasic
PD.CustomerProduct.selectList

전체 GUID는 동일하게 유지합니다.

GUID
G202607170001

각 하위 호출은 별도 Trace·Span을 사용할 수 있습니다.

상위 Trace
T-SV-001

IC 하위 Trace
T-IC-001

PD 하위 Trace
T-PD-001

## **47.4 호출 관계 기록**

연계로그에 다음 정보를 기록합니다.

| **항목** | **예** |
| --- | --- |
| 호출 ServiceId | SV.Customer.selectOverview |
| 피호출 ServiceId | IC.Customer.selectBasic |
| GUID | G202607170001 |
| Parent TraceId | T-SV-001 |
| Child TraceId | T-IC-001 |
| 요청시각 | 서버 시각 |
| 응답시각 | 서버 시각 |
| 처리시간 | 85ms |
| 결과코드 | S0000 |

## **47.5 Route 방식**

내부 호출 경로는 환경에 따라 다를 수 있습니다.

### **Gateway 경유**

sv-service
→ Gateway
→ ic-service

장점:

-   Route 중앙화
-   공통 인증·통제
-   호출로그 통합

단점:

-   Gateway 추가 Hop
-   Gateway 장애 의존

### **내부 Service Route**

sv-service
→ 내부 L4·Service Discovery
→ ic-service

장점:

-   경로 단축
-   내부 호출 최적화

단점:

-   인증·Route·통제를 Client와 업무 WAR가 책임

프로젝트 표준 경로를 하나로 정해야 합니다.

## **47.6 사용자 Token과 Service Token**

### **사용자 Token 전달**

SV → IC
사용자 JWT 전달

장점:

-   최종 사용자 권한 확인 가능
-   감사가 명확함

주의:

-   Audience 정합성
-   토큰 수명
-   토큰 전파 범위

### **Service Token 사용**

SV 서비스 인증
→ IC 호출

추가로 원 사용자를 전달합니다.

actorService=SV
originalUser=user01

장점:

-   서비스 간 신원 분리
-   사용자 토큰 무분별 전파 방지

주의:

-   원 사용자 감사정보 별도 검증
-   Service Token 권한 최소화

## **47.7 요청 Header 재작성**

호출자는 피호출 업무에 맞게 다음 값을 변경합니다.

businessCode
serviceId
transactionCode
processingType

다음 값은 원 거래에서 유지합니다.

GUID
원 사용자
원 지점
원 채널

TraceId는 하위 호출 정책에 따라 새로 생성합니다.

## **47.8 하위 요청 Context**

개념 예:

InternalCallContext childContext =
InternalCallContext.builder()
.guid(parentContext.getGuid())
.parentTraceId(parentContext.getTraceId())
.traceId(traceIdGenerator.next())
.originalUserId(parentContext.getUserId())
.callerServiceId(parentContext.getServiceId())
.targetServiceId("IC.Customer.selectBasic")
.build();

## **47.9 응답 계약 검증**

피호출 응답이 성공이라고 해도 필요한 Body가 없을 수 있습니다.

{
"result": {
"resultStatus": "SUCCESS"
},
"body": null
}

호출자는 계약 위반 여부를 확인해야 합니다.

성공 상태
\+ 필수 Body 존재
\+ 필수 필드 존재
\+ 계약 버전 호환

## **47.10 하위 업무 오류 처리**

피호출 응답:

IC-CUSTOMER-NOT-FOUND

상위 SV 거래는 이를 어떻게 처리할지 결정해야 합니다.

### **필수 데이터**

고객 기본정보 없음
→ SV 고객 종합조회 실패

### **선택 데이터**

최근 상담이력 없음
→ 빈 목록으로 계속 처리

하위 오류를 무조건 무시하거나 무조건 전체 실패로 처리하면 안 됩니다.

## **47.11 내부 호출 Timeout**

상위 Timeout:

SV.Customer.selectOverview
3000ms

하위 호출 예산:

IC 호출 700ms
PD 호출 700ms
CT 호출 500ms
결과 조합 300ms
예비시간 800ms

하위 Client Timeout을 상위 Timeout보다 길게 설정하면 안 됩니다.

## **제47장 요약**

학습 목표 | 47장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

다른 WAR는 ServiceId 기반 표준 요청으로 호출한다.

GUID와 원 사용자 문맥은 유지하고,
하위 TraceId와 호출관계를 기록한다.

하위 오류가 전체 실패인지
선택 데이터 누락인지 계약에서 정의한다.

# **제48장. 공통 Client와 업무별 Client**

학습 목표 | 48장. 공통 Client와 업무별 Client의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **48.1 Client 계층이 필요한 이유**

Service에서 직접 HTTP 코드를 작성하면 다음 책임이 섞입니다.

HttpClient client = ...
String json = ...
HttpResponse response = ...

Service는 업무 흐름에 집중해야 합니다.

HTTP 연결, 직렬화, Timeout, 인증 Header, 오류 변환은 Client 계층이 담당합니다.

## **48.2 Client 계층 구조**

업무 Service
→ 업무별 Client
→ 공통 TCF Client
→ HTTP Client

예:

SvCustomerOverviewService
→ IcCustomerClient
→ TcfInternalClient
→ WebClient·RestClient

## **48.3 공통 TCF Client 책임**

공통 Client는 기술 공통기능을 담당합니다.

표준 요청 생성
JSON 직렬화
Authorization 전달
GUID·TraceId 전달
Connection·Read Timeout
HTTP 상태 처리
표준 응답 역직렬화
공통 연계로그
Metric

## **48.4 업무별 Client 책임**

업무별 Client는 업무 계약을 담당합니다.

어느 ServiceId를 호출할지 결정
요청 DTO 생성
응답 DTO 검증
외부 오류코드 매핑
선택·필수 데이터 판단

## **48.5 공통 Client 인터페이스**

개념 예:

public interface TcfInternalClient {

<Q, R> R call(
String targetBusinessCode,
String targetServiceId,
String transactionCode,
Q requestBody,
Class<R> responseType,
TransactionContext parentContext,
ClientPolicy policy
);
}

## **48.6 업무 Client 구현**

@Component
@RequiredArgsConstructor
public class IcCustomerClient {

private static final String SERVICE\_ID =
"IC.Customer.selectBasic";

private static final String TRANSACTION\_CODE =
"IC-INQ-0001";

private final TcfInternalClient tcfClient;

public IcCustomerBasicResponse selectBasic(
String customerNo,
TransactionContext context) {

IcCustomerBasicRequest request =
new IcCustomerBasicRequest(customerNo);

return tcfClient.call(
"IC",
SERVICE\_ID,
TRANSACTION\_CODE,
request,
IcCustomerBasicResponse.class,
context,
ClientPolicy.ofMillis(700)
);
}
}

## **48.7 Service 사용**

@Service
@RequiredArgsConstructor
public class SvCustomerOverviewService {

private final IcCustomerClient customerClient;
private final PdCustomerProductClient productClient;

public CustomerOverviewResponse selectOverview(
CustomerOverviewRequest request,
TransactionContext context) {

IcCustomerBasicResponse customer =
customerClient.selectBasic(
request.customerNo(),
context
);

List<CustomerProductSummary> products =
productClient.selectProducts(
request.customerNo(),
context
);

return CustomerOverviewResponse.of(
customer,
products
);
}
}

Service에는 URL, HTTP Header, JSON 처리 코드가 나타나지 않습니다.

## **48.8 Client Policy**

Client별 정책을 객체로 정의할 수 있습니다.

public record ClientPolicy(
Duration connectTimeout,
Duration readTimeout,
int maxRetries,
boolean circuitBreakerEnabled,
boolean requiredResponse
) {
}

정책은 코드 하드코딩보다 환경설정과 OM 기준정보로 관리할 수 있습니다.

## **48.9 URL 하드코딩 금지**

금지:

String url =
"http://10.10.10.21:8080/ic/online";

권장:

tcf:
clients:
ic:
base-url: ${IC\_SERVICE\_URL}

또는 Service Discovery·내부 Route를 사용합니다.

## **48.10 공통 Client의 오류 분류**

| **상황** | **내부 예외** |
| --- | --- |
| 연결 실패 | ExternalConnectionException |
| Read Timeout | ExternalTimeoutException |
| HTTP 401 | DownstreamAuthenticationException |
| HTTP 403 | DownstreamAuthorizationException |
| HTTP 5xx | DownstreamSystemException |
| 업무 실패 응답 | 업무별 오류 매핑 |
| JSON 변환 실패 | ContractViolationException |
| 필수 Body 없음 | ContractViolationException |

## **48.11 Client에서 하지 않을 일**

공통 Client가 다음 업무 판단을 하면 안 됩니다.

고객이 없으면 빈 객체 반환

캠페인 상태를 승인 가능으로 변환

특정 오류를 무조건 성공으로 변환

이 판단은 업무별 Client 또는 Service가 담당합니다.

## **48.12 Client 테스트**

공통 Client 테스트:

Header 전달
Timeout 적용
HTTP 오류 변환
표준 응답 파싱
TraceId 전파

업무 Client 테스트:

정확한 ServiceId 사용
요청 DTO 변환
오류코드 매핑
필수 응답 검증

## **제48장 요약**

학습 목표 | 48장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Service에서 HTTP를 직접 처리하지 않는다.

공통 Client는 기술을 담당하고,
업무별 Client는 ServiceId와 업무 계약을 담당한다.

URL·Timeout·인증 Header를
업무 Service에 흩어놓지 않는다.

# **제49장. 외부 시스템과 EAI 연계**

학습 목표 | 49장. 외부 시스템과 EAI 연계의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 계약과 결합도: 호출자는 공개 계약에만 의존하고 상대 시스템의 내부 테이블이나 구현 세부사항을 전제로 삼지 않아야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **49.1 외부 시스템 연계의 특징**

외부기관이나 레거시 시스템은 NSIGHT와 다른 형식을 사용할 수 있습니다.

예:

NSIGHT
JSON·UTF-8·ServiceId

외부 시스템
고정길이 전문·MS949·거래코드

레거시 시스템
XML·SOAP

파일 연계
CSV·대용량 파일

따라서 업무 Service가 외부 형식을 직접 다루면 안 됩니다.

## **49.2 Adapter의 역할**

NSIGHT 업무 DTO
→ Adapter
→ 외부 전문

외부 응답
→ Adapter
→ NSIGHT 업무 DTO

Adapter는 형식 차이를 변환합니다.

## **49.3 EAI 경유 구조**

NSIGHT 업무 WAR
→ tcf-eai Client
→ EAI
→ 외부 시스템

EAI의 주요 역할:

-   Route
-   전문 변환
-   프로토콜 변환
-   보안 연결
-   송수신 로그
-   재처리
-   기관별 연결 관리

## **49.4 외부 전문 계약**

연계설계서에는 다음 항목이 필요합니다.

| **항목** | **설명** |
| --- | --- |
| 연계 ID | 연계 식별자 |
| 송신 시스템 | 호출자 |
| 수신 시스템 | 피호출자 |
| 요청 방식 | HTTP·MQ·File |
| 전문 형식 | JSON·XML·Fixed Length |
| 인코딩 | UTF-8·MS949 |
| Timeout | 연결·응답 |
| 재시도 | 횟수·조건 |
| 중복방지 | 요청번호 |
| 오류코드 | 외부·내부 매핑 |
| 보안 | 인증서·암호화 |
| 운영시간 | 호출 가능 시간 |
| 재처리 | 수동·자동 |
| 담당자 | 시스템별 책임자 |

## **49.5 외부 요청번호**

외부기관 변경 요청에는 고유 요청번호가 필요합니다.

externalRequestId
\= NSIGHT-CM-20260717-000001

이 값은 다음에 사용됩니다.

-   중복방지
-   처리결과 조회
-   외부기관 문의
-   Timeout 후 상태확인
-   감사로그

## **49.6 외부 오류 매핑**

외부기관 응답:

E102
고객 미존재

내부 오류:

E-IC-CUST-0002
고객을 찾을 수 없습니다.

매핑표:

| **외부 코드** | **외부 의미** | **내부 코드** | **재시도** |
| --- | --- | --- | --- |
| 0000 | 정상 | S0000 | 해당 없음 |
| E102 | 고객 없음 | 업무 오류 | 아니오 |
| E201 | 일시 장애 | 외부 장애 | 가능 |
| E999 | 시스템 오류 | 외부 시스템 오류 | 제한적 |

외부 코드를 사용자에게 그대로 노출하지 않습니다.

## **49.7 인코딩 변환**

외부 전문이 MS949이고 내부는 UTF-8일 수 있습니다.

주의사항:

-   한글 깨짐
-   바이트 길이와 문자 길이 차이
-   고정길이 필드 절단
-   특수문자 변환
-   대체문자 발생

고정길이 전문은 문자 수가 아니라 바이트 수 기준일 수 있습니다.

## **49.8 전문 검증**

송신 전:

필수값
필드 길이
숫자·날짜 형식
코드값
바이트 길이
Checksum

수신 후:

응답 전문 길이
응답 코드
요청번호 일치
기관 코드
전문 무결성

## **49.9 외부 시스템 운영시간**

외부기관이 24시간 운영하지 않을 수 있습니다.

평일 09:00~18:00만 호출 가능

운영시간 외 호출 정책:

-   즉시 업무 오류
-   예약 처리
-   Queue 적재 후 운영시간에 처리
-   다음 영업일 Batch

화면에서 즉시 결과가 필요한지 업무와 협의해야 합니다.

## **49.10 외부기관 장애 시 처리**

외부 Timeout
→ 내부 트랜잭션 상태 확인
→ 외부 요청번호로 결과조회
→ UNKNOWN 상태
→ 재처리 대상 등록

외부기관이 처리했는지 모르는 상황에서 무조건 같은 요청을 다시 보내면 중복처리될 수 있습니다.

## **49.11 개인정보 전송**

외부 연계 시 다음을 확인합니다.

전송 목적
최소 정보
암호화
마스킹
보관기간
접근권한
감사로그
법적 근거

업무에 필요하지 않은 고객정보를 전문에 포함하지 않습니다.

## **제49장 요약**

학습 목표 | 49장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

외부 시스템의 전문과 오류를
업무 Service가 직접 처리하지 않는다.

Adapter와 EAI가 형식·프로토콜을 변환하고,
업무 Client가 오류 의미를 변환한다.

Timeout 후에는 외부 요청번호로
실제 처리결과를 확인한다.

# **제50장. 동기·비동기 연계 선택하기**

학습 목표 | 50장. 동기·비동기 연계 선택하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 계약과 결합도: 호출자는 공개 계약에만 의존하고 상대 시스템의 내부 테이블이나 구현 세부사항을 전제로 삼지 않아야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **50.1 동기 호출**

동기 호출은 결과가 올 때까지 기다립니다.

요청
→ 상대 시스템 처리
→ 응답
→ 다음 업무 진행

적합한 경우:

-   화면에 즉시 결과 필요
-   짧은 처리시간
-   필수 데이터 조회
-   즉시 승인 결과
-   오류를 사용자에게 바로 안내

## **50.2 비동기 호출**

비동기 호출은 요청을 전달하고 나중에 처리합니다.

요청
→ Queue·Event 저장
→ 접수 응답

이후
→ Consumer 처리
→ 상태 갱신

적합한 경우:

-   대량 처리
-   시간이 오래 걸리는 작업
-   결과를 즉시 보여줄 필요 없음
-   외부기관 운영시간 차이
-   후속 알림·통계·이력 처리
-   장애 시 재처리 필요

## **50.3 선택 기준**

| **질문** | **동기** | **비동기** |
| --- | --- | --- |
| 즉시 결과 필요 | 적합 | 부적합 |
| 처리시간이 김 | 부적합 | 적합 |
| 대량 대상 | 부적합 | 적합 |
| 상대 장애 영향 | 직접 전파 | 격리 가능 |
| 구현 단순성 | 비교적 단순 | 상태관리 필요 |
| 사용자 경험 | 즉시 결과 | 접수·진행상태 |
| 재처리 | 호출자가 판단 | Queue 기반 가능 |

## **50.4 잘못된 동기 연계**

사용자 화면 요청
→ 고객조회
→ 상품조회
→ 상담조회
→ 캠페인조회
→ 행동조회
→ 외부기관조회

모든 호출을 순차 수행하면 전체 응답시간이 합산됩니다.

500ms + 700ms + 800ms + 900ms + 1000ms
\= 3900ms 이상

화면 목표가 3초라면 실패합니다.

## **50.5 병렬 조회**

서로 독립적인 조회는 병렬 수행을 검토할 수 있습니다.

고객 기본정보 ─┐
상품정보 ├─ 병렬 호출 → 결과 조합
상담이력 ┘

주의사항:

-   전용 Executor
-   최대 동시 호출 수
-   전체 Timeout 예산
-   일부 실패 정책
-   Context 전파
-   ThreadLocal·MDC 전파
-   하위 시스템 부하

무조건 병렬화하면 하위 시스템에 동시 부하가 집중될 수 있습니다.

## **50.6 비동기 Event**

캠페인 생성 후 알림과 통계를 처리한다고 가정합니다.

캠페인 생성 Commit
→ CampaignCreated Event
→ 알림 Consumer
→ 통계 Consumer
→ 감사 Consumer

핵심 업무 등록과 후속 처리를 분리할 수 있습니다.

## **50.7 Event와 Command**

### **Event**

이미 발생한 사실을 알립니다.

CampaignCreated
CustomerContactUpdated

### **Command**

특정 작업 수행을 요청합니다.

SendCampaignMessage
GenerateReport

Event를 “실행 명령”처럼 사용하면 의미가 모호해질 수 있습니다.

## **50.8 비동기 상태관리**

화면이 비동기 작업을 요청하면 접수 ID를 반환합니다.

{
"jobId": "JOB202607170001",
"status": "RECEIVED"
}

상태 예:

RECEIVED
→ PROCESSING
→ SUCCESS

PROCESSING
→ PARTIAL\_SUCCESS

PROCESSING
→ FAIL

FAIL
→ RETRYING

## **50.9 비동기 오류**

비동기 작업은 사용자 HTTP 연결이 이미 종료된 뒤 실패할 수 있습니다.

따라서 다음이 필요합니다.

-   작업상태 조회
-   실패사유
-   재처리
-   운영 경보
-   사용자 알림
-   DLQ
-   보관기간

## **50.10 동기에서 비동기로 전환**

기존 동기 거래를 비동기로 바꾸면 응답 계약이 달라집니다.

기존:

{
"result": "SUCCESS",
"reportUrl": "..."
}

변경:

{
"result": "ACCEPTED",
"jobId": "JOB001"
}

화면, 권한, 운영, 테스트를 함께 변경해야 합니다.

## **제50장 요약**

학습 목표 | 50장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

즉시 결과가 필요한 짧은 작업은 동기,
오래 걸리고 재처리가 필요한 작업은 비동기를 검토한다.

여러 동기 호출을 무조건 순차 실행하면
전체 Timeout이 합산된다.

비동기는 접수·처리·실패·재처리 상태를 관리해야 한다.

# **제51장. Timeout·Retry·Circuit Breaker·Bulkhead**

학습 목표 | 51장. Timeout·Retry·Circuit Breaker·Bulkhead의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **51.1 연계 장애는 전파된다**

하위 시스템이 느리면 호출자도 느려집니다.

IC 장애
→ SV 호출 대기
→ SV Thread 점유
→ SV DB Pool·Thread 부족
→ 고객 종합화면 전체 장애

이를 장애 전파라고 합니다.

## **51.2 Timeout**

하위 호출에는 반드시 Timeout이 있어야 합니다.

Connection Timeout
Read Timeout
Call Timeout

상위 거래 예산보다 짧아야 합니다.

상위 ServiceId 3000ms
→ 하위 Client 700ms

## **51.3 Retry**

재시도는 일시 오류에만 제한적으로 적용합니다.

재시도 가능 후보:

-   연결 초기화 실패
-   HTTP 503
-   일시적 네트워크 오류
-   Rate Limit 후 Retry-After
-   읽기 전용 조회

재시도 금지 후보:

-   Validation 오류
-   권한 오류
-   업무 상태 오류
-   중복 등록
-   서명 오류
-   계약 변환 오류

## **51.4 Retry Storm**

하위 시스템이 장애인데 여러 호출자가 모두 재시도하면 트래픽이 폭증합니다.

원 요청 100건
× 재시도 3회
\= 최대 400건 호출

이를 Retry Storm이라고 합니다.

방지:

-   최대 재시도 횟수 제한
-   Exponential Backoff
-   Jitter
-   Circuit Breaker
-   전체 시간 예산
-   변경 거래 재시도 금지

## **51.5 Circuit Breaker**

상태:

CLOSED
→ 정상 호출

OPEN
→ 즉시 실패

HALF\_OPEN
→ 일부 시험 호출

흐름:

연속 실패 임계치 초과
→ OPEN

대기시간 경과
→ HALF\_OPEN

시험 성공
→ CLOSED

시험 실패
→ OPEN

## **51.6 Circuit Breaker 응답**

하위 시스템 호출을 생략하고 즉시 오류를 반환합니다.

E-INT-CIRCUIT-OPEN
현재 연계 시스템을 사용할 수 없습니다.

선택 데이터라면 Fallback을 적용할 수 있습니다.

상담이력 조회 Circuit Open
→ 빈 상담이력 + 부분응답

필수 데이터라면 전체 실패 처리합니다.

## **51.7 Bulkhead**

Bulkhead는 선박의 격벽처럼 자원을 분리합니다.

IC 호출 Thread Pool
PD 호출 Thread Pool
외부기관 호출 Thread Pool

외부기관 호출이 모두 대기해도 내부 IC 호출 Thread까지 고갈되지 않도록 합니다.

분리 대상:

-   Thread Pool
-   Connection Pool
-   Queue
-   동시 요청 수
-   Timeout
-   Rate Limit

## **51.8 업무별 Thread Pool**

금지:

모든 외부 호출
→ 공통 무제한 Thread Pool

권장:

customer-client-executor
product-client-executor
external-agency-executor

단, Thread Pool을 지나치게 많이 만들면 운영이 복잡해지므로 중요 장애경계 기준으로 구분합니다.

## **51.9 Fallback**

Fallback은 하위 실패 시 대체 결과를 제공하는 방식입니다.

가능한 예:

선택적 추천정보 실패
→ 추천정보 없이 화면 제공

실시간 기준정보 실패
→ 유효한 Cache 사용

금지:

고객 기본정보 조회 실패
→ 임의의 기본 고객 생성

Fallback은 업무적으로 허용된 경우에만 사용합니다.

## **51.10 부분 성공**

고객 종합조회에서 선택정보 일부가 실패했다고 가정합니다.

응답 예:

{
"result": {
"resultStatus": "PARTIAL\_SUCCESS",
"resultCode": "S-PARTIAL"
},
"body": {
"customer": {
"customerNo": "CUST001"
},
"products": \[\],
"warnings": \[
{
"component": "PRODUCT",
"code": "E-PD-TIME-0001"
}
\]
}
}

부분 성공을 사용하려면 화면 계약과 사용자 안내를 명확히 해야 합니다.

## **51.11 정책 조합**

Connection Timeout
→ 연결 대기 제한

Read Timeout
→ 응답 대기 제한

Retry
→ 일시 오류 재시도

Circuit Breaker
→ 반복 장애 차단

Bulkhead
→ 자원 격리

Fallback
→ 대체 결과

각 기능을 모두 켠다고 안정적인 것은 아닙니다.

호출 특성에 맞게 조합해야 합니다.

## **제51장 요약**

학습 목표 | 51장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Timeout은 대기를 제한하고,
Retry는 일시 장애를 다시 시도하며,
Circuit Breaker는 반복 장애를 차단하고,
Bulkhead는 자원을 격리한다.

변경 거래의 자동 재시도는
멱등성이 보장될 때만 허용한다.

# **제52장. 분산 트랜잭션과 보상처리**

학습 목표 | 52장. 분산 트랜잭션과 보상처리의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **52.1 로컬 트랜잭션의 한계**

다음 거래를 생각해 봅시다.

CM DB에 캠페인 등록
→ 외부 메시지 시스템에 캠페인 생성

CM DB는 Rollback할 수 있습니다.

하지만 외부 시스템의 처리는 CM DB 트랜잭션이 자동으로 취소하지 못합니다.

## **52.2 잘못된 기대**

@Transactional
public void createCampaign() {
insertLocalDb();
callExternalSystem();
}

@Transactional은 일반적으로 로컬 DB 트랜잭션을 관리합니다.

외부 HTTP 호출까지 자동 Rollback하는 것은 아닙니다.

## **52.3 분산 트랜잭션 선택지**

| **방식** | **특징** |
| --- | --- |
| JTA·2PC | 여러 XA 자원을 하나의 트랜잭션으로 묶음 |
| Saga | 로컬 트랜잭션과 보상 연결 |
| Outbox | DB 변경과 Event 발행 정합성 확보 |
| 상태기반 처리 | 처리상태를 저장하고 재시도 |
| 수동 보상 | 운영자가 확인 후 되돌림 |

JTA는 강한 일관성을 제공할 수 있지만 복잡도와 장애 영향이 큽니다.

모든 연계에 JTA를 적용하지 않습니다.

## **52.4 보상처리**

처리:

1\. 캠페인 등록
2\. 메시지 템플릿 연결
3\. 외부 시스템 등록

3단계가 실패하면 1·2단계를 업무적으로 취소할 수 있습니다.

CampaignStatus = CANCELLED
TemplateLink = INACTIVE

보상은 DB Rollback과 다릅니다.

Rollback
\= 트랜잭션이 Commit되기 전 취소

Compensation
\= 이미 Commit된 업무를 반대 업무로 되돌림

## **52.5 Saga**

### **Orchestration**

중앙 Orchestrator가 순서를 제어합니다.

CampaignSaga
→ 캠페인 생성
→ 대상조건 생성
→ 외부 등록
→ 실패 시 보상

### **Choreography**

각 서비스가 Event를 받고 다음 Event를 발생시킵니다.

CampaignCreated
→ TargetPrepared
→ ExternalRegistered

초보 프로젝트에서는 책임과 상태를 명확히 볼 수 있는 Orchestration이 이해하기 쉬울 수 있습니다.

## **52.6 Outbox Pattern**

문제:

DB Commit 성공
→ Event 발행 실패

이 경우 DB에는 캠페인이 있지만 후속 Consumer는 알지 못합니다.

Outbox:

하나의 DB 트랜잭션에서
Campaign INSERT
\+ Outbox INSERT
→ Commit

별도 Publisher가 Outbox를 읽어 Event를 발행합니다.

Outbox NEW
→ 발행
→ PUBLISHED

## **52.7 Outbox 테이블 예**

| **컬럼** | **설명** |
| --- | --- |
| EVENT\_ID | Event 식별자 |
| AGGREGATE\_ID | 캠페인 ID |
| EVENT\_TYPE | CampaignCreated |
| PAYLOAD | 최소 Event 데이터 |
| STATUS | NEW·PUBLISHED·FAIL |
| RETRY\_COUNT | 재시도 횟수 |
| CREATE\_DTM | 생성시각 |
| PUBLISH\_DTM | 발행시각 |
| TRACE\_ID | 거래 추적 |

## **52.8 Consumer 멱등성**

같은 Event가 두 번 전달될 수 있습니다.

CampaignCreated EVENT001
→ Consumer 처리 성공
→ ACK 실패
→ 동일 Event 재전달

Consumer는 EVENT\_ID를 기준으로 중복처리를 막아야 합니다.

EVENT001 이미 처리
→ 재실행하지 않고 성공 응답

## **52.9 상태기반 연계**

외부 등록 상태를 내부 DB에 저장합니다.

REQUESTED
→ PROCESSING
→ SUCCESS

PROCESSING
→ FAIL

PROCESSING
→ UNKNOWN

Timeout이면 UNKNOWN으로 두고 외부 조회 API로 최종상태를 확인합니다.

## **52.10 보상 실패**

보상도 실패할 수 있습니다.

외부 등록 성공
→ 내부 후속 실패
→ 외부 취소 요청
→ 외부 취소 실패

필요한 처리:

-   보상 상태 저장
-   재시도
-   운영 경보
-   수동 처리
-   감사로그
-   최종 정합성 확인

## **52.11 분산 정합성 원칙**

모든 시스템을 하나의 긴 트랜잭션으로 묶지 않는다.

각 시스템은 로컬 트랜잭션을 짧게 유지한다.

업무 상태와 Event 상태를 기록한다.

중복처리를 방지한다.

실패와 UNKNOWN을 운영자가 확인할 수 있게 한다.

## **제52장 요약**

학습 목표 | 52장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

로컬 @Transactional은
외부 시스템을 자동 Rollback하지 않는다.

이미 Commit된 작업은
보상거래로 되돌린다.

DB 변경과 Event 발행은
Outbox로 정합성을 높일 수 있다.

Consumer는 중복 Event를 안전하게 처리해야 한다.

# **제53장. 연계 보안과 사용자 문맥 전파**

학습 목표 | 53장. 연계 보안과 사용자 문맥 전파의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 신뢰 경계: 사용자 신원과 권한, 토큰의 유효기간, 민감정보 노출 여부를 분리해 확인해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **53.1 서비스 간 신뢰**

내부 시스템이라고 무조건 신뢰하면 안 됩니다.

검증 대상:

호출 서비스의 신원
최종 사용자
호출 ServiceId
허용된 대상 ServiceId
Audience
네트워크 경로

## **53.2 두 개의 신원**

연계 호출에는 두 신원이 존재할 수 있습니다.

실제 호출 서비스
SV

원 사용자
user01

감사로그는 둘을 구분해야 합니다.

| **항목** | **예** |
| --- | --- |
| Caller Service | SV |
| Original User | user01 |
| Target Service | IC |
| Target ServiceId | IC.Customer.selectBasic |

## **53.3 Token 전달 최소화**

사용자 Access Token을 모든 하위 시스템에 무분별하게 전달하면 노출 범위가 커집니다.

대안:

-   Audience 제한
-   내부용 Service Token
-   Token Exchange
-   짧은 수명
-   최소 Claim
-   mTLS

## **53.4 개인정보 최소화**

하위 호출에 필요한 정보만 전달합니다.

금지:

{
"customer": {
"residentNo": "...",
"address": "...",
"phoneNo": "...",
"allAccounts": \[...\]
}
}

고객번호만 필요한 호출:

{
"customerNo": "CUST000001"
}

## **53.5 전문 암호화와 전송구간**

기본적으로 TLS를 사용합니다.

외부기관 요구에 따라 다음을 추가할 수 있습니다.

-   전문 필드 암호화
-   전자서명
-   mTLS
-   VPN
-   전용선
-   인증서 Pinning
-   Message Authentication Code

## **53.6 민감정보 로그**

연계 Request·Response 전체를 로그에 남기지 않습니다.

권장 로그:

ServiceId
externalRequestId
결과코드
처리시간
요청 Hash
마스킹된 대상 ID

## **53.7 외부 오류의 정보노출**

외부 시스템이 다음 메시지를 반환할 수 있습니다.

DB account ICADM failed at 10.10.1.20

이를 사용자에게 그대로 전달하면 안 됩니다.

내부 표준 메시지로 변환합니다.

외부 시스템 처리 중 오류가 발생했습니다.

상세정보는 제한된 운영로그에 기록합니다.

## **제53장 요약**

학습 목표 | 53장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

연계에서는 호출 서비스와 원 사용자를 구분한다.

Token과 개인정보는 필요한 범위만 전달한다.

내부 연계도 인증·암호화·감사를 적용한다.

외부 상세 오류를 사용자에게 그대로 노출하지 않는다.

# **제54장. 연계 로그·모니터링·장애 대응**

학습 목표 | 54장. 연계 로그·모니터링·장애 대응의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **54.1 연계 장애를 추적하려면**

상위 거래가 실패했다고 하위 시스템 장애라고 단정할 수 없습니다.

다음 흐름을 확인해야 합니다.

상위 ServiceId
→ 업무 Client
→ Route
→ 하위 ServiceId
→ 하위 SQL·외부 호출
→ 응답

## **54.2 연계로그 항목**

| **항목** | **설명** |
| --- | --- |
| GUID | 전체 거래 |
| Parent TraceId | 상위 실행 |
| Child TraceId | 하위 호출 |
| Caller ServiceId | 호출 기능 |
| Target ServiceId | 대상 기능 |
| Endpoint Alias | 대상 Route |
| Start Dtm | 시작 |
| End Dtm | 종료 |
| Elapsed | 처리시간 |
| HTTP Status | 통신 상태 |
| Result Code | 업무 결과 |
| Retry Count | 재시도 횟수 |
| Circuit State | CLOSED·OPEN |
| Request ID | 외부 요청번호 |

## **54.3 연계 Metric**

호출 건수
성공률
업무 오류율
시스템 오류율
Timeout율
평균·p95·p99
재시도 건수
Circuit Open 건수
Queue 적체
DLQ 건수
UNKNOWN 거래

Label에 고객번호나 TraceId를 넣으면 Metric 카디널리티가 폭증하므로 사용하지 않습니다.

## **54.4 장애 점검 순서**

1\. 실패한 상위 ServiceId 확인

2\. GUID·TraceId 확보

3\. 어느 하위 ServiceId에서 실패했는지 확인

4\. 호출 자체 실패인지 업무 오류인지 구분

5\. Timeout·Retry·Circuit 상태 확인

6\. 하위 시스템 거래로그 확인

7\. SQL·DB Pool·Thread 확인

8\. 최근 Route·인증서·설정 변경 확인

9\. 미확정 요청과 중복 요청 확인

## **54.5 증상별 원인**

### **Connection Refused**

대상 서버 Down
잘못된 Route
포트 차단
배포 미완료

### **Read Timeout**

하위 처리 지연
Slow SQL
Thread 고갈
외부기관 응답 지연

### **401·403**

Token 만료
Audience 불일치
Service 권한 부족
인증 Header 누락

### **계약 변환 오류**

응답 필드 변경
Contract Version 불일치
JSON 형식 오류
인코딩 오류

## **54.6 장애 완화**

가능한 조치:

-   문제 연계 ServiceId 통제
-   Circuit Open
-   선택 데이터 Fallback
-   비동기 전환
-   Queue 적재
-   Rate Limit
-   대상 Route 우회
-   이전 버전 Rollback
-   재시도 중지

## **54.7 재처리 전 확인**

외부 요청이 실제 반영되었는가?

Idempotency Key가 존재하는가?

기존 결과조회 API가 있는가?

재시도하면 중복처리되는가?

보상거래가 필요한가?

## **제54장 요약**

학습 목표 | 54장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

연계 장애는
상위와 하위 ServiceId를 함께 추적해야 한다.

GUID·Parent Trace·Child Trace로 호출관계를 연결하고,
Timeout·Retry·Circuit·Queue 상태를 함께 확인한다.

# **3\. 목표 아키텍처**

\[사용자 화면\]
│
▼
\[Gateway\]
│
▼
\[SV 업무 WAR\]
TCF / STF
│
▼
\[SV Handler\]
│
▼
\[SV Facade\]
│
▼
\[SV Service\]
├─────────────────────────────┐
│ │
▼ ▼
\[같은 WAR Contract\] \[업무별 Remote Client\]
Customer·Product IcCustomerClient
│ │
▼ ▼
\[내부 Domain Service\] \[공통 TCF Client\]
│
GUID·Trace·Token·Timeout
│
▼
\[Gateway·Internal Route\]
│
┌───────────────┼──────────────┐
▼ ▼ ▼
\[IC WAR\] \[PD WAR\] \[tcf-eai\]
TCF/STF TCF/STF │
│ │ ▼
▼ ▼ \[외부 시스템\]
DB·SQL DB·SQL

비동기 연계:

\[업무 트랜잭션\]
Campaign INSERT
Outbox INSERT
│
▼
Commit
│
▼
\[Outbox Publisher\]
│
▼
\[Queue·Event\]
│
▼
\[Consumer\]
│
├─ SUCCESS
├─ RETRY
└─ DLQ

# **4\. 표준 형식**

## **4.1 내부 업무 호출 요청**

{
"header": {
"businessCode": "IC",
"serviceId": "IC.Customer.selectBasic",
"transactionCode": "IC-INQ-0001",
"processingType": "INQUIRY",
"channelId": "INTERNAL",
"guid": "G202607170001",
"traceId": "T-IC-0001",
"parentTraceId": "T-SV-0001",
"callerServiceId": "SV.Customer.selectOverview",
"originalUserId": "user01",
"contractVersion": "1.0"
},
"body": {
"customerNo": "CUST000001"
}
}

## **4.2 내부 업무 호출 응답**

{
"header": {
"businessCode": "IC",
"serviceId": "IC.Customer.selectBasic",
"guid": "G202607170001",
"traceId": "T-IC-0001"
},
"result": {
"resultStatus": "SUCCESS",
"resultCode": "S0000"
},
"body": {
"customerNo": "CUST000001",
"customerName": "홍길동",
"customerStatus": "ACTIVE"
},
"error": null
}

## **4.3 비동기 Event**

{
"eventId": "EVT202607170001",
"eventType": "CampaignCreated",
"eventVersion": "1.0",
"occurredAt": "2026-07-17T10:30:00+09:00",
"guid": "G202607170001",
"traceId": "T-CM-0001",
"producer": "cm-service",
"data": {
"campaignId": "CMP202600001"
}
}

# **5\. 구성요소 및 속성**

| **구성요소** | **주요 속성** |
| --- | --- |
| Application Contract | 공개 메서드·DTO·버전 |
| 업무별 Client | ServiceId·오류 매핑 |
| 공통 TCF Client | HTTP·전문·Context |
| Adapter | 외부 전문 변환 |
| EAI | Route·프로토콜·재처리 |
| Client Policy | Timeout·Retry·Circuit |
| Outbox | Event 상태·재시도 |
| Consumer | 멱등성·처리상태 |
| 연계로그 | 호출자·대상·처리시간 |
| Contract Registry | 버전·호환성 |
| DLQ | 반복 실패 메시지 |
| Compensation | 보상 ServiceId·상태 |

# **6\. 책임 경계와 RACI**

| **활동** | **AA** | **DEV** | **FW** | **EAI** | **SEC** | **DBA** | **OM** | **QA** | **OPS** | **상대 시스템** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 연계 방식 결정 | A | C | C | C | C | I | I | C | C | C |
| ServiceId 계약 | A | R | C | I | C | I | C | C | I | C |
| 공통 Client | C | C | A/R | C | C | I | I | C | C | I |
| 업무별 Client | C | A/R | C | C | I | I | I | C | I | C |
| 외부 전문 | C | C | C | A/R | C | I | I | C | C | R/C |
| Timeout·Retry | A | R | C | C | I | C | C | C | C | C |
| 보상처리 | A | R | C | C | I | C | C | C | C | C |
| 보안·인증 | C | C | C | C | A/R | I | I | C | C | C |
| 계약 테스트 | C | R | C | C | I | I | I | A/R | I | C |
| 장애 대응 | C | C | C | C | C | C | C | C | A/R | R/C |
| 재처리 | C | C | C | R/C | I | I | A/C | C | R | C |

# **7\. 정상 처리 흐름**

## **7.1 다른 WAR 동기 조회**

1\. 상위 Service가 업무별 Client 호출

2\. Client가 하위 ServiceId 요청 생성

3\. GUID와 사용자 문맥 유지

4\. Child TraceId 생성

5\. Client Timeout 적용

6\. Gateway·내부 Route 전송

7\. 하위 WAR가 JWT·Service Token 검증

8\. 하위 STF가 권한·거래통제 확인

9\. 하위 Handler 이하 업무 실행

10\. 표준 응답 반환

11\. Client가 응답 계약 검증

12\. 상위 Service가 결과 조합

## **7.2 비동기 Event 처리**

1\. 업무 DB 변경

2\. 동일 트랜잭션에서 Outbox 저장

3\. Commit

4\. Publisher가 Event 발행

5\. Consumer가 Event 수신

6\. Event ID 중복 확인

7\. 업무 처리

8\. 성공상태 저장

9\. 실패 시 제한적 재시도

10\. 최대 실패 시 DLQ

# **8\. 오류·Timeout·장애 흐름**

## **8.1 하위 업무 오류**

하위 ServiceId BUSINESS\_FAIL
→ 업무별 Client 오류 매핑
→ 필수 데이터면 상위 실패
→ 선택 데이터면 부분 성공 검토

## **8.2 하위 Timeout**

Client Timeout
→ Retry 정책 확인
→ Circuit 실패건수 증가
→ 상위 시간 예산 내 처리
→ 필요 시 Fallback

## **8.3 계약 오류**

SUCCESS 응답
→ 필수 Body 없음
→ ContractViolationException
→ 시스템 오류·경보

## **8.4 외부 UNKNOWN**

요청 전송
→ 응답 Timeout
→ 외부 반영 여부 불명
→ 상태 UNKNOWN
→ 결과조회 또는 운영확인

## **8.5 비동기 반복 실패**

Consumer 실패
→ Retry
→ 최대 횟수 초과
→ DLQ
→ 운영자 확인
→ 수정 후 재처리

# **9\. 정상 예시**

상위 ServiceId
SV.Customer.selectOverview

하위 ServiceId
IC.Customer.selectBasic

GUID
G202607170001

Parent Trace
T-SV-0001

Child Trace
T-IC-0001

Client Timeout
700ms

하위 처리
85ms

결과
SUCCESS

상위 결과
고객 종합정보 정상 조합

# **10\. 금지 예시**

## **10.1 다른 업무 Mapper 직접 호출**

private final IcCustomerMapper mapper;

## **10.2 다른 업무 테이블 직접 변경**

UPDATE IC\_CUSTOMER\_MASTER

## **10.3 Service에서 HTTP 직접 처리**

HttpURLConnection connection = ...

## **10.4 URL 하드코딩**

"http://10.1.1.20:8080/ic/online"

## **10.5 Timeout 없는 외부 호출**

connectTimeout 없음
readTimeout 없음

## **10.6 변경 거래 무조건 재시도**

등록 응답 Timeout
→ 같은 요청번호 없이 재전송

## **10.7 외부 오류 무조건 성공 변환**

catch (Exception e) {
return EmptyResponse.success();
}

## **10.8 긴 로컬 트랜잭션에서 외부 호출**

DB Lock 유지
→ 외부기관 30초 대기

# **11\. 연계 규칙**

같은 도메인
→ 내부 계층 호출

같은 WAR·다른 도메인
→ 공개 Application Contract

다른 업무 WAR
→ ServiceId 기반 표준 Client

외부 시스템
→ 업무 Client + Adapter + EAI

대량·장시간 후속처리
→ Event·Queue·Batch

공통 전파 항목:

GUID
Parent TraceId
Child TraceId
Caller ServiceId
Original User
Channel
Request Time
Contract Version

# **12\. 데이터 및 상태관리**

## **12.1 연계 요청 상태**

RECEIVED
→ PROCESSING
→ SUCCESS

PROCESSING
→ BUSINESS\_FAIL

PROCESSING
→ SYSTEM\_ERROR

PROCESSING
→ TIMEOUT

PROCESSING
→ UNKNOWN

## **12.2 Event 상태**

NEW
→ PUBLISHING
→ PUBLISHED

PUBLISHING
→ FAIL
→ RETRY

FAIL
→ DLQ

## **12.3 보상 상태**

NOT\_REQUIRED
COMPENSATION\_REQUIRED
COMPENSATING
COMPENSATED
COMPENSATION\_FAILED

# **13\. 성능·용량·확장성**

| **영역** | **기준** |
| --- | --- |
| 동기 호출 수 | 화면 거래 내 최소화 |
| 병렬 호출 | 동시성 제한 |
| Timeout | 상위 시간 예산 내 분배 |
| Retry | 최대 횟수·Backoff |
| Thread Pool | 장애경계별 격리 |
| Connection Pool | 대상별 적정 크기 |
| Queue | 적체량·처리율 |
| Outbox | 인덱스·정리주기 |
| DLQ | 최대 보관기간 |
| Payload | 최소 데이터 |
| 로그 | 전문 원문 과다기록 금지 |
| Metric | 고카디널리티 방지 |

# **14\. 보안·개인정보·감사**

내부 호출도 인증한다.

호출 서비스와 원 사용자를 구분한다.

Token Audience와 권한을 검증한다.

전송 데이터는 필요한 항목만 포함한다.

TLS와 필요 시 mTLS를 적용한다.

민감정보는 로그와 DLQ에서 보호한다.

외부 요청번호와 처리결과를 감사한다.

보상과 재처리 작업자를 기록한다.

# **15\. 운영·모니터링·장애 대응**

운영 화면에서 다음을 확인할 수 있어야 합니다.

호출자 ServiceId
피호출 ServiceId
GUID·Trace 관계
평균·p95 응답시간
Timeout 건수
재시도 건수
Circuit 상태
Queue 적체
Outbox 미발행
DLQ 건수
UNKNOWN 거래
보상 실패

# **16\. 자동검증 및 품질 Gate**

| **Gate** | **검증** |
| --- | --- |
| 업무 경계 | 다른 업무 Mapper Import 금지 |
| DB | 다른 업무 테이블 Update 금지 |
| Client | Service의 HTTP 직접 사용 금지 |
| URL | 하드코딩 금지 |
| Timeout | 모든 원격 Client 설정 |
| Retry | 변경 거래 멱등성 검증 |
| Context | GUID·Trace 전파 |
| Contract | 요청·응답 Schema 검증 |
| 오류 | 외부 코드 매핑 등록 |
| 보안 | Token·개인정보 로그 금지 |
| Event | Event ID와 Version |
| Consumer | 멱등성 |
| Outbox | DB 변경과 동일 트랜잭션 |
| DLQ | 재처리 절차 |
| 보상 | 실패 시나리오 테스트 |

# **17\. 테스트 시나리오**

| **ID** | **시나리오** | **기대 결과** |
| --- | --- | --- |
| INT-001 | 같은 WAR Contract 정상호출 | 정상 결과 |
| INT-002 | 다른 도메인 DAO 직접참조 | 품질 Gate 실패 |
| INT-003 | 다른 WAR 정상조회 | 결과 조합 |
| INT-004 | GUID 전파 | 전체 로그 연결 |
| INT-005 | Child Trace 생성 | 호출관계 확인 |
| INT-006 | 하위 업무 오류 | 표준 오류 매핑 |
| INT-007 | 선택 데이터 없음 | 빈 결과·부분 성공 |
| INT-008 | 필수 데이터 없음 | 상위 업무 실패 |
| INT-009 | 하위 Timeout | 제한시간 내 종료 |
| INT-010 | HTTP 503 | 제한적 Retry |
| INT-011 | 변경 거래 Timeout | 자동 재시도 금지 |
| INT-012 | Circuit Open | 즉시 실패 |
| INT-013 | Fallback 허용 조회 | 대체 결과 |
| INT-014 | 계약 필드 누락 | 계약 오류 |
| INT-015 | 잘못된 Audience | 인증 실패 |
| INT-016 | 원 사용자 누락 | 감사·인증 오류 |
| INT-017 | 외부 요청번호 중복 | 중복 반영 없음 |
| INT-018 | 외부 Timeout 후 성공 확인 | 재전송 없음 |
| INT-019 | Outbox 저장 실패 | 업무 DB Rollback |
| INT-020 | Event 중복 전달 | Consumer 1회 처리 |
| INT-021 | Consumer 반복 실패 | DLQ 이동 |
| INT-022 | 보상 성공 | 상태 COMPENSATED |
| INT-023 | 보상 실패 | 경보·수동 처리 |
| INT-024 | Queue 적체 | 임계치 경보 |
| INT-025 | 외부 전문 인코딩 | 한글·길이 정상 |
| INT-026 | 민감정보 로그 | 원문 미노출 |
| INT-027 | 순환 호출 | 차단·오류 |
| INT-028 | 병렬 호출 일부 실패 | 부분 성공 정책 |
| INT-029 | Retry Storm 조건 | Circuit·Backoff 작동 |
| INT-030 | 배포 버전 불일치 | 계약 호환성 유지 |

# **18\. 제7부 체크리스트**

## **18.1 업무 경계**

| **점검 항목** | **확인** |
| --- | --- |
| 데이터 소유 업무가 명확한가? | □ |
| 다른 업무 Mapper를 직접 호출하지 않는가? | □ |
| 다른 업무 테이블을 직접 변경하지 않는가? | □ |
| 공개 Contract가 정의되어 있는가? | □ |
| 호출 방향이 순환하지 않는가? | □ |

## **18.2 계약**

| **점검 항목** | **확인** |
| --- | --- |
| 요청·응답 DTO가 정의되었는가? | □ |
| 필수·선택 데이터가 구분되는가? | □ |
| 오류코드 매핑이 있는가? | □ |
| 계약 버전이 있는가? | □ |
| 하위 호환성이 검토되었는가? | □ |
| 계약 테스트가 있는가? | □ |

## **18.3 Client**

| **점검 항목** | **확인** |
| --- | --- |
| 공통 Client와 업무 Client가 분리되었는가? | □ |
| Service에서 HTTP를 직접 사용하지 않는가? | □ |
| URL이 설정으로 관리되는가? | □ |
| Timeout이 설정되었는가? | □ |
| 응답 Body를 검증하는가? | □ |
| 호출 Metric이 기록되는가? | □ |

## **18.4 Context와 보안**

| **점검 항목** | **확인** |
| --- | --- |
| GUID가 유지되는가? | □ |
| Parent·Child Trace가 연결되는가? | □ |
| 원 사용자가 전달되는가? | □ |
| 호출 서비스가 식별되는가? | □ |
| Token Audience를 검증하는가? | □ |
| 민감정보를 최소 전송하는가? | □ |

## **18.5 장애 제어**

| **점검 항목** | **확인** |
| --- | --- |
| Connection·Read Timeout이 있는가? | □ |
| Retry 대상 오류가 제한되는가? | □ |
| 최대 Retry 횟수가 있는가? | □ |
| Circuit Breaker가 필요한가? | □ |
| 자원 Bulkhead가 필요한가? | □ |
| Fallback이 업무적으로 허용되는가? | □ |
| Retry Storm을 방지하는가? | □ |

## **18.6 비동기·정합성**

| **점검 항목** | **확인** |
| --- | --- |
| 즉시 결과가 필요한지 판단했는가? | □ |
| 작업상태를 관리하는가? | □ |
| Event ID가 있는가? | □ |
| Consumer가 멱등한가? | □ |
| Outbox가 필요한가? | □ |
| DLQ와 재처리 절차가 있는가? | □ |
| 보상 ServiceId와 상태가 정의되었는가? | □ |

## **18.7 운영**

| **점검 항목** | **확인** |
| --- | --- |
| 호출자·피호출자 ServiceId를 조회할 수 있는가? | □ |
| 연계 Timeout을 모니터링하는가? | □ |
| Circuit 상태를 확인할 수 있는가? | □ |
| Queue 적체를 확인할 수 있는가? | □ |
| UNKNOWN 거래를 찾을 수 있는가? | □ |
| 보상 실패를 경보하는가? | □ |

# **19\. 변경·호환성·폐기 관리**

## **19.1 계약 버전 변경**

다음 변경은 하위 호환 가능성이 있습니다.

선택 필드 추가
새 오류코드 추가
선택 Header 추가

다음 변경은 호환성 위험이 큽니다.

필수 필드 추가
필드 삭제
필드 타입 변경
필드 의미 변경
성공·오류 구조 변경

## **19.2 병행 버전**

필요하면 일정 기간 두 버전을 운영합니다.

IC.Customer.selectBasic
contractVersion=1.0

IC.Customer.selectBasic
contractVersion=2.0

또는 신규 ServiceId를 만들 수 있습니다.

IC.Customer.selectBasicV2

ServiceId에 버전을 넣을지는 프로젝트 표준에 따라 결정하며, 무분별한 버전 접미사는 피합니다.

## **19.3 연계 폐기**

ACTIVE
→ DEPRECATED
→ DISABLED
→ REMOVED

폐기 전 확인:

-   호출자 목록
-   최근 호출 건수
-   화면·Batch 영향
-   계약 대체 기능
-   운영 통제
-   로그 보관
-   Route 제거
-   인증서·계정 정리

## **19.4 외부기관 변경**

외부기관의 전문 변경 시 다음 순서를 사용합니다.

변경 전문 분석
→ Adapter 영향
→ 오류코드 매핑
→ 인증서·보안
→ 병행 테스트
→ 전환
→ 구 버전 종료

# **20\. 시사점**

## **20.1 핵심 아키텍처 판단**

업무 간 연계의 핵심은 연결 기술이 아닙니다.

HTTP를 사용할 것인가?
MQ를 사용할 것인가?

보다 먼저 다음을 결정해야 합니다.

누가 데이터를 소유하는가?

누가 최종 결과를 책임지는가?

즉시 결과가 필요한가?

실패 시 전체를 취소해야 하는가?

중복 실행을 막을 수 있는가?

운영자가 원인을 추적할 수 있는가?

## **20.2 주요 위험**

| **위험** | **영향** |
| --- | --- |
| 다른 업무 DB 직접 접근 | 업무 경계 붕괴 |
| Service의 HTTP 직접 구현 | 기술코드 중복 |
| URL 하드코딩 | 환경 전환 장애 |
| Timeout 없음 | Thread 고갈 |
| 무제한 Retry | 장애 증폭 |
| 변경 거래 자동 Retry | 중복 처리 |
| 사용자 Token 과도 전파 | 보안 노출 |
| 외부 오류 그대로 노출 | 내부정보 유출 |
| 긴 분산 트랜잭션 | Lock·장애 확대 |
| Outbox 없음 | DB·Event 불일치 |
| Consumer 멱등성 없음 | 중복 처리 |
| Trace 전파 없음 | 장애 추적 불가 |
| 계약 버전관리 없음 | 연계 동시 장애 |
| 보상처리 없음 | 부분 성공 데이터 누적 |

## **20.3 우선 보완 과제**

1\. 업무별 데이터 소유권 확정
2\. 도메인 간 공개 Contract 정의
3\. 다른 WAR ServiceId 목록 정리
4\. 공통 TCF Client 구현
5\. 업무별 Client와 오류 매핑
6\. GUID·Trace·사용자 Context 전파
7\. Client별 Timeout 정책
8\. Retry·Circuit 기준 확정
9\. 외부 요청번호와 멱등성 적용
10\. Outbox·Consumer 중복방지
11\. UNKNOWN·DLQ·보상 운영절차
12\. 계약·장애·보안 테스트 자동화

## **20.4 중장기 발전 방향**

개별 HTTP Client
→ 공통 TCF Client
→ Contract Registry
→ Service Discovery
→ 분산 Trace
→ Circuit Breaker
→ Event 기반 연계
→ Outbox·Saga
→ 자동 계약검증
→ 장애 영향 자동 분석
→ 안전한 자동 재처리

자동 재처리와 자동 보상은 멱등성, 상태검증, 승인정책이 충분히 준비된 후 적용해야 합니다.

# **21\. 마무리말**

제7부에서 가장 중요하게 기억해야 할 내용은 다음과 같습니다.

같은 도메인
\= 내부 계층 호출

같은 WAR의 다른 도메인
\= 공개 Application Contract

다른 업무 WAR
\= ServiceId 기반 Remote Client

외부 시스템
\= 업무 Client + Adapter + EAI

장시간·대량 후속처리
\= Event·Queue·Batch

안정적인 연계는 다음 구조를 가집니다.

업무 Service
→ 업무별 Client
→ 공통 Client
→ Route·EAI
→ 대상 ServiceId
→ 표준 응답
→ 오류 매핑
→ 결과 조합

호출 문맥은 다음과 같이 연결됩니다.

GUID
\= 전체 업무 거래

Parent TraceId
\= 상위 거래

Child TraceId
\= 하위 호출

Caller ServiceId
\= 호출 기능

Target ServiceId
\= 피호출 기능

Original User
\= 실제 사용자

초보 개발자가 다른 시스템을 호출하기 전에 마지막으로 확인해야 할 질문은 다음과 같습니다.

이 데이터는 어느 업무가 소유하는가?

같은 WAR인가, 다른 WAR인가?

공개된 ServiceId와 Contract가 있는가?

다른 업무의 DAO를 직접 호출하고 있지는 않은가?

호출 결과가 반드시 필요한가?

동기 처리여야 하는가?

얼마나 기다릴 것인가?

재시도해도 중복되지 않는가?

하위 장애가 상위 전체 장애로 전파되는가?

일부 실패를 허용할 수 있는가?

이미 Commit된 작업을 어떻게 보상할 것인가?

GUID와 TraceId로 끝까지 추적 가능한가?

계약이 바뀌어도 호출자가 계속 동작하는가?

이 질문에 답할 수 있다면 단순히 API를 연결하는 개발자를 넘어, 업무 경계와 장애를 통제하며 여러 시스템을 안전하게 연결하는 개발자가 될 수 있습니다.
