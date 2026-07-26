TCF 개발방법론 — 패키지 구조 설계기준

1. 도입 전 안내말

TCF 패키지 구조는 Java 클래스를 폴더별로 정리하기 위한 단순 분류 기준이 아니다.

패키지 구조는 다음 사항을 코드 수준에서 강제하는 아키텍처 통제 수단이다.

```
업무코드와 업무 WAR의 경계
→ 업무 도메인의 책임 범위
→ Handler·Facade·Service·Rule·DAO·Mapper 계층
→ 허용되는 호출 방향
→ 트랜잭션 경계
→ 외부 시스템 연계 경계
→ 화면·ServiceId·프로그램·SQL 추적성
→ 운영 장애의 영향 범위
→ 자동검증과 품질 Gate
```

NSIGHT TCF의 온라인 거래는 공통 OnlineTransactionController에서 시작하여 TCF → STF → Dispatcher → Handler → Facade → Service → Rule·DAO → Mapper → ETF 순서로 실행된다. 업무 패키지는 이 실행 흐름과 동일한 책임 구조를 가져야 한다.

따라서 업무 개발자가 임의로 controller, service, mapper를 만들거나, 모든 클래스를 하나의 common 패키지에 모으는 방식은 허용하지 않는다.

본 기준의 핵심 원칙은 다음과 같다.

```
패키지는 기술 이름만으로 나누지 않는다.

업무코드
  ↓
업무 도메인
  ↓
책임 계층

순서로 구성한다.
```

최종 권장 패키지 형식은 다음과 같다.

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

2. 문서 개요

2.1 목적

본 기준서의 목적은 NSIGHT TCF 기반 애플리케이션의 패키지 구조와 패키지 간 의존관계를 표준화하는 것이다.

| 목적 | 설명 |
| --- | --- |
| 업무 경계 명확화 | 업무코드와 업무 도메인의 소유 범위 식별 |
| 계층 책임 분리 | Handler부터 Mapper까지 역할 혼합 방지 |
| 의존성 통제 | 상위 계층에서 하위 계층으로만 호출 |
| 변경 영향 축소 | 특정 도메인 변경이 다른 도메인에 미치는 영향 최소화 |
| 추적성 확보 | ServiceId, 프로그램, SQL, DB 객체 연결 |
| 테스트 용이성 | 도메인 및 계층별 독립 테스트 가능 |
| 운영성 강화 | 장애 발생 프로그램과 업무 영역 즉시 식별 |
| 확장성 확보 | 17개 업무 WAR와 신규 도메인 추가에 대응 |
| 자동검증 | ArchUnit, Checkstyle, CI/CD를 통한 구조 검증 |
| 기술부채 방지 | common, util, impl 패키지의 무분별한 확대 방지 |

2.2 적용범위

본 기준은 다음 영역에 적용한다.

| 적용 대상 | 적용 내용 |
| --- | --- |
| 업무 WAR | SV, IC, PC, BC, MS, PD, CM, EB, EP, BP, BD, SS, CS, CT, MG, OM 등 |
| 플랫폼 모듈 | tcf-core, tcf-web, tcf-gateway, tcf-jwt, tcf-eai, tcf-cache, tcf-batch |
| 온라인 프로그램 | Handler, Facade, Service, Rule, DAO, Mapper |
| 데이터 객체 | Request, Response, Command, Query, Result DTO |
| 외부 연계 | Client, Adapter, 연계 전문 DTO |
| 배치 | Job, Step, Tasklet, Reader, Processor, Writer |
| 파일 처리 | Upload, Download, File Metadata |
| 설정 | Spring Configuration, Properties, Security, MyBatis |
| 테스트 소스 | 단위·통합·아키텍처 테스트 패키지 |
| 리소스 | Mapper XML, SQL, 메시지, 설정 파일 |

2.3 적용 제외

다음은 본 업무 패키지 구조의 직접 적용 대상에서 제외하지만, 별도 플랫폼 패키지 기준을 적용한다.

| 제외 대상 | 담당 기준 |
| --- | --- |
| UI React·WEBTOPSUITE 소스 | 화면 개발 표준 |
| DB 물리 스키마 | DB 객체 명명규칙 |
| 인프라 스크립트 | 배포·운영 스크립트 표준 |
| CI/CD Pipeline | DevOps 저장소 구조 표준 |
| 외부 제품 내부 패키지 | 해당 제품 표준 |
| 생성 코드 | 생성 도구의 공식 패키지 구조 |

2.4 대상 독자

| 대상 | 활용 목적 |
| --- | --- |
| 애플리케이션 아키텍트 | 패키지 구조와 의존관계 승인 |
| TCF 프레임워크팀 | 플랫폼 모듈 구조 관리 |
| 업무 개발팀 | 업무 프로그램 생성 기준 |
| 코드 리뷰어 | 계층 위반과 책임 혼합 검토 |
| 테스트팀 | 패키지 단위 테스트 범위 정의 |
| DevOps팀 | 구조 자동검증과 품질 Gate 적용 |
| 운영팀 | 장애 프로그램과 업무 영역 식별 |
| PMO·품질팀 | 설계서와 실제 소스 정합성 확인 |

2.5 선행조건

패키지를 생성하기 전에 다음 항목이 확정되어야 한다.

| 선행조건 | 확인 기준 |
| --- | --- |
| 업무코드 | WAR 및 Context Path에 대응하는 코드 확정 |
| 업무 도메인 | 고객, 캠페인, 상품 등 책임 범위 정의 |
| ServiceId | {업무코드}.{도메인}.{행위} 형식 확정 |
| 프로그램 계층 | Handler·Facade·Service·Rule·DAO·Mapper 적용 |
| 데이터 소유권 | 테이블 및 외부 시스템 소유 업무 확인 |
| 연계 방식 | 직접 의존 또는 tcf-eai 호출 여부 결정 |
| 트랜잭션 | Facade 기준 트랜잭션 경계 확정 |
| 예외체계 | 공통 오류와 업무 오류 구분 |
| 명명규칙 | 클래스·메서드·DTO·Mapper 기준 확정 |
| 모듈 관계 | 업무 WAR와 공통 모듈 간 Gradle 의존관계 확정 |

2.6 용어 정의

| 용어 | 정의 |
| --- | --- |
| 기본 패키지 | 시스템 전체의 최상위 Java 패키지 |
| 업무코드 패키지 | SV, IC, CM 등 업무 WAR를 식별하는 패키지 |
| 도메인 패키지 | 고객, 캠페인, 상품 등 업무 책임 단위 |
| 계층 패키지 | Handler, Facade, Service 등 기술적 책임 단위 |
| 플랫폼 패키지 | TCF 공통 실행·인증·연계·운영 기능 패키지 |
| 내부 공통 | 동일 업무 WAR 내부에서만 공유되는 기능 |
| 플랫폼 공통 | 여러 업무 WAR가 공통으로 사용하는 표준 기능 |
| 순환 의존 | 두 패키지가 서로를 참조하는 구조 |
| 의존성 역전 | 상위 정책이 구체 구현 대신 인터페이스에 의존하는 구조 |
| 어댑터 | 외부 기술 또는 시스템을 내부 표준으로 변환하는 구성요소 |
| 패키지 응집도 | 하나의 패키지가 하나의 관련 책임에 집중하는 정도 |

3. 문제 정의 및 설계 배경

3.1 패키지 구조가 필요한 이유

패키지 기준이 없으면 개발자는 클래스 이름만 보고 임의의 위치에 프로그램을 배치하게 된다.

```
com.nh.nsight.sv
├─ controller
├─ service
├─ service2
├─ common
├─ common2
├─ util
├─ helper
├─ manager
├─ mapper
└─ test
```

이 구조는 초기에는 단순해 보이지만 업무가 증가하면 다음 문제가 발생한다.

