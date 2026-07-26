<!-- source: ztcf-집필본/NSIGHT TCF 개발 입문서 - 제20부. 코드 리뷰·리팩터링·품질개선 실전_재구성확장판_v3.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

나는 제20부. 코드 리뷰·리팩터링·품질개선 실전의 개념을 코드·설정·로그·산출물 중 어디에서 확인할 수 있는가? 문제가 발생했을 때 어느 경계부터 조사해야 하는가?

읽기 전 질문

① 장의 학습 목표를 먼저 읽습니다. ② 기존 설명과 예제를 따라갑니다. ③ 실무 적용 절차를 자신의 프로젝트에 대입합니다. ④ 완료 기준으로 이해 여부를 확인합니다.

권장 학습법

개발환경 구축부터 기존 코드 분석, 신규 CRUD, 코드 리뷰까지 혼자 수행하는 흐름을 완성합니다.

이 부의 학습 좌표

RESTRUCTURED & EXPANDED EDITION

6단계 · 독립 수행

**초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서**

# **제20부. 코드 리뷰·리팩터링·품질개선 실전**

# **1\. 도입 전 안내말**

제19부에서는 화면과 테이블 정보만 제공된 상황에서 신규 CRUD 기능을 다음 흐름으로 설계하고 구현했습니다.

요구사항

→ 화면 이벤트

→ ServiceId

→ Handler

→ Facade

→ Service

→ Rule

→ DAO

→ Mapper

→ SQL

→ DB

→ 테스트

→ OM·운영

개발자가 기능 구현을 완료하면 다음과 같이 판단하기 쉽습니다.

화면에서 조회가 된다.

등록과 수정이 된다.

SQL 오류가 없다.

단위 테스트가 성공한다.

따라서 개발이 끝났다.

하지만 운영 가능한 프로그램의 품질은 단순히 정상 동작 여부만으로 판단할 수 없습니다.

다음 코드는 현재 정상적으로 동작할 수 있습니다.

public ReservationResponse save(
ReservationRequest request) {

String userId = request.getUserId();

if ("CREATE".equals(request.getActionType())) {
mapper.insert(request);
} else if ("UPDATE".equals(request.getActionType())) {
mapper.update(request);
} else {
mapper.delete(request.getReservationId());
}

return new ReservationResponse("SUCCESS");
}

그러나 이 코드에는 다음과 같은 위험이 숨어 있습니다.

화면에서 전달한 사용자 ID를 신뢰한다.

등록·수정·삭제가 하나의 메서드에 혼합되어 있다.

상태와 Version을 검증하지 않는다.

영향 건수를 확인하지 않는다.

물리 삭제를 수행한다.

이력을 남기지 않는다.

트랜잭션 경계가 불명확하다.

권한·Timeout·감사 처리와 연결되지 않는다.

오류가 발생해도 항상 SUCCESS를 반환할 가능성이 있다.

코드 리뷰는 문법 오류를 찾는 활동이 아닙니다.

코드 리뷰
\= 구현된 기능이 요구사항·아키텍처·보안·데이터·운영 기준을
실제로 만족하는지 검증하는 활동

리팩터링도 단순히 코드를 예쁘게 정리하는 작업이 아닙니다.

리팩터링
\= 외부 동작을 유지하면서
구조·책임·가독성·테스트 가능성·운영성을 개선하는 작업

NSIGHT TCF의 코드 리뷰에서는 최소한 다음을 확인해야 합니다.

ServiceId의 목적이 명확한가?

Handler가 업무로직을 수행하지 않는가?

Facade가 트랜잭션 경계를 담당하는가?

Service의 책임이 지나치게 크지 않은가?

Rule이 업무검증을 분리하는가?

Request의 사용자정보를 신뢰하지 않는가?

SQL에 데이터권한과 Version 조건이 있는가?

Timeout과 외부 호출이 적절한가?

오류를 숨기지 않는가?

운영에서 ServiceId와 SQL을 추적할 수 있는가?

테스트가 정상뿐 아니라 실패를 검증하는가?

변경이 다른 업무 WAR에 미치는 영향을 확인했는가?

제20부의 핵심 원칙은 다음과 같습니다.

코드 리뷰는 취향 검토가 아니라 표준과 위험 검토다.

리뷰 의견에는 근거와 예상 영향을 포함한다.

동작하는 코드라도 책임경계가 틀리면 수정한다.

리팩터링과 기능 변경을 가능한 한 분리한다.

테스트 없이 대규모 리팩터링을 수행하지 않는다.

운영 장애와 보안사고 가능성이 높은 항목을 우선 검토한다.

반복 지적사항은 사람의 주의가 아니라 자동 Gate로 전환한다.

# **2\. 제20부 개요**

## **2.1 목적**

제20부의 목적은 NSIGHT TCF 프로그램에 대한 코드 리뷰 기준을 수립하고, 기존 기능을 안전하게 리팩터링하며, 반복 가능한 품질 개선체계를 구축하는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

1.  코드 리뷰의 목적과 범위를 정의한다.
2.  기능 변경과 리팩터링을 구분한다.
3.  요구사항과 Source 변경의 정합성을 검토한다.
4.  Handler·Facade·Service·Rule·DAO·Mapper 책임을 검토한다.
5.  트랜잭션·예외·Timeout 구조를 검토한다.
6.  인증·권한·개인정보·감사 기준을 검토한다.
7.  SQL·DB·동시성·영향 건수를 검토한다.
8.  성능·메모리·Thread·DB Pool 위험을 확인한다.
9.  테스트의 품질과 누락 시나리오를 검토한다.
10.  코드 중복·긴 메서드·거대 Service를 개선한다.
11.  안전한 단계로 리팩터링한다.
12.  리뷰 의견의 심각도와 처리상태를 관리한다.
13.  기술부채와 예외를 공식적으로 관리한다.
14.  반복 위반사항을 자동 품질 Gate로 전환한다.
15.  리뷰 결과를 설계서와 추적성에 반영한다.
16.  AI가 생성하거나 수정한 코드를 동일 기준으로 검증한다.

## **2.2 적용범위**

| **영역** | **주요 검토 대상** |
| --- | --- |
| 요구사항 | 기능·수용기준·누락 |
| 아키텍처 | 모듈·계층·책임경계 |
| 거래 | ServiceId·거래코드·Handler |
| 트랜잭션 | Facade·Rollback·외부 호출 |
| 오류 | 업무·시스템·Timeout |
| 보안 | 인증·권한·개인정보 |
| 데이터 | DTO·DAO·Mapper·SQL |
| 동시성 | Version·Lock·Idempotency |
| 성능 | SQL·객체·Thread·Pool |
| 테스트 | Unit·Integration·E2E |
| 운영 | Log·Metric·Alert·Runbook |
| 배포 | 호환성·Rollback |
| 유지보수 | 명명·중복·복잡도 |
| 자동화 | 정적분석·Architecture Test |

## **2.3 대상 독자**

-   처음 코드 리뷰를 수행하는 개발자
-   리뷰 의견을 어떻게 작성할지 어려운 개발자
-   동작하는 코드를 수정해야 하는 이유가 궁금한 개발자
-   거대 Service와 복잡한 조건문을 리팩터링해야 하는 개발자
-   SQL과 트랜잭션까지 포함해 검토하는 모듈 리더
-   공통 Framework 변경을 검토하는 아키텍트
-   코드 품질 Gate를 설계하는 QA·DevOps 담당자
-   AI가 생성한 소스의 품질을 검증하는 개발자

## **2.4 선행조건**

다음 내용을 이해하고 있어야 합니다.

ServiceId는 하나의 업무 목적을 식별한다.

Handler는 ServiceId 분기와 DTO 변환을 담당한다.

Facade는 유스케이스와 트랜잭션 경계를 담당한다.

Service는 업무 처리순서를 담당한다.

Rule은 검증과 계산을 담당한다.

DAO·Mapper는 데이터 접근을 담당한다.

AuthenticationContext는 신뢰 가능한 사용자정보를 제공한다.

OM은 거래정책과 관측정보를 관리한다.

## **2.5 주요 용어**

| **용어** | **설명** |
| --- | --- |
| Code Review | 소스 변경의 정확성·위험·표준을 검토 |
| Refactoring | 외부 동작을 유지하면서 내부구조 개선 |
| Code Smell | 잠재적 설계문제를 나타내는 코드 징후 |
| Technical Debt | 향후 수정비용을 증가시키는 기술적 부담 |
| Cyclomatic Complexity | 조건분기 기반 코드 복잡도 |
| Cohesion | 하나의 모듈이 관련 책임에 집중한 정도 |
| Coupling | 모듈 간 의존 정도 |
| Regression | 변경으로 기존 기능이 깨지는 현상 |
| Behavioral Compatibility | 외부 동작의 호환성 |
| Breaking Change | 기존 호출자와 호환되지 않는 변경 |
| Static Analysis | 실행하지 않고 코드 품질 분석 |
| Quality Gate | 기준 미달 변경을 자동 차단하는 검증점 |
| Blocking Comment | 해결 전 Merge할 수 없는 리뷰 의견 |
| Non-blocking Comment | 개선 권고 성격의 의견 |
| Suppression | 정적분석 경고를 예외로 무시하는 설정 |

# **제205장. 코드 리뷰의 목적과 우선순위**

학습 목표 | 205장. 코드 리뷰의 목적과 우선순위의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **205.1 코드 리뷰는 오류 찾기만이 아니다**

코드 리뷰의 목적은 다음과 같습니다.

요구사항 누락 발견

아키텍처 표준 검증

보안 위험 방지

데이터 정합성 검증

장애 확산 가능성 확인

테스트 누락 확인

운영 추적성 확보

지식 공유

기술부채 억제

## **205.2 검토 우선순위**

리뷰 시간은 제한되어 있으므로 위험도가 높은 순서로 검토합니다.

1\. 보안·개인정보

2\. 데이터 손상·중복·누락

3\. 트랜잭션·동시성

4\. 장애·Timeout·자원 고갈

5\. 외부 계약·호환성

6\. 아키텍처 책임경계

7\. 테스트 품질

8\. 가독성·스타일

변수명보다 데이터 손상 위험을 먼저 검토합니다.

## **205.3 코드가 동작해도 거절할 수 있는 경우**

권한 검증을 우회한다.

