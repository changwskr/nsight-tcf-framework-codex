TCF 개발 방법론

기본 BASE 패키지 구조 설계기준

1. 도입 전 안내말

BASE 패키지는 Java 클래스가 저장되는 최상위 디렉터리 이름이 아니다.

NSIGHT TCF에서 BASE 패키지는 다음 사항을 코드 수준에서 식별하고 통제하는 최상위 아키텍처 네임스페이스다.

```
기관
→ 시스템
→ 플랫폼 또는 업무 영역
→ 실행 모듈·업무코드
→ 업무 도메인
→ 책임 계층
→ 세부 구현
```

BASE 패키지가 일관되게 설계되면 클래스의 전체 경로만으로 다음을 판단할 수 있다.

```
com.nh.nsight.marketing.sv.customer.service.SvCustomerService
│      │       │        │     │        │
│      │       │        │     │        └─ 프로그램
│      │       │        │     └────────── 책임 계층
│      │       │        └──────────────── 업무 도메인
│      │       └───────────────────────── 업무코드·WAR
│      └───────────────────────────────── NSIGHT 업무 영역
└──────────────────────────────────────── 기관·시스템 네임스페이스
```

반대로 BASE 패키지 기준이 없으면 다음과 같은 구조가 동시에 발생한다.

```
com.nh.nsight.sv
com.nh.marketing.sv
kr.co.nh.nsight.sv
com.nonghyup.nsight.customer
com.nh.tcf.core
com.nh.nsight.common
```

이러한 혼재는 단순한 명명 불일치가 아니다.

- Spring Component Scan 범위가 불명확해진다.
- 업무 WAR 사이의 직접 의존을 통제하기 어렵다.
- 공통 프레임워크와 업무 프로그램의 소유권이 섞인다.
- Stack Trace만으로 장애 담당 영역을 판단하기 어렵다.
- ArchUnit과 Checkstyle 자동검증 규칙을 적용하기 어렵다.
- 패키지 이동 시 대규모 Import 변경과 호환성 문제가 발생한다.
- 동일한 클래스와 Bean이 중복 등록될 수 있다.
- Mapper Scan 누락 또는 과잉 등록이 발생할 수 있다.
NSIGHT TCF의 온라인 실행 흐름은 공통 진입점에서 TCF → STF → Dispatcher → Handler → Facade → Service → Rule·DAO → Mapper → ETF 순서로 수행된다. BASE 패키지와 하위 패키지는 이 실행 책임과 업무 경계를 그대로 표현해야 한다.

본 기준의 최종 판단은 다음과 같다.

```
전체 공통 ROOT BASE
= com.nh.nsight

TCF 플랫폼 BASE
= com.nh.nsight.tcf

NSIGHT 업무 BASE
= com.nh.nsight.marketing
```

업무 프로그램은 다음 형식을 적용한다.

```
com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}
```

TCF 플랫폼 프로그램은 다음 형식을 적용한다.

```
com.nh.nsight.tcf.{플랫폼모듈}.{세부책임}
```

2. 문서 개요

2.1 목적

본 기준서의 목적은 NSIGHT TCF 전체 Java 소스에 적용할 최상위 BASE 패키지와 하위 네임스페이스 구조를 정의하는 것이다.

| 목적 | 설명 |
| --- | --- |
| 최상위 네임스페이스 통일 | 모든 Java 소스를 com.nh.nsight 아래에 배치 |
| 플랫폼·업무 분리 | TCF 공통 기능과 NSIGHT 업무 프로그램의 책임 분리 |
| 모듈 식별 | 패키지 경로에서 Gradle 모듈·WAR·업무코드 식별 |
| 도메인 식별 | 프로그램이 소속된 업무 책임과 변경 범위 식별 |
| 계층 통제 | Handler·Facade·Service·Rule·DAO·Mapper 호출 방향 통제 |
| Component Scan 통제 | 불필요한 Bean 검색과 중복 등록 방지 |
| Mapper Scan 통제 | 업무 WAR별 MyBatis Mapper 범위 제한 |
| 의존성 통제 | 업무 WAR 간 Java 패키지 직접 참조 방지 |
| 운영 추적성 | Stack Trace·로그에서 업무와 담당 조직 식별 |
| 자동검증 | Checkstyle·ArchUnit·Gradle을 통한 구조 위반 차단 |
| 확장성 확보 | 17개 업무코드와 신규 플랫폼 모듈 확장 지원 |
| 변경관리 | 패키지 이동·폐기·호환성 관리 절차 정의 |

2.2 적용범위

본 기준은 다음 Java 소스와 리소스에 적용한다.

| 적용 대상 | 적용 내용 |
| --- | --- |
| TCF 플랫폼 모듈 | tcf-core, tcf-web, tcf-util, tcf-gateway, tcf-jwt |
| 공통 확장 모듈 | tcf-eai, tcf-cache, tcf-batch, tcf-om |
| 업무 WAR | CC, IC, PC, BC, MS, SV, PD, CM, EB, EP, BP, BD, SS, CS, CT, MG |
| 온라인 프로그램 | Handler, Facade, Service, Rule, DAO, Mapper |
| 외부 연계 | Client, Adapter, Contract, 외부 전문 DTO |
| 배치 프로그램 | Job, Step, Tasklet, Reader, Processor, Writer |
| 설정 프로그램 | Configuration, Properties, Security, MyBatis 설정 |
| 운영 프로그램 | Health, Runtime Diagnostics, Audit, Metrics |
| 테스트 소스 | 단위·통합·아키텍처 테스트 |
| Mapper XML | Java Mapper 패키지와 Namespace 정합성 |
| Spring Boot 기동 클래스 | 모듈별 Component Scan 기준점 |

다음 영역은 동일한 문자열을 강제로 적용하지 않고 별도 표준을 사용한다.

| 제외 대상 | 적용 기준 |
| --- | --- |
| React·WEBTOPSUITE JavaScript | UI 소스 디렉터리 표준 |
| DB 스키마·테이블 | DB 객체 명명규칙 |
| CI/CD Pipeline | DevOps 저장소 구조 |
| Shell·Apache·Tomcat 설정 | 인프라 설정파일 기준 |
| 외부 제품 생성 코드 | 제품 공식 Namespace |
| 외부 라이브러리 | 원 공급자의 패키지 유지 |

2.3 대상 독자

| 대상 | 활용 목적 |
| --- | --- |
| 애플리케이션 아키텍트 | BASE 패키지와 모듈 경계 승인 |
| TCF 프레임워크팀 | 플랫폼 모듈 패키지 관리 |
| 업무개발팀 | 업무코드·도메인·계층 패키지 생성 |
| 코드 리뷰어 | 패키지 위치와 호출 방향 검증 |
| DevOps팀 | CI/CD 구조검사와 빌드 차단 |
| 테스트팀 | 패키지별 테스트 범위와 구조 테스트 작성 |
| 운영팀 | 장애 클래스의 업무·모듈 소유자 식별 |
| 보안팀 | 인증·키·개인정보 관련 패키지 접근 검토 |
| PMO·품질팀 | 설계서와 실제 소스 구조의 정합성 점검 |

2.4 선행조건

| 선행조건 | 기준 |
| --- | --- |
| 시스템명 | NSIGHT가 공식 시스템 식별자로 확정되어 있어야 함 |
| 업무영역 | 업무 플랫폼 영역을 marketing으로 사용할지 승인되어야 함 |
| 업무코드 | 2자리 업무코드와 WAR 매핑이 확정되어야 함 |
| 플랫폼 모듈 | TCF 플랫폼 Gradle 모듈 목록이 확정되어야 함 |
| 업무 도메인 | 각 업무코드 하위의 도메인이 정의되어야 함 |
| 계층구조 | Handler·Facade·Service·Rule·DAO·Mapper 기준 확정 |
| 모듈 의존성 | Gradle 프로젝트 간 허용 의존관계 정의 |
| ServiceId | {업무코드}.{도메인}.{행위} 기준 확정 |
| Mapper 기준 | Java Mapper와 XML Namespace 기준 확정 |
| 자동검증 도구 | Checkstyle·ArchUnit 또는 동등 도구 적용 가능 |
| 변경 승인체계 | BASE 패키지 변경 승인자와 영향분석 절차 지정 |