| 문제 | 결과 |
| --- | --- |
| 도메인 경계 부재 | 고객·캠페인·상품 프로그램이 한 패키지에 혼재 |
| common 확대 | 소유자와 변경 책임 불명확 |
| 계층 우회 | Handler가 Mapper를 직접 호출 |
| 클래스 충돌 | 동일한 이름의 Service·DTO가 반복 생성 |
| 변경 영향 확대 | 한 업무 수정이 전체 WAR에 영향 |
| 테스트 어려움 | 업무 단위 테스트 범위 식별 곤란 |
| 순환 의존 | Service 간 상호 호출과 재귀적 의존 발생 |
| 운영 추적성 저하 | 오류 클래스가 어느 업무에 속하는지 식별 곤란 |
| 신규 개발자 혼선 | 파일을 어디에 생성해야 하는지 판단 불가 |
| 코드 리뷰 편차 | 리뷰어마다 서로 다른 구조 요구 |

3.2 TCF 구조와 패키지 구조의 정합성

TCF의 프로그램 실행 계층은 다음과 같다.

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

각 계층은 단일 책임을 가지며, Handler는 ServiceId 분기와 Facade 호출, Facade는 유스케이스와 트랜잭션, Service는 업무 흐름, Rule은 부작용 없는 업무 검증, DAO와 Mapper는 데이터 접근을 담당한다.

따라서 물리적인 패키지 구조도 이 책임을 그대로 표현해야 한다.

3.3 패키지 구조의 설계 목표

```
소스 위치만 보아도 다음을 판단할 수 있어야 한다.

1. 어느 업무 WAR인가
2. 어느 업무 도메인인가
3. 어떤 책임의 프로그램인가
4. 어떤 프로그램을 호출할 수 있는가
5. 트랜잭션을 선언할 수 있는가
6. DB나 외부 시스템에 접근할 수 있는가
7. 다른 업무에서 직접 참조할 수 있는가
8. 장애 발생 시 어느 팀이 담당하는가
```

4. 현행 구조와 문제점

4.1 계층 우선 구조

```
com.nh.nsight.marketing.sv
├─ handler
│  ├─ SvCustomerHandler
│  ├─ SvCampaignHandler
│  └─ SvProductHandler
├─ service
│  ├─ SvCustomerService
│  ├─ SvCampaignService
│  └─ SvProductService
└─ mapper
   ├─ SvCustomerMapper
   ├─ SvCampaignMapper
   └─ SvProductMapper
```

문제점

- 하나의 도메인 변경 파일이 여러 패키지에 분산된다.
- 고객 도메인의 전체 프로그램을 한 번에 파악하기 어렵다.
- 업무가 증가할수록 패키지당 클래스 수가 과도하게 증가한다.
- 도메인별 개발팀 소유권을 설정하기 어렵다.
- 모듈 분리 시 재구성 비용이 크다.
4.2 업무별 Controller 구조

```
com.nh.nsight.marketing.sv.customer
├─ controller
├─ service
└─ mapper
```

문제점

업무별 Controller를 허용하면 공통 TCF Endpoint, STF, 거래통제, Timeout, 거래로그, ETF를 우회할 수 있다.

```
금지 구조

화면
  → SvCustomerController
  → SvCustomerService
  → SvCustomerMapper
```

업무 온라인 거래의 진입점은 공통 OnlineTransactionController이며, 업무 프로그램의 최초 진입 계층은 Handler다. TCF는 업무 Controller를 별도로 생성하지 않는 구조를 기본 원칙으로 한다.

4.3 과도한 공통 패키지

```
com.nh.nsight.marketing.common
├─ CommonService
├─ CommonUtil
├─ CommonDto
├─ CommonManager
└─ CommonHelper
```

문제점

| 문제 | 설명 |
| --- | --- |
| 소유권 부재 | 어느 팀이 변경 승인하는지 불명확 |
| 결합도 증가 | 모든 업무가 공통 패키지에 의존 |
| 배포 영향 확대 | 작은 변경에도 모든 WAR 재검증 필요 |
| 의미 상실 | CommonService 이름만으로 책임 식별 불가 |
| 순환 의존 | 공통이 다시 업무 구현에 의존할 가능성 |
| 무분별한 재사용 | 유사하지만 다른 업무 개념을 억지로 통합 |

공통화는 동일한 이름이 두 번 등장했기 때문이 아니라, 다음 조건을 모두 만족할 때만 허용한다.

```
의미가 동일하다.
변경 주기가 동일하다.
소유 조직이 동일하다.
모든 사용자가 동일한 계약을 요구한다.
업무별 예외가 거의 없다.
독립적으로 테스트할 수 있다.
```

5. 요구사항과 제약조건

5.1 기능 요구사항

| ID | 요구사항 |
| --- | --- |
| PKG-FR-01 | 업무코드와 도메인을 패키지에서 식별할 수 있어야 한다. |
| PKG-FR-02 | Handler부터 Mapper까지 표준 계층을 표현해야 한다. |
| PKG-FR-03 | 온라인, 배치, 파일, 외부 연계를 구분할 수 있어야 한다. |
| PKG-FR-04 | Request·Response·Query·Result DTO를 구분해야 한다. |
| PKG-FR-05 | 업무별 예외와 공통 예외를 구분해야 한다. |
| PKG-FR-06 | Mapper Interface와 XML 경로를 추적할 수 있어야 한다. |
| PKG-FR-07 | 테스트 패키지가 운영 패키지를 동일하게 반영해야 한다. |
| PKG-FR-08 | 신규 업무 WAR와 도메인을 동일한 패턴으로 추가할 수 있어야 한다. |

5.2 비기능 요구사항

| 항목 | 기준 |
| --- | --- |
| 응집도 | 하나의 도메인 패키지는 단일 업무 책임에 집중 |
| 결합도 | 업무 도메인 간 직접 참조 최소화 |
| 가독성 | 패키지 경로로 클래스 역할 식별 |
| 확장성 | 도메인 추가가 기존 패키지 변경을 요구하지 않음 |
| 테스트성 | 계층별 단위 테스트와 도메인별 통합 테스트 가능 |
| 운영성 | 로그의 클래스명으로 업무·도메인·계층 식별 |
| 보안성 | 인증·권한 기능을 업무 패키지에서 임의 구현하지 않음 |
| 자동화 | 패키지 규칙을 CI/CD에서 검증 가능 |
| 호환성 | 패키지 이동에 따른 영향 분석과 이력 관리 가능 |

5.3 제약조건

- 모든 Java 패키지명은 소문자로 작성한다.
- _, -, 공백, 한글을 패키지명에 사용하지 않는다.
- 업무코드는 승인된 2자리 소문자 코드만 사용한다.
- 업무 온라인 패키지에 controller를 생성하지 않는다.
- Handler가 DAO·Mapper·Client를 직접 호출하지 않는다.
- Rule은 DAO·Mapper·Client·Cache에 의존하지 않는다.
- DAO는 업무 Service 또는 Rule에 의존하지 않는다.
- Mapper는 상위 업무 계층을 참조하지 않는다.
- 서로 다른 업무 WAR의 구현 클래스를 직접 참조하지 않는다.
- 트랜잭션은 원칙적으로 Facade에만 선언한다.
- 패키지명에 환경명 dev, stg, prod를 넣지 않는다.
- 패키지명에 버전 v1, v2를 일반적으로 넣지 않는다.
- impl 패키지를 일률적으로 생성하지 않는다.
- common, util, helper, manager 패키지는 승인 없이 생성하지 않는다.
- Spring의 전체 com.nh 패키지 스캔을 금지한다.
6. 설계 원칙

6.1 도메인 우선, 계층 후순위 원칙

패키지는 기술 계층보다 업무 도메인을 먼저 구분한다.

```
권장

업무코드
  ↓
도메인
  ↓
계층
com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.mapper
```

다음과 같은 계층 우선 구조는 대규모 업무 WAR의 표준으로 사용하지 않는다.

```
비권장

com.nh.nsight.marketing.sv.handler.customer
com.nh.nsight.marketing.sv.service.customer
com.nh.nsight.marketing.sv.mapper.customer
```

6.2 단방향 의존 원칙

```
handler
  ↓
facade
  ↓
service
  ├─ rule
  ├─ dao
  │   ↓
  │ mapper
  └─ client
```

