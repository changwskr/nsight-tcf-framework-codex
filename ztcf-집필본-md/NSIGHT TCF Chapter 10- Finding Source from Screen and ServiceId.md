<!-- source: ztcf-집필본/NSIGHT TCF Chapter 10- Finding Source from Screen and ServiceId.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제10장. 화면과 ServiceId에서 소스 찾기

## 이 장을 시작하며

제9장에서는 처음 보는 저장소에서 실행 모듈과 라이브러리를 구분하고, 대표 거래 한 건을 선택해 저장소 지도를 만드는 방법을 살펴보았다.

이제 실제 업무 요청을 분석해야 한다.

개발자가 업무팀으로부터 받는 요청은 대개 다음과 같은 형태다.

고객 종합정보 화면의 조회 버튼이 동작하지 않습니다.

상품 목록을 조회할 때 오류가 발생합니다.

저장 버튼을 누르면 어떤 프로그램이 실행되는지 확인해 주세요.

이 화면에서 호출하는 SQL을 수정해야 합니다.

운영 로그에 나온 ServiceId가 어느 화면에서 호출되는지 찾아 주세요.

이때 초보 개발자는 화면 이름과 비슷한 Java 클래스부터 찾으려는 경향이 있다.

고객 화면
→ CustomerController 검색
→ CustomerService 검색
→ 비슷한 클래스를 임의로 선택

일반적인 웹 애플리케이션에서는 URL이나 Controller가 기능 탐색의 출발점이 될 수 있다.

그러나 NSIGHT TCF에서 화면과 업무 프로그램을 연결하는 핵심 식별자는 Controller 이름이 아니라 ServiceId다.

화면
↓
화면 이벤트
↓
StandardRequest.header.serviceId
↓
OnlineTransactionController
↓
TCF
↓
TransactionDispatcher
↓
ServiceId에 등록된 Handler
↓
Facade
↓
Service
↓
Rule·DAO·Mapper

따라서 화면에서 소스를 찾을 때는 다음 두 가지를 구분해야 한다.

화면을 식별하는 값
\= 화면 ID

실행할 서버 거래를 식별하는 값
\= ServiceId

화면 ID는 사용자가 보고 있는 화면을 알려 준다.

ServiceId는 그 화면의 특정 이벤트가 서버에서 어떤 업무 거래를 실행할지를 알려 준다.

화면과 ServiceId의 관계는 항상 일대일이 아니다.

화면 하나
→ 조회·저장·삭제·다운로드 등 여러 ServiceId

ServiceId 하나
→ 여러 화면에서 공통 호출 가능

이벤트 하나
→ 여러 ServiceId를 순차 또는 병렬 호출 가능

따라서 화면 이름과 클래스 이름을 추측하는 방식으로는 정확한 처리 경로를 찾기 어렵다.

이 장에서는 다음 경로를 단계적으로 추적한다.

화면 ID
→ 이벤트 ID
→ UI 객체와 이벤트 함수
→ 요청 생성 코드
→ ServiceId
→ Handler 등록정보
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ SQL
→ Table
→ ETF 표준 응답
→ 화면 결과 처리

화면 이벤트와 ServiceId, Java 프로그램, SQL과 DB 객체는 하나의 추적체계로 연결되어야 하며, 화면 이벤트와 ServiceId가 다대다 관계가 될 수 있다는 점도 산출물에서 표현해야 한다.

## 핵심 관점

화면의 이름과
Java 클래스의 이름이 비슷하다고 해서
같은 거래라고 단정하지 않는다.

화면 이벤트가 실제로 전송한 ServiceId와
Dispatcher가 선택한 Handler를 근거로
처리 경로를 확인한다.

## 학습 목표

이 장을 마치면 다음 내용을 직접 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 화면 ID·이벤트 ID·기능 ID의 차이를 설명한다. |
| 2 | 화면 하나가 여러 ServiceId를 호출하는 구조를 설명한다. |
| 3 | 화면 정의서에서 이벤트와 ServiceId를 찾는다. |
| 4 | WEBTOPSUITE·React 소스에서 화면 이벤트 함수를 찾는다. |
| 5 | 브라우저 Network Payload에서 ServiceId를 확인한다. |
| 6 | 저장소 전체에서 ServiceId 문자열을 정확히 검색한다. |
| 7 | 소스·설정·문서·테스트의 검색 결과를 구분한다. |
| 8 | Handler의 serviceId() 또는 serviceIds() 등록방식을 설명한다. |
| 9 | 중복 ServiceId가 기동 오류를 일으키는 이유를 설명한다. |
| 10 | Handler와 OM Service Catalog의 정합성을 확인한다. |
| 11 | Handler에서 Facade·Service·Rule·DAO·Mapper를 정방향 추적한다. |
| 12 | Facade의 트랜잭션 경계를 확인한다. |
| 13 | Mapper Interface와 XML의 Namespace·SQL ID를 연결한다. |
| 14 | 정상·업무 오류·시스템 오류·Timeout 경로를 구분한다. |
| 15 | 정적 검색 결과를 Breakpoint·GUID 로그·SQL 결과로 검증한다. |
| 16 | 화면–ServiceId–프로그램 추적성 매트릭스를 작성한다. |
| 17 | 미등록·중복·폐기 ServiceId를 구분한다. |
| 18 | 변경 시 화면·권한·OM·테스트 영향 범위를 식별한다. |

# 한눈에 보는 전체 추적 흐름

┌────────────────────────────────────────────────────────────┐
│ 1. 화면 │
│ 화면 ID: SV-CUS-0001 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. 화면 이벤트 │
│ 이벤트 ID: SV-CUS-0001-E01 │
│ 조회 버튼 Click │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. UI 요청 생성 │
│ StandardRequest Header·Body 작성 │
│ ServiceId = SV.Customer.selectSummary │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. 공통 진입 │
│ OnlineTransactionController → TCF.process() │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. 공통 사전 처리 │
│ STF: 인증·권한·거래통제·Timeout·로그 시작 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. ServiceId 분배 │
│ TransactionDispatcher │
│ handlerMap.get("SV.Customer.selectSummary") │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 7. 업무 프로그램 │
│ Handler → Facade → Service → Rule·DAO │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 8. 데이터 처리 │
│ Mapper → SQL ID → Table │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 9. 결과 표준화 │
│ ETF → StandardResponse │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 10. 화면 결과 처리 │
│ 데이터 표시·오류 메시지·재조회 │
└────────────────────────────────────────────────────────────┘

# 핵심 식별자 비교

| 식별자 | 핵심 질문 | 대표 예 |
| --- | --- | --- |
| 요구사항 ID | 왜 만드는가 | REQ-SV-CUS-001 |
| 화면 ID | 어느 화면인가 | SV-CUS-0001 |
| 이벤트 ID | 어떤 사용자 행동인가 | SV-CUS-0001-E01 |
| UI 객체 ID | 어떤 버튼·그리드인가 | btnSearch |
| 기능 ID | 어떤 논리 기능인가 | SV-CUS-0001-F01 |
| ServiceId | 어떤 서버 거래인가 | SV.Customer.selectSummary |
| 거래코드 | 어떤 통제·감사 분류인가 | SV-INQ-0001 |
| Handler | 어느 업무 진입점인가 | SvCustomerHandler |
| Facade Method | 어느 유스케이스인가 | selectSummary() |
| Mapper SQL ID | 어떤 SQL인가 | selectCustomerSummary |
| GUID | 어느 거래 한 건인가 | 거래별 생성값 |
| TraceId | 어느 시스템 간 호출인가 | 호출 체인 식별값 |

# 화면과 프로그램 관계의 다중성

