<!-- source: ztcf-집필본/처음_시작하는_NSIGHT_TCF_개발_원고_v1.3_출판편집본.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

**BEGINNER'S GUIDE TO ENTERPRISE TRANSACTIONS**

**처음 시작하는 NSIGHT TCF 개발**

화면에서 DB까지, 개발에서 운영까지 따라 만드는
엔터프라이즈 업무 시스템

전체 원고 · 제0부~제10부 · 0장~61장

출판 편집본 v1.3 · 2026년 7월 18일

# 전체 목차

## 제0부. 책을 시작하기 전에

0장. 이 책을 읽기 전에

1장. 역할별 학습 경로

2장. 실습 프로젝트 소개

## 제1부. 초보 개발자를 위한 IT 시스템 기초

3장. 기업의 IT 시스템은 어떻게 구성되는가

4장. 웹 애플리케이션의 기본 구조

5장. Java와 Spring Boot 이해하기

6장. Tomcat과 WAR 이해하기

7장. 프레임워크가 필요한 이유

## 제2부. NSIGHT TCF 전체 구조 이해

8장. NSIGHT 프로젝트란 무엇인가

9장. NSIGHT 전체 아키텍처

10장. TCF 실행 흐름 한눈에 보기

11장. ServiceId와 거래코드

12장. OM 운영관리란 무엇인가

## 제3부. 개발환경 만들기

13장. 개발도구 준비

14장. Git 저장소와 소스 내려받기

15장. Gradle 멀티모듈 구조

16장. 프로젝트를 IDE에서 실행하기

17장. application.yml 읽기

## 제4부. 화면 요청 한 건 따라가기

18장. 화면 이벤트에서 거래가 시작된다

19장. 표준 요청 전문

20장. Controller와 TCF 진입

21장. STF 전처리 이해하기

22장. Dispatcher와 Handler 찾기

23장. ETF와 표준 응답

## 제5부. 업무 프로그램 6계층 개발

24장. 패키지와 도메인 설계

25장. Handler 개발

26장. Facade 개발

27장. Service 개발

28장. Rule 개발

29장. DAO 개발

30장. MyBatis Mapper 개발

31장. DTO와 Validation

## 제6부. DB·SQL·업무 연계 개발

32장. 데이터베이스 기초

33장. CRUD 거래 만들기

34장. SQL 작성 기준

35장. 페이징과 대량 데이터

36장. DB 객체 명명규칙

37장. 다른 업무와 연계하기

## 제7부. 인증·보안·권한 이해

38장. 로그인과 인증의 기본 개념

39장. JWT 쉽게 이해하기

40장. JWT 발급과 검증

41장. Gateway가 있는 구조와 없는 구조

42장. 권한과 개인정보

43장. 세션 제거와 JWT 전환

## 제8부. 배포·운영·장애 대응

44장. 업무 WAR 배포하기

45장. JVM과 Tomcat 자원 이해하기

46장. DB Connection Pool 이해하기

47장. 거래 Timeout과 거래통제

48장. 로그로 거래 추적하기

49장. TCF-OM으로 운영 상태 확인하기

50장. 장애 원인 찾기

## 제9부. 테스트·품질·개발방법론

51장. 단위테스트와 통합테스트

52장. 오류·Timeout·보안 테스트

53장. 성능과 장애 테스트

54장. 코드 품질과 자동검증

55장. 프로젝트는 어떤 순서로 진행하는가

## 제10부. 처음부터 끝까지 만드는 실전 프로젝트

56장. 고객 종합정보 조회 설계

57장. 조회 거래 구현

58장. 등록·변경·삭제 거래 구현

59장. 보안과 오류 처리 추가

60장. 배포와 운영 확인

61장. 설계서와 산출물 완성하기

부록 A. 핵심 용어집

부록 B. 신규 거래 개발 완료 체크리스트

부록 C. 추적성 매트릭스 작성 예시

# 제0부. 책을 시작하기 전에

제0부는 독자가 이 책의 목표와 실습 범위를 이해하고 자신의 역할에 맞는 학습 경로를 선택하도록 돕는다. 여기서 정의한 고객 360 뷰는 이후 모든 개념·코드·운영 설명을 연결하는 기준 사례다.

## 제0부 목차

-   0장. 이 책을 읽기 전에
-   1장. 역할별 학습 경로
-   2장. 실습 프로젝트 소개

**CHAPTER 0**

**이 책을 읽기 전에**

> **이번 장에서 해결할 질문** 이 책에서 무엇을 만들고, 어떤 관점으로 읽어야 하는가?

## 0.1 이 책을 만든 이유

기업의 업무 시스템을 처음 접한 개발자는 코드보다 먼저 전체 구조에서 막힌다. 화면에서 버튼을 눌렀을 뿐인데 요청은 웹 서버, Gateway, 업무 WAR, 거래 프레임워크, 업무 프로그램, 데이터베이스를 차례로 지난다. 그 사이에는 인증, 권한, 거래통제, Timeout, 로그처럼 화면에 보이지 않는 규칙도 작동한다.

작은 예제에서는 Controller 하나와 SQL 하나만으로 결과를 만들 수 있다. 하지만 여러 업무팀이 같은 시스템을 함께 개발하고 운영하려면 요청 형식, 프로그램 배치, 오류 응답, 거래 추적 방법을 통일해야 한다. NSIGHT TCF는 이 공통 규칙을 프레임워크로 제공한다.

> **한 문장으로** 업무 코드는 개발자가 만들고, 거래를 안전하게 실행·통제·기록하는 공통 절차는 TCF가 맡는다.

이 책은 TCF의 모든 클래스와 설정을 처음부터 외우게 하지 않는다. 고객 종합정보 조회라는 한 건의 거래를 따라가며 먼저 큰 그림을 익히고, 그 거래를 CRUD·연계·보안·운영으로 확장한다. 마지막에는 설계서와 테스트 결과까지 연결해 하나의 업무 기능을 끝까지 완성한다.

## 0.2 이 책에서 만들 시스템

실습의 중심은 ‘고객 360 뷰’다. 고객번호로 기본정보와 등급을 조회하고, 상담 과정에서 남긴 고객 메모를 등록·수정·삭제한다. 이후 상품정보 시스템을 호출하고, JWT와 데이터 권한을 적용하며, 배포 후 OM과 로그에서 거래를 확인한다.

| **거래** | **대표 식별자** | **배우는 것** |
| --- | --- | --- |
| 고객 종합정보 조회 | SV.Customer.selectSummary | 표준 전문, Dispatcher, 6계층 조회 |
| 고객 목록 조회 | SV.Customer.selectList | 검색조건, 페이징, 대량조회 통제 |
| 고객 메모 등록 | SV.CustomerMemo.create | Validation, 트랜잭션, 감사로그 |
| 고객 메모 변경·삭제 | SV.CustomerMemo.update/delete | 동시성, 논리삭제, Rollback |
| 상품정보 연계 조회 | SV.Product.selectLinked | WAR 간 호출, Timeout, 오류 전파 |

예제의 업무코드와 식별자는 학습용 기준이다. 실제 프로젝트에서는 OM Service Catalog, 업무코드 표준표, 현행 코드베이스를 대조해 확정해야 한다.

## 0.3 대상 독자와 선행지식

주 독자는 Java와 Spring Boot를 조금 사용해 보았지만 NSIGHT TCF는 처음인 개발자다. 클래스와 메서드, JSON, SELECT 문을 본 적이 있으면 충분하다. 익숙하지 않은 개념은 제1부에서 다시 설명한다.

| **독자** | **권장 출발점** | **목표** |
| --- | --- | --- |
| 완전 초보자 | 0장부터 순서대로 | 전체 요청 흐름과 용어 설명 |
| Spring 개발자 | 2부→4부→5부 | TCF 거래와 6계층 구현 |
| 업무 개발자 | 4부→5부→10부 | 조회·CRUD 거래 독립 구현 |
| 운영 담당자 | 2부→8부 | OM·로그·자원 지표 기반 진단 |
| 예비 아키텍트 | 전편 + 9부 | 책임 경계와 품질 Gate 판단 |

## 0.4 초보 개발자가 어려워하는 지점

TCF가 어려운 이유는 개별 기술이 특별해서가 아니라, 여러 기술과 규칙이 한 거래 안에서 동시에 움직이기 때문이다. 다음 네 지점을 먼저 구분하면 학습 속도가 빨라진다.

-   기능을 URL이 아니라 serviceId로 찾는다는 점
-   STF·Dispatcher·ETF 같은 프레임워크 영역과 Handler 아래 업무 영역의 경계
-   Handler → Facade → Service → Rule → DAO → Mapper의 책임 차이
-   코드만 맞아도 끝이 아니며 OM 등록, Timeout, 로그, 배포가 함께 맞아야 한다는 점

| **초보자의 첫 질문** | **바꿔야 할 질문** |
| --- | --- |
| 어떤 Controller를 만들지? | 어떤 serviceId와 Handler에 연결할지? |
| 로직을 어디에 넣지? | 어느 계층의 책임인지? |
| 오류 JSON을 어떻게 만들지? | 어떤 예외를 던져 ETF에 맡길지? |
| 느리면 Thread를 늘릴지? | 어느 구간에서 어떤 자원을 기다리는지? |

## 0.5 책에서 사용하는 예제 업무

고객 종합정보 조회는 여러 정보를 한 화면에 모으는 정보계의 대표 업무다. 조회로 시작하기 때문에 데이터 변경 부담이 작고, 이후 메모 CRUD와 상품 연계를 붙이면 트랜잭션·보안·Timeout까지 자연스럽게 확장할 수 있다.

> POST /sv/online
> header.businessCode = "SV"
> header.serviceId = "SV.Customer.selectSummary"
> header.transactionCode = "SV-INQ-0001"
> body.customerNo = "C0000001"

요청은 공통 Online Endpoint로 들어간다. STF가 Header와 보안 문맥을 검사하고, Dispatcher가 serviceId에 대응하는 Handler를 찾는다. Handler 아래 6계층이 고객정보를 조회해 반환하면 ETF가 결과를 표준 응답으로 감싸고 거래로그를 마무리한다.

## 0.6 소스와 설계서 활용 방법

문서와 코드가 다를 때는 한쪽을 무조건 틀렸다고 판단하지 않는다. 설계서는 목표 구조와 의도를 설명하고, 코드는 특정 시점의 구현을 보여 준다. 집필과 개발에서는 다음 우선순위를 사용한다.

1.  동작·클래스·serviceId는 현행 코드와 OM Catalog를 확인한다.
2.  구현·설정 상세는 기술 정의서와 실제 설정 파일을 확인한다.
3.  절차와 체크리스트는 개발 매뉴얼을 따른다.
4.  설계 의도와 원칙은 아키텍처 설계서와 zman을 참고한다.
5.  이 책은 위 근거를 초보자의 학습 순서로 재구성한다.

> **주의** 설계서의 클래스명이나 포트 값을 그대로 복사하지 말고, 적용 환경의 코드·Catalog·설정 파일과 반드시 대조한다.

## 장 요약

-   TCF는 온라인 거래의 실행·통제·기록 절차를 통일한다.
-   이 책은 고객 종합정보 조회에서 시작해 CRUD·보안·운영으로 확장한다.
-   기술 사실은 코드, Catalog, 설정, 설계 의도를 구분해 검증한다.

## 근거 자료

-   ztcfbook-m/서문/00-서문.md
-   ztcfbook/서문/00-서문.md
-   zman/01-문서개요.md, 03-전체아키텍처.md

**CHAPTER 1**

**역할별 학습 경로**

> **이번 장에서 해결할 질문** 내 역할과 경험에 맞춰 이 책을 어떤 순서로 읽어야 하는가?

600쪽 규모의 기술서를 처음부터 끝까지 같은 속도로 읽는 것은 비효율적이다. 먼저 자신의 역할에 필요한 최소 경로를 완주하고, 실무에서 부딪히는 문제를 기준으로 나머지 장을 확장해 읽는 편이 좋다.

## 1.1 완전 초보자를 위한 읽기 순서

완전 초보자는 용어보다 흐름을 먼저 익힌다. 사용자가 버튼을 누른 뒤 응답이 돌아오기까지의 경로를 그림으로 설명할 수 있으면 첫 단계는 성공이다.

1.  제1부에서 브라우저, WAS, DB, Java, Spring Boot의 역할을 익힌다.
2.  제2부에서 NSIGHT 전체 구조와 TCF 실행 흐름을 한 장의 그림으로 정리한다.
3.  제4부에서 요청 한 건이 STF·Dispatcher·Handler·ETF를 통과하는 과정을 따라간다.
4.  제10부 56~57장에서 고객 조회 거래를 실행한다.

> **완주 기준** serviceId가 왜 필요한지, Handler가 무엇을 호출하는지, SQL이 어디에 있는지를 자기 말로 설명한다.

## 1.2 업무 개발자를 위한 읽기 순서

업무 개발자는 개발 표준과 계층 책임을 빠르게 익힌 뒤 실제 거래를 구현해야 한다. 제4부와 제5부가 중심이고, DB·연계·보안 장은 맡은 기능에 따라 선택한다.

| **순서** | **읽을 부분** | **산출물** |
| --- | --- | --- |
| 1 | 11장, 18~23장 | ServiceId·표준 전문·TCF 흐름 메모 |
| 2 | 24~31장 | Handler~Mapper 프로그램 목록 |
| 3 | 32~37장 | SQL·테이블·연계 설계 |
| 4 | 51~52장 | 단위·통합·오류 테스트 |
| 5 | 56~61장 | 실전 거래와 설계서 완성 |

## 1.3 프레임워크 개발자를 위한 읽기 순서

프레임워크 개발자는 업무 기능보다 공통 파이프라인의 계약과 확장 지점을 먼저 본다. STF의 검사 순서, Dispatcher 등록 규칙, ETF 오류 매핑, ThreadLocal 정리, Timeout 전파가 핵심이다.

-   제2부: TCF 전체 구조, ServiceId, OM 기준정보
-   제4부: Controller → STF → Dispatcher → Handler → ETF
-   제7부: JWT·Gateway·권한 책임 경계
-   제8부: Thread·Pool·Timeout·로그·장애 진단
-   제9부: ArchUnit, 중복 ServiceId 검사, CI/CD 품질 Gate

> **검토 질문** 공통 기능을 추가할 때 모든 업무 WAR에 어떤 영향이 생기며, 실패 시 거래로그와 Context 정리는 보장되는가?

## 1.4 운영 담당자를 위한 읽기 순서

운영 담당자는 코드 전체보다 거래를 식별하고 상태를 관찰하는 방법이 중요하다. GUID·TraceId·serviceId를 기준으로 거래를 찾고, Tomcat Thread, JVM, DB Pool, SQL, 외부 연계 순으로 병목을 좁힌다.

| **증상** | **먼저 볼 것** | **연결 장** |
| --- | --- | --- |
| 거래가 실행되지 않음 | 거래통제·Catalog·권한 | 12, 47, 49장 |
| 응답이 느림 | 거래로그·Thread·DB Pool·Slow SQL | 45, 46, 48, 50장 |
| Timeout | 서비스·SQL·외부호출 Timeout 체인 | 47, 52장 |
| 배포 후 오류 | Context Path·Profile·WAR·로그 | 44, 60장 |
| 보안 사고 우려 | 권한·마스킹·감사로그 | 42, 48, 59장 |

## 1.5 아키텍트를 목표로 하는 학습 경로

아키텍트는 ‘어떻게 구현하는가’뿐 아니라 ‘왜 이 책임을 이 위치에 두는가’를 설명해야 한다. 설계와 현행 구현의 차이를 기록하고, 품질 속성을 시험과 운영 지표로 연결한다.

1.  전체 요청 흐름과 모듈 경계를 그린다.
2.  식별자·계층·DB·연계·보안 표준의 일관성을 검토한다.
3.  성능·가용성·보안·운영성 요구를 수치와 시험조건으로 바꾼다.
4.  요구사항→설계→프로그램→테스트의 추적성을 만든다.
5.  Architecture Gate에서 예외와 기술부채를 승인·기록한다.

## 장 요약

-   모든 독자가 같은 순서로 읽을 필요는 없다.
-   먼저 역할별 최소 완결 경로를 완주하고 필요한 영역을 확장한다.
-   읽기의 결과는 설명, 코드, 운영 확인, 설계 판단 같은 구체적 산출물이어야 한다.

## 근거 자료

-   ztcfbook-m/README.md 및 제01편
-   ztcfbook/서문/00-서문.md의 역할별 권장 경로
-   znsight-구축방법론/Architecture Gate 단계별 체크리스트

**CHAPTER 2**

**실습 프로젝트 소개**

> **이번 장에서 해결할 질문** 책 전체를 관통하는 고객 360 뷰를 어떤 거래로 나누어 구현할 것인가?

## 2.1 고객 종합정보 조회 화면

사용자는 고객번호를 입력하고 조회 버튼을 누른다. 화면은 고객 기본정보, 등급, 담당 지점, 최근 접촉 이력을 한 번에 보여 준다. 첫 실습에서는 범위를 줄여 고객번호·이름·등급·상태만 반환한다.

> 화면 이벤트: customerSearch.click
> 업무코드: SV
> serviceId: SV.Customer.selectSummary
> 거래코드: SV-INQ-0001
> 처리유형: INQUIRY
> 진입점: POST /sv/online

| **입력** | **필수** | **설명** |
| --- | --- | --- |
| customerNo | Y | 고객 식별번호 |
| includeContact | N | 최근 접촉정보 포함 여부 |

| **출력** | **설명** |
| --- | --- |
| customerNo | 조회한 고객번호 |
| customerName | 마스킹 정책이 적용된 고객명 |
| customerGrade | 고객등급 |
| customerStatus | 정상·휴면·해지 등 상태 |

## 2.2 고객 목록 조회

목록 조회는 단건 조회보다 운영 위험이 크다. 검색조건 없이 전체 고객을 읽거나 Page Size를 제한하지 않으면 DB와 WAS 자원을 빠르게 소모한다. 따라서 DB 레벨 페이징, 최대 Page Size, 안정적인 정렬 기준을 처음부터 설계한다.

| **항목** | **학습 기준** |
| --- | --- |
| serviceId | SV.Customer.selectList |
| 검색조건 | 고객명·지점·상태 중 최소 하나 또는 별도 승인 |
| Page Size | 기본값과 최대값을 정책으로 제한 |
| SQL | Count SQL과 List SQL을 분리 |
| 정렬 | 동일 값에서도 순서가 바뀌지 않는 보조키 포함 |

> **운영 관점** 목록 조회의 성공 기준은 결과가 나오는 것만이 아니다. 대량 요청을 통제하고 반복 호출에서도 예측 가능한 응답시간을 유지해야 한다.

## 2.3 고객 메모 등록·수정·삭제

조회 거래를 이해한 뒤 데이터 변경 거래로 확장한다. 메모 등록은 입력 검증과 생성자 기록, 수정은 변경 가능 상태와 동시성, 삭제는 논리삭제와 감사로그를 학습하기에 적합하다.

| **행위** | **serviceId** | **핵심 검증** |
| --- | --- | --- |
| 등록 | SV.CustomerMemo.create | 고객 존재, 내용 필수, 길이 제한 |
| 수정 | SV.CustomerMemo.update | 메모 존재, 작성자·권한, 버전 |
| 삭제 | SV.CustomerMemo.delete | 삭제 권한, 이미 삭제된 상태 |

CUD 거래의 트랜잭션 경계는 Facade에 둔다. 업무 오류는 BusinessException으로 전파하고 Handler에서 임의의 응답을 만들지 않는다. 실패 시 변경은 Rollback되고 ETF가 표준 오류 응답과 거래로그를 마무리한다.

## 2.4 상품정보 연계 조회

고객이 가입한 상품의 상세정보가 다른 업무 WAR나 외부 시스템에 있다면 직접 테이블을 읽지 않는다. 표준 연계 Client를 통해 호출하고, 호출 Timeout·오류 변환·추적 ID 전파를 설계한다.

> SV 고객요약 Service
> → 고객 기본정보 DAO
> → 상품 연계 Client
> → 상대 업무의 /online + serviceId
> → 결과 조립
> → ETF 표준 응답

-   호출 대상과 계약을 serviceId·전문으로 명확히 한다.
-   전체 거래 Timeout보다 짧은 외부 호출 Timeout을 둔다.
-   상대 시스템 오류를 숨기지 말고 표준 오류로 변환한다.
-   GUID·TraceId를 전달해 End-to-End 추적성을 유지한다.

## 2.5 오류·Timeout·권한 처리

정상 경로만 동작하는 프로그램은 운영 가능한 프로그램이 아니다. 실습에서는 실패를 의도적으로 만들고, 어느 계층이 어떤 책임으로 처리하는지 확인한다.

| **상황** | **책임 위치** | **기대 결과** |
| --- | --- | --- |
| customerNo 누락 | Rule / Validation | 업무 오류 코드 |
| 권한 없는 고객 조회 | STF 권한 + Rule 데이터 범위 | 접근 거부·감사 기록 |
| DB 지연 | SQL Timeout + 거래 Timeout | Timeout 표준 응답 |
| 상품 시스템 오류 | 연계 Client / Service | 표준화된 외부 연계 오류 |
| 중복 등록 요청 | STF 멱등성 + 업무키 | 중복 처리 방지 |

## 2.6 배포와 운영 확인

개발 완료의 기준은 로컬 테스트 통과가 아니다. WAR를 빌드하고 Tomcat에 배포한 뒤 OM 기준정보와 정책을 등록하고, 실제 거래로그에서 요청을 추적해야 한다.

1.  Gradle Build와 Test를 수행해 WAR를 생성한다.
2.  Tomcat Context Path와 Profile을 확인하고 배포한다.
3.  OM Service Catalog에 serviceId와 거래 속성을 등록한다.
4.  거래통제와 Timeout 정책을 등록·확인한다.
5.  정상·업무 오류·Timeout 거래를 호출한다.
6.  GUID·TraceId·serviceId로 거래로그를 검색한다.
7.  장애 재현과 Rollback 절차를 확인한다.

## 직접 해보기

아직 코드를 작성하지 말고 한 장짜리 거래 정의서를 만든다. 화면 이벤트, businessCode, serviceId, 거래코드, 요청·응답 필드, 관련 프로그램, SQL, 정상·오류 테스트 조건을 적는다. 빈칸이 있다면 이후 장에서 무엇을 배워야 하는지가 드러난다.

## 확인 문제

1.  고객 상세 조회와 고객 목록 조회를 같은 serviceId로 만들면 어떤 문제가 생기는가?
2.  메모 등록의 트랜잭션 경계를 Handler가 아니라 Facade에 두는 이유는 무엇인가?
3.  상품정보를 다른 업무 테이블에서 직접 조회하면 어떤 책임 경계가 무너지는가?
4.  로컬에서 성공한 거래가 운영에서 통제될 수 있는 이유는 무엇인가?

## 장 요약

-   책 전체의 실습은 고객 조회→목록→메모 CRUD→상품 연계→보안·운영으로 확장된다.
-   각 거래는 화면 이벤트, serviceId, 거래코드, 전문, 프로그램, SQL, 테스트 조건으로 추적된다.
-   운영 가능한 완료 기준에는 배포, OM 정책, 로그 추적, 실패 시나리오가 포함된다.

## 근거 자료

-   ztcfbook-m/제08편/22-SV-고객요약-따라하기.md
-   ztcfbook-m/제08편/23-목록-등록-한-걸음-더.md
-   znsight-man/71~73 샘플 문서
-   ztcfbook/제08편/22~23장

# 제1부. 초보 개발자를 위한 IT 시스템 기초

제1부는 웹·Java·Spring·Tomcat을 따로 암기하지 않고, 화면 요청 한 건이 기업 시스템을 통과하는 흐름 안에서 이해하도록 구성한다.

**CHAPTER 3**

**기업의 IT 시스템은 어떻게 구성되는가**

> **이번 장에서 해결할 질문** 화면에서 시작된 요청은 어떤 계층을 거쳐 데이터베이스까지 도달하는가?

## 3.1 사용자와 업무 화면

기업 시스템의 출발점은 기술이 아니라 업무다. 사용자는 고객조회, 상담등록, 상품검색처럼 업무 목적을 화면에서 수행한다. 화면은 데이터를 보여 주고 입력을 받지만, 업무 규칙과 데이터 원장을 직접 소유하지 않는다.

> **핵심 관점** 화면은 거래의 시작점이지 업무 처리의 전부가 아니다. 버튼 하나는 서버에서 실행할 거래 식별자와 요청 데이터를 만들어야 한다.

| **화면 요소** | **서버로 전달되는 의미** | **예** |
| --- | --- | --- |
| 화면 ID | 어느 화면에서 시작됐는지 | SV-CUST-001 |
| 이벤트 ID | 어떤 사용자 동작인지 | search.click |
| serviceId | 실행할 서버 거래 | SV.Customer.selectSummary |
| 입력 필드 | 업무 처리 조건 | customerNo=C0000001 |

## 3.2 웹서버와 WAS

웹서버는 외부 요청을 받아 SSL을 종료하고 정적 파일을 제공하며, URL에 따라 적절한 WAS로 요청을 전달한다. WAS는 Java 애플리케이션을 실행하고 세션, Thread, 트랜잭션, DB 연결을 관리한다.

> 사용자 → GSLB → L4 → Apache → Tomcat(WAS) → 업무 WAR

| **구성요소** | **주요 책임** | **개발자가 확인할 것** |
| --- | --- | --- |
| Apache | SSL·Reverse Proxy·Routing·접속 로그 | Context Path와 전달 Header |
| Tomcat | HTTP Thread·WAR 실행·ClassLoader | Connector, Heap, 배포 상태 |
| Spring Boot | Bean·설정·트랜잭션·애플리케이션 실행 | Profile, 의존성, 기동 로그 |

