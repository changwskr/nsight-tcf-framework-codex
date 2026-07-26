<!-- source: ztcf-집필본/NSIGHT TCF Chapter 부록 D. 오류·로그·보안 검토표.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 확장 부록 D. 오류·로그·보안 검토표

## 도입 전 안내말

오류처리·로그·보안은 개발이 끝난 뒤 공통 모듈에서 한 번에 추가하는 부가기능이 아니다.

하나의 거래를 설계할 때부터 다음 사항을 함께 결정해야 한다.

어떤 입력을 허용할 것인가?

사용자는 누구이며 어떤 권한이 있는가?

어떤 상황을 업무 오류로 판단할 것인가?

어떤 상황을 시스템 장애로 판단할 것인가?

오류가 발생하면 Transaction은 Rollback되는가?

Timeout 이후 실제 처리결과를 확인할 수 있는가?

운영자는 어떤 식별자로 거래를 찾는가?

로그에 남겨도 되는 정보와 남기면 안 되는 정보는 무엇인가?

중요 조회·변경 행위는 감사증적으로 남는가?

외부 시스템의 오류를 어떤 표준 오류로 변환하는가?

NSIGHT TCF의 기본 오류 흐름은 다음과 같다.

요청

→ Gateway·Filter 인증

→ STF 입력·인가·거래통제

→ Handler·Facade·Service·Rule

→ DAO·Mapper·외부 Client

→ 성공
또는
업무 오류
또는
시스템 오류
또는
Timeout

→ ETF 표준 응답

→ 거래로그·감사로그·Metric 종료

오류가 발생했다는 사실만 기록해서는 운영자가 원인을 찾기 어렵다.

반대로 요청·응답 전체를 모두 기록하면 개인정보·인증정보 유출과 로그 용량 폭증이 발생한다.

따라서 오류·로그·보안 설계의 목표는 다음과 같다.

사용자에게는
안전하고 행동 가능한 메시지를 제공한다.

운영자에게는
원인을 추적할 충분한 기술정보를 제공한다.

감사자에게는
누가 무엇을 수행했는지 증명한다.

공격자에게는
내부 구조와 민감정보를 노출하지 않는다.

현재 TCF 기준은 업무 오류와 예상하지 못한 시스템 오류를 분리하고, 시스템 오류의 상세 원인은 서버 내부에 보존하되 외부 응답에는 안전한 오류코드·메시지·GUID만 반환하도록 정의한다.

# 문서 개요

## 목적

본 부록의 목적은 NSIGHT TCF 거래를 대상으로 오류처리·로그·인증·인가·개인정보·감사·Timeout·외부 연계·데이터 변경의 안전성을 검토할 수 있는 표준 질문과 증적 기준을 제공하는 것이다.

세부 목적은 다음과 같다.

입력 오류와 업무 오류 구분

업무 오류와 시스템 장애 구분

오류코드·메시지·HTTP 상태의 일관성 확보

원인 예외 보존과 안전한 외부 응답

GUID·TraceId 기반 거래 추적

애플리케이션·거래·감사·보안로그 역할 분리

Token·Password·개인정보 로그 노출 방지

JWT 서명·만료·발급자·대상 검증

기능권한과 데이터권한 분리

Gateway 우회 및 관리 Endpoint 통제

계층별 Timeout 예산 관리

Retry·멱등성·UNKNOWN 결과 통제

영향 행·중복·Version·부분 반영 검증

외부 연계 계약·오류변환·장애격리

파일·Batch·Cache 특수 흐름 검토

정적분석·테스트·CI/CD 품질 Gate 적용

운영 Alert·Runbook·변경관리 기준 수립

## 적용범위

| 영역 | 적용 대상 |
| --- | --- |
| 온라인 거래 | TCF 표준 /online 거래 |
| Gateway | JWT 검증·라우팅·Gateway 로그 |
| 업무 WAR | Handler 이하 업무처리 |
| 데이터 | DAO·Mapper·Transaction·History |
| 외부 연계 | 업무 WAR 간·외부기관 API |
| 인증 | SSO·JWT·Session |
| 인가 | 기능권한·데이터권한·관리자권한 |
| 오류 | 입력·업무·시스템·Timeout·연계 오류 |
| 로그 | 애플리케이션·거래·감사·보안·연계·SQL |
| 개인정보 | 고객·사용자·상담내용·파일 |
| 운영 | OM·Dashboard·Alert·Runbook |
| 비동기 | Batch·Scheduler·Event·Queue |
| 파일 | 업로드·다운로드·대용량 처리 |
| 배포 | Secret·설정·관리 Endpoint·Artifact |

## 대상 독자

업무 개발자

프레임워크 개발자

코드 리뷰어

애플리케이션 아키텍트

보안 담당자

DBA·데이터 담당자

QA·성능시험 담당자

운영·OM 담당자

DevOps 담당자

PMO·품질 담당자

## 선행조건

표준 오류코드 체계

StandardRequest·StandardResponse

BusinessException·SystemException 기준

GUID·TraceId 생성·전파 기준

MDC 로그 Pattern

JWT 발급·검증 구조

기능권한·데이터권한 관리체계

ServiceId·거래코드·Timeout 등록정보

Transaction·Idempotency 정책

개인정보 분류·마스킹 기준

감사대상 거래 목록

로그 보존·접근통제 정책

외부 연계 계약서

CI/CD 보안·품질도구

# 핵심 관점

좋은 오류처리는
예외를 Catch하는 기술이 아니다.

오류의 의미를 정확히 분류하고,
데이터를 안전하게 되돌리며,

사용자·운영자·감사자에게
각자 필요한 정보만 제공하는 설계다.

# 핵심 용어

| 용어 | 정의 |
| --- | --- |
| Validation Error | 필수값·형식·길이·범위가 올바르지 않은 오류 |
| Authentication Error | 사용자 신원을 확인하지 못한 오류 |
| Authorization Error | 인증된 사용자가 기능·데이터에 접근할 권한이 없는 오류 |
| Business Error | 시스템은 정상이나 업무규칙상 요청을 거절한 결과 |
| System Error | 코드·DB·Network·설정·인프라의 비정상 실패 |
| Timeout | 정해진 처리시간 안에 결과를 확정하지 못한 상태 |
| UNKNOWN | 응답은 실패했으나 실제 Commit·외부처리 결과를 확정하지 못한 상태 |
| Correlation ID | 여러 로그와 시스템 호출을 하나의 흐름으로 연결하는 식별자 |
| GUID | 하나의 업무거래를 식별하는 전역 ID |
| TraceId | 기술 호출경로를 연결하는 추적 ID |
| MDC | 현재 Thread의 로그에 공통 식별자를 자동 포함하는 문맥 |
| Transaction Log | 거래 시작·종료·결과·시간을 기록하는 로그 |
| Audit Log | 누가 어떤 중요 행위를 수행했는지 증명하는 로그 |
| Security Log | 인증·인가·차단·이상접근을 기록하는 로그 |
| Masking | 민감정보 일부를 가려 식별 가능성을 낮추는 처리 |
| Redaction | 민감정보 전체를 삭제하거나 대체하는 처리 |
| Idempotency | 동일 요청이 반복돼도 업무효과가 한 번만 발생하는 성질 |
| Error Mapping | 내부·외부 오류를 프로젝트 표준 오류로 변환하는 처리 |
| Fail Closed | 보안·감사정보를 확인할 수 없을 때 요청을 거절하는 정책 |
| Fail Open | 보조기능 실패에도 주 업무를 계속 처리하는 정책 |

# 현재 기준 구현상태 검토

현재 기준자료와 소스구조에서는 다음 기반을 확인할 수 있다.

| 영역 | 상태 | 검토 판단 |
| --- | --- | --- |
| BusinessException·SystemException | 구현 기반 존재 | 업무·시스템 오류 Mapping 일관성 확인 |
| ETF 성공·업무 오류·시스템 오류 | 구현 기반 존재 | Timeout 독립 분류와 UNKNOWN 보완 필요 |
| GUID·TraceId | 구현 기반 존재 | Gateway·업무·외부 호출 전파 확인 |
| 거래 시작·종료 로그 | 구현 기반 존재 | 누락·중복 종료 여부 검증 |
| 감사로그 기본 Field | 부분 기반 존재 | 대상식별자·행위·변경 전후 보완 |
| JWT 서명·issuer·audience 검증 | 구현 기반 존재 | 만료·jti 폐기·Header 정합 추가 확인 |
| 기능권한 | TCF 기반 존재 | 업무 데이터권한 추가 검증 필요 |
| Timeout 정책 | TCF 기반 존재 | 전체 예산·하위 Timeout 순서 검증 |
| Idempotency | 구조·설계 기반 | 변경거래별 실제 적용 확인 |
| 로그 마스킹 공통 Filter | 프로젝트 확인 필요 | 중앙 Redaction 적용 권장 |
| 감사로그 유실 대응 | 프로젝트 확인 필요 | 중요 거래 Fail Closed·Outbox 검토 |
| Secret Scan | CI 적용 확인 필요 | 배포 차단 Gate 필요 |
| 관리 Endpoint 통제 | 개별 확인 필요 | 운영망·관리자 인증 필수 |

거래로그에는 GUID·TraceId·업무코드·ServiceId·거래코드·사용자·지점·채널·결과·오류코드·처리시간 등이 연결되어야 하며, 요청·응답 Body 전체 저장은 개인정보와 용량 위험 때문에 별도 승인이 필요하다.

# D.1 원본 핵심 검토표

원본 부록의 8개 영역을 다음과 같이 유지한다.

| 검토 영역 | 핵심 질문 | 금지·주의 |
| --- | --- | --- |
| **입력** | 필수·형식·길이·범위 검증이 있는가? | 민감 입력을 로그에 남기지 않는다. |
| **인증** | Token 서명·만료·발급자를 검증하는가? | Access Token·Refresh Token·인증 Cookie 원문을 기록하지 않는다. |
| **인가** | 기능과 데이터 범위를 각각 검증하는가? | 화면 버튼 숨김은 서버 인가를 대체하지 않는다. |
| **업무 오류** | 사용자가 수정 가능한 메시지인가? | 내부 Table·SQL·Class를 노출하지 않는다. |
| **시스템 오류** | 원인 예외와 상관관계 ID가 남는가? | Stack Trace를 응답하지 않는다. |
| **Timeout** | 어느 계층의 제한인지 구분되는가? | 멱등성·오류 유형·전체 예산 확인 없는 Retry를 금지한다. |
| **데이터 변경** | 영향 행·중복·동시 수정이 검증되는가? | 부분 반영 여부를 확인한다. |
| **외부 연계** | 계약·Timeout·멱등성 정책이 있는가? | 상대 오류를 표준 오류로 변환한다. |

