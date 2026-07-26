NSIGHT 표준 거래설계서

SV.Customer.selectSummary — 고객 종합정보 조회

1. 도입 전 안내말

NSIGHT에서 거래설계서는 단순히 요청·응답 JSON을 정의하는 문서가 아니다.

거래 하나가 화면에서 어떻게 시작되고, TCF 공통 통제를 거쳐 어떤 업무 프로그램과 SQL을 실행하며, 정상·오류·Timeout 상황에서 어떻게 종료되는지를 정의하는 End-to-End 실행 설계서다.

```
화면 이벤트
  ↓
표준 요청 전문
  ↓
공통 Online Endpoint
  ↓
TCF.process()
  ↓
STF.preProcess()
  ├─ Header 검증
  ├─ 인증·권한 검증
  ├─ 거래통제
  ├─ Timeout
  ├─ 중복요청 확인
  └─ 거래로그 시작
  ↓
TransactionDispatcher
  ↓ ServiceId
Handler
  ↓
Facade
  ↓
Service
  ├─ Rule
  └─ DAO → Mapper → SQL → DB
  ↓
ETF
  ├─ 정상 응답
  ├─ 업무 오류
  └─ 시스템 오류
  ↓
화면 결과 처리
```

TCF의 표준 실행 흐름은 OnlineTransactionController → TCF → STF → TimeoutExecutor → Dispatcher → Handler → Facade → Service → Rule → DAO → Mapper → ETF이며, STF 전처리가 실패하면 Handler와 업무 트랜잭션은 실행하지 않는 것이 기본 원칙이다.

업무 프로그램은 Handler → Facade → Service → Rule → DAO → Mapper 구조를 따르며, Handler는 ServiceId 분기와 Facade 호출만 담당하고 트랜잭션 경계는 Facade에 둔다.

본 문서의 클래스명·테이블명은 NSIGHT 표준 설계 예시다. 실제 소스가 확정되면 동일한 형식을 유지하면서 실제 프로그램명과 DB 객체명으로 교체한다.

2. 문서 개요

2.1 목적

본 설계서의 목적은 SV.Customer.selectSummary 거래에 대한 다음 기준을 정의하는 것이다.

| 구분 | 목적 |
| --- | --- |
| 거래 식별 | ServiceId와 거래코드의 역할 정의 |
| 호출 조건 | 어떤 화면 이벤트에서 거래가 실행되는지 정의 |
| 전문 표준 | Header·Body·Result 구조 정의 |
| 공통 통제 | 인증·권한·거래통제·Timeout·감사 기준 정의 |
| 프로그램 구조 | Handler 이하 프로그램 호출 관계 정의 |
| 데이터 처리 | DAO·Mapper·SQL·DB 객체 관계 정의 |
| 트랜잭션 | DB 트랜잭션 시작·종료·Rollback 기준 정의 |
| 오류 처리 | 업무 오류·시스템 오류·Timeout 처리 정의 |
| 운영 추적 | GUID·TraceId·거래로그·감사로그 기준 정의 |
| 품질 검증 | 테스트와 자동 품질 Gate 정의 |
| 변경관리 | 거래 변경·호환·폐기 절차 정의 |

2.2 적용범위

| 영역 | 적용 대상 |
| --- | --- |
| 화면 | SV-CUS-0001 고객 종합정보 조회 |
| 화면 이벤트 | SV-CUS-0001-E01 고객요약 조회 |
| 업무코드 | SV |
| 업무 WAR | sv-service |
| Context Path | /sv |
| Endpoint | POST /sv/online |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 처리유형 | 조회 INQUIRY |
| 프로그램 | Handler·Facade·Service·Rule·DAO·Mapper |
| 데이터 | 고객요약·고객등급·상품현황 |
| 운영 | OM Catalog·거래통제·Timeout·거래로그·감사로그 |

2.3 대상 독자

| 대상 | 활용 목적 |
| --- | --- |
| 업무 분석가 | 거래 목적과 업무 규칙 확인 |
| UI 개발자 | 요청·응답 전문과 오류 처리 구현 |
| 업무 개발자 | Handler 이하 프로그램 구현 |
| 프레임워크팀 | STF·ETF 공통 통제 검토 |
| 아키텍트 | 책임 경계와 구조 적합성 검증 |
| DBA·DA | SQL·DB 객체와 데이터 권한 검토 |
| 보안 담당자 | 인증·권한·개인정보·감사 검토 |
| 운영 담당자 | 거래 추적과 장애 대응 |
| 테스트 담당자 | 정상·오류·성능·장애 시험 작성 |
| PMO·품질 담당자 | 설계 완전성과 추적성 검증 |

2.4 선행조건

- 화면 ID와 이벤트 ID가 확정되어 있어야 한다.
- 업무코드 SV와 Context Path /sv가 등록되어 있어야 한다.
- ServiceId와 거래코드가 OM에 등록되어 있어야 한다.
- Handler가 Spring Bean으로 등록되어 있어야 한다.
- 거래통제와 Timeout 정책이 정의되어 있어야 한다.
- 사용자·지점·채널 인증 문맥이 생성되어 있어야 한다.
- 고객정보 조회권한과 데이터 범위가 정의되어 있어야 한다.
- 요청·응답 DTO와 Mapper SQL이 식별되어 있어야 한다.
- 개인정보 마스킹 기준이 승인되어 있어야 한다.
2.5 용어 정의

| 용어 | 정의 |
| --- | --- |
| 거래 | 하나의 업무 목적을 수행하는 논리적 실행 단위 |
| ServiceId | Dispatcher가 실행할 Handler를 선택하는 논리 거래 식별자 |
| 거래코드 | 거래통제·감사·통계·운영 분류를 위한 식별자 |
| 표준 전문 | Header·Body·Result로 구성되는 공통 요청·응답 형식 |
| STF | Handler 실행 전 공통 검증과 거래 준비를 수행하는 전처리 |
| ETF | 업무 처리 후 응답과 거래로그를 종료하는 후처리 |
| TransactionContext | 요청 처리 동안 유지되는 거래 문맥 |
| GUID | 시스템 간 거래를 연결하는 전역 추적 식별자 |
| TraceId | 애플리케이션 내부 호출을 연결하는 추적 식별자 |
| 업무 오류 | 정상적인 업무 판단으로 처리할 수 없는 조건 |
| 시스템 오류 | 프로그램·DB·네트워크·인프라에서 발생한 비정상 오류 |
| Timeout | 지정된 거래 제한시간을 초과한 상태 |

3. 문제 정의 및 설계 배경

3.1 문제 정의

고객 종합정보 조회는 고객 기본정보, 등급, 상품정보 등 여러 데이터를 조합하는 거래다.

거래설계가 불명확하면 다음 문제가 발생할 수 있다.

