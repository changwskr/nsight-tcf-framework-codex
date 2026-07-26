NSIGHT TCF 개발방법론

업무 도메인 간 상호 연동 관계 설계기준

1. 도입 전 안내말

NSIGHT TCF에서 업무 도메인 간 연동은 한 Service 클래스가 다른 Service 클래스를 호출하는 단순한 프로그램 관계가 아니다.

도메인 간 연동은 다음 경계를 넘는 아키텍처 행위다.

```
업무 책임 경계
→ 프로그램 패키지 경계
→ 데이터 소유권 경계
→ 트랜잭션 경계
→ 장애 전파 경계
→ 보안·권한 경계
→ 운영 책임 경계
```

따라서 도메인 간 연동 방식이 명확하지 않으면 다음과 같은 구조가 만들어진다.

```
CustomerService
  → ProductService
      → CampaignService
          → CustomerDao
              → ContactMapper
```

이 구조에서는 호출 순서만 보아서는 다음 사항을 판단하기 어렵다.

- 어느 도메인이 유스케이스를 소유하는가
- 어느 도메인이 데이터를 변경할 권한을 갖는가
- 트랜잭션은 어디에서 시작하고 종료되는가
- 하위 도메인 장애 시 전체 거래를 실패시킬 것인가
- Timeout과 재시도는 누가 관리하는가
- 순환 호출이 발생하지 않는가
- 어느 조직이 운영 장애를 책임지는가
NSIGHT TCF의 업무 프로그램은 다음 계층구조를 기본으로 한다.

```
Handler
  ↓
Facade
  ↓
Service
  ├─ Rule
  ├─ DAO
  │   ↓
  │ Mapper
  └─ Client
```

Handler는 ServiceId와 업무 유스케이스를 연결하고, Facade는 유스케이스 조립과 트랜잭션 경계를 담당하며, Service는 업무 처리, Rule은 부작용 없는 업무 규칙, DAO와 Mapper는 데이터 접근을 담당한다.

도메인 설계기준에서는 다른 도메인의 DAO·Mapper·테이블을 직접 사용하는 것을 금지하고, 동일 WAR에서는 공개된 애플리케이션 계약, 다른 WAR에서는 TCF/EAI ServiceId, 비동기 처리는 이벤트 계약을 사용하도록 정의한다.

본 기준의 핵심 판단은 다음과 같다.

```
도메인 내부 구현은 외부에 공개하지 않는다.

도메인 간 연동은
구현 클래스가 아니라 공개 계약을 통해 수행한다.

다른 WAR와의 연동은
Java 클래스 호출이 아니라 ServiceId 기반 표준 거래로 수행한다.

데이터 변경은
반드시 데이터 소유 도메인이 수행한다.
```

2. 문서 개요

2.1 목적

본 설계기준의 목적은 NSIGHT TCF 업무 애플리케이션에서 도메인 간 호출·데이터 공유·상태 변경·이벤트 전달에 대한 표준을 정의하는 것이다.

| 목적 | 설명 |
| --- | --- |
| 도메인 경계 보호 | 다른 도메인의 내부 구현에 직접 의존하지 않도록 통제 |
| 계층 책임 유지 | Handler·Facade·Service·Rule·DAO·Mapper 책임 보존 |
| 데이터 소유권 보호 | 데이터 변경은 소유 도메인을 통해서만 수행 |
| 연동 방식 표준화 | 동일 도메인·동일 WAR·다른 WAR·비동기 호출 구분 |
| 트랜잭션 명확화 | 로컬 트랜잭션과 분산 업무 흐름의 경계 정의 |
| 장애 격리 | 하위 도메인 장애가 전체 시스템으로 확산되는 것을 방지 |
| 운영 추적성 | ServiceId·GUID·TraceId·호출 대상·처리시간 연결 |
| 계약 안정성 | 요청·응답·이벤트 스키마의 버전과 호환성 관리 |
| 자동검증 | 직접 DAO 접근, WAR 간 Import, 순환 의존을 빌드 단계에서 차단 |
| 변경 영향 관리 | 제공 도메인 변경 시 소비 도메인 영향을 식별 |

2.2 적용범위

본 기준은 다음 영역에 적용한다.

| 적용 영역 | 적용 내용 |
| --- | --- |
| 동일 도메인 내부 | Handler에서 Mapper까지 내부 계층 호출 |
| 동일 WAR 내 도메인 간 | Customer, Product, Contact 등 도메인 간 Java 호출 |
| 다른 업무 WAR 간 | SV, IC, PC, CM, CT 등 업무 애플리케이션 간 호출 |
| 플랫폼 연동 | tcf-eai, tcf-cache, tcf-om, tcf-batch 연계 |
| 동기 연동 | 조회·검증·즉시 처리 결과가 필요한 호출 |
| 비동기 연동 | 이벤트·메시지·후속 처리·대량 처리 |
| 데이터 연동 | Read Model, View, Cache, 파일, 메시지 |
| 트랜잭션 | 로컬 트랜잭션, 보상 처리, 최종 일관성 |
| 운영관리 | OM Service Catalog, Timeout, 거래통제, 로그 |
| 품질관리 | 계약 테스트, ArchUnit, CI/CD 품질 Gate |

다음은 별도 설계기준에서 상세 정의한다.

| 제외 영역 | 별도 기준 |
| --- | --- |
| 외부기관 전문 세부 규격 | 외부 인터페이스 설계기준 |
| JWT 발급·키 관리 | 토큰 인증 설계기준 |
| DB 컬럼·인덱스 설계 | DB 설계 및 명명규칙 |
| 메시지 제품 물리 구성 | 메시징 플랫폼 설계기준 |
| 네트워크 라우팅 | 인프라·Gateway 설계기준 |

2.3 대상 독자

| 대상 | 활용 목적 |
| --- | --- |
| 애플리케이션 아키텍트 | 도메인 간 관계와 호출 방향 승인 |
| 업무 분석가 | 도메인별 업무 책임과 연계 요구 정의 |
| 업무 개발자 | 공개 계약·Client·Facade 구현 |
| TCF 프레임워크팀 | ServiceId·EAI·Timeout·추적 기능 제공 |
| 데이터 아키텍트·DBA | 데이터 소유권과 통합 조회 검토 |
| 보안 담당자 | 도메인 간 인증정보·개인정보 전달 검토 |
| 운영 담당자 | 호출 장애와 영향 도메인 추적 |
| 테스트 담당자 | 계약·Timeout·장애·보상 시나리오 검증 |
| DevOps·품질팀 | 의존성 자동검증과 배포 Gate 운영 |
| PMO | 도메인별 책임·진척·위험 관리 |

2.4 선행조건

| 선행조건 | 기준 |
| --- | --- |
| 업무 도메인 | 각 업무코드 하위 도메인이 정의되어 있어야 함 |
| 도메인 소유자 | 업무 및 시스템 담당 조직이 지정되어 있어야 함 |
| ServiceId | {업무코드}.{도메인}.{행위} 형식으로 정의 |
| 계층구조 | Handler·Facade·Service·Rule·DAO·Mapper 책임 확정 |
| 데이터 소유권 | 테이블·View·이벤트의 소유 도메인 지정 |
| 표준 전문 | Header·Body·Result 형식 정의 |
| 오류코드 | 제공·소비 도메인의 오류코드 체계 정의 |
| Timeout 정책 | 호출 ServiceId별 제한시간 정의 |
| 인증 문맥 | 사용자·지점·채널·권한 정보 생성 |
| 추적정보 | GUID·TraceId·호출 순번 전달 가능 |
| 운영등록 | OM Service Catalog와 거래통제 정책 등록 |
| 계약관리 | 연계 계약과 버전 관리 저장소 마련 |

2.5 용어 정의

| 용어 | 정의 |
| --- | --- |
| 소비 도메인 | 다른 도메인의 기능이나 데이터를 사용하는 도메인 |
| 제공 도메인 | 공개 계약을 통해 기능이나 데이터를 제공하는 도메인 |
| 도메인 내부 구현 | Service, Rule, DAO, Mapper, Entity 등 외부 비공개 구성 |
| 공개 계약 | 다른 도메인이 사용할 수 있도록 승인된 Interface·DTO·ServiceId·이벤트 |
| Application Contract | 동일 WAR에서 도메인 간 호출에 사용하는 Java 인터페이스 |
| Client Adapter | 다른 WAR·외부 시스템 호출을 내부 인터페이스로 변환하는 구성요소 |
| Orchestrator | 여러 도메인의 공개 계약을 조합하여 하나의 유스케이스를 수행하는 구성요소 |
| ServiceId 연동 | TCF/EAI 표준 전문으로 다른 WAR의 거래를 호출하는 방식 |
| Domain Event | 도메인에서 발생한 업무 사실을 나타내는 비동기 메시지 |
| Read Model | 여러 데이터 원천을 조회 목적에 맞게 구성한 읽기 전용 모델 |
| 데이터 소유 도메인 | 특정 데이터의 생성·변경·삭제 규칙을 책임지는 도메인 |
| 계약 버전 | 요청·응답·이벤트 구조의 호환성을 식별하는 버전 |
| 필수 의존 | 하위 호출이 실패하면 상위 거래도 실패해야 하는 의존관계 |
| 선택 의존 | 하위 호출이 실패해도 부분 결과로 처리할 수 있는 관계 |
| 보상 처리 | 이미 완료된 처리의 업무 효과를 반대 거래로 상쇄하는 방식 |
| 최종 일관성 | 여러 도메인의 상태가 즉시가 아닌 일정 시간 후 일치하는 특성 |