| 관계 | 일반 형태 | 설계 판단 |
| --- | --- | --- |
| 화면 → 이벤트 | 1:N | 한 화면에 여러 사용자 행동 존재 |
| 이벤트 → ServiceId | N:M | 한 이벤트가 여러 거래 호출 가능 |
| ServiceId → Handler | 논리적 1:1 | 실행 Handler는 하나여야 함 |
| Handler Class → ServiceId | 1:N 가능 | 도메인 Handler가 여러 거래 처리 |
| ServiceId → Facade Method | 원칙적 1:1 | 유스케이스와 트랜잭션 명확화 |
| Facade → Service | 1:N | 여러 업무 서비스를 조합 가능 |
| Service → DAO | 1:N | 여러 데이터 소스 사용 가능 |
| Mapper SQL → Table | N:M | Join·Subquery 사용 가능 |
| ServiceId → 화면 | 1:N 가능 | 여러 화면에서 공통 거래 사용 |

현재 NSIGHT 기준에서도 하나의 Handler 클래스가 여러 ServiceId를 등록할 수 있지만, 하나의 ServiceId가 두 Handler에 중복 등록되는 것은 허용하지 않는다.

# 10.1 화면 ID로 거래 찾기

## 10.1.1 화면 ID는 무엇을 알려 주는가

화면 ID는 다음 정보를 식별하기 위한 기준이다.

업무 영역
업무 세구분
화면 순번
화면 정의서
메뉴
권한
화면 소스
테스트 시나리오

예:

SV-CUS-0001
│ │ │
│ │ └─ 화면 일련번호
│ └───── 고객 업무 세구분
└───────── SV 업무코드

화면 ID만으로 서버 ServiceId를 완전히 추측할 수는 없다.

화면 ID
\= 화면·메뉴 식별자

ServiceId
\= 업무 거래 실행 식별자

따라서 화면 ID를 찾은 뒤 반드시 화면 이벤트와 요청 생성 코드를 확인해야 한다.

## 10.1.2 화면 탐색의 출발자료

다음 자료를 우선 확인한다.

| 우선순위 | 자료 | 확인 내용 |
| --- | --- | --- |
| 1 | 화면 정의서 | 화면 ID·이벤트·호출 거래 |
| 2 | 화면 이벤트 정의서 | 버튼·그리드·초기화 이벤트 |
| 3 | 화면–ServiceId 매핑표 | 이벤트별 ServiceId |
| 4 | UI 소스 | 실제 요청 생성 코드 |
| 5 | 브라우저 Network | 실제 전송 전문 |
| 6 | 메뉴·권한 등록 | 화면 접근 경로 |
| 7 | 통합테스트 | 실제 기대 거래 |
| 8 | 운영 로그 | 실제 호출된 ServiceId |

문서에 ServiceId가 적혀 있더라도 실제 Network Payload와 다를 수 있으므로 런타임 요청을 함께 확인한다.

## 10.1.3 화면 정의서에서 확인할 항목

| 항목 | 확인 기준 |
| --- | --- |
| 화면 ID | 프로젝트 표준에 맞는가 |
| 화면명 | 사용자 기능과 일치하는가 |
| 업무코드 | 배포 WAR와 일치하는가 |
| 이벤트 ID | 화면 내 고유한가 |
| 이벤트명 | 조회·저장·삭제 등이 명확한가 |
| UI 객체 ID | 실제 소스 객체와 일치하는가 |
| 발생 조건 | 화면 상태와 선행조건이 있는가 |
| 호출 ServiceId | 실제 요청값과 일치하는가 |
| 거래코드 | 처리유형과 일치하는가 |
| 입력 항목 | Body DTO와 일치하는가 |
| 성공 처리 | 화면 갱신 방식이 정의됐는가 |
| 실패 처리 | 오류유형별 행동이 정의됐는가 |
| 중복 방지 | 멱등성·버튼 잠금 기준이 있는가 |
| 권한 | 화면권한과 거래권한이 연결됐는가 |

## 10.1.4 화면 ID 검색

IDE 전체 검색:

SV-CUS-0001

명령행 예:

rg "SV-CUS-0001" .

생성파일 제외:

rg "SV-CUS-0001" . \\
\-g "!build/\*\*" \\
\-g "!node\_modules/\*\*" \\
\-g "!.git/\*\*"

검색 결과는 다음과 같이 분류한다.

| 검색 위치 | 의미 |
| --- | --- |
| 화면 정의 XML·JSON | 화면 자체 정의 |
| JavaScript·TypeScript | 이벤트와 요청 생성 |
| 메뉴 설정 | 메뉴 연결 |
| 권한 설정 | 접근권한 |
| 테스트 | 화면 시나리오 |
| 문서 | 설계·산출물 |
| 로그 샘플 | 운영 사례 |
| 생성파일 | 분석 제외 가능 |

## 10.1.5 WEBTOPSUITE 화면에서 찾기

WEBTOPSUITE 계열 화면에서는 프로젝트별 구현 방식이 다를 수 있으므로 다음 항목을 찾는다.

화면 ID
UI Component ID
onClick·onChange·onLoad 이벤트
공통 거래 호출 함수
Request Header 구성
ServiceId 설정
Callback 함수

대표 흐름:

btnSearch 클릭
↓
fnSearch()
↓
공통 요청 객체 생성
↓
header.serviceId 설정
↓
gfnTransaction() 또는 프로젝트 공통 호출
↓
Callback

검색어 예:

btnSearch
fnSearch
serviceId
transaction
callback

화면 생성 도구가 이벤트 함수를 자동 생성하는 경우 화면 정의 파일과 별도 Script 파일을 함께 확인한다.

## 10.1.6 React 화면에서 찾기

React에서는 다음 경로를 확인한다.

Route
→ Page Component
→ Button onClick
→ Hook·Action·Service
→ API Client
→ Request DTO
→ ServiceId

대표 예:

const handleSearch = async () => {
const request = {
header: {
businessCode: "SV",
serviceId: "SV.Customer.selectSummary",
transactionCode: "SV-INQ-0001"
},
body: {
customerNo
}
};

const response = await onlineClient.execute(request);
};

실제 프로젝트에서는 다음 형태로 ServiceId가 분리될 수 있다.

SERVICE\_IDS.SV\_CUSTOMER\_SELECT\_SUMMARY

또는:

createHeader("SV.Customer.selectSummary")

따라서 문자열 검색 결과가 없으면 상수·Enum·Request Builder도 확인한다.

## 10.1.7 화면 초기화 이벤트

사용자가 버튼을 누르지 않아도 화면 진입 시 거래가 실행될 수 있다.

화면 로딩
↓
사용자 기본정보 조회
↓
공통코드 조회
↓
권한 조회
↓
초기 목록 조회

예:

| 순서 | 이벤트 | ServiceId |
| --- | --- | --- |
| 1 | 화면 초기화 | CC.Code.selectList |
| 2 | 권한 확인 | OM.Auth.selectFunctionPermission |
| 3 | 고객요약 조회 | SV.Customer.selectSummary |

화면 로딩 오류를 조회 버튼 오류로 오인하지 않도록 호출 순서를 확인해야 한다.

## 10.1.8 하나의 이벤트가 여러 거래를 호출하는 경우

조회 버튼 클릭
↓
고객 기본정보 조회
↓
고객 등급 조회
↓
보유상품 조회
↓
화면 통합 표시

구현 방식:

| 방식 | 특징 |
| --- | --- |
| UI 순차 호출 | 화면에서 여러 ServiceId 호출 |
| UI 병렬 호출 | 성능 개선 가능, 실패 조합 복잡 |
| 통합 ServiceId | Facade가 여러 업무를 조합 |
| Read Model 조회 | 통합 조회 전용 데이터 사용 |

어떤 방식이 맞는지는 거래 일관성, 응답시간, 장애 격리와 데이터 소유권을 기준으로 판단한다.

## 10.1.9 브라우저 Network에서 확인

UI 소스를 읽기 어려울 때 가장 확실한 방법 중 하나는 실제 Network 요청을 확인하는 것이다.

확인 항목:

Request URL
HTTP Method
Authorization Header
Content-Type
Request Payload
businessCode
serviceId
transactionCode
GUID·TraceId
Response Result

예:

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"channelId": "WEBTOP"
},
"body": {
"customerNo": "C000001234"
}
}

주의:

브라우저에서 본 사용자 ID
≠ 서버가 신뢰해야 할 인증정보

서버는 검증된 JWT Claim·인증 Context를 우선한다.

## 10.1.10 화면 ID만 있고 ServiceId를 모를 때

다음 순서로 찾는다.

화면 ID 검색
↓
화면 이벤트 함수 확인
↓
공통 요청 함수 호출 확인
↓
요청 Header 생성 위치 확인
↓
ServiceId 상수·문자열 확인
↓
Network Payload 검증

ServiceId가 동적으로 생성된다면 다음 항목을 확인한다.

업무코드
도메인
처리유형
공통 Header Builder
상수 정의
환경별 설정

## 10.1.11 화면 이벤트–ServiceId 매핑표

| 화면 ID | 이벤트 ID | 이벤트명 | 호출순서 | ServiceId | 거래코드 | 필수 여부 |
| --- | --- | --- | --- | --- | --- | --- |
| SV-CUS-0001 | E00 | 화면 초기화 | 1 | CC.Code.selectList | CC-INQ-0001 | 필수 |
| SV-CUS-0001 | E01 | 고객요약 조회 | 1 | SV.Customer.selectSummary | SV-INQ-0001 | 필수 |
| SV-CUS-0001 | E02 | 보유상품 조회 | 1 | SV.Customer.selectProducts | SV-INQ-0002 | 선택 |
| SV-CUS-0001 | E03 | 메모 저장 | 1 | SV.Customer.updateMemo | SV-UPD-0001 | 필수 |
| SV-CUS-0001 | E04 | 엑셀 다운로드 | 1 | SV.Customer.downloadExcel | SV-DWN-0001 | 선택 |

## 10.1.12 화면 기준 완료 조건

화면에서 거래를 찾았다고 판단하려면 다음이 확인되어야 한다.

화면 ID 확인
\+ 이벤트 ID 확인
\+ 실제 UI 함수 확인
\+ Network ServiceId 확인
\+ Handler 등록 확인
\+ 성공·실패 Callback 확인

# 10.2 ServiceId 문자열 검색

## 10.2.1 ServiceId가 핵심 검색어인 이유

NSIGHT TCF에서 ServiceId는 다음을 연결한다.

화면 요청
↔ Handler 등록
↔ Dispatcher
↔ OM Service Catalog
↔ 거래통제
↔ Timeout 정책
↔ 거래로그
↔ 테스트
↔ 운영 장애

따라서 화면에서 ServiceId를 찾았다면 동일 문자열을 저장소 전체에서 검색하는 것이 가장 빠른 다음 단계다.

## 10.2.2 정확한 문자열 검색

예:

SV.Customer.selectSummary

명령행:

rg -F "SV.Customer.selectSummary" .

\-F는 정규식이 아니라 정확한 문자열로 검색한다.

파일 유형 제한:

rg -F "SV.Customer.selectSummary" \\
\-g "\*.java" \\
\-g "\*.xml" \\
\-g "\*.yml" \\
\-g "\*.yaml" \\
\-g "\*.json" \\
\-g "\*.ts" \\
\-g "\*.js" \\
\-g "\*.md" .

## 10.2.3 검색 결과 분류

| 분류 | 대표 위치 | 의미 |
| --- | --- | --- |
| UI 호출 | JavaScript·TypeScript·화면 XML | 거래 요청 |
| Handler 등록 | Java serviceIds() | 실행 프로그램 |
| 테스트 | Test Source·Request JSON | 검증 시나리오 |
| OM 설정 | SQL·YML·초기데이터 | 운영 등록 |
| 문서 | 설계서·매핑표 | 설계 의도 |
| 로그 샘플 | 운영 문서 | 장애 사례 |
| 폐기 코드 | Legacy·Deprecated | 신규 사용 금지 |
| 생성파일 | Build·Report | 분석 제외 가능 |

검색 결과가 많다고 모두 현재 실행 코드인 것은 아니다.

## 10.2.4 검색 우선순위

1\. src/main/java
2\. src/main/resources
3\. src/test
4\. UI 소스
5\. 운영 설정·DB Script
6\. 문서
7\. Build·생성파일

운영 경로를 찾을 때는 src/main의 Handler 등록을 가장 먼저 확인한다.

## 10.2.5 문자열이 검색되지 않는 경우

ServiceId가 다음 형태로 생성될 수 있다.

### 상수

public static final String SELECT\_SUMMARY =
"SV.Customer.selectSummary";

### Enum

SV\_CUSTOMER\_SELECT\_SUMMARY("SV.Customer.selectSummary")

### 분할 조합

BUSINESS\_CODE + "." + DOMAIN + "." + ACTION

### Annotation

@TcfService("SV.Customer.selectSummary")

### 설정 기반

service-id:
select-summary: SV.Customer.selectSummary

따라서 전체 문자열이 없으면 다음 단위로 검색한다.

selectSummary
Customer
SV.
serviceIds
TcfService

## 10.2.6 유사 ServiceId 구분

검색 결과:

SV.Customer.selectSummary
SV.Customer.selectSummaryV2
SV.Customer.selectSummaryLegacy
SV.Customer.selectSummaryTest

확인 기준:

| 기준 | 질문 |
| --- | --- |
| Handler 등록 | 실제 Dispatcher에 등록되는가 |
| Build 대상 | 현재 모듈에 포함되는가 |
| Profile | 운영 Profile에서 활성화되는가 |
| OM Catalog | 사용 상태인가 |
| 화면 호출 | 현재 화면이 호출하는가 |
| 최근 Commit | 유지보수 중인가 |
| 테스트 | 운영 거래 테스트인가 |
| Deprecated | 폐기 표시가 있는가 |

이름이 가장 비슷한 클래스를 임의로 선택하지 않는다.

## 10.2.7 화면 요청과 Handler 등록 정합성

UI ServiceId
↕
Handler serviceIds()
↕
OM Catalog
↕
거래통제
↕
Timeout Policy

모든 값이 동일해야 한다.

예:

| 영역 | 값 | 판정 |
| --- | --- | --- |
| UI | SV.Customer.selectSummary | 일치 |
| Handler | SV.Customer.selectSummary | 일치 |
| Catalog | SV.Customer.selectSummary | 일치 |
| Timeout | SV.Customer.selectSummary | 일치 |
| 테스트 | SV.Customer.selectSummary | 일치 |

대소문자나 점 하나의 차이도 다른 ServiceId로 처리될 수 있다.

## 10.2.8 ServiceId 명명 구조 해석

SV.Customer.selectSummary
│ │ │
│ │ └─ 행위
│ └────────────── 도메인
└──────────────────── 업무코드

| 구간 | 의미 | 확인할 내용 |
| --- | --- | --- |
| SV | 업무코드 | sv-service 소유 여부 |
| Customer | 업무 도메인 | 고객 책임 영역 |
| selectSummary | 행위 | 조회 유스케이스 |

ServiceId는 단순 문자열이 아니라 업무 소유권과 실행 책임을 나타낸다.

## 10.2.9 거래코드와 혼동하지 않는다

| 구분 | ServiceId | 거래코드 |
| --- | --- | --- |
| 목적 | 실행할 Handler 선택 | 통제·감사·통계 분류 |
| 예 | SV.Customer.selectSummary | SV-INQ-0001 |
| 사용 | Dispatcher | OM·감사·통계 |
| 변경 영향 | 코드·화면·Catalog | 운영 정책·통계 |
| 중복 | 절대 금지 | 프로젝트 규칙에 따라 금지 |

ServiceId만 찾고 거래코드를 무시하면 운영 통제와 감사 관계를 놓칠 수 있다.

## 10.2.10 소스 검색과 운영 검색

