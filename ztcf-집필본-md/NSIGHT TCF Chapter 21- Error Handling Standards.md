<!-- source: ztcf-집필본/NSIGHT TCF Chapter 21- Error Handling Standards.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제21장. 표준 오류처리

## 이 장을 시작하며

제18장에서는 등록 거래의 중복과 멱등성을 다루었다.

제19장에서는 변경·삭제 거래의 상태 전이와 동시 수정 충돌을 다루었다.

제20장에서는 트랜잭션 Rollback과 Batch의 부분 실패·재시작 기준을 살펴보았다.

이번 장에서는 이러한 모든 거래에서 발생하는 실패를 어떻게 분류하고, 예외·오류코드·메시지·응답·로그로 일관되게 표현할 것인지 다룬다.

초보 개발자는 오류처리를 다음과 같이 생각하기 쉽다.

try {
업무를 처리한다.
} catch (Exception e) {
"오류가 발생했습니다."를 반환한다.
}

이 방식은 사용자에게 실패 사실은 알려 줄 수 있다.

그러나 운영 시스템에는 충분하지 않다.

다음 상황은 모두 “오류”이지만 의미와 대응이 완전히 다르다.

고객번호를 입력하지 않았다.

조회 대상 고객이 존재하지 않는다.

다른 사용자가 먼저 수정했다.

사용자의 권한이 없다.

현재 거래가 운영자에 의해 중지됐다.

동일 등록 요청이 이미 처리됐다.

DB Connection을 얻지 못했다.

SQL 문법이 잘못됐다.

외부 시스템이 업무적으로 요청을 거절했다.

외부 시스템에 연결할 수 없다.

거래 제한시간이 초과됐다.

애플리케이션 코드에서 NullPointerException이 발생했다.

사용자의 다음 행동도 다르다.

| 실패 상황 | 사용자의 다음 행동 |
| --- | --- |
| 필수값 누락 | 값을 입력한다. |
| 업무 데이터 없음 | 조회조건을 변경한다. |
| 동시 수정 | 최신 데이터를 다시 조회한다. |
| 권한 없음 | 권한 담당자에게 문의한다. |
| 중복 요청 | 기존 처리결과를 확인한다. |
| Timeout | 무조건 재실행하지 않고 처리상태를 확인한다. |
| 시스템 장애 | GUID를 이용해 운영자에게 문의한다. |

운영자의 대응 역시 다르다.

입력 오류 증가
→ 특정 화면 배포 문제를 점검한다.

업무 오류 증가
→ 데이터·업무조건·사용자 교육을 점검한다.

동시 수정 오류 증가
→ 화면 체류시간과 Version 전달을 확인한다.

Timeout 증가
→ SQL·DB Pool·외부 연계를 확인한다.

시스템 오류 증가
→ 애플리케이션·DB·인프라 장애를 확인한다.

따라서 모든 예외를 하나의 오류코드로 바꾸면 안 된다.

모든 오류
→ E-COM-SYS-0001

이렇게 처리하면 사용자는 무엇을 수정해야 할지 알 수 없고, 운영자는 어떤 장애가 발생했는지 구분할 수 없다.

반대로 Java Exception 이름과 DB 메시지를 그대로 사용자에게 반환해서도 안 된다.

{
"errorCode": "java.sql.SQLSyntaxErrorException",
"message": "ORA-00942: table or view does not exist",
"sql": "SELECT \* FROM CM\_CAMPAIGN",
"stackTrace": "..."
}

이 응답은 다음 문제를 발생시킨다.

DB 구조 노출

서버 내부 클래스 노출

보안 취약점 정보 노출

사용자 혼란

운영 메시지와 사용자 메시지 혼재

표준 오류처리의 핵심은 다음 두 가지를 동시에 만족하는 것이다.

사용자에게는
→ 안전하고 이해할 수 있는 메시지

운영자에게는
→ 원인을 추적할 수 있는 충분한 증거

NSIGHT TCF는 업무 처리 결과를 다음 세 경로로 구분한다.

정상
→ ETF.success()

예상 가능한 업무 실패
→ ETF.businessFail()

예상하지 못한 시스템 실패
→ ETF.systemError()

TCF는 BusinessException을 업무 실패로 처리하고 그 밖의 예외는 Timeout 여부를 확인한 뒤 시스템 오류로 처리한다. ETF는 각 경로에서 멱등성 상태, 거래로그, 감사로그와 Metric을 종료하고 StandardResponse를 생성한다.

## 핵심 관점

좋은 오류처리는
예외를 없애는 것이 아니다.

실패를 정확히 분류하고,
트랜잭션을 안전하게 종료하며,
사용자에게 다음 행동을 안내하고,
운영자가 원인을 다시 찾을 수 있도록
증거를 남기는 것이다.

# 문서 개요

## 목적

본 장의 목적은 NSIGHT TCF 거래에서 발생하는 오류를 일관되게 분류하고 다음 항목의 표준을 정의하는 것이다.

예외 발생 위치

예외 계층

오류코드

표준 응답

사용자 메시지

운영 메시지

원인 예외 보존

로그 Level

Metric·알림

오류코드 등록·변경·폐기

## 적용범위

Gateway·인증 Filter

OnlineTransactionController

TCF·STF·ETF

Timeout Executor

Dispatcher·Handler

Facade·Service·Rule

DAO·MyBatis Mapper

tcf-eai 외부 연계

Cache·파일·Batch

OM 오류코드 관리

화면·내부 Client의 오류 응답 처리

## 대상 독자

초보·중급 업무 개발자

프레임워크 개발자

애플리케이션 아키텍트

QA·테스트 담당자

운영·장애 대응 담당자

보안·감사 담당자

## 선행조건

TCF 전체 거래 흐름 이해

StandardRequest·StandardResponse 이해

Facade Transaction 이해

GUID·TraceId 이해

ServiceId·거래코드 이해

OM 기준정보 관리 이해

## 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 입력 오류·업무 오류·시스템 오류를 구분한다. |
| 2 | 인증·권한·거래통제 오류의 발생 위치를 설명한다. |
| 3 | 동시성 오류와 중복 요청 오류를 구분한다. |
| 4 | 외부 업무 거절과 외부 통신 장애를 구분한다. |
| 5 | Timeout을 일반 업무 오류와 다르게 관리해야 하는 이유를 설명한다. |
| 6 | 오류 유형별 사용자 행동과 운영 대응을 정의한다. |
| 7 | 현재 BusinessException과 SystemException의 역할을 설명한다. |
| 8 | 예외를 어느 계층에서 생성하고 변환할지 판단한다. |
| 9 | Mapper·DAO가 사용자 메시지를 결정하면 안 되는 이유를 설명한다. |
| 10 | 오류코드의 영역·유형·일련번호를 정의한다. |
| 11 | resultCode와 errorCode의 차이를 설명한다. |
| 12 | HTTP 상태와 TCF 업무 결과를 구분한다. |
| 13 | 오류코드를 OM Catalog에 등록한다. |
| 14 | 사용자 메시지와 운영 메시지를 분리한다. |
| 15 | 오류코드 중복과 의미 변경을 자동 검증한다. |
| 16 | 원인 예외를 Exception Chain에 보존한다. |
| 17 | 예외를 이중으로 Logging하지 않는다. |
| 18 | 시스템 예외의 Stack Trace를 최종 경계에서 기록한다. |
| 19 | 오류 응답에 SQL·클래스명·Stack Trace를 노출하지 않는다. |
| 20 | GUID로 오류 응답과 운영로그를 연결한다. |
| 21 | 예상 오류와 장애 오류의 Log Level을 구분한다. |
| 22 | 오류코드별 Metric과 알림 기준을 정의한다. |
| 23 | 입력·업무·시스템·Timeout 테스트를 작성한다. |
| 24 | 오류코드 변경과 폐기의 호환성 영향을 판단한다. |

# 한눈에 보는 오류 처리 흐름

┌────────────────────────────────────────────────────────────┐
│ 1. 오류 발생 │
│ 입력·인증·업무·DB·연계·Timeout │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. 발생 계층에서 의미 있는 예외 생성·변환 │
│ 원인 예외 보존 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. Facade Transaction 종료 │
│ Commit 또는 Rollback │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. TCF 오류 분류 │
│ Business·Timeout·System │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. ETF 표준 후처리 │
│ 멱등성·거래로그·감사·Metric │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. StandardResponse │
│ 안전한 코드·사용자 메시지·GUID │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 7. 운영 분석 │
│ GUID → ServiceId → 오류코드 → Cause │
└────────────────────────────────────────────────────────────┘

# 핵심 용어

| 용어 | 의미 |
| --- | --- |
| Exception | Java 실행 중 발생한 예외 객체 |
| Error Code | 오류 원인을 안정적으로 식별하는 표준 코드 |
| Error Type | 입력·업무·시스템·Timeout 등 오류 분류 |
| User Message | 사용자에게 보여 줄 안전한 안내 |
| Operator Message | 운영자가 장애를 분류할 수 있는 설명 |
| Action Guide | 사용·운영자가 수행할 후속 조치 |
| Root Cause | 예외 Chain의 가장 근본적인 원인 |
| Correlation ID | 요청·로그·연계를 연결하는 GUID·TraceId |
| Retryable | 동일 요청을 재시도할 수 있는지 여부 |
| Alertable | 운영 경보가 필요한 오류인지 여부 |
| Error Catalog | 오류코드와 메시지·정책을 관리하는 기준정보 |
| Error Detail | 기술 상세정보로, 사용자 응답 노출을 제한해야 하는 값 |
| Failure Domain | 장애가 발생한 시스템·계층·자원 영역 |

# 현재 구현과 목표 구조

## 현재 구현에서 확인되는 구성

현재 기준 소스에는 다음 구조가 존재한다.

BusinessException
├─ RuntimeException
├─ errorCode
├─ message
└─ cause 선택 가능

SystemException
├─ RuntimeException
├─ errorCode
├─ message
└─ cause 필수

TCF.process()
├─ BusinessException
│ → ETF.businessFail()
├─ 일반 Exception
│ ├─ Timeout 변환
│ │ → ETF.businessFail()
│ └─ 그 밖의 예외
│ → ETF.systemError()
└─ Context·MDC clear