3. 본문

3.1 문제 정의 및 설계 배경

3.1.1 도메인 우선 구조와 연동 문제

NSIGHT 업무 패키지는 다음 구조를 기본으로 한다.

```
com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}
```

예:

```
com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.rule
com.nh.nsight.marketing.sv.customer.dao
com.nh.nsight.marketing.sv.customer.mapper
```

업무가 증가하면 하나의 화면이나 유스케이스가 여러 도메인의 데이터를 필요로 할 수 있다.

```
고객 종합정보 조회
  ├─ Customer 도메인
  ├─ ProductHolding 도메인
  ├─ Contact 도메인
  └─ Campaign 도메인
```

이때 단순히 다른 도메인의 Service·DAO·Mapper를 직접 호출하면 도메인 우선 구조의 장점이 사라진다.

도메인 설계기준은 여러 도메인의 조합 유스케이스를 Facade 또는 별도 Orchestrator가 조정하도록 하고, 다른 도메인의 DAO·Mapper 직접 호출을 금지한다.

3.1.2 기술 계층과 업무 경계의 차이

계층은 기술적 책임을 나타내고, 도메인은 업무 책임을 나타낸다.

```
계층
= 어떤 방식으로 처리하는가

도메인
= 어떤 업무 책임을 수행하는가
```

따라서 다음 두 기준을 동시에 지켜야 한다.

```
업무 경계
Customer / Product / Contact / Campaign

기술 계층
Handler / Facade / Service / Rule / DAO / Mapper
```

도메인 간 연동은 기술 계층을 우회해서는 안 된다.

```
금지:
CustomerService
  → ProductMapper

허용:
CustomerViewFacade
  → ProductHoldingQueryContract
      → ProductHoldingFacade
          → ProductHoldingService
              → ProductHoldingDao
```

3.1.3 직접 호출이 만드는 구조적 문제

| 직접 호출 유형 | 문제 |
| --- | --- |
| 다른 도메인 Service 직접 호출 | 내부 구현과 강결합 |
| 다른 도메인 DAO 호출 | 업무 규칙과 데이터 소유권 우회 |
| 다른 도메인 Mapper 호출 | 저장 구조와 SQL에 직접 결합 |
| 다른 도메인 Entity 사용 | 모델 의미와 상태 규칙 침투 |
| 다른 WAR Java Import | 독립 배포와 ClassLoader 경계 훼손 |
| 다른 도메인 테이블 DML | 데이터 정합성과 감사 책임 훼손 |
| 양방향 호출 | 순환 의존과 장애 전파 |
| 다단계 동기 호출 | 응답시간 증가와 Timeout 연쇄 |

3.2 현행 구조와 문제점

3.2.1 비표준 동일 WAR 연동

```
CustomerFacade
  ↓
ProductService
  ↓
ProductDao
  ↓
ProductMapper
```

문제점

- 소비 도메인이 제공 도메인의 내부 계층을 알고 있다.
- ProductService 메서드 변경이 Customer 도메인까지 전파된다.
- Product 도메인이 제공할 정보의 범위가 통제되지 않는다.
- 순환 호출을 정적 구조에서 차단하기 어렵다.
- 공개 기능과 내부 기능이 구분되지 않는다.
3.2.2 비표준 다른 WAR 연동

```
ic-service
  └─ implementation project(":sv-service")
       └─ SvCustomerService 직접 호출
```

또는:

```
IC Service
  → SV DB
  → SV_CUSTOMER 테이블 직접 조회
```

문제점

| 문제 | 영향 |
| --- | --- |
| 배포 결합 | SV 변경 시 IC 재빌드·재배포 필요 |
| ClassLoader 결합 | WAR 분리 구조와 충돌 |
| 운영통제 우회 | TCF·STF·거래통제·Timeout 미적용 |
| 추적성 상실 | 원격 ServiceId와 거래로그 미생성 |
| 데이터 소유권 침해 | SV 규칙을 거치지 않고 데이터 사용 |
| 장애 확산 | SV 내부 변경이 IC 장애로 연결 |
| 보안 우회 | SV 권한·마스킹·감사정책 미적용 |

업무 WAR 간 Java Import로 순환 의존이 발생하면 빌드 또는 기동 실패로 이어질 수 있으므로, WAR 간 직접 Import를 제거하고 ServiceId·Client·tcf-eai 방식으로 전환해야 한다.

3.2.3 통합 조회의 비표준 구현

```
SELECT ...
  FROM SV_CUSTOMER C
       JOIN PD_PRODUCT_HOLDING P
       JOIN CT_CONTACT_HISTORY H
```

위 SQL을 Customer 도메인의 Mapper에 작성하면 다음 문제가 발생한다.

- 다른 도메인의 물리 테이블 구조에 직접 의존한다.
- 테이블 변경 시 모든 조회 도메인에 영향이 발생한다.
- 개인정보·접근권한 정책이 누락될 수 있다.
- 데이터 소유자와 SQL 운영 책임이 불명확해진다.
- 조회 성능 문제의 책임 영역을 분리하기 어렵다.
3.3 요구사항과 제약조건

3.3.1 기능 요구사항

| ID | 요구사항 |
| --- | --- |
| INT-DOM-001 | 모든 도메인 간 연동은 소비·제공 도메인이 식별되어야 한다. |
| INT-DOM-002 | 연동은 공개된 계약을 통해서만 수행해야 한다. |
| INT-DOM-003 | 동일 WAR와 다른 WAR 연동 방식을 구분해야 한다. |
| INT-DOM-004 | 다른 도메인의 데이터 변경은 소유 도메인이 수행해야 한다. |
| INT-DOM-005 | 호출별 필수·선택 의존 여부가 정의되어야 한다. |
| INT-DOM-006 | Timeout·재시도·중복방지 정책이 정의되어야 한다. |
| INT-DOM-007 | 오류코드 변환과 사용자 응답 기준이 정의되어야 한다. |
| INT-DOM-008 | GUID·TraceId·호출 ServiceId가 전 구간 전달되어야 한다. |
| INT-DOM-009 | 요청·응답·이벤트 계약의 버전을 관리해야 한다. |
| INT-DOM-010 | 도메인 간 순환 의존성을 허용하지 않아야 한다. |
| INT-DOM-011 | 비동기 연동은 재처리와 Dead Letter 기준을 가져야 한다. |
| INT-DOM-012 | 통합 조회는 Read Model 우선으로 설계해야 한다. |

3.3.2 비기능 요구사항

| 항목 | 기준 |
| --- | --- |
| 결합도 | 제공 도메인의 내부 구현·테이블 구조에 의존하지 않음 |
| 응집도 | 조합 유스케이스는 별도 Facade·Orchestrator에 집중 |
| 추적성 | 상위 ServiceId부터 하위 ServiceId까지 호출관계 추적 |
| 성능 | 동기 호출 수와 전체 Timeout Budget 통제 |
| 가용성 | 선택 의존 장애 시 부분 성공 또는 대체 처리 |
| 보안 | 사용자·지점·채널·권한 문맥 위변조 방지 |
| 확장성 | 제공 도메인의 구현 변경이 소비 도메인에 미치는 영향 최소화 |
| 호환성 | 이전 계약을 사용하는 소비자에 대한 보호 |
| 운영성 | 제공·소비 도메인별 응답시간·오류율 집계 |
| 테스트성 | 제공자·소비자 계약을 독립적으로 테스트 |

3.3.3 제약조건

- Handler는 자기 도메인의 Facade만 호출한다.
- Handler에서 다른 도메인 호출을 조정하지 않는다.
- Rule은 다른 도메인·DB·Cache·외부 시스템을 호출하지 않는다.
- DAO와 Mapper는 다른 도메인의 Service를 역참조하지 않는다.
- 다른 도메인의 DAO·Mapper를 직접 호출하지 않는다.
- 다른 도메인의 소유 테이블을 임의로 변경하지 않는다.
- 다른 업무 WAR의 Java 클래스를 직접 Import하지 않는다.
- 도메인 간 양방향 의존과 순환 호출을 허용하지 않는다.
- 원격 호출을 로컬 메서드 호출처럼 무제한 사용하지 않는다.
- 조회 편의를 이유로 공통 테이블을 무소유 상태로 만들지 않는다.
- 분산 트랜잭션이나 JTA를 기본 연동 방식으로 사용하지 않는다.
- 재시도 가능 여부를 판단하지 않고 상태변경 거래를 자동 재시도하지 않는다.
- Provider의 원본 오류·Stack Trace를 사용자 응답으로 전달하지 않는다.
- 개인정보 원문을 이벤트·로그·추적 Header에 저장하지 않는다.
- 하위 호출 Timeout을 상위 거래 Timeout보다 길게 설정하지 않는다.
3.4 설계 원칙

3.4.1 공개 계약 우선 원칙

도메인 외부에서는 공개 계약만 사용할 수 있다.

```
도메인 외부 공개 가능
- Query Contract
- Command Contract
- ServiceId
- 연계 Request·Response DTO
- Domain Event
- 식별자와 표준 코드

도메인 외부 공개 금지
- 내부 Service 구현
- Rule
- DAO
- Mapper
- Entity 내부 상태
- DB Result DTO
- 테이블 구조
```

동일 WAR의 도메인 간 호출도 구체 Facade 클래스보다 공개 인터페이스에 의존하는 것을 권장한다.

```
public interface CustomerQueryContract {
    CustomerSummaryResult findCustomerSummary(
        CustomerSummaryQuery query
    );
}
```