Request의 사용자 ID를 신뢰한다.

등록과 이력이 다른 트랜잭션이다.

수정 SQL에 Version 조건이 없다.

예외를 잡아 성공으로 반환한다.

무제한 대량조회를 허용한다.

운영 Secret을 Source에 포함한다.

다른 업무 Table을 승인 없이 직접 변경한다.

## **205.4 리뷰 범위**

리뷰 대상은 Java 파일에 한정되지 않습니다.

Java

Mapper XML

SQL

YAML

Gradle

DB Migration

테스트

OM Seed

권한 설정

배포 Script

설계서·RTM

## **205.5 리뷰의 입력자료**

| **입력** | **확인 목적** |
| --- | --- |
| 요구사항 | 무엇을 구현하는가 |
| 수용기준 | 완료 판단 |
| 화면 이벤트 | 호출 관계 |
| ServiceId | 거래 목적 |
| 프로그램 설계 | 계층 책임 |
| DB 설계 | 데이터 구조 |
| 테스트 결과 | 검증 증적 |
| 영향도 분석 | 변경 범위 |
| 성능 결과 | 자원 영향 |

## **205.6 작은 변경도 위험할 수 있다**

예:

Timeout 3초 → 10초

코드 한 줄의 변경이지만 다음에 영향을 줍니다.

사용자 대기시간

Tomcat Thread 점유

DB Connection 점유

Gateway Timeout

외부 Client 시간예산

경보 임계값

변경 파일 수와 위험도는 비례하지 않습니다.

## **205.7 대규모 변경이 항상 위험한 것은 아니다**

자동 생성 코드 정렬이나 Package 이동은 파일 수가 많아도 동작 변경이 적을 수 있습니다.

중요한 것은 다음입니다.

외부 계약이 바뀌는가?

데이터가 바뀌는가?

Runtime 흐름이 바뀌는가?

배포단위가 바뀌는가?

Rollback이 가능한가?

## **제205장 요약**

학습 목표 | 205장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

코드 리뷰는 문법과 스타일보다
보안·데이터·트랜잭션·운영 위험을 우선한다.

변경 파일 수가 아니라 변경 의미와 영향범위를 기준으로 검토한다.

# **제206장. 리뷰 준비와 변경 범위 확인**

학습 목표 | 206장. 리뷰 준비와 변경 범위 확인의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **206.1 리뷰 전 확인사항**

변경 목적

관련 요구사항

기준 Branch

변경 Commit

대상 Module

ServiceId

DB 변경

설정 변경

배포 영향

테스트 결과

## **206.2 좋은 Pull Request 설명**

목적
상담예약 수정 시 동시성 충돌을 방지한다.

변경
\- VERSION\_NO 컬럼 적용
\- 수정 SQL Version 조건 추가
\- 영향 건수 0건 업무 오류 처리
\- 동시 수정 통합 테스트 추가

영향
CT.Reservation.update 거래만 영향

DB
기존 VERSION\_NO 컬럼 사용

호환성
Request에 versionNo 필수화로 기존 화면 동시 배포 필요

Rollback
구 화면과 구 WAR 동시 복구

## **206.3 나쁜 Pull Request 설명**

예약 기능 수정했습니다.

테스트 완료했습니다.

무엇을 왜 변경했는지 알 수 없습니다.

## **206.4 변경 범위 비교**

요구된 변경

실제 변경

부수 변경

을 구분합니다.

예:

| **구분** | **내용** |
| --- | --- |
| 요구 | 수정 Version 추가 |
| 실제 | DTO·SQL·테스트 변경 |
| 부수 | 공통 오류코드 구조 변경 |

부수 변경이 크면 별도 Pull Request로 분리합니다.

## **206.5 기능 변경과 리팩터링 분리**

나쁜 예:

업무규칙 변경

\+ Class 이름 변경

\+ Package 이동

\+ SQL 재작성

\+ 공통 Library Upgrade

한 번에 문제가 발생하면 원인을 분리하기 어렵습니다.

권장:

1차
기존 동작 Characterization Test

2차
구조 리팩터링

3차
업무기능 변경

4차
성능 개선

## **206.6 Diff 검토 순서**

DB Migration

→ 외부 계약 DTO

→ ServiceId·Handler

→ Transaction·Service

→ DAO·Mapper

→ 테스트

→ 설정·OM

→ 문서

데이터와 계약 변경을 먼저 보면 전체 영향을 이해하기 쉽습니다.

## **206.7 생성파일과 수작업파일 구분**

다음은 리뷰 방식이 다를 수 있습니다.

AI 생성 코드

Code Generator 산출물

DB Metadata 생성 DTO

직접 작성 업무로직

자동 Format 변경

생성코드도 실행되는 소스라면 품질 책임에서 제외되지 않습니다.

## **206.8 리뷰 가능한 크기**

대규모 변경은 다음 단위로 분리합니다.

DB 확장

공통 계약 추가

업무 구현

화면 연계

데이터 Migration

운영 설정

작은 단위는 위험 확인과 Rollback을 쉽게 합니다.

## **제206장 요약**

학습 목표 | 206장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

리뷰 전에 변경 목적·범위·호환성·Rollback을 확인한다.

기능 변경과 구조 리팩터링을 가능한 한 분리한다.

요구사항과 무관한 부수 변경은 별도 작업으로 분리한다.

# **제207장. 아키텍처·계층 책임 리뷰**

학습 목표 | 207장. 아키텍처·계층 책임 리뷰의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **207.1 전체 계층 검토**

Controller
→ TCF 공통 진입만 담당

Handler
→ ServiceId 분기

Facade
→ 유스케이스·트랜잭션

Service
→ 업무 흐름

Rule
→ 검증·계산

DAO
→ 데이터 접근 추상화

Mapper
→ SQL

## **207.2 Controller 검토**

정상:

@PostMapping("/{businessCode}/online")
public StandardResponse process(
@PathVariable String businessCode,
@RequestBody StandardRequest request) {

return tcf.process(
businessCode,
request
);
}

금지:

@PostMapping("/reservation/create")
public Object create(
@RequestBody ReservationRequest request) {

return reservationService.create(request);
}

업무별 Controller가 TCF 공통 흐름을 우회합니다.

## **207.3 Handler 검토**

정상 책임:

ServiceId 선언

Request 변환

Facade 호출

Response 반환

검토 질문:

DB를 직접 호출하는가?

업무상태를 검증하는가?

권한을 임의 계산하는가?

Transaction을 시작하는가?

외부 Client를 직접 호출하는가?

하나라도 해당하면 책임경계 위반 가능성이 있습니다.

## **207.4 Facade 검토**

정상:

@Transactional
public ReservationCreateResponse create(
ReservationCreateRequest request,
TransactionContext context) {

return service.create(request, context);
}

검토:

Transaction 범위가 유스케이스와 일치하는가?

Master와 History가 같은 범위인가?

외부 호출을 장시간 포함하는가?

readOnly 설정이 올바른가?

Self Invocation 문제가 없는가?

## **207.5 Service 검토**

Service는 업무 흐름을 읽을 수 있어야 합니다.

정상적인 형태:

현재 데이터 조회

→ 존재 검증

→ 권한 검증

→ 상태 검증

→ 변경 수행

→ History 저장

→ 응답 생성

위험한 형태:

public Object process(Map<String, Object> input) {
// 수백 줄의 조건문과 SQL 호출
}

## **207.6 Rule 검토**

Rule에 적합한 책임:

입력값 검증

상태전이 검증

Version 검증

날짜·금액 계산

업무 오류코드 결정

Rule에서 피해야 할 책임:

Mapper 호출

외부 연계

Transaction 시작

파일 저장

Session 접근

## **207.7 DAO·Mapper 검토**

DAO:

Mapper 호출

데이터 접근 예외 변환

영향 건수 반환

DataSource 경계

Mapper:

SQL과 Result Mapping

DAO가 업무상태를 판단하거나 Mapper가 Java 업무로직을 대신하지 않도록 합니다.

## **207.8 업무 WAR 간 의존성**

금지:

implementation project(":ic-service")

권장:

공개 Contract

업무 Client

HTTP·EAI ServiceId 호출

업무 WAR의 배포경계를 유지합니다.

## **207.9 공통모듈 역의존**

금지:

tcf-core
→ ct-service Class 참조

공통모듈이 특정 업무를 의존하면 전체 구조가 역전됩니다.

## **207.10 순환 의존**

예:

Service A
→ Service B
→ Service A

개선 대안:

책임 재분리

상위 Orchestrator

공통 Rule 추출

Event 전환

데이터 소유권 재정의

## **제207장 요약**

학습 목표 | 207장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

계층 리뷰의 핵심은 Class 개수가 아니라 책임경계다.

업무 WAR끼리 직접 의존하지 않고 공개 계약으로 연계한다.

공통모듈은 특정 업무를 역으로 참조하지 않는다.

# **제208장. 트랜잭션·예외·Timeout 리뷰**

학습 목표 | 208장. 트랜잭션·예외·Timeout 리뷰의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 실패를 설계하는 관점: 오류를 사용자 입력, 업무 규칙, 의존 시스템, 인프라 문제로 구분해야 복구 책임과 메시지가 선명해집니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **208.1 트랜잭션 검토 질문**

어디에서 시작하는가?

어디에서 Commit되는가?

어떤 예외에서 Rollback되는가?

외부 호출이 포함되는가?

복수 DB를 사용하는가?

History와 Master가 같은 범위인가?

## **208.2 잘못된 예외 처리**

try {
reservationDao.insertHistory(command);
} catch (Exception e) {
log.warn("history insert failed", e);
}

return ReservationResponse.success();

History 실패를 숨기고 성공으로 반환합니다.

개선:

reservationDao.insertHistory(command);

필수 이력 실패는 예외를 전파해 전체 Transaction을 Rollback합니다.

## **208.3 업무 예외와 시스템 예외**

| **예외** | **예** |
| --- | --- |
| 업무 예외 | 상태 변경 불가 |
| 시스템 예외 | DB 연결 실패 |
| Timeout | 처리시간 초과 |
| 보안 예외 | 권한 없음 |
| 연계 예외 | 외부 응답 실패 |

모든 예외를 하나의 RuntimeException으로 변환하면 운영 원인분류가 어려워집니다.