ETF
├─ success()
├─ businessFail()
└─ systemError()

현재 StandardResponse는 다음 구조를 가진다.

StandardResponse
├─ header
├─ result
│ ├─ resultCode
│ ├─ resultMessage
│ ├─ errorCode
│ ├─ errorMessage
│ ├─ errorDetail
│ ├─ errorSystemId
│ └─ errorDateTime
└─ body

OM 오류코드 관리에는 다음 속성이 구현돼 있다.

errorCode

errorCategory

userMessage

operatorMessage

actionGuide

notifyTarget

useYn

이는 사용자용 메시지와 운영자용 설명을 분리할 수 있는 좋은 기반이다.

## 현재 구현 상태 판단

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| BusinessException | 구현 확인 | 업무 오류 기본 예외 |
| SystemException | 구현 확인 | 시스템 오류 명시 가능 |
| 예외 Cause 생성자 | 구현 확인 | 원인 보존 가능 |
| TCF 업무·시스템 분기 | 구현 확인 | 기본 구조 양호 |
| ETF 후처리 분리 | 구현 확인 | 로그·Metric 일원화 가능 |
| StandardResponse 오류 필드 | 구현 확인 | 표준 응답 기반 존재 |
| OM 오류코드 관리 | 구현 확인 | 메시지·조치 관리 가능 |
| Timeout 별도 운영 분류 | 부분 구현 | 현재 Business 경로지만 별도 유형 필요 |
| Body Validation 오류 | 부분 구현 | Header 오류와 분리 필요 |
| 오류코드 형식 | 혼재 | 3자리·4자리 일련번호 혼합 |
| 오류코드 상수 단일 원천 | 보완 필요 | Core·Util 복제 상수 존재 |
| 시스템 오류 Detail | 보완 필요 | 예외 클래스명 노출 금지 |
| Global Handler Detail | 보완 필요 | 원예외 메시지 응답 금지 |
| HTTP 상태 정책 | 보완 필요 | 전 구간 일관성 확정 |
| 전체 전문 System.out | 운영 금지 | 개인정보·성능 위험 |
| 구조화 오류 로그 | 보완 필요 | GUID·단계·코드 표준화 |
| Retryable·Notify 정책 | 권장 확장 | Catalog 속성 확장 검토 |

TCF 오류 기준은 Header·인증·권한·거래통제·중복요청·업무 규칙·DB·외부 연계·Timeout·Handler 미등록을 서로 다른 유형으로 분류하도록 정의한다.

# 목표 오류처리 구조

오류 발생
↓
해당 계층에서 의미 분류
↓
Standard Exception
├─ errorCode
├─ errorType
├─ messageArguments
├─ retryable
├─ logLevel
├─ notifyPolicy
└─ cause
↓
Transaction Rollback
↓
TCF Error Classifier
↓
Error Catalog 조회
├─ userMessage
├─ operatorMessage
├─ actionGuide
└─ notifyTarget
↓
ETF
├─ 거래로그 종료
├─ 감사
├─ Metric
├─ Alert
└─ StandardResponse

# 21.1 입력·업무·시스템 오류 구분

## 21.1.1 오류를 구분해야 하는 이유

오류 유형은 사용자의 다음 행동과 운영자의 대응을 결정한다.

| 오류 유형 | 사용자 행동 | 운영 행동 |
| --- | --- | --- |
| 입력 | 값 수정 | 화면·계약 오류 추세 확인 |
| 인증 | 재로그인·Token 갱신 | 인증 서버·키·시간 확인 |
| 권한 | 권한 요청 | 권한설정·조직정보 확인 |
| 거래통제 | 안내 확인 | OM 통제정책 확인 |
| 업무 | 조건 수정 | 업무 데이터·정책 확인 |
| 동시성 | 재조회 후 수정 | Version 전달·장기 화면 확인 |
| 중복요청 | 기존 결과 확인 | 멱등성 상태 확인 |
| 연계 업무거절 | 조건 수정·상태 확인 | 상대 응답·매핑 확인 |
| 연계 장애 | 잠시 후 재시도 | 네트워크·상대 시스템 확인 |
| Timeout | 상태조회 후 판단 | SQL·Pool·외부 호출 확인 |
| 시스템 | GUID 문의 | Stack·DB·인프라 확인 |

## 21.1.2 권장 오류 유형

VALIDATION

HEADER

AUTHENTICATION

AUTHORIZATION

CONTROL

IDEMPOTENCY

BUSINESS

NOT\_FOUND

CONCURRENCY

INTEGRATION\_BUSINESS

INTEGRATION\_SYSTEM

TIMEOUT

DATA\_ACCESS

CONFIGURATION

CAPACITY

SYSTEM

오류 유형은 오류코드보다 상위의 운영 분류다.

오류 유형
BUSINESS

세부 오류코드
E-SV-BIZ-0001
E-SV-BIZ-0002
E-CM-BIZ-0001

## 21.1.3 입력 오류

입력 오류는 요청 형식이나 값의 모양이 계약과 맞지 않는 경우다.

예:

필수 Header 누락

필수 Body 필드 누락

날짜 형식 오류

허용 길이 초과

숫자 범위 초과

Enum에 없는 값

JSON 문법 오류

잘못된 Content-Type

허용되지 않은 HTTP Method

입력 오류는 업무 Handler 실행 전에 가능한 한 빠르게 차단한다.

요청
↓
JSON Parsing
↓
Header Validation
↓
DTO Conversion
↓
Bean Validation
↓
실패
↓
DAO·DB 미실행

## 21.1.4 Header 오류와 Body 오류를 구분한다

Header 오류:

serviceId 누락

businessCode 누락

transactionCode 누락

channelId 누락

Body 오류:

customerNo 누락

pageSize 범위 초과

startDate 형식 오류

campaignName 길이 초과

다음과 같이 같은 코드로 처리하지 않는다.

모든 Validation
→ INVALID\_HEADER

권장:

E-TCF-HDR-0001
→ Header 필수항목 누락

E-SV-VAL-0001
→ 고객번호 필수

E-CM-VAL-0002
→ 캠페인 종료일 형식 오류

## 21.1.5 JSON Parsing 오류

요청:

{
"pageSize": "one hundred"
}

DTO:

Integer pageSize;

JSON을 DTO로 변환할 수 없으므로 Handler·Service까지 도달하지 않는다.

응답은 안전하게 표현한다.

{
"result": {
"resultCode": "E0001",
"errorCode": "E-COM-VAL-0002",
"errorMessage": "요청 데이터 형식을 확인해 주세요."
}
}

금지:

Cannot deserialize value of type java.lang.Integer
from String "one hundred"
at line 14 column 28

## 21.1.6 업무 오류

업무 오류는 시스템이 정상 작동했지만 업무 규칙상 요청을 수용할 수 없는 경우다.

예:

조회 대상 고객 없음

이미 등록된 캠페인

현재 상태에서 승인 불가

조회기간 정책 초과

잔여 한도 부족

삭제할 수 없는 상태

다른 사용자가 먼저 수정

기존 처리결과 존재

특징:

예상 가능한 조건

재현 가능한 테스트

업무 담당자가 의미를 설명 가능

사용자가 조건을 수정하거나 상태를 확인 가능

## 21.1.7 업무 오류와 시스템 오류의 경계

상황:

고객 조회 결과가 0건

업무 계약이 “고객이 반드시 존재해야 한다”면 업무 오류다.

CUSTOMER\_NOT\_FOUND

상황:

고객 단건 조회 결과가 2건

업무 유일성이 깨진 데이터 정합성 문제다.

일반적으로 시스템 오류로 처리한다.

DATA\_INTEGRITY\_ERROR

단순히 조회결과가 예상과 다르다고 모두 업무 오류로 바꾸지 않는다.

## 21.1.8 시스템 오류

시스템 오류는 사용자가 입력을 수정해서 해결할 수 없는 기술적 실패다.

예:

NullPointerException

SQL 문법 오류

DB Connection 실패

Mapper Statement 미등록

설정파일 누락

Bean 생성 실패

파일 시스템 오류

메모리 부족

Thread Pool 고갈

예상하지 못한 프로그램 오류

시스템 오류 처리:

Transaction Rollback

서버에 Cause·Stack Trace 기록

사용자에게 공통 안전 메시지

GUID 제공

시스템 오류 Metric 증가

경보정책에 따라 운영 알림

## 21.1.9 인증 오류

인증 오류는 요청자의 신원을 신뢰할 수 없는 경우다.

예:

Token 없음

Token 만료

서명 오류

issuer 불일치

audience 불일치

세션 만료

JWT Claim과 Header 사용자 불일치

발생 위치:

Gateway

Security Filter

STF

인증 오류를 일반 업무 오류로 표시하면 화면이 재로그인 처리를 하지 못할 수 있다.

## 21.1.10 권한 오류

인증된 사용자라도 모든 기능과 데이터에 접근할 수 있는 것은 아니다.

기능권한 없음

타 지점 데이터 접근

승인권한 없음

본인 작성건 본인 승인 금지

삭제 데이터 관리자 조회권한 없음

권한 오류는 다음을 구분한다.

기능권한
→ ServiceId 실행 여부

데이터권한
→ 특정 대상 접근 여부

상태별 권한
→ 현재 상태에서 행위 허용 여부

## 21.1.11 거래통제 오류

운영자가 거래를 중지하거나 특정 채널·시간대의 실행을 차단한 경우다.

ServiceId 사용중지

특정 채널 차단

점검시간 차단

지점별 거래 제한

긴급 거래통제

이 경우 Handler와 DB 거래를 실행하지 않는다.

사용자 메시지:

현재 해당 업무를 이용할 수 없습니다.
잠시 후 다시 시도해 주세요.

운영 메시지:

ServiceId CM.Campaign.create가
OM 정책 TXCTRL-20260718-01에 의해 차단됨

## 21.1.12 중복 요청 오류

같은 Idempotency Key

기존 상태 PROCESSING

또는 SUCCESS

다음 상태를 구분한다.

