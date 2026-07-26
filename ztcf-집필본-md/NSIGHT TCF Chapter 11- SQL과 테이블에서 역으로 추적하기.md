<!-- source: ztcf-집필본/NSIGHT TCF Chapter 11- SQL과 테이블에서 역으로 추적하기.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제11장. SQL과 테이블에서 역으로 추적하기

## 이 장을 시작하며

제10장에서는 화면 ID와 ServiceId에서 시작하여 Handler·Facade·Service·DAO·Mapper·SQL과 테이블까지 정방향으로 처리 경로를 추적했다.

이번 장에서는 반대 방향으로 이동한다.

개발자와 운영자가 받는 요청은 항상 화면에서 시작하지 않는다.

SV\_CUSTOMER 테이블의 컬럼을 변경해야 합니다.

이 SQL이 어느 화면에서 사용되는지 확인해 주세요.

운영에서 Slow SQL이 발견됐는데 영향 거래가 무엇입니까?

이 테이블을 폐기해도 되는지 조사해 주세요.

인덱스를 변경하면 어떤 ServiceId를 다시 테스트해야 합니까?

특정 테이블을 UPDATE하는 프로그램을 모두 찾아 주세요.

DBA가 전달한 SQL ID가 어느 업무 프로그램인지 확인해 주세요.

이러한 요청에서는 화면 이름이나 ServiceId를 먼저 알 수 없다.

출발점은 다음 중 하나다.

Table
Column
View
Index
Mapper XML
Mapper Statement ID
SQL 문장
Stored Procedure
Slow SQL 로그
DB Lock 정보

따라서 정방향 추적과 반대되는 다음 흐름을 사용한다.

DB 객체
↓
Mapper XML
↓
Mapper Statement ID
↓
Mapper Interface
↓
DAO
↓
Service
↓
Facade
↓
Handler
↓
ServiceId
↓
화면 이벤트
↓
영향 사용자·운영정책·테스트

현재 소스의 고객요약 조회 거래는 다음과 같이 실제 역추적할 수 있다.

SV\_CUSTOMER
↓
SvCustomerMapper.xml
↓
selectCustomerSummary
↓
SvCustomerMapper
↓
SvCustomerDao
↓
SvCustomerService
↓
SvCustomerFacade
↓
SvCustomerHandler
↓
SV.Customer.selectSummary
↓
SV-CUS-0001 고객 종합정보 화면

이러한 역추적 경로는 화면–ServiceId–프로그램–SQL–DB 객체 추적성의 반대 방향이며, 테이블이나 SQL 변경 전에 영향 범위를 식별하는 핵심 수단이다.

그러나 단순 문자열 검색만으로는 정확한 영향 범위를 찾기 어렵다.

동적 SQL
공통 SQL 조각
View·Synonym
Stored Procedure
DB Link
Cache
Batch 생성 테이블
다른 업무 WAR의 직접 조회
외부 시스템 API
환경별 Mapper
런타임 분기

이러한 구조에서는 테이블명이 현재 Java 소스에 직접 나타나지 않을 수 있다.

따라서 역추적 완료 기준은 검색 결과의 개수가 아니다.

정적 검색
\+ Mapper 구조 확인
\+ Call Hierarchy
\+ 트랜잭션 경계 확인
\+ 런타임 Statement ID
\+ GUID·TraceId 로그
\+ SQL 실행 결과
\+ 영향 화면·테스트 확인

위 증거가 연결되어야 역추적이 완료된다.

## 핵심 관점

SQL과 테이블을 변경하는 일은
DB 객체 한 개만 수정하는 일이 아니다.

그 데이터를 사용하는
업무 규칙,
트랜잭션,
ServiceId,
화면,
배치,
외부 연계,
권한,
감사,
운영 절차를 함께 변경하는 일이다.

## 학습 목표

이 장을 마치면 다음 내용을 직접 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | Mapper Namespace와 Statement ID를 구분한다. |
| 2 | MyBatis Mapper Interface와 XML을 연결한다. |
| 3 | SQL ID와 ServiceId를 구분한다. |
| 4 | Mapper XML에서 DAO·Service 호출자를 찾는다. |
| 5 | 테이블명으로 전체 저장소의 사용 프로그램을 검색한다. |
| 6 | 조회·등록·변경·삭제 SQL을 구분한다. |
| 7 | 하나의 테이블을 사용하는 여러 ServiceId를 식별한다. |
| 8 | ServiceId를 호출하는 화면과 이벤트를 찾는다. |
| 9 | View·Synonym·Procedure를 통한 간접 사용을 찾는다. |
| 10 | 공통 Mapper와 SQL Fragment의 실제 사용처를 찾는다. |
| 11 | 동적 SQL의 조건별 실행 경로를 설명한다. |
| 12 | 해당 SQL의 트랜잭션 경계를 확인한다. |
| 13 | UPDATE·DELETE 영향 행 수의 의미를 설명한다. |
| 14 | 테이블 데이터 소유권과 직접 접근 위반을 판단한다. |
| 15 | 다른 업무 WAR와 외부 시스템 연계를 식별한다. |
| 16 | SQL Timeout과 전체 거래 Timeout의 관계를 확인한다. |
| 17 | 정적 검색 결과를 실행 로그와 테스트로 검증한다. |
| 18 | 테이블 변경의 영향 화면·거래·배치·운영 항목을 작성한다. |
| 19 | 미사용 SQL과 미사용 테이블의 폐기 여부를 판단한다. |
| 20 | SQL·테이블 변경 품질 Gate를 구성한다. |

# 한눈에 보는 역추적 흐름

┌────────────────────────────────────────────────────────────┐
│ 1. DB 객체 │
│ SV\_CUSTOMER · CUSTOMER\_NO · INDEX · VIEW │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. Mapper XML │
│ FROM·JOIN·INSERT·UPDATE·DELETE에서 객체 검색 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. MyBatis Statement │
│ Namespace + Statement ID │
│ SvCustomerMapper.selectCustomerSummary │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. Mapper Interface │
│ selectCustomerSummary(criteria) │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. DAO │
│ SvCustomerDao.selectCustomerSummary() │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. Service·Rule │
│ 업무 규칙·결과 판단·상태 전이 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 7. Facade │
│ 트랜잭션·Timeout·유스케이스 경계 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 8. Handler │
│ ServiceId 등록과 분기 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 9. ServiceId │
│ SV.Customer.selectSummary │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 10. 화면·Batch·외부 호출자 │
│ 화면 이벤트·Scheduler·다른 업무 WAR │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 11. 운영 영향 │
│ Catalog·Timeout·권한·감사·로그·테스트·배포 │
└────────────────────────────────────────────────────────────┘

# 정방향과 역방향 추적 비교

| 구분 | 정방향 추적 | 역방향 추적 |
| --- | --- | --- |
| 출발점 | 화면·ServiceId | SQL·테이블·컬럼 |
| 목적 | 거래가 어디로 실행되는지 확인 | 데이터 변경의 영향 범위 확인 |
| 핵심 식별자 | ServiceId | Mapper Statement ID·DB 객체 |
| 주요 경로 | Handler → Mapper | Mapper → Handler |
| 주요 질문 | 이 화면이 어떤 SQL을 실행하는가 | 이 테이블을 어떤 화면이 사용하는가 |
| 운영 활용 | 장애 실행경로 확인 | Slow SQL·DB 변경 영향분석 |
| 완료 증적 | Call Stack·GUID 로그 | SQL 로그·영향 매트릭스·회귀테스트 |

# SQL 관련 식별자 구분

SQL을 추적할 때 다음 식별자를 혼동하지 않아야 한다.

| 구분 | 예시 | 역할 |
| --- | --- | --- |
| ServiceId | SV.Customer.selectSummary | 업무 거래 식별 |
| Mapper Interface | SvCustomerMapper | Java SQL 실행 계약 |
| Mapper Method | selectCustomerSummary | Java 호출 메서드 |
| Mapper Namespace | com.nh...SvCustomerMapper | XML과 Interface 연결 |
| Statement ID | selectCustomerSummary | Mapper XML 문장 식별 |
| 완전한 Statement ID | com.nh...SvCustomerMapper.selectCustomerSummary | MyBatis 런타임 식별 |
| SQL Catalog ID | 프로젝트 정의값 | 설계·운영 SQL 관리번호 |
| DB 객체 | SV\_CUSTOMER | 물리 데이터 객체 |
| 거래코드 | SV-INQ-0001 | 운영 통제·감사 분류 |