# D.2 검토 수행 절차

## D.2.1 검토 입력자료

요구사항

화면·이벤트 설계서

ServiceId·거래코드

Request·Response DTO

Handler·Facade·Service·Rule

DAO·Mapper·SQL

DB DDL·Index

오류코드 대장

JWT·권한 설정

Timeout 정책

외부 연계 계약

로그 Pattern

감사대상 정의

테스트 결과

배포·운영 Runbook

## D.2.2 검토 순서

1\. 거래의 목적과 데이터 변경 여부 확인

2\. 입력과 신뢰경계 확인

3\. 인증·인가 확인

4\. 업무 오류 조건 확인

5\. Transaction·데이터 정합성 확인

6\. 시스템 오류·예외 Mapping 확인

7\. Timeout·Retry·멱등성 확인

8\. 로그·개인정보·감사 확인

9\. 외부 연계 장애전파 확인

10\. 테스트·운영·Rollback 확인

## D.2.3 검토결과 상태

| 상태 | 의미 |
| --- | --- |
| PASS | 기준 충족·증적 확인 |
| FAIL | 기준 위반 |
| PARTIAL | 일부 구현·보완 필요 |
| N/A | 적용대상 아님 |
| VERIFY | 실행환경에서 추가 확인 필요 |
| ACCEPTED RISK | 임시 위험 승인 |

## D.2.4 심각도

| 등급 | 기준 | 예 |
| --- | --- | --- |
| Blocker | 운영 배포 불가 | Token 로그, 권한 우회 |
| Critical | 데이터·보안 중대위험 | 부분 Commit, 개인정보 원문 |
| Major | 운영 전 필수 보완 | Timeout 미등록, 감사 누락 |
| Minor | 위험이 낮은 표준 미준수 | 로그 Event명 불일치 |
| Suggestion | 향후 개선 | 추가 Metric |

## D.2.5 검토표 기록형식

| ID | 영역 | 질문 | 결과 | 증적 | 심각도 | Owner | 완료일 | 재검증 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SEC-001 | 인증 | JWT aud 검증 | PASS | Test 결과 | \- | 개발 | \- | 완료 |
| LOG-004 | 로그 | Request Body 원문 | FAIL | 코드 위치 | Critical | 개발 | 7/20 | 예정 |

# D.3 오류 분류와 응답 기준

## D.3.1 오류 분류체계

| 유형 | 발생 예 | 처리주체 | 사용자 행동 |
| --- | --- | --- | --- |
| 입력 오류 | 필수값 누락·날짜형식 | STF·DTO | 입력 수정 |
| 인증 오류 | Token 없음·만료 | Gateway·Filter | 재로그인·갱신 |
| 인가 오류 | 기능·데이터권한 없음 | STF·Rule | 권한 문의 |
| 거래통제 | Service 중지 | STF·OM | 운영안내 |
| 업무 오류 | 중복·상태 위반 | Service·Rule | 업무값 수정 |
| 동시성 오류 | Version 충돌 | Service·SQL | 최신조회 |
| 외부 업무 오류 | 고객 없음 | Client·Adapter | 업무안내 |
| 외부 시스템 오류 | 503·연결 실패 | Client·Adapter | 잠시 후 확인 |
| DB 오류 | SQL·Connection | Mapper·DB | 시스템 문의 |
| 프로그램 오류 | NPE·설정 누락 | Application | 시스템 문의 |
| Timeout | 제한시간 초과 | Executor·Client·DB | 결과확인 |
| UNKNOWN | Commit 여부 불명 | TCF·운영 | 재실행 전 조회 |

## D.3.2 오류 판정 원칙

예측 가능하고
업무적으로 설명 가능한가?

→ 업무 오류

예상하지 못했고
시스템 복구·운영조치가 필요한가?

→ 시스템 오류

시간을 초과했지만
실제 처리결과가 불명확한가?

→ Timeout·UNKNOWN

## D.3.3 BusinessException

적용 대상:

입력 검증 실패

대상 없음

중복

상태전이 불가

권한 없음

동시 수정

거래통제

금지:

DB Connection 실패를 BusinessException으로 숨김

NullPointerException을 업무 오류로 변환

모든 Exception을 “처리할 수 없습니다” 업무 오류로 변환

## D.3.4 SystemException

적용 대상:

DB 연결·SQL 오류

필수 설정 누락

외부 시스템 기술오류

Serialization 오류

예상하지 못한 프로그램 오류

파일 I/O 오류

SystemException에는 내부 원인 예외를 보존한다.

throw new SystemException(
CtReservationErrorCode.PERSISTENCE\_ERROR,
"상담예약 DB 처리 오류",
cause
);

## D.3.5 Catch 책임

업무 Handler·Service에서 다음 형태를 금지한다.

try {
service.execute();
} catch (Exception ex) {
return null;
}

try {
service.execute();
} catch (Exception ex) {
return successResponse();
}

광범위 Catch는 TCF가 오류를 분류하고 Transaction을 Rollback하는 것을 방해한다.

예외를 Catch할 수 있는 경우:

외부 오류를 표준 오류로 변환

기술 예외에 업무 Context 추가

자원 정리

허용된 Fallback 수행

특정 예외만 복구

Catch 후에는 원인 예외를 보존하거나 명시적 복구결과를 반환한다.

## D.3.6 안전한 오류응답

{
"header": {
"serviceId": "CT.Reservation.update",
"guid": "G-20260718-000001",
"traceId": "T-20260718-000001"
},
"result": {
"success": false,
"code": "E-CT-RSV-0006",
"message": "다른 사용자가 먼저 변경했습니다. 최신 내용을 다시 조회해 주세요.",
"resultType": "BUSINESS\_ERROR"
},
"body": null
}

## D.3.7 시스템 오류응답

{
"header": {
"serviceId": "CT.Reservation.create",
"guid": "G-20260718-000002"
},
"result": {
"success": false,
"code": "E-CT-RSV-9002",
"message": "처리 중 오류가 발생했습니다. 문의 ID를 확인해 주세요.",
"resultType": "SYSTEM\_ERROR"
},
"body": null
}

외부에 포함하지 않을 항목:

Stack Trace

Exception Class

SQL 원문

Bind Parameter

Table·Column명

DB 계정·Host

서버 파일경로

내부 URL

Library Version

소스코드 위치

## D.3.8 HTTP 상태와 업무 결과

HTTP 상태와 업무 결과코드는 별도 책임이다.

| 상황 | HTTP 예 | 결과 유형 |
| --- | --- | --- |
| Token 없음·만료 | 401 | AUTHENTICATION\_ERROR |
| 기능권한 없음 | 403 | AUTHORIZATION\_ERROR |
| 잘못된 URL | 404 | TECHNICAL\_ERROR |
| 입력 오류 | 프로젝트 API 정책 | VALIDATION\_ERROR |
| 업무 거절 | 프로젝트 TCF 정책 | BUSINESS\_ERROR |
| 시스템 오류 | 500 | SYSTEM\_ERROR |
| Gateway Timeout | 504 | TIMEOUT |
| 서비스 일시중지 | 503 또는 표준 통제응답 | CONTROLLED |

TCF 온라인 전문이 HTTP 200과 내부 결과코드를 사용하는 경우에도 인증·Gateway 수준 오류는 401·403·404·5xx로 먼저 종료될 수 있다.

## D.3.9 오류코드 원칙

한 오류코드는 하나의 의미만 가진다.

폐기된 오류코드를 다른 의미로 재사용하지 않는다.

오류코드만 보고 사용자 행동과 운영대응을 구분할 수 있어야 한다.

공통 오류와 업무 오류를 분리한다.

외부 시스템 오류는 소비 업무 표준으로 Mapping한다.

# D.4 거래로그·애플리케이션 로그 기준

## D.4.1 로그 유형

| 로그 | 목적 | 주요 사용자 |
| --- | --- | --- |
| 애플리케이션 로그 | 코드·기술 오류 분석 | 개발·운영 |
| 거래로그 | 거래상태·결과·성능 | 운영·업무 |
| 감사로그 | 중요 조회·변경 증명 | 감사·보안 |
| 보안로그 | 인증·인가·이상접근 | 보안 |
| 연계로그 | 시스템 간 호출 분석 | 개발·운영 |
| SQL·성능로그 | Mapper·SQL 지연 분석 | DBA·운영 |
| 배치로그 | Job·Step·건수·재시작 | 운영 |
| 배포로그 | Artifact·설정·실행자 | DevOps·감사 |

하나의 로그가 모든 목적을 대신하지 않는다.

## D.4.2 표준 로그 흐름

Gateway 요청수신

→ 인증 결과

→ TCF TX\_START

→ 주요 업무 단계

→ 외부·SQL 구간

→ ETF 결과처리

→ TCF TX\_END

→ 감사·Metric

## D.4.3 거래로그 필수 Field

| 분류 | Field |
| --- | --- |
| 식별 | guid, traceId, requestId |
| 업무 | businessCode, serviceId, transactionCode |
| 사용자 | userId, branchId, channelId |
| 시스템 | host, instanceId, war, artifactVersion |
| 시간 | requestTime, startTime, endTime, elapsedMs |
| 결과 | resultType, resultCode, errorCode |
| 연계 | targetSystem, childServiceId, externalResult |
| 데이터 | affectedRows, itemCount, fileCount |
| 통제 | timeoutMs, idempotencyStatus, controlResult |
| 보안 | authType, auditTargetYn, maskingAppliedYn |

## D.4.4 MDC Field

TCF 거래 시작 시 다음 값을 MDC에 등록하는 구조가 적절하다.

guid

traceId

businessCode

serviceId

transactionCode

userId

branchId

channelId

비동기 Thread·Executor·CompletableFuture를 사용할 때 MDC와 TransactionContext를 명시적으로 전달한다.

금지:

부모 Thread의 MDC가 자동 전달될 것이라고 가정

Thread Pool 재사용 후 MDC를 정리하지 않음

다른 거래의 GUID가 다음 거래 로그에 남음

## D.4.5 구조화 로그 예