| 문제 | 영향 |
| --- | --- |
| 화면 버튼과 ServiceId 연결 누락 | 거래 시작점을 알 수 없음 |
| ServiceId와 거래코드 혼용 | 라우팅과 운영 통제 기준이 불명확 |
| Handler에서 직접 DB 호출 | 계층 책임과 테스트 구조 훼손 |
| Rule에서 DB 조회 | 업무 규칙과 데이터 접근 결합 |
| Timeout 정책 누락 | 느린 SQL로 Thread·Pool 장기 점유 |
| 사용자 ID를 화면에서 전달 | 사용자 위·변조 위험 |
| 결과 없음과 오류를 동일 처리 | 사용자와 운영자의 판단 오류 |
| 일부 데이터 실패 기준 미정의 | 전체 거래 성공 여부 불명확 |
| 거래로그 종료 누락 | PROCESSING 상태가 장기 잔존 |
| SQL 변경 추적성 부족 | 영향 화면과 ServiceId 식별 불가 |

3.2 설계 배경

화면설계서에서는 고객요약 조회 버튼을 다음 거래와 연결한다.

| 항목 | 내용 |
| --- | --- |
| 화면 ID | SV-CUS-0001 |
| 이벤트 ID | SV-CUS-0001-E01 |
| UI 객체 | btnSearch |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 요청 DTO | SvCustomerSummaryRequest |
| 응답 DTO | SvCustomerSummaryResponse |
| Timeout | 3초 |
| 권한 | SV_CUSTOMER_VIEW |
| 감사 대상 | 예 |

화면 이벤트부터 ServiceId, Handler, SQL과 DB 객체까지 한 행으로 연결하는 것이 전체 추적성 기준이다.

4. 현행 구조와 문제점

4.1 비표준 현행 구조

다음과 같은 업무별 REST API 구조는 적용하지 않는다.

```
화면
  ↓
POST /sv/customer/summary
  ↓
SvCustomerController
  ↓
SvCustomerService
  ↓
SvCustomerMapper
```

문제점

| 구분 | 문제 |
| --- | --- |
| 거래 식별 | URL과 Controller가 거래 식별자가 됨 |
| 공통 통제 | TCF·STF를 우회할 가능성 |
| 운영 등록 | OM Service Catalog와 연결이 어려움 |
| Timeout | Controller별 개별 구현 가능성 |
| 오류 응답 | 업무별 응답 형식이 달라질 수 있음 |
| 추적성 | ServiceId 중심의 로그 조회 불가 |
| 권한 | Controller마다 권한검증이 중복됨 |
| 유지보수 | 공통 정책 변경 시 전체 Controller 수정 |

4.2 목표 구조

```
화면
  ↓
POST /sv/online
  ↓
OnlineTransactionController
  ↓
TCF.process()
  ↓
STF.preProcess()
  ↓
TransactionDispatcher
  ↓ SV.Customer.selectSummary
SvCustomerHandler
  ↓
SvCustomerFacade
  ↓
SvCustomerService
  ├─ SvCustomerInquiryRule
  ├─ SvCustomerDao
  └─ SvCustomerGradeDao
```

5. 요구사항과 제약조건

5.1 기능 요구사항

| ID | 요구사항 |
| --- | --- |
| FR-01 | 고객번호 또는 허용된 검색조건으로 고객을 조회해야 한다. |
| FR-02 | 사용자 지점과 데이터 권한을 검증해야 한다. |
| FR-03 | 고객 기본정보를 조회해야 한다. |
| FR-04 | 고객등급을 함께 조회해야 한다. |
| FR-05 | 조회 가능한 상품현황을 반환해야 한다. |
| FR-06 | 개인정보는 권한에 따라 마스킹해야 한다. |
| FR-07 | 고객 미존재는 정상적인 결과 없음으로 구분해야 한다. |
| FR-08 | 중요 고객정보 조회는 감사로그를 기록해야 한다. |
| FR-09 | 모든 결과에 GUID와 TraceId를 반환해야 한다. |
| FR-10 | 거래 제한시간을 초과하면 Timeout으로 종료해야 한다. |

5.2 비기능 요구사항

| 항목 | 기준 |
| --- | --- |
| 응답시간 | p95 3초 이내 |
| DB 처리시간 | 정상 목표 100~300ms |
| 가용성 | 부가정보 일부 실패 정책 명확화 |
| 보안 | 인증된 사용자만 실행 |
| 권한 | 지점·역할·기능권한 검증 |
| 개인정보 | 최소조회·마스킹·감사 적용 |
| 추적성 | GUID·TraceId·ServiceId 유지 |
| 일관성 | ETF 표준 응답 사용 |
| 운영성 | 거래 시작·종료 상태 기록 |
| 확장성 | 화면과 무관하게 ServiceId 재사용 가능 |

5.3 제약조건

- 업무별 Controller를 생성하지 않는다.
- Dispatcher 라우팅 키는 ServiceId만 사용한다.
- URL이나 화면번호로 Handler를 선택하지 않는다.
- Handler에서 DAO·Mapper를 직접 호출하지 않는다.
- Rule에서 DB나 외부 시스템을 호출하지 않는다.
- 트랜잭션은 Facade에 선언한다.
- 사용자·지점 정보는 인증 문맥을 신뢰 원천으로 사용한다.
- SQL 원문과 개인정보를 거래로그에 저장하지 않는다.
- 조회 거래에 재시도 로직을 무분별하게 적용하지 않는다.
- Timeout 발생 후 백그라운드 DB 작업이 계속 실행되지 않도록 기술 검증한다.
6. 설계 원칙

6.1 거래 식별자 분리

```
ServiceId
= 실행 프로그램을 찾는 식별자

거래코드
= 운영 통제·감사·통계 분류 식별자
```

| 구분 | ServiceId | 거래코드 |
| --- | --- | --- |
| 값 | SV.Customer.selectSummary | SV-INQ-0001 |
| 목적 | Handler 라우팅 | 통제·감사·통계 |
| 변경 빈도 | 업무 기능 변경 시 | 운영 분류 변경 시 |
| 관리 주체 | 업무팀·프레임워크팀 | 업무팀·OM 운영팀 |
| 실행 사용 | Dispatcher | STF·ETF·운영로그 |

ServiceId 형식은 {업무코드}.{도메인}.{행위}, 거래코드는 {업무코드}-{처리유형}-{일련번호}를 사용한다.

6.2 선 검증 후 실행

다음 항목 중 하나라도 실패하면 Handler를 실행하지 않는다.

- 표준 Header 형식
- 인증 문맥
- 기능권한
- 사용자·지점 정합성
- 거래 허용 정책
- Timeout 정책
- Handler 등록
- 필수 입력값
6.3 거래당 Handler 단일화

하나의 ServiceId에는 논리적으로 하나의 Handler만 연결한다.

```
SV.Customer.selectSummary
  → SvCustomerHandler
```

하나의 Handler 클래스가 여러 ServiceId를 처리할 수는 있으나, 중복 ServiceId 등록은 기동 실패로 처리한다.

6.4 트랜잭션 경계 단일화

조회 거래의 트랜잭션 경계는 Facade에 둔다.

```
@Transactional(readOnly = true, timeout = 3)
public SvCustomerSummaryResponse selectSummary(
        SvCustomerSummaryRequest request) {
    return customerService.selectSummary(request);
}
```

| 계층 | 트랜잭션 |
| --- | --- |
| Handler | 금지 |
| Facade | 허용·권장 |
| Service | 원칙적 금지 |
| Rule | 금지 |
| DAO | 금지 |
| Mapper | 적용 대상 아님 |