## 3.3 애플리케이션 서버와 DB

애플리케이션 서버는 업무를 판단하고 DB는 데이터를 저장·검색한다. 두 영역 사이에는 HikariCP Connection Pool과 MyBatis Mapper가 있다. Connection은 제한된 자원이므로 요청마다 무제한으로 만들 수 없다.

> Handler → Facade → Service → Rule → DAO → Mapper → HikariCP → DB

-   Facade는 거래의 트랜잭션 경계를 관리한다.
-   DAO는 Mapper 호출과 DB 예외 변환을 담당한다.
-   Mapper XML은 SQL과 결과 매핑을 담당한다.
-   DB Pool 부족은 WAS Thread 대기와 전체 응답 지연으로 이어질 수 있다.

## 3.4 외부 시스템과 인터페이스

한 업무 WAR가 모든 데이터를 소유하지 않는다. 다른 업무나 외부 솔루션의 정보가 필요하면 표준 인터페이스를 통해 호출한다. 직접 테이블 접근은 빠르게 보이지만 소유권과 변경 책임을 무너뜨린다.

| **방식** | **적합한 경우** | **주의사항** |
| --- | --- | --- |
| TCF ServiceId 호출 | 다른 업무 WAR의 온라인 기능 | Timeout·GUID·오류 전파 |
| 외부 REST Client | 외부 시스템 API | 인증·재시도·Circuit 정책 |
| 파일·배치 | 대량·비실시간 교환 | 재처리·정합성·완료 통지 |
| 이벤트 | 비동기 상태 전달 | 중복·순서·멱등성 |

## 3.5 운영 시스템과 개발 시스템

로컬·개발·검증·운영 환경은 목적과 데이터, 보안 수준이 다르다. 로컬에서는 빠른 실험이 중요하지만 운영에서는 비밀정보, 접근통제, 이중화, 변경 승인, 모니터링이 우선이다.

| **환경** | **주요 목적** | **금지·주의** |
| --- | --- | --- |
| local | 개발자 단독 실행 | 실운영 비밀값 사용 금지 |
| dev | 기능 통합 | 공유 데이터 훼손 주의 |
| stg | 운영 유사 검증 | 운영 설정과 차이 기록 |
| prd | 실서비스 | 수동 임의 변경 금지 |

## 3.6 전체 요청 흐름

> 화면 → Apache/Gateway → 업무 WAR → OnlineTransactionController
> → STF → Dispatcher → Handler → 6계층 → DB/외부 시스템
> → ETF → 표준 응답 → 화면

장애를 찾을 때도 이 순서를 따른다. 요청이 도착했는지, STF에서 차단됐는지, Handler가 선택됐는지, DB나 외부 호출에서 대기하는지, ETF가 어떤 오류로 변환했는지를 단계적으로 확인한다.

## 장 요약

-   기업 시스템은 화면·인프라·WAS·업무 코드·데이터·운영 계층으로 구성된다.
-   각 계층은 다른 책임과 장애 지점을 가진다.
-   개발과 장애 분석 모두 End-to-End 요청 흐름을 기준으로 한다.

## 근거 자료

-   zman/03-전체아키텍처.md
-   ztcfbook/제01편/02-전체-시스템-구조.md
-   znsight-man/09-업무-WAR-구조.md

**CHAPTER 4**

**웹 애플리케이션의 기본 구조**

> **이번 장에서 해결할 질문** 브라우저와 서버는 어떤 약속으로 요청과 응답을 주고받는가?

## 4.1 브라우저와 서버

브라우저는 URL로 서버를 찾고 HTTP 요청을 보낸다. 서버는 요청을 해석해 상태코드, Header, Body로 응답한다. 화면은 응답 Body의 데이터를 렌더링한다.

TCF에서도 HTTP를 사용하지만 업무 기능은 URL이 아니라 표준 전문 Header의 serviceId로 구분한다.

## 4.2 URL과 HTTP

URL은 프로토콜, 호스트, 포트, 경로로 구성된다. http://localhost:8086/sv/online에서 8086은 로컬 SV 포트, /sv는 Context Path, /online은 공통 거래 진입점이다.

> http://localhost:8086/sv/online
> └─ host ─┘ └port┘└context┘└endpoint┘

## 4.3 GET과 POST

GET은 조회 의미와 캐시 가능성을 갖고, POST는 요청 Body를 전달하는 데 적합하다. NSIGHT 표준 온라인 거래는 조회·등록 여부와 관계없이 POST /{businessCode}/online을 사용하고 processingType과 serviceId로 의미를 구분한다.

## 4.4 Header와 Body

HTTP Header는 전송 메타정보를 담고, TCF StandardRequest의 header는 업무 거래 메타정보를 담는다. 두 Header는 이름이 같아 보여도 계층이 다르다.

| **구분** | **예** | **역할** |
| --- | --- | --- |
| HTTP Header | Content-Type, Authorization | 전송·인증 메타정보 |
| TCF header | serviceId, transactionCode | 거래 식별·통제 정보 |
| TCF body | customerNo | 업무 입력 데이터 |

## 4.5 JSON 요청과 응답

JSON은 객체와 배열로 데이터를 표현한다. TCF 요청은 header와 body, 응답은 header·result·body의 안정된 구조를 사용한다.

> {
> "header": {"businessCode":"SV", "serviceId":"SV.Customer.selectSummary"},
> "body": {"customerNo":"C0000001"}
> }

## 4.6 상태코드와 오류 메시지

HTTP 상태코드는 전송 결과를, TCF result는 업무 거래 결과를 표현한다. HTTP 200이더라도 result가 실패일 수 있으므로 화면은 두 수준을 함께 확인해야 한다.

> **자주 하는 실수** HTTP 상태코드만 보고 거래 성공으로 판단하거나, HTTP Header와 StandardRequest.header를 같은 것으로 생각하지 않는다.

## 장 요약

-   브라우저와 서버는 HTTP로 통신한다.
-   TCF는 공통 POST Endpoint와 JSON serviceId로 업무를 구분한다.
-   전송 결과와 업무 결과는 서로 다른 수준에서 확인한다.

## 근거 자료

-   ztcfbook-m/제01편/01-TCF가-뭐예요.md
-   ztcfbook/제01편/01-NSIGHT-TCF란-무엇인가.md

**CHAPTER 5**

**Java와 Spring Boot 이해하기**

> **이번 장에서 해결할 질문** TCF 업무 코드는 Java 객체와 Spring Bean으로 어떻게 조립되는가?

## 5.1 Java 클래스와 객체

클래스는 설계도이고 객체는 실행 중 생성된 실체다. Handler·Facade·Service는 각각 클래스로 정의되며 Spring이 객체로 만들어 연결한다.

## 5.2 Interface와 구현 클래스

Interface는 호출 계약을 정의한다. TransactionHandler와 Mapper Interface 덕분에 Dispatcher와 MyBatis는 구체 클래스 내부를 몰라도 동일한 방식으로 호출할 수 있다.

## 5.3 Spring Bean

Spring Bean은 Spring Container가 생성·관리하는 객체다. @Component, @Service, @Repository 같은 Annotation으로 등록되며 기동 시 의존관계가 조립된다.

## 5.4 의존성 주입

의존성 주입은 객체가 필요한 협력자를 직접 new 하지 않고 외부에서 전달받는 방식이다. 생성자 주입을 사용하면 필수 의존성이 명확하고 테스트가 쉬워진다.

> @Service
> class CustomerService {
> private final CustomerDao dao;
> CustomerService(CustomerDao dao) { this.dao = dao; }
> }

## 5.5 Annotation

Annotation은 코드에 의미와 처리 규칙을 부여한다. @Transactional은 트랜잭션 경계, @Mapper는 SQL 매퍼, @Valid는 입력 검증에 사용된다.

## 5.6 Spring Boot 애플리케이션 실행

main 메서드가 SpringApplication을 실행하면 설정을 읽고 Bean을 생성하고 내장 서버를 시작한다. WAR 배포에서는 NsightWarBootstrap과 외부 Tomcat이 기동을 연결한다.

| **Annotation** | **대표 위치** | **의미** |
| --- | --- | --- |
| @Component | Handler·Rule | 일반 Bean |
| @Service | Facade·Service | 업무 서비스 |
| @Repository | DAO | 영속 계층·예외 변환 |
| @Transactional | Facade 메서드 | 트랜잭션 경계 |
| @Mapper | Mapper Interface | MyBatis SQL 연결 |

> **초보자 기준** Annotation 이름을 모두 외우기보다 ‘누가 객체를 만들고, 어느 객체가 어느 객체를 호출하는가’를 먼저 추적한다.

## 장 요약

-   Java 클래스는 책임을 표현하고 Spring은 객체와 의존관계를 관리한다.
-   Interface는 프레임워크와 업무 구현 사이의 계약을 만든다.
-   트랜잭션·검증·SQL 연결은 Annotation과 설정을 통해 적용된다.

## 근거 자료

-   ztcfbook-m/제01편/04-6계층-역할만-기억하기.md
-   znsight-man/73-등록변경-거래-샘플.md

**CHAPTER 6**

**Tomcat과 WAR 이해하기**

> **이번 장에서 해결할 질문** Java 업무 애플리케이션은 어떤 실행 단위로 배포되고 어떤 자원을 공유하는가?

## 6.1 Tomcat의 역할

Tomcat은 HTTP 요청을 받아 Servlet과 Spring 애플리케이션을 실행하는 Web Container다. Connector Thread, 세션 Cookie, WAR 배포, ClassLoader를 관리한다.

## 6.2 JVM과 Tomcat의 관계

JVM은 Java Bytecode를 실행하는 프로세스 환경이고 Tomcat은 JVM 위에서 동작한다. 하나의 Tomcat에 여러 WAR를 배포하면 같은 JVM Heap과 Connector Thread를 공유할 수 있다.

## 6.3 JAR와 WAR의 차이

실행 JAR는 애플리케이션과 내장 서버를 함께 시작하기 좋고, WAR는 외부 Tomcat의 Context로 배포하기 좋다. 로컬 bootRun과 운영 WAR 배포의 목적을 구분한다.

## 6.4 Context Path

Context Path는 한 Tomcat 안에서 애플리케이션을 구분하는 URL 접두사다. sv.war는 /sv, tcf-om은 /om을 사용하며 Header businessCode와 일치해야 한다.

## 6.5 하나의 Tomcat에 여러 WAR 배포

여러 WAR를 같은 8080 포트의 서로 다른 Context로 배포할 수 있다. 배포 단순성은 높지만 JVM 장애와 공유 Thread 고갈의 영향 범위가 커진다.

## 6.6 공유 자원과 장애 영향

Heap·Metaspace·Connector Thread는 Tomcat 수준에서 공유될 수 있고 Hikari Pool과 Spring Bean은 WAR별로 생성될 수 있다. 따라서 한 WAR의 과도한 요청이 다른 WAR에 미치는 영향을 용량과 격리 설계에서 검토한다.

| **자원** | **공유 범위** | **장애 영향** |
| --- | --- | --- |
| JVM Heap | Tomcat 프로세스 | OOM 시 전체 WAR 영향 |
| Metaspace | JVM, WAR별 ClassLoader 사용 | 다중 WAR에서 증가 |
| Connector Thread | Tomcat Connector | 고갈 시 전체 요청 지연 |
| Hikari Pool | 일반적으로 WAR/DataSource별 | 특정 DB 대기 집중 |
| 로그 | 업무별 분리 권장 | GUID로 통합 추적 |

> $CATALINA\_BASE/webapps/
> sv.war → /sv
> ic.war → /ic
> om.war → /om

> **현행과 목표** 문서의 17개 업무 WAR는 목표 구조다. 현행 코드베이스의 업무 WAR 수와 모듈명은 배포 스크립트·settings.gradle·Catalog에서 다시 확인한다.

## 장 요약

-   Tomcat은 JVM 위에서 WAR를 실행한다.
-   Context Path는 WAR의 입구이고 serviceId는 WAR 내부 거래의 식별자다.
-   다중 WAR 배포는 공유 자원과 장애 영향 범위를 함께 고려해야 한다.

## 근거 자료

-   znsight-man/09-업무-WAR-구조.md
-   ztcfbook/제01편/02-전체-시스템-구조.md
-   znsight-config-value-word/02\_Tomcat\_DeltaManager.docx

**CHAPTER 7**

**프레임워크가 필요한 이유**

> **이번 장에서 해결할 질문** 여러 팀이 안전하고 일관되게 거래를 개발하려면 무엇을 공통화해야 하는가?

## 7.1 개발자가 모든 기능을 직접 만들면 생기는 문제

각 개발자가 인증, 오류, 로그, Timeout을 다르게 구현하면 같은 시스템 안에서도 거래마다 품질과 운영 방식이 달라진다. 누락은 보안 사고와 장애 분석 실패로 이어진다.

## 7.2 공통 기능과 업무 기능

Header 검증·권한·거래통제·Timeout·로그·표준 응답은 공통 기능이고, 고객 조회·캠페인 등록 같은 도메인 판단은 업무 기능이다.

## 7.3 표준화와 재사용

공통 전문과 실행 파이프라인을 사용하면 화면과 업무가 달라도 같은 방식으로 호출·추적·통제할 수 있다. 개발자는 이미 검증된 공통 기능을 재사용한다.

## 7.4 TCF가 통제하는 기능

TCF는 STF 전처리, serviceId Dispatcher, Timeout 실행, ETF 후처리를 통해 요청 검증부터 오류 응답과 거래로그까지 일관되게 처리한다.

## 7.5 업무 개발자가 담당하는 기능

업무 개발자는 serviceId 계약과 Handler 아래 6계층, 업무 Rule, SQL, 테스트를 책임진다. OM 등록과 운영 확인도 거래 완료의 일부다.

## 7.6 프레임워크를 올바르게 사용하는 방법

표준 진입점을 우회하지 않고 Handler를 얇게 유지하며, 예외를 ETF까지 전파하고, 설정과 Catalog를 코드와 함께 변경한다.

| **영역** | **TCF 책임** | **업무 개발자 책임** |
| --- | --- | --- |
| 진입 | 표준 Endpoint·Header 검증 | serviceId·요청 Body 설계 |
| 보안 | 인증 문맥·공통 권한 | 데이터 범위·업무 권한 |
| 실행 | Dispatcher·Timeout | Handler와 6계층 |
| 오류 | 표준 코드 매핑·응답 | BusinessException 발생 |
| 운영 | 거래로그·통제 정책 연계 | Catalog 등록·검증 |

> **경계 원칙** 공통 파이프라인은 업무 코드를 알지 않고, 업무 코드는 공통 파이프라인을 우회하지 않는다.

## 직접 해보기

고객 조회 거래에서 필요한 기능을 ‘공통’과 ‘업무’ 두 열로 나누어 적어 본다. 팀마다 구현하면 안 되는 기능과 도메인마다 달라야 하는 기능을 구분할 수 있으면 프레임워크의 존재 이유를 이해한 것이다.

## 확인 문제

1.  업무별 Controller를 새로 만들면 어떤 공통 처리를 우회할 수 있는가?
2.  Handler에서 SQL을 직접 호출하면 어떤 계층 책임이 무너지는가?
3.  Timeout을 코드에만 고정하면 운영 변경이 어려운 이유는 무엇인가?

## 장 요약

-   프레임워크는 공통 품질과 운영 규칙을 강제한다.
-   TCF와 업무 코드의 책임 경계를 지키는 것이 핵심이다.
-   표준 사용에는 코드뿐 아니라 Catalog·정책·테스트·운영 확인이 포함된다.

## 근거 자료

-   ztcfbook-m/제01편/01-TCF가-뭐예요.md
-   zman/05-TCF처리구조.md
-   ztcfbook/제01편/01-NSIGHT-TCF란-무엇인가.md

# 제2부. NSIGHT TCF 전체 구조 이해

제2부는 개별 기술을 넘어 NSIGHT의 업무·모듈·거래·운영 구조를 하나의 지도 위에 올린다. 이 지도를 이해하면 이후 개발환경과 프로그램 계층을 배울 때 각 요소의 위치를 잃지 않는다.

**CHAPTER 8**

**NSIGHT 프로젝트란 무엇인가**

> **이번 장에서 해결할 질문** NSIGHT는 어떤 업무를 어떤 모듈과 팀 경계로 나누어 처리하는가?

## 8.1 NSIGHT의 업무 목적

NSIGHT는 고객·상품·마케팅 정보를 통합해 현장과 채널이 고객을 이해하고 적절한 업무를 수행하도록 지원하는 정보계 플랫폼이다. 단순 조회 시스템이 아니라 고객 분석, 캠페인, 이벤트, 메시지, 운영 통제를 연결한다.

## 8.2 마케팅·고객·상품·캠페인 업무

업무는 고객 통합, Single View, 상품, 캠페인, 이벤트 처리, 세그먼트, 메시지처럼 서로 다른 도메인으로 나뉜다. 각 도메인은 데이터와 프로그램의 책임 경계를 가진다.

## 8.3 정보계와 온라인 거래

정보계라도 사용자의 화면 요청에 즉시 응답하는 기능은 온라인 거래다. 분석 데이터의 대량 처리와 화면의 짧은 응답시간 요구를 같은 방식으로 다루면 자원 경합이 생기므로 온라인·배치·분석 경계를 구분한다.

## 8.4 업무코드와 업무영역

업무코드는 Context Path, WAR, 패키지, ServiceId Prefix, Mapper 경로를 연결하는 공통 식별자다. SV라면 /sv, sv.war, SV.Customer.selectSummary처럼 같은 축을 유지한다.

> SV → /sv → sv.war → com.nh.nsight.marketing.sv
> → SV.{Domain}.{action} → mapper/sv/

## 8.5 17개 업무 WAR 구조

설계 목표는 17개 업무 WAR로 책임을 세분화하는 것이다. 현행 코드베이스는 ic, pc, ms, sv, pd, eb, ep, ss, mg의 9개 업무 WAR와 tcf-om을 중심으로 구현되어 있으므로 목표와 현행을 구분한다.

## 8.6 플랫폼팀과 업무팀의 책임

플랫폼팀은 TCF 공통 계약과 실행 품질을, 업무팀은 도메인 로직과 데이터 품질을 책임진다. 신규 거래는 양쪽이 Catalog·Timeout·오류·배포 기준을 함께 검토해야 운영 가능해진다.

| **구분** | **플랫폼팀** | **업무팀** |
| --- | --- | --- |
| 표준 전문 | 구조·검증 규칙 | 업무 Body 설계 |
| 실행 | STF·Dispatcher·ETF | Handler와 6계층 |
| 데이터 | 공통 연결·추적 기준 | 테이블·SQL·업무 정합성 |
| 운영 | Catalog·통제·Timeout 체계 | 정책값 등록·운영 확인 |
| 품질 | 공통 테스트·Gate | 거래·도메인 테스트 |

> **현행 확인** 업무 WAR 수, Handler 수, 포트와 Context는 문서의 목표값만 믿지 말고 settings.gradle, 배포 스크립트, Catalog와 대조한다.

## 장 요약

-   NSIGHT는 고객·마케팅 업무를 여러 도메인과 WAR로 분리한다.
-   업무코드는 배포·소스·거래 식별을 연결한다.
-   목표 구조와 현행 구현을 구분하고 플랫폼팀과 업무팀의 책임을 맞춘다.

## 근거 자료

-   zman/04-모듈구성.md
-   ztcfbook/제01편/02-전체-시스템-구조.md
-   znsight-man/09-업무-WAR-구조.md

**CHAPTER 9**

**NSIGHT 전체 아키텍처**

> **이번 장에서 해결할 질문** 사용자 요청과 운영 정보는 어떤 계층과 모듈을 거쳐 흐르는가?

## 9.1 사용자와 UI

WebTopSuite와 테스트 UI는 표준 JSON 전문을 만들어 서버에 보낸다. UI는 업무 로직을 소유하지 않고 입력·출력과 사용자 경험을 담당한다.

## 9.2 GSLB·L4·Apache

GSLB는 센터·사이트 선택, L4는 서버 부하분산, Apache는 SSL·Reverse Proxy·URL Routing·접속 로그를 담당한다. 이 계층은 애플리케이션 거래 규칙과 구분된다.

## 9.3 Gateway

tcf-gateway는 businessCode를 기준으로 업무 WAR에 Relay하고 JWT를 1차 검증할 수 있다. Gateway가 있어도 업무 WAR의 STF 최종 검증은 생략하지 않는다.

## 9.4 업무 WAR

업무 WAR는 Context Path로 요청을 받고 tcf-web과 tcf-core를 포함해 동일한 TCF 파이프라인을 실행한다. 각 WAR 안에는 도메인별 Handler와 6계층이 있다.

## 9.5 TCF 플랫폼 모듈

tcf-util은 순수 유틸, tcf-core는 STF·Dispatcher·ETF, tcf-web은 HTTP 진입과 AutoConfiguration을 제공한다. 업무 WAR가 플랫폼 모듈을 참조하며 역방향 의존은 금지한다.

## 9.6 DB·파일·외부 시스템

RDW·ADW·SESSIONDB·LOGDB·OMDB는 서로 다른 목적을 가진다. 파일과 외부 시스템은 별도 Client·전송·재처리 정책으로 연결한다.

## 9.7 OM 운영관리

tcf-om은 Service Catalog, 거래통제, Timeout, 오류코드, 권한, 거래로그와 감사로그를 관리하는 운영 기준정보 원장이다.

> 사용자/UI → GSLB → L4 → Apache → \[Gateway\]
> → 업무 WAR → tcf-web → tcf-core → Handler 6계층
> → RDW/ADW·외부 시스템
> ↘ OMDB·LOGDB·SESSIONDB

| **계층** | **대표 구성요소** | **핵심 책임** |
| --- | --- | --- |
| 채널 | WebTop·tcf-ui·tcf-uj | 표준 요청 생성 |
| 인프라 | GSLB·L4·Apache | 분산·SSL·Proxy |
| 관문 | tcf-gateway | Relay·1차 검증 |
| 업무 | 9개 업무 WAR | 도메인 거래 |
| 플랫폼 | tcf-core·web·util | 공통 실행 통제 |
| 운영 | tcf-om·batch | 정책·로그·관측 |

> **아키텍처 판단** Gateway는 인프라 L4를 대체하지 않고, OM은 업무 데이터의 원장이 아니라 운영 기준정보의 원장이다.

## 장 요약

-   NSIGHT는 채널·인프라·Gateway·업무 WAR·TCF·데이터·OM의 다층 구조다.
-   각 계층은 대체 관계가 아니라 서로 다른 책임을 가진다.
-   의존 방향과 데이터 소유권을 지켜 장애와 변경 영향을 통제한다.

## 근거 자료

-   zman/03-전체아키텍처.md
-   zman/04-모듈구성.md
-   ztcfbook/제01편/02-전체-시스템-구조.md

**CHAPTER 10**

**TCF 실행 흐름 한눈에 보기**

> **이번 장에서 해결할 질문** 표준 요청 한 건은 TCF 안에서 어떤 순서로 검사·실행·기록되는가?

> 화면 이벤트 → OnlineTransactionController → TCF.process()
> → STF → TimeoutExecutor → Dispatcher → Handler → 6계층 → ETF
> → StandardResponse

## 10.1 요청 진입

클라이언트가 POST /{businessCode}/online으로 StandardRequest를 보낸다. OnlineTransactionController는 HTTP 요청을 Java 객체로 변환하고 businessCode·Client 정보를 보정한 뒤 TCF.process()에 위임한다.

## 10.2 공통 전처리

STF는 Header 필수값, Path와 businessCode, GUID·TraceId, 세션, 인증, 권한, 거래통제, Timeout, 멱등성을 순서대로 확인하고 거래로그를 PROCESSING으로 시작한다.

## 10.3 ServiceId 분기

Dispatcher는 request.header.serviceId로 Spring Registry의 Handler를 찾는다. Catalog는 거래가 운영 등록되었는지를, Registry는 어느 Bean이 실행하는지를 판단한다.

## 10.4 업무 처리

Handler는 요청 Body와 TransactionContext를 Facade에 전달한다. Facade의 트랜잭션 안에서 Service·Rule·DAO·Mapper가 업무를 처리한다.

## 10.5 DB 처리

DAO가 Mapper를 호출하고 MyBatis가 SQL을 실행한다. Query Timeout과 Connection Pool 대기는 전체 거래 Timeout보다 안쪽에서 통제되어야 한다.

## 10.6 공통 후처리

ETF는 성공·업무 오류·시스템 오류·Timeout을 StandardResponse로 변환하고 거래로그 상태와 경과시간을 갱신한다. 마지막에는 ContextHolder와 MDC를 정리한다.

| **단계** | **성공 시** | **실패 시** |
| --- | --- | --- |
| STF | Context·정책 생성 | Header·권한·통제 오류 |
| Dispatcher | Handler 선택 | 미등록·중복 문제 |
| 업무 6계층 | 응답 Body 반환 | BusinessException·DB 오류 |
| TimeoutExecutor | 시간 내 완료 | TIMEOUT·결과 UNKNOWN 가능 |
| ETF | SUCCESS 로그·응답 | FAIL/TIMEOUT 로그·표준 오류 |

> **중요** 등록·외부 연계 거래가 Timeout이면 실제 반영 여부가 불명확할 수 있다. 이 경우 실패로 단정하지 않고 UNKNOWN 상태와 확인 절차를 사용한다.

## 확인 문제

1.  STF가 Handler보다 먼저 실행되어야 하는 이유는 무엇인가?
2.  Catalog에는 있지만 Registry에 Handler가 없으면 어떤 상태인가?
3.  ETF가 Handler 대신 응답을 조립하는 장점은 무엇인가?

## 장 요약

-   모든 온라인 거래는 TCF.process()의 동일한 순서를 따른다.
-   STF는 공통 전처리, Dispatcher는 실행 연결, ETF는 공통 후처리를 담당한다.
-   업무 코드는 Handler 아래에서 실행되며 공통 Context와 예외 계약을 지킨다.

