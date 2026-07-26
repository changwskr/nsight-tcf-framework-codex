<!-- source: ztcf-집필본/NSIGHT TCF Chapter 26- Code Review and Quality Gate.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제26장. 코드 리뷰와 품질 Gate

## 도입 전 안내말

제25장에서는 단위·통합·TCF 거래 테스트를 구분하고, 정상 응답뿐 아니라 데이터베이스 상태·거래로그·Rollback·Timeout·권한·동시성까지 검증하는 테스트 전략을 살펴보았다.

테스트가 모두 통과했더라도 코드가 운영에 적합하다고 단정할 수는 없다.

테스트는 작성자가 예상한 상황을 검증한다.

코드 리뷰는 작성자가 미처 예상하지 못한 위험을 발견한다.

품질 Gate는 발견된 위험이나 표준 위반이 수정되지 않은 상태로 다음 단계에 넘어가지 못하도록 차단한다.

세 활동의 관계는 다음과 같다.

자동 테스트
→ 작성자가 정의한 기대결과를 검증

코드 리뷰
→ 설계·구현·운영 위험을 사람의 관점으로 검토

품질 Gate
→ 기준 미달 변경의 Merge·배포를 차단

초보 개발자는 코드 리뷰를 다음과 같이 생각하기 쉽다.

변수명이 적절한가?

중괄호 위치가 맞는가?

코드가 보기 좋은가?

이 항목도 필요하다.

그러나 NSIGHT TCF 프로젝트에서 더 먼저 검토해야 할 것은 다음과 같다.

요구사항과 다른 기능을 구현하지 않았는가?

데이터 소유권을 위반하지 않았는가?

Handler가 Mapper를 직접 호출하지 않는가?

Transaction 경계가 Facade에 있는가?

예외를 catch한 뒤 정상 반환해 부분 Commit되지 않는가?

변경 거래에 Version·멱등성이 적용됐는가?

화면에서 전달한 userId를 인증정보로 신뢰하지 않는가?

Timeout 없이 외부 시스템을 호출하지 않는가?

SQL 영향 행 수를 확인하는가?

오류 응답에 SQL·Stack Trace·개인정보가 노출되지 않는가?

ServiceId가 Handler·OM·Timeout·권한정보와 일치하는가?

배포 후 장애가 발생하면 GUID로 추적할 수 있는가?

Rollback과 운영 복구 절차가 준비됐는가?

다음 코드는 문법적으로 정상이고 테스트도 통과할 수 있다.

@Component
@RequiredArgsConstructor
public class CustomerHandler
implements TransactionHandler {

private final CustomerMapper mapper;

@Override
public Object handle(
StandardRequest<?> request,
TransactionContext context) {

return mapper.selectCustomer(
request.getBody()
);
}
}

그러나 아키텍처적으로는 부적합하다.

Handler가 Mapper를 직접 호출한다.

Facade Transaction 경계를 우회한다.

Service와 Rule의 업무 판단을 우회한다.

DAO의 데이터 접근 추상화를 우회한다.

영향도와 테스트 범위가 불명확하다.

이 문제는 컴파일러가 발견하지 못한다.

일반적인 단위 테스트도 발견하지 못할 수 있다.

따라서 다음 규칙을 품질 Gate로 자동화해야 한다.

Handler
→ Facade만 호출 가능

Rule
→ DAO·Mapper·Client 호출 금지

Transaction
→ Facade에 위치

업무 WAR
→ 다른 업무 WAR 구현 직접 의존 금지

프로젝트 설계기준에서도 계층 의존성, ServiceId 중복, Handler 등록, OM Catalog, 거래통제, Timeout, Mapper 정합성, Secret, 오류 응답과 거래로그 완결성을 CI/CD 자동검증 대상으로 정의한다.

# 문서 개요

## 목적

본 장의 목적은 NSIGHT TCF 코드 변경을 검토하고 승인하기 위한 코드 리뷰와 품질 Gate 기준을 정의하는 것이다.

세부 목적은 다음과 같다.

요구사항과 구현의 일치 확인

아키텍처 계층과 책임 경계 보호

데이터 소유권과 업무 불변식 보호

트랜잭션·동시성·멱등성 검토

인증·권한·개인정보·감사 검토

Timeout·Retry·장애 전파 검토

SQL·DB·성능 영향 검토

오류·로그·Metric·운영 추적성 확인

좋은 Pull Request 작성 기준 정의

리팩터링과 기능 변경의 위험 분리

자동검증과 수동검토 범위 분리

심각도에 따른 Merge·배포 차단

예외 승인과 기술부채 관리

변경·호환성·폐기 영향관리

## 적용범위

| 대상 | 주요 검토 내용 |
| --- | --- |
| Java 소스 | 계층·책임·예외·동시성 |
| DTO | 계약·필수값·개인정보 |
| Handler | ServiceId·Facade 위임 |
| Facade | Transaction·Timeout |
| Service | 업무 흐름·상태 전이 |
| Rule | 순수 업무 규칙 |
| DAO·Mapper | SQL·영향 행 수 |
| 설정 | Profile·Timeout·Pool·Secret |
| DB DDL | 명명·호환성·Rollback |
| 내부 연계 | Contract·오류·Timeout |
| 외부 연계 | Retry·Circuit·멱등성 |
| JWT·보안 | Claim·키·권한·로그 |
| Cache | Commit 이후 Evict·일관성 |
| Batch | Chunk·Checkpoint·재시작 |
| 파일 | 경로·크기·악성코드·파기 |
| 테스트 | 정상·경계·실패·Rollback |
| 로그 | GUID·마스킹·오류코드 |
| CI/CD | Build·Test·Gate·Artifact |
| 문서·OM | 코드·설계·운영 기준정보 정합성 |

## 대상 독자

업무 개발자

Pull Request 작성자

Peer Reviewer

업무 리더

애플리케이션 아키텍트

프레임워크 개발자

DBA·데이터 아키텍트

보안 담당자

QA·테스트 담당자

DevOps·CI/CD 담당자

운영 담당자

PMO·품질관리 담당자

## 선행조건

요구사항 ID

화면 이벤트·화면 ID

ServiceId·거래코드

요청·응답 계약

업무 규칙과 상태 전이

사용 Table·SQL

권한·감사정책

Timeout·연계정책

테스트 결과

배포·Rollback 계획

# 핵심 관점

좋은 코드 리뷰는
코드를 작성자 취향에 맞게 바꾸는 활동이 아니다.

운영 장애와 데이터 오류가 될 가능성이 높은 부분을
배포 전에 발견하고,

자동화할 수 있는 규칙은 Gate로 옮기며,

표준과 다른 판단은 근거와 책임자를 남기는
품질 통제 활동이다.

# 학습 목표

이 장을 마치면 다음 내용을 설명하고 적용할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 코드 리뷰와 품질 Gate의 차이를 설명한다. |
| 2 | 스타일보다 업무·데이터 위험을 우선 검토한다. |
| 3 | 요구사항과 ServiceId를 코드 변경에 연결한다. |
| 4 | 데이터 소유권과 상태 전이를 검토한다. |
| 5 | 계층별 책임 위반을 발견한다. |
| 6 | Transaction 경계와 Rollback 위험을 검토한다. |
| 7 | 동시 수정·멱등성·영향 행 수를 검토한다. |
| 8 | 인증과 기능·데이터권한을 검토한다. |
| 9 | 민감정보가 응답·로그에 노출되는지 확인한다. |
| 10 | Timeout·Retry·Circuit의 정합성을 검토한다. |
| 11 | SQL 실행계획과 대량 데이터 위험을 검토한다. |
| 12 | 오류코드·로그·Metric의 운영성을 확인한다. |
| 13 | Blocker·Critical·Major·Minor를 구분한다. |
| 14 | 자동검증 가능 항목과 사람의 판단 항목을 구분한다. |
| 15 | 작은 Pull Request가 필요한 이유를 설명한다. |
| 16 | 좋은 PR 설명과 증적을 작성한다. |
| 17 | 리뷰 의견을 명확하고 검증 가능하게 작성한다. |
| 18 | 리뷰 지적을 코드·테스트·문서에 반영한다. |
| 19 | 리팩터링과 기능 변경을 분리한다. |
| 20 | 대규모 자동 포맷 변경을 기능 PR과 분리한다. |
| 21 | PR 승인과 Merge 가능 상태를 구분한다. |
| 22 | Gate 예외 승인 절차를 설명한다. |
| 23 | 조건부 승인 조치사항을 추적한다. |
| 24 | 품질 Gate 결과를 배포 승인 증적으로 남긴다. |
| 25 | 운영 장애를 신규 Gate 규칙으로 전환한다. |

# 핵심 용어