6.5 업무 오류와 시스템 오류 분리

```
고객 미존재·조회권한 없음
= 업무 결과 또는 업무 오류

DB 연결 실패·SQL 오류·예상하지 못한 예외
= 시스템 오류
```

6.6 개인정보 최소 반환

거래 응답은 화면 표시와 업무 수행에 필요한 필드만 반환한다.

- 조회하지 않는 컬럼은 SQL에서 제외한다.
- DTO에 불필요한 개인정보를 포함하지 않는다.
- 마스킹은 가능한 서버에서 적용한다.
- UI에 원문이 필요할 때는 별도 권한을 검증한다.
7. 대안 비교 및 의사결정

7.1 거래 구성 대안

| 대안 | 설명 | 장점 | 문제점 | 판단 |
| --- | --- | --- | --- | --- |
| A | 화면 전체를 하나의 대형 거래로 조회 | 호출 1회 | 변경 영향·장애 범위 큼 | 조건부 |
| B | 고객 기본정보·등급·상품을 각각 거래로 분리 | 독립 장애 격리 | 화면 호출 증가 | 조건부 |
| C | 핵심 고객요약은 한 거래, 부가정보는 별도 거래 | 균형적 구조 | 설계 기준 필요 | 권장 |

7.2 결정

본 거래는 다음 데이터를 하나의 고객요약 유스케이스로 처리한다.

- 고객 기본정보
- 고객등급
- 기본 상품현황
접촉이력, 캠페인이력, 다운로드는 별도 ServiceId로 분리한다.

```
핵심 고객요약
→ SV.Customer.selectSummary

고객 상세
→ SV.Customer.selectDetail

접촉이력
→ CT.Contact.selectHistory

파일 다운로드
→ SV.Customer.downloadSummary
```

결정 사유

- 최초 화면 조회에 필요한 핵심정보는 한 번에 제공한다.
- 접촉이력과 다운로드는 권한·감사·Timeout 특성이 다르므로 분리한다.
- 대량 데이터가 기본 조회 거래에 포함되는 것을 방지한다.
- 장애가 발생한 부가영역을 독립적으로 추적할 수 있다.
8. 거래 기본정보

| 항목 | 내용 |
| --- | --- |
| 거래명 | 고객 종합정보 조회 |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 업무코드 | SV |
| 업무 도메인 | Customer |
| 처리유형 | INQUIRY |
| 거래 구분 | 온라인 동기 조회 |
| 업무 WAR | sv-service |
| Context Path | /sv |
| HTTP Method | POST |
| Endpoint | /sv/online |
| 요청 DTO | SvCustomerSummaryRequest |
| 응답 DTO | SvCustomerSummaryResponse |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService |
| Rule | SvCustomerInquiryRule |
| 기본 Timeout | 3초 |
| 트랜잭션 | readOnly=true |
| 중복방지 | 미적용 |
| 인증 필요 | 예 |
| 권한코드 | SV_CUSTOMER_VIEW |
| 감사 대상 | 예 |
| 중요 거래 | 예 |
| 예상 호출 화면 | SV-CUS-0001 |
| 소유 조직 | SV 업무팀 |

9. 목표 아키텍처

```
[WEBTOPSUITE]
SV-CUS-0001
btnSearch
     │
     │ POST /sv/online
     │ ServiceId: SV.Customer.selectSummary
     ▼
[sv-service]
OnlineTransactionController
     ↓
TCF.process()
     ↓
STF.preProcess()
     ├─ HeaderValidator
     ├─ AuthenticationValidator
     ├─ AuthorizationValidator
     ├─ TransactionControl
     ├─ TimeoutPolicy
     └─ TransactionLog.start
     ↓
OnlineTransactionTimeoutExecutor
     ↓
TransactionDispatcher
     ↓
SvCustomerHandler
     ↓
SvCustomerFacade
     ↓
SvCustomerService
     ├─ SvCustomerInquiryRule
     ├─ SvCustomerDao
     │    └─ SvCustomerMapper
     └─ SvCustomerGradeDao
          └─ SvCustomerGradeMapper
     ↓
ETF
     ├─ success
     ├─ businessFail
     └─ systemError
     ↓
StandardResponse
```

10. 표준 요청 전문