2.5 용어 정의

| 용어 | 정의 |
| --- | --- |
| ROOT BASE | 전체 NSIGHT Java 소스의 최상위 패키지 |
| 업무 BASE | 업무 애플리케이션이 공통으로 사용하는 최상위 패키지 |
| 플랫폼 BASE | TCF 공통 프레임워크가 사용하는 최상위 패키지 |
| 모듈 BASE | 하나의 Gradle 모듈 또는 실행 애플리케이션의 기준 패키지 |
| 업무코드 BASE | 하나의 업무 WAR를 식별하는 패키지 |
| 도메인 BASE | 하나의 업무 책임을 식별하는 패키지 |
| 계층 패키지 | Handler·Service·DAO 등 기술적 책임을 표현하는 패키지 |
| Bootstrap Class | Spring Boot 애플리케이션을 기동하는 클래스 |
| Component Scan | Spring Bean 후보를 검색하는 패키지 범위 |
| Mapper Scan | MyBatis Mapper Interface를 검색하는 범위 |
| 공개 API 패키지 | 다른 모듈에서 참조하도록 승인된 인터페이스 영역 |
| 내부 구현 패키지 | 해당 모듈 외부에서 직접 참조할 수 없는 구현 영역 |
| 패키지 소유권 | 패키지의 설계·개발·변경 승인 책임 |

3. 문제 정의 및 설계 배경

3.1 BASE 패키지가 불명확한 경우

다음과 같은 구조가 혼재할 수 있다.

```
com.nh.nsight.tcf.core
com.nh.tcf.gateway
com.nh.nsight.marketing.sv
com.nh.nsight.sv
kr.co.nh.nsight.om
com.nh.nsight.common.util
```

| 문제 | 영향 |
| --- | --- |
| 기관 Namespace 불일치 | 동일 시스템 소스가 여러 루트로 분산 |
| 시스템명 누락 | NSIGHT 소스인지 식별하기 어려움 |
| 플랫폼·업무 혼재 | 공통 프레임워크와 업무 소유권 충돌 |
| 업무코드 누락 | 어느 WAR에 배포되는 클래스인지 불명확 |
| 과도한 공통화 | common, util에 모든 기능 집중 |
| Scan 범위 확대 | 기동시간 증가와 Bean 충돌 |
| Mapper 오등록 | 다른 업무 Mapper가 함께 등록될 가능성 |
| 순환 의존 | 업무 WAR 간 직접 Import 증가 |
| 장애 대응 지연 | Stack Trace에서 담당 업무 식별 곤란 |
| 자동검증 불가 | 일관된 패턴이 없어 규칙 작성이 어려움 |

3.2 BASE 패키지와 배포 단위의 불일치

다음 구조는 금지한다.

```
Gradle 모듈: sv-service
WAR: sv.war

Java 패키지:
com.nh.nsight.customer
com.nh.nsight.product
com.nh.nsight.common
```

이 구조에서는 클래스만 보아서는 어느 WAR에 포함되는지 알 수 없다.

목표 구조는 다음과 같다.

```
Gradle 모듈: sv-service
WAR: sv.war
Context Path: /sv
업무코드: SV

Module BASE:
com.nh.nsight.marketing.sv
Gradle 모듈
↔ WAR
↔ Context Path
↔ 업무코드
↔ Module BASE
```

위 관계는 원칙적으로 1:1로 관리한다.

4. 현행 구조와 문제점

4.1 단일 공통 ROOT 사용 구조

```
com.nh.nsight
├─ controller
├─ service
├─ dao
├─ mapper
├─ common
└─ util
```

문제점

- TCF 플랫폼과 업무 프로그램이 구분되지 않는다.
- 업무코드·WAR·도메인이 패키지에 나타나지 않는다.
- 모든 팀이 같은 service, common, util을 변경하게 된다.
- 패키지 단위 소유권과 접근통제를 적용하기 어렵다.
- 업무 WAR 분리 시 대규모 소스 이동이 필요하다.
4.2 업무코드가 없는 구조

```
com.nh.nsight.marketing.customer
com.nh.nsight.marketing.campaign
com.nh.nsight.marketing.product
```

문제점

하나의 도메인이 어느 업무 WAR에 배포되는지 알 수 없다.

예를 들어 customer 도메인이 IC, PC, SV 중 어디에 속하는지 식별하기 어렵다.

```
도메인명만으로는 부족하다.

업무코드
+ 도메인

두 단계가 함께 필요하다.
```

4.3 환경명이 포함된 구조

```
com.nh.nsight.marketing.prod.sv
com.nh.nsight.marketing.dev.sv
com.nh.nsight.marketing.dr.sv
```

문제점

- 동일 소스를 환경별로 복제하게 된다.
- 개발·검증·운영 간 클래스 경로가 달라진다.
- 환경 전환 시 빌드 산출물이 달라진다.
- 소스와 설정의 책임이 혼합된다.
환경 차이는 패키지가 아니라 다음 수단으로 관리한다.

```
application-{profile}.yml
환경변수
Secret·Config 저장소
배포 Pipeline 변수
```

4.4 서버·센터·포트가 포함된 구조

```
com.nh.nsight.center1.sv
com.nh.nsight.ap01.sv
com.nh.nsight.port8080.sv
```

물리 배치정보는 Java 프로그램의 논리적 책임이 아니므로 패키지에 포함하지 않는다.

5. 요구사항과 제약조건

5.1 기능 요구사항

| ID | 요구사항 |
| --- | --- |
| BASE-01 | 모든 내부 Java 소스는 승인된 ROOT BASE 아래에 위치해야 한다. |
| BASE-02 | TCF 플랫폼과 NSIGHT 업무 애플리케이션은 별도 하위 BASE를 사용해야 한다. |
| BASE-03 | 업무 애플리케이션은 업무코드를 패키지에 포함해야 한다. |
| BASE-04 | 업무코드 아래에는 도메인과 책임 계층이 나타나야 한다. |
| BASE-05 | 플랫폼 모듈은 플랫폼 기능과 세부 책임으로 구성해야 한다. |
| BASE-06 | Spring Boot 기동 클래스는 모듈 BASE에 위치해야 한다. |
| BASE-07 | 테스트 패키지는 운영 소스의 패키지 구조를 동일하게 따라야 한다. |
| BASE-08 | Java Mapper와 Mapper XML Namespace가 일치해야 한다. |
| BASE-09 | 다른 업무 WAR의 내부 패키지를 직접 Import할 수 없어야 한다. |
| BASE-10 | 패키지 위반은 CI/CD 단계에서 자동 차단되어야 한다. |

5.2 비기능 요구사항

| 항목 | 기준 |
| --- | --- |
| 가독성 | 전체 클래스명만으로 업무와 계층 식별 가능 |
| 확장성 | 신규 업무코드·도메인·모듈 추가 가능 |
| 운영성 | Stack Trace에서 담당 모듈과 업무 식별 가능 |
| 기동성 | Component Scan 범위를 최소화 |
| 안정성 | Bean·Mapper 중복 등록 방지 |
| 보안성 | 인증·키관리·내부 API 패키지 책임 분리 |
| 유지보수성 | 패키지 이동 없이 도메인 기능 확장 가능 |
| 자동화 | 정규식·ArchUnit·Gradle 규칙으로 검증 가능 |
| 호환성 | 패키지 변경 시 명시적 Migration 절차 적용 |
| 일관성 | Main·Test·Mapper XML 구조 동일 기준 적용 |