| 용어 | 의미 |
| --- | --- |
| Code Review | 변경 코드를 동료·전문가가 검토하는 활동 |
| Self Review | 작성자가 PR 전에 자신의 Diff를 검토 |
| Peer Review | 동료 개발자의 기능·구현 검토 |
| Specialist Review | 보안·DB·아키텍처·운영 전문 검토 |
| Quality Gate | 기준 미달 시 다음 단계 진행을 차단하는 규칙 |
| Blocking Comment | 해결 전 Merge할 수 없는 리뷰 의견 |
| Non-blocking Comment | 개선 권고나 후속 기술부채 의견 |
| Blocker | 데이터 손상·보안사고·전체 장애 위험 |
| Critical | 핵심 기능·계층·Transaction 위반 |
| Major | 운영성과 유지보수성에 큰 문제 |
| Minor | 가독성·일관성 개선 |
| Advisory | 향후 개선 권고 |
| Pull Request | 변경 이유·코드·테스트·승인을 묶는 검토 단위 |
| Diff | 기준 Branch와 변경 Branch의 차이 |
| Baseline | 승인된 현재 기준 |
| Evidence | 테스트·로그·설계서·설정 등의 증적 |
| Waiver | Gate 기준을 한시적으로 면제하는 공식 승인 |
| Technical Debt | 즉시 해결하지 못하고 추적 관리하는 개선 과제 |
| ADR | 아키텍처 의사결정 기록 |
| Code Owner | 특정 코드영역의 승인 책임자 |
| Merge Gate | 보호 Branch에 Merge하기 위한 조건 |
| Release Gate | 배포 Artifact를 운영으로 승격하기 위한 조건 |

# 코드 리뷰와 품질 Gate의 관계

개발자 Self Review
↓
자동 정적검사
↓
자동 테스트
↓
Pull Request 생성
↓
Peer Review
↓
전문분야 Review
├─ AA
├─ DBA
├─ 보안
└─ 운영
↓
미해결 의견 0
↓
Merge Gate
↓
통합·성능·보안 검증
↓
Release Gate

# 현재 구현과 목표 구조

## 현재 기준자료에서 확인되는 구현

현재 기준 소스와 문서에는 다음 기반이 존재한다.

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| TransactionDispatcher | 구현 확인 | ServiceId 중복 시 기동 실패 |
| Handler Registry | 구현 확인 | ServiceId별 Handler 등록 |
| tcf-cicd | 구현 확인 | Profile·설정·Build·Deploy 스크립트 |
| manifest.yaml | 구현 확인 | 모듈·Profile 기준정보 |
| Health Check 옵션 | 스크립트 확인 | 배포 후 검증 기반 |
| 단위·통합 테스트 | 부분 구현 | 공통·SV 중심 |
| Architecture Gate 문서 | 기준 수립 | G0~G13 |
| 패키지·계층 Gate | 설계 기준 | ArchUnit 적용 대상 |
| OM 정합성 Gate | 설계 기준 | 코드·Catalog 대조 |
| Mapper 검증 | 설계 기준 | Interface·XML 정합성 |
| Secret Scan | 설계 기준 | 운영 전 필수 |
| Checkstyle·정적분석 | 설계 기준 | 실제 Pipeline 확인 필요 |
| SonarQube·SAST·SCA | 목표 기준 | 현재 실행 정의 확인 필요 |
| PR 보호 규칙 | 프로젝트 확인 필요 | 저장소 설정 증적 필요 |
| Code Owner | 프로젝트 확인 필요 | 소유권 파일·규칙 필요 |
| Gate Waiver 관리 | 보완 필요 | OM·이슈 관리 연계 |

tcf-cicd는 Build·Deploy·Profile 관리 역할을 수행하는 모듈로 정의돼 있다. 다만 현재 분석한 기준 소스에서는 저장소 차원의 실제 PR Pipeline, ArchUnit·Checkstyle·SonarQube 설정 전체가 명확히 확인되지 않았으므로, 문서에 정의된 Gate를 실행 가능한 CI 규칙으로 전환하는 작업이 필요하다.

## 현재 구현에서 즉시 리뷰가 필요한 사례

현재 TransactionDispatcher는 중복 ServiceId를 기동 단계에서 차단하는 좋은 방어를 갖고 있다.

그러나 진단 목적으로 Handler Map을 System.out에 출력하는 부분은 운영 표준에서는 구조화 Logging으로 전환해야 한다.

좋은 부분
→ ServiceId 중복 시 IllegalStateException
→ 잘못된 상태로 기동하지 않음

보완 부분
→ System.out 제거
→ 구조화 로그
→ 환경별 Log Level
→ ServiceId 수 Metric

## 목표 품질 Gate

G1 Compile
G2 Unit Test
G3 Integration Test
G4 Architecture Test
G5 Mapper·Schema Validation
G6 ServiceId Registry
G7 OM Catalog 정합성
G8 Security Scan
G9 Dependency·License Scan
G10 Contract·Compatibility
G11 Performance·Capacity
G12 Operational Readiness
G13 Release Approval

Architecture Gate는 패키지 구조·계층 호출·ServiceId·OM 등록·Timeout·Mapper·Secret·테스트·Health·추적성을 자동검증과 연결하도록 정의한다.

# 리뷰 심각도

| 등급 | 판단 기준 | 처리 |
| --- | --- | --- |
| Blocker | 데이터 손상·보안사고·전면 장애 | 즉시 Merge 차단 |
| Critical | 계층·Transaction·인증·복구 위반 | 수정 전 Merge 차단 |
| Major | 성능·운영·호환성의 큰 위험 | 수정 또는 공식 예외승인 |
| Minor | 가독성·표준 일관성 | 가능하면 현재 PR 수정 |
| Advisory | 장기 구조 개선 | 기술부채 등록 가능 |
| Question | 의도·근거 확인 | 답변 후 상태 결정 |

프로젝트 패키지 설계기준도 순환 의존과 다른 WAR 구현 참조를 Blocker, Handler→Mapper·Rule→DAO를 Critical로 분류한다.

# 26.1 리뷰 우선순위

## 26.1.1 리뷰는 스타일보다 위험을 먼저 본다

권장 우선순위:

1\. 요구사항·계약
2\. 데이터 소유권·업무 불변식
3\. Transaction·동시성·멱등성
4\. 인증·권한·개인정보
5\. 실패·Timeout·복구
6\. 계층·의존성
7\. SQL·성능·용량
8\. 운영로그·Metric·감사
9\. 테스트
10\. 명명·가독성·스타일

다음 리뷰 순서는 좋지 않다.

변수명
→ 줄바꿈
→ 중괄호
→ 주석
→ 마지막에 Transaction 검토

Transaction·보안 문제를 먼저 발견해야 한다.

## 26.1.2 요구사항 검토

리뷰 질문:

어떤 요구사항 ID를 구현하는가?

요청한 기능과 실제 구현이 일치하는가?

요구하지 않은 기능이 추가되지 않았는가?

화면·ServiceId·거래코드가 일치하는가?

정상 완료조건은 무엇인가?

업무 오류조건은 무엇인가?

운영 완료조건은 무엇인가?

검토 근거:

요구사항 정의서

화면 설계서

거래 설계서

프로그램 설계서

DB 설계서

테스트 케이스

## 26.1.3 입출력 계약 검토

Request:

필수·선택 필드

길이·형식·범위

Null·빈 문자열 의미

코드값

날짜·시간대

금액 단위

목록 최대 건수

Idempotency Key

Response:

StandardResponse 형식

정상·오류 Body

Masking

정렬·페이징

부분 성공

Contract Version

하위호환성

## 26.1.4 데이터 소유권 검토

리뷰 질문:

어느 업무가 해당 데이터를 소유하는가?

다른 업무 Table을 직접 조회·변경하지 않는가?

변경은 데이터 소유 업무의 ServiceId를 통해 수행하는가?

논리삭제·권한·상태조건을 우회하지 않는가?

Read Model이나 승인된 View인가?

Blocker 예:

UPDATE IC\_CUSTOMER
SET CUSTOMER\_NAME = #{name}
WHERE CUSTOMER\_NO = #{customerNo}

이 SQL이 SV Mapper에 존재한다면 데이터 소유권을 위반한다.

## 26.1.5 상태 전이 검토

현재 상태를 확인하는가?

허용 상태를 명시했는가?

목표 상태를 서버가 결정하는가?

상태 역행을 차단하는가?

Version을 함께 검증하는가?

상태 변경 이력이 남는가?

금지:

entity.setStatus(
request.getStatus()
);

Client가 임의 상태를 결정하게 해서는 안 된다.

## 26.1.6 영향 행 수 검토

INSERT 결과가 1건인가?

UPDATE 0건을 성공 처리하지 않는가?

UPDATE 2건 이상을 정합성 오류로 보는가?

DELETE 조건에 Version·상태가 포함되는가?

금지:

mapper.updateCampaign(command);
return success();

권장:

int affected =
mapper.updateCampaign(command);

if (affected == 0) {
throw classifyUpdateFailure(command);
}

if (affected != 1) {
throw new DataIntegrityException(...);
}

## 26.1.7 실패와 복구 검토

어떤 예외가 발생할 수 있는가?

업무 오류와 시스템 오류가 구분되는가?

원인 예외를 보존하는가?

실패 시 Transaction이 Rollback되는가?

Timeout 후 결과가 불명확하지 않은가?

재시도 가능한가?

상태조회·보상·대사가 준비됐는가?

## 26.1.8 성능 검토