### 개발 검색

Handler
Facade
Service
Mapper
Test

### 운영 검색

Service Catalog
거래통제
Timeout
권한
거래로그
감사로그

개발 소스가 존재해도 운영 기준정보가 없으면 운영환경에서 실행되지 않을 수 있다.

## 10.2.11 로그에서 ServiceId 확인

운영 로그 예:

guid=G202607180001
traceId=T202607180001
businessCode=SV
serviceId=SV.Customer.selectSummary
step=HANDLER
elapsedMs=128
result=SUCCESS

로그를 이용하면 다음을 확인할 수 있다.

실제 ServiceId
실제 Handler 실행
업무코드
처리 단계
응답시간
오류코드
배포 버전

## 10.2.12 ServiceId 검색 완료 조건

UI 요청 위치
\+ Handler 등록 위치
\+ OM Catalog
\+ Timeout·통제
\+ 테스트
\+ 실제 로그

를 모두 연결해야 완료다.

# 10.3 Handler와 등록 정보 확인

## 10.3.1 Handler는 업무 진입점이다

Handler는 공통 Dispatcher와 업무 유스케이스를 연결한다.

ServiceId
↓
TransactionDispatcher
↓
TransactionHandler
↓
Facade

Handler의 주요 책임은 다음과 같다.

| 책임 | 설명 |
| --- | --- |
| ServiceId 등록 | 담당 거래 선언 |
| 거래 분기 | 요청 ServiceId별 Facade Method 선택 |
| 요청 변환 | Standard Body를 업무 DTO로 변환 |
| Facade 호출 | 유스케이스 실행 위임 |
| 결과 반환 | 업무 결과를 ETF로 전달 |
| 미지원 처리 | 명시적 오류 발생 |

## 10.3.2 Handler가 하지 말아야 할 일

SQL 직접 실행
Mapper 직접 호출
복잡한 업무 규칙 판단
다수 DB 트랜잭션 조립
JWT 직접 파싱
운영설정 임의 조회
HTTP 응답 직접 생성
예외 무시

Handler는 얇게 유지해야 한다.

## 10.3.3 단일 ServiceId Handler

프로젝트 구현에 따라 Handler 하나가 거래 하나를 담당할 수 있다.

@Component
@RequiredArgsConstructor
public class SvCustomerSummaryHandler
implements TransactionHandler {

private final SvCustomerFacade facade;

@Override
public String serviceId() {
return "SV.Customer.selectSummary";
}

@Override
public Object doHandle(StandardRequest request) {
return facade.selectSummary(request.body());
}
}

적합한 경우:

거래가 독립적임
업무 복잡도가 높음
거래별 담당자가 다름
Handler별 테스트·배포 관리가 필요함

## 10.3.4 복수 ServiceId Handler

현재 NSIGHT 설계에서는 도메인 단위 Handler가 여러 ServiceId를 등록할 수 있다.

@Component
@RequiredArgsConstructor
public class SvCustomerHandler
implements TransactionHandler {

private final SvCustomerFacade facade;

@Override
public Set<String> serviceIds() {
return Set.of(
"SV.Customer.selectSummary",
"SV.Customer.selectProducts",
"SV.Customer.updateMemo"
);
}

@Override
public Object doHandle(StandardRequest request) {
return switch (request.header().serviceId()) {
case "SV.Customer.selectSummary" ->
facade.selectSummary(request.body());

case "SV.Customer.selectProducts" ->
facade.selectProducts(request.body());

case "SV.Customer.updateMemo" ->
facade.updateMemo(request.body());

default ->
throw new BusinessException(
"TCF-SERVICE-0001",
"지원하지 않는 ServiceId입니다."
);
};
}
}

이 방식에서는 하나의 Handler가 관련 도메인의 여러 ServiceId를 응집해 관리한다.

## 10.3.5 단일·복수 Handler 비교

| 구분 | 단일 ServiceId | 복수 ServiceId |
| --- | --- | --- |
| 클래스 수 | 많음 | 적음 |
| 거래별 독립성 | 높음 | 도메인 중심 |
| 분기 코드 | 거의 없음 | 명시적 분기 필요 |
| 대규모 Handler 위험 | 낮음 | 존재 |
| 공통 의존성 재사용 | 제한적 | 용이 |
| 적용 기준 | 복잡·독립 거래 | 응집된 도메인 거래 |

어느 방식을 사용하더라도 하나의 ServiceId는 하나의 Handler에만 등록되어야 한다.

## 10.3.6 Dispatcher 등록 구조

애플리케이션 기동 시 Dispatcher는 모든 Handler Bean을 수집한다.

Spring Context 기동
↓
TransactionHandler Bean 목록 수집
↓
각 Handler의 ServiceId 목록 조회
↓
handlerMap 등록
↓
중복 확인
↓
거래 수신 준비 완료

개념 예:

for (TransactionHandler handler : handlers) {
for (String serviceId : handler.serviceIds()) {
TransactionHandler previous =
handlerMap.putIfAbsent(serviceId, handler);

if (previous != null) {
throw new IllegalStateException(
"Duplicate ServiceId: " + serviceId);
}
}
}

## 10.3.7 중복 ServiceId를 허용하면 안 되는 이유

Handler A
→ SV.Customer.selectSummary

Handler B
→ SV.Customer.selectSummary

중복을 허용하면 실행 결과가 다음 요소에 따라 달라질 수 있다.

Bean 등록순서
ClassPath 순서
Spring Scan 순서
배포환경

이는 동일 요청이 환경마다 다른 프로그램으로 실행될 수 있음을 의미한다.

따라서 중복은 경고로 끝내지 않고 기동 실패로 처리하는 것이 안전하다.

## 10.3.8 미등록 ServiceId

화면 요청
↓
ServiceId = SV.Customer.selectUnknown
↓
handlerMap 조회
↓
Handler 없음
↓
SERVICE\_NOT\_FOUND
↓
ETF 업무 실패 응답

확인 항목:

| 원인 | 확인 |
| --- | --- |
| UI 오타 | 실제 Payload |
| Handler 등록 누락 | serviceIds() |
| Bean Scan 누락 | Package·Component Scan |
| 모듈 미포함 | Gradle Dependency |
| Profile 제외 | Conditional Bean |
| 폐기 ServiceId | Catalog 상태 |
| 배포 버전 불일치 | Commit·Artifact |

## 10.3.9 Handler Bean 미등록

클래스가 존재해도 Spring Bean이 아니면 Dispatcher에 등록되지 않는다.

확인:

@Component
@Service
@Bean
Component Scan 범위
Conditional Property
Profile

예:

클래스 존재
\+ 컴파일 성공
\- Spring Bean 등록
\= 실행 불가

## 10.3.10 Handler 등록 로그

기동 시 다음 정보를 구조화해 남기는 것이 좋다.

application=sv-service
handler=SvCustomerHandler
serviceIds=\[
SV.Customer.selectSummary,
SV.Customer.selectProducts,
SV.Customer.updateMemo
\]
count=3

주의:

현재 샘플 소스의 일부 System.out 출력은 교육·진단 목적일 수 있다. 운영환경에서는 구조화 Logging을 사용하고 개인정보와 요청 원문을 출력하지 않는다.

## 10.3.11 Handler와 Facade 연결

Handler에서 확인할 핵심은 어떤 Facade Method를 호출하는가이다.

| ServiceId | Handler | Facade Method |
| --- | --- | --- |
| SV.Customer.selectSummary | SvCustomerHandler | selectSummary() |
| SV.Customer.selectProducts | SvCustomerHandler | selectProducts() |
| SV.Customer.updateMemo | SvCustomerHandler | updateMemo() |

ServiceId와 Facade Method를 원칙적으로 일대일로 연결하면 유스케이스와 트랜잭션 경계를 이해하기 쉽다.

## 10.3.12 Handler 등록과 OM Catalog

