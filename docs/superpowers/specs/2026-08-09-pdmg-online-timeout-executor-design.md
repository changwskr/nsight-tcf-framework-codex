# PDMG 온라인 거래 TimeoutExecutor 설계

## 1. 목적

`pdmg-fw`의 온라인 거래 실행 전체에 공통 제한시간을 적용한다. 제한시간을 초과한 요청에는 타임아웃 응답을 반환하고, Worker가 늦게 종료되더라도 해당 업무 트랜잭션이 커밋되지 않도록 롤백한다.

대상 실행 범위는 다음과 같다.

```text
TransactionDispatcher
  → TransactionHandler
    → 업무 Facade
      → 업무 선처리
        → Service
          → DAO / Mapper
      → 업무 후처리
```

시스템 Filter, 시스템 선처리와 시스템 후처리는 타임아웃 실행 범위 및 업무 트랜잭션 밖에 둔다.

## 2. 범위

### 포함

- 모든 온라인 거래에 적용하는 하나의 공통 타임아웃
- 제한된 크기의 Worker Thread Pool과 대기 큐
- Worker Thread에서 시작되는 외부 업무 트랜잭션
- 호출 Thread의 시간 제한 대기와 작업 취소 요청
- Worker의 Commit 직전 Deadline 재검사
- 타임아웃 전용 예외와 HTTP 504 응답
- 타임아웃, 정상 완료, 업무 예외, 과부하 테스트

### 제외

- `serviceId`별 개별 타임아웃
- 운영 화면에서의 동적 설정 변경
- 분산 트랜잭션
- JDBC Statement/Query timeout 강제 설정
- 실행 중인 JDBC 호출의 물리적 즉시 종료 보장
- 배치 및 비동기 거래

## 3. 설계 원칙

1. 타임아웃 코드는 Controller, Handler와 업무 Service에 복제하지 않는다.
2. `TcfFacade`가 온라인 실행 경계에서 `OnlineTimeoutExecutor`를 한 번 호출한다.
3. Worker Thread가 트랜잭션을 소유한다.
4. 호출 Thread의 `Future.cancel(true)`와 Worker의 Deadline 검사를 함께 사용한다.
5. Deadline을 초과한 Worker는 결과를 반환하지 않고 트랜잭션을 롤백한다.
6. Executor 포화는 무제한 대기시키지 않고 즉시 명시적인 오류로 반환한다.
7. Executor가 비활성화되면 기존 동기 경로로 실행한다.

## 4. 목표 아키텍처

```text
HTTP Request Thread
  → DefaultFilter
  → ServicePreventionInterceptor.preHandle
  → OnlineTransactionController
  → TcfFacade
  → OnlineTimeoutExecutor
      ├─ Future.get(commonTimeout)
      └─ Worker Thread
          → TransactionTemplate(rdwTransactionManager)
              → TransactionDispatcher
              → TransactionHandler
              → Facade(@Transactional REQUIRED)
              → BizPrePostAspect.before
              → Service
              → DAO / Mapper
              → BizPrePostAspect.afterReturning
              → Deadline 재검사
              → Commit 또는 Rollback
  → ResponseBodyArgumentResolver
  → ServicePreventionInterceptor.afterCompletion
  → HTTP Response
```

Facade의 기존 `@Transactional`은 기본 전파 속성인 `REQUIRED`로 외부 `TransactionTemplate` 트랜잭션에 참여한다. 온라인 거래당 실제 트랜잭션은 하나만 사용한다.

## 5. 구성요소

### 5.1 `OnlineTimeoutProperties`

공통 설정을 타입 안전하게 바인딩한다.

```yaml
nhnis:
  fw:
    timeout:
      enabled: true
      milliseconds: 5000
      pool-size: 20
      queue-capacity: 100
```

유효성 규칙:

- `milliseconds >= 1`
- `pool-size >= 1`
- `queue-capacity >= 0`
- 잘못된 값이면 애플리케이션 시작을 실패시킨다.

### 5.2 `OnlineTimeoutExecutor`

외부에 제공하는 최소 계약은 다음과 같다.

```java
public interface OnlineTimeoutExecutor {
    <T> T execute(Callable<T> action);
}
```

구현 책임:

- 실행 시작 시 단조 시계인 `System.nanoTime()`으로 Deadline 계산
- Worker Pool에 작업 제출
- Worker 안에서 `TransactionTemplate` 실행
- 호출 Thread에서 공통 제한시간 동안 결과 대기
- 초과 시 `future.cancel(true)` 호출 후 타임아웃 예외 발생
- Worker의 업무 반환 직후 Deadline과 interrupt 상태 재검사
- 초과 또는 interrupt 상태이면 `rollbackOnly` 설정 후 예외 발생
- `ExecutionException`의 원인을 기존 예외 계약에 맞게 전달
- `RejectedExecutionException`을 과부하 예외로 변환

### 5.3 `OnlineTimeoutException`

최소 진단 정보를 가진 RuntimeException으로 정의한다.

- `timeoutMs`
- `elapsedMs`
- `serviceId`
- `guid`

로그나 응답에 요청·응답 전문, Token 또는 개인정보를 포함하지 않는다.

### 5.4 `OnlineOverloadException`

Thread Pool과 큐가 모두 찬 경우 발생시킨다. 타임아웃과 과부하는 운영 원인과 대응이 다르므로 예외를 구분한다.

### 5.5 `OnlineTimeoutConfiguration`

- 제한된 크기의 `ThreadPoolTaskExecutor` 또는 동등한 `ThreadPoolExecutor` 생성
- Thread 이름 접두사: `pdmg-online-`
- Core/Max Pool 크기는 첫 구현에서 같은 값 사용
- 종료 시 신규 작업을 받지 않고 진행 중인 작업의 제한된 종료 대기
- `PlatformTransactionManager`로 `TransactionTemplate` 생성

`pdmg-fw`가 업무 Bean 이름을 하드코딩하지 않도록 `PlatformTransactionManager`를 주입받는다. 여러 TransactionManager가 존재하는 애플리케이션에서는 `pdmg-service`가 온라인용 Manager를 명시적으로 제공하거나 `@Qualifier("rdwTransactionManager")` 연결 설정을 둔다.

## 6. 실행 흐름

### 정상 완료

```text
요청 Thread: Future 제출 및 대기
Worker: TX BEGIN
Worker: Dispatcher → Handler → Facade → Service → DAO
Worker: 업무 후처리 완료
Worker: Deadline 정상 확인
Worker: TX COMMIT
요청 Thread: 결과 반환
```

### 제한시간 초과 후 Worker가 interrupt에 반응

```text
요청 Thread: Future.get timeout
요청 Thread: cancel(true)
요청 Thread: OnlineTimeoutException
Worker: interrupt 또는 업무 예외 감지
Worker: TX ROLLBACK
```

### JDBC가 interrupt를 무시하고 늦게 반환

```text
요청 Thread: 타임아웃 응답 반환
Worker: JDBC 작업이 늦게 반환
Worker: Deadline 초과 확인
Worker: rollbackOnly 지정
Worker: OnlineTimeoutException
Worker: TX ROLLBACK
```

이 설계는 JDBC 작업의 즉시 중단을 보장하지 않는다. 대신 JDBC가 반환된 후 늦은 커밋이 일어나지 않도록 한다. 물리적 SQL 실행시간 제한이 필요하면 후속 설계에서 MyBatis Statement timeout 또는 JDBC Query timeout을 추가한다.

## 7. 트랜잭션 정책

- 온라인 외부 트랜잭션 Manager: `rdwTransactionManager`
- 전파 속성: `PROPAGATION_REQUIRED`
- 타임아웃 롤백: Deadline 초과 또는 interrupt 시 `rollbackOnly`
- 업무 예외 롤백: RuntimeException과 명시적으로 지정된 checked Exception
- 타임아웃 예외는 RuntimeException으로 정의

외부 트랜잭션이 read-write이면 내부 Facade의 `readOnly=true`는 이미 시작된 트랜잭션의 속성을 바꾸지 못한다. 첫 단순 구현에서는 정합성과 공통 실행 경계를 우선한다. 조회 최적화가 필요하면 후속 단계에서 거래 메타데이터에 read-only 속성을 추가한다.

## 8. 컨텍스트 전파와 정리

현재 `ServiceContextHolder`와 Log4j2 `ThreadContext`는 요청 Thread의 ThreadLocal이다. 업무가 Worker Thread로 이동하면 자동 전파되지 않는다.