전체 데이터를 List로 적재하지 않는가?

반복문 안에서 원격 호출하지 않는가?

N+1 SQL이 발생하지 않는가?

WHERE 조건과 Index가 적절한가?

최대 조회 건수가 있는가?

불필요한 Count가 없는가?

대형 CLOB·BLOB을 온라인에서 읽지 않는가?

Cache가 권한과 데이터 변경을 고려하는가?

## 26.1.9 운영성 검토

GUID·TraceId·ServiceId가 유지되는가?

오류코드가 표준인가?

거래로그가 성공·실패 모두 종료되는가?

감사 대상 변경인가?

Metric으로 오류율·처리시간을 볼 수 있는가?

장애 시 어느 팀이 확인하는가?

Feature Flag·거래통제가 가능한가?

Rollback 절차가 있는가?

## 26.1.10 리뷰 범위 결정

모든 PR에 모든 전문 리뷰어가 참여할 필요는 없다.

| 변경 | 필수 Reviewer |
| --- | --- |
| 업무 Rule | 업무 리더 |
| 공통 TCF | FW·AA |
| Transaction | AA·업무 리더 |
| Mapper·DDL | DBA·DA |
| JWT·권한 | 보안·FW |
| 외부 연계 | EAI·보안 |
| Cache | AA·운영 |
| Batch | 업무·운영·DBA |
| Pool·Thread | TA·운영·성능 |
| 운영 설정 | 운영·보안 |
| 공통 Contract | Provider·Consumer |

## 26.1.11 Self Review

PR 생성 전에 작성자가 다음을 수행한다.

git diff 확인

불필요한 변경 제거

생성파일 제거

Secret 검사

TODO·임시코드 확인

System.out 확인

주석 처리 코드 확인

테스트 결과 확인

설계·OM 변경 확인

Rollback 검토

Self Review 없이 Reviewer에게 최초 디버깅을 맡기지 않는다.

## 26.1.12 Reviewer의 검토 순서

PR 설명
↓
요구사항·영향 범위
↓
파일 목록
↓
계약·DB·설정
↓
핵심 업무 흐름
↓
실패·Transaction
↓
테스트
↓
운영·Rollback
↓
스타일

파일을 위에서 아래로만 읽지 않는다.

# 26.2 계층·보안·트랜잭션 검토

## 26.2.1 계층 구조

표준:

Handler
→ Facade
→ Service
→ Rule·DAO·Client
→ Mapper

검토표:

| 계층 | 허용 | 금지 |
| --- | --- | --- |
| Handler | Facade·DTO 변환 | DAO·Mapper·Client |
| Facade | Service·Transaction | SQL·업무 세부규칙 |
| Service | Rule·DAO·Client | StandardResponse 생성 |
| Rule | 순수 업무판단 | DB·Cache·Client |
| DAO | Mapper·영속 변환 | 업무 상태판단 |
| Mapper | SQL | Service·외부 호출 |
| Client | 연계 Adapter | 내부 DB 직접 변경 |

패키지 구조와 코드 리뷰 체크리스트에서도 Handler→Facade, Rule의 DB·Cache·외부 호출 금지, 다른 업무 Mapper 직접 참조 금지를 요구한다.

## 26.2.2 Handler 검토

ServiceId 목록이 명확한가?

업무코드 Prefix가 맞는가?

Body를 목적별 DTO로 변환하는가?

Validation을 수행하는가?

Facade만 호출하는가?

응답을 임의로 StandardResponse로 만들지 않는가?

예외를 숨기지 않는가?

금지:

public Object handle(...) {
try {
return mapper.select(...);
} catch (Exception exception) {
return Map.of("success", false);
}
}

## 26.2.3 Facade 검토

유스케이스 단위인가?

@Transactional이 필요한가?

readOnly 조회인가?

Timeout이 있는가?

여러 Service 호출의 원자성이 명확한가?

외부 호출을 DB Lock 상태에서 기다리는가?

예외를 catch하고 정상 반환하지 않는가?

Transaction은 업무 불변식을 지키는 유스케이스 경계로 Facade에 두는 것이 표준이다.

## 26.2.4 Service 검토

업무 처리 순서가 읽히는가?

Rule이 분리돼 있는가?

DAO 결과를 업무 의미로 해석하는가?

상태 전이가 명확한가?

영향 행 수를 확인하는가?

다른 업무의 내부 구현을 호출하지 않는가?

외부 호출은 Client Adapter를 사용하는가?

Response에 불필요한 내부정보가 없는가?

## 26.2.5 Rule 검토

입력과 현재 상태만으로 판단 가능한가?

DB·Cache·외부 호출이 없는가?

허용·금지 조건이 명시돼 있는가?

오류코드가 업무 의미를 나타내는가?

단위 테스트가 있는가?

경계값과 상태 Matrix가 있는가?

Rule이 DAO를 호출하면 단위 테스트와 책임 경계가 무너진다.

## 26.2.6 DAO·Mapper 검토

DAO:

Mapper 호출과 데이터 변환만 수행하는가?

업무 오류를 임의 판단하지 않는가?

DataAccessException Cause를 보존하는가?

Mapper:

Namespace가 Interface와 일치하는가?

SQL ID가 메서드와 일치하는가?

다른 업무 Table DML이 없는가?

논리삭제 조건이 있는가?

Version 조건이 있는가?

최대 조회 건수가 있는가?

영향 행 수를 확인할 수 있는가?

실행계획이 검증됐는가?

## 26.2.7 트랜잭션 검토

리뷰 질문:

어디에서 Transaction이 시작되는가?

함께 Commit돼야 하는 데이터는 무엇인가?

Master·Detail·History·Outbox가 같은 Transaction인가?

Checked Exception Rollback 정책이 맞는가?

예외를 catch한 뒤 정상 반환하지 않는가?

REQUIRES\_NEW가 필요한가?

Commit 이후 Cache·메시지는 어떻게 처리하는가?

원격 시스템 변경을 로컬 Transaction으로 오해하지 않는가?

## 26.2.8 외부 호출과 Transaction

위험:

Transaction 시작

DB UPDATE

외부 API 5초 대기

외부 Retry

DB 후속 UPDATE

Commit

문제:

DB Connection 장기 점유

Row Lock 장기 유지

Timeout 전파

Rollback 비용

상대 장애의 내부 확산

리뷰에서는 다음 대안을 확인한다.

외부 조회를 Transaction 전에 수행

Outbox로 비동기 처리

상태머신

보상 거래

Transaction 범위 축소

## 26.2.9 동시성 검토

Version 필드가 있는가?

UPDATE WHERE에 expectedVersion이 있는가?

상태조건이 함께 있는가?

UPDATE 0건을 분류하는가?

업무 Unique Constraint가 있는가?

비관적 Lock이 과도하지 않은가?

Lock 순서가 일관되는가?

## 26.2.10 멱등성 검토

변경 거래:

Idempotency Key가 있는가?

Request Hash를 비교하는가?

PROCESSING·SUCCESS·FAIL·UNKNOWN 상태가 있는가?

같은 Key 다른 Body를 차단하는가?

Timeout 후 신규 Key 재실행을 막는가?

하위 변경 연계에도 파생 Key를 전달하는가?

## 26.2.11 인증 검토

JWT 서명·exp·iss·aud를 검증하는가?

Gateway 우회 가능 시 업무 WAR Filter가 있는가?

JWT Claim과 Header 사용자를 비교하는가?

클라이언트 userId를 인증정보로 신뢰하지 않는가?

인증 실패 시 Handler가 실행되지 않는가?

## 26.2.12 권한 검토

ServiceId 기능권한이 있는가?

지점·조직 데이터권한이 있는가?

작성자·승인자 분리가 있는가?

삭제·복구 관리자 권한이 있는가?

권한정보 조회 실패 시 Fail Closed인가?

존재 여부를 과도하게 노출하지 않는가?

## 26.2.13 개인정보·로그 검토

금지 대상:

JWT 원문

Refresh Token

Private Key

비밀번호

주민등록번호

계좌번호 전체

요청·응답 Body 전체

SQL·Stack Trace 사용자 응답

권장 로그:

GUID

TraceId

ServiceId

오류코드

실패단계

처리시간

마스킹된 업무키

배포버전

## 26.2.14 Secret 검토

application.yml 평문 Secret이 없는가?

Git History에 Key가 없는가?

환경변수·Vault Reference를 사용하는가?

Test Secret과 운영 Secret이 분리됐는가?

인증서·Key Rotation이 가능한가?

Secret Scan은 자동 Gate로 차단해야 한다.

## 26.2.15 Timeout 검토

서비스 Timeout이 있는가?

SQL Query Timeout이 더 짧은가?

내부·외부 Read Timeout이 전체보다 짧은가?

Retry를 포함한 최대시간이 예산 안인가?

Timeout 후 Thread·Connection이 반환되는가?

## 26.2.16 Retry·Circuit 검토

Retryable 오류만 재시도하는가?

업무 오류를 재시도하지 않는가?

변경 거래는 멱등한가?

Backoff·Jitter가 있는가?

Circuit 실패집계에서 업무 오류를 제외하는가?