## **208.4 지나치게 넓은 Catch**

금지:

catch (Exception e) {
return null;
}

위험:

오류가 정상 데이터 없음처럼 보인다.

Rollback이 발생하지 않을 수 있다.

운영로그가 사라진다.

NullPointerException이 다른 위치에서 발생한다.

## **208.5 Self Invocation**

public void create() {
saveTransactional();
}

@Transactional
public void saveTransactional() {
}

같은 객체 내부 직접 호출이면 Proxy를 거치지 않을 수 있습니다.

리뷰 시 통합 테스트로 실제 Rollback을 확인합니다.

## **208.6 외부 호출과 트랜잭션**

위험:

Transaction 시작

→ DB 조회

→ 외부 시스템 8초 대기

→ DB 변경

→ Commit

영향:

DB Connection 장기 점유

Lock 유지

Pool 고갈

Timeout 후 UNKNOWN

대안:

외부 검증을 Transaction 전 수행

Outbox·보상 처리

짧은 DB Transaction

비동기 연계

## **208.7 Timeout 리뷰**

검토 대상:

UI Timeout

Gateway Timeout

TCF ServiceId Timeout

외부 Connect·Read Timeout

SQL Query Timeout

DB Connection Timeout

원칙:

하위 Timeout < 상위 Timeout

## **208.8 Timeout을 무조건 늘리는 변경**

리뷰 질문:

느린 구간이 확인되었는가?

Thread 점유가 얼마나 증가하는가?

DB Pool 영향은 무엇인가?

상위 Gateway Timeout도 변경되는가?

성능시험이 있는가?

임시 변경인가?

## **208.9 Retry와 Transaction**

변경 거래를 Transaction 내부에서 자동 Retry하면 중복 가능성을 확인해야 합니다.

DB Deadlock Retry

외부 HTTP Retry

Message Retry

모든 Retry에는 다음이 필요합니다.

최대 횟수

전체 시간예산

Idempotency

재시도 가능한 오류 정의

## **208.10 Rollback 검증**

단순히 @Transactional을 확인하지 않고 다음을 테스트합니다.

Master 성공

History 실패

→ Master 0건

→ History 0건

## **제208장 요약**

학습 목표 | 208장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

트랜잭션 리뷰는 Annotation보다 실제 경계와 Rollback 결과를 확인한다.

예외를 숨기거나 성공으로 변환하지 않는다.

Timeout과 Retry는 Thread·Pool·중복 위험까지 함께 검토한다.

# **제209장. 보안·개인정보·감사 리뷰**

학습 목표 | 209장. 보안·개인정보·감사 리뷰의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 신뢰 경계: 사용자 신원과 권한, 토큰의 유효기간, 민감정보 노출 여부를 분리해 확인해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **209.1 Request 사용자정보 검토**

금지:

String userId = request.userId();
String branchId = request.branchId();

권장:

String userId = context.getUserId();
String branchId = context.getBranchId();

## **209.2 인증과 권한 구분**

인증
누구인가?

기능권한
이 ServiceId를 실행할 수 있는가?

데이터권한
이 데이터를 볼 수 있는가?

리뷰에서 세 항목이 모두 적용되는지 확인합니다.

## **209.3 관리자 권한 우회**

위험:

if ("ADMIN".equals(request.roleCode())) {
return allData;
}

Role은 Token과 서버 권한정책에서 확인해야 합니다.

## **209.4 SQL 데이터권한**

예:

AND BRANCH\_ID = #{branchId}

검토:

branchId가 Context에서 왔는가?

관리자 예외조건은 안전한가?

Count와 List SQL에 동일하게 적용되는가?

상세조회에도 권한검증이 있는가?

## **209.5 민감정보 로그**

금지:

log.info("request={}", request);

Request 전체에 고객번호·상담내용·Token이 포함될 수 있습니다.

권장:

log.info(
"serviceId={}, reservationId={}, result={}",
context.getServiceId(),
mask(reservationId),
resultCode
);

## **209.6 SQL 로그**

다음을 원문으로 기록하지 않습니다.

주민등록번호

계좌번호

상담내용

JWT

Password

Private Key

## **209.7 Secret 하드코딩**

금지:

password: realPassword123!
jwt-private-key: |
\-----BEGIN PRIVATE KEY-----

Secret은 환경변수 또는 보안 저장소에서 주입합니다.

## **209.8 입력 Validation**

검토:

길이

형식

범위

허용코드

HTML·Script

파일 MIME

정렬 Field

검색조건

Validation은 화면뿐 아니라 서버에서 수행합니다.

## **209.9 SQL Injection**

금지:

ORDER BY ${sortColumn}

권장:

허용된 Enum을 서버에서 매핑

또는 고정 정렬

## **209.10 개인정보 과다조회**

목록 API에 상세정보를 모두 반환하지 않습니다.

목록
식별·요약 정보

상세
권한 확인 후 상세정보

## **209.11 감사로그 검토**

다음 기능은 감사대상 여부를 확인합니다.

개인정보 상세조회

등록·수정·취소

권한 변경

파일 다운로드

운영 거래통제

## **209.12 보안 실패정책**

보안 검증 모듈이 장애일 때 무조건 허용하는 코드는 금지합니다.

Fail-open
→ 장애 시 모두 허용

금융·개인정보 거래에서는 원칙적으로 차단 또는 제한된 안전모드를 검토합니다.

## **제209장 요약**

학습 목표 | 209장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

사용자·지점·권한정보는 신뢰 Context에서 가져온다.

기능권한과 데이터권한을 모두 검토한다.

로그·SQL·설정에서 개인정보와 Secret을 제거한다.

# **제210장. SQL·DB·동시성 리뷰**

학습 목표 | 210장. SQL·DB·동시성 리뷰의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 데이터 일관성: 화면 동작보다 데이터 소유권, 상태 전이, 동시성, 트랜잭션 경계를 먼저 결정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **210.1 SQL 리뷰의 기본 질문**

어떤 ServiceId가 실행하는가?

예상 호출량은 얼마인가?

조회·영향 건수는 얼마인가?

인덱스를 사용하는가?

권한조건이 포함되는가?

Timeout은 얼마인가?

Lock 위험이 있는가?

## **210.2 SELECT \***

금지 이유:

불필요한 데이터 전송

컬럼 추가 시 결과 변경

민감정보 노출

Covering Index 활용 저하

DTO 계약 불명확

필요한 컬럼만 명시합니다.

## **210.3 무제한 목록조회**

금지:

SELECT ...
FROM CT\_CONTACT\_RESERVATION
ORDER BY RESERVATION\_DTM DESC

권장:

필수 검색조건

최대 조회기간

Paging

최대 Page Size

정렬 기준

## **210.4 Count SQL과 목록 SQL 정합성**

다음을 비교합니다.

고객번호 조건

기간 조건

상태 조건

데이터권한

사용중 상태

Join 조건

Count와 목록의 조건이 다르면 전체건수가 맞지 않습니다.

## **210.5 Update 조건**

정상 예:

UPDATE CT\_CONTACT\_RESERVATION
SET MEMO\_CONTENT = #{memoContent},
VERSION\_NO = VERSION\_NO + 1
WHERE RESERVATION\_ID = #{reservationId}
AND STATUS\_CD = 'READY'
AND VERSION\_NO = #{versionNo}

## **210.6 영향 건수**

int affectedRows =
mapper.updateReservation(command);

if (affectedRows != 1) {
throw new OptimisticLockException();
}

영향 건수를 무시하면 실패가 성공처럼 처리될 수 있습니다.

## **210.7 물리 삭제**

업무 데이터는 다음을 먼저 검토합니다.

상태 변경

폐기일자

폐기사유

이력

보관기간

물리 삭제는 보관기간 종료 후 별도 Batch로 수행하는 것이 일반적입니다.

## **210.8 Index 검토**

Index는 조회 SQL만 보고 추가하지 않습니다.

조회 이득

등록·수정 비용

선택도

컬럼 순서

Index 수

Storage

운영 통계

## **210.9 함수와 인덱스**

예:

WHERE TO\_CHAR(RESERVATION\_DTM, 'YYYYMMDD')
\= #{reservationDate}

일반 Index 활용이 어려울 수 있습니다.

범위조건을 권장합니다.

WHERE RESERVATION\_DTM >= #{startDtm}
AND RESERVATION\_DTM < #{endDtm}

## **210.10 동적 SQL**

검토:

필수조건이 누락될 수 있는가?

빈 문자열이 조건에 들어가는가?

권한조건이 동적으로 빠질 수 있는가?

AND·OR 괄호가 올바른가?

동일 SQL이 지나치게 많은 형태로 변하는가?

## **210.11 N+1 조회**

위험:

for (ReservationData item : items) {
item.setCustomerName(
customerClient.getName(item.customerNo())
);
}

100건 조회 시 외부 호출 100회가 발생합니다.

대안:

Bulk API

Join 또는 Read Model

Batch 조회

사전 Cache

## **210.12 Lock 순서**

여러 테이블을 변경하는 거래는 Update 순서를 통일합니다.

Master

→ Detail

→ History

거래마다 반대 순서로 접근하면 Deadlock 위험이 증가합니다.

## **210.13 DB Schema 변경**

검토:

기존 WAR와 호환되는가?

Null 허용인가?

기본값이 필요한가?

Migration 시간이 얼마나 걸리는가?

Index 생성 중 Lock이 발생하는가?

Rollback 가능한가?

## **제210장 요약**

학습 목표 | 210장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

SQL 리뷰는 문법보다 조회범위·권한·성능·동시성을 검토한다.

수정 SQL에는 상태·Version 조건과 영향 건수 검증이 필요하다.

DB 변경은 구·신 WAR 혼재와 Migration 위험을 고려한다.

# **제211장. 성능·용량·자원 사용 리뷰**

학습 목표 | 211장. 성능·용량·자원 사용 리뷰의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **211.1 성능은 SQL만의 문제가 아니다**

검토 대상:

요청 크기

객체 생성

Collection 크기

JSON 직렬화

SQL

외부 호출

Thread

DB Connection

Cache

파일

## **211.2 전체 데이터 메모리 적재**

위험:

List<Customer> customers =
mapper.selectAllCustomers();

대량 데이터는 Heap과 GC에 영향을 줍니다.

대안:

Paging

Streaming