log.info(
"RESERVATION\_CREATED reservationId={} status={} version={} affectedRows={}",
SafeLog.maskIdentifier(reservationId),
statusCode,
versionNo,
affectedRows
);

MDC Pattern이 적용돼 있다면 각 로그문에 GUID·ServiceId를 반복해 문자열로 붙이지 않는다.

## D.4.6 로그 Event 이름

권장:

TX\_START

TX\_END

AUTH\_FAILED

AUTHZ\_DENIED

RESERVATION\_CREATED

RESERVATION\_UPDATED

RESERVATION\_CANCELED

EXTERNAL\_CALL\_START

EXTERNAL\_CALL\_END

SQL\_SLOW

IDEMPOTENCY\_HIT

UNKNOWN\_RESULT

금지:

여기 들어옴

1번 성공

처리중

TEST

DEBUG1

에러남

## D.4.7 로그 Level

| Level | 기준 | 예 |
| --- | --- | --- |
| TRACE | 매우 상세한 개발 진단 | 운영 기본 Off |
| DEBUG | 내부 분기·계산 | 운영 제한 |
| INFO | 정상 주요 상태·거래 완료 | 생성·상태전이 |
| WARN | 복구 가능 이상·업무 주의 | 재시도·충돌 증가 |
| ERROR | 예상하지 못한 시스템 실패 | DB·프로그램 오류 |

업무상 예상된 중복·Not Found·상태오류를 모두 ERROR Stack Trace로 기록하지 않는다.

## D.4.8 Stack Trace 기준

| 상황 | Stack Trace |
| --- | --- |
| Validation | 불필요 |
| 예상 업무 오류 | 일반적으로 불필요 |
| 권한 거절 | 보안 Event 중심 |
| 시스템 오류 | 필요 |
| Timeout | 원인·구간에 따라 필요 |
| 외부 4xx 업무 오류 | 불필요 |
| 외부 5xx·연결 실패 | 필요 |
| 재시도 성공 | 최초 오류 요약·최종 결과 |

같은 예외를 Gateway·TCF·Handler·Service에서 반복적으로 전체 Stack Trace로 기록하지 않는다.

## D.4.9 요청·응답 Body 로그

기본 원칙:

원문 전체 기록 금지

허용 가능한 정보:

Body 크기

Item 건수

필드 존재 여부

업무 식별자의 Masking 값

Request Schema Version

Payload Hash

검증 실패 Field명

예:

requestBodyBytes=1240

itemCount=20

customerNoMasked=C0000\*\*\*\*\*

payloadHash=SHA256:...

금지:

log.info("request={}", request);
log.info("response={}", response);

DTO toString()에 민감정보가 포함될 수 있다.

# D.5 개인정보·비밀정보 로그 검토

## D.5.1 절대 원문 기록 금지

Password

PIN

Access Token

Refresh Token

Authorization Header

Private Key

Client Secret

Session Cookie

JSESSIONID

인증서 Private Data

주민등록번호

카드번호 전체

계좌번호 전체

개인정보 포함 파일 원문

## D.5.2 조건부 기록

| 정보 | 허용 방식 |
| --- | --- |
| 고객번호 | 부분 Masking·Hash |
| 사용자 ID | 거래·감사 목적 최소 기록 |
| 지점 ID | 운영·감사 목적 |
| 휴대전화 | 일부 Masking |
| 이메일 | 일부 Masking |
| IP | 보안로그·접근통제 하 기록 |
| 예약 ID | 운영로그 Masking 또는 내부식별 |
| 상담메모 | 원칙적 미기록 |
| 파일명 | 경로 제거·이름 Sanitizing |
| SQL Bind | 운영 기본 미기록 |

## D.5.3 Masking 예

| 원문 | 로그 |
| --- | --- |
| C000012345 | C0000\*\*\*\*\* |
| honggildong | hon\*\*\*\*\*\* |
| 01012345678 | 010\*\*\*\*5678 |
| user@example.com | u\*\*\*@example.com |
| 1234567890123456 | 1234\*\*\*\*\*\*\*\*3456 |

## D.5.4 Hash 사용

다음과 같은 상관분석은 원문 대신 Hash를 사용할 수 있다.

같은 고객에서 오류가 반복되는가?

같은 Idempotency Key가 재사용됐는가?

같은 대상에 무권한 접근이 반복되는가?

Hash 기준:

승인된 Algorithm

Salt·Key 관리

환경 간 비교 필요성

보존기간

재식별 위험

단순 MD5로 개인정보를 Hash한 뒤 안전하다고 판단하지 않는다.

## D.5.5 Redaction 대상 Key

공통 Logger·HTTP Filter에서 다음 Key를 대소문자 구분 없이 제거한다.

password

passwd

pwd

authorization

accessToken

refreshToken

token

secret

privateKey

cookie

sessionId

jSessionId

token이라는 단어를 가진 정상 업무필드가 존재할 수 있으므로 Schema 기반 Redaction과 Allow List를 병행한다.

# D.6 인증 검토

JWT 검증은 문자열 Decode가 아니라 서명·만료·발급자·대상·Claim 정합성 검증을 포함해야 한다. Gateway가 검증했더라도 업무 권한과 데이터 범위는 TCF·업무 Service에서 다시 판단해야 한다.

## D.6.1 인증 책임

| 영역 | 책임 |
| --- | --- |
| tcf-jwt | 발급·갱신·폐기·JWKS |
| tcf-gateway | 외부 JWT 1차 검증 |
| tcf-web | Gateway 우회 가능 시 Filter 검증 |
| STF | 인증문맥·Header 정합 |
| 업무 Service·Rule | 데이터·상태 기반 권한 |
| OM | 사용자·권한·예외·폐기 정책 |

## D.6.2 JWT 검토표

| 검토 | 질문 |
| --- | --- |
| Algorithm | 허용 Algorithm을 고정했는가? |
| Signature | JWKS·Public Key로 검증하는가? |
| kid | Key Rotation을 지원하는가? |
| iss | 승인 발급자와 일치하는가? |
| aud | 대상 시스템과 일치하는가? |
| exp | 만료 여부를 검증하는가? |
| nbf | 사용 시작시각을 검증하는가? |
| Clock Skew | 허용 오차가 과도하지 않은가? |
| sub | 사용자 식별자가 존재하는가? |
| jti | 폐기·재사용 확인이 가능한가? |
| Claim | 사용자·지점·권한 형식이 올바른가? |
| Header 비교 | 요청 사용자와 인증 사용자가 일치하는가? |
| 오류 | 401과 403을 구분하는가? |
| 로그 | Token 원문을 기록하지 않는가? |

## D.6.3 Refresh Token

원문 DB 저장 금지

Hash 저장

Rotation

재사용 탐지

Token Family

만료

로그아웃·강제폐기

탈취 의심 시 전체 Family 폐기

## D.6.4 Session 검토

| 검토 | 질문 |
| --- | --- |
| Cookie | Secure·HttpOnly·SameSite 적용 |
| Session ID | 로그인 후 재발급 |
| Timeout | 최종 정책과 일치 |
| Logout | 서버 Session 제거 |
| Cluster | 장애 시 유지정책 |
| 동시 로그인 | 허용·차단정책 |
| 로그 | Session ID 원문 금지 |
| 고정 공격 | Session Fixation 방지 |

## D.6.5 인증 오류 로그

필수:

eventType

resultCode

failureReasonCode

requestId

sourceIp

userIdMasked 또는 Claim 유무

issuer

audience

kid

instanceId

timestamp

금지:

Token 원문

Signature 원문

Refresh Token

Cookie 원문

Private Key

## D.6.6 인증 실패 위치

인증은 TCF 전에 실패할 수 있다.

Bearer Token 없음

→ Gateway·Filter 401

→ TCF 미진입

→ TCF 거래로그 없음

따라서 장애 추적 시 다음을 확인한다.

| 실패 위치 | 우선 로그 |
| --- | --- |
| Proxy·L4 | Access Log |
| Gateway 진입 | Gateway Request Log |
| JWT 검증 | Security Log |
| 업무 WAR Filter | WAR Security Log |
| STF | TCF 거래로그 |
| Handler 이후 | 업무·SQL 로그 |

# D.7 인가 검토

## D.7.1 기능권한과 데이터권한

기능권한
\=
CT.Reservation.update를 실행할 수 있는가?

데이터권한
\=
이 사용자가 이 지점·고객의 예약을 수정할 수 있는가?

둘 중 하나라도 실패하면 변경을 허용하지 않는다.

## D.7.2 권한 검토표

| 항목 | 질문 |
| --- | --- |
| 인증문맥 | userId·branchId가 검증된 값인가? |
| 기능권한 | ServiceId 실행권한이 있는가? |
| 데이터범위 | 지점·조직·소유자 범위를 확인하는가? |
| 상태권한 | 현재 상태에서 행위가 가능한가? |
| 관리자 | 일반권한과 분리됐는가? |
| 직접 URL | 화면 우회 호출도 차단하는가? |
| SQL Scope | 권한조건이 SQL에도 반영되는가? |
| 존재 은닉 | 타 지점 대상 존재 여부를 숨기는가? |
| 감사 | 권한 변경·거절을 기록하는가? |
| Cache | 권한 변경 후 즉시 반영되는가? |

## D.7.3 Request Header 신뢰

금지:

Body.userId

Body.branchId

Body.roles

Body.isAdmin

Body.ownerUserId

사용:

JWT Claim

검증된 Gateway Header

AuthenticationContext

TransactionContext

## D.7.4 관리자 기능

관리 Endpoint·강제실행·거래통제 해제에는 다음이 필요하다.

별도 관리자 권한

운영망 접근

강한 인증

승인절차

변경사유

감사로그

변경 전후 값

복구방법

## D.7.5 Gateway 우회 방어

외부 Client

→ Gateway만 접근

Gateway

→ 업무 WAR 접근

외부 Client

× 업무 WAR 직접 접근

방어:

L4·Firewall ACL

Tomcat Binding

mTLS 또는 내부 인증

업무 WAR JWT Filter

Header 위조검증

허용 Source 제한

# D.8 입력 검증

## D.8.1 검증 단계

전문 Header 검증

→ JSON 형식

→ DTO 변환

→ 필수값

→ 길이·범위

→ Enum·Code

→ 상호 필드 관계

→ 업무규칙

## D.8.2 입력 검토표

