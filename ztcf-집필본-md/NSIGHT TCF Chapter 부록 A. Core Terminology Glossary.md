<!-- source: ztcf-집필본/NSIGHT TCF Chapter 부록 A. Core Terminology Glossary.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 확장 부록 A. 핵심 용어사전

## 도입 전 안내말

NSIGHT TCF를 처음 접하면 기술 자체보다 용어 때문에 어려움을 느끼기 쉽다.

\`\`\`text id=“glsA001” ServiceId와 거래코드는 무엇이 다른가?

Service와 Facade는 왜 나뉘는가?

DAO·Repository·Mapper는 같은 것인가?

GUID·TraceId·Correlation ID는 어떻게 다른가?

Timeout과 Retry를 함께 적용해도 되는가?

Rollback과 보상처리는 같은 것인가?

Health가 UP인데 왜 업무거래는 실패하는가?



용어를 단순히 암기하면 실제 개발과 장애 상황에서 적용하기 어렵다.

각 용어는 다음 네 가지 관점에서 이해해야 한다.

\`\`\`text id="glsA002"
무엇인가?

TCF 처리 흐름의 어디에 있는가?

어떤 책임을 가지는가?

운영에서 무엇을 확인해야 하는가?

예를 들어 ServiceId는 단순한 문자열이 아니다.

\`\`\`text id=“glsA003” 화면 이벤트

→ ServiceId

→ Handler

→ 업무 프로그램

→ 권한·Timeout

→ 거래로그·운영통제



를 연결하는 핵심 식별자다.

\`Transaction\`도 단순한 \`@Transactional\` Annotation이 아니다.

\`\`\`text id="glsA004"
어떤 데이터 변경을

어디서 시작해

어떤 오류에서 Rollback하고

외부 호출과 어떤 경계로 분리할 것인지

를 결정하는 설계 개념이다.

이 부록은 다음 목적으로 사용한다.

\`\`\`text id=“glsA005” 본문을 읽다가 모르는 용어를 빠르게 찾는다.

설계회의에서 같은 단어를 같은 의미로 사용한다.

코드리뷰에서 책임 경계를 확인한다.

장애 상황에서 지표와 원인을 구분한다.

개발자·운영자·아키텍트 간 의사소통 오류를 줄인다.



프로젝트에서 실제 적용되는 정의와 설정값은 본 부록의 일반 설명보다 프로젝트 표준·소스·운영 설정을 우선한다.

\---

\# 부록 개요

\## 목적

본 부록의 목적은 NSIGHT TCF 설계·개발·테스트·배포·운영에 사용되는 핵심 용어를 초보자가 이해할 수 있는 표현으로 정리하고, 각 용어의 실무 확인점을 제공하는 것이다.

\## 적용범위

\`\`\`text id="glsA006"
TCF 거래처리

애플리케이션 계층

식별자·전문·계약

데이터·Transaction·동시성

오류·Timeout·복원력

인증·인가·보안

Cache·Batch·파일

성능·용량

CI/CD·배포

모니터링·장애대응

아키텍처·품질관리

## 대상 독자

\`\`\`text id=“glsA007” 신규 업무 개발자

화면 개발자

프레임워크 개발자

DB·SQL 개발자

테스트 담당자

운영 담당자

DevOps 담당자

아키텍트

PMO·품질 담당자


\## 용어 사용 원칙

\`\`\`text id="glsA008"
같은 개념에 여러 이름을 사용하지 않는다.

같은 이름을 서로 다른 의미로 사용하지 않는다.

프로젝트 표준 용어를 코드·설정·문서·로그에서 동일하게 사용한다.

약어는 최초 사용 시 의미와 책임을 함께 설명한다.

기술 용어와 업무 용어를 구분한다.

운영자가 이해할 수 없는 내부 구현명만 사용하지 않는다.

# A.1 원본 핵심 용어 30선

| 용어 | 쉬운 정의 | NSIGHT TCF 적용 위치 | 실무 확인점 |
| --- | --- | --- | --- |
| **TCF** | 거래의 진입·통제·호출·오류·로그 처리를 표준화하는 공통 프레임워크 | OnlineTransactionController → TCF.process() | 한 거래의 전체 시작부터 종료까지 이해한다. |
| **ServiceId** | 화면 요청과 서버의 업무 처리기를 연결하는 논리적 거래 식별자 | Header·Dispatcher·Handler·OM | 소스·설정·화면·로그의 값이 모두 같은지 확인한다. |
| **업무코드** | 업무영역과 데이터·배포·운영 책임을 구분하는 코드 | WAR·패키지·ServiceId·오류코드 | 다른 업무의 소유 데이터를 직접 변경하지 않는다. |
| **화면 ID** | 사용자 화면을 식별하는 표준 키 | 화면설계서·메뉴·권한·추적성 | 화면 이벤트와 ServiceId를 연결한다. |
| **거래코드** | 운영·통계·로그에서 거래유형을 분류하는 코드 | 거래로그·OM·통계 | ServiceId와 구분하고 명명규칙·등록정보를 확인한다. |
| **DTO** | 계층 또는 시스템 경계를 넘는 데이터 계약 | Request·Response·Command·Query·Data | 외부 요청·내부 명령·DB 객체를 분리한다. |
| **Validation** | 입력의 필수값·형식·길이·범위를 확인하는 검증 | STF·Handler 경계·Request DTO | 상태·중복·권한 같은 업무규칙과 구분한다. |
| **Service** | 업무 흐름과 여러 구성요소 호출을 조정하는 계층 | Handler·Facade 이후 | SQL·HTTP·응답형식의 세부 구현을 직접 담당하지 않는다. |
| **Repository** | 업무 관점에서 영속성 접근을 추상화하는 경계 | Domain·Application과 DB 사이 | 현재 TCF에서는 DAO·Mapper 조합이 이 역할을 수행할 수 있다. |
| **Mapper** | Java 객체와 SQL 실행을 연결하는 구성요소 | MyBatis Interface·XML | Bind 변수, Result Mapping, SQL ID와 영향 행 수를 확인한다. |
| **Transaction** | 여러 데이터 변경을 하나의 성공·실패 단위로 묶는 경계 | Facade·Spring Transaction | 외부 호출과 DB Transaction의 혼합을 주의한다. |
| **멱등성** | 같은 요청을 반복해도 업무효과가 한 번만 발생하는 성질 | 등록·변경·외부연계·Batch | 변경거래 재시도 전에 반드시 확인한다. |
| **Correlation ID** | 여러 계층과 시스템의 로그를 하나의 업무 흐름으로 연결하는 값 | Header·로그·연계 | 모든 하위 호출에 전달하고 응답에도 문의 ID로 제공한다. |
| **Timeout** | 처리결과를 기다릴 수 있는 최대시간 | Gateway·TCF·Transaction·SQL·외부 | 상위·하위 Timeout의 순서와 전체 Budget을 맞춘다. |
| **Retry** | 일시적 실패 후 같은 요청을 다시 수행하는 정책 | 외부연계·DB·Batch | 멱등성·재시도 횟수·간격·대상 오류를 정의한다. |
| **Circuit Breaker** | 반복 실패하는 외부 호출을 일정 시간 차단하는 보호장치 | EAI·HTTP Client | 차단 후 복구 확인과 Fallback 정책이 필요하다. |
| **JWT** | 사용자·권한 등의 Claim을 담고 서명한 Token 형식 | Gateway·업무 WAR·tcf-jwt | 서명·만료·발급자·대상·권한을 검증한다. |
| **Session** | 서버가 사용자별 상태를 유지하는 저장 단위 | HttpSession·Spring Session | 수명·동시 로그인·클러스터·장애복구를 고려한다. |
| **Cache** | 원본 접근을 줄이기 위해 데이터를 임시 저장하는 구조 | tcf-cache·업무 Cache | Cache는 원본이 아니며 무효화와 장애 Fallback이 핵심이다. |
| **TTL** | Cache나 임시 데이터가 유효한 시간 | Cache Entry·Token·Lock | 업무 변경주기와 장애정책에 맞게 설정한다. |
| **Batch** | 대량 데이터를 일정 단위로 반복 처리하는 작업 | tcf-batch | Chunk·체크포인트·재처리·대사를 설계한다. |
| **Scheduler** | 정해진 시각이나 조건에 작업을 시작하는 구성요소 | @Scheduled·Batch Scheduler | 실행 성공과 업무처리 성공을 구분하고 중복실행을 막는다. |
| **RTM** | 요구사항과 설계·구현·테스트를 연결하는 추적표 | 요구사항·화면·ServiceId·SQL·Test | 누락 구현과 불필요 구현을 발견한다. |
| **ADR** | 중요 아키텍처 결정과 대안·근거를 기록한 문서 | 아키텍처 의사결정 | 선택한 결론뿐 아니라 당시 제약과 포기한 대안을 남긴다. |
| **Quality Gate** | 다음 개발·배포 단계로 넘어가기 위한 필수 품질조건 | CI/CD·리뷰·운영 승인 | 자동검사 결과와 승인 증적을 함께 관리한다. |
| **Observability** | 로그·Metric·Trace로 시스템 내부상태를 추론하는 능력 | TCF·OM·Dashboard | 장애가 발생한 뒤가 아니라 설계단계부터 준비한다. |
| **Rollback** | 배포 후 문제 발생 시 이전의 안정 상태로 되돌리는 절차 | WAR·설정·DB·OM | Application뿐 아니라 데이터·설정의 호환성도 검증한다. |
| **RTO** | 장애 후 서비스를 복구해야 하는 목표시간 | DR·장애대응·Runbook | 복구 자동화와 대응조직 수준을 결정한다. |
| **RPO** | 장애 시 허용할 수 있는 데이터 손실시점 | Backup·복제·DR | Backup 주기와 복제정책을 결정한다. |
| **Runbook** | 운영자가 반복 실행할 수 있는 표준 대응절차 | 배포·Rollback·장애·DB | 명령뿐 아니라 판단조건·중단조건·종료조건을 포함한다. |

# A.2 TCF 거래처리와 애플리케이션 계층 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **STF** | 업무처리 전에 인증·권한·거래통제·입력·Timeout 등을 수행하는 공통 전처리 단계 | TCF.process() 초기 구간 | 업무 Handler 실행 전에 어떤 검증이 끝나는지 확인한다. |
| **ETF** | 업무처리 후 결과·오류·로그·응답을 마무리하는 공통 후처리 단계 | 업무 Handler 이후 | 성공·오류·Timeout 모두 종료로그가 남는지 확인한다. |
| **OnlineTransactionController** | 온라인 표준전문을 받는 공통 HTTP 진입점 | tcf-web | 업무별 Controller를 새로 만들지 않는지 확인한다. |
| **TransactionDispatcher** | ServiceId를 보고 처리할 Handler를 찾는 구성요소 | tcf-core | ServiceId 중복·미등록을 기동·배포 전에 검증한다. |
| **TransactionHandler** | 여러 ServiceId를 실제 업무 Facade에 연결하는 업무 진입 구성요소 | 업무 WAR | Handler가 DAO·Mapper를 직접 호출하지 않게 한다. |
| **Handler** | ServiceId별 요청을 받아 적절한 유스케이스로 전달하는 계층 | CtReservationHandler | 분기·DTO 변환만 수행하고 업무규칙은 넣지 않는다. |
| **Facade** | 한 유스케이스의 Transaction 경계와 처리순서를 조정하는 계층 | 등록·수정·취소 | Transaction Owner가 어느 계층인지 명확히 한다. |
| **Rule** | 상태·중복·권한·기간 등 업무규칙을 판단하는 구성요소 | ReservationRule | SQL·HTTP 호출과 혼합하지 않는다. |
| **DAO** | Mapper 호출을 감싸고 영속성 처리결과를 상위 계층에 전달하는 계층 | ReservationDao | 영향 행 수를 숨기지 않고 반환한다. |
| **Client** | 다른 도메인이나 외부 시스템을 호출하는 계약 경계 | CustomerClient | 상대 구현 Class나 Table을 직접 참조하지 않는다. |
| **Adapter** | 외부 기술형식을 내부 계약으로 변환하는 구성요소 | HTTP·EAI·파일 Adapter | 외부 오류·DTO를 내부 업무모델과 분리한다. |
| **Domain Model** | 업무 의미·상태·행위를 표현하는 내부 객체 | 고객·예약·캠페인 | DB Row와 화면 DTO를 그대로 Domain으로 사용하지 않는다. |
| **Aggregate** | 하나의 Transaction에서 일관성을 유지해야 하는 업무 객체 묶음 | 예약 Master+History | Aggregate 경계를 지나친 대형 Transaction으로 만들지 않는다. |
| **Aggregate Root** | Aggregate 내부 변경을 통제하는 대표 객체 | ConsultationReservation | 외부가 내부 Entity를 직접 변경하지 않게 한다. |
| **Entity** | 고유 식별자를 가지며 시간에 따라 상태가 변하는 객체 | 고객·예약 | 속성이 같아도 식별자가 다르면 다른 객체다. |
| **Value Object** | 식별자보다 값 자체가 의미를 가지는 객체 | 금액·기간·주소 | 불변성·검증규칙을 객체 안에 둘 수 있다. |
| **Dependency** | 하나의 Module·Class가 다른 구성요소를 필요로 하는 관계 | Gradle·Java Import | 상위 업무가 하위 기술에 과도하게 결합되지 않는지 확인한다. |
| **Circular Dependency** | 두 구성요소가 서로를 참조하는 순환관계 | A→B→A | 기동 오류와 변경 영향 확산의 원인이므로 제거한다. |
| **Module** | 독립적으로 빌드되거나 공통기능을 제공하는 코드 단위 | tcf-core, ct-service | 실행 Module과 Library Module을 구분한다. |
| **WAR** | Tomcat에 배포하는 Web Application 묶음 | ct.war, sv.war | Context Path·공통 Library·다중 WAR 영향을 확인한다. |
| **JAR** | Java Class와 Resource를 묶은 Library 또는 실행 산출물 | tcf-core.jar | WAR 내부 Library인지 독립 실행 JAR인지 구분한다. |
| **Context Path** | Tomcat 안에서 WAR를 구분하는 URL 경로 | /ct, /sv | Apache·Gateway Route와 일치해야 한다. |
| **Profile** | 환경별 설정을 선택하는 Spring 구분값 | local·dev·prod | Profile과 실제 외부설정 Source가 일치하는지 확인한다. |
| **Component Scan** | Spring Bean을 자동 탐색하는 범위 | 업무 Root Package | Handler·Mapper가 Scan 범위 밖에 있지 않은지 확인한다. |

# A.3 식별자·전문·계약 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **도메인** | 같은 업무목적·규칙·데이터 책임을 공유하는 업무영역 | 고객·상담예약·캠페인 | 패키지 구분보다 데이터 소유권과 책임을 먼저 정의한다. |
| **화면 이벤트 ID** | 화면에서 발생하는 조회·저장·취소 등의 동작 식별자 | CT-RSV-0001-E04 | 서버 호출 여부와 ServiceId를 연결한다. |
| **SQL ID** | SQL을 설계·운영·튜닝에서 식별하는 값 | CT-RSV-SEL-001 | Mapper Method·로그·설계서에서 동일하게 관리한다. |
| **GUID** | 한 업무거래를 고유하게 식별하는 값 | 거래 Header·로그 | 재전송 시 동일 거래인지 신규 거래인지 정책을 정한다. |
| **TraceId** | 여러 Service·연계 구간을 하나의 분산 Trace로 연결하는 식별자 | Gateway→TCF→외부연계 | 하위 호출에도 동일 Trace 문맥을 전달한다. |
| **Request ID** | 개별 HTTP 요청을 구분하는 식별자 | Proxy·Gateway·Controller | 재시도된 요청과 업무거래 ID를 구분한다. |
| **Idempotency Key** | 동일 변경요청의 반복 여부를 식별하는 Key | 등록·수정·취소 | 같은 Key가 다른 요청내용에 재사용되지 않게 한다. |
| **오류코드** | 오류 유형과 사용자·운영 대응을 표준화하는 식별자 | E-CT-RSV-0006 | Validation·업무·시스템·Timeout 오류를 구분한다. |
| **표준전문** | 채널과 TCF가 약속한 Header·Body·결과 구조 | JSON Request·Response | 업무별 독자 형식으로 공통통제를 우회하지 않는다. |
| **Header** | ServiceId·사용자·채널·추적정보 등 거래의 공통정보 | 표준전문 상단 | 화면에서 위조 가능한 값과 검증된 값을 구분한다. |
| **Body** | 업무별 실제 요청·응답 데이터 영역 | 조회조건·등록값 | 공통정보와 업무정보를 섞지 않는다. |
| **StandardRequest** | 공통 Header와 업무 Body를 담는 표준 요청객체 | TCF 진입점 | 원문 전체를 로그에 남기지 않는다. |
| **StandardResponse** | 결과코드·메시지·추적 ID·업무결과를 담는 표준 응답객체 | ETF | 오류 시에도 일관된 형식을 유지한다. |
| **Contract** | 두 계층·시스템이 주고받기로 합의한 입력·출력 약속 | DTO·API·Event | 구현 세부보다 필드·오류·호환성을 우선한다. |
| **Schema** | 데이터 구조와 타입을 정의한 명세 | JSON Schema·DB Schema | 필수값·길이·Enum·Version을 관리한다. |
| **Route** | 요청을 대상 WAR·Service로 전달하는 규칙 | Gateway·Apache | 업무코드·Context·Target Health를 확인한다. |
| **Endpoint** | 요청을 받을 수 있는 Network 주소 | /tcf/online | Endpoint와 ServiceId의 역할을 혼동하지 않는다. |
| **API** | 시스템 간 기능과 데이터 교환 계약 | 고객조회 API | URL뿐 아니라 인증·오류·Timeout·멱등성을 포함한다. |
| **Backward Compatibility** | 신 버전이 구 Client 요청을 계속 처리할 수 있는 성질 | Rolling 배포 | 필수필드 추가·필드 삭제·의미 변경을 주의한다. |
| **Versioning** | 계약·Artifact·DB 변경세대를 관리하는 방법 | API v1·WAR 1.0.0 | Version 번호만 바꾸지 말고 호환정책을 정의한다. |

# A.4 데이터·Transaction·동시성 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **ACID** | Transaction이 가져야 할 원자성·일관성·격리성·지속성 | RDB Transaction | 외부 시스템까지 자동으로 ACID가 적용되는 것은 아니다. |
| **Commit** | Transaction의 변경을 최종 확정하는 행위 | DB | 응답 실패와 Commit 성공이 동시에 발생할 수 있다. |
| **Rollback** | Transaction의 미확정 변경을 취소하는 행위 | DB·Application | 외부 호출과 이미 전송한 Event는 자동 취소되지 않는다. |
| **Compensation** | 이미 확정된 업무를 반대 거래로 보정하는 처리 | 외부송금·상태복구 | DB Rollback과 구분하고 이력을 남긴다. |
| **JTA** | 여러 Transaction 자원을 하나의 분산 Transaction으로 묶는 표준 | 다중 DB | 복잡도·장애·성능비용을 충분히 검토한다. |
| **Isolation Level** | 동시 Transaction이 서로의 변경을 보는 범위 | Oracle·Spring | Dirty Read·Non-repeatable Read·Phantom을 이해한다. |
| **Lock** | 동시 변경 충돌을 막기 위해 데이터 접근을 제한하는 장치 | DB Row·Java Lock | Lock 보유시간과 대기·Deadlock을 확인한다. |
| **Optimistic Lock** | 충돌이 드물다고 보고 Version으로 변경충돌을 검출하는 방식 | 상담예약 수정 | UPDATE SQL에 Version 조건을 넣는다. |
| **Pessimistic Lock** | 먼저 DB Lock을 잡고 다른 변경을 기다리게 하는 방식 | FOR UPDATE | 사용자 Think Time과 결합하지 않는다. |
| **Version Number** | 데이터가 몇 번 변경됐는지 나타내는 번호 | VERSION\_NO | 성공한 변경마다 증가하고 화면에서 다시 전달한다. |
| **Lost Update** | 오래된 화면의 값이 다른 사용자의 최신 변경을 덮어쓰는 문제 | 동시 수정 | Version 조건으로 방지한다. |
| **Affected Rows** | INSERT·UPDATE·DELETE로 실제 변경된 행 수 | MyBatis 반환값 | 단건 변경은 1건인지 반드시 확인한다. |
| **Business Key** | 업무적으로 동일 데이터를 판단하는 값의 조합 | 고객+예약일시 | PK와 구분하고 Unique 적용을 검토한다. |
| **Primary Key** | Table Row를 고유하게 식별하는 DB Key | RESERVATION\_ID | 업무 중복 기준과 항상 같지는 않다. |
| **Unique Constraint** | 특정 값의 중복 저장을 DB가 최종 차단하는 규칙 | 활성 예약 Key | 사전 중복조회와 함께 사용한다. |
| **Logical Delete** | Row를 지우지 않고 취소·비활성 상태로 바꾸는 방식 | STATUS\_CD=CANCELED | 조회·재등록·보존·폐기정책을 함께 설계한다. |
| **Physical Delete** | DB Row를 실제 삭제하는 처리 | 보존기간 만료 Batch | 감사·복구·참조 무결성을 확인한다. |
| **State** | 업무 객체의 현재 단계 | READY·COMPLETED·CANCELED | 단순 코드값이 아니라 허용행위를 결정한다. |
| **State Transition** | 하나의 상태에서 다른 상태로 이동하는 업무규칙 | READY→CANCELED | 허용되지 않은 전이를 서버에서 차단한다. |
| **Master Table** | 업무 객체의 현재 상태를 보관하는 주 테이블 | 예약 Master | History·Detail과 Transaction 경계를 맞춘다. |
| **History Table** | 업무 데이터가 어떻게 변경됐는지 보관하는 테이블 | 예약 변경이력 | Master와 같은 Transaction으로 저장한다. |
| **Audit Log** | 누가 어떤 중요행위를 수행했는지 증명하는 기록 | 개인정보 조회·변경 | Domain History와 목적·보존기간이 다르다. |
| **Data Ownership** | 특정 데이터를 생성·변경할 책임이 어느 도메인에 있는지 정의한 것 | 고객은 IC 소유 | 같은 DB라도 다른 도메인이 직접 변경하지 않는다. |
| **Read Model** | 조회 목적에 맞게 별도로 구성한 데이터 구조 | 고객요약·검색 View | 원본과 동기화·최신성 정책이 필요하다. |
| **Pagination** | 대량 결과를 여러 번 나누어 조회하는 방식 | 목록 Grid | 최대 Page Size와 안정적 정렬이 필요하다. |
| **Offset Paging** | 앞의 N건을 건너뛰고 다음 결과를 조회하는 방식 | Page 1·2·3 | 깊은 Page에서 느려질 수 있다. |
| **Keyset Paging** | 마지막 정렬 Key 이후의 데이터를 조회하는 방식 | Cursor Paging | 고유한 Tie-breaker가 필요하다. |
| **Index** | DB가 검색범위를 빠르게 줄이도록 만든 구조 | 고객번호·예약일시 | 조회개선과 INSERT·UPDATE 비용을 함께 측정한다. |
| **Execution Plan** | DB가 SQL을 어떤 순서와 방법으로 실행할지 나타낸 계획 | Oracle Plan | Cost뿐 아니라 실제 Row·대기·I/O를 확인한다. |
| **N+1 Query** | 목록 1회 조회 후 각 행마다 추가 SQL·호출을 반복하는 문제 | 고객명 개별조회 | Join·Batch 조회·Read Model을 검토한다. |

# A.5 오류·Timeout·복원력 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **Business Rule** | 업무상 허용·금지 조건 | READY만 취소 | 단순 입력형식 검증과 구분한다. |
| **Business Error** | 시스템은 정상이나 업무규칙 때문에 요청을 거절한 결과 | 중복 예약·상태 오류 | 사용자가 수정할 수 있는 메시지를 제공한다. |
| **System Error** | DB·Network·코드 등 시스템 문제로 처리가 실패한 결과 | Connection 실패 | 사용자에게 내부정보를 노출하지 않는다. |
| **Validation Error** | 필수값·형식·범위가 맞지 않는 오류 | 날짜형식·길이 | 가능한 한 처리 초기에 차단한다. |
| **Exception** | 프로그램 실행 중 발생한 비정상 상황을 표현하는 객체 | Java Exception | 원인 예외를 보존하고 무조건 하나의 오류로 바꾸지 않는다. |
| **Root Cause** | 장애를 직접 발생시킨 근본 원인 | Connection Leak | Timeout 같은 증상과 구분한다. |
| **Timeout Budget** | 전체 허용시간을 각 처리구간에 나눈 시간계획 | TCF 5초·SQL 3초 | 하위 Timeout이 상위보다 길지 않게 한다. |
| **Connect Timeout** | 상대 시스템과 연결을 맺기까지 기다리는 시간 | HTTP Client | Read Timeout과 구분한다. |
| **Read Timeout** | 연결 후 응답 데이터를 기다리는 시간 | 외부 API | 업무 전체 Timeout 안에 포함한다. |
| **Query Timeout** | DB SQL 실행을 기다리는 최대시간 | MyBatis | Slow SQL을 빠르게 만드는 기능은 아니다. |
| **Connection Timeout** | DB Pool에서 Connection을 얻기 위해 기다리는 시간 | Hikari | SQL 실행시간과 구분해 측정한다. |
| **Backoff** | 재시도 사이의 대기시간을 점차 늘리는 정책 | Retry | 모든 Client가 동시에 재시도하는 폭주를 줄인다. |
| **Jitter** | 재시도 시각을 임의로 분산하는 작은 시간차 | Retry | 동시 재시도 집중을 방지한다. |
| **Fallback** | 주 처리 실패 시 제공하는 제한적 대체결과 | Cache·외부연계 | 데이터 최신성·기능제한을 사용자에게 명확히 한다. |
| **Bulkhead** | 한 기능의 장애가 전체 자원을 독점하지 못하게 격리하는 구조 | 별도 Thread·Pool | 업무별 자원경계를 정의한다. |
| **Rate Limit** | 일정 시간에 허용할 요청량을 제한하는 통제 | Gateway·ServiceId | 사용자·채널·Service별 기준을 정한다. |
| **Back Pressure** | 처리능력보다 요청이 많을 때 유입속도를 줄이는 방식 | Queue·Stream | 무제한 Queue로 문제를 숨기지 않는다. |
| **Fail Fast** | 성공 가능성이 낮을 때 오래 기다리지 않고 빠르게 실패하는 정책 | Open Circuit·Pool 포화 | Retry·Fallback과 함께 설계한다. |
| **Partial Failure** | 여러 처리 중 일부만 성공하고 일부가 실패한 상태 | Master 성공·History 실패 | Transaction 또는 보상처리로 통제한다. |
| **UNKNOWN 상태** | 응답으로 성공·실패를 확정하지 못한 상태 | Commit 후 응답 유실 | GUID·Idempotency·DB 상태로 결과를 확인한다. |
| **Deadlock** | 두 Transaction·Thread가 서로 가진 자원을 기다리는 상태 | DB·JVM | Lock 순서·범위·Transaction 시간을 분석한다. |
| **Poison Message** | 반복 처리할 때 계속 실패하는 데이터·메시지 | Batch·Queue | 실패격리와 수동조치가 필요하다. |
| **Dead Letter** | 반복 실패한 메시지를 별도로 격리한 저장소 | Messaging | 재처리 기준·보존·감사가 필요하다. |
| **Graceful Shutdown** | 신규 요청을 중지하고 기존 작업을 안전하게 끝낸 뒤 종료하는 방식 | Tomcat·Executor | 배포 전 Drain과 종료 Timeout을 적용한다. |

# A.6 인증·인가·보안 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **Authentication** | 요청한 사용자가 누구인지 확인하는 절차 | SSO·JWT | 인증 성공과 업무권한을 구분한다. |
| **Authorization** | 인증된 사용자가 어떤 기능·데이터를 사용할 수 있는지 판단하는 절차 | STF·Rule | 화면 버튼 숨김만으로 대체할 수 없다. |
| **기능권한** | 특정 기능이나 ServiceId를 실행할 수 있는 권한 | CT\_RESERVATION\_UPDATE | STF·OM·권한관리의 값이 일치해야 한다. |
| **데이터권한** | 특정 지점·고객·조직 데이터에 접근할 수 있는 권한 | 지점 Scope | SQL 조건에도 반영한다. |
| **SSO** | 한 번 인증해 여러 시스템을 사용하는 통합인증 방식 | 사내 IdP | SSO 인증결과와 업무 Token 발급경계를 정한다. |
| **Access Token** | 짧은 시간 동안 API 접근에 사용하는 Token | Authorization Header | 만료시간을 짧게 하고 원문을 로그에 남기지 않는다. |
| **Refresh Token** | Access Token을 다시 발급받기 위한 Token | tcf-jwt | 더 강한 보호·폐기·재사용 탐지가 필요하다. |
| **Claim** | JWT 안에 저장된 사용자·권한·발급정보 | sub, iss, aud | Client가 보낸 Header보다 검증된 Claim을 신뢰한다. |
| **Issuer** | Token을 발급한 주체 | iss | 허용된 발급자인지 확인한다. |
| **Audience** | Token 사용대상 시스템 | aud | 다른 시스템용 Token 사용을 차단한다. |
| **Signature** | Token·데이터가 변조되지 않았음을 확인하는 서명 | RS256 | Algorithm 고정과 Key Rotation을 적용한다. |
| **Private Key** | 서명을 생성하는 비공개 Key | tcf-jwt 발급서버 | 사용자별이 아니라 발급주체가 안전하게 관리한다. |
| **Public Key** | 서명을 검증하는 공개 가능한 Key | Gateway·업무 WAR | 배포·Rotation·Cache 정책을 관리한다. |
| **JWKS** | JWT 검증용 공개 Key 목록을 제공하는 표준형식 | Gateway Validator | kid와 Key Rotation을 처리한다. |
| **Cookie** | Browser가 서버에 자동 전송하는 작은 데이터 | Session Cookie | Secure·HttpOnly·SameSite를 설정한다. |
| **CSRF** | 사용자가 모르는 사이 Browser 인증정보로 요청을 보내게 하는 공격 | Cookie 인증 | Token 인증방식과 Browser 구조에 맞춰 방어한다. |
| **XSS** | 악성 Script를 화면에서 실행하게 하는 공격 | 메모·사유 출력 | 출력 Encoding·CSP·Sanitizer를 적용한다. |
| **SQL Injection** | 입력값으로 SQL 구조를 조작하는 공격 | Mapper SQL | ${}보다 #{} Bind를 사용한다. |
| **Mass Assignment** | Request 필드를 객체에 자동 복사해 허용되지 않은 값까지 변경하는 문제 | 상태·Owner 변경 | 명시적 Command Mapping을 사용한다. |
| **Masking** | 일부 정보만 보이도록 값을 가리는 처리 | 고객번호·사용자명 | 사용목적·권한별 마스킹 수준을 정한다. |
| **Encryption** | Key 없이는 내용을 알 수 없도록 변환하는 처리 | 개인정보·파일 | 전송구간·저장구간을 구분한다. |
| **Hash** | 원문 복원이 어려운 단방향 변환 | Password·Key Fingerprint | Salt·Algorithm과 비교목적을 명확히 한다. |
| **Secret** | Password·Private Key·API Key 같은 비밀정보 | 환경변수·Vault | Git·WAR·로그에 포함하지 않는다. |
| **Least Privilege** | 필요한 최소한의 권한만 부여하는 원칙 | DB 계정·운영계정 | 개발·배포·운영 권한을 분리한다. |
| **Trust Boundary** | 데이터·요청을 다시 검증해야 하는 신뢰경계 | Gateway→업무 WAR | Gateway가 있어도 업무 WAR의 2차 검증을 고려한다. |

# A.7 Cache·Batch·Scheduler·파일 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **Cache Hit** | 원하는 값이 Cache에 있어 원본 조회 없이 반환한 경우 | 기준코드 Cache | Hit 결과의 최신성을 보장하는 정책이 필요하다. |
| **Cache Miss** | Cache에 값이 없어 원본을 조회해야 하는 경우 | DB Fallback | Miss 급증 시 DB 부하를 관찰한다. |
| **Eviction** | Cache Entry를 제거하는 처리 | TTL·수동 제거 | 삭제 시점과 대상범위를 통제한다. |
| **Invalidation** | 원본 변경에 맞춰 Cache를 무효화하는 처리 | 데이터 변경 Event | 성공순서와 장애 재처리를 설계한다. |
| **Cache-Aside** | Application이 Cache를 먼저 보고 없으면 원본을 읽어 저장하는 패턴 | 업무조회 | 동시 Miss와 Stale 문제를 고려한다. |
| **Cache Stampede** | 많은 요청이 동시에 Cache Miss가 되어 원본으로 몰리는 현상 | TTL 동시 만료 | Lock·TTL 분산·사전 Warm-up을 사용한다. |
| **Job** | Batch가 수행하는 하나의 전체 업무 | 일별 상담예약 마감 | 실행 ID와 업무 기준일을 구분한다. |
| **Job Instance** | 같은 업무조건으로 실행되는 논리적 Batch 단위 | Job+업무일자 | 동일 Instance 재실행 정책을 정한다. |
| **Job Execution** | Job Instance를 실제로 한 번 실행한 기록 | 재시도 1·2회 | 시작·종료·결과·오류를 기록한다. |
| **Step** | Batch Job을 구성하는 세부 처리단계 | 읽기·변환·저장 | 단계별 재시작과 결과를 관리한다. |
| **Chunk** | 일정 건수 단위로 읽고 처리하고 Commit하는 묶음 | 1,000건 | 너무 크거나 작지 않게 성능·Rollback 비용을 측정한다. |
| **Checkpoint** | Batch가 어디까지 처리했는지 기록한 재시작 위치 | 마지막 ID | 업무 데이터와 같은 Transaction에서 기록하는지 검토한다. |
| **Restart** | 실패한 Batch를 완료지점부터 다시 시작하는 처리 | Job 재기동 | 처음부터 재실행과 구분한다. |
| **Skip** | 특정 실패 항목을 격리하고 나머지 처리를 계속하는 정책 | 잘못된 Row | 허용 오류유형·최대건수·사후조치를 정한다. |
| **Misfire** | 예정된 Scheduler 실행시각을 놓친 상태 | 서버중단 | 즉시 실행·건너뛰기·다음 실행 중 정책을 정한다. |
| **Job Lock** | 동일 Batch가 동시에 여러 번 실행되는 것을 막는 잠금 | DB Lock Table | Lock 만료와 장애 후 회수를 설계한다. |
| **Lease** | 일정 시간 동안만 유효한 분산 Lock 권한 | Scheduler | Heartbeat와 만료 후 재획득을 처리한다. |
| **Fencing Token** | 오래된 Lock 보유자의 쓰기를 차단하는 증가번호 | 분산 Job | Lock 만료 후 늦게 도착한 작업을 막는다. |
| **Streaming** | 전체 데이터를 메모리에 올리지 않고 조금씩 읽고 쓰는 방식 | 대용량 파일 | 메모리 복사와 Connection 점유시간을 확인한다. |
| **Range Download** | 파일의 일부 구간만 요청해 내려받는 방식 | 대용량 다운로드 | 중단 후 이어받기와 접근권한을 적용한다. |
| **Resumable Upload** | 파일을 여러 조각으로 올리고 중단지점부터 이어가는 방식 | 수백 GB 파일 | Chunk 순서·Checksum·완료조립을 검증한다. |
| **Checksum** | 파일·Artifact가 변조되지 않았는지 확인하는 Hash | SHA-256 | 업로드·전송·배포 전후 값을 비교한다. |
| **Quarantine** | 검증 전 파일을 업무영역과 분리해 보관하는 공간 | 악성코드 검사 | 검사 완료 전 다운로드·처리를 금지한다. |
| **Reconciliation** | 입력·출력·성공·실패 건수와 금액이 맞는지 대조하는 작업 | Batch·외부거래 | 처리 완료와 데이터 완료를 구분한다. |

# A.8 성능·용량 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **TPS** | 1초 동안 완료한 거래 건수 | 성능시험·운영 Metric | 요청건수보다 완료건수 기준인지 확인한다. |
| **Throughput** | 단위시간에 처리한 전체 업무량 | TPS·MB/s | 응답시간과 함께 판단한다. |
| **Latency** | 요청 후 응답을 받기까지의 지연시간 | 화면·연계 | 평균뿐 아니라 상위 백분위를 본다. |
| **Concurrency** | 같은 시간에 동시에 진행 중인 작업 수 | Thread·요청·Transaction | 동시 사용자와 동시 요청을 구분한다. |
| **p50** | 전체 요청의 절반이 이 시간 이하에 완료되는 값 | 응답시간 | 일반 사용자의 중앙경향을 본다. |
| **p95** | 전체 요청의 95%가 이 시간 이하에 완료되는 값 | NFR | 느린 상위 5% 사용자를 관리한다. |
| **p99** | 전체 요청의 99%가 이 시간 이하에 완료되는 값 | 장애 전조 | Tail Latency를 확인한다. |
| **Little’s Law** | 동시 처리량은 처리율과 평균 체류시간의 곱이라는 관계 | 용량산정 | 사용자 수와 TPS를 직접 동일시하지 않는다. |
| **Baseline** | 변경 전 비교기준이 되는 성능·운영 결과 | 튜닝·배포 전 | Version·데이터·설정·부하조건을 함께 기록한다. |
| **Bottleneck** | 전체 처리량을 가장 먼저 제한하는 자원 | DB·CPU·외부 | 추측보다 Metric과 Trace로 확인한다. |
| **Saturation** | 요청량이 처리능력을 넘어 Queue와 지연이 증가하는 상태 | Thread·Pool·DB | TPS가 정체되고 p95가 급증하는 지점을 찾는다. |
| **Thread Pool** | 작업을 재사용 가능한 Thread들로 처리하는 구조 | Tomcat·Executor | 최대 Thread·Queue·거절정책을 정한다. |
| **maxThreads** | Tomcat이 동시에 실행할 수 있는 최대 요청 Thread 수 | Connector | DB Pool 크기와 동일하게 설정하지 않는다. |
| **acceptCount** | 모든 Tomcat Thread가 바쁠 때 대기할 연결 수 | Connector Queue | 큰 Queue가 문제를 단순히 늦게 실패하게 할 수 있다. |
| **HikariCP** | DB Connection을 효율적으로 재사용하는 Connection Pool | 업무 WAR | WAR·Instance별 전체 Pool 합계를 계산한다. |
| **Active Connection** | 현재 사용 중인 DB Connection 수 | Hikari Metric | Max와 함께 SQL 보유시간을 확인한다. |
| **Idle Connection** | 즉시 사용할 수 있는 유휴 Connection 수 | Hikari Metric | 과도한 Idle은 DB Session 낭비가 될 수 있다. |
| **Pending Thread** | DB Connection을 얻기 위해 기다리는 Thread 수 | Hikari Metric | 지속적으로 0보다 크면 원인을 분석한다. |
| **Connection Acquire Time** | Pool에서 Connection을 얻는 데 걸린 시간 | Trace·Metric | SQL 실행시간과 분리해 측정한다. |
| **JVM** | Java Program이 실행되는 Runtime 환경 | Tomcat Process | 여러 WAR가 CPU·Heap·GC를 공유할 수 있다. |
| **Heap** | Java 객체가 저장되는 주요 Memory 영역 | JVM | 사용률뿐 아니라 GC 후 잔존량을 본다. |
| **GC** | 사용하지 않는 Java 객체를 회수하는 작업 | G1GC 등 | Pause·빈도·Allocation Rate를 함께 본다. |
| **Metaspace** | Class Metadata를 저장하는 JVM Memory | 다중 WAR·ClassLoader | Hot Deploy와 ClassLoader Leak을 주의한다. |
| **Load Test** | 목표부하에서 성능 기준을 검증하는 시험 | 720 TPS | 실제 거래 Mix와 데이터를 사용한다. |
| **Stress Test** | 처리한계를 넘겨 포화·실패지점을 찾는 시험 | 120~150% 부하 | 안전한 실패와 회복 여부를 확인한다. |
| **Spike Test** | 요청량이 순간적으로 급증하는 시험 | 이벤트 시작 | Queue·Pool·Cache Stampede를 본다. |
| **Soak Test** | 장시간 부하를 유지하는 시험 | 8·24·72시간 | Memory·Connection·Thread Leak을 찾는다. |

# A.9 운영·모니터링·장애 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **Log** | 개별 사건과 처리맥락을 기록한 데이터 | 거래·오류·감사로그 | 구조화하고 민감정보를 제외한다. |
| **Metric** | 시스템 상태를 숫자와 시간추세로 표현한 데이터 | TPS·CPU·Pool | Label Cardinality를 통제한다. |
| **Trace** | 요청이 여러 계층·시스템을 지나간 경로와 시간을 연결한 정보 | 분산추적 | TraceId와 Span 관계를 유지한다. |
| **Span** | Trace 안의 하나의 처리구간 | Gateway·SQL·외부호출 | 시작·종료·결과·대상을 기록한다. |
| **Dashboard** | 주요 Metric과 상태를 한 화면에 보여주는 운영화면 | tcf-om | 단순 그래프보다 원인판정에 필요한 지표를 묶는다. |
| **Alert** | 지표나 사건이 기준을 넘었음을 담당자에게 알리는 통보 | 오류율·p95·Pending | Owner·등급·억제·해제기준을 정한다. |
| **Incident** | 서비스·데이터·보안에 실제 영향을 주는 장애사건 | 장애관리 | 단순 Alert와 구분한다. |
| **Severity** | 장애의 업무영향과 긴급도를 나타내는 등급 | SEV-1~4 | CPU 수치보다 사용자·데이터 영향을 우선한다. |
| **MTTD** | 장애가 시작된 뒤 탐지하기까지의 평균시간 | 운영 KPI | 사용자 신고보다 자동 탐지를 앞당긴다. |
| **MTTA** | Alert 발생 후 담당자가 대응을 시작하기까지의 평균시간 | 운영 KPI | 당직·연락체계를 개선한다. |
| **MTTR** | 장애 후 서비스를 복구하기까지의 평균시간 | 운영 KPI | 원인분석 완료시간과 구분한다. |
| **RCA** | 장애의 근본 원인과 기여요인을 분석하는 활동 | 사후분석 | 개인 실수로만 결론내리지 않는다. |
| **Postmortem·PIR** | 장애의 영향·타임라인·원인·개선사항을 정리하는 사후검토 | 장애 종료 후 | 비난보다 시스템 개선에 집중한다. |
| **Incident Commander** | 장애대응의 우선순위와 조치를 총괄하는 역할 | War Room | 모든 기술작업을 직접 수행하는 사람이 아니다. |
| **War Room** | 장애 관계자가 하나의 채널에서 판단·조치하는 대응체계 | SEV-1·2 | 한 명의 지휘자와 기록담당자를 둔다. |
| **Liveness** | Application Process가 살아 있는지 확인하는 상태 | Actuator | 업무 요청을 받을 준비와는 다르다. |
| **Readiness** | 신규 업무요청을 정상적으로 받을 준비가 됐는지 나타내는 상태 | Traffic 투입 전 | 초기화·필수 설정·Handler 등록을 확인한다. |
| **Deep Health** | DB·Cache·외부연계 등 주요 의존성을 포함한 상태점검 | 운영 Health | 점검 자체가 업무에 부하를 주지 않게 한다. |
| **Smoke Test** | 배포 후 실제 대표 업무가 정상인지 빠르게 확인하는 시험 | 조회·등록·수정 | Health UP만으로 대체할 수 없다. |
| **Health Check** | 시스템이 정상 서비스를 제공할 수 있는지 확인하는 점검 | L4·OM·Actuator | Liveness·Readiness·Deep·Smoke를 구분한다. |
| **Runbook Drill** | Runbook을 실제로 실행해 정확성을 검증하는 훈련 | 배포·Rollback·DR | 문서만 작성하고 끝내지 않는다. |
| **Stabilization** | 운영 전환 후 집중적으로 상태를 관찰하는 기간 | Go-Live 후 | 오류·p95·데이터·문의 추세를 확인한다. |

# A.10 CI/CD·배포·품질 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **CI** | 코드 변경마다 Build·Test·품질검사를 자동 수행하는 절차 | GitLab·Jenkins·Actions | 실패한 Artifact가 다음 단계로 가지 않게 한다. |
| **CD** | 승인된 Artifact를 환경에 일관되게 배포하는 절차 | Dev·Staging·Prod | 배포와 Release 승인을 구분한다. |
| **Artifact** | 빌드 결과로 생성된 배포 가능한 불변 파일 | WAR·JAR | Commit·Tag·Checksum과 연결한다. |
| **Artifact Repository** | Artifact를 Version별로 보관하는 저장소 | Nexus·사내 저장소 | 운영배포본과 직전본을 보존한다. |
| **Build Once, Deploy Many** | 한 번 만든 동일 Artifact를 환경별 설정으로 승격하는 원칙 | Dev→Staging→Prod | 환경마다 다시 빌드하지 않는다. |
| **Release** | 운영에 제공할 코드·설정·DB 변경의 승인된 묶음 | 상담예약 1.0.0 | WAR 하나만 Release로 보지 않는다. |
| **Release Manifest** | Artifact·설정·DB·승인정보를 모아놓은 배포 명세 | YAML·문서 | 누가 무엇을 어디에 배포하는지 명확히 한다. |
| **Git Tag** | 특정 Commit을 Release Version으로 고정한 표시 | v1.0.0 | 같은 Tag 내용을 다시 바꾸지 않는다. |
| **Checksum** | Artifact가 Build 후 변조되지 않았음을 검증하는 값 | SHA-256 | Repository와 대상 서버의 값을 비교한다. |
| **SBOM** | Software에 포함된 Library·Component 목록 | 공급망 보안 | 취약 Library 영향범위를 빠르게 확인한다. |
| **Rolling Deployment** | Instance를 하나씩 순차 교체하는 배포방식 | AP01→AP02 | 구·신 버전과 DB의 호환성이 필요하다. |
| **Blue-Green Deployment** | 구 환경과 신 환경을 따로 준비해 Traffic을 전환하는 방식 | 중요 Release | 환경비용과 데이터 호환성을 확인한다. |
| **Canary Deployment** | 일부 사용자·Traffic에 신규 버전을 먼저 적용하는 방식 | 특정 지점 | 판단 Metric과 즉시 중단기준을 정한다. |
| **Traffic Drain** | 신규요청을 막고 처리 중인 기존 요청이 끝나기를 기다리는 절차 | 배포 전 | 장기 거래·Timeout을 고려한다. |
| **Roll-forward** | 이전 버전으로 돌아가지 않고 수정된 새 버전으로 복구하는 방식 | 비가역 DB 변경 | Hotfix도 Review·Test·새 Artifact를 거친다. |
| **Expand-Migrate-Contract** | DB 변경을 추가·이행·폐기의 세 단계로 나누는 전략 | Rolling DB Migration | 구·신 WAR 공존기간을 지원한다. |
| **Deprecated** | 사용중단 예정이지만 호환을 위해 잠시 유지하는 상태 | 구 ServiceId | 신규 사용 차단·호출량 0 확인 후 제거한다. |
| **Quality Gate** | Merge·Artifact·배포를 허용하는 품질조건 | CI·Go/No-Go | Blocker를 자동 차단한다. |
| **Static Analysis** | 실행하지 않고 소스의 결함·규칙위반을 분석하는 검사 | Sonar·ArchUnit | 실행 Test를 대체하지 않는다. |
| **Architecture Test** | 계층의존·Package 규칙을 자동 검증하는 Test | ArchUnit | Handler→Mapper 같은 위반을 차단한다. |
| **Go·No-Go** | 운영 전환을 진행할지 중단할지 결정하는 공식 승인 | Cut-over 회의 | 잔여위험·Rollback·운영준비를 함께 본다. |
| **Operational Acceptance** | 운영조직이 서비스 운영책임을 공식 인수하는 절차 | 운영전환 완료 | Runbook·교육·권한·연락망이 필요하다. |

# A.11 아키텍처·관리 용어

| 용어 | 쉬운 정의 | 적용 위치·예 | 실무 확인점 |
| --- | --- | --- | --- |
| **Architecture** | 시스템의 주요 구성요소·책임·관계·의사결정을 정의한 구조 | Application·Data·Infra | 그림보다 책임과 제약·변경원칙이 중요하다. |
| **Target Architecture** | 프로젝트가 도달하려는 목표 구조 | TO-BE | 현행 Gap과 전환순서를 함께 작성한다. |
| **Current Architecture** | 현재 운영 중인 실제 구조 | AS-IS | 문서가 아니라 소스·설정·운영 증거로 확인한다. |
| **Gap** | 현행과 목표 사이에 부족하거나 달라진 부분 | Session·Gateway·OM | 구현·검증·운영 과제로 전환한다. |
| **Constraint** | 설계 선택을 제한하는 조건 | Redis 미사용·Oracle | 숨기지 말고 ADR과 설계서에 기록한다. |
| **Non-functional Requirement** | 성능·보안·가용성·운영성 같은 품질 요구 | p95·RTO·감사 | 기능 개발 전에 수치와 검증방법을 정한다. |
| **RACI** | 업무별 책임자를 실행·승인·자문·공유로 구분한 표 | 설계·배포·장애 | 승인책임자가 둘 이상이 되지 않게 한다. |
| **WBS** | 프로젝트 작업을 관리 가능한 단위로 나눈 구조 | 단계별 구축계획 | 산출물·Owner·완료조건을 포함한다. |
| **Baseline** | 공식 승인돼 변경관리 기준이 되는 Version | 요구사항·설계·설정 | 변경 시 영향분석과 재승인이 필요하다. |
| **Traceability** | 요구사항에서 화면·ServiceId·코드·SQL·Test까지 연결되는 성질 | RTM | 정방향·역방향으로 모두 추적한다. |
| **Governance** | 표준과 의사결정을 조직적으로 승인·검증·관리하는 체계 | Architecture Gate | 문서 작성보다 실제 Gate와 책임이 중요하다. |
| **Policy as Code** | 표준규칙을 자동검사 가능한 코드로 표현하는 방식 | CI Gate | 사람의 반복검토를 자동화한다. |
| **Technical Debt** | 빠른 선택 때문에 미래에 추가 비용을 만드는 기술적 미완료 | 임시 설정·수동절차 | Owner·우선순위·해소기한을 둔다. |
| **Compatibility** | 서로 다른 Version·구성요소가 함께 동작할 수 있는 성질 | Rolling Deploy | API·DB·설정·데이터를 함께 본다. |
| **Deprecation** | 기존 기능의 단계적 사용중단 절차 | ServiceId·Metric | 공지·전환·호출량 확인·제거 순서를 따른다. |
| **Decommission** | 시스템·기능·데이터를 운영에서 완전히 폐기하는 절차 | 구 WAR·Table | 데이터 보존·법적 Hold·Route 제거가 필요하다. |

# A.12 혼동하기 쉬운 용어 비교

## 1\. ServiceId와 거래코드

| 구분 | ServiceId | 거래코드 |
| --- | --- | --- |
| 목적 | 서버 처리기 선택 | 운영·통계 분류 |
| 예 | CT.Reservation.create | CT-REG-0101 |
| 주요 사용처 | Dispatcher·Handler | 거래로그·통계·OM |
| 핵심 질문 | 어떤 Program이 실행되는가 | 어떤 유형의 거래인가 |

## 2\. 화면 ID와 ServiceId

| 구분 | 화면 ID | ServiceId |
| --- | --- | --- |
| 대상 | 사용자 화면 | 서버 거래 |
| 관계 | 한 화면에 여러 이벤트 | 이벤트마다 별도 ServiceId 가능 |
| 예 | CT-RSV-0001 | selectList, create, cancel |

## 3\. Handler와 Controller

| 구분 | Controller | Handler |
| --- | --- | --- |
| 책임 | HTTP 진입 | ServiceId 업무 분기 |
| NSIGHT 기준 | 공통 Controller | 업무별 Handler |
| 금지 | 업무별 독자 Controller 난립 | SQL·Transaction 직접 처리 |

## 4\. Facade와 Service

| 구분 | Facade | Service |
| --- | --- | --- |
| 책임 | 유스케이스·Transaction 경계 | 업무 처리순서와 구성요소 조립 |
| 예 | 예약 등록 Transaction | 고객 확인·Rule·DAO 호출 |
| 프로젝트 기준 | Transaction Owner 후보 | 업무 흐름 담당 |

## 5\. DAO와 Mapper

| 구분 | DAO | Mapper |
| --- | --- | --- |
| 책임 | 영속성 호출 추상화 | 실제 SQL 실행 |
| 반환 | 업무가 판단할 수 있는 영향 행·Row | MyBatis 결과 |
| 금지 | 업무 상태판단 | 업무 메시지 생성 |

## 6\. Validation과 업무규칙

| 구분 | Validation | 업무규칙 |
| --- | --- | --- |
| 내용 | 필수·형식·길이 | 상태·중복·권한 |
| 시점 | 처리 초기에 | 업무 처리 중 |
| 예 | 날짜형식 오류 | 완료 예약 수정 금지 |

## 7\. GUID·TraceId·Correlation ID

| 구분 | 의미 |
| --- | --- |
| GUID | 하나의 업무거래 식별자 |
| TraceId | 분산 호출 전체 경로 식별자 |
| Correlation ID | 관련 로그·이벤트를 연결하는 포괄적 개념 |

프로젝트 구현에 따라 GUID와 TraceId가 동일하거나 별도일 수 있으므로 Header·로그 표준을 확인한다.

## 8\. Timeout과 Retry

| Timeout | Retry |
| --- | --- |
| 기다릴 수 있는 최대시간 | 실패 후 다시 실행하는 정책 |
| 자원점유를 제한 | 일시 실패를 회복 |
| 결과가 UNKNOWN일 수 있음 | 중복효과를 만들 수 있음 |

Timeout이 발생했다고 무조건 Retry하지 않는다.

## 9\. Version과 Idempotency Key

| Version | Idempotency Key |
| --- | --- |
| 데이터 변경세대 확인 | 동일 요청 재전송 확인 |
| Lost Update 방지 | 중복 업무효과 방지 |
| 대상 Row에 저장 | 요청 처리상태에 저장 |
| 수정·취소에 중요 | 등록·외부 변경에 중요 |

## 10\. Rollback과 Compensation

| Rollback | Compensation |
| --- | --- |
| 아직 Commit되지 않은 변경 취소 | 이미 확정된 업무를 반대 거래로 보정 |
| DB Transaction 내부 | 시스템·시간 경계를 넘을 수 있음 |
| 자동 가능 | 업무규칙과 승인이 필요 |

## 11\. History와 Audit Log

| History | Audit Log |
| --- | --- |
| 업무 데이터 변화 기록 | 사용자 중요행위 기록 |
| Aggregate 복원·조회 | 보안·감사 증명 |
| Master와 같은 Transaction | 별도 보존정책 가능 |
| Before·After 중심 | Who·When·What·Result 중심 |

## 12\. Authentication과 Authorization

| Authentication | Authorization |
| --- | --- |
| 누구인지 확인 | 무엇을 할 수 있는지 판단 |
| SSO·JWT | 기능권한·데이터권한 |
| 401 | 403 또는 존재 은닉 |

## 13\. Cache와 원본 데이터

| Cache | 원본 |
| --- | --- |
| 임시 복제 | 최종 진실 |
| 삭제 가능 | 업무 기준 |
| TTL·무효화 필요 | Transaction·정합성 필요 |

Cache 조회가 빠르다고 원본 정합성을 생략하지 않는다.

## 14\. Batch와 Scheduler

| Batch | Scheduler |
| --- | --- |
| 무엇을 어떻게 처리하는가 | 언제 시작하는가 |
| Chunk·재시작·대사 | Cron·Misfire·중복실행 |
| 업무 성공 여부 관리 | 실행 요청 여부 관리 |

Scheduler가 정상 호출됐다고 Batch가 성공한 것은 아니다.

## 15\. Liveness와 Readiness

| Liveness | Readiness |
| --- | --- |
| Process가 살아 있는가 | 신규 요청을 받을 준비가 됐는가 |
| 실패 시 재기동 검토 | 실패 시 Traffic 투입 금지 |
| 최소 점검 | 필수 초기화·의존성 점검 |

## 16\. Health Check와 Smoke Test

| Health Check | Smoke Test |
| --- | --- |
| Component 상태 확인 | 실제 업무거래 확인 |
| DB 연결·Process | ServiceId·Handler·SQL·응답 |
| 빠르고 반복적 | 대표 거래 중심 |

## 17\. 평균과 p95

| 평균 | p95 |
| --- | --- |
| 전체 합계÷건수 | 95% 요청이 완료되는 경계 |
| 극단값이 숨겨질 수 있음 | 느린 사용자 경험을 보여줌 |
| 단독 사용 금지 | p99·오류율과 함께 사용 |

## 18\. Thread Pool과 DB Connection Pool

| Thread Pool | Connection Pool |
| --- | --- |
| Java 작업 실행 | DB 연결 재사용 |
| HTTP·Worker 처리 | SQL 실행 시 사용 |
| CPU·대기와 관계 | DB Session·SQL과 관계 |
| 같은 크기로 설정하지 않음 | DB 처리능력 기준 산정 |

## 19\. RTO와 RPO

| RTO | RPO |
| --- | --- |
| 얼마나 빨리 복구할 것인가 | 어느 시점까지 데이터 손실을 허용할 것인가 |
| 시간 기준 | 데이터 시점 기준 |
| 자동전환·Runbook | Backup·복제 |

## 20\. CI와 CD

| CI | CD |
| --- | --- |
| 코드 검증·Build·Test | 승인 Artifact 배포 |
| 개발변경 품질 | 환경 반영 일관성 |
| Artifact 생성 | Artifact Promotion |

# A.13 용어를 실제 거래에 적용하는 방법

상담예약 등록거래를 예로 들면 다음 용어들이 하나의 흐름에서 연결된다.

\`\`\`text id=“glsA009” 화면 ID CT-RSV-0001

화면 이벤트 ID CT-RSV-0001-E04

ServiceId CT.Reservation.create

거래코드 CT-REG-0101

Handler CtReservationHandler

Facade CtReservationFacade.create

Service CtReservationService.create

Rule 중복·고객·코드 검증

DAO·Mapper Master·History INSERT

Transaction 두 INSERT의 원자성

Idempotency Key 반복 등록 방지

GUID·TraceId 전체 로그 추적

Timeout 전체 처리시간 제한

History 예약 생성 이력

Audit Log 누가 등록했는지 기록

Metric 건수·처리시간·오류율

Quality Gate 배포 전 자동검증

Smoke Test 운영 배포 후 실제 등록 확인

Runbook 등록장애 대응절차



용어를 개별 정의로만 외우지 말고 하나의 거래 안에서 위치와 관계를 찾는 것이 중요하다.

\---

\# A.14 역할별 우선 학습 용어

\## 업무 개발자

\`\`\`text id="glsA010"
업무코드

도메인

ServiceId

Handler

Facade

Service

Rule

DTO

Transaction

Version

멱등성

오류코드

## 화면 개발자

\`\`\`text id=“glsA011” 화면 ID

이벤트 ID

ServiceId

표준전문

Validation

오류코드

Version

Idempotency Key

Authentication

Authorization


\## DB·SQL 개발자

\`\`\`text id="glsA012"
Data Ownership

Transaction

Affected Rows

Business Key

Unique Constraint

Index

Execution Plan

Lock

Optimistic Lock

History

## 운영자

\`\`\`text id=“glsA013” GUID

TraceId

Correlation ID

ServiceId

Timeout

TPS

p95

Thread

Hikari Pending

Health

Incident

Runbook

Rollback


\## 아키텍트

\`\`\`text id="glsA014"
도메인

Aggregate

Contract

Trust Boundary

NFR

ADR

RACI

RTM

Quality Gate

Compatibility

RTO·RPO

## DevOps 담당자

\`\`\`text id=“glsA015” CI·CD

Artifact

Checksum

Release Manifest

Profile

Rolling

Drain

Liveness

Readiness

Smoke Test

Rollback

Roll-forward


\---

\# A.15 용어 사용 금지 사례

\`\`\`text id="glsA016"
모든 Java Class를 Service라고 부른다.

모든 ID를 거래 ID라고 부른다.

GUID·TraceId·Request ID를 구분하지 않는다.

업무 오류와 시스템 오류를 모두 Exception이라고만 부른다.

DB Rollback과 업무 보상처리를 같은 의미로 사용한다.

화면 권한과 서버 인가를 같은 것으로 본다.

Session과 JWT를 서로 대체 가능한 동일 개념으로 설명한다.

Cache를 원본 데이터라고 표현한다.

Scheduler 실행 성공을 Batch 성공이라고 보고한다.

Tomcat Thread와 Hikari Connection을 같은 자원이라고 설명한다.

Health UP을 업무 정상이라고 보고한다.

WAR 복원을 전체 Rollback이라고 부른다.

테스트 통과를 운영 전환 완료라고 표현한다.

문서 작성 완료를 Quality Gate 통과라고 보고한다.

# A.16 용어 정의서 관리 기준

프로젝트에서 새로운 용어를 추가할 때 다음 항목을 기록한다.

| 속성 | 설명 |
| --- | --- |
| 용어 ID | 관리용 식별자 |
| 한글 용어 | 프로젝트 공식 명칭 |
| 영문 용어 | 코드·표준에서 사용하는 명칭 |
| 약어 | TCF·RTM 등 |
| 정의 | 한 문장 정의 |
| 포함 범위 | 해당 용어가 담당하는 범위 |
| 제외 범위 | 혼동하면 안 되는 영역 |
| 예시 | ServiceId·Class·Table 예 |
| 관련 용어 | 상위·하위·비교 개념 |
| 소유 조직 | 정의 승인 책임 |
| 적용 위치 | 문서·소스·설정·로그 |
| 상태 | 초안·승인·폐기 |
| Version | 정의 변경이력 |

# A.17 자동검증 및 품질 Gate

다음 항목은 용어 정합성 자동검증 대상으로 관리한다.

\`\`\`text id=“glsA017” 업무코드와 Module·WAR 일치

ServiceId와 Handler 등록 일치

화면 이벤트와 ServiceId 연결

거래코드와 OM Catalog 연결

오류코드 Prefix와 업무코드 일치

Mapper Method와 SQL ID 일치

Transaction 용어와 실제 Annotation 위치 일치

Version Column과 수정 SQL 조건 일치

Metric 이름과 Dashboard 이름 일치

Release Version과 Artifact Manifest 일치


\---

\# A.18 학습 확인문제

\## 기본

\`\`\`text id="glsA018"
TCF와 업무 WAR의 책임 차이는 무엇인가?

ServiceId와 거래코드는 왜 둘 다 필요한가?

DTO를 하나만 만들어 모든 계층에서 사용하면 왜 문제가 되는가?

Transaction은 어느 계층에서 시작해야 하는가?

Validation과 업무규칙은 무엇이 다른가?

Cache가 원본 데이터가 아닌 이유는 무엇인가?

## 구현

\`\`\`text id=“glsA019” UPDATE 영향 행이 0건이면 어떤 원인이 가능한가?

Version과 Idempotency Key는 각각 어떤 문제를 해결하는가?

Master와 History를 같은 Transaction으로 묶는 이유는 무엇인가?

다른 업무의 Table을 직접 조회하면 어떤 문제가 생기는가?

목록조회에서 Keyset Paging이 필요한 경우는 언제인가?

Query Timeout이 Slow SQL 해결책이 아닌 이유는 무엇인가?


\## 운영

\`\`\`text id="glsA020"
Health UP인데 업무거래가 실패할 수 있는 이유는 무엇인가?

Hikari Pending이 증가한다는 것은 무엇을 의미하는가?

Timeout 증가가 근본 원인이 아닌 이유는 무엇인가?

Rollback과 Roll-forward는 어떤 상황에서 선택하는가?

RTO와 RPO는 각각 무엇을 결정하는가?

Runbook에 명령만 적으면 부족한 이유는 무엇인가?

# A.19 완료 체크리스트

| 확인 항목 | 완료 |
| --- | --- |
| TCF의 역할을 자신의 말로 설명할 수 있다. | □ |
| 한 거래의 전체 처리경로를 설명할 수 있다. | □ |
| ServiceId와 거래코드를 구분한다. | □ |
| Handler·Facade·Service·Rule 책임을 구분한다. | □ |
| DAO·Repository·Mapper 관계를 설명할 수 있다. | □ |
| Request·Command·Data·Response DTO를 구분한다. | □ |
| Validation과 업무규칙을 구분한다. | □ |
| Transaction과 보상처리를 구분한다. | □ |
| Version과 Idempotency를 구분한다. | □ |
| History와 Audit Log를 구분한다. | □ |
| Authentication과 Authorization을 구분한다. | □ |
| 기능권한과 데이터권한을 구분한다. | □ |
| Session과 JWT의 차이를 설명한다. | □ |
| Timeout과 Retry의 위험을 설명한다. | □ |
| Cache와 원본 데이터의 관계를 설명한다. | □ |
| Batch와 Scheduler의 역할을 구분한다. | □ |
| TPS·동시성·p95의 차이를 설명한다. | □ |
| Thread Pool과 DB Pool을 구분한다. | □ |
| Liveness·Readiness·Smoke를 구분한다. | □ |
| Incident·RCA·Runbook을 설명할 수 있다. | □ |
| CI·CD·Artifact·Release를 구분한다. | □ |
| Rollback과 Roll-forward를 구분한다. | □ |
| RTM·ADR·RACI·Quality Gate의 목적을 설명한다. | □ |
| 프로젝트 소스에서 각 용어의 실제 위치를 찾을 수 있다. | □ |
| 용어를 문서·소스·설정·로그에서 동일하게 사용한다. | □ |

# 시사점

## 핵심 아키텍처 판단

첫째, NSIGHT TCF의 용어는 단순한 기술명칭이 아니라 구성요소의 책임과 경계를 나타낸다.

둘째, ServiceId, 업무코드, 화면 ID, 거래코드, GUID와 같은 식별자는 설계·소스·운영을 연결하는 공통 Key다.

셋째, Handler, Facade, Service, Rule, DAO, Mapper를 구분하는 목적은 Class 수를 늘리는 것이 아니라 변경과 장애의 책임을 분리하는 것이다.

넷째, Transaction, Version, Idempotency, History는 변경거래의 중복·유실·부분 반영을 막는 하나의 통합 통제체계다.

다섯째, Authentication, Authorization, 기능권한, 데이터권한을 구분해야 Gateway 우회와 Request 위조를 방지할 수 있다.

여섯째, Timeout, Retry, Circuit Breaker, Bulkhead는 각각 다른 장애문제를 해결하며 무분별하게 함께 적용해서는 안 된다.

일곱째, Log, Metric, Trace, Health, Smoke Test는 서로 대체하는 도구가 아니라 함께 사용해야 하는 운영 증거다.

여덟째, CI/CD, Artifact, Quality Gate, Rollback, Runbook은 개발한 코드를 운영 가능한 서비스로 전환하기 위한 필수 개념이다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 용어 의미 불일치 | 설계·개발 의사소통 오류 |
| 식별자 혼용 | 거래 추적 실패 |
| 계층 용어 혼용 | 책임 중복·의존성 증가 |
| 오류 유형 혼용 | 잘못된 사용자·운영 대응 |
| Version·멱등성 혼동 | 중복·Lost Update |
| History·감사 혼동 | 감사 증적 부족 |
| Session·JWT 혼동 | 인증구조 결함 |
| Timeout·Retry 혼동 | 장애 증폭 |
| Health·Smoke 혼동 | 배포장애 미탐지 |
| Rollback·보상 혼동 | 데이터 유실 |
| TPS·동시성 혼동 | 용량산정 오류 |
| RTO·RPO 혼동 | DR 목표 불명확 |

## 우선 보완 과제

1.  프로젝트 공식 용어 정의서를 별도 Baseline으로 관리한다.
2.  업무코드·화면 ID·ServiceId·거래코드·SQL ID의 관계를 자동검증한다.
3.  GUID·TraceId·Request ID 사용기준을 Header·로그 표준에 명확히 반영한다.
4.  Handler·Facade·Service·Rule·DAO·Mapper 책임을 Architecture Test로 검증한다.
5.  오류 유형별 표준 용어와 오류코드 분류를 통일한다.
6.  Version·Idempotency·History 구현 여부를 변경거래 Gate에 포함한다.
7.  Authentication·Authorization·기능권한·데이터권한 용어를 보안설계서와 일치시킨다.
8.  Thread·DB Pool·JVM Metric 명칭을 OM Dashboard와 일치시킨다.
9.  Health·Smoke·Rollback·Runbook 정의를 운영 전환 절차에 반영한다.
10.  신규 개발자 교육에서 용어 암기보다 실제 거래 추적 실습을 수행한다.

# 마무리말

NSIGHT TCF의 핵심 용어를 이해한다는 것은 정의를 외우는 것이 아니다.

\`\`\`text id=“glsA021” 이 용어가 거래 흐름의 어디에 있는가?

어떤 책임을 가져야 하는가?

어떤 구성요소와 연결되는가?

정상·오류·장애 상황에서 무엇을 확인해야 하는가?



를 설명할 수 있어야 한다.

예를 들어 한 개의 상담예약 등록거래에서도 다음 용어가 함께 작동한다.

\`\`\`text id="glsA022"
화면 ID

→ 이벤트 ID

→ ServiceId

→ Handler

→ Facade

→ Service·Rule

→ Transaction

→ Mapper·SQL

→ Version·Idempotency

→ History·Audit

→ GUID·TraceId

→ Metric·Alert

→ Quality Gate·Rollback

각 용어는 독립된 지식이 아니다.

\`\`\`text id=“glsA023” 거래를 식별하고

책임을 나누고

데이터를 안전하게 변경하며

오류를 통제하고

운영에서 추적하고

장애 후 복구하기 위한

하나의 연결된 언어다. \`\`\`

개발자·운영자·아키텍트가 같은 용어를 같은 의미로 사용할 때 NSIGHT TCF의 표준과 책임 경계가 실제 시스템에서 유지될 수 있다.