| 호출 주체 | 허용 대상 | 금지 대상 |
| --- | --- | --- |
| Handler | Facade, Request DTO | DAO, Mapper, Client |
| Facade | Service, DTO | Mapper, Controller |
| Service | Rule, DAO, Client, DTO | Handler, Controller |
| Rule | DTO, 값 객체, 순수 Utility | DAO, Mapper, Client, Cache |
| DAO | Mapper, Query·Result DTO | Service, Rule, Handler |
| Mapper | Query·Result DTO | 모든 상위 업무 계층 |
| Client | 외부 전문 DTO, 공통 연계 모듈 | Handler, Mapper |
| DTO | 표준 Java 타입, 값 객체 | Service, DAO, Spring Bean |

6.3 패키지 간 순환 의존 금지

다음 구조는 금지한다.

```
customer.service
  → grade.service
      → customer.service
```

순환이 발생하면 다음 중 하나를 선택한다.

- 공통된 상위 업무 기능을 새로운 도메인 Service로 분리한다.
- 호출 방향을 한쪽으로 고정한다.
- 이벤트 또는 표준 연계로 전환한다.
- 인터페이스를 소유 도메인에 두고 구현체를 어댑터로 분리한다.
- 서로 다른 WAR라면 tcf-eai 표준 호출로 전환한다.
6.4 패키지 공개 범위 최소화

다른 패키지에서 사용할 필요가 없는 클래스는 public으로 선언하지 않는다.

| 대상 | 공개 기준 |
| --- | --- |
| Handler | Dispatcher 등록을 위해 public |
| Facade | Handler 호출을 위해 public |
| Service | 같은 도메인 Facade에서만 사용하면 제한적 공개 검토 |
| Rule | 동일 도메인 내부 사용을 원칙 |
| DAO | 동일 도메인 Service 내부 사용 |
| Mapper | MyBatis Proxy 생성을 위해 Interface 공개 |
| 내부 변환기 | package-private 우선 |
| 내부 상수 | package-private 우선 |

6.5 인터페이스 남용 금지

다음과 같은 일률적인 구조를 만들지 않는다.

```
service
├─ CustomerService.java
└─ impl
   └─ CustomerServiceImpl.java
```

구현체가 하나이고 교체 가능성이 없다면 클래스로 직접 정의한다.

인터페이스는 다음 경우에만 사용한다.

- 구현체가 둘 이상 존재한다.
- 외부 시스템 또는 기술 어댑터를 교체해야 한다.
- 모듈 간 공개 계약을 정의한다.
- 테스트 대체 구현이 구조적으로 필요하다.
- 업무 정책과 기술 구현을 분리해야 한다.
6.6 공통화보다 소유권 우선 원칙

두 도메인에 유사한 코드가 있더라도 의미와 변경 주기가 다르면 별도로 유지한다.

```
SV 고객 조회조건
≠
CM 캠페인 대상 고객 조회조건
```

중복 코드보다 잘못된 공통화가 더 큰 결합도와 장애 영향을 만들 수 있다.

6.7 기술 프레임워크 격리 원칙

업무 Service와 Rule에 다음 기술 상세가 노출되지 않도록 한다.

- HttpServletRequest
- HttpSession
- SecurityContextHolder
- MyBatis SqlSession
- JDBC Connection
- 외부 HTTP Client 구현체
- 파일 시스템 절대 경로
- Spring Environment 직접 조회
기술 정보는 TCF Context, DAO, Client, Configuration 계층에서 변환하여 제공한다.

7. 대안 비교 및 의사결정

7.1 패키지 구성 대안

| 대안 | 구조 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- | --- |
| A. 계층 우선 | sv.service, sv.dao | 구조가 단순함 | 도메인 파일 분산, 대규모 확장 불리 | 비권장 |
| B. 도메인 우선 | sv.customer.service | 응집도·소유권·추적성 우수 | 패키지 수 증가 | 권장 |
| C. 완전한 Clean Architecture | domain/application/adapter | 기술 격리 강함 | 기존 TCF 계층과 용어 불일치, 학습비용 큼 | 일부 적용 |
| D. ServiceId별 패키지 | customer.selectsummary | 거래 단위 추적 쉬움 | 클래스 중복, 지나친 세분화 | 금지 |
| E. 테이블별 패키지 | customerTable | DB 중심 개발에 익숙 | 업무와 DB가 강결합 | 금지 |

7.2 최종 의사결정

NSIGHT TCF의 목표 패키지 구조는 도메인 우선형과 TCF 6계층을 결합한 구조로 결정한다.

```
com.nh.nsight.marketing
  .{업무코드}
  .{도메인}
  .{계층}
```

선정 사유는 다음과 같다.

| 판단 기준 | 선정 이유 |
| --- | --- |
| TCF 정합성 | Handler·Facade·Service·Rule·DAO·Mapper 구조 유지 |
| 업무 확장성 | 17개 업무코드와 다수 도메인 확장 가능 |
| 추적성 | ServiceId의 도메인과 패키지 도메인을 연결 가능 |
| 소유권 | 업무 도메인별 담당팀 지정 가능 |
| 변경 격리 | 특정 도메인 변경을 해당 패키지 내부에 제한 |
| 자동검증 | 패키지명으로 계층과 의존성 판정 가능 |
| 운영성 | Stack Trace만으로 업무·도메인·계층 식별 가능 |

8. 목표 패키지 아키텍처

8.1 업무 WAR 최상위 구조

SV 업무 WAR 예시는 다음과 같다.

```
src/main/java
└─ com
   └─ nh
      └─ nsight
         └─ marketing
            └─ sv
               ├─ NsightSvApplication.java
               ├─ config
               ├─ customer
               ├─ segment
               ├─ product
               ├─ analytics
               ├─ batch
               └─ shared
```

| 패키지 | 역할 |
| --- | --- |
| config | SV WAR 전역 Spring 설정 |
| customer | 고객 관련 업무 도메인 |
| segment | 고객 세그먼트 업무 |
| product | 상품 관련 업무 |
| analytics | 분석 결과 조회 업무 |
| batch | SV 업무 배치 |
| shared | SV WAR 내부에서만 공유되는 제한적 구성요소 |

8.2 도메인 표준 구조

```
com.nh.nsight.marketing.sv.customer
├─ handler
│  └─ SvCustomerHandler.java
├─ facade
│  └─ SvCustomerFacade.java
├─ service
│  ├─ SvCustomerService.java
│  └─ SvCustomerMaskingService.java
├─ rule
│  └─ SvCustomerInquiryRule.java
├─ dao
│  ├─ SvCustomerDao.java
│  ├─ SvCustomerGradeDao.java
│  └─ SvCustomerProductDao.java
├─ mapper
│  ├─ SvCustomerMapper.java
│  ├─ SvCustomerGradeMapper.java
│  └─ SvCustomerProductMapper.java
├─ dto
│  ├─ request
│  ├─ response
│  ├─ command
│  ├─ query
│  ├─ result
│  └─ event
├─ client
│  ├─ CustomerProfileClient.java
│  └─ dto
├─ model
├─ converter
├─ exception
└─ constant
```

8.3 필수·선택 패키지

| 패키지 | 구분 | 생성 기준 |
| --- | --- | --- |
| handler | 필수 | 온라인 ServiceId 존재 |
| facade | 필수 | 온라인 유스케이스와 트랜잭션 경계 |
| service | 필수 | 업무 흐름 수행 |
| rule | 조건부 필수 | 별도 업무 검증이 존재 |
| dao | 조건부 | DB 접근이 존재 |
| mapper | 조건부 | MyBatis SQL이 존재 |
| dto/request | 조건부 필수 | 요청 Body 존재 |
| dto/response | 조건부 필수 | 업무 응답 존재 |
| dto/query | 조건부 | Mapper 조회조건 존재 |
| dto/result | 조건부 | Mapper 결과 객체 존재 |
| dto/command | 조건부 | 변경 유스케이스의 내부 명령 객체 |
| client | 조건부 | 외부 또는 타 업무 연계 존재 |
| model | 조건부 | 재사용되는 업무 값·상태 모델 존재 |
| converter | 조건부 | DTO 간 변환이 복잡한 경우 |
| exception | 조건부 | 도메인 고유 업무 오류 존재 |
| constant | 제한적 | 도메인 내부 고정값 존재 |
| config | 제한적 | 도메인 전용 기술 설정이 꼭 필요한 경우 |

