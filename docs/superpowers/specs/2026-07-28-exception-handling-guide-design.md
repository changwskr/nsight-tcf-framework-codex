# NSIGHT TCF 예외 처리 방식 문서 설계

## 1. 목적

`zdocs-1/architecture/05-exception.md`와 실제 NSIGHT TCF 구현을 근거로, 개발자가 다음 질문에 일관되게 답할 수 있는 예외 처리 기준서를 작성한다.

- 어느 계층에서 어떤 예외를 발생시키는가?
- 프레임워크는 예외를 어떻게 표준 응답으로 변환하는가?
- HTTP 상태와 `resultCode`는 각각 무엇을 의미하는가?
- 클라이언트 응답과 서버 로그에는 어떤 정보를 남기는가?
- 현재 구현과 목표 개발 표준 사이의 차이는 무엇인가?

최종 산출물은 `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md`이다.

## 2. 범위

전체 예외 처리 표준을 다룬다.

- 업무 예외와 시스템 예외
- Header·요청 검증 및 Dispatcher 예외
- 표준 TCF 경로와 ETF 응답 조립
- Spring MVC 및 전역 예외 처리
- 거래 Timeout과 트랜잭션 Rollback의 접점
- `tcf-eai` 연동 예외
- 오류 코드와 표준 오류 응답
- HTTP 상태 정책
- 로깅, 감사, 추적성과 보안
- 현재 구현 정합성 및 개선 권고
- 개발·변경 체크리스트와 테스트 시나리오

트랜잭션의 상세 전파·Rollback 규칙과 메시지 Header 전체 필드 사전은 기존 아키텍처 문서에 연결하고 중복 서술하지 않는다.

## 3. 작성 원칙

### 3.1 근거 우선순위

1. 실제 Java 구현과 테스트
2. `zdocs-1/architecture/05-exception.md`
3. 관련 아키텍처 문서

문서와 구현이 다르면 구현을 최종 근거로 삼되, 차이를 숨기지 않고 정합성 표에 기록한다.

### 3.2 상태 구분

각 정책은 필요한 경우 다음 표기로 구분한다.

- **현재 구현**: 저장소에서 확인한 실제 동작
- **개발 표준**: 신규 코드와 변경 코드가 따라야 할 규칙
- **개선 권고**: 현재 구현을 목표 표준에 맞추기 위한 후속 과제

### 3.3 구성 방식

예외 유형을 단순 나열하지 않고, 예외가 발생한 위치에서 표준 응답과 로그로 종료되는 생명주기를 중심으로 구성한다. 유형별 분류와 계층별 책임은 생명주기를 보완하는 참조 기준으로 제공한다.

## 4. 예외 분류 모델

| 분류 | 대표 예외·상황 | 의미 | 표준 처리 |
| --- | --- | --- | --- |
| 업무 예외 | `BusinessException` | 정상적인 업무 거절과 검증 실패 | `ETF.businessFail` |
| 시스템 예외 | `SystemException`, 일반 `Exception` | 결함, 장애, 인프라 실패 | `ETF.systemError` |
| 전송·웹 예외 | JSON 오류, Method 오류, MVC 검증 실패 | TCF 진입 전 요청 처리 실패 | `GlobalStandardExceptionHandler` 또는 MVC 처리 |
| Timeout 예외 | 거래·연동 Timeout | 처리 완료 여부가 불확실한 시간 초과 | Resolver 또는 연동 예외 계층으로 변환 |
| 연동 예외 | `IntegrationBusinessException` 등 | 대상 업무 실패, 통신·응답·Timeout 실패 | 의미를 보존해 호출 계층으로 전달 |

## 5. 표준 처리 생명주기

```text
요청
  → STF / Dispatcher / Handler / 업무 계층
  → BusinessException
      → TCF catch
      → ETF.businessFail
      → resultCode=E0001 + 업무 errorCode
  → 일반 Exception
      → Timeout 여부 판별
      → Timeout이면 업무 실패 경로
      → 그 외 ETF.systemError
      → resultCode=E0001 + 공통 시스템 errorCode
```

TCF 진입 전에 발생한 역직렬화·MVC 검증·지원하지 않는 HTTP Method 예외는 전역 웹 예외 처리 경로로 분리한다. 응답 외형이 유사하더라도 ETF 경로와 거래 로그, 감사, 멱등성 종료 처리 범위가 다를 수 있음을 명시한다.

## 6. 계층별 책임

- **Handler**: 거래 분기와 Facade 호출만 수행하며 예외를 임의 응답 DTO로 변환하지 않는다.
- **Facade**: DTO 변환, 유스케이스 조정과 트랜잭션 경계를 관리한다.
- **Service·Rule**: 업무 판단 실패를 `BusinessException`으로 발생시킨다.
- **DAO·Mapper**: 데이터 접근 예외를 숨기거나 업무 예외로 남발하지 않고 상위로 전달한다.
- **Client·tcf-eai**: 대상 업무 실패와 통신·응답·Timeout 실패를 구분한다.
- **TCF·ETF·Web Advice**: 예외를 공개 가능한 최종 응답으로 변환한다.

