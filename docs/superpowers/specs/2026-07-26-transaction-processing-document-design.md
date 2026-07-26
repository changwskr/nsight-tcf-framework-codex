# NSIGHT TCF 트랜잭션 처리 방식 문서 설계

## 1. 목적

NSIGHT TCF의 온라인 요청 처리와 DB 트랜잭션 처리를 하나의 실행 흐름으로 설명하는 개발 표준 문서를 작성한다. 업무 개발자는 Handler부터 DAO/Mapper까지의 책임과 Commit·Rollback 기준을 이해하고, 프레임워크 개발자는 STF·TCF·ETF, 거래통제와 세 단계 Timeout의 연결 관계를 검증할 수 있어야 한다.

## 2. 산출물과 범위

- 생성 파일: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md`
- 형식: UTF-8 Markdown 단일 문서
- 변경 범위: 새 Markdown 파일만 생성한다.
- 구현 소스, 설정, 샘플 요청, 도움말 색인은 변경하지 않는다.
- 현재 구현을 우선 근거로 사용하고 설명을 `현재 구현`, `개발 표준`, `개선 권고`로 구분한다.
- 기존 `2026-07-26-03-어플리케이션레이어드아키텍처.md`와 `2026-07-26-04-전문구성.md`를 반복하지 않고 트랜잭션 경계와 실패 처리에 집중한다.

## 3. 대상 독자

- Handler·Facade·Service·Rule·DAO/Mapper를 작성하는 업무 개발자
- STF·TCF·ETF, 거래통제, Timeout과 거래로그를 유지하는 프레임워크 개발자
- 장애·Timeout·부분 성공을 분석하는 운영 및 품질 담당자

## 4. 문서 구성 방식

본문은 처리 흐름 중심으로 구성한다. 정책은 별도 요약표로 제공하고, 각 계층의 책임과 금지사항은 책임표로 보완한다.

```text
Client
→ JWT/Gateway 또는 OnlineTransactionController
→ TCF.process()
→ STF.preProcess()
→ OnlineTransactionTimeoutExecutor
→ TransactionDispatcher
→ Handler
→ Facade
→ Service
→ Rule
→ DAO/Mapper
→ ETF.success | businessFail | systemError
→ Client
```

다음 세 경계를 서로 다른 개념으로 설명한다.

1. 온라인 처리 경계: `TCF.process()`의 업무 실행과 응답 조립
2. DB 트랜잭션 경계: Service 또는 유스케이스 Facade의 Commit·Rollback
3. DB 질의 경계: 개별 MyBatis/JDBC 질의의 Query Timeout

## 5. 계층별 책임

| 계층 | 주요 책임 | 트랜잭션 원칙 |
| --- | --- | --- |
| Controller/Gateway | 요청 수신, 경로·클라이언트 정보 보완, TCF 호출 | 업무 DB 트랜잭션을 시작하지 않는다. |
| STF | Header 검증, 인증·권한, 멱등성, 거래통제, Timeout 정책 준비 | 업무 데이터를 변경하지 않는다. |
| TCF | 전 처리·업무 실행·후 처리와 예외 경로 조정 | 성공·업무 오류·시스템 오류를 ETF로 연결한다. |
| Online Timeout Executor | Dispatcher 실행 시간 제한 | 클라이언트 Timeout과 DB Rollback 완료를 동일시하지 않는다. |
| Dispatcher/Handler | `serviceId` 라우팅과 거래 분기 | 직접 SQL이나 복잡한 업무 흐름을 두지 않는다. |
| Facade | DTO 변환과 유스케이스 조정 | 여러 Service를 하나의 원자적 유스케이스로 묶을 때 경계 후보가 된다. |
| Service | 업무 흐름과 상태 변경 | 기본적인 `@Transactional` 적용 위치다. |
| Rule | 검증과 계산 | 가능한 한 무상태·무트랜잭션으로 유지한다. |
| DAO/Mapper | 데이터 접근 | Commit·Rollback을 직접 수행하지 않는다. |
| ETF | 표준 응답, 거래·감사 로그와 메트릭 마감 | 업무 DB 처리 결과를 표준 결과로 변환한다. |

## 6. DB 트랜잭션 설계 원칙

- 조회는 구현과 데이터소스 특성을 확인한 뒤 원칙적으로 `@Transactional(readOnly = true)`를 사용한다.
- 등록·수정·삭제는 Service 또는 유스케이스 Facade에 하나의 명확한 트랜잭션 경계를 둔다.
- Controller와 Handler에는 `@Transactional`을 두지 않는다.
- DAO/Mapper가 Commit·Rollback 또는 트랜잭션 시작을 직접 제어하지 않는다.
- Spring 프록시가 적용되지 않는 동일 클래스 내부 호출, private 메서드와 비동기 실행을 별도 주의사항으로 설명한다.
- 외부 서비스와 로컬 DB는 하나의 로컬 트랜잭션으로 원자화할 수 없으므로 호출 순서, 상태 확인, 보상과 멱등성을 명시한다.
- `REQUIRES_NEW`는 독립 Commit이 업무적으로 필요한 경우에만 사용한다.

## 7. 결과별 처리 경로

### 7.1 정상 성공

Service 작업 완료, DB Commit, Handler 결과 반환, `ETF.success()`, `S0000` 표준 응답과 성공 로그 마감 순으로 설명한다. 실제 Commit 시점은 Spring 트랜잭션 프록시 경계 반환 시점임을 구분한다.

### 7.2 업무 오류

Rule 또는 Service가 `BusinessException`을 발생시키면 TCF가 업무 실패 경로로 분류하고 `ETF.businessFail()`이 표준 실패 응답을 조립한다. 해당 예외가 DB 트랜잭션 경계를 통과할 때 실제 Rollback 대상인지 구현을 확인하여 기록한다.

### 7.3 시스템 오류

예상하지 못한 예외는 시스템 실패로 분류한다. DB 트랜잭션 경계에서는 Rollback되고, ETF는 공통 시스템 오류 응답과 실패 로그를 만든다. 내부 예외, SQL과 Stack Trace를 외부 응답에 노출하지 않는 목표 기준과 현재 구현의 차이가 있으면 명시한다.

### 7.4 Timeout

- Online Timeout: Dispatcher 전체 실행 시간 제한
- Transaction Timeout: Spring DB 트랜잭션 실행 제한과 Rollback
- DB Query Timeout: 개별 MyBatis/JDBC 질의 실행 제한
- 외부 연동 Timeout: 호출 결과 불명 상태를 고려한 상태 확인·재처리·보상

클라이언트가 Timeout 응답을 받은 시점과 서버 작업 중단 또는 DB Rollback 완료 시점이 다를 수 있음을 명확히 설명한다.

## 8. 예외와 Rollback 규칙

- Runtime Exception과 Checked Exception의 Spring 기본 Rollback 차이를 설명한다.
- Checked Exception을 Rollback해야 하면 예외 정책 또는 `rollbackFor`를 명시한다.
- 예외를 잡고 정상 반환하면 Commit될 수 있으므로 재던지기, rollback-only 표시 또는 명시적 실패 계약을 선택한다.
- 업무 오류를 정상 값으로 변환하는 위치와 ETF 실패 분류가 모순되지 않게 한다.
- 비동기·새 스레드에서는 기존 트랜잭션과 ThreadLocal/MDC가 자동 전파되지 않음을 설명한다.

## 9. 멱등성·재시도·부분 성공

- 재시도는 조회 또는 명시적으로 멱등한 거래에만 기본 허용한다.
- 변경 거래는 `idempotencyKey`, 중복 처리 결과 재사용과 상태 확인 절차를 요구한다.
- 외부 호출과 DB 변경의 순서를 업무별로 결정하고 중간 실패 시 보상 동작을 정의한다.
- Timeout 후 동일 요청을 즉시 반복하지 않고 거래 상태 또는 멱등성 결과를 먼저 확인한다.
- 분산 원자성이 필요하면 로컬 트랜잭션 확대가 아니라 Outbox, Saga 또는 보상 설계를 별도 과제로 검토한다.

## 10. 문서 목차

1. 목적과 범위
2. 핵심 용어
3. 전체 트랜잭션 처리 구조
4. 단계별 실행 흐름
5. 계층별 책임과 금지사항
6. DB 트랜잭션 경계
7. 조회·등록·수정·삭제 처리 기준
8. Commit과 Rollback 규칙
9. 예외별 처리 방식
10. 3단계 Timeout
11. 외부 서비스 연동과 트랜잭션
12. 멱등성·재시도·중복 방지
13. 거래통제·권한·세션
14. 로그·감사·메트릭
15. ThreadLocal·MDC 정리
16. 구현 정합성 및 개선 권고
17. 신규 거래 체크리스트
18. 변경 거래 체크리스트
19. 테스트 시나리오
20. 근거 소스와 관련 문서
21. 핵심 원칙 요약

## 11. 테스트 시나리오

최소 다음 16개 사례를 표로 정의한다.

1. 조회 성공
2. 변경 거래 Commit
3. 업무 오류 Rollback
4. 시스템 오류 Rollback
5. Checked Exception 처리
6. 예외를 잡아 삼킨 경우
7. 동일 클래스 내부 호출
8. Online Timeout
9. Transaction Timeout
10. DB Query Timeout
11. 외부 연동 Timeout
12. 멱등성 중복 요청
13. 거래통제 차단
14. 인증·권한 실패
15. 로그·MDC 정리
16. 부분 성공과 보상 처리

각 행은 `ID`, `분류`, `입력·조건`, `기대 트랜잭션 상태`, `기대 응답·증적`을 포함한다.

## 12. 근거 범위

문서는 다음 구현과 관련 문서를 직접 확인해 작성한다.

- `OnlineTransactionController`, `TcfGateway`
- `TCF`, `STF`, `ETF`
- `OnlineTransactionTimeoutExecutor`, `TimeoutPolicyService`, `TimeoutExceptionResolver`
- `TransactionDispatcher`, `TransactionHandler`
- `PolicyDrivenTransactionExecutor`, `PolicyDrivenTransactionAttributeSource`
- `PolicyDrivenQueryTimeoutInterceptor`
- `TransactionControlService`, 인증·권한·멱등성 구성요소
- `TransactionContextHolder`, `TimeoutContextHolder`, MDC 정리 필터
- `zdocs-1/architecture/03-transaction.md`
- `zdocs-1/architecture/05-exception.md`
- `zdocs-1/architecture/08-timeout.md`
- `zdocs-1/architecture/39-header-transaction-control.md`
- `zdocs-1/architecture/41-service-timeout-policy.md`
- 기존 레이어드 아키텍처와 전문 구성 문서

## 13. 검증 기준

- 상위 절 1~21이 모두 존재한다.
- 테스트 시나리오 16개가 모두 존재한다.
- 신규·변경 거래 체크리스트가 재사용 가능한 unchecked 항목으로 제공된다.
- 모든 저장소 상대 링크가 실제 파일로 해석된다.
- 오류 코드, Timeout 명칭, 예외 분류와 트랜잭션 경계 설명이 현재 구현과 일치한다.
- 현재 구현과 개발 표준 또는 개선 권고가 섞이지 않는다.
- 비밀번호, 토큰, 세션 ID, 개인정보, SQL과 Stack Trace를 예시에 포함하지 않는다.
- `git diff --check`가 통과하고 대상 Markdown만 커밋한다.

## 14. 완료 조건

- 승인된 목차와 책임 모델을 반영한 Markdown 문서가 생성된다.
- 전체 처리 흐름과 DB 트랜잭션 경계가 하나의 문서에서 연결된다.
- Commit·Rollback·Timeout·외부 연동·멱등성의 개발 기준을 실행 가능한 체크리스트와 테스트 시나리오로 제공한다.
- 구현과 다른 주장은 현재 차이와 개선 권고로 명시한다.