현재 구현 예시의 Mapper XML에는 <select id="selectCustomerSummary">가 실제 MyBatis Statement ID로 사용되고, 주석에는 SV.Customer.selectSummary가 추적용 SQL\_ID로 표시되어 있다.

그러나 아키텍처적으로는 다음 세 가지를 명확히 구분하는 것이 안전하다.

ServiceId
\= 업무 거래 식별자

Mapper Statement ID
\= MyBatis SQL 실행 식별자

SQL Catalog ID
\= 설계·운영 관리 식별자

한 값을 세 목적에 혼용하면 SQL 하나를 여러 ServiceId가 공유하거나 ServiceId가 여러 SQL을 호출할 때 관계를 정확하게 표현할 수 없다.

# 현재 소스 기준 구현 상태

| 항목 | 상태 | 확인 내용 |
| --- | --- | --- |
| SvCustomerMapper.xml | 구현 확인 | SV\_CUSTOMER 조회 |
| Mapper Namespace | 구현 확인 | SvCustomerMapper FQCN과 연결 |
| Statement ID | 구현 확인 | selectCustomerSummary |
| SQL Timeout | 구현 확인 | XML timeout="3" |
| Mapper Interface | 구현 확인 | 동일 Method 선언 |
| DAO | 구현 확인 | Mapper 호출 위임 |
| Service | 구현 확인 | Rule·DAO 호출과 결과 검증 |
| Facade | 구현 확인 | readOnly=true, timeout=3 |
| Handler | 구현 확인 | SV.Customer.selectSummary 등록 |
| 로컬 DB | 구현 확인 | H2 SV\_CUSTOMER |
| 운영 DB | 프로젝트 확인 필요 | RDW 다중 테이블 JOIN 전환 필요 |
| SQL Catalog | 부분 구현 | 주석 기반 식별, 공식 Catalog 보완 필요 |
| SQL 런타임 추적 | 권장 확장 | Statement ID·시간·행 수 공통 수집 |
| 자동 영향분석 | 권장 확장 | Mapper·Table·ServiceId 연결 자동 생성 |

# 11.1 Mapper XML에서 호출자 찾기

## 11.1.1 Mapper XML을 먼저 읽는 이유

Mapper XML에는 다음 정보가 한곳에 모여 있다.

SQL 종류
조회·변경 테이블
입력 Parameter
출력 Result
동적 조건
Join
정렬
페이징
Timeout
공통 SQL Fragment

예:

<mapper namespace=
"com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">

<select id="selectCustomerSummary"
parameterType=
"com.nh.nsight.marketing.sv.application.dto.customer.CustomerSummaryCriteria"
resultType=
"com.nh.nsight.marketing.sv.persistence.dto.customer.CustomerSummaryRow"
timeout="3">

SELECT
CUSTOMER\_NO
, CUSTOMER\_NAME
, CUSTOMER\_GRADE
FROM SV\_CUSTOMER
WHERE CUSTOMER\_NO = #{customerNo}
</select>

</mapper>

이 XML에서 다음 연결을 추출할 수 있다.

| XML 정보 | 연결 대상 |
| --- | --- |
| Namespace | Mapper Interface |
| Statement ID | Mapper Method |
| Parameter Type | Query·Criteria DTO |
| Result Type | Row·Result DTO |
| FROM·JOIN | 조회 테이블 |
| INSERT·UPDATE·DELETE | 변경 테이블 |
| Timeout | SQL 실행 제한시간 |

## 11.1.2 Namespace로 Mapper Interface 찾기

XML:

<mapper namespace=
"com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">

Java:

@Mapper
public interface SvCustomerMapper {

CustomerSummaryRow selectCustomerSummary(
CustomerSummaryCriteria criteria
);
}

확인 조건:

XML Namespace
\= Mapper Interface의 전체 패키지명

XML Statement ID
\= Mapper Method 이름

불일치 시 다음 오류가 발생할 수 있다.

Invalid bound statement
Mapped Statements collection does not contain value
Mapper Bean 생성 실패
실행 시 Statement 미발견

## 11.1.3 Statement ID 검색

검색 대상:

selectCustomerSummary

명령행:

rg -F "selectCustomerSummary" .

검색 결과를 다음 순서로 분류한다.

| 검색 위치 | 의미 |
| --- | --- |
| Mapper XML | SQL 정의 |
| Mapper Interface | Java 계약 |
| DAO | 직접 호출자 |
| Service | DAO 직접 호출 가능성 |
| Test | SQL 검증 |
| 문서 | 설계 관계 |
| 로그 | 런타임 실행 증거 |

## 11.1.4 완전한 MyBatis Statement ID

MyBatis 런타임에서는 Namespace와 Statement ID를 합친 값이 사용된다.

com.nh.nsight.marketing.sv.persistence.mapper
.SvCustomerMapper.selectCustomerSummary

운영 로그나 MyBatis Interceptor에서는 가능한 한 이 완전한 값을 남기는 것이 좋다.

mapperStatement=
com.nh.nsight.marketing.sv.persistence.mapper
.SvCustomerMapper.selectCustomerSummary

이렇게 하면 서로 다른 Mapper에 동일한 Method 이름이 있어도 구분할 수 있다.

## 11.1.5 Mapper Method의 직접 호출자 찾기

IDE 기능:

Find Usages
Call Hierarchy
Go to Declaration
Go to Implementation

명령행:

rg -F "mapper.selectCustomerSummary" .

현재 구조:

@Repository
public class SvCustomerDao {

private final SvCustomerMapper mapper;

public CustomerSummaryRow selectCustomerSummary(
CustomerSummaryCriteria criteria) {
return mapper.selectCustomerSummary(criteria);
}
}

여기서 직접 호출자는 SvCustomerDao다.

## 11.1.6 DAO 호출자 찾기

검색:

selectCustomerSummary(

DAO Method와 Mapper Method 이름이 같을 수 있으므로 타입과 패키지를 함께 확인한다.

현재 구조:

CustomerSummaryRow customer =
dao.selectCustomerSummary(criteria);

직접 호출자:

SvCustomerService

## 11.1.7 Service 호출자 찾기

현재 구조:

SvCustomerService.selectCustomerSummary()

호출자:

SvCustomerFacade.selectCustomerSummary()

Facade에서는 다음을 확인한다.

@Transactional(readOnly = true, timeout = 3)
public Map<String, Object> selectCustomerSummary(...) {
...
}

확인 항목:

| 항목 | 확인 내용 |
| --- | --- |
| @Transactional | 트랜잭션 적용 여부 |
| readOnly | 조회 거래 여부 |
| timeout | 전체 DB 트랜잭션 제한 |
| 호출 Service 수 | 여러 SQL 조합 여부 |
| 예외 처리 | Rollback 조건 |
| 외부 호출 | DB Transaction 내부 포함 여부 |

## 11.1.8 Handler와 ServiceId 찾기

Facade 호출자를 따라가면 Handler에 도달한다.

private static final String SELECT\_SUMMARY =
"SV.Customer.selectSummary";

case SELECT\_SUMMARY ->
facade.selectCustomerSummary(
request.getBody(),
context
);

따라서 역추적 결과는 다음과 같다.

selectCustomerSummary SQL
↓
SvCustomerMapper
↓
SvCustomerDao
↓
SvCustomerService
↓
SvCustomerFacade
↓
SvCustomerHandler
↓
SV.Customer.selectSummary

## 11.1.9 ServiceId에서 화면 찾기

마지막으로 ServiceId 전체 검색을 수행한다.

rg -F "SV.Customer.selectSummary" .

검색 위치:

UI 요청
화면 정의서
Handler
OM Catalog
거래통제
Timeout
권한
감사
통합테스트
운영 로그

화면까지 연결되면 최종 역추적이 완성된다.

## 11.1.10 Parameter Type 추적

Mapper XML:

parameterType="CustomerSummaryCriteria"

확인:

누가 Criteria를 생성하는가?
어떤 요청 필드가 Criteria로 변환되는가?
기본값은 어디에서 적용되는가?
권한 조건은 포함되는가?
날짜와 코드가 정규화되는가?

현재 예에서는 Service가 Rule을 이용해 Criteria를 만든다.

Request DTO
↓
Rule.buildSummaryCriteria()
↓
CustomerSummaryCriteria
↓
DAO
↓
Mapper

## 11.1.11 Result Type 추적

Mapper XML:

resultType="CustomerSummaryRow"

확인:

DB Column Alias
→ Row DTO Field
→ 업무 Response DTO
→ StandardResponse Body
→ 화면 표시항목

DB 결과를 그대로 화면에 반환하지 않는다.

DB Row
↓
업무 결과 판단
↓
Response DTO
↓
화면 계약

이 분리를 통해 DB 컬럼 변경이 화면 계약에 직접 전파되는 것을 방지한다.

## 11.1.12 공통 SQL Fragment

예:

<sql id="customerBaseColumns">
CUSTOMER\_NO,
CUSTOMER\_NAME,
CUSTOMER\_GRADE
</sql>

사용:

<include refid="customerBaseColumns"/>

역추적 시 SQL Fragment의 사용처도 확인해야 한다.

SQL Fragment
↓
include 검색
↓
사용 Statement
↓
Mapper Method
↓
호출 ServiceId

Fragment 변경은 여러 Statement에 동시에 영향을 줄 수 있다.

## 11.1.13 동적 SQL

예:

<where>
<if test="customerNo != null">
CUSTOMER\_NO = #{customerNo}
</if>
<if test="branchCode != null">
AND BRANCH\_CODE = #{branchCode}
</if>
</where>

확인해야 할 위험:

조건이 모두 비어 전체조회가 되는가?
AND·OR 우선순위가 올바른가?
권한 조건이 누락될 수 있는가?
Index 사용이 가능한가?
환경별 실행계획이 달라지는가?

정적 XML만 읽지 말고 조건 조합별 생성 SQL을 테스트해야 한다.

## 11.1.14 choose·foreach

### choose

<choose>
<when test="customerNo != null">
CUSTOMER\_NO = #{customerNo}
</when>
<otherwise>
BRANCH\_CODE = #{branchCode}
</otherwise>
</choose>

### foreach

CUSTOMER\_NO IN
<foreach collection="customerNos"
item="customerNo"
open="("
separator=","
close=")">
#{customerNo}
</foreach>

확인:

빈 Collection 처리
최대 Collection 크기
DB IN 절 제한
SQL 길이
Parameter 수
대량조회 성능

## 11.1.15 Annotation Mapper

모든 SQL이 XML에 있는 것은 아니다.

@Select("""
SELECT CUSTOMER\_NO
FROM SV\_CUSTOMER
WHERE CUSTOMER\_NO = #{customerNo}
""")
CustomerRow selectCustomer(String customerNo);

또는:

@SelectProvider(
type = CustomerSqlProvider.class,
method = "buildSelectSql"
)

따라서 XML 검색 결과가 없으면 다음을 확인한다.

@Select
@Insert
@Update
@Delete
@SelectProvider
SQL Provider

## 11.1.16 Statement Type별 역추적

| SQL 종류 | 주요 확인 항목 |
| --- | --- |
| SELECT | 데이터 범위·권한·마스킹·페이징 |
| INSERT | 중복방지·PK·필수값·감사 |
| UPDATE | 상태조건·Version·영향 행 수 |
| DELETE | 물리·논리 삭제·연계 영향 |
| Procedure | OUT Parameter·Commit 책임 |
| Batch | 건수·재시작·부분 실패 |

## 11.1.17 영향 행 수 확인

변경 SQL:

int updated = mapper.updateCustomerMemo(command);

금지:

mapper.updateCustomerMemo(command);
return success;

권장 판단:

updated = 1
→ 정상

updated = 0
→ 미존재·권한·상태변경·동시수정 구분

updated > 1
→ 조건 오류·데이터 이상

영향 행 수는 데이터 정합성을 판단하는 핵심 증거다.

## 11.1.18 Mapper XML 역추적 완료 조건

Namespace 확인
\+ Statement ID 확인
\+ Mapper Interface 확인
\+ DAO 호출 확인
\+ Service·Facade 확인
\+ Handler·ServiceId 확인
\+ 화면·Batch·외부 호출 확인
\+ 실행 로그 확인

# 11.2 테이블 사용 프로그램 찾기

## 11.2.1 테이블명 전체 검색

예:

SV\_CUSTOMER

검색:

rg -i "\\bSV\_CUSTOMER\\b" .

검색 범위:

Java
Mapper XML
SQL Script
DDL
View
Procedure
Batch
Test
문서
운영 Script
BI Report

생성파일과 백업파일은 구분한다.

## 11.2.2 검색 결과를 사용유형으로 분류

| 유형 | SQL 예 | 영향 |
| --- | --- | --- |
| 조회 | SELECT | 응답·보고서·배치 입력 |
| 등록 | INSERT | 신규 데이터 생성 |
| 변경 | UPDATE | 상태·금액·속성 변경 |
| 삭제 | DELETE | 데이터 제거 |
| 병합 | MERGE | 등록·변경 복합 |
| DDL | CREATE·ALTER·DROP | 구조 변경 |
| View | CREATE VIEW | 간접 조회 |
| Procedure | 내부 SQL | 프로그램 외부 변경 |
| Trigger | 자동 변경 | 숨은 부작용 |
| Batch | 대량 처리 | 시간대·재시작 영향 |
| Report | BI·통계 | 사용자 조회 영향 |

## 11.2.3 컬럼명 검색

테이블명 검색만으로 부족할 때 컬럼명도 찾는다.

CUSTOMER\_GRADE
LAST\_TRANSACTION\_DATE

주의:

동일 컬럼명이 여러 테이블에 존재할 수 있다.

Column Alias가 다른 이름으로 사용될 수 있다.

SELECT \* 사용 시 컬럼명이 SQL에 나타나지 않는다.

SELECT \*는 영향 분석과 계약 안정성을 어렵게 하므로 운영 SQL에서는 지양한다.

## 11.2.4 Alias가 있는 SQL

SELECT C.CUSTOMER\_NO
FROM SV\_CUSTOMER C

테이블명은 한 번만 나오고 이후에는 Alias만 사용된다.

따라서 테이블 검색 결과를 찾은 뒤 전체 Statement를 읽어야 한다.

## 11.2.5 Join 테이블 확인

SELECT ...
FROM SV\_CUSTOMER C
JOIN SV\_CUSTOMER\_GRADE G
ON C.CUSTOMER\_NO = G.CUSTOMER\_NO
LEFT JOIN SV\_CUSTOMER\_PRODUCT P
ON C.CUSTOMER\_NO = P.CUSTOMER\_NO

이 SQL은 세 테이블의 변경 영향을 받는다.

SV\_CUSTOMER 변경
SV\_CUSTOMER\_GRADE 변경
SV\_CUSTOMER\_PRODUCT 변경

각 Join에 대해 다음을 확인한다.

| 항목 | 확인 |
| --- | --- |
| Join Key | PK·FK·업무키 |
| Join Type | INNER·LEFT |
| Cardinality | 1:1·1:N |
| 중복 행 | 집계 왜곡 가능성 |
| Index | Join 성능 |
| 데이터 기준시각 | Snapshot 일치 |
| 소유 도메인 | 직접 Join 허용 여부 |

## 11.2.6 View를 통한 간접 사용

SELECT \*
FROM VW\_SV\_CUSTOMER\_SUMMARY

View 정의:

CREATE VIEW VW\_SV\_CUSTOMER\_SUMMARY AS
SELECT ...
FROM SV\_CUSTOMER C
JOIN SV\_CUSTOMER\_GRADE G ...

역추적:

SV\_CUSTOMER
↓
View 정의
↓
VW\_SV\_CUSTOMER\_SUMMARY
↓
Mapper SQL
↓
ServiceId

테이블 사용 프로그램을 조사할 때 View 정의를 반드시 포함한다.

## 11.2.7 Synonym

SELECT ...
FROM CUSTOMER\_SUMMARY

실제 객체:

CUSTOMER\_SUMMARY
→ Synonym
→ RDW\_OWNER.SV\_CUSTOMER\_SUMMARY

애플리케이션 SQL에 물리 테이블명이 직접 나타나지 않을 수 있다.

DBA에게 다음 정보를 확인한다.

Synonym 대상
Owner
DB Link
권한
환경별 차이

## 11.2.8 Stored Procedure

<select id="callCustomerSummary"
statementType="CALLABLE">
{ CALL PKG\_CUSTOMER.SELECT\_SUMMARY(
#{customerNo},
#{result, mode=OUT}
) }
</select>

실제 테이블은 Procedure 내부에서 사용된다.

Mapper
→ Procedure
→ Package Body
→ SQL
→ Table

Procedure 내부 Commit 여부와 예외 처리도 확인해야 한다.

## 11.2.9 Trigger

애플리케이션은 한 테이블만 UPDATE하지만 Trigger가 다른 테이블을 변경할 수 있다.

UPDATE SV\_CUSTOMER
↓
TRG\_SV\_CUSTOMER\_AU
↓
INSERT SV\_CUSTOMER\_HIST

영향 분석에 포함할 항목:

Trigger
History Table
Audit Table
Sequence
Procedure
Exception

## 11.2.10 DB Link

SELECT ...
FROM CUSTOMER@RDW\_LINK

확인:

| 항목 | 내용 |
| --- | --- |
| 원격 DB | 실제 데이터 위치 |
| 네트워크 | 지연·장애 영향 |
| Timeout | DB Link 호출 제한 |
| Transaction | 분산 트랜잭션 여부 |
| 권한 | 계정·접근권한 |
| 운영 책임 | 원격 DB 담당 조직 |

## 11.2.11 Batch 사용 여부

테이블은 온라인 거래뿐 아니라 Batch가 사용할 수 있다.

온라인 조회
Batch 적재
정산
통계
파일 생성
데이터 정제
보관·삭제

검색 대상:

Job
Step
Reader
Processor
Writer
Scheduler
MERGE
INSERT SELECT
TRUNCATE

온라인 프로그램에서 사용하지 않는다고 테이블을 미사용으로 판단해서는 안 된다.

## 11.2.12 BI·보고서 사용 여부

다음 영역도 확인한다.

BI Portal
DataEye
Report Tool
Excel Download
Ad-hoc Query
통계 Mart

DB 객체 폐기 전에는 애플리케이션 저장소 외의 소비자도 조사해야 한다.

## 11.2.13 데이터 소유권

테이블이 어느 업무 소유인지 확인한다.

| 테이블 | 권장 소유 |
| --- | --- |
| SV\_\* | SV 도메인 |
| IC\_\* | IC 도메인 |
| OM\_\* | 운영관리 |
| TCF\_\* | TCF 플랫폼 |
| JWT\_\* | 인증 |
| BATCH\_\* | 배치 운영 |

다른 도메인의 Mapper가 소유 테이블을 직접 변경하는 것은 금지한다.

IC Mapper
→ UPDATE SV\_CUSTOMER

금지

권장:

IC Service
↓
승인된 Client·ServiceId
↓
SV Service
↓
SV Mapper
↓
SV\_CUSTOMER

다른 도메인의 물리 테이블에 직접 의존하면 변경·권한·감사·장애 책임이 불명확해진다.

## 11.2.14 조회와 변경 권한 구분

다른 도메인의 Read Model 조회와 원천 테이블 변경은 다른 문제다.

| 행위 | 판단 |
| --- | --- |
| 승인된 통합 View 조회 | 조건부 허용 |
| Read Replica 조회 | 조건부 허용 |
| 승인된 API 호출 | 권장 |
| 다른 도메인 테이블 UPDATE | 금지 |
| 다른 도메인 Mapper Import | 금지 |
| 직접 DB Link 변경 | 원칙적 금지 |

## 11.2.15 상태 전이 확인

변경 테이블에서는 단순 컬럼 변경보다 상태 전이가 중요하다.

REQUESTED
→ APPROVED
→ PROCESSING
→ COMPLETED

UPDATE SQL:

UPDATE SV\_REQUEST
SET STATUS = 'APPROVED'
WHERE REQUEST\_ID = #{requestId}
AND STATUS = 'REQUESTED'

확인:

허용 이전 상태
목표 상태
권한
Version
영향 행 수
감사로그
실패 응답

## 11.2.16 낙관적 잠금

UPDATE SV\_CUSTOMER\_MEMO
SET MEMO\_TEXT = #{memoText},
VERSION\_NO = VERSION\_NO + 1
WHERE CUSTOMER\_NO = #{customerNo}
AND VERSION\_NO = #{versionNo}

updated = 0이면 다음을 구분해야 한다.

데이터 미존재
동시 수정
권한 조건 불일치
상태 변경

무조건 성공이나 미존재로 처리하지 않는다.

## 11.2.17 논리 삭제

UPDATE SV\_CUSTOMER\_MEMO
SET DELETE\_YN = 'Y'
WHERE MEMO\_ID = #{memoId}

영향 확인:

모든 조회 SQL에 DELETE\_YN 조건이 있는가?
유일 제약은 삭제 데이터와 충돌하지 않는가?
복구가 가능한가?
보존기간은 얼마인가?
개인정보 파기정책과 일치하는가?

## 11.2.18 인덱스 영향

테이블 사용 프로그램을 찾은 뒤 SQL별 조건을 분석한다.

WHERE
JOIN
ORDER BY
GROUP BY

인덱스 변경 영향:

조회 성능
변경 성능
Lock
저장공간
Batch 적재시간
통계정보

인덱스를 추가하는 것만으로 성능 개선이 보장되지는 않는다.

## 11.2.19 파티션 영향

확인:

Partition Key
조회조건 포함 여부
Partition Pruning
Local·Global Index
보관·삭제 방식
Batch 교환

파티션 키가 SQL 조건에 없으면 전체 파티션을 읽을 수 있다.

## 11.2.20 테이블 사용 매트릭스

| DB 객체 | 사용유형 | Mapper Statement | ServiceId | 호출자 |
| --- | --- | --- | --- | --- |
| SV\_CUSTOMER | SELECT | selectCustomerSummary | SV.Customer.selectSummary | 화면 |
| SV\_CUSTOMER\_GRADE | SELECT | selectCustomerGrade | SV.Customer.selectSummary | 화면 |
| SV\_CUSTOMER\_MEMO | SELECT | selectMemoList | SV.Customer.selectMemoList | 화면 |
| SV\_CUSTOMER\_MEMO | INSERT | insertMemo | SV.Customer.createMemo | 화면 |
| SV\_CUSTOMER\_MEMO | UPDATE | updateMemo | SV.Customer.updateMemo | 화면 |
| SV\_CUSTOMER\_HIST | INSERT | Trigger·Audit | 간접 | DB Trigger |
| SV\_CUSTOMER\_DAILY | MERGE | Batch Writer | Batch Job | Scheduler |

## 11.2.21 테이블 변경 영향 범위

테이블
↓
컬럼·제약·인덱스
↓
SQL Statement
↓
Mapper DTO
↓
DAO·Service
↓
ServiceId
↓
화면·Batch·외부 소비자
↓
권한·감사·성능·운영

## 11.2.22 테이블 폐기 전 확인

소스 검색 결과 0건
≠ 미사용 확정

추가 확인:

View
Synonym
Procedure
Trigger
Batch
Report
BI
DB Link
운영 SQL
Ad-hoc
최근 접근 이력

# 11.3 공통 모듈과 외부 연계 확인

## 11.3.1 로컬 Mapper가 없을 수 있다

Service가 데이터를 반환하지만 해당 업무 WAR에 Mapper가 없을 수 있다.

Service
↓
Client
↓
다른 업무 WAR
↓
원격 Mapper
↓
DB

또는:

Service
↓
Cache
↓
Cache Miss
↓
외부 API

따라서 SQL을 찾지 못했다고 데이터 처리가 없다고 판단해서는 안 된다.

## 11.3.2 Client·Adapter 검색

검색 대상:

Client
Adapter
Gateway
Eai
RestTemplate
WebClient
HttpClient
Feign

NSIGHT 권장 구조:

업무 Service
↓
업무 전용 Client
↓
tcf-eai 또는 표준 호출 모듈
↓
대상 ServiceId
↓
대상 업무 WAR

## 11.3.3 다른 업무 WAR 호출

예:

IC.Customer.selectIntegration
↓
IcCustomerService
↓
SvCustomerClient
↓
SV.Customer.selectSummary
↓
SV Mapper
↓
SV\_CUSTOMER

이 경우 SV\_CUSTOMER의 최종 사용자에는 IC 화면도 포함될 수 있다.

역추적:

SV\_CUSTOMER
↓
SV.Customer.selectSummary
↓
직접 SV 화면
+
IC·CM·CT 등 원격 호출자

## 11.3.4 원격 ServiceId 소비자 찾기

대상 ServiceId 검색:

rg -F "SV.Customer.selectSummary" .

검색 위치:

UI
Handler
Client
Contract Test
Gateway Route
OM Catalog
문서

Handler 등록뿐 아니라 다른 업무 Client의 호출도 구분한다.

## 11.3.5 외부 시스템 호출

Service
↓
ExternalClient
↓
신용정보·고객정보·상품정보 시스템

확인:

| 영역 | 확인 |
| --- | --- |
| Endpoint | 대상 시스템 |
| Contract | 요청·응답 |
| 인증 | Token·인증서 |
| Timeout | 연결·응답 |
| Retry | 대상 오류 |
| Idempotency | 중복 처리 |
| Circuit Breaker | 차단·복구 |
| Fallback | 대체 데이터 |
| Logging | TraceId |
| 개인정보 | 전달 최소화 |

## 11.3.6 Timeout 예산

전체 거래 제한이 3초인 경우:

| 구간 | 예산 예 |
| --- | --- |
| STF·공통 처리 | 200ms |
| DB 조회 | 1,200ms |
| 외부 연계 | 1,200ms |
| 결과 조립·ETF | 200ms |
| 여유 | 200ms |

금지:

TCF Timeout 3초
DB Query Timeout 5초
외부 Client 10초

하위 Timeout이 전체 Timeout보다 길면 상위 거래가 먼저 종료되고 하위 작업이 계속 실행될 위험이 있다.

## 11.3.7 재시도

재시도 대상:

일시 네트워크 오류
Connection Reset
일시적인 503
승인된 Timeout 유형

재시도 금지 또는 주의:

업무 오류
검증 실패
권한 오류
비멱등 등록
DB 변경 결과 불명확

변경 거래의 재시도에는 멱등성 키가 필요하다.

## 11.3.8 Circuit Breaker

확인:

실패율 임계치
Open 시간
Half-Open 시험
복구 기준
Fallback
운영 알림

Circuit Breaker가 열려 있으면 실제 외부 호출이 발생하지 않으므로 정적 호출 관계와 런타임 경로가 달라질 수 있다.

## 11.3.9 Cache

Service
↓
Cache 조회
├─ Hit → DB 미호출
└─ Miss → DAO·Mapper

테이블 변경 영향:

DB 변경
\+ Cache 무효화
\+ TTL
\+ 분산 Cache 일관성

DB 데이터만 수정하고 Cache를 갱신하지 않으면 화면에서 이전 데이터가 보일 수 있다.

## 11.3.10 Read Model

통합 조회는 원천 업무 테이블을 직접 JOIN하지 않고 별도 Read Model을 사용할 수 있다.

원천 도메인
↓ CDC·Batch·Event
통합 Read Model
↓
조회 Mapper
↓
화면

확인:

데이터 기준시각
적재 주기
지연 허용
원천 시스템
재처리
정합성 검증

## 11.3.11 파일과 메시지

데이터가 DB가 아니라 파일이나 메시지로 들어올 수 있다.

File
→ Batch
→ Table

Message
→ Consumer
→ Service
→ Table

역추적 시 다음도 포함한다.

File Layout
Topic
Consumer Group
Event Type
Batch Job
재처리 정책

## 11.3.12 공통 Mapper

여러 업무에서 하나의 공통 Mapper를 공유하면 편리하지만 데이터 소유권이 불명확해질 수 있다.

금지 예:

CommonMapper.selectCustomer()
CommonMapper.updateAnything()

권장:

업무 소유 Mapper
도메인 공개 Contract
Read Model 전용 Mapper

CommonMapper가 비대해지면 테이블 변경 영향이 전체 업무로 확산된다.

## 11.3.13 공통 코드 테이블

공통 코드 조회는 여러 업무에서 사용될 수 있다.

OM\_COMMON\_CODE
CC\_COMMON\_CODE

확인:

소유 모듈
Cache
변경 반영시간
다국어
사용중지
코드 삭제 영향

공통 코드 삭제는 다수 화면과 Batch에 영향을 줄 수 있다.

## 11.3.14 공통 모듈 변경 영향

| 공통 영역 | 변경 영향 |
| --- | --- |
| 공통 Mapper | 모든 소비 업무 |
| 공통 DTO | 계약 소비자 |
| 공통 SQL Fragment | 여러 Statement |
| 공통 코드 | 다수 화면 |
| 공통 Cache | 전체 데이터 조회 |
| TCF Context | 모든 거래 |
| 공통 Error Code | 화면·운영 |
| MyBatis Plugin | 모든 SQL |

## 11.3.15 외부 연계 역추적 완료 조건

Client 확인
\+ 대상 시스템 확인
\+ 원격 ServiceId 확인
\+ Timeout 확인
\+ Retry·멱등성 확인
\+ TraceId 확인
\+ 소비 화면 확인
\+ 장애·복구 책임 확인

# 11.4 정적 검색의 한계와 보완 방법

## 11.4.1 문자열 검색은 출발점이다

정적 검색은 빠르지만 다음을 증명하지 못한다.

현재 실행되는가?
어떤 조건에서 실행되는가?
어느 Profile에서 활성화되는가?
어느 Branch가 운영 중인가?
Cache Hit로 SQL이 생략되는가?
동적 SQL이 어떻게 만들어지는가?
Proxy가 어떤 구현을 선택하는가?

따라서 검색 결과를 실행 증거로 보완해야 한다.

## 11.4.2 동적 테이블명

금지 또는 특별관리 대상:

SELECT \*
FROM ${tableName}

#{}가 아닌 ${}는 문자열 치환이므로 SQL Injection 위험과 영향분석 어려움이 있다.

정적 검색에서는 실제 테이블명이 나타나지 않을 수 있다.

권장:

허용 테이블 Enum
Whitelist
분리된 Statement
자동 보안검증

## 11.4.3 환경별 Mapper

mapper/local
mapper/dev
mapper/prod

또는 Profile별 다른 Bean이 있을 수 있다.

확인:

active profile
mapper-locations
Conditional Property
배포 WAR 포함 Resource

로컬 H2 SQL과 운영 RDW SQL이 다를 수 있으므로 로컬 성공만으로 운영 SQL을 검증했다고 판단하지 않는다.

## 11.4.4 View·Synonym·Procedure

애플리케이션 저장소 검색만으로 실제 테이블을 찾기 어려운 대표 사례다.

보완:

DB Dictionary
DDL 저장소
DBA 확인
View Dependency
Synonym 정보
Procedure Source

## 11.4.5 Reflection·Proxy

MyBatis Mapper는 런타임 Proxy로 실행된다.

Call Stack에는 다음 클래스가 나타날 수 있다.

MapperProxy
MapperMethod
SqlSessionTemplate
Executor
StatementHandler

Java 구현 클래스가 없다고 호출자가 없는 것은 아니다.

## 11.4.6 Spring AOP

TransactionInterceptor
CacheInterceptor
AsyncExecutionInterceptor
SecurityInterceptor

이러한 Proxy는 실제 호출 경로에 영향을 준다.

특히 다음을 확인한다.

Transaction 적용
Cache Hit
Async Thread 전환
보안 차단
Retry 실행

## 11.4.7 런타임 SQL 로깅

운영 로그 권장 항목:

GUID
TraceId
ServiceId
Mapper Statement ID
DB Pool
Start Time
Elapsed Time
Row Count
Result
Error Code

금지:

비밀번호
JWT
주민번호
계좌번호 원문
전체 SQL Parameter
대용량 Request·Response

## 11.4.8 MyBatis Interceptor

공통 Interceptor를 이용하면 다음을 수집할 수 있다.

Statement ID
SQL 종류
실행시간
영향 행 수
오류
현재 ServiceId
GUID

개념 흐름:

Mapper 호출
↓
MyBatis Interceptor
↓
Statement ID 확인
↓
실행시간 측정
↓
성공·오류·행 수 기록
↓
Mapper 실행 종료

진단 기능이 업무 실패를 유발하지 않도록 Fail-Safe로 구현한다.

## 11.4.9 Slow SQL 로그

Slow 기준 예:

정상 < 500ms
주의 ≥ 500ms
Slow ≥ 1,000ms
거래초과 전체 Timeout 예산 초과 가능

실제 임계치는 업무와 환경 기준으로 확정한다.

Slow SQL 로그:

serviceId=SV.Customer.selectSummary
statementId=...SvCustomerMapper.selectCustomerSummary
elapsedMs=1320
rowCount=1
guid=G...

이 로그가 있으면 SQL에서 ServiceId까지 직접 연결할 수 있다.

## 11.4.10 Breakpoint 검증

Breakpoint 위치:

DAO Method
Mapper Interface 호출 직전
MyBatis Interceptor
Service 결과 판단
Facade Transaction
Handler

확인:

실제 Parameter
실제 Statement ID
호출 Thread
Transaction 활성 여부
결과 Row
예외 변환

운영환경에서 임의 Breakpoint를 사용하지 않는다.

## 11.4.11 Call Stack

실제 호출 스택 예:

SvCustomerMapper.selectCustomerSummary
SvCustomerDao.selectCustomerSummary
SvCustomerService.selectCustomerSummary
SvCustomerFacade.selectCustomerSummary
SvCustomerHandler.doHandle
TransactionDispatcher.dispatch
OnlineTransactionTimeoutExecutor.execute
TCF.process
OnlineTransactionController

Call Stack은 정적 검색의 추측을 실제 실행 경로로 검증한다.

## 11.4.12 GUID·TraceId 연계

화면 요청
↓ GUID·TraceId
ServiceId
↓
Handler
↓
Mapper Statement ID
↓
SQL 실행
↓
ETF 응답

다른 업무 WAR나 외부 시스템 호출에는 TraceId를 전달해야 한다.

## 11.4.13 DB 실행정보

DBA와 함께 확인할 수 있는 정보:

SQL 실행계획
SQL Hash
Session
Wait Event
Lock
실행시간
읽은 Block
반환 Row
Bind 유형

애플리케이션 로그와 DB 로그의 시각·ServiceId·GUID를 연결할 수 있어야 한다.

## 11.4.14 테스트 보완

### Mapper Test

Parameter Mapping
Result Mapping
SQL 문법
영향 행 수

### DAO Test

조회 없음
중복 결과
DB 예외 변환

### Service Test

업무 규칙
상태 판단
응답 변환

### 거래 통합 Test

ServiceId
Transaction
ETF
로그
DB 결과

## 11.4.15 운영 접근이 없는 SQL

운영 로그에 최근 실행 기록이 없더라도 즉시 폐기하지 않는다.

확인:

월말·분기·연말 거래
장애복구 거래
관리자 기능
Batch
법정 보고서
비상 기능

## 11.4.16 미사용 SQL 판정

정적 참조 없음
\+ 런타임 실행 없음
\+ Batch·외부 사용 없음
\+ 문서·운영 기능 없음
\+ 담당 조직 확인
\+ 폐기 승인

이 조건을 만족해야 폐기 후보로 판단한다.

## 11.4.17 정적 검색 보완 표

| 정적 검색 한계 | 보완 방법 |
| --- | --- |
| 동적 SQL | 조건별 통합테스트 |
| 동적 테이블명 | Whitelist·실행 로그 |
| View | DB Dependency 조회 |
| Synonym | DBA Dictionary |
| Procedure | Package Source 분석 |
| Cache Hit | Cache Metric·Miss Test |
| 원격 호출 | TraceId·Client 로그 |
| Profile 분기 | 실제 활성 Profile |
| Proxy | Call Stack |
| 미실행 코드 | Build·Bean·배포 확인 |
| Batch | Job 이력 |
| BI | Report Catalog |
| Legacy | 운영 Artifact·Commit 확인 |

# 대표 역추적 예시

## 고객요약 테이블

DB 객체
SV\_CUSTOMER

사용 SQL
SvCustomerMapper.xml
selectCustomerSummary

Mapper
SvCustomerMapper.selectCustomerSummary()

DAO
SvCustomerDao.selectCustomerSummary()

Service
SvCustomerService.selectCustomerSummary()

Facade
SvCustomerFacade.selectCustomerSummary()

트랜잭션
readOnly=true
timeout=3

Handler
SvCustomerHandler

ServiceId
SV.Customer.selectSummary

화면
SV-CUS-0001
고객 종합정보 조회

운영
Catalog·Timeout·권한·감사 대상

# 컬럼 변경 역추적 예시

변경 대상:

SV\_CUSTOMER.CUSTOMER\_GRADE
VARCHAR(10) → VARCHAR(20)

영향 분석:

Column
↓
Mapper SELECT
↓
CustomerSummaryRow.customerGrade
↓
CustomerSummaryResponse.customerGrade
↓
화면 고객등급 표시
↓
엑셀 다운로드
↓
테스트 데이터

추가 확인:

코드 길이 검증
화면 표시 폭
공통 코드
인덱스
외부 계약
마스킹

# 테이블 폐기 역추적 예시

폐기 후보
SV\_CUSTOMER\_OLD
↓
소스 검색
↓
Mapper 참조 없음
↓
View·Procedure 확인
↓
Batch 이력 확인
↓
BI Report 확인
↓
최근 접근 이력 확인
↓
담당 조직 승인
↓
Deprecated
↓
백업·보존
↓
최종 Drop

# 정상 처리 흐름

DB 객체 검색
↓
Mapper Statement 식별
↓
Mapper Interface 연결
↓
DAO·Service 호출 확인
↓
Facade Transaction 확인
↓
Handler·ServiceId 확인
↓
화면·Batch·원격 호출 확인
↓
실제 거래 실행
↓
GUID·Statement ID 로그 확인
↓
DB 결과 확인
↓
영향 매트릭스 작성

# SQL 오류 흐름

Mapper 실행
↓
SQL 문법·객체·권한 오류
↓
Persistence Exception
↓
DAO·공통 예외 변환
↓
Facade Transaction Rollback
↓
ETF.systemError()
↓
GUID·Statement ID·오류코드 기록

사용자에게 SQL 원문과 DB 내부정보를 노출하지 않는다.

# 조회 결과 없음 흐름

SELECT 실행
↓
결과 0건
↓
Service·Rule 판단
├─ 정상 빈 목록
├─ 미존재 업무 오류
└─ 데이터 이상
↓
ETF 표준 응답

결과 0건의 의미는 Mapper가 아니라 업무 Service에서 판단한다.

# UPDATE 영향 행 0건 흐름

UPDATE 실행
↓
updated = 0
↓
Service 판단
├─ 데이터 미존재
├─ 동시 수정
├─ 상태 변경
├─ 권한 조건 불일치
└─ 삭제됨
↓
업무 오류·재조회 안내

# SQL Timeout 흐름

Mapper SQL 실행
↓
Query Timeout
↓
DB 작업 중단 시도
↓
Connection 상태 확인
↓
Transaction Rollback
↓
TCF Timeout 또는 시스템 오류
↓
Slow SQL·GUID 기록

SQL Timeout과 TCF Timeout의 선후관계가 설계되어야 한다.

# Lock 대기 흐름

UPDATE 실행
↓
DB Lock 대기
↓
SQL 지연
↓
Pool Connection 장기 점유
↓
Tomcat Thread 대기 증가
↓
거래 Timeout

Lock은 SQL 한 건의 문제가 전체 Thread·Pool 문제로 확산될 수 있다.

# 정상 예시

대상 테이블
SV\_CUSTOMER

검색 결과
SvCustomerMapper.xml

Statement ID
selectCustomerSummary

Mapper Interface
SvCustomerMapper

직접 호출자
SvCustomerDao

업무 호출자
SvCustomerService

Transaction
SvCustomerFacade

ServiceId
SV.Customer.selectSummary

화면
SV-CUS-0001

실행 증적
GUID·Statement ID·1 Row 조회

판정
영향 거래와 화면 식별 완료

# 금지 예시

테이블명 검색 결과 한 건만 보고 영향 범위를 확정한다.

Mapper XML만 찾고 Java 호출자를 확인하지 않는다.

SQL ID와 ServiceId를 같은 개념으로 사용한다.

SELECT \*를 사용해 컬럼 변경 영향을 숨긴다.

영향 행 수를 확인하지 않고 UPDATE 성공을 반환한다.

다른 업무의 테이블을 직접 UPDATE한다.

운영 테이블을 로컬 H2 구조와 같다고 가정한다.

View·Synonym·Procedure를 확인하지 않는다.

Batch와 BI 사용 여부를 확인하지 않는다.

정적 검색 결과 0건만으로 테이블을 Drop한다.

SQL Parameter와 개인정보를 로그에 남긴다.

전체 거래 Timeout보다 긴 Query Timeout을 설정한다.

등록 거래를 자동으로 재시도한다.

Cache를 무시하고 DB 결과만 확인한다.

Slow SQL을 발견하자마자 인덱스를 임의 추가한다.

오류를 catch하고 영향 행 0건을 성공으로 처리한다.

다른 도메인의 Mapper Interface를 직접 Import한다.

System.out으로 SQL과 Parameter를 출력한다.

# 책임 경계와 RACI

| 활동 | 업무개발 | DA·DBA | 프레임워크 | 운영 | 업무분석 | 아키텍트 |
| --- | --- | --- | --- | --- | --- | --- |
| Mapper 설계 | R | C | C | I | C | A |
| SQL 작성 | R | C/A | I | I | C | C |
| 테이블 소유권 | C | R | I | C | C | A |
| SQL 실행계획 | C | R/A | I | C | I | C |
| 트랜잭션 설계 | R | C | C | I | C | A |
| Statement 로그 | C | C | R/A | C | I | C |
| 영향분석 | R | R | C | C | C | A |
| View·Procedure 조사 | C | R/A | I | C | I | C |
| 외부 연계 확인 | R | I | C | C | C | A |
| 성능검증 | R | R | C | C | I | A |
| 폐기 승인 | C | R | I | C | C | A |
| 운영 모니터링 | C | C | C | R/A | I | C |

# 자동검증 및 품질 Gate

## 1\. Mapper Interface–XML 정합성

Interface FQCN
↔ XML Namespace

Mapper Method
↔ Statement ID

차단 조건:

Namespace 불일치
Method 없는 Statement
Statement 없는 Method
Parameter Type 불일치
Result Type 불일치

## 2\. 테이블 소유권 Gate

검출:

IC Mapper에서 SV\_\* UPDATE
SV Mapper에서 OM\_\* DELETE
업무 Mapper에서 TCF\_\* 직접 변경

승인된 예외가 아니면 Build 또는 설계 Gate를 통과하지 못한다.

## 3\. SQL 금지 패턴

SELECT \*
무조건 전체조회
WHERE 조건 없는 UPDATE·DELETE
${} 비통제 사용
직접 민감정보 조회
Timeout 미설정
영향 행 무시

## 4\. 추적성 Gate

필수 연결:

Table
→ SQL Statement
→ Mapper Method
→ DAO
→ Service
→ Facade
→ Handler
→ ServiceId
→ 화면·Batch·Client
→ Test

화면–ServiceId–프로그램–SQL–DB 객체가 정방향과 역방향 모두 추적되어야 한다.

## 5\. Timeout Gate

Connection Timeout
< Query Timeout
< Transaction Timeout
≤ TCF Timeout

실제 정책은 거래 특성에 따라 확정하지만 제한시간 간 모순은 없어야 한다.

## 6\. 변경 SQL Gate

상태 조건
Version 조건
권한 조건
영향 행 수 검증
감사로그
Rollback Test

## 7\. 폐기 Gate

정적 참조 0건
런타임 실행 0건
View·Procedure 0건
Batch·BI 0건
호출 조직 승인
보존·복구 계획

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| SQL-001 | Namespace 정상 | Mapper와 XML 연결 |
| SQL-002 | Namespace 불일치 | 기동·실행 실패 |
| SQL-003 | Statement ID 정상 | Method 실행 |
| SQL-004 | Statement ID 누락 | Bound Statement 오류 |
| SQL-005 | Parameter Type 불일치 | 명확한 Mapping 오류 |
| SQL-006 | Result Type 불일치 | Mapping 오류 검출 |
| SQL-007 | 테이블명 검색 | 전체 Mapper 목록 확인 |
| SQL-008 | 컬럼명 검색 | DTO·화면 영향 확인 |
| SQL-009 | 공통 SQL Fragment 변경 | 모든 Include 영향 확인 |
| SQL-010 | 동적 SQL 전체 조건 | 정상 Query 생성 |
| SQL-011 | 동적 SQL 빈 조건 | 전체조회 차단 |
| SQL-012 | 빈 IN 목록 | 표준 결과·오류 |
| SQL-013 | SELECT 0건 | 정책에 맞는 결과 |
| SQL-014 | SELECT 다건 | 목록·중복 정책 확인 |
| SQL-015 | 단건 조회 다건 | 데이터 이상 검출 |
| SQL-016 | INSERT 중복 | 표준 업무 오류 |
| SQL-017 | UPDATE 1건 | 정상 |
| SQL-018 | UPDATE 0건 | 동시성·상태 오류 구분 |
| SQL-019 | UPDATE 다건 | 조건 오류 차단 |
| SQL-020 | DELETE | 논리·물리 정책 확인 |
| SQL-021 | Transaction Rollback | 변경 전체 원복 |
| SQL-022 | Query Timeout | 표준 Timeout 처리 |
| SQL-023 | DB Lock | 지연·Rollback 확인 |
| SQL-024 | View 사용 | 원천 테이블 영향 확인 |
| SQL-025 | Synonym 사용 | 물리 객체 확인 |
| SQL-026 | Procedure 사용 | 내부 테이블 확인 |
| SQL-027 | Trigger 실행 | 간접 변경 확인 |
| SQL-028 | DB Link | 원격 장애 처리 |
| SQL-029 | Cache Hit | DB 미호출 확인 |
| SQL-030 | Cache Miss | Mapper 호출 확인 |
| SQL-031 | 다른 WAR 호출 | TraceId 연결 |
| SQL-032 | 외부 API Timeout | 전체 Timeout 예산 확인 |
| SQL-033 | Batch 사용 테이블 | 온라인 외 영향 확인 |
| SQL-034 | BI Report 사용 | 폐기 대상 제외 |
| SQL-035 | 미사용 SQL 후보 | 전체 증적 확인 |
| SQL-036 | 민감정보 로그 | 마스킹·미기록 |
| SQL-037 | Slow SQL | Statement ID·ServiceId 연결 |
| SQL-038 | 컬럼 길이 변경 | DTO·UI 호환 확인 |
| SQL-039 | Index 변경 | 개선 전후 비교 |
| SQL-040 | 다른 개발자 역추적 | 동일 영향 목록 재현 |

# 따라 하는 실무 절차

## 1단계. 분석 기준 기록

Branch
Commit ID
대상 DB
Schema
Table·Column
변경 목적
분석 시각

## 2단계. 테이블명 전체 검색

rg -i "\\bSV\_CUSTOMER\\b" .

분류:

Mapper
DDL
View
Procedure
Batch
Test
문서

## 3단계. Mapper Statement 식별

Namespace
Statement ID
SQL Type
Parameter
Result
Timeout

## 4단계. Mapper Interface 연결

XML Namespace
↔ Java FQCN

Statement ID
↔ Method

## 5단계. DAO·Service 호출자 찾기

IDE:

Find Usages
Call Hierarchy

명령행:

rg -F "selectCustomerSummary" .

## 6단계. Transaction 확인

Facade
@Transactional
readOnly
timeout
rollbackFor

## 7단계. Handler·ServiceId 확인

serviceIds()
switch 분기
Facade Method

## 8단계. 화면·Batch·Client 확인

UI ServiceId
다른 업무 Client
Batch Job
Report

## 9단계. 운영 등록 확인

Catalog
Timeout
권한
감사
목표 응답시간
Slow SQL 기준

## 10단계. 런타임 검증

대표 거래 실행
GUID 확보
Statement ID 로그
SQL 실행시간
Row Count
DB 결과

## 11단계. 오류 검증

0건
중복
DB 오류
Timeout
Lock
Rollback

## 12단계. 영향 매트릭스 작성

DB 객체
→ SQL
→ 프로그램
→ ServiceId
→ 화면·Batch·연계
→ 테스트

# 완료 체크리스트

## Mapper

| 확인 항목 | 완료 |
| --- | --- |
| Mapper XML을 찾았다. | □ |
| Namespace를 확인했다. | □ |
| Statement ID를 확인했다. | □ |
| Mapper Interface를 확인했다. | □ |
| Parameter Type을 확인했다. | □ |
| Result Type을 확인했다. | □ |
| Timeout을 확인했다. | □ |
| 공통 SQL Fragment를 확인했다. | □ |
| 동적 SQL 조건을 확인했다. | □ |

## 호출자

| 확인 항목 | 완료 |
| --- | --- |
| DAO를 확인했다. | □ |
| Service를 확인했다. | □ |
| Rule을 확인했다. | □ |
| Facade Transaction을 확인했다. | □ |
| Handler를 확인했다. | □ |
| ServiceId를 확인했다. | □ |
| 화면 이벤트를 확인했다. | □ |
| Batch·Client 호출을 확인했다. | □ |

## DB 객체

| 확인 항목 | 완료 |
| --- | --- |
| 조회·변경 유형을 구분했다. | □ |
| Join 테이블을 확인했다. | □ |
| View·Synonym을 확인했다. | □ |
| Procedure·Trigger를 확인했다. | □ |
| DB Link를 확인했다. | □ |
| Index를 확인했다. | □ |
| Lock 영향을 확인했다. | □ |
| 영향 행 수 정책을 확인했다. | □ |
| 데이터 소유권을 확인했다. | □ |

## 운영·검증

| 확인 항목 | 완료 |
| --- | --- |
| GUID 로그를 확인했다. | □ |
| Mapper Statement ID를 확인했다. | □ |
| SQL 실행시간을 확인했다. | □ |
| 반환·영향 행 수를 확인했다. | □ |
| Query Timeout을 확인했다. | □ |
| 전체 거래 Timeout을 확인했다. | □ |
| 권한·감사를 확인했다. | □ |
| 정상·오류·Rollback을 검증했다. | □ |
| 영향 매트릭스를 작성했다. | □ |
| 다른 개발자가 재현했다. | □ |

# 변경·호환성·폐기 관리

## SQL 변경

SQL 변경 시 확인:

요청조건
Result Mapping
실행계획
Index
Lock
Timeout
영향 행 수
업무 결과

## 컬럼 추가

하위 호환 가능한 경우:

Nullable 컬럼
기본값
기존 SQL 영향 없음
기존 DTO 영향 없음

그러나 SELECT \*가 있으면 Result Mapping이 바뀔 수 있으므로 확인한다.

## 컬럼 타입·길이 변경

DB 타입
Java 타입
DTO 검증
JSON 계약
UI 길이
외부 연계
Index
데이터 이관

## 컬럼명 변경

단순 Rename으로 처리하지 않는다.

Mapper SQL
View
Procedure
Trigger
Batch
BI
DTO Alias
문서

## 테이블 분할·통합

구버전 View 제공
이행 기간
양방향 동기화 여부
데이터 이관
Cutover
Rollback

## SQL ID 변경

Mapper Method 이름 변경은 다음에 영향을 줄 수 있다.

XML Statement
Java Call
Runtime 로그
SQL Catalog
테스트
운영 Dashboard

## 테이블 폐기

신규 사용 중지
↓
전체 소비자 조사
↓
대체 객체 제공
↓
Deprecated
↓
런타임 미사용 확인
↓
백업·보존
↓
권한 제거
↓
최종 Drop

# 시사점

## 핵심 아키텍처 판단

첫째, DB 객체는 단순 저장공간이 아니다.

Table
↔ Mapper
↔ Service
↔ ServiceId
↔ 화면
↔ 운영정책

이 연결관계의 일부다.

둘째, 역추적은 문자열 검색으로 끝나지 않는다.

정적 검색
→ 구조 분석
→ 런타임 검증
→ 데이터 확인
→ 영향 매트릭스

순서로 수행해야 한다.

셋째, SQL ID와 ServiceId는 분리해야 한다.

하나의 ServiceId
→ 여러 SQL

하나의 SQL
→ 여러 ServiceId에서 재사용 가능

따라서 다대다 관계를 추적성 매트릭스로 표현해야 한다.

넷째, 다른 도메인의 테이블 직접 접근은 기술적 편의보다 데이터 소유권과 운영 책임을 우선해 판단해야 한다.

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| 문자열 검색만 수행 | 간접 사용 누락 |
| SQL ID·ServiceId 혼용 | 운영 추적 불완전 |
| View·Procedure 누락 | 영향 범위 축소 판단 |
| Batch·BI 누락 | 운영 장애 |
| 데이터 소유권 위반 | 도메인 결합 |
| 영향 행 무시 | 정합성 오류 |
| Timeout 불일치 | Thread·Pool 고갈 |
| 동적 SQL 전체조회 | 대규모 부하 |
| 민감 Parameter 로그 | 개인정보 사고 |
| Cache 영향 누락 | 구 데이터 노출 |
| 인덱스 임의 변경 | 변경 성능 저하 |
| 미사용 오판 | 기능 장애 |
| 로컬 SQL만 검증 | 운영 DB 실패 |

## 우선 보완 과제

1.  Mapper Statement ID를 모든 SQL 로그에 기록한다.
2.  Statement ID와 ServiceId를 GUID 기준으로 연결한다.
3.  Mapper XML에서 테이블 사용 목록을 자동 추출한다.
4.  Interface–XML 정합성 검사를 CI에 추가한다.
5.  SQL–DAO–ServiceId 추적표를 자동 생성한다.
6.  다른 도메인 테이블 직접 접근을 자동 검출한다.
7.  View·Procedure·Synonym 정보를 DB Catalog와 연계한다.
8.  UPDATE·DELETE 영향 행 수 검증을 표준화한다.
9.  SQL Timeout과 거래 Timeout 정합성 검사를 자동화한다.
10.  Slow SQL에 ServiceId·GUID·App Version을 기록한다.
11.  Batch·BI·외부 소비자를 통합 Data Lineage에 등록한다.
12.  미사용 SQL·테이블 폐기 절차를 공식화한다.

## 중장기 발전 방향

수동 테이블 검색
↓
Mapper·Table 자동 추출
↓
SQL–ServiceId 자동 매핑
↓
DB Catalog·View·Procedure 연계
↓
런타임 SQL 실행이력 통합
↓
변경 영향 자동 분석
↓
화면·거래·프로그램·데이터 통합 Lineage

# 마무리말

SQL과 테이블에서 역으로 추적하는 과정은 다음 질문에 답하는 일이다.

이 DB 객체를 누가 사용하는가?

어떤 Mapper Statement가 실행되는가?

어느 DAO와 Service가 호출하는가?

트랜잭션 경계는 어디인가?

어떤 Handler와 ServiceId에 연결되는가?

어느 화면과 Batch에서 사용하는가?

다른 업무와 외부 시스템도 사용하는가?

변경하면 어떤 테스트를 다시 해야 하는가?

운영에서 어떤 GUID와 Statement ID로 확인할 수 있는가?

제11장에서 기억할 핵심 경로는 다음과 같다.

Table·Column
↓
Mapper XML
↓
Statement ID
↓
Mapper Interface
↓
DAO
↓
Service
↓
Facade
↓
Handler
↓
ServiceId
↓
화면·Batch·Client
↓
운영·테스트

가장 중요한 원칙은 다음과 같다.

테이블명이 검색되었다
≠ 영향 분석이 완료되었다.

SQL이 실제로 실행되고,
어떤 거래와 화면에 연결되며,
변경 후 데이터와 운영 결과가 정상인지
증명해야 영향 분석이 완료된다.

다음 장에서는 요구사항이나 변경 요청을 기준으로 화면·ServiceId·프로그램·SQL·DB·설정·배포·운영 영향 범위를 종합적으로 판단하는 방법을 학습한다.
