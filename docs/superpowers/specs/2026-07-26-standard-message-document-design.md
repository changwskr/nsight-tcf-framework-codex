# NSIGHT TCF 전문 구성 문서 설계

## 1. 목적

NSIGHT TCF Framework의 표준 요청·응답 전문 구조를 실제 구현 기준으로 설명하는 개발 기준서를 작성한다. 문서·코드·샘플의 차이는 숨기지 않고 현재 구현, 문서 설명과 개선 권고로 구분한다.

## 2. 대상 산출물

```text
ztcf-다이어리/2026-07-26-아키텍처-이것저것/
└─ 2026-07-26-04-전문구성.md
```

단일 UTF-8 Markdown 파일로 작성하며 기존 명명규칙 및 애플리케이션 레이어드 아키텍처 문서 다음 번호를 사용한다.

## 3. 독자와 사용 목적

주 독자는 다음과 같다.

- 표준 전문을 설계하는 업무 개발자
- Handler·DTO·UI 샘플을 구현하는 개발자
- TCF 공통 파이프라인을 운영하는 프레임워크 개발자
- 전문 계약과 오류 처리를 검증하는 테스트 담당자
- CRUD 생성 프롬프트를 관리하는 AI 에이전트 운영자

문서는 아키텍처 설명뿐 아니라 신규·변경 작업에서 직접 사용할 필드 사전, 체크리스트와 테스트 시나리오를 제공한다.

## 4. 범위

### 4.1 포함

- `StandardRequest<T>`, `StandardHeader`, `StandardResponse<T>`, `Result`
- 요청·응답 JSON 구조와 예시
- Header·Body·Result 필드 계약
- Controller, STF, Dispatcher, Handler, Service와 ETF 처리 흐름
- 필드별 생성·검증·정규화·사용 주체
- 성공, 공통 검증 오류, 업무 오류와 시스템 오류
- 보안·PII·로그 원칙
- 구현·문서·샘플 정합성 이슈
- 신규·변경 체크리스트
- 정상·실패·호환성 테스트 시나리오
- 실제 근거 소스 링크

### 4.2 제외

- 표준 메시지 클래스 변경
- 신규 Header 필드 구현
- 기존 샘플 JSON 일괄 수정
- 비표준 multipart·Relay REST의 상세 설계
- 명명규칙과 애플리케이션 계층 문서의 재작성

## 5. 문서 구성

```text
1. 목적·범위·용어
2. 전문 전체 구조
3. 전송 계약
4. 요청 전문
5. 요청 Header 데이터 사전
6. 요청 Body 계약
7. 응답 전문
8. Result 데이터 사전
9. 전문 처리 흐름과 필드 생명주기
10. 검증과 정규화
11. 성공·업무오류·시스템오류
12. 보안·개인정보·로그
13. 구현·문서·샘플 정합성 이슈
14. 신규 전문 체크리스트
15. 전문 변경 체크리스트
16. 테스트 시나리오
17. 근거 소스와 관련 문서
```

## 6. 전문 기준 구조

실제 구현 기준 구조는 다음과 같다.

```text
요청
StandardRequest<T>
├─ header: StandardHeader
└─ body: T

응답
StandardResponse<T>
├─ header: StandardHeader
├─ result: Result
└─ body: T
```

요청과 응답의 `header`는 같은 거래의 식별·추적 정보를 유지한다. 요청 `body`와 응답 `body`는 업무별 계약이며 공통 Header 필드를 반복하지 않는 것을 원칙으로 한다.

## 7. 전송 계약

문서는 다음 기본 계약을 명시한다.

| 항목 | 기준 |
| --- | --- |
| Protocol | HTTP/1.1 이상 |
| Method | `POST` |
| Content-Type | `application/json; charset=UTF-8` |
| Endpoint | `/online` 또는 `/{businessCode}/online` |
| 성공·실패 판정 | 기본적으로 HTTP 200과 `result.resultCode` 조합 |

HTTP 상태만으로 업무 성공을 판정하지 않으며 표준 JSON 거래와 multipart·Relay·비표준 REST를 구분한다.

## 8. Header 데이터 사전

Header 필드는 다음 범주로 분류한다.

- 라우팅: `businessCode`, `serviceId`
- 거래 식별: `transactionCode`, `processingType`
- 추적: `guid`, `traceId`
- 호출 주체: `systemId`, `channelId`, `userId`
- 조직: `branchId`, `centerId`
- 시각·네트워크: `requestTime`, `clientIp`
- 중복 방지: `idempotencyKey`