10.1 요청 예시

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "channelId": "WEBTOP",
    "userId": "",
    "branchId": "",
    "guid": "",
    "traceId": "",
    "requestDtm": "20260715222031001"
  },
  "body": {
    "customerNo": "CUST000001",
    "customerName": "",
    "searchBranchId": "001234",
    "baseDate": "20260715"
  }
}
```

표준 요청 Header에는 업무코드, ServiceId, 거래코드, 처리유형, 채널, 사용자, 지점, GUID와 TraceId가 포함되어야 한다.

10.2 Header 정의

| 필드 | 유형 | 필수 | 출처 | 검증 기준 |
| --- | --- | --- | --- | --- |
| businessCode | String | 예 | UI 고정값 | SV 일치 |
| serviceId | String | 예 | 화면 이벤트 | 등록된 ServiceId |
| transactionCode | String | 예 | 화면 이벤트 | SV-INQ-0001 |
| processingType | String | 예 | UI 고정값 | INQUIRY |
| channelId | String | 예 | 채널 문맥 | 허용 채널 |
| userId | String | 예 | 인증 문맥 | 화면값 신뢰 금지 |
| branchId | String | 예 | 인증 문맥 | 사용자 소속지점 |
| guid | String | 조건 | Gateway·TCF | 없으면 생성 |
| traceId | String | 조건 | Gateway·TCF | 없으면 생성 |
| requestDtm | String | 예 | UI·Gateway | 허용 시간 편차 |

10.3 Body 정의

| 필드 | 유형 | 길이 | 필수 | 설명 | 검증 |
| --- | --- | --- | --- | --- | --- |
| customerNo | String | 20 | 조건 | 고객번호 | 영문·숫자 |
| customerName | String | 50 | 조건 | 고객명 | 특수문자 제한 |
| searchBranchId | String | 6 | 조건 | 조회 대상 지점 | 권한 범위 |
| baseDate | String | 8 | 예 | 조회 기준일 | yyyyMMdd |
| includeProducts | Boolean | 아니오 | 상품조회 여부 | 기본 true |  |

검색조건 규칙

다음 조건 중 하나를 충족해야 한다.

```
조건 1: customerNo 존재
또는
조건 2: customerName + searchBranchId 존재
```

고객번호가 존재하면 고객번호 조회를 우선한다.

11. 표준 응답 전문

11.1 정상 응답 예시

```
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "guid": "G202607150000001",
    "traceId": "T202607150000001",
    "responseDtm": "20260715222031542"
  },
  "result": {
    "code": "S0000",
    "message": "정상 처리되었습니다.",
    "detailCode": "",
    "retryable": false
  },
  "body": {
    "customer": {
      "customerNo": "CUST000001",
      "customerName": "홍*동",
      "customerTypeCode": "01",
      "customerTypeName": "개인",
      "managementBranchId": "001234",
      "managementBranchName": "서울중앙지점",
      "mobileNo": "010-****-1234",
      "email": "h***@example.com",
      "marketingConsentYn": "Y"
    },
    "grade": {
      "gradeCode": "GOLD",
      "gradeName": "GOLD",
      "evaluationDate": "20260701"
    },
    "products": [
      {
        "productCode": "DP001",
        "productName": "예금상품 A",
        "joinDate": "20250103",
        "statusCode": "ACTIVE",
        "statusName": "정상",
        "balanceAmount": 1000000
      }
    ],
    "dataAsOfDtm": "20260715222030000"
  }
}
```

11.2 결과 없음 응답

고객 미존재를 시스템 오류로 처리하지 않는다.

```
{
  "result": {
    "code": "SV-CUS-002",
    "message": "조회된 고객정보가 없습니다.",
    "retryable": false
  },
  "body": {
    "customer": null,
    "grade": null,
    "products": []
  }
}
```

11.3 응답 필드 기준

| 영역 | 필드 | 설명 | 개인정보 |
| --- | --- | --- | --- |
| Customer | customerNo | 고객 식별번호 | 예 |
| Customer | customerName | 고객명 | 예 |
| Customer | mobileNo | 휴대전화 | 예 |
| Customer | email | 이메일 | 예 |
| Customer | managementBranchId | 관리지점 | 아니오 |
| Grade | gradeCode | 고객등급 | 조건 |
| Product | productCode | 상품코드 | 아니오 |
| Product | balanceAmount | 상품 잔액 | 예 |
| Meta | dataAsOfDtm | 데이터 기준시각 | 아니오 |

12. 구성요소 및 책임

12.1 공통 구성요소

| 구성요소 | 책임 |
| --- | --- |
| OnlineTransactionController | 표준 요청 수신과 TCF 위임 |
| TCF | 전체 실행 흐름 제어 |
| STF | 실행 전 공통 통제 |
| TimeoutExecutor | 제한시간 내 Handler 실행 |
| TransactionDispatcher | ServiceId로 Handler 선택 |
| ETF | 응답·로그·감사 종료 처리 |

12.2 업무 구성요소

| 구성요소 | 책임 | 금지 |
| --- | --- | --- |
| SvCustomerHandler | Body 변환, Facade 호출 | DB·트랜잭션·응답 조립 |
| SvCustomerFacade | 유스케이스와 트랜잭션 경계 | SQL 직접 호출 |
| SvCustomerService | 조회 흐름 조립과 결과 구성 | Controller 역할 |
| SvCustomerInquiryRule | 입력·업무 규칙 검증 | DB·외부 호출 |
| SvCustomerDao | 고객요약 Mapper 호출 | 업무 판단 |
| SvCustomerGradeDao | 고객등급 Mapper 호출 | 업무 판단 |
| SvCustomerMapper | 고객요약 SQL 실행 | 권한·업무 흐름 판단 |
| SvCustomerGradeMapper | 고객등급 SQL 실행 | 권한·업무 흐름 판단 |

계층별 허용 호출은 Handler → Facade → Service → Rule/DAO → Mapper이며 역방향 호출과 계층 건너뛰기는 금지한다.

13. 프로그램 호출 설계

| 순번 | 계층 | 클래스 | 메서드 | 입력 | 출력 |
| --- | --- | --- | --- | --- | --- |
| 1 | Controller | OnlineTransactionController | online() | StandardRequest | StandardResponse |
| 2 | TCF | TCF | process() | 표준 요청 | 업무 결과 |
| 3 | STF | STF | preProcess() | 요청 Header | TransactionContext |
| 4 | Dispatcher | TransactionDispatcher | dispatch() | ServiceId·Body | 업무 결과 |
| 5 | Handler | SvCustomerHandler | handleSelectSummary() | 요청 Body | 응답 DTO |
| 6 | Facade | SvCustomerFacade | selectSummary() | 요청 DTO | 응답 DTO |
| 7 | Service | SvCustomerService | selectSummary() | 요청 DTO | 응답 DTO |
| 8 | Rule | SvCustomerInquiryRule | validateInquiry() | 요청 DTO·인증 문맥 | 검증 결과 |
| 9 | DAO | SvCustomerDao | selectSummary() | 조회조건 | 고객요약 |
| 10 | DAO | SvCustomerGradeDao | selectGrade() | 고객번호·기준일 | 등급 |
| 11 | Mapper | SvCustomerMapper | selectSummary() | 조회조건 | 고객요약 |
| 12 | Mapper | SvCustomerGradeMapper | selectGrade() | 조회조건 | 고객등급 |
| 13 | ETF | ETF | success() | 업무 결과 | 표준 응답 |

14. 정상 처리 흐름

14.1 전체 정상 흐름

```
1. 사용자가 고객번호를 입력하고 조회 버튼 클릭
2. UI가 입력값 형식 검증
3. UI가 표준 요청 전문 생성
4. POST /sv/online 호출
5. Controller가 TCF.process() 호출
6. STF가 Header·인증·권한·통제·Timeout 검증
7. 거래로그 상태를 PROCESSING으로 기록
8. Dispatcher가 SV.Customer.selectSummary Handler 조회
9. Handler가 Body를 요청 DTO로 변환
10. Facade가 읽기전용 트랜잭션 시작
11. Service가 Rule 검증 호출
12. 고객요약·등급·상품현황 DAO 호출
13. Mapper SQL 실행
14. Service가 응답 DTO 조립
15. Facade 트랜잭션 종료
16. ETF가 정상 표준 응답 생성
17. 거래로그를 SUCCESS로 종료
18. 감사로그 기록
19. UI가 고객요약 영역 표시
```

14.2 STF 상세 흐름

```
StandardHeaderValidator
  ↓
GUID·TraceId 생성 또는 검증
  ↓
TransactionContext·MDC 설정
  ↓
Session·AuthenticationContext 검증
  ↓
AuthorizationValidator
  ↓
TransactionControlService.check()
  ↓
TimeoutPolicyService.resolveAndApply()
  ↓
Idempotency 정책 확인
  ↓