제공 도메인의 Facade 또는 Application Service가 해당 계약을 구현한다.

3.4.2 애플리케이션 계층 조정 원칙

여러 도메인의 기능을 조합하는 책임은 Facade 또는 Orchestrator에 둔다.

```
Handler
  ↓
UseCase Facade
  ↓
Orchestrator
  ├─ CustomerQueryContract
  ├─ ProductHoldingQueryContract
  └─ ContactQueryContract
```

Service와 Rule은 원격 연동 순서, 재시도, 부분 성공 같은 기술적 조정을 담당하지 않는다.

적용 판단

| 상황 | 조정 위치 |
| --- | --- |
| 단일 도메인 유스케이스 | 해당 도메인 Facade |
| 동일 WAR의 2개 도메인 단순 조회 | 소비 도메인 Facade |
| 3개 이상 도메인 조합 | 별도 Orchestrator 권장 |
| 여러 상태변경과 보상 필요 | Process/Saga Orchestrator |
| 단순 데이터 변환 | Adapter 또는 Assembler |

3.4.3 데이터 소유권 단일화 원칙

하나의 업무 데이터는 하나의 도메인만 변경 책임을 갖는다.

| 권한 | 소유 도메인 | 참조 도메인 |
| --- | --- | --- |
| 생성 | 허용 | 금지 |
| 변경 | 허용 | 금지 |
| 삭제 | 허용 | 금지 |
| 상태 전이 | 허용 | 금지 |
| 승인된 조회 | 허용 | 허용 |
| Read Model 수신 | 제공 | 사용 가능 |
| 이벤트 구독 | 발행 | 구독 가능 |

```
Customer 데이터 변경
→ Customer 도메인의 Command 계약 호출

ProductHolding 데이터 변경
→ ProductHolding 도메인의 Command 계약 호출
```

다른 도메인의 테이블 DML은 데이터 소유권 위반으로 처리한다.

3.4.4 단방향 의존 원칙

도메인 간 의존관계는 방향성을 가져야 한다.

```
CustomerView
  → Customer
  → ProductHolding
  → Contact
```

다음 구조는 금지한다.

```
Customer
  → Product

Product
  → Customer
```

양방향 업무관계가 필요하다면 다음 방식 중 하나를 적용한다.

- 별도 조정 도메인 분리
- 이벤트 기반 역방향 전달
- Shared Kernel이 아닌 최소 공통 식별자 사용
- 상위 Process Orchestrator 도입
3.4.5 동기 최소화 원칙

동기 호출은 호출자 Thread와 Timeout Budget을 점유한다.

```
상위 거래
  → 도메인 A 500ms
  → 도메인 B 700ms
  → 도메인 C 900ms
  → 조립 200ms
```

하위 호출 시간이 단순 합산되므로 동기 호출 수를 최소화해야 한다.

동기 호출 적용 조건

- 즉시 결과가 반드시 필요하다.
- 호출 수가 제한적이다.
- 제공 도메인의 응답시간이 안정적이다.
- 실패 시 상위 거래를 즉시 종료할 수 있다.
- 전체 Timeout Budget 내에서 처리할 수 있다.
비동기 호출 적용 조건

- 후속 처리 결과를 즉시 반환할 필요가 없다.
- 대량·장시간 처리가 필요하다.
- 도메인 간 장애 격리가 중요하다.
- 최종 일관성을 허용할 수 있다.
- 재처리·중복방지가 가능하다.
3.4.6 계약 변환 원칙

제공 도메인의 모델을 소비 도메인 내부에서 직접 사용하지 않는다.

```
Provider Response
  ↓
Client Adapter / ACL
  ↓
Consumer Internal Model
```

예:

```
SvCustomerSummaryResponse
  ↓ CustomerClientAdapter
IcIntegratedCustomer
```

Anti-Corruption Layer를 통해 제공 도메인의 용어와 모델이 소비 도메인의 업무 모델에 무분별하게 침투하지 않도록 한다.

3.4.7 트랜잭션 지역화 원칙

트랜잭션은 가능한 한 하나의 도메인과 하나의 저장소 안에서 종료한다.

```
권장:
Facade
  → 같은 도메인 Service
  → 같은 도메인 DAO
  → Local Transaction Commit
```

다른 WAR나 독립 도메인의 상태변경을 하나의 DB 트랜잭션으로 묶지 않는다.

```
비권장:
IC Facade Transaction
  → IC DB 변경
  → SV 원격 호출
  → CM 원격 호출
  → 전체 JTA Commit
```

분산 상태변경은 다음 구조를 적용한다.

```
로컬 상태 저장
→ Outbox 이벤트 기록
→ Commit
→ 이벤트 발행
→ 대상 도메인 처리
→ 성공 또는 보상
```

3.5 대안 비교 및 의사결정

3.5.1 연동 방식 비교

| 연동 방식 | 적용 범위 | 장점 | 문제점 | 판단 |
| --- | --- | --- | --- | --- |
| 내부 Service 직접 호출 | 동일 도메인 | 단순·빠름 | 외부 공개 시 결합 증가 | 도메인 내부만 허용 |
| 공개 Application Contract | 동일 WAR, 다른 도메인 | 형식 안전성·빠른 호출 | 순환 의존 관리 필요 | 권장 |
| 공개 Facade 직접 호출 | 동일 WAR | 기존 구조 활용 | 구체 클래스 결합 가능 | 계약 Interface 경유 권장 |
| TCF/EAI ServiceId | 다른 WAR | 통제·추적·독립 배포 | 네트워크·직렬화 비용 | 표준 |
| 비동기 이벤트 | 독립 도메인 | 느슨한 결합·장애 격리 | 최종 일관성·재처리 | 권장 |
| Read Model | 통합 조회 | 조회 성능과 독립성 | 동기화 지연·별도 저장 | 빈번한 통합조회 권장 |
| DB View | 동일 DB 조회 | 구현 단순 | 스키마 결합 | DA 승인 시 제한 허용 |
| 테이블 직접 Join | 다른 도메인 | 초기 개발 편리 | 강결합·소유권 침해 | 원칙적 금지 |
| 다른 테이블 DML | 다른 도메인 | 즉시 변경 | 규칙·감사 우회 | 금지 |
| WAR 간 Java Import | 다른 WAR | 컴파일 편의 | 배포·ClassLoader 결합 | 금지 |
| 분산 JTA | 여러 시스템 | 원자성 | 복잡성·장애범위 증가 | 예외 승인 사항 |

도메인 설계기준에서도 동일 WAR에서는 공개 Facade, 다른 WAR에서는 TCF/EAI ServiceId, 비동기에서는 이벤트를 적용하고 DB 직접 접근과 공통 테이블 공유를 원칙적으로 선택하지 않는다.

3.5.2 통합 조회 대안

| 우선순위 | 방식 | 적용 기준 |
| --- | --- | --- |
| 1 | 조회 전용 Read Model | 호출 빈도가 높고 응답시간이 중요 |
| 2 | Facade·Orchestrator 조합 | 호출 수가 적고 실시간 정합성이 중요 |
| 3 | 승인된 DB View | 동일 DB이며 DA가 소유권·성능을 승인 |
| 4 | 다른 도메인 테이블 직접 Join | 원칙적으로 금지 |

기존 도메인 설계기준도 여러 도메인의 통합 조회에 대해 Read Model을 우선하고, 직접 테이블 Join을 금지하는 방향으로 정의한다.

3.5.3 최종 의사결정

```
같은 도메인
→ 기존 6계층 내부 호출

같은 WAR의 다른 도메인
→ 공개 Application Contract
→ 제공 도메인 Facade 구현
→ 소비 도메인 Facade·Orchestrator에서 호출

다른 WAR
→ tcf-eai Client
→ 표준 전문
→ ServiceId 호출
→ TCF/STF/ETF 적용

비동기 후속 처리
→ Domain Event
→ Outbox
→ Consumer
→ 재처리·DLQ

빈번한 통합 조회
→ 조회 전용 Read Model
```

3.6 목표 아키텍처

3.6.1 전체 구조

```
┌──────────────────────────────────────────────────────────┐
│ 채널·화면                                                │
│ WEBTOPSUITE / BI Portal / UJ / 외부 채널                 │
└─────────────────────────┬────────────────────────────────┘
                          │ 표준 전문
                          ▼
┌──────────────────────────────────────────────────────────┐
│ TCF 플랫폼                                               │
│ Controller → TCF → STF → Dispatcher → ETF                │
└─────────────────────────┬────────────────────────────────┘
                          │ ServiceId
                          ▼
┌──────────────────────────────────────────────────────────┐
│ 소비 도메인                                              │
│ Handler                                                  │
│   ↓                                                      │
│ UseCase Facade                                           │
│   ↓                                                      │
│ Orchestrator                                             │
│   ├─ Local Domain Service                                │
│   ├─ Same-WAR Application Contract                      │
│   ├─ Remote Domain Client → tcf-eai                     │
│   └─ Event Publisher                                    │
└────────────┬────────────────┬────────────────────────────┘
             │                │
       동일 WAR Java 계약     │ 다른 WAR ServiceId
             │                │
             ▼                ▼
┌────────────────────┐  ┌──────────────────────────────────┐
│ 제공 도메인 A      │  │ 제공 업무 WAR B                 │
│ Public Contract    │  │ TCF → Handler → Facade           │
│   ↓                │  │   ↓                              │
│ Facade             │  │ Service → Rule → DAO → Mapper   │
│   ↓                │  └──────────────────────────────────┘
│ Service            │
│   ↓                │
│ DAO → Mapper       │
└────────────────────┘
```