Handler 등록은 코드 관점의 실행 가능성을 의미한다.

OM Catalog 등록은 운영 관점의 허용과 통제 가능성을 의미한다.

Handler 등록
\= 프로그램이 존재한다.

Catalog 등록
\= 운영에서 관리할 거래로 인정한다.

둘 중 하나만 존재하면 문제가 발생한다.

| Handler | Catalog | 결과 |
| --- | --- | --- |
| O | O | 정상 후보 |
| O | X | 개발 성공·운영 차단 가능 |
| X | O | 운영정보만 있고 실행 불가 |
| X | X | 존재하지 않는 거래 |

## 10.3.13 운영 등록정보 확인

| 등록 영역 | 확인 내용 |
| --- | --- |
| Service Catalog | 사용 여부·담당 업무 |
| 거래통제 | 허용·중지·시간대 |
| Timeout | 제한시간 |
| 권한 | 기능·업무 권한 |
| 감사 | 감사대상 여부 |
| 개인정보 | 마스킹·조회 사유 |
| 오류코드 | 사용자 메시지 |
| 성능 | 목표 응답시간 |
| 배포 | 적용 버전 |

## 10.3.14 Timeout 정합성

OM Timeout
↓
TCF TimeoutExecutor
↓
Facade Transaction Timeout
↓
외부 Client Timeout
↓
DB Query Timeout

일반 원칙:

외부 Client·DB Timeout
< 전체 TCF Timeout

또는 최소한 전체 Timeout 전에 하위 호출이 종료될 수 있도록 예산을 배분해야 한다.

예:

| 구간 | Timeout |
| --- | --- |
| TCF 전체 | 3,000ms |
| 외부 Client | 2,000ms |
| DB Query | 1,500ms |
| Connection 획득 | 500ms |

코드·OM·DB Timeout이 서로 다르면 실제 장애 시 처리 결과가 불명확해진다.

## 10.3.15 Handler 등록 완료 조건

ServiceId 등록
\+ 중복 없음
\+ Spring Bean 등록
\+ Facade 연결
\+ Catalog 등록
\+ Timeout 등록
\+ 권한·감사 등록
\+ 정상·미등록 테스트

# 10.4 호출 경로를 정방향으로 추적하기

## 10.4.1 정방향 추적의 기준

정방향 추적은 사용자의 행동에서 시작하여 최종 데이터 처리까지 이동한다.

화면 이벤트
→ ServiceId
→ 공통 진입
→ Handler
→ 업무 계층
→ SQL
→ DB
→ 표준 응답
→ 화면 처리

단순히 Java Method 호출만 따라가는 것이 아니다.

다음 네 가지를 함께 추적한다.

코드 경로
설정 경로
데이터 경로
운영 증거

## 10.4.2 전체 정방향 경로

화면 ID
↓
이벤트 ID
↓
요청 생성 함수
↓
StandardRequest
↓
Gateway 또는 JWT Filter
↓
OnlineTransactionController
↓
TCF.process()
↓
STF.preProcess()
↓
OnlineTransactionTimeoutExecutor
↓
TransactionDispatcher.dispatch()
↓
TransactionHandler.handle()
↓
Handler.doHandle()
↓
Facade
↓
Service
├─ Rule
├─ DAO
│ ↓
│ Mapper Interface
│ ↓
│ Mapper XML
│ ↓
│ SQL
│ ↓
│ DB
└─ Client
↓
외부 시스템
↓
ETF
↓
StandardResponse
↓
화면 Callback

TCF의 공통 실행 흐름은 Controller, STF, Timeout Executor, Dispatcher, Handler와 ETF 순서로 통제된다.

## 10.4.3 1단계: 화면 이벤트 확인

확인 질문:

어떤 사용자 행동인가?

어떤 조건에서 실행되는가?

중복 클릭이 가능한가?

어떤 입력값을 보내는가?

성공·실패 후 화면 상태는 무엇인가?

증적:

화면 정의서
이벤트 함수
Network Payload
화면 캡처

## 10.4.4 2단계: StandardRequest 확인

확인 항목:

| Header | Body |
| --- | --- |
| businessCode | 조회조건 |
| serviceId | 등록·변경 데이터 |
| transactionCode | 페이징 조건 |
| channelId | 파일·추가정보 |
| user·branch Context | 업무 입력 |
| idempotencyKey |  |
| trace 정보 |  |

클라이언트가 전송한 사용자 정보보다 검증된 인증 Context를 우선해야 한다.

## 10.4.5 3단계: Gateway·JWT Filter 확인

Gateway가 있는 경우:

JWT 검증
→ 업무코드·Route 결정
→ 업무 WAR 전달

Gateway가 없는 경우:

업무 WAR 공통 JWT Filter
→ 인증 Context 생성
→ Controller 진입

확인:

Token 존재
서명·만료
issuer·audience
Route
전달 Header

## 10.4.6 4단계: OnlineTransactionController

공통 Controller는 요청을 수신해 TCF로 위임한다.

확인:

Endpoint
BusinessCode 보정
Client IP
StandardRequest 변환
TCF.process() 호출

금지:

Controller에서 업무 Service 직접 호출
Controller에서 Mapper 실행
Controller에서 거래통제 구현

## 10.4.7 5단계: STF 전처리

확인 대상:

Header 검증
GUID·TraceId
TransactionContext
인증·권한
거래통제
Timeout 정책
중복 요청
거래로그 시작

STF에서 실패하면 Handler는 실행되지 않아야 한다.

## 10.4.8 6단계: Timeout Executor

업무 거래는 제한시간 안에서 실행된다.

확인:

적용 Timeout 값
실행 Thread
Timeout 발생 시 예외
업무 Thread 취소
Transaction Rollback
Connection 반환
Context 정리

Breakpoint를 너무 오래 멈추면 실제 코드 오류가 아닌 가짜 Timeout이 발생할 수 있다.

## 10.4.9 7단계: Dispatcher

런타임 분배:

StandardHeader.serviceId
↓
handlerMap.get(serviceId)
↓
TransactionHandler.handle()

대표 예:

| 입력 ServiceId | Handler |
| --- | --- |
| SV.Customer.selectSummary | SvCustomerHandler |
| SV.Customer.selectProducts | SvCustomerHandler |
| CT.Contact.selectHistory | CtContactHandler |

ServiceId 누락·미등록·중복은 서로 다른 시점과 오류로 처리된다.

## 10.4.10 8단계: Handler

확인:

담당 ServiceId
Request DTO 변환
Facade Method
미지원 Default 처리

Handler의 Breakpoint에서는 다음 값을 본다.

request.header.serviceId
request.header.businessCode
request.body
context.guid
context.traceId

민감정보 전체를 로그로 출력하지 않는다.

## 10.4.11 9단계: Facade

Facade는 유스케이스와 트랜잭션 경계다.

확인:

@Transactional
readOnly
timeout
Isolation
Service 호출순서
외부연계 포함 여부
Rollback 조건

조회:

@Transactional(readOnly = true)
public CustomerSummaryResponse selectSummary(...) {
...
}

변경:

@Transactional
public UpdateMemoResponse updateMemo(...) {
...
}

## 10.4.12 10단계: Service

Service에서는 실제 업무 처리 흐름을 확인한다.

입력 정규화
업무 Rule 호출
데이터 조회
상태 판단
결과 조립
외부연계

확인 질문:

어떤 업무 조건을 판단하는가?

데이터가 없으면 정상 빈 결과인가, 업무 오류인가?

외부 연계 실패 시 전체 거래를 실패시키는가?

여러 DAO 결과를 어떻게 조합하는가?

## 10.4.13 11단계: Rule

Rule은 업무 조건을 판단한다.

예:

고객번호 필수
조회권한
고객 상태
중복 등록
상태 전이
변경 가능 기간

원칙:

Rule은 가능한 한 부작용 없이 판단한다.

Rule이 Mapper를 직접 호출하지 않는다.