| 상태 | 처리 |
| --- | --- |
| PROCESSING | 처리 중 안내 |
| SUCCESS | 기존 결과 반환 |
| FAIL | 재시도 정책 확인 |
| TIMEOUT | 상태조회 안내 |
| UNKNOWN | 자동 재실행 금지 |

중복 요청은 시스템 장애가 아니다.

그러나 같은 Key로 다른 Body가 들어온 경우 보안·계약 오류로 별도 기록해야 한다.

## 21.1.13 동시성 오류

expectedVersion = 3

현재 Version = 4

다른 사용자가 먼저 수정한 경우다.

사용자 메시지:

다른 사용자가 먼저 변경했습니다.
최신 내용을 다시 조회한 후 처리해 주세요.

운영에서는 시스템 장애로 경보할 필요는 없지만 급증 여부를 확인한다.

특정 화면에서 Version 누락

장시간 열린 화면

Batch와 온라인 변경 충돌

## 21.1.14 외부 연계 오류

외부 연계 오류도 두 종류로 구분한다.

### 외부 업무 거절

상대 고객 없음

상대 업무상태 불가

상대 한도 부족

상대 중복 거래

외부 계약이 안정적이고 사용자 행동으로 해결 가능하면 INTEGRATION\_BUSINESS로 매핑할 수 있다.

### 외부 기술 장애

Connection Refused

Read Timeout

HTTP 500

응답 JSON Parsing 실패

인증서 오류

DNS 오류

INTEGRATION\_SYSTEM으로 처리한다.

상대 시스템의 모든 오류를 BusinessException으로 바꾸지 않는다.

## 21.1.15 Timeout 오류

Timeout은 일반 업무 거절과 운영 의미가 다르다.

사용자에게는 실패 응답

하지만
DB 또는 외부 시스템은 성공했을 가능성

특히 등록·변경 거래는 다음 상태가 가능하다.

사용자 응답
TIMEOUT

DB 상태
COMMIT 완료

Idempotency 상태
UNKNOWN

따라서 Timeout에는 다음 정보가 필요하다.

timeoutLayer

configuredTimeoutMs

elapsedMs

lastCompletedStep

idempotencyStatus

retryAllowed

statusCheckGuide

현재 TCF는 Timeout을 BusinessException 형태로 변환해 업무 실패 경로로 종료할 수 있지만, 운영 Metric과 멱등성 상태에서는 일반 업무 오류와 구분하는 것이 필요하다. Timeout 응답 실패가 실제 업무 처리 실패를 의미하지 않을 수 있기 때문이다.

## 21.1.16 오류 분류표

| 유형 | 대표 발생 계층 | ETF 경로 | 사용자 재시도 | 운영 경보 |
| --- | --- | --- | --- | --- |
| 입력 | Controller·Handler | Business 계열 | 값 수정 후 | 추세 기반 |
| Header | STF | Business 계열 | 요청 수정 | 추세 기반 |
| 인증 | Gateway·STF | 인증 정책 | 재로그인 | 급증 시 |
| 권한 | STF·Rule | Business 계열 | 권한 확인 | 추세 기반 |
| 거래통제 | STF | Business 계열 | 안내 후 | 정책 변경 시 |
| 중복요청 | STF | Business 계열 | 상태 확인 | 비정상 급증 시 |
| 업무 | Rule·Service | businessFail | 조건 수정 | 일반적으로 없음 |
| 동시성 | Service | businessFail | 재조회 후 | 급증 시 |
| 외부 업무 | EAI·Service | businessFail | 조건 확인 | 추세 기반 |
| 외부 기술 | EAI | systemError | 정책에 따라 | O |
| Timeout | Timeout Executor | 별도 Timeout 권장 | 상태 확인 | O |
| DB | DAO·Mapper | systemError | 직접 재시도 금지 | O |
| 프로그램 | 전 계층 | systemError | 직접 재시도 금지 | O |

## 21.1.17 HTTP 상태와 TCF 결과

현재 TCF 거래는 HTTP 통신에 성공했더라도 resultCode가 실패일 수 있다.

HTTP 200
≠ 업무 성공

구간별 정책을 구분한다.

| 발생 구간 | HTTP 예 | TCF 응답 |
| --- | --- | --- |
| 정상 업무 | 200 | 성공 |
| 업무 오류 | 200 정책 가능 | 실패 Result |
| 입력 오류 | 200 또는 400 | 프로젝트 표준 |
| 인증 실패 | 401 | Gateway·인증 오류 |
| 권한 실패 | 403 또는 표준 실패 | 경계별 정책 |
| Route 없음 | 404 | TCF 이전 |
| 대상 서비스 불가 | 503 | Gateway 오류 |
| TCF 시스템 오류 | 200 또는 500 | 프로젝트 표준 |

핵심은 Service마다 다르게 처리하지 않는 것이다.

SV는 시스템 오류 HTTP 200

CM은 HTTP 500

EP는 HTTP 400

같은 구조는 Client 처리를 복잡하게 한다.

# 21.2 예외 계층과 오류코드

## 21.2.1 현재 예외 구조

현재 기준 소스:

public class BusinessException
extends RuntimeException {

private final String errorCode;

public BusinessException(
String errorCode,
String message) {

super(message);
this.errorCode = errorCode;
}

public BusinessException(
String errorCode,
String message,
Throwable cause) {

super(message, cause);
this.errorCode = errorCode;
}
}

public class SystemException
extends RuntimeException {

private final String errorCode;

public SystemException(
String errorCode,
String message,
Throwable cause) {

super(message, cause);
this.errorCode = errorCode;
}
}

장점:

RuntimeException
→ 변경 트랜잭션 Rollback에 유리

errorCode
→ 표준 오류 응답 가능

cause
→ 원인 예외 보존 가능

## 21.2.2 목표 예외 속성

장기적으로 예외는 다음 정보를 가질 수 있다.

public record ErrorDescriptor(
String errorCode,
ErrorType errorType,
boolean retryable,
LogLevel logLevel,
NotifyPolicy notifyPolicy
) {}

public abstract class StandardException
extends RuntimeException {

private final ErrorDescriptor descriptor;
private final Object\[\] messageArguments;

protected StandardException(
ErrorDescriptor descriptor,
Object\[\] messageArguments,
Throwable cause) {

super(descriptor.errorCode(), cause);
this.descriptor = descriptor;
this.messageArguments =
messageArguments == null
? new Object\[0\]
: messageArguments.clone();
}
}

업무 개발자는 Java 메시지를 직접 조립하기보다 오류코드와 메시지 Argument를 제공한다.

## 21.2.3 권장 예외 계층

RuntimeException
↓
StandardException
├─ ValidationException
├─ AuthenticationException
├─ AuthorizationException
├─ ControlException
├─ BusinessException
├─ NotFoundException
├─ ConcurrencyException
├─ IdempotencyException
├─ IntegrationBusinessException
├─ IntegrationSystemException
├─ TransactionTimeoutException
└─ SystemException

예외 클래스를 오류코드마다 하나씩 만들 필요는 없다.

CustomerNotFoundException

CustomerGradeNotFoundException

CampaignNotFoundException

EventNotFoundException

업무 의미가 분명하고 반복 사용된다면 전용 예외가 유용할 수 있다.

그러나 수백 개 예외 클래스로 오류코드 Catalog를 대신하지 않는다.

## 21.2.4 계층별 예외 책임

| 계층 | 발생·변환할 오류 |
| --- | --- |
| Gateway | 인증·Route·Proxy 오류 |
| Controller·Advice | JSON·Method·Content-Type 오류 |
| STF | Header·인증·권한·통제·중복 |
| Dispatcher | ServiceId 미등록 |
| Handler | DTO 변환·거래 분기 |
| Rule | 업무 상태·정책·권한 |
| Service | 결과 없음·동시성·업무 흐름 |
| DAO | 기술적 데이터 접근 예외 변환 |
| Mapper | MyBatis·JDBC 예외를 자연 발생 |
| EAI Adapter | 상대 코드·통신 오류 분류 |
| Timeout Executor | Timeout 계층과 상태 |
| TCF | 최종 오류 유형 분류 |
| ETF | 표준 응답·로그·Metric 종료 |

## 21.2.5 Mapper가 업무 메시지를 결정하지 않는다

금지:

catch (SQLException exception) {
throw new BusinessException(
"E-SV-BIZ-0001",
"고객이 존재하지 않습니다."
);
}

SQL 오류는 고객 미존재가 아니다.

권장:

Mapper
→ DataAccessException

DAO
→ CustomerPersistenceException

TCF
→ systemError

고객 미존재는 조회 결과가 정상적으로 0건일 때 Service가 판단한다.

## 21.2.6 DAO 예외 변환

@Repository
@RequiredArgsConstructor
public class CampaignDao {

private final CampaignMapper mapper;

public int insertCampaign(
CampaignCreateCommand command) {

try {
return mapper.insertCampaign(command);

} catch (DuplicateKeyException exception) {
throw new CampaignPersistenceException(
"E-CM-DAT-0001",
"캠페인 등록 중 데이터 중복이 발생했습니다.",
exception
);

} catch (DataAccessException exception) {
throw new CampaignPersistenceException(
"E-CM-DAT-0002",
"캠페인 데이터 처리에 실패했습니다.",
exception
);
}
}
}

Service는 필요한 경우 특정 Unique Constraint를 업무 중복으로 변환한다.

## 21.2.7 오류코드의 역할

오류코드는 다음을 위한 안정적인 식별자다.

화면 분기

사용자 메시지

운영 검색

Metric

알림

테스트

장애 통계

변경 영향분석

Java Exception 클래스명은 오류코드를 대신할 수 없다.

DataIntegrityViolationException

은 여러 원인을 포함할 수 있기 때문이다.

## 21.2.8 오류코드와 Result Code

현재 표준 응답에서 다음을 구분한다.

resultCode
→ 거래 성공·실패의 상위 결과
→ S0000·E0001

errorCode
→ 구체적인 실패 원인
→ E-SV-BIZ-0001

예:

{
"result": {
"resultCode": "E0001",
"resultMessage": "처리 중 오류가 발생했습니다.",
"errorCode": "E-SV-BIZ-0001",
"errorMessage": "조회 대상 고객이 없습니다."
}
}

## 21.2.9 목표 오류코드 형식

권장 형식:

E-{오류소유영역}-{오류유형}-{4자리 일련번호}

예:

E-TCF-HDR-0001

E-COM-VAL-0001

E-SV-BIZ-0001

E-CM-CON-0001

E-EP-EAI-0001

E-TCF-TMO-0001

E-COM-SYS-0001

## 21.2.10 오류 소유영역

| 영역 | 의미 |
| --- | --- |
| TCF | 거래 프레임워크 |
| COM | 공통 |
| JWT | 토큰 |
| GTW | Gateway |
| EAI | 연계 공통 |
| OM | 운영관리 |
| SV | Single View |
| IC | 고객 통합 |
| CM | Campaign |
| EP | Event Processing |
| BAT | Batch |

오류코드는 오류가 관측된 화면이 아니라 오류를 소유하고 해결할 책임이 있는 영역을 기준으로 한다.

## 21.2.11 오류 유형 코드

| 코드 | 의미 |
| --- | --- |
| HDR | 표준 Header |
| VAL | 입력 Validation |
| AUT | 인증·인가 |
| CTL | 거래통제 |
| IDM | 멱등성·중복 |
| BIZ | 업무 규칙 |
| NFD | 필수 대상 없음 |
| CON | 동시성 |
| DAT | 데이터 접근 |
| EAI | 외부 연계 |
| TMO | Timeout |
| CFG | 설정 |
| CAP | 용량·자원 |
| SYS | 기타 시스템 |

## 21.2.12 현재 코드 형식 혼재

현재 소스에는 다음과 같은 형식이 함께 존재한다.

E-COM-VALID-0001

E-COM-DISP-0001

E-TCF-HDR-001

E-TCF-TIME-001

E-COM-SYS-0001

문제:

오류 유형 길이 불일치

일련번호 3자리·4자리 혼재

TIME·TMO 등 약어 혼재

VALID·VAL 등 약어 혼재

기존 운영코드를 즉시 변경하지 않고 다음 방식으로 전환한다.

기존 코드
→ 유지·Deprecated 표시

신규 코드
→ 통일된 형식 사용

필요 시
→ Legacy Code Mapping 제공

## 21.2.13 오류코드 단일 원천

현재 Core 오류코드가 다른 모듈에 복제된 상수로 존재하면 다음 위험이 있다.

Core 수정

Util 복제본 미수정

모듈별 서로 다른 코드 사용

목표:

OM Error Catalog
또는
하나의 공통 Error Descriptor 원천
↓
빌드 시 Java 상수 생성
↓
문서·테스트·Seed 생성

수작업 복사본을 줄인다.

## 21.2.14 오류코드 Catalog 속성

| 속성 | 설명 |
| --- | --- |
| errorCode | 표준 코드 |
| ownerBusiness | 소유 업무 |
| errorType | 오류 유형 |
| userMessage | 사용자 메시지 |
| operatorMessage | 운영 메시지 |
| actionGuide | 조치 안내 |
| retryableYn | 재시도 가능 여부 |
| notifyTarget | 알림 대상 |
| logLevel | INFO·WARN·ERROR |
| httpStatus | 적용 정책 |
| resultCode | 상위 결과 |
| useYn | 사용 여부 |
| effectiveFrom | 적용일 |
| deprecatedAt | 폐기 예정일 |
| replacementCode | 대체 코드 |
| messageVersion | 메시지 버전 |
| maskingPolicy | Argument 마스킹 |

## 21.2.15 오류코드 등록 절차

1\. 실패 조건 정의
2\. 오류 소유영역 확정
3\. 오류 유형 선택
4\. 기존 코드 중복·유사 의미 검색
5\. 신규 코드 채번
6\. 사용자 메시지 작성
7\. 운영 메시지 작성
8\. Action Guide 작성
9\. Retry·Alert 정책 확정
10\. OM Catalog 등록
11\. Java Descriptor 생성
12\. 단위·Contract Test 작성
13\. 배포 전 정합성 검사
14\. 운영 Metric·Dashboard 반영

오류코드는 코드에 먼저 하드코딩한 뒤 나중에 OM에 맞추는 방식이 아니라 설계·등록·구현을 함께 진행한다.

## 21.2.16 중복 오류코드

다음 두 업무가 같은 코드를 다른 의미로 사용하면 안 된다.

E-SV-BIZ-0001
→ 고객 미존재

E-SV-BIZ-0001
→ 고객 조회권한 없음

오류코드 중복은 빌드·기동 단계에서 차단하는 것이 기준이다.

## 21.2.17 Global Exception Handler의 범위

TCF 내부에서 처리된 예외는 TCF·ETF가 응답한다.

Global Advice는 다음과 같은 TCF 이전 오류를 주로 처리한다.

JSON Parsing 오류

지원하지 않는 HTTP Method

Content-Type 오류

Controller Parameter Binding 오류

TCF 진입 전 Filter 밖의 예외

업무 예외를 Global Advice와 ETF가 서로 다르게 처리하지 않도록 해야 한다.

TCF 경로
→ ETF

TCF 이전 Web 경계
→ Global Advice

두 경로의 StandardResponse 계약과 메시지 정책은 동일해야 한다.

# 21.3 원인 예외 보존

## 21.3.1 원인 예외를 보존해야 하는 이유

다음 코드는 사용자에게는 간단하지만 운영 원인을 잃는다.

catch (Exception exception) {
throw new RuntimeException(
"처리 중 오류"
);
}

운영 로그에는 다음 정보만 남을 수 있다.

RuntimeException: 처리 중 오류

실제 원인이 다음 중 무엇인지 알 수 없다.

DB Connection 실패

Unique Constraint

SQL 문법 오류

외부 Timeout

JSON Parsing

파일 권한

NullPointerException

## 21.3.2 올바른 Cause 보존

catch (DataAccessException exception) {
throw new CampaignPersistenceException(
"E-CM-DAT-0002",
"캠페인 데이터 처리 중 오류가 발생했습니다.",
exception
);
}

Exception Chain:

CampaignPersistenceException
↓ cause
DataAccessResourceFailureException
↓ cause
SQLTransientConnectionException
↓ cause
Oracle Network Exception

사용자에게는 최상위 표준 오류만 반환하고 운영자는 Cause Chain을 확인한다.

## 21.3.3 잘못된 예외 변환

금지:

throw new BusinessException(
"E-CM-BIZ-0001",
exception.getMessage()
);

문제:

원인 예외 손실

SQL·서버 메시지 노출

기술 오류를 업무 오류로 오분류

## 21.3.4 메시지와 Cause를 분리한다

Exception Message
→ 개발·운영 내부 설명

User Message
→ Error Catalog의 안전한 메시지

Operator Message
→ 운영 원인 분류 설명

exception.getMessage()를 사용자 메시지로 사용하지 않는다.

## 21.3.5 최종 경계에서 Stack Trace 기록

권장 원칙:

하위 계층
→ Cause를 보존해 예외 전파

최종 처리 경계
→ Stack Trace 한 번 기록

예:

log.error(
"TCF system error. "
\+ "serviceId={}, guid={}, errorCode={}",
serviceId,
guid,
errorCode,
exception
);

## 21.3.6 이중 Logging 방지

금지:

Mapper에서 ERROR Stack Trace

DAO에서 같은 Stack Trace

Service에서 같은 Stack Trace

Facade에서 같은 Stack Trace

TCF에서 같은 Stack Trace

하나의 오류가 로그에 다섯 번 기록되면 실제 장애 건수를 오판할 수 있다.

권장:

하위 계층
→ 필요한 업무 Context 추가 후 전파

TCF·Web 최종 경계
→ Stack Trace 기록

하위 계층에서 로그가 필요한 경우 Stack Trace 없이 처리단계 정보를 남길 수 있다.

## 21.3.7 업무 오류 Stack Trace

예상 가능한 업무 오류에 매번 Stack Trace를 남기면 로그가 과도해진다.

예:

고객 미존재

현재 상태에서 수정 불가

조회기간 초과

동시 수정

권장 Log Level:

| 오류 | Level | Stack Trace |
| --- | --- | --- |
| 입력 오류 | INFO·DEBUG | 일반적으로 없음 |
| 업무 오류 | INFO·WARN | 일반적으로 없음 |
| 권한 거절 | WARN | 필요 시 제한 |
| 동시 수정 | INFO·WARN | 없음 |
| 중복 요청 | INFO | 없음 |
| Timeout | WARN·ERROR | Cause에 따라 |
| DB·프로그램 오류 | ERROR | O |
| 보안 공격 의심 | WARN·ERROR | 정책 |

## 21.3.8 원인 예외와 Rollback

예외를 원인과 함께 RuntimeException으로 전파하면 Facade Transaction은 Rollback된다.

위험:

try {
mapper.update(...);

} catch (DataAccessException exception) {
log.error("실패", exception);
return false;
}

예외를 정상 반환으로 바꾸면 Transaction이 Commit될 수 있다.

원인 보존은 진단뿐 아니라 트랜잭션 안전성과도 연결된다.

## 21.3.9 예외 변환 위치

### Mapper

MyBatis·JDBC 예외 자연 발생

### DAO

DB 기술영역 정보를 데이터 접근 예외로 변환

### Service

기술 결과를 업무 의미로 해석할 수 있을 때만 변환

예:

Duplicate Key
\+ 제약조건 UX\_CM\_CAMPAIGN\_CODE
→ 캠페인 업무 중복

Duplicate Key
\+ PK\_CM\_CAMPAIGN
→ 시스템 ID 충돌

모든 Duplicate Key를 같은 업무 오류로 처리하지 않는다.

## 21.3.10 외부 연계 Cause

catch (SocketTimeoutException exception) {
throw new IntegrationSystemException(
"E-EP-EAI-0002",
"외부 시스템 응답시간이 초과되었습니다.",
exception
);
}

로그 Context:

targetSystem

interfaceId

elapsedMs

timeoutMs

HTTP status

retryCount