5.3 제약조건

- ROOT BASE는 프로젝트 단위로 임의 변경하지 않는다.
- com.nh와 kr.co.nh를 혼용하지 않는다.
- 환경명·서버명·센터명·포트명을 패키지에 포함하지 않는다.
- 일반 업무 프로그램을 com.nh.nsight.tcf 아래에 배치하지 않는다.
- TCF 플랫폼 구현을 com.nh.nsight.marketing 아래에 배치하지 않는다.
- 업무코드가 없는 업무 프로그램을 생성하지 않는다.
- ROOT 바로 아래에 common, util, helper, manager를 생성하지 않는다.
- 다른 업무 WAR 패키지를 직접 Import하지 않는다.
- 업무 패키지에 별도 REST Controller를 생성하지 않는다.
- Spring Scan 범위를 com.nh 전체로 설정하지 않는다.
- 운영과 테스트 코드에서 서로 다른 BASE 패키지를 사용하지 않는다.
- 패키지명에 개인명, 프로젝트 단계명, 날짜를 사용하지 않는다.
6. 설계 원칙

6.1 단일 ROOT BASE 원칙

NSIGHT의 전체 Java 내부 소스는 다음 ROOT 아래에 둔다.

```
com.nh.nsight
```

| 세그먼트 | 의미 | 변경 가능 여부 |
| --- | --- | --- |
| com | 역도메인 최상위 | 고정 |
| nh | 기관 식별 | 고정 |
| nsight | 시스템 식별 | 고정 |

다음과 같은 변형을 허용하지 않는다.

```
kr.co.nh.nsight
com.nonghyup.nsight
com.nh.nhnsight
com.nh.marketing
com.nh.tcf
```

6.2 플랫폼과 업무 분리 원칙

ROOT BASE 아래에서 플랫폼과 업무 애플리케이션을 분리한다.

```
com.nh.nsight
├─ tcf
└─ marketing
```

| BASE | 책임 |
| --- | --- |
| com.nh.nsight.tcf | TCF 공통 실행·웹·인증·연계·캐시·배치·운영 플랫폼 |
| com.nh.nsight.marketing | NSIGHT 마케팅·정보분석 업무 애플리케이션 |

```
TCF 플랫폼
= 여러 업무 WAR가 공통으로 사용하는 실행 기반

Marketing 업무
= 업무코드와 도메인에 따라 실행되는 업무 프로그램
```

6.3 모듈 BASE와 배포 단위 정합성 원칙

하나의 실행 모듈은 하나의 명확한 모듈 BASE를 가져야 한다.

| Gradle 모듈 | 배포 단위 | 모듈 BASE |
| --- | --- | --- |
| tcf-core | Library JAR | com.nh.nsight.tcf.core |
| tcf-web | Library JAR | com.nh.nsight.tcf.web |
| tcf-gateway | Gateway WAR/JAR | com.nh.nsight.tcf.gateway |
| sv-service | sv.war | com.nh.nsight.marketing.sv |
| ic-service | ic.war | com.nh.nsight.marketing.ic |
| cm-service | cm.war | com.nh.nsight.marketing.cm |
| tcf-om | om.war | com.nh.nsight.tcf.om |

동일한 모듈에 서로 무관한 BASE를 두지 않는다.

6.4 업무코드 우선 원칙

업무 애플리케이션은 업무 BASE 바로 아래에 업무코드를 둔다.

```
com.nh.nsight.marketing.{업무코드}
```

예:

```
com.nh.nsight.marketing.sv
com.nh.nsight.marketing.ic
com.nh.nsight.marketing.cm
com.nh.nsight.marketing.mg
```

업무코드는 다음 항목과 일치해야 한다.

```
업무코드
↔ Gradle 모듈
↔ WAR명
↔ Context Path
↔ ServiceId Prefix
↔ OM Service Catalog
↔ 로그 분류
```

6.5 도메인 우선 원칙

업무코드 아래에는 기술 계층보다 도메인을 먼저 둔다.

```
권장

com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.mapper
비권장

com.nh.nsight.marketing.sv.handler.customer
com.nh.nsight.marketing.sv.service.customer
com.nh.nsight.marketing.sv.mapper.customer
```

도메인 우선 구조는 업무 소유권, 변경 영향, ServiceId 정합성을 명확히 한다.

6.6 패키지와 의존성 일치 원칙

Java 패키지 의존관계는 Gradle 모듈 의존관계를 초과할 수 없다.

```
Gradle에서 의존하지 않는 모듈
→ Java Import도 금지

Gradle에서 의존하더라도 내부 패키지
→ 직접 Import 금지
```

업무 WAR 간 호출은 Java 클래스 참조가 아니라 표준 연계 계약으로 수행한다.

```
SV Service
  → IC Service 직접 Import: 금지

SV Client
  → tcf-eai
  → IC 표준 ServiceId 호출: 허용
```

7. 대안 비교 및 의사결정

7.1 BASE 패키지 대안

| 대안 | 구조 | 장점 | 단점 | 판단 |
| --- | --- | --- | --- | --- |
| A | com.nh.nsight.* 단일 평면 | 단순함 | 플랫폼·업무·모듈 경계 불명확 | 비권장 |
| B | com.nh.nsight.marketing.* 단일 업무 ROOT | 업무 구조 단순 | TCF 플랫폼 모듈 위치가 부자연스러움 | 비권장 |
| C | com.nh.nsight.tcf와 com.nh.nsight.marketing 분리 | 책임·소유권·Scan·자동검증 우수 | BASE가 두 갈래로 나뉨 | 최종 권장 |
| D | WAR별 독립 ROOT | 각 WAR 독립성 | 시스템 전체 일관성 상실 | 금지 |
| E | kr.co.nh.*와 com.nh.* 병행 | 기존 소스 수용 | 장기적 혼란과 Import 불일치 | 금지 |

7.2 최종 의사결정

```
ROOT BASE
└─ com.nh.nsight
   ├─ tcf
   │  └─ 플랫폼 모듈
   └─ marketing
      └─ 업무코드
         └─ 도메인
            └─ 계층
```

선정 이유는 다음과 같다.

| 판단 항목 | 결정 근거 |
| --- | --- |
| 시스템 식별 | 모든 내부 클래스가 NSIGHT 소속임을 표현 |
| 플랫폼 분리 | TCF 공통 기능이 업무 패키지에 침투하는 것을 방지 |
| 업무 확장 | 다수 업무코드를 동일한 구조로 확장 가능 |
| Scan 최적화 | 모듈 BASE별로 Spring Scan 범위 제한 가능 |
| 자동검증 | 플랫폼·업무·도메인·계층 규칙을 분리해 검사 가능 |
| 운영 추적 | 클래스명으로 모듈과 업무 담당자를 식별 |
| 변경 격리 | 특정 업무·도메인 변경을 해당 패키지에 제한 |
| 보안 통제 | JWT·Gateway·OM 등 민감 모듈의 접근 경계 명확화 |

8. 목표 BASE 패키지 아키텍처

8.1 전체 구조

```
com.nh.nsight
├─ tcf
│  ├─ util
│  ├─ core
│  ├─ web
│  ├─ gateway
│  ├─ jwt
│  ├─ eai
│  ├─ cache
│  ├─ batch
│  └─ om
│
└─ marketing
   ├─ cc
   ├─ ic
   ├─ pc
   ├─ bc
   ├─ ms
   ├─ sv
   ├─ pd
   ├─ cm
   ├─ eb
   ├─ ep
   ├─ bp
   ├─ bd
   ├─ ss
   ├─ cs
   ├─ ct
   └─ mg
```