## 근거 자료

-   zman/05-TCF처리구조.md
-   zman/06-표준전문구조.md
-   ztcfbook-m/제01편/03-요청이-지나가는-길.md

**CHAPTER 11**

**ServiceId와 거래코드**

> **이번 장에서 해결할 질문** 한 거래를 실행하고 운영에서 추적하려면 어떤 식별자가 필요한가?

## 11.1 왜 거래를 식별해야 하는가

사용자가 같은 화면에서 여러 기능을 실행하고 같은 업무 WAR에 수백 개 거래가 있어도 서버는 정확한 Handler를 선택해야 한다. 운영자는 로그와 정책에서 같은 거래를 찾아야 한다.

## 11.2 ServiceId의 역할

ServiceId는 실행할 기능을 선택하는 Key다. {업무코드}.{도메인}.{행위} 형식을 사용하고 Handler의 serviceIds()와 OM Catalog에 동일하게 등록한다.

## 11.3 거래코드의 역할

거래코드는 통계·감사·재처리와 업무 분류를 위한 식별자다. 예를 들어 SV-INQ-0001은 SV 조회 거래임을 표현한다.

## 11.4 업무코드와의 차이

업무코드는 WAR와 도메인 경계를, ServiceId는 실행 기능을, 거래코드는 운영 분류와 추적을 표현한다. 서로 대체하지 않는다.

## 11.5 화면 ID와의 관계

화면 ID와 이벤트 ID는 사용자 접점, ServiceId는 서버 기능이다. 여러 화면이 같은 ServiceId를 호출할 수 있고 한 화면이 여러 ServiceId를 호출할 수 있다.

## 11.6 정상·금지 예시

이름은 구현 클래스가 아니라 업무 의미를 드러내야 한다. 행위어와 대소문자, Prefix를 표준화하고 임시·모호한 이름을 금지한다.

| **식별자** | **질문** | **예** |
| --- | --- | --- |
| businessCode | 어느 업무 WAR인가? | SV |
| serviceId | 어떤 기능을 실행하는가? | SV.Customer.selectSummary |
| transactionCode | 어떤 종류의 거래로 기록하는가? | SV-INQ-0001 |
| screenId | 어느 화면에서 시작했는가? | SV-CUST-001 |
| GUID/TraceId | 이 실행 한 건은 무엇인가? | 요청마다 생성 |

| **구분** | **예** | **판단** |
| --- | --- | --- |
| 정상 | SV.Customer.selectSummary | 업무·도메인·행위 명확 |
| 금지 | SV.Customer.doIt | 행위가 모호 |
| 금지 | Customer.selectSummary | 업무 Prefix 누락 |
| 금지 | SV\_CUST\_001 | ServiceId 형식 위반 |
| 주의 | SV.Customer.selectV2 | 버전 의미·폐기 정책 필요 |

> **Catalog와 Registry** OM\_SERVICE\_CATALOG는 거래의 운영 존재·정책을 관리하고, Spring Registry는 기동 시 serviceId를 Handler Bean에 연결한다. 둘 다 일치해야 실행된다.

## 장 요약

-   ServiceId는 실행 Key, 거래코드는 운영 분류 Key다.
-   업무코드·화면 ID·GUID는 서로 다른 관점을 식별한다.
-   코드·Catalog·통제·로그에서 동일한 식별자를 사용해야 추적성이 유지된다.

## 근거 자료

-   zman/07-ServiceIdDispatcher.md
-   zman/06-표준전문구조.md
-   ztcfbook/부록 B·C

**CHAPTER 12**

**OM 운영관리란 무엇인가**

> **이번 장에서 해결할 질문** 코드 밖에서 거래의 존재·허용·시간·오류·감사를 어떻게 관리하는가?

## 12.1 Service Catalog

Catalog는 serviceId, 업무코드, Handler 정보, 활성 상태 등 거래의 기본 정의를 보관한다. ‘이 거래가 존재하는가’를 판단하는 운영 원장이다.

## 12.2 거래통제

거래통제는 serviceId·거래코드·업무코드·사용자·채널·지점 등의 조합을 Allow-List로 관리한다. 권한이 있어도 허용 조합이 등록되지 않으면 실행을 차단할 수 있다.

## 12.3 Timeout 정책

Timeout 정책은 serviceId가 몇 초 안에 끝나야 하는지를 관리한다. 거래 허용 여부와 분리하며 Online·DB·SQL·외부 호출 Timeout의 순서를 맞춘다.

## 12.4 오류코드와 공통코드

ETF는 오류코드 기준정보를 이용해 사용자 메시지, 오류유형, 재시도 가능 여부를 표준화한다. 공통코드는 채널·상태·업무 분류의 일관성을 유지한다.

## 12.5 사용자·권한·메뉴

OM은 사용자와 권한그룹, 메뉴·기능·데이터 권한을 연결한다. 화면 메뉴가 보이는 것과 실제 거래·데이터 접근이 허용되는 것은 별도 검증 대상이다.

## 12.6 거래로그와 감사로그

거래로그는 모든 거래의 처리 흔적, 감사로그는 민감한 관리 행위의 증적이다. GUID·TraceId·serviceId로 조회하고 개인정보는 기록하지 않거나 마스킹한다.

## 12.7 운영 기준정보의 중요성

코드가 배포되어도 Catalog·통제·Timeout·오류·권한이 맞지 않으면 거래는 실행되지 않거나 위험하게 동작한다. 기준정보 변경은 승인·이력·Cache Evict·검증 절차를 가져야 한다.

| **운영 영역** | **판단 질문** | **대표 결과** |
| --- | --- | --- |
| Catalog | 거래가 등록·활성인가? | 존재/미등록 |
| 권한 | 사용자가 기능·데이터를 쓸 수 있는가? | 허용/거부 |
| 거래통제 | Header 조합이 허용됐는가? | Allow/Block |
| Timeout | 몇 초 안에 끝나야 하는가? | 완료/TIMEOUT |
| 거래로그 | 이 요청은 어떻게 끝났는가? | SUCCESS/FAIL/TIMEOUT/UNKNOWN |
| 감사로그 | 누가 민감한 변경을 했는가? | Before/After 증적 |

> 신규 serviceId 운영 등록 순서
> Catalog → 거래통제 → Timeout → 오류코드
> → 권한·감사 정책 → Cache 반영 → 호출 검증

> **권한과 거래통제** 권한은 ‘이 사용자가 기능을 쓸 수 있는가’, 거래통제는 ‘이 Header 조합의 거래가 허용 등록됐는가’를 묻는다. 둘은 별도 검증이다.

## 운영 체크리스트

-   신규 serviceId와 Handler Registry가 일치한다.
-   채널·지점별 거래통제 정상·차단 케이스를 시험했다.
-   Query Timeout이 Online Timeout보다 짧다.
-   오류코드 메시지에 내부 정보나 개인정보가 없다.
-   PROCESSING 장기 잔존과 UNKNOWN 거래 확인 절차가 있다.
-   OM 변경 후 Cache가 갱신되고 감사로그가 남는다.

## 장 요약

-   OM은 운영 기준정보의 원장이다.
-   Catalog·권한·거래통제·Timeout은 서로 다른 질문에 답한다.
-   로그와 감사, 변경 이력까지 확인해야 거래가 운영 가능해진다.

## 근거 자료

-   zman/12-OM운영관리.md
-   zman/13-거래통제.md
-   zman/14-Timeout관리.md
-   zman/15-거래로그-감사로그.md

# 제3부. 개발환경 만들기

제3부는 도구 설치 목록이 아니라 동일한 소스를 동일한 방식으로 빌드·실행·검증하기 위한 재현 가능한 개발환경을 만든다.

**CHAPTER 13**

**개발도구 준비**

> **이번 장에서 해결할 질문** TCF 프로젝트를 안전하게 내려받고 빌드·실행·검증하려면 무엇이 필요한가?

## 13.1 JDK 설치

프로젝트가 요구하는 Java 버전과 IDE가 사용하는 JDK를 일치시킨다. java -version과 Gradle JVM을 함께 확인하며 여러 JDK가 설치된 PC에서는 JAVA\_HOME과 PATH의 우선순위를 기록한다.

## 13.2 Eclipse 또는 IDE 설정

IDE는 소스 편집기만이 아니라 Gradle 모델, Annotation 처리, 실행 Profile, 문자 인코딩을 관리한다. 프로젝트 JDK와 Gradle JVM, UTF-8, 줄바꿈 규칙을 팀 기준에 맞춘다.

## 13.3 Git 설치

git --version과 사용자 이름·이메일을 확인한다. 사내 인증서와 Proxy는 보안 정책에 맞게 설정하고 인증정보를 저장소 파일에 기록하지 않는다.

## 13.4 Gradle 확인

Wrapper를 사용하면 팀이 같은 Gradle 버전으로 빌드할 수 있다. 시스템 Gradle보다 gradlew 또는 gradlew.bat을 우선하며 Wrapper 파일은 임의 변경하지 않는다.

## 13.5 DB 접속도구 준비

DB 도구는 SQL 확인과 데이터 검증에 사용하되 애플리케이션 계정의 권한 범위를 지킨다. 운영 DB 직접 수정은 금지하고 조회·DDL·변경 권한을 환경별로 분리한다.

## 13.6 환경 점검 체크리스트

기동 전에 JDK, Git, Wrapper, 포트, DB, Profile, 비밀정보, 로그 경로를 점검한다. 도구 설치 성공보다 프로젝트 빌드와 최소 테스트 성공이 완료 기준이다.

> java -version
> git --version
> gradlew.bat --version
> gradlew.bat projects

| **점검 항목** | **확인 방법** | **정상 기준** |
| --- | --- | --- |
| Java | java -version | 프로젝트 요구 버전 |
| Gradle JVM | IDE Gradle 설정 | Java 버전과 일치 |
| Encoding | IDE·Git 설정 | UTF-8 |
| 포트 | 실행 전 점유 확인 | 담당 모듈 포트 사용 가능 |
| DB | 개발 계정 접속 | 최소 권한·Schema 확인 |
| 비밀정보 | 환경변수·외부 파일 | Git 미포함 |

> **완료 기준** 도구를 설치했다는 사실이 아니라 프로젝트 목록 조회, 컴파일, 단위테스트, 담당 모듈 기동까지 성공해야 한다.

## 장 요약

-   JDK·IDE·Gradle JVM 버전을 일치시킨다.
-   Gradle Wrapper와 UTF-8을 팀 표준으로 사용한다.
-   비밀정보와 운영 DB 접근은 개발환경에서 분리한다.

## 근거 자료

-   znsight-man/06-로컬-개발환경-구성.md
-   znsight-man/63-로컬-빌드-방법.md

**CHAPTER 14**

**Git 저장소와 소스 내려받기**

> **이번 장에서 해결할 질문** 여러 개발자가 같은 코드베이스를 변경할 때 이력을 안전하게 관리하는 방법은 무엇인가?

## 14.1 Git 저장소의 개념

Git 저장소는 파일의 현재 상태뿐 아니라 변경 이력과 분기 구조를 보관한다. 작업 디렉터리, Staging Area, Commit, 원격 저장소의 차이를 이해한다.

## 14.2 Clone과 Pull

Clone은 원격 저장소를 처음 복제하고 Pull은 원격 변경을 현재 Branch에 반영한다. 작업 중 Pull하기 전에 미커밋 변경과 충돌 가능성을 확인한다.

## 14.3 Branch 이해

Branch는 목적이 다른 변경 흐름을 분리한다. 기능·오류수정·릴리즈 Branch 규칙을 따르고 장기간 분리된 Branch는 작은 단위로 자주 동기화한다.

## 14.4 Commit과 Push

Commit은 의미 있는 변경 단위를 설명하고 Push는 원격에 공유한다. 생성물·비밀값·개인 IDE 설정이 포함되지 않았는지 diff를 검토한다.

## 14.5 Merge와 Pull Request

Pull Request는 코드 병합 전 변경 목적, 영향 범위, 테스트 결과, 설정·DB·Catalog 변경을 검토하는 품질 관문이다.

## 14.6 초보자가 자주 하는 Git 실수

다른 사람의 변경을 덮거나, 큰 Commit에 여러 목적을 섞거나, 비밀정보를 올리거나, 충돌 내용을 이해하지 않고 표시만 삭제하는 실수를 피한다.

> git status
> git diff
> git add <검토한 파일>
> git commit -m "SV 고객요약 조회 Rule 추가"
> git push

| **실수** | **위험** | **예방** |
| --- | --- | --- |
| 전체 파일 무조건 add | 생성물·비밀값 포함 | status와 diff 확인 |
| 큰 Commit | 리뷰·복구 어려움 | 목적별 분리 |
| Pull 전 변경 방치 | 충돌 확대 | 작업 상태 정리 |
| 충돌 표시만 제거 | 코드 의미 손상 | 양쪽 의도 확인 |
| 강제 Push | 공유 이력 유실 | 보호 Branch·PR 사용 |

> **PR 체크** 코드 외에도 application.yml, Mapper XML, DB DDL, OM Catalog, 거래통제, Timeout 정책의 동반 변경 여부를 확인한다.

## 장 요약

-   Git은 협업 이력과 변경 책임을 관리한다.
-   Commit 전에 status·diff·테스트를 확인한다.
-   PR은 코드와 운영 설정의 정합성을 함께 검토한다.

## 근거 자료

-   znsight-man/07-Git-브랜치-기준.md
-   ztcf-개발북 제17부 개발환경 문서

**CHAPTER 15**

**Gradle 멀티모듈 구조**

> **이번 장에서 해결할 질문** 공통 플랫폼과 여러 업무 WAR를 하나의 빌드에서 어떻게 분리·연결하는가?

## 15.1 Root Project

Root Project는 settings.gradle의 모듈 목록, 공통 Plugin·Repository·버전 정책, 전체 빌드 진입점을 관리한다. 자체 업무 기능을 넣지 않는다.

## 15.2 플랫폼 모듈

tcf-util→tcf-core→tcf-web의 단방향 기반 위에 cache·eai와 OM·Gateway·JWT가 놓인다. 공통 모듈은 업무 WAR를 참조하지 않는다.

## 15.3 업무 WAR 모듈

ic·pc·ms·sv·pd·eb·ep·ss·mg 같은 업무 모듈은 tcf-web과 필요한 플랫폼 모듈을 의존하고 bootWar로 배포 산출물을 만든다.

## 15.4 모듈 의존관계

업무 WAR끼리 Java Project 의존으로 연결하지 않고 tcf-eai와 표준 전문으로 호출한다. 순환·역방향 의존은 빌드와 배포 독립성을 무너뜨린다.

## 15.5 Build와 Test

전체 build는 공통 변경의 회귀를 확인하고 모듈 build는 빠른 피드백을 제공한다. CI는 변경 영향에 따라 두 범위를 조합한다.

## 15.6 WAR 생성

bootWar 결과의 파일명·Context·포함 Library를 확인한다. 성공 로그만 보지 말고 산출물 위치와 크기, 중복 Library, Profile 외부화를 검토한다.

> tcf-util → tcf-core → tcf-web → 업무 WAR
> ├→ tcf-om
> ├→ tcf-gateway
> └→ tcf-jwt

> gradlew.bat projects
> gradlew.bat :sv-service:test
> gradlew.bat :sv-service:build
> gradlew.bat :sv-service:bootWar

| **변경** | **최소 검증** | **확대 검증** |
| --- | --- | --- |
| 업무 Rule | 해당 모듈 Test | 거래 통합Test |
| Mapper XML | Mapper·DAO Test | DB 통합Test |
| tcf-core | 공통 모듈 Test | 전체 업무 WAR 회귀 |
| 공통 설정 | 관련 Profile 기동 | ztomcat 통합 |
| WAR Plugin | bootWar 산출물 | 외부 Tomcat 배포 |

## 장 요약

-   Root는 모듈과 공통 빌드 정책을 관리한다.
-   플랫폼→업무의 단방향 의존을 지킨다.
-   변경 영향에 맞춰 모듈·전체·통합 빌드를 수행한다.

## 근거 자료

-   znsight-man/08-Gradle-멀티모듈.md
-   zman/04-모듈구성.md

**CHAPTER 16**

**프로젝트를 IDE에서 실행하기**

> **이번 장에서 해결할 질문** 소스를 가져온 뒤 담당 모듈을 올바른 Profile로 기동하고 오류를 좁히는 순서는 무엇인가?

## 16.1 프로젝트 Import

Gradle Project로 Import하고 Root settings.gradle을 기준으로 모든 모듈을 인식시킨다. 일반 Java Project로 가져오면 의존성과 생성 Task가 누락될 수 있다.

## 16.2 Gradle Refresh

build.gradle이나 settings.gradle 변경 후 Refresh해 IDE 모델을 갱신한다. 무작정 Cache를 삭제하기 전에 실제 Gradle 명령의 성공 여부를 확인한다.

## 16.3 Spring Boot 실행

담당 Application 또는 bootRun으로 기동하고 Active Profile, 포트, Context Path, Datasource, Mapper Scan, Handler 등록 로그를 확인한다.

## 16.4 외부 Tomcat 실행

WAR를 webapps 또는 Context 설정으로 배포한 뒤 기동 로그와 배포 완료를 확인한다. bootRun 성공이 외부 Tomcat 성공을 보장하지 않는다.

## 16.5 Profile 변경

local·dev·stg·prd는 설정 목적이 다르다. Profile은 실행 인자나 환경변수로 선택하고 운영 Profile을 개발 PC 기본값으로 두지 않는다.

## 16.6 실행 오류 해결

오류 메시지의 첫 원인을 기준으로 JDK→Gradle→Port→Config→DB→Mapper→Bean→Handler 순서로 좁힌다. 마지막 Exception만 보지 말고 caused by 체인을 읽는다.

| **증상** | **첫 확인** | **대표 원인** |
| --- | --- | --- |
| 기동 전 실패 | JDK·Gradle | 버전·의존성 |
| Port already in use | 포트 점유 | 기존 프로세스 |
| Datasource 오류 | URL·계정·Driver | Profile·비밀값 |
| Mapper 없음 | Scan·XML 경로 | Namespace·Resource |
| 중복 serviceId | Handler Registry | serviceIds 중복 |
| 404 | 포트·Context | /sv 누락 |

> **디버깅 원칙** 한 번에 여러 설정을 바꾸지 말고, 한 가설을 한 변경으로 검증한 뒤 결과를 기록한다.

## 장 요약

-   Gradle 모델과 IDE 모델을 일치시킨다.
-   bootRun과 외부 Tomcat을 별도 검증한다.
-   실행 오류는 계층별 순서로 좁히고 원인 체인을 읽는다.

## 근거 자료

-   znsight-man/06-로컬-개발환경-구성.md
-   znsight-man/10-bootRun-Tomcat-WAR-차이.md

**CHAPTER 17**

**application.yml 읽기**

> **이번 장에서 해결할 질문** 애플리케이션의 실행·DB·Pool·MyBatis·로그 정책은 어디에서 어떻게 연결되는가?

## 17.1 Server 설정

server.port와 server.servlet.context-path는 로컬 주소와 WAR 진입점을 결정한다. Context는 업무코드와 일치시키고 운영 Reverse Proxy 경로와 대조한다.

## 17.2 Datasource 설정

Datasource URL·Driver·계정은 어느 DB와 Schema에 연결되는지 결정한다. 실제 비밀번호는 환경변수나 외부 Secret으로 주입한다.

## 17.3 HikariCP 설정

maximumPoolSize는 성능 숫자가 아니라 DB 동시 사용량의 상한이다. connectionTimeout, maxLifetime, validation과 DB 자원 한도를 함께 설계한다.

## 17.4 MyBatis 설정

mapper-locations, type aliases, underscore-to-camel, statement timeout을 관리한다. 업무코드별 Mapper 경로와 Namespace가 코드와 일치해야 한다.

## 17.5 로그 설정

로그 Level·파일·보존·MDC를 환경별로 설정한다. GUID·TraceId·serviceId를 포함하되 비밀번호·Token·개인정보는 기록하지 않는다.

## 17.6 환경별 설정 분리

공통 기본값과 local·dev·stg·prd 차이를 Profile로 분리한다. 같은 키의 Override 순서를 문서화하고 환경마다 파일을 복사해 표류시키지 않는다.

## 17.7 비밀번호와 비밀정보 관리

비밀정보는 Git에 Commit하지 않는다. 환경변수, Secret Manager, 보호된 외부 설정 파일을 사용하고 유출 시 이력 삭제만이 아니라 즉시 폐기·교체한다.

> spring:
> application:
> name: sv-service
> config:
> import: optional:file:/app/nsight/config/application-common.yml
> server:
> servlet:
> context-path: /sv
> mybatis:
> mapper-locations: classpath:/mapper/sv/\*\*/\*.xml

| **설정** | **잘못된 접근** | **올바른 질문** |
| --- | --- | --- |
| Pool | 크게 하면 빠르다 | DB가 허용할 동시성은? |
| Timeout | 길면 안전하다 | 실패 전파를 어디서 끊나? |
| Profile | 파일 복사로 관리 | 공통과 차이를 어떻게 분리하나? |
| 로그 | 모두 DEBUG | 운영 진단에 필요한 최소 정보는? |
| Secret | yml에 암호 저장 | 어디서 안전하게 주입·교체하나? |

> **운영 반영 전** IP·Port·인증서·DB 접속정보를 실제 환경값으로 바꾸고 Apache/Tomcat 설정 검사와 Spring Boot 기동 테스트를 수행한다.

## 확인 문제

1.  Context Path와 businessCode가 다르면 어떤 문제가 생기는가?
2.  Pool 크기와 Timeout을 독립적으로 정하면 왜 위험한가?
3.  운영 비밀번호가 Git에 올라갔다면 파일 삭제만으로 충분한가?

## 장 요약

-   application.yml은 실행·DB·Pool·SQL·로그 정책의 연결점이다.
-   환경별 차이와 Override 순서를 통제한다.
-   비밀정보는 외부 주입하고 유출 시 즉시 교체한다.

## 근거 자료

-   znsight-man/11-application-yml-기준.md
-   znsight-config-info/nsight\_env\_config
-   znsight-config-value-word/03~07 설정 문서

# 제4부. 화면 요청 한 건 따라가기

제4부에서는 고객요약 조회 버튼 한 번을 따라가며 화면 이벤트, 표준 요청, 공통 Controller, STF, Dispatcher, Handler, ETF가 어떻게 한 거래를 완성하는지 살펴본다.

**CHAPTER 18**

**화면 이벤트에서 거래가 시작된다**

> **이번 장에서 해결할 질문** 사용자가 조회 버튼을 누른 순간부터 서버에 표준 요청이 도착할 때까지 어떤 정보가 만들어지는가?

## 18.1 화면 ID

화면 ID는 사용자가 보고 있는 업무 화면을 식별한다. 거래 실행 키인 ServiceId와는 목적이 다르다. 한 화면에서 조회·저장·삭제 등 여러 거래를 호출할 수 있으므로 화면 ID 하나를 ServiceId 하나와 동일시하지 않는다.

## 18.2 이벤트 ID

이벤트 ID는 버튼 클릭, 행 선택, 초기 조회처럼 화면에서 발생한 행동을 구분한다. 화면 로그와 테스트 시나리오를 연결하는 단서이며, 서버 거래 식별자는 아니다.

## 18.3 버튼과 ServiceId

버튼 이벤트는 호출할 ServiceId를 결정한다. 예제의 고객요약 조회 버튼은 SV.Customer.selectSummary를 사용한다. 문자열을 여러 화면에 복사하기보다 화면 거래 정의나 공통 호출 모듈에서 관리해야 오타와 불일치를 줄일 수 있다.

## 18.4 요청 데이터 만들기

화면은 공통 header와 업무 body를 분리해 만든다. header에는 업무코드·ServiceId·거래코드·채널·사용자·지점 정보를, body에는 고객번호 같은 업무 입력만 둔다. 화면의 표시용 값과 서버에 필요한 값을 구분한다.

## 18.5 거래 호출

요청은 일반적으로 POST /sv/online으로 전송한다. URL은 SV 업무 WAR의 입구를 정하고, 실제 실행 대상은 JSON header의 serviceId가 정한다. 전송 중에는 중복 클릭을 막고 변경 거래에는 idempotencyKey 적용을 검토한다.

## 18.6 응답 처리

화면은 HTTP 상태만 보지 않고 표준 result를 먼저 판정한다. 성공이면 body를 화면 모델에 반영하고, 실패이면 표준 오류 메시지를 표시하며 traceId를 고객지원·운영 추적용으로 남긴다.

> 화면: 고객 종합정보
> 이벤트: 고객요약조회 클릭
> URL: POST /sv/online
> ServiceId: SV.Customer.selectSummary
> 거래코드: SV-INQ-0001

| **식별자** | **책임** | **예시** |
| --- | --- | --- |
| 화면 ID | 화면 식별 | SV-CUST-001 |
| 이벤트 ID | 사용자 행동 식별 | searchCustomer |
| ServiceId | 서버 실행 대상 식별 | SV.Customer.selectSummary |
| 거래코드 | 로그·감사·통계 기준 | SV-INQ-0001 |

> **초보자 핵심** URL이 Handler를 직접 고르는 것이 아니다. URL은 업무 WAR까지 안내하고, ServiceId가 Dispatcher에서 Handler를 선택한다.

## 장 요약

-   화면 ID·이벤트 ID·ServiceId·거래코드는 서로 다른 식별자다.
-   요청은 공통 header와 업무 body로 나눈다.
-   응답은 HTTP 상태와 함께 표준 result를 판정한다.

## 근거 자료

-   ztcfbook-m/제03편/09-JSON-요청-응답-만들기.md
-   znsight-man/20-표준-전문-구조.md
-   zman/06-표준전문구조.md