| 항목 | 질문 |
| --- | --- |
| Null | 필수값 검증 |
| Blank | 공백문자 처리 |
| Length | DB·계약 최대길이 |
| Number | 최소·최대·Scale |
| Date | 형식·Timezone·기간 |
| Enum | Allow List |
| List | 최대 건수 |
| File | 최대 크기·MIME |
| Paging | Page Size 상한 |
| Sort | 허용 Column |
| Text | 제어문자·HTML |
| Nested | 깊이·개수 제한 |
| Encoding | UTF-8 |
| Duplicate Field | Parser 정책 |
| Unknown Field | 허용·거절정책 |

## D.8.3 잘못된 변환

금지:

"ABC" 숫자변환 실패

→ null

→ 기본값 20 사용

잘못된 형식은 명확한 Validation 오류로 반환한다.

## D.8.4 Allow List

다음은 Allow List로 통제한다.

상태코드

정렬 Column

정렬방향

파일확장자

업무코드

ServiceId

Redirect URL

외부 Host

## D.8.5 SQL Injection

권장:

WHERE RESERVATION\_ID = #{reservationId}

금지:

WHERE RESERVATION\_ID = '${reservationId}'

정렬:

<choose>
<when test="sortCode == 'DATE'">
ORDER BY RESERVATION\_DTM
</when>
<otherwise>
ORDER BY RESERVATION\_ID
</otherwise>
</choose>

금지:

ORDER BY ${sortColumn}

## D.8.6 XSS

상담메모·사유·이름은 출력 Context에 맞게 Encoding한다.

HTML Body

HTML Attribute

JavaScript

URL

CSV·Excel

각 Context의 Encoding 방식은 다르다.

입력값에서 <를 제거하는 것만으로 XSS가 해결되지 않는다.

# D.9 업무 오류 검토

## D.9.1 좋은 업무 오류

오류 조건이 예측 가능하다.

업무코드가 고유하다.

사용자가 취할 행동을 알려준다.

시스템 장애와 구분된다.

운영로그에서 ServiceId·GUID로 찾을 수 있다.

변경거래이면 Rollback된다.

## D.9.2 상담예약 예

| 조건 | 오류코드 | 메시지 |
| --- | --- | --- |
| 예약 없음 | E-CT-RSV-0001 | 예약을 찾을 수 없습니다. |
| 상태 위반 | E-CT-RSV-0004 | 현재 상태에서는 처리할 수 없습니다. |
| 권한 없음 | E-CT-RSV-0005 | 변경할 권한이 없습니다. |
| Version 충돌 | E-CT-RSV-0006 | 최신 내용을 다시 조회해 주세요. |
| 중복 | E-CT-RSV-0008 | 동일 예약이 이미 존재합니다. |
| 취소사유 없음 | E-CT-RSV-0011 | 취소사유를 입력해 주세요. |

## D.9.3 업무 오류 로그 Level

| 오류 | 권장 |
| --- | --- |
| 입력 오류 | INFO·WARN |
| Not Found | INFO |
| 중복 | INFO·Metric |
| 동시성 충돌 | INFO·WARN·Metric |
| 권한 거절 | Security WARN |
| 거래통제 | WARN |
| 반복 비정상 요청 | WARN·보안 Alert |

예상 업무 오류마다 전체 Stack Trace를 ERROR로 남기면 실제 장애를 찾기 어렵다.

# D.10 시스템 오류 검토

## D.10.1 시스템 오류 흐름

DB·외부·코드·설정 오류

→ 원인 Exception

→ Facade Transaction Rollback

→ TCF Catch

→ ETF.systemError

→ 서버 내부 상세 로그

→ 거래로그 SYSTEM\_ERROR

→ Metric·Alert

→ 외부 안전 응답

## D.10.2 검토표

| 항목 | 질문 |
| --- | --- |
| Cause | 원인 예외가 유지되는가? |
| Rollback | 변경이 전체 원복되는가? |
| GUID | 응답과 로그에 같은 값이 있는가? |
| Error Code | 업무별·공통 오류가 등록됐는가? |
| Stack | 서버 내부에 한 번 기록되는가? |
| Response | 내부정보가 제거됐는가? |
| Alert | 운영대상 오류인가? |
| Metric | 시스템 오류율에 반영되는가? |
| Idempotency | FAIL·UNKNOWN 상태가 갱신되는가? |
| Resource | Connection·Thread·파일이 반환되는가? |

## D.10.3 로그 예

log.error(
"RESERVATION\_CREATE\_FAILED reservationId={} errorCode={}",
SafeLog.maskIdentifier(reservationId),
CtReservationErrorCode.PERSISTENCE\_ERROR.code(),
exception
);

## D.10.4 금지

catch (Exception ex) {
log.error(ex.getMessage());
throw new BusinessException("오류");
}

문제:

Stack Trace 소실

오류 유형 왜곡

Root Cause 분석 불가

시스템 장애가 업무 오류율로 집계

# D.11 Timeout 검토

Timeout은 Connect·Read·DB Query·DB Pool·TCF 전체 거래 등 계층별로 구분해야 하며, 하위 Timeout과 내부 처리시간의 합은 상위 거래 제한을 넘지 않아야 한다.

## D.11.1 Timeout 종류

| 종류 | 의미 |
| --- | --- |
| Gateway Timeout | Gateway가 전체 Downstream 응답을 기다리는 시간 |
| TCF Online Timeout | 업무거래 전체 제한시간 |
| Transaction Timeout | DB Transaction 제한 |
| Query Timeout | SQL 실행 제한 |
| Connection Timeout | Pool에서 Connection 대기 |
| Connect Timeout | 외부 TCP 연결 대기 |
| Read Timeout | 외부 응답 대기 |
| Cache Timeout | 원격 Cache 응답 제한 |
| File Timeout | 업·다운로드·변환 제한 |
| Batch Timeout | Job·Step 최대 실행시간 |

## D.11.2 Timeout 예산 예

### 목록조회 3초

| 구간 | 예산 |
| --- | --- |
| Gateway·STF | 200ms |
| DB Pool 대기 | 300ms |
| SQL | 1,800ms |
| 응답조립·ETF | 200ms |
| 여유 | 500ms |
| 합계 | 3,000ms |

### 등록 5초

| 구간 | 예산 |
| --- | --- |
| Gateway·STF | 300ms |
| 고객 연계 | 1,500ms |
| DB Transaction | 2,000ms |
| ETF | 200ms |
| 여유 | 1,000ms |
| 합계 | 5,000ms |

금지:

TCF Timeout 3초

Query Timeout 5초

외부 Read Timeout 10초

## D.11.3 Timeout 이후 확인

업무 Thread가 실제 종료됐는가?

SQL이 취소됐는가?

Connection이 반환됐는가?

DB Commit이 수행됐는가?

외부 시스템은 처리했는가?

History가 저장됐는가?

Idempotency 상태는 무엇인가?

사용자가 다시 요청할 수 있는가?

## D.11.4 UNKNOWN

변경거래에서 다음 상황은 단순 실패가 아니다.

DB Commit 성공

→ 응답 전송 실패

→ Client Timeout

처리:

resultType=UNKNOWN

GUID·Idempotency Key 제공

상태조회

Master Version 확인

History TraceId 확인

외부 결과조회

대사 후 SUCCESS·FAIL 확정

## D.11.5 Retry Matrix

| 오류 | 조회 Retry | 변경 Retry |
| --- | --- | --- |
| Connect 실패 | 제한적 가능 | 기본 금지 |
| Connection Reset | 가능 | 멱등성 필요 |
| HTTP 503 | 제한적 가능 | 승인 필요 |
| Read Timeout | 주의 | 즉시 금지 |
| Validation | 금지 | 금지 |
| 인증 401 | Token 갱신 후 1회 | 동일 Idempotency Key |
| 권한 403 | 금지 | 금지 |
| 업무 오류 | 금지 | 금지 |
| Version 충돌 | 금지 | 최신조회 |
| UNKNOWN | 결과조회 | 결과조회 |

## D.11.6 Retry 조건

최대 횟수

Backoff

Jitter

전체 Timeout 예산

멱등성

상대 시스템 정책

Circuit Breaker

Retry Metric

최종 실패처리

# D.12 데이터 변경 검토

## D.12.1 변경거래 필수 통제

인증·인가

상태조건

Version

업무 중복

DB Unique

Idempotency

영향 행 수

Master·History Transaction

Rollback Test

Timeout 결과조회

## D.12.2 영향 행 수

INSERT 1
→ 정상

UPDATE 1
→ 정상

UPDATE 0
→ 미존재·권한·상태·동시성 구분

UPDATE 2+
→ 조건·데이터 결함

금지:

mapper.updateReservation(command);
return success();

권장:

int affected = mapper.updateReservation(command);

if (affected != 1) {
throw ...
}

## D.12.3 Version

UPDATE CT\_CONTACT\_RESERVATION
SET VERSION\_NO = VERSION\_NO + 1
WHERE RESERVATION\_ID = #{reservationId}
AND STATUS\_CD = 'READY'
AND VERSION\_NO = #{expectedVersion}
AND BRANCH\_ID = #{branchId}

검토:

상세 Response에 Version이 있는가?

수정 Request에 Version이 있는가?

SQL에 Version 조건이 있는가?

성공 시 증가하는가?

0건을 동시성 오류로 처리하는가?

## D.12.4 중복

화면 중복클릭 방지

\+ 사전 중복조회

\+ DB Unique Constraint

\+ Idempotency

화면 버튼 비활성화와 사전조회만으로 동시 중복을 막을 수 없다.

## D.12.5 Transaction

Master

\+ History Header

\+ History Detail

\+ Idempotency 결과

→ 같은 Transaction 또는 승인된 일관성 전략

## D.12.6 로그·감사 실패 정책

| 대상 | 권장 정책 |
| --- | --- |
| Domain History | 실패 시 업무 Rollback |
| 중요 변경 감사 | Fail Closed 또는 Durable Outbox |
| 일반 거래로그 DB 적재 | 업무 지속 가능하더라도 즉시 Alert |
| Application File Log | 대체 Appender·운영 Alert |
| Metric | 업무 지속 가능, 관측성 Alert |
| 보안로그 | 중요 인증·관리행위는 유실 방지 |

## D.12.7 부분 반영 대사

Master 존재·History 없음

History 존재·Master 없음

외부 성공·내부 실패

내부 성공·외부 실패