필요한 데이터는 Service가 조회해 Rule에 전달한다.

## 10.4.14 12단계: DAO

DAO는 데이터 접근을 추상화한다.

확인:

Query·Command 객체
Mapper Method
조회 결과 변환
영향 행 수 확인
DB 예외 변환

금지:

DAO에 업무 상태 전이 규칙 구현
DAO에서 화면 메시지 생성
DAO에서 다른 업무 Service 호출

## 10.4.15 13단계: Mapper Interface

확인:

Method 이름
Parameter Type
Return Type
XML Namespace
SQL ID

예:

CustomerSummaryRow selectCustomerSummary(
CustomerSummaryCriteria criteria);

## 10.4.16 14단계: Mapper XML

<mapper namespace=
"com.nh.nsight.marketing.sv.persistence.mapper.SvCustomerMapper">

<select id="selectCustomerSummary"
parameterType="CustomerSummaryCriteria"
resultType="CustomerSummaryRow">
SELECT ...
</select>
</mapper>

확인:

| 항목 | 기준 |
| --- | --- |
| Namespace | Mapper FQCN과 일치 |
| SQL ID | Method와 일치 |
| Parameter | Criteria와 일치 |
| Result | Row DTO와 일치 |
| Table | 업무 소유권 확인 |
| 조건 | 전체조회 방지 |
| 정렬 | 안정적인 순서 |
| 페이징 | 프로젝트 표준 |
| 개인정보 | 최소 조회·마스킹 |
| Timeout | 거래 예산 내 |

## 10.4.17 15단계: DB

확인:

조회·변경 테이블
Join 관계
Index
Lock
영향 행 수
Commit·Rollback
개인정보

변경 거래에서는 응답 성공만 확인하지 않고 실제 데이터와 영향 행 수도 확인한다.

## 10.4.18 16단계: ETF

ETF는 결과를 표준화하고 거래를 종료한다.

성공
→ ETF.success()

업무 오류
→ ETF.businessFail()

시스템 오류
→ ETF.systemError()

확인:

결과코드
사용자 메시지
오류코드
거래로그 종료
감사로그
Metric
Context 정리

## 10.4.19 17단계: 화면 결과 처리

화면은 HTTP 상태만 보지 않고 표준 result를 확인해야 한다.

| 결과 | 화면 행동 |
| --- | --- |
| 성공 | 데이터 표시·상태 갱신 |
| 입력 오류 | 입력항목 강조 |
| 업무 오류 | 조건 수정 안내 |
| 인증 만료 | 재인증 |
| 권한 오류 | 접근 제한 |
| Timeout | 중복 실행 주의·재조회 |
| 시스템 오류 | GUID 포함 문의 안내 |

# 대표 정방향 추적 예시

화면
SV-CUS-0001

이벤트
SV-CUS-0001-E01 고객요약 조회

UI 함수
fnSearchCustomerSummary()

ServiceId
SV.Customer.selectSummary

거래코드
SV-INQ-0001

공통 진입
OnlineTransactionController

공통 처리
TCF → STF → TimeoutExecutor → Dispatcher

Handler
SvCustomerHandler

Facade
SvCustomerFacade.selectSummary()

Service
SvCustomerService.selectSummary()

Rule
SvCustomerRule.validateInquiry()

DAO
SvCustomerDao.selectCustomerSummary()

Mapper
SvCustomerMapper.selectCustomerSummary()

SQL ID
selectCustomerSummary

Table
SV\_CUSTOMER\_SUMMARY

후처리
ETF.success()

화면
고객요약 영역 표시

# 화면–ServiceId–프로그램 추적성 매트릭스

| 추적 ID | 화면 이벤트 | ServiceId | Handler | Facade | Service | SQL ID | Table |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SV-CUS-0001-E01-C01-D01 | 고객요약 조회 | SV.Customer.selectSummary | SvCustomerHandler | selectSummary | selectSummary | selectSummary | SV\_CUSTOMER\_SUMMARY |
| SV-CUS-0001-E01-C01-D02 | 고객요약 조회 | SV.Customer.selectSummary | SvCustomerHandler | selectSummary | selectSummary | selectGrade | SV\_CUSTOMER\_GRADE |
| SV-CUS-0001-E02-C01-D01 | 보유상품 조회 | SV.Customer.selectProducts | SvCustomerHandler | selectProducts | selectProducts | selectProductList | SV\_CUSTOMER\_PRODUCT |
| SV-CUS-0001-E03-C01-D01 | 메모 저장 | SV.Customer.updateMemo | SvCustomerHandler | updateMemo | updateMemo | updateMemo | SV\_CUSTOMER\_MEMO |

# 정상 처리 흐름

화면 이벤트
↓
정상 ServiceId 전송
↓
STF 사전처리 통과
↓
Dispatcher가 단일 Handler 선택
↓
Facade 트랜잭션 시작
↓
Service·Rule 처리
↓
Mapper SQL 성공
↓
Commit
↓
ETF 성공 응답
↓
GUID·ServiceId·처리시간 기록
↓
화면 결과 표시

# 업무 오류 흐름

화면 이벤트
↓
Handler 실행
↓
Rule에서 업무조건 불충족
↓
BusinessException
↓
Rollback 필요 시 Rollback
↓
ETF.businessFail()
↓
표준 오류코드·사용자 메시지
↓
화면 조건 수정 안내

# 시스템 오류 흐름

화면 이벤트
↓
DAO·Mapper 실행
↓
DB·코드 예외
↓
System Exception 변환
↓
Rollback
↓
ETF.systemError()
↓
안전한 사용자 메시지
↓
로그에 원인 예외·GUID 기록

# Timeout 흐름

화면 이벤트
↓
TCF TimeoutExecutor 실행
↓
업무 처리가 제한시간 초과
↓
TimeoutException
↓
업무 Thread 취소 시도
↓
Rollback·Connection 반환 검증
↓
ETF Timeout 응답
↓
화면 중복 재실행 주의

# 오류 유형별 탐색 위치

| 오류 | 먼저 볼 위치 |
| --- | --- |
| 화면 버튼 무반응 | UI 이벤트 연결 |
| 요청 미발생 | Request 함수·Network |
| 401 | Gateway·JWT Filter |
| 403 | 권한·STF |
| 404 | Route·Context·Endpoint |
| 미등록 거래 | Handler·Dispatcher |
| 업무 오류 | Rule·Service |
| DB 오류 | DAO·Mapper·SQL |
| Timeout | OM·TCF·Client·DB Timeout |
| 응답 표시 오류 | Callback·Result 처리 |
| 운영만 실패 | Catalog·권한·Timeout·환경설정 |

# 정상 예시

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"processingType": "INQUIRY",
"channelId": "WEBTOP"
},
"body": {
"customerNo": "C000001234"
}
}

연결:

화면 SV-CUS-0001
이벤트 SV-CUS-0001-E01
ServiceId SV.Customer.selectSummary
Handler SvCustomerHandler
Facade SvCustomerFacade.selectSummary()
Service SvCustomerService.selectSummary()
Mapper SvCustomerMapper.selectSummary()
Table SV\_CUSTOMER\_SUMMARY
Catalog 사용
Timeout 3초
로그 GUID·TraceId·ServiceId

# 금지 예시

화면 이름과 비슷한 Service 클래스를 임의로 선택한다.

URL만 보고 업무 프로그램을 판단한다.

화면 ID와 ServiceId가 같을 것이라고 가정한다.

문서에 적힌 ServiceId만 믿고 실제 Payload를 확인하지 않는다.

테스트 코드의 ServiceId를 운영 거래로 단정한다.

두 Handler의 중복 ServiceId를 허용한다.

Handler에서 Mapper를 직접 호출한다.

Handler에 복잡한 업무 규칙을 구현한다.

Catalog 등록 없이 소스만 배포한다.

Timeout 정책 없이 기본값에 의존한다.