**CHAPTER 19**

**표준 요청 전문**

> **이번 장에서 해결할 질문** 모든 온라인 거래가 공유하는 요청 계약은 어떤 정보로 이루어지는가?

## 19.1 StandardRequest 구조

StandardRequest는 header와 body를 감싼 공통 요청 객체다. 프레임워크는 header를 검증·통제·추적에 사용하고, 업무 계층은 body를 DTO로 변환해 처리한다.

## 19.2 Header와 Body

header에는 실행과 통제를 위한 공통 메타데이터를 둔다. body에는 고객번호·검색조건처럼 해당 거래에만 필요한 값만 둔다. ServiceId나 사용자 ID를 body에 다시 넣으면 값의 우선순위가 모호해진다.

## 19.3 업무코드

businessCode는 WAR와 Context의 업무 경계를 나타낸다. URL의 sv, header의 SV, ServiceId 접두어 SV가 논리적으로 일치해야 한다.

## 19.4 ServiceId

ServiceId는 {업무코드}.{도메인}.{행위} 형식의 실행 키다. Dispatcher Registry와 OM Service Catalog에서 같은 값으로 관리되어야 한다.

## 19.5 거래코드

transactionCode는 거래로그·감사로그·통계·통제에서 사용하는 관리 식별자다. ServiceId와 목적은 다르지만 둘 사이의 매핑이 흔들리면 운영 추적이 어려워진다.

## 19.6 사용자·지점·채널 정보

userId·branchId·channelId는 권한과 데이터 범위를 결정하는 근거다. 클라이언트가 보낸 값을 그대로 신뢰하지 않고 인증 문맥과 재검증한다.

## 19.7 GUID와 TraceId

GUID는 시스템 간 End-to-End 추적, traceId는 내부 호출 흐름 추적에 사용한다. 값이 없으면 서버에서 생성하거나 보정하며 MDC와 거래로그에 일관되게 기록한다.

> {
> "header": {
> "businessCode": "SV",
> "serviceId": "SV.Customer.selectSummary",
> "transactionCode": "SV-INQ-0001",
> "processingType": "INQUIRY",
> "channelId": "WEBTOP",
> "userId": "U123456",
> "branchId": "001234",
> "guid": "G202607040001",
> "traceId": "T202607040001"
> },
> "body": { "customerNo": "CUST00000001" }
> }

| **영역** | **검증 질문** | **실패 영향** |
| --- | --- | --- |
| 경로·업무코드 | URL과 businessCode가 같은가? | 잘못된 WAR 진입 |
| ServiceId | 업무 접두어와 Catalog가 맞는가? | 분기·통제 실패 |
| 거래코드 | 정의된 코드와 행위가 맞는가? | 로그·감사 불일치 |
| 사용자 문맥 | 인증 사용자와 같은가? | 권한 우회 위험 |
| 추적 ID | 전 구간에서 유지되는가? | 장애 추적 단절 |

## 장 요약

-   StandardRequest는 header와 body의 계약이다.
-   업무 데이터와 공통 통제 정보를 섞지 않는다.
-   식별자는 코드·Catalog·로그에서 같은 값으로 이어져야 한다.

## 근거 자료

-   zman/06-표준전문구조.md
-   znsight-man/20-표준-전문-구조.md
-   znsight-man/21-Header-작성-기준.md

**CHAPTER 20**

**Controller와 TCF 진입**

> **이번 장에서 해결할 질문** 공통 Endpoint가 요청을 받아 TCF 실행 문맥으로 넘기는 과정은 어떻게 이루어지는가?

## 20.1 공통 Online Endpoint

온라인 업무 요청은 POST /{businessCode}/online이라는 공통 입구로 들어온다. OnlineTransactionController는 HTTP 요청을 StandardRequest로 받고 path businessCode와 함께 TCF.process()에 전달한다.

## 20.2 업무별 Controller를 만들지 않는 이유

거래마다 Controller를 만들면 인증·검증·통제·Timeout·로그·응답 형식이 분산된다. 공통 Endpoint를 유지하면 업무 개발자는 Handler 이후에 집중하고 플랫폼은 진입 정책을 한 곳에서 통제할 수 있다.

## 20.3 요청 검증

역직렬화 성공만으로 정상 요청은 아니다. header 존재, 필수 필드, URL과 businessCode 일치, ServiceId와 거래코드 접두어, 허용 채널을 차례로 확인한다.

## 20.4 TransactionContext 생성

검증된 header를 TransactionContext로 변환한다. 이후 Handler·Facade·Service는 원본 요청의 임의 문자열보다 검증된 context에서 사용자·지점·ServiceId를 읽는다.

## 20.5 거래로그 시작

STF는 GUID·traceId·ServiceId를 MDC에 넣고 거래 상태를 PROCESSING으로 기록한다. 시작 기록이 있어야 처리 중 장애나 강제 종료도 미완료 거래로 찾아낼 수 있다.

## 20.6 TCF 처리 호출

TCF.process()는 STF, TimeoutExecutor, Dispatcher, Handler, ETF의 실행 순서를 통제한다. Controller는 업무 결과를 조립하거나 예외별 JSON을 직접 만들지 않는다.

> @PostMapping("/{businessCode}/online")
> public StandardResponse<?> online(
> @PathVariable String businessCode,
> @RequestBody StandardRequest<?> request) {
> return tcf.process(businessCode, request);
> }

| **계층** | **해야 할 일** | **하지 말아야 할 일** |
| --- | --- | --- |
| Controller | HTTP 수신·TCF 위임 | 업무별 분기·SQL |
| TCF | 공통 실행 순서 통제 | 도메인 업무 판단 |
| TransactionContext | 검증된 실행 문맥 제공 | 클라이언트 값 무검증 복사 |
| Global Handler | 예외 진입점 정리 | 거래별 응답 임의 구성 |

> **진입 원칙** 새 거래를 추가할 때 /online용 @RestController를 만들지 않는다. ServiceId와 Handler를 등록하는 것이 표준 확장 방식이다.

## 장 요약

-   공통 Controller는 요청을 TCF에 위임한다.
-   TransactionContext에는 검증된 공통 문맥을 담는다.
-   PROCESSING 시작 기록은 장애 추적의 출발점이다.

## 근거 자료

-   zman/05-TCF처리구조.md
-   zman/06-표준전문구조.md
-   znsight-man/22-Online-Endpoint-기준.md

**CHAPTER 21**

**STF 전처리 이해하기**

> **이번 장에서 해결할 질문** 업무 코드가 실행되기 전에 프레임워크가 거래를 안전하게 걸러내는 기준은 무엇인가?

## 21.1 Header 검증

STF는 businessCode·serviceId·transactionCode·processingType·channelId·userId·branchId 등 필수 항목과 식별자 간 일치 여부를 검증한다.

## 21.2 인증 문맥 확인

header의 userId는 신뢰의 원천이 아니다. 세션이나 JWT에서 확인된 인증 주체와 비교해 위조된 사용자 문맥을 차단한다.

## 21.3 권한 확인

사용자·지점·채널을 기준으로 해당 거래와 데이터 범위에 접근할 수 있는지 확인한다. 메뉴 노출 여부만으로 서버 권한 검사를 대신할 수 없다.

## 21.4 거래통제

OM의 Service Catalog와 거래통제 정책에서 등록·활성·허용 상태를 확인한다. 긴급 중지된 거래는 Handler에 도달하기 전에 거절한다.

## 21.5 Timeout 정책

ServiceId별 Timeout 정책을 조회해 전체 실행 제한시간을 정한다. 모든 거래에 같은 값을 적용하지 않고 조회·변경·외부연계의 특성을 반영한다.

## 21.6 중복 요청 방지

등록·변경처럼 반복 실행의 영향이 큰 거래는 idempotencyKey와 처리 이력을 이용해 중복 실행을 차단하거나 기존 결과를 반환한다.

## 21.7 전처리 실패 시 처리

전처리 실패는 Handler를 호출하지 않는다. Header·인증·권한·통제·Timeout 정책 오류를 표준 오류코드로 변환하고 실패 거래로그를 남긴다.

> Header 검증 → 추적 ID/MDC → 인증 → 권한 → 거래통제
> → 중복 요청 확인 → Timeout 정책 → PROCESSING 기록 → Dispatcher

| **검사** | **대표 실패** | **처리 원칙** |
| --- | --- | --- |
| Header | 필수값·접두어 불일치 | E-TCF-HDR 계열 |
| 인증 | 사용자 불일치·만료 | 업무 실행 차단 |
| 권한 | 기능·데이터 범위 없음 | 최소 정보만 응답 |
| 거래통제 | 미등록·중지 | Handler 호출 금지 |
| 중복 | 동일 idempotencyKey | 정책에 따라 차단·재사용 |
| Timeout | 정책 없음·제한 초과 | 기본정책 또는 TIMEOUT |

> **책임 경계** STF는 공통 안전장치다. 고객 등급이나 상품 가입 가능 여부 같은 업무 규칙은 Rule과 Service에서 판단한다.

## 장 요약

-   STF는 업무 실행 전에 공통 정책을 검증한다.
-   클라이언트의 사용자·지점 값은 인증 문맥으로 재검증한다.
-   전처리 실패 시 Handler는 호출되지 않는다.

## 근거 자료

-   zman/05-TCF처리구조.md
-   zman/13-거래통제.md
-   zman/14-Timeout관리.md
-   znsight-man/21-Header-작성-기준.md

**CHAPTER 22**

**Dispatcher와 Handler 찾기**

> **이번 장에서 해결할 질문** 공통 URL로 들어온 여러 거래 중 정확한 업무 Handler를 어떻게 선택하는가?

## 22.1 ServiceId 기반 분기

TransactionDispatcher는 검증된 context의 serviceId로 Registry를 조회한다. URL, 화면 ID, 거래코드로 Handler를 고르지 않는다.

## 22.2 Handler 등록

TransactionHandler 구현체는 Spring Bean으로 등록되고 serviceIds()에서 담당 ServiceId 목록을 반환한다. 기동 시 Registry가 이를 Map<ServiceId, Handler>로 구성한다.

## 22.3 도메인 단위 Handler

고객·상품처럼 도메인 단위로 Handler를 두면 관련 거래를 한곳에서 찾기 쉽다. Handler는 분기와 DTO 변환, Facade 호출까지만 담당한다.

## 22.4 여러 ServiceId 관리

한 Handler가 여러 행위를 담당할 때는 serviceIds() 목록과 switch 분기가 반드시 일치해야 한다. 기본 분기는 SERVICE\_NOT\_FOUND 예외로 방어한다.

## 22.5 Handler를 찾지 못한 경우

Catalog에 등록됐더라도 Registry에 구현 Bean이 없으면 실행할 수 없다. 배포 누락, @Component 누락, 문자열 오타, 모듈 스캔 범위를 확인한다.

## 22.6 중복 ServiceId 문제

같은 ServiceId를 둘 이상의 Handler가 등록하면 어느 구현을 실행할지 결정할 수 없다. 조용히 덮어쓰지 말고 서버 기동을 실패시켜 배포 전에 발견한다.

> @Component
> class SvCustomerHandler implements TransactionHandler {
> public Collection<String> serviceIds() {
> return List.of("SV.Customer.selectSummary");
> }
> public Object doHandle(StandardRequest<?> req, TransactionContext ctx) {
> return facade.selectCustomerSummary(req.getBody(), ctx);
> }
> }

| **등록 상태** | **Catalog** | **Registry** | **결과** |
| --- | --- | --- | --- |
| 정상 | 있음·활성 | Handler 1개 | 실행 |
| 구현 누락 | 있음 | 없음 | SERVICE\_NOT\_FOUND |
| 운영 미등록 | 없음 | 있음 | STF에서 차단 |
| 중복 구현 | 있음 | Handler 2개 이상 | 기동 실패 |

> **Catalog와 Registry** Catalog는 '실행을 허용할 거래인가'를 답하고 Registry는 '어떤 Bean이 실행하는가'를 답한다. 둘 다 정상이어야 거래가 실행된다.

## 장 요약

-   Dispatcher의 실행 키는 ServiceId다.
-   Handler는 Spring Registry와 OM Catalog 양쪽에서 정합해야 한다.
-   중복 ServiceId는 기동 실패로 조기에 차단한다.

## 근거 자료

-   zman/07-ServiceIdDispatcher.md
-   zman/08-업무Handler개발.md
-   ztcfbook-m/제03편/10-Handler-만드는-법.md

**CHAPTER 23**

**ETF와 표준 응답**

> **이번 장에서 해결할 질문** 업무 결과와 예외가 화면이 이해할 수 있는 하나의 응답 계약으로 바뀌는 과정은 무엇인가?

## 23.1 정상 응답

Handler가 반환한 업무 결과를 ETF가 StandardResponse body에 담는다. 요청의 추적 정보를 유지하고 responseTime·elapsedTimeMs와 성공 result를 추가한다.

## 23.2 업무 오류

입력 조건이나 업무 규칙 위반은 BusinessException으로 표현한다. ETF는 오류코드와 사용자 메시지를 표준 result/error 구조로 변환하고 재시도 가능 여부를 정책에 맞게 표시한다.

## 23.3 시스템 오류

예상하지 못한 예외, DB 접속 실패, 구현 오류는 시스템 오류로 분류한다. 화면에는 내부 클래스명·SQL·StackTrace를 노출하지 않고 traceId와 안전한 메시지만 제공한다.

## 23.4 Timeout 응답

TimeoutExecutor가 제한시간을 초과하면 ETF는 TIMEOUT 상태와 표준 오류코드를 만든다. Timeout 응답이 왔다고 DB 변경이 반드시 취소됐다고 단정해서는 안 되므로 멱등성과 최종 상태 확인이 필요하다.

## 23.5 거래로그 종료

ETF는 PROCESSING 거래를 SUCCESS·FAIL·TIMEOUT·UNKNOWN 중 하나로 갱신하고 경과시간·오류코드·결과를 기록한다. 감사 대상 거래는 별도의 감사로그도 남긴다.

## 23.6 화면에서 오류 보여주기

화면은 사용자 메시지와 운영 상세를 구분한다. 사용자가 다음 행동을 결정할 수 있는 짧은 메시지, 재시도 가능 여부, 문의 시 필요한 traceId를 제공한다.

> {
> "header": {
> "serviceId": "SV.Customer.selectSummary",
> "traceId": "T202607040001",
> "elapsedTimeMs": 120
> },
> "result": { "resultCode": "SUCCESS", "message": "정상 처리되었습니다." },
> "body": { "customerNo": "CUST00000001", "customerGrade": "VIP" },
> "error": null
> }

| **결과** | **로그 상태** | **화면 처리** | **주의점** |
| --- | --- | --- | --- |
| 정상 | SUCCESS | body 반영 | 추적 ID 유지 |
| 업무 오류 | FAIL | 업무 메시지 표시 | 민감정보 제외 |
| 시스템 오류 | FAIL | 일반 안내·문의 유도 | 내부 정보 숨김 |
| Timeout | TIMEOUT | 재시도 정책 적용 | 처리 결과 불명 가능 |
| 종료 불명 | UNKNOWN | 상태 조회·운영 확인 | 자동 재처리 주의 |

> **Timeout의 함정** 클라이언트가 응답을 받지 못한 것과 서버의 변경 작업이 실행되지 않은 것은 같은 말이 아니다. 변경 거래는 idempotencyKey와 상태 조회 수단을 함께 설계한다.

## 제4부 실습 점검

1.  Postman 또는 테스트 도구에서 정상 고객요약 요청을 보내고 traceId를 기록한다.
2.  ServiceId를 오타로 바꿔 STF 또는 Dispatcher의 실패 응답을 비교한다.
3.  필수 header를 하나씩 제거해 표준 오류코드와 Handler 미호출을 확인한다.
4.  거래로그에서 PROCESSING이 최종 상태로 갱신되는지 확인한다.

## 장 요약

-   ETF는 업무 결과와 예외를 표준 응답으로 통일한다.
-   내부 오류 정보와 개인정보를 화면 응답에 노출하지 않는다.
-   Timeout·UNKNOWN은 멱등성과 최종 상태 확인이 필요하다.

## 근거 자료

-   zman/05-TCF처리구조.md
-   zman/06-표준전문구조.md
-   zman/15-거래로그-감사로그.md
-   ztcfbook-m/제03편/11-로그-Timeout-실수-방지.md

# 제5부. 업무 프로그램 6계층 개발

제5부에서는 한 거래의 업무 코드를 패키지에 배치하고 Handler에서 Mapper까지 연결한다. 각 계층을 많이 만드는 것이 목적이 아니라 변경 이유와 테스트 경계를 분명히 만드는 것이 목적이다.

**CHAPTER 24**

**패키지와 도메인 설계**

> **이번 장에서 해결할 질문** 새 업무 코드를 어느 패키지와 계층에 배치해야 변경 영향과 책임이 분명해지는가?

## 24.1 BASE 패키지

BASE 패키지는 조직·시스템·업무의 공통 접두어다. 예제에서는 com.nh.nsight.marketing.sv 아래에 고객 도메인을 둔다. 운영 코드에서는 프로젝트가 정한 정확한 BASE를 사용하고 임의의 최상위 패키지를 만들지 않는다.

## 24.2 업무코드 패키지

sv·ic·pc 같은 업무코드는 WAR와 배포 경계를 반영한다. 다른 업무의 구현 클래스를 직접 참조하지 않고 표준 전문과 ServiceId 기반 연계를 사용한다.

## 24.3 도메인 패키지

customer·product·campaign처럼 업무 개념을 기준으로 묶는다. 화면 메뉴나 개발자 이름을 패키지 기준으로 삼으면 업무 변화 때 구조가 쉽게 무너진다.

## 24.4 계층 패키지

entry, application, persistence, client, support로 책임을 분리한다. Handler·Facade·Service·Rule·DAO·Mapper의 의존 방향은 위에서 아래로 유지한다.

## 24.5 클래스 배치 원칙

클래스는 가장 가까운 책임의 패키지에 둔다. JSON 변환은 entry, 유스케이스는 application, SQL 접근은 persistence에 둔다. 공통화는 두 곳 이상에서 안정적으로 반복되는 책임에만 적용한다.

## 24.6 common과 util 남용 방지

common이나 util은 책임을 숨기는 쓰레기통이 되기 쉽다. 도메인 규칙은 Rule, 외부 호출은 client, 형식 변환은 명시적 converter처럼 목적이 드러나는 이름으로 배치한다.

> com.nh.nsight.marketing.sv.customer
> ├─ entry/handler, entry/dto
> ├─ application/facade, service, rule, dto
> ├─ persistence/dao, mapper, dto
> ├─ client
> └─ support

| **패키지** | **대표 클래스** | **허용 의존** |
| --- | --- | --- |
| entry | Handler, Request/Response | application |
| application | Facade, Service, Rule | persistence·client |
| persistence | DAO, Mapper, Result | DB/MyBatis |
| client | 외부 시스템 Client | 표준 연계 모듈 |
| support | 국소 공통 지원 | 상위 계층 비의존 |

> **의존성 원칙** persistence가 entry를 참조하거나 Rule이 Controller를 참조하면 계층 방향이 뒤집힌다. 패키지 이름보다 실제 import 방향을 검사해야 한다.

## 장 요약

-   업무코드와 도메인으로 먼저 경계를 나눈다.
-   계층 패키지는 책임과 의존 방향을 드러낸다.
-   common·util은 명확한 책임이 있을 때만 사용한다.

## 근거 자료

-   znsight-man/09-업무-WAR-구조.md
-   znsight-man/04-개발표준-전체요약.md
-   zman/08-업무Handler개발.md

**CHAPTER 25**

**Handler 개발**

> **이번 장에서 해결할 질문** ServiceId를 안전하게 업무 유스케이스로 연결하는 가장 얇은 진입 어댑터는 어떻게 작성하는가?

## 25.1 Handler의 역할

Handler는 Dispatcher와 업무 계층 사이의 Adapter다. 담당 ServiceId를 선언하고 body를 Request DTO로 변환한 뒤 Facade를 호출해 업무 결과만 반환한다.

## 25.2 ServiceId 등록

serviceIds() 목록은 OM Catalog와 정확히 일치해야 한다. 한 도메인의 여러 행위를 담당할 수 있지만 중복 등록은 서버 기동 단계에서 실패시킨다.

## 25.3 요청 DTO 변환

Map이나 JsonNode를 계층 전체로 전달하지 않는다. Handler 경계에서 타입이 있는 Request DTO로 변환하고 Bean Validation 결과를 표준 오류로 연결한다.

## 25.4 Facade 호출

Handler는 context와 검증된 DTO를 Facade에 넘긴다. 거래별 switch는 명시적으로 작성하고 기본 분기는 SERVICE\_NOT\_FOUND로 방어한다.

## 25.5 Handler에서 하면 안 되는 일

SQL 실행, 트랜잭션 선언, 복잡한 업무 판단, 표준 응답 조립, 사용자 권한 재구현을 금지한다. 이런 코드가 보이면 각각 DAO·Facade·Rule·ETF·STF로 이동한다.

## 25.6 Handler 테스트

ServiceId 분기, DTO 변환 실패, Facade 호출 인자와 반환값, 미지원 ServiceId를 단위테스트한다. DB를 연결하지 않고도 Handler 책임을 검증할 수 있어야 한다.

> @Component
> @RequiredArgsConstructor
> class SvCustomerHandler implements TransactionHandler {
> private final SvCustomerFacade facade;
> public Collection<String> serviceIds() {
> return List.of("SV.Customer.selectSummary");
> }
> public Object doHandle(StandardRequest<Map<String,Object>> req,
> TransactionContext ctx) {
> var dto = converter.toSummaryRequest(req.getBody());
> return facade.selectSummary(dto, ctx);
> }
> }

| **검토 항목** | **정상 기준** | **위반 징후** |
| --- | --- | --- |
| 분기 | ServiceId와 Facade 1:1 연결 | 문자열 오타·누락 |
| 변환 | Body→Request DTO | Map의 하위 계층 전파 |
| 업무 로직 | 없음 | if·loop·계산 증가 |
| 응답 | 업무 결과 반환 | StandardResponse 직접 생성 |
| 테스트 | DB 없이 수행 | 통합환경에서만 검증 |

## 장 요약

-   Handler는 얇은 진입 Adapter다.
-   ServiceId 등록과 분기를 함께 테스트한다.
-   업무 판단·SQL·응답 조립을 Handler에 넣지 않는다.

## 근거 자료

-   znsight-man/23-TransactionHandler-개발.md
-   zman/08-업무Handler개발.md
-   ztcfbook-m/제03편/10-Handler-만드는-법.md

**CHAPTER 26**

**Facade 개발**

> **이번 장에서 해결할 질문** 하나의 거래를 구성하는 여러 업무 서비스를 어디에서 묶고 트랜잭션을 결정하는가?

## 26.1 유스케이스 조립

Facade는 고객요약 조회나 고객메모 변경처럼 사용자가 실행한 유스케이스를 조립한다. 여러 Service 호출 순서와 최종 Response 변환을 관리한다.

## 26.2 트랜잭션 경계

DB 상태를 바꾸는 거래는 Facade 메서드를 트랜잭션 경계로 삼는다. 조회는 readOnly를 검토하고 외부 호출이 포함되면 DB 트랜잭션을 길게 잡지 않도록 순서를 설계한다.

## 26.3 여러 Service 호출

서로 다른 도메인의 Service가 필요하면 명시적으로 순서를 조립한다. 순환 호출이 생기면 도메인 경계나 별도 오케스트레이션 책임을 재검토한다.

## 26.4 결과 조립

각 Service 결과를 화면 Response DTO로 조립한다. 개인정보 마스킹과 코드명 변환의 책임 위치를 프로젝트 기준에 맞춰 일관되게 유지한다.

## 26.5 Facade와 Service의 차이

Facade는 거래 단위의 순서와 경계, Service는 도메인 업무 흐름을 담당한다. Facade가 세부 규칙을 모두 가지거나 Service가 다른 유스케이스를 무분별하게 조립하지 않게 한다.

## 26.6 트랜잭션 Rollback

검사 예외와 업무 예외의 Rollback 정책을 명확히 정한다. 예외를 잡아 성공처럼 반환하면 Rollback이 사라질 수 있으므로 ETF까지 예외를 전달하는 원칙을 지킨다.

> @Transactional
> public SvCustomerMemoResponse updateMemo(Request req, TransactionContext ctx) {
> customerRule.validateMemo(req);
> customerService.assertCustomerExists(req.customerNo());
> var result = memoService.update(req, ctx.getUserId());
> return responseAssembler.toResponse(result);
> }

| **상황** | **트랜잭션 설계** | **주의** |
| --- | --- | --- |
| 단건 조회 | readOnly 검토 | 불필요한 잠금 방지 |
| 등록·변경 | Facade 경계 | 영향 건수 확인 |
| 여러 DB | 분산 트랜잭션 여부 검토 | 부분 성공 정책 |
| 외부 호출 포함 | 호출 순서·보상 설계 | 장시간 DB 점유 |
| 업무 예외 | Rollback 규칙 명시 | 예외 삼키기 금지 |

> **트랜잭션 주의** @Transactional은 모든 문제를 해결하지 않는다. 외부 시스템 성공 후 DB 실패처럼 원자성을 보장할 수 없는 구간은 보상·재처리·상태 관리가 필요하다.

## 장 요약

-   Facade는 유스케이스와 트랜잭션 경계다.
-   Service 호출 순서와 결과 조립을 명시한다.
-   예외와 외부 호출의 부분 성공 정책을 설계한다.

## 근거 자료

-   znsight-man/24-Facade-개발.md
-   znsight-man/03-TCF-개발원칙.md

**CHAPTER 27**

**Service 개발**