Idempotency PROCESSING 잔존

Version과 History 불일치

부분 반영은 일반 오류가 아니라 데이터 정합성 장애로 관리한다.

# D.13 감사로그 검토

중요 조회·변경·다운로드·정책관리 거래에는 사용자·지점·대상·행위·결과·시각·GUID와 필요한 변경 전후 값을 마스킹해 기록해야 한다.

## D.13.1 감사대상

고객 상세정보 조회

대량 조회·다운로드

파일 다운로드

고객정보 변경

예약 등록·수정·취소

권한 변경

사용자 활성·비활성

거래통제 변경

Timeout 정책 변경

Gateway Route 변경

Cache 강제삭제

Batch 강제실행

관리자 강제처리

## D.13.2 필수 Field

| Field | 설명 |
| --- | --- |
| auditId | 감사 Event ID |
| guid | 원거래 |
| traceId | 기술 추적 |
| serviceId | 수행 기능 |
| transactionCode | 운영 거래 |
| userId | 수행자 |
| branchId | 조직 |
| sourceIp | 접속 위치 |
| targetType | 고객·예약·파일·정책 |
| targetIdMasked | 대상 식별 |
| action | VIEW·CREATE·UPDATE·CANCEL·DOWNLOAD |
| beforeValue | 중요 변경 전 값 |
| afterValue | 중요 변경 후 값 |
| reason | 변경·강제처리 사유 |
| result | SUCCESS·FAIL |
| timestamp | 수행시각 |
| artifactVersion | 실행 Version |

## D.13.3 History와 Audit

| Domain History | Audit Log |
| --- | --- |
| 업무상태 변화 | 사용자 행위 증명 |
| Aggregate Transaction 참여 | 별도 보존·보안 정책 |
| 복원·업무조회 | 감사·조사 |
| Before·After 데이터 | Who·What·When·Result |

하나의 History Table로 감사요건 전체를 대신하지 않는다.

## D.13.4 감사로그 보안

일반 개발자 수정 금지

별도 접근권한

열람 자체 감사

위변조 방지

시각 동기화

보존기간

법적 Hold

파기절차

# D.14 외부 연계 검토

## D.14.1 기본 검토표

| 영역 | 질문 |
| --- | --- |
| 소유권 | 제공·소비 도메인은 누구인가? |
| 계약 | Request·Response·Version이 있는가? |
| 인증 | Token·인증서·mTLS 정책은? |
| 개인정보 | 최소한만 전달하는가? |
| Timeout | Connect·Read·전체 예산은? |
| Retry | 대상 오류와 횟수는? |
| 멱등성 | 변경 재시도 안전성은? |
| Circuit Breaker | 지속 실패를 차단하는가? |
| Fallback | 대체결과의 의미는? |
| 오류 Mapping | 상대 오류를 어떻게 변환하는가? |
| Trace | GUID·TraceId를 전달하는가? |
| Log | 원문 전문을 저장하지 않는가? |
| 장애격리 | Thread·Pool이 분리됐는가? |
| 테스트 | Contract·지연·오류주입이 있는가? |

## D.14.2 오류 Mapping 예

| 제공 시스템 결과 | 소비 업무 오류 | 사용자 |
| --- | --- | --- |
| 고객 없음 | E-CT-RSV-0007 | 고객 없음 |
| 제공 권한 거절 | E-CT-INT-0403 | 권한 오류 |
| 제공 Timeout | E-CT-INT-0504 | 결과 확인 |
| 제공 시스템 오류 | E-CT-INT-0500 | 시스템 문의 |

운영로그에는 다음을 보존한다.

원본 오류코드

대상 시스템

대상 ServiceId

HTTP 상태

처리시간

재시도 횟수

Circuit 상태

GUID·TraceId

## D.14.3 상대 오류 원문 노출 금지

금지:

{
"message": "SV\_CUSTOMER table ORA-00942"
}

소비 시스템이 제공 시스템의 내부 Table·URL·Stack Trace를 사용자에게 그대로 전달하지 않는다.

## D.14.4 Fallback

Fallback은 정상결과와 구분돼야 한다.

{
"dataStatus": "STALE",
"asOfDtm": "2026-07-18T09:00:00",
"items": \[\]
}

금지:

외부 호출 실패

→ 빈 목록

→ 정상 SUCCESS

빈 결과와 연계 실패를 구분하지 못하면 업무판단이 왜곡된다.

# D.15 보안 취약점 검토

## D.15.1 주요 항목

SQL Injection

XSS

CSRF

SSRF

Path Traversal

Command Injection

Insecure Deserialization

Mass Assignment

Broken Access Control

Sensitive Data Exposure

Security Misconfiguration

Dependency Vulnerability

Secret Exposure

Unrestricted File Upload

Open Redirect

CORS Misconfiguration

## D.15.2 Mass Assignment

금지:

BeanUtils.copyProperties(
request,
entity
);

Request에 포함된 다음 값이 변경될 수 있다.

statusCode

ownerUserId

branchId

versionNo

createdBy

adminYn

명시적 Command Mapping을 사용한다.

## D.15.3 SSRF

외부 URL을 입력받는 기능에서는 다음을 확인한다.

허용 Host Allow List

Private IP 차단

Metadata Endpoint 차단

Redirect 제한

Protocol 제한

DNS Rebinding 대응

Connect·Read Timeout

## D.15.4 CORS

운영 Origin Allow List

Credentials 정책

허용 Method·Header 최소화

Wildcard + Credentials 금지

Preflight Cache 검토

## D.15.5 관리 Endpoint

Actuator 상세

Swagger

OM Admin API

Runtime Diagnostic

Cache Evict

Batch Force Run

필수 통제:

운영망

관리자 인증

기능권한

감사로그

최소 정보

Rate Limit

# D.16 파일 처리 검토

## D.16.1 업로드

| 항목 | 질문 |
| --- | --- |
| 크기 | 최대 크기와 사용자별 제한 |
| 확장자 | Allow List |
| MIME | 실제 Content 검사 |
| 파일명 | Path 제거·Sanitize |
| 저장경로 | 웹 Root 외부 |
| 악성코드 | Quarantine·검사 |
| 권한 | 업로드 기능·대상 데이터권한 |
| 중복 | Checksum·Idempotency |
| 로그 | 파일명 Masking·크기·Checksum |
| 개인정보 | 암호화·보존·파기 |
| Streaming | 메모리 전체 적재 금지 |

## D.16.2 다운로드

JWT·권한 검증

대상 데이터권한

Path Traversal 차단

Content-Disposition 안전처리

Range 정책

다운로드 감사

대량 추출 통제

URL 만료

직접 저장경로 노출 금지

## D.16.3 금지

../../etc/passwd

원본 파일경로 응답

업로드 확장자만 검사

실행 가능 파일 웹 Root 저장

대용량 파일 byte\[\] 전체 로딩

파일 내용 전체 로그

# D.17 Batch·Scheduler 검토

## D.17.1 Batch 오류

Job 시작 실패

Step 실패

부분 건수 처리

Skip·Retry

Checkpoint 불일치

중복 실행

재시작

Poison Data

결과 대사

## D.17.2 Batch 로그

필수:

jobName

jobInstanceId

jobExecutionId

businessDate

stepName

readCount

writeCount

skipCount

retryCount

commitCount

rollbackCount

start·end

result

errorCode

checkpoint

개별 고객정보 원문을 건별 INFO 로그로 남기지 않는다.

## D.17.3 Scheduler

Scheduler 실행 성공
≠ Batch 업무 성공

다음 로그를 분리한다.

SCHEDULE\_TRIGGERED

JOB\_STARTED

JOB\_COMPLETED

JOB\_FAILED

MISFIRE

DUPLICATE\_EXECUTION\_BLOCKED

## D.17.4 중복 실행

Job Lock

Lease 만료

Fencing Token

업무일자+Job Instance Unique

강제실행 감사

# D.18 Cache 검토

## D.18.1 보안·오류

Cache Key에 개인정보 원문이 없는가?

Cache Value가 암호화 대상인가?

권한별 데이터가 공유되지 않는가?

사용자 A의 결과가 사용자 B에게 노출되지 않는가?

Cache 장애 시 Fallback이 정의됐는가?

Stale 데이터임을 구분하는가?

강제 Evict가 감사되는가?

## D.18.2 Cache 로그

권장:

cacheName

operation

result=HIT|MISS|EVICT|ERROR

elapsedMs

keyHash

금지:

고객번호 원문 Key

Cache Value 전체

Token Cache 원문

모든 Hit 건별 INFO

# D.19 Secret·설정 검토

## D.19.1 금지 위치

Git Source

application-prod.yml 원문

WAR

Docker Image Layer

Build Log

Shell History

README

Test Fixture

Error Response

## D.19.2 허용 관리

Vault

Secret Manager

환경변수

보호된 운영 설정파일

HSM·Key Store

CI Protected Variable

## D.19.3 검토표

| 항목 | 질문 |
| --- | --- |
| DB Password | 외부 주입인가? |
| JWT Private Key | 발급서버만 접근하는가? |
| Public Key | Rotation 가능한가? |
| API Key | 최소권한·만료가 있는가? |
| 인증서 | 만료 Alert가 있는가? |
| 로그 | Secret Redaction이 있는가? |
| Build | Secret Scan이 있는가? |
| Rotation | 무중단 교체가 가능한가? |
| 폐기 | 퇴직·사고 시 즉시 폐기 가능한가? |

# D.20 로그 저장·보존·접근

## D.20.1 저장 기준

| 로그 | 보존 기준 |
| --- | --- |
| 애플리케이션 | 운영·장애 분석정책 |
| 거래로그 | 업무·운영 통계정책 |
| 감사로그 | 감사·법적 정책 |
| 보안로그 | 보안관제 정책 |
| 배치로그 | 재처리·대사기간 |
| 배포로그 | Artifact 감사기간 |

구체 기간은 개인정보·감사·운영 정책으로 승인한다.

## D.20.2 접근통제

개발

운영

보안

감사

업무

역할별 조회범위와 Download 권한을 분리한다.

## D.20.3 로그 위변조 방지

중앙 수집

Append-only

Checksum·서명

시간 동기화

접근감사

삭제 승인

Backup

## D.20.4 로그 Rotation

파일 최대크기

일자 Rotation

압축

보존개수

Disk 사용률 Alert

중앙 수집 성공 확인

