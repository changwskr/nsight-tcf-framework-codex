<!-- source: ztcf-집필본/NSIGHT TCF Chapter 부록 G. ADR 작성 예시.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 확장 부록 G. ADR 작성 예시

## 도입 전 안내말

아키텍처 설계 과정에서는 하나의 문제에 여러 해결방법이 존재한다.

예를 들어 외부 고객정보 조회가 간헐적으로 지연될 때 다음과 같은 선택이 가능하다.

재시도하지 않는다.

1회만 재시도한다.

3회 재시도한다.

Cache 데이터를 반환한다.

비동기 방식으로 전환한다.

전체 Timeout을 늘린다.

외부 연계를 제거하고 데이터를 복제한다.

이 가운데 어떤 선택이 절대적으로 옳다고 단정하기는 어렵다.

프로젝트의 응답시간 목표, 데이터 정합성, 업무 중요도, 장애복구 방식, 운영역량, 보안정책과 현재 기술제약에 따라 판단이 달라지기 때문이다.

문제는 시간이 지나면 당시 선택의 배경이 사라진다는 점이다.

왜 Gateway를 독립 Module로 분리했는가?

왜 업무별 Controller 대신 ServiceId Dispatcher를 사용하는가?

왜 업무 WAR가 다른 업무 Table을 직접 조회하면 안 되는가?

왜 재시도를 1회로 제한했는가?

왜 Redis가 아닌 JDBC Session을 선택했는가?

왜 한 Tomcat에 여러 WAR를 배포했는가?

왜 tcf-batch를 tcf-om에서 분리했는가?

결정의 근거가 남아 있지 않으면 다음 문제가 반복된다.

새로운 담당자가 과거 논의를 다시 시작한다.

이미 검토해 탈락한 대안이 다시 제안된다.

코드는 변경됐지만 운영설정은 그대로 남는다.

설계서와 실제 구현이 서로 다른 방향으로 발전한다.

장애가 발생해도 어떤 가정을 전제로 설계했는지 알 수 없다.

임시 결정이 영구 표준으로 굳어진다.

이를 방지하기 위한 문서가 ADR이다.

ADR
Architecture Decision Record

중요한 아키텍처 의사결정의
맥락·대안·선택·근거·영향을 기록하는 문서

ADR은 회의록이 아니다.

회의록
\=
누가 무슨 말을 했는가

ADR
\=
어떤 문제에 대해
어떤 대안을 비교했고
무엇을 선택했으며
왜 그렇게 결정했는가

ADR은 상세 설계서 전체를 대신하지도 않는다.

ADR
결정과 근거를 기록

설계서
결정된 구조의 상세 구현방법을 정의

Runbook
운영자가 수행할 절차를 정의

RTM
요구사항·설계·구현·테스트를 연결

ADR의 핵심 목적은 다음과 같다.

미래의 개발자와 운영자가

결정된 내용뿐 아니라

그 결정을 내린 당시의
문제·제약·대안·위험을 이해하도록 하는 것

# 문서 개요

## 목적

본 부록의 목적은 NSIGHT TCF 프로젝트에서 중요한 아키텍처 판단을 일관된 형식으로 기록하고, 설계·구현·배포·운영·폐기까지 추적할 수 있는 ADR 작성기준과 실무 예시를 제공하는 것이다.

세부 목적은 다음과 같다.

중요한 아키텍처 결정의 배경 보존

검토한 대안과 제외 사유 기록

정량·정성 평가기준 명확화

결정권자와 책임경계 명확화

긍정적·부정적 결과 사전 인식

결정에 따른 후속 구현과제 연결

성능·보안·운영 검증방법 정의

코드·설정·문서 간 정합성 유지

결정 변경과 폐기 이력 보존

반복적인 아키텍처 논쟁 방지

신규 개발자와 운영자 교육자료 제공

감사·장애·변경 영향 분석 근거 제공

## 적용범위

| 영역 | ADR 작성대상 예 |
| --- | --- |
| 전체 구조 | Gateway·업무 WAR·OM 배치구조 |
| Module | tcf-core, tcf-web, tcf-eai 분리 |
| 거래 | Handler·ServiceId Dispatcher |
| 배포 | 단일 Tomcat 다중 WAR·업무그룹 분리 |
| 데이터 | 데이터 소유권·History·Outbox |
| Transaction | Local Transaction·JTA·보상처리 |
| 인증 | Session·JWT·SSO·Gateway 검증 |
| 성능 | Thread·DB Pool·Timeout |
| 연계 | 동기·비동기·Retry·Circuit Breaker |
| Cache | Local Cache·분산 Cache |
| 운영 | OM·Prometheus·자체 관측 |
| 로그 | 거래로그·감사로그 저장방식 |
| 배포 | Rolling·Blue-Green·Rollback |
| 표준 | 명명규칙·ServiceId·오류코드 |
| 폐기 | 레거시 Module·ServiceId 제거 |

## 대상 독자

애플리케이션 아키텍트

시스템 아키텍트

데이터 아키텍트

보안 아키텍트

Framework 개발자

업무 개발리더

DevOps·WAS 운영자

DBA

보안 담당자

QA·성능 담당자

PMO·의사결정 승인자

## 선행조건

문제와 적용범위가 정의돼 있어야 한다.

현행구조와 문제점이 파악돼 있어야 한다.

기능·비기능 요구사항이 존재해야 한다.

최소 두 개 이상의 대안이 검토돼야 한다.

주요 제약조건이 명시돼야 한다.

결정권자와 검토자가 지정돼야 한다.

검증방법과 완료조건을 정의할 수 있어야 한다.

# 핵심 관점

좋은 ADR은
결정을 그럴듯하게 정당화하는 문서가 아니다.

대안을 공정하게 비교하고,

선택한 결정의 단점과 위험까지 공개하며,

언제 그 결정을 다시 검토해야 하는지
명확히 기록하는 문서다.

# 주요 용어

| 용어 | 정의 |
| --- | --- |
| ADR | 개별 아키텍처 의사결정 기록 |
| FAD | 여러 핵심 ADR을 종합한 최종 아키텍처 결정서 |
| Decision Driver | 의사결정에 영향을 주는 핵심 요구·품질속성 |
| Context | 결정을 필요로 만든 환경과 문제 |
| Constraint | 선택 가능한 대안을 제한하는 조건 |
| Assumption | 현재 사실로 간주했으나 검증이 필요한 전제 |
| Option | 검토 가능한 대안 |
| Decision | 최종 선택한 대안 |
| Rationale | 해당 대안을 선택한 이유 |
| Consequence | 결정으로 인해 발생하는 긍정·부정 결과 |
| Trade-off | 한 속성을 얻기 위해 다른 속성을 포기하는 관계 |
| Mitigation | 부정적 결과나 위험을 줄이는 조치 |
| Validation | 결정이 목표를 달성하는지 확인하는 검증 |
| Revisit Trigger | 결정을 다시 검토해야 하는 조건 |
| Supersede | 새로운 ADR이 기존 결정을 대체하는 것 |
| Deprecated | 폐기 예정이지만 호환을 위해 유지하는 상태 |
| Accepted Risk | 완전히 제거하지 못하고 승인한 잔여위험 |
| Decision Owner | 결정의 유지·재검토에 책임을 지는 주체 |

# G.1 ADR이 필요한 경우

## 반드시 ADR을 작성해야 하는 경우

여러 업무 WAR에 영향을 주는 Framework 변경

서비스 간 책임경계 변경

업무코드·ServiceId 체계 변경

Gateway·Session·JWT 인증방식 변경

업무 WAR 배포단위 변경

DB 소유권 또는 Schema 경계 변경

Transaction 방식 변경

외부 연계의 동기·비동기 전환

Timeout·Retry·Circuit Breaker 공통정책 변경

Cache 기술 또는 정합성 방식 변경

로그·감사 저장구조 변경

운영 모니터링 도구 변경

CI/CD와 배포전략 변경

중대한 보안예외 허용

기존 표준의 예외 적용