3.6.2 동일 WAR 연동 구조

```
CustomerViewHandler
  ↓
CustomerViewFacade
  ↓
CustomerOverviewOrchestrator
  ├─ CustomerQueryContract
  │    └─ CustomerFacade
  ├─ ProductHoldingQueryContract
  │    └─ ProductHoldingFacade
  └─ ContactQueryContract
       └─ ContactFacade
  ↓
CustomerOverviewResponse
```

소비 도메인은 제공 도메인의 Service·DAO·Mapper를 알지 못한다.

3.6.3 다른 WAR 연동 구조

```
IC.CustomerIntegration.selectCustomer
  ↓
IcCustomerIntegrationHandler
  ↓
IcCustomerIntegrationFacade
  ↓
SvCustomerClient
  ↓
tcf-eai
  ↓
POST /sv/online

Header
- businessCode: SV
- serviceId: SV.Customer.selectSummary
- transactionCode: SV-INQ-0001
- guid: 상위 거래 GUID
- traceId: 상위 TraceId
- parentServiceId: IC.CustomerIntegration.selectCustomer

  ↓
SV TCF / STF
  ↓
SvCustomerHandler
  ↓
SvCustomerFacade
  ↓
표준 응답
```

다른 WAR 호출에서도 ServiceId·Timeout·오류·GUID·TraceId·거래로그를 유지해야 한다.

3.6.4 비동기 이벤트 구조

```
Campaign 승인
  ↓
CmCampaignFacade
  ├─ Campaign 상태 변경
  └─ Outbox Event 저장
  ↓ Local Commit

Event Publisher
  ↓
CAMPAIGN_APPROVED_V1
  ↓
Event Broker
  ├─ EP 도메인 구독
  ├─ MG 도메인 구독
  └─ OM 감사 도메인 구독
```

3.7 표준 형식

3.7.1 도메인 연동 ID

```
{소비업무코드}-{소비도메인코드}
-{제공업무코드}-{제공도메인코드}
-{연동유형}-{4자리순번}
```

예:

```
IC-CUST-SV-CUST-SYNC-0001
SV-CVIEW-PD-HOLD-SYNC-0001
CM-CAMP-EP-EVENT-ASYNC-0001
```

3.7.2 연동유형 코드

| 코드 | 의미 |
| --- | --- |
| LOCAL | 동일 도메인 내부 |
| CONTRACT | 동일 WAR의 공개 Application Contract |
| SYNC | 다른 WAR 동기 ServiceId 호출 |
| ASYNC | 이벤트·메시지 비동기 호출 |
| READ | Read Model·조회 전용 연동 |
| FILE | 파일 기반 연계 |
| BATCH | 배치 기반 연계 |

3.7.3 연동 계약 관리 항목

| 항목 | 필수 | 설명 |
| --- | --- | --- |
| 연동 ID | 예 | 도메인 관계 고유 식별자 |
| 소비 업무·도메인 | 예 | 호출 주체 |
| 제공 업무·도메인 | 예 | 기능 제공 주체 |
| 연동 목적 | 예 | 조회·검증·상태변경·이벤트 |
| 호출 방식 | 예 | Contract·ServiceId·Event·Read Model |
| ServiceId·Event Type | 조건 | 실행 또는 이벤트 식별자 |
| 요청 계약 | 예 | 필드·필수값·검증 기준 |
| 응답 계약 | 조건 | 결과·상태·오류 구조 |
| 데이터 분류 | 예 | 일반·개인·민감·고유식별정보 |
| 필수·선택 의존 | 예 | 실패 전파 기준 |
| Timeout | 동기 필수 | 호출 제한시간 |
| 재시도 | 예 | 가능 여부와 횟수 |
| Idempotency Key | 상태변경 필수 | 중복방지 기준 |
| 일관성 방식 | 예 | 즉시·최종 일관성 |
| 오류 매핑 | 예 | 제공 오류와 소비 오류 관계 |
| 보상 방식 | 상태변경 조건 | 취소·역거래 기준 |
| 계약 버전 | 예 | V1, V2 |
| 제공 조직 | 예 | 업무·개발·운영 담당 |
| 소비 조직 | 예 | 업무·개발·운영 담당 |
| 폐기 예정일 | 조건 | 구버전 종료일 |

3.7.4 동일 WAR 공개 계약 패키지

```
com.nh.nsight.marketing.sv.customer
├─ contract
│  ├─ CustomerQueryContract.java
│  ├─ CustomerCommandContract.java
│  └─ dto
│     ├─ CustomerSummaryQuery.java
│     └─ CustomerSummaryResult.java
├─ facade
│  └─ SvCustomerFacade.java
├─ service
├─ rule
├─ dao
└─ mapper
```

계약 패키지만 다른 도메인에서 참조할 수 있다.

3.7.5 다른 WAR Client 패키지

```
com.nh.nsight.marketing.ic.customerintegration
├─ handler
├─ facade
├─ service
├─ client
│  ├─ SvCustomerClient.java
│  ├─ SvCustomerClientAdapter.java
│  └─ dto
└─ dto
```

SvCustomerClientAdapter만 tcf-eai와 원격 전문을 알고, 업무 Service는 Client Interface에 의존한다.

3.7.6 이벤트 계약 형식

```
{
  "eventId": "0190a...",
  "eventType": "CM.Campaign.Approved",
  "eventVersion": "1.0",
  "occurredAt": "2026-07-17T08:30:00+09:00",
  "sourceBusinessCode": "CM",
  "sourceDomain": "Campaign",
  "aggregateId": "CMP00001234",
  "guid": "GUID...",
  "traceId": "TRACE...",
  "payload": {
    "campaignId": "CMP00001234",
    "approvedBy": "masked-or-internal-id"
  }
}
```

3.8 구성요소 및 속성

| 구성요소 | 위치 | 책임 |
| --- | --- | --- |
| UseCaseFacade | 소비 도메인 | 유스케이스와 트랜잭션 경계 |
| DomainOrchestrator | 소비·조정 도메인 | 여러 도메인 호출 순서와 결과 조합 |
| QueryContract | 제공 도메인 | 승인된 조회 기능 공개 |
| CommandContract | 제공 도메인 | 승인된 상태변경 기능 공개 |
| DomainClient | 소비 도메인 | 다른 WAR 호출 추상화 |
| ClientAdapter | 소비 도메인 | ServiceId 전문 조립·오류 변환 |
| AntiCorruptionMapper | 소비 도메인 | 제공 모델을 내부 모델로 변환 |
| EventPublisher | 제공 도메인 | Domain Event 발행 |
| EventConsumer | 소비 도메인 | 이벤트 수신·검증·중복방지 |
| OutboxRepository | 제공 도메인 | 트랜잭션과 이벤트 기록 일치 |
| ReadModelBuilder | 조회 도메인 | 통합 조회 데이터 구성 |
| IntegrationPolicy | 플랫폼·도메인 | Timeout·재시도·부분성공 정책 |
| IntegrationLog | TCF·OM | 도메인 간 호출 추적 |

3.9 책임 경계와 RACI

3.9.1 계층별 책임 경계

| 계층 | 도메인 간 연동 책임 | 금지 사항 |
| --- | --- | --- |
| Handler | 자기 ServiceId와 Facade 연결 | 다른 도메인 호출 조정 |
| Facade | 유스케이스·트랜잭션·Orchestrator 호출 | 다른 도메인 DAO·Mapper 호출 |
| Orchestrator | 호출 순서·부분성공·보상 조정 | 업무 데이터 직접 저장 |
| Service | 자기 도메인 업무 처리 | 원격 호출 흐름 남용 |
| Rule | 자기 도메인 규칙 검증 | DB·Client·다른 도메인 호출 |
| DAO | 자기 도메인 데이터 접근 | 다른 도메인 업무 판단 |
| Mapper | 승인된 SQL 실행 | 다른 도메인 DML |
| Client | 원격 계약 호출 추상화 | 업무 규칙 판단 |
| Adapter | 전문·오류·모델 변환 | 도메인 상태 직접 변경 |
| Event Consumer | 이벤트 수신·중복검증·업무 위임 | 원본 이벤트 임의 변경 |

3.9.2 조직 RACI

| 활동 | AA | 소비 도메인 | 제공 도메인 | TCF팀 | DA/DBA | 보안 | 운영 | QA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 연동 방식 결정 | A | R | C | C | C | C | C | C |
| 공개 계약 정의 | C | C | A/R | C | C | C | I | C |
| 소비 Client 구현 | C | A/R | C | C | I | C | I | C |
| 데이터 소유권 승인 | C | C | R | I | A/R | C | I | I |
| ServiceId 등록 | C | C | R | A/R | I | I | C | I |
| Timeout·재시도 정의 | A | R | R | C | I | I | C | C |
| 오류코드 매핑 | C | R | R | C | I | I | C | C |
| 보안·개인정보 검토 | C | R | R | I | C | A | C | C |
| 운영 모니터링 | C | C | C | C | I | I | A/R | C |
| 계약 테스트 | C | R | R | C | I | I | I | A |
| 계약 폐기 승인 | A | R | R | C | I | C | C | C |

R: 수행, A: 최종 책임, C: 협의, I: 통보

3.10 정상 처리 흐름

3.10.1 동일 WAR의 조합 조회