> **이번 장에서 해결할 질문** 도메인 업무 흐름을 읽기 쉽고 재사용 가능하게 구현하려면 무엇을 Service에 두어야 하는가?

## 27.1 업무 처리 흐름

Service는 조회·등록·변경·삭제의 도메인 처리 순서를 표현한다. 메서드 이름만 읽어도 검증, 조회, 상태 변경, 저장의 흐름이 드러나야 한다.

## 27.2 Rule과 DAO 호출

계산·판정은 Rule에, 데이터 접근은 DAO에 위임한다. Service는 두 책임을 조합하고 결과가 없거나 영향 건수가 다른 상황을 업무 의미로 해석한다.

## 27.3 상태 변경

변경 전 현재 상태를 확인하고 허용 가능한 전이인지 Rule로 검증한다. 수정자·수정시각·채널 같은 감사정보는 TransactionContext에서 Command에 반영한다.

## 27.4 업무 오류 발생

고객 없음, 이미 삭제됨, 허용되지 않은 상태 같은 예상 가능한 실패는 표준 BusinessException으로 발생시킨다. DB 예외 문자열을 사용자 메시지로 그대로 전달하지 않는다.

## 27.5 다른 도메인 호출

같은 WAR 내부라도 다른 도메인의 DAO를 직접 사용하지 않고 해당 Service 계약을 통해 호출한다. 다른 WAR는 Java 참조 대신 ServiceId 기반 연계를 사용한다.

## 27.6 Service 책임 위반 사례

HTTP 객체, SQL 문자열, 화면 컴포넌트, StandardResponse 조립이 Service에 들어오면 계층이 섞인 것이다. 지나치게 큰 Service는 도메인 책임과 유스케이스 경계를 다시 나눈다.

> public SummaryResult selectSummary(SummaryQuery query, TransactionContext ctx) {
> customerRule.validateReadableBranch(query.branchId(), ctx.getBranchId());
> return customerDao.findSummary(query)
> .orElseThrow(() -> new BusinessException("E-SV-BIZ-0001",
> "고객 정보를 찾을 수 없습니다."));
> }

| **Service 코드** | **판단** |
| --- | --- |
| Rule 호출 후 DAO 조회 | 정상 |
| Mapper 직접 호출 | DAO 경계 위반 |
| ResponseEntity 반환 | HTTP 책임 침범 |
| 다른 WAR Service 직접 import | 배포 경계 위반 |
| 영향 건수 0을 성공 처리 | 업무 의미 누락 |

## 장 요약

-   Service는 도메인 처리 순서를 표현한다.
-   Rule과 DAO를 조합하되 각 책임을 대신하지 않는다.
-   상태 전이·영향 건수·업무 오류를 명확히 처리한다.

## 근거 자료

-   znsight-man/25-Service-개발.md
-   zman/08-업무Handler개발.md

**CHAPTER 28**

**Rule 개발**

> **이번 장에서 해결할 질문** 업무 규칙을 DB와 프레임워크로부터 분리해 빠르게 검증하려면 어떻게 작성하는가?

## 28.1 업무 규칙이란 무엇인가

업무 규칙은 입력과 현재 상태를 바탕으로 허용 여부·등급·계산 결과를 결정한다. 예를 들어 메모 길이, 조회 가능 지점, 상태 전이 가능 여부가 Rule의 대상이다.

## 28.2 입력값 검증과 업무 규칙의 차이

필수값·길이·형식은 DTO Annotation으로 1차 검증하고, 날짜 선후관계·권한 범위·상태 조건은 Rule에서 검증한다. DB 존재 여부는 Service와 DAO가 협력한다.

## 28.3 부작용 없는 Rule

가능하면 같은 입력에 같은 결과를 내는 순수 함수로 작성한다. Rule 안에서 DB·파일·네트워크·현재 시각을 직접 읽지 말고 필요한 값을 인자로 받는다.

## 28.4 권한과 데이터 범위 검증

STF가 기능 권한을 확인하더라도 고객·지점·조직 범위 같은 업무 데이터 권한은 Rule에서 추가 검증할 수 있다. 클라이언트의 branchId가 아니라 검증된 context를 기준으로 삼는다.

## 28.5 Rule에서 금지할 기능

DAO 호출, 트랜잭션 선언, 로그 저장, 응답 DTO 조립, 외부 시스템 호출을 금지한다. 이런 기능은 Rule 테스트를 느리고 불안정하게 만든다.

## 28.6 Rule 단위테스트

경계값, 정상·실패, null, 상태 전이 조합을 표 형태로 테스트한다. Spring Context나 DB 없이 밀리초 단위로 실행되는 것이 이상적이다.

> public void validateMemo(String memo) {
> if (memo == null \|\| memo.isBlank())
> throw new BusinessException("E-SV-VAL-0001", "메모를 입력하세요.");
> if (memo.length() > 500)
> throw new BusinessException("E-SV-VAL-0002", "메모는 500자 이하입니다.");
> }

| **검증** | **입력** | **기대 결과** |
| --- | --- | --- |
| 필수 | null·공백 | E-SV-VAL-0001 |
| 경계 | 500자 | 통과 |
| 초과 | 501자 | E-SV-VAL-0002 |
| 지점 범위 | 다른 지점·권한 없음 | 권한 오류 |
| 상태 전이 | 삭제→활성 | 업무 오류 |

## 장 요약

-   형식 검증과 업무 규칙을 구분한다.
-   Rule은 가능한 한 부작용 없는 순수 함수로 만든다.
-   경계값과 상태 조합을 빠른 단위테스트로 검증한다.

## 근거 자료

-   znsight-man/26-Rule-개발.md
-   znsight-man/19-Validation-작성-기준.md

**CHAPTER 29**

**DAO 개발**

> **이번 장에서 해결할 질문** 업무 계층과 MyBatis 사이에서 데이터 접근 결과와 기술 오류를 어떻게 정돈하는가?

## 29.1 DAO의 역할

DAO는 Service와 Mapper 사이의 데이터 접근 경계다. 적절한 Mapper를 선택하고 DB 결과를 Optional·List·영향 건수처럼 Java 업무 코드가 해석하기 쉬운 형태로 반환한다.

## 29.2 Mapper 호출

DAO는 RDW·ADW·업무 DB 목적에 맞는 Mapper를 호출한다. 여러 Mapper를 조합해야 하면 데이터 접근 관점의 최소 조합만 수행하고 업무 판단은 Service에 남긴다.

## 29.3 조회·등록·수정·삭제

단건 조회는 Optional, 목록은 빈 List, 변경은 영향 건수 반환을 권장한다. 0건 수정이 업무 오류인지 동시성 충돌인지는 Service가 판단한다.

## 29.4 DB 오류 변환

MyBatis·JDBC 예외를 그대로 화면까지 보내지 않는다. 기술 예외는 표준 시스템 오류로 매핑하고 SQL ID·traceId를 내부 로그에 남긴다.

## 29.5 DAO와 Repository

프로젝트가 DAO 명칭을 표준으로 정했다면 혼용하지 않는다. 이름보다 중요한 것은 persistence 경계와 단방향 의존성이다.

## 29.6 DAO에서 금지할 업무 판단

고객 등급 계산, 사용자 메시지 선택, 상태 전이 판단, 화면 Response 생성은 금지한다. DAO 메서드는 어떤 데이터를 읽고 쓰는지가 드러나야 한다.

> @Repository
> @RequiredArgsConstructor
> class SvCustomerDao {
> private final SvCustomerMapper mapper;
> Optional<SvCustomerSummaryResult> findSummary(SvCustomerCriteria criteria) {
> return Optional.ofNullable(mapper.selectCustomerSummary(criteria));
> }
> int updateMemo(SvCustomerMemoCommand command) {
> return mapper.updateCustomerMemo(command);
> }
> }

| **작업** | **권장 반환** | **Service 판단** |
| --- | --- | --- |
| 단건 조회 | Optional<Result> | 없음 처리 |
| 목록 조회 | List<Result> | 빈 목록·페이징 |
| 등록 | int 또는 생성 키 | 1건 여부 |
| 수정·삭제 | 영향 건수 | 0건·동시성 |
| DB 실패 | 기술 예외 | ETF 시스템 오류 |

## 장 요약

-   DAO는 데이터 접근 경계를 제공한다.
-   조회 없음과 변경 영향 건수를 명시적으로 반환한다.
-   업무 판단과 화면 응답 생성을 DAO에 넣지 않는다.

## 근거 자료

-   znsight-man/27-DAO-개발.md
-   znsight-man/28-MyBatis-Mapper-개발.md

**CHAPTER 30**

**MyBatis Mapper 개발**

> **이번 장에서 해결할 질문** Java 계약과 SQL을 일치시키고 운영에서 추적 가능한 쿼리로 만드는 기준은 무엇인가?

## 30.1 Mapper Interface

Mapper Interface는 SQL 실행의 Java 계약이다. 메서드명·파라미터·반환 타입을 명확히 하고 업무 로직이나 SQL 문자열을 Java에 넣지 않는다.

## 30.2 Mapper XML

XML namespace는 Interface FQCN, statement id는 메서드명과 일치시킨다. 파일은 resources/mapper/{업무코드} 아래에 배치한다.

## 30.3 Namespace와 SQL ID

MyBatis statement ID와 운영 추적용 업무 SQL ID를 구분한다. SQL 주석이나 로그 메타에 ServiceId·SQL ID를 남겨 Slow SQL을 거래까지 역추적한다.

## 30.4 Parameter와 Result

Map 대신 Criteria·Command DTO를 사용하고 결과는 전용 Result DTO로 받는다. SELECT \*를 피하고 컬럼·별칭·resultMap을 명시한다.

## 30.5 동적 SQL

선택 조건은 if·where·foreach를 제한적으로 사용한다. 값은 #{}로 바인딩하고 ${}는 검증된 정렬 컬럼처럼 whitelist를 통과한 경우에만 사용한다.

## 30.6 SQL 실행 확인

단건·0건·다건, null, 경계값, Paging, Timeout과 실행계획을 확인한다. RDW에서 Full Scan·대량 집계를 실행하거나 목록 조회를 무제한으로 열지 않는다.

> <select id="selectCustomerSummary"
> parameterType="SvCustomerCriteria"
> resultMap="SvCustomerSummaryResultMap">
> /\* SQL\_ID: SV.Customer.selectSummary \*/
> SELECT C.CUSTOMER\_NO, C.CUSTOMER\_NAME, C.CUSTOMER\_GRADE
> FROM RDW\_CUSTOMER\_SUMMARY C
> WHERE C.CUSTOMER\_NO = #{customerNo}
> </select>

| **검사** | **정상 기준** | **위험** |
| --- | --- | --- |
| namespace | Interface FQCN 일치 | Binding 오류 |
| statement id | 메서드명 일치 | 실행 시 미탐색 |
| 바인딩 | #{} 기본 | ${} Injection |
| 목록 | Paging·최대건수 | Pool·Heap 고갈 |
| DB 선택 | RDW/ADW 목적 일치 | 온라인 지연 |
| 추적 | 업무 SQL ID 기록 | 장애 분석 단절 |

> **SQL 리뷰 핵심** 결과가 나온다는 사실만으로 완료가 아니다. 실행계획, 인덱스 조건, 최대 건수, Query Timeout, 개인정보 컬럼까지 확인한다.

## 장 요약

-   Interface·XML·SQL ID의 이름을 일치시킨다.
-   타입 있는 파라미터와 결과 DTO를 사용한다.
-   Paging·Timeout·실행계획을 운영 기준으로 검증한다.

## 근거 자료

-   znsight-man/28-MyBatis-Mapper-개발.md
-   ztcfbook-m/부록/E-Mapper-XML-기본.md

**CHAPTER 31**

**DTO와 Validation**

> **이번 장에서 해결할 질문** 화면·업무·DB 계약을 안전하게 분리하고 검증 책임을 단계별로 배치하려면 어떻게 하는가?

## 31.1 Request DTO

화면이나 API body의 입력 계약이다. @NotBlank·@Size·@Min 같은 Annotation으로 필수·길이·형식을 검증하며 Header 항목을 중복해서 넣지 않는다.

## 31.2 Response DTO

화면에 필요한 값만 제공한다. DB 내부 컬럼과 Entity를 그대로 노출하지 않고 개인정보 마스킹·코드명·null 정책을 반영한다.

## 31.3 Query DTO

업무 조회 조건 또는 Mapper Criteria를 나타낸다. pageNo를 offset으로 바꾸고 지점 범위처럼 서버가 보정한 조건을 명시적으로 담는다.

## 31.4 Result DTO

DB 조회 결과나 업무 처리 결과를 담는다. Result DTO를 Response로 직접 반환하지 않고 필요한 필드 선택과 마스킹을 거쳐 변환한다.

## 31.5 Bean Validation

1차 형식 검증은 DTO Annotation, 2차 업무 규칙은 Rule, 3차 존재·중복·동시성은 Service와 DAO로 나눈다. 각 오류는 표준 코드로 연결한다.

## 31.6 DTO 무분별한 재사용 방지

Request 하나를 Command·Criteria·Response로 돌려쓰면 화면 변경이 DB까지 전파되고 쓰기 금지 필드가 노출된다. 목적이 다르면 작더라도 별도 타입을 만든다.

> public record SvCustomerSummaryRequest(
> @NotBlank String customerNo) {}
> public record SvCustomerCriteria(String customerNo, String branchId) {}
> public record SvCustomerSummaryResponse(
> String customerNo, String maskedName, String customerGrade) {}

| **검증 단계** | **위치** | **예시** |
| --- | --- | --- |
| 형식 | Request DTO | 필수·길이·범위 |
| 업무 규칙 | Rule | 기간·상태 전이 |
| DB 정합성 | Service·DAO | 존재·중복·영향 건수 |
| 공통 통제 | STF | ServiceId·사용자·채널 |
| 응답 보호 | Service·Assembler | 마스킹·필드 제한 |

## 제5부 계층 연결 실습

1.  SV.Customer.selectSummary의 Request·Criteria·Result·Response DTO를 각각 정의한다.
2.  Handler에서 Request로 변환하고 Facade를 호출한다.
3.  Service가 Rule과 DAO를 조합하고 조회 없음 오류를 발생시킨다.
4.  Mapper XML에서 SQL ID와 인덱스 조건을 확인한다.
5.  정상·필수값 누락·조회 없음·DB 오류 테스트를 계층별로 실행한다.

## 장 요약

-   DTO는 외부·업무·DB 목적별 계약이다.
-   Validation을 형식·업무·DB·공통 통제로 나눈다.
-   작은 중복보다 잘못된 DTO 재사용이 더 큰 결합을 만든다.

## 근거 자료

-   znsight-man/18-DTO-작성-기준.md
-   znsight-man/19-Validation-작성-기준.md
-   ztcfbook/제03편/09-표준-전문과-DTO.md

# 제6부. DB·SQL·업무 연계 개발

제6부에서는 업무 코드가 데이터를 읽고 변경하며 다른 업무와 협력하는 방법을 다룬다. SQL 한 줄도 DB 자원, 트랜잭션, Timeout, 운영 추적과 연결된다는 관점으로 접근한다.

**CHAPTER 32**

**데이터베이스 기초**

> **이번 장에서 해결할 질문** 업무 프로그램이 데이터를 안전하게 읽고 바꾸려면 어떤 DB 개념을 먼저 이해해야 하는가?

## 32.1 테이블과 컬럼

테이블은 같은 성격의 행을 저장하고 컬럼은 각 속성의 이름·타입·제약을 정의한다. Java 필드와 DB 컬럼은 비슷해 보여도 null, 길이, 숫자 정밀도, 날짜 기준을 별도로 확인해야 한다.

## 32.2 기본키와 외래키

기본키는 행을 유일하게 식별하고 외래키는 테이블 간 참조 관계를 표현한다. 업무키와 물리 PK를 구분하고 등록·삭제 순서와 참조 무결성을 설계한다.

## 32.3 인덱스

인덱스는 특정 조건의 행을 빠르게 찾도록 돕지만 저장 비용과 공간을 사용한다. WHERE·JOIN·ORDER BY 조건과 데이터 분포를 근거로 설계하고 존재만으로 사용을 보장한다고 생각하지 않는다.

## 32.4 View

View는 복잡한 조회나 접근 범위를 캡슐화하지만 성능을 자동 개선하지 않는다. 내부 SQL과 권한, 변경 가능성, 실행계획을 함께 확인한다.

## 32.5 Schema

Schema는 객체의 논리적 소유와 이름 공간이다. RDW·ADW·OMDB·LOGDB처럼 목적이 다른 DB와 Schema를 구분하고 계정의 최소 권한을 지킨다.

## 32.6 Transaction과 Commit

트랜잭션은 여러 변경을 하나의 성공·실패 단위로 묶는다. Commit은 확정, Rollback은 취소이며 NSIGHT에서는 Facade를 기본 경계로 삼는다.

| **DB 영역** | **주요 목적** | **온라인 주의점** |
| --- | --- | --- |
| RDW | 실시간 단건·소량 조회 | Full Scan·집계 금지 |
| ADW | 분석·통계·집계 | 실시간 변경 금지 |
| OMDB | Catalog·권한·코드 | 감사·캐시 정합성 |
| LOGDB | 거래·오류·감사 로그 | 기간 조건 필수 |
| SESSIONDB/JWTDB | 인증 상태 | TTL·정리 정책 |

> **DB 선택이 먼저** SQL을 쓰기 전에 어느 DB가 그 질문에 답해야 하는지 결정한다. RDW에 분석 SQL을 넣는 순간 한 화면의 문제가 전체 온라인 지연으로 확산될 수 있다.

## 장 요약

-   키·제약·인덱스는 데이터 정합성과 성능의 기반이다.
-   DB와 Schema는 목적별 경계를 지킨다.
-   트랜잭션은 Facade에서 업무 성공 단위로 관리한다.

## 근거 자료

-   zman/19-DB-테이블.md
-   ztcfbook/제05편/18-데이터-DB-아키텍처.md

**CHAPTER 33**

**CRUD 거래 만들기**

> **이번 장에서 해결할 질문** 조회·등록·변경·삭제를 같은 계층 원칙으로 구현하되 각 행위의 위험을 어떻게 다르게 다루는가?

## 33.1 목록 조회

검색조건과 Paging을 필수로 두고 list와 page 정보를 반환한다. 빈 결과는 오류가 아니라 빈 목록으로 처리하는 것이 일반적이다.

## 33.2 단건 조회

PK나 유니크 조건으로 0건 또는 1건을 조회한다. 0건이 정상인지 업무 오류인지는 Service가 결정한다.

## 33.3 등록

Rule 검증, 중복 확인, Command 생성, INSERT, 영향 건수 확인 순으로 처리한다. 등록자·등록시각과 idempotencyKey를 고려한다.

## 33.4 변경

현재 상태를 조회하고 허용 전이를 검증한 뒤 PK 조건으로 UPDATE한다. 수정자·수정시각과 낙관적 잠금 기준을 반영한다.

## 33.5 삭제

참조 관계·보관·감사 정책을 먼저 확인한다. 물리 삭제가 필요한 경우 승인 범위와 복구 방법을 명확히 한다.

## 33.6 논리 삭제

USE\_YN이나 삭제 상태를 변경하고 기본 조회에서 제외한다. 유니크 제약, 재등록, 이력 보관 기준까지 함께 설계한다.

## 33.7 CRUD별 오류 처리

중복·미존재·영향 건수 0·동시성 충돌·제약 위반을 구분해 업무 오류와 시스템 오류로 변환한다.

> Handler → Facade @Transactional → Service
> → Rule(검증) → DAO → Mapper XML → DB
> → 영향 건수 확인 → ETF 표준 응답

| **행위** | **정상 판정** | **대표 위험** |
| --- | --- | --- |
| 목록 | 0건 이상+Paging | 대량 조회 |
| 단건 | 0/1건 정책 | 다건 데이터 오류 |
| 등록 | 영향 1건 | 중복·재전송 |
| 변경 | 영향 1건 | Lost Update |
| 삭제 | 정책에 따른 1건 | 참조·감사 유실 |

## 장 요약

-   CRUD마다 정상·오류 판정 기준이 다르다.
-   변경 거래는 감사정보와 중복 방지를 포함한다.
-   영향 건수를 반드시 확인한다.

## 근거 자료

-   znsight-man/27-DAO-개발.md
-   znsight-man/28-MyBatis-Mapper-개발.md
-   znsight-man/36-트랜잭션-기준.md

**CHAPTER 34**

**SQL 작성 기준**

> **이번 장에서 해결할 질문** 결과만 나오는 SQL이 아니라 운영 중에도 안전한 SQL은 어떻게 작성하는가?

## 34.1 SELECT 작성

SELECT \*를 금지하고 필요한 컬럼만 명시한다. 인덱스를 활용할 수 있는 조건, 예측 가능한 최대 건수, DTO 매핑을 확인한다.

## 34.2 JOIN 작성

JOIN 키와 카디널리티를 먼저 확인하고 Cartesian Join을 차단한다. 여러 1:N 관계를 동시에 결합해 행이 폭증하지 않는지 테스트한다.

## 34.3 INSERT·UPDATE·DELETE

컬럼 목록과 바인딩을 명시하고 UPDATE·DELETE에는 PK 또는 유니크 조건을 둔다. 대량 변경은 온라인 거래에서 분리한다.

## 34.4 NULL 처리

NULL과 빈 문자열·0을 구분한다. NVL/COALESCE 남용과 인덱스 컬럼 함수 적용이 실행계획에 미치는 영향을 확인한다.

## 34.5 대량 조회 주의사항

온라인 목록은 Paging과 상한을 적용한다. 대량 다운로드·집계는 별도 ServiceId, ADW, 배치 또는 파일 처리로 분리한다.

## 34.6 SQL Timeout

RDW 3초, ADW 5초 같은 기본 기준과 ServiceId 정책을 맞춘다. SQL Timeout은 거래 Timeout보다 짧아야 원인을 통제하기 쉽다.

## 34.7 실행계획의 기본 개념

접근 경로, 예상·실제 건수, Join 방식, Sort, Full Scan을 확인한다. 개발 데이터의 빠른 결과만으로 운영 성능을 판단하지 않는다.

> /\* SQL\_ID: SV.Customer.selectList \*/
> SELECT C.CUSTOMER\_NO, C.CUSTOMER\_NAME, C.CUSTOMER\_GRADE
> FROM RDW\_CUSTOMER\_SUMMARY C
> WHERE C.BRANCH\_CODE = #{branchCode}
> ORDER BY C.CUSTOMER\_NO
> OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY

| **금지 패턴** | **문제** | **대안** |
| --- | --- | --- |
| SELECT \* | 불필요 I/O·변경 영향 | 필요 컬럼 명시 |
| WHERE 없는 목록 | Full Scan·대량 유입 | 조건+Paging |
| ${userInput} | SQL Injection | #{}·whitelist |
| 인덱스 컬럼 함수 | 접근 경로 악화 | 범위 조건 변환 |
| ORDER BY 없는 Paging | 페이지 중복·누락 | 안정적 정렬키 |

> **성능 연쇄** 느린 SQL은 DB 세션 증가→Connection Pool 점유→Tomcat Thread 대기→거래 Timeout으로 이어진다. SQL 리뷰는 애플리케이션 안정성 리뷰다.

## 장 요약

-   필요 컬럼·조건·정렬·최대 건수를 명시한다.
-   Paging과 Timeout을 기본 전제로 둔다.
-   실행계획과 운영 데이터 규모로 검증한다.

## 근거 자료

-   znsight-man/29-SQL-작성-기준.md
-   znsight-man/28-MyBatis-Mapper-개발.md

**CHAPTER 35**

**페이징과 대량 데이터**

> **이번 장에서 해결할 질문** 사용자에게 필요한 범위만 빠르게 제공하고 대량 처리를 온라인 거래에서 격리하려면 어떻게 하는가?

## 35.1 페이징이 필요한 이유

전체 결과를 WAS 메모리로 가져오면 DB Connection과 Tomcat Thread가 오래 점유된다. DB가 필요한 범위만 반환하도록 한다.

## 35.2 전체 건수 조회

목록 SQL과 같은 WHERE의 Count SQL을 별도로 작성한다. Count가 매우 무거우면 hasNext 방식이나 제한 Count를 검토한다.

## 35.3 페이지 번호 방식

화면은 pageNo·pageSize를 보내고 Rule이 offset=(pageNo-1)×pageSize를 계산한다. offset을 클라이언트가 직접 보내지 않는다.

## 35.4 대용량 조회 방지

pageSize 기본 100, 일반 최대 500 등 상한을 서버에서 강제한다. 검색 기간·지점·상태 조건도 업무별로 제한한다.

## 35.5 엑셀 다운로드

화면 목록과 다른 ServiceId로 분리하고 권한·사유·최대건수·마스킹·감사로그를 적용한다. 동기 생성 한계를 넘으면 비동기 파일 작업으로 전환한다.

## 35.6 대용량 파일 처리

Chunk 조회·스트리밍·임시파일·만료·다운로드 이력을 설계한다. Heap에 전체 데이터를 쌓거나 하나의 장시간 DB 트랜잭션으로 묶지 않는다.

> pageNo = max(request.pageNo, 1)
> pageSize = min(max(request.pageSize, 1), 500)
> offset = (pageNo - 1) \* pageSize
> totalPage = ceil(totalCount / pageSize)

| **처리** | **온라인 목록** | **대량 다운로드** |
| --- | --- | --- |
| ServiceId | 조회 전용 | 별도 다운로드 거래 |
| 건수 | 페이지 상한 | 업무 최대건수 |
| 응답 | 즉시 JSON | 비동기 파일 가능 |
| 보안 | 화면 권한 | 사유·감사·마스킹 |
| 자원 | 짧은 DB 점유 | Chunk·스트리밍 |

## 장 요약