Chunk

Batch

최대건수

## **211.3 Collection 중첩 탐색**

위험:

for (Customer customer : customers) {
for (Reservation reservation : reservations) {
// 매번 비교
}
}

대량 데이터에서 수행시간이 급증할 수 있습니다.

Key 기반 Map을 검토합니다.

## **211.4 반복 객체 생성**

다음이 고빈도 거래 안에서 반복되는지 확인합니다.

정규식 Compile

ObjectMapper 생성

HTTP Client 생성

Thread Pool 생성

Formatter 생성

공유 가능한 객체는 Bean 또는 상수로 관리합니다.

## **211.5 Thread Pool 직접 생성**

금지:

Executors.newFixedThreadPool(100);

업무코드가 임의로 Executor를 생성하면 다음이 어렵습니다.

종료 관리

Thread 수 통제

Context 전파

Metric

장애 격리

Framework가 관리하는 Executor를 사용합니다.

## **211.6 비동기 처리**

검토:

TransactionContext가 전파되는가?

SecurityContext가 전파되는가?

MDC가 전파되는가?

Timeout이 적용되는가?

예외가 관측되는가?

Queue가 무제한인가?

## **211.7 Cache 리뷰**

확인:

Cache Key

TTL

Maximum Size

민감정보

무효화

Fallback

Metric

무제한 Local Cache는 OOM 위험이 있습니다.

## **211.8 외부 호출 횟수**

한 거래에서 같은 외부 API를 반복 호출하는지 확인합니다.

동일 고객 기본정보를 세 번 조회

동일 공통코드를 반복 조회

목록 행별 외부 호출

## **211.9 응답 크기**

검토:

목록 최대건수

상세 본문 포함 여부

Base64 파일 포함

중복 데이터

불필요한 공통코드명

## **211.10 로그 성능**

고빈도 거래에서 대량 INFO 로그를 출력하면 I/O와 저장비용이 증가합니다.

다음 정보를 중심으로 구조화합니다.

ServiceId

TraceId

처리시간

결과코드

SQL ID

## **211.11 성능 변경 증적**

성능 개선 Pull Request에는 다음을 포함합니다.

변경 전 Baseline

변경 후 결과

동일 데이터·부하조건

p95·p99

CPU·Heap·DB Pool

SQL 실행계획

평균시간만 제시하지 않습니다.

## **제211장 요약**

학습 목표 | 211장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

성능 리뷰는 SQL·객체·Thread·Cache·외부 호출을 함께 본다.

대량 데이터는 Paging·Chunk·Streaming으로 제한한다.

성능 개선은 변경 전후의 동일 조건 증적으로 검증한다.

# **제212장. 테스트 코드 품질 리뷰**

학습 목표 | 212장. 테스트 코드 품질 리뷰의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **212.1 테스트 성공만으로 충분하지 않다**

검토:

무엇을 검증하는가?

실패했을 때 결함을 발견할 수 있는가?

정상 경로만 있는가?

실제 DB·Transaction을 검증하는가?

순서와 환경에 독립적인가?

## **212.2 좋은 테스트 이름**

shouldRollbackMasterWhenHistoryInsertFails()

나쁜 이름:

test1()

## **212.3 Arrange·Act·Assert**

@Test
void shouldRejectCompletedReservationUpdate() {

// Arrange
ReservationData completed =
completedReservation();

// Act
Executable action =
() -> rule.validateUpdatable(completed);

// Assert
assertThrows(
BusinessException.class,
action
);
}

## **212.4 과도한 Mock**

모든 것을 Mock으로 만들면 실제 Transaction과 SQL을 확인할 수 없습니다.

Unit Test
업무 분기·Rule

Integration Test
Transaction·Mapper·DB

E2E Test
인증·TCF·응답

역할별 테스트가 필요합니다.

## **212.5 호출 여부만 검증하는 테스트**

verify(mapper).insert(any());

만으로는 다음을 확인하지 못합니다.

Command 값

감사 사용자

상태값

Version

Rollback

Argument 값을 검증합니다.

## **212.6 실패 시나리오**

필수 후보:

Validation 실패

권한 없음

데이터 없음

상태 오류

동시성 충돌

History 실패

SQL 오류

외부 Timeout

TCF Timeout

중복 요청

## **212.7 테스트 데이터 독립성**

금지:

테스트 A가 등록한 데이터를 테스트 B가 사용

권장:

각 테스트가 데이터 생성

고유 Key 사용

Transaction Rollback

실행순서 무관

## **212.8 시간 테스트**

업무코드에 현재시각 의존성이 있으면 Clock을 주입합니다.

Clock fixedClock =
Clock.fixed(
Instant.parse("2026-07-17T00:00:00Z"),
ZoneId.of("Asia/Seoul")
);

## **212.9 Disabled 테스트**

@Disabled("temporary")

리뷰 질문:

왜 비활성화했는가?

언제 복구하는가?

담당자는 누구인가?

품질위험을 승인했는가?

## **212.10 Coverage의 한계**

Coverage가 높아도 Assertion이 약하면 결함을 찾지 못합니다.

Line Coverage

Branch Coverage

Mutation 관점

업무규칙 Coverage

오류 시나리오 Coverage

를 함께 봅니다.

## **212.11 Characterization Test**

Legacy 리팩터링 전 현재 동작을 고정하는 테스트입니다.

기존 응답

오류코드

DB 결과

부수효과

를 기록해 구조 변경 중 동작이 달라지는 것을 방지합니다.

## **제212장 요약**

학습 목표 | 212장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

테스트 리뷰는 개수와 Coverage보다
검증하는 업무조건과 실패경로를 확인한다.

리팩터링 전에는 기존 동작을 고정하는 테스트를 먼저 만든다.

# **제213장. 가독성·명명·유지보수성 리뷰**

학습 목표 | 213장. 가독성·명명·유지보수성 리뷰의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **213.1 좋은 이름의 기준**

이름만으로 다음이 드러나야 합니다.

업무 대상

동작

계층

단수·복수

입력·출력 목적

예:

selectReservationList

validateReservationWritable

ReservationUpdateCommand

ReservationDetailResponse

## **213.2 모호한 이름**

process

handleData

doWork

temp

data

result2

공통 Framework 인터페이스가 handle()을 요구하는 경우 내부 메서드는 업무 의미를 가져야 합니다.

## **213.3 Boolean 이름**

권장:

active

writable

admin

duplicated

또는 메서드:

isActive()

canUpdate()

hasPermission()

부정 Boolean을 중첩하면 이해가 어려워집니다.

if (!isNotAllowed) {
}

## **213.4 긴 메서드**

다음 징후가 있으면 분리를 검토합니다.

수백 줄

여러 추상화 수준 혼합

많은 지역변수

중첩 조건

DB·외부·변환 혼합

## **213.5 추상화 수준**

나쁜 예:

validateRequest();
mapper.insert(command);
String json = objectMapper.writeValueAsString(result);
sendHttp(json);

업무 검증·DB·직렬화·연계가 한 메서드에 혼재합니다.

## **213.6 매직 값**

금지:

if ("01".equals(statusCode)) {
}

권장:

if (ReservationStatus.READY.matches(statusCode)) {
}

## **213.7 긴 Parameter 목록**

createReservation(
id,
customerNo,
reservationDtm,
purposeCode,
memo,
branchId,
userId,
status,
version,
traceId
)

Command 객체로 묶습니다.

## **213.8 Map 남용**

금지:

Map<String, Object> request

문제:

Compile-time 검증 없음

필드명 오타

형 변환 오류

계약 불명확

명시적 DTO를 사용합니다.

## **213.9 주석**

좋은 주석:

왜 이렇게 설계했는가?

어떤 제약 때문에 필요한가?

업무상 주의점은 무엇인가?

나쁜 주석:

// 예약을 조회한다.
reservationDao.selectReservation();

코드가 이미 설명하는 내용을 반복합니다.

## **213.10 복사 코드**

유사 코드가 세 곳에 있다고 즉시 공통화하지 않습니다.

확인:

업무 의미가 같은가?

변경주기가 같은가?

소유 업무가 같은가?

공통화로 결합이 증가하는가?

## **213.11 불변성**

가능하면 DTO와 값 객체를 불변으로 설계합니다.

public record ReservationUpdateCommand(
String reservationId,
LocalDateTime reservationDateTime,
long versionNo
) {
}

중간 상태 변경과 Thread 안전 문제를 줄입니다.

## **제213장 요약**

학습 목표 | 213장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

이름은 업무 의미와 책임을 표현해야 한다.

긴 메서드·Map·매직 값·긴 Parameter는 유지보수성을 저하시킨다.

중복 코드는 의미와 변경주기를 확인한 후 공통화한다.

# **제214장. 안전한 리팩터링 전략**

학습 목표 | 214장. 안전한 리팩터링 전략의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **214.1 리팩터링 전 조건**

현재 동작 이해

테스트 확보

변경범위 고정

Baseline 측정

Rollback 가능

작은 단계 계획

## **214.2 리팩터링과 기능 변경**

리팩터링은 외부 동작을 유지하는 것이 원칙입니다.

다음이 바뀌면 기능 변경일 수 있습니다.

응답 필드

오류코드

Transaction 결과

SQL 결과

권한

Timeout

정렬

상태전이

## **214.3 Strangler 방식**

거대 Legacy 기능을 한 번에 교체하지 않고 일부부터 전환합니다.

기존 Service

→ 새 Rule 분리

→ 새 DAO 분리

→ 새 ServiceId 전환

→ 기존 호출 제거

## **214.4 긴 메서드 분리**

기존:

public ReservationResponse update(...) {
// 입력 검증
// 권한 검증
// 상태 검증
// Update
// History
// 응답 변환
}

개선:

validateUpdateRequest(request);
ReservationData current =
loadCurrentReservation(request);
validateUpdateAllowed(current, context);
applyUpdate(request, current, context);
return createResponse(request);

분리 메서드가 과도하게 작고 의미가 없지 않도록 합니다.

## **214.5 거대 Service 분리**

예:

CustomerService
\- 고객조회
\- 상담관리
\- 마케팅동의
\- 파일 Export
\- 외부연계
\- Batch

도메인 기준으로 분리합니다.

CustomerQueryService

ContactService

ConsentService

CustomerExportService