Circuit Open 시 Fallback이 안전한가?

Bulkhead 상한이 있는가?

## 26.2.17 Cache 검토

Cache Key에 권한·기준일이 필요한가?

개인정보가 Cache에 저장되는가?

변경 Commit 후 Evict하는가?

Rollback 시 Cache가 잘못 삭제되지 않는가?

다중 WAR에 Evict가 전파되는가?

TTL과 Stale 허용시간이 명확한가?

## 26.2.18 Batch 검토

전체 데이터를 메모리에 적재하지 않는가?

Chunk와 Commit 크기가 있는가?

Checkpoint가 있는가?

중복 실행 Lock이 있는가?

Item 멱등성이 있는가?

Retry·Skip 상한이 있는가?

입출력 건수·금액 대사가 있는가?

수동 재처리가 감사되는가?

## 26.2.19 SQL·성능 검토

조건 없는 UPDATE·DELETE가 없는가?

SELECT \*를 사용하지 않는가?

불필요한 DISTINCT가 없는가?

Function으로 Index를 무력화하지 않는가?

Offset 깊은 페이징을 검토했는가?

정렬 기준이 안정적인가?

N+1이 없는가?

통계·Execution Plan을 확인했는가?

DML Index 비용을 검토했는가?

## 26.2.20 오류처리 검토

업무·시스템·Timeout 오류를 구분하는가?

원인 예외를 보존하는가?

exception.getMessage()를 사용자에게 노출하지 않는가?

오류코드가 OM에 등록됐는가?

Stack Trace가 최종 경계에서 한 번 기록되는가?

오류 응답에 GUID가 있는가?

# 26.3 좋은 Pull Request

## 26.3.1 Pull Request의 목적

Pull Request는 단순히 “코드를 Merge해 달라”는 요청이 아니다.

다음 정보를 하나의 검토 단위로 묶는다.

왜 변경하는가?

무엇을 변경했는가?

어디에 영향을 미치는가?

어떻게 검증했는가?

어떻게 배포하는가?

문제가 생기면 어떻게 되돌리는가?

어떤 위험이 남아 있는가?

## 26.3.2 좋은 PR 크기

좋은 PR은 한 가지 논리적 목적을 가진다.

REQ-CM-014
캠페인 승인 거래 추가

다음이 함께 들어가면 리뷰가 어려워진다.

캠페인 승인 기능

전체 코드 포맷

Spring Boot 버전 변경

패키지 이동

미사용 파일 삭제

로그 Framework 교체

DB Index 변경

## 26.3.3 PR 크기 기준

프로젝트별 기준 예:

| 변경 규모 | 판단 |
| --- | --- |
| 1~10개 핵심 파일 | 일반 리뷰 |
| 10~30개 | 영향도 설명 필수 |
| 30개 이상 | 분리 가능성 검토 |
| 대규모 생성·이동 | 전용 PR |
| 수천 줄 포맷 변경 | 기능 변경과 분리 |

줄 수는 참고값이다.

하나의 SQL 한 줄이 수백 파일보다 위험할 수 있다.

## 26.3.4 PR 제목

좋은 제목:

\[CM\]\[REQ-CM-014\] 캠페인 승인 거래와 감사이력 추가

\[SV\]\[FIX\] 고객목록 Version 조건 누락 수정

\[TCF\]\[SECURITY\] JWT Header 사용자 정합성 검증 강화

나쁜 제목:

수정

개발 완료

오류 처리

최종

진짜 최종2

## 26.3.5 PR 설명 표준

\## 변경 목적
\- 요구사항: REQ-CM-014
\- ServiceId: CM.Campaign.approve
\- 거래코드: CM-CMD-0004

\## 변경 내용
\- 승인 상태 전이 Rule 추가
\- Version 조건 UPDATE 추가
\- 승인 이력·Outbox 저장
\- OM ServiceId·권한·Timeout 등록

\## 영향 범위
\- 화면: CM-CAM-0002
\- Table: CM\_CAMPAIGN, CM\_CAMPAIGN\_HISTORY
\- 내부 연계: EP.Campaign.registerExecution
\- Cache: CM campaign detail Evict

\## 테스트
\- 단위: 승인 가능·불가 상태
\- 거래: 정상 승인
\- 실패: Version 충돌
\- Rollback: 이력 INSERT 실패
\- 권한: 승인자 권한 없음

\## 배포
\- DDL 선반영 없음
\- CM WAR 배포
\- OM Catalog 동시 반영

\## Rollback
\- 이전 CM WAR 복원
\- 신규 ServiceId 거래통제
\- Outbox 미처리 건 대사

\## 잔여 위험
\- EP 연계 장애 시 Outbox 재처리 필요

## 26.3.6 PR 필수 증적

요구사항·결함 ID

ServiceId·거래코드

변경 화면·프로그램

사용 Table·SQL

설정·OM 변경

정상·경계·실패 테스트

Rollback 테스트

성능·보안 영향

배포·Rollback 절차

잔여 위험

## 26.3.7 Commit 구성

권장:

Commit 1
→ Contract·DTO

Commit 2
→ 업무 구현

Commit 3
→ Mapper·DDL

Commit 4
→ 테스트

Commit 5
→ OM·문서

모든 PR을 반드시 여러 Commit으로 나눌 필요는 없지만 각 Commit은 Build 가능하고 의미가 명확해야 한다.

## 26.3.8 Commit 메시지

예:

feat(cm): 캠페인 승인 거래 추가

Refs: REQ-CM-014
ServiceId: CM.Campaign.approve

유형:

feat

fix

refactor

test

docs

build

ci

perf

security

chore

revert

## 26.3.9 리뷰 의견 작성법

좋은 리뷰 의견은 다음 요소를 포함한다.

위치

문제

위험

권장 변경

검증방법

예:

\[Critical\]

CampaignFacade에서 DataAccessException을 catch한 후
실패 DTO를 정상 반환하고 있습니다.

Spring은 메서드 정상 종료로 판단해
Master UPDATE를 Commit할 수 있습니다.

예외를 RuntimeException으로 전파하고,
Master·History Rollback 통합 테스트를 추가해 주세요.

## 26.3.10 나쁜 리뷰 의견

이상합니다.

다시 해 주세요.

왜 이렇게 했나요?

코드가 별로입니다.

제 방식으로 바꾸세요.

문제와 위험을 설명하지 않는다.

## 26.3.11 질문과 결함 구분

질문:

이 호출을 Transaction 안에 둔 이유가 있나요?

결함:

\[Critical\]
외부 호출이 DB Row Lock을 보유한 Transaction 안에서 실행됩니다.

확신하지 못하는 사항을 바로 결함으로 단정하지 않는다.

반대로 명확한 표준 위반을 단순 질문으로 남겨 해결되지 않게 하지 않는다.

## 26.3.12 리뷰 의견 상태

OPEN

AUTHOR\_RESPONDED

CHANGES\_REQUESTED

RESOLVED

ACCEPTED\_AS\_DEBT

WAIVED

“수정 완료”라는 답변만으로 Resolve하지 않는다.

무엇을 수정했는가?

어떤 Commit인가?

어떤 테스트로 확인했는가?

를 확인한다.

## 26.3.13 Reviewer 승인 기준

승인은 다음 의미다.

검토 범위에서 Blocker·Critical이 없다.

Major 항목이 해결되거나 공식 승인됐다.

필수 테스트와 Gate가 통과했다.

배포·Rollback 위험이 설명됐다.

승인은 코드가 완벽하다는 보증이 아니다.

검토자의 책임범위와 근거를 남긴다.

## 26.3.14 Draft PR

Draft PR이 유용한 상황:

설계 방향을 먼저 검토받을 때

대규모 변경의 구조를 공유할 때

Contract를 Provider와 협의할 때

테스트 접근방식을 검토할 때

Draft PR을 장기간 미완료 상태의 코드 저장소로 사용하지 않는다.

## 26.3.15 자동 생성 코드

생성 코드가 포함될 경우:

생성 원천

Generator Version

생성 명령

수동 수정 금지 여부

Diff 검토방법

재생성 가능성

을 기록한다.

생성 결과 수천 줄을 사람이 모두 직접 검토하는 대신 Schema·Generator·대표 결과를 검토한다.

## 26.3.16 DB 변경 PR

필수:

DDL

Forward Script

Rollback Script

데이터 이관

Lock 예상

소요시간

온라인 호환성

구·신 WAR 병행 가능성

Index 생성방식

백업·대사

## 26.3.17 설정 변경 PR

변경 전 값

변경 후 값

환경별 차이

근거 측정값

Secret 여부

재기동 여부

Rollback

Metric

예:

Hikari maximumPoolSize
120 → 160

단순 숫자 변경이 아니라 DB 총 Connection과 부하검증 근거가 필요하다.

# 26.4 리팩터링과 기능 변경 분리

## 26.4.1 리팩터링이란 무엇인가

리팩터링은 외부 동작을 유지하면서 내부 구조를 개선하는 변경이다.

예:

메서드 추출

클래스 분리

이름 변경

중복 제거

패키지 정리

의존성 역전

복잡도 감소