기록 금지:

Authorization Header

전체 요청 전문

전체 개인정보 응답

Client Secret

## 21.3.11 Timeout Cause Chain

Timeout은 다음과 같은 Cause를 가질 수 있다.

TransactionTimeoutException

QueryTimeoutException

SQLTimeoutException

SocketTimeoutException

ConnectTimeoutException

TimeoutException

CancellationException

단순히 메시지에 “timeout” 문자열이 있는지로 분류하지 않는다.

승인된 Exception Type과 실행 계층을 기준으로 분류한다.

## 21.3.12 비동기 Thread의 예외

별도 Executor에서 발생한 예외는 다음 형태로 감싸질 수 있다.

ExecutionException

CompletionException

Future Timeout

실제 Cause를 해제해 분류해야 한다.

Throwable cause =
ExceptionUnwrapper.unwrap(exception);

동시에 다음 Context가 유지돼야 한다.

GUID

TraceId

ServiceId

Authentication Context

Timeout Context

MDC

## 21.3.13 Suppressed Exception

Resource Close 과정에서도 예외가 발생할 수 있다.

업무 처리 예외
+
Connection Close 예외

Java는 보조 예외를 suppressed로 보존할 수 있다.

Root Cause만 보고 Suppressed Exception을 모두 버리지 않는다.

## 21.3.14 시스템 오류 응답 Detail

현재 일부 구현에서 시스템 오류 응답의 errorDetail에 Java Exception의 단순 클래스명을 넣는 방식이 존재할 수 있다.

예:

NullPointerException

SQLGrammarException

운영환경에서는 다음 이유로 외부 노출을 금지하는 것이 안전하다.

내부 구현정보 노출

공격자에게 기술정보 제공

Client가 Java 클래스명에 의존

리팩터링 시 계약 변경

권장:

errorDetail
→ 운영환경 null

supportReference
→ 응답 Header의 GUID 사용

개발 Profile에서만 제한적으로 기술 Detail을 허용할 수 있으나 개인정보·SQL·경로는 제외한다.

## 21.3.15 요청·응답 전문 Logging

현재 교육·진단용 소스에는 System.out으로 Header와 Body 전체를 출력하는 코드가 포함될 수 있다.

운영에서는 금지한다.

Request Body 전체

Response Body 전체

JWT

고객정보

파일내용

운영 Logging은 다음 식별자를 중심으로 한다.

guid

traceId

serviceId

transactionCode

errorType

errorCode

failureStage

elapsedMs

appVersion

instanceId

# 21.4 사용자 메시지와 운영 메시지

## 21.4.1 메시지를 분리해야 하는 이유

같은 오류라도 사용자와 운영자가 필요한 정보가 다르다.

예: DB Connection 실패

### 사용자

처리 중 오류가 발생했습니다.
잠시 후 다시 시도해 주세요.
계속 발생하면 문의번호 G-20260718-000401을 알려 주세요.

### 운영자

sv-service에서 DB Connection 획득 실패.
Hikari Pool active=120, idle=0, waiting=84.
serviceId=SV.Customer.selectSummary
guid=G-20260718-000401

## 21.4.2 메시지 계층

| 메시지 | 대상 | 내용 |
| --- | --- | --- |
| User Message | 최종 사용자 | 이해 가능한 안내 |
| Field Message | 입력 화면 | 어느 필드를 수정할지 |
| Operator Message | 운영자 | 원인영역·점검대상 |
| Action Guide | 사용자·운영자 | 다음 조치 |
| Developer Detail | 개발자 | Stack·Cause·Context |
| Audit Description | 감사자 | 누가 무엇을 시도했는지 |

## 21.4.3 좋은 사용자 메시지의 조건

간결하다.

사용자가 이해할 수 있다.

사용자가 할 수 있는 행동을 알려 준다.

내부 기술용어를 노출하지 않는다.

실제 오류 의미와 일치한다.

불필요한 개인정보를 포함하지 않는다.

과도한 책임 전가 표현을 사용하지 않는다.

## 21.4.4 나쁜 사용자 메시지

NullPointerException이 발생했습니다.

ORA-00001이 발생했습니다.

FeignException$ServiceUnavailable입니다.

Mapper XML 오류입니다.

알 수 없는 오류 999입니다.

관리자에게 문의하세요.

마지막 문장처럼 조치가 너무 모호한 것도 좋지 않다.

## 21.4.5 행동 중심 사용자 메시지

입력 오류:

고객번호를 입력해 주세요.

업무 오류:

조회 대상 고객이 없습니다.
검색조건을 확인해 주세요.

동시성 오류:

다른 사용자가 먼저 변경했습니다.
최신 내용을 다시 조회해 주세요.

거래통제:

현재 해당 업무를 이용할 수 없습니다.
잠시 후 다시 시도해 주세요.

Timeout:

처리 확인이 지연되고 있습니다.
같은 요청을 다시 실행하기 전에 처리상태를 확인해 주세요.

시스템 오류:

처리 중 오류가 발생했습니다.
문의 시 화면에 표시된 거래번호를 알려 주세요.

## 21.4.6 운영 메시지

운영 메시지는 사용자 메시지보다 구체적이어야 한다.

ServiceId CM.Campaign.approve에서
expectedVersion=4 조건 UPDATE 결과가 0건임.
현재 Version 재조회 결과 5.
동시 수정으로 분류.

단, 운영 메시지도 비밀번호·Token·주민번호를 포함하면 안 된다.

## 21.4.7 Action Guide

예:

| 오류코드 | Action Guide |
| --- | --- |
| E-SV-VAL-0001 | 고객번호 입력 여부 확인 |
| E-CM-CON-0001 | 최신 데이터 재조회 후 재처리 |
| E-TCF-CTL-0001 | OM 거래통제 상태와 적용기간 확인 |
| E-TCF-TMO-0001 | Slow SQL·Thread·DB Pool·외부 호출 확인 |
| E-COM-SYS-0001 | GUID로 Stack Trace와 배포버전 확인 |
| E-EP-EAI-0002 | 상대 시스템 상태·Timeout·Retry 확인 |

## 21.4.8 Notify Target

오류마다 모든 운영자에게 알림을 보내지 않는다.

| 오류 | 알림 |
| --- | --- |
| 필수값 누락 1건 | 없음 |
| 업무 데이터 없음 | 없음 |
| 권한 거절 | 임계치·보안정책 |
| 동시 수정 | 급증 시 |
| Timeout | 성능 운영팀 |
| DB Connection 실패 | AP·DB 운영 |
| 외부 연계 장애 | 연계·상대 시스템 |
| 개인정보 접근 공격 | 보안관제 |
| 시스템 오류 급증 | 개발·운영 On-call |

## 21.4.9 메시지 Parameter

고객번호 {0}을 찾을 수 없습니다.

개인정보가 포함될 수 있으므로 메시지 Argument를 그대로 노출하지 않는다.

권장:

요청한 고객을 찾을 수 없습니다.

운영로그에는 마스킹값을 기록한다.

customerNo=C0\*\*\*\*01

## 21.4.10 다국어·채널별 메시지

장기적으로 다음 구조를 검토할 수 있다.

errorCode

locale

channelId

userMessage

actionGuide

그러나 같은 오류코드의 업무 의미가 채널별로 달라져서는 안 된다.

메시지 표현만 달라질 수 있다.

## 21.4.11 사용자 응답의 거래번호

오류 응답에는 별도의 내부 Ticket 번호를 새로 만드는 것보다 이미 존재하는 GUID를 이용한다.

{
"header": {
"serviceId": "SV.Customer.selectSummary",
"guid": "G-20260718-000401"
},
"result": {
"resultCode": "E0001",
"errorCode": "E-COM-SYS-0001",
"errorMessage": "처리 중 오류가 발생했습니다. 문의 시 거래번호를 알려 주세요.",
"errorDetail": null
},
"body": null
}

## 21.4.12 입력 오류 응답

{
"header": {
"serviceId": "SV.Customer.selectList",
"guid": "G-20260718-000402"
},
"result": {
"resultCode": "E0001",
"resultMessage": "처리 중 오류가 발생했습니다.",
"errorCode": "E-SV-VAL-0003",
"errorMessage": "페이지 크기는 1건 이상 500건 이하로 입력해 주세요.",
"errorDetail": null,
"errorSystemId": "NSIGHT-MP",
"errorDateTime": "2026-07-18T15:10:00+09:00"
},
"body": null
}

## 21.4.13 업무 오류 응답

{
"header": {
"serviceId": "CM.Campaign.approve",
"guid": "G-20260718-000403"
},
"result": {
"resultCode": "E0001",
"errorCode": "E-CM-BIZ-0004",
"errorMessage": "현재 상태에서는 캠페인을 승인할 수 없습니다.",
"errorDetail": null
},
"body": null
}

## 21.4.14 동시성 오류 응답

{
"header": {
"serviceId": "CM.Campaign.update",
"guid": "G-20260718-000404"
},
"result": {
"resultCode": "E0001",
"errorCode": "E-CM-CON-0001",
"errorMessage": "다른 사용자가 먼저 변경했습니다. 최신 내용을 다시 조회해 주세요.",
"errorDetail": null
},
"body": null
}

## 21.4.15 Timeout 응답

{
"header": {
"serviceId": "CM.Campaign.create",
"guid": "G-20260718-000405"
},
"result": {
"resultCode": "E0001",
"errorCode": "E-TCF-TMO-0001",
"errorMessage": "처리 확인이 지연되고 있습니다. 같은 요청을 다시 실행하기 전에 처리상태를 확인해 주세요.",
"errorDetail": null
},
"body": {
"statusCheckRequired": true
}
}

기존 응답 계약상 실패 Body를 허용하지 않는다면 상태조회 ServiceId를 Action Guide로 제공한다.

## 21.4.16 시스템 오류 응답

{
"header": {
"serviceId": "SV.Customer.selectSummary",
"guid": "G-20260718-000406"
},
"result": {
"resultCode": "E0001",
"resultMessage": "처리 중 오류가 발생했습니다.",
"errorCode": "E-COM-SYS-0001",
"errorMessage": "처리 중 오류가 발생했습니다. 문의 시 거래번호를 알려 주세요.",
"errorDetail": null,
"errorSystemId": "NSIGHT-MP",
"errorDateTime": "2026-07-18T15:15:00+09:00"
},
"body": null
}