8.2 TCF 플랫폼 BASE

| 모듈 | BASE 패키지 | 주요 책임 |
| --- | --- | --- |
| tcf-util | com.nh.nsight.tcf.util | 기술 중립 공통 유틸리티 |
| tcf-core | com.nh.nsight.tcf.core | TCF·STF·ETF·Dispatcher·Context |
| tcf-web | com.nh.nsight.tcf.web | 공통 Endpoint·Filter·Web 예외처리 |
| tcf-gateway | com.nh.nsight.tcf.gateway | 인증·라우팅·Proxy |
| tcf-jwt | com.nh.nsight.tcf.jwt | 토큰 발급·갱신·폐기·JWKS |
| tcf-eai | com.nh.nsight.tcf.eai | 업무·외부 시스템 표준 호출 |
| tcf-cache | com.nh.nsight.tcf.cache | Cache 추상화·정책 |
| tcf-batch | com.nh.nsight.tcf.batch | 공통 Batch 실행 구조 |
| tcf-om | com.nh.nsight.tcf.om | 운영 기준정보·거래통제·진단 |

OM 패키지 판단

tcf-om은 ServiceId 기준으로는 OM 업무코드를 사용하더라도 Java BASE는 다음과 같이 고정한다.

```
com.nh.nsight.tcf.om
```

다음 중복 구조는 사용하지 않는다.

```
com.nh.nsight.tcf.om
com.nh.nsight.marketing.om
```

별도 업무형 OM 시스템이 실제로 분리되는 경우에만 ADR과 모듈 분리 승인을 거쳐 marketing.om 사용 여부를 재검토한다.

8.3 업무 BASE

업무 애플리케이션의 표준 BASE 형식은 다음과 같다.

```
com.nh.nsight.marketing.{업무코드}
```

| 업무코드 | 업무 | BASE 패키지 |
| --- | --- | --- |
| CC | 공통 | com.nh.nsight.marketing.cc |
| IC | 고객통합 | com.nh.nsight.marketing.ic |
| PC | 개인고객 | com.nh.nsight.marketing.pc |
| BC | 기업고객 | com.nh.nsight.marketing.bc |
| MS | Mini Single View | com.nh.nsight.marketing.ms |
| SV | Single View | com.nh.nsight.marketing.sv |
| PD | 상품 | com.nh.nsight.marketing.pd |
| CM | 캠페인 | com.nh.nsight.marketing.cm |
| EB | EBM | com.nh.nsight.marketing.eb |
| EP | Event Processing | com.nh.nsight.marketing.ep |
| BP | Behavior Processing | com.nh.nsight.marketing.bp |
| BD | Behavior Data | com.nh.nsight.marketing.bd |
| SS | Sales Support | com.nh.nsight.marketing.ss |
| CS | Common Service | com.nh.nsight.marketing.cs |
| CT | Contact | com.nh.nsight.marketing.ct |
| MG | Management | com.nh.nsight.marketing.mg |

업무코드는 소문자를 사용하며 승인된 코드만 허용한다.

8.4 업무코드 BASE 하위 표준 구조

SV 업무 WAR 예시는 다음과 같다.

```
com.nh.nsight.marketing.sv
├─ NsightSvApplication.java
├─ config
├─ customer
│  ├─ handler
│  ├─ facade
│  ├─ service
│  ├─ rule
│  ├─ dao
│  ├─ mapper
│  ├─ dto
│  ├─ client
│  ├─ model
│  ├─ converter
│  └─ exception
├─ segment
├─ productholding
├─ analytics
├─ batch
└─ shared
```

| 패키지 | 사용 기준 |
| --- | --- |
| 기동 클래스 | 업무코드 BASE 바로 아래에 1개 |
| config | WAR 전역 설정만 배치 |
| 도메인 | 업무 규칙과 데이터 소유권 기준으로 생성 |
| batch | 해당 업무가 소유하는 배치 |
| shared | 동일 WAR 안에서만 공유하는 최소 구성요소 |
| common | 생성 금지 |
| util | 원칙적으로 생성 금지 |

9. 표준 형식

9.1 ROOT BASE 형식

```
com.nh.nsight
```

9.2 플랫폼 모듈 형식

```
com.nh.nsight.tcf.{모듈}.{세부책임}
```

예:

```
com.nh.nsight.tcf.core.transaction
com.nh.nsight.tcf.core.dispatcher
com.nh.nsight.tcf.web.filter
com.nh.nsight.tcf.gateway.routing
com.nh.nsight.tcf.jwt.issuer
com.nh.nsight.tcf.om.servicecatalog
```

9.3 업무 프로그램 형식

```
com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}[.{세부구분}]
```

예:

```
com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.rule
com.nh.nsight.marketing.sv.customer.dao
com.nh.nsight.marketing.sv.customer.mapper
com.nh.nsight.marketing.sv.customer.dto.request
com.nh.nsight.marketing.sv.customer.dto.response
```

9.4 패키지명 문자 기준

| 항목 | 기준 |
| --- | --- |
| 대소문자 | 영문 소문자만 사용 |
| 시작 문자 | 영문 소문자 |
| 구분자 | 마침표 . |
| 언더스코어 | 금지 |
| 하이픈 | 금지 |
| 한글 | 금지 |
| 숫자 | 표준 업무용어에 필요한 경우만 제한 허용 |
| 단수·복수 | 도메인은 원칙적으로 단수형 |
| 약어 | 승인된 업무코드와 표준 기술 약어만 허용 |
| 환경명 | 금지 |
| 개인명 | 금지 |
| 버전명 | 금지 |
| 날짜 | 금지 |

9.5 권장 패키지 깊이

| 구조 | 세그먼트 수 |
| --- | --- |
| ROOT BASE | 3 |
| 플랫폼 모듈 BASE | 5 |
| 업무코드 BASE | 5 |
| 업무 도메인 | 6 |
| 업무 계층 | 7 |
| DTO 세부구분 | 8 |

원칙적으로 9단계 이상 깊어지지 않도록 한다.

```
권장 최대 예

com.nh.nsight.marketing.sv.customer.dto.request
```

다음과 같은 과도한 구조는 피한다.

```
com.nh.nsight.marketing.sv.customer.inquiry.online.dto.request.internal
```

10. 구성요소 및 속성

10.1 Spring Boot 기동 클래스

기동 클래스는 해당 실행 모듈 BASE에 위치한다.

```
package com.nh.nsight.marketing.sv;

@SpringBootApplication
public class NsightSvApplication {

    public static void main(String[] args) {
        SpringApplication.run(NsightSvApplication.class, args);
    }
}
```

플랫폼 실행 모듈도 같은 기준을 적용한다.

```
package com.nh.nsight.tcf.gateway;

@SpringBootApplication
public class TcfGatewayApplication {
}
```

10.2 Component Scan 기준

목표 기준

Spring Boot 기동 클래스가 모듈 BASE에 위치하도록 하여 별도의 광범위한 Scan 설정을 최소화한다.

```
NsightSvApplication 위치
= com.nh.nsight.marketing.sv

기본 Component Scan
= com.nh.nsight.marketing.sv 하위
```

TCF 공통 기능은 가능한 한 Spring Boot Auto Configuration 또는 명시적 @Import로 제공한다.

금지

```
@SpringBootApplication(
    scanBasePackages = "com.nh"
)
@ComponentScan("com.nh.nsight")
```

제한적 과도기 허용

기존 구조상 명시적 Scan이 필요한 경우 대상 모듈을 정확히 지정한다.

```
@SpringBootApplication(
    scanBasePackages = {
        "com.nh.nsight.marketing.sv",
        "com.nh.nsight.tcf.web"
    }
)
```

com.nh.nsight.tcf 전체를 일괄 Scan하지 않는다.