Disk가 가득 차 로그뿐 아니라 Application이 중단되지 않도록 별도 Volume·Alert를 구성한다.

# D.21 성능·용량 검토

## D.21.1 로그량 산정

일 로그량

\=
일 거래건수
× 거래당 평균 로그건수
× 로그 한 건 평균 Byte

예:

일 거래 1억 건

거래당 4줄

평균 500Byte

≈ 200GB/일

따라서 요청·응답 Body와 행별 DEBUG 로그는 운영에서 심각한 저장·I/O 부하를 만들 수 있다.

## D.21.2 성능 기준

정상 거래에서 불필요한 Stack 생성 금지

대형 객체 JSON 직렬화 로그 금지

민감정보 Masking 비용 측정

비동기 Logger Queue 포화 대응

로그 I/O 실패 시 정책

Metric High Cardinality 금지

Audit 저장의 Transaction 영향 측정

## D.21.3 비동기 로그

장점:

업무 Thread I/O 감소

위험:

Queue 포화

Process 종료 시 유실

오류로그 지연

메모리 증가

중요 감사·거래결과를 일반 비동기 로그에만 의존하지 않는다.

# D.22 운영 Metric과 Alert

## D.22.1 Metric

tcf.transaction.count

tcf.business.error.count

tcf.system.error.count

tcf.timeout.count

tcf.unknown.count

tcf.auth.failure.count

tcf.authz.denied.count

tcf.idempotency.hit.count

tcf.audit.failure.count

tcf.transaction.log.failure.count

tcf.external.error.count

tcf.external.timeout.count

업무:

ct.reservation.concurrent.conflict.count

ct.reservation.duplicate.count

ct.reservation.history.failure.count

## D.22.2 Metric Label

권장:

serviceId

resultType

errorType

targetSystem

instanceId

artifactVersion

금지:

guid

traceId

customerNo

reservationId

userId

requestUrl 전체

## D.22.3 Alert

| 조건 | 등급 |
| --- | --- |
| 인증 실패 급증 | Security |
| 특정 사용자 반복 거절 | Security |
| 시스템 오류율 급증 | Major·Critical |
| Timeout 증가 | Major |
| UNKNOWN 1건 | Critical 검토 |
| History 저장 실패 | Critical |
| 감사로그 저장 실패 | Critical |
| 거래로그 유실 | Major |
| Secret 탐지 | Blocker |
| Stack Trace 응답 탐지 | Critical |
| 개인정보 로그 탐지 | Critical |
| DB UPDATE 다건 | Critical |

# D.23 표준 검토 작업표

## D.23.1 거래 단위 검토표

| 분류 | 검토 질문 | 결과 | 증적 |
| --- | --- | --- | --- |
| 식별 | ServiceId·거래코드·GUID가 있는가? | □ |  |
| 입력 | 필수·길이·범위·Enum을 검증하는가? | □ |  |
| 인증 | Token·Session을 검증하는가? | □ |  |
| 인가 | 기능·데이터권한을 확인하는가? | □ |  |
| 상태 | 현재 상태에서 허용되는가? | □ |  |
| 중복 | DB Unique·Idempotency가 있는가? | □ |  |
| 동시성 | Version을 검증하는가? | □ |  |
| Transaction | 부분 Commit을 방지하는가? | □ |  |
| 영향 행 | 정확한 건수를 확인하는가? | □ |  |
| 오류 | 업무·시스템·Timeout을 구분하는가? | □ |  |
| 응답 | 내부정보가 노출되지 않는가? | □ |  |
| 로그 | GUID로 전체 흐름을 찾을 수 있는가? | □ |  |
| 개인정보 | Body·Token·메모가 노출되지 않는가? | □ |  |
| 감사 | 중요행위를 기록하는가? | □ |  |
| 연계 | Timeout·Retry·Mapping이 있는가? | □ |  |
| 운영 | Metric·Alert·Runbook이 있는가? | □ |  |
| 테스트 | 정상·실패·경계 Test가 있는가? | □ |  |

## D.23.2 로그 문장 검토표

| 질문 | 확인 |
| --- | --- |
| Event 이름이 명확한가? | □ |
| ServiceId·GUID가 MDC로 연결되는가? | □ |
| 민감정보가 없는가? | □ |
| 정상 거래에서 불필요한 ERROR가 없는가? | □ |
| 같은 예외가 중복 Stack으로 기록되지 않는가? | □ |
| 로그만으로 결과와 처리구간을 알 수 있는가? | □ |
| 고유값을 Metric Label로 사용하지 않는가? | □ |
| 운영 검색어가 표준화됐는가? | □ |

## D.23.3 보안 검토표

| 질문 | 확인 |
| --- | --- |
| Gateway 우회를 차단하는가? | □ |
| JWT 서명·만료·issuer·audience를 검증하는가? | □ |
| Header 사용자 위조를 막는가? | □ |
| 기능·데이터권한이 분리되는가? | □ |
| 관리자 Endpoint가 분리되는가? | □ |
| SQL Injection 방지가 있는가? | □ |
| XSS 출력 Encoding이 있는가? | □ |
| 파일 Path·MIME을 검증하는가? | □ |
| Secret이 외부화됐는가? | □ |
| 민감정보 로그를 차단하는가? | □ |
| 보안 이벤트가 감사·Alert로 연결되는가? | □ |

# D.24 정상 처리 흐름

## 정상 조회

1\. Gateway가 JWT를 검증한다.
2\. STF가 Header와 기능권한을 확인한다.
3\. 업무가 데이터 Scope를 SQL에 적용한다.
4\. Mapper가 허용된 데이터만 조회한다.
5\. Response에서 개인정보를 Masking한다.
6\. 거래로그에 건수·처리시간을 기록한다.
7\. 개인정보 상세조회이면 감사로그를 기록한다.
8\. Body 원문과 Token은 로그에 남지 않는다.

## 정상 변경

1\. 인증 사용자와 권한을 확인한다.
2\. 입력·상태·Version을 검증한다.
3\. Idempotency 상태를 확인한다.
4\. 상태·Version·권한 조건으로 UPDATE한다.
5\. 영향 행 1건을 확인한다.
6\. Master와 History를 함께 Commit한다.
7\. 거래로그에 결과·영향 행·처리시간을 기록한다.
8\. 감사로그에 수행자·대상·변경 전후를 기록한다.
9\. 민감정보는 Masking한다.

## 정상 외부 연계

1\. 전체 Timeout 잔여시간을 계산한다.
2\. 대상 Client가 표준 계약으로 호출한다.
3\. GUID·TraceId를 전달한다.
4\. 대상 오류를 소비 업무 오류로 Mapping한다.
5\. Retry는 승인된 오류만 수행한다.
6\. 결과와 처리시간을 연계로그에 기록한다.
7\. 상대 전문 원문과 Token은 기록하지 않는다.

# D.25 오류·Timeout·장애 흐름

## 인증 실패

Token 만료

→ Gateway 401

→ 업무 WAR 미호출

→ Security Log

→ 인증 실패 Metric

→ Client Token 갱신·로그인

## 권한 실패

기능권한 없음

→ STF 거절

→ Handler 미호출

→ 403 또는 표준 권한 오류

→ Security·Audit Log

## 업무 오류

READY가 아닌 예약 취소

→ BusinessException

→ Transaction 변경 없음

→ ETF.businessFail

→ 거래로그 BUSINESS\_ERROR

→ 사용자 상태 안내

## 시스템 오류

History INSERT 실패

→ 전체 Transaction Rollback

→ ETF.systemError

→ 원인 Stack 서버 기록

→ 외부 안전 메시지

→ Critical Alert

## Timeout UNKNOWN

DB Commit 성공 가능

→ 응답 Timeout

→ 결과 UNKNOWN

→ 동일 Key 즉시 신규 처리 금지

→ Master·History·Idempotency 대사

→ 결과 확정

# D.26 정상 예시

거래
CT.Reservation.update

인증
JWT 서명·만료·issuer·audience 정상

인가
CT\_RESERVATION\_UPDATE
\+ 지점 Scope 정상

입력
예약 ID·Version·예약일시 검증

데이터
READY
Version 3

SQL
상태·Version·지점 조건 UPDATE

영향 행
1

History
Before Version 3
After Version 4

응답
SUCCESS
Version 4
GUID 포함

거래로그
serviceId
resultType
elapsedMs
affectedRows

감사로그
수행자
대상 ID Masking
변경 필드

금지정보
Token 없음
메모 원문 없음
SQL 원문 없음

# D.27 금지 예시

Request와 Response 전체를 INFO 로그로 출력한다.

Authorization Header를 로그에 기록한다.

Access Token을 Decode해 전체 Claim을 출력한다.

Password를 Debug 로그에 기록한다.

System.out.println으로 거래를 추적한다.

printStackTrace를 사용한다.

업무 오류마다 ERROR Stack Trace를 출력한다.

원인 예외 없이 새 BusinessException을 생성한다.

DB 오류를 “입력값 오류”로 반환한다.

Stack Trace를 JSON 응답에 넣는다.

Table명과 SQL을 사용자 메시지에 포함한다.

화면 버튼 숨김만으로 권한을 통제한다.

Body의 userId·branchId·isAdmin을 신뢰한다.

Gateway 검증만 믿고 업무 데이터권한을 생략한다.

UPDATE 영향 행을 확인하지 않는다.

Version 조건 없이 수정한다.

중복조회만 하고 Unique Constraint를 생략한다.

Master 저장 후 History 오류를 무시한다.

Timeout 후 무조건 신규 요청으로 재실행한다.

비멱등 변경거래를 자동 Retry한다.

외부 시스템의 오류응답을 그대로 사용자에게 전달한다.

Fallback 빈 목록을 정상 데이터로 반환한다.

Metric Label에 GUID·고객번호를 넣는다.

Cache Key에 개인정보 원문을 사용한다.

파일명을 그대로 서버 경로로 사용한다.

관리 Endpoint를 인터넷에 공개한다.

Secret을 application-prod.yml에 Commit한다.

보안로그·감사로그 저장 실패를 조용히 무시한다.

# D.28 자동검증 및 품질 Gate

## D.28.1 정적검사

금지 Pattern:

System.out

System.err

printStackTrace

log.\*(request)

log.\*(response)

Authorization

accessToken

refreshToken

password

catch (Exception) return null

catch (Exception) return success

${...} MyBatis 문자열 치환