각 필드는 다음 정보를 표로 제공한다.

```text
필드
Java 타입
필수 여부
생성 주체
검증 주체
정규화 규칙
로그·통제 사용처
예시
보안 주의
```

필수 여부는 문서 추정이 아니라 `StandardHeader`, Validator와 `TcfStandardMessageCatalog`를 비교해 기록한다. 서로 다르면 정합성 이슈로 분리한다.

## 9. Body 계약

- Body는 업무·ServiceId별 계약이다.
- 공통 Header와 같은 의미의 필드를 중복 정의하지 않는다.
- Handler가 Body를 Request DTO로 변환한다.
- Service 결과는 Response DTO로 변환한 뒤 응답 Body가 된다.
- Request·Response·Criteria·Row의 책임을 구분한다.
- Body 원문 전체 로깅을 기본값으로 사용하지 않는다.

## 10. Result 계약

Result 필드는 다음 범주로 설명한다.

- 공통 성공·실패: `resultCode`, `resultMessage`
- 업무·시스템 오류: `errorCode`, `errorMessage`
- 내부 진단: `errorDetail`, `errorSystemId`, `errorDateTime`

`errorDetail`이 구현에 존재하더라도 외부 응답에 예외 클래스, Stack Trace, SQL 또는 내부 경로를 노출하지 않는 것을 원칙으로 한다.

문서에는 다음 JSON 예시를 각각 포함한다.

1. 정상 요청
2. 정상 응답
3. Header 검증 실패
4. 업무 오류
5. 시스템 오류

## 11. 처리 흐름

```text
Client
→ OnlineTransactionController
→ TCF.process
→ STF.preProcess
→ TransactionDispatcher
→ Handler → Facade → Service
→ ETF.success / businessFail / systemError
→ Client
```

| 단계 | 전문 처리 책임 |
| --- | --- |
| Client | Header·Body 작성과 JSON 전송 |
| Controller | URL businessCode와 clientIp 보완 |
| STF | Header 검증·정규화, GUID/TraceId, 세션·권한·멱등성·거래통제 |
| Dispatcher | `serviceId`로 Handler 선택 |
| Handler | Body를 Request DTO로 변환하고 Facade 호출 |
| Service | 업무 처리 결과 생성 |
| ETF | Result·응답 전문 조립과 거래로그·감사·메트릭 정리 |
| Client | `result.resultCode`로 성공·실패 판정 |

## 12. 필드 생명주기

문서는 최소 다음 필드의 생성·변화·소비 지점을 설명한다.

```text
businessCode
Client 또는 URL → Controller 보완 → STF 대문자 정규화 → 응답 유지

guid / traceId
Client 선택 입력 → STF 미입력 시 생성 → 로그·응답 유지

serviceId
Client 입력 → STF 검증 → Dispatcher → Timeout·통제·로그

body
Client → Handler DTO → 업무 처리 → Response DTO → ETF body
```

## 13. 검증 책임

| 계층 | 검증 책임 |
| --- | --- |
| Controller | HTTP·JSON 역직렬화와 URL 기반 보완 |
| STF | 공통 Header 필수값, 정규화, 세션·권한·멱등성·거래통제 |
| Handler·DTO | Body 구조와 형식 |
| Rule·Service | 업무 규칙과 상태 |

공통 Header 검증을 업무 모듈에 복제하지 않고 업무 검증을 STF에 넣지 않는다.

## 14. 오류 모델

| 유형 | 예 | 응답 원칙 |
| --- | --- | --- |
| 공통 검증 오류 | 필수 Header 누락 | 표준 오류코드와 안전한 사용자 메시지 |
| 업무 오류 | 중복, 상태 위반, 결과 없음 | 업무 오류코드와 안전한 메시지 |
| 시스템 오류 | DB, 외부 연동, 예상하지 못한 예외 | 일반화된 메시지, 내부 상세 비노출 |

오류 처리 경로는 ETF의 성공, 업무 실패와 시스템 실패 경로에 연결해 설명한다.

## 15. 보안과 로그