-   페이징은 업무 WAR의 Rule→Service→DAO→SQL에서 처리한다.
-   서버가 pageSize 상한과 offset을 통제한다.
-   대량 다운로드는 별도 거래와 자원 정책으로 분리한다.

## 근거 자료

-   znsight-man/30-페이징-처리-기준.md
-   znsight-man/29-SQL-작성-기준.md

**CHAPTER 36**

**DB 객체 명명규칙**

> **이번 장에서 해결할 질문** 이름만으로 객체의 업무 영역과 역할을 추적할 수 있게 하려면 어떤 기준이 필요한가?

## 36.1 테이블명

업무 또는 시스템 접두어와 목적이 드러나는 명사를 사용한다. OM\_SERVICE\_CATALOG, TCF\_TX\_LOG처럼 소유와 역할을 표현한다.

## 36.2 컬럼명

CUSTOMER\_NO, REG\_DT, UPD\_USER, USE\_YN처럼 공통 용어와 데이터 사전을 따른다. 같은 개념에 ID·NO·CD를 혼용하지 않는다.

## 36.3 인덱스명

인덱스임을 나타내는 접두어와 테이블·순번 또는 주요 목적을 결합한다. 이름만으로 대상 객체를 찾을 수 있게 한다.

## 36.4 제약조건명

PK·FK·UK·CK 유형과 대상 테이블을 표현한다. 자동 생성 이름을 방치하면 장애 로그에서 원인을 찾기 어렵다.

## 36.5 Sequence명

대상 업무 객체와 채번 목적을 드러낸다. 캐시 크기·증가값·순환 여부는 업무 키 정책과 함께 검토한다.

## 36.6 업무코드와 DB Prefix

업무 테이블은 업무코드 접두어를, 플랫폼·OM·파일 객체는 정해진 시스템 접두어를 사용한다. 접두어는 소유권과 변경 승인 경계를 보여준다.

## 36.7 Java 필드와 컬럼 매핑

DB의 대문자 스네이크 표기와 Java camelCase를 일관되게 매핑한다. 의미가 다른 필드를 자동 매핑 편의 때문에 같은 이름으로 만들지 않는다.

| **객체** | **예시** | **확인** |
| --- | --- | --- |
| Table | SV\_CUSTOMER\_MEMO | 업무·목적 |
| Column | CUSTOMER\_NO, REG\_DT | 표준 용어·타입 |
| PK | PK\_SV\_CUSTOMER\_MEMO | 대상 명확 |
| FK | FK\_SV\_MEMO\_CUSTOMER | 참조 관계 |
| Index | IX\_SV\_MEMO\_01 | 조회 조건 |
| Sequence | SEQ\_SV\_CUSTOMER\_MEMO | 채번 목적 |

> **이름은 운영 도구** 명명규칙은 미관이 아니다. DDL, Mapper XML, 오류 로그, 실행계획, 설계서에서 같은 객체를 빠르게 찾기 위한 추적 장치다.

## 장 요약

-   DB 이름은 업무 소유와 객체 역할을 드러낸다.
-   공통 컬럼·용어 사전을 일관되게 사용한다.
-   Java와 DB 매핑은 이름과 의미를 함께 맞춘다.

## 근거 자료

-   znsight-man/명명규칙-13-DB-객체.md
-   zman/19-DB-테이블.md

**CHAPTER 37**

**다른 업무와 연계하기**

> **이번 장에서 해결할 질문** 도메인과 WAR 경계를 지키면서 다른 기능과 데이터를 안전하게 사용하는 방법은 무엇인가?

## 37.1 동일 도메인 내부 호출

같은 도메인에서는 공개 Service 메서드를 통해 규칙과 데이터 접근을 재사용한다. 내부 메서드 호출을 지나치게 잘게 나누지 않는다.

## 37.2 동일 WAR의 다른 도메인 호출

다른 도메인의 Service 계약을 호출하고 DAO·Mapper를 직접 사용하지 않는다. 순환 의존이 생기면 별도 조정 Service나 경계 재설계를 검토한다.

## 37.3 다른 WAR 호출

상대 WAR의 Java 클래스를 의존성에 추가하지 않는다. HTTP/JSON 표준 전문과 tcf-eai 공통 Client를 사용한다.

## 37.4 ServiceId 기반 연계

대상 businessCode·ServiceId·transactionCode·Body·응답 타입을 명시한다. 호출·피호출 거래 모두 GUID와 traceId를 이어 기록한다.

## 37.5 외부 시스템 Client

통신 설정·인증·직렬화·오류 변환은 client/adapter에 캡슐화한다. Service 안에 URL과 HTTP 코드를 직접 작성하지 않는다.

## 37.6 Timeout과 오류 전파

연결·읽기·전체 거래 Timeout을 구분하고 업무 오류·통신 오류·Timeout을 다른 코드로 변환한다. Retry는 멱등한 조회성 호출에 제한적으로 적용한다.

## 37.7 직접 테이블 접근이 위험한 이유

다른 업무 테이블을 직접 읽으면 권한·규칙·마스킹·변경 계약을 우회한다. 단기적으로 빠르더라도 데이터 소유권과 배포 독립성을 파괴한다.

> IC Service → SvIntegrationClient → NsightIntegrationClient
> → POST /sv/online
> → serviceId=SV.Customer.selectSummary
> → SV TCF → Handler → 업무 처리

| **호출 범위** | **권장 방식** | **금지** |
| --- | --- | --- |
| 같은 도메인 | Service 내부 계약 | 계층 우회 |
| 같은 WAR·다른 도메인 | 상대 Service | 상대 DAO 직접 호출 |
| 다른 WAR | 표준 전문+tcf-eai | Java 직접 참조 |
| 외부 시스템 | 전용 Client/Adapter | Service의 HTTP 코드 |
| 다른 업무 데이터 | 소유 서비스 호출 | 테이블 직접 조회 |

> **원격 트랜잭션** 원격 서비스와 로컬 DB를 하나의 @Transactional로 묶을 수 있다고 가정하지 않는다. 호출 순서, 부분 성공, 보상, 상태 조회, 재처리를 설계한다.

## 제6부 실습 점검

1.  고객목록 조회에 pageNo·pageSize·Count SQL·안정적 ORDER BY를 적용한다.
2.  고객메모 등록·변경·논리삭제의 영향 건수와 감사정보를 확인한다.
3.  SQL 실행계획과 Query Timeout을 기록한다.
4.  다른 업무의 고객요약 기능을 표준 ServiceId 연계로 호출한다.
5.  연계 Timeout과 부분 성공 상황을 테스트한다.

## 장 요약

-   같은 WAR 안에서는 Service 경계를, 다른 WAR에서는 표준 전문 경계를 지킨다.
-   직접 Java·DB 참조를 금지한다.
-   연계는 Timeout·오류·추적·부분 성공 정책까지 포함한다.

## 근거 자료

-   znsight-man/31-서비스간-연동-개발.md
-   znsight-man/36-트랜잭션-기준.md
-   zman/05-TCF처리구조.md

# 제7부. 인증·보안·권한 이해

제7부에서는 인증 수단과 거래 실행 권한을 구분하고, SSO·세션·JWT·Gateway·STF가 각 신뢰 경계에서 맡는 책임을 연결한다. 보안 기능을 한곳에 몰아넣는 대신 우회가 불가능한 다층 검증 구조를 이해하는 것이 목표다.

**CHAPTER 38**

**로그인과 인증의 기본 개념**

> **이번 장에서 해결할 질문** 사용자가 누구인지 확인하고 이후 요청에서 그 상태를 안전하게 유지하는 방법은 무엇인가?

## 38.1 인증과 인가

인증은 사용자가 누구인지 확인하는 과정이고 인가는 인증된 사용자가 특정 기능과 데이터에 접근할 수 있는지 판단하는 과정이다. 로그인 성공만으로 모든 ServiceId를 실행할 수 있는 것은 아니다.

## 38.2 SSO

SSO는 외부 IdP에서 한 번 인증한 결과를 여러 시스템이 신뢰하도록 연결한다. NSIGHT는 SSO Token을 tcf-om에서 검증하고 OM\_USER 상태·지점·권한그룹을 확인한다.

## 38.3 Session

세션은 서버가 로그인 상태를 유지하는 방식이다. 기준 구조는 Spring Session JDBC와 SESSIONDB이며 업무 코드는 HttpSession을 직접 탐색하지 않고 검증된 SessionContext를 사용한다.

## 38.4 Cookie와 JSESSIONID

브라우저는 세션 식별 Cookie를 요청마다 전송한다. HttpOnly·Secure·SameSite·Path·만료 정책을 설정하고 Cookie 값을 로그나 화면 저장소에 노출하지 않는다.

## 38.5 JWT

JWT는 인증 결과를 서명된 Claim으로 전달하는 토큰이다. 서버 세션과 목적이 다르며 JWT가 권한·거래통제·업무 규칙을 대체하지 않는다.

## 38.6 로그인 상태와 사용자 상태

세션이나 토큰이 유효해도 OM\_USER가 잠김·퇴직·비활성 상태이면 거래를 차단해야 한다. 인증 상태와 현재 사용자 기준정보를 함께 판단한다.

| **개념** | **답하는 질문** | **NSIGHT 책임** |
| --- | --- | --- |
| 인증 | 누구인가? | SSO·세션·JWT |
| 인가 | 무엇을 할 수 있나? | 기능·데이터 권한 |
| 거래통제 | 이 Header 조합이 허용됐나? | STF Allow-List |
| 업무 규칙 | 현재 상태에서 가능한가? | Service·Rule |

> **구분 원칙** 인증 성공, 권한 보유, 거래통제 등록, 업무 조건 충족은 서로 다른 검사다. 어느 하나도 다른 검사를 대신하지 않는다.

## 장 요약

-   인증과 인가는 다른 책임이다.
-   세션은 SESSIONDB, JWT는 서명된 인증 전달 수단이다.
-   현재 사용자 상태를 매 요청에서 정책에 맞게 확인한다.

## 근거 자료

-   znsight-man/39-세션-사용-기준.md
-   znsight-man/41-JWT-SSO-연계.md
-   zman/11-JWT-SSO인증.md

**CHAPTER 39**

**JWT 쉽게 이해하기**

> **이번 장에서 해결할 질문** JWT 안에는 무엇이 들어가고 무엇이 절대 들어가면 안 되는가?

## 39.1 JWT 구조

JWT는 Header.Payload.Signature 세 구간을 점으로 연결한 문자열이다. Base64Url 인코딩은 암호화가 아니므로 Payload는 누구나 읽을 수 있다고 가정한다.

## 39.2 Header·Payload·Signature

Header는 알고리즘과 Key ID, Payload는 Claim, Signature는 발급자의 개인키로 만든 위변조 검증값이다. 서명 검증 없이 Payload만 읽어 인증에 사용하면 안 된다.

## 39.3 Access Token

업무 API 호출에 사용하는 단기 토큰이다. Authorization: Bearer로 전송하고 메모리 또는 안전한 HttpOnly Cookie 저장 방식을 정책에 맞게 선택한다.

## 39.4 Refresh Token

Access Token을 재발급하기 위한 장기 자격증명이다. DB에는 원문 대신 Hash를 저장하고 회전·재사용 탐지·폐기 정책을 적용한다.

## 39.5 만료시간

exp·iat·nbf와 서버 시간 오차를 검증한다. Access Token은 짧게, Refresh Token은 더 길게 운용하되 위험과 사용성을 함께 고려한다.

## 39.6 Claim

iss·sub·aud·exp·iat·nbf·jti를 검증하고 userId·branchId·channelId 등 최소 Claim을 사용한다. 고객번호·계좌번호·조회결과·비밀번호·토큰 원문을 넣지 않는다.

> Header: alg=RS256, kid=key-2026-01
> Payload: iss, sub, aud, exp, iat, nbf, jti, userId, branchId
> Signature: RSASSA-PKCS1-v1\_5 using SHA-256

| **토큰** | **용도** | **권장 보호** |
| --- | --- | --- |
| Access | 업무 요청 인증 | 단기·Bearer·로그 금지 |
| Refresh | Access 재발급 | Hash 저장·회전 |
| Admin Access | 관리자 기능 | 더 짧은 만료·강한 권한 |
| One-Time | 다운로드·민감 승인 | 1회 사용·수분 만료 |

> **Payload는 공개 정보** JWT가 서명됐다는 것은 변조 여부와 발급자를 검증할 수 있다는 뜻이지 Payload가 비밀이라는 뜻이 아니다.

## 장 요약

-   JWT는 Header·Payload·Signature 구조다.
-   Access와 Refresh Token의 목적과 보관을 분리한다.
-   Claim에는 최소 인증 문맥만 넣는다.

## 근거 자료

-   znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md
-   znsight-man/41-JWT-SSO-연계.md

**CHAPTER 40**

**JWT 발급과 검증**

> **이번 장에서 해결할 질문** tcf-jwt가 토큰을 발급하고 Gateway와 업무 WAR가 신뢰를 검증하는 과정은 어떻게 이루어지는가?

## 40.1 tcf-jwt의 역할

tcf-jwt는 Access·Refresh Token 발급, 갱신, 폐기와 JWKS 제공을 담당한다. 업무 처리와 사용자 권한 판단을 직접 수행하지 않는다.

## 40.2 Private Key와 Public Key

Private Key는 서명에만 사용하고 tcf-jwt 밖으로 내보내지 않는다. Gateway와 업무 WAR는 Public Key로 서명을 검증한다.

## 40.3 RS256

RS256은 SHA-256과 RSA 비대칭 서명을 사용한다. 대칭 Secret을 여러 서비스에 배포하는 방식보다 검증 주체가 개인키를 가지지 않아도 되는 장점이 있다.

## 40.4 JWKS

JWKS Endpoint는 kid에 대응하는 공개키를 제공한다. 검증기는 캐시 만료와 갱신 실패 정책을 두고 임의의 외부 JWKS URL을 신뢰하지 않는다.

## 40.5 서명·만료·발급자 검증

알고리즘 고정, 서명, iss, aud, exp, nbf, jti와 Denylist를 검증한다. 이후 Claim의 userId·branchId·channelId를 표준 Header와 교차검증한다.

## 40.6 키 교체

새 kid로 서명하기 전에 공개키를 배포하고 기존 토큰 만료까지 이전 공개키를 유지한다. 개인키 유출 시 즉시 폐기·교체·Denylist·감사 절차를 수행한다.

> tcf-om SSO 검증 → JWT.Auth.ssoIssue 내부 호출
> → tcf-jwt Private Key 서명 → Access+Refresh 발급
> → Gateway JWKS Public Key 검증 → STF Claim↔Header 검증

| **검증** | **실패 예** | **처리** |
| --- | --- | --- |
| alg·kid | 허용 알고리즘 아님 | 즉시 거부 |
| signature | 위변조·잘못된 키 | 인증 오류 |
| iss·aud | 다른 발급자·대상 | 인증 오류 |
| exp·nbf | 만료·사용 전 | 재로그인/갱신 |
| jti | Denylist 등록 | 강제 폐기 |
| Claim↔Header | 사용자·지점 불일치 | STF 차단 |

## 장 요약

-   개인키는 발급자만, 공개키는 검증자가 사용한다.
-   서명뿐 아니라 발급자·대상·시간·폐기 상태를 검증한다.
-   키 교체는 이전·신규 키가 공존하는 절차로 수행한다.

## 근거 자료

-   znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md
-   zman/11-JWT-SSO인증.md

**CHAPTER 41**

**Gateway가 있는 구조와 없는 구조**

> **이번 장에서 해결할 질문** 인증을 어느 구간에서 검사해야 우회 경로 없이 안전한가?

## 41.1 Gateway 1차 검증

Gateway는 Authorization Header를 받고 JWT 서명·만료를 1차 검증한 뒤 업무코드에 따라 라우팅한다. Token 원문은 로그에 남기지 않는다.

## 41.2 업무 WAR 직접 접근 차단

네트워크·L4·방화벽·Ingress 정책으로 외부가 업무 WAR를 직접 호출하지 못하게 한다. Gateway 검증만 믿고 직접 경로를 열어두면 인증을 우회할 수 있다.

## 41.3 Gateway가 없는 경우

내부망이나 단순 구성에서도 인증 책임이 사라지지 않는다. 업무 WAR Security Filter가 JWT 또는 세션을 검증하고 STF가 최종 통제를 수행한다.

## 41.4 업무 WAR JWT Filter

Filter는 Bearer 추출, Decoder 검증, 인증 문맥 생성을 담당한다. ServiceId 권한과 업무 데이터 권한은 STF·Rule의 책임으로 남긴다.

## 41.5 STF 방어 검증

STF는 Gateway 검증 결과를 무조건 신뢰하지 않고 Claim과 Header, 사용자 상태, 권한, 거래통제를 재확인한다. 이는 중복 구현이 아니라 신뢰 경계별 방어다.

## 41.6 인증 책임 중복 방지

Gateway·Filter·STF 각각의 입력·출력·오류코드를 문서화한다. 같은 Token 파싱을 여러 곳에서 제각각 구현하지 않고 공통 보안 모듈을 사용한다.

| **구간** | **책임** | **하지 않을 일** |
| --- | --- | --- |
| Gateway | Token 1차 검증·라우팅 | 업무 규칙 |
| WAR Filter | 직접 접근 시 인증 문맥 | ServiceId 통제 |
| STF | Claim·Header·권한·통제 | 토큰 발급 |
| Rule | 데이터 범위·업무 규칙 | JWT 파싱 |

> **최종 방어선** Gateway가 있다고 업무 WAR 검증을 제거하지 않는다. 내부 오호출·설정 오류·우회 경로까지 고려해 STF가 최종 실행 판단을 유지한다.

## 장 요약

-   Gateway는 1차 검증과 라우팅을 담당한다.
-   업무 WAR 직접 접근을 네트워크와 Filter로 차단한다.
-   STF는 거래 실행의 최종 방어선이다.

## 근거 자료

-   znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md
-   znsight-man/81-JWT-TCF-WEB-개발-매뉴얼.md
-   zman/09-Gateway라우팅.md

**CHAPTER 42**

**권한과 개인정보**

> **이번 장에서 해결할 질문** 메뉴가 보이는 것과 실제 거래·데이터에 접근할 수 있는 것은 어떻게 다른가?

## 42.1 메뉴권한

메뉴권한은 UI 탐색 가능 여부를 결정한다. 메뉴를 숨기는 것은 편의 기능이며 서버의 ServiceId 권한 검사를 대신하지 않는다.

## 42.2 기능권한

조회·등록·변경·삭제·다운로드처럼 기능별 권한을 ServiceId와 연결한다. 관리자 기능은 별도 권한과 강화된 감사 기준을 적용한다.

## 42.3 데이터권한

같은 기능 권한이 있어도 지점·조직·담당 고객 범위 밖의 데이터는 접근하지 못해야 한다. Rule과 SQL 조건에 검증된 데이터 범위를 반영한다.

## 42.4 사용자·지점 정보

Header 값은 세션 또는 JWT Claim과 비교한다. 사용자가 보낸 branchId를 그대로 SQL 조건에 사용해 권한을 확장하지 않는다.

## 42.5 개인정보 마스킹

화면 목적과 권한에 따라 이름·전화·계좌 등 필요한 범위만 마스킹한다. DB 원본 조회와 응답 변환, 파일 다운로드 정책을 함께 설계한다.

## 42.6 다운로드와 감사로그

대량 다운로드는 별도 권한·사유·최대건수·파일 만료를 적용하고 누가 언제 무엇을 내려받았는지 감사로그에 남긴다.

## 42.7 관리자 권한

권한 부여·사용자 상태·Token 폐기·거래통제 변경은 고위험 작업이다. 최소 권한, 이중 승인 또는 재인증, 변경 전후 값, traceId를 기록한다.

| **권한 층** | **예시 질문** | **검증 위치** |
| --- | --- | --- |
| 메뉴 | 화면에 보이나? | UI·OM |
| 기능 | 이 ServiceId를 실행하나? | STF |
| 데이터 | 이 고객·지점을 보나? | Rule·SQL |
| 관리자 | 고위험 변경 가능한가? | 강화 인증·감사 |
| 다운로드 | 대량 반출 가능한가? | 전용 거래·감사 |

> **로그 최소화** Token, 비밀번호, 주민번호, 계좌번호 원문을 애플리케이션·거래·감사 로그에 남기지 않는다. 추적에는 식별자와 마스킹 값을 사용한다.

## 장 요약

-   메뉴·기능·데이터 권한을 분리한다.
-   사용자·지점은 서버 인증 문맥으로 재검증한다.
-   개인정보 조회·다운로드·관리자 변경은 감사와 마스킹을 강화한다.

## 근거 자료

-   znsight-man/40-권한-검증-기준.md
-   znsight-man/42-보안-코딩-기준.md

**CHAPTER 43**

**세션 제거와 JWT 전환**

> **이번 장에서 해결할 질문** 서버 상태를 줄이면서도 즉시 권한 변경과 강제 로그아웃을 유지하려면 무엇을 설계해야 하는가?

## 43.1 서버 세션의 문제

공유 SESSIONDB는 확장성과 일관성을 돕지만 저장·정리·직렬화·DB 의존 비용이 있다. 업무 데이터를 세션에 저장하면 크기와 장애 영향이 급격히 커진다.

## 43.2 Stateless 구조

매 요청이 Access Token만으로 인증 가능하면 업무 서버는 사용자 세션 상태를 덜 보유한다. 다만 Refresh Token·Denylist·권한 기준정보는 여전히 서버 상태와 운영 관리가 필요하다.

## 43.3 로그아웃과 Token 폐기

클라이언트 저장소 삭제만으로 로그아웃이 완성되지 않는다. Refresh Token을 폐기하고 필요 시 Access Token jti를 Denylist에 등록하며 세션 Cookie도 무효화한다.

## 43.4 강제 로그아웃

퇴직·권한 회수·침해 사고에는 사용자 또는 세션 단위로 즉시 폐기해야 한다. OM에서 조작하고 감사로그와 전파 지연을 모니터링한다.

## 43.5 Token Version과 DenyList

사용자 tokenVersion을 올리면 이전 버전을 일괄 거부할 수 있고 Denylist는 개별 jti를 즉시 차단한다. 규모·조회비용·TTL에 따라 두 방식을 조합한다.

## 43.6 OM의 인증 관리 역할

OM은 세션·Token 상태, 만료, 중복 로그인, 강제 종료, Refresh 폐기, Denylist, 키 교체 감사와 이상 징후를 관리한다.

| **전환 단계** | **변경** | **확인** |
| --- | --- | --- |
| 1\. 혼합 | 세션+JWT 병행 | Claim↔Header 정합 |
| 2\. API 우선 | Bearer 인증 확대 | 직접 경로 Filter |
| 3\. 상태 축소 | 업무 세션 정보 제거 | 권한 최신성 |
| 4\. 폐기 체계 | Denylist·Version | 전파 지연 |
| 5\. 운영 전환 | OM 모니터링·감사 | 장애·롤백 |

## 제7부 보안 실습 점검

1.  정상·만료·위변조·다른 aud의 Access Token을 검증한다.
2.  JWT userId와 요청 Header userId를 다르게 보내 STF 차단을 확인한다.
3.  업무 WAR 직접 접근 경로가 네트워크와 Filter에서 막히는지 확인한다.
4.  권한 회수와 강제 로그아웃 후 기존 Token이 거부되는 시간을 측정한다.
5.  로그와 오류 응답에 Token·개인정보·StackTrace가 없는지 검사한다.

## 장 요약

-   Stateless는 서버 상태가 완전히 사라진다는 뜻이 아니다.
-   로그아웃은 Refresh·Access·세션 폐기를 함께 고려한다.
-   Token Version·Denylist·OM 운영으로 즉시 폐기 능력을 확보한다.

## 근거 자료

-   znsight-man/39-세션-사용-기준.md
-   znsight-man/41-JWT-SSO-연계.md
-   znsight-man/79-TCF-GATEWAY-JWT-개발-매뉴얼.md

# 제8부. 배포·운영·장애 대응

제8부에서는 개발이 끝난 코드를 운영 가능한 WAR로 배포하고, JVM·Tomcat·DB Pool·Timeout·로그·OM 지표를 이용해 상태를 확인하며 장애 원인을 증거로 좁히는 방법을 다룬다.

**CHAPTER 44**

**업무 WAR 배포하기**

> **이번 장에서 해결할 질문** 검증된 소스를 독립적인 WAR로 만들고 Tomcat에 안전하게 반영하려면 무엇을 확인해야 하는가?

## 44.1 WAR 생성

Gradle bootWar로 업무별 산출물을 만든다. 운영 서버에서 직접 빌드하지 않고 CI/CD가 동일한 JDK·의존성·테스트 기준으로 생성한다.

## 44.2 Tomcat 배포

승인된 WAR를 대상 인스턴스에 반영하고 기동 로그·Health Check·대표 거래를 확인한다. Class 파일만 교체하는 식의 부분 배포는 금지한다.

## 44.3 Context Path

WAR 파일명과 Context Path, businessCode, Gateway 경로를 일치시킨다. sv.war는 일반적으로 /sv로 배포한다.

## 44.4 여러 WAR 배포

업무 WAR는 독립 배포 단위지만 같은 Tomcat의 Thread·Heap·공용 설정을 공유할 수 있다. 배포 순서와 장애 영향을 사전에 분석한다.

## 44.5 재기동과 무중단 배포

다중 인스턴스에서 한 대씩 트래픽을 제외하고 배포·Health 확인 후 복귀한다. 세션·캐시·진행 중 거래의 드레이닝 기준을 둔다.

## 44.6 배포 확인

빌드 Commit ID, 파일 Hash·크기, Profile, Secret 외부 주입, Mapper·ServiceId, Health, 로그, 대표 조회·변경 거래와 롤백 가능성을 확인한다.