TransactionLogService.start()
```

STF는 Header, 추적정보, Context, 인증, 권한, 거래통제, Timeout, 중복요청과 거래로그 시작을 Handler 실행 전에 처리한다.

15. 업무 규칙

| 규칙 ID | 규칙 | 처리 |
| --- | --- | --- |
| BR-SV-CUS-001 | 검색조건이 최소 하나 존재해야 함 | 업무 오류 |
| BR-SV-CUS-002 | 기준일은 미래일 수 없음 | 업무 오류 |
| BR-SV-CUS-003 | 사용자가 고객 조회권한을 보유해야 함 | 권한 오류 |
| BR-SV-CUS-004 | 타 지점 조회는 데이터권한 범위 내에서만 허용 | 권한 오류 |
| BR-SV-CUS-005 | 고객이 존재하지 않으면 결과 없음 반환 | 정상 결과 없음 |
| BR-SV-CUS-006 | 개인정보 원문권한이 없으면 마스킹 | 마스킹 |
| BR-SV-CUS-007 | 해지·탈퇴 고객 표시 기준을 적용 | 상태값 표시 |
| BR-SV-CUS-008 | 상품 목록은 조회 가능 상품만 반환 | 필터링 |
| BR-SV-CUS-009 | 응답 최대 상품 건수를 제한 | 초과 시 절단·페이징 |
| BR-SV-CUS-010 | 감사 대상 고객 조회는 감사로그 기록 | 감사 처리 |

15.1 Rule 실행 기준

Rule은 다음 항목만 검증한다.

- 필수값
- 형식
- 날짜 범위
- 인증 문맥의 필수 속성
- 요청값 간 논리적 관계
- 조회 조건의 업무적 유효성
Rule에서는 다음을 수행하지 않는다.

- Mapper 호출
- 외부 API 호출
- 데이터 변경
- 로그 테이블 직접 저장
- 트랜잭션 시작
- 표준 응답 생성
16. 데이터 및 SQL 설계

16.1 DAO·Mapper 매핑

| 순번 | DAO Method | Mapper Namespace | SQL ID | SQL 유형 | 대상 객체 |
| --- | --- | --- | --- | --- | --- |
| 1 | selectSummary() | SvCustomerMapper | selectCustomerSummary | SELECT | SV_CUSTOMER_SUMMARY |
| 2 | selectGrade() | SvCustomerGradeMapper | selectCustomerGrade | SELECT | SV_CUSTOMER_GRADE |
| 3 | selectProducts() | SvCustomerProductMapper | selectCustomerProducts | SELECT | SV_CUSTOMER_PRODUCT |

16.2 DB 객체

| DB 객체 | 용도 | CRUD | 주요 조건 |
| --- | --- | --- | --- |
| SV_CUSTOMER_SUMMARY | 고객 요약정보 | R | 고객번호·기준일 |
| SV_CUSTOMER_GRADE | 고객등급 | R | 고객번호·평가일 |
| SV_CUSTOMER_PRODUCT | 상품현황 | R | 고객번호·상품상태 |
| OM_COMMON_CODE | 코드명 변환 | R | 코드그룹·코드 |

16.3 SQL 설계 원칙

- SELECT *를 사용하지 않는다.
- 화면에 필요하지 않은 개인정보 컬럼을 조회하지 않는다.
- 조회조건 컬럼의 인덱스를 검토한다.
- 고객번호 조회와 고객명 조회의 실행계획을 분리 검증한다.
- 함수로 인덱스 컬럼을 감싸는 조건을 지양한다.
- 결과 건수를 제한한다.
- SQL ID는 DAO Method와 원칙적으로 일대일로 관리한다.
- Mapper에서 권한을 임의로 판단하지 않는다.
- 권한에 필요한 지점범위는 Service가 검증된 조건으로 전달한다.
17. 트랜잭션 및 상태관리

17.1 DB 트랜잭션

| 항목 | 기준 |
| --- | --- |
| 선언 위치 | SvCustomerFacade.selectSummary() |
| 유형 | 읽기전용 |
| Timeout | 3초 |
| Isolation | 기본 DB 정책 |
| Commit | 정상 종료 시 |
| Rollback | RuntimeException·시스템 오류 |
| 업무 오류 | DB 변경이 없으므로 응답 변환 후 종료 |
| Open Session | 거래 범위 내에서만 유지 |

17.2 TCF 거래 상태

```
RECEIVED
  ↓
VALIDATING
  ↓
PROCESSING
  ├─ SUCCESS
  ├─ BUSINESS_FAIL
  ├─ SYSTEM_ERROR
  ├─ TIMEOUT
  └─ UNKNOWN
```

| 상태 | 의미 |
| --- | --- |
| RECEIVED | 요청 수신 |
| VALIDATING | STF 검증 중 |
| PROCESSING | Handler 실행 시작 |
| SUCCESS | 정상 완료 |
| BUSINESS_FAIL | 업무 조건 불충족 |
| SYSTEM_ERROR | 시스템 예외 |
| TIMEOUT | 제한시간 초과 |
| UNKNOWN | 시작됐으나 정상 종료 확인 불가 |

17.3 Idempotency

본 거래는 조회 거래이므로 업무 데이터 중복 변경 위험이 없다.

| 항목 | 기준 |
| --- | --- |
| 적용 여부 | 미적용 |
| UI 중복 클릭 | 화면에서 차단 |
| 서버 중복 호출 | 허용 가능 |
| 캐시 적용 | 향후 별도 검토 |
| 감사로그 | 요청별 별도 기록 |

18. 오류·Timeout·장애 흐름

18.1 입력 오류

```
요청 Body 검증 실패
  ↓
Handler 업무 처리 미실행
  ↓
BusinessException
  ↓
ETF.businessFail()
  ↓
BUSINESS_FAIL 거래로그 종료
```

18.2 권한 오류

```
STF AuthorizationValidator 실패
  ↓
Handler 미실행
  ↓
권한 오류 응답
  ↓
거래로그·보안로그 기록
```

18.3 DB 오류

```
Mapper SQL 실행
  ↓
SQLException
  ↓
DAO 예외 변환
  ↓
Facade Rollback
  ↓
ETF.systemError()
  ↓
SYSTEM_ERROR 거래로그 종료
```

18.4 Timeout

```
TimeoutExecutor 제한시간 초과
  ↓
업무 Future 취소 요청
  ↓
DB Statement 취소 시도
  ↓
Facade Rollback
  ↓
ETF Timeout 응답
  ↓