10.3 Mapper Scan 기준

업무 Mapper는 해당 업무코드 BASE로 제한한다.

```
@MapperScan(
    basePackages = "com.nh.nsight.marketing.sv",
    annotationClass = Mapper.class
)
```

더 엄격한 방식은 도메인별 Mapper Marker를 사용하는 것이다.

```
com.nh.nsight.marketing.sv.customer.mapper
com.nh.nsight.marketing.sv.segment.mapper
```

다른 업무코드의 Mapper를 함께 Scan하지 않는다.

10.4 Mapper XML Namespace

```
Java Mapper:
com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper

XML 위치:
mapper/sv/customer/SvCustomerMapper.xml

Namespace:
com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper
```

다음 세 항목은 자동으로 정합성을 검증한다.

```
Java Interface 전체명
= XML Namespace
= 리소스 경로의 업무코드·도메인
```

10.5 테스트 BASE

테스트 소스는 운영 소스와 동일한 패키지를 사용한다.

```
src/main/java
└─ com.nh.nsight.marketing.sv.customer.service

src/test/java
└─ com.nh.nsight.marketing.sv.customer.service
```

다음 구조는 금지한다.

```
com.nh.nsight.marketing.sv.test.customer
com.nh.nsight.test.sv.customer
```

11. 책임 경계와 RACI

11.1 책임 경계

| 영역 | 책임 |
| --- | --- |
| com.nh.nsight | 시스템 전체 Namespace |
| com.nh.nsight.tcf | TCF 플랫폼팀 소유 |
| com.nh.nsight.marketing | NSIGHT 업무 애플리케이션 영역 |
| 업무코드 BASE | 해당 업무팀 소유 |
| 도메인 패키지 | 도메인 담당팀 소유 |
| config | 모듈 기술 설정 |
| shared | 동일 WAR 내부 공유만 허용 |
| 공개 계약 | 명시적으로 승인된 API·Contract만 외부 공개 |
| 내부 구현 | 다른 모듈 직접 참조 금지 |

11.2 RACI

| 활동 | 아키텍처팀 | TCF팀 | 업무팀 | DevOps·품질 | 운영팀 |
| --- | --- | --- | --- | --- | --- |
| ROOT BASE 결정 | A/R | C | I | C | I |
| 플랫폼 BASE 결정 | A | R | I | C | I |
| 업무코드 BASE 등록 | A | C | R | C | I |
| 도메인 패키지 생성 | A | C | R | I | I |
| 공통 패키지 승인 | A | R | C | I | I |
| Component Scan 설정 | C | A/R | R | C | I |
| Mapper Scan 설정 | C | C | A/R | C | I |
| 자동검증 규칙 | A | R | C | R | I |
| 패키지 이동 승인 | A/R | C | R | C | I |
| 장애 소유권 매핑 | C | C | R | I | A/R |

12. 정상 처리 흐름

12.1 신규 업무 거래 개발

```
1. 업무코드 확인
   SV

2. 도메인 확인
   customer

3. ServiceId 확정
   SV.Customer.selectSummary

4. 모듈 BASE 확인
   com.nh.nsight.marketing.sv

5. 도메인 BASE 생성
   com.nh.nsight.marketing.sv.customer

6. 책임 계층 생성
   handler
   facade
   service
   rule
   dao
   mapper

7. DTO 패키지 생성
   dto.request
   dto.response
   dto.query
   dto.result

8. Mapper XML 경로 생성
   mapper/sv/customer

9. ArchUnit·Checkstyle 검증

10. OM Catalog·거래설계서와 연결
```

12.2 신규 TCF 플랫폼 기능 개발

```
1. 플랫폼 모듈 확인
   tcf-core

2. 책임 영역 확인
   timeout

3. BASE 확인
   com.nh.nsight.tcf.core

4. 세부 패키지 생성
   com.nh.nsight.tcf.core.transaction.timeout

5. 공개 API와 내부 구현 분리

6. 업무 패키지 참조 금지 확인

7. 단위·아키텍처 테스트 수행

8. 플랫폼 버전과 변경이력 관리
```

13. 오류·Timeout·장애 흐름

BASE 패키지는 직접 Timeout을 처리하지 않지만 잘못된 구조가 런타임 장애를 유발할 수 있다.

13.1 Bean 미등록

```
기동 클래스 위치 오류
→ Component Scan 대상에서 Handler 제외
→ Handler Bean 미등록
→ Dispatcher가 ServiceId Handler를 찾지 못함
→ TCF-HANDLER-NOT-FOUND
```

조치:

- Bootstrap Class가 모듈 BASE에 있는지 확인한다.
- Handler 패키지가 업무코드 BASE 아래인지 확인한다.
- 임의의 Scan 범위 설정을 제거한다.
13.2 Bean 중복

```
Component Scan 범위 과다
→ 다른 업무 WAR 또는 공통 구현까지 Scan
→ 동일 Bean Name 중복
→ 애플리케이션 기동 실패
```

조치:

- com.nh 또는 com.nh.nsight 전체 Scan을 금지한다.
- TCF 기능은 Auto Configuration으로 제공한다.
- 업무별 Scan 범위를 업무코드 BASE로 제한한다.
13.3 Mapper 미등록

```
Mapper Interface 위치 불일치
→ Mapper Scan 누락
→ DAO 호출
→ NoSuchBeanDefinitionException
```

조치:

- Mapper를 {업무코드}.{도메인}.mapper에 배치한다.
- Mapper Scan 범위를 업무코드 BASE로 제한한다.
- XML Namespace와 Java 전체명을 검증한다.
13.4 순환 의존

```
SV Service
→ IC Service 직접 Import
→ IC Service가 다시 SV 참조
→ Gradle·Spring 순환 의존
→ 빌드 또는 기동 실패
```

조치:

- 업무 WAR 간 Java Import를 제거한다.
- 표준 ServiceId·Client·tcf-eai 연계로 전환한다.
- 공통 계약이 필요하면 별도 승인된 Contract 모듈로 분리한다.
13.5 다중 WAR ClassLoader 문제

공통 Library를 Tomcat 전역과 WAR 내부에 동시에 배치하면 클래스 충돌이 발생할 수 있다.

```
$CATALINA_BASE/lib
  tcf-core.jar

sv.war/WEB-INF/lib
  tcf-core.jar

→ 서로 다른 ClassLoader의 동일 클래스
→ ClassCastException 또는 LinkageError
```

BASE 패키지 정합성과 함께 Library 배치 기준을 통제해야 한다.

14. 정상 예시

14.1 SV 고객 도메인

```
com.nh.nsight.marketing.sv.customer.handler.SvCustomerHandler
com.nh.nsight.marketing.sv.customer.facade.SvCustomerFacade
com.nh.nsight.marketing.sv.customer.service.SvCustomerService
com.nh.nsight.marketing.sv.customer.rule.SvCustomerInquiryRule
com.nh.nsight.marketing.sv.customer.dao.SvCustomerDao
com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper
com.nh.nsight.marketing.sv.customer.dto.request.SvCustomerSummaryRequest
com.nh.nsight.marketing.sv.customer.dto.response.SvCustomerSummaryResponse
```

14.2 캠페인 도메인

```
com.nh.nsight.marketing.cm.campaign.handler.CmCampaignHandler
com.nh.nsight.marketing.cm.campaign.facade.CmCampaignFacade
com.nh.nsight.marketing.cm.campaign.service.CmCampaignService
com.nh.nsight.marketing.cm.campaign.rule.CmCampaignApprovalRule
com.nh.nsight.marketing.cm.campaign.dao.CmCampaignDao
com.nh.nsight.marketing.cm.campaign.mapper.CmCampaignMapper
```

14.3 Gateway