빈 패키지를 미리 생성하지 않는다.

8.4 리소스 패키지 구조

```
src/main/resources
├─ application.yml
├─ application-local.yml
├─ application-dev.yml
├─ application-prod.yml
├─ mapper
│  └─ sv
│     ├─ customer
│     │  ├─ SvCustomerMapper.xml
│     │  ├─ SvCustomerGradeMapper.xml
│     │  └─ SvCustomerProductMapper.xml
│     └─ segment
│        └─ SvSegmentMapper.xml
├─ messages
│  └─ messages_ko.properties
└─ db
   └─ migration
```

Java Mapper와 XML은 다음처럼 대응해야 한다.

```
Java
com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper

XML
mapper/sv/customer/SvCustomerMapper.xml

Namespace
com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper
```

8.5 테스트 패키지 구조

테스트 패키지는 운영 소스 구조를 그대로 반영한다.

```
src/test/java
└─ com.nh.nsight.marketing.sv.customer
   ├─ handler
   │  └─ SvCustomerHandlerTest.java
   ├─ facade
   │  └─ SvCustomerFacadeTest.java
   ├─ service
   │  └─ SvCustomerServiceTest.java
   ├─ rule
   │  └─ SvCustomerInquiryRuleTest.java
   ├─ dao
   │  └─ SvCustomerDaoTest.java
   └─ architecture
      └─ SvPackageArchitectureTest.java
```

다음과 같은 별도 test 하위 패키지는 사용하지 않는다.

```
금지

com.nh.nsight.marketing.sv.test.customer
```

9. 패키지 표준 형식

9.1 기본 구문

```
com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}[.{세부구분}]
```

| 구성요소 | 형식 | 예시 |
| --- | --- | --- |
| 기관 역도메인 | 고정 | com.nh |
| 시스템 | 고정 | nsight |
| 플랫폼 | 고정 | marketing |
| 업무코드 | 승인된 소문자 코드 | sv |
| 도메인 | 영문 소문자 단수형 | customer |
| 계층 | 표준 계층명 | service |
| 세부구분 | 필요한 경우만 사용 | dto.request |

9.2 허용 문자와 길이

| 항목 | 기준 |
| --- | --- |
| 허용 문자 | 영문 소문자, 숫자는 제한적으로 허용 |
| 시작 문자 | 영문 소문자 |
| 구분자 | 마침표 . |
| 권장 세그먼트 수 | 6~9개 |
| 최대 세그먼트 수 | 원칙적으로 10개 이하 |
| 도메인명 길이 | 3~20자 권장 |
| 약어 | 승인된 업무코드와 표준 약어만 허용 |
| 복수형 | 원칙적으로 사용하지 않음 |

9.3 업무코드 예시

| 업무 | 패키지 코드 |
| --- | --- |
| 공통 | cc |
| 고객통합 | ic |
| 개인고객 | pc |
| 기업고객 | bc |
| Single View | sv |
| 상품 | pd |
| 캠페인 | cm |
| Event Processing | ep |
| Behavior Processing | bp |
| Sales Support | ss |
| Contact | ct |
| Management | mg |
| Operation Management | om |

9.4 도메인 명명

도메인 패키지는 업무 대상을 나타내는 명사로 작성한다.

```
customer
campaign
product
segment
contact
performance
message
authorization
transaction
deployment
```

다음 표현은 금지한다.

```
data
process
logic
business
module
function
work
temp
new
old
```

이들은 도메인의 책임을 구체적으로 설명하지 못한다.

9.5 계층 표준어

| 표준 패키지 | 사용 목적 |
| --- | --- |
| handler | ServiceId 진입 |
| facade | 유스케이스와 트랜잭션 |
| service | 업무 처리 |
| rule | 업무 규칙 |
| dao | 데이터 접근 추상화 |
| mapper | MyBatis Interface |
| dto | 데이터 전달 객체 |
| client | 외부·타 업무 호출 |
| model | 도메인 값과 상태 |
| converter | 객체 변환 |
| exception | 도메인 오류 |
| config | Spring·기술 설정 |
| batch | 배치 구성요소 |
| event | 내부 이벤트 |
| listener | 이벤트 수신 |
| scheduler | 스케줄 트리거 |

10. 구성요소 및 속성

10.1 Handler 패키지

```
{domain}.handler
```

| 항목 | 기준 |
| --- | --- |
| 책임 | ServiceId 선언, 요청 변환, Facade 호출 |
| 호출 가능 | Facade, Request DTO |
| 호출 금지 | Service 직접 호출, DAO, Mapper, Client |
| 트랜잭션 | 금지 |
| DB 접근 | 금지 |
| 응답 조립 | ETF 책임이므로 금지 |
| 클래스명 | {업무코드}{도메인}Handler |
| 메서드명 | handle{Action} |

10.2 Facade 패키지

```
{domain}.facade
```

| 항목 | 기준 |
| --- | --- |
| 책임 | 유스케이스 조립과 트랜잭션 경계 |
| 호출 가능 | Service |
| 호출 금지 | Mapper 직접 호출 |
| 트랜잭션 | 허용·권장 |
| HTTP 객체 | 직접 접근 금지 |
| 클래스명 | {업무코드}{도메인}Facade |
| 메서드명 | ServiceId의 행위명과 정합 |

10.3 Service 패키지

```
{domain}.service
```

| 항목 | 기준 |
| --- | --- |
| 책임 | 업무 처리 흐름과 결과 조립 |
| 호출 가능 | Rule, DAO, Client, 다른 도메인의 공개 Service |
| 호출 금지 | Handler, Controller, Mapper 직접 호출 |
| 트랜잭션 | 원칙적으로 Facade에 위임 |
| 기술 객체 | Servlet·SQL Session 직접 사용 금지 |
| 클래스명 | {업무코드}{기능}Service |

10.4 Rule 패키지

```
{domain}.rule
```

| 항목 | 기준 |
| --- | --- |
| 책임 | 업무 규칙과 조건 검증 |
| 부수효과 | 없어야 함 |
| DB 접근 | 금지 |
| 외부 호출 | 금지 |
| Cache 접근 | 원칙적으로 금지 |
| 입력 | DTO, 값 객체, 사전 조회된 데이터 |
| 결과 | 정상 통과 또는 업무 예외 |
| 클래스명 | {업무코드}{기능}Rule |

10.5 DAO 패키지

```
{domain}.dao
```

| 항목 | 기준 |
| --- | --- |
| 책임 | Mapper 호출과 데이터 접근 추상화 |
| 업무 판단 | 금지 |
| Mapper 호출 | 허용 |
| 다중 Mapper 조합 | 단순 데이터 접근 범위 내 허용 |
| 예외 변환 | DB 기술 예외를 공통 데이터 접근 예외로 변환 |
| 클래스명 | {업무코드}{대상}Dao |

10.6 Mapper 패키지

```
{domain}.mapper
```

| 항목 | 기준 |
| --- | --- |
| 책임 | MyBatis SQL 실행 |
| 업무 로직 | 금지 |
| 상위계층 참조 | 금지 |
| 입력 | Query·Command DTO 또는 단순 타입 |
| 출력 | Result DTO |
| Namespace | Mapper Interface 전체 경로 |
| 클래스명 | {업무코드}{대상}Mapper |

10.7 DTO 패키지

```
{domain}.dto.request
{domain}.dto.response
{domain}.dto.command
{domain}.dto.query
{domain}.dto.result
{domain}.dto.event
```

| DTO | 사용 범위 |
| --- | --- |
| Request | 표준 요청 Body에서 Handler로 전달 |
| Response | 업무 처리 결과를 ETF에 전달 |
| Command | 등록·변경 유스케이스 내부 명령 |
| Query | DAO·Mapper 조회조건 |
| Result | DB 조회 결과 |
| Event | 비동기 또는 내부 이벤트 데이터 |

하나의 DTO를 모든 계층에서 공용으로 사용하지 않는다.

```
Request DTO
≠ Query DTO
≠ Result DTO
≠ Response DTO
```

10.8 Client 패키지

```
{domain}.client
{domain}.client.dto
{domain}.client.adapter
```

