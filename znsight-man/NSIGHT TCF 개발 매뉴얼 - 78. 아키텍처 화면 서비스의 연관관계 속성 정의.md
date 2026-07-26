좋습니다. 지금부터 만들 대상은 단순한 프로그램 목록이 아니라, 화면 이벤트를 시작점으로 ServiceId·Java 프로그램·SQL·테이블까지 연결하는 산출물 추적체계입니다.

NSIGHT TCF에서는 업무별 Controller가 Service를 직접 호출하는 구조보다, 공통 Controller가 요청을 받아 ServiceId로 Handler를 선택하고 업무 계층을 실행하는 구조가 기준입니다.

1. 전체 관계 기준

화면

↓

화면 이벤트

↓

ServiceId를 포함한 표준 전문 요청

↓

OnlineTransactionController

↓

TCF.process()

↓

STF 전처리

↓

TransactionDispatcher

↓ ServiceId 기준

Handler

↓

Facade

↓

Service

├─ Rule

├─ DAO

│    ↓

│  Mapper Interface

│    ↓

│  Mapper XML / SQL ID

│    ↓

│  Table / View

└─ 외부 시스템 Client

↓

ETF

↓

표준 응답

↓

화면 결과 처리

DB 객체와 프로그램도 ServiceId → Handler/Service/Rule → DAO/Mapper → SQL ID → Table의 추적 관계를 유지해야 합니다.

2. 중요한 구조 판단

2.1 Controller와 Service의 관계

NSIGHT TCF에서는 다음 구조를 금지하는 것이 원칙입니다.

화면

→ 업무별 Controller

→ Service

→ Mapper

권장 구조는 다음과 같습니다.

화면

→ 공통 OnlineTransactionController

→ TCF

→ ServiceId Dispatcher

→ Handler

→ Facade

→ Service

→ Rule / DAO / Mapper

따라서 산출물에서 Controller는 대부분 다음처럼 공통 프로그램으로 나타납니다.

| 구분 | 프로그램 |
| --- | --- |
| 공통 Controller | OnlineTransactionController |
| 공통 처리 엔진 | TCF, STF, ETF |
| 거래 라우터 | TransactionDispatcher |
| 업무 진입 프로그램 | 업무별 Handler |
| 업무 처리 프로그램 | Facade, Service, Rule |
| 데이터 접근 | DAO, Mapper |

즉, 화면 이벤트와 직접 연결되는 업무 식별자는 Controller가 아니라 ServiceId입니다.

3. 산출물의 핵심 식별자

| 식별자 | 역할 | 예시 |
| --- | --- | --- |
| 화면 ID | 화면 식별 | SV-CUS-0001 |
| 이벤트 ID | 화면 이벤트 식별 | SV-CUS-0001-E01 |
| 기능 ID | 조회·등록·다운로드 등 기능 식별 | SV-CUS-0001-F01 |
| ServiceId | 실행할 업무 거래 식별 | SV.Customer.selectSummary |
| 거래코드 | 통제·감사·통계 식별 | SV-INQ-0001 |
| Handler | ServiceId 대응 업무 진입점 | SvCustomerHandler |
| Facade Method | 유스케이스·트랜잭션 경계 | selectCustomerSummary() |
| Service Method | 업무 처리 | selectSummary() |
| Rule Method | 업무 규칙 검증 | validateInquiry() |
| DAO Method | 데이터 접근 추상화 | selectCustomerSummary() |
| Mapper ID | MyBatis Mapper 식별 | CustomerMapper.selectSummary |
| SQL ID | XML Statement 식별 | selectSummary |
| DB 객체 | 실제 조회·변경 대상 | SV_CUSTOMER_SUMMARY |

4. 관계의 다중성

모든 관계를 단순히 1:1로 보면 실제 구현을 표현하기 어렵습니다.