TIMEOUT 거래로그 종료
```

Timeout 설계 주의사항

- Java Future 취소만으로 DB SQL이 반드시 중단되는 것은 아니다.
- JDBC Query Timeout과 MyBatis Statement Timeout을 함께 적용한다.
- Hikari Connection이 반환되는지 검증한다.
- Timeout 이후 늦게 완료된 결과가 정상 응답으로 전달되지 않게 한다.
- Timeout 로그에는 현재 단계와 SQL ID를 기록한다.
- 사용자에게 자동 재시도를 수행하지 않는다.
18.5 부분 데이터 오류

고객요약은 성공했지만 고객등급 조회가 실패한 상황은 두 가지 대안이 있다.

| 정책 | 설명 | 본 거래 판단 |
| --- | --- | --- |
| 전체 실패 | 하나라도 실패하면 거래 실패 | 기본 원칙 |
| 부분 성공 | 핵심정보를 반환하고 일부 오류 표시 | 업무 승인 시 적용 |

본 기준에서는 고객 기본정보와 등급을 하나의 핵심 결과로 보고 전체 실패를 기본값으로 한다. 상품현황 등 부가정보는 별도 정책으로 부분 성공을 허용할 수 있다.

19. 오류코드 설계

| 오류코드 | 구분 | 설명 | 재시도 |
| --- | --- | --- | --- |
| S0000 | 정상 | 정상 처리 | N |
| SV-CUS-001 | 업무 | 조회조건 누락 | N |
| SV-CUS-002 | 업무 | 고객 미존재 | N |
| SV-CUS-003 | 권한 | 고객 조회권한 없음 | N |
| SV-CUS-004 | 권한 | 지점 데이터권한 없음 | N |
| SV-CUS-005 | 업무 | 기준일 오류 | N |
| SV-CUS-006 | 업무 | 조회 가능 건수 초과 | 조건 |
| TCF-AUTH-001 | 인증 | 인증정보 만료 | 인증 후 |
| TCF-CTL-001 | 통제 | 운영 거래 차단 | N |
| TCF-TMO-001 | Timeout | 거래 제한시간 초과 | 조건 |
| TCF-SVC-404 | 라우팅 | ServiceId 미등록 | N |
| TCF-SYS-001 | 시스템 | 내부 시스템 오류 | 조건 |
| TCF-DB-001 | 시스템 | DB 처리 오류 | 조건 |

19.1 오류 메시지 원칙

- 사용자 메시지에는 SQL·테이블·클래스명을 포함하지 않는다.
- 내부 예외 메시지를 그대로 노출하지 않는다.
- 재시도 가능한 오류인지 표시한다.
- 운영 상세정보는 GUID로 조회한다.
- 개인정보와 요청 전문 원문을 오류 메시지에 포함하지 않는다.
20. 연계 규칙

20.1 Gateway 연계

| 항목 | 기준 |
| --- | --- |
| JWT 검증 | Gateway 또는 업무 WAR 공통 Filter |
| 사용자 정보 | 검증된 Token Claim 사용 |
| Header 보정 | Claim과 Header 불일치 시 차단 권장 |
| GUID | 기존 값 유지, 없으면 생성 |
| 업무 라우팅 | /sv 또는 업무코드 기준 |
| 직접 접근 | 네트워크 차단 또는 JWT Filter 적용 |

20.2 OM 연계

신규 거래는 다음 정보를 하나의 변경 단위로 관리한다.

```
Handler ServiceId
+ OM Service Catalog
+ 거래통제 정책
+ Timeout 정책
+ 권한코드
+ 감사 여부
+ 오류코드
+ 운영상태
```

OM은 Service Catalog, 거래통제, Timeout, 감사 여부, 중요 거래 여부와 운영 상태를 관리하며 TCF는 실행 단계에서 이를 적용한다.

OM 등록정보

| 항목 | 값 |
| --- | --- |
| ServiceId | SV.Customer.selectSummary |
| 서비스명 | 고객 종합정보 조회 |
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

20.3 업무 WAR 간 연계

다른 업무 WAR가 고객요약을 호출할 때 Java Bean을 직접 참조하지 않는다.

```
호출 업무 Service
  ↓
TcfServiceClient
  ↓
표준 전문
  ↓
SV /online
  ↓
SV.Customer.selectSummary
```

전달 항목:

- 원거래 GUID
- 신규 TraceId 또는 SpanId
- 호출자 ServiceId
- 대상 ServiceId
- 사용자·지점·채널
- 내부 호출 표시
- 남은 Timeout
21. 성능·용량·확장성

21.1 성능 목표

| 구분 | 목표 |
| --- | --- |
| 전체 p95 | 3초 이하 |
| STF | 50ms 이하 목표 |
| Rule | 10ms 이하 목표 |
| 고객요약 SQL | 300ms 이하 목표 |
| 등급 SQL | 200ms 이하 목표 |
| 상품 SQL | 300ms 이하 목표 |
| 응답 조립 | 50ms 이하 목표 |
| 거래로그 비동기 처리 | 업무 응답 영향 최소화 |

21.2 조회 건수 제한

| 항목 | 기준 |
| --- | --- |
| 고객 기본정보 | 1건 |
| 고객등급 | 1건 |
| 상품현황 | 최대 100건 |
| 코드정보 | 캐시 활용 |
| 전체 응답 크기 | 업무 기준 이내 제한 |

21.3 확장 기준

다음 상황에서는 거래를 분리한다.

- 상품현황이 대량 목록으로 증가하는 경우
- 고객등급 산정이 실시간 연산으로 변경되는 경우
- 외부 시스템 호출이 추가되는 경우
- 응답시간 특성이 다른 데이터가 포함되는 경우
- 권한이나 감사 수준이 다른 데이터가 포함되는 경우
- 일부 데이터의 독립 장애 처리가 필요한 경우
22. 보안·개인정보·감사

22.1 인증·권한

| 검증 | 위치 |
| --- | --- |
| JWT 서명·만료 | Gateway 또는 JWT Filter |
| 인증 문맥 존재 | STF |
| 사용자·Header 정합성 | STF |
| 기능권한 | STF Authorization |
| 지점 데이터권한 | Service·Rule |
| 개인정보 원문권한 | Service·마스킹 처리 |

22.2 개인정보 처리

| 데이터 | 기본 처리 |
| --- | --- |
| 고객번호 | 업무 필요 범위 표시 |
| 고객명 | 부분 마스킹 |
| 휴대전화 | 중간자리 마스킹 |
| 이메일 | 계정 일부 마스킹 |
| 주소 | 상세주소 제한 |
| 잔액 | 조회권한 검증 |
| 주민등록번호 | 거래 응답 제외 |

22.3 감사로그

| 항목 | 기록값 |
| --- | --- |
| GUID·TraceId | 거래 추적값 |
| ServiceId | SV.Customer.selectSummary |
| 화면·이벤트 | SV-CUS-0001, E01 |
| 사용자·지점 | 인증 문맥 |
| 대상 고객 | 고객 식별자 또는 암호화값 |
| 실행시각 | 요청·응답 시각 |
| 결과 | 성공·실패·Timeout |
| 마스킹 여부 | 적용 여부 |
| 조회 건수 | 고객·상품 건수 |
| 오류코드 | 실패 시 코드 |

감사로그에 고객명·전화번호·응답 전문 원문은 저장하지 않는다.

23. 운영·모니터링·장애 대응

23.1 거래로그

시작 로그

```
status          = PROCESSING
guid            = G202607150000001
traceId         = T202607150000001
serviceId       = SV.Customer.selectSummary
transactionCode = SV-INQ-0001
businessCode    = SV
userId          = user01
branchId        = 001234
startDtm        = ...
```

종료 로그

```
status          = SUCCESS
endDtm          = ...
elapsedMillis   = 542
resultCode      = S0000
```

23.2 필수 모니터링 지표

| 지표 | 설명 |
| --- | --- |
| 호출 건수 | ServiceId별 요청량 |
| 성공률 | 정상 완료 비율 |
| 업무 오류율 | Business Fail 비율 |
| 시스템 오류율 | System Error 비율 |
| Timeout 건수 | 제한시간 초과 |
| 평균·p95 | 응답시간 |
| DB 대기시간 | Connection 획득시간 |
| SQL 실행시간 | Mapper SQL별 시간 |
| Active 거래 | 현재 실행 중인 거래 |
| Current Step | 현재 DB·SQL·외부 대기 단계 |

23.3 장애 확인 순서

```
ServiceId 오류율·응답시간 확인
  ↓
현재 실행 거래와 Current Step 확인
  ↓
DB Pool Pending 확인
  ↓
Slow SQL 확인
  ↓
JVM CPU·GC·Thread 확인
  ↓