| 항목 | 기준 |
| --- | --- |
| 책임 | 외부 시스템 또는 타 업무 WAR 호출 |
| 내부 호출 | tcf-eai 표준 전문 사용 |
| 인증 | 공통 연계 모듈에 위임 |
| 재시도 | 멱등성과 오류 유형 검토 후 적용 |
| DTO | 내부 업무 DTO와 외부 전문 DTO 분리 |
| 구현체 | 다중 구현이 있을 때만 adapter 사용 |

11. 플랫폼 모듈 패키지 구조

업무 WAR와 플랫폼 모듈은 동일한 패키지 구조를 사용하지 않는다.

플랫폼 모듈은 플랫폼 기능 → 세부 책임 순서로 구성한다.

11.1 tcf-core

```
com.nh.nsight.tcf.core
├─ transaction
│  ├─ TCF.java
│  ├─ stf
│  ├─ etf
│  └─ timeout
├─ dispatcher
├─ handler
├─ context
├─ control
├─ idempotency
├─ logging
├─ error
├─ metrics
└─ config
```

11.2 tcf-web

```
com.nh.nsight.tcf.web
├─ controller
├─ filter
├─ security
├─ advice
├─ converter
├─ endpoint
└─ config
```

controller는 tcf-web, Gateway, OM 관리 API 등 프로토콜 진입 모듈에는 허용되지만 일반 업무 온라인 도메인에는 허용하지 않는다.

11.3 tcf-gateway

```
com.nh.nsight.tcf.gateway
├─ controller
├─ routing
├─ authentication
├─ proxy
├─ filter
├─ logging
├─ error
└─ config
```

11.4 tcf-jwt

```
com.nh.nsight.tcf.jwt
├─ issuer
├─ validator
├─ jwks
├─ refresh
├─ revoke
├─ key
├─ repository
├─ audit
└─ config
```

11.5 tcf-eai

```
com.nh.nsight.tcf.eai
├─ client
├─ contract
├─ codec
├─ interceptor
├─ retry
├─ error
└─ config
```

11.6 tcf-om

tcf-om은 플랫폼 모듈이면서 독립 업무를 수행하므로 플랫폼 기능과 업무 도메인을 함께 적용한다.

```
com.nh.nsight.tcf.om
├─ servicecatalog
├─ transactioncontrol
├─ timeoutpolicy
├─ user
├─ authorization
├─ deployment
├─ runtime
├─ audit
└─ config
```

각 도메인 내부에는 필요한 경우 Handler·Facade·Service·DAO·Mapper 구조를 적용한다.

12. 책임 경계와 RACI

| 활동 | 아키텍처팀 | TCF팀 | 업무팀 | DevOps | 품질팀 |
| --- | --- | --- | --- | --- | --- |
| 최상위 패키지 결정 | A/R | C | I | I | C |
| 업무코드 패키지 승인 | A | C | R | I | C |
| 도메인 경계 정의 | A | C | R | I | C |
| 플랫폼 패키지 관리 | C | A/R | I | C | C |
| 업무 패키지 생성 | C | C | A/R | I | I |
| 공통화 승인 | A | R | C | I | C |
| 패키지 예외 승인 | A/R | C | C | I | C |
| ArchUnit 규칙 구현 | C | A/R | C | R | C |
| CI 품질 Gate 운영 | I | C | C | A/R | C |
| 패키지 위반 조치 | A | C | R | C | R |
| 패키지 변경 영향분석 | A | C | R | C | C |
| 폐기 패키지 관리 | A | R | R | C | C |

범례:

```
R: 실행 책임
A: 최종 승인
C: 협의
I: 통보
```

13. 정상 처리 흐름

13.1 온라인 조회 거래

```
화면
  ↓
POST /sv/online
  ↓
tcf-web.controller.OnlineTransactionController
  ↓
tcf-core.transaction.TCF
  ↓
tcf-core.transaction.stf
  ↓
tcf-core.dispatcher.TransactionDispatcher
  ↓
sv.customer.handler.SvCustomerHandler
  ↓
sv.customer.facade.SvCustomerFacade
  ↓
sv.customer.service.SvCustomerService
  ├─ sv.customer.rule.SvCustomerInquiryRule
  └─ sv.customer.dao.SvCustomerDao
       ↓
     sv.customer.mapper.SvCustomerMapper
       ↓
     mapper/sv/customer/SvCustomerMapper.xml
  ↓
tcf-core.transaction.etf
  ↓
StandardResponse
```

13.2 등록·변경 거래

```
Handler
  ↓
Facade
  ├─ @Transactional
  ↓
Service
  ├─ Rule.validate()
  ├─ DAO.insert()
  ├─ DAO.update()
  └─ Event 발행
  ↓
ETF
```

13.3 외부 시스템 호출

```
Service
  ↓
{domain}.client
  ↓
tcf-eai
  ↓
외부 시스템
```

Service에서 특정 HTTP 라이브러리나 URL을 직접 호출하지 않는다.

14. 오류·Timeout·장애 흐름

14.1 ServiceId 미등록

```
TransactionDispatcher
  ↓
Handler 조회 실패
  ↓
TCF 라우팅 오류
  ↓
ETF.systemError
```

업무 패키지의 Handler는 실행되지 않는다.

14.2 업무 규칙 오류

```
Service
  ↓
Rule
  ↓
BusinessException
  ↓
Facade Rollback 여부 판단
  ↓
ETF.businessFail
```

Rule 패키지는 업무 오류를 발생시킬 수 있지만 DB 접근이나 외부 시스템 호출을 해서는 안 된다.

14.3 DB 오류

```
Mapper
  ↓
MyBatis / JDBC Exception
  ↓
DAO에서 공통 데이터 접근 예외 변환
  ↓
Facade Transaction Rollback
  ↓
ETF.systemError
```

Mapper가 업무 오류코드나 화면 메시지를 결정하지 않는다.

14.4 외부 호출 오류

```
Service
  ↓
Client
  ↓
tcf-eai
  ↓
Timeout / Connection Error
  ↓
연계 표준 예외
  ↓
업무 정책에 따라 실패·부분성공 판단
```

14.5 TCF Timeout

```
TCF TimeoutExecutor
  ↓
Facade·Service 실행 제한시간 초과
  ↓
업무 Thread 중단 요청 및 결과 폐기
  ↓
ETF Timeout 오류
  ↓
거래로그 TIMEOUT 종료
```

Timeout 정책은 패키지별로 하드코딩하지 않고 ServiceId와 OM 정책으로 관리한다.

14.6 패키지 순환 의존

```
Compile 또는 ArchUnit 검사
  ↓
순환 의존 탐지
  ↓
Build 실패
  ↓
배포 차단
```

운영 시점이 아니라 CI 단계에서 차단해야 한다.

15. 정상 예시

15.1 고객요약 조회

```
com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.rule
com.nh.nsight.marketing.sv.customer.dao
com.nh.nsight.marketing.sv.customer.mapper
com.nh.nsight.marketing.sv.customer.dto.request
com.nh.nsight.marketing.sv.customer.dto.response
com.nh.nsight.marketing.sv.customer.dto.query
com.nh.nsight.marketing.sv.customer.dto.result
```

15.2 캠페인 등록

```
com.nh.nsight.marketing.cm.campaign.handler
com.nh.nsight.marketing.cm.campaign.facade
com.nh.nsight.marketing.cm.campaign.service
com.nh.nsight.marketing.cm.campaign.rule
com.nh.nsight.marketing.cm.campaign.dao
com.nh.nsight.marketing.cm.campaign.mapper
com.nh.nsight.marketing.cm.campaign.dto.command
com.nh.nsight.marketing.cm.campaign.dto.response
```

15.3 메시지 외부 연계

```
com.nh.nsight.marketing.mg.message.service
com.nh.nsight.marketing.mg.message.client
com.nh.nsight.marketing.mg.message.client.dto
com.nh.nsight.marketing.mg.message.dao
com.nh.nsight.marketing.mg.message.mapper
```

15.4 내부 공통 값 객체

```
com.nh.nsight.marketing.sv.shared.model.BranchScope
com.nh.nsight.marketing.sv.shared.model.CustomerKey
```

이는 SV WAR 내부에서만 사용한다. 다른 업무 WAR에서 직접 참조하지 않는다.