## **214.6 조건문을 상태 객체로 전환**

기존:

if ("READY".equals(status)) {
} else if ("COMPLETED".equals(status)) {
} else if ("CANCELED".equals(status)) {
}

단순 상태라면 Enum으로 충분합니다.

ReservationStatus status =
ReservationStatus.from(code);

status.validateCancelable();

복잡한 상태전이가 많을 때 State Pattern을 검토합니다.

## **214.7 공통 Validation 추출**

같은 업무규칙이 여러 Service에서 반복된다면 Rule로 추출합니다.

단, 모든 Validation을 하나의 거대한 CommonValidationUtils에 넣지 않습니다.

CtReservationRule

CtContactRule

CustomerNumberValidator

처럼 업무 의미를 유지합니다.

## **214.8 Mapper 분리**

하나의 Mapper가 수십 개 Table과 도메인을 다루면 도메인별로 분리합니다.

CtReservationMapper

CtContactMapper

CtContactHistoryMapper

테이블별 분리와 업무 유스케이스별 분리 사이에서 응집도를 고려합니다.

## **214.9 DTO 리팩터링**

기존:

ReservationDto
→ Request·DB·Response 전부 사용

단계적 분리:

1\. Response DTO 분리

2\. Request DTO 분리

3\. Command·Data 분리

4\. Mapper ResultMap 변경

## **214.10 리팩터링 단계별 Commit**

Commit 1
Characterization Test

Commit 2
Class Rename

Commit 3
Rule 추출

Commit 4
DTO 분리

Commit 5
불필요 코드 삭제

문제가 생기면 원인과 Rollback 지점이 명확합니다.

## **214.11 리팩터링 중 성능 변화**

구조 개선이 성능을 악화할 수 있습니다.

예:

메서드 분리 과정에서 SQL 호출 증가

DTO 변환 반복

외부 Client 중복 호출

Cache 제거

핵심 거래는 성능 Baseline을 비교합니다.

## **제214장 요약**

학습 목표 | 214장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

리팩터링 전 테스트와 현재 동작을 확보한다.

작은 단계로 변경하고 기능 변경과 분리한다.

거대 Service·DTO·Mapper는 도메인 책임을 기준으로 나눈다.

# **제215장. 대표 Code Smell과 개선 사례**

학습 목표 | 215장. 대표 Code Smell과 개선 사례의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **215.1 거대 Handler**

### **문제**

public Object handle(...) {
// 검증
// SQL
// 외부 호출
// 파일 저장
// 응답 생성
}

### **개선**

Handler
→ Facade

Facade
→ Service

Service
→ Rule·DAO·Client

## **215.2 하나의 Save 거래**

### **문제**

actionType
CREATE·UPDATE·DELETE

### **개선**

ServiceId 분리

create

update

cancel

권한과 오류가 명확해집니다.

## **215.3 God Service**

### **문제**

Class 3,000줄

Dependency 20개

Public Method 40개

### **개선 기준**

도메인

유스케이스

조회·변경

외부연계

Batch

로 책임을 분리합니다.

## **215.4 Primitive Obsession**

### **문제**

String statusCode;
String customerNo;
String businessDate;

### **개선**

중요한 업무개념은 값 객체를 검토합니다.

ReservationStatus status;
CustomerNumber customerNumber;
BusinessDate businessDate;

## **215.5 Flag Argument**

### **문제**

selectCustomer(
customerNo,
true,
false,
true
);

무엇을 의미하는지 어렵습니다.

### **개선**

명시적 Option 객체

또는 별도 Method

## **215.6 깊은 중첩 조건**

### **문제**

if (reservation != null) {
if ("READY".equals(status)) {
if (hasPermission) {
if (versionMatches) {
}
}
}
}

### **개선**

Guard Clause를 사용합니다.

rule.validateExists(reservation);
rule.validateUpdatable(reservation);
rule.validateWritable(reservation, context);
rule.validateVersion(...);

## **215.7 예외를 정상흐름으로 사용**

금지:

try {
return mapper.selectOne(id);
} catch (Exception e) {
return null;
}

데이터 없음과 시스템 오류를 구분합니다.

## **215.8 Hidden Side Effect**

메서드명이 validateReservation()인데 DB를 변경하면 위험합니다.

이름과 실제 부수효과를 일치시킵니다.

## **215.9 Feature Envy**

한 Service가 다른 도메인의 내부 데이터를 과도하게 조작하면 소유권이 잘못되었을 수 있습니다.

CT Service가 IC 고객상태 규칙을 직접 판단

IC에 공개된 판단 ServiceId 또는 Contract를 검토합니다.

## **215.10 Shotgun Surgery**

작은 업무규칙 변경에 수십 개 Class를 수정해야 한다면 책임이 분산되었을 수 있습니다.

예:

조회기간 3개월 규칙이
화면·Handler·Service·Mapper·Util에 반복

Rule과 중앙 정책으로 모읍니다.

## **215.11 Dead Code**

호출이 없는 코드라도 다음을 확인한 후 제거합니다.

Spring Bean

Reflection

Batch

Scheduler

운영 수동 호출

최근 호출량

## **215.12 Temporary Field**

특정 상황에서만 사용되는 필드가 객체에 계속 존재하면 DTO 분리를 검토합니다.

등록·수정·조회 필드가 하나의 DTO에 혼합

## **제215장 요약**

학습 목표 | 215장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Code Smell은 즉시 오류는 아니지만 변경비용과 장애위험을 높인다.

거대 Class·Flag·중첩 조건·부수효과를
업무 목적과 책임경계 중심으로 개선한다.

# **제216장. 리뷰 의견 작성과 처리**

학습 목표 | 216장. 리뷰 의견 작성과 처리의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **216.1 좋은 리뷰 의견의 구조**

문제

근거

영향

권고안

예:

현재 Update SQL에 VERSION\_NO 조건이 없습니다.

동시에 두 사용자가 수정하면 후행 요청이 선행 변경을
덮어쓸 수 있습니다.

WHERE 조건에 VERSION\_NO를 포함하고,
영향 건수 0건을 동시성 오류로 처리해 주세요.

## **216.2 나쁜 리뷰 의견**

이상합니다.

다시 작성해 주세요.

제가 보기에는 별로입니다.

왜 이렇게 했나요?

근거와 개선방향이 없습니다.

## **216.3 심각도**

| **등급** | **의미** |
| --- | --- |
| Critical | 보안·데이터 손상·전체 장애 |
| Major | 기능·트랜잭션·호환성 결함 |
| Minor | 유지보수·표준 위반 |
| Suggestion | 선택적 개선 |
| Question | 의도 확인 |

## **216.4 Blocking 기준**

다음은 일반적으로 Merge 전 해결해야 합니다.

보안 취약점

데이터 정합성 오류

Rollback 미동작

계약 호환성 파괴

운영 Secret 노출

무제한 대량조회

테스트 실패

Architecture Gate 위반

## **216.5 리뷰 의견에 대한 응답**

좋은 응답:

동의합니다.

VERSION\_NO 조건과 영향 건수 검증을 추가했습니다.

동시 수정 통합 테스트도 추가했고
Commit abc123에 반영했습니다.

설계상 유지해야 하는 경우:

현재 구조를 유지한 이유는 외부 Legacy 계약 때문입니다.

대신 최대 조회건수 1,000건과 Timeout 3초를 적용했고,
예외 승인 EXC-CT-014에 등록했습니다.

## **216.6 단순 “수정 완료” 금지**

수정 내용과 검증결과를 남깁니다.

어떻게 수정했는가?

어떤 테스트를 실행했는가?

잔여 위험은 무엇인가?

## **216.7 의견 충돌**

다음 순서로 해결합니다.

요구사항 확인

→ Architecture Principle 확인

→ 대안 비교

→ Trade-off

→ 책임자 결정

→ ADR 또는 예외 기록

개인의 선호로 장기간 논쟁하지 않습니다.

## **216.8 리뷰 상태**

OPEN

ACKNOWLEDGED

RESOLVED

REJECTED

DEFERRED

DEFERRED에는 담당자와 기한이 필요합니다.

## **216.9 리뷰 회고**

반복 지적사항을 집계합니다.

Request userId 사용

SELECT \*

Version 조건 누락

Mapper XML 불일치

Test 누락

반복되면 Template·교육·자동 Gate를 개선합니다.

## **제216장 요약**

학습 목표 | 216장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

리뷰 의견은 문제·근거·영향·권고안을 포함한다.

보안·데이터·트랜잭션 문제는 Blocking으로 관리한다.

반복 의견은 자동검증과 개발표준으로 전환한다.

# **제217장. 자동 품질 Gate와 정적분석**

학습 목표 | 217장. 자동 품질 Gate와 정적분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **217.1 자동화 대상**

사람이 반복 확인할 필요가 없는 규칙은 자동화합니다.

Package 규칙

계층 의존성

ServiceId 중복

Mapper 정합성

SELECT \* 사용

Secret 패턴

Test 실패

Coverage 기준

취약 Library

Format

## **217.2 Architecture Test**

예시 개념:

classes()
.that()
.resideInAPackage("..handler..")
.should()
.onlyDependOnClassesThat()
.resideInAnyPackage(
"..handler..",
"..facade..",
"..dto..",
"java.."
);

## **217.3 업무 WAR 간 의존성 검사**

ct-service가 ic-service Class를 직접 참조하면 실패

Gradle Dependency Graph와 ArchUnit을 함께 사용할 수 있습니다.

## **217.4 ServiceId Gate**

형식 준수

중복 없음

Handler 등록

OM Catalog 등록

권한 연결

Timeout 연결

## **217.5 Mapper Gate**

Interface Method 존재

XML Statement 존재

Namespace 일치

Parameter Type 일치

Result Mapping 검증

## **217.6 SQL Gate**

자동 검출 후보:

SELECT \*

${}

조건 없는 UPDATE·DELETE

최대건수 없는 온라인 목록

VERSION\_NO 없는 변경

감사 컬럼 누락

SQL Parser의 한계가 있으므로 오탐과 예외관리 체계를 둡니다.

## **217.7 Secret Scan**

검출:

Password

API Key

Private Key

Access Token

Connection String

오탐이라도 보안담당 검토 후 예외 처리합니다.

## **217.8 Dependency 취약점**

확인:

직접 Dependency