레거시 Module·API·Table 폐기

## ADR을 작성하지 않아도 되는 경우

업무규칙에 따른 일반적인 조건문 추가

기존 표준을 그대로 적용한 단순 CRUD

문자 오탈자 수정

Test Case 추가

기존 Class 내부의 작은 Refactoring

승인된 표준 안에서의 상세 SQL 튜닝

단순 환경값 조정

다만 작은 변경처럼 보여도 여러 시스템의 계약·운영·보안에 영향을 주면 ADR 대상이 된다.

## 판단 질문

다음 질문 중 하나 이상이 예라면 ADR 작성 여부를 검토한다.

여러 팀이나 Module에 영향을 주는가?

되돌리기 어렵거나 비용이 큰가?

장기간 유지될 기술표준인가?

비기능 요구사항에 영향을 주는가?

데이터 손실·보안·가용성 위험이 있는가?

여러 대안 사이의 Trade-off가 존재하는가?

기존 프로젝트 표준을 벗어나는가?

향후 담당자가 왜 그렇게 했는지 물을 가능성이 높은가?

# G.2 ADR과 다른 문서의 차이

| 문서 | 핵심 질문 | 주요 내용 |
| --- | --- | --- |
| ADR | 왜 이 대안을 선택했는가? | 맥락·대안·결정·결과 |
| 아키텍처 설계서 | 시스템은 어떻게 구성되는가? | 구성요소·흐름·인터페이스 |
| 프로그램 설계서 | 어떤 Class와 Method로 구현하는가? | 프로그램 구조 |
| 회의록 | 회의에서 무엇을 논의했는가? | 참석자·발언·Action |
| 변경요청서 | 무엇을 왜 변경하는가? | 범위·일정·비용 |
| RTM | 요구사항이 어디에 반영됐는가? | 요구–설계–Test 연결 |
| Runbook | 운영자가 어떻게 조치하는가? | 실행절차·판단조건 |
| PIR | 장애가 왜 발생했는가? | 원인·타임라인·개선 |

하나의 결정은 다음과 같이 연결된다.

회의록
논의 과정

→ ADR
최종 결정과 근거

→ 설계서
상세 구조

→ 구현·설정

→ RTM·테스트

→ Runbook
운영절차

# G.3 ADR 식별자와 파일명

## G.3.1 권장 ID

ADR-{4자리 순번}

예:

ADR-0001

ADR-0002

ADR-0125

업무별 ADR Repository를 별도로 관리하는 경우:

ADR-CT-0001

ADR-TCF-0001

ADR-SEC-0001

프로젝트 전체 결정의 중복을 줄이기 위해 NSIGHT TCF에서는 중앙 순번을 기본으로 권장한다.

## G.3.2 파일명

ADR-{번호}-{영문-slug}.md

예:

ADR-0001-handler-serviceid-dispatch.md

ADR-0002-gateway-independent-stack.md

ADR-0003-customer-lookup-timeout-retry.md

## G.3.3 권장 저장위치

docs/
└─ architecture/
└─ adr/
├─ README.md
├─ ADR-0001-handler-serviceid-dispatch.md
├─ ADR-0002-gateway-independent-stack.md
└─ ADR-0003-customer-lookup-timeout-retry.md

## G.3.4 ADR Index

README.md 또는 별도 ADR 대장에 다음을 관리한다.

| ID | 제목 | 상태 | 결정일 | Owner | 대체 ADR |
| --- | --- | --- | --- | --- | --- |
| ADR-0001 | Handler+ServiceId Dispatcher | Accepted | 2026-06-28 | AA | \- |
| ADR-0002 | Gateway 독립 Stack | Accepted | 2026-06-28 | AA | \- |
| ADR-0003 | 고객조회 Timeout·Retry | Accepted | 2026-07-18 | 연계 AA | \- |

# G.4 기존 ADR 번호와 신규 표준

현재 아키텍처 요약자료에는 다음과 같은 기존 ADR 식별자가 존재한다.

| 기존 ID | 결정 |
| --- | --- |
| ADR-01 | common-core/web을 tcf-core/web으로 변경 |
| ADR-02 | Handler와 ServiceId Dispatcher 사용 |
| ADR-03 | HTTP 상태와 내부 resultCode 병행 |
| ADR-04 | 운영관리를 tcf-om으로 통합 |
| ADR-05 | 운영 수집 Batch를 tcf-batch로 분리 |
| ADR-06 | tcf-ui Relay 사용 |
| ADR-07 | 통합 Tomcat 다중 WAR 배포 |
| ADR-08 | 로컬 공유 거래로그 DB 경로 사용 |
| ADR-09 | tcf-integration을 tcf-eai로 변경 |
| ADR-10 | tcf-gateway 독립 Stack |
| ADR-11 | 운영·플랫폼 설계서 분리 |
| ADR-12 | 명명규칙을 단일 기준으로 통합 |

기존 승인 ID를 임의로 다시 번호 매기지 않는다.

전환이 필요하면 Alias 표를 둔다.

| 기존 ID | 신규 ID | 상태 |
| --- | --- | --- |
| ADR-01 | ADR-0001 | Alias |
| ADR-02 | ADR-0002 | Alias |
| ADR-03 | ADR-0003 | Alias |

원본 문서·Commit·회의록에서 기존 ID가 사용되고 있다면 검색 가능성을 위해 두 값을 함께 유지한다.

# G.5 ADR 상태 생명주기

## G.5.1 상태

| 상태 | 의미 |
| --- | --- |
| Draft | 작성 중 |
| Proposed | 공식 검토 요청 |
| Under Review | 관계자 검토 중 |
| Accepted | 승인돼 적용해야 하는 결정 |
| Rejected | 검토 후 채택하지 않음 |
| Deferred | 조건이 준비될 때까지 보류 |
| Superseded | 신규 ADR이 기존 결정을 대체 |
| Deprecated | 폐기 예정 |
| Withdrawn | 제안자가 철회 |
| Experimental | 제한 범위에서 시험 적용 |
| Implemented | 구현과 검증 완료 |
| Violated | 실제 구현이 ADR과 불일치 |
| Retired | 대상 기능 자체가 폐기됨 |

## G.5.2 권장 상태 흐름

Draft

→ Proposed

→ Under Review

→ Accepted
또는
Rejected
또는
Deferred

승인 후:

Accepted

→ Implemented

→ Superseded
또는
Deprecated
또는
Retired

## G.5.3 승인된 ADR 수정

승인된 ADR의 결정내용을 조용히 수정하지 않는다.

허용:

오탈자

깨진 링크

표현 명확화

구현상태·검증결과 추가

결정 자체가 바뀌면 신규 ADR을 작성한다.

ADR-0003
고객조회 1회 재시도

→ ADR-0042
고객조회 비동기 조회모델 전환

ADR-0042이 ADR-0003을 Supersede

# G.6 ADR 표준 형식

\# ADR-0000. 결정 제목

\## 상태
Proposed | Accepted | Rejected | Superseded

\## 메타정보
\- 작성일:
\- 결정일:
\- Decision Owner:
\- 작성자:
\- 검토자:
\- 승인자:
\- 적용범위:
\- 관련 요구사항:
\- 관련 ADR:
\- 대체 대상:
\- 대체 ADR:

\## 1. 결정 요약
한두 문장으로 최종 결정을 작성한다.

\## 2. 문제와 맥락
왜 결정이 필요한지 설명한다.

\## 3. 결정 동인
성능·가용성·보안·운영성 등 판단기준을 작성한다.

\## 4. 전제와 제약
현재 사실로 간주한 전제와 선택을 제한하는 조건을 작성한다.

\## 5. 검토 대안
각 대안의 구조·장점·단점·위험을 작성한다.

\## 6. 평가
동일한 기준으로 대안을 비교한다.

\## 7. 결정
선택한 대안과 적용범위를 작성한다.

\## 8. 결정 근거
왜 다른 대안보다 적합한지 작성한다.

\## 9. 결과와 영향
긍정적·부정적 결과를 모두 작성한다.