16. 금지 예시

16.1 업무 Controller

```
금지

com.nh.nsight.marketing.sv.customer.controller.SvCustomerController
```

수정:

```
com.nh.nsight.marketing.sv.customer.handler.SvCustomerHandler
```

16.2 무의미한 공통 패키지

```
금지

com.nh.nsight.marketing.common.util.CommonUtil
com.nh.nsight.marketing.common.service.CommonService
com.nh.nsight.marketing.sv.common.CommonManager
```

수정:

```
구체적인 책임으로 이동

com.nh.nsight.tcf.core.context.TraceIdGenerator
com.nh.nsight.marketing.sv.customer.service.SvCustomerMaskingService
```

16.3 계층 우회

```
// 금지: Handler가 Mapper를 직접 호출
public class SvCustomerHandler {
    private final SvCustomerMapper mapper;
}
```

수정:

```
Handler → Facade → Service → DAO → Mapper
```

16.4 Rule의 DB 접근

```
// 금지
public class SvCustomerRule {
    private final SvCustomerMapper mapper;
}
```

Rule에 필요한 데이터는 Service가 사전에 조회하여 인자로 전달한다.

16.5 일률적인 Impl 패키지

```
금지

service
├─ CustomerService
└─ impl
   └─ CustomerServiceImpl
```

구현체가 하나이면 다음처럼 작성한다.

```
service
└─ SvCustomerService
```

16.6 다른 업무 WAR 구현 참조

```
// 금지
import com.nh.nsight.marketing.ic.customer.service.IcCustomerService;
```

수정:

```
SV Service
  → SV Client
  → tcf-eai
  → IC ServiceId
```

16.7 Mapper XML 불일치

```
금지

Java:
com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper

XML Namespace:
customerMapper
```

수정:

```
namespace=
com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper
```

16.8 환경별 패키지

```
금지

com.nh.nsight.marketing.sv.prod.customer
com.nh.nsight.marketing.sv.dev.customer
```

환경 차이는 설정 Profile로 관리한다.

17. 연계 규칙

17.1 ServiceId와 패키지 연결

```
ServiceId
SV.Customer.selectSummary

업무코드
SV → sv

도메인
Customer → customer

권장 Handler 패키지
com.nh.nsight.marketing.sv.customer.handler
```

| ServiceId | 패키지 |
| --- | --- |
| SV.Customer.selectSummary | sv.customer |
| CM.Campaign.create | cm.campaign |
| OM.ServiceCatalog.update | om.servicecatalog |
| MG.Message.send | mg.message |

ServiceId 도메인과 패키지 도메인의 의미가 달라서는 안 된다.

17.2 화면과 패키지 연결

```
화면 ID
SV-CUS-0001

업무코드
SV

업무세구분
CUS

도메인 패키지
sv.customer
```

화면세구분코드와 패키지 도메인명이 반드시 동일 문자열일 필요는 없지만, 관리대장에서 매핑되어야 한다.

17.3 Mapper와 DB 객체 연결

```
ServiceId
→ Handler
→ Facade
→ Service
→ DAO
→ Mapper
→ Mapper XML
→ SQL ID
→ Table / View
```

프로그램 설계서와 추적성 매트릭스에서 이 경로를 관리한다.

17.4 업무 WAR 간 연계

서로 다른 업무 WAR의 Java 패키지를 직접 의존하지 않는다.

```
허용

SV
  → SvCustomerProfileClient
  → tcf-eai
  → IC.Customer.selectProfile
금지

sv-service
  → Gradle dependency
  → ic-service 구현 클래스
```

17.5 공통 모듈 의존

| 업무 패키지 | 허용 공통 모듈 |
| --- | --- |
| Handler | tcf-core 공개 Handler API |
| Facade·Service | tcf-core Context·Exception API |
| Client | tcf-eai |
| Cache Adapter | tcf-cache |
| JWT 검증 | tcf-web 또는 보안 Starter |
| 배치 | tcf-batch |

업무 패키지가 플랫폼 모듈의 internal 패키지를 참조하지 않도록 한다.

18. 데이터 및 상태관리

18.1 DTO 상태관리

DTO는 다음 원칙을 적용한다.

- 가능한 한 불변 객체 또는 Java record를 사용한다.
- Request DTO에 인증 사용자 정보를 신뢰값으로 두지 않는다.
- Result DTO를 화면 응답으로 직접 반환하지 않는다.
- Mapper Result에 화면 전용 계산 필드를 넣지 않는다.
- 개인정보 필드는 Response 조립 전에 마스킹한다.
- DTO에 Spring Bean 또는 기술 객체를 포함하지 않는다.
18.2 TransactionContext 사용

guid, traceId, serviceId, userId, branchId는 업무 패키지별 전역 변수로 저장하지 않는다.

```
tcf-core TransactionContext
  ↓
Facade / Service에서 읽기 전용 사용
```

18.3 ThreadLocal 사용 제한

업무 패키지가 자체 ThreadLocal을 생성하는 것은 원칙적으로 금지한다.

필요한 경우 다음을 검토해야 한다.

- 생성과 정리 책임
- 비동기 Thread 전파
- Timeout 후 정리
- 메모리 누수
- 다중 WAR 환경의 ClassLoader 누수
18.4 Cache 패키지

업무 Cache가 필요한 경우 다음 구조를 사용한다.

```
{domain}.cache
├─ {Domain}CacheReader
├─ {Domain}CacheWriter
└─ {Domain}CacheKey
```

Cache 접근을 Rule 또는 Mapper에서 직접 수행하지 않는다.

19. 성능·용량·확장성

패키지 구조는 직접적인 처리성능 기능은 아니지만, 다음 런타임 요소에 영향을 미친다.

19.1 Component Scan 범위

전체 조직 패키지를 스캔하지 않는다.

```
금지

@SpringBootApplication(
    scanBasePackages = "com.nh"
)
```

업무 WAR와 필요한 TCF 공개 패키지만 스캔한다.

```
권장 범위 예

com.nh.nsight.marketing.sv
com.nh.nsight.tcf
```

지나치게 넓은 Component Scan은 다음 문제를 발생시킨다.

- 기동시간 증가
- 불필요한 Bean 등록
- Bean 이름 충돌
- 다른 업무 설정의 잘못된 로딩
- 테스트 Context 확대
19.2 Mapper Scan 범위

Mapper Scan은 해당 업무 WAR 범위로 제한한다.

```
com.nh.nsight.marketing.sv
```

Mapper Interface에는 @Mapper 또는 승인된 Marker Interface를 적용하여 일반 Interface가 Mapper로 등록되지 않도록 한다.

19.3 도메인 분리 확장

특정 도메인의 규모가 커지면 다음 순서로 확장한다.

```
1단계
단일 도메인 패키지

2단계
하위 업무 도메인 분리

3단계
독립 Gradle 모듈 분리

4단계
별도 WAR 또는 서비스 분리
```

처음부터 모든 도메인을 별도 모듈로 만들지 않는다.

19.4 클래스 수 기준

절대적인 제한은 아니지만 다음을 구조 점검 신호로 사용한다.

| 점검 항목 | 검토 기준 |
| --- | --- |
| 하나의 패키지 클래스 | 30개 초과 시 분리 검토 |
| 하나의 Service 클래스 | 공개 메서드 15개 초과 시 책임 분리 검토 |
| 하나의 Handler 담당 ServiceId | 10개 초과 시 도메인 분리 검토 |
| 하나의 Mapper Statement | 30개 초과 시 Mapper 분리 검토 |
| 하나의 도메인 패키지 | 100개 클래스 초과 시 하위 도메인 검토 |

20. 보안·개인정보·감사

20.1 인증 패키지 중복 구현 금지

업무 도메인별로 JWT 파싱이나 세션 검증 패키지를 만들지 않는다.

```
금지

sv.customer.security.JwtParser
cm.campaign.security.JwtValidator
```

JWT 검증은 tcf-gateway, tcf-web 보안 Filter 또는 공통 보안 Starter가 담당한다.

20.2 권한 책임 분리

