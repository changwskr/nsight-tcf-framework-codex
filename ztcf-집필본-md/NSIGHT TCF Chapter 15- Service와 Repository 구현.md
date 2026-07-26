<!-- source: ztcf-집필본/NSIGHT TCF Chapter 15- Service와 Repository 구현.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제15장. Service와 Repository 구현

## 이 장을 시작하며

제13장에서는 자연어 요구사항을 실행 가능한 거래 단위로 바꾸었다.

제14장에서는 그 거래가 주고받는 데이터를 Request·Query·Result·Response DTO로 구분하고, 형식 Validation과 업무 검증의 책임을 나누었다.

이제 검증된 요청을 실제 데이터 처리로 연결해야 한다.

text id="ha2ehq" Request DTO ↓ Service 업무 처리 ↓ Rule 업무 판단 ↓ DAO·Repository ↓ MyBatis Mapper ↓ DB ↓ Result DTO ↓ Response DTO

초보 개발자는 Service와 Repository를 다음 정도로 생각하기 쉽다.

\`\`\`text id=“qbiiht” Service = Controller와 Mapper 사이의 클래스

Repository = DB를 조회하는 클래스

Mapper = SQL을 실행하는 Interface



기능만 구현한다면 이러한 이해로도 프로그램을 작성할 수 있다.

그러나 운영 가능한 시스템을 만들기 위해서는 각 계층의 책임을 더 명확하게 구분해야 한다.

\`\`\`text id="hv7k67"
누가 업무 처리 순서를 결정하는가?

누가 데이터 존재 여부를 해석하는가?

누가 권한과 상태를 판단하는가?

트랜잭션은 어디에서 시작되는가?

누가 SQL을 선택하는가?

조회 결과가 없다는 것을 누가 오류로 판단하는가?

UPDATE 결과가 0건이면 누가 실패로 판단하는가?

DB 오류를 어떤 시스템 오류로 변환하는가?

어떤 로그로 ServiceId와 SQL을 연결하는가?

이 질문에 답하지 않고 코드를 작성하면 다음과 같은 구조가 만들어진다.

\`\`\`text id=“nk2zxq” Handler ↓ Mapper 직접 호출

Service ↓ SQL 결과 그대로 반환

DAO ↓ 고객 미존재 업무 오류 발생

Mapper XML ↓ 권한과 상태를 모두 SQL 조건으로 숨김



코드 줄 수는 짧을 수 있다.

그러나 책임이 섞이면 다음 문제가 발생한다.

| 문제 | 결과 |
|---|---|
| 업무 판단 위치 불명확 | 개발자마다 다른 오류 반환 |
| 트랜잭션 경계 불명확 | 부분 Commit·Rollback 누락 |
| 데이터 소유권 무시 | 다른 업무 테이블 직접 변경 |
| SQL에 규칙 은닉 | 테스트와 변경 영향 분석 어려움 |
| 결과 없음 의미 혼재 | 정상 빈 결과와 오류 혼동 |
| 영향 행 수 미검증 | 변경 실패를 성공으로 처리 |
| 예외 숨김 | 운영 원인 추적 불가 |
| 계층 직접 호출 | 단위테스트와 재사용성 저하 |

NSIGHT TCF의 실제 대표 거래는 다음 구조로 구현되어 있다.

\`\`\`text id="yhrjxr"
SV.Customer.selectSummary
↓
SvCustomerHandler
↓
SvCustomerFacade
↓
SvCustomerService
↓
SvCustomerRule
↓
SvCustomerDao
↓
SvCustomerMapper
↓
SvCustomerMapper.xml
↓
SV\_CUSTOMER

현재 SvCustomerDao는 SvCustomerMapper.selectCustomerSummary()를 호출하고, SvCustomerService가 DAO 결과를 받아 업무 결과를 판단하며, SvCustomerFacade는 @Transactional(readOnly = true, timeout = 3)로 조회 거래의 트랜잭션 경계를 가진다.

## 핵심 관점

\`\`\`text id=“sztc1z” Service는 SQL을 실행하는 계층이 아니다.

Service는 업무 처리 순서와 결과 의미를 결정하는 계층이다.

DAO·Repository와 Mapper는 데이터를 읽고 쓰는 방법을 담당하지만, 그 결과가 업무적으로 성공인지 실패인지는 결정하지 않는다.


\---

\## 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | Service의 책임과 금지사항을 설명한다. |
| 2 | Facade와 Service의 차이를 설명한다. |
| 3 | Service와 Rule의 책임을 구분한다. |
| 4 | DAO·Repository·Mapper의 차이를 설명한다. |
| 5 | NSIGHT TCF에서 Repository가 \`DAO + Mapper\`로 구현되는 이유를 설명한다. |
| 6 | 복잡한 Aggregate에서 별도 Repository Interface가 필요한 경우를 판단한다. |
| 7 | Request DTO를 Query·Command로 변환한다. |
| 8 | 여러 DAO 조회 결과를 Service에서 조합한다. |
| 9 | 단건 조회 결과 없음의 업무 의미를 결정한다. |
| 10 | 상세 조회와 선택 조회의 결과 없음 정책을 구분한다. |
| 11 | 목록 조회에서 빈 목록을 안전하게 반환한다. |
| 12 | Mapper Interface와 XML Namespace·Statement ID를 연결한다. |
| 13 | SQL Parameter와 Result 타입을 명확하게 정의한다. |
| 14 | 조회·등록·변경 SQL의 영향 행 수를 검증한다. |
| 15 | DB 예외와 업무 오류를 구분한다. |
| 16 | 트랜잭션 경계를 Facade에 배치하는 이유를 설명한다. |
| 17 | DB 트랜잭션 안에서 외부 호출을 기다릴 때의 위험을 설명한다. |
| 18 | 조회 거래의 \`readOnly\`와 Timeout을 적용한다. |
| 19 | Service·DAO·Mapper 단위테스트를 작성한다. |
| 20 | 실제 Mapper SQL을 통합테스트로 검증한다. |
| 21 | GUID·ServiceId·Mapper Statement ID로 거래를 추적한다. |
| 22 | 계층 위반과 SQL 품질을 CI/CD에서 자동 검증한다. |

\---

\# 한눈에 보는 구현 흐름

\`\`\`text id="87qpl3"
┌────────────────────────────────────────────────────────────┐
│ 1. Handler │
│ ServiceId 분기·Request DTO 전달 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. Facade │
│ 유스케이스·트랜잭션·Timeout 경계 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. Service │
│ 업무 처리 순서·결과 조립·오류 의미 결정 │
└───────────────┬───────────────────────────────┬────────────┘
▼ ▼
┌──────────────────────────────┐ ┌───────────────────────────┐
│ 4. Rule │ │ 5. DAO·Repository │
│ 상태·권한·업무 판단 │ │ 데이터 접근 추상화 │
└──────────────────────────────┘ └──────────────┬────────────┘
▼
┌──────────────────────────────┐
│ 6. Mapper Interface │
│ Java SQL 실행 계약 │
└──────────────┬───────────────┘
▼
┌──────────────────────────────┐
│ 7. Mapper XML │
│ SQL·Parameter·Result │
└──────────────┬───────────────┘
▼
┌──────────────────────────────┐
│ 8. DB │
│ Table·View·Index │
└──────────────┬───────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 9. Result·Row DTO │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 10. Service │
│ 결과 없음·마스킹·코드명·업무 결과 판단 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 11. Response DTO │
└────────────────────────────────────────────────────────────┘

# 현재 구현과 목표 구조

## 현재 대표 거래

text id="p6a3gp" SvCustomerHandler ↓ SvCustomerFacade ├─ @Transactional(readOnly = true, timeout = 3) └─ Request 변환 ↓ SvCustomerService ├─ Rule을 통해 Criteria 생성 ├─ DAO 호출 ├─ 결과 검증 └─ Response 조립 ↓ SvCustomerDao ↓ SvCustomerMapper ↓ SvCustomerMapper.xml

현재 소스의 대표 거래 지도는 CustomerSummaryRequest → CustomerSummaryCriteria → SvCustomerDao → SvCustomerMapper → CustomerSummaryRow → CustomerSummaryResponse의 구조를 갖는다.

## 구현 상태 판단

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| Handler·Facade·Service 분리 | 구현 확인 | 양호 |
| Facade 트랜잭션 | 구현 확인 | 양호 |
| readOnly=true | 구현 확인 | 조회 거래에 적절 |
| timeout=3 | 구현 확인 | 전체 정책과 정합 확인 필요 |
| Service의 DAO 호출 | 구현 확인 | 양호 |
| DAO의 Mapper 위임 | 구현 확인 | 양호 |
| Mapper Interface·XML | 구현 확인 | 양호 |
| Row·Response 분리 | 구현 확인 | 양호 |
| 형식 검증 Rule 혼재 | 부분 구현 | Bean Validation으로 이동 권장 |
| Result 없음 정책 | 구현 확인 필요 | 거래별 명시 필요 |
| DAO 반환 타입 | 보완 가능 | Optional 적용 검토 |
| Mapper 실행시간 로그 | 보완 필요 | 공통 Interceptor 권장 |
| 영향 행 수 표준검증 | 보완 필요 | 변경 거래에 필수 |
| 데이터 소유권 검증 | 설계 기준 | 자동 Gate 필요 |

# 15.1 Service의 책임

## 15.1.1 Service란 무엇인가

Service는 한 유스케이스 안의 업무 처리 순서를 실행하는 계층이다.

text id="n9259i" 검증된 요청을 받는다. ↓ 업무 처리에 필요한 데이터를 조회한다. ↓ Rule에 업무 판단을 요청한다. ↓ 필요한 데이터 변경을 수행한다. ↓ 여러 결과를 조합한다. ↓ 업무 의미가 있는 Response를 만든다.

Service의 핵심 책임은 다음과 같다.

| 책임 | 설명 |
| --- | --- |
| 업무 흐름 | 조회·검증·변경 순서 결정 |
| Query·Command 생성 | Request와 인증 Context 조합 |
| Rule 호출 | 상태·권한·업무 조건 판단 |
| DAO 호출 | 필요한 데이터 조회·변경 |
| 결과 없음 해석 | 정상·업무 오류 결정 |
| 결과 조합 | 여러 DAO·Client 결과 결합 |
| 외부 연계 조정 | Client 호출과 결과 해석 |
| 마스킹 | 검증된 권한 기준 적용 |
| 오류 변환 | 데이터·연계 결과를 업무 오류로 변환 |
| 응답 생성 | 외부 공개 Response 작성 |

## 15.1.2 Service가 하지 말아야 할 일

\`\`\`text id=“wdpo7n” HTTP Request·Response 직접 처리

JWT 문자열 직접 파싱

StandardResponse 직접 생성

ServiceId 라우팅

SQL 문자열 작성

Mapper XML 직접 선택

DB Connection 직접 관리

화면 메시지 조립

트랜잭션 범위를 임의로 확대

다른 업무 WAR의 Java 클래스 직접 호출


\---

\## 15.1.3 Facade와 Service의 차이

| 구분 | Facade | Service |
|---|---|---|
| 관점 | 유스케이스 경계 | 업무 처리 흐름 |
| 트랜잭션 | 시작·종료 | 경계 안에서 실행 |
| Timeout | 거래 단위 | 하위 작업 준수 |
| 호출 | 하나 이상 Service 조합 | Rule·DAO·Client |
| 입력 | Request·Context | Query·Command·업무 입력 |
| 출력 | 거래 Response | 업무 결과 |
| 금지 | SQL 직접 실행 | HTTP·표준 응답 처리 |

대표 구조:

\`\`\`java id="h6i8w3"
@Component
@RequiredArgsConstructor
public class SvCustomerFacade {

private final SvCustomerService customerService;

@Transactional(
readOnly = true,
timeout = 3
)
public CustomerSummaryResponse selectCustomerSummary(
CustomerSummaryRequest request,
TransactionContext context) {

return customerService.selectCustomerSummary(
request,
context
);
}
}

Facade는 트랜잭션과 유스케이스 실행 경계를 제공한다.

Service는 그 안에서 업무 처리 순서를 구현한다.

트랜잭션을 Facade에 두는 구조는 NSIGHT TCF의 표준 계층 원칙으로 정의되어 있다.

## 15.1.4 Service와 Rule의 차이

| 구분 | Service | Rule |
| --- | --- | --- |
| 책임 | 처리 순서·조합 | 조건 판단 |
| 데이터 조회 | DAO 호출 가능 | 직접 호출 금지 |
| 외부 연계 | Client 호출 가능 | 직접 호출 금지 |
| 상태 변경 | Command 실행 조정 | 가능 여부 판단 |
| 부수효과 | 존재 가능 | 원칙적으로 없음 |
| 테스트 | Mock DAO·Client | 입력값만으로 테스트 |

권장 흐름:

text id="j5b5qj" Service ↓ DAO로 데이터 조회 ↓ Rule에 데이터와 Context 전달 ↓ Rule이 허용·거절 판단 ↓ Service가 다음 처리 수행

금지:

text id="h96kfl" Rule ↓ DAO ↓ DB

Rule이 DAO를 직접 호출하면 같은 입력에도 DB 상태에 따라 결과가 달라지며, 순수 업무 규칙으로 테스트하기 어려워진다.

## 15.1.5 조회 Service 구현 예

\`\`\`java id=“ak4nqv” @Service @RequiredArgsConstructor public class SvCustomerService {

private final SvCustomerDao customerDao;
private final SvCustomerRule customerRule;
private final CustomerResponseAssembler responseAssembler;
private final BusinessClock businessClock;

public CustomerSummaryResponse selectCustomerSummary(
CustomerSummaryRequest request,
TransactionContext context) {

AuthenticationContext auth =
context.getAuthenticationContext();

CustomerSummaryQuery query =
CustomerSummaryQuery.from(
request,
auth,
businessClock.today()
);

CustomerSummaryResult customer =
customerDao.findCustomerSummary(query)
.orElseThrow(
CustomerNotFoundException::new
);

customerRule.validateInquiryAllowed(
customer,
auth
);

return responseAssembler.toResponse(
customer,
auth
);
}

}



이 Service는 다음 순서를 명확히 표현한다.

\`\`\`text id="8relpg"
Query 생성
→ 고객 조회
→ 결과 없음 판단
→ 권한·상태 검증
→ 마스킹된 Response 생성

## 15.1.6 여러 데이터 결과 조합

고객요약이 여러 데이터로 구성될 수 있다.

text id="qntcyq" 고객 기본정보 + 고객등급 + 활성상품 건수 + 최근 접촉정보

Service 예:

\`\`\`java id=“74qjv8” public CustomerSummaryResponse selectCustomerSummary( CustomerSummaryRequest request, TransactionContext context) {

CustomerSummaryQuery query =
createQuery(request, context);

CustomerResult customer =
customerDao.findCustomer(query)
.orElseThrow(
CustomerNotFoundException::new
);

customerRule.validateViewable(
customer,
context.getAuthenticationContext()
);

CustomerGradeResult grade =
gradeDao.findCurrentGrade(query.customerNo())
.orElse(CustomerGradeResult.none());

int productCount =
productDao.countActiveProducts(
query.customerNo()
);

return responseAssembler.toResponse(
customer,
grade,
productCount,
context
);

}


\---

\## 15.1.7 조합 조회 시 주의사항

| 항목 | 확인 |
|---|---|
| 필수 데이터 | 없으면 전체 거래 실패인가 |
| 선택 데이터 | 없으면 null·0·빈 목록인가 |
| 조회 순서 | 앞 결과가 뒤 조건에 필요한가 |
| SQL 개수 | 지나치게 많은 왕복은 아닌가 |
| 일관성 | 같은 기준시점 데이터인가 |
| Timeout | 전체 3초 안에 가능한가 |
| 권한 | 각 데이터에 동일 권한인가 |
| 장애 | 일부 데이터 실패를 허용하는가 |

부분 성공을 허용하려면 응답 계약에 명시해야 한다.

\`\`\`json id="c40tov"
{
"customer": {
"status": "SUCCESS"
},
"products": {
"status": "UNAVAILABLE",
"items": \[\]
}
}

명시적 계약 없이 예외를 숨기고 빈 값을 반환하지 않는다.

## 15.1.8 입력 기본값 적용

기준일 미입력:

java id="yu1i5z" LocalDate baseDate = request.baseDate() == null ? businessClock.today() : request.baseDate();

기본값은 다음 조건을 만족해야 한다.

\`\`\`text id=“0vfe8m” 업무적으로 승인됨

문서에 정의됨

테스트에서 고정 가능

환경과 시각에 따라 재현 가능

운영 로그에서 실제값 확인 가능



\`LocalDate.now()\`를 Service 여러 곳에서 직접 호출하기보다 주입된 Clock을 사용한다.

\---

\## 15.1.9 Service에서 인증 Context 사용

\`\`\`java id="k1r9nu"
AuthenticationContext auth =
context.getAuthenticationContext();

String userId = auth.getUserId();
String branchId = auth.getBranchId();

금지:

java id="razm9e" String userId = request.userId(); String branchId = request.branchId();

화면이 보낸 사용자·지점 값을 데이터권한 기준으로 사용하지 않는다.

## 15.1.10 데이터권한 적용

방법 1: 조회 후 Rule 판단

text id="mm9w5r" 고객 조회 → 관리지점 확인 → Rule 권한 판단

방법 2: 검증된 권한범위를 Query 조건으로 전달

text id="byh85n" AuthenticationContext → allowedBranchIds → Query → SQL

권장 판단:

\`\`\`text id=“0vrw73” 대량 목록 조회 → 권한조건을 SQL에 포함

단건 상세 조회 → SQL 조건 + Service 정책을 함께 검토

존재 여부 비공개 → 권한 없는 데이터와 미존재를 동일 응답 가능



권한조건을 SQL에 넣더라도 그 조건의 생성 책임은 Service·보안 정책에 있다.

Mapper가 임의로 현재 사용자의 지점을 판단하지 않는다.

\---

\## 15.1.11 Service에서 예외 처리

Service가 처리해야 할 대표 오류:

| 상황 | 오류 |
|---|---|
| 필수 단건 없음 | 업무 오류 |
| 조회 불가 상태 | 업무 오류 |
| 기능권한 부족 | 권한 오류 |
| 데이터권한 부족 | 권한 오류 |
| 중복 데이터 | 업무·데이터 정합성 오류 |
| 변경 영향 0건 | 동시성·미존재 오류 |
| 변경 영향 2건 이상 | 시스템·정합성 오류 |
| 외부 시스템 실패 | 연계 오류 |
| DB 예외 | 시스템 오류로 전파 |

금지:

\`\`\`java id="ftt2me"
try {
return customerDao.find(...);
} catch (Exception exception) {
return CustomerSummaryResponse.empty();
}

예외를 빈 결과로 바꾸면 다음을 구분할 수 없다.

text id="i4u2je" 실제 고객 없음 DB 장애 SQL 오류 Mapper 오류 Timeout

## 15.1.12 원인 예외 보존

잘못된 예:

java id="ozdd07" catch (Exception exception) { throw new RuntimeException( "고객 조회 실패" ); }

원인 예외가 손실될 수 있다.

권장:

java id="2708g2" catch (DataAccessException exception) { throw new CustomerPersistenceException( "고객 조회 처리 중 데이터 접근 오류가 발생했습니다.", exception ); }

사용자 응답에는 내부 예외를 노출하지 않지만, 운영 로그와 Exception Chain에서는 원인을 보존한다.

## 15.1.13 외부 호출과 DB 트랜잭션

위험 구조:

text id="cy93a4" DB Transaction 시작 ↓ DB Row Lock ↓ 외부 시스템 호출 ↓ 5초 대기 ↓ DB 변경

문제:

text id="4wb2kx" Lock 장기 유지 DB Connection 장기 점유 Timeout 증가 외부 장애가 DB 거래로 전파 재시도 시 중복 가능

권장 검토:

\`\`\`text id=“cxo5qd” 외부 조회를 트랜잭션 전에 수행할 수 있는가?

DB 변경 후 이벤트로 처리할 수 있는가?

Outbox가 필요한가?

보상처리가 가능한가?

전체 업무 일관성이 반드시 하나의 동기 거래여야 하는가?



조회 Service에서도 외부 호출을 여러 번 조합하면 전체 Timeout 예산을 초과할 수 있다.

\---

\## 15.1.14 Service Logging

Service 로그는 처리의 중요한 업무 단계만 남긴다.

권장:

\`\`\`text id="e0qf8p"
serviceId=SV.Customer.selectSummary
guid=G...
step=SERVICE
action=SELECT\_CUSTOMER\_SUMMARY
result=SUCCESS
elapsedMs=84

금지:

text id="51l7q7" 고객 DTO 전체 고객번호 원문 고객명 원문 JWT Request Body 전체 Response Body 전체

# 15.2 Repository·Mapper의 책임

## 15.2.1 Repository라는 용어

Repository는 도메인 또는 업무 계층이 데이터 저장방식의 세부사항을 직접 알지 않도록 하는 추상화다.

일반적인 구조:

text id="dld9yz" Service ↓ Repository ↓ DB

NSIGHT TCF의 기본 데이터 접근 구조는 다음과 같이 더 구체화되어 있다.

text id="4yjvyf" Service ↓ DAO ↓ MyBatis Mapper Interface ↓ Mapper XML ↓ DB

따라서 본 장에서 Repository는 넓은 의미의 데이터 접근 경계를 의미하며, 실제 기본 구현은 DAO + Mapper다.

복잡한 Aggregate가 필요한 경우에만 다음과 같이 별도 Repository Interface를 추가할 수 있다.

text id="jo223x" Service ↓ CustomerRepository Interface ↓ MyBatisCustomerRepository ↓ Mapper

## 15.2.2 DAO의 책임

| 책임 | 설명 |
| --- | --- |
| Mapper 호출 | 올바른 Statement 실행 |
| Parameter 전달 | Query·Command 전달 |
| 결과 반환 | Row·Result 반환 |
| 기술 예외 변환 | 필요 시 Persistence 예외로 변환 |
| Mapper 은닉 | 상위 계층에 Mapper 직접 노출 방지 |
| 다중 Mapper 조합 | 데이터 접근 수준에서 필요한 경우 제한 적용 |
| 영향 행 수 전달 | Insert·Update·Delete 결과 반환 |

DAO가 하지 말아야 할 일:

\`\`\`text id=“4ajppu” 고객이 VIP인지 판단

사용자 권한 판단

고객 미존재 사용자 메시지 생성

Response DTO 생성

다른 업무 Service 호출

트랜잭션 시작

StandardResponse 생성


\---

\## 15.2.3 DAO 구현 예

\`\`\`java id="rg5vr3"
@Repository
@RequiredArgsConstructor
public class SvCustomerDao {

private final SvCustomerMapper mapper;

public Optional<CustomerSummaryRow> findCustomerSummary(
CustomerSummaryQuery query) {

return Optional.ofNullable(
mapper.selectCustomerSummary(query)
);
}

public int countActiveProducts(
String customerNo) {

return mapper.countActiveProducts(
customerNo
);
}
}

DAO에서 Optional을 사용할 수 있지만 MyBatis Mapper 자체 반환은 일반적으로 Row 또는 null을 사용한다.

\`\`\`text id=“7yfssx” Mapper → Row 또는 null

DAO → Optional

Service → 업무 의미 결정


\---

\## 15.2.4 Repository Interface 적용 기준

별도 Repository Interface가 유용한 경우:

\`\`\`text id="c718f0"
복잡한 Aggregate 복원

저장소 기술 교체 가능성

여러 Mapper·Table을 하나의 도메인 저장 단위로 캡슐화

Domain Object 저장 계약 필요

테스트에서 영속 구현 완전 분리 필요

예:

\`\`\`java id=“qwr3qm” public interface CampaignRepository {

Optional<Campaign> findById(
CampaignId campaignId
);

void save(Campaign campaign);

}



구현:

\`\`\`java id="819wm5"
@Repository
@RequiredArgsConstructor
public class MyBatisCampaignRepository
implements CampaignRepository {

private final CampaignMapper mapper;
private final CampaignMapperAssembler assembler;

@Override
public Optional<Campaign> findById(
CampaignId campaignId) {

CampaignRow row =
mapper.selectCampaign(
campaignId.value()
);

return Optional.ofNullable(row)
.map(assembler::toDomain);
}

@Override
public void save(Campaign campaign) {
mapper.updateCampaign(
assembler.toCommand(campaign)
);
}
}

## 15.2.5 Repository Interface를 남용하지 않는다

단순 조회 거래:

text id="q3sc55" Service → DAO → Mapper

만으로 충분할 수 있다.

모든 테이블에 Repository Interface·Implementation·Mapper를 추가하면 다음 문제가 발생한다.

text id="uj65bg" 클래스 수 급증 단순 위임 코드 증가 업무 의미 없는 추상화 추적 경로 복잡 개발자 혼란

기준:

\`\`\`text id=“qp2p30” 단순 CRUD·조회 → DAO + Mapper

복잡한 Aggregate·도메인 모델 → Repository Interface 검토


\---

\## 15.2.6 Mapper Interface의 책임

Mapper Interface는 Java Method와 SQL Statement를 연결하는 계약이다.

\`\`\`java id="lp2h63"
@Mapper
public interface SvCustomerMapper {

CustomerSummaryRow selectCustomerSummary(
CustomerSummaryQuery query
);

int countActiveProducts(
String customerNo
);
}

확인:

| 항목 | 기준 |
| --- | --- |
| Method명 | XML Statement ID와 일치 |
| Parameter | 구체 Query·Command |
| Return | 구체 Row·숫자·목록 |
| Overload | MyBatis에서는 신중 적용 |
| Annotation SQL | 프로젝트 기준에 따라 제한 |
| 업무 판단 | 금지 |

## 15.2.7 Mapper XML 연결

\`\`\`xml id=“wx7w5h”

<select
id="selectCustomerSummary"
parameterType=
"com.nh.nsight.marketing.sv.application.dto.query.CustomerSummaryQuery"
resultType=
"com.nh.nsight.marketing.sv.persistence.dto.CustomerSummaryRow"
timeout="2">

SELECT CUSTOMER\_NO
, CUSTOMER\_NAME
, CUSTOMER\_GRADE
, BRANCH\_CODE
, TOTAL\_BALANCE
, PRODUCT\_COUNT
, LAST\_TRANSACTION\_DATE
FROM SV\_CUSTOMER
WHERE CUSTOMER\_NO = #{customerNo}
AND BRANCH\_CODE = #{allowedBranchId}

</select>



정합조건:

\`\`\`text id="03h753"
XML namespace
\= Mapper Interface 전체 패키지명

XML id
\= Mapper Method명

parameterType
\= 실제 Parameter 타입

resultType·resultMap
\= 실제 반환 타입

## 15.2.8 완전한 Mapper Statement ID

런타임 Statement ID:

text id="oi5rxp" com.nh.nsight.marketing.sv.persistence.mapper .SvCustomerMapper.selectCustomerSummary

운영 로그에는 단순 selectCustomerSummary보다 완전한 Statement ID를 남기는 것이 좋다.

text id="56u87n" serviceId=SV.Customer.selectSummary mapperStatement=com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper.selectCustomerSummary guid=G... elapsedMs=184 rowCount=1

현재 소스의 DAO·Mapper 연결과 Statement 추적 방식도 같은 구조로 정리되어 있다.

## 15.2.9 SQL 작성 기본 원칙

\`\`\`text id=“w84jzx” SELECT \* 금지

필요한 컬럼만 조회

컬럼 Alias와 Row Field 정합

조회조건 명확화

안정된 정렬키 사용

최대 조회 건수 제한

인덱스 사용 가능 조건

함수로 인덱스 컬럼 감싸기 지양

동적 SQL 조건 검증

개인정보 최소 조회

Query Timeout 적용

영향 행 수 검증


\---

\## 15.2.10 SQL에 업무 규칙을 숨기지 않는다

예:

\`\`\`sql id="gkwsm1"
SELECT ...
FROM SV\_CUSTOMER
WHERE CUSTOMER\_NO = #{customerNo}
AND STATUS\_CODE = 'ACTIVE'
AND BRANCH\_CODE = #{branchId}

결과 0건의 원인이 다음 중 무엇인지 알 수 없다.

text id="ntp00s" 고객 미존재 비활성 고객 권한 없는 지점 잘못된 고객번호

보안상 존재 여부를 숨기기 위해 의도적으로 같은 결과를 반환할 수 있다.

그러나 해당 정책을 Service·Rule 설계에 기록해야 한다.

SQL 조건 자체가 업무 오류 의미를 결정하게 해서는 안 된다.

## 15.2.11 resultMap 사용

컬럼과 필드가 복잡하거나 중첩 객체가 필요하면 resultMap을 사용한다.

\`\`\`xml id=“jybw5x”

<id
property="customerNo"
column="CUSTOMER\_NO"/>

<result
property="customerName"
column="CUSTOMER\_NAME"/>

<result
property="customerGrade"
column="CUSTOMER\_GRADE"/>

<result
property="totalBalance"
column="TOTAL\_BALANCE"/>



\`map-underscore-to-camel-case\`를 사용하더라도 중요한 업무 SQL은 컬럼 Alias 또는 \`resultMap\`으로 정합성을 명확히 할 수 있다.

\---

\## 15.2.12 SQL Parameter Binding

권장:

\`\`\`sql id="nlm6lo"
WHERE CUSTOMER\_NO = #{customerNo}

금지:

sql id="d967x2" WHERE CUSTOMER\_NO = '${customerNo}'

${}는 문자열 치환이므로 SQL Injection 위험이 있다.

동적 정렬 컬럼처럼 #{}를 사용할 수 없는 경우 허용값을 서버에서 제한한다.

java id="pda86j" SortColumn sortColumn = SortColumn.from(request.sortColumn());

사용자 입력을 SQL Fragment로 직접 전달하지 않는다.

## 15.2.13 조회 Timeout

전체 거래 Timeout:

text id="v07dtj" 3초

Mapper Query Timeout:

text id="lujmtb" 2초

권장 관계:

text id="e9qr6l" DB Query Timeout < Facade Transaction Timeout ≤ TCF 전체 Timeout

여러 SQL을 실행하는 거래라면 각 SQL Timeout 합계와 처리 여유시간을 고려한다.

## 15.2.14 Insert·Update·Delete 영향 행 수

Mapper:

java id="m11tic" int updateCustomerMemo( CustomerMemoUpdateCommand command );

Service:

\`\`\`java id=“m2ui9v” int updated = customerDao.updateCustomerMemo(command);

if (updated == 0) { throw new ConcurrentModificationException( “변경 대상이 없거나 다른 사용자가 먼저 수정했습니다.” ); }

if (updated > 1) { throw new DataIntegrityException( “하나의 메모 변경에서 여러 건이 수정되었습니다.” ); }



영향 행 수를 확인하지 않으면 다음 상황을 성공으로 처리할 수 있다.

\`\`\`text id="gx2y7d"
대상 없음
Version 불일치
권한조건 불일치
잘못된 WHERE 조건

## 15.2.15 다중 Row가 반환된 단건 조회

단건 조회 SQL이 두 건 이상 반환하면 MyBatis에서 TooManyResultsException이 발생할 수 있다.

이를 첫 행만 반환하도록 숨기지 않는다.

금지:

sql id="qvl5wi" SELECT ... FROM ... WHERE ... FETCH FIRST 1 ROW ONLY

업무적으로 유일해야 하는 데이터인데 중복이 존재한다면 데이터 정합성 문제다.

확인:

text id="d41am5" PK·Unique Constraint 업무 유일키 적재 중복 기준일 조건 상태 조건

단, “최신 한 건” 조회가 명시된 업무라면 안정된 정렬과 상한을 사용한다.

sql id="9f5ii1" ORDER BY EFFECTIVE\_DATE DESC, SEQUENCE\_NO DESC FETCH FIRST 1 ROW ONLY

## 15.2.16 DB 예외 처리

대표 예외:

| 예외 | 의미 |
| --- | --- |
| Duplicate Key | 중복 키 |
| Data Integrity | 제약조건 위반 |
| Query Timeout | SQL 제한시간 초과 |
| Deadlock | 교착상태 |
| Lock Timeout | 잠금 대기 초과 |
| Bad SQL Grammar | SQL·컬럼 오류 |
| Connection Failure | DB 연결 실패 |
| Too Many Results | 단건 정합성 오류 |

DAO는 기술 예외를 필요한 범위에서 데이터 접근 예외로 분류할 수 있다.

Service는 이를 업무 오류로 임의 변환하지 않는다.

예:

text id="4d14bq" DB 연결 실패 ≠ 고객 미존재

## 15.2.17 다중 DataSource

정보계에서는 RDW·ADW·업무 DB·OM DB 등 여러 DataSource가 존재할 수 있다.

확인:

\`\`\`text id=“s17erj” 어느 DAO가 어느 DataSource를 사용하는가?

Mapper Scan 범위가 정확한가?

TransactionManager가 일치하는가?

ReadOnly DB에 변경 SQL이 없는가?

두 DB 변경을 한 트랜잭션으로 묶어야 하는가?



패키지 또는 명시적 Annotation으로 DataSource 경계를 구분한다.

다중 DB 변경은 기본 로컬 트랜잭션만으로 원자성을 보장하지 못한다.

\---

\## 15.2.18 데이터 소유권

\`\`\`text id="km6arb"
SV 업무가 소유한 고객요약
→ SV DAO·Mapper에서 조회

다른 도메인이 소유한 상태 변경
→ 소유 도메인의 공개 Service·ServiceId 사용

금지:

text id="x6mz37" IC Service ↓ SV Mapper 직접 Import ↓ SV Table UPDATE

데이터 변경은 반드시 데이터 소유 도메인이 수행한다.

# 15.3 단건 조회와 결과 없음 처리

## 15.3.1 결과 없음은 하나의 의미가 아니다

다음 조회는 모두 결과가 0건일 수 있다.

text id="w7wb8v" 고객 상세 조회 고객등급 선택 조회 상품 목록 조회 최근 접촉 조회 공통코드 조회 권한 조회

하지만 업무 의미는 서로 다르다.

| 조회 | 0건 의미 |
| --- | --- |
| 필수 고객 상세 | 업무 오류 |
| 선택 고객등급 | 정상 null 가능 |
| 상품 목록 | 정상 빈 목록 |
| 최근 접촉 | 정상 없음 |
| 필수 공통코드 | 설정 오류 가능 |
| 사용자 권한 | 권한 오류 |
| 변경 전 대상 조회 | 미존재·동시성 오류 |

원본도 상세 조회와 선택 조회의 결과 없음은 오류 정책이 다를 수 있으므로 명시적으로 정의해야 한다고 설명한다.

## 15.3.2 필수 단건 조회

java id="z8wqur" CustomerSummaryResult customer = customerDao.findCustomerSummary(query) .orElseThrow( CustomerNotFoundException::new );

적합:

\`\`\`text id=“41k9q8” 요청한 고객이 반드시 존재해야 함

없으면 사용자가 조건을 수정해야 함

이후 업무 처리가 고객 존재를 전제로 함


\---

\## 15.3.3 선택 단건 조회

\`\`\`java id="ew0o2n"
Optional<CustomerGradeResult> grade =
gradeDao.findCurrentGrade(
query.customerNo()
);

응답:

java id="gijk6w" CustomerGradeResponse gradeResponse = grade.map( responseAssembler::toGradeResponse ).orElse(null);

또는 업무적으로 명시적인 없음 객체를 사용할 수 있다.

java id="2q25hy" CustomerGradeResult grade = gradeDao.findCurrentGrade(customerNo) .orElse( CustomerGradeResult.none() );

## 15.3.4 목록 조회

Mapper:

java id="5mid1f" List<CustomerProductRow> selectProducts( CustomerProductQuery query );

DAO:

\`\`\`java id=“fafkxb” public List findProducts( CustomerProductQuery query) {

List<CustomerProductRow> rows =
mapper.selectProducts(query);

return rows == null
? List.of()
: List.copyOf(rows);

}



목록은 일반적으로 빈 목록을 반환한다.

\`\`\`json id="0e8d0u"
{
"items": \[\],
"totalCount": 0
}

## 15.3.5 null 목록 금지

금지:

java id="9cuyhm" return null;

문제:

text id="03vqs7" 호출자 Null 검사 반복 JSON null·\[\] 혼재 화면 분기 증가 계약 불일치

업무적으로 조회 자체가 적용되지 않는 경우 null이 필요할 수 있으나 계약에 명시해야 한다.

## 15.3.6 결과 없음과 권한 오류

다음 두 방식 중 정책을 결정한다.

### 방식 A: 명확한 구분

\`\`\`text id=“m52bj9” 고객 없음 → CUSTOMER\_NOT\_FOUND

권한 없음 → CUSTOMER\_ACCESS\_DENIED



장점:

\- 사용자 안내 명확
\- 운영 원인 구분 가능

위험:

\- 고객 존재 여부 노출 가능

\### 방식 B: 동일 응답

\`\`\`text id="5rud2b"
고객 없음
또는 권한 없음
→ 조회 가능한 고객이 없습니다.

보안상 존재 여부를 숨길 수 있다.

어느 방식을 적용할지는 개인정보·보안 정책으로 결정한다.

## 15.3.7 기본값과 결과 없음

금지:

\`\`\`java id=“rpm46d” CustomerSummaryRow row = mapper.selectCustomerSummary(query);

if (row == null) { return new CustomerSummaryResponse( request.customerNo(), ““,”“, BigDecimal.ZERO ); }



이 응답은 실제 고객이 존재하는 것처럼 보일 수 있다.

결과 없음에 대한 업무 의미를 명확히 결정한다.

\---

\## 15.3.8 중복 데이터

단건으로 정의된 조회가 여러 건이라면 첫 행을 선택하지 않는다.

\`\`\`text id="5dsw4g"
예상 1건
실제 2건
→ 데이터 정합성 오류

운영 조치:

text id="qtyijr" GUID 확인 Mapper Statement ID 확인 조회조건 확인 중복 데이터 확인 PK·Unique Constraint 검토 적재 Job 확인

## 15.3.9 목록 조회 안정된 정렬

목록 조회:

sql id="8baobz" ORDER BY CUSTOMER\_NAME

이름이 같은 고객이 여러 명이면 페이지 간 중복·누락이 발생할 수 있다.

권장:

sql id="l73qsi" ORDER BY CUSTOMER\_NAME, CUSTOMER\_NO

마지막 정렬키는 가능한 한 유일해야 한다.

## 15.3.10 최대 조회 건수

조건 없는 전체 조회를 허용하지 않는다.

text id="17j24z" 기본 조회기간 최대 조회기간 페이지 크기 최대 페이지 크기 전체 다운로드 기준

예:

\`\`\`text id=“jcsqeh” 화면 페이지 최대 100건

일반 조회기간 최대 31일

대량 다운로드 별도 비동기 거래


\---

\## 15.3.11 개인정보 단건 조회

고객번호 단건 조회라도 다음을 확인한다.

\`\`\`text id="pmymbs"
기능권한
데이터권한
조회사유
개인정보 마스킹
감사로그
반복 대량조회 탐지

단건 API를 반복 호출하면 사실상 대량 정보 수집이 가능하므로 사용자·채널·ServiceId별 호출량도 감시한다.

## 15.3.12 Cache와 결과 없음

Cache에서 null을 저장할지 정책이 필요하다.

\`\`\`text id=“nm6usg” 고객 없음 결과 Cache → 반복 DB 조회 감소

하지만 → 신규 고객 생성 후 오래된 없음 결과 유지 가능



Negative Cache를 사용한다면 짧은 TTL과 명확한 무효화 정책을 적용한다.

\---

\# 15.4 계층별 테스트 포인트

\## 15.4.1 테스트의 목적

테스트는 메서드가 호출되었다는 사실보다 다음을 증명해야 한다.

\`\`\`text id="x05fbq"
업무 계약이 지켜지는가?

결과 없음의 의미가 정확한가?

권한과 상태가 올바르게 판단되는가?

트랜잭션이 성공·실패에 맞게 종료되는가?

SQL이 올바른 데이터를 반환하는가?

오류가 표준 유형으로 변환되는가?

운영 로그로 실행경로를 추적할 수 있는가?

원본은 정상 사례뿐 아니라 경계값·권한·동시성·의존 시스템 실패를 함께 검증하고, 요구사항 ID와 테스트를 연결하도록 정의한다.

## 15.4.2 Rule 단위테스트

대상:

text id="jg2n2u" 고객 상태 지점 권한 마스킹 여부 기준일 범위 변경 가능 여부

예:

\`\`\`java id=“j82xmm” @Test void 일반권한\_사용자는\_고객명을\_마스킹한다() {

AuthenticationContext auth =
fixture.generalUser();

CustomerSummaryResult customer =
fixture.activeCustomer();

MaskingDecision decision =
customerRule.decideMasking(
customer,
auth
);

assertThat(decision.masked())
.isTrue();

}



Rule Test는 DB와 Spring Context 없이 실행할 수 있어야 한다.

\---

\## 15.4.3 Service 단위테스트

Mock 대상:

\`\`\`text id="iawiqm"
DAO
Client
Clock
Rule 필요 시
Assembler 필요 시

정상:

\`\`\`java id=“77dr8i” @Test void 고객요약을\_조회한다() {

given(
customerDao.findCustomerSummary(any())
).willReturn(
Optional.of(fixture.customer())
);

CustomerSummaryResponse response =
customerService.selectCustomerSummary(
fixture.request(),
fixture.context()
);

assertThat(response.customerNo())
.isEqualTo("C000001");

then(customerRule)
.should()
.validateInquiryAllowed(
any(),
any()
);

}


\---

\## 15.4.4 Service 결과 없음 테스트

\`\`\`java id="kd6qbj"
@Test
void 고객이\_없으면\_업무오류를\_발생시킨다() {

given(
customerDao.findCustomerSummary(any())
).willReturn(
Optional.empty()
);

assertThatThrownBy(() ->
customerService.selectCustomerSummary(
fixture.request(),
fixture.context()
)
)
.isInstanceOf(
CustomerNotFoundException.class
);
}

## 15.4.5 선택 데이터 없음 테스트

\`\`\`java id=“mp6n1h” @Test void 등급이\_없어도\_고객요약은\_정상이다() {

given(customerDao.findCustomerSummary(any()))
.willReturn(
Optional.of(fixture.customer())
);

given(gradeDao.findCurrentGrade(any()))
.willReturn(Optional.empty());

CustomerSummaryResponse response =
service.selectCustomerSummary(
fixture.request(),
fixture.context()
);

assertThat(response.customerGrade())
.isNull();

}


\---

\## 15.4.6 DAO 단위테스트

DAO가 Mapper에 올바른 Parameter를 전달하는지 확인한다.

\`\`\`java id="dtzu5q"
@Test
void DAO는\_Mapper에\_Query를\_전달한다() {

CustomerSummaryQuery query =
fixture.query();

given(
mapper.selectCustomerSummary(query)
).willReturn(
fixture.row()
);

Optional<CustomerSummaryRow> result =
dao.findCustomerSummary(query);

assertThat(result).isPresent();

then(mapper)
.should()
.selectCustomerSummary(query);
}

단순 위임 DAO 테스트의 가치가 낮다면 Mapper 통합테스트에 집중할 수 있다.

## 15.4.7 Mapper 통합테스트

검증:

text id="51rryr" XML Namespace Statement ID Parameter Binding Column Alias Result Mapping 날짜·금액 타입 동적 SQL 정렬 영향 행 수

예:

\`\`\`java id=“fm1iwr” @MybatisTest class SvCustomerMapperTest {

@Autowired
private SvCustomerMapper mapper;

@Test
void 고객번호로\_고객요약을\_조회한다() {

CustomerSummaryQuery query =
new CustomerSummaryQuery(
"C000001",
LocalDate.of(2026, 7, 18),
"001234",
true
);

CustomerSummaryRow row =
mapper.selectCustomerSummary(query);

assertThat(row).isNotNull();
assertThat(row.getCustomerNo())
.isEqualTo("C000001");
}

}


\---

\## 15.4.8 운영 DB와 유사한 DB 검증

H2에서 성공한 SQL이 Oracle에서 실패할 수 있다.

차이:

\`\`\`text id="zdwejf"
날짜 함수
문자열 함수
페이징 문법
NULL 정렬
Sequence
MERGE
Hint
데이터 타입
예약어

운영 DB 호환이 중요한 Mapper는 Oracle 호환 Test Container 또는 검증 DB에서 시험한다.

## 15.4.9 Facade 트랜잭션 테스트

조회 거래:

\`\`\`text id=“ep2d0o” readOnly Transaction 활성

Timeout 적용

예외 시 정상 종료

Connection 반환



변경 거래:

\`\`\`text id="i7kc5t"
여러 변경 모두 Commit

중간 실패 시 전체 Rollback

Checked Exception 정책

Timeout 시 Rollback

## 15.4.10 변경 영향 행 수 테스트

\`\`\`java id=“bbr3i7” @Test void Version이\_다르면\_동시수정오류다() {

given(
dao.updateCustomerMemo(any())
).willReturn(0);

assertThatThrownBy(() ->
service.updateCustomerMemo(
fixture.updateRequest(),
fixture.context()
)
)
.isInstanceOf(
ConcurrentModificationException.class
);

}


\---

\## 15.4.11 TCF 통합테스트

전체 경로:

\`\`\`text id="goi4gz"
StandardRequest
→ TCF
→ STF
→ Dispatcher
→ Handler
→ Facade
→ Service
→ DAO·Mapper
→ ETF
→ StandardResponse

확인:

text id="zhaqif" ServiceId 등록 권한 거래통제 Timeout 표준 오류 GUID 거래로그 감사로그

## 15.4.12 Timeout 테스트

방법:

\`\`\`text id=“ajy9pc” Test Mapper 지연

DB Lock 유도

Stub Client 지연

낮은 Test Timeout 적용



검증:

\`\`\`text id="q7qa3u"
Timeout 오류 반환

트랜잭션 종료

Connection Pool 반환

Thread 정리

후속 거래 정상

로그에 지연 단계 기록

## 15.4.13 동시성 테스트

변경 거래 대상:

\`\`\`text id=“1o2nn8” 같은 Row 동시 수정

중복 등록

멱등성 키

Version 증가

Lock 대기

Deadlock



조회 거래 대상:

\`\`\`text id="6aueks"
피크 동시 조회

DB Pool 대기

동일 Cache Key 집중

대량 반복 단건조회

## 15.4.14 SQL 성능 테스트

확인:

text id="m663zc" 실행계획 Index 사용 Logical Read Physical Read 예상·실제 Row 수 정렬 비용 Join 순서 Bind 값별 편차

단순 개발 데이터 10건으로 빠르다고 운영 성능을 보장할 수 없다.

## 15.4.15 권한·개인정보 테스트

| 시나리오 | 기대 결과 |
| --- | --- |
| 정상 지점 고객 | 조회 성공 |
| 타 지점 고객 | 정책상 거절·미존재 처리 |
| 일반권한 | 마스킹 |
| 원문권한 | 승인 필드 표시 |
| 권한 없는 사용자 | Handler·업무 처리 차단 |
| 로그 | 개인정보 원문 미기록 |
| 감사 | 사용자·대상·사유 기록 |

# 계층별 테스트 매트릭스

| 계층 | 주요 테스트 | 외부 의존 |
| --- | --- | --- |
| Rule | 상태·권한·계산 | 없음 |
| Service | 흐름·결과·오류 | DAO·Client Mock |
| DAO | Mapper 위임·예외 | Mapper Mock |
| Mapper | SQL·Mapping | Test DB |
| Facade | Transaction·Rollback | Spring·DB |
| Handler | ServiceId 분기 | Facade Mock |
| TCF 통합 | End-to-End | Spring·DB·OM |
| 계약 | Request·Response | UI·Client Schema |
| 성능 | TPS·p95·Pool | 유사 운영환경 |

# 정상 처리 흐름

text id="8axguz" 1. Handler가 검증된 Request DTO를 Facade에 전달한다. 2. Facade가 readOnly Transaction과 Timeout을 시작한다. 3. Service가 인증 Context와 Request를 이용해 Query를 만든다. 4. DAO가 Query를 Mapper에 전달한다. 5. Mapper가 SQL을 실행한다. 6. DB가 고객 Row 한 건을 반환한다. 7. DAO가 Optional Result로 반환한다. 8. Service가 고객 존재와 상태를 확인한다. 9. Rule이 데이터권한과 마스킹 정책을 판단한다. 10. Service가 Response DTO를 생성한다. 11. Facade Transaction이 정상 종료된다. 12. ETF가 표준 성공 응답을 생성한다. 13. GUID·ServiceId·Mapper Statement ID 로그가 연결된다.

# 결과 없음 흐름

\`\`\`text id=“og0dy5” Mapper → null

DAO → Optional.empty()

Service → 필수 단건인지 판단

필수 → CustomerNotFoundException

선택 → null·None Object

목록 → 빈 목록


\---

\# DB 오류 흐름

\`\`\`text id="paord2"
Mapper SQL 실행
↓
DataAccessException
↓
DAO·Service에서 예외 숨기지 않음
↓
Facade Transaction Rollback
↓
ETF.systemError()
↓
안전한 메시지와 GUID 반환
↓
운영 로그에 Statement ID·원인 예외 기록

# Timeout 흐름

text id="72trmt" Facade Transaction 시작 ↓ Slow SQL·DB Lock ↓ Query 또는 거래 Timeout ↓ SQL 중단 요청 ↓ Transaction Rollback ↓ Connection 반환 ↓ TCF Timeout 오류 ↓ Slow SQL·Pool·Thread 확인

# 정상 예시

## Service

\`\`\`java id=“hygl8e” @Service @RequiredArgsConstructor public class SvCustomerService {

private final SvCustomerDao customerDao;
private final SvCustomerRule customerRule;
private final CustomerResponseAssembler assembler;

public CustomerSummaryResponse selectCustomerSummary(
CustomerSummaryRequest request,
TransactionContext context) {

CustomerSummaryQuery query =
CustomerSummaryQuery.from(
request,
context.getAuthenticationContext()
);

CustomerSummaryRow row =
customerDao.findCustomerSummary(query)
.orElseThrow(
CustomerNotFoundException::new
);

customerRule.validateInquiryAllowed(
row,
context.getAuthenticationContext()
);

return assembler.toResponse(
row,
context.getAuthenticationContext()
);
}

}


\## DAO

\`\`\`java id="fl1yme"
@Repository
@RequiredArgsConstructor
public class SvCustomerDao {

private final SvCustomerMapper mapper;

public Optional<CustomerSummaryRow>
findCustomerSummary(
CustomerSummaryQuery query) {

return Optional.ofNullable(
mapper.selectCustomerSummary(query)
);
}
}

## Mapper

\`\`\`java id=“2f84k7” @Mapper public interface SvCustomerMapper {

CustomerSummaryRow selectCustomerSummary(
CustomerSummaryQuery query
);

}


\---

\# 금지 예시

\`\`\`text id="v7engz"
Handler가 Mapper를 직접 호출한다.

Facade가 일부 SQL을 직접 실행한다.

Service가 SQL 문자열을 만든다.

Service가 StandardResponse를 직접 생성한다.

Service가 JWT 문자열을 직접 파싱한다.

Rule이 DAO나 외부 API를 호출한다.

DAO가 고객 미존재 업무 메시지를 결정한다.

Mapper XML에서 사용자 권한을 임의 판단한다.

DB Row를 Response DTO로 직접 반환한다.

조회 결과 null을 빈 고객 객체로 바꾼다.

DB 오류를 결과 없음으로 바꾼다.

단건 다중 결과에서 첫 행만 반환한다.

UPDATE 영향 행 수를 확인하지 않는다.

금액과 건수를 Object로 반환한다.

SQL Parameter에 \`${사용자입력}\`을 사용한다.

외부 시스템 호출을 DB Lock 안에서 장시간 기다린다.

다른 업무 WAR의 Mapper를 직접 Import한다.

다른 도메인 테이블을 직접 UPDATE한다.

목록 정렬에 유일한 정렬키가 없다.

전체 조회에 최대 건수 제한이 없다.

Mapper SQL만 테스트하고 Service 업무 의미를 테스트하지 않는다.

H2 테스트 성공만으로 Oracle 운영 호환을 확정한다.

예외 원인을 제거한 새 RuntimeException만 발생시킨다.

# 책임 경계와 RACI

| 활동 | 업무개발 | AA | DA·DBA | FW | 보안 | QA | 운영 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Service 흐름 설계 | R | A | C | C | C | C | I |
| Rule 설계 | R | A | C | I | C | C | I |
| 데이터 소유권 | C | A | R | I | I | I | C |
| DAO 설계 | R | A | C | I | I | C | I |
| Mapper·SQL | R | C | A/R | I | C | C | I |
| 트랜잭션 경계 | R | A | C | C | I | C | C |
| Timeout | R | A | C | C | I | C | R |
| 결과 없음 정책 | R | A | C | I | C | C | I |
| 권한·마스킹 | C | C | I | C | A/R | C | C |
| SQL 실행계획 | C | C | A/R | I | I | C | C |
| 단위테스트 | R | C | I | I | I | A/C | I |
| 통합테스트 | R | C | C | C | C | A/R | C |
| 운영로그 | C | C | I | R | C | C | A/R |

# 데이터 및 상태관리

## 조회 거래

조회 거래는 DB 상태를 변경하지 않지만 다음 상태는 생성될 수 있다.

text id="kjmnv6" 거래로그 감사로그 Metric Cache 접근이력

## 변경 거래

text id="cpi06f" 현재 상태 조회 ↓ 변경 가능 여부 Rule ↓ Version 확인 ↓ Update ↓ 영향 행 수 확인 ↓ 감사로그

## 상태 전이

text id="045aq3" REQUESTED → APPROVED → RUNNING → COMPLETED

허용되지 않은 전이는 Rule 또는 Domain Object에서 거절한다.

Mapper SQL의 WHERE 조건만으로 상태 전이 규칙을 숨기지 않는다.

# 성능·용량·확장성

| 항목 | 기준 |
| --- | --- |
| 단건 SQL | 초기 Slow 기준 1초 검토 |
| 목록 SQL | 초기 Slow 기준 2초 검토 |
| 전체 거래 | p95 3초 |
| Query Timeout | 2초 내외 |
| 최대 목록 | 화면 기준 100건 |
| 정렬 | 안정된 유일키 포함 |
| Index | 조회조건·Join 검토 |
| DB Pool | 장시간 점유 방지 |
| 외부 호출 | Timeout 예산 분리 |
| Cache | 최신성·무효화 정의 |

특정 SQL의 Slow 기준은 거래 특성과 운영환경을 기준으로 OM 또는 환경설정에서 관리한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| Query 조건 | 검증된 권한범위 반영 |
| SQL Injection | #{} Binding 사용 |
| 동적 정렬 | 허용 Enum만 사용 |
| 최소 조회 | 필요한 개인정보 컬럼만 SELECT |
| Row·Response | 직접 노출 금지 |
| 마스킹 | Service·Assembler에서 적용 |
| 로그 | SQL Parameter 원문 제한 |
| 감사 | 중요 조회·변경 기록 |
| 데이터 소유권 | 소유 도메인만 변경 |
| 대량조회 | 호출량·다운로드 통제 |

# 운영·모니터링·장애 대응

권장 SQL 실행 로그:

text id="ef4j6l" guid traceId businessCode serviceId mapperStatementId sqlType elapsedMs rowCount dbPoolName result errorCode appVersion

금지:

text id="l1nqvr" SQL Parameter 전체 주민번호 계좌번호 고객명 원문 JWT 비밀번호

장애 확인 순서:

text id="jgm68s" GUID ↓ ServiceId ↓ Service 단계 ↓ Mapper Statement ID ↓ 실행시간·Row Count ↓ DB Pool ↓ DB Lock·실행계획

# 자동검증 및 품질 Gate

## 1\. 계층 의존성

\`\`\`text id=“ks9rpq” Handler → DAO·Mapper 금지

Facade → Mapper 금지

Rule → DAO·Mapper 금지

DAO → Response DTO 금지

Mapper → Service·Rule 금지


\---

\## 2. 트랜잭션 위치

\`\`\`text id="eqa0bm"
@Transactional
→ Facade 패키지만 허용

예외가 필요한 경우 ADR과 승인 근거를 남긴다.

## 3\. Mapper 정합성

\`\`\`text id=“1jhatp” Interface 전체명 = XML namespace

Method명 = Statement ID

Parameter Type = XML parameterType

Return Type = resultType·resultMap


\---

\## 4. SQL 품질

검출:

\`\`\`text id="c2qi83"
SELECT \*
${사용자입력}
조건 없는 UPDATE·DELETE
정렬 없는 페이징
최대 건수 없는 목록
Query Timeout 누락
민감컬럼 과다 조회

## 5\. 영향 행 수

등록·변경·삭제 거래는 기대 영향 행 수를 Service에서 검증한다.

## 6\. 데이터 소유권

다른 업무 WAR의 Mapper Import와 다른 도메인 테이블 직접 변경을 차단한다.

## 7\. 오류 처리

text id="x6jmp7" Exception 무시 금지 null 반환으로 오류 은닉 금지 원인 예외 제거 금지 Response에 SQL·Stack Trace 금지

## 8\. 운영 추적성

text id="9kq6n9" ServiceId ↔ Service Method ↔ DAO Method ↔ Mapper Statement ID ↔ Table ↔ Test ↔ Runtime Log

TCF 설계의 CI/CD 기준에도 Handler·Rule 계층 위반, 트랜잭션 위치, 오류 응답과 거래로그 완결성을 자동 검증하도록 정의되어 있다.

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| SRV-001 | 정상 고객 단건 조회 | Response 정상 |
| SRV-002 | 고객 미존재 | 업무 오류 |
| SRV-003 | 선택 등급 미존재 | 정상 null |
| SRV-004 | 상품 목록 0건 | 빈 목록 |
| SRV-005 | 고객 중복 2건 | 정합성 오류 |
| SRV-006 | 일반 사용자 조회 | 마스킹 |
| SRV-007 | 원문권한 사용자 | 승인 필드 표시 |
| SRV-008 | 타 지점 고객 | 데이터권한 오류 |
| SRV-009 | Mapper null | DAO Optional.empty |
| SRV-010 | DB 연결 실패 | 시스템 오류 |
| SRV-011 | SQL 문법 오류 | 시스템 오류·원인 보존 |
| SRV-012 | Query Timeout | Timeout 오류 |
| SRV-013 | DB Lock | 제한시간 내 종료 |
| SRV-014 | Update 1건 | 성공 |
| SRV-015 | Update 0건 | 미존재·동시성 오류 |
| SRV-016 | Update 2건 | 정합성 오류 |
| SRV-017 | Duplicate Key | 중복 오류 정책 |
| SRV-018 | 목록 최대건수 | 상한 준수 |
| SRV-019 | 목록 정렬 동일값 | 중복·누락 없음 |
| SRV-020 | SQL Injection 입력 | Parameter Binding |
| SRV-021 | 동적 정렬 공격 | 허용값 외 거절 |
| SRV-022 | Mapper Namespace 오류 | 테스트 실패 |
| SRV-023 | Statement ID 오류 | 테스트 실패 |
| SRV-024 | Result Mapping 오류 | 테스트 실패 |
| SRV-025 | 금액 Decimal | 정밀도 유지 |
| SRV-026 | 날짜 타입 | 날짜 정상 매핑 |
| SRV-027 | Transaction 성공 | Commit |
| SRV-028 | 중간 예외 | 전체 Rollback |
| SRV-029 | Timeout | Rollback·Connection 반환 |
| SRV-030 | 외부 호출 지연 | 전체 예산 준수 |
| SRV-031 | 다른 도메인 Mapper 접근 | Architecture Gate 실패 |
| SRV-032 | Rule의 DAO 접근 | Architecture Gate 실패 |
| SRV-033 | Handler의 Mapper 접근 | Architecture Gate 실패 |
| SRV-034 | 개인정보 로그 | 원문 미기록 |
| SRV-035 | GUID 추적 | Service–SQL 연결 |
| SRV-036 | Slow SQL | 기준 초과 로그 |
| SRV-037 | H2 SQL 성공 | Oracle 호환 추가검증 |
| SRV-038 | 반복 단건조회 | Rate·성능 확인 |
| SRV-039 | Negative Cache | 신규 데이터 반영 |
| SRV-040 | 다른 개발자 재현 | 동일 결과 |

# 따라 하는 실무 절차

## 1단계. 거래와 데이터 소유권을 확인한다

완료 증적:

text id="ig7t8q" ServiceId 업무 도메인 소유 Table 허용 접근방식

## 2단계. Service 처리 순서를 작성한다

text id="iyw7dt" Query 생성 → 데이터 조회 → Rule → 결과 조합 → Response

## 3단계. 결과 없음 정책을 정한다

text id="y13mqs" 필수 단건 선택 단건 목록

## 4단계. DAO·Repository 구조를 선택한다

\`\`\`text id=“nb0146” 단순 조회 → DAO + Mapper

복잡한 Aggregate → Repository Interface + 구현체


\---

\## 5단계. Mapper 계약을 작성한다

\`\`\`text id="wo3729"
Method
Parameter
Return
Namespace
Statement ID

## 6단계. SQL을 작성한다

완료 증적:

text id="tp4hmq" 대상 Table 조회조건 정렬 Index Timeout 예상 건수

## 7단계. 영향 행 수 정책을 정의한다

text id="7ws4fc" 예상 1건 0건 처리 2건 이상 처리

## 8단계. Service·Rule 단위테스트를 작성한다

text id="5ilcdd" 정상 결과 없음 권한 상태 오류

## 9단계. Mapper 통합테스트를 작성한다

text id="er9p38" Parameter SQL Result Mapping 정렬 영향 행 수

## 10단계. Facade 트랜잭션을 검증한다

text id="8j6thz" readOnly timeout rollback connection 반환

## 11단계. TCF 통합거래를 호출한다

완료 증적:

text id="57b18w" StandardResponse GUID 거래로그 Mapper Statement 로그 DB 결과

## 12단계. 품질 Gate를 통과한다

text id="0cf23n" 계층 트랜잭션 SQL 보안 추적성 테스트

# 완료 체크리스트

## Service

| 확인 항목 | 완료 |
| --- | --- |
| Service의 업무 처리 순서가 명확하다. | □ |
| Request를 Query·Command로 변환한다. | □ |
| 인증 Context를 신뢰값으로 사용한다. | □ |
| Rule과 DAO의 책임을 구분했다. | □ |
| 결과 없음의 의미를 Service가 결정한다. | □ |
| 여러 결과의 필수·선택 여부가 정의됐다. | □ |
| Response DTO를 Service·Assembler에서 만든다. | □ |
| 예외를 빈 결과로 숨기지 않는다. | □ |
| 외부 호출과 DB Transaction 범위를 검토했다. | □ |

## DAO·Repository

| 확인 항목 | 완료 |
| --- | --- |
| DAO가 Mapper를 은닉한다. | □ |
| DAO에 업무 판단이 없다. | □ |
| 단순 거래와 Aggregate Repository를 구분했다. | □ |
| Parameter 타입이 명확하다. | □ |
| Result 타입이 명확하다. | □ |
| 기술 예외 원인을 보존한다. | □ |
| 다른 업무의 Mapper를 직접 사용하지 않는다. | □ |

## Mapper·SQL

| 확인 항목 | 완료 |
| --- | --- |
| Namespace와 Interface가 일치한다. | □ |
| Statement ID와 Method가 일치한다. | □ |
| SELECT \*를 사용하지 않는다. | □ |
| ${사용자입력}을 사용하지 않는다. | □ |
| Query Timeout이 정의됐다. | □ |
| 조회조건과 Index를 확인했다. | □ |
| 정렬키가 안정적이다. | □ |
| 최대 조회 건수가 있다. | □ |
| 개인정보 컬럼을 최소 조회한다. | □ |
| 영향 행 수를 반환한다. | □ |

## 결과 없음·정합성

| 확인 항목 | 완료 |
| --- | --- |
| 필수 단건의 0건 정책이 있다. | □ |
| 선택 단건의 0건 정책이 있다. | □ |
| 목록은 빈 목록을 반환한다. | □ |
| 단건 다중 결과를 오류 처리한다. | □ |
| Null을 빈 업무객체로 위장하지 않는다. | □ |
| 권한 없음과 미존재 노출정책이 있다. | □ |
| Negative Cache 정책이 있다. | □ |

## 트랜잭션·운영

| 확인 항목 | 완료 |
| --- | --- |
| 트랜잭션이 Facade에 있다. | □ |
| 조회에 readOnly를 적용했다. | □ |
| 전체 Timeout과 Query Timeout이 정합하다. | □ |
| 오류 시 Rollback을 검증했다. | □ |
| Connection 반환을 확인했다. | □ |
| GUID·ServiceId·Statement ID가 연결된다. | □ |
| Slow SQL 로그가 있다. | □ |
| 민감 Parameter를 로그에 남기지 않는다. | □ |

## 테스트·품질

| 확인 항목 | 완료 |
| --- | --- |
| Rule 단위테스트가 있다. | □ |
| Service 단위테스트가 있다. | □ |
| Mapper 통합테스트가 있다. | □ |
| 결과 없음 테스트가 있다. | □ |
| 영향 행 수 테스트가 있다. | □ |
| 권한·마스킹 테스트가 있다. | □ |
| Timeout·Rollback 테스트가 있다. | □ |
| 운영 DB 호환성을 검증했다. | □ |
| 계층 Architecture Test가 있다. | □ |
| SQL 품질 Gate를 통과했다. | □ |

# 변경·호환성·폐기 관리

## Service 변경

다음 변경은 영향분석 대상이다.

\`\`\`text id=“eqma2x” 결과 없음 정책 변경

DAO 호출 순서 변경

필수·선택 데이터 변경

외부 호출 추가

권한 정책 변경

Response 조립 변경

예외코드 변경


\---

\## DAO·Mapper 변경

\`\`\`text id="al1akm"
Parameter 타입 변경
Return 타입 변경
Statement ID 변경
Table 변경
Join 추가
조회조건 변경
정렬 변경
Timeout 변경

영향 ServiceId와 화면·Batch·외부 소비자를 확인한다.

## SQL 변경

SQL 변경 전후에 다음을 비교한다.

text id="d59b2i" 결과 건수 Null 처리 정렬 실행계획 응답시간 영향 행 수 권한조건 개인정보 컬럼

## Table·Column 변경

text id="r8xb8n" Mapper → DAO → Service → Response → 화면 → Batch·BI

정방향과 역방향 영향을 모두 분석한다.

## Repository 폐기

text id="khgdxo" 신규 호출 중지 ↓ Service 호출자 조사 ↓ Mapper·SQL 사용량 확인 ↓ 대체 경로 전환 ↓ 테스트·문서 제거 ↓ 코드 폐기

# 시사점

## 핵심 아키텍처 판단

첫째, Service는 DB 접근 계층이 아니라 업무 처리 계층이다.

둘째, Repository는 무조건 Interface를 추가하는 패턴이 아니다.

\`\`\`text id=“dpuqvg” 단순 조회 → DAO + Mapper

복잡한 Aggregate → Repository Interface



셋째, 조회 결과가 없다는 사실과 그 업무 의미는 다르다.

\`\`\`text id="3c4x60"
DB 0건
→ Service가 정상·오류를 판단

넷째, 트랜잭션 경계는 SQL 개수가 아니라 하나의 업무 불변식을 보장해야 하는 범위로 정한다.

다섯째, Mapper와 SQL은 데이터를 읽고 쓰는 방법을 담당하지만 권한·상태·업무 성공 여부를 결정하지 않는다.

여섯째, 단건 조회에서 여러 건이 반환되는 것은 편의상 첫 행을 선택할 문제가 아니라 데이터 정합성 문제일 수 있다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| Service의 SQL 직접 실행 | 계층·테스트 훼손 |
| DAO의 업무 판단 | 오류 의미 혼재 |
| Rule의 DAO 호출 | 순수성 상실 |
| Mapper의 권한 판단 | 정책 은닉 |
| Row 직접 응답 | DB·화면 강결합 |
| 결과 없음 숨김 | 장애를 정상으로 오인 |
| 영향 행 수 미검증 | 변경 실패 누락 |
| 단건 중복 무시 | 데이터 오류 은폐 |
| 외부 호출을 Lock 안에서 대기 | 장애 전파·Pool 고갈 |
| Query Timeout 없음 | Thread·Connection 장기 점유 |
| 데이터 소유권 위반 | 업무 경계 붕괴 |
| H2 테스트만 수행 | 운영 DB 오류 |
| SQL Parameter 원문 로그 | 개인정보 유출 |
| 비유일 정렬 | 페이지 중복·누락 |
| 전체 조회 허용 | DB·Heap 부하 |

## 우선 보완 과제

1.  Facade 트랜잭션 위치를 Architecture Gate로 강제한다.
2.  Service·Rule·DAO·Mapper 책임을 ArchUnit으로 검증한다.
3.  DAO 반환의 Optional 사용 기준을 정한다.
4.  결과 없음 정책을 거래설계서에 필수화한다.
5.  변경 SQL의 영향 행 수 검증을 표준화한다.
6.  MyBatis Statement ID·실행시간·Row Count 로깅을 추가한다.
7.  Query Timeout과 TCF Timeout 정합성을 자동검사한다.
8.  다른 업무 WAR Mapper 직접 참조를 차단한다.
9.  DB Row와 Response DTO 직접 결합을 차단한다.
10.  SQL Injection·SELECT \*·무조건 UPDATE 정적검사를 적용한다.
11.  Oracle 호환 Mapper 통합테스트 환경을 마련한다.
12.  데이터 소유권과 허용 접근방식을 기준정보화한다.
13.  Slow SQL을 ServiceId·GUID와 자동 연결한다.
14.  목록 조회의 최대 건수와 안정된 정렬키를 표준화한다.
15.  외부 호출이 포함된 트랜잭션을 별도 리뷰 대상으로 지정한다.

## 중장기 발전 방향

text id="pwcn1x" 수동 DAO·Mapper 구현 ↓ 표준 Query·Command·Result 템플릿 ↓ Mapper 정합성 자동검사 ↓ SQL·ServiceId 자동 추적 ↓ 계층·트랜잭션 자동 Gate ↓ 실행계획·성능 회귀검사 ↓ 운영 Slow SQL 기반 개선 추천 ↓ 데이터 소유권 기반 접근 자동통제

# 마무리말

Service와 Repository를 구현하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id=“h6x9rq” 어떤 업무 처리 순서가 필요한가?

어떤 데이터가 필수이고 어떤 데이터가 선택인가?

조회 결과가 없으면 정상인가, 오류인가?

누가 데이터권한을 판단하는가?

어느 도메인이 데이터를 소유하는가?

트랜잭션은 어디에서 시작하고 끝나는가?

SQL은 어떤 Parameter와 Result를 사용하는가?

변경 결과가 0건이면 무슨 의미인가?

DB 오류와 업무 오류를 어떻게 구분하는가?

어떤 테스트로 데이터 정합성을 증명하는가?

운영에서 어떤 GUID와 Statement ID로 추적하는가?



제15장의 핵심 흐름은 다음과 같다.

\`\`\`text id="1eh3v0"
Request
↓
Facade Transaction
↓
Service 업무 흐름
↓
Rule 업무 판단
↓
DAO·Repository
↓
Mapper·SQL
↓
DB Result
↓
Service 결과 의미 판단
↓
Response

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“qocb5d” Mapper는 데이터를 반환한다.

Service는 그 데이터가 업무적으로 무엇을 의미하는지 판단한다.

이 두 책임을 섞지 않아야 변경·테스트·운영이 안전해진다. \`\`\`

다음 장에서는 지금까지 만든 Request DTO, Handler, Facade, Service, DAO와 Mapper를 하나의 ServiceId에 등록하고, 실제 로컬 환경에서 표준 요청을 호출하여 거래로그와 응답을 확인한다.