기능 변경은 사용자·업무·운영 동작을 바꾼다.

새로운 Validation

오류코드 변경

상태 전이 변경

SQL 조건 변경

Timeout 변경

권한 변경

응답 필드 변경

## 26.4.2 왜 분리해야 하는가

하나의 PR에 함께 들어가면 다음을 구분하기 어렵다.

동작 변경이 의도된 것인가?

리팩터링 중 발생한 결함인가?

SQL 결과가 왜 달라졌는가?

Rollback 시 어느 변경을 되돌려야 하는가?

회귀범위가 어디까지인가?

## 26.4.3 권장 순서

1\. 기존 동작을 테스트로 고정
2\. 순수 리팩터링 PR
3\. 회귀 테스트
4\. 기능 변경 PR
5\. 신규 요구사항 테스트

## 26.4.4 Characterization Test

기존 코드의 동작을 정확히 모를 때 먼저 현재 동작을 테스트로 기록한다.

입력

현재 응답

DB 변경

오류코드

로그

이 테스트가 업무적으로 올바르다는 뜻은 아니다.

리팩터링 중 의도하지 않은 변경을 탐지하는 기준이다.

## 26.4.5 포맷 변경 분리

금지:

고객조회 오류 수정
+
프로젝트 전체 Import 정리
+
줄바꿈 변경

실제 업무 Diff가 포맷 변경 속에 숨는다.

포맷 PR:

자동화 도구 Version

적용 범위

동작 변경 없음

전체 Build·Test

## 26.4.6 이름 변경

CustomerService
→ CustomerProfileService

영향:

Java Import

Spring Bean 이름

Reflection

Mapper Namespace

OM Handler Class

로그 검색

문서

테스트 Mock

단순 IDE Rename으로 끝나지 않을 수 있다.

## 26.4.7 패키지 이동

com.nh.nsight.marketing.sv.service

→

com.nh.nsight.marketing.sv.customer.service

검토:

Component Scan

Mapper Scan

XML Namespace

Gradle 의존

Serialization

AOP Pointcut

ArchUnit

OM 등록

운영 Log Category

패키지 이동은 코드·XML·OM·문서를 함께 변경해야 한다.

## 26.4.8 공통화 리팩터링

중복 코드가 있다고 무조건 공통 모듈로 이동하지 않는다.

질문:

정말 같은 업무 의미인가?

변경주기가 같은가?

소유자가 누구인가?

모든 업무가 안정적으로 사용할 계약인가?

업무 특화 코드를 TCF에 넣는 것은 아닌가?

금지:

common

common2

util

helper

manager

같은 모호한 패키지를 승인 근거 없이 만든다.

## 26.4.9 Framework 공통화 기준

공통 승격 조건:

3개 이상 업무의 반복 사용

업무 의미에 독립적

API 안정성

Owner 지정

Version·호환성 정책

전체 회귀 테스트

문서·샘플

폐기정책

## 26.4.10 기능 Flag

대규모 기능을 작은 PR로 나누려면 Feature Flag를 사용할 수 있다.

코드는 배포

기능은 비활성

검증 완료 후 활성

주의:

Flag 기본값

권한

환경별 상태

만료일

Flag 제거 계획

두 경로 테스트

영구적인 Flag를 만들지 않는다.

## 26.4.11 DB 호환 리팩터링

Expand–Migrate–Contract:

1\. 신규 컬럼 추가
2\. 구·신 프로그램 병행
3\. 데이터 이관
4\. 신규 컬럼 사용 전환
5\. 구 컬럼 사용량 0 확인
6\. 구 컬럼 제거

한 번의 배포로 컬럼을 바꾸고 코드까지 전환하지 않는다.

## 26.4.12 공개 Contract 리팩터링

공개 DTO·ServiceId·오류코드는 내부 클래스처럼 자유롭게 변경할 수 없다.

기존 V1 유지

V2 추가

Consumer 전환

호출량 확인

V1 Deprecated

폐기

## 26.4.13 리팩터링 리뷰 기준

외부 동작이 동일한가?

기존 테스트가 변경되지 않았는가?

새로운 업무조건이 추가되지 않았는가?

오류코드가 바뀌지 않았는가?

SQL 결과와 영향 행 수가 같은가?

로그·Metric이 유지되는가?

성능이 악화되지 않았는가?

# 품질 Gate 설계

## Gate 단계

Commit
↓
Pre-Commit Gate
↓
Pull Request Gate
↓
Merge Gate
↓
Integration Gate
↓
Release Gate
↓
Deployment Gate
↓
Post-Deployment Gate

## Pre-Commit Gate

Format

Compile

빠른 Unit Test

Secret Scan

금지파일

Diff 확인

## Pull Request Gate

| Gate | 기준 |
| --- | --- |
| PR 설명 | 요구사항·영향·테스트·Rollback |
| Build | 영향 모듈 Compile 성공 |
| Unit | 관련 테스트 성공 |
| Architecture | 계층·의존성 검사 |
| Security | Secret·SAST |
| Dependency | 취약점·License |
| Traceability | ServiceId·OM·테스트 |
| Reviewer | 필수 Owner 승인 |
| Comments | Blocking 미해결 0 |

## Merge Gate

보호 Branch 직접 Push 금지

필수 CI 성공

최소 승인자 수

Code Owner 승인

Self Approval 금지

미해결 대화 0

최신 기준 Branch 반영

서명된 Commit·승인정책

## Release Gate

전체 Build

전체 Unit·Integration

Contract Test

DB Migration 검증

보안 Scan

Artifact Hash

SBOM

설정 정합성

운영 Runbook

Rollback Artifact

## Deployment Gate

배포 승인

Change Ticket

배포 순서

사전 백업

환경 설정

OM Catalog

Gateway Route

DB Script

담당자 대기

Rollback 판단기준

## Post-Deployment Gate

Health Check

Smoke Test

핵심 거래

오류율

p95

Thread·Pool

DB Connection

거래로그

배포버전

Rollback 결정

# 자동검증 대상

## Package·Architecture

Handler → Facade

Facade → Service

Service → Rule·DAO·Client

Rule → Persistence 금지

DAO → Mapper

다른 WAR 직접 의존 금지

TCF → 업무 의존 금지

순환 의존 금지

## ServiceId

형식

업무코드 Prefix

중복

Handler 누락

빈 선언

OM Catalog

거래통제

Timeout

권한

코드·OM·정책 집합:

A = Handler ServiceId
B = OM Catalog
C = 거래통제
D = Timeout
E = 권한

운영 기준
A와 필수 B·C·D·E의 차이가 0

## Mapper

Interface 존재

XML 존재

Namespace 일치

Method·SQL ID 일치

중복 ID

ResultType 존재

다른 업무 DML

조건 없는 UPDATE·DELETE

## Transaction

@Transactional 위치

readOnly와 변경 SQL

REQUIRES\_NEW 승인

예외 catch 후 정상 반환

Checked Exception 정책

외부 호출 포함 여부

## 보안

Secret

Private Key

Token Logging

취약 Dependency

위험 API

SSRF

SQL Injection

Path Traversal

약한 암호화

인증 우회

## 운영성

GUID

오류코드

표준 응답

거래로그 완결

감사 대상

Metric

Timeout

Health Check

Rollback

# 수동검토가 필요한 항목

자동화로 완전히 판단하기 어려운 항목:

업무 요구사항이 올바른가?

도메인 경계가 적절한가?

상태 전이가 업무적으로 맞는가?

데이터 소유권 예외가 타당한가?

Fallback 데이터가 안전한가?

보상처리가 충분한가?

사용자 메시지가 이해하기 쉬운가?

성능 Trade-off가 타당한가?

잔여 위험을 수용할 수 있는가?

자동검사는 사람의 리뷰를 대체하지 않는다.

# Gate 판정

| 판정 | 의미 |
| --- | --- |
| PASS | 모든 필수 기준 충족 |
| CONDITIONAL PASS | 승인된 조치사항과 기한 존재 |
| FAIL | 기준 미달, 진행 차단 |
| WAIVED | 공식 예외승인 |
| NOT APPLICABLE | 근거와 승인 하에 제외 |
| INCONCLUSIVE | 증적 부족으로 판정 불가 |

# 증적 원칙

다음 중 하나 이상의 객관적 증적이 있어야 완료로 판정한다.

승인 설계서

Pull Request

소스코드

CI 실행결과

테스트 Report

OM 등록화면·SQL

로그·GUID 추적

성능 측정

보안 검사

장애·복구 시험

운영 Runbook

공식 승인서

담당자의 구두 설명만으로 PASS 처리하지 않는다.

# Gate 예외 승인

## 예외가 가능한 경우

도구 기술제약

긴급 장애 복구

외부 계약 제약

단계적 전환

기존 Legacy 호환

“일정이 부족하다”만으로 영구 예외를 승인하지 않는다.

## 예외 승인 필수항목