```
[1] 화면
    ↓ SV.CustomerView.selectOverview

[2] TCF / STF
    ↓

[3] CustomerViewHandler
    ↓

[4] CustomerViewFacade
    ↓

[5] CustomerOverviewOrchestrator
    ├─ CustomerQueryContract
    ├─ ProductHoldingQueryContract
    └─ ContactQueryContract
    ↓

[6] 계약별 응답을 소비 도메인 모델로 변환
    ↓

[7] CustomerOverviewResponse 조립
    ↓

[8] ETF.success
```

기존 도메인 설계기준에서도 여러 도메인의 조회를 조합할 때 공개 조회계약을 사용하고 다른 도메인의 DAO·Mapper를 직접 호출하지 않도록 정의한다.

3.10.2 다른 WAR 필수 조회

```
IC 거래
  ↓
IcCustomerIntegrationFacade
  ↓
SvCustomerClient
  ↓
tcf-eai
  ↓
SV.Customer.selectSummary
  ↓
SV 처리 성공
  ↓
Client Adapter 변환
  ↓
IC 업무 계속 처리
```

필수 조회이므로 SV 호출 실패 시 IC 거래도 실패한다.

3.10.3 다른 WAR 선택 조회

```
고객 종합조회
  ├─ Customer 필수 조회
  ├─ ProductHolding 필수 조회
  └─ RecentCampaign 선택 조회
```

RecentCampaign 호출 실패 시 사전 정의된 경우 다음처럼 처리할 수 있다.

```
{
  "customer": {},
  "productHoldings": [],
  "recentCampaigns": [],
  "partialResult": true,
  "warnings": [
    {
      "domain": "Campaign",
      "code": "CAMPAIGN_DATA_UNAVAILABLE"
    }
  ]
}
```

부분 성공 여부는 개발자가 임의로 결정하지 않고 거래설계서에 명시한다.

3.10.4 상태변경 이벤트 처리

```
Campaign 승인
  ↓
Campaign 상태 APPROVED 저장
  ↓
Outbox CAMPAIGN_APPROVED 기록
  ↓
Local Commit
  ↓
Publisher 발행
  ↓
EP Consumer 수신
  ↓
eventId 중복 확인
  ↓
실시간 처리대상 등록
  ↓
처리 완료 기록
```

3.11 오류·Timeout·장애 흐름

3.11.1 오류 유형

| 오류 유형 | 예 | 처리 주체 |
| --- | --- | --- |
| 제공 업무 오류 | 고객 미존재, 승인 불가 | 제공 도메인 |
| 연동 계약 오류 | 필수 필드 누락, 버전 불일치 | Client·Provider |
| 인증·권한 오류 | 조회권한 없음 | STF·제공 도메인 |
| Timeout | 응답 제한시간 초과 | Client·TCF |
| 시스템 오류 | DB·프로그램 오류 | 제공 도메인·ETF |
| 연결 오류 | 대상 WAR 접근 불가 | Client·인프라 |
| 메시지 오류 | 발행·소비 실패 | 이벤트 플랫폼 |
| 중복 이벤트 | 동일 eventId 재수신 | Consumer |
| 데이터 불일치 | 참조 ID 미존재 | 소비 도메인 |

3.11.2 오류 변환 기준

```
제공 도메인 원본 오류
  ↓
Client Adapter
  ↓
소비 도메인 연동 오류
  ↓
상위 거래 오류 또는 경고
```

예:

| 제공 오류 | 소비 오류 | 사용자 처리 |
| --- | --- | --- |
| E-SV-CUST-0001 고객 미존재 | 동일 업무 의미 유지 | 고객 미존재 안내 |
| E-SV-AUTH-0002 권한 없음 | E-IC-INT-0403 | 접근권한 오류 |
| TCF-TIMEOUT | E-IC-INT-0504 | 잠시 후 재시도 |
| SYSTEM_ERROR | E-IC-INT-0500 | 시스템 오류 |

원본 오류코드·대상 ServiceId·TraceId는 운영로그에 보존한다.

3.11.3 Timeout Budget

상위 거래 Timeout이 3초라면 하위 호출 Timeout의 합과 내부 처리시간이 3초를 초과해서는 안 된다.

| 처리 구간 | 예산 |
| --- | --- |
| STF·라우팅 | 200ms |
| Customer 호출 | 700ms |
| Product 호출 | 700ms |
| Contact 호출 | 500ms |
| 응답 조립 | 300ms |
| 여유시간 | 600ms |
| 합계 | 3,000ms |

병렬 호출이 가능한 조회는 병렬화를 검토할 수 있지만 다음 조건을 충족해야 한다.

- 별도 Executor 용량 통제
- 상위 TransactionContext 전달
- MDC·TraceId 전달
- 각 호출 독립 Timeout 적용
- 부분 실패 처리 정의
- Thread 무제한 생성 금지
3.11.4 재시도 기준

| 호출 유형 | 자동 재시도 |
| --- | --- |
| 단순 조회 | 제한적으로 가능 |
| 멱등 상태조회 | 가능 |
| 멱등키가 있는 상태변경 | 정책 승인 후 가능 |
| 결제·승인·등록 | 기본 금지 |
| Timeout 결과 불명 상태변경 | 즉시 재시도 금지 |
| 이벤트 소비 | 재시도 Queue 적용 |
| 파일 배치 | Checkpoint 기반 재처리 |

재시도는 최대 횟수, 간격, 전체 Timeout을 함께 설정한다.

3.11.5 Circuit Breaker 및 격리

중요한 원격 도메인이 지속적으로 실패하면 호출을 계속하여 Thread를 소모하지 않도록 차단 정책을 적용한다.

```
오류율·Timeout 임계치 초과
  ↓
Circuit OPEN
  ↓
즉시 실패 또는 대체 응답
  ↓
일정 시간 후 HALF-OPEN
  ↓
시험 호출 성공 시 CLOSE
```

선택 의존 서비스는 대체 응답을 사용할 수 있고, 필수 의존 서비스는 빠르게 실패시킨다.

3.11.6 비동기 장애

```
이벤트 처리 실패
  ↓
재시도 Queue
  ↓ 지정 횟수 초과
Dead Letter Queue
  ↓
운영 알림
  ↓
원인 수정
  ↓
관리자 재처리
```

DLQ 데이터에는 다음을 포함한다.

- eventId
- eventType
- eventVersion
- sourceDomain
- consumerDomain
- 최초 발생시각
- 마지막 실패시각
- 재시도 횟수
- 오류코드
- TraceId
- 재처리 상태
3.12 정상 예시

3.12.1 동일 WAR 계약 호출

```
@Component
@RequiredArgsConstructor
public class CustomerOverviewOrchestrator {

    private final CustomerQueryContract customerQuery;
    private final ProductHoldingQueryContract productHoldingQuery;
    private final ContactQueryContract contactQuery;

    public CustomerOverviewResult selectOverview(
            CustomerOverviewQuery query) {

        CustomerSummaryResult customer =
            customerQuery.findSummary(
                new CustomerSummaryQuery(query.customerNo())
            );

        ProductHoldingResult products =
            productHoldingQuery.findHoldings(
                new ProductHoldingQuery(query.customerNo())
            );

        ContactSummaryResult contacts =
            contactQuery.findRecentContacts(
                new ContactSummaryQuery(query.customerNo())
            );

        return CustomerOverviewResult.of(
            customer,
            products,
            contacts
        );
    }
}
```

3.12.2 다른 WAR Client

```
public interface SvCustomerClient {

    CustomerSummaryResult selectCustomerSummary(
        CustomerSummaryQuery query,
        IntegrationContext context
    );
}
@Component
@RequiredArgsConstructor
class SvCustomerClientAdapter implements SvCustomerClient {

    private static final String SERVICE_ID =
        "SV.Customer.selectSummary";

    private final TcfEaiClient tcfEaiClient;

    @Override
    public CustomerSummaryResult selectCustomerSummary(
            CustomerSummaryQuery query,
            IntegrationContext context) {

        StandardRequest request =
            requestFactory.create(
                SERVICE_ID,
                query,
                context
            );

        StandardResponse response =
            tcfEaiClient.call(
                "SV",
                request,
                Duration.ofMillis(700)
            );

        return responseMapper.toCustomerSummary(response);
    }
}
```

3.12.3 데이터 변경 요청

```
IC 도메인이 고객 연락처를 변경해야 함
  ↓
IC가 SV_CUSTOMER 테이블 UPDATE
  = 금지

IC
  ↓ SV.Customer.updateContact ServiceId
SV Customer 도메인
  ↓ 권한·업무규칙 검증
  ↓ 고객 연락처 변경
  ↓ 감사로그
  = 정상
```

3.13 금지 예시

3.13.1 다른 도메인 Mapper 호출

```
@Service
class CustomerViewService {

    private final ProductHoldingMapper productHoldingMapper;
}
```

금지 사유:

- ProductHolding 데이터 소유권 침해
- SQL·테이블 구조 직접 결합
- 제공 도메인의 업무 규칙 우회
3.13.2 다른 WAR 클래스 Import

```
dependencies {
    implementation project(":sv-service")
}
```

금지 사유:

- 독립 WAR 배포 불가
- 업무 모듈 순환 의존 가능
- ClassLoader·버전 충돌
- 런타임 TCF 통제 우회
3.13.3 Handler에서 다중 도메인 조정

```
public Object handle(StandardRequest request) {
    customerFacade.select(...);
    productFacade.select(...);
    campaignFacade.select(...);
}
```

금지 사유:

- Handler 책임 초과
- 테스트와 트랜잭션 경계 불명확
- 부분 실패·Timeout 정책 표현 어려움
조정 로직은 Facade 또는 Orchestrator로 이동한다.

3.13.4 Rule에서 원격 호출

