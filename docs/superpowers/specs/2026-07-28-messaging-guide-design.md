# NSIGHT TCF 메시징 정리 문서 설계

## 1. 목적

`zdocs-1/architecture/04-messaging.md`와 현재 구현을 근거로 NSIGHT TCF 메시지가 채널에서 작성되어 전송, 역직렬화, 보완, 검증, 라우팅, 업무 처리, 응답 조립과 직렬화를 거쳐 반환되는 전체 생명주기를 개발 표준 문서로 정리한다.

## 2. 산출물과 변경 범위

- 생성 파일: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md`
- 형식: UTF-8 Markdown 단일 문서
- 변경 범위: 새 Markdown 파일만 생성한다.
- Java, JavaScript, 설정, 샘플, 도움말 색인과 기존 Markdown은 변경하지 않는다.
- 현재 구현을 우선 근거로 사용하고 차이를 `현재 구현`, `개발 표준`, `개선 권고`로 분리한다.
- 기존 `2026-07-26-04-전문구성.md`의 필드 사전과 `2026-07-26-05-트랜잭션처리방식.md`의 트랜잭션 상세를 반복하지 않고 메시지 이동·변환·진입점에 집중한다.

## 3. 대상 독자

- 채널과 UI에서 요청 메시지를 작성하는 개발자
- 업무 WAR의 Handler·Facade·Service를 구현하는 개발자
- TCF, Gateway, Relay와 서비스 연동을 유지하는 프레임워크 개발자
- 메시지 오류, Timeout과 보안 문제를 분석하는 운영·품질 담당자

## 4. 구성 방식

문서는 메시지 생명주기를 본문 골격으로 사용한다. 진입점 유형 비교표와 계층 책임표를 보조 자료로 포함한다.

```text
채널에서 Header + Body 작성
→ HTTP POST
→ Spring MVC 수신
→ Jackson 역직렬화
→ Controller 또는 Gateway 보완
→ STF 정규화·검증·상관관계 ID 준비
→ serviceId 기반 Handler 라우팅
→ 업무 처리
→ ETF 응답 조립
→ Jackson 직렬화
→ HTTP 응답
→ 채널의 HTTP 상태·resultCode 판정
```

## 5. 메시지 처리 4계층

| 계층 | 책임 | 대표 구현 |
| --- | --- | --- |
| Transport | HTTP Method, URL, Content-Type, Cookie, Authorization, 상태 코드 | Spring MVC, RestClient |
| Serialization | JSON과 Java 객체 사이의 변환 | Jackson, `StandardRequest`, `StandardResponse` |
| Semantic | Header·Body·Result 계약, 정규화, 검증, 라우팅과 응답 조립 | STF, Dispatcher, Handler, ETF |
| Adaptation | 비표준 REST·multipart·UI 요청을 표준 거래로 연결하거나 투명 중계 | `TcfGateway`, UI Relay, Gateway Relay |

각 계층의 입력, 출력, 책임과 금지사항을 독립적으로 설명한다. Transport 오류를 Semantic 업무 오류처럼 설명하거나, 표준 응답과 유사한 JSON을 TCF 전체 적용으로 오인하지 않게 한다.

## 6. 진입점 유형

### 6.1 표준 JSON

`OnlineTransactionController → TCF.process() → STF → TransactionDispatcher → Handler → ETF` 전체 경로를 사용한다. `POST /online`과 `POST /{businessCode}/online`, `application/json; charset=UTF-8`, `StandardRequest` 계약을 설명한다.

### 6.2 프로그램 위임

REST 또는 multipart Controller가 `TcfGateway.invoke()`를 통해 `StandardRequest`와 Header 기본값을 조립하고 동일한 TCF 파이프라인을 재사용하는 경로를 설명한다.

### 6.3 UI·Gateway Relay

`tcf-ui`와 API Gateway가 요청 JSON을 가능한 한 변경하지 않고 대상 업무 WAR의 `/online`으로 전달하는 경로를 설명한다. 실제 STF·Handler·ETF 처리는 대상 WAR에서 일어나며, Relay 자체 오류와 대상 표준 응답을 구분한다.

### 6.4 하이브리드 REST

파일 업·다운로드처럼 multipart·바이너리 특성으로 표준 JSON을 직접 사용할 수 없는 경로를 설명한다. `header/result/body`와 유사한 응답을 사용해도 STF·ETF·멱등성·거래로그가 자동 적용된다고 간주하지 않는다.

각 유형은 `TCF 전체 적용`, `대상 WAR에서 적용`, `TCF 미적용` 중 하나로 표시한다.

## 7. 표준 메시지 생명주기

표준 경로를 다음 단계로 설명한다.

1. 채널이 Header와 Body를 작성한다.
2. UTF-8 JSON으로 `/online` 또는 `/{businessCode}/online`에 POST한다.
3. Spring MVC가 요청을 수신한다.
4. Jackson이 `StandardRequest<Map<String,Object>>`로 역직렬화한다.
5. Controller가 필요한 Header와 네트워크 정보를 보완한다.
6. STF가 Header를 정규화하고 공통 검증과 상관관계 ID 준비를 수행한다.
7. Dispatcher가 `serviceId`로 Handler를 선택한다.
8. Handler가 Body와 Context를 업무 계층으로 전달한다.
9. ETF가 성공·업무 오류·시스템 오류에 맞는 `StandardResponse`를 조립한다.
10. Jackson이 응답을 JSON으로 직렬화한다.
11. 채널은 HTTP 상태와 `result.resultCode`를 경로별 계약에 따라 판정한다.

Header 필드 상세는 기존 전문 구성 문서로 연결하고 이 문서에서는 변경 주체와 시점만 설명한다.

## 8. 직렬화·변환 규칙

- JSON 계약은 camelCase 필드를 사용한다.
- `StandardRequest`는 `header + body`, `StandardResponse`는 `header + result + body` 구조다.
- `body`는 공통 계층에서 `Map<String,Object>`로 취급하고 업무 DTO 변환은 Facade 또는 명시된 경계에서 수행한다.
- Header 정규화, GUID·TraceId 생성, URL businessCode와 clientIp 보완은 각각 실제 구현 주체를 확인해 기록한다.
- Relay는 투명 전달을 원칙으로 하며 Header·Body·Result를 임의로 재작성하지 않는다.
- 하이브리드 REST의 수동 응답 조립은 표준 TCF 팩토리 사용과 구분한다.

## 9. 응답과 오류 처리

오류를 발생 위치별로 분리한다.

| 위치 | 사례 | 설명 기준 |
| --- | --- | --- |
| Transport | Content-Type, 크기 제한, 연결 실패 | HTTP 상태와 Transport 응답 |
| Serialization | 잘못된 JSON, 타입 변환 실패 | Handler 진입 전 MVC 오류 |
| Semantic/STF | Header, 인증·권한, 거래통제 | 표준 실패 응답 |
| Handler/Service | 업무 검증 실패 | 공개 업무 코드·메시지 |
| ETF/System | 예기치 않은 예외 | 공통 시스템 오류, 내부 상세 비노출 목표 |
| Relay/Gateway | 대상 연결 실패, Read Timeout | Relay 오류와 대상 응답 구분 |
| Client | HTTP 200만 확인 | 표준 경로에서는 `resultCode` 확인 |

표준 TCF 경로는 주로 HTTP 200과 `result.resultCode`로 결과를 나타내지만 JWT Filter, Gateway와 비표준 REST는 HTTP 4xx/5xx 또는 다른 JSON 구조를 사용할 수 있음을 명시한다.

## 10. 보안·Timeout·멱등성

- Cookie와 Authorization은 승인된 Relay 경로에서만 전달하며 로그에 값을 남기지 않는다.
- 비밀번호, 토큰, 세션 ID, 개인정보, 전체 Body와 내부 예외 상세를 예시나 외부 응답에 노출하지 않는다.
- `X-Forwarded-For`의 현재 신뢰 방식과 목표 신뢰 프록시 정책을 구분한다.
- `errorDetail`의 현재 동작과 외부 비노출 목표를 구분한다.
- Relay 또는 서비스 연동 Timeout 후 변경 거래를 즉시 반복하지 않고 GUID, 멱등성 키와 거래 상태를 먼저 확인한다.
- 재시도는 조회 또는 명시적으로 멱등한 거래에만 기본 허용한다.

## 11. 서비스 간 메시징

`tcf-eai`의 동기 HTTP/JSON 연동을 표준 전문 재사용 경로로 설명한다. 요청 조립, endpoint 설정, Connect/Read Timeout, 업무 오류·시스템 오류·Timeout 변환, GUID·TraceId 연계를 포함한다. 타 업무 DB 직접 접근과 업무 WAR 간 Gradle 의존을 허용하지 않는다.

Kafka·JMS 등 비동기 브로커는 현재 코어 기능이 아니므로 `확장 고려사항`으로만 설명한다. 향후 도입 시 메시지 키, 스키마 버전, 중복 소비, 순서 보장, 재처리, DLQ와 관측성 계약이 필요함을 기록하되 구현된 기능처럼 서술하지 않는다.

## 12. 최종 목차

1. 목적과 범위
2. 핵심 용어
3. 메시지 처리 4계층
4. 전체 메시지 생명주기
5. 진입점 유형 비교
6. 표준 JSON 메시지 처리
7. `TcfGateway` 프로그램 위임
8. `tcf-ui` Relay
9. API Gateway Relay
10. 하이브리드 REST·multipart
11. 직렬화·역직렬화
12. Header 보완·정규화·검증
13. Handler 라우팅과 Body 전달
14. 응답 메시지 조립
15. 오류·HTTP 상태 처리
16. Cookie·Authorization·보안
17. Timeout·재시도·멱등성
18. 서비스 간 `tcf-eai` 메시징
19. 비동기 메시징 확장 고려사항
20. 구현 정합성 및 개선 권고
21. 신규 메시지 체크리스트
22. 메시지 변경 체크리스트
23. 테스트 시나리오
24. 근거 소스와 관련 문서
25. 핵심 원칙 요약

## 13. 테스트 시나리오

다음 사례를 표로 정의한다.

1. 표준 요청 성공
2. 잘못된 JSON
3. Content-Type 오류
4. 필수 Header 누락
5. GUID·TraceId 생성
6. 미등록 `serviceId`
7. 업무 오류
8. 시스템 오류
9. JWT 또는 Gateway 401
10. UI Relay 성공
11. UI Relay 연결 실패
12. UI Relay Timeout
13. Cookie 전달
14. Authorization 전달
15. `TcfGateway` Header 기본값
16. multipart 비표준 경로
17. 응답 직렬화
18. 멱등성 중복 요청
19. 변경 거래 Timeout 후 재처리 방지
20. `tcf-eai` 성공
21. `tcf-eai` 업무 오류
22. `tcf-eai` Timeout

각 행은 `ID`, `경로`, `입력·조건`, `기대 처리`, `기대 응답·증적`을 포함한다.

## 14. 근거 범위

- `zdocs-1/architecture/04-messaging.md`
- `StandardRequest`, `StandardHeader`, `StandardResponse`, `Result`
- `OnlineTransactionController`, `TcfGateway`
- `STF`, `TCF`, `ETF`, `TransactionDispatcher`
- `TransactionRelayService`, `TcfApiController`, UI 메시지 작성 JavaScript
- API Gateway Route와 Relay 구현
- 파일 업·다운로드 Controller와 응답 지원 코드
- `tcf-eai` 요청 빌더, 클라이언트, 설정과 예외
- 기존 전문 구성·트랜잭션 처리 방식 문서
- 서비스 연동, 보안 운영, 관측성, Gateway와 테스트 아키텍처 문서

## 15. 검증 기준

- 상위 절 1~25가 모두 존재한다.
- 진입점 네 유형의 TCF 적용 위치가 명시된다.
- 테스트 시나리오 22개가 모두 존재한다.
- 신규·변경 메시지 체크리스트가 unchecked 항목으로 제공된다.
- 실제 Java·JavaScript·설정 파일을 근거로 설명한다.
- 모든 저장소 상대 링크가 실제 파일로 연결된다.
- 현재 구현, 개발 표준과 개선 권고가 섞이지 않는다.
- 비밀번호, 토큰, 세션 ID, 개인정보, SQL, Stack Trace를 예시에 포함하지 않는다.
- Core, Web, UI와 EAI의 관련 검증을 실행하거나 실행 불가 사유와 미검증 범위를 기록한다.
- `git diff --check`를 통과하고 대상 Markdown만 커밋한다.

## 16. 완료 조건

- 승인된 25개 절을 포함한 메시징 정리 Markdown이 생성된다.
- 표준 JSON, 프로그램 위임, Relay와 하이브리드 REST의 차이가 명확하다.
- 메시지 생명주기, 변환 주체, 오류 경계, 보안, Timeout과 서비스 연동 규칙이 실제 구현과 연결된다.
- 개발자가 신규 메시지 작성과 변경 검증에 사용할 수 있는 체크리스트와 테스트 시나리오를 제공한다.