정적 검색만 하고 Dispatcher Call Stack을 확인하지 않는다.

HTTP 200만 보고 거래 성공으로 판단한다.

오류를 catch한 뒤 정상 응답으로 바꾼다.

요청·응답 원문과 개인정보를 로그에 출력한다.

System.out을 운영 로그 표준으로 사용한다.

# 책임 경계와 RACI

| 활동 | 업무분석 | UI 개발 | 업무개발 | 프레임워크 | OM·운영 | 아키텍트 |
| --- | --- | --- | --- | --- | --- | --- |
| 화면 ID 정의 | R | C | I | I | C | A |
| 이벤트 ID 정의 | R | R | C | I | I | A |
| ServiceId 설계 | C | C | R | C | C | A |
| UI 요청 구현 | C | R/A | C | I | I | C |
| Handler 등록 | I | I | R/A | C | I | C |
| Dispatcher 관리 | I | I | C | R/A | I | C |
| Facade·Service 구현 | C | I | R/A | C | I | C |
| Catalog 등록 | C | I | C | C | R/A | C |
| 거래통제·Timeout | C | I | C | C | R | A |
| 추적성 매트릭스 | R | R | R | C | C | A |
| 중복 자동검증 | I | I | C | R | C | A |
| 운영 로그 검증 | I | I | C | C | R/A | C |

# 자동검증 및 품질 Gate

## 1\. 화면–ServiceId 정합성

화면 이벤트 정의서
↕
UI 소스
↕
Network Payload
↕
Handler serviceIds()

차단 조건:

| 조건 | 결과 |
| --- | --- |
| 화면에 ServiceId 없음 | 설계 Gate 실패 |
| UI와 설계서 값 불일치 | Build·검토 실패 |
| UI에 폐기 ServiceId | 배포 실패 |
| Handler 없는 화면 ServiceId | 배포 실패 |

## 2\. Handler 등록 Gate

모든 Handler Bean 수집
→ ServiceId 목록 추출
→ 중복 확인
→ 빈 값 확인
→ 명명규칙 확인

차단:

중복 ServiceId
빈 ServiceId
업무코드 불일치
지원하지 않는 형식

## 3\. Catalog 정합성 Gate

Handler ServiceId
↕
OM Service Catalog
↕
거래통제
↕
Timeout
↕
권한

| 불일치 | 결과 |
| --- | --- |
| Handler만 존재 | 배포 차단 |
| Catalog만 존재 | 배포 차단 |
| Timeout 미등록 | 정책에 따라 경고·차단 |
| 업무코드 불일치 | 배포 차단 |
| 폐기 ServiceId 호출 | 배포 차단 |

## 4\. 프로그램 계층 Gate

Handler → Facade
Facade → Service
Service → Rule·DAO·Client
DAO → Mapper

금지 자동검증:

Handler → Mapper
Controller → Service
Rule → Mapper
업무 WAR → 다른 업무 WAR Java Import

## 5\. Mapper Gate

Mapper Interface Method
↕
XML SQL ID

Interface FQCN
↕
XML Namespace

차단:

Namespace 불일치
SQL ID 누락
Result Type 불일치
Mapper XML 산출물 누락

## 6\. 추적성 Gate

필수 연결:

화면 ID
→ 이벤트 ID
→ ServiceId
→ Handler
→ Facade
→ Service
→ Mapper SQL
→ Table
→ Test Case

누락 연결이 있는 신규 거래는 완료로 판정하지 않는다.

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| TRC-001 | 화면 ID 검색 | 화면 정의·소스 확인 |
| TRC-002 | 이벤트 ID 검색 | UI 객체와 함수 확인 |
| TRC-003 | Network Payload 확인 | 실제 ServiceId 확인 |
| TRC-004 | ServiceId 전체 검색 | UI·Handler·Catalog 확인 |
| TRC-005 | 상수형 ServiceId | 실제 문자열 확인 |
| TRC-006 | 동적 조합 ServiceId | 생성규칙 확인 |
| TRC-007 | 정상 Handler 등록 | 단일 Handler 선택 |
| TRC-008 | 중복 Handler 등록 | 기동 실패 |
| TRC-009 | 미등록 ServiceId | SERVICE\_NOT\_FOUND |
| TRC-010 | Handler Bean 누락 | 거래 실행 불가 |
| TRC-011 | Catalog 미등록 | 운영 차단 |
| TRC-012 | Handler·Catalog 불일치 | 배포 Gate 실패 |
| TRC-013 | 화면 초기화 다중 호출 | 호출순서 확인 |
| TRC-014 | 하나의 이벤트 다중 거래 | 전체 결과 조립 확인 |
| TRC-015 | 여러 화면 공통 ServiceId | 영향 화면 모두 식별 |
| TRC-016 | Handler→Facade | 올바른 Method 확인 |
| TRC-017 | Facade Transaction | 경계 확인 |
| TRC-018 | Rule 업무 오류 | 표준 업무 실패 |
| TRC-019 | Mapper 정상 조회 | SQL·결과 Mapping 성공 |
| TRC-020 | Mapper Namespace 오류 | 기동·실행 실패 |
| TRC-021 | DB 오류 | Rollback·시스템 오류 |
| TRC-022 | Timeout | 취소·Rollback·반환 확인 |
| TRC-023 | GUID 로그 | 전체 경로 연결 |
| TRC-024 | TraceId 외부연계 | 시스템 간 호출 연결 |
| TRC-025 | HTTP 200 업무 오류 | 화면 업무오류 처리 |
| TRC-026 | 폐기 ServiceId 호출 | 차단 |
| TRC-027 | 화면권한만 존재 | 거래권한 오류 확인 |
| TRC-028 | 거래권한만 존재 | 화면 접근성 검토 |
| TRC-029 | UI·설계서 불일치 | 품질 Gate 실패 |
| TRC-030 | 화면–SQL 매트릭스 | 역추적 가능 |
| TRC-031 | 변경 ServiceId 영향분석 | 화면·OM·테스트 식별 |
| TRC-032 | 다른 개발자 재현 | 동일 경로 확인 |

# 따라 하는 실무 절차

## 1단계. 분석 기준 기록

Branch
Commit ID
화면 ID
화면명
이벤트명
분석 목적

## 2단계. 화면 ID 검색

rg -F "SV-CUS-0001" .

완료 증적:

화면 정의 파일
이벤트 Script
메뉴·권한 위치

## 3단계. 이벤트 함수 확인

btnSearch
→ fnSearch()
→ Request Builder

완료 증적:

UI 객체 ID
Event 함수
호출 조건

## 4단계. 실제 Network 요청 확인

POST URL
Header
ServiceId
거래코드
Body

## 5단계. ServiceId 전체 검색

rg -F "SV.Customer.selectSummary" .

검색 결과를 UI·Handler·Catalog·Test·문서로 분류한다.

## 6단계. Handler 등록 확인

serviceId()
또는
serviceIds()

확인:

중복 없음
Bean 등록
Facade 연결

## 7단계. 운영 등록 확인

Catalog
거래통제
Timeout
권한
감사

## 8단계. Handler부터 정방향 추적

Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ SQL
→ Table

## 9단계. Breakpoint로 검증

Breakpoint:

OnlineTransactionController
TCF.process()
STF.preProcess()
TransactionDispatcher.dispatch()
Handler.doHandle()
Facade Method
Mapper Method
ETF

## 10단계. 정상·오류 거래 실행

정상 고객번호
없는 고객번호
미등록 ServiceId
DB 오류
Timeout

## 11단계. 로그·DB 결과 확인

GUID
TraceId
ServiceId
처리 단계
SQL ID
영향 행 수
Result

## 12단계. 추적성 매트릭스 작성

화면
→ 이벤트
→ ServiceId
→ Handler
→ 프로그램
→ SQL
→ Table
→ Test

# 완료 체크리스트

## 화면·이벤트