- Token, 비밀번호, 세션 ID와 개인정보를 샘플·로그에 기록하지 않는다.
- `errorDetail`에 내부 예외와 SQL을 외부 노출하지 않는다.
- `clientIp`는 신뢰 가능한 Proxy 정책과 함께 해석한다.
- `idempotencyKey`에 개인정보 또는 업무 원문을 사용하지 않는다.
- GUID, TraceId와 ServiceId를 기본 추적 키로 사용한다.
- Body 전체를 무조건 로그에 남기지 않는다.
- PII 필드는 마스킹 또는 로그 제외 정책을 따른다.

## 16. 정합성 이슈

다음 항목은 `현재 구현`, `문서 설명`, `개선 권고` 열로 구분해 기록한다.

1. 샘플의 `transactionIntime`, `transactionOuttime`, `systemDate`, `bizDate`는 현재 `StandardHeader`에 없다.
2. 문서와 `TcfStandardMessageCatalog`의 필수 여부가 다를 수 있다.
3. 기존 예시는 `inquiry`, `save`를 사용하지만 신규 명명 표준은 `selectList`, `selectDetail`, `create`를 사용한다.
4. 일부 응답 Body가 Header의 `businessCode`, `serviceId`, `guid`를 반복한다.
5. 표준 JSON 거래의 HTTP 200 + 결과코드 계약과 비표준 REST의 HTTP Status 사용이 다르다.

정합성 이슈는 자동으로 사실을 통합하지 않고 근거를 각각 제시한다.

## 17. 신규 전문 체크리스트

- 업무코드와 ServiceId 확정
- `processingType`과 transactionCode 정합
- Header 필수·선택값 확정
- Request·Response Body 필드 정의
- DTO와 JSON 필드 매핑
- Header와 Body 중복 제거
- 입력 검증과 업무 Rule 분리
- 성공·업무·시스템 오류코드 정의
- PII·인증정보·로그 마스킹 검토
- Service Catalog·거래통제·Timeout 정책 연결
- 샘플 JSON과 도움말 색인 반영

## 18. 변경 체크리스트

- 기존 호출자 하위 호환성
- 필드 추가·삭제·타입 변경 영향
- 필수 여부 변경 영향
- ServiceId·transactionCode 사용처
- Header Validator와 Catalog 정합성
- Handler DTO·UI·샘플·테스트 동시 변경
- Rollback과 구·신 전문 공존 여부

## 19. 테스트 시나리오

1. 정상 요청과 성공 응답
2. Header 필수값 누락
3. businessCode와 ServiceId Prefix 불일치
4. 알 수 없는 ServiceId
5. 잘못된 processingType
6. GUID·TraceId 자동 생성
7. 권한·세션 실패
8. 멱등 키 중복
9. 거래통제 차단
10. Online·TX·DB Query Timeout
11. Body Validation 실패
12. 업무 오류
13. DB·외부 연동 시스템 오류
14. 오류 응답 내부 상세 비노출
15. 미등록 Header 필드 처리
16. 기존 호출자 호환성

## 20. 근거 자료

실제 작성 시 다음 파일을 직접 링크하고 구현을 확인한다.

- `tcf-core/.../support/message/StandardRequest.java`
- `tcf-core/.../support/message/StandardHeader.java`
- `tcf-core/.../support/message/StandardResponse.java`
- `tcf-core/.../support/message/Result.java`
- `StandardHeaderValidator` 실제 경로
- `OnlineTransactionController` 실제 경로
- STF·ETF 실제 구현
- `TcfStandardMessageCatalog.java`
- 대표 `tcf-ui/.../sample-requests/*.json`
- `zdocs-1/architecture/02-junmun.md`
- `zdocs-1/architecture/04-messaging.md`

## 21. 수용 기준

1. 요청·응답 전문 구조가 실제 Java 클래스와 일치한다.
2. Header와 Result의 모든 실제 필드가 데이터 사전에 포함된다.
3. 필드의 생성·검증·정규화·소비 주체를 추적할 수 있다.
4. 표준 거래와 비표준 REST의 계약을 혼동하지 않는다.
5. 정상·검증·업무·시스템 오류 JSON 예시가 있다.
6. 민감정보와 내부 오류 상세 비노출 원칙이 명시된다.
7. 구현·문서·샘플 불일치가 근거와 함께 구분된다.
8. 신규·변경 체크리스트와 테스트 시나리오를 바로 사용할 수 있다.
9. 기존 디렉터리 문서의 스타일과 번호 체계를 따른다.
10. 확인하지 않은 내용을 구현 사실처럼 기록하지 않는다.
