# PDMG 레이어드 아키텍처 문서 업데이트 설계

## 목적

`pdmg-service/docs/03.어플리케이션 레이어드 아키텍처.md`를 현재 `pdmg-service`와 `pdmg-fw` 구현에 맞는 AS-IS 설명서로 갱신한다. 저장소 공통 권장 구조는 현재 구현과 혼동되지 않도록 별도 비교 구획에서만 설명한다.

## 문서 원칙

1. 런타임 호출 흐름과 Java/Gradle 컴파일 의존 방향을 분리한다.
2. TCF 및 Timeout 설정 조합별로 트랜잭션 시작 지점을 구분한다.
3. DTO는 Application과 Persistence 사이의 수직 레이어가 아니라 공유 입출력 계약으로 표현한다.
4. Aspect, Config, Client, Support는 주 호출 스택과 구분해 횡단·조립·연동 요소로 표현한다.
5. 현재 구현과 저장소 권장 패키지 규칙의 차이를 명시하되 TO-BE 변경을 구현된 사실처럼 쓰지 않는다.

## AS-IS 런타임 구조

### TCF ON, Timeout ON

```text
pdmg-ui
  → HTTP
  → DefaultFilter / ServicePreventionInterceptor
  → OnlineTransactionController
  → TcfFacade
  → DefaultOnlineTimeoutExecutor
      → Worker Thread
      → TransactionTemplate BEGIN
      → TransactionDispatcher
      → Handler
      → Facade(@Transactional REQUIRED 참여)
      → BizPrePostAspect Before
      → Service
      → DAO(MyBatis Mapper)
      → Mapper XML / DB
      → BizPrePostAspect AfterReturning
      → COMMIT 또는 ROLLBACK
```

### TCF ON, Timeout OFF

`SyncOnlineTimeoutExecutor`가 현재 요청 스레드에서 Dispatcher를 호출한다. 외부 `TransactionTemplate`은 없고 Facade의 `@Transactional`이 트랜잭션을 시작한다.

### TCF OFF

Legacy Controller가 Service를 직접 호출한다. Handler, Facade와 TimeoutExecutor를 우회하므로 현재 구현에는 명시적인 업무 트랜잭션이 없다. Service Pointcut 기반 업무 선후처리는 실행된다.

## 레이어와 책임

| 구분 | 현재 구현 책임 |
|---|---|
| 외부 채널 | `pdmg-ui`가 HTTP/JSON으로 거래를 중계 |
| FW 진입·제어 | Filter, Interceptor, 공통 Controller, TcfFacade, TimeoutExecutor, Dispatcher |
| Entry | Handler의 거래 분기, Aspect의 횡단 처리 |
| Application | Facade의 DTO 변환·기본 TX 선언, Service의 업무 흐름 |
| Persistence | MyBatis Mapper 인터페이스와 Mapper XML |
| 공유 계약 | DTOin, DTOout, Sub DTO |
| 조립·연동 | Config, Client, Support |

## 의존 관계 표현

런타임에는 FW Dispatcher가 업무 Handler를 호출하지만, 컴파일 시에는 `pdmg-service`가 `pdmg-fw`의 `TransactionHandler` SPI에 의존한다. 문서에서는 두 방향을 별도 도식으로 표시한다.

```text
런타임: pdmg-fw Dispatcher → pdmg-service Handler
컴파일: pdmg-service → pdmg-fw API/SPI
```

## 저장소 권장 구조와의 차이

현재 구현은 `application.controller`, `application.facade`를 사용하고 `application.rule`이 없다. 저장소 공통 규칙의 `entry.web`, `entry.facade`, `application.rule`은 별도 비교표에서 권장 구조로만 표시한다.

## 정확성 주의사항

- Timeout ON에서는 FW가 업무 애플리케이션의 `rdwTransactionManager`를 주입받아 최외곽 트랜잭션을 시작한다.
- Handler는 트랜잭션을 소유하지 않지만 Timeout ON에서는 트랜잭션 안에서 실행된다.
- `@AfterReturning` 업무 후처리는 정상 반환에만 실행된다.
- Timeout 응답 시점과 작업 스레드의 실제 DB 롤백 완료 시점은 다를 수 있다.
- 조회 Facade의 `readOnly=true`는 이미 시작된 외부 트랜잭션 속성을 변경하지 않는다.
- 구 패키지와 `nhnis.mg.co.a` 신규 패키지가 함께 스캔되어 동일 Bean 이름 충돌 가능성이 있다.

## 검증

문서 갱신 후 다음을 확인한다.

1. 문서 내 `Facade에만 TX`, `FW는 TX를 열지 않음` 같은 현재 구현과 반대되는 문장이 없는지 검색한다.
2. 클래스명, 설정 키, 패키지 경로를 `rg`로 실제 소스와 대조한다.
3. Markdown 제목, 표, 코드 블록 구조를 확인한다.
4. 소스 변경이 없음을 `git diff`로 확인한다.