| 항목 | 내용 |
| --- | --- |
| Waiver ID | 유일 식별자 |
| 위반 Gate | 기준 항목 |
| 사유 | 기술·업무 근거 |
| 위험 | 데이터·보안·운영 영향 |
| 임시 통제 | 추가 모니터링·거래통제 |
| 적용 범위 | 모듈·ServiceId·버전 |
| Owner | 조치 책임자 |
| 승인자 | AA·보안·운영 등 |
| 만료일 | 자동 재검토일 |
| 해소계획 | 수정 일정 |
| 검증 | 완료 증적 |

# 정상 처리 흐름

1\. 개발자가 요구사항과 설계를 확인한다.
2\. 작은 논리 단위로 코드를 변경한다.
3\. Self Review를 수행한다.
4\. Compile·Unit·Secret Scan을 실행한다.
5\. PR에 영향 범위·테스트·Rollback을 작성한다.
6\. 자동 Architecture·ServiceId·Mapper Gate가 실행된다.
7\. 업무 Reviewer가 업무 규칙을 검토한다.
8\. 전문 Reviewer가 DB·보안·운영 영향을 검토한다.
9\. 리뷰 의견을 코드·테스트·문서에 반영한다.
10\. 모든 Blocking 의견을 해결한다.
11\. 필수 Gate가 PASS된다.
12\. 승인 후 보호 Branch에 Merge한다.
13\. 통합·Release Gate가 실행된다.
14\. Artifact와 검증 증적을 보존한다.

# 리뷰 실패 흐름

PR 생성
↓
ArchUnit 실패
↓
Handler → Mapper 직접 참조 발견
↓
Merge 차단
↓
Facade·Service·DAO 구조로 수정
↓
Unit·거래 테스트 추가
↓
Gate 재실행
↓
PASS

# 보안 Gate 실패 흐름

PR
↓
Secret Scan
↓
Private Key 발견
↓
즉시 Merge 차단
↓
노출 Key 폐기
↓
Git History 정리
↓
Vault Reference로 변경
↓
침해범위 확인
↓
Gate 재검증

단순히 파일에서 Key 문자열만 삭제하면 완료가 아니다.

# 조건부 승인 흐름

Major 결함

현재 Release에서 즉시 수정 어려움
↓
위험 분석

임시 거래통제·모니터링
↓
Owner·기한 지정

AA·운영 승인
↓
CONDITIONAL PASS
↓
만료 전 조치
↓
재검증

Blocker·Critical 보안·데이터 정합성 문제는 원칙적으로 조건부 승인 대상이 아니다.

# 정상 예시

## PR

요구사항
REQ-SV-014

ServiceId
SV.Customer.selectSummary

변경
고객요약 조회 필드 추가

영향
Response Contract·Mapper·화면

테스트
기존 V1 Consumer 호환
신규 필드 정상
고객 없음
권한 없음

운영
Metric 영향 없음
로그 개인정보 없음

Rollback
이전 WAR 복원

Gate
전체 PASS

# 금지 예시

컴파일되므로 리뷰 없이 Merge한다.

테스트가 통과했으므로 아키텍처 검토를 생략한다.

변수명만 보고 Transaction을 검토하지 않는다.

PR 설명을 “수정했습니다”로 작성한다.

요구사항 ID와 ServiceId를 기록하지 않는다.

기능 변경과 전체 포맷 변경을 함께 Commit한다.

다른 업무 WAR를 직접 Import한다.

Handler가 Mapper를 호출한다.

Rule이 DAO·외부 API를 호출한다.

Transaction을 Handler·Service·DAO에 중복 선언한다.

예외를 catch하고 false를 반환한다.

변경 거래의 영향 행 수를 확인하지 않는다.

화면 userId를 인증 사용자로 사용한다.

Timeout 없는 외부 호출을 승인한다.

비멱등 변경에 자동 Retry를 적용한다.

SQL 실행계획 없이 대량 조회를 승인한다.

오류 응답에 Exception 메시지를 노출한다.

System.out을 운영 로그로 사용한다.

Secret을 나중에 제거한다는 이유로 Merge한다.

Gate 실패를 CI 재실행으로 숨긴다.

Flaky Test를 무시한다.

리뷰 의견에 “이상함”만 남긴다.

작성자가 자신의 PR만 승인한다.

미해결 Blocking Comment가 있는데 Merge한다.

Waiver에 만료일과 Owner가 없다.

리팩터링 중 오류코드·SQL 결과를 함께 변경한다.

공개 Contract를 소비자 확인 없이 변경한다.

운영 배포 후 확인할 Metric과 Rollback 기준이 없다.

# 리뷰 체크리스트

## 요구사항·계약

| 확인 항목 | 완료 |
| --- | --- |
| 요구사항 ID가 있다. | □ |
| 화면·ServiceId·거래코드가 연결된다. | □ |
| 요청·응답 계약이 명확하다. | □ |
| 필수·선택·Null 의미가 있다. | □ |
| 오류코드가 정의됐다. | □ |
| 하위호환성을 검토했다. | □ |
| 요구하지 않은 기능이 추가되지 않았다. | □ |

## 아키텍처·계층

| 확인 항목 | 완료 |
| --- | --- |
| Handler는 Facade만 호출한다. | □ |
| Facade가 유스케이스 경계다. | □ |
| Service가 업무 흐름을 담당한다. | □ |
| Rule은 DB·Client를 호출하지 않는다. | □ |
| DAO는 Mapper만 호출한다. | □ |
| 다른 WAR 구현을 직접 참조하지 않는다. | □ |
| 순환 의존이 없다. | □ |
| 모호한 공통 패키지가 없다. | □ |

## 데이터·Transaction

| 확인 항목 | 완료 |
| --- | --- |
| 데이터 소유권이 맞다. | □ |
| 다른 업무 Table DML이 없다. | □ |
| Transaction이 Facade에 있다. | □ |
| Rollback 정책이 명확하다. | □ |
| 영향 행 수를 확인한다. | □ |
| Version·상태조건이 있다. | □ |
| 멱등성을 적용했다. | □ |
| History·Outbox 정합성이 있다. | □ |
| 원격 변경 보상정책이 있다. | □ |

## 보안

| 확인 항목 | 완료 |
| --- | --- |
| JWT·Header 정합성을 검증한다. | □ |
| 기능·데이터권한이 있다. | □ |
| 권한 실패 시 Fail Closed다. | □ |
| 개인정보를 최소화했다. | □ |
| 응답에 기술정보가 없다. | □ |
| Token·Secret 로그가 없다. | □ |
| Secret Vault를 사용한다. | □ |
| 감사 대상이 정의됐다. | □ |

## 성능·장애

| 확인 항목 | 완료 |
| --- | --- |
| 최대 조회 건수가 있다. | □ |
| N+1 SQL·원격호출이 없다. | □ |
| Execution Plan을 검증했다. | □ |
| Timeout Budget이 있다. | □ |
| Retry가 제한적이다. | □ |
| Circuit·Bulkhead를 검토했다. | □ |
| 전체 List 적재가 없다. | □ |
| Batch·온라인 자원경합을 확인했다. | □ |

## 운영

| 확인 항목 | 완료 |
| --- | --- |
| GUID·TraceId가 유지된다. | □ |
| 거래로그가 완결된다. | □ |
| 오류코드가 OM에 등록됐다. | □ |
| ServiceId·Timeout·권한이 등록됐다. | □ |
| Metric·Alert가 있다. | □ |
| 배포·Rollback 절차가 있다. | □ |
| 운영 Owner가 있다. | □ |
| Runbook이 있다. | □ |

# 책임 경계와 RACI

| 활동 | 작성자 | Peer | 업무리더 | AA | DBA | 보안 | QA | 운영 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Self Review | A/R | I | I | I | I | I | I | I |
| 업무 규칙 | R | C | A/R | C | I | C | C | I |
| 계층 구조 | R | C | C | A/R | I | I | C | I |
| Transaction | R | C | C | A/R | C | I | C | I |
| SQL·DDL | R | C | C | C | A/R | I | C | C |
| 인증·보안 | R | C | C | C | I | A/R | C | C |
| 테스트 | R | C | C | C | C | C | A/R | I |
| 운영성 | R | C | C | C | C | C | C | A/R |
| Gate 정책 | C | I | C | A/R | C | C | C | C |
| Waiver 승인 | C | I | C | A | C | A/C | C | A/C |
| Merge 승인 | R | R | A/C | 조건별 | 조건별 | 조건별 | C | I |
| Release 승인 | C | I | C | A/C | C | C | A/C | A/R |

# 자동검증 예시

## ArchUnit

@AnalyzeClasses(
packages = "com.nh.nsight"
)
class TcfArchitectureTest {

@ArchTest
static final ArchRule
handlersMustOnlyAccessFacades =
classes()
.that()
.resideInAPackage("..handler..")
.should()
.onlyDependOnClassesThat()
.resideInAnyPackage(
"..handler..",
"..facade..",
"..dto..",
"com.nh.nsight.tcf..",
"java..",
"jakarta..",
"org.slf4j.."
);

@ArchTest
static final ArchRule
rulesMustNotAccessInfrastructure =
noClasses()
.that()
.resideInAPackage("..rule..")
.should()
.dependOnClassesThat()
.resideInAnyPackage(
"..dao..",
"..mapper..",
"..client.."
);

@ArchTest
static final ArchRule
transactionsMustResideInFacade =
classes()
.that()
.areAnnotatedWith(
Transactional.class
)
.should()
.resideInAPackage("..facade..");
}