BeanUtils.copyProperties(Request, Entity)

예외 사용은 Allow List와 리뷰승인을 요구한다.

## D.28.2 응답 Schema 검사

금지 Field:

stackTrace

exception

sql

query

tableName

serverPath

dbHost

password

token

## D.28.3 변경 SQL Gate

상태조건

Version 조건

권한 Scope

영향 행 검증

DB Unique

History

Rollback Test

## D.28.4 JWT Gate

Algorithm 고정

Signature

issuer

audience

exp

nbf

kid

Claim

Header 정합

DenyList 정책

Token 로그 금지

## D.28.5 로그 Gate

MDC Field

거래 시작·종료

결과 유형

오류코드

처리시간

Masking

고유값 Metric 금지

Stack 중복 금지

## D.28.6 보안 Gate

Secret Scan

Dependency Scan

SAST

DAST

SQL Injection

XSS

권한 우회

파일 업로드

관리 Endpoint

Actuator 노출

CORS

## D.28.7 배포 차단조건

Token·Password 로그 노출

권한 우회 가능

Stack Trace 외부 응답

변경거래 부분 Commit

Version 조건 누락

감사대상 로그 누락

Critical 취약점

Secret 포함 Artifact

Timeout·Retry 정책 없음

UNKNOWN 결과 확인절차 없음

# D.29 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| ERR-001 | 필수값 누락 | Validation 오류 |
| ERR-002 | 숫자형식 오류 | 기본값으로 숨기지 않음 |
| ERR-003 | 최대길이 초과 | 명확한 오류 |
| ERR-004 | 미등록 Enum | 오류 |
| ERR-005 | 빈 목록조건 | 전체조회 차단 |
| ERR-006 | SQL Injection 입력 | Bind 처리 |
| ERR-007 | XSS 입력 | 출력 Encoding |
| AUTH-001 | Token 없음 | 401 |
| AUTH-002 | 서명 오류 | 401 |
| AUTH-003 | 만료 Token | 401 |
| AUTH-004 | issuer 불일치 | 401 |
| AUTH-005 | audience 불일치 | 401 |
| AUTH-006 | Header 사용자 불일치 | 거절 |
| AUTH-007 | 폐기 jti | 거절 |
| AUTH-008 | Token 원문 로그검색 | 없어야 함 |
| AUTHZ-001 | 기능권한 없음 | 403 |
| AUTHZ-002 | 데이터권한 없음 | 거절 |
| AUTHZ-003 | 타 지점 대상 | 존재 은닉 |
| AUTHZ-004 | Body userId 위조 | 무시 |
| AUTHZ-005 | 관리자 Endpoint 일반 사용자 | 거절 |
| BUS-001 | 중복 예약 | 업무 오류 |
| BUS-002 | 상태전이 위반 | 업무 오류 |
| BUS-003 | Version 충돌 | 최신조회 안내 |
| BUS-004 | Not Found | 안전 메시지 |
| SYS-001 | DB 연결 실패 | System Error·Rollback |
| SYS-002 | SQL 오류 | SQL 미노출 |
| SYS-003 | NPE | 안전 응답·Stack 내부 |
| SYS-004 | History 실패 | Master Rollback |
| SYS-005 | Audit 실패 | 승인 정책대로 차단·Alert |
| TMO-001 | Query Timeout | Timeout 분류 |
| TMO-002 | 외부 Read Timeout | 대상 구간 식별 |
| TMO-003 | Pool 대기 Timeout | SQL Timeout과 구분 |
| TMO-004 | 변경 후 응답 Timeout | UNKNOWN |
| TMO-005 | UNKNOWN 재요청 | 결과조회 |
| TMO-006 | 하위 Timeout이 상위보다 큼 | Gate 실패 |
| RET-001 | 조회 Connect 실패 | 제한 Retry |
| RET-002 | Validation Retry | 실행 안 함 |
| RET-003 | 변경 Timeout Retry | 즉시 금지 |
| RET-004 | 동일 Key 재요청 | 기존 결과 |
| DATA-001 | INSERT 1건 | 성공 |
| DATA-002 | UPDATE 0건 | 원인 분류 |
| DATA-003 | UPDATE 2건 | Critical |
| DATA-004 | 동시 등록 | Unique 1건 성공 |
| DATA-005 | 동시 수정 | 한 건 충돌 |
| DATA-006 | History Detail 실패 | 전체 Rollback |
| DATA-007 | Idempotency PROCESSING 잔존 | 대사 |
| EXT-001 | 외부 업무 오류 | 표준 Mapping |
| EXT-002 | 외부 503 | Circuit·Retry |
| EXT-003 | 외부 Timeout | 전체 예산 확인 |
| EXT-004 | Fallback | STALE 표시 |
| LOG-001 | GUID 검색 | 전체 흐름 연결 |
| LOG-002 | TX\_START·TX\_END | 한 쌍 |
| LOG-003 | 업무 오류 | Stack Trace 없음 |
| LOG-004 | 시스템 오류 | Stack Trace 1회 |
| LOG-005 | Request Body 검색 | 원문 없음 |
| LOG-006 | 고객번호 | Masking |
| LOG-007 | Metric GUID Label | 없어야 함 |
| AUD-001 | 개인정보 상세조회 | 감사 생성 |
| AUD-002 | 예약 수정 | Before·After |
| AUD-003 | 권한 변경 | 사유·감사 |
| FILE-001 | 경로조작 파일명 | 차단 |
| FILE-002 | 잘못된 MIME | 차단 |
| FILE-003 | 대용량 업로드 | Streaming |
| FILE-004 | 다운로드 | 권한·감사 |
| BAT-001 | Job 중복 실행 | Lock 차단 |
| BAT-002 | 일부 실패 | 건수·Checkpoint |
| CACHE-001 | 권한별 Cache | 사용자 간 비노출 |
| CACHE-002 | Cache 장애 | Fallback 정책 |
| SEC-001 | Secret Commit | CI 차단 |
| SEC-002 | Actuator 외부접근 | 차단 |
| SEC-003 | Swagger 운영접근 | 차단·관리자 |
| SEC-004 | 취약 Library | Gate 판정 |

# D.30 책임 경계와 RACI

| 활동 | 개발 | 개발리더 | AA | 보안 | DBA | QA | 운영 | DevOps | 업무 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 오류 분류 | R | A | C | C | I | C | C | I | C |
| 오류코드 | R | C | A/C | C | I | C | C | I | A/C |
| 예외 Mapping | R | A | A/C | C | I | C | C | I | I |
| 거래로그 | R/C | C | A | C | I | C | A/C | C | I |
| 감사로그 | R/C | C | C | A/C | I | C | C | I | A/C |
| JWT | C | I | C | A/R | I | C | C | C | I |
| 기능권한 | R/C | C | C | A | I | C | C | I | A/C |
| 데이터권한 | R | C | A/C | A/C | C | C | I | I | A/C |
| 개인정보 | R/C | C | C | A/R | C | C | C | I | C |
| Timeout | R | C | A | C | C | R/C | A/C | C | C |
| Retry·멱등성 | R | C | A | C | C | R/C | C | I | C |
| DB 정합성 | R/C | C | C | I | A/R | C | C | I | C |
| Secret | R/C | C | C | A | C | I | A/C | A/R | I |
| 보안 Test | C | I | C | A/R | I | R | C | C | I |
| Alert·Runbook | C | I | C | C | C | C | A/R | C | C |

# D.31 성능·용량·확장성

## 로그

일 거래량

× 거래당 로그건수

× 평균 로그크기

× 보존기간

× 복제계수

를 기준으로 저장용량을 산정한다.

## 감사

중요 변경마다 Before·After 전체 JSON을 저장하면 History·감사 저장량이 급증한다.

권장:

변경 Field만 저장

민감 Field 별도 암호화

대형 Text 제외·Hash

Archive·Partition

조회 Index 최소화

## 보안 Filter

모든 Body를 복사해 Masking하는 Filter는 대용량 요청에서 Memory와 CPU를 증가시킬 수 있다.

Schema 기반 Field Redaction

최대 Body 크기

Streaming

Sampled Debug

민감정보 DTO Annotation

을 검토한다.

## Alert 폭주

인증 실패 공격 시 건별 Alert를 발송하면 관제체계가 마비될 수 있다.

집계 Window

Threshold

사용자·IP별 Grouping

Suppression

Incident 승격

을 적용한다.

# D.32 운영·모니터링·장애 대응

## 오류 발생 시 점검순서

1\. 사용자 증상과 시각 확보
2\. GUID·TraceId·RequestId 확보
3\. Gateway 인증 여부 확인
4\. TCF TX\_START 존재 확인
5\. ServiceId·결과유형 확인
6\. 업무·SQL·외부 구간 처리시간 비교
7\. Transaction·영향 행 확인
8\. Master·History·Idempotency 대사
9\. 개인정보·보안 영향 확인
10\. 복구·Rollback·거래통제 결정

## 운영 Dashboard

업무 오류율

시스템 오류율

인증 실패

권한 거절

Timeout

UNKNOWN

동시성 충돌

중복 차단

History 실패

감사로그 실패

외부 시스템별 오류

로그 적재 실패

## Runbook

필수:

인증 실패 급증

권한 오류 급증

시스템 오류

Timeout·UNKNOWN

DB Pool 고갈

Slow SQL

외부 시스템 장애

History·Audit 실패

개인정보 로그 노출

Secret 노출

관리 Endpoint 오픈

파일 악성코드

# D.33 완료 체크리스트

## 입력·오류

| 점검 | 완료 |
| --- | --- |
| 필수·형식·길이·범위를 검증한다. | □ |
| 변환 실패를 기본값으로 숨기지 않는다. | □ |
| 업무·시스템·Timeout을 구분한다. | □ |
| 오류코드가 고유하다. | □ |
| 사용자 메시지가 행동 가능하다. | □ |
| 원인 예외를 보존한다. | □ |
| Stack Trace를 응답하지 않는다. | □ |
| GUID를 응답한다. | □ |

## 로그

| 점검 | 완료 |
| --- | --- |
| GUID·TraceId가 전 구간에 있다. | □ |
| TX\_START·TX\_END가 존재한다. | □ |
| 로그 유형별 목적이 분리됐다. | □ |
| 정상 업무 오류를 ERROR로 남발하지 않는다. | □ |
| 시스템 오류 Stack은 한 번 기록한다. | □ |
| Request·Response 원문 전체를 기록하지 않는다. | □ |
| Token·Password·Cookie를 기록하지 않는다. | □ |
| 개인정보를 Masking한다. | □ |
| Metric에 고유값 Label이 없다. | □ |
| 로그 유실 Alert가 있다. | □ |