\## 10. 위험과 완화방안
위험·발생조건·통제방안을 작성한다.

\## 11. 구현계획
코드·설정·DB·운영 변경을 작성한다.

\## 12. 검증방법
Test·Metric·완료조건을 작성한다.

\## 13. Rollback·대체계획
결정 실패 시 복구방법을 작성한다.

\## 14. 재검토 조건
언제 ADR을 다시 검토할지 작성한다.

\## 15. 추적성
설계·소스·Test·Runbook·Commit을 연결한다.

\## 16. 변경이력
ADR 문서의 수정이력을 작성한다.

# G.7 ADR 메타정보

| 속성 | 필수 | 설명 |
| --- | --- | --- |
| ADR ID | ● | 고유번호 |
| 제목 | ● | 결정형 문장 |
| 상태 | ● | 생명주기 |
| 작성일 | ● | 초안일 |
| 결정일 | ● | 승인일 |
| Decision Owner | ● | 유지·재검토 책임 |
| 작성자 | ● | 초안 작성 |
| 검토자 | ● | 기술·운영·보안 검토 |
| 승인자 | ● | 최종 승인 |
| 적용범위 | ● | Module·업무·환경 |
| 관련 요구사항 | ● | REQ ID |
| 관련 설계 | ● | 설계서 |
| 관련 ADR | ○ | 선행·의존 |
| Supersedes | ○ | 대체하는 ADR |
| Superseded By | ○ | 대체한 ADR |
| Target Release | ○ | 적용 Release |
| Review Date | ○ | 정기 재검토일 |

# G.8 제목 작성기준

## 좋은 제목

외부 고객조회 연계에 1회 조건부 재시도를 적용한다.

업무 거래는 Handler와 ServiceId Dispatcher를 사용한다.

Gateway는 세션을 소유하지 않는 독립 Stack으로 구성한다.

업무 WAR 간 데이터 변경은 소유 도메인 API를 경유한다.

상담예약 변경은 Version 기반 낙관적 Lock을 적용한다.

## 좋지 않은 제목

Gateway 검토

Timeout 건

세션 관련

아키텍처 변경

성능 개선

제목만 읽어도 무엇을 결정했는지 알 수 있어야 한다.

# G.9 맥락과 문제 작성

맥락에는 다음 내용을 포함한다.

현행 구조

발생한 문제

업무 영향

기술적 영향

기존 결정

결정이 필요한 시점

결정하지 않을 경우의 결과

좋은 예:

고객조회는 상담예약 등록의 선행조건이다.

외부 고객 시스템은 평균 150ms에 응답하지만,
피크시간에 일시적인 연결 실패와 503이 발생한다.

화면 전체 응답목표는 p95 3초이며,
고객조회는 읽기 전용으로 멱등하다.

현재 Client는 Retry 없이 실패하므로
일시적인 연결오류가 상담예약 실패로 직접 전파된다.

좋지 않은 예:

외부 시스템이 가끔 느려서 Retry가 필요하다.

# G.10 결정 동인

결정 동인은 대안을 평가하는 공통 기준이다.

대표 품질속성:

업무 적합성

응답시간

처리량

가용성

복구 가능성

데이터 정합성

중복 방지

보안

개인정보

운영성

관측성

확장성

변경 용이성

구현 복잡도

비용

일정

결정 동인은 우선순위와 측정방법을 포함해야 한다.

| 동인 | 우선순위 | 측정방법 |
| --- | --- | --- |
| 화면 응답시간 | Critical | p95 3초 |
| 일시 장애 회복 | High | 실패주입 성공률 |
| 중복 안전성 | Critical | 멱등성 Test |
| 자원 보호 | High | Thread·Pool 사용률 |
| 운영 추적 | High | Metric·GUID |
| 구현 복잡도 | Medium | 개발·운영 공수 |

# G.11 전제와 제약조건

## 전제

전제는 현재 사실로 사용하지만 변경될 수 있다.

고객조회 API는 읽기 전용이다.

동일 조회의 반복 호출은 데이터를 변경하지 않는다.

외부 시스템의 정상 p95는 500ms 이하다.

전체 화면 응답목표는 3초다.

Gateway·TCF가 GUID를 전달한다.

## 제약

제약은 대안 선택을 제한한다.

화면 전체 Timeout은 3초를 초과할 수 없다.

외부 고객 시스템을 직접 변경할 수 없다.

신규 Messaging Platform을 현재 Release에 도입할 수 없다.

운영 Redis를 사용할 수 없다.

개인정보를 Local Cache에 장기 저장할 수 없다.

업무 변경거래는 자동 Retry할 수 없다.

## 전제 검증

전제가 틀리면 결정도 잘못될 수 있다.

따라서 ADR에는 전제의 검증방법을 기록한다.

| 전제 | 검증 |
| --- | --- |
| 조회 멱등 | 제공 API 계약 확인 |
| p95 500ms | 운영 Metric |
| 503 일시 오류 | 최근 장애 통계 |
| 3초 목표 | NFR Baseline |
| 중복효과 없음 | 반복 호출 Test |

# G.12 대안 작성기준

각 대안은 동일한 수준으로 설명한다.

구조

동작방식

장점

단점

전제

위험

비용

운영영향

적용조건

선택한 대안만 자세히 쓰고 나머지는 이름만 나열해서는 안 된다.

## 대안 수

일반적으로 다음을 포함한다.

현행 유지

최소 변경안

권장안

장기 목표안

대안이 하나뿐이라면 실제로 결정을 내리는 것인지, 이미 결정된 구현을 사후 정당화하는 것인지 확인한다.

# G.13 대안 평가방식

## G.13.1 단순 비교표

| 기준 | 대안 A | 대안 B | 대안 C |
| --- | --- | --- | --- |
| 성능 | 좋음 | 보통 | 낮음 |
| 가용성 | 낮음 | 좋음 | 매우 좋음 |
| 복잡도 | 낮음 | 보통 | 높음 |
| 비용 | 낮음 | 보통 | 높음 |

단순 표는 초기 논의에는 유용하지만 최종 판단근거로는 부족할 수 있다.

## G.13.2 가중치 평가

점수
1 매우 불리

2 불리

3 보통

4 유리

5 매우 유리

계산:

대안 점수

\=
Σ
평가기준 가중치
× 대안 점수

가중치는 합계 100으로 작성한다.

## G.13.3 점수의 한계

점수가 가장 높다고 자동으로 선택하는 것은 아니다.

다음은 별도 Fail-fast 조건으로 관리한다.

보안정책 위반

법규 위반

데이터 손실 가능

RTO·RPO 미충족

승인되지 않은 기술 사용

운영조직이 지원할 수 없음

# G.14 원본 ADR 작성 예시

| 항목 | 기록 예 |
| --- | --- |
| 제목 | 외부 고객조회 연계의 Timeout과 재시도 정책 |
| 상태 | 승인됨 |
| 맥락 | 화면 응답 목표 3초, 외부 시스템 간헐 지연, 조회 API는 멱등 |
| 결정 | 연결 300ms, 읽기 1.5초, 최대 1회 지수 백오프 재시도 |
| 대안 | 재시도 없음 / 3회 재시도 / 비동기 전환 |
| 결과 | 최대 지연 증가를 제한하며 일시 오류 일부 흡수 |
| 위험 | 재시도 폭주 가능성, 전체 Timeout 예산 초과 가능성 |
| 검증 | 부하·지연·실패 주입 테스트와 운영 Metric Alert |

이 예시는 ADR의 핵심 구조를 보여준다.

다음 절에서는 이를 NSIGHT TCF 실무 수준으로 확장한다.

# G.15 완성형 ADR 예시

# ADR-0013. 외부 고객조회 연계에 1회 조건부 재시도를 적용한다

## 상태

Accepted