실제 예외 패키지는 승인 목록으로 제한한다.

## ServiceId 검증

private static final Pattern SERVICE\_ID =
Pattern.compile(
"^\[A-Z\]{2,5}\\\\."
\+ "\[A-Z\]\[A-Za-z0-9\]\*\\\\."
\+ "\[a-z\]\[A-Za-z0-9\]\*$"
);

검증:

형식

중복

업무 Prefix

Handler 소유권

OM Catalog

## 금지 API 검사

System.out.println

printStackTrace

new RestTemplate

DriverManager.getConnection

Thread.sleep

Executors.newCachedThreadPool

catch (Exception) { return null; }

@Value("${password}")

무조건 금지인지 승인 예외가 가능한지는 규칙별로 구분한다.

# 품질 Gate 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| QG-001 | 정상 업무 PR | PASS |
| QG-002 | 요구사항 ID 없음 | PR Gate 실패 |
| QG-003 | ServiceId 없음 | Gate 실패 |
| QG-004 | ServiceId 형식 오류 | Build 실패 |
| QG-005 | ServiceId 중복 | 기동·CI 실패 |
| QG-006 | Handler 누락 | Context Gate 실패 |
| QG-007 | OM Catalog 미등록 | Release Gate 실패 |
| QG-008 | Timeout 미등록 | Gate 실패 |
| QG-009 | Handler→Mapper | ArchUnit 실패 |
| QG-010 | Rule→DAO | ArchUnit 실패 |
| QG-011 | Facade→Mapper | Architecture 실패 |
| QG-012 | Handler @Transactional | Gate 실패 |
| QG-013 | 업무 WAR 직접 의존 | Dependency Gate 실패 |
| QG-014 | 순환 의존 | Build 실패 |
| QG-015 | 다른 업무 Table UPDATE | Blocker |
| QG-016 | Mapper Namespace 불일치 | Mapper Gate 실패 |
| QG-017 | 조건 없는 UPDATE | SQL Gate 실패 |
| QG-018 | UPDATE 영향 행 미확인 | Critical |
| QG-019 | Version 조건 없음 | Critical |
| QG-020 | 예외 catch 후 false | Critical |
| QG-021 | Cause 없는 SystemException | Major·Critical |
| QG-022 | Stack Trace 응답 | Security Gate 실패 |
| QG-023 | JWT 원문 로그 | Security Gate 실패 |
| QG-024 | Private Key 포함 | 즉시 차단 |
| QG-025 | 운영 Secret 설정 | 차단 |
| QG-026 | 사용자 userId 신뢰 | Critical |
| QG-027 | 권한정보 null 허용 | Critical |
| QG-028 | 외부 Timeout 없음 | Critical |
| QG-029 | 변경 Retry·멱등성 없음 | Critical |
| QG-030 | N+1 원격 호출 | Performance Gate 실패 |
| QG-031 | 대량 전체 List | Performance Gate 실패 |
| QG-032 | SQL Plan 없음 | 조건부 실패 |
| QG-033 | 테스트 정상만 존재 | Review 보완 |
| QG-034 | Rollback 테스트 없음 | Merge 차단 |
| QG-035 | Flaky Test | Gate 실패 |
| QG-036 | Coverage 기준 미달 | 정책상 실패 |
| QG-037 | Contract 필드 삭제 | Compatibility 실패 |
| QG-038 | 오류코드 의미 변경 | Compatibility 실패 |
| QG-039 | 기능+전체 포맷 | PR 분리 요청 |
| QG-040 | 기능+Framework Upgrade | PR 분리 요청 |
| QG-041 | 순수 리팩터링 | 기존 동작 유지 |
| QG-042 | 리팩터링 중 SQL 변경 | 기능 변경으로 분리 |
| QG-043 | 좋은 PR 설명 | 검토 가능 |
| QG-044 | PR 설명 “수정” | 보완 요청 |
| QG-045 | 미해결 Blocking Comment | Merge 차단 |
| QG-046 | 작성자 Self Approval | 정책 실패 |
| QG-047 | Code Owner 미승인 | Merge 차단 |
| QG-048 | Waiver 만료일 없음 | 승인 실패 |
| QG-049 | Waiver 만료 | 자동 차단 |
| QG-050 | Blocker Waiver 요청 | 원칙적 거절 |
| QG-051 | Build Artifact Hash | 생성 |
| QG-052 | SBOM 없음 | Release Gate 실패 |
| QG-053 | Health Check 실패 | 배포 중단 |
| QG-054 | Smoke 실패 | Rollback |
| QG-055 | 오류율 급증 | Rollback 판단 |
| QG-056 | GUID 추적 불가 | 운영 Gate 실패 |
| QG-057 | 거래로그 종료 누락 | 통합 Gate 실패 |
| QG-058 | System.out 발견 | 운영 품질 실패 |
| QG-059 | DDL Rollback 없음 | DB Gate 실패 |
| QG-060 | 설정 변경 근거 없음 | 승인 보류 |
| QG-061 | Pool 확대 용량근거 없음 | 승인 보류 |
| QG-062 | 인증서 만료경보 없음 | 운영 Gate 실패 |
| QG-063 | Cache Evict Commit 전 실행 | 정합성 실패 |
| QG-064 | Batch Checkpoint 없음 | Batch Gate 실패 |
| QG-065 | Callback 중복검증 없음 | 연계 Gate 실패 |
| QG-066 | 정상 Waiver 완료 | 조건부 PASS |
| QG-067 | Waiver 해소 검증 | PASS |
| QG-068 | 운영 장애 신규 규칙 추가 | 회귀 Gate 편입 |
| QG-069 | 다른 개발자 재검증 | 동일 결과 |
| QG-070 | 전체 증적 보존 | Release 승인 |

# 따라 하는 실무 절차

## 1단계. 변경 목적을 한 문장으로 정의한다

REQ-CM-014의
캠페인 승인 거래를 추가한다.

두 개 이상의 목적이 있으면 PR 분리를 검토한다.

## 2단계. 영향 범위를 작성한다

화면

ServiceId

프로그램

Table·SQL

설정

권한

연계

Cache

Batch

운영

## 3단계. Self Review를 수행한다

완료 증적:

Clean Diff

불필요한 파일 없음

Secret 없음

System.out 없음

TODO 없음

테스트 성공

## 4단계. PR 설명을 작성한다

목적

변경 내용

영향 범위

테스트

배포

Rollback

잔여 위험

## 5단계. 자동 Gate를 실행한다

Compile

Unit

Integration

ArchUnit

ServiceId

Mapper

Security

Dependency

Contract

## 6단계. 업무 리뷰를 수행한다

요구사항

상태 전이

오류조건

데이터 소유권

사용자 결과

## 7단계. 기술 리뷰를 수행한다

계층

Transaction

SQL

성능

보안

운영

복구

## 8단계. 리뷰 의견을 반영한다

지적사항

수정 내용

Commit

테스트 결과

를 연결한다.

## 9단계. Gate 판정을 확정한다

PASS

CONDITIONAL PASS

FAIL

WAIVED

판정 근거와 증적을 남긴다.

## 10단계. Merge한다

필수 승인

미해결 Blocking 0

최신 Branch

CI 성공

보호 Branch 정책

## 11단계. Release Gate를 실행한다

전체 회귀

Artifact

SBOM

설정

OM

운영 Runbook

Rollback

## 12단계. 운영 결과를 다시 Gate에 반영한다

운영 장애나 리뷰 누락사항을 신규 정적 규칙·테스트·체크리스트로 전환한다.

# 완료 체크리스트

## 리뷰 준비

| 확인 항목 | 완료 |
| --- | --- |
| 변경 목적이 하나의 문장으로 설명된다. | □ |
| 요구사항 ID가 있다. | □ |
| ServiceId·거래코드가 있다. | □ |
| 영향 범위를 작성했다. | □ |
| Self Review를 했다. | □ |
| 불필요한 Diff를 제거했다. | □ |
| 기능과 포맷 변경을 분리했다. | □ |

## 계층·Transaction

| 확인 항목 | 완료 |
| --- | --- |
| Handler는 Facade만 호출한다. | □ |
| Transaction은 Facade에 있다. | □ |
| Service·Rule 책임이 분리됐다. | □ |
| DAO·Mapper 책임이 분리됐다. | □ |
| 다른 WAR 직접 의존이 없다. | □ |
| 데이터 소유권을 지킨다. | □ |
| 영향 행 수를 확인한다. | □ |
| Version·멱등성이 있다. | □ |
| Rollback 테스트가 있다. | □ |

## 보안·운영

| 확인 항목 | 완료 |
| --- | --- |
| JWT·Header 정합성이 있다. | □ |
| 기능·데이터권한이 있다. | □ |
| Secret이 소스에 없다. | □ |
| 개인정보가 마스킹됐다. | □ |
| 오류 응답에 기술정보가 없다. | □ |
| GUID·오류코드가 있다. | □ |
| 거래로그가 완결된다. | □ |
| 감사 대상이 정의됐다. | □ |
| Metric·Alert가 있다. | □ |