| 확인 항목 | 완료 |
| --- | --- |
| 화면 ID를 확인했다. | □ |
| 이벤트 ID를 확인했다. | □ |
| UI 객체 ID를 확인했다. | □ |
| 이벤트 함수를 확인했다. | □ |
| 호출 조건을 확인했다. | □ |
| 초기화 거래를 확인했다. | □ |
| 성공·실패 Callback을 확인했다. | □ |
| Network Payload를 확인했다. | □ |

## ServiceId

| 확인 항목 | 완료 |
| --- | --- |
| 실제 요청 ServiceId를 확인했다. | □ |
| 전체 저장소에서 검색했다. | □ |
| 상수·Enum 여부를 확인했다. | □ |
| 유사·Legacy ServiceId를 구분했다. | □ |
| 거래코드를 확인했다. | □ |
| 업무코드·도메인·행위를 해석했다. | □ |
| 화면–ServiceId 매핑표를 작성했다. | □ |

## Handler·운영 등록

| 확인 항목 | 완료 |
| --- | --- |
| 담당 Handler를 확인했다. | □ |
| serviceId()·serviceIds()를 확인했다. | □ |
| 중복 등록이 없음을 확인했다. | □ |
| Spring Bean 등록을 확인했다. | □ |
| Facade Method를 확인했다. | □ |
| OM Catalog를 확인했다. | □ |
| 거래통제를 확인했다. | □ |
| Timeout 정책을 확인했다. | □ |
| 권한·감사 등록을 확인했다. | □ |

## 정방향 추적

| 확인 항목 | 완료 |
| --- | --- |
| Controller부터 Handler까지 추적했다. | □ |
| Facade 트랜잭션을 확인했다. | □ |
| Service 업무 흐름을 확인했다. | □ |
| Rule 조건을 확인했다. | □ |
| DAO·Mapper를 확인했다. | □ |
| XML Namespace·SQL ID를 확인했다. | □ |
| Table·영향 행 수를 확인했다. | □ |
| ETF 결과를 확인했다. | □ |
| 화면 결과 처리를 확인했다. | □ |
| GUID 로그로 전체 경로를 검증했다. | □ |

# 변경·호환성·폐기 관리

## ServiceId 변경 원칙

ServiceId는 단순 문자열 Rename으로 변경하지 않는다.

영향 대상:

화면 소스
화면 정의서
Handler
OM Catalog
거래통제
Timeout
권한
감사
테스트
로그 조회조건
연계 시스템

## 호환 변경

기존 ServiceId를 유지하면서 요청·응답을 확장할 때:

선택 필드 추가
기본값 정의
구버전 호출 정상
응답 필드 하위 호환
계약테스트

## 비호환 변경

다음은 신규 ServiceId 또는 Version 전략을 검토한다.

필수 입력 추가
필드 의미 변경
결과 구조 대폭 변경
업무 처리 의미 변경
데이터 소유권 변경

## ServiceId 폐기 절차

신규 화면 호출 차단
↓
사용 화면·연계 조사
↓
대체 ServiceId 제공
↓
Deprecation 기간 운영
↓
OM Catalog 비활성
↓
Handler 등록 제거
↓
거래통제·Timeout 폐기
↓
로그·문서 보존

## 화면 폐기 절차

메뉴 비활성
권한 제거
이벤트 호출 제거
공통 ServiceId 사용 여부 확인
UI 소스 폐기
테스트 갱신
추적성 매트릭스 상태 변경

화면이 폐기되더라도 다른 화면이 동일 ServiceId를 사용한다면 Handler를 함께 제거해서는 안 된다.

# 시사점

## 핵심 아키텍처 판단

NSIGHT TCF에서 화면과 서버 프로그램을 연결하는 중심축은 URL이나 Controller 클래스명이 아니라 ServiceId다.

화면 ID
\= 사용자의 작업공간 식별

이벤트 ID
\= 사용자의 행동 식별

ServiceId
\= 서버 업무 거래 식별

거래코드
\= 운영 통제·감사 분류

이 네 식별자를 혼용하지 않아야 한다.

또한 ServiceId가 소스에 존재한다는 사실만으로 거래가 완성되는 것은 아니다.

UI 요청
\+ Handler 등록
\+ Catalog
\+ 거래통제
\+ Timeout
\+ 권한
\+ 테스트
\+ 로그

가 일치해야 완성된 거래다.

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| 화면명으로 클래스 추측 | 잘못된 프로그램 수정 |
| URL 중심 탐색 | TCF 분배 구조 누락 |
| ServiceId 오타 | 미등록 거래 |
| 중복 ServiceId | 비결정적 실행 |
| Handler Bean 누락 | 실행 불가 |
| Catalog 누락 | 개발 성공·운영 실패 |
| Timeout 불일치 | 취소·Rollback 불명확 |
| 화면–ServiceId 다중성 누락 | 영향분석 오류 |
| 문서만 확인 | 실제 Payload와 불일치 |
| 정적 검색만 수행 | Runtime 분기 누락 |
| Legacy 혼동 | 사용하지 않는 코드 변경 |
| 개인정보 로그 | 보안·감사 위반 |

## 우선 보완 과제

1.  화면 이벤트–ServiceId 매핑표를 공식 산출물로 관리한다.
2.  UI 소스의 ServiceId를 자동 추출한다.
3.  Handler의 serviceIds() 목록을 자동 추출한다.
4.  UI ServiceId와 Handler 등록정보를 CI에서 대조한다.
5.  Handler와 OM Catalog를 배포 전 자동 대조한다.
6.  중복 ServiceId 발생 시 빌드를 차단한다.
7.  미사용·폐기 ServiceId를 주기적으로 식별한다.
8.  화면–ServiceId–SQL 추적성 매트릭스를 자동 생성한다.
9.  기동 로그에 Handler별 등록 ServiceId를 구조화해 기록한다.
10.  대표 화면 이벤트 Smoke Test를 자동화한다.
11.  GUID·TraceId·ServiceId를 모든 업무 로그에 포함한다.
12.  ServiceId 변경을 일반 Rename이 아닌 변경관리 대상으로 운영한다.

## 중장기 발전 방향

수동 화면 분석
↓
화면 이벤트 Catalog
↓
UI ServiceId 자동 추출
↓
Handler·OM 자동 대조
↓
프로그램·SQL 추적 자동화
↓
변경 영향 자동 분석
↓
화면·거래·프로그램·운영 통합 Catalog

# 마무리말

화면에서 소스를 찾는 과정은 다음 질문에 순서대로 답하는 일이다.

어느 화면인가?

어떤 사용자 이벤트인가?

실제 요청에 어떤 ServiceId가 들어가는가?

어느 Handler가 그 ServiceId를 등록했는가?

Handler는 어느 Facade를 호출하는가?

Facade의 트랜잭션 경계는 어디인가?

Service와 Rule은 무엇을 판단하는가?

어떤 Mapper SQL과 테이블을 사용하는가?

결과는 ETF에서 어떻게 표준화되는가?

화면은 성공과 실패를 어떻게 처리하는가?

운영에서는 어떤 GUID로 다시 찾을 수 있는가?

제10장에서 기억할 핵심 경로는 다음과 같다.

화면 ID
→ 이벤트 ID
→ UI 이벤트 함수
→ StandardRequest
→ ServiceId
→ Handler 등록
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ SQL
→ Table
→ ETF
→ 화면 Callback
→ GUID 로그

가장 중요한 원칙은 다음과 같다.

이름이 비슷한 프로그램을 찾는 것
≠ 거래를 추적하는 것

화면이 전송한 ServiceId와
Dispatcher가 선택한 Handler를 기준으로
코드·설정·데이터·로그를 연결하는 것
\= 거래를 추적하는 것

다음 장에서는 SQL ID와 테이블에서 출발하여 Mapper·DAO·Service·Handler·ServiceId와 영향 화면까지 거꾸로 찾아가는 역추적 방법을 학습한다.