동일 GUID·TraceId 로그 연결
```

23.4 초기 장애 대응

| 증상 | 우선 확인 |
| --- | --- |
| Timeout 증가 | Slow SQL·DB Pool·외부 대기 |
| 업무 오류 증가 | 입력값·기준정보·업무 Rule |
| 시스템 오류 증가 | DB·배포·Mapper·설정 |
| 특정 지점만 실패 | 데이터 권한·지점코드 |
| 고객명 조회만 지연 | 인덱스·실행계획 |
| 전체 응답 지연 | Tomcat Thread·JVM·DB Pool |
| PROCESSING 잔존 | ETF·finally·서버 강제종료 |

24. 책임 경계와 RACI

| 활동 | 업무팀 | 프레임워크팀 | OM팀 | UI팀 | DA·DBA | 보안팀 | 테스트팀 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 거래 목적 정의 | A/R | C | I | C | C | I | C |
| ServiceId 정의 | R | C | C | C | I | I | C |
| 거래코드 정의 | R | I | A | I | I | C | C |
| 표준 전문 | R | A | I | C | I | C | C |
| Handler 이하 구현 | A/R | C | I | I | C | I | C |
| STF·ETF 구현 | I | A/R | C | I | I | C | C |
| OM 정책 등록 | C | C | A/R | I | I | C | C |
| SQL 구현·튜닝 | R | I | I | I | A/R | I | C |
| 개인정보 기준 | C | C | I | C | C | A/R | C |
| 테스트 | C | C | C | C | C | C | A/R |
| 운영 대응 | C | C | A/R | I | R | C | I |

25. 정상 예시

```
화면 조회 버튼 클릭
→ SV.Customer.selectSummary 요청
→ STF 검증 성공
→ SvCustomerHandler 실행
→ SvCustomerFacade 읽기전용 트랜잭션 시작
→ SvCustomerInquiryRule 검증
→ 고객요약·등급·상품 SQL 실행
→ 개인정보 마스킹
→ 응답 DTO 조립
→ ETF 정상 응답
→ 거래로그 SUCCESS
→ 감사로그 기록
→ 고객요약 화면 표시
```

26. 금지 예시

26.1 별도 Controller 생성

```
@PostMapping("/customer/summary")
public CustomerResponse selectSummary(...) {
    return customerService.selectSummary(...);
}
```

TCF·STF 통제를 우회할 수 있으므로 금지한다.

26.2 Handler에서 DAO 호출

```
public Object handle(...) {
    return customerDao.selectSummary(...);
}
```

Facade·Service 책임을 우회하므로 금지한다.

26.3 Rule에서 DB 조회

```
public void validate(...) {
    customerMapper.selectCustomer(...);
}
```

Rule은 순수 검증을 원칙으로 하므로 금지한다.

26.4 사용자 ID 신뢰

```
String userId = request.getHeader().getUserId();
```

클라이언트가 전달한 값을 그대로 신뢰하지 않는다.

26.5 개인정보 로그 출력

```
log.info("customer={}", response);
```

응답 DTO 전체와 개인정보 출력은 금지한다.

26.6 Timeout 무시

```
@Transactional(readOnly = true)
public Response selectSummary(...) {
    // 거래별 timeout 없음
}
```

OM·TCF Timeout과 DB Query Timeout을 함께 적용한다.

27. 자동검증 및 품질 Gate

| 검증 ID | 검증 내용 | 실패 처리 |
| --- | --- | --- |
| TRX-001 | ServiceId 명명규칙 | 빌드 실패 |
| TRX-002 | ServiceId 중복 등록 | 기동 실패 |
| TRX-003 | Handler 미등록 | 배포 차단 |
| TRX-004 | OM Catalog 미등록 | 배포 차단 |
| TRX-005 | 거래통제 정책 누락 | 배포 차단 |
| TRX-006 | Timeout 정책 누락 | 배포 차단 |
| TRX-007 | 권한코드 미등록 | 배포 차단 |
| TRX-008 | Handler → DAO 직접 호출 | 정적검증 실패 |
| TRX-009 | Rule의 Mapper 의존 | 정적검증 실패 |
| TRX-010 | Facade 외 @Transactional | 검토 또는 실패 |
| TRX-011 | 요청·응답 DTO 미정의 | 설계 Gate 실패 |
| TRX-012 | SQL ID 미연결 | 설계 Gate 실패 |
| TRX-013 | 개인정보 마스킹 누락 | 보안 Gate 실패 |
| TRX-014 | 테스트 미연결 | 배포 차단 |
| TRX-015 | 폐기 ServiceId 참조 | 빌드·배포 차단 |

배포 전에는 코드 Handler ServiceId, OM Catalog, 거래통제와 Timeout 등록 집합을 비교해야 한다. 코드에만 있거나 OM 정책이 누락된 ServiceId는 배포를 차단한다.

28. 테스트 시나리오

28.1 기능 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| TC-SV-INQ-0001-001 | 정상 고객번호 조회 | 고객요약 정상 반환 |
| TC-SV-INQ-0001-002 | 고객번호 누락 | SV-CUS-001 |
| TC-SV-INQ-0001-003 | 고객 미존재 | SV-CUS-002 |
| TC-SV-INQ-0001-004 | 미래 기준일 | SV-CUS-005 |
| TC-SV-INQ-0001-005 | 상품 없음 | 고객정보 정상·빈 목록 |
| TC-SV-INQ-0001-006 | 개인정보 일반권한 | 마스킹 반환 |
| TC-SV-INQ-0001-007 | 개인정보 원문권한 | 허용 범위 원문 반환 |

28.2 인증·권한 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| TC-SV-INQ-0001-101 | Token 없음 | 인증 오류 |
| TC-SV-INQ-0001-102 | Token 만료 | TCF-AUTH-001 |
| TC-SV-INQ-0001-103 | 사용자 Header 불일치 | 거래 차단 |
| TC-SV-INQ-0001-104 | 기능권한 없음 | SV-CUS-003 |
| TC-SV-INQ-0001-105 | 타 지점 데이터권한 없음 | SV-CUS-004 |

28.3 TCF 통제 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| TC-SV-INQ-0001-201 | ServiceId 미등록 | Handler 미실행 |
| TC-SV-INQ-0001-202 | 거래 운영 차단 | TCF-CTL-001 |
| TC-SV-INQ-0001-203 | Timeout 정책 누락 | 배포 또는 실행 차단 |
| TC-SV-INQ-0001-204 | 중복 ServiceId | 기동 실패 |
| TC-SV-INQ-0001-205 | Header 누락 | STF 실패 |

28.4 장애 테스트

| 테스트 ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| TC-SV-INQ-0001-301 | SQL 오류 | Rollback·시스템 오류 |
| TC-SV-INQ-0001-302 | DB Connection 획득 실패 | DB 오류·Pool 반환 확인 |
| TC-SV-INQ-0001-303 | SQL 3초 초과 | Timeout |
| TC-SV-INQ-0001-304 | ETF 처리 예외 | 거래로그 종료 보장 |
| TC-SV-INQ-0001-305 | 서버 강제 종료 | PROCESSING 잔존 탐지 |
| TC-SV-INQ-0001-306 | 고객등급 SQL 실패 | 전체 실패 정책 확인 |

28.5 성능 테스트

| 테스트 ID | 시나리오 | 검증 |
| --- | --- | --- |
| TC-SV-INQ-0001-401 | 정상 부하 | p95 3초 이내 |
| TC-SV-INQ-0001-402 | 설계 피크 | 오류율·Pool·Thread |
| TC-SV-INQ-0001-403 | 고객명 부분검색 | 실행계획·인덱스 |
| TC-SV-INQ-0001-404 | 상품 100건 고객 | 응답 크기·시간 |
| TC-SV-INQ-0001-405 | DB 지연 | Timeout·자원 반환 |

29. 거래설계 체크리스트

| No. | 점검 항목 | 확인 |
| --- | --- | --- |
| 1 | 거래 목적이 하나의 업무 유스케이스로 명확한가 | □ |
| 2 | 화면 이벤트와 ServiceId가 연결되었는가 | □ |
| 3 | ServiceId와 거래코드가 구분되었는가 | □ |
| 4 | 업무코드·Context Path·WAR가 일치하는가 | □ |
| 5 | 표준 Header 필수항목이 정의되었는가 | □ |
| 6 | 요청 Body 필드와 검증조건이 정의되었는가 | □ |
| 7 | 정상·결과 없음·업무 오류가 구분되었는가 | □ |
| 8 | Handler와 Facade 책임이 분리되었는가 | □ |
| 9 | 트랜잭션이 Facade에 선언되었는가 | □ |
| 10 | Rule이 DB·외부 시스템을 호출하지 않는가 | □ |
| 11 | DAO Method와 SQL ID가 연결되었는가 | □ |
| 12 | DB 객체와 CRUD가 정의되었는가 | □ |
| 13 | 거래통제 정책이 등록되었는가 | □ |
| 14 | Timeout 정책이 등록되었는가 | □ |
| 15 | DB Query Timeout도 적용되었는가 | □ |
| 16 | 인증·권한·지점 데이터권한이 정의되었는가 | □ |
| 17 | 개인정보 최소조회와 마스킹 기준이 있는가 | □ |
| 18 | 거래로그와 감사로그 항목이 정의되었는가 | □ |
| 19 | 오류코드와 사용자 메시지가 정의되었는가 | □ |
| 20 | 정상·오류·Timeout 테스트가 존재하는가 | □ |
| 21 | 성능 목표와 최대 건수가 정의되었는가 | □ |
| 22 | OM Catalog와 코드 정합성을 검증하는가 | □ |
| 23 | 변경 시 화면·프로그램·SQL 영향 추적이 가능한가 | □ |
| 24 | 폐기 절차와 호환기간이 정의되었는가 | □ |

30. 변경·호환성·폐기 관리

30.1 요청 필드 추가

하위 호환 가능한 선택 필드는 기존 ServiceId에 추가할 수 있다.

```
선택 필드 추가
→ 기본값 정의
→ 구버전 클라이언트 영향 없음 확인
→ DTO·테스트·문서 갱신
```

필수 필드 추가는 기존 호출 화면에 영향을 주므로 신규 ServiceId 또는 호환기간을 검토한다.

30.2 응답 필드 변경

| 변경 | 판단 |
| --- | --- |
| 선택 필드 추가 | 일반적으로 호환 가능 |
| 필드 삭제 | 호환성 위반 |
| 필드명 변경 | 호환성 위반 |
| 데이터 유형 변경 | 호환성 위반 |
| 코드값 의미 변경 | 영향도 분석 필요 |
| 마스킹 강화 | UI 표시 영향 검토 |

30.3 SQL 변경

SQL 변경 시 다음 관계를 확인한다.

```
SQL ID
→ DAO Method
→ ServiceId
→ 화면 이벤트
→ 응답 필드
→ 테스트
```

30.4 ServiceId 변경

ServiceId는 단순 문자열이 아니라 코드·OM·화면·로그·테스트의 연결키다.

변경 시 다음 항목을 함께 변경한다.

- 화면 호출 정의
- Handler Registry
- OM Service Catalog
- 거래통제
- Timeout 정책
- 권한·감사 정책
- 거래로그 조회조건
- 테스트 케이스
- 추적성 매트릭스
- 운영 Runbook
30.5 폐기 절차

```
신규 대체 ServiceId 제공
  ↓