## 7. 응답 계약

표준 업무·시스템 실패는 다음 구조를 기준으로 설명한다.

```json
{
  "header": {
    "guid": "...",
    "traceId": "...",
    "serviceId": "..."
  },
  "result": {
    "resultCode": "E0001",
    "resultMessage": "FAIL",
    "errorCode": "업무 또는 공통 오류 코드",
    "errorMessage": "클라이언트 공개 가능 메시지"
  },
  "body": null
}
```

- 실제 성공 여부는 표준 경로에서 `result.resultCode`로 판단한다.
- HTTP 상태는 전송·인증·라우팅·MVC 처리 결과와 표준 거래 계약의 차이를 설명한다.
- 오류 코드 형식은 저장소의 `ErrorCode`와 업무 코드 사례를 검증해 문서에 확정한다.
- 업무 오류와 시스템 오류 모두 내부 구현 상세를 공개 메시지에 포함하지 않는다.

## 8. 로깅·추적·보안

- 로그와 거래 증적은 `guid`, `traceId`, `serviceId`, `errorCode`, 처리시간으로 연결한다.
- 서버 로그에는 원인 예외와 Stack Trace를 진단 가능하게 보존한다.
- 클라이언트에는 내부 예외명, SQL, Stack Trace, 파일 경로와 내부 URL을 노출하지 않는다.
- 비밀번호, Access·Refresh Token, Private Key, 세션 ID와 개인정보는 응답이나 로그에 남기지 않는다.
- Cookie와 Authorization 값은 기록하지 않고 전달 범위만 검증한다.
- 공개 메시지와 내부 진단 메시지를 분리한다.

## 9. 정합성 검토 대상

다음 항목을 구현 근거와 함께 `현재 구현 / 개발 표준 / 개선 권고` 표로 정리한다.

1. `SystemException`의 실제 사용 범위
2. ETF와 `GlobalStandardExceptionHandler`의 응답 차이
3. 일반 시스템 예외의 내부 메시지 노출 가능성
4. TCF 진입 전 예외의 Header·추적 ID 보존 여부
5. Timeout을 업무 실패로 변환하는 현재 정책
6. 하이브리드 REST의 비표준 오류 응답
7. `tcf-eai` 대상 오류 코드 보존과 호출자 변환 책임
8. HTTP 상태와 `resultCode` 판정 기준의 경로별 차이

## 10. 최종 문서 목차

1. 목적과 적용 범위
2. 핵심 원칙과 용어
3. 예외 처리 전체 생명주기
4. 예외 분류 체계
5. 핵심 예외 클래스의 역할
6. 계층별 예외 발생·처리 책임
7. STF·Dispatcher·Handler 예외 흐름
8. TCF Catch 및 ETF 응답 조립
9. Web MVC·Global Exception Handler
10. 검증·Timeout·트랜잭션 예외
11. `tcf-eai` 연동 예외
12. 오류 코드와 표준 응답 계약
13. HTTP 상태 정책
14. 로깅·감사·추적성
15. 보안과 내부 정보 비노출
16. 현재 구현 정합성 및 개선 권고
17. 신규 예외 체크리스트
18. 예외 변경 체크리스트
19. 테스트 시나리오
20. 근거 소스와 관련 문서
21. 핵심 원칙 요약

## 11. 테스트 시나리오 설계

테스트 시나리오는 `EXC01`부터 누락 없이 연속 번호로 작성하며 최소한 다음 범위를 포함한다.

- 정상 성공과 업무 예외
- 예상하지 못한 시스템 예외
- Header 필수값 검증
- 미등록 `serviceId`
- MVC Bean Validation 실패
- 잘못된 JSON과 지원하지 않는 HTTP Method
- 거래 Timeout과 연동 Connect·Read Timeout
- `tcf-eai` 대상 업무 실패와 잘못된 응답
- Runtime 예외 발생 시 트랜잭션 Rollback
- 오류 응답과 로그의 민감정보 비노출
- `guid`·`traceId`를 통한 응답과 로그 상관관계

각 시나리오는 경로, 입력·조건, 기대 변환, 기대 응답과 로그·DB 증적을 포함한다.

## 12. 완료 조건

- 최종 문서가 승인된 21개 장을 모두 포함한다.
- 기준 문서의 핵심 내용을 보존하면서 실제 구현 차이를 반영한다.
- 모든 구현 주장에 저장소 상대 링크 또는 명확한 클래스·메서드 근거가 있다.
- `현재 구현 / 개발 표준 / 개선 권고`가 혼동 없이 구분된다.
- 오류 응답, HTTP 상태, 로깅과 보안 정책이 서로 모순되지 않는다.
- 체크리스트와 `EXC` 테스트 시나리오가 개발·리뷰에 바로 사용 가능하다.
- 로컬 Markdown 링크가 모두 유효하고 UTF-8 및 `git diff --check` 검사를 통과한다.
- 관련 모듈의 테스트 또는 문서 변경에 비례한 검증 결과를 기록한다.