## PR

| 확인 항목 | 완료 |
| --- | --- |
| PR 제목이 목적을 설명한다. | □ |
| 변경 내용을 작성했다. | □ |
| DB·설정 영향을 작성했다. | □ |
| 테스트 결과를 작성했다. | □ |
| 배포 순서를 작성했다. | □ |
| Rollback을 작성했다. | □ |
| 잔여 위험을 작성했다. | □ |
| Reviewer가 적절히 지정됐다. | □ |

## 품질 Gate

| 확인 항목 | 완료 |
| --- | --- |
| Compile이 성공했다. | □ |
| Unit·Integration이 성공했다. | □ |
| Architecture Test가 성공했다. | □ |
| ServiceId·OM 정합성이 맞다. | □ |
| Mapper 검증이 성공했다. | □ |
| Security Scan이 성공했다. | □ |
| Dependency Scan이 성공했다. | □ |
| Contract Test가 성공했다. | □ |
| Blocking 의견이 0건이다. | □ |
| 필수 승인자가 승인했다. | □ |

## 예외·운영전환

| 확인 항목 | 완료 |
| --- | --- |
| Waiver에 근거가 있다. | □ |
| 임시 통제가 있다. | □ |
| Owner·만료일이 있다. | □ |
| 해소계획이 있다. | □ |
| Health·Smoke가 준비됐다. | □ |
| Rollback Artifact가 있다. | □ |
| 운영 Runbook이 있다. | □ |
| Gate 증적이 보존된다. | □ |

# 변경·호환성·폐기 관리

## Gate 규칙 추가

신규 규칙을 바로 Blocker로 적용하면 기존 코드 전체가 실패할 수 있다.

전환:

1\. 전체 위반현황 측정
2\. 신규 코드부터 차단
3\. 기존 위반 Baseline 등록
4\. Owner·개선일정 지정
5\. 경고 → 실패 전환
6\. Baseline 제거

## Gate 규칙 변경

ServiceId 정규식 변경

Package 규칙 변경

Coverage 기준 변경

CVE 심각도 변경

영향:

전체 모듈

기존 Legacy

CI 실행시간

False Positive

개발일정

RFC·ADR과 사전 시뮬레이션이 필요하다.

## 도구 Version 변경

Gradle

JDK

ArchUnit

Checkstyle

SonarQube

SAST·SCA

Secret Scanner

도구 변경으로 결과가 달라질 수 있다.

전체 Baseline과 Gate 결과를 비교한다.

## Waiver 폐기

만료일 도래

해소 PR 완료

Gate 재실행

PASS 확인

Waiver 종료

임시 통제 제거

종료되지 않은 Waiver는 자동 알림과 Release 차단 대상으로 관리한다.

## 리뷰 체크리스트 폐기

체크리스트 항목이 자동 Gate로 완전히 대체됐다면 수동 항목을 줄일 수 있다.

그러나 자동검사 결과의 의미와 예외상황은 Reviewer가 계속 확인해야 한다.

# 시사점

## 핵심 아키텍처 판단

첫째, 코드 리뷰의 우선순위는 코드 스타일이 아니라 요구사항·데이터·Transaction·보안·복구 위험이다.

둘째, 반복적으로 발견되는 리뷰 항목은 사람의 기억에 맡기지 말고 자동 Gate로 전환해야 한다.

셋째, 자동 Gate는 구조·형식·등록 누락을 잘 찾지만 업무 의미와 위험 수용 여부까지 대신할 수 없다.

자동검사
→ 규칙 위반 발견

사람의 리뷰
→ 규칙의 타당성과 업무 의미 판단

넷째, 좋은 Pull Request는 코드보다 먼저 변경 이유·영향 범위·테스트·Rollback을 설명한다.

다섯째, 리팩터링과 기능 변경을 분리해야 실제 동작 변경의 원인을 정확히 검토할 수 있다.

여섯째, 품질 Gate의 PASS는 구두 확인이 아니라 테스트·로그·설정·OM·승인 증적으로 판단해야 한다.

일곱째, 예외승인은 규칙을 무력화하는 절차가 아니라 위험·임시 통제·Owner·만료일을 명확히 하는 한시적 통제다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 스타일 중심 리뷰 | 핵심 결함 누락 |
| 대형 PR | 검토 품질 저하 |
| 기능·포맷 혼합 | 실제 변경 은폐 |
| 계층 위반 미검출 | 책임·Transaction 우회 |
| 데이터 소유권 위반 | 업무 정합성 훼손 |
| 영향 행 미검증 | 잘못된 성공 |
| 예외 숨김 | 부분 Commit |
| 보안 리뷰 누락 | 인증우회·정보유출 |
| Timeout 검토 누락 | Thread·Pool 장애 |
| SQL 검토 누락 | 운영 성능 저하 |
| 테스트 결과만 신뢰 | 설계결함 누락 |
| 자동 Gate 부재 | Reviewer 편차 |
| 과도한 False Positive | Gate 우회 문화 |
| Waiver 무기한 | 표준 영구 훼손 |
| 구두 승인 | 책임·증적 부재 |
| 미해결 의견 Merge | 알려진 결함 배포 |
| 운영 장애 미반영 | 동일 결함 재발 |

## 우선 보완 과제

1.  실제 저장소에 PR 보호 규칙과 필수 승인자를 적용한다.
2.  Handler·Facade·Service·Rule·DAO·Mapper ArchUnit 규칙을 구현한다.
3.  업무 WAR 간 Gradle 직접 의존을 CI에서 차단한다.
4.  ServiceId 형식·중복·Handler·OM Catalog 정합성을 자동검증한다.
5.  Mapper Interface·XML·SQL ID 검증 Task를 추가한다.
6.  @Transactional 위치와 예외 숨김 패턴을 검사한다.
7.  Secret·Private Key·Token Logging 검사를 Merge Gate로 적용한다.
8.  SAST·SCA·License·SBOM Gate를 구성한다.
9.  표준 PR Template과 리뷰 심각도 체계를 적용한다.
10.  DB·보안·운영 Code Owner를 지정한다.
11.  기능·리팩터링·포맷 변경 분리 원칙을 적용한다.
12.  Gate Waiver에 Owner·만료일·임시 통제를 필수화한다.
13.  System.out과 전체 전문 출력 코드를 운영 전 제거한다.
14.  Gate 결과와 테스트·OM·배포 증적을 하나의 Release Report로 연결한다.
15.  운영 장애를 자동검사와 회귀 테스트로 전환하는 절차를 운영한다.

## 중장기 발전 방향

수동 코드 리뷰
↓
표준 체크리스트

체크리스트 중심
↓
ArchUnit·정적검사

개별 Build
↓
PR Merge Gate

코드 검사
↓
코드·OM·DB·설계 정합성

단순 승인
↓
증적 기반 Release Gate

일회성 예외
↓
만료·Owner 기반 Waiver

운영 장애 보고
↓
신규 자동 Gate·회귀 테스트

파일 Diff 리뷰
↓
요구사항–ServiceId–Artifact–운영로그 추적

# 마무리말

코드 리뷰와 품질 Gate를 수행하는 과정은 다음 질문에 답하는 일이다.

이 변경은 어떤 요구사항을 해결하는가?

화면·ServiceId·거래코드와 연결되는가?

데이터의 소유 업무는 어디인가?

업무 상태와 불변식이 보호되는가?

계층 책임을 건너뛰지 않는가?

Transaction은 올바른 범위에 있는가?

실패했을 때 전체 Rollback되는가?

동시 수정과 중복 요청을 통제하는가?

사용자 신원과 권한을 안전하게 검증하는가?

개인정보와 Secret이 노출되지 않는가?

Timeout·Retry·Circuit이 전체 예산 안에 있는가?

SQL과 대량 처리의 운영 영향은 검증됐는가?

오류와 로그로 장애를 추적할 수 있는가?

정상·경계·실패·Rollback 테스트가 있는가?

Pull Request가 하나의 목적과 영향 범위를 설명하는가?

기능 변경과 리팩터링이 분리됐는가?

자동화할 수 있는 표준은 Gate로 적용됐는가?

Gate 실패가 실제로 Merge와 배포를 차단하는가?

예외승인에 위험·Owner·만료일이 있는가?

운영 장애가 다음 변경의 품질 기준으로 반영되는가?

제26장의 핵심 흐름은 다음과 같다.

변경 목적
↓
Self Review
↓
자동검사·테스트
↓
Pull Request
↓
업무·기술 리뷰
↓
품질 Gate
↓
승인·Merge
↓
Release Gate
↓
운영 검증
↓
기준 개선

가장 중요한 원칙은 다음과 같다.

리뷰를 많이 받았다는 사실보다
어떤 위험을 발견하고 차단했는지가 중요하다.

사람이 반복해서 확인하는 규칙은 자동화하고,
자동화가 판단할 수 없는 업무 의미는 사람이 검토하며,

모든 승인은 증적과 책임을 남겨야
안전하게 변경할 수 있는 개발 체계가 된다.