| 항목 | 내용 |
| --- | --- |
| 작성일 | 2026-07-15 |
| 결정일 | 2026-07-18 |
| Decision Owner | 연계 아키텍트 |
| 작성자 | CT 업무개발 리더 |
| 검토자 | AA·운영·성능·보안 |
| 승인자 | NSIGHT 아키텍처 책임자 |
| 적용범위 | ct-service → ic-service 고객조회 |
| Target Release | CT 1.0.0 |
| 관련 요구사항 | REQ-CT-RSV-003, REQ-CT-RSV-013 |
| 관련 설계 | 외부연계·Timeout 설계서 |
| 관련 ADR | Gateway·TCF Timeout 정책 ADR |

## 1\. 결정 요약

상담예약 등록 전에 수행하는 외부 고객조회는 다음 정책을 적용한다.

Connect Timeout
300ms

Read Timeout
1.5초

최대 재시도
1회

재시도 간격
100ms 지수 Backoff
\+ 0~50ms Jitter

재시도 대상
연결 실패
연결 초기화
응답 Body 수신 전 502·503

재시도 제외
업무 오류
401·403·404
Read Timeout
Validation 오류
전체 잔여시간 부족

전체 외부연계 예산
TCF 전체 5초 안의 2.2초 이내

Retry는 첫 실패가 빠르게 발생하고 다음 시도를 수행할 충분한 전체 Timeout 잔여시간이 있을 때만 실행한다.

## 2\. 문제와 맥락

상담예약 등록거래는 예약 대상 고객의 유효성을 확인하기 위해 IC 고객조회 API를 호출한다.

현행 고객조회는 다음 특성이 있다.

읽기 전용

평균 응답시간 약 150ms

정상 p95 500ms 이하

피크시간 간헐적 연결 실패

일시적 502·503 발생

업무 오류는 명확한 오류코드 제공

현재 Client는 실패 시 즉시 오류를 반환한다.

이 때문에 실제 고객 시스템은 정상인데도 순간적인 연결 실패가 상담예약 등록 실패로 직접 전파된다.

화면 응답목표는 p95 3초이고 전체 TCF 변경거래 Timeout은 5초다.

재시도를 과도하게 적용하면 다음 문제가 발생한다.

화면 응답시간 증가

Tomcat Thread 점유

외부 시스템 부하 증가

동시 재시도 폭주

전체 Timeout 초과

장애 전파 확대

## 3\. 결정 동인

| 기준 | 가중치 | 목표 |
| --- | --- | --- |
| 일시 장애 회복 | 25 | 순간 연결오류 흡수 |
| 화면 응답시간 | 20 | p95 3초 |
| 중복 안전성 | 15 | 읽기 멱등 |
| 자원 보호 | 15 | Thread·Connection 폭주 방지 |
| 운영 추적성 | 15 | Retry·대상 오류 Metric |
| 구현·운영 복잡도 | 10 | 현재 Release 내 적용 가능 |
| 합계 | 100 |  |

## 4\. 전제

고객조회 API는 데이터를 변경하지 않는다.

동일 요청 반복은 같은 업무효과를 가진다.

외부 API 계약에 Request ID 중복 제한이 없다.

IC 시스템은 피크 부하에서 1회 추가 호출을 수용할 수 있다.

TCF가 전체 Timeout 잔여시간을 확인할 수 있다.

GUID·TraceId가 재시도 호출에도 전달된다.

## 5\. 제약조건

화면 p95 3초를 초과해서는 안 된다.

TCF 전체 Timeout 5초 안에 종료돼야 한다.

업무 오류는 재시도하지 않는다.

변경 API에는 동일 정책을 자동 적용하지 않는다.

신규 Message Broker 도입은 이번 Release 범위가 아니다.

외부 시스템의 소스를 변경할 수 없다.

## 6\. 검토 대안

### 대안 A. 재시도하지 않는다

외부 호출 1회

실패 시 즉시 표준 오류 반환

장점:

응답시간 예측이 쉽다.

외부 부하가 가장 낮다.

구현이 단순하다.

장애 시 Fail Fast한다.

단점:

순간적인 연결오류가 그대로 사용자 오류가 된다.

사용자가 수동으로 다시 시도하게 된다.

일시 장애 회복성이 낮다.

### 대안 B. 최대 1회 조건부 재시도

첫 호출이 재시도 가능 오류로 빠르게 실패하고

전체 Timeout 잔여시간이 충분할 때

Backoff·Jitter 후 1회 재호출

장점:

일시 오류 일부를 흡수한다.

최대 지연 증가를 제한한다.

현재 동기 구조를 유지한다.

운영 복잡도가 비교적 낮다.

단점:

호출량이 일부 증가한다.

재시도 조건이 잘못되면 전체 Timeout을 초과할 수 있다.

외부 장애 시 동시 재시도 가능성이 있다.

### 대안 C. 최대 3회 재시도

장점:

일시 장애 회복 가능성이 높다.

단점:

화면 3초 목표를 만족하기 어렵다.

장애 시 호출폭주를 유발할 수 있다.

업무 Thread와 Connection 점유가 증가한다.

장애 복구를 지연시킬 수 있다.

### 대안 D. 비동기 전환

예약요청을 접수하고

고객조회와 예약확정을 비동기로 처리

장점:

외부 지연이 화면 Thread를 장기 점유하지 않는다.

재처리·Queue 기반 복구가 가능하다.

대규모 확장성이 높다.

단점:

즉시 예약확정이 불가능하다.

PROCESSING 상태와 사용자 UX가 필요하다.

Queue·Event·보상처리가 필요하다.

현재 업무요건과 일정에 비해 복잡하다.

## 7\. 평가

점수는 1점에서 5점까지 사용한다.

| 기준 | 가중치 | A 없음 | B 1회 | C 3회 | D 비동기 |
| --- | --- | --- | --- | --- | --- |
| 일시 장애 회복 | 25 | 2 | 5 | 5 | 5 |
| 화면 응답시간 | 20 | 5 | 4 | 2 | 3 |
| 중복 안전성 | 15 | 5 | 5 | 5 | 5 |
| 자원 보호 | 15 | 5 | 4 | 2 | 4 |
| 운영 추적성 | 15 | 4 | 5 | 2 | 3 |
| 구현 복잡도 | 10 | 5 | 4 | 3 | 2 |
| 가중합 | 100 | 410 | **455** | 330 | 385 |
| 환산점수 | 100 | 82 | **91** | 66 | 77 |

## 8\. 결정

대안 B인 **최대 1회 조건부 재시도**를 선택한다.

적용조건:

조회 API에만 적용한다.

첫 실패가 재시도 가능한 기술오류여야 한다.

첫 실패 발생시점이 충분히 빨라야 한다.

전체 Timeout 잔여시간이
Connect+Read+Backoff 예산보다 커야 한다.

Circuit Breaker가 OPEN이면 재시도하지 않는다.

Retry 후에도 실패하면 즉시 표준 연계오류를 반환한다.

## 9\. 결정 근거

재시도 없음보다 일시 장애 회복성이 높다.

3회 재시도보다 응답시간과 자원점유 위험이 낮다.

비동기 전환보다 현재 업무요건과 일정에 적합하다.

조회 API이므로 반복 호출에 따른 데이터 중복위험이 없다.

Retry 대상 오류와 잔여 Timeout을 제한하면
재시도 폭주와 전체 Timeout 초과를 통제할 수 있다.

## 10\. 긍정적 결과

순간적인 연결오류 일부를 자동 회복한다.

사용자의 수동 재조회 빈도를 줄인다.

현재 동기 업무 흐름을 유지한다.

기존 tcf-eai Client 구조 안에서 구현할 수 있다.

Retry 횟수와 성공률을 운영 Metric으로 관찰할 수 있다.

## 11\. 부정적 결과

외부 장애 시 호출량이 최대 두 배까지 증가할 수 있다.

일부 거래의 응답시간이 증가한다.

Retry 조건과 전체 Timeout 계산 Logic이 추가된다.

운영자가 최초 실패와 최종 결과를 함께 분석해야 한다.

잘못된 오류분류가 재시도 폭주를 만들 수 있다.

## 12\. 위험과 완화방안