전이 Dependency

사용 Version

취약점 심각도

실제 노출 경로

Upgrade 영향

단순 Version Upgrade가 Framework 호환성을 깨뜨릴 수 있으므로 테스트가 필요합니다.

## **217.9 품질 Gate 단계**

Compile

→ Unit Test

→ Static Analysis

→ Architecture Test

→ Mapper·SQL Gate

→ Security Scan

→ Integration Test

→ Packaging

→ Deployment Test

## **217.10 Suppression 관리**

금지:

@SuppressWarnings("all")

예외가 필요하다면 다음을 기록합니다.

규칙 ID

예외 사유

영향

승인자

종료기한

## **217.11 품질 Dashboard**

| **지표** | **예** |
| --- | --- |
| Build 성공률 | 98% |
| Test 성공률 | 100% |
| Architecture 위반 | 0 |
| Critical 취약점 | 0 |
| ServiceId 등록률 | 100% |
| Mapper 정합률 | 100% |
| 미해결 Major 의견 | 0 |
| 기술부채 기한 초과 | 3 |

숫자만 높이는 것이 아니라 실제 위험을 낮추는 것이 목적입니다.

## **제217장 요약**

학습 목표 | 217장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

반복 가능한 구조·형식·참조 검사는 자동 Gate로 전환한다.

Suppression은 무제한 허용하지 않고 사유와 종료기한을 관리한다.

품질지표는 실질적인 운영위험과 연결해야 한다.

# **제218장. 종합 코드 리뷰와 리팩터링 사례**

학습 목표 | 218장. 종합 코드 리뷰와 리팩터링 사례의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **218.1 리뷰 대상 코드**

@Service
@RequiredArgsConstructor
public class ReservationService {

private final ReservationMapper mapper;

public Map<String, Object> save(
Map<String, Object> request) {

String action =
(String) request.get("action");

String userId =
(String) request.get("userId");

if ("C".equals(action)) {
mapper.insert(request);
} else if ("U".equals(action)) {
mapper.update(request);
} else if ("D".equals(action)) {
mapper.delete(request);
}

try {
mapper.insertHistory(request);
} catch (Exception e) {
log.warn("history error");
}

Map<String, Object> result =
new HashMap<>();

result.put("result", "SUCCESS");

return result;
}
}

## **218.2 발견된 문제**

| **영역** | **문제** | **등급** |
| --- | --- | --- |
| 계약 | Map 사용 | Major |
| 거래 | Create·Update·Delete 혼합 | Major |
| 보안 | Request 사용자 ID 신뢰 | Critical |
| 트랜잭션 | 경계 없음 | Critical |
| 이력 | 실패 무시 | Critical |
| 동시성 | Version 없음 | Major |
| 삭제 | 물리 Delete | Major |
| 오류 | 항상 SUCCESS | Critical |
| 테스트 | 검증 불명 | Major |
| 운영 | ServiceId·Metric 없음 | Major |

## **218.3 목표 구조**

CT.Reservation.create
CT.Reservation.update
CT.Reservation.cancel

→ CtReservationHandler

→ CtReservationFacade

→ CtReservationService

→ CtReservationRule

→ CtReservationDao

→ CtReservationMapper

## **218.4 1단계: Characterization Test**

현재 동작을 확인합니다.

등록 시 Master Insert

수정 시 Update

삭제 시 Delete

History 실패해도 SUCCESS

현재 동작 중 잘못된 부분도 기록하되, 목표 요구사항과 구분합니다.

## **218.5 2단계: DTO 분리**

ReservationCreateRequest

ReservationUpdateRequest

ReservationCancelRequest

ReservationCreateCommand

ReservationUpdateCommand

ReservationCancelCommand

## **218.6 3단계: ServiceId 분리**

CT.Reservation.create

CT.Reservation.update

CT.Reservation.cancel

권한과 Timeout을 각각 등록합니다.

## **218.7 4단계: Context 적용**

String userId =
context.getUserId();

String branchId =
context.getBranchId();

Request의 사용자·지점 필드를 제거합니다.

## **218.8 5단계: Transaction 적용**

@Transactional
public ReservationCreateResponse create(...) {
service.create(...);
}

Master와 History를 같은 Transaction으로 처리합니다.

## **218.9 6단계: Version 적용**

수정 SQL:

WHERE RESERVATION\_ID = #{reservationId}
AND STATUS\_CD = 'READY'
AND VERSION\_NO = #{versionNo}

영향 건수 0건은 동시성 오류로 처리합니다.

## **218.10 7단계: 논리 취소**

DELETE
→ STATUS\_CD = CANCELED

History와 감사정보를 저장합니다.

## **218.11 8단계: 표준 오류**

데이터 없음

권한 없음

상태 오류

동시성 충돌

시스템 오류

를 구분합니다.

## **218.12 9단계: 테스트**

정상 등록

History 실패 Rollback

Request userId 무시

동시 수정

완료 상태 취소 차단

권한 없음

Timeout

중복 등록

## **218.13 10단계: 운영정보**

Service Catalog

Timeout

권한

Metric

Alert

Runbook

Trace

를 등록합니다.

## **218.14 개선 결과**

| **항목** | **개선 전** | **개선 후** |
| --- | --- | --- |
| 계약 | Map | 명시적 DTO |
| 기능 | Save 통합 | ServiceId 분리 |
| 사용자 | Request | Context |
| Transaction | 없음 | Facade |
| History | 실패 무시 | 전체 Rollback |
| 동시성 | 없음 | Version |
| 삭제 | 물리 | 논리 취소 |
| 오류 | 항상 SUCCESS | 표준 오류 |
| 운영 | 추적 불가 | ServiceId·Trace |
| 테스트 | 불명확 | 계층별 검증 |

## **제218장 요약**

학습 목표 | 218장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

리팩터링은 코드 모양만 변경하는 것이 아니다.

ServiceId·권한·트랜잭션·동시성·운영정보까지
전체 구조를 표준에 맞게 개선해야 한다.

대규모 변경은 테스트를 기반으로 단계적으로 수행한다.

# **3\. 문제 정의 및 설계 배경**

## **3.1 문제 정의**

운영 장애와 유지보수 비용이 증가하는 이유는 단순한 문법 오류보다 다음 구조적 문제에 있습니다.

계층 책임 혼합

업무 거래 경계 불명확

보안정보 출처 오류

Transaction·Timeout 설계 미흡

SQL 영향 건수 미검증

테스트 실패경로 부족

운영관측 누락

반복되는 수동 리뷰

## **3.2 설계 배경**

NSIGHT는 여러 업무 WAR와 공통 TCF 모듈이 함께 동작합니다.

따라서 하나의 공통모듈 변경이나 잘못된 거래 구현은 다음으로 확산될 수 있습니다.

여러 ServiceId

여러 업무 WAR

공유 Tomcat Thread

공유 JVM Heap

DB Connection Pool

운영·감사 체계

코드 품질을 개인 개발자 수준이 아니라 아키텍처와 운영 수준으로 관리해야 합니다.

# **4\. 현행 구조와 문제점**

| **현행 문제** | **결과** |
| --- | --- |
| 정상 동작만 확인 | 실패·장애 누락 |
| 리뷰 기준 개인화 | 의견 충돌 |
| 스타일 중심 리뷰 | 구조적 위험 누락 |
| 큰 Pull Request | 영향 파악 어려움 |
| 테스트 후순위 | 리팩터링 위험 |
| 정적분석 예외 남용 | 품질 Gate 무력화 |
| 문서 미반영 | Source·설계 불일치 |
| 기술부채 미관리 | 임시구조 영구화 |

# **5\. 요구사항과 제약조건**

## **5.1 품질 요구사항**

Critical 보안 결함 0건

데이터 정합성 결함 0건

Architecture 위반 0건

단위·통합 테스트 성공

ServiceId·OM 등록 정합률 100%

Mapper Interface·XML 정합률 100%

Rollback 검증 완료

공통모듈 변경 전체 Regression 완료

## **5.2 제약조건**

기존 Legacy 코드가 존재한다.

모든 코드를 한 번에 리팩터링할 수 없다.

운영 호환성을 유지해야 한다.

Rolling 배포 중 구·신 버전이 혼재한다.

업무 일정상 기술부채를 즉시 제거하지 못할 수 있다.

# **6\. 설계 원칙**

위험 우선 리뷰

작은 변경 단위

외부 동작 보존

책임경계 준수

테스트 우선

증적 기반 판단

자동화 우선

예외의 기한 관리

운영 가능성 포함

문서·코드 동시 변경

# **7\. 대안 비교 및 의사결정**

## **7.1 전면 재작성과 단계적 리팩터링**

| **대안** | **장점** | **단점** |
| --- | --- | --- |
| 전면 재작성 | 구조 정리 | 높은 실패 위험 |
| 단계적 리팩터링 | 위험 통제 | 기간 증가 |
| 현행 유지 | 일정 단축 | 기술부채 증가 |

기본 결정:

Characterization Test를 확보하고
업무단위로 단계적 리팩터링한다.

## **7.2 수동 리뷰와 자동 Gate**

| **대안** | **장점** | **단점** |
| --- | --- | --- |
| 수동 리뷰 | 맥락 판단 | 반복·누락 |
| 자동 Gate | 일관성 | 복잡한 업무판단 한계 |
| 결합 방식 | 정확도·효율 | 구축 필요 |

결정:

구조·형식·보안패턴은 자동화하고,
업무규칙·Trade-off는 사람이 검토한다.

# **8\. 목표 품질관리 아키텍처**

\[요구사항·설계\]
│
▼
\[Feature Branch·Pull Request\]
│
▼
\[자동 사전검증\]
Compile·Format·Secret·Unit
│
▼
\[코드 리뷰\]
아키텍처·보안·데이터·성능
│
▼
\[통합 품질 Gate\]
Mapper·Transaction·E2E·Security
│
▼
\[Artifact 생성\]
│
▼
\[배포 검증\]
Smoke·Compatibility·Rollback
│
▼
\[운영 관측\]
ServiceId·Metric·Error·SQL
│
▼
\[피드백\]
RCA·리팩터링·Gate 개선

# **9\. 표준 리뷰 형식**

## **9.1 리뷰 Header**

Review ID

Pull Request

요구사항 ID

ServiceId

대상 Module

변경유형