```
com.nh.nsight.tcf.gateway.controller
com.nh.nsight.tcf.gateway.routing
com.nh.nsight.tcf.gateway.authentication
com.nh.nsight.tcf.gateway.proxy
com.nh.nsight.tcf.gateway.logging
com.nh.nsight.tcf.gateway.error
com.nh.nsight.tcf.gateway.config
```

14.4 JWT

```
com.nh.nsight.tcf.jwt.issuer
com.nh.nsight.tcf.jwt.validator
com.nh.nsight.tcf.jwt.jwks
com.nh.nsight.tcf.jwt.refresh
com.nh.nsight.tcf.jwt.revoke
com.nh.nsight.tcf.jwt.key
com.nh.nsight.tcf.jwt.audit
com.nh.nsight.tcf.jwt.config
```

15. 금지 예시

15.1 ROOT 불일치

```
kr.co.nh.nsight.sv
com.nh.marketing.sv
com.nh.nsightframework.core
```

수정:

```
com.nh.nsight.marketing.sv
com.nh.nsight.tcf.core
```

15.2 업무코드 누락

```
com.nh.nsight.marketing.customer.service
```

수정:

```
com.nh.nsight.marketing.sv.customer.service
```

15.3 광역 공통 패키지

```
com.nh.nsight.common
com.nh.nsight.common.util
com.nh.nsight.marketing.common.service
```

수정 기준:

| 실제 책임 | 이동 위치 |
| --- | --- |
| TCF 기술 공통 | com.nh.nsight.tcf.util 또는 해당 TCF 모듈 |
| SV 내부 공통 | com.nh.nsight.marketing.sv.shared |
| 고객 업무 기능 | com.nh.nsight.marketing.sv.customer |
| 외부 연계 | 해당 도메인의 client |
| 설정 | 해당 모듈의 config |

15.4 의미 없는 패키지

```
util
helper
manager
misc
temp
test2
new
old
impl2
```

클래스의 실제 책임이 드러나는 패키지로 이동한다.

15.5 환경 포함

```
com.nh.nsight.marketing.sv.prod.customer
com.nh.nsight.marketing.sv.dev.customer
```

수정:

```
com.nh.nsight.marketing.sv.customer
```

환경 차이는 설정으로 관리한다.

15.6 업무 WAR 직접 의존

```
import com.nh.nsight.marketing.ic.customer.service.IcCustomerService;
```

SV에서 IC 업무를 호출해야 하는 경우 다음처럼 구현한다.

```
com.nh.nsight.marketing.sv.customer.client.IcCustomerClient
  → tcf-eai
  → IC.Customer.selectProfile
```

16. 연계 규칙

16.1 ServiceId 연계

패키지와 ServiceId는 다음처럼 정합성을 가져야 한다.

```
ServiceId
SV.Customer.selectSummary

패키지
com.nh.nsight.marketing.sv.customer.handler
```

| ServiceId 구성 | 패키지 구성 |
| --- | --- |
| SV | sv |
| Customer | customer |
| selectSummary | Handler·Facade 메서드 또는 거래 매핑 |

ServiceId의 도메인과 패키지 도메인이 다르면 설계검토 대상이다.

16.2 Gradle 연계

```
업무 WAR
→ tcf-web
→ tcf-core
→ tcf-util
```

금지 예:

```
sv-service
→ ic-service 직접 project dependency
```

허용 예:

```
sv-service
→ tcf-eai
→ 표준 호출
```

16.3 OM Service Catalog 연계

OM에는 다음 항목을 함께 관리한다.

| 항목 | 예시 |
| --- | --- |
| 업무코드 | SV |
| ServiceId | SV.Customer.selectSummary |
| Handler Class | 전체 클래스명 |
| Module BASE | com.nh.nsight.marketing.sv |
| Domain BASE | com.nh.nsight.marketing.sv.customer |
| 배포 WAR | sv.war |
| Context Path | /sv |
| 담당팀 | SV 고객업무팀 |

16.4 로그 연계

로그의 Logger Name은 클래스의 전체 패키지명을 사용한다.

```
com.nh.nsight.marketing.sv.customer.service.SvCustomerService
```

이를 통해 로그 검색 시 다음 조건을 사용할 수 있다.

```
업무 전체:
com.nh.nsight.marketing.sv

고객 도메인:
com.nh.nsight.marketing.sv.customer

Mapper:
com.nh.nsight.marketing.sv.customer.mapper
```

17. 데이터 및 상태관리

17.1 업무 WAR 간 객체 공유 금지

다른 업무 WAR의 DTO·Entity·Result 객체를 직접 공유하지 않는다.

```
SV Customer DTO
≠ IC Customer DTO
```

업무 간 데이터 전달은 표준 계약 DTO 또는 표준 전문으로 변환한다.

17.2 Static 상태 제한

업무 BASE 아래의 클래스에서 가변 Static 상태를 관리하지 않는다.

금지:

```
private static final Map<String, Object> USER_CACHE = new HashMap<>();
```

상태관리는 다음 전용 구조를 사용한다.

- Spring 관리 Cache
- DB
- 외부 Cache 저장소
- TCF TransactionContext
- 승인된 Registry
17.3 ThreadLocal 책임

ThreadLocal은 업무 패키지에서 임의 생성하지 않는다.

거래 문맥은 tcf-core가 관리하며 요청 종료 시 반드시 정리한다.

```
TCF Context 생성
→ 업무 처리
→ ETF 종료
→ finally
→ ContextHolder.clear()
→ MDC.clear()
```

18. 성능·용량·확장성

18.1 Component Scan 최소화

전체 조직 패키지를 Scan하면 다음 문제가 발생한다.

- 기동시간 증가
- 불필요한 Bean Definition 증가
- Bean 이름 충돌
- 테스트 Context 과대화
- 다른 업무 설정 오등록
- 메모리 사용 증가
따라서 실행 애플리케이션은 자기 모듈 BASE만 기본 Scan한다.

18.2 대규모 도메인 확장

특정 도메인이 커지면 다음 순서로 확장한다.

```
1단계
업무코드 하위 단일 도메인

2단계
도메인 하위 서브도메인 분리

3단계
독립 Gradle 업무 모듈 분리

4단계
별도 WAR·실행 서비스 분리
```

단순히 패키지 깊이를 계속 늘리는 방식은 사용하지 않는다.

18.3 공통화 판단

두 개 이상의 업무에서 동일한 코드가 발견되었다고 즉시 TCF 공통으로 이동하지 않는다.

공통화 판단 기준:

| 기준 | 질문 |
| --- | --- |
| 의미 동일성 | 이름뿐 아니라 업무 의미가 동일한가 |
| 변경주기 | 모든 사용처가 함께 변경되는가 |
| 소유권 | 단일 책임 조직을 정할 수 있는가 |
| 플랫폼성 | 특정 업무 지식 없이 사용할 수 있는가 |
| 안정성 | API 호환성을 장기간 유지할 수 있는가 |

업무 의미가 포함되면 업무 도메인에 유지한다.

19. 보안·개인정보·감사

19.1 보안 기능 소유권

| 기능 | 패키지 |
| --- | --- |
| JWT 발급 | com.nh.nsight.tcf.jwt.issuer |
| JWT 검증 | com.nh.nsight.tcf.jwt.validator 또는 tcf.web.security |
| Gateway 인증 | com.nh.nsight.tcf.gateway.authentication |
| 키 관리 | com.nh.nsight.tcf.jwt.key |
| 권한 문맥 | com.nh.nsight.tcf.core.context |
| 업무 데이터권한 | 해당 업무 도메인의 rule·service |
| 감사로그 | tcf.om.audit 및 업무 감사 연계 |

업무 Service에서 JWT 문자열을 직접 파싱하지 않는다.

19.2 Secret 저장 금지