| 권한 종류 | 담당 |
| --- | --- |
| JWT 서명·만료 검증 | Gateway 또는 공통 Filter |
| 공통 기능권한 | STF |
| 상세 업무 권한 | Service·Rule |
| 데이터 접근범위 | Service·Rule |
| SQL 조건 반영 | Service가 확정한 Query DTO |

Mapper가 사용자 역할을 해석하거나 권한을 판단하지 않는다.

20.3 개인정보 DTO 분리

개인정보 포함 Result DTO와 화면 Response DTO를 분리한다.

```
Mapper Result
  ↓
Service 마스킹
  ↓
Response DTO
```

20.4 감사 추적

감사로그에는 최소한 다음 정보를 기록한다.

- 업무코드
- ServiceId
- 거래코드
- GUID
- TraceId
- 사용자 ID
- 지점 ID
- 대상 도메인
- 수행 결과
- 오류코드
패키지 전체명은 장애로그와 프로그램 추적정보에 포함할 수 있으나 개인정보를 패키지명이나 클래스명에 포함해서는 안 된다.

21. 운영·모니터링·장애 대응

21.1 운영 식별성

Stack Trace의 클래스 전체 경로만으로 다음을 식별할 수 있어야 한다.

```
com.nh.nsight.marketing.sv.customer.dao.SvCustomerDao
```

해석:

```
플랫폼: NSIGHT 마케팅
업무: SV
도메인: Customer
계층: DAO
프로그램: SvCustomerDao
```

21.2 로그 분류

| 계층 | 권장 로그 |
| --- | --- |
| Handler | ServiceId 진입과 요청 식별정보 |
| Facade | 유스케이스 시작·종료, 트랜잭션 결과 |
| Service | 주요 업무 분기 |
| Rule | 업무 규칙 실패 사유 |
| DAO | Mapper ID와 수행시간 |
| Mapper | SQL ID, 실행시간 |
| Client | 대상 시스템과 연계 결과 |

DTO 전체의 toString() 출력은 금지한다.

21.3 패키지별 로그레벨

장애 분석 시 다음처럼 도메인 단위로 로그레벨을 제한적으로 조정할 수 있다.

```
com.nh.nsight.marketing.sv.customer = DEBUG
com.nh.nsight.marketing.sv.customer.mapper = TRACE
```

운영환경에서 업무코드 전체를 장기간 DEBUG로 변경하지 않는다.

21.4 장애 책임 식별

| 장애 패키지 | 1차 담당 |
| --- | --- |
| tcf.core.* | TCF 프레임워크팀 |
| tcf.web.* | 웹·보안 프레임워크팀 |
| marketing.sv.customer.service | SV 고객 업무팀 |
| marketing.sv.customer.mapper | SV 업무팀·DBA |
| tcf.gateway.routing | Gateway팀 |
| tcf.jwt.key | 인증·보안팀 |
| tcf.om.runtime | OM 운영관리팀 |

22. 자동검증 및 품질 Gate

22.1 Checkstyle 패키지명 검증

기본 패턴:

```
com.nh.nsight.marketing.[업무코드].[도메인].[계층]
```

검증 항목:

- 소문자 여부
- 언더스코어·하이픈 사용 여부
- 승인되지 않은 업무코드
- 금지 패키지명
- 최대 깊이 초과
- 환경명 포함 여부
22.2 ArchUnit 계층 검증

다음 규칙을 CI에서 자동검증한다.

```
handler
  → facade만 호출 가능

facade
  → service 호출 가능

service
  → rule, dao, client 호출 가능

rule
  → dao, mapper, client 접근 금지

dao
  → mapper만 데이터 접근 계층으로 호출

mapper
  → 상위 업무 패키지 의존 금지
```

추가 규칙:

- ..controller..는 업무 WAR에 존재하지 않아야 한다.
- @Transactional은 원칙적으로 ..facade..에만 존재해야 한다.
- 업무 패키지 간 순환 의존이 없어야 한다.
- 다른 업무코드 구현 패키지를 직접 참조하지 않아야 한다.
- ..rule..은 Spring Repository·Mapper에 의존하지 않아야 한다.
- ..mapper..는 ..service..에 의존하지 않아야 한다.
- ..dto..는 Spring Framework에 의존하지 않아야 한다.
- internal 패키지는 외부 모듈에서 참조하지 않아야 한다.
22.3 Mapper 검증

CI에서 다음을 확인한다.

- Mapper Interface와 XML 파일 존재 여부
- Namespace와 Interface 전체 경로 일치
- SQL ID와 Mapper 메서드 일치
- 중복 SQL ID
- 미사용 Mapper XML
- XML 경로와 업무코드·도메인 정합성
- ResultType의 패키지 존재 여부
22.4 Handler Registry 검증

- 모든 ServiceId가 하나의 Handler에만 등록되는지 확인한다.
- Handler 패키지가 업무코드 및 ServiceId 도메인과 일치하는지 확인한다.
- ServiceId 미등록 Handler를 탐지한다.
- 동일 Handler가 과도한 도메인을 담당하지 않는지 확인한다.
22.5 금지 문자열 검증

다음 패키지는 승인 목록에 없으면 빌드를 실패시킨다.

```
..common..
..common2..
..util..
..helper..
..manager..
..temp..
..testdata..
..legacy..
..old..
..new..
..impl..
```

util이 항상 금지되는 것은 아니지만 플랫폼 공통의 명확한 책임이 확인된 경우에만 예외 승인한다.

22.6 품질 Gate

| 등급 | 조건 | 처리 |
| --- | --- | --- |
| Blocker | 순환 의존, 다른 WAR 구현 직접 참조 | 배포 차단 |
| Critical | Handler→Mapper, Rule→DAO, 업무 Controller | 배포 차단 |
| Major | 패키지명 위반, Mapper Namespace 불일치 | 수정 후 재검증 |
| Minor | 클래스 수 과다, 불필요한 공개 클래스 | 코드리뷰 보완 |
| Advisory | 도메인 분리 후보 | 기술부채 등록 |

23. 테스트 시나리오

| ID | 테스트 | 기대 결과 |
| --- | --- | --- |
| PKG-T01 | Handler가 Facade만 참조 | 통과 |
| PKG-T02 | Handler가 Mapper 참조 | ArchUnit 실패 |
| PKG-T03 | Rule이 DAO 참조 | ArchUnit 실패 |
| PKG-T04 | Facade에 @Transactional 선언 | 통과 |
| PKG-T05 | Handler에 @Transactional 선언 | 검증 실패 |
| PKG-T06 | 업무 패키지에 Controller 생성 | 검증 실패 |
| PKG-T07 | sv가 ic 구현 클래스 참조 | 검증 실패 |
| PKG-T08 | Mapper Namespace 전체 경로 일치 | 통과 |
| PKG-T09 | Mapper Namespace 불일치 | 빌드 실패 |
| PKG-T10 | 순환 의존 발생 | 빌드 실패 |
| PKG-T11 | common2 패키지 생성 | 검증 실패 |
| PKG-T12 | Request DTO가 Spring Bean 의존 | 검증 실패 |
| PKG-T13 | 테스트 패키지가 운영 구조와 일치 | 통과 |
| PKG-T14 | 미등록 업무코드 패키지 생성 | 검증 실패 |
| PKG-T15 | 동일 ServiceId가 두 Handler에 등록 | 기동 또는 CI 실패 |
| PKG-T16 | 신규 도메인 패키지 정상 생성 | 아키텍처 테스트 통과 |
| PKG-T17 | Mapper XML 경로 불일치 | CI 실패 |
| PKG-T18 | 패키지 변경 후 OM·설계서 미갱신 | 품질 Gate 실패 |

24. 체크리스트

24.1 신규 도메인 생성

| No. | 확인 항목 | 결과 |
| --- | --- | --- |
| 1 | 업무코드가 승인되었는가 | □ |
| 2 | 도메인 책임이 명확한가 | □ |
| 3 | 기존 도메인과 역할이 중복되지 않는가 | □ |
| 4 | ServiceId 도메인과 패키지 도메인이 정합한가 | □ |
| 5 | Handler·Facade·Service 구조가 정의되었는가 | □ |
| 6 | DB 접근이 있는 경우 DAO·Mapper가 분리되었는가 | □ |
| 7 | 외부 호출이 Client 패키지로 분리되었는가 | □ |
| 8 | DTO 유형이 목적별로 분리되었는가 | □ |
| 9 | 테스트 패키지가 동일 구조로 생성되었는가 | □ |
| 10 | ArchUnit 검증이 추가되었는가 | □ |