`OnlineTimeoutExecutor`는 작업 제출 전에 필요한 컨텍스트의 스냅샷을 만들고 Worker 시작 시 설치해야 한다.

- `ServiceContext`의 업무 처리에 필요한 불변 또는 복사 가능한 값
- GUID
- `serviceId`
- 사용자 ID
- 클라이언트 IP

Worker의 `finally`에서는 성공, 업무 예외, 타임아웃과 취소 여부와 관계없이 다음을 정리한다.

```text
ServiceContextHolder.removeInstance()
ThreadContext.clearAll()
```

Servlet request/response 객체를 Worker에 장기간 보관하지 않는다. 필요한 값만 복사한 실행 컨텍스트를 사용한다.

## 9. 오류 응답

### 타임아웃

- HTTP 상태: `504 Gateway Timeout`
- 코드: `FW_TIMEOUT`
- 메시지: `온라인 거래 처리시간을 초과했습니다.`

### Executor 포화

- HTTP 상태: `503 Service Unavailable`
- 코드: `FW_OVERLOADED`
- 메시지: `온라인 거래 처리 요청이 일시적으로 많습니다.`

응답 예시:

```json
{
  "code": "FW_TIMEOUT",
  "message": "온라인 거래 처리시간을 초과했습니다.",
  "serviceId": "mgcoa9000C0",
  "guid": "...",
  "timeoutMs": 5000
}
```

## 10. 로깅

정상 거래에는 시작·종료와 경과시간을 DEBUG 수준으로 남긴다. 타임아웃과 과부하는 WARN으로 남긴다.

```text
[ONLINE-TIMEOUT] guid=... serviceId=mgcoa9000C0 timeoutMs=5000 elapsedMs=5007 cancelRequested=true
[ONLINE-OVERLOAD] guid=... serviceId=mgcoa9000C0 active=20 poolSize=20 queueSize=100
```

동일 타임아웃을 요청 Thread와 Worker Thread에서 중복 ERROR로 기록하지 않는다. 요청 Thread가 대표 WARN 로그를 남기고 Worker는 필요할 때 DEBUG 로그만 남긴다.

## 11. 테스트 전략

### 단위 테스트

- 제한시간 안에 완료되는 Callable의 결과 반환
- 제한시간 초과 시 `OnlineTimeoutException`
- 타임아웃 시 `Future.cancel(true)` 실행
- Worker가 늦게 반환하면 Deadline 검사로 롤백
- 업무 RuntimeException 전달 및 롤백
- Executor 포화 시 `OnlineOverloadException`
- 비활성 설정에서 현재 Thread로 직접 실행
- Worker 완료 후 ThreadLocal 및 MDC 정리

### 통합 테스트

- H2에서 insert 후 의도적으로 지연하여 타임아웃 발생, 데이터 미반영 확인
- 제한시간 내 insert는 Commit 확인
- 업무 후처리 insert까지 동일 트랜잭션 참여 확인
- Handler와 Facade가 거래당 한 번씩 실행되는지 확인
- HTTP 타임아웃 응답이 504와 표준 오류 전문인지 확인
- Executor 포화 응답이 503인지 확인

## 12. 완료 조건

- 공통 설정 하나로 모든 TCF 온라인 거래에 제한시간이 적용된다.
- 정상 거래의 기존 응답 계약이 바뀌지 않는다.
- 타임아웃 요청은 제한시간 근처에서 504를 반환한다.
- Worker가 제한시간 이후 반환하더라도 DB 변경은 커밋되지 않는다.
- Executor 포화 시 무제한 대기하지 않고 503을 반환한다.
- Worker Thread의 ServiceContext와 MDC가 다음 요청으로 누출되지 않는다.
- `pdmg-fw`, `pdmg-service`, `pdmg-ui` 관련 테스트가 통과한다.

## 13. 구현 전제조건

현재 `pdmg-service`에는 기존 `nhnis.mg.*`와 신규 `nhnis.mg.co.a.*` Bean이 중복되어 ApplicationContext가 시작되지 않는 문제가 있다. TimeoutExecutor 통합 테스트 전에 공식 업무 패키지를 하나로 확정하고 중복 Bean 문제를 해결해야 한다. TimeoutExecutor 작업은 이 문제를 우회하거나 Bean overriding으로 숨기지 않는다.