# 정상 처리 흐름

업무 Handler 정상 반환
↓
Facade Transaction Commit
↓
TCF
↓
ETF.success()
├─ Idempotency SUCCESS
├─ 거래로그 SUCCESS
├─ 감사로그
├─ Metric
└─ StandardResponse.success()

# 입력 오류 흐름

잘못된 Request
↓
JSON·Header·DTO Validation
↓
ValidationException
↓
Handler·DB 미실행
↓
입력 오류코드
↓
Field·User Message
↓
INFO Metric

# 업무 오류 흐름

Service·Rule
↓
BusinessException
↓
Facade Transaction Rollback
↓
TCF catch
↓
ETF.businessFail()
├─ Idempotency FAIL
├─ 거래로그 BUSINESS\_FAIL
├─ 업무 Metric
└─ 안전한 사용자 메시지

# 시스템 오류 흐름

Mapper·DB·프로그램
↓
Exception
↓
Cause 보존
↓
Facade Transaction Rollback
↓
TCF catch
↓
ETF.systemError()
├─ ERROR Stack Trace 1회
├─ Idempotency FAIL·UNKNOWN
├─ 거래로그 SYSTEM\_FAIL
├─ 시스템 Metric
├─ Alert
└─ 공통 시스템 오류 응답

# Timeout 흐름

Timeout Executor
↓
제한시간 초과
↓
작업 취소 요청
↓
DB·외부 처리상태 확인 필요
↓
Transaction Rollback 시도
↓
Idempotency TIMEOUT·UNKNOWN
↓
거래로그 TIMEOUT
↓
Timeout Metric·Alert
↓
상태조회 안내

# TCF 이전 오류 흐름

지원하지 않는 HTTP Method
또는 JSON Parsing 실패
↓
Global Exception Handler
↓
표준 오류코드 변환
↓
기술 Detail 제거
↓
StandardResponse

TCF 이전 응답에서도 가능하면 GUID를 생성해 추적성을 유지한다.

# 정상 예시

## 업무 오류 발생

if (customer == null) {
throw new BusinessException(
"E-SV-NFD-0001",
"조회 대상 고객이 없습니다."
);
}

## 원인 예외 보존

catch (DataAccessException exception) {
throw new SystemException(
"E-SV-DAT-0001",
"고객 데이터 조회 중 오류가 발생했습니다.",
exception
);
}

## 최종 Logging

log.error(
"event=TCF\_SYSTEM\_ERROR "
\+ "guid={} serviceId={} "
\+ "errorCode={} failureStage={}",
guid,
serviceId,
errorCode,
failureStage,
exception
);

# 금지 예시

모든 오류를 E-COM-SYS-0001로 반환한다.

모든 Exception을 BusinessException으로 바꾼다.

SQLException 메시지를 사용자에게 반환한다.

Exception 클래스명을 errorDetail에 노출한다.

Stack Trace를 응답에 넣는다.

요청·응답 Body 전체를 System.out으로 출력한다.

BusinessException을 발생시키고 같은 계층에서 다시 catch한다.

예외를 catch하고 false·null을 반환한다.

원인 예외 없이 새 RuntimeException을 발생시킨다.

exception.getMessage()를 사용자 메시지로 사용한다.

Mapper가 업무 오류코드를 결정한다.

DAO가 화면 문구를 결정한다.

업무 오류마다 ERROR Stack Trace를 기록한다.

한 오류를 DAO·Service·Facade·TCF에서 중복 Logging한다.

오류코드를 여러 모듈에 복사해 관리한다.

같은 오류코드를 다른 의미로 사용한다.

사용 중인 오류코드의 의미를 변경한다.

Validation 오류를 모두 Header 오류로 처리한다.

Timeout을 일반 업무 거절과 같은 Metric으로 집계한다.

변경 거래 Timeout 후 무조건 재실행을 안내한다.

HTTP 상태 정책을 Service마다 다르게 적용한다.

권한 오류에서 대상 데이터 존재 여부를 과도하게 노출한다.

오류 메시지에 고객번호·계좌번호 원문을 넣는다.

오류코드 미등록 상태로 운영 배포한다.

# 연계 규칙

## 내부 업무 WAR 연계

하위 업무 오류를 상위 오류로 변환할 때 다음을 보존한다.

원 하위 오류코드

상대 ServiceId

TraceId

상대 시스템

재시도 가능 여부

사용자에게 하위 내부 구조를 노출하지 않는다.

예:

하위 오류
E-SV-NFD-0001

상위 업무
IC.Customer.compose

상위 정책
선택 데이터 없음으로 정상 처리
또는
E-IC-BIZ-0004로 변환

정책을 명시적으로 정의한다.

## 외부 시스템 연계

상대 업무코드
→ IntegrationBusinessException

HTTP·Network·Parsing
→ IntegrationSystemException

상대 오류코드와 내부 표준 오류코드의 매핑표를 관리한다.

## Gateway 오류

Gateway에서 응답이 종료되면 업무 WAR의 ETF가 실행되지 않는다.

따라서 Gateway 오류도 다음 식별자를 갖도록 한다.

traceId

gatewayErrorCode

targetBusinessCode

targetRoute

HTTP status

occurredAt

## Batch 오류

Batch 오류는 온라인 사용자 메시지보다 다음 정보가 중요하다.

jobId

jobExecutionId

stepId

chunkNo

itemKey

checkpoint

retryCount

skipCount

reconciliationStatus

오류코드 Catalog는 온라인과 Batch 공통 분류를 사용할 수 있지만 메시지·Action Guide는 다를 수 있다.

# 책임 경계와 RACI

| 활동 | 업무개발 | FW | AA | OM·운영 | 보안 | QA | 업무분석 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 업무 오류조건 정의 | R | I | C | I | C | C | A/R |
| 예외계층 표준 | C | R | A | C | C | C | I |
| 오류코드 형식 | C | R | A | R/C | C | C | I |
| 오류코드 채번 | C | C | A | R | I | I | C |
| 사용자 메시지 | R | I | C | C | C | C | A |
| 운영 메시지 | C | C | C | A/R | C | I | I |
| 원인 예외 보존 | R | A/R | C | I | I | C | I |
| 표준 응답 | C | R | A | C | C | C | I |
| 로그·Metric | C | R | A | A/R | C | C | I |
| 알림정책 | I | C | C | A/R | R/C | C | I |
| 보안 마스킹 | C | C | C | C | A/R | C | I |
| 오류 Contract Test | R | C | C | I | C | A/R | C |
| 폐기·호환성 | C | C | A | R | C | C | C |

# 데이터 및 상태관리

## 오류코드 상태

DRAFT

APPROVED

ACTIVE

DEPRECATED

DISABLED

오류코드를 바로 삭제하지 않는다.

기존 로그·Client·대시보드에서 코드 의미를 조회할 수 있어야 하기 때문이다.

## 오류코드 이력

다음 변경을 기록한다.

사용자 메시지

운영 메시지

Action Guide

알림대상

재시도 정책

사용 여부

대체 코드

이력 속성:

변경자

변경시각

변경사유

승인자

변경 전 값

변경 후 값

## 메시지 Version

오류코드는 유지하면서 문구를 개선할 수 있다.

errorCode
E-CM-CON-0001

messageVersion
3

오류의 업무 의미가 달라지면 메시지만 바꾸지 말고 새 오류코드를 만든다.

# 성능·용량·확장성

## Stack Trace 비용

예상 가능한 입력·업무 오류에 Stack Trace를 매번 기록하면 다음 문제가 발생한다.

로그 I/O 증가

Disk 사용량 증가

로그 수집비용 증가

실제 시스템 오류 탐색 어려움

업무 오류는 코드·Context 중심으로 기록한다.

## 오류 폭주

같은 시스템 오류가 초당 수천 건 발생할 수 있다.

대응:

동일 오류 집계

Log Rate Limit

중복 알림 억제

첫 오류 상세 보존

후속 오류 Count 집계

Circuit Breaker

거래통제

로그를 완전히 제거하면 안 되지만 같은 Stack Trace를 무제한 반복하지 않는다.

## Metric Cardinality

Metric Label에 다음을 넣지 않는다.

GUID

userId

customerNo

전체 메시지

Exception Message

권장 Label:

serviceId

errorType

errorCode

targetSystem

appVersion

instanceGroup

오류코드는 Catalog로 통제되므로 비교적 안정적인 Label로 사용할 수 있다.

## Error Catalog Cache

OM 오류코드를 Runtime에서 조회한다면 Cache를 사용할 수 있다.

확인:

Cache TTL

변경 반영

사용중지 반영

기본 메시지 Fallback

OM 장애 시 동작

업무 WAR별 일관성

오류 메시지를 가져오기 위해 오류 처리 중 다시 원격 OM을 호출하지 않는다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| 사용자 응답 | SQL·Stack·클래스명 금지 |
| 로그 | Token·비밀번호·주민번호 금지 |
| 운영 메시지 | 마스킹 적용 |
| Error Argument | 허용 항목만 치환 |
| 인증 오류 | Token 원문 금지 |
| 권한 오류 | 데이터 존재 여부 노출 검토 |
| 외부 연계 | Credential·전문 원문 금지 |
| 오류 Detail | 운영환경 외부 노출 금지 |
| 감사 | 권한거절·관리자 변경 기록 |
| Catalog 변경 | 관리자 감사로그 필수 |
| 로그 접근 | 역할 기반 권한 |
| 보존·파기 | 오류로그 종류별 기간 |

# 운영·모니터링·장애 대응

## 권장 오류 로그

event=TCF\_TRANSACTION\_FAILED
guid=G-20260718-000406
traceId=T-20260718-000406
businessCode=SV
serviceId=SV.Customer.selectSummary
transactionCode=SV-INQ-0001
errorType=SYSTEM
errorCode=E-SV-DAT-0001
failureStage=MAPPER
retryable=false
elapsedMs=384
appVersion=sv-1.4.0
instanceId=sv-02