> gradlew.bat :sv-service:clean :sv-service:test :sv-service:bootWar
> 산출물: sv-service/build/libs/sv.war
> 배포 후: /sv/actuator/health + 대표 ServiceId 확인

| **단계** | **확인** | **실패 시** |
| --- | --- | --- |
| 빌드 | Test·bootWar·Hash | 배포 중단 |
| 반영 | Context·Profile·Secret | 이전 WAR 유지 |
| 기동 | 로그·Health | 트래픽 미복귀 |
| 검증 | 대표 거래·OM 로그 | 롤백 |
| 완료 | 버전·감사 이력 | 배포 기록 보완 |

## 장 요약

-   운영 WAR는 CI/CD에서 생성한다.
-   Context와 업무코드를 일치시킨다.
-   Health와 대표 거래 확인 후 트래픽을 복귀한다.

## 근거 자료

-   znsight-man/64-WAR-생성-기준.md
-   znsight-man/09-업무-WAR-구조.md

**CHAPTER 45**

**JVM과 Tomcat 자원 이해하기**

> **이번 장에서 해결할 질문** 요청이 늘어날 때 CPU·메모리·Thread·Queue는 어떻게 상호작용하는가?

## 45.1 CPU와 Core

CPU 사용률만 보지 말고 Core 수, Load, Run Queue, GC와 애플리케이션 Thread를 함께 본다. CPU가 낮아도 DB·외부 호출 대기로 느릴 수 있다.

## 45.2 Heap과 Metaspace

Heap은 객체, Metaspace는 클래스 메타데이터에 사용된다. 최대값을 OS 메모리와 무관하게 크게 잡지 않고 Native·Thread Stack·Buffer 공간을 남긴다.

## 45.3 Garbage Collection

GC는 사용하지 않는 객체를 회수한다. Pause 시간·빈도·할당률·Old 영역 증가와 요청 지연을 같은 시간축에서 비교한다.

## 45.4 Tomcat Thread

Thread는 동시 요청을 처리하지만 대기 작업이 길면 모두 점유된다. maxThreads는 CPU·DB Pool·응답시간과 함께 산정한다.

## 45.5 Request Queue

가용 Thread가 없을 때 요청이 Queue에서 기다린다. Queue를 크게 하면 장애가 늦게 보일 뿐 지연과 메모리 사용이 증가할 수 있다.

## 45.6 자원 부족의 증상

CPU 포화, 긴 GC, OOM, Thread 고갈, Queue 증가, 응답시간 상승, Timeout을 구분하고 Thread Dump·GC Log·Metrics로 증명한다.

| **증상** | **지표** | **다음 확인** |
| --- | --- | --- |
| CPU 포화 | 사용률·Load | Hot Method |
| GC 지연 | Pause·Old | 할당·Leak |
| Thread 고갈 | Busy=max | DB·외부 대기 |
| Queue 증가 | acceptCount | 처리율·Timeout |
| OOM | Heap/Native | Dump·Stack 수 |

> **크게 잡기의 함정** Thread·Heap·Queue를 크게 만드는 것은 용량 개선이 아니다. 하위 DB와 외부 시스템이 처리할 수 있는 양을 넘기면 장애 규모만 커진다.

## 장 요약

-   JVM 자원은 OS 메모리 안에서 함께 설계한다.
-   Thread 수는 하위 자원과 응답시간에 맞춘다.
-   증상과 시간축 지표로 병목을 구분한다.

## 근거 자료

-   znsight-capacity-word/NSIGHT\_JVM\_산정\_가이드.docx
-   znsight-capacity-word/NSIGHT\_Tomcat\_산정\_가이드.docx

**CHAPTER 46**

**DB Connection Pool 이해하기**

> **이번 장에서 해결할 질문** DB 연결을 재사용하면서도 대기와 고갈을 통제하는 방법은 무엇인가?

## 46.1 Connection Pool이 필요한 이유

연결 생성 비용을 줄이고 DB 동시 세션 상한을 통제한다. Pool은 처리 능력을 만들어내는 것이 아니라 제한된 연결을 관리한다.

## 46.2 HikariCP

HikariCP는 maximumPoolSize, minimumIdle, connectionTimeout, maxLifetime 등으로 연결 생명주기와 대기를 관리한다. DB·네트워크 Timeout과 정합시킨다.

## 46.3 Maximum Pool Size

인스턴스 수×WAR 수×Pool 수를 합산해 DB 허용 세션 안에서 정한다. Tomcat Thread보다 무조건 크게 잡지 않는다.

## 46.4 Active·Idle·Pending

Active는 사용 중, Idle은 대기 연결, Pending은 연결을 기다리는 Thread다. Pending 지속은 SQL 지연·Pool 부족·누수 신호다.

## 46.5 Connection Timeout

연결 대기 상한이다. 길게 두면 Thread가 쌓이고 짧게 두면 순간 피크에 실패할 수 있어 Online Timeout보다 짧게 계층화한다.

## 46.6 Connection 누수

반환되지 않는 연결은 Pool을 고갈시킨다. 트랜잭션 범위, 예외 경로, 장시간 SQL을 확인하고 leakDetection은 진단 목적으로 제한 사용한다.

> DB 허용 세션 ≥ 인스턴스 수 × WAR별 Pool 합계 + 운영 여유
> Query Timeout < Transaction Timeout < Online/Client Timeout

| **지표 조합** | **해석** | **조치 방향** |
| --- | --- | --- |
| Active↑ Pending↑ | DB 작업 지연/Pool 부족 | Slow SQL·세션 확인 |
| Idle↑ Pending=0 | 여유 또는 과대 | 최소 Idle 검토 |
| Active 고정·반환 없음 | 누수 의심 | Stack·Tx 경계 |
| Connection Timeout↑ | 대기 상한 초과 | DB·Pool·피크 분석 |

## 장 요약

-   Pool은 DB 동시성 상한이다.
-   전체 인스턴스와 WAR의 합계로 산정한다.
-   Pending·Slow SQL·누수를 함께 분석한다.

## 근거 자료

-   znsight-capacity-word/2026-06-03 NSIGHT \_ 용량산정 \_ DB Pool.docx
-   znsight-man/11-application-yml-기준.md

**CHAPTER 47**

**거래 Timeout과 거래통제**

> **이번 장에서 해결할 질문** 장애 확산을 막으면서 허용된 거래만 실행하도록 어떤 정책을 적용하는가?

## 47.1 Timeout이 필요한 이유

느린 거래가 Thread·Connection을 무기한 점유하지 않게 하고 SLA와 재처리 판단 기준을 제공한다.

## 47.2 ServiceId별 Timeout

OM의 TCF\_SERVICE\_TIMEOUT\_POLICY에서 거래 특성별 Online Timeout을 관리하고 STF가 조회해 Executor에 전달한다.

## 47.3 SQL Timeout

MyBatis Query Timeout은 전체 거래보다 짧게 둔다. DB가 계속 실행 중인데 애플리케이션만 포기하는 상태를 최소화한다.

## 47.4 외부 호출 Timeout

Connect·Read·전체 호출 Timeout을 분리한다. 대상별 정책과 Circuit Breaker·격리·제한 Retry를 적용한다.

## 47.5 거래 허용·중지

거래통제는 허용 여부, Timeout은 허용된 거래의 시간 상한이다. Catalog·Header 조합·ACTIVE 상태를 확인한다.

## 47.6 중복 요청과 멱등성

Timeout 후 재시도가 중복 변경을 만들 수 있다. 변경 거래에는 idempotencyKey와 최종 상태 조회를 설계한다.

| **계층** | **설정** | **관계** |
| --- | --- | --- |
| Proxy/Gateway | 요청 상한 | 가장 바깥 |
| Online Executor | ServiceId 정책 | 거래 전체 |
| Transaction | Facade timeout | DB 업무 단위 |
| MyBatis | Query timeout | SQL 단위 |
| Hikari | connectionTimeout | 연결 대기 |
| EAI Client | connect/read | 원격 호출 |

> **Timeout 결과** TIMEOUT은 항상 실패 확정이 아니다. 등록·연계 거래는 UNKNOWN일 수 있으므로 무조건 재실행하지 말고 상태를 확인한다.

## 장 요약

-   거래통제와 Timeout은 별도 정책이다.
-   안쪽 Timeout을 바깥쪽보다 짧게 둔다.
-   변경 거래의 Timeout은 멱등성과 상태 조회가 필요하다.

## 근거 자료

-   zman/13-거래통제.md
-   zman/14-Timeout관리.md
-   znsight-man/49-Timeout-정책-관리.md

**CHAPTER 48**

**로그로 거래 추적하기**

> **이번 장에서 해결할 질문** 화면 오류 한 건을 Web부터 SQL까지 어떻게 같은 거래로 연결하는가?

## 48.1 애플리케이션 로그

애플리케이션 이벤트와 예외를 기록한다. Level을 목적에 맞게 사용하고 운영에서 무분별한 DEBUG를 피한다.

## 48.2 거래로그

모든 거래를 PROCESSING으로 시작해 SUCCESS·FAIL·TIMEOUT·UNKNOWN으로 종료한다. ServiceId·거래코드·경과시간을 기록한다.

## 48.3 오류로그

표준 오류코드, 예외 유형, 원인 구간, 안전한 Stack 정보를 내부에 남긴다. 동일 오류의 중복 로그 폭증을 통제한다.

## 48.4 감사로그

권한·기준정보·다운로드·개인정보·관리자 변경처럼 민감 행위의 주체, 시각, 전후 값, 사유를 증적으로 남긴다.

## 48.5 GUID와 TraceId

GUID는 End-to-End, traceId는 내부 호출 흐름을 연결한다. MDC를 이용해 Web·Gateway·TCF·SQL 로그에 전파한다.

## 48.6 ServiceId로 검색하기

ServiceId와 시간·사용자·지점·상태로 범위를 좁히고 traceId로 Step Log와 Mapper ID를 연결한다.

## 48.7 개인정보를 로그에 남기지 않기

비밀번호·Token·주민번호·계좌 원문과 전체 전문을 금지한다. 필드별 마스킹과 로그 보관·접근권한을 적용한다.

| **로그** | **목적** | **핵심 키** |
| --- | --- | --- |
| Access | HTTP 진입 | GUID·URI·status |
| Transaction | 거래 처리 | ServiceId·status·elapsed |
| Step/SQL | 구간 성능 | traceId·Mapper ID |
| Error | 오류 진단 | errorCode·traceId |
| Audit | 민감 행위 증적 | user·before/after |

## 장 요약

-   거래로그와 감사로그의 목적을 구분한다.
-   GUID·traceId·ServiceId로 전 구간을 연결한다.
-   민감정보와 Token 원문을 기록하지 않는다.

## 근거 자료

-   zman/15-거래로그-감사로그.md
-   znsight-man/34-로그-작성-기준.md

**CHAPTER 49**

**TCF-OM으로 운영 상태 확인하기**

> **이번 장에서 해결할 질문** 운영 기준정보와 실시간 상태를 어디에서 확인하고 변경해야 하는가?

## 49.1 Service Catalog 조회

ServiceId 존재·활성·Handler·거래코드와 배포 상태를 확인한다. 신규 거래는 Catalog만이 아니라 통제·Timeout·권한도 함께 등록한다.

## 49.2 거래통제 변경

Header 조합의 Allow-List를 조회·변경한다. 긴급 중지와 재개는 승인·사유·변경 전후·캐시 반영을 감사한다.

## 49.3 Timeout 정책 확인

ServiceId별 Online·DB·외부 호출 정책과 실제 적용값을 비교한다. 설정 파일과 OM 값의 우선순위를 확인한다.

## 49.4 실행 중 거래

PROCESSING 거래의 시작시각·인스턴스·Thread·traceId를 보고 장기 실행과 orphan을 식별한다.

## 49.5 Slow ServiceId

p95·p99·처리량·오류율을 ServiceId별로 비교한다. 평균값만으로 피크 지연을 숨기지 않는다.

## 49.6 Slow SQL

Step Log와 Mapper ID, DB 세션·실행계획을 연결한다. SQL 자체와 Pool 대기 시간을 구분한다.

## 49.7 운영 변경 감사

사용자·권한·통제·Timeout·오류코드·캐시 변경은 모두 감사 대상이며 업무 WAR가 직접 DB를 수정하지 않는다.

| **운영 질문** | **OM 진입점** | **후속 확인** |
| --- | --- | --- |
| 거래가 등록됐나? | Service Catalog | Registry·배포 |
| 왜 차단됐나? | Transaction Control | Header 7 |
| 왜 시간초과인가? | Timeout Policy | Step·SQL |
| 누가 바꿨나? | Audit Log | 승인·전후 값 |
| 어디가 느린가? | Dashboard/Tx Log | JVM·Pool·DB |

## 장 요약

-   OM은 운영 기준정보의 원장이다.
-   Catalog·통제·Timeout·권한을 함께 본다.
-   모든 운영 변경은 감사와 캐시 정합성을 확인한다.

## 근거 자료

-   znsight-man/46-OM-운영관리-개발.md
-   zman/12-OM운영관리.md

**CHAPTER 50**

**장애 원인 찾기**

> **이번 장에서 해결할 질문** 거래가 느리거나 실패할 때 추측이 아니라 증거로 병목을 좁히는 순서는 무엇인가?

## 50.1 거래가 느릴 때 확인 순서

시간·ServiceId·traceId·영향 범위를 고정하고 Gateway→Tomcat→TCF Step→Pool→SQL→외부 호출 순으로 구간 시간을 비교한다.

## 50.2 Tomcat Thread 부족

Busy Thread, Queue, Thread Dump를 확인한다. 무작정 maxThreads를 늘리기 전에 무엇을 기다리는지 찾는다.

## 50.3 CPU·GC 문제

CPU·Load·GC Pause·Heap 추세와 배포 시점을 비교한다. Heap Dump와 프로파일링은 운영 영향과 보안을 고려해 수행한다.

## 50.4 DB Pool 대기

Active·Idle·Pending·Connection Timeout과 DB Active Session을 같은 시간축에서 본다. Pool 확대 전에 Slow SQL과 DB 상한을 확인한다.

## 50.5 Slow SQL

Mapper ID, 바인딩 조건, 실행계획, 실제 건수, Lock·I/O를 확인한다. 개발 DB의 빠른 재현만으로 종결하지 않는다.

## 50.6 외부 시스템 지연

Connect·Read 시간, 대상 오류율, Circuit 상태를 확인하고 호출 격리·Timeout·Fallback이 동작했는지 검증한다.

## 50.7 특정 WAR 자원 독점

같은 Tomcat의 WAR별 Thread·Heap·Pool·트래픽을 비교한다. 필요하면 인스턴스나 Tomcat을 분리해 장애 격리한다.

## 50.8 장애 보고서 작성

타임라인, 영향, 탐지, 직접 원인, 기여 요인, 임시조치, 근본조치, 재발방지, 담당·기한과 검증 증거를 기록한다.

> 1\. 시간·영향·ServiceId·traceId 확정
> 2\. 구간별 elapsed 비교
> 3\. Thread/CPU/GC/Pool/DB/외부 지표 대조
> 4\. 변경·배포·설정 이력 확인
> 5\. 가설을 로그·덤프·실행계획으로 검증

| **증거** | **알 수 있는 것** | **주의** |
| --- | --- | --- |
| 거래/Step 로그 | 느린 구간 | 누락·시계 차이 |
| Thread Dump | 현재 대기 지점 | 여러 시점 필요 |
| GC Log | 메모리 압력 | 원인 객체 별도 |
| Pool Metrics | 연결 대기 | DB 상태 병행 |
| 실행계획 | SQL 접근 경로 | 실제 건수 비교 |

## 제8부 운영 훈련

1.  Slow ServiceId 하나를 선택해 traceId부터 Mapper ID까지 추적한다.
2.  DB Pool Pending 증가 상황에서 Thread Dump와 SQL 실행시간을 비교한다.
3.  Timeout 거래가 FAIL인지 UNKNOWN인지 판정하고 재처리 절차를 작성한다.
4.  OM 통제 변경 후 감사로그와 캐시 반영을 확인한다.
5.  가상 장애의 타임라인과 재발방지 조치를 작성한다.

## 장 요약

-   장애 분석은 범위와 시간축을 먼저 고정한다.
-   Thread·JVM·Pool·DB·외부 지표를 함께 본다.
-   보고서는 원인과 재발방지의 검증 책임까지 포함한다.

## 근거 자료

-   zman/14-Timeout관리.md
-   zman/15-거래로그-감사로그.md
-   znsight-capacity-word/NSIGHT\_OOM\_장애대응\_종합\_가이드.docx

# 제9부. 테스트·품질·개발방법론

제9부에서는 계층별 테스트를 전체 거래 검증으로 연결하고 성능·보안·장애 시험과 자동 품질 Gate를 적용한다. 마지막으로 프로젝트의 요구사항부터 운영전환까지 Architecture Gate로 의사결정하는 흐름을 정리한다.

**CHAPTER 51**

**단위테스트와 통합테스트**

> **이번 장에서 해결할 질문** 각 계층의 책임과 한 건의 전체 거래를 어떤 테스트 조합으로 검증하는가?

## 51.1 테스트가 필요한 이유

테스트는 정상 결과만 확인하는 작업이 아니라 변경 시 깨진 계약과 책임 침범을 조기에 찾는 장치다. 요구사항·설계·ServiceId·코드·SQL·OM 기준정보를 실행 가능한 증적으로 연결한다.

## 51.2 Rule 단위테스트

DB와 Spring Context 없이 경계값·상태 전이·권한 범위·오류코드를 검증한다. 같은 입력에 같은 결과를 내는 빠른 테스트를 만든다.

## 51.3 Service 테스트

Rule·DAO·Client를 Mock으로 두고 호출 순서, 정상·없음·업무 오류·기술 오류·연계 오류 분기를 확인한다.

## 51.4 Mapper 테스트

Test DB에서 Interface·XML·파라미터·resultMap·동적 SQL을 검증한다. 실행계획과 Timeout은 별도의 SQL 검증 결과로 남긴다.

## 51.5 거래 통합테스트

POST /{businessCode}/online부터 STF·Dispatcher·업무계층·DB·ETF·거래로그까지 End-to-End로 확인한다.

## 51.6 테스트 데이터 관리

개인정보를 복제하지 않고 합성·마스킹 데이터를 사용한다. 시나리오별 초기상태와 정리 절차를 자동화해 반복 실행 가능하게 만든다.

| **테스트** | **격리/환경** | **핵심 검증** |
| --- | --- | --- |
| Rule | 순수 Java | 규칙·경계값 |
| Handler/Service | Mockito | 분기·협력 |
| Mapper | Test DB | SQL·매핑 |
| 통합 | Spring+DB | TCF 전체 흐름 |
| 회귀 | CI/CD | 기존 ServiceId 영향 |

> **테스트 피라미드** 느린 통합테스트만 늘리지 않는다. Rule·Service의 빠른 단위테스트를 넓게 두고 중요한 거래를 통합테스트로 연결한다.

## 장 요약

-   계층 책임에 맞는 테스트 종류를 선택한다.
-   Mapper와 End-to-End 거래는 실제 통합 환경에서 검증한다.
-   테스트 데이터와 초기상태를 재현 가능하게 관리한다.

## 근거 자료

-   znsight-man/54-단위-테스트-기준.md
-   znsight-man/55-통합-테스트-기준.md

**CHAPTER 52**

**오류·Timeout·보안 테스트**

> **이번 장에서 해결할 질문** 정상 경로보다 더 중요한 실패 경로를 어떻게 체계적으로 검증하는가?

## 52.1 필수값 누락

Header 필수값과 Body Validation을 분리해 누락·공백·길이·형식 오류가 올바른 코드와 필드 메시지로 반환되는지 확인한다.

## 52.2 업무 오류

미존재·중복·허용되지 않은 상태·데이터 범위를 재현하고 BusinessException이 표준 FAIL 응답과 거래로그로 연결되는지 확인한다.

## 52.3 DB 오류

접속 실패·제약 위반·Lock·SQL 오류를 주입하고 내부 SQL·StackTrace가 화면에 노출되지 않으며 트랜잭션이 Rollback되는지 확인한다.

## 52.4 Timeout

Online·Transaction·Query·Connection·외부 호출 시간을 의도적으로 초과시켜 TIMEOUT/UNKNOWN 상태와 자원 반환을 검증한다.

## 52.5 JWT 만료·위변조

만료·서명 위변조·잘못된 iss/aud·Denylist·Claim/Header 불일치 Token을 각각 거부하는지 확인한다.

## 52.6 권한 없음

메뉴 노출과 무관하게 ServiceId 기능권한과 지점·고객 데이터권한을 서버에서 차단하는지 확인한다.

## 52.7 중복 요청

같은 idempotencyKey로 동시·순차 재요청해 한 번만 변경되거나 기존 결과가 반환되는지 검증한다.

| **주입 오류** | **기대 결과** | **추가 확인** |
| --- | --- | --- |
| Header 누락 | HDR 오류 | Handler 미호출 |
| 업무 규칙 | 업무 FAIL | Rollback 정책 |
| DB 장애 | SYSTEM 오류 | Connection 반환 |
| Timeout | TIMEOUT/UNKNOWN | 멱등성·상태조회 |
| JWT 위변조 | AUTH 오류 | 원문 로그 없음 |
| 권한 없음 | AUTHZ 오류 | 감사로그 |

## 장 요약

-   실패 유형마다 기대 오류코드와 거래상태를 정의한다.
-   Timeout 후 자원 반환과 최종 상태를 확인한다.
-   보안 테스트는 우회 경로와 민감정보 노출까지 검사한다.

## 근거 자료

-   znsight-man/55-통합-테스트-기준.md
-   znsight-man/42-보안-코딩-기준.md
-   zman/14-Timeout관리.md

**CHAPTER 53**

**성능과 장애 테스트**

> **이번 장에서 해결할 질문** 정상 피크부터 구성요소 장애까지 운영 목표를 수치와 증적으로 검증하려면 어떻게 하는가?

## 53.1 사용자와 동시요청

로그인 사용자 수와 실제 동시 요청은 다르다. 업무별 Think Time·세션·요청 분포를 모델링한다.

## 53.2 TPS와 응답시간

평균뿐 아니라 p95·p99, 처리량, 오류율, Timeout, CPU·GC·Thread·Pool을 같은 시간축에서 측정한다.

## 53.3 정상 피크

예상 피크 부하를 충분한 시간 유지해 자원 누적, Pool 대기, GC 증가, SLA 충족 여부를 검증한다.

## 53.4 스트레스 테스트

부하를 단계적으로 올려 포화점과 병목, 오류 형태, 복구 시간을 찾는다. 한계치를 운영 목표로 사용하지 않고 안전 여유를 둔다.

## 53.5 서버 장애 테스트

업무 인스턴스·Tomcat·Gateway를 중지해 트래픽 우회, 세션, 진행 거래, Health Check와 자동 복구를 확인한다.

## 53.6 DB 장애 테스트

Connection 실패·지연·Failover에서 Pool 재연결, Query Timeout, 거래상태, 데이터 정합성을 검증한다.

## 53.7 DR 전환 테스트

RTO·RPO, DNS/L4·애플리케이션·DB·배치·파일·Secret 전환과 복귀 절차를 실제 시간으로 측정한다.

| **시험** | **목표** | **주요 결과** |
| --- | --- | --- |
| Baseline | 단건 정상 | 구간 시간 |
| Peak | 예상 최대 | SLA·자원 여유 |
| Stress | 포화점 | 병목·보호동작 |
| Soak | 장시간 | 누수·누적 |
| Failover | 구성요소 장애 | 복구시간·정합성 |
| DR | 센터 전환 | RTO·RPO |

> **성능 결과의 조건** TPS 숫자만 기록하지 않는다. 데이터량, SQL, 서버 수, Core·Heap·Thread·Pool, 부하 분포, 측정 시간과 오류율이 함께 있어야 재현 가능한 증적이다.

## 장 요약

-   사용자 수를 업무별 동시요청 모델로 변환한다.
-   응답시간과 자원 지표를 함께 측정한다.
-   장애·DR 시험은 복구시간과 데이터 정합성까지 확인한다.

## 근거 자료

-   znsight-capacity-word 용량·Tomcat·JVM·Pool 자료
-   znsight-man/55-통합-테스트-기준.md

**CHAPTER 54**

**코드 품질과 자동검증**

> **이번 장에서 해결할 질문** 사람의 주의에만 의존하지 않고 표준 위반을 Merge 전에 차단하려면 무엇을 자동화하는가?

## 54.1 코드리뷰

변경 목적·영향 범위·테스트·설정·DB·OM·보안·운영성을 검토한다. 스타일 지적보다 책임 경계와 장애 위험을 우선한다.

## 54.2 명명규칙 검사

패키지·클래스·ServiceId·거래코드·Mapper·SQL ID·DB 객체의 형식을 Lint와 정규식으로 검사한다.

## 54.3 ArchUnit 구조검사

entry→application→persistence 의존 방향, Controller 추가 금지, Rule의 DAO 접근 금지, 다른 WAR 직접 참조 금지를 코드로 고정한다.

## 54.4 ServiceId 중복검사

전체 Handler의 serviceIds(), OM Catalog, 거래코드와 통제·Timeout Seed를 수집해 중복·누락·불일치를 빌드에서 차단한다.

## 54.5 Mapper·SQL 정합성 검사

Interface·XML namespace/id, resultMap, SQL ID 주석, SELECT \*, ${}, Paging 누락과 금지 DB 접근을 검사한다.

## 54.6 CI/CD 품질 Gate

validate→compile→test→quality→package 순서로 실행하고 실패한 Gate의 Artifact는 배포하지 않는다. 예외는 승인·만료·보완 계획을 남긴다.

> validate → compile → unit test → mapper validation
> → ArchUnit/ServiceId check → SAST/SCA/Secret scan
> → coverage/quality gate → bootWar → artifact publish