| 위험 | 발생조건 | 완화방안 |
| --- | --- | --- |
| Retry 폭주 | 외부 전체 장애 | 최대 1회·Circuit Breaker |
| Timeout 초과 | 첫 호출 장기지연 | Read Timeout은 재시도 제외 |
| 동시 집중 | 피크시간 | Jitter·Bulkhead |
| 오류 은폐 | Retry 후 성공 | 최초 실패 Metric 기록 |
| 외부 과부하 | 장애 중 호출 증가 | Rate Limit·Open Circuit |
| 설정 Drift | WAR별 다른 설정 | 중앙 Profile·자동검증 |
| 변경 API 오적용 | 공통 Client 무조건 Retry | Method·Policy별 명시 |

## 13\. 구현계획

| 영역 | 변경 |
| --- | --- |
| tcf-eai | Retry Policy Interface |
| 업무 Client | 고객조회 전용 Policy 지정 |
| Timeout | 잔여 Budget 계산 |
| 오류 | Retry 가능 오류 분류 |
| Metric | Attempt·Retry·Recovery |
| 로그 | Attempt 번호·최종 결과 |
| 설정 | Connect·Read·Retry 값 |
| OM | 외부연계 정책 조회 |
| Test | 지연·502·503·Timeout 주입 |
| Runbook | 고객조회 장애대응 |

설정 예:

nsight:
integration:
customer:
connect-timeout-ms: 300
read-timeout-ms: 1500
retry:
max-attempts: 2
initial-backoff-ms: 100
jitter-ms: 50
retryable-statuses:
\- 502
\- 503

max-attempts: 2는 최초 호출 1회와 재시도 1회를 의미한다.

설정 의미가 혼동되지 않도록 문서와 Configuration Property에 명확히 설명한다.

## 14\. 정상 처리 흐름

CT 예약등록

→ 고객조회 호출 Attempt 1

→ 150ms 정상응답

→ Retry 없음

→ 고객 유효성 확인

→ 예약 Master·History 저장

→ 성공응답

## 15\. 재시도 처리 흐름

CT 예약등록

→ 고객조회 Attempt 1

→ 200ms 시점 Connection Reset

→ 재시도 가능 오류 판정

→ 전체 잔여 Timeout 확인

→ Circuit CLOSED 확인

→ 100ms+Jitter 대기

→ Attempt 2

→ 정상응답

→ 예약 등록 계속

→ Retry Recovery Metric 기록

## 16\. 재시도하지 않는 흐름

### 업무 오류

고객조회

→ 고객 없음

→ 업무 오류

→ Retry 없음

→ E-CT-RSV-0007

### Read Timeout

고객조회

→ 1.5초 Read Timeout

→ 남은 화면 예산 부족

→ Retry 없음

→ 표준 연계 Timeout

### Circuit Open

고객조회

→ Circuit OPEN

→ 외부 호출 없음

→ Fail Fast

→ 운영 Alert

## 17\. 로그·Metric

필수 로그:

guid

traceId

targetSystem=IC

targetServiceId

attempt

elapsedMs

failureType

retryDecision

finalResult

금지:

고객정보 원문

Authorization Token

외부 Request·Response 전체

Metric:

tcf.integration.call.count

tcf.integration.retry.count

tcf.integration.retry.recovered.count

tcf.integration.timeout.count

tcf.integration.circuit.open.count

Label:

sourceBusinessCode

targetSystem

targetServiceId

resultType

attempt

GUID와 고객번호는 Metric Label로 사용하지 않는다.

## 18\. 검증방법

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| ADR13-T01 | 첫 호출 정상 | 1회 호출 |
| ADR13-T02 | Connect 실패 후 정상 | 2회·최종 성공 |
| ADR13-T03 | 503 후 정상 | 2회·최종 성공 |
| ADR13-T04 | Read Timeout | Retry 없음 |
| ADR13-T05 | 업무 오류 | Retry 없음 |
| ADR13-T06 | 401 | Retry 없음 |
| ADR13-T07 | Circuit Open | 호출 없음 |
| ADR13-T08 | 잔여 Budget 부족 | Retry 없음 |
| ADR13-T09 | 동시 1,000건 장애 | 폭주 없음 |
| ADR13-T10 | p95 부하 | 화면 3초 목표 |
| ADR13-T11 | Metric | Retry·Recovery 집계 |
| ADR13-T12 | 설정 불일치 | CI Gate 실패 |

완료조건:

정상 p95 3초 이하

Retry로 회복한 비율 측정 가능

외부 장애 시 호출배수 2 이하

Thread·Pool 임계치 초과 없음

전체 Timeout 초과 없음

개인정보 로그 없음

## 19\. Rollback 계획

문제가 발생하면 다음 순서로 복구한다.

OM·환경설정에서 Retry 비활성화

→ max-attempts=1

→ 설정 Reload 또는 WAR 재배포

→ 고객조회 성공률·p95 확인

→ 필요 시 직전 Artifact 복원

Retry 비활성화 후에도 고객조회 자체는 1회 수행된다.

## 20\. 재검토 조건

다음 중 하나가 발생하면 ADR을 재검토한다.

외부 고객조회 p95가 1초를 지속 초과

Retry 비율이 전체 호출의 5% 초과

Retry 후 회복률이 20% 미만

화면 p95 3초 목표 미충족

외부 시스템이 Retry 금지 정책을 통보

고객조회가 변경 API로 변경

비동기 업무접수 요구 추가

Cache·Read Model 도입

월 2회 이상 외부 장애 발생

## 21\. 추적성

요구사항
REQ-CT-RSV-003
REQ-CT-RSV-013

설계
TCF 도메인 연동 설계서
Timeout 설계서

소스
tcf-eai RetryPolicy
CtCustomerClient

테스트
ADR13-T01~T12

Metric
tcf.integration.retry.\*

Runbook
RB-CT-INT-001

Release
CT 1.0.0

# G.16 ADR 예시 2 — Handler와 ServiceId Dispatcher

## 제목

업무 온라인 거래는 업무별 Controller 대신
Handler와 ServiceId Dispatcher를 사용한다.

## 맥락

업무 거래마다 Controller를 생성하면 다음 문제가 발생한다.

공통 STF·ETF 우회

Endpoint 난립

인증·권한·Timeout 불일치

거래로그 누락

Controller별 응답형식 차이

운영 Service Catalog 추적 어려움

## 대안

| 대안 | 설명 |
| --- | --- |
| A | 거래마다 REST Controller |
| B | 도메인별 Controller |
| C | 단일 /online과 ServiceId Dispatcher |

## 결정

대안 C를 선택한다.

OnlineTransactionController

→ TCF.process()

→ TransactionDispatcher

→ TransactionHandler

→ Facade

## 긍정적 결과

모든 온라인 거래에 STF·ETF 적용

Endpoint 표준화

ServiceId 기반 통제·로그·권한

Handler 자동등록

업무 프로그램 추적성 향상

## 부정적 결과

ServiceId Registry 관리 필요

일반 REST 경험과 다름

파일·Streaming 같은 예외 Endpoint 별도 필요

Dispatcher 장애가 공통 장애가 될 수 있음

## 금지

일반 CRUD 편의를 이유로 독자 Controller 추가

Handler에서 Mapper 직접 호출

ServiceId 미등록 상태로 배포

표준 전문을 우회해 임의 Response 반환

# G.17 ADR 예시 3 — Gateway 독립 Stack

## 제목

tcf-gateway는 tcf-core에 의존하지 않는
독립 Gateway Pipeline으로 구성한다.

## 결정

Gateway
GRF → GSF → Route Dispatcher → GEF

업무 WAR
STF → Dispatcher → Handler → ETF

Gateway는 다음을 수행한다.

Route

JWT·Session 관문

Header 보정

Downstream Proxy

Gateway 거래로그

Gateway는 다음을 수행하지 않는다.

업무 Handler 실행

업무 DB 접근

JWT Private Key를 이용한 Token 발급

업무 Transaction 처리

## 근거