## 권장 Metric

tcf.transaction.error.count

tcf.transaction.error.rate

tcf.transaction.timeout.count

tcf.transaction.validation.count

tcf.transaction.business.fail.count

tcf.transaction.system.fail.count

tcf.transaction.auth.fail.count

tcf.transaction.concurrent.conflict.count

tcf.transaction.error.byServiceId

tcf.integration.error.byTarget

## 알림 기준 예

| 오류 | 알림 기준 |
| --- | --- |
| 입력 오류 | 평시 대비 급증 |
| 업무 오류 | 업무별 임계치 |
| 인증 오류 | 급증·공격 패턴 |
| 권한 오류 | 동일 사용자 반복 |
| 동시 수정 | 특정 버전 배포 후 증가 |
| Timeout | 5분 오류율 기준 |
| 시스템 오류 | 즉시 또는 임계치 |
| DB Connection | 즉시 |
| 외부 연계 장애 | 대상별 임계치 |
| UNKNOWN 거래 | 즉시 업무 확인 |

## 장애 점검 순서

1\. 사용자 응답의 GUID 확인
2\. 거래로그에서 결과상태 확인
3\. ServiceId·오류코드 확인
4\. failureStage 확인
5\. 배포버전·인스턴스 확인
6\. 원인 Exception Chain 확인
7\. Transaction Rollback 여부 확인
8\. DB·외부 처리결과 확인
9\. 멱등성 상태 확인
10\. 재시도·복구 가능 여부 판단

## 오류 대응 Runbook

각 중요 시스템 오류코드는 다음 정보를 가져야 한다.

증상

영향 ServiceId

주요 원인

확인 로그

확인 Metric

DB 확인 SQL

임시 조치

재처리 여부

데이터 대사

복구 승인

재발방지

# 자동검증 및 품질 Gate

## 1\. 오류코드 형식 Gate

^E-\[A-Z0-9\]{2,5}-\[A-Z\]{3}-\[0-9\]{4}$

Legacy Code는 승인된 Allow List로 관리한다.

## 2\. 오류코드 중복 Gate

Java Descriptor

OM Catalog

Seed SQL

문서 표준표

Contract Test

전체 집합에서 중복과 누락을 검사한다.

## 3\. 예외 Gate

검출 대상:

throw new RuntimeException(...)

throw new Exception(...)

cause 없는 SystemException

catch (Exception) 후 null 반환

catch 후 false 반환

빈 catch

exception.printStackTrace()

exception.getMessage() 사용자 응답

## 4\. 계층 Gate

Mapper
→ BusinessException 금지

DAO
→ 사용자 메시지 결정 금지

Handler
→ 시스템 예외 숨김 금지

Service
→ StandardResponse 생성 금지

Facade
→ 실패 DTO 정상 반환 금지

## 5\. 응답 보안 Gate

응답에서 다음 패턴을 검사한다.

Exception 클래스명

ORA-

SELECT·UPDATE·DELETE SQL

java.lang.

com.nh.nsight 패키지

파일 경로

Stack Trace

JWT

## 6\. Logging Gate

System.out 금지

Request Body 전체 로그 금지

Response Body 전체 로그 금지

Token 로그 금지

Business 오류 Stack Trace 남용 금지

시스템 오류 Cause 누락 금지

## 7\. 오류 응답 Contract Gate

오류 응답에 다음을 확인한다.

header

guid

resultCode

errorCode

errorMessage

errorDateTime

body 정책

errorDetail 운영 null

## 8\. OM 정합성 Gate

코드에서 사용된 errorCode
\-
OM ACTIVE errorCode
\= 0

반대 방향도 확인한다.

OM ACTIVE errorCode
\-
코드·정책 사용 errorCode

미사용 코드는 폐기 후보로 관리한다.

## 9\. Timeout Gate

Timeout 오류 유형 별도

Idempotency 상태 확인

변경 거래 재실행 안내 금지

DB·외부 처리결과 확인 절차

Connection 반환 테스트

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| ERR-001 | 정상 거래 | S0000 |
| ERR-002 | Header 없음 | Header 오류 |
| ERR-003 | ServiceId 없음 | Header 오류 |
| ERR-004 | JSON 문법 오류 | Parsing 오류 |
| ERR-005 | 숫자 타입 오류 | Body Validation |
| ERR-006 | 필수값 누락 | Field 오류 |
| ERR-007 | 길이 초과 | 입력 오류 |
| ERR-008 | 업무 데이터 없음 | 업무 오류 |
| ERR-009 | 업무 상태 불가 | 업무 오류 |
| ERR-010 | 기능권한 없음 | 권한 오류 |
| ERR-011 | 데이터권한 없음 | 권한 오류 |
| ERR-012 | Token 없음 | 인증 오류 |
| ERR-013 | Token 만료 | 인증 오류 |
| ERR-014 | 거래 사용중지 | 통제 오류 |
| ERR-015 | PROCESSING 중복 | 중복요청 오류 |
| ERR-016 | SUCCESS 재요청 | 기존 결과 |
| ERR-017 | 같은 Key 다른 Body | Key 재사용 오류 |
| ERR-018 | Version 불일치 | 동시성 오류 |
| ERR-019 | 외부 업무 거절 | 연계 업무 오류 |
| ERR-020 | 외부 HTTP 500 | 연계 시스템 오류 |
| ERR-021 | 외부 Connect Timeout | 연계 Timeout |
| ERR-022 | 외부 Read Timeout | 연계 Timeout |
| ERR-023 | DB Connection 실패 | 시스템 오류 |
| ERR-024 | SQL 문법 오류 | 시스템 오류 |
| ERR-025 | Mapper 미등록 | 시스템 오류 |
| ERR-026 | 단건 결과 2건 | 정합성 오류 |
| ERR-027 | NullPointerException | 시스템 오류 |
| ERR-028 | Query Timeout | Timeout 오류 |
| ERR-029 | TCF 전체 Timeout | Timeout 오류 |
| ERR-030 | Timeout 후 DB Commit | UNKNOWN·상태확인 |
| ERR-031 | BusinessException Cause | Cause 보존 |
| ERR-032 | SystemException Cause | Cause 보존 |
| ERR-033 | 예외 변환 | 원인 Chain 유지 |
| ERR-034 | 예외 catch 후 null | Gate 실패 |
| ERR-035 | SystemException Cause 없음 | Gate 실패 |
| ERR-036 | Business 오류 Logging | Stack 없음 |
| ERR-037 | 시스템 오류 Logging | Stack 1회 |
| ERR-038 | 다중 계층 Logging | 중복 검출 |
| ERR-039 | 응답에 ORA 메시지 | 보안 Gate 실패 |
| ERR-040 | 응답에 클래스명 | 보안 Gate 실패 |
| ERR-041 | 응답 errorDetail | 운영 null |
| ERR-042 | 사용자 메시지 | 행동 안내 |
| ERR-043 | 운영 메시지 | 원인영역 포함 |
| ERR-044 | 오류코드 중복 | Build 실패 |
| ERR-045 | OM 미등록 코드 | 배포 실패 |
| ERR-046 | 사용중지 코드 | 신규 사용 차단 |
| ERR-047 | Legacy 형식 코드 | Allow List |
| ERR-048 | HTTP 200 업무 실패 | Client가 Result 확인 |
| ERR-049 | 401 인증 실패 | 재로그인 |
| ERR-050 | GUID 로그 추적 | Cause까지 연결 |
| ERR-051 | Metric Label | 개인정보 없음 |
| ERR-052 | 오류 폭주 | 중복 알림 억제 |
| ERR-053 | Catalog Cache 장애 | 안전 Fallback |
| ERR-054 | 메시지 Argument 개인정보 | 마스킹 |
| ERR-055 | 오류코드 변경 | 호환성 검사 |
| ERR-056 | 폐기 코드 호출 | 대체 코드 안내 |
| ERR-057 | Batch Item 오류 | Job Context 포함 |
| ERR-058 | 권한거절 반복 | 보안 경보 |
| ERR-059 | Transaction Rollback | DB 원상복구 |
| ERR-060 | 다른 개발자 재현 | 같은 코드·로그 확인 |

# 따라 하는 실무 절차

## 1단계. 실패 조건을 작성한다

어떤 조건에서 실패하는가?

사용자가 수정 가능한가?

운영자가 조치해야 하는가?

재시도 가능한가?

데이터가 부분 반영될 수 있는가?

## 2단계. 오류 유형을 결정한다

VALIDATION

BUSINESS

CONCURRENCY

INTEGRATION

TIMEOUT

SYSTEM

## 3단계. 오류 소유영역을 결정한다

TCF

COM

SV

CM

EP

EAI

## 4단계. 기존 오류코드를 검색한다

완료 증적:

OM 조회 결과

Java 상수 검색

문서 표준표

유사 오류 비교

## 5단계. 메시지와 정책을 작성한다

userMessage

operatorMessage

actionGuide

retryable

notifyTarget

logLevel

## 6단계. OM에 등록한다

완료 증적:

errorCode

errorCategory

useYn

변경사유

승인자

## 7단계. 예외를 구현한다

오류코드

메시지 Argument

Cause

발생 계층

## 8단계. 표준 응답을 확인한다

GUID

resultCode

errorCode

errorMessage

errorDetail=null

## 9단계. 로그와 Metric을 확인한다

ServiceId

오류유형

오류코드

failureStage

원인 예외

처리시간

## 10단계. Rollback과 실제 데이터 상태를 확인한다

DB 변경

외부 처리

Idempotency

거래로그

감사로그

## 11단계. 정상·경계·실패 테스트를 수행한다

입력

업무

권한

동시성

연계

Timeout

시스템

## 12단계. 품질 Gate를 통과한다

코드 형식

중복

OM 정합성

Cause

응답 보안

로그 보안

Contract Test

# 완료 체크리스트

## 오류 분류