위험등급

Reviewer

검토일

결과

## **9.2 리뷰 의견 형식**

심각도:
Major

위치:
CtReservationMapper.xml / updateReservation

문제:
VERSION\_NO 조건이 없습니다.

영향:
동시 수정 시 선행 변경을 덮어쓸 수 있습니다.

권고:
WHERE 조건에 VERSION\_NO를 추가하고
영향 건수 0건을 동시성 오류로 처리하십시오.

검증:
동시 수정 통합 테스트 추가.

## **9.3 리팩터링 기록**

Refactoring ID

대상 Class

기존 문제

변경 내용

외부 동작 변경 여부

테스트 증적

성능 비교

Rollback

잔여 기술부채

# **10\. 구성요소 및 속성**

| **구성요소** | **주요 속성** |
| --- | --- |
| Pull Request | 목적·영향 |
| Review Checklist | 영역별 기준 |
| Static Analyzer | 자동 규칙 |
| Architecture Test | 의존성 |
| Security Scanner | Secret·취약점 |
| Test Suite | 동작 검증 |
| Technical Debt Registry | 미해결 개선 |
| Exception Registry | 승인 예외 |
| Quality Dashboard | 품질지표 |
| RCA Feedback | 운영 피드백 |

# **11\. 책임 경계와 RACI**

| **활동** | **개발자** | **리뷰어** | **모듈리더** | **AA** | **QA** | **보안** | **DBA** | **OPS** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 변경설명 | R | C | A | I | I | I | I | I |
| Self Review | R | I | C | I | I | I | I | I |
| 업무로직 검토 | C | R | A | C | C | I | C | I |
| 아키텍처 검토 | C | C | C | A/R | C | I | I | C |
| 보안 검토 | C | C | C | C | C | A/R | I | C |
| SQL·DB 검토 | C | C | C | C | C | I | A/R | C |
| 테스트 검토 | R | C | C | C | A/R | C | C | I |
| 운영성 검토 | C | C | C | C | C | C | C | A/R |
| Merge 승인 | C | C | A | C | C | C | C | I |
| 예외 승인 | C | C | C | A | C | C | C | C |

# **12\. 정상 처리 흐름**

1\. 개발자가 요구사항과 변경범위를 확인한다.

2\. 작은 Feature Branch에서 구현한다.

3\. Self Review와 Clean Build를 수행한다.

4\. Pull Request에 영향도·테스트·Rollback을 작성한다.

5\. 자동 품질 Gate를 실행한다.

6\. 리뷰어가 위험 우선순위로 검토한다.

7\. 개발자가 의견을 반영하고 증적을 남긴다.

8\. 미해결 Major·Critical 의견이 없는지 확인한다.

9\. 설계서·RTM·OM 정보를 갱신한다.

10\. Merge 후 동일 Artifact를 생성한다.

11\. 배포 전 호환성·Smoke·Rollback을 검증한다.

12\. 운영지표에서 변경 결과를 확인한다.

# **13\. 오류·Timeout·장애 흐름**

## **13.1 Gate 실패**

정적분석 위반

→ Pull Request 차단

→ 원인 수정 또는 예외 신청

→ 재검증

## **13.2 리뷰 누락 후 장애**

Version 조건 누락

→ 동시 수정 데이터 유실

→ Incident

→ Rollback·정합성 복구

→ RCA

→ SQL Gate 추가

## **13.3 리팩터링 후 성능 저하**

SQL 호출 횟수 증가

→ p95 초과

→ 이전 Version 비교

→ 구조 수정 또는 Rollback

## **13.4 공통모듈 호환성 실패**

tcf-core 변경

→ 일부 WAR 기동 실패

→ Release 차단

→ 전체 WAR Regression

→ API 호환성 수정

# **14\. 정상 예시**

변경
상담예약 수정 Version 통제

리뷰 결과
\- Request versionNo 추가
\- Update SQL Version 조건 추가
\- 영향 건수 검증
\- 동시 수정 Test 추가
\- 오류코드 등록
\- 화면 재조회 처리
\- OM 충돌 Metric 추가

결과
Major 의견 0건
Architecture Gate PASS
동시 수정 통합 테스트 성공

# **15\. 금지 예시**

## **15.1 “코드가 짧다”만으로 승인**

보안·데이터·운영 위험을 확인하지 않습니다.

## **15.2 대규모 변경 하나의 Pull Request**

Framework·업무·DB·문서를 한꺼번에 변경합니다.

## **15.3 테스트 없이 리팩터링**

기존 동작을 확인할 방법이 없습니다.

## **15.4 모든 경고 Suppress**

@SuppressWarnings("all")

## **15.5 리뷰 의견을 개인 공격으로 표현**

근거 대신 작성자를 비난합니다.

## **15.6 “나중에 수정”만 기록**

책임자와 기한이 없습니다.

## **15.7 AI 생성코드 자동 Merge**

Architecture·보안·SQL 검토를 생략합니다.

# **16\. 연계 규칙**

Requirement

→ Pull Request

→ Source Commit

→ Review ID

→ Test Result

→ Quality Gate

→ Build ID

→ Artifact Version

→ Release ID

→ ServiceId Metric

→ RCA·Technical Debt

# **17\. 데이터 및 상태관리**

## **17.1 Pull Request 상태**

DRAFT

READY\_FOR\_REVIEW

CHANGES\_REQUESTED

APPROVED

MERGED

CLOSED

## **17.2 리뷰 의견 상태**

OPEN

ACKNOWLEDGED

RESOLVED

DEFERRED

REJECTED

## **17.3 기술부채 상태**

REGISTERED

PLANNED

IN\_PROGRESS

IMPLEMENTED

VERIFIED

CLOSED

## **17.4 예외 상태**

REQUESTED

APPROVED

EXPIRED

REJECTED

CLOSED

# **18\. 성능·용량·확장성**

코드 품질개선 자체도 프로젝트 규모를 고려해야 합니다.

업무 WAR 수

ServiceId 수

Pull Request 수

자동 테스트 시간

정적분석 시간

전체 Build 시간

공통모듈 영향 범위

대규모 프로젝트에서는 다음 전략을 적용합니다.

변경 Module 우선검증

공통모듈 변경 전체검증

병렬 Test

Test Cache

단계별 Gate

야간 전체 Regression

속도를 위해 Critical 검증을 제거하지 않습니다.

# **19\. 보안·개인정보·감사**

보안 리뷰 결과와 예외 승인을 감사한다.

Pull Request에 Secret과 개인정보를 포함하지 않는다.

운영 Log Sample은 마스킹한다.

취약점 Suppression에는 승인자와 만료일을 둔다.

보안 관련 변경은 독립 Reviewer를 지정한다.

관리자 권한·JWT·Key 변경은 별도 승인한다.

코드 리뷰 도구의 Repository 접근권한을 최소화한다.

# **20\. 운영·모니터링·장애 대응**

코드 리뷰 시 다음 운영 질문에 답해야 합니다.

변경 후 어떤 Metric을 봐야 하는가?

어떤 오류코드가 새로 발생하는가?

어떤 ServiceId가 영향을 받는가?

SQL ID를 추적할 수 있는가?

장애 시 기능만 통제할 수 있는가?

Rollback 조건은 무엇인가?

미확정 거래가 발생할 수 있는가?

운영 장애가 발생하면 RCA 결과를 다음으로 환류합니다.

코드 리뷰 Checklist

Static Rule

Test Scenario

Runbook

Architecture Principle

개발자 교육

# **21\. 자동검증 및 품질 Gate**

| **Gate** | **합격기준** |
| --- | --- |
| Compile | 오류 0 |
| Unit Test | 실패 0 |
| Architecture | 계층 위반 0 |
| Security | Critical 0 |
| Secret | 노출 0 |
| ServiceId | 중복·미등록 0 |
| Mapper | Interface·XML 일치 |
| SQL | 금지 패턴 0 |
| Transaction | Rollback Test |
| Compatibility | Contract Test |
| Documentation | RTM 반영 |
| Operations | Metric·Runbook |
| Review | Major·Critical 미해결 0 |

# **22\. 테스트 시나리오**

| **ID** | **시나리오** | **기대 결과** |
| --- | --- | --- |
| REV-001 | Request userId 사용 | Critical 의견 |
| REV-002 | Handler Mapper 호출 | Architecture 실패 |
| REV-003 | History 예외 무시 | Merge 차단 |
| REV-004 | Version 조건 없음 | Major 의견 |
| REV-005 | SELECT \* | SQL Gate 실패 |
| REV-006 | 무제한 목록조회 | 성능 Gate 실패 |
| REV-007 | Secret Commit | 보안 Gate 실패 |
| REV-008 | ServiceId 중복 | Build 실패 |
| REV-009 | Mapper XML 누락 | Test 실패 |
| REV-010 | 공통모듈 업무의존 | Architecture 실패 |
| REV-011 | Self Invocation | Rollback Test 실패 |
| REV-012 | Timeout 증가 | 영향분석 요구 |
| REV-013 | N+1 외부 호출 | 성능 의견 |
| REV-014 | 무제한 Cache | 메모리 위험 |
| REV-015 | Disabled Test | 기술부채 등록 |
| REV-016 | Response 필드 삭제 | 호환성 실패 |
| REV-017 | DB Column Not Null 추가 | Migration 검토 |
| REV-018 | 대규모 Refactoring | 단계 분리 |
| REV-019 | AI 생성코드 | 동일 Gate 적용 |
| REV-020 | 모든 경고 Suppress | 예외 거절 |
| REV-021 | 정상 리팩터링 | 외부 동작 동일 |
| REV-022 | 리팩터링 성능 저하 | Rollback 검토 |
| REV-023 | 리뷰 의견 Deferred | 기한 등록 |
| REV-024 | 공통 JAR 변경 | 전체 Regression |
| REV-025 | 운영 Metric 누락 | 운영 Gate 실패 |

# **23\. 제20부 체크리스트**

## **23.1 리뷰 준비**

| **점검 항목** | **확인** |
| --- | --- |
| 변경 목적이 구체적인가? | □ |
| 요구사항 ID가 연결되는가? | □ |
| 대상 ServiceId가 명확한가? | □ |
| 영향도 분석이 있는가? | □ |
| DB·설정 변경이 표시되는가? | □ |
| 테스트와 Rollback이 설명되는가? | □ |
| 기능 변경과 리팩터링이 분리되는가? | □ |