```
@Component
class CustomerEligibilityRule {

    private final CampaignClient campaignClient;
}
```

금지 사유:

- Rule의 순수성 훼손
- 검증 결과가 네트워크 상태에 따라 달라짐
- 단위 테스트 어려움
- 예상하지 못한 Timeout 발생
3.13.5 다른 도메인 테이블 DML

```
UPDATE SV_CUSTOMER
   SET CUSTOMER_STATUS = 'D'
 WHERE CUSTOMER_NO = #{customerNo};
```

위 SQL을 IC·CM·CT Mapper에서 실행하는 것은 금지한다.

3.13.6 호출 체인 과다

```
A → B → C → D → E
```

문제:

- Timeout 누적
- 장애 원인 추적 어려움
- 중간 도메인 장애 전파
- 운영 책임 분산
온라인 동기 호출 깊이는 원칙적으로 2단계를 넘지 않도록 설계하고, 초과 시 Read Model·이벤트·조정 도메인을 검토한다.

3.14 연계 규칙

3.14.1 계층 호출 허용표

| 호출 주체 | 자기 Facade | 자기 Service | 다른 도메인 Contract | 다른 WAR Client | DAO | Mapper |
| --- | --- | --- | --- | --- | --- | --- |
| Handler | 허용 | 금지 | 금지 | 금지 | 금지 | 금지 |
| Facade | 해당 없음 | 허용 | 허용 | 허용 | 금지 | 금지 |
| Orchestrator | 제한 | 제한 | 허용 | 허용 | 금지 | 금지 |
| Service | 해당 없음 | 제한 | 예외 승인 | 허용된 Client만 | 자기 DAO 허용 | 금지 |
| Rule | 금지 | 금지 | 금지 | 금지 | 금지 | 금지 |
| DAO | 금지 | 금지 | 금지 | 금지 | 해당 없음 | 자기 Mapper 허용 |
| Mapper | 금지 | 금지 | 금지 | 금지 | 금지 | 해당 없음 |

핵심 기준

```
도메인 간 조정
= Facade 또는 Orchestrator

도메인 내부 업무 처리
= Service

데이터 접근
= 소유 도메인의 DAO·Mapper
```

3.14.2 ServiceId 호출 Header

| Header | 기준 |
| --- | --- |
| businessCode | 제공 업무코드 |
| serviceId | 제공 ServiceId |
| transactionCode | 제공 거래코드 |
| guid | 최초 거래의 GUID 유지 |
| traceId | 전체 호출 체인 유지 |
| parentServiceId | 호출자 ServiceId |
| callerBusinessCode | 소비 업무코드 |
| callerDomain | 소비 도메인 |
| channelId | 원 요청 채널 유지 |
| userId | 검증된 인증 문맥에서 생성 |
| branchId | 검증된 인증 문맥에서 생성 |
| requestTimestamp | 호출 생성시각 |
| contractVersion | 연동 계약 버전 |
| idempotencyKey | 상태변경 조건부 필수 |

클라이언트가 전달한 사용자 정보보다 검증된 AuthenticationContext를 우선한다.

3.14.3 오류 연계 규칙

- 제공 도메인의 업무 오류 의미를 불필요하게 시스템 오류로 변환하지 않는다.
- 소비 도메인은 필요한 경우 자기 도메인의 연동 오류코드로 매핑한다.
- 원본 오류코드와 제공 ServiceId는 로그에 보존한다.
- 사용자에게 내부 Class·SQL·Endpoint 정보를 노출하지 않는다.
- 동일 오류를 여러 계층에서 중복 로깅하지 않는다.
- 상위 거래로그와 하위 거래로그를 GUID·TraceId로 연결한다.
3.14.4 이벤트 연계 규칙

- 이벤트는 이미 발생한 업무 사실을 과거형으로 표현한다.
- 명령을 이벤트처럼 사용하지 않는다.
- 이벤트 payload는 소비자별 화면 DTO가 아닌 안정된 업무 계약으로 정의한다.
- 이벤트 발행 전 DB Commit 여부를 보장한다.
- eventId를 이용해 중복 처리를 방지한다.
- 이벤트 순서가 중요하면 Aggregate 단위 순서키를 사용한다.
- 이벤트 계약에서 필드 삭제·의미 변경을 금지한다.
- 개인정보는 최소화하거나 비식별 식별자로 전달한다.
정상 이벤트명:

```
CM.Campaign.Approved
SV.Customer.StatusChanged
CT.Contact.Registered
```

비권장 이벤트명:

```
ProcessCampaign
CallCustomerService
UpdateTable
```

3.15 데이터 및 상태관리

3.15.1 데이터 분류

| 데이터 유형 | 연동 방식 |
| --- | --- |
| 소유 데이터 | 소유 도메인의 계약을 통해 조회·변경 |
| 참조 기준정보 | 승인된 Cache·Snapshot·조회계약 |
| 통합 조회 데이터 | Read Model |
| 변경 이벤트 | Domain Event |
| 대용량 데이터 | 파일·배치·전용 조회저장소 |
| 민감정보 | 최소 필드·마스킹·감사 적용 |

3.15.2 Read Model 관리

Read Model은 여러 도메인의 데이터를 조회 목적에 맞게 복제·조합한 모델이다.

```
Customer Event ─┐
Product Event  ─┼→ CustomerOverview Read Model
Contact Event  ─┘
```

관리 기준

- Read Model은 원본 데이터의 소유자가 아니다.
- Read Model에서 원본 업무상태를 변경하지 않는다.
- 원본과의 동기화 지연시간을 정의한다.
- 재구축 절차를 마련한다.
- 데이터 기준시각을 응답에 제공할 수 있어야 한다.
- 민감정보 보관 범위와 기간을 별도로 승인받는다.
- 원본 삭제·정정 이벤트를 반영해야 한다.
3.15.3 상태변경 일관성

동일 도메인

```
Facade Transaction
→ 상태변경
→ Commit 또는 Rollback
```

여러 도메인

```
도메인 A Local Commit
→ Event
→ 도메인 B Local Commit
→ 실패 시 재시도 또는 보상
```

여러 도메인의 상태를 하나의 물리 DB 트랜잭션으로 묶는 것보다 상태별 업무 의미와 보상 절차를 명확히 하는 것을 우선한다.

3.15.4 Outbox 상태

| 상태 | 설명 |
| --- | --- |
| READY | 발행 대기 |
| PUBLISHING | 발행 처리 중 |
| PUBLISHED | 발행 성공 |
| RETRY | 재시도 대기 |
| FAILED | 재시도 초과 |
| CANCELLED | 관리자 취소 |

Outbox 정리·보관 기간과 재발행 절차를 운영기준으로 정의한다.

3.16 성능·용량·확장성

3.16.1 동기 호출 수 통제

온라인 거래 하나가 호출하는 원격 도메인 수는 최소화한다.

| 등급 | 원격 동기 호출 수 | 판단 |
| --- | --- | --- |
| 권장 | 0~2 | 일반 온라인 거래 |
| 주의 | 3~4 | 성능시험과 장애격리 검토 |
| 위험 | 5 이상 | Read Model·재설계 필요 |

3.16.2 N+1 원격 호출 금지

```
고객 100명 조회
  ↓
고객별 Product Service 100회 호출
```

금지한다.

대안:

- Bulk 조회 ServiceId
- 일괄 Query Contract
- Read Model
- 비동기 사전 구성
- Batch 조회
3.16.3 Bulk 계약

```
ProductHoldingResult findHoldings(
    List<String> customerNos
);
```

Bulk 호출 시 다음을 정의한다.

- 최대 건수
- 요청 크기
- 응답 크기
- 페이지 크기
- Timeout
- 부분 실패 표현
- 순서 보장 여부
3.16.4 자원 격리

- 원격 호출용 Thread Pool을 무제한으로 사용하지 않는다.
- 업무 WAR별·대상 시스템별 동시 호출 수를 제한한다.
- 제공 도메인별 Bulkhead 적용을 검토한다.
- Connection Pool과 HTTP Client Pool을 대상별로 구분한다.
- 이벤트 Consumer 동시성은 DB Pool·처리시간과 함께 산정한다.
- 재시도가 정상 요청보다 많은 부하를 만들지 않도록 제한한다.
3.16.5 확장 기준

신규 도메인을 추가할 때 기존 도메인의 내부 구현을 수정하지 않고 다음 항목만 확장할 수 있어야 한다.

```
신규 도메인 정의
→ 공개 계약 또는 ServiceId 등록
→ Client·Adapter 추가
→ Orchestrator 연결
→ 오류·Timeout 정책 등록
→ 테스트와 운영등록
```

3.17 보안·개인정보·감사

3.17.1 인증 문맥 전달

다른 WAR 호출에서도 다음 문맥을 유지한다.

- 인증 사용자 ID
- 지점 ID
- 채널 ID
- 역할·권한 식별자
- 최초 로그인·인증 출처
- GUID·TraceId
소비 도메인이 임의로 사용자 ID나 권한을 생성하지 않는다.

3.17.2 권한검증 책임

| 검증 | 책임 |
| --- | --- |
| 토큰 서명·만료 | Gateway 또는 JWT Filter |
| 공통 인증 문맥 | STF |
| 소비 거래 실행권한 | 소비 도메인 |
| 제공 기능 접근권한 | 제공 도메인 |
| 데이터 범위 권한 | 데이터 소유 도메인 |
| 개인정보 마스킹 | 제공·소유 도메인 |
| 최종 화면 노출 | 소비 도메인·UI |