24.2 코드 리뷰

| No. | 확인 항목 | 결과 |
| --- | --- | --- |
| 1 | 상위 계층에서 하위 계층으로만 호출하는가 | □ |
| 2 | Handler가 Facade 이외를 직접 호출하지 않는가 | □ |
| 3 | Rule에 DB·Cache·외부 호출이 없는가 | □ |
| 4 | DAO에 업무 판단이 없는가 | □ |
| 5 | Mapper가 SQL 실행만 담당하는가 | □ |
| 6 | 트랜잭션이 Facade에 선언되었는가 | □ |
| 7 | DTO를 전 계층에서 재사용하지 않는가 | □ |
| 8 | 다른 업무 WAR 구현을 직접 참조하지 않는가 | □ |
| 9 | 무의미한 공통 패키지가 추가되지 않았는가 | □ |
| 10 | Package-private 적용 가능성을 검토했는가 | □ |

24.3 운영 전환

| No. | 확인 항목 | 결과 |
| --- | --- | --- |
| 1 | 소스 패키지와 프로그램 설계서가 일치하는가 | □ |
| 2 | ServiceId와 Handler 패키지가 매핑되는가 | □ |
| 3 | Mapper Interface와 XML Namespace가 일치하는가 | □ |
| 4 | 로그에서 업무·도메인·계층을 식별할 수 있는가 | □ |
| 5 | ArchUnit·Checkstyle 검증이 모두 통과했는가 | □ |
| 6 | 폐기 패키지 참조가 남아 있지 않은가 | □ |
| 7 | 운영 Runbook에 담당 조직이 등록되었는가 | □ |
| 8 | 배포 패키지에 테스트·임시 클래스가 없는가 | □ |

25. 변경·호환성·폐기 관리

25.1 패키지 변경의 영향

Java 패키지 변경은 단순 파일 이동이 아니다.

다음 항목에 영향을 미친다.

- Java Import
- 컴파일된 클래스 전체명
- MyBatis Namespace
- XML ResultType·ParameterType
- Spring Bean 등록
- Component Scan
- Mapper Scan
- Reflection 설정
- JSON Type 정보
- 테스트 클래스
- 로그 분류 설정
- 보안 접근규칙
- 운영 프로그램 관리대장
- 프로그램·거래·SQL 추적성 매트릭스
25.2 변경 절차

```
변경 필요성 등록
  ↓
도메인·책임 영향분석
  ↓
기존·신규 패키지 매핑표 작성
  ↓
아키텍처 승인
  ↓
소스·Mapper XML·테스트 일괄 변경
  ↓
자동검증
  ↓
전체 회귀 테스트
  ↓
설계서·운영대장 갱신
  ↓
기존 패키지 폐기
```

25.3 호환성 원칙

Java는 패키지 Alias를 제공하지 않으므로 패키지 변경은 일반적으로 바이너리 호환성을 깨뜨린다.

따라서 다음 원칙을 적용한다.

- 운영 안정화 기간에는 불필요한 패키지명을 변경하지 않는다.
- 외부 모듈에서 참조하는 공개 API 패키지는 별도 승인한다.
- 패키지 변경과 기능 변경을 가능한 한 분리한다.
- Mapper Namespace 변경은 SQL 테스트를 반드시 수행한다.
- 직렬화된 클래스 전체명을 저장하는 구조가 있는지 확인한다.
- 패키지 변경 전 전체 참조 검색 결과를 증적으로 보관한다.
- 업무 WAR 간 직접 참조를 제거한 후 패키지를 이동한다.
25.4 폐기 기준

패키지를 즉시 삭제하지 않고 다음 상태로 관리한다.

| 상태 | 설명 |
| --- | --- |
| ACTIVE | 신규 개발과 변경 허용 |
| DEPRECATED | 신규 참조 금지, 기존 기능만 유지 |
| FROZEN | 결함 수정 외 변경 금지 |
| REMOVED | 코드와 설정에서 완전 제거 |

25.5 폐기 완료 조건

- 소스 Import가 존재하지 않는다.
- Mapper XML에서 참조하지 않는다.
- Spring 설정에서 스캔하지 않는다.
- Reflection·문자열 참조가 존재하지 않는다.
- 테스트 코드 참조가 없다.
- 운영 로그 설정에 남아 있지 않다.
- 프로그램 설계서와 관리대장이 갱신되었다.
- 최소 1회 이상 전체 회귀 테스트를 통과했다.
26. 시사점

26.1 핵심 아키텍처 판단

NSIGHT TCF의 업무 패키지는 다음 구조를 표준으로 해야 한다.

```
com.nh.nsight.marketing
  .{업무코드}
  .{도메인}
  .{계층}
```

이는 단순한 명명방식이 아니라 다음을 코드에 반영하는 구조다.

```
업무 WAR의 경계
+ 업무 도메인의 소유권
+ TCF 실행 계층
+ 허용 의존성
+ 트랜잭션 책임
+ 데이터 접근 책임
+ 운영 추적성
```

26.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 계층 우선 구조 유지 | 도메인 변경 파일이 여러 패키지로 분산 |
| 업무 Controller 허용 | TCF 공통 통제 우회 |
| 공통 패키지 확대 | 전 업무 결합도와 배포 영향 증가 |
| 직접 WAR 참조 | 독립 배포와 장애 격리 불가 |
| Rule의 외부 의존 | 테스트와 업무 규칙 재사용 어려움 |
| Impl 패키지 남용 | 실질 가치 없는 클래스·파일 증가 |
| 자동검증 부재 | 표준이 문서에만 존재하고 코드에서 무시됨 |

26.3 우선 보완 과제

| 우선순위 | 과제 |
| --- | --- |
| 1 | 현재 업무 WAR의 패키지 현황 자동 추출 |
| 2 | 업무코드·도메인·계층 기준 Gap 분석 |
| 3 | Handler→Facade→Service→DAO 의존성 검사 |
| 4 | 업무 Controller와 직접 Mapper 호출 제거 |
| 5 | Mapper Namespace·XML 경로 정비 |
| 6 | common, util, impl 패키지 사용 현황 점검 |
| 7 | 업무 WAR 간 직접 Gradle 의존 제거 |
| 8 | ArchUnit·Checkstyle 품질 Gate 적용 |
| 9 | 프로그램 설계서와 실제 패키지 자동 대조 |
| 10 | 폐기 패키지와 호환성 관리대장 작성 |

26.4 중장기 발전 방향

- 패키지 구조와 ServiceId Catalog를 자동 비교한다.
- 화면 이벤트–ServiceId–프로그램–SQL 추적성 매트릭스를 소스에서 자동 생성한다.
- 업무 도메인별 소유팀과 코드오너를 지정한다.
- 패키지 간 의존성을 그래프로 시각화한다.
- 순환 의존과 업무 경계 위반을 Pull Request 단계에서 차단한다.
- 규모가 커진 도메인은 독립 Gradle 모듈 또는 WAR로 단계적으로 분리한다.
- 플랫폼의 공개 API와 내부 API를 패키지 수준에서 구분한다.
27. 마무리말

TCF 패키지 구조의 목적은 파일을 보기 좋게 배치하는 것이 아니다.

올바른 패키지 구조는 개발자가 코드를 작성하기 전부터 다음 질문에 답하게 한다.

```
이 프로그램은 어느 업무의 책임인가?
어느 도메인에 속하는가?
어떤 계층인가?
어떤 계층을 호출할 수 있는가?
트랜잭션을 선언할 수 있는가?
DB나 외부 시스템에 접근할 수 있는가?
다른 업무에서 직접 참조해도 되는가?
장애가 발생하면 누가 담당하는가?
```

최종 기준은 다음 한 문장으로 정리할 수 있다.

```
업무 프로그램은 업무코드와 도메인으로 응집시키고,
Handler → Facade → Service → Rule·DAO → Mapper의
단방향 책임 구조를 패키지와 자동검증으로 강제한다.
```