## **23.2 아키텍처·계층**

| **점검 항목** | **확인** |
| --- | --- |
| TCF 공통 Controller를 사용하는가? | □ |
| Handler 책임이 단순한가? | □ |
| Facade가 Transaction을 담당하는가? | □ |
| Service가 업무흐름에 집중하는가? | □ |
| Rule에 부수효과가 없는가? | □ |
| DAO·Mapper 책임이 명확한가? | □ |
| 업무 WAR 직접 의존이 없는가? | □ |
| 순환 의존이 없는가? | □ |

## **23.3 트랜잭션·오류·Timeout**

| **점검 항목** | **확인** |
| --- | --- |
| Transaction 경계가 명확한가? | □ |
| Master·History가 동일 Transaction인가? | □ |
| Self Invocation 문제가 없는가? | □ |
| 예외를 숨기지 않는가? | □ |
| 업무·시스템 오류가 구분되는가? | □ |
| 하위 Timeout이 상위보다 짧은가? | □ |
| Retry에 Idempotency가 있는가? | □ |
| Rollback 테스트가 있는가? | □ |

## **23.4 보안·개인정보**

| **점검 항목** | **확인** |
| --- | --- |
| Context 사용자정보를 사용하는가? | □ |
| 기능권한이 적용되는가? | □ |
| 데이터권한이 적용되는가? | □ |
| SQL 권한조건이 누락되지 않는가? | □ |
| 개인정보 로그가 마스킹되는가? | □ |
| Secret 하드코딩이 없는가? | □ |
| ${} 동적 SQL이 없는가? | □ |
| 감사대상 거래가 기록되는가? | □ |

## **23.5 SQL·성능**

| **점검 항목** | **확인** |
| --- | --- |
| SELECT \*가 없는가? | □ |
| 목록에 조회범위가 있는가? | □ |
| Paging이 적용되는가? | □ |
| Count와 List 조건이 동일한가? | □ |
| 수정에 상태·Version 조건이 있는가? | □ |
| 영향 건수를 검사하는가? | □ |
| N+1 호출이 없는가? | □ |
| 대량 Collection을 제한하는가? | □ |
| Cache에 최대크기와 TTL이 있는가? | □ |
| 성능 증적이 있는가? | □ |

## **23.6 테스트**

| **점검 항목** | **확인** |
| --- | --- |
| 업무규칙 테스트가 있는가? | □ |
| 실패경로 테스트가 있는가? | □ |
| Mapper 테스트가 있는가? | □ |
| Transaction 통합 테스트가 있는가? | □ |
| 권한 테스트가 있는가? | □ |
| Timeout·동시성 테스트가 있는가? | □ |
| 테스트가 순서에 독립적인가? | □ |
| Disabled Test에 계획이 있는가? | □ |
| 기존 동작 Characterization Test가 있는가? | □ |

## **23.7 유지보수성**

| **점검 항목** | **확인** |
| --- | --- |
| 이름이 업무 의미를 표현하는가? | □ |
| 긴 메서드를 분리했는가? | □ |
| 깊은 중첩 조건이 없는가? | □ |
| 매직 값이 상수·Enum인가? | □ |
| Map 대신 DTO를 사용하는가? | □ |
| 긴 Parameter를 Command로 묶었는가? | □ |
| 주석이 의도와 제약을 설명하는가? | □ |
| 중복 코드의 의미를 검토했는가? | □ |

## **23.8 운영·배포**

| **점검 항목** | **확인** |
| --- | --- |
| OM Service Catalog가 갱신되는가? | □ |
| Metric과 Alert가 있는가? | □ |
| 새 오류코드가 등록되는가? | □ |
| Runbook이 갱신되는가? | □ |
| Rolling 호환성을 확인했는가? | □ |
| DB Migration이 안전한가? | □ |
| Rollback이 가능한가? | □ |
| 설계서와 RTM이 수정되었는가? | □ |

# **24\. 변경·호환성·폐기 관리**

## **24.1 리팩터링 Version 관리**

리팩터링도 Release Note에 기록합니다.

Class·Package 변경

내부 API 변경

성능 영향

Deprecated 코드

Migration 필요 여부

## **24.2 하위 호환성**

확인:

Request

Response

ServiceId

오류코드

JWT Claim

DB Schema

Cache Value

Event Payload

## **24.3 Deprecated 관리**

ACTIVE

→ DEPRECATED

→ DISABLED

→ RETIRED

Deprecated에는 대체 API와 종료일을 제공합니다.

## **24.4 기술부채 관리**

즉시 수정하지 못하는 리뷰 의견은 다음을 등록합니다.

Debt ID

위험

현재 통제

담당자

기한

종료조건

단순히 리뷰 Thread를 닫고 잊지 않습니다.

## **24.5 정적분석 규칙 변경**

새 규칙을 적용할 때 기존 전체 Source가 대량 실패할 수 있습니다.

전환방안:

신규 변경부터 적용

기존 위반 Baseline 등록

업무별 개선 계획

기한 후 전체 강제

## **24.6 리뷰 Checklist 폐기**

기술구조가 바뀌면 오래된 Checklist를 유지하지 않습니다.

예:

Session 제거 후 Session 검토항목 개편

JPA 도입 시 Mapper 검토항목 조정

Gateway 도입 후 인증책임 변경

# **25\. 시사점**

## **25.1 핵심 아키텍처 판단**

제20부의 핵심은 다음과 같습니다.

코드 리뷰
\= 문법과 Coding Style을 확인하는 활동

이 아닙니다.

코드 리뷰
\= 요구사항·아키텍처·보안·데이터·운영 위험을
Source와 테스트 증적으로 검증하는 활동

입니다.

또한 다음도 중요합니다.

리팩터링
\= 코드를 짧게 만드는 작업

이 아닙니다.

리팩터링
\= 외부 동작을 보호하면서
책임경계와 변경 가능성을 개선하는 작업

입니다.

## **25.2 주요 위험**

| **위험** | **영향** |
| --- | --- |
| 스타일 중심 리뷰 | 핵심 결함 누락 |
| 큰 Pull Request | 영향 파악 실패 |
| 테스트 없는 리팩터링 | Regression |
| Request 사용자정보 | 보안 위변조 |
| History 예외 무시 | 감사 불일치 |
| Version 조건 누락 | 데이터 유실 |
| 무제한 조회 | 성능 장애 |
| 공통모듈 업무의존 | 전체 결합 |
| Suppression 남용 | Gate 무력화 |
| 리뷰 의견 기한 없음 | 기술부채 고착 |
| AI 코드 무검증 | 구조·보안 결함 |
| 문서 미반영 | 추적성 상실 |

## **25.3 우선 보완 과제**

1\. 위험 우선 코드 리뷰 Checklist

2\. Pull Request 표준 Template

3\. ServiceId·Handler 자동검증

4\. 계층 의존성 Architecture Test

5\. Request 사용자정보 금지 Rule

6\. Mapper·SQL 정합성 Gate

7\. Version·영향 건수 검증

8\. Rollback·Timeout·권한 Test

9\. Secret·취약점 Scan

10\. 기술부채 Registry

11\. 리뷰 의견 심각도·기한 관리

12\. 운영 RCA의 품질 Gate 환류

## **25.4 중장기 발전 방향**

개인 경험 기반 리뷰
→ 조직 표준 Checklist

수동 구조 확인
→ Architecture Test

사후 보안 점검
→ Shift-left Security

대규모 일괄 리팩터링
→ 지속적 소규모 개선

기술부채 문서화
→ 위험 기반 Backlog

정적 품질지표
→ 운영 장애와 연계한 품질분석

AI 코드생성
→ AI 생성·검증·Gate 통합

사람 중심 반복검사
→ 자동 Fitness Function

# **26\. 마무리말**

제20부에서 가장 중요하게 기억해야 할 코드 리뷰 순서는 다음과 같습니다.

변경 목적

→ 요구사항

→ 영향범위

→ 아키텍처 책임경계

→ 보안·권한

→ Transaction·Timeout

→ SQL·DB·동시성

→ 성능·자원

→ 테스트

→ 운영·배포

→ 문서·추적성

→ 자동 Gate

리팩터링 순서는 다음과 같습니다.

현재 동작 확인

→ Characterization Test

→ 작은 변경단위

→ 책임 분리

→ DTO·Rule·Transaction 개선

→ Regression Test

→ 성능 비교

→ 운영정보 반영

→ 기술부채 종료

초보 개발자와 리뷰어가 Merge 전에 마지막으로 확인해야 할 질문은 다음과 같습니다.

이 변경은 어떤 요구사항을 해결하는가?

변경 범위와 부수 변경이 구분되어 있는가?

ServiceId의 목적이 명확한가?

Handler·Facade·Service·Rule의 책임이 올바른가?

공통모듈이 업무모듈을 참조하지 않는가?

Request 사용자·지점정보를 신뢰하지 않는가?

기능권한과 데이터권한을 모두 확인하는가?

Transaction 경계와 Rollback이 실제로 검증되는가?

외부 호출이 DB Connection을 장기 점유하지 않는가?

Timeout과 Retry의 전체 시간예산이 적절한가?

업무 예외와 시스템 예외가 구분되는가?

수정 SQL에 상태·Version 조건이 있는가?

영향 건수 0건과 다건을 처리하는가?

목록조회에 기간·Paging·최대건수가 있는가?

N+1 SQL·외부 호출이 없는가?

대량 객체와 무제한 Cache가 없는가?

정상뿐 아니라 권한·Rollback·Timeout·동시성을 테스트하는가?

리팩터링 전 기존 동작을 테스트로 고정했는가?

구·신 Version 혼재 시 호환되는가?

OM·Metric·Alert·Runbook이 변경되었는가?

설계서와 추적성 매트릭스가 Source와 일치하는가?

미해결 리뷰 의견에 책임자와 기한이 있는가?

반복 위반사항을 자동 Gate로 전환했는가?

이 질문에 답할 수 있다면 단순히 코드가 실행되는지를 확인하는 수준을 넘어, NSIGHT TCF의 아키텍처·보안·데이터·운영 기준을 만족하는지 검증하고 기존 프로그램을 안전하게 개선할 수 있습니다.