상위 도메인에서 권한을 검증했더라도 제공 도메인의 데이터권한 검증을 생략하지 않는다.

3.17.3 개인정보 전달

- 연동 목적에 필요한 최소 필드만 전달한다.
- 주민등록번호·계좌번호 등 민감정보 원문 전달을 최소화한다.
- 불필요한 개인정보를 이벤트 payload에 포함하지 않는다.
- 로그에는 마스킹 또는 Hash 값을 기록한다.
- 데이터 분류등급을 계약 관리대장에 등록한다.
- 개인정보 조회·다운로드는 제공 도메인에서 감사로그를 기록한다.
- Read Model에 민감정보를 복제할 때 별도 승인과 보관기간을 적용한다.
3.17.4 감사로그

다음 연동은 감사 대상으로 지정한다.

- 고객 개인정보 조회
- 개인정보 변경
- 권한·사용자 상태 변경
- 대량 조회·다운로드
- 캠페인 승인·취소
- 관리자 강제 처리
- 다른 도메인 데이터 변경 요청
- 관리자 이벤트 재처리
감사로그에는 소비 ServiceId와 제공 ServiceId를 함께 기록한다.

3.18 운영·모니터링·장애 대응

3.18.1 필수 모니터링 지표

| 영역 | 지표 |
| --- | --- |
| 호출량 | 소비→제공 도메인별 요청 건수 |
| 응답시간 | 평균·p95·p99 |
| 오류율 | 업무·시스템·Timeout 오류율 |
| 재시도 | 최초 요청 대비 재시도 비율 |
| Circuit | Open·Half-Open 상태 |
| 동시호출 | 대상별 Active 호출 수 |
| Queue | 이벤트 대기·재시도·DLQ 건수 |
| Read Model | 동기화 지연시간 |
| 계약 | 버전별 호출 건수 |
| 데이터 | 부분 성공·누락 건수 |

3.18.2 연동 로그

```
timestamp
guid
traceId
spanId
parentServiceId
callerBusinessCode
callerDomain
targetBusinessCode
targetDomain
targetServiceId
contractVersion
callType
startTime
endTime
elapsedMs
resultCode
originalErrorCode
retryCount
circuitState
```

요청·응답 원문은 기본적으로 기록하지 않고 필요한 식별자와 크기만 기록한다.

3.18.3 장애 분석 순서

```
상위 ServiceId 장애 확인
  ↓
하위 호출 목록 확인
  ↓
어느 도메인에서 지연·오류가 발생했는지 확인
  ↓
Timeout·Connection·Thread 상태 확인
  ↓
제공 ServiceId 거래로그 확인
  ↓
SQL·외부연계 확인
  ↓
필수·선택 의존 정책에 따른 조치
```

3.18.4 운영 조치

| 장애 | 즉시 조치 |
| --- | --- |
| 제공 도메인 Timeout | Circuit·호출량·Slow 거래 확인 |
| 제공 WAR 중단 | 필수 호출 차단 또는 선택 기능 대체 |
| 계약 버전 오류 | 배포버전과 계약 호환성 확인 |
| 이벤트 적체 | Consumer 상태·DB Pool·처리시간 확인 |
| DLQ 증가 | 오류유형 분류 후 재처리 승인 |
| Read Model 지연 | 이벤트 발행·소비 지연 구간 확인 |
| 중복 처리 | Idempotency Key와 Consumer 처리이력 확인 |
| 순환 호출 의심 | Trace 호출 그래프 확인 |

3.19 자동검증 및 품질 Gate

3.19.1 패키지 의존성 검증

다음 항목을 ArchUnit 또는 동등 도구로 검증한다.

```
@ArchTest
static final ArchRule noCrossDomainMapperAccess =
    noClasses()
        .that().resideOutsideOfPackage(
            "..customer.."
        )
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "..customer.dao..",
            "..customer.mapper.."
        );
```

추가 검증 대상:

- Handler → 자기 Facade 외 직접 호출 금지
- Handler → DAO·Mapper·Client 금지
- Rule → DAO·Mapper·Client 금지
- 다른 도메인 Mapper 의존 금지
- 다른 도메인 내부 Service 구현 의존 금지
- 다른 WAR 패키지 의존 금지
- 도메인 간 순환 의존 금지
- 계약 패키지 외부 공개 금지
기존 도메인 설계기준도 다른 도메인의 Mapper 의존, WAR 간 클래스 직접 의존, 순환 의존을 자동검증 대상으로 정의한다.

3.19.2 Gradle 의존성 검증

```
허용:
ic-service
  → tcf-core
  → tcf-web
  → tcf-eai
  → 승인된 contract 모듈

금지:
ic-service
  → sv-service
  → cm-service
```

업무 WAR 간 implementation project() 의존이 발견되면 빌드를 실패시킨다.

3.19.3 데이터 소유권 검증

Mapper SQL을 분석하여 다음을 확인한다.

| 검증 | 결과 |
| --- | --- |
| 자기 소유 테이블 DML | 허용 |
| 승인된 참조 테이블 SELECT | 조건부 허용 |
| 다른 도메인 테이블 DML | 실패 |
| 미등록 테이블 사용 | 실패 |
| 승인되지 않은 Cross-Domain Join | 실패 |
| 승인된 Read View | 승인번호 확인 |
| 민감 테이블 조회 | 권한·감사 등록 확인 |

3.19.4 계약 정합성 검증

```
연동 관리대장
↔ ServiceId 또는 Event Type
↔ Request·Response Schema
↔ Client Adapter
↔ Provider Handler
↔ OM Catalog
↔ Timeout 정책
↔ 오류 매핑표
↔ 계약 테스트
↔ 운영 대시보드
```

하나라도 누락되면 운영 반영 Gate를 통과하지 못한다.

3.19.5 품질 Gate

| Gate | 검증 기준 |
| --- | --- |
| 설계 Gate | 소비·제공 도메인, 연동방식, 데이터 소유권 승인 |
| 개발 Gate | 계층·패키지·직접 참조 위반 없음 |
| 계약 Gate | 제공자·소비자 계약 테스트 성공 |
| 보안 Gate | 인증·권한·개인정보·감사 검토 완료 |
| 성능 Gate | p95·Timeout·호출 수 기준 충족 |
| 장애 Gate | Timeout·Circuit·재시도·DLQ 시험 성공 |
| 배포 Gate | 계약 버전과 배포 순서 호환 |
| 운영 Gate | 모니터링·알림·Runbook·담당자 등록 |

3.20 테스트 시나리오

| 번호 | 테스트 시나리오 | 기대 결과 |
| --- | --- | --- |
| 1 | 동일 WAR 공개 Query Contract 호출 | 제공 Facade를 통해 정상 조회 |
| 2 | 다른 도메인 Mapper 직접 참조 | ArchUnit 실패 |
| 3 | 다른 WAR Java 모듈 의존 | Gradle Gate 실패 |
| 4 | 도메인 A↔B 순환 의존 | 구조검증 실패 |
| 5 | 다른 도메인 소유 테이블 UPDATE | SQL Gate 실패 |
| 6 | 정상 ServiceId 원격 호출 | GUID·TraceId 유지 |
| 7 | 제공 도메인 업무 오류 | 정의된 소비 오류로 변환 |
| 8 | 제공 도메인 시스템 오류 | 원본 오류는 로그, 표준 오류 반환 |
| 9 | 하위 호출 Timeout | 상위 Timeout 내 빠른 실패 |
| 10 | 선택 의존 Timeout | 부분 성공과 Warning 반환 |
| 11 | 필수 의존 Timeout | 전체 거래 실패 |
| 12 | 조회 호출 1회 재시도 성공 | 중복 문제 없이 성공 |
| 13 | 상태변경 Timeout 후 재호출 | Idempotency Key로 중복 차단 |
| 14 | Circuit Open | 원격 호출 없이 즉시 대체·실패 |
| 15 | 이벤트 정상 발행 | Outbox와 Broker 상태 일치 |
| 16 | 동일 이벤트 중복 수신 | 한 번만 업무 처리 |
| 17 | 이벤트 소비 반복 실패 | DLQ 이동 |
| 18 | DLQ 관리자 재처리 | 감사로그와 결과 기록 |
| 19 | Read Model 갱신 지연 | 지연 지표와 알림 발생 |
| 20 | 구버전 소비자·신버전 제공자 | 하위호환 유지 |
| 21 | 필수 필드 삭제 계약 변경 | 호환성 Gate 실패 |
| 22 | 개인정보 필드 과다 전달 | 보안 Gate 실패 |
| 23 | 원격 N+1 호출 | 성능검사 또는 코드리뷰 실패 |
| 24 | 3개 도메인 병렬 조회 | Context·MDC 정상 전달 |
| 25 | 하위 도메인 장애 복구 | Circuit Half-Open 후 정상 전환 |

3.21 체크리스트

3.21.1 설계 체크리스트

| 점검 | 확인 |
| --- | --- |
| 소비·제공 도메인이 명확한가 | □ |
| 연동 목적이 조회·변경·이벤트 중 하나로 정의되었는가 | □ |
| 동일 WAR와 다른 WAR 방식이 구분되었는가 | □ |
| 제공 도메인의 공개 계약이 정의되었는가 | □ |
| 데이터 소유 도메인이 지정되었는가 | □ |
| 다른 도메인 테이블 DML이 없는가 | □ |
| 동기·비동기 선택 근거가 있는가 | □ |
| 필수·선택 의존이 정의되었는가 | □ |
| Timeout Budget이 계산되었는가 | □ |
| 재시도·멱등성 기준이 정의되었는가 | □ |
| 부분 성공 기준이 정의되었는가 | □ |
| 보상 처리 또는 최종 일관성이 정의되었는가 | □ |
| 오류코드 매핑이 정의되었는가 | □ |
| 계약 버전이 정의되었는가 | □ |
| 개인정보 전달 범위가 최소화되었는가 | □ |