Gateway와 업무 Framework의 배포·변경주기 분리

업무 Class 의존 방지

관문 장애와 업무 장애의 책임 분리

Gateway 경량화

Security Boundary 명확화

## 위험

유사한 STF·ETF 개념이 중복될 수 있음

Gateway와 업무 로그 연결 필요

공통 Header 규격 Drift 가능

인증정책 이중관리 가능

## 완화

공통 계약 Module 최소화

GUID·TraceId 표준

Contract Test

Gateway·업무 Header 정합 Test

보안정책 단일 Baseline

# G.18 ADR 예시 4 — tcf-batch 분리

## 제목

운영 상태수집 Scheduler는
tcf-om 본체가 아닌 tcf-batch에서 실행한다.

## 문제

OM 본체에서 AP·DB·배포상태를 주기적으로 수집하면 다음 위험이 있다.

관리화면 요청과 수집작업 자원경합

수집 지연으로 OM 응답저하

Scheduler 장애가 OM 전체에 영향

운영기능과 Batch 배포주기 결합

## 대안

| 대안 | 설명 |
| --- | --- |
| A | tcf-om 내부 Scheduler |
| B | 별도 tcf-batch |
| C | Prometheus 등 외부 수집도구 |

## 결정

현재 단계에서는 대안 B를 선택한다.

장기적으로 외부 관측도구 도입 시 역할을 재검토한다.

## 결과

수집 부하 격리

Scheduler 재기동 독립

OM 화면과 저장소는 유지

Module 하나 증가

수집상태 자체 모니터링 필요

# G.19 ADR 예시 5 — 다중 WAR 배포

## 제목

업무 WAR는 업무코드별로 분리하고
업무그룹별 Tomcat에 함께 배포한다.

## 대안

| 대안 | 설명 |
| --- | --- |
| A | 모든 업무를 단일 WAR로 통합 |
| B | 하나의 Tomcat에 업무별 WAR 배포 |
| C | WAR마다 독립 Tomcat |
| D | 업무별 Container·Kubernetes |

## 결정

현재 전환단계에서는 업무별 WAR를 유지하고 업무그룹 단위 Tomcat에 배포한다.

업무코드별

WAR·Context·ServiceId 책임 분리

운영단위

업무그룹별 Tomcat

장기적으로 자원독점·장애격리가 필요한 업무는 독립 Instance로 분리할 수 있다.

## Trade-off

| 확보 | 포기·부담 |
| --- | --- |
| 업무별 배포 산출물 | JVM 자원은 일부 공유 |
| Context 격리 | Tomcat 장애는 공동 영향 |
| 단일 WAR보다 변경격리 | ClassLoader·운영 복잡도 |
| WAR별 DB Pool | DB Connection 총량 증가 |

# G.20 ADR 정상 작성 흐름

1\. 문제를 한 문장으로 정의한다.

2\. 결정이 필요한 이유와 시점을 기록한다.

3\. Decision Owner와 검토자를 지정한다.

4\. 요구사항·제약·전제를 수집한다.

5\. 현행 유지안을 포함해 대안을 작성한다.

6\. 모든 대안을 같은 기준으로 비교한다.

7\. 선택한 대안과 제외한 대안의 근거를 기록한다.

8\. 긍정적·부정적 결과를 모두 기록한다.

9\. 보안·데이터·운영·성능 영향을 검토한다.

10\. 구현·검증·Rollback 계획을 연결한다.

11\. Architecture Review를 수행한다.

12\. 승인 후 Git에 Merge한다.

13\. 구현 MR이 ADR을 참조하도록 한다.

14\. Test·Metric으로 결정 결과를 검증한다.

15\. 재검토 조건 발생 여부를 운영한다.

# G.21 오류·Timeout·장애 흐름

## ADR 검토 중 정보 부족

핵심 성능수치 없음

→ 추측으로 승인하지 않음

→ Spike·PoC·운영자료 수집

→ Experimental 또는 Deferred

## 결정 구현 실패

ADR Accepted

→ 구현 결과 목표 미달

→ ADR 상태 Violated 또는 재검토

→ 보완·Rollback·신규 ADR

## 전제 변경

외부 고객조회가 조회에서 변경 API로 전환

→ 멱등 전제 무효

→ 기존 Retry ADR 재검토

→ 변경거래 Retry 금지 또는 Idempotency 설계

## 운영 장애로 결정 부적합 확인

다중 WAR에서 특정 업무 자원독점

→ 장애증거 수집

→ 독립 Tomcat 대안 신규 ADR

→ 기존 ADR Supersede 검토

# G.22 정상 예시

제목
상담예약 수정에 Version 기반 낙관적 Lock을 적용한다.

맥락
동일 예약을 여러 사용자가 조회·수정할 수 있다.

대안
마지막 저장 우선
비관적 Lock
낙관적 Lock

평가기준
사용자 Think Time
DB Lock
Lost Update
운영 복잡도

결정
VERSION\_NO 조건 UPDATE

결과
Lost Update 방지
충돌 시 재조회 필요

검증
동시 사용자 A·B Test

재검토
충돌률이 10%를 지속 초과할 때

# G.23 금지 예시

결정을 이미 구현한 뒤 사후 정당화를 위해 ADR을 작성한다.

선택한 대안만 작성하고 다른 대안을 숨긴다.

대안의 단점을 기록하지 않는다.

“업계 표준이므로”만 근거로 사용한다.

정량자료 없이 무조건 성능이 좋다고 작성한다.

현재 제약과 가정을 구분하지 않는다.

승인자를 기록하지 않는다.

구현·검증·운영 후속조치가 없다.

보안·개인정보·감사 영향을 검토하지 않는다.

Rollback이 불가능한데 그 사실을 숨긴다.

승인된 ADR의 결론을 조용히 수정한다.

기존 ADR을 삭제하고 신규 결정을 덮어쓴다.

회의록을 ADR로 대신한다.

ADR 번호를 다른 결정에 재사용한다.

모든 작은 코딩 선택에 ADR을 작성한다.

상태가 Draft인 ADR을 운영표준으로 사용한다.

Rejected ADR을 삭제해 검토이력을 없앤다.

점수표 숫자만으로 자동 의사결정한다.

실제 코드가 ADR과 다른데 상태를 Accepted로 방치한다.

임시 보안예외에 만료일을 두지 않는다.

# G.24 연계 규칙

ADR은 다음 산출물과 연결해야 한다.

| 대상 | 연결정보 |
| --- | --- |
| 요구사항 | REQ ID |
| 설계서 | 문서명·절 |
| 코드 | Module·Class |
| 설정 | Property·Profile |
| DB | Table·Migration |
| ServiceId | Handler |
| 테스트 | Test ID |
| 결함 | Defect ID |
| 배포 | Release·Artifact |
| 운영 | Metric·Dashboard |
| Runbook | 대응 절차 |
| 후속 ADR | Supersedes 관계 |

MR 설명 예:

Architecture Decision

ADR-0013
외부 고객조회 연계에 1회 조건부 재시도를 적용한다.

Implementation

tcf-eai RetryPolicy
CtCustomerClient
application-dev.yml

Verification

ADR13-T01~T12

# G.25 데이터 및 상태관리

## ADR Registry 속성

| 속성 | 설명 |
| --- | --- |
| adrId | 고유 ID |
| title | 제목 |
| status | 상태 |
| decisionOwner | 책임자 |
| createdDate | 작성일 |
| decisionDate | 결정일 |
| reviewDate | 재검토일 |
| scope | 적용범위 |
| targetRelease | 적용 Release |
| supersedes | 대체 대상 |
| supersededBy | 신규 ADR |
| implementationStatus | 구현상태 |
| verificationStatus | 검증상태 |
| riskStatus | 잔여위험 |
| documentPath | 파일위치 |

## ADR와 구현상태 분리

ADR 상태
Accepted

구현상태
Not Started

검증상태
Not Tested

결정이 승인됐다고 구현까지 완료된 것은 아니다.

권장 상태 조합:

| ADR | 구현 | 검증 | 의미 |
| --- | --- | --- | --- |
| Accepted | Not Started | Not Tested | 결정만 승인 |
| Accepted | In Progress | Partial | 구현 중 |
| Accepted | Completed | Passed | 완료 |
| Accepted | Completed | Failed | 결정 재검토 |
| Superseded | Deprecated | N/A | 대체 진행 |

# G.26 성능·용량·확장성

성능 관련 ADR에는 최소한 다음을 포함한다.

기준 거래량

목표 TPS

평균·p95·p99

동시성

Thread·Connection 사용량

Queue

최대 지연

장애 시 호출배수

용량 증가계획

부하시험 조건

예:

Retry 1회 적용 시

정상상태
호출배수 약 1.00

Retry 비율 2%
호출배수 약 1.02

전체 외부 장애
최대 호출배수 2.00

따라서 Retry ADR은 단순 횟수뿐 아니라 장애 시 최대 증폭률을 평가해야 한다.

확장성 질문:

업무가 두 배 증가해도 결정이 유효한가?

Instance가 4대에서 8대로 늘면 어떤 영향이 있는가?

Local Cache와 Lock은 확장 가능한가?

운영자가 늘어난 구성요소를 관리할 수 있는가?

DB Connection 총량은 어떻게 증가하는가?

# G.27 보안·개인정보·감사

다음 결정은 보안 검토자를 필수 RACI에 포함한다.

인증·Session·JWT

개인정보 저장·복제

로그·감사

외부 연계

파일 업·다운로드

Cache

관리 Endpoint

Secret·Key

Gateway 우회

운영 권한

ADR에 기록할 보안 항목:

Trust Boundary

인증주체

인가 위치

암호화

Key 소유권

개인정보 최소화

로그 Masking

감사대상

위협

보안 Test

잔여위험

보안예외 ADR에는 다음이 필수다.

예외 대상

불가피한 사유

공격 가능성

영향범위

대체 통제

승인자

Owner

만료일

해소계획

# G.28 운영·모니터링·장애 대응

ADR은 운영자가 결정의 성공·실패를 판단할 수 있어야 한다.

필수 운영정보:

Metric

Alert

Dashboard

로그 Event

운영 Threshold

Runbook

Rollback 조건

재검토 Trigger

예:

| Metric | 정상 | 경고 | 재검토 |
| --- | --- | --- | --- |
| Retry 비율 | 1% 미만 | 3% | 5% |
| Retry 회복률 | 50% 이상 | 30% | 20% 미만 |
| 외부 p95 | 500ms | 1초 | 1.5초 |
| 화면 p95 | 3초 이하 | 3초 | 지속 초과 |

운영 결과가 결정 당시 전제와 다르면 ADR을 다시 검토한다.

# G.29 책임 경계와 RACI

| 활동 | AA | 업무 | Framework | 보안 | DBA | 운영 | DevOps | QA | PMO |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 문제 정의 | A/R | R/C | C | C | C | C | C | C | I |
| 대안 작성 | A/R | C | R/C | C | C | C | C | C | I |
| 평가기준 | A/R | C | C | C | C | C | C | C | I |
| 보안 검토 | C | C | C | A/R | I | C | C | C | I |
| 데이터 검토 | C | C | C | C | A/R | C | I | C | I |
| 운영 검토 | C | C | C | C | C | A/R | C | C | I |
| 최종 승인 | A | C | C | C | C | C | C | C | A/C |
| 구현 | C | R | R | C | C | C | R/C | C | I |
| 검증 | C | C | C | C | C | C | C | A/R | I |
| 상태 갱신 | A/R | C | C | C | C | C | C | C | I |
| 재검토 | A/R | C | C | C | C | C | C | C | I |
| 폐기 | A | C | C | C | C | A/C | R/C | C | C |

# G.30 자동검증 및 품질 Gate

## G.30.1 문서형식 검사

자동검증:

ADR ID 존재

제목 존재

상태 유효

Decision Owner 존재

최소 2개 대안

결정 존재

긍정·부정 결과 존재

검증방법 존재

재검토 조건 존재

관련 요구사항 존재

## G.30.2 파일명 검사

정규식 예:

^ADR-\[0-9\]{4}-\[a-z0-9-\]+\\.md$

## G.30.3 링크 검사

관련 ADR 파일 존재

요구사항 ID 존재

설계서 링크 유효

Test ID 존재

Supersedes 양방향 연결

코드 경로 유효

## G.30.4 코드리뷰 Gate

다음 변경에는 ADR 링크가 없으면 Merge를 차단한다.

tcf-core 공개 API 변경

Module 의존성 변경

ServiceId 명명체계 변경

Gateway 인증구조 변경

공통 Timeout·Retry 변경

DB 소유권 변경

Session 저장방식 변경

배포단위 변경

공통 보안예외

## G.30.5 구현 정합성 검사

예:

ADR
Gateway는 tcf-core 미의존

자동검사
tcf-gateway build.gradle에
tcf-core Dependency가 있으면 실패

ADR
Handler는 Facade만 호출

자동검사
Handler→Mapper 의존 시 실패

# G.31 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| ADR-T01 | ADR ID 누락 | Gate 실패 |
| ADR-T02 | 중복 ID | Gate 실패 |
| ADR-T03 | 상태 오타 | Gate 실패 |
| ADR-T04 | 대안 1개 | Review 실패 |
| ADR-T05 | 부정적 결과 누락 | Review 실패 |
| ADR-T06 | Decision Owner 없음 | Review 실패 |
| ADR-T07 | 요구사항 링크 없음 | Review 실패 |
| ADR-T08 | 관련 파일 링크 오류 | CI 실패 |
| ADR-T09 | Accepted ADR 결론 직접 수정 | Review 차단 |
| ADR-T10 | 신규 ADR로 Supersede | 양방향 연결 |
| ADR-T11 | Rejected ADR 보존 | Index 유지 |
| ADR-T12 | 코드가 ADR 위반 | Architecture Test 실패 |
| ADR-T13 | Retry 정책 정상 | 결정대로 실행 |
| ADR-T14 | Retry 3회 설정 | Gate 실패 |
| ADR-T15 | Read Timeout Retry | 정책 위반 |
| ADR-T16 | 운영 Metric 미수집 | 구현 미완료 |
| ADR-T17 | 재검토 Trigger 초과 | Review 생성 |
| ADR-T18 | 보안예외 만료 | 배포 차단 |
| ADR-T19 | Legacy ADR Alias | 검색 가능 |
| ADR-T20 | ADR·FAD 불일치 | Architecture Review |

# G.32 ADR 검토 체크리스트

## 문제와 맥락

| 점검 | 완료 |
| --- | --- |
| 결정이 필요한 문제가 명확하다. | □ |
| 현행 구조를 설명한다. | □ |
| 결정하지 않을 경우의 영향이 있다. | □ |
| 적용범위와 제외범위가 있다. | □ |
| 긴급성과 결정시점이 명확하다. | □ |

## 요구·전제·제약

| 점검 | 완료 |
| --- | --- |
| 기능·비기능 요구가 있다. | □ |
| 결정 동인이 우선순위화됐다. | □ |
| 전제와 사실을 구분했다. | □ |
| 전제 검증방법이 있다. | □ |
| 기술·조직·일정 제약이 있다. | □ |

## 대안

| 점검 | 완료 |
| --- | --- |
| 현행 유지안을 포함했다. | □ |
| 최소 두 개 이상 대안이 있다. | □ |
| 모든 대안을 같은 기준으로 설명했다. | □ |
| 장점과 단점이 모두 있다. | □ |
| 장기 대안을 검토했다. | □ |

## 결정

| 점검 | 완료 |
| --- | --- |
| 선택한 대안이 명확하다. | □ |
| 적용조건과 예외가 있다. | □ |
| 선택 근거가 요구사항과 연결된다. | □ |
| 제외 대안의 사유가 있다. | □ |
| Decision Owner와 승인자가 있다. | □ |

## 결과·위험