| 확인 항목 | 완료 |
| --- | --- |
| 입력·업무·시스템 오류를 구분했다. | □ |
| 인증·권한 오류를 구분했다. | □ |
| 거래통제 오류를 구분했다. | □ |
| 중복요청과 업무 중복을 구분했다. | □ |
| 동시성 오류를 구분했다. | □ |
| 외부 업무·기술 오류를 구분했다. | □ |
| Timeout을 별도 분류했다. | □ |
| 사용자 행동과 운영 대응을 정의했다. | □ |

## 예외 계층

| 확인 항목 | 완료 |
| --- | --- |
| 발생 계층이 적절하다. | □ |
| Mapper가 업무 메시지를 결정하지 않는다. | □ |
| DAO가 기술 예외를 보존한다. | □ |
| Service가 업무 의미만 변환한다. | □ |
| Facade가 예외를 숨기지 않는다. | □ |
| Business·System 예외를 구분한다. | □ |
| Runtime Rollback 정책을 확인했다. | □ |
| 예외 클래스 남용이 없다. | □ |

## 오류코드

| 확인 항목 | 완료 |
| --- | --- |
| 소유영역이 명확하다. | □ |
| 오류유형이 명확하다. | □ |
| 형식이 표준과 일치한다. | □ |
| 기존 코드와 중복되지 않는다. | □ |
| 한 코드가 한 의미만 가진다. | □ |
| OM에 등록됐다. | □ |
| Java·OM·문서가 일치한다. | □ |
| Legacy Mapping이 필요하면 정의했다. | □ |
| 폐기·대체 코드가 정의됐다. | □ |

## 메시지

| 확인 항목 | 완료 |
| --- | --- |
| 사용자 메시지가 이해하기 쉽다. | □ |
| 사용자의 다음 행동을 안내한다. | □ |
| 운영 메시지가 별도로 있다. | □ |
| Action Guide가 있다. | □ |
| Retry 가능 여부가 있다. | □ |
| Notify Target이 있다. | □ |
| 개인정보가 마스킹됐다. | □ |
| SQL·클래스명·경로를 노출하지 않는다. | □ |

## 원인·로그

| 확인 항목 | 완료 |
| --- | --- |
| Cause가 보존된다. | □ |
| 시스템 오류 Stack Trace가 있다. | □ |
| Stack Trace는 최종 경계에서 한 번 기록한다. | □ |
| 업무 오류에 Stack Trace를 남용하지 않는다. | □ |
| GUID·ServiceId가 있다. | □ |
| failureStage가 있다. | □ |
| 요청·응답 전체 로그가 없다. | □ |
| System.out을 사용하지 않는다. | □ |
| Token·개인정보 원문이 없다. | □ |

## 응답·운영

| 확인 항목 | 완료 |
| --- | --- |
| StandardResponse 형식이 일치한다. | □ |
| resultCode와 errorCode를 구분한다. | □ |
| 운영 errorDetail은 null이다. | □ |
| GUID가 사용자에게 제공된다. | □ |
| HTTP 상태 정책이 일관된다. | □ |
| 오류 Metric이 있다. | □ |
| 알림 임계치가 있다. | □ |
| 장애 Runbook이 있다. | □ |
| Timeout 상태확인 절차가 있다. | □ |
| Rollback·DB 결과를 검증했다. | □ |

# 변경·호환성·폐기 관리

## 오류코드 의미 변경 금지

기존:

E-SV-BIZ-0001
→ 조회 대상 고객 없음

신규:

E-SV-BIZ-0001
→ 고객 조회권한 없음

같은 코드를 다른 의미로 바꾸면 안 된다.

새 오류코드를 만든다.

## 메시지 변경

맞춤법·표현 개선은 같은 코드를 유지할 수 있다.

기존
고객이 없습니다.

신규
조회 대상 고객이 없습니다.
검색조건을 확인해 주세요.

그러나 사용자의 행동이나 오류 의미가 달라지면 신규 코드가 필요할 수 있다.

## 오류 유형 변경

기존
BUSINESS

신규
SYSTEM

운영 경보·화면 분기·재시도 정책에 영향을 미친다.

변경 영향분석과 회귀 테스트가 필요하다.

## Retry 정책 변경

retryable=false
→ true

다음 영향을 확인한다.

중복 처리

Idempotency

외부 부하

Timeout

사용자 화면

운영 자동재처리

## HTTP 상태 변경

업무 오류를 HTTP 200에서 400으로 바꾸면 기존 Client가 응답 Body를 읽지 못할 수 있다.

소비자 전환계획과 Version 관리가 필요하다.

## 오류코드 폐기

신규 사용 금지
↓
DEPRECATED 표시
↓
대체 코드 제공
↓
코드·Client 호출 조사
↓
운영 발생량 0 확인
↓
DISABLED 전환
↓
과거 이력 조회는 유지

OM Row를 물리 삭제하지 않는 것이 좋다.

# 시사점

## 핵심 아키텍처 판단

첫째, 오류 유형은 Java Exception 이름이 아니라 사용자 행동과 운영 대응을 기준으로 나눠야 한다.

둘째, 예상 가능한 업무 실패와 예상하지 못한 시스템 장애는 같은 로그·Metric·알림 정책을 사용해서는 안 된다.

셋째, 오류코드는 메시지 번호가 아니라 화면·서버·운영·테스트를 연결하는 안정적인 계약이다.

넷째, 예외를 변환하더라도 Cause를 보존해야 Transaction Rollback과 장애 원인 추적이 가능하다.

다섯째, 사용자 메시지와 운영 메시지는 반드시 분리해야 한다.

사용자
→ 무엇을 해야 하는가

운영자
→ 어디를 확인해야 하는가

여섯째, 시스템 오류의 Exception 클래스명과 SQL은 운영 응답에 노출하지 않아야 한다.

일곱째, Timeout은 사용자에게 실패로 보이더라도 실제 데이터 처리결과가 불명확할 수 있으므로 일반 업무 오류와 다르게 관리해야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 모든 오류를 하나로 처리 | 사용자·운영 대응 불가 |
| 모든 예외를 업무 오류화 | 시스템 장애 은폐 |
| Cause 손실 | 원인 분석 불가 |
| 예외 catch 후 정상 반환 | 부분 Commit |
| Mapper의 업무 메시지 | 계층 책임 혼재 |
| 오류코드 중복 | 잘못된 화면·운영 분기 |
| 상수 복제 관리 | 모듈별 코드 불일치 |
| 오류 Detail 노출 | 내부 정보 유출 |
| Body 전체 로그 | 개인정보 유출 |
| 업무 오류 Stack 남용 | 로그 폭주 |
| 시스템 오류 Cause 미기록 | 장애 분석 불가 |
| Timeout 일반 업무 처리 | UNKNOWN 거래 누락 |
| HTTP 정책 혼재 | Client 처리 복잡 |
| 메시지 의미 변경 | 기존 소비자 오동작 |
| 오류코드 즉시 삭제 | 과거 로그 해석 불가 |

## 우선 보완 과제

1.  오류 유형 Enum과 표준 분류기를 정의한다.
2.  Header Validation과 Body Validation 오류를 분리한다.
3.  Timeout을 별도 오류 유형·Metric으로 관리한다.
4.  시스템 오류 응답의 Exception 클래스명 노출을 제거한다.
5.  Global Exception Handler의 원예외 메시지 노출을 제거한다.
6.  TCF의 전체 Request·Response System.out 출력을 구조화 Logging으로 교체한다.
7.  오류코드 형식을 4자리 일련번호로 표준화한다.
8.  복제된 오류코드 상수를 단일 원천에서 생성하도록 개선한다.
9.  OM 오류코드에 retryable·logLevel·httpStatus 속성을 확장한다.
10.  코드 사용 오류코드와 OM ACTIVE Catalog를 CI에서 비교한다.
11.  Cause 없는 시스템 예외를 정적 분석으로 차단한다.
12.  catch(Exception) 후 정상 반환 패턴을 차단한다.
13.  업무 오류와 시스템 오류의 Log Level을 표준화한다.
14.  시스템 오류 응답 Contract Test를 공통화한다.
15.  오류코드별 Runbook과 알림대상을 OM에서 연결한다.

## 중장기 발전 방향

하드코딩 오류코드·메시지
↓
OM Error Catalog
↓
단일 Error Descriptor
↓
Java 상수·문서 자동생성
↓
TCF 공통 Error Classifier
↓
응답·로그·Metric 자동표준화
↓
오류코드 기반 Runbook·알림
↓
운영 발생 데이터 기반 오류정책 개선

# 마무리말

표준 오류처리를 설계하는 과정은 다음 질문에 답하는 일이다.

이 실패는 입력 오류인가, 업무 오류인가, 시스템 오류인가?

사용자가 값을 수정해서 해결할 수 있는가?

운영자가 즉시 대응해야 하는가?

어느 계층에서 오류가 발생했는가?

어느 계층에서 업무 의미로 변환해야 하는가?

원인 예외가 보존됐는가?

트랜잭션은 Rollback됐는가?

오류코드는 어느 업무가 소유하는가?

같은 코드가 다른 의미로 사용되지 않는가?

사용자 메시지는 안전하고 행동 중심인가?

운영 메시지는 원인과 점검대상을 알려 주는가?

응답에 SQL·Stack Trace·클래스명이 노출되지 않는가?

GUID로 응답에서 Root Cause까지 추적할 수 있는가?

Timeout 후 실제 처리상태를 확인할 수 있는가?

오류 발생량과 경보를 운영에서 확인할 수 있는가?

제21장의 핵심 흐름은 다음과 같다.

실패 발생
↓
오류 유형 분류
↓
표준 예외와 오류코드
↓
Cause 보존
↓
Transaction Rollback
↓
ETF 표준 후처리
↓
사용자 메시지
↓
운영 로그·Metric·알림
↓
복구와 재발방지

가장 중요한 원칙은 다음과 같다.

사용자에게 자세히 보여 주는 것이
좋은 오류처리는 아니다.

운영로그에 Exception을 많이 남기는 것도
좋은 오류처리는 아니다.

사용자에게는 안전한 행동 안내를,
운영자에게는 정확한 원인 증거를,
시스템에는 일관된 코드와 상태를 남겨야

실패해도 데이터와 신뢰를 지킬 수 있다.