다음 패키지의 클래스에 Secret 값을 상수로 저장하지 않는다.

```
constant
config
util
security
```

금지 대상:

- JWT Private Key
- DB 비밀번호
- API Secret
- 인증서 비밀번호
- 운영 사용자정보
- 개인정보 샘플
Secret은 KMS·Vault·환경변수·승인된 Secret 저장소에서 공급한다.

19.3 개인정보 DTO 분리

개인정보를 포함한 외부 응답 DTO와 내부 조회 Result DTO를 구분한다.

```
mapper result
→ service 업무처리
→ masking
→ response DTO
```

DB Result 객체를 화면 응답으로 직접 반환하지 않는다.

20. 운영·모니터링·장애 대응

20.1 패키지 소유자 관리

운영관리 기준정보에 BASE 패키지별 소유자를 등록한다.

| BASE 패키지 | 소유 조직 |
| --- | --- |
| com.nh.nsight.tcf.core | TCF Core팀 |
| com.nh.nsight.tcf.web | TCF Web팀 |
| com.nh.nsight.tcf.gateway | Gateway팀 |
| com.nh.nsight.tcf.jwt | 인증·보안팀 |
| com.nh.nsight.tcf.om | OM 운영관리팀 |
| com.nh.nsight.marketing.sv | SV 업무팀 |
| com.nh.nsight.marketing.sv.customer | SV 고객 도메인팀 |

20.2 장애 분석

Stack Trace의 최초 업무 클래스를 기준으로 담당 영역을 분류한다.

```
com.nh.nsight.tcf.*
→ TCF 플랫폼 장애 후보

com.nh.nsight.marketing.sv.*
→ SV 업무 장애 후보

com.nh.nsight.marketing.sv.customer.mapper.*
→ SV 고객 SQL·Mapper 장애 후보
```

다만 실제 근본 원인은 거래 GUID·ServiceId·SQL ID·외부 호출 정보를 함께 확인하여 판단한다.

20.3 로그 레벨 관리

패키지 단위 로그 레벨을 사용할 수 있다.

```
logging:
  level:
    com.nh.nsight.marketing.sv: INFO
    com.nh.nsight.marketing.sv.customer: DEBUG
    com.nh.nsight.marketing.sv.customer.mapper: TRACE
```

운영환경의 Mapper TRACE는 개인정보와 SQL Parameter 노출 가능성이 있으므로 제한적으로 적용한다.

21. 자동검증 및 품질 Gate

21.1 패키지명 정규식

업무 프로그램 기본 패턴 예시는 다음과 같다.

```
^com\.nh\.nsight\.marketing\.(cc|ic|pc|bc|ms|sv|pd|cm|eb|ep|bp|bd|ss|cs|ct|mg)\.[a-z][a-z0-9]{2,19}\.[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$
```

플랫폼 프로그램 기본 패턴:

```
^com\.nh\.nsight\.tcf\.(util|core|web|gateway|jwt|eai|cache|batch|om)(\.[a-z][a-z0-9]*)+$
```

Bootstrap Class는 모듈 BASE 바로 아래에 위치할 수 있으므로 별도 예외 규칙을 둔다.

21.2 Checkstyle 검증

| 검증 항목 | 실패 처리 |
| --- | --- |
| 대문자 포함 | 빌드 실패 |
| 언더스코어·하이픈 포함 | 빌드 실패 |
| 미승인 업무코드 | 빌드 실패 |
| 금지 패키지명 사용 | 빌드 실패 |
| ROOT BASE 불일치 | 빌드 실패 |
| 환경명 포함 | 빌드 실패 |
| 최대 깊이 초과 | 경고 또는 승인 필요 |
| 테스트 BASE 불일치 | 빌드 실패 |

금지 패키지명:

```
common
common2
util
helper
manager
misc
temp
new
old
test2
impl2
```

tcf-util의 공식 BASE인 com.nh.nsight.tcf.util은 예외적으로 허용한다.

21.3 ArchUnit 검증

```
업무 패키지는 tcf 공개 패키지에 의존할 수 있다.

tcf 패키지는 marketing 업무 패키지에 의존할 수 없다.

서로 다른 업무코드 패키지는 직접 의존할 수 없다.

handler는 facade만 호출한다.

facade는 service를 호출한다.

service는 rule·dao·client를 호출한다.

rule은 dao·mapper·client에 의존할 수 없다.

dao는 mapper를 호출한다.

mapper는 상위 업무 계층에 의존할 수 없다.
```

예시:

```
noClasses()
    .that().resideInAPackage("com.nh.nsight.tcf..")
    .should().dependOnClassesThat()
    .resideInAPackage("com.nh.nsight.marketing..");
noClasses()
    .that().resideInAPackage("com.nh.nsight.marketing.sv..")
    .should().dependOnClassesThat()
    .resideInAPackage("com.nh.nsight.marketing.ic..");
```

21.4 Gradle 품질 Gate

| Gate | 검증 |
| --- | --- |
| G1 Compile | 잘못된 Import와 패키지 선언 검출 |
| G2 Package Naming | ROOT·업무코드·도메인·계층 형식 검증 |
| G3 Architecture Test | ArchUnit 의존성 검증 |
| G4 Mapper Validation | Mapper Interface·XML Namespace 정합성 |
| G5 Spring Context | Bean 중복·미등록 검사 |
| G6 Service Catalog | Handler 전체명과 OM 등록정보 정합성 |
| G7 Dependency Check | 업무 WAR 간 직접 Gradle 의존 검출 |
| G8 Release Gate | 미승인 BASE 변경 차단 |

22. 테스트 시나리오

| ID | 테스트 | 기대 결과 |
| --- | --- | --- |
| PKG-01 | 정상 업무 패키지 빌드 | 성공 |
| PKG-02 | kr.co.nh ROOT 사용 | 빌드 실패 |
| PKG-03 | 승인되지 않은 업무코드 사용 | 빌드 실패 |
| PKG-04 | 업무코드 없는 업무 Service | 빌드 실패 |
| PKG-05 | common.util 생성 | 품질 Gate 실패 |
| PKG-06 | SV에서 IC Service 직접 Import | ArchUnit 실패 |
| PKG-07 | TCF Core가 업무 DTO 참조 | ArchUnit 실패 |
| PKG-08 | Handler가 Mapper 직접 호출 | ArchUnit 실패 |
| PKG-09 | Bootstrap Class가 BASE 외부 위치 | Spring Context 테스트 실패 |
| PKG-10 | Mapper XML Namespace 불일치 | Mapper 검증 실패 |
| PKG-11 | Main·Test 패키지 불일치 | 품질 Gate 실패 |
| PKG-12 | scanBasePackages="com.nh" 사용 | 정적검사 실패 |
| PKG-13 | 업무코드 BASE만 Scan | 정상 기동 |
| PKG-14 | 동일 Bean 중복 등록 | Context 테스트 실패 |
| PKG-15 | 패키지 변경 후 OM Handler 경로 미변경 | 배포 Gate 실패 |

23. 체크리스트

23.1 설계 체크리스트

| 확인 항목 | 결과 |
| --- | --- |
| ROOT BASE가 com.nh.nsight인가 | □ |
| 플랫폼과 업무 BASE가 분리되어 있는가 | □ |
| Gradle 모듈과 Module BASE가 일치하는가 | □ |
| 업무 WAR에 업무코드가 포함되어 있는가 | □ |
| 업무코드 아래 도메인이 존재하는가 | □ |
| 도메인 아래 표준 계층을 사용하는가 | □ |
| 다른 업무 WAR를 직접 참조하지 않는가 | □ |
| common, util, helper를 무분별하게 사용하지 않는가 | □ |
| 기동 클래스가 Module BASE에 위치하는가 | □ |
| Component Scan 범위가 최소화되어 있는가 | □ |
| Mapper Scan이 업무 범위로 제한되어 있는가 | □ |
| Java Mapper와 XML Namespace가 일치하는가 | □ |
| 테스트 패키지가 운영 소스와 동일한가 | □ |
| 환경·서버·센터명이 포함되지 않았는가 | □ |
| 패키지 소유자가 지정되어 있는가 | □ |