| 관계 | 일반적인 관계 | 설명 |
| --- | --- | --- |
| 화면 → 이벤트 | 1:N | 한 화면에 조회·등록·다운로드 등 여러 이벤트 존재 |
| 이벤트 → ServiceId | N:M | 한 이벤트가 여러 거래를 호출할 수 있고 같은 거래가 여러 화면에서 사용될 수 있음 |
| ServiceId → Handler | 1:1 논리 관계 | 하나의 ServiceId는 실행할 Handler가 하나여야 함 |
| Handler Class → ServiceId | 1:N 가능 | 하나의 Handler 클래스가 여러 ServiceId를 처리할 수 있음 |
| ServiceId → Facade Method | 원칙적 1:1 | 거래별 유스케이스와 트랜잭션 경계 명확화 |
| Facade → Service | 1:N | 하나의 유스케이스가 여러 업무 서비스를 조합할 수 있음 |
| Service → Rule | 1:N | 여러 업무 규칙 검증 가능 |
| Service → DAO | 1:N | 여러 데이터 접근 프로그램 호출 가능 |
| DAO Method → Mapper SQL ID | 원칙적 1:1 | SQL 추적성을 위해 일대일 권장 |
| Mapper SQL → Table | N:M | 하나의 SQL이 여러 테이블을 조인할 수 있음 |

특히 화면 이벤트 ↔ ServiceId는 다대다 관계가 가능하므로 별도의 매핑표가 필요합니다.

5. 작성해야 할 산출물

5.1 화면 이벤트 정의서

| 항목 | 설명 |
| --- | --- |
| 화면 ID | 이벤트가 발생하는 화면 |
| 화면명 | 사용자 화면명 |
| 이벤트 ID | 화면 내 고유 이벤트 |
| 이벤트명 | 조회, 저장, 삭제, 다운로드 등 |
| 이벤트 유형 | 클릭, 변경, 초기화, 화면 로딩 |
| UI 객체 ID | 버튼, 그리드, 입력항목 |
| 발생 조건 | 이벤트 실행 조건 |
| 선행 검증 | 필수값·형식·권한 검증 |
| 호출 ServiceId | 서버 거래 식별자 |
| 성공 처리 | 화면 표시·팝업·재조회 |
| 실패 처리 | 오류 메시지와 화면 상태 |
| 중복 방지 | 버튼 잠금·요청키 적용 여부 |

5.2 화면 이벤트–ServiceId 매핑표

| 화면 ID | 이벤트 ID | 호출순서 | ServiceId | 거래코드 | 호출조건 | 동기 여부 | 필수 여부 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SV-CUS-0001 | E01 | 1 | SV.Customer.selectSummary | SV-INQ-0001 | 고객번호 입력 | 동기 | 필수 |
| SV-CUS-0001 | E01 | 2 | SV.Customer.selectContact | SV-INQ-0002 | 요약조회 성공 | 병렬 | 선택 |

이 표가 화면과 서버 프로그램을 연결하는 첫 번째 핵심 산출물입니다.

5.3 ServiceId–프로그램 매핑표

| ServiceId | Controller | Handler | Facade | Service | Rule | DAO | Mapper |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SV.Customer.selectSummary | OnlineTransactionController | SvCustomerHandler | SvCustomerFacade | SvCustomerService | SvCustomerInquiryRule | SvCustomerDao | SvCustomerMapper |

5.4 DAO–Mapper–DB 객체 매핑표

| DAO Method | Mapper Namespace | SQL ID | SQL 유형 | 대상 객체 | 처리 |
| --- | --- | --- | --- | --- | --- |
| selectCustomerSummary() | SvCustomerMapper | selectCustomerSummary | SELECT | SV_CUSTOMER_SUMMARY | 고객요약 조회 |
| selectCustomerGrade() | SvCustomerMapper | selectCustomerGrade | SELECT | SV_CUSTOMER_GRADE | 고객등급 조회 |

5.5 전체 추적성 매트릭스

최종 산출물에서는 다음 항목을 한 행으로 연결합니다.