## 인증·인가

| 점검 | 완료 |
| --- | --- |
| JWT 서명을 검증한다. | □ |
| issuer·audience·exp를 검증한다. | □ |
| Header 사용자와 Claim을 비교한다. | □ |
| 기능권한을 확인한다. | □ |
| 데이터권한을 확인한다. | □ |
| 관리자 권한을 분리한다. | □ |
| Gateway 우회를 차단한다. | □ |
| Token 폐기가 가능하다. | □ |
| 인증·인가 실패를 보안로그에 남긴다. | □ |

## Timeout·연계

| 점검 | 완료 |
| --- | --- |
| 전체 Timeout 예산이 있다. | □ |
| Connect·Read·Query·Pool을 구분한다. | □ |
| 하위 Timeout이 상위보다 짧다. | □ |
| Retry 대상 오류가 정의됐다. | □ |
| 변경 Retry에 멱등성이 있다. | □ |
| UNKNOWN 결과확인 절차가 있다. | □ |
| 외부 오류를 표준 Mapping한다. | □ |
| Circuit Breaker·Fallback을 검토했다. | □ |
| GUID·TraceId를 외부 호출에 전달한다. | □ |

## 데이터 변경

| 점검 | 완료 |
| --- | --- |
| 영향 행을 확인한다. | □ |
| 상태조건이 있다. | □ |
| Version 조건이 있다. | □ |
| DB Unique가 있다. | □ |
| Idempotency가 있다. | □ |
| Master·History가 원자적이다. | □ |
| 부분 반영 대사방법이 있다. | □ |
| Rollback Test가 있다. | □ |
| Timeout 후 Commit을 확인할 수 있다. | □ |

## 보안·감사

| 점검 | 완료 |
| --- | --- |
| SQL Injection을 방지한다. | □ |
| XSS 출력 Encoding이 있다. | □ |
| Mass Assignment를 방지한다. | □ |
| 파일 Path·MIME을 검증한다. | □ |
| Secret이 외부화됐다. | □ |
| 관리 Endpoint가 제한된다. | □ |
| 중요 조회·변경을 감사한다. | □ |
| 감사로그 접근을 통제한다. | □ |
| 보존·파기정책이 있다. | □ |
| Security Test가 있다. | □ |

# D.34 변경·호환성·폐기 관리

## 오류코드 변경

기존 오류코드 의미 변경 금지

신규 오류코드 추가

구 Client 호환

메시지 변경 영향

OM·화면·테스트 갱신

## 로그 Schema 변경

Field 추가

Dashboard·검색 Query

수집 Pipeline

보존공간

Masking

구·신 로그 혼재

를 확인한다.

필수 Field 삭제는 단계적으로 수행한다.

## JWT 정책 변경

Key Rotation

구·신 Public Key 공존

Token 수명

issuer·audience

Clock Skew

Refresh 정책

업무 WAR 설정 동기화

## Timeout 변경

Timeout 상향은 장애해결이 아니라 자원점유 증가일 수 있다.

변경 시:

원인 분석

부하시험

Thread·Pool 영향

하위 Timeout

사용자 경험

Rollback 값

승인·감사

가 필요하다.

## Masking 변경

Masking을 강화해도 기존 로그·DB에 저장된 과거 원문이 자동 삭제되지 않는다.

과거 데이터 탐지

접근차단

삭제·재가공

Backup 처리

사고 신고 여부

를 검토한다.

## 보안 예외

모든 예외에는 다음이 필요하다.

대상

근거

위험

임시 통제

Owner

승인자

만료일

해소계획

만료일 없는 보안 예외는 허용하지 않는다.

## 로그 폐기

보존기간 종료 후에도 다음을 확인한다.

법적 Hold

장애 조사

감사 진행

Backup

개인정보 파기

삭제 증적

# 시사점

## 핵심 아키텍처 판단

첫째, 오류는 Exception Class가 아니라 사용자의 행동·데이터 상태·운영 대응에 따라 분류해야 한다.

둘째, 업무 오류는 예상된 거절이고 시스템 오류는 운영조치가 필요한 장애이므로 로그 Level·Metric·Alert를 분리해야 한다.

셋째, 시스템 오류의 상세 Stack과 SQL은 내부에 보존하되 사용자에게는 안전한 오류코드·메시지·GUID만 제공해야 한다.

넷째, 거래로그·애플리케이션 로그·감사로그·보안로그는 목적과 보존·접근권한이 다르므로 하나의 로그로 통합해서는 안 된다.

다섯째, JWT 검증은 Token Decode가 아니라 서명·만료·발급자·대상·Claim·폐기상태의 검증이며 업무권한을 자동 보장하지 않는다.

여섯째, 화면의 버튼통제와 Gateway 인증은 업무 데이터권한 검증을 대신할 수 없다.

일곱째, Timeout은 단순 실패가 아니라 실제 Commit 결과가 불명확한 UNKNOWN 상태를 만들 수 있으므로 멱등성과 결과조회가 필요하다.

여덟째, 변경거래의 안전성은 상태·Version·Unique·Idempotency·영향 행·History·Rollback이 함께 작동할 때 확보된다.

아홉째, 로그에 더 많은 정보를 남기는 것이 항상 좋은 것은 아니며 필요한 정보만 구조화·마스킹해 기록해야 한다.

열째, 오류·로그·보안 기준은 코드리뷰 문서가 아니라 정적검사·테스트·CI/CD Gate·운영 Alert로 실행돼야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 모든 오류를 업무 오류로 변환 | 장애 은폐 |
| 원인 예외 제거 | Root Cause 분석 불가 |
| Stack Trace 응답 | 내부정보 노출 |
| Request Body 전체 로그 | 개인정보 유출 |
| Token 로그 | 계정 탈취 |
| 업무 오류 ERROR 남발 | 실제 장애 탐지 저하 |
| 기능권한만 확인 | 데이터 무단 접근 |
| Body 사용자정보 신뢰 | 권한 위조 |
| Timeout 무조건 Retry | 중복 처리 |
| Version 누락 | Lost Update |
| 영향 행 미검사 | 변경실패 은폐 |
| History 실패 무시 | 데이터·감사 불일치 |
| 외부 오류 원문 전달 | 내부구조 노출 |
| 빈 Fallback 정상처리 | 잘못된 업무판단 |
| Metric 고유값 | 모니터링 장애 |
| 관리 Endpoint 공개 | 시스템 장악 위험 |
| Secret Source 포함 | 보안사고 |
| 감사로그 유실 | 책임 추적 불가 |

## 우선 보완 과제

1.  오류 유형과 표준 결과코드를 중앙 관리한다.
2.  BusinessException·SystemException·Timeout Mapping을 ETF에서 일관되게 적용한다.
3.  Timeout 결과에 TIMEOUT과 UNKNOWN을 구분하는 기준을 추가한다.
4.  GUID·TraceId·ServiceId MDC 전파를 비동기 처리까지 검증한다.
5.  요청·응답·Token·개인정보 중앙 Redaction Filter를 구현한다.
6.  거래로그·감사로그·보안로그 Schema와 접근권한을 확정한다.
7.  JWT jti 폐기·Refresh Rotation·Header 정합 검증을 완료한다.
8.  기능권한과 데이터권한 검증을 자동 보안테스트에 포함한다.
9.  변경 SQL의 상태·Version·영향 행·History를 품질 Gate로 검사한다.
10.  외부 연계 Timeout Budget·Retry·Circuit Breaker 표준을 적용한다.
11.  거래로그·감사로그 적재 실패 Alert와 Fail 정책을 확정한다.
12.  Secret Scan·SAST·Dependency Scan을 배포 차단 Gate로 적용한다.
13.  관리 Endpoint·Actuator·Swagger를 운영망과 관리자권한으로 제한한다.
14.  개인정보 로그 탐지와 과거 로그 정리절차를 마련한다.
15.  오류·Timeout·보안 Runbook을 실제 장애훈련으로 검증한다.

# 마무리말

오류·로그·보안 검토를 완료하려면 다음 질문에 답할 수 있어야 한다.

입력 오류와 업무 오류를 구분하는가?

업무 오류와 시스템 장애를 구분하는가?

원인 예외를 잃지 않는가?

사용자 응답에 내부정보가 없는가?

GUID로 Gateway부터 SQL까지 추적할 수 있는가?

거래로그·감사로그·보안로그가 분리돼 있는가?

Token·Password·개인정보가 로그에 없는가?

JWT 서명·만료·issuer·audience를 검증하는가?

기능권한과 데이터권한을 모두 확인하는가?

다른 지점 데이터 존재 여부를 노출하지 않는가?

각 계층의 Timeout을 구분하는가?

하위 Timeout이 전체 예산 안에 있는가?

변경거래 Retry에 Idempotency가 있는가?

Timeout 후 실제 Commit 결과를 확인할 수 있는가?

UPDATE 영향 행과 Version을 확인하는가?

Master와 History가 함께 Rollback되는가?

외부 오류를 안전한 표준 오류로 변환하는가?

Fallback 결과가 정상 데이터와 구분되는가?

중요 조회·변경을 감사하는가?

로그·보안 위반을 CI/CD가 자동 차단하는가?

운영자가 Alert와 Runbook으로 대응할 수 있는가?

부록 D의 핵심 흐름은 다음과 같다.

입력 검증

→ 인증·인가

→ 업무규칙

→ 안전한 데이터 변경

→ 오류 분류

→ 표준 응답

→ 거래·감사·보안 로그

→ Metric·Alert

→ 장애대응·대사

→ 자동 품질 Gate

가장 중요한 원칙은 다음과 같다.

사용자에게는 필요한 안내만 제공하고,

운영자에게는 원인을 찾을 수 있는 증거를 제공하며,

감사자에게는 행위와 변경을 증명하고,

공격자에게는 아무런 내부정보도 제공하지 않아야 한다.

오류·로그·보안이
하나의 거래 흐름 안에서 함께 설계될 때

NSIGHT TCF는 단순히 동작하는 시스템을 넘어
추적 가능하고 복구 가능하며
신뢰할 수 있는 운영 시스템이 된다.