23.2 코드 리뷰 체크리스트

| 확인 항목 | 결과 |
| --- | --- |
| 클래스가 실제 책임에 맞는 패키지에 있는가 | □ |
| 업무코드와 ServiceId Prefix가 일치하는가 | □ |
| ServiceId 도메인과 패키지 도메인이 일치하는가 | □ |
| Handler가 Facade 외 계층을 호출하지 않는가 | □ |
| Rule이 DB·Cache·외부 API를 호출하지 않는가 | □ |
| DAO가 다른 도메인의 Mapper를 직접 호출하지 않는가 | □ |
| 외부 호출이 client 패키지에 격리되어 있는가 | □ |
| DTO가 목적별로 구분되어 있는가 | □ |
| 가변 Static 상태가 없는가 | □ |
| Secret이 소스에 포함되지 않았는가 | □ |
| 신규 공통 패키지에 승인 근거가 있는가 | □ |

24. 변경·호환성·폐기 관리

24.1 신규 BASE 등록

새로운 플랫폼 모듈 또는 업무코드를 추가할 때 다음 정보를 등록한다.

| 항목 | 예시 |
| --- | --- |
| 모듈명 | tcf-notification |
| BASE 패키지 | com.nh.nsight.tcf.notification |
| 책임 | 공통 알림 전송 |
| 소유 조직 | TCF 플랫폼팀 |
| 공개 API | Contract Interface |
| 의존 가능 모듈 | tcf-core, tcf-util |
| 금지 의존 | 모든 업무 WAR |
| 승인 ADR | ADR-TCF-xxx |

24.2 패키지 이동

패키지 이동은 클래스명 변경 이상의 영향이 있다.

- Java Import
- Spring Bean 이름
- Component Scan
- Mapper Namespace
- XML 리소스 경로
- 직렬화 Type 정보
- Reflection 설정
- OM Handler Class 등록정보
- 운영 로그 검색조건
- 테스트 Mock 경로
- GraalVM 또는 AOT 설정
- 외부 플러그인 참조
따라서 다음 절차를 따른다.

```
변경 요청
→ 영향도 분석
→ ADR 승인
→ 코드·XML·OM·문서 동시 변경
→ 호환 Adapter 검토
→ 통합시험
→ 운영 배포
→ 기존 경로 폐기
```

24.3 호환성 전환

외부 모듈에서 참조하는 공개 API의 패키지를 이동해야 하는 경우 즉시 삭제하지 않는다.

```
기존 공개 Interface
→ Deprecated 선언
→ 신규 Interface로 위임
→ 사용처 Migration
→ 사용량 확인
→ 다음 Major Version에서 폐기
```

내부 구현 클래스는 공개 API보다 짧은 전환기간을 적용할 수 있지만, OM 등록정보와 Reflection 참조를 반드시 확인한다.

24.4 금지된 이중 운영

다음과 같이 동일 기능을 구·신 패키지에 중복 구현하지 않는다.

```
com.nh.nsight.sv.customer.CustomerService
com.nh.nsight.marketing.sv.customer.service.SvCustomerService
```

과도기에는 기존 클래스가 신규 클래스로 위임하도록 하고 실제 구현은 한 곳에만 둔다.

25. 시사점

25.1 핵심 아키텍처 판단

첫째, NSIGHT 전체의 최상위 ROOT BASE는 com.nh.nsight로 고정해야 한다.

둘째, TCF 플랫폼과 업무 애플리케이션은 각각 다음 BASE로 분리해야 한다.

```
TCF 플랫폼
= com.nh.nsight.tcf

NSIGHT 업무
= com.nh.nsight.marketing
```

셋째, 업무 프로그램은 반드시 다음 순서로 구성해야 한다.

```
업무코드
→ 업무 도메인
→ 책임 계층
```

넷째, 패키지 구조는 단순한 명명 표준이 아니라 Spring Scan, Gradle 의존성, ServiceId, OM Catalog, Mapper XML, 로그, 장애 소유권을 연결하는 아키텍처 통제 기준이다.

25.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 기존 소스의 ROOT 혼재 | 대규모 Migration 필요 |
| 광범위 Component Scan | Bean 충돌과 기동 성능 저하 |
| 업무 WAR 직접 의존 | 순환 의존과 독립 배포 불가 |
| common 확대 | 책임과 소유권 불명확 |
| Mapper Namespace 불일치 | 런타임 SQL 실행 장애 |
| OM Handler 경로 미동기화 | 배포 후 거래 실행 불가 |
| OM 패키지 이중화 | tcf.om과 marketing.om 책임 충돌 |
| 패키지 이동의 영향 누락 | 운영환경에서만 발생하는 Reflection 오류 |

25.3 우선 보완 과제

| 우선순위 | 과제 |
| --- | --- |
| 1 | 현재 전체 소스의 실제 BASE 패키지 목록 추출 |
| 2 | com.nh.nsight 외 ROOT 사용 현황 식별 |
| 3 | 플랫폼·업무 패키지 혼재 현황 분석 |
| 4 | common, util, helper, manager 사용 현황 분석 |
| 5 | 업무 WAR 간 직접 Import와 Gradle 의존 검사 |
| 6 | Spring Component Scan·Mapper Scan 범위 점검 |
| 7 | Java Mapper와 XML Namespace 정합성 검사 |
| 8 | OM Handler Class 등록정보와 실제 클래스 비교 |
| 9 | Checkstyle·ArchUnit 품질 Gate 구현 |
| 10 | 단계적 Package Migration 계획 수립 |

25.4 중장기 발전 방향

초기에는 패키지 규칙을 정적검사하는 수준으로 적용한다.

이후에는 다음 단계로 발전시킨다.

```
1단계
패키지명과 ROOT BASE 자동검증

2단계
계층·업무코드 간 의존성 자동검증

3단계
ServiceId·Handler·패키지 정합성 검증

4단계
Gradle 모듈·WAR·Context·OM Catalog 통합검증

5단계
도메인 소유권과 변경 영향 자동분석
```

최종적으로는 신규 프로그램 생성 시 업무코드·도메인·계층을 선택하면 표준 패키지와 클래스가 자동 생성되는 TCF 개발 템플릿을 제공하는 것이 적절하다.

26. 마무리말

BASE 패키지는 소스 디렉터리의 첫 번째 줄이 아니라 NSIGHT TCF 전체 구조를 지탱하는 최상위 경계다.

최종 기준은 다음과 같다.

```
ROOT BASE
com.nh.nsight
TCF 플랫폼
com.nh.nsight.tcf.{플랫폼모듈}.{세부책임}
NSIGHT 업무
com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}
```

그리고 다음 원칙을 함께 지켜야 한다.

```
플랫폼과 업무를 분리한다.

업무코드를 생략하지 않는다.

도메인을 계층보다 먼저 둔다.

다른 업무 WAR를 직접 참조하지 않는다.

Component Scan과 Mapper Scan을 최소화한다.

공통 패키지는 승인 없이 만들지 않는다.

패키지 구조를 CI/CD에서 자동검증한다.

패키지 변경은 코드·Mapper·OM·로그·문서를 함께 변경한다.
```

이 기준이 적용되면 소스의 위치만으로 업무, 도메인, 프로그램 책임, 배포 모듈과 담당 조직을 식별할 수 있으며, 17개 업무 WAR와 TCF 플랫폼 모듈을 일관된 구조로 확장할 수 있다.