| 화면 | 이벤트 | ServiceId | Handler | Facade | Service | Rule | DAO | Mapper SQL | Table |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 고객요약 | 조회 버튼 | SV.Customer.selectSummary | SvCustomerHandler.handleSelectSummary | SvCustomerFacade.selectSummary | SvCustomerService.selectSummary | SvCustomerInquiryRule.validate | SvCustomerDao.selectSummary | SvCustomerMapper.selectSummary | SV_CUSTOMER_SUMMARY |

이 매트릭스가 있으면 다음 질문에 바로 답할 수 있습니다.

이 버튼을 누르면 어떤 ServiceId가 호출되는가?

이 ServiceId는 어떤 Java 프로그램을 실행하는가?

어떤 Rule을 검사하는가?

어떤 DAO와 Mapper SQL을 실행하는가?

어떤 테이블을 조회하거나 변경하는가?

이 SQL을 변경하면 어떤 화면이 영향을 받는가?

6. 예시

6.1 화면 이벤트

화면 ID    : SV-CUS-0001

화면명      : 고객 종합정보 조회

이벤트 ID   : SV-CUS-0001-E01

이벤트명    : 고객요약 조회

UI 객체 ID  : btnSearch

ServiceId   : SV.Customer.selectSummary

거래코드    : SV-INQ-0001

6.2 프로그램 호출 흐름

SV-CUS-0001의 조회 버튼

↓

callTransaction("SV.Customer.selectSummary")

↓

POST /sv/online

↓

OnlineTransactionController

↓

TCF.process()

↓

TransactionDispatcher

↓

SvCustomerHandler.handle()

↓

SvCustomerFacade.selectSummary()

↓

SvCustomerService.selectSummary()

├─ SvCustomerInquiryRule.validate()

└─ SvCustomerDao.selectSummary()

↓

SvCustomerMapper.selectSummary

↓

SV_CUSTOMER_SUMMARY

6.3 상세 추적표 예시

| 구분 | 프로그램·식별자 | 메서드·처리 |
| --- | --- | --- |
| 화면 | SV-CUS-0001 | 고객 종합정보 조회 |
| 이벤트 | SV-CUS-0001-E01 | 조회 버튼 클릭 |
| ServiceId | SV.Customer.selectSummary | 고객요약조회 거래 |
| 거래코드 | SV-INQ-0001 | 조회·감사·통제 식별 |
| Controller | OnlineTransactionController | online() |
| Handler | SvCustomerHandler | handleSelectSummary() |
| Facade | SvCustomerFacade | selectSummary() |
| Service | SvCustomerService | selectSummary() |
| Rule | SvCustomerInquiryRule | validate() |
| DAO | SvCustomerDao | selectSummary() |
| Mapper | SvCustomerMapper | selectSummary() |
| SQL ID | selectCustomerSummary | 고객요약 SELECT |
| DB 객체 | SV_CUSTOMER_SUMMARY | 고객요약 데이터 |
| 응답 DTO | SvCustomerSummaryResponse | 화면 응답 데이터 |

7. 최종 산출물 권장 구성

1. 화면 목록

2. 화면별 이벤트 목록

3. 화면 이벤트–ServiceId 매핑표

4. ServiceId 정의서

5. ServiceId–Handler 매핑표

6. 프로그램 목록

7. Controller/Handler 설계서

8. Facade/Service 설계서

9. Rule 설계서

10. DAO 설계서

11. Mapper·SQL 설계서

12. DB 객체 영향도표

13. 전체 End-to-End 추적성 매트릭스

14. 화면 기준 영향도 분석표

15. 프로그램 기준 역추적표

가장 먼저 작성할 기준 산출물은 **화면 이벤트–ServiceId–프로그램 전체 추적성 매트릭스**입니다. 이 매트릭스를 기준으로 각 Controller, Service, Rule, DAO, Mapper 프로그램 설계서를 분리하면 산출물 간 불일치를 최소화할 수 있습니다.