호출 화면·시스템 전환
  ↓
구 ServiceId 사용량 모니터링
  ↓
운영 중지 예정 공지
  ↓
OM에서 DEPRECATED 전환
  ↓
호출 0건 확인
  ↓
Handler·정책·테스트 제거
  ↓
최종 폐기
```

31. 시사점

31.1 핵심 아키텍처 판단

거래설계의 기준 단위는 Controller나 화면이 아니라 ServiceId다.

```
화면은 거래를 요청하고,
ServiceId는 거래를 식별하며,
Handler는 거래를 진입시키고,
Facade는 트랜잭션을 경계 짓고,
Service는 업무를 처리하며,
Rule은 업무 조건을 검증하고,
DAO·Mapper는 데이터를 처리하며,
ETF는 결과를 표준화한다.
```

31.2 주요 위험

- 코드에만 존재하고 OM에는 등록되지 않은 ServiceId
- 동일 ServiceId를 여러 Handler가 등록하는 구조
- Handler가 DAO를 직접 호출하는 구조
- Rule이 DB 조회를 수행하는 구조
- Timeout은 있으나 SQL 취소가 되지 않는 구조
- 인증 Header의 사용자 정보를 그대로 신뢰하는 구조
- 개인정보를 응답·로그에 과다 포함하는 구조
- SQL 변경이 화면 영향 분석과 연결되지 않는 구조
- 거래로그가 PROCESSING에서 종료되지 않는 구조
31.3 우선 보완 과제

| 우선순위 | 과제 |
| --- | --- |
| 1 | 전체 ServiceId 거래 목록 확정 |
| 2 | 화면 이벤트와 ServiceId 연결 |
| 3 | 코드 Handler와 OM Catalog 정합성 확보 |
| 4 | 거래별 권한·Timeout·감사정책 등록 |
| 5 | Handler 이하 6계층 프로그램 매핑 |
| 6 | DAO·Mapper·SQL·DB 객체 추적 |
| 7 | 정상·오류·Timeout 테스트 자동화 |
| 8 | 배포 전 품질 Gate 적용 |

31.4 중장기 발전 방향

거래설계서는 향후 다음 원천을 기반으로 자동 생성·검증해야 한다.

```
화면 이벤트 정의
+ Handler ServiceId 선언
+ Java 메서드 의존관계
+ MyBatis Mapper XML
+ OM Service Catalog
+ 거래통제·Timeout 정책
+ 테스트 결과
= 자동 거래설계 및 추적성 보고서
```

32. 마무리말

SV.Customer.selectSummary 거래는 단순한 고객 조회 API가 아니다.

이 거래는 화면 이벤트에서 시작하여 인증·권한·거래통제·Timeout을 거치고, 업무 프로그램과 SQL을 실행한 뒤 거래로그와 감사로그까지 완결되는 하나의 운영 가능한 실행 단위다.

```
화면 ID로 사용자 기능을 찾고,
이벤트 ID로 호출 시점을 찾고,
ServiceId로 업무 프로그램을 찾고,
거래코드로 통제·감사 기준을 찾고,
GUID·TraceId로 실제 장애 거래를 찾는다.
```

모든 NSIGHT 온라인 거래는 본 문서 형식을 기준으로 한 ServiceId당 하나의 거래설계서를 작성하고, 화면설계서·프로그램설계서·SQL설계서·테스트설계서·OM 기준정보와 연결하여 관리한다.