| **자동검사** | **차단 대상** | **증적** |
| --- | --- | --- |
| Unit/Integration | 기능 회귀 | Test report |
| ArchUnit | 계층 위반 | Rule report |
| ServiceId Scan | 중복·Catalog 누락 | 정합성 목록 |
| SQL Lint | SELECT \*·${}·Paging | 검사 결과 |
| SAST/SCA | 취약 코드·의존성 | 보안 보고서 |
| Secret Scan | 키·비밀번호 | 차단 로그 |

## 장 요약

-   반복 가능한 표준은 자동검사로 전환한다.
-   코드·OM·SQL의 식별자 정합성을 함께 검사한다.
-   품질 Gate 실패 Artifact는 배포하지 않는다.

## 근거 자료

-   znsight-man/54-단위-테스트-기준.md
-   znsight-man/65-CICD-파이프라인-기준.md

**CHAPTER 55**

**프로젝트는 어떤 순서로 진행하는가**

> **이번 장에서 해결할 질문** 요구사항부터 운영전환까지 어떤 의사결정과 증적을 순서대로 완성해야 하는가?

## 55.1 요구사항

기능 요구와 함께 응답시간·용량·보안·가용성·DR·운영 KPI를 정량화하고 추적 ID를 부여한다.

## 55.2 도메인과 업무 경계

업무코드·WAR·데이터 소유권·서비스 간 계약을 정한다. 직접 DB·Java 참조를 허용하지 않는 경계를 합의한다.

## 55.3 목표 아키텍처

사용자부터 Gateway·업무 WAR·TCF·DB·OM까지 목표 구조와 ADR, 품질속성 대응을 확정한다.

## 55.4 상세설계

화면 이벤트, ServiceId, 거래코드, 전문, 6계층 프로그램, SQL·DB, OM 기준정보, 테스트케이스를 연결한다.

## 55.5 선도개발

대표 조회·변경·연계 거래를 End-to-End로 구현해 표준·환경·배포·로그·성능의 실행 가능성을 검증한다.

## 55.6 본 개발

검증된 템플릿과 자동 Gate로 업무를 확장하고 설계·코드·OM·시험의 정합성을 지속 확인한다.

## 55.7 통합·성능·보안 시험

운영 유사 환경에서 정상·오류·부하·장애·권한·개인정보·DR 시나리오와 품질 목표를 검증한다.

## 55.8 운영전환

Cutover·데이터·배포·Smoke·모니터링·비상연락·Rollback·Go/No-Go 기준을 준비하고 리허설한다.

## 55.9 Architecture Gate

G0~G14에서 다음 단계 진입 가능성을 증적으로 판정한다. Blocker·Critical은 원칙적으로 다음 단계 진행을 막는다.

| **Gate 구간** | **핵심 질문** | **대표 증적** |
| --- | --- | --- |
| G0~G4 | 범위·요구·책임 확정? | RACI·Baseline |
| G5~G7 | 개발 가능한 설계? | ADR·상세설계 |
| G8~G10 | 용량·배포·선도 검증? | 산정·CI/CD·E2E |
| G11~G12 | 품질 목표 충족? | Gate·시험 결과 |
| G13~G14 | 전환·안정화 가능한가? | Runbook·RCA |

## 제9부 완료 체크

1.  요구사항-설계-ServiceId-코드-SQL-테스트-OM 추적성을 확인한다.
2.  정상·오류·Timeout·보안·중복 요청 자동 테스트를 실행한다.
3.  성능 결과에 조건·데이터·자원·p95/p99·오류율을 기록한다.
4.  CI/CD Gate 실패 시 배포가 차단되는지 확인한다.
5.  Architecture Gate의 필수 증적과 미조치 결함을 정리한다.

## 장 요약

-   프로젝트 단계마다 다음 단계 진입 조건을 명확히 한다.
-   선도개발로 표준의 실행 가능성을 먼저 검증한다.
-   Architecture Gate는 문서 유무가 아니라 운영 가능성을 증적으로 판정한다.

## 근거 자료

-   NSIGHT TCF 아키텍처 구축 방법론 - Architecture Gate 단계별 체크리스트.md
-   znsight-구축방법론 자료
-   znsight-man/65-CICD-파이프라인-기준.md

# 제10부. 처음부터 끝까지 만드는 실전 프로젝트

제10부에서는 앞서 배운 모든 기준을 고객 종합정보 조회와 고객 메모 CRUD에 적용한다. 화면 이벤트에서 시작해 ServiceId, 6계층 코드, SQL, 보안, OM, 배포, 테스트, 설계서까지 하나의 추적 가능한 결과물로 완성한다.

**CHAPTER 56**

**고객 종합정보 조회 설계**

> **이번 장에서 해결할 질문** 코딩 전에 화면·거래·프로그램·DB·시험을 하나의 추적 가능한 설계로 어떻게 연결하는가?

## 56.1 화면과 이벤트 정의

화면 ID SV-CUST-001에 조회 조건, 고객요약 영역, 상품목록, 메모 영역을 배치한다. 초기화·검색·상세조회·메모 저장 이벤트와 입력·출력·오류 동작을 정의한다.

## 56.2 ServiceId와 거래코드 정의

고객요약은 SV.Customer.selectSummary/SV-INQ-0001, 목록은 SV.Customer.selectList/SV-INQ-0002로 정의한다. 메모 등록·변경·삭제는 별도 ServiceId와 변경 거래코드를 부여한다.

## 56.3 요청·응답 전문 정의

Header에는 업무코드·ServiceId·거래코드·사용자·지점·채널·추적 ID, Body에는 고객번호·기준일·검색조건만 둔다. 성공·업무 오류·권한 오류·Timeout 응답 예시를 함께 설계한다.

## 56.4 프로그램 목록 정의

SvCustomerHandler, Facade, Service, Rule, DAO, Mapper와 Request·Criteria·Result·Response DTO를 프로그램 목록에 기록하고 각 책임을 한 줄로 정의한다.

## 56.5 SQL과 테이블 정의

RDW\_CUSTOMER\_SUMMARY 조회 컬럼·인덱스 조건과 SV\_CUSTOMER\_MEMO의 PK·논리삭제·감사 컬럼을 정의한다. SQL ID와 ServiceId 연결을 기록한다.

## 56.6 테스트 조건 정의

정상·필수값·미존재·지점권한·Paging·중복·DB 오류·Timeout·마스킹·거래로그 시나리오와 기대 오류코드를 설계 단계에서 확정한다.

| **설계 요소** | **고객요약 예시** | **추적 대상** |
| --- | --- | --- |
| 화면 이벤트 | searchSummary | 화면설계 |
| ServiceId | SV.Customer.selectSummary | Handler·Catalog |
| 거래코드 | SV-INQ-0001 | 로그·감사 |
| SQL ID | SV.Customer.selectSummary | Mapper·APM |
| Timeout | 3초 | OM Policy |
| Test ID | IT-SV-001~ | 결과서 |

> **코딩 시작 조건** 화면 이벤트→ServiceId→Handler→SQL→OM→테스트가 한 줄로 이어지지 않으면 아직 개발 가능한 상세설계가 아니다.

## 장 요약

-   구현 전에 식별자와 계약을 확정한다.
-   프로그램·SQL·테이블의 책임과 관계를 기록한다.
-   실패 시나리오를 설계 단계에서 정의한다.

## 근거 자료

-   znsight-man/71-SV-고객요약조회-샘플.md
-   znsight-man/72-목록조회-페이징-샘플.md

**CHAPTER 57**

**조회 거래 구현**

> **이번 장에서 해결할 질문** 고객번호 한 건이 Handler부터 RDW 조회와 표준 응답까지 통과하도록 어떻게 구현하는가?

## 57.1 Handler 생성

SvCustomerHandler에 ServiceId를 등록하고 Body를 SvCustomerSummaryRequest로 변환해 Facade를 호출한다. 별도 업무 Controller는 만들지 않는다.

## 57.2 Facade 생성

readOnly=true와 3초 Timeout의 거래 경계를 선언하고 Service 결과를 Response로 반환한다.

## 57.3 Service 생성

Rule로 요청과 지점 범위를 검증하고 DAO 결과가 없으면 E-SV-BIZ-0001 업무 오류를 발생시킨다.

## 57.4 Rule 생성

고객번호 형식, 기준일, 데이터권한과 조회 가능 조건을 부작용 없는 규칙으로 구현한다.

## 57.5 DAO·Mapper 생성

Criteria를 Mapper에 전달하고 필요한 컬럼만 PK·인덱스 조건으로 조회한다. namespace·id·SQL ID 주석과 ResultMap을 일치시킨다.

## 57.6 화면 호출 및 결과 확인

POST /sv/online 요청 후 result와 body를 표시하고 traceId로 거래로그·Step Log·SQL 시간을 확인한다.

> POST /sv/online
> serviceId = SV.Customer.selectSummary
> transactionCode = SV-INQ-0001
> body = { "customerNo": "CUST00000001", "baseDate": "20260705" }
> Handler → Facade(readOnly, 3s) → Service → Rule → DAO → Mapper → RDW

| **완료 검사** | **기대** |
| --- | --- |
| 정상 고객 | SUCCESS+마스킹 응답 |
| 고객번호 누락 | Validation 오류 |
| 고객 없음 | E-SV-BIZ-0001 |
| 다른 지점 | 데이터권한 오류 |
| 느린 SQL | TIMEOUT·자원 반환 |
| 로그 | traceId→Mapper ID 연결 |

## 장 요약

-   조회 거래도 6계층 책임을 지킨다.
-   Response는 DB Result를 직접 노출하지 않는다.
-   응답과 거래·SQL 로그를 함께 검증한다.

## 근거 자료

-   znsight-man/71-SV-고객요약조회-샘플.md
-   ztcfbook/제08편/22-조회-거래-SV-고객요약.md

**CHAPTER 58**

**등록·변경·삭제 거래 구현**

> **이번 장에서 해결할 질문** 고객 메모의 상태를 안전하게 바꾸고 중복·동시성·감사를 통제하려면 어떻게 하는가?

## 58.1 고객 메모 등록

SV.CustomerMemo.create 요청을 검증하고 Command에 고객번호·메모·등록자·지점·idempotencyKey를 담아 INSERT한다. 생성 키와 영향 1건을 확인한다.

## 58.2 고객 메모 변경

현재 메모와 소유·상태를 조회하고 수정 가능 여부를 Rule로 확인한다. 버전 또는 수정시각 조건으로 Lost Update를 방지한다.

## 58.3 고객 메모 삭제

기본은 USE\_YN='N' 논리삭제로 구현한다. 이미 삭제됨·미존재·다른 작성자·보관 정책을 구분한다.

## 58.4 트랜잭션과 Rollback

Facade에서 메모 본문·이력·감사 대상 데이터를 하나의 트랜잭션으로 묶고 중간 실패 시 전체 Rollback한다.

## 58.5 중복 요청 방지

같은 idempotencyKey가 재전송되면 두 번 등록하지 않는다. 진행·성공·실패 상태와 기존 응답 재사용 정책을 둔다.

## 58.6 감사로그 기록

등록·변경·삭제의 주체, 고객 식별자의 마스킹 값, 변경 전후, 사유, ServiceId, traceId를 기록하되 메모 민감 원문은 정책에 맞게 제한한다.

| **거래** | **ServiceId 예시** | **핵심 통제** |
| --- | --- | --- |
| 등록 | SV.CustomerMemo.create | 중복·영향 1건 |
| 변경 | SV.CustomerMemo.update | 버전·상태 전이 |
| 삭제 | SV.CustomerMemo.delete | 논리삭제·참조 |
| 조회 | SV.CustomerMemo.selectList | 권한·Paging |

> **재시도 안전성** Timeout 응답 뒤 무조건 다시 저장하지 않는다. idempotencyKey로 기존 처리 결과를 확인하고 UNKNOWN이면 상태 조회 후 재처리한다.

## 장 요약

-   변경 전 현재 상태와 권한을 검증한다.
-   Facade 트랜잭션에서 영향 건수와 Rollback을 통제한다.
-   멱등성과 감사로그를 변경 거래의 일부로 구현한다.

## 근거 자료

-   znsight-man/73-등록변경-거래-샘플.md
-   znsight-man/36-트랜잭션-기준.md
-   znsight-man/38-Idempotency-중복요청.md

**CHAPTER 59**

**보안과 오류 처리 추가**

> **이번 장에서 해결할 질문** 기능 구현 후가 아니라 거래 계약 안에 보안·권한·마스킹·오류·Timeout을 어떻게 포함하는가?

## 59.1 JWT 검증

Gateway와 WAR Filter에서 서명·iss·aud·시간·Denylist를 확인하고 STF에서 Claim의 userId·branchId를 Header와 비교한다.

## 59.2 데이터권한 확인

고객 소유 지점·사용자 권한 범위를 Rule과 SQL Criteria에 반영한다. 화면에서 숨겼다는 이유로 서버 검증을 생략하지 않는다.

## 59.3 개인정보 마스킹

권한과 사용 목적에 따라 고객명·연락처·계좌를 Response 변환 단계에서 마스킹하고 로그·다운로드에도 동일 정책을 적용한다.

## 59.4 업무 오류 처리

필수값·미존재·중복·상태·권한 오류에 안정된 코드와 사용자 메시지를 부여한다. 내부 예외 문자열을 직접 반환하지 않는다.

## 59.5 Timeout 처리

ServiceId 3초, Transaction·Query·외부 호출 Timeout의 계층을 맞추고 Timeout 후 거래상태와 자원 반환을 확인한다.

## 59.6 표준 오류 응답

ETF가 result·error·traceId를 조립하고 화면은 errorType·retryable을 기준으로 재입력·재로그인·문의·상태조회 동작을 선택한다.

> {
> "result": { "resultCode": "FAIL", "message": "고객 정보를 조회할 권한이 없습니다." },
> "error": { "errorCode": "E-SV-AUTH-0001", "errorType": "AUTHORIZATION",
> "retryable": false },
> "header": { "traceId": "TRACE-20260705-000101" }
> }

| **실패** | **화면 행동** | **운영 행동** |
| --- | --- | --- |
| Validation | 입력 수정 | 필요 시 통계 |
| Business | 업무 안내 | 코드·빈도 확인 |
| Authentication | 재로그인 | Token/세션 확인 |
| Authorization | 권한 안내 | 감사 확인 |
| Timeout/Unknown | 상태 조회 | 구간·최종상태 |
| System | 문의+traceId | RCA |

## 장 요약

-   JWT와 Header를 교차검증한다.
-   데이터권한과 마스킹을 응답·로그·파일에 일관 적용한다.
-   오류 유형별 화면·운영 행동을 정의한다.

## 근거 자료

-   znsight-man/41-JWT-SSO-연계.md
-   znsight-man/42-보안-코딩-기준.md
-   znsight-man/33-오류코드-메시지-기준.md

**CHAPTER 60**

**배포와 운영 확인**

> **이번 장에서 해결할 질문** 완성된 거래를 공식 WAR로 배포하고 OM과 로그에서 운영 가능 상태를 어떻게 확인하는가?

## 60.1 WAR 빌드

CI/CD에서 compile·test·quality Gate를 통과한 sv.war와 checksum·Commit ID·build-info를 Artifact 저장소에 게시한다.

## 60.2 Tomcat 배포

대상 인스턴스를 트래픽에서 제외하고 WAR·외부 설정을 반영한 뒤 Liveness·Readiness·Deep Check를 통과시킨다.

## 60.3 OM 기준정보 등록

Service Catalog에 ServiceId·거래코드·Handler·업무코드·활성 상태를 등록하고 기능·데이터 권한과 오류코드를 연결한다.

## 60.4 거래통제·Timeout 등록

허용 Header 조합과 ServiceId별 Online Timeout, 감사·Idempotency 정책을 등록하고 캐시 Evict/Reload를 확인한다.

## 60.5 거래로그 확인

정상·오류 거래의 PROCESSING→최종 상태, elapsed, user·branch·channel, traceId와 Mapper ID 연결을 확인한다.

## 60.6 장애 상황 재현

DB 지연·권한 없음·미등록 ServiceId·Timeout·인스턴스 중지에서 표준 오류, 격리, Alert, 복구가 동작하는지 시험한다.

## 60.7 롤백

오류율·Health·업무 Smoke가 기준을 벗어나면 직전 WAR·설정·OM 기준정보를 순차 복구하고 재검증한다.

| **전환 확인** | **통과 기준** | **증적** |
| --- | --- | --- |
| Artifact | 공식 Runner·Hash | 빌드 기록 |
| Health | Live/Ready/Deep | Check 결과 |
| OM | Catalog·통제·Timeout | 등록 화면 |
| Smoke | 조회·CRUD 정상 | 거래로그 |
| Monitoring | 지표·Alert 수신 | Dashboard |
| Rollback | 직전본 복구 가능 | 리허설 결과 |

## 장 요약

-   공식 Artifact와 외부 설정만 운영에 반영한다.
-   OM 기준정보와 캐시까지 배포 범위에 포함한다.
-   Smoke·모니터링·롤백 검증 후 전환을 완료한다.

## 근거 자료

-   znsight-man/66-배포-절차.md
-   znsight-man/67-롤백-절차.md
-   znsight-man/68-운영-전환-체크리스트.md

**CHAPTER 61**

**설계서와 산출물 완성하기**

> **이번 장에서 해결할 질문** 구현된 거래를 다른 개발자와 운영자가 이해·검증·복구할 수 있는 공식 산출물로 어떻게 남기는가?

## 61.1 화면설계서

화면 ID, 이벤트, 입력·출력, Validation, 권한, 마스킹, 오류 메시지와 호출 ServiceId를 기록한다.

## 61.2 거래설계서

ServiceId·거래코드·Header·Body·Timeout·통제·Idempotency·감사·오류코드와 처리 흐름을 정의한다.

## 61.3 프로그램설계서

Handler부터 Mapper까지 클래스·메서드·책임·호출 관계와 트랜잭션 경계, 외부 Client를 기록한다.

## 61.4 SQL·DB 설계서

테이블·컬럼·키·인덱스·SQL ID·Mapper·실행계획·예상 건수·Query Timeout과 데이터 보관 정책을 기록한다.

## 61.5 추적성 매트릭스

요구사항→화면 이벤트→ServiceId→거래코드→프로그램→SQL·테이블→OM→테스트→배포 Artifact를 한 행으로 연결한다.

## 61.6 테스트 결과서

환경·버전·데이터·절차·기대·실제·로그·결함·재시험을 남기고 성능 시험은 부하와 자원 조건을 포함한다.

## 61.7 운영 체크리스트

기동·중지·Health·로그·OM·Alert·배포·롤백·장애 연락·RCA·정기 점검과 담당자를 운영 Runbook으로 만든다.

| **산출물** | **최소 식별자** | **승인/사용자** |
| --- | --- | --- |
| 화면설계 | 화면·이벤트·ServiceId | 업무·UI |
| 거래설계 | ServiceId·거래코드 | AA·개발 |
| 프로그램설계 | 클래스·메서드 | 개발·리뷰 |
| DB/SQL | 객체·SQL ID | DA·DBA |
| 시험결과 | Test ID·버전 | QA·승인자 |
| Runbook | 배포 ID·담당 | 운영 |

> **최종 목표** 좋은 산출물은 코드를 다시 설명하는 문서가 아니다. 요구사항에서 운영 로그와 롤백까지 같은 거래를 추적하고 다음 변경의 영향을 판단하게 하는 기준본이다.

## 전체 실전 프로젝트 완료 체크

1.  고객요약·목록·메모 CRUD의 정상과 실패 시나리오를 통과했다.
2.  ServiceId·거래코드·Handler·SQL ID·OM·Test ID가 추적성 매트릭스에서 연결된다.
3.  개인정보 마스킹·권한·JWT·Timeout·Idempotency가 적용됐다.
4.  공식 WAR 배포·Health·Smoke·모니터링·롤백을 검증했다.
5.  개발·테스트·운영 담당자가 산출물 기준본을 승인했다.

## 장 요약

-   산출물은 거래 전체의 추적성을 제공해야 한다.
-   테스트 결과에는 환경·버전·데이터와 증거를 포함한다.
-   운영 Runbook과 롤백 검증까지 완료해야 개발이 끝난다.

## 근거 자료

-   znsight-man/68-운영-전환-체크리스트.md
-   NSIGHT TCF 아키텍처 구축 방법론 - Architecture Gate 단계별 체크리스트.md

# 맺음말

이 책의 목표는 표준을 외우는 개발자가 아니라 자신이 만든 거래가 화면에서 DB까지 어떻게 실행되고, 보안·Timeout·로그·OM·배포·장애 대응으로 어떻게 운영되는지 설명할 수 있는 개발자를 만드는 것이다. 이제 독자는 한 건의 ServiceId를 설계하고 구현하고 검증하고 운영 산출물로 완성하는 전체 흐름을 갖추었다.

# 부록 A. 핵심 용어집

| **용어** | **정의** |
| --- | --- |
| TCF | 온라인 거래의 전처리·실행·후처리와 통제·로그를 표준화하는 프레임워크 |
| STF | Header·인증·권한·거래통제·Timeout을 검증하는 공통 전처리 |
| ETF | 업무 결과·예외를 표준 응답으로 만들고 거래로그를 종료하는 후처리 |
| ServiceId | Dispatcher가 실행 Handler를 찾는 {업무}.{도메인}.{행위} 형식의 키 |
| 거래코드 | 거래로그·감사·통계에서 사용하는 운영 식별자 |
| TransactionContext | 검증된 사용자·지점·채널·추적 정보를 업무 계층에 전달하는 문맥 |
| Handler | ServiceId를 Facade 유스케이스로 연결하는 진입 Adapter |
| Facade | 거래 단위 흐름과 트랜잭션 경계 |
| Service | 도메인 업무 처리 순서를 조립하는 계층 |
| Rule | 부작용 없이 업무 검증·계산·상태 판단을 수행하는 계층 |
| DAO | Service와 Mapper 사이의 데이터 접근 경계 |
| Mapper | Java Interface와 XML SQL을 연결하는 MyBatis 계약 |
| OM | Service Catalog·통제·Timeout·권한·로그 등 운영 기준정보 원장 |
| GUID | 여러 시스템을 잇는 End-to-End 거래 추적 ID |
| traceId | 한 거래의 내부 호출과 Step·SQL을 잇는 추적 ID |
| Idempotency | 동일 변경 요청을 반복해도 결과가 중복되지 않게 하는 성질 |
| UNKNOWN | Timeout·연계 중단 등으로 최종 성공·실패를 확정할 수 없는 거래상태 |
| RDW | 실시간 단건·소량 조회 목적의 데이터 저장소 |
| ADW | 분석·집계·대량 조회 목적의 데이터 저장소 |
| Architecture Gate | 다음 단계 진입 가능성과 운영 가능성을 증적으로 판정하는 의사결정 절차 |

# 부록 B. 신규 거래 개발 완료 체크리스트

## 설계

-   □ 화면 이벤트와 ServiceId·거래코드가 연결됐다.
-   □ 요청·응답·오류·Timeout 계약이 정의됐다.
-   □ 프로그램·SQL·DB·테스트 목록이 추적된다.

## 코드

-   □ Handler→Facade→Service→Rule→DAO→Mapper 책임을 지킨다.
-   □ 다른 WAR의 Java·DB를 직접 참조하지 않는다.
-   □ DTO와 Header·DB Result가 목적별로 분리됐다.

## 데이터·성능

-   □ SELECT \*·무제한 목록·검증되지 않은 ${}가 없다.
-   □ Paging·인덱스·실행계획·Query Timeout을 확인했다.
-   □ 변경 영향 건수·Rollback·Idempotency를 확인했다.

## 보안

-   □ 세션/JWT Claim과 Header를 교차검증한다.
-   □ 기능·데이터 권한과 마스킹을 적용했다.
-   □ Token·비밀번호·개인정보 원문이 로그에 없다.

## 운영

-   □ Catalog·거래통제·Timeout·권한·오류코드를 OM에 등록했다.
-   □ 거래로그·감사로그·traceId·Mapper ID가 연결된다.
-   □ Health·Smoke·Alert·Rollback을 검증했다.

## 품질

-   □ 단위·통합·오류·Timeout·보안 테스트를 통과했다.
-   □ ServiceId·Mapper·ArchUnit·Secret Scan Gate를 통과했다.
-   □ 설계서·결과서·Runbook 기준본을 승인받았다.

# 부록 C. 추적성 매트릭스 작성 예시

추적성 매트릭스는 요구사항부터 운영 증적까지 같은 거래를 한 행으로 연결한다. 식별자 중 하나가 비어 있으면 설계·코드·OM·시험 중 어느 구간이 끊겼는지 확인한다.

| **요구사항** | **화면/이벤트** | **ServiceId·거래코드** | **프로그램·SQL** | **OM·시험** |
| --- | --- | --- | --- | --- |
| REQ-SV-001 고객요약 | SV-CUST-001/search | SV.Customer.selectSummary  <br>SV-INQ-0001 | SvCustomerHandler→Mapper  <br>SQL\_ID 동일 | Catalog·3초  <br>IT-SV-001 |
| REQ-SV-002 고객목록 | SV-CUST-001/searchList | SV.Customer.selectList  <br>SV-INQ-0002 | Count+List Mapper  <br>Paging | Catalog·3초  <br>IT-SV-010 |
| REQ-SV-003 메모등록 | SV-CUST-001/saveMemo | SV.CustomerMemo.create  <br>SV-INS-0001 | Memo Facade→INSERT  <br>Idempotency | 통제·감사·5초  <br>IT-SV-020 |

> **편집 메모** 페이지 번호는 Word에서 최종 조판 후 갱신한다. 현재 목차는 장 구성 검토를 위한 정적 목차이며 장 제목과 본문 구조를 기준으로 생성됐다.