3.21.2 구현 체크리스트

| 점검 | 확인 |
| --- | --- |
| Handler가 자기 Facade만 호출하는가 | □ |
| 조정 로직이 Facade·Orchestrator에 있는가 | □ |
| Rule이 외부 의존성을 갖지 않는가 | □ |
| 다른 도메인의 DAO·Mapper를 참조하지 않는가 | □ |
| 다른 WAR를 Java Import하지 않는가 | □ |
| 원격 호출이 Client Interface로 추상화되었는가 | □ |
| Adapter에서 계약 변환을 수행하는가 | □ |
| GUID·TraceId를 전달하는가 | □ |
| 하위 Timeout이 상위 Timeout보다 짧은가 | □ |
| 상태변경에 Idempotency Key가 적용되었는가 | □ |
| 이벤트 Outbox 또는 동등한 보장수단이 있는가 | □ |
| 계약 테스트가 작성되었는가 | □ |
| 호출 수와 N+1 문제가 검토되었는가 | □ |
| 민감정보가 로그에 기록되지 않는가 | □ |

3.21.3 운영 체크리스트

| 점검 | 확인 |
| --- | --- |
| 소비→제공 호출량을 확인할 수 있는가 | □ |
| 제공 ServiceId 응답시간을 확인할 수 있는가 | □ |
| 원본 오류코드와 TraceId를 조회할 수 있는가 | □ |
| Circuit 상태를 확인할 수 있는가 | □ |
| 재시도율을 확인할 수 있는가 | □ |
| 이벤트 적체와 DLQ를 확인할 수 있는가 | □ |
| Read Model 지연시간을 확인할 수 있는가 | □ |
| 장애 담당 도메인과 조직이 등록되어 있는가 | □ |
| 장애 Runbook이 존재하는가 | □ |
| 구버전 계약 사용 현황을 확인할 수 있는가 | □ |

3.22 변경·호환성·폐기 관리

3.22.1 계약 변경 유형

| 변경 | 호환성 | 처리 |
| --- | --- | --- |
| 선택 필드 추가 | 일반적으로 호환 | Minor 버전 |
| 필수 필드 추가 | 비호환 가능 | 신규 버전 권장 |
| 필드 삭제 | 비호환 | Major 버전 |
| 필드명 변경 | 비호환 | 신규 필드 추가 후 단계 폐기 |
| 데이터 타입 변경 | 비호환 | 신규 버전 |
| Enum 값 추가 | 소비자 구현에 따라 위험 | 계약 테스트 필수 |
| 오류코드 추가 | 조건부 호환 | 소비자 기본 처리 확인 |
| 의미 변경 | 비호환 | 신규 필드·신규 버전 |
| ServiceId 변경 | 비호환 | 신규 ServiceId 생성 |
| Event Type 변경 | 비호환 | 신규 이벤트 발행 |

3.22.2 변경 절차

```
변경 요청
  ↓
소비자 목록 조회
  ↓
호환성 분석
  ↓
AA·제공·소비 도메인 검토
  ↓
신규 계약 또는 하위호환 결정
  ↓
Provider 구현
  ↓
계약 테스트
  ↓
소비자 전환
  ↓
구버전 호출량 확인
  ↓
폐기 승인
```

3.22.3 배포 순서

하위호환 변경

```
Provider 선배포
→ 소비자 순차 배포
```

비호환 변경

```
Provider V1 + V2 동시 지원
→ 소비자 V2 전환
→ V1 호출 0건 확인
→ V1 폐기
```

소비자보다 Provider 구버전을 먼저 제거하지 않는다.

3.22.4 폐기 기준

계약을 폐기하려면 다음 조건을 모두 충족해야 한다.

- 등록된 모든 소비 도메인이 전환을 완료했다.
- 일정 기간 구버전 호출이 0건이다.
- 배치·비정기 호출 여부를 확인했다.
- 계약 테스트와 운영 설정을 제거했다.
- Service Catalog에 폐기 상태를 등록했다.
- 보관해야 할 로그·감사·이벤트 기간을 확인했다.
- 롤백 필요 기간이 종료되었다.
- AA와 제공·소비 도메인 소유자가 승인했다.
4. 시사점

4.1 핵심 아키텍처 판단

판단 1. 도메인 간 연동의 기본 단위는 클래스가 아니라 계약이다

```
동일 WAR
→ Application Contract

다른 WAR
→ ServiceId Contract

비동기
→ Event Contract
```

구현 클래스와 테이블을 계약으로 사용해서는 안 된다.

판단 2. 여러 도메인의 조합 책임은 Facade·Orchestrator에 둔다

Handler와 Rule은 도메인 조정 책임을 갖지 않는다. 도메인 간 호출 순서, 부분 성공, 보상, Timeout Budget은 애플리케이션 조정 계층에서 관리한다.

판단 3. 데이터 변경권한은 하나의 소유 도메인만 갖는다

다른 도메인은 데이터를 직접 변경하지 않고 소유 도메인의 Command 계약이나 ServiceId를 호출해야 한다.

판단 4. 다른 WAR 연동은 항상 원격 호출로 취급한다

현재 같은 Tomcat이나 같은 JVM에 배포되어 있더라도 업무 WAR는 독립 경계다. Java Import나 공유 메모리에 의존하지 않고 표준 전문과 ServiceId를 사용해야 한다.

판단 5. 통합 조회는 Read Model을 우선 검토한다

여러 도메인을 반복적으로 동기 호출하는 구조는 응답시간과 장애 범위를 증가시킨다. 빈번한 종합조회는 Read Model을 이용해 조회 부하와 결합을 분리한다.

4.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 공개 계약 없이 Service 직접 호출 | 내부 변경이 전체 도메인으로 전파 |
| 다른 도메인 테이블 직접 접근 | 데이터 소유권·업무규칙·감사 훼손 |
| WAR 간 Java 의존 | 독립 배포 불가와 순환 의존 |
| 다단계 동기 호출 | Timeout·Thread·Pool 고갈 |
| 무분별한 재시도 | 중복 처리와 장애 부하 증폭 |
| 계약 버전 미관리 | 배포 시점 장애 |
| 이벤트 중복방지 미적용 | 동일 업무 여러 번 처리 |
| Read Model 지연 미관리 | 화면 데이터 정합성 논란 |
| 부분 성공 기준 미정의 | 사용자·운영 판단 불일치 |
| 도메인 소유자 부재 | 변경·장애 책임 불명확 |

4.3 우선 보완 과제

| 우선순위 | 과제 |
| --- | --- |
| 1 | 전체 업무 도메인과 데이터 소유권 표 작성 |
| 2 | 현행 Cross-Domain Service·DAO·Mapper 참조 분석 |
| 3 | 업무 WAR 간 Gradle 직접 의존 제거 |
| 4 | 동일 WAR 공개 Contract 패키지 표준화 |
| 5 | tcf-eai ServiceId Client 템플릿 제공 |
| 6 | 연동 관리대장과 오류 매핑표 작성 |
| 7 | Timeout Budget과 필수·선택 의존 정의 |
| 8 | 도메인 간 순환 의존 ArchUnit 적용 |
| 9 | 통합 조회 후보에 대한 Read Model 검토 |
| 10 | 이벤트 Outbox·Idempotency·DLQ 기준 구현 |
| 11 | 계약 테스트를 CI/CD Gate에 포함 |
| 12 | OM에 도메인 호출관계·응답시간 화면 추가 |

4.4 중장기 발전 방향

```
1단계
도메인·소유권·계약 정리

2단계
직접 DAO·Mapper·WAR 의존 제거

3단계
ServiceId Client와 Contract 표준화

4단계
통합조회 Read Model 확대

5단계
이벤트·Outbox·보상 처리 적용

6단계
도메인 호출 그래프와 자동 영향분석

7단계
계약 버전·호환성·폐기 자동관리
```

장기적으로 OM에서 다음 관계를 조회할 수 있어야 한다.

```
소비 ServiceId
→ 소비 도메인
→ Client
→ 제공 업무 WAR
→ 제공 ServiceId
→ 제공 도메인
→ 데이터 소유권
→ Timeout·오류율
→ 계약 버전
→ 담당 조직
```

5. 마무리말

업무 도메인 간 연동 설계의 목표는 도메인을 서로 호출하지 못하게 만드는 것이 아니다.

핵심은 도메인이 서로 협력하되, 상대 도메인의 내부 구조와 데이터 저장방식을 알지 않아도 되도록 만드는 것이다.

```
도메인은 내부 구현을 보호하고,
공개 계약으로 기능을 제공한다.

동일 WAR는 Application Contract로 연결하고,
다른 WAR는 ServiceId로 연결한다.

즉시 결과가 필요하지 않으면 이벤트로 연결하고,
반복되는 통합조회는 Read Model로 분리한다.

데이터는 소유 도메인만 변경하고,
모든 연동은 추적·통제·호환·폐기 가능해야 한다.
```

이 기준이 적용되어야 NSIGHT TCF의 Handler → Facade → Service → Rule → DAO → Mapper 계층구조가 도메인 경계를 넘어설 때도 훼손되지 않으며, 업무 WAR가 증가하더라도 변경 영향·장애 범위·운영 책임을 통제할 수 있다.