| 점검 | 완료 |
| --- | --- |
| 긍정적 결과를 기록했다. | □ |
| 부정적 결과를 숨기지 않았다. | □ |
| 주요 위험이 있다. | □ |
| 완화방안이 있다. | □ |
| 잔여위험 승인 여부가 있다. | □ |

## 구현·검증

| 점검 | 완료 |
| --- | --- |
| 코드·설정·DB 변경이 있다. | □ |
| 운영변경이 있다. | □ |
| Test 시나리오가 있다. | □ |
| Metric과 완료조건이 있다. | □ |
| Rollback 방법이 있다. | □ |
| 재검토 조건이 있다. | □ |

## 추적성

| 점검 | 완료 |
| --- | --- |
| 요구사항과 연결된다. | □ |
| 설계서와 연결된다. | □ |
| 소스·MR과 연결된다. | □ |
| Test와 연결된다. | □ |
| Runbook과 연결된다. | □ |
| 관련 ADR 관계가 있다. | □ |

# G.33 변경·호환성·폐기 관리

## ADR 변경

승인 전:

같은 ADR에서 수정 가능

승인 후:

결정 내용 변경

→ 신규 ADR 작성

→ 기존 ADR Superseded

## 결정 범위 확장

기존 결정의 단순 적용이면 관련 ADR을 참조한다.

기존 결정의 전제·영향·책임을 바꾸면 신규 ADR이 필요하다.

## 호환성

ADR 변경 시 다음 호환성을 확인한다.

API

ServiceId

DB Schema

환경설정

로그 Schema

Metric

운영 Script

구·신 WAR

사용자 Session

외부 연계

## ADR 폐기

기능이 제거돼 결정 자체가 더 이상 의미가 없으면 Retired로 변경한다.

기존 결정을 새로운 결정이 대신하면 Superseded다.

Retired
대상 기능 자체가 사라짐

Superseded
대상 기능은 존재하지만 결정이 변경됨

## 보존

Rejected·Superseded·Retired ADR도 삭제하지 않는다.

과거 결정은 다음 상황에서 필요하다.

장애 원인 분석

설계 변경 배경

감사

레거시 코드 이해

동일 대안 재검토

데이터 이행

# 시사점

## 핵심 아키텍처 판단

첫째, ADR은 아키텍처 설계결과를 설명하는 문서가 아니라 의사결정의 이유와 Trade-off를 보존하는 문서다.

둘째, ADR은 중요한 결정에만 적용해야 하며 모든 Class·Method 선택을 기록하면 오히려 핵심 판단이 묻힌다.

셋째, 선택한 대안만 기록해서는 ADR의 가치가 없으며 현행 유지안과 제외된 대안의 사유를 함께 남겨야 한다.

넷째, ADR의 품질은 결론의 길이가 아니라 결정 동인·전제·제약·대안·부정적 결과가 얼마나 명확한지에 따라 결정된다.

다섯째, 승인된 ADR은 소스·설정·테스트·운영 Metric으로 검증돼야 하며 문서만 승인된 상태는 완료가 아니다.

여섯째, 결정 당시의 전제가 달라지면 기존 ADR을 고집하지 말고 신규 ADR로 재검토해야 한다.

일곱째, Gateway·Session·ServiceId·배포단위와 같은 횡단 결정은 전체 업무 WAR에 영향을 주므로 중앙 Architecture Governance가 필요하다.

여덟째, ADR에는 Rollback과 재검토 조건이 포함돼야 결정이 실패했을 때 신속히 대응할 수 있다.

아홉째, 승인된 ADR의 결론을 직접 수정하지 않고 신규 ADR로 대체해야 의사결정 이력이 유지된다.

열째, ADR은 FAD·설계서·RTM·Runbook·코드리뷰와 연결될 때 실제 프로젝트 통제수단으로 작동한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| ADR 미작성 | 결정근거 소실 |
| 모든 변경 ADR화 | 문서 과부하 |
| 대안 편향 | 사후 정당화 |
| 전제 미기록 | 환경 변화 대응 실패 |
| 부정적 결과 누락 | 운영위험 은폐 |
| Owner 없음 | 결정 방치 |
| 검증방법 없음 | 성공 여부 불명 |
| Metric 없음 | 운영 재검토 불가 |
| 승인 ADR 직접수정 | 이력 훼손 |
| Rejected ADR 삭제 | 논의 반복 |
| 코드·ADR 불일치 | 표준 붕괴 |
| 보안검토 누락 | 취약구조 승인 |
| Rollback 없음 | 실패 시 복구지연 |
| 재검토 조건 없음 | 임시결정 영구화 |
| 기존 ID 재사용 | 추적성 상실 |

## 우선 보완 과제

1.  docs/architecture/adr를 공식 ADR 저장소로 확정한다.
2.  기존 ADR-01~12의 상세 ADR 문서를 순차적으로 작성한다.
3.  ADR ID·파일명·상태·Owner 표준을 적용한다.
4.  최종 아키텍처 결정서와 개별 ADR의 관계를 정리한다.
5.  공통 Framework 변경 MR에 ADR 링크를 필수화한다.
6.  Module 의존성·ServiceId·Gateway 구조를 Architecture Test로 검증한다.
7.  Accepted ADR의 구현·검증상태를 별도로 관리한다.
8.  운영 Metric과 ADR 재검토 Trigger를 연결한다.
9.  보안예외 ADR에 Owner·만료일·해소계획을 필수화한다.
10.  Supersedes 관계를 자동검증한다.
11.  아키텍처 Review 회의에서 ADR 초안을 기준자료로 사용한다.
12.  분기별로 Accepted ADR과 실제 코드의 불일치를 점검한다.
13.  장애 PIR에서 잘못된 전제나 결정을 ADR 개선과제로 연결한다.
14.  신규 개발자가 주요 ADR을 읽고 Runtime 구조를 설명하도록 교육한다.

# 마무리말

ADR 작성을 완료하려면 다음 질문에 답할 수 있어야 한다.

어떤 문제를 해결하려는가?

왜 지금 결정해야 하는가?

어떤 요구사항과 품질속성이 중요한가?

현재 사실과 검증되지 않은 전제를 구분했는가?

선택을 제한하는 제약조건은 무엇인가?

현행 유지를 포함해 어떤 대안을 검토했는가?

모든 대안을 같은 기준으로 비교했는가?

어떤 대안을 선택했는가?

다른 대안을 선택하지 않은 이유는 무엇인가?

선택으로 얻는 이점은 무엇인가?

어떤 단점과 위험을 받아들이는가?

위험을 어떻게 완화할 것인가?

어떤 코드·설정·DB·운영요소가 변경되는가?

결정이 성공했는지 무엇으로 검증하는가?

실패하면 어떻게 Rollback하는가?

어떤 조건에서 다시 검토해야 하는가?

누가 결정을 유지하고 재검토하는가?

요구사항·설계·소스·Test·Runbook과 연결되는가?

미래의 담당자가 이 문서만 읽고
당시 판단을 이해할 수 있는가?

부록 G의 핵심 흐름은 다음과 같다.

문제 정의

→ 결정 동인

→ 전제·제약

→ 대안 도출

→ 공통기준 평가

→ 결정·근거

→ 긍정·부정 결과

→ 위험·완화

→ 구현·검증

→ 운영 Metric

→ 재검토·Supersede

가장 중요한 원칙은 다음과 같다.

아키텍처 결정은
정답을 찾는 과정이 아니다.

현재의 요구사항과 제약 안에서
가장 적절한 Trade-off를 선택하고,

그 선택으로 얻는 것과 잃는 것을
정직하게 기록하는 과정이다.

결정의 맥락과 대안과 위험이 남아 있을 때

미래의 개발자와 운영자는
같은 논의를 처음부터 반복하지 않고,

환경이 달라졌을 때
기존 결정을 근거 있게 다시 판단할 수 있다.

그때 ADR은 단순한 문서가 아니라
아키텍처 지식과 책임을 보존하는
프로젝트의 장기 기억이 된다.
