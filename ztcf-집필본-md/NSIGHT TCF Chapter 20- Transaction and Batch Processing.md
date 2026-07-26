<!-- source: ztcf-집필본/NSIGHT TCF Chapter 20- Transaction and Batch Processing.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제20장. 트랜잭션과 일괄 처리

## 이 장을 시작하며

제18장에서는 새로운 업무 데이터를 생성하는 등록 거래를 구현했다.

제19장에서는 기존 데이터의 수정·삭제·상태 전이와 동시 수정 충돌을 다루었다.

이번 장에서는 데이터 변경을 하나의 성공·실패 단위로 묶는 **트랜잭션**과, 많은 데이터를 일정 단위로 나누어 처리하는 **일괄 처리**, 즉 Batch를 다룬다.

초보 개발자는 트랜잭션을 다음과 같이 이해하기 쉽다.

text id="gaegqe" @Transactional을 붙이면 오류가 발생했을 때 DB가 알아서 되돌아간다.

Batch는 다음과 같이 생각하기 쉽다.

text id="xj3eao" @Scheduled를 붙이면 정해진 시간에 데이터를 반복 처리한다.

두 설명 모두 일부는 맞지만 운영 시스템을 설계하기에는 충분하지 않다.

트랜잭션을 설계할 때는 다음 질문에 답해야 한다.

\`\`\`text id=“lqyokt” 어떤 데이터 변경이 반드시 함께 성공해야 하는가?

어느 계층에서 트랜잭션을 시작하고 끝낼 것인가?

예외가 다른 예외로 변환돼도 Rollback되는가?

예외를 catch한 뒤 정상 반환하면 Commit되지 않는가?

DB 변경 중 외부 시스템 호출을 기다려도 되는가?

여러 DB를 변경하면 하나의 트랜잭션으로 묶을 수 있는가?

Timeout이 발생했을 때 실제 DB 작업은 종료됐는가?

Commit 이후 수행할 Cache·메시지 처리는 어떻게 보장하는가?



일괄 처리를 설계할 때는 다음 질문이 추가된다.

\`\`\`text id="n4x6f9"
전체 데이터를 한 번에 읽을 것인가?

한 번에 몇 건씩 처리할 것인가?

Chunk 하나는 하나의 트랜잭션인가?

10만 건 중 9만 건 처리 후 실패하면 어디서 재시작하는가?

같은 Job이 두 서버에서 동시에 실행되지 않는가?

성공한 데이터를 재실행하면 중복 반영되지 않는가?

실패한 한 건 때문에 전체 Job을 실패시킬 것인가?

부분 성공을 허용한다면 실패 건은 어디에 저장하는가?

입력 100만 건과 출력 99만 9천 건의 차이를 어떻게 설명하는가?

Scheduler가 메서드를 호출했다는 사실과 업무 Batch 성공을 어떻게 구분하는가?

온라인 거래와 Batch는 모두 데이터를 변경할 수 있지만 처리 특성이 다르다.

| 구분 | 온라인 거래 | 일괄 처리 |
| --- | --- | --- |
| 시작 주체 | 사용자·외부 요청 | Scheduler·운영자·파일·이벤트 |
| 응답시간 | 수 초 이내 | 수 분·수 시간 가능 |
| 처리 건수 | 단건·소량 | 대량 |
| 트랜잭션 | 유스케이스 단위 | Chunk 단위 |
| 실패 처리 | 즉시 오류 응답 | 재시작·Skip·재처리 |
| 상태 | 거래로그 | Job·Step·Chunk·Item 상태 |
| 중복 방지 | Idempotency Key | Job Instance·업무 키·Checkpoint |
| 완료 증명 | 응답·DB·거래로그 | 입출력 건수·금액·오류·대사 |
| 운영 통제 | ServiceId·거래통제 | Job 등록·스케줄·동시 실행 잠금 |

Batch는 전체 데이터를 메모리에 적재하는 프로그램이 아니다.

text id="3uw3re" 대량 입력 ↓ 일정 단위로 읽기 ↓ 일정 단위로 처리 ↓ 일정 단위로 Commit ↓ 진행 위치 저장 ↓ 실패 지점부터 재시작 ↓ 입력·성공·실패·제외 건수 대사

프로젝트의 배치 설계 기준도 Batch를 대량 데이터를 일정 단위로 처리하는 작업으로 정의하며, Chunk·Checkpoint·재처리와 중복 실행 통제를 핵심 요소로 본다.

## 핵심 관점

\`\`\`text id=“ms735x” 트랜잭션은 메서드에 붙이는 Annotation이 아니다.

하나의 업무 불변식을 어디까지 함께 Commit하고 어디까지 함께 Rollback할지를 결정하는 경계다.

Batch는 많은 SQL을 반복하는 프로그램이 아니다.

중단돼도 안전하게 재시작할 수 있고, 중복 처리 없이 처리결과를 대사할 수 있는 운영 가능한 데이터 처리 체계다.


\---

\## 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 트랜잭션의 목적을 업무 불변식 관점에서 설명한다. |
| 2 | Facade를 온라인 유스케이스 트랜잭션 경계로 사용하는 이유를 설명한다. |
| 3 | 읽기 전용 트랜잭션과 변경 트랜잭션을 구분한다. |
| 4 | Runtime Exception과 Checked Exception의 Rollback 차이를 설명한다. |
| 5 | 예외를 변환해도 원인과 Rollback 정책을 보존한다. |
| 6 | 예외를 catch하고 숨길 때 발생하는 부분 Commit 위험을 설명한다. |
| 7 | Transaction Propagation의 기본 의미를 설명한다. |
| 8 | \`REQUIRES\_NEW\`를 남용할 때의 문제를 설명한다. |
| 9 | DB Transaction 안에서 외부 호출을 기다릴 때의 위험을 설명한다. |
| 10 | 다중 DB 변경과 분산 트랜잭션의 한계를 설명한다. |
| 11 | Commit 이후 수행할 Cache·메시지 처리를 설계한다. |
| 12 | 온라인 처리와 Batch 처리의 선택 기준을 설명한다. |
| 13 | Scheduler 실행 성공과 Batch 업무 성공을 구분한다. |
| 14 | Job·Step·Chunk·Item의 관계를 설명한다. |
| 15 | Chunk 크기와 Commit 단위를 설계한다. |
| 16 | 전체 데이터를 메모리에 적재하지 않고 반복 처리한다. |
| 17 | Batch Reader·Processor·Writer의 책임을 구분한다. |
| 18 | Checkpoint와 재시작 위치를 저장한다. |
| 19 | Job Instance와 중복 실행 방지 기준을 정의한다. |
| 20 | Batch Item의 멱등성을 설계한다. |
| 21 | Retry·Skip·Fail 정책을 오류 유형별로 구분한다. |
| 22 | 부분 성공 상태와 실패 레코드를 관리한다. |
| 23 | 입력·출력·성공·실패·제외 건수를 대사한다. |
| 24 | Batch와 온라인 거래의 DB·Thread·Pool 경합을 분석한다. |
| 25 | Job 실행이력과 운영 상태를 OM에서 추적한다. |
| 26 | 재처리·보상·대사 절차를 Runbook으로 작성한다. |

\---

\# 한눈에 보는 온라인 트랜잭션

\`\`\`text id="k0ntb1"
┌────────────────────────────────────────────────────────────┐
│ 1. Handler │
│ 검증된 Request DTO 전달 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. Facade │
│ @Transactional │
│ 유스케이스 시작 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. Service │
│ 업무 순서·Rule·DAO·Client 조정 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. DAO·Mapper │
│ Master·Detail·History·Outbox 변경 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. 정상 반환 │
│ Transaction Commit │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. ETF │
│ 성공 응답·거래로그·Idempotency 상태 │
└────────────────────────────────────────────────────────────┘

# 한눈에 보는 일괄 처리

text id="inif33" ┌────────────────────────────────────────────────────────────┐ │ 1. Scheduler·운영자 │ │ Job 실행 요청 │ └─────────────────────────┬──────────────────────────────────┘ ▼ ┌────────────────────────────────────────────────────────────┐ │ 2. Job Launcher │ │ Job Instance·중복 실행·Parameter 확인 │ └─────────────────────────┬──────────────────────────────────┘ ▼ ┌────────────────────────────────────────────────────────────┐ │ 3. Job Execution │ │ 실행이력 STARTED │ └─────────────────────────┬──────────────────────────────────┘ ▼ ┌────────────────────────────────────────────────────────────┐ │ 4. Reader │ │ 다음 Chunk 읽기 │ └─────────────────────────┬──────────────────────────────────┘ ▼ ┌────────────────────────────────────────────────────────────┐ │ 5. Processor │ │ 검증·변환·업무 판단 │ └─────────────────────────┬──────────────────────────────────┘ ▼ ┌────────────────────────────────────────────────────────────┐ │ 6. Writer │ │ Bulk INSERT·UPDATE │ └─────────────────────────┬──────────────────────────────────┘ ▼ ┌────────────────────────────────────────────────────────────┐ │ 7. Chunk Commit │ │ Checkpoint 저장 │ └─────────────────────────┬──────────────────────────────────┘ ▼ ┌────────────────────────────────────────────────────────────┐ │ 8. 반복 │ │ 완료·실패·부분 성공 │ └─────────────────────────┬──────────────────────────────────┘ ▼ ┌────────────────────────────────────────────────────────────┐ │ 9. 대사 │ │ 입력 = 성공 + 실패 + 제외 │ └────────────────────────────────────────────────────────────┘

# 주요 용어

| 용어 | 의미 |
| --- | --- |
| Transaction | 여러 데이터 변경을 하나의 성공·실패 단위로 묶는 경계 |
| Commit | 트랜잭션 변경을 확정 |
| Rollback | 트랜잭션 변경을 취소 |
| Local Transaction | 하나의 DB 또는 Transaction Manager가 관리하는 거래 |
| Distributed Transaction | 여러 자원에 걸친 원자적 거래 |
| Propagation | 기존 트랜잭션 참여·신규 생성 정책 |
| Isolation | 동시 거래 간 데이터 노출 수준 |
| Batch Job | 일괄 처리 전체 업무 단위 |
| Job Instance | 동일 Job과 식별 Parameter의 논리 실행 단위 |
| Job Execution | Job Instance의 실제 실행 1회 |
| Step | Job 안의 처리 단계 |
| Chunk | 한 번 읽고 처리해 Commit하는 데이터 묶음 |
| Item | Chunk 안의 개별 업무 데이터 |
| Checkpoint | 재시작 가능한 마지막 완료 위치 |
| Retry | 일시 실패를 같은 처리에서 다시 시도 |
| Skip | 특정 실패 Item을 격리하고 다음 Item 진행 |
| Restart | 중단된 Job을 저장된 지점부터 다시 실행 |
| Re-run | Job 전체를 새 실행으로 다시 시작 |
| Reconciliation | 입력·출력·금액·상태를 대조하는 절차 |
| Compensation | 이미 완료된 외부·분산 변경을 업무적으로 되돌리는 처리 |

# 현재 구현과 목표 구조

## 현재 tcf-batch에서 확인되는 구조

현재 기준 소스의 tcf-batch는 OM 대시보드에서 사용하는 상태정보를 주기적으로 수집한다.

text id="h6663d" ApStatusCollectScheduler DbStatusCollectScheduler DeployStatusCollectScheduler ↓ 각 CollectService ↓ 대상 AP·DB·배포 상태 수집 ↓ OmDashboardStatusRepository ↓ OM\_\*\_STATUS UPSERT ↓ OM\_BATCH\_HISTORY 기록

특징:

\`\`\`text id=“w1bhd9” @Scheduled 주기 실행

수동 실행 Controller 제공

대상별 개별 수집

일부 대상 실패 시 PARTIAL

SUCCESS·PARTIAL·FAIL 이력

Tomcat 순차 WAR 기동 중 Warm-up Skip

tcf-om과 연계된 상태·이력 조회



\`OM\_BATCH\_JOB\`은 Job 등록 기준정보를, \`OM\_BATCH\_HISTORY\`는 실행 이력을 관리하는 테이블로 정의돼 있다.

\## 현재 구현 상태 판단

| 항목 | 상태 | 판단 |
|---|---|---|
| 독립 \`tcf-batch\` 모듈 | 구현 확인 | 플랫폼 수집 Job 배치 |
| \`@Scheduled\` 자동 실행 | 구현 확인 | 단순 주기 Job에 적합 |
| 수동 실행 API | 구현 확인 | OM 재실행 연계 가능 |
| 실행 이력 저장 | 구현 확인 | \`OM\_BATCH\_HISTORY\` |
| \`SUCCESS/PARTIAL/FAIL\` | 구현 확인 | 부분 실패 표현 |
| 대상별 예외 격리 | 구현 확인 | 수집 Job에 적합 |
| 기동 Warm-up Gate | 구현 확인 | 다중 WAR 순차 기동 보호 |
| Job Registry | 부분 설계 | 운영형 공통 Launcher 보완 필요 |
| 중복 실행 DB Lock | 확인되지 않음 | 다중 인스턴스 운영 보완 |
| 일반 Chunk Reader·Writer | 확인되지 않음 | 대량 업무 Batch 보완 |
| Checkpoint | 확인되지 않음 | 재시작 기능 보완 |
| Item 실패 테이블 | 확인되지 않음 | 부분 실패 재처리 보완 |
| Job Parameter 유일성 | 보완 필요 | Job Instance 식별 |
| Retry·Skip 정책 | 보완 필요 | 오류분류 기반 |
| 입력·출력 대사 | 보완 필요 | 업무 Batch 완료조건 |
| 자원 격리 | 설계 필요 | 온라인과 Pool·시간창 분리 |

\## 해석

현재 \`tcf-batch\`는 다음과 같은 \*\*소규모 반복 수집 Job\*\*에는 적합하다.

\`\`\`text id="z43799"
대상 수가 제한적이다.

각 대상 처리가 독립적이다.

한 대상 실패가 다른 대상에 영향을 주지 않는다.

Job 결과를 SUCCESS·PARTIAL·FAIL로 표현할 수 있다.

Checkpoint보다 다음 주기 재수집이 적합하다.

반면 다음 업무는 일반적인 Chunk·Checkpoint 기반 구조가 필요하다.

\`\`\`text id=“rkqd12” 수백만 고객 데이터 갱신

대형 파일 적재

일별 실적 집계

대량 이벤트 재처리

데이터 이관

다수 외부 전문 발송

대량 개인정보 파기


\---

\# 20.1 트랜잭션 경계

\## 20.1.1 트랜잭션의 목적

트랜잭션은 여러 SQL을 묶는 기술 기능이 아니라 하나의 업무 불변식을 보호하는 경계다.

예를 들어 캠페인 승인에서는 다음 변경이 모두 함께 성공해야 할 수 있다.

\`\`\`text id="mjiwgv"
캠페인 상태
REQUESTED → APPROVED

승인자·승인시각

업무이력

감사 Outbox

후속 이벤트 Outbox

다음 상태는 허용할 수 없다.

\`\`\`text id=“a39h85” 캠페인 상태는 APPROVED

승인이력 없음

감사 이벤트 없음

후속 실행 이벤트 없음



따라서 하나의 로컬 트랜잭션 경계를 구성한다.

\---

\## 20.1.2 트랜잭션 경계 판단 질문

\`\`\`text id="5242kz"
이 변경 중 하나만 성공하면 업무적으로 유효한가?

중간 상태가 다른 거래에 노출돼도 되는가?

실패했을 때 모두 원상복구해야 하는가?

같은 DB Transaction Manager가 관리할 수 있는가?

외부 시스템 변경이 포함되는가?

Transaction이 몇 초 동안 유지되는가?

어떤 Row Lock이 얼마나 오래 유지되는가?

## 20.1.3 NSIGHT TCF 온라인 경계

기본 온라인 거래:

\`\`\`text id=“g98n3p” Handler → Request 변환·Facade 호출

Facade → @Transactional → 유스케이스 경계

Service → 업무 흐름

DAO·Mapper → SQL



권장:

\`\`\`java id="9cjv5e"
@Component
@RequiredArgsConstructor
public class CampaignFacade {

private final CampaignService campaignService;

@Transactional(timeout = 5)
public CampaignUpdateResponse updateCampaign(
CampaignUpdateRequest request,
TransactionContext context) {

return campaignService.updateCampaign(
request,
context
);
}
}

트랜잭션을 Handler·Service·DAO에 중복 선언하지 않는다.

## 20.1.4 조회 트랜잭션

java id="rpsxmh" @Transactional( readOnly = true, timeout = 3 ) public CustomerSummaryResponse selectSummary(...) { ... }

readOnly=true의 의미:

\`\`\`text id=“2cv6cb” 조회 의도 표현

ORM 사용 시 변경감지 최적화 가능

ReadOnly Route 정책에 활용 가능

코드 리뷰 시 변경 SQL 여부 판단



주의:

\`\`\`text id="0iyv4d"
readOnly=true
≠ DB가 모든 UPDATE를 자동 차단

MyBatis·DB·Transaction Manager 구성에 따라 실제 차단 여부가 다를 수 있으므로 조회 Facade 아래 변경 Mapper 호출을 Architecture Test로 금지해야 한다.

## 20.1.5 변경 트랜잭션

java id="adt8d3" @Transactional(timeout = 5) public CampaignCreateResponse createCampaign(...) { ... }

변경 트랜잭션에 포함할 수 있는 항목:

\`\`\`text id=“zx7t1b” Master INSERT·UPDATE

Detail 변경

업무이력

Outbox

업무 처리상 필수 감사 데이터

Idempotency 업무상태 연계 데이터


\---

\## 20.1.6 트랜잭션을 너무 크게 잡으면 생기는 문제

\`\`\`text id="ekel7f"
Transaction 시작
↓
DB 조회
↓
대형 파일 읽기
↓
외부 API 3회
↓
사용자 입력 대기
↓
DB 변경
↓
Commit

문제:

\`\`\`text id=“d88s59” DB Connection 장기 점유

Row Lock 장기 유지

Deadlock 가능성 증가

Timeout 증가

외부 장애가 DB로 전파

Rollback 비용 증가

장애 원인 복잡


\---

\## 20.1.7 외부 호출과 트랜잭션

위험 구조:

\`\`\`text id="ifurcm"
DB Row Lock
↓
외부 시스템 호출
↓
Read Timeout 5초
↓
Retry 2회
↓
최대 15초 Lock

권장 검토 순서:

1.  외부 조회를 Transaction 전에 수행할 수 있는가
2.  DB 변경 후 Outbox로 비동기 처리할 수 있는가
3.  외부 성공·DB 실패를 보상할 수 있는가
4.  외부 호출을 멱등하게 만들 수 있는가
5.  동기 일관성이 정말 필요한가

원본도 DB 트랜잭션 안에서 느린 외부 호출을 기다리면 잠금시간과 장애 전파가 커진다고 경고한다.

## 20.1.8 Outbox Pattern

text id="nrvvrj" 업무 테이블 UPDATE + OUTBOX\_EVENT INSERT ↓ 같은 DB Transaction Commit ↓ Outbox Publisher ↓ 외부 시스템·메시지

장점:

\`\`\`text id=“34hi8u” DB 변경과 이벤트 발행 대기를 같은 Commit에 기록

외부 장애가 온라인 DB Lock에 직접 전파되지 않음

재발행·대사 가능

중복 발행 멱등성 적용 가능


\---

\## 20.1.9 다중 DB Transaction

예:

\`\`\`text id="m405wm"
CM DB
→ 캠페인 변경

OM DB
→ 감사로그 저장

두 DB가 서로 다른 Transaction Manager라면 기본 @Transactional 하나로 원자성을 보장하지 못한다.

대안:

| 방식 | 장점 | 주의 |
| --- | --- | --- |
| JTA·XA | 원자성 | 구성·운영 복잡성 |
| Outbox | 재처리·확장성 | 최종 일관성 |
| 보상 처리 | 외부 포함 가능 | 업무 복잡성 |
| 단일 소유 DB | 단순 | 데이터 경계 재설계 |
| 대사·재처리 | 운영 가능 | 일시 불일치 허용 |

분산 변경은 로컬 트랜잭션처럼 보이게 숨기지 않는다.

## 20.1.10 Transaction Propagation

주요 개념:

| Propagation | 의미 |
| --- | --- |
| REQUIRED | 기존 Transaction 참여, 없으면 생성 |
| REQUIRES\_NEW | 기존 Transaction 일시 중지 후 신규 생성 |
| SUPPORTS | 존재하면 참여, 없어도 실행 |
| MANDATORY | 기존 Transaction이 반드시 있어야 함 |
| NOT\_SUPPORTED | Transaction 없이 실행 |
| NEVER | Transaction이 있으면 오류 |
| NESTED | Savepoint 기반 중첩 처리, 지원 여부 확인 |

기본은 REQUIRED다.

## 20.1.11 REQUIRES\_NEW 남용

text id="iwjqho" Main Transaction ↓ Audit Service REQUIRES\_NEW ↓ Audit Commit ↓ Main Transaction Rollback

결과:

text id="us2vko" 감사에는 성공처럼 보일 수 있으나 업무 데이터는 Rollback

또는 반대 상황이 생길 수 있다.

별도 트랜잭션이 필요한 이유와 상태 표현을 명확히 정의해야 한다.

## 20.1.12 Self Invocation 문제

\`\`\`java id=“2vepnh” @Service public class CampaignService {

public void outer() {
innerRequiresNew();
}

@Transactional(
propagation = Propagation.REQUIRES\_NEW
)
public void innerRequiresNew() {
...
}

}



같은 객체 내부 직접 호출은 Spring Proxy를 통과하지 않아 Annotation이 적용되지 않을 수 있다.

트랜잭션 동작은 Annotation 존재가 아니라 실제 Proxy·Transaction 로그·테스트로 검증한다.

\---

\## 20.1.13 Isolation Level

| 수준 | 주요 의미 |
|---|---|
| READ UNCOMMITTED | 미Commit 데이터 노출 가능 |
| READ COMMITTED | Commit 데이터만 조회 |
| REPEATABLE READ | 같은 Row 반복조회 안정 |
| SERIALIZABLE | 가장 강한 격리·동시성 비용 |

DB 기본값을 무조건 바꾸지 않는다.

다음 문제는 Version·상태조건·Unique Constraint로 해결할 수 있는지 먼저 검토한다.

\`\`\`text id="5x6c0t"
중복 등록

Lost Update

상태 전이 충돌

업무 유일성

## 20.1.14 Transaction Timeout

text id="szcu0o" DB Query Timeout < Spring Transaction Timeout ≤ TCF 거래 Timeout < Gateway Read Timeout

Transaction Timeout은 모든 DB 드라이버 작업을 즉시 강제 종료한다고 단정할 수 없다.

다음을 함께 검증한다.

\`\`\`text id=“0yztmn” Statement Query Timeout

Thread Interrupt

Connection 반환

Rollback

후속 거래 영향


\---

\## 20.1.15 Commit 이후 처리

Cache Evict·메시지 발행이 Commit 이후에만 실행돼야 하는 경우:

\`\`\`java id="n3urhi"
@TransactionalEventListener(
phase = TransactionPhase.AFTER\_COMMIT
)
public void handleCampaignUpdated(
CampaignUpdatedEvent event) {
cache.evict(event.campaignId());
}

주의:

text id="79vw02" AFTER\_COMMIT 처리 실패 → DB는 이미 Commit됨

따라서 중요 후속 처리는 Outbox와 재처리 구조가 더 적합하다.

# 20.2 롤백 조건과 예외

## 20.2.1 기본 Rollback

Spring의 일반적 기본 정책:

\`\`\`text id=“86jdio” RuntimeException → Rollback

Error → Rollback

Checked Exception → 기본적으로 Commit 가능



프로젝트 예외 계층과 실제 설정을 확인해야 한다.

\---

\## 20.2.2 Checked Exception 주의

\`\`\`java id="qmapo0"
@Transactional
public void process() throws IOException {

mapper.insertMaster(...);

throw new IOException(
"파일 처리 오류"
);
}

기본 설정에 따라 DB 변경이 Commit될 수 있다.

필요한 경우:

java id="awpr09" @Transactional( rollbackFor = Exception.class )

그러나 모든 예외를 무조건 rollbackFor=Exception.class로 처리하기 전에 업무·기술 예외 분류를 명확히 해야 한다.

## 20.2.3 예외 변환과 Rollback

정상적인 변환:

java id="swxus1" catch (DuplicateKeyException exception) { throw new CampaignDuplicateException( "이미 등록된 캠페인입니다.", exception ); }

변환한 예외가 Runtime Exception이면 Rollback 정책이 유지된다.

위험:

java id="f0jftq" catch (DataAccessException exception) { throw new CampaignCheckedException( exception ); }

Checked Exception으로 바뀌면 Rollback 정책이 달라질 수 있다.

## 20.2.4 예외 숨김

금지:

\`\`\`java id=“4xtbt4” @Transactional public boolean updateCampaign(…) {

try {
mapper.updateCampaign(...);
mapper.insertHistory(...);
return true;

} catch (Exception exception) {
log.error("오류", exception);
return false;
}

}



예외가 Transaction 경계 밖으로 전달되지 않으면 Spring은 정상 종료로 판단해 Commit할 수 있다.

\---

\## 20.2.5 예외를 응답으로 바꾸는 위치

\`\`\`text id="lc3p2i"
Mapper·DAO
→ 기술 예외 발생

Service
→ 업무 의미가 있는 경우 변환

Facade
→ 예외를 숨기지 않고 전파

TCF·ETF
→ 표준 응답 변환

Facade 내부에서 실패 응답 DTO를 정상 반환하면 Transaction이 Commit될 수 있다.

금지:

java id="5emk9x" @Transactional public Result update(...) { try { ... } catch (Exception e) { return Result.fail(); } }

## 20.2.6 Rollback Only

내부 메서드에서 예외가 발생해 Transaction이 Rollback-only로 표시된 뒤 외부에서 예외를 잡고 정상 반환하면 마지막 Commit 단계에서 UnexpectedRollbackException이 발생할 수 있다.

\`\`\`text id=“5s7odm” 내부 예외

→ Rollback-only

→ 외부 catch

→ 정상 반환 시도

→ Commit 시 UnexpectedRollbackException



예외를 숨기지 말고 유스케이스 경계에서 일관되게 종료한다.

\---

\## 20.2.7 업무 오류도 Rollback해야 하는가

업무 오류 종류에 따라 다르다.

\### 변경 전 업무 검증 실패

\`\`\`text id="kek19o"
상태 불가

권한 없음

중복 데이터

DB 변경 전 발생하므로 Rollback할 내용이 없지만 Transaction은 실패 종료한다.

### 일부 변경 후 업무 오류

\`\`\`text id=“yng9y5” Master UPDATE

→ Detail 업무검증 실패



전체 Rollback돼야 한다.

업무 예외가 Runtime Exception인지 확인한다.

\---

\## 20.2.8 로그 저장 실패

업무로그와 기술로그를 구분한다.

\`\`\`text id="36db6v"
애플리케이션 File Log 실패
→ 일반적으로 업무 Rollback 대상 아님

법적 감사 데이터 저장 실패
→ 정책에 따라 업무 Rollback

거래로그 저장 실패
→ 별도 복구·대사 또는 중요 거래 차단

모든 로그 실패를 같은 정책으로 처리하지 않는다.

## 20.2.9 Batch Chunk Rollback

Chunk 크기 100:

\`\`\`text id=“spxwjn” Item 1~100 처리

Item 78 실패



정책 A:

\`\`\`text id="8ajtdk"
Chunk 전체 Rollback

1~100 다시 처리

정책 B:

\`\`\`text id=“z0sjd7” Item 78 Skip

나머지 Commit

Job PARTIAL



어떤 방식을 적용할지는 데이터 의존성과 실패 유형으로 결정한다.

\---

\## 20.2.10 Retryable Exception

재시도 후보:

\`\`\`text id="jkjy03"
일시적 DB 연결 오류

Lock Timeout

HTTP 503

일시 Network 오류

Rate Limit

재시도 금지 또는 수정 필요:

\`\`\`text id=“k9xzod” 입력 형식 오류

권한 오류

업무 중복

SQL 문법 오류

필수 데이터 미존재

Schema 불일치


\---

\## 20.2.11 Retry와 전체 Timeout

\`\`\`text id="uww78t"
Read Timeout 2초

Retry 3회

처리시간·Backoff 제외 최대 6초 이상

전체 Job·Step·온라인 Timeout 예산 안에서 횟수와 간격을 정한다.

재시도는 부하를 증가시키므로 장애 중에는 정상 요청보다 더 큰 부하를 만들 수 있다.

## 20.2.12 Rollback 테스트

최소 테스트:

\`\`\`text id=“5ms8g6” Master 성공 + Detail 실패

Main UPDATE 성공 + History 실패

DB 변경 성공 + Outbox 실패

Checked Exception

Runtime Exception

예외 변환

예외 catch·숨김

Timeout

Deadlock

Commit 실패



검증:

\`\`\`text id="5b3zte"
DB 원상복구

영향 행 수

Idempotency 상태

거래로그 상태

Connection 반환

후속 거래 정상

# 20.3 반복 SQL과 Batch

## 20.3.1 온라인 반복문과 Batch의 차이

금지에 가까운 온라인 처리:

\`\`\`java id=“n91muo” @Transactional public void updateAllCustomers( List customerNos) {

for (String customerNo : customerNos) {
mapper.updateCustomer(customerNo);
}

}



입력이 수만 건이면 온라인 Thread와 DB Connection을 장시간 점유한다.

다음 조건이면 Batch 전환을 검토한다.

\`\`\`text id="bupbsl"
수 초 안에 끝나지 않는다.

수천 건 이상 처리한다.

재시작이 필요하다.

진행률이 필요하다.

실패 Item 격리가 필요하다.

사용자가 결과 파일을 기다릴 수 있다.

온라인 피크와 자원을 분리해야 한다.

## 20.3.2 Batch Job 설계 항목

| 항목 | 내용 |
| --- | --- |
| Job ID | 운영 식별자 |
| Job명 | 업무 의미 |
| 소유 업무 | 데이터·운영 책임 |
| 실행 주기 | Cron·수동·Event |
| Job Parameter | 기준일·파일ID·회차 |
| 중복 기준 | 동일 실행 판단키 |
| Reader | 입력 범위와 순서 |
| Processor | 업무 검증·변환 |
| Writer | 출력·영향 행 수 |
| Chunk Size | Commit 건수 |
| Checkpoint | 재시작 위치 |
| Retry | 재시도 오류·횟수 |
| Skip | 제외 오류·상한 |
| Timeout | Job·Step·SQL |
| 대사 | 건수·금액·Hash |
| 알림 | 성공·부분 성공·실패 |
| 운영자 | 실행·재처리 책임 |
| 보존 | 이력·오류·파일 |

## 20.3.3 Job과 Scheduler 구분

\`\`\`text id=“i7gfib” Scheduler = 언제 시작할 것인가

Batch Job = 무엇을 어떻게 처리할 것인가



Scheduler가 메서드를 호출했어도 Job이 실패할 수 있다.

\`\`\`text id="woaq3g"
Scheduler Trigger 성공

→ Job 실행 STARTED

→ Chunk 3 실패

→ Job FAIL

따라서 Scheduler 로그만으로 Batch 성공을 판단하지 않는다.

## 20.3.4 @Scheduled 적용 기준

적합:

\`\`\`text id=“dk69nm” 단순 주기 수집

처리시간이 짧음

대상 수가 작음

재시작보다 다음 주기 재처리가 적합

복잡한 Parameter가 없음



주의:

\`\`\`text id="a61e6d"
Job마다 @Scheduled 분산

운영에서 스케줄 변경 어려움

다중 인스턴스 중복 실행

실행 이력·재시작 공통화 부족

WAR 재기동 시 실행 누락·중복

운영형 구조에서는 Scheduler Runner가 OM Job 등록정보를 스캔하고 Job Launcher를 호출하는 방식을 검토한다.

## 20.3.5 Job 식별자

예:

text id="e9dtmp" BAT-CM-001

Job Instance Key:

text id="bh3o9m" Job ID + 기준일 + 실행회차 + 입력파일 ID + 업무 Parameter Hash

예:

text id="gqfrrd" BAT-CM-001 + 2026-07-18 + 01 + FILE-000123

## 20.3.6 중복 실행 방지

다중 인스턴스:

\`\`\`text id=“2o1116” Batch VM A → 02:00 실행

Batch VM B → 02:00 실행



두 서버 모두 같은 데이터를 처리해서는 안 된다.

방식:

\`\`\`text id="r0petp"
DB Job Lock Row

Unique Job Instance

Lease 기반 Lock

Leader Election

전용 Scheduler

단순 JVM synchronized는 다른 인스턴스를 막지 못한다.

## 20.3.7 Job Lock 예

sql id="ylv8te" UPDATE OM\_BATCH\_JOB SET LOCK\_OWNER = #{instanceId}, LOCKED\_AT = SYSTIMESTAMP, RUN\_STATUS = 'RUNNING' WHERE JOB\_ID = #{jobId} AND USE\_YN = 'Y' AND ( RUN\_STATUS <> 'RUNNING' OR LOCKED\_AT < #{expiredAt} )

영향 행 수가 1인 인스턴스만 실행한다.

Lock 만료정책에는 장기 실행 Job을 강제로 중복 시작시키지 않도록 Heartbeat가 필요할 수 있다.

## 20.3.8 Job 상태

\`\`\`text id=“j1o9zb” READY

STARTING

RUNNING

SUCCESS

PARTIAL

FAIL

STOPPING

STOPPED

ABANDONED

UNKNOWN



현재 수집 Job의 \`SUCCESS/PARTIAL/FAIL\`은 대상별 결과를 표현하는 데 적합하다.

일반 대량 Batch에서는 Step·Chunk·Item 상태를 추가로 관리해야 한다.

\---

\## 20.3.9 Reader

Reader의 책임:

\`\`\`text id="odqpm5"
처리 대상 범위 결정

안정된 순서

다음 위치 관리

전체 메모리 적재 금지

Checkpoint 가능한 Key 제공

권장 Keyset Reader:

sql id="ulwbff" SELECT CUSTOMER\_NO, BASE\_DATE, STATUS\_CODE FROM SV\_CUSTOMER\_TARGET WHERE BASE\_DATE = #{baseDate} AND CUSTOMER\_NO > #{lastCustomerNo} ORDER BY CUSTOMER\_NO FETCH FIRST #{chunkSize} ROWS ONLY

깊은 Offset보다 Keyset이 재시작에 유리할 수 있다.

## 20.3.10 안정된 처리 순서

text id="ag5af7" ORDER BY 없음 → 처리 순서 비결정적 → Checkpoint 재시작 시 누락·중복 가능

Checkpoint Key는 가능한 한 불변이며 유일해야 한다.

\`\`\`text id=“hs3u81” CUSTOMER\_NO

EVENT\_ID

FILE\_LINE\_NO

SOURCE\_SEQUENCE



처리 중 변경될 수 있는 상태값만 Cursor로 사용하지 않는다.

\---

\## 20.3.11 Processor

Processor 책임:

\`\`\`text id="12yt06"
형식 검증

업무 규칙

코드 변환

출력 Command 생성

Skip·Fail 분류

Processor가 직접 Commit하거나 Job 상태를 변경하지 않는다.

## 20.3.12 Writer

Writer 책임:

\`\`\`text id=“wuj3fs” Bulk INSERT·UPDATE

영향 행 수 확인

업무 키 중복 방지

Item 멱등성

출력 건수 반환



Writer에서 외부 시스템을 Item마다 동기 호출하면 처리량과 재시작이 어려워진다.

외부 전송은 Outbox·별도 Step을 검토한다.

\---

\## 20.3.13 Chunk Transaction

Chunk 크기 1,000:

\`\`\`text id="mv3vjp"
1,000건 Read

→ 1,000건 Process

→ Bulk Write

→ Commit

→ Checkpoint 저장

Chunk 실패 시 해당 Chunk만 Rollback하고 이전 Chunk는 Commit 상태를 유지한다.

\`\`\`text id=“1s1gzm” Chunk 1 성공 → Commit

Chunk 2 성공 → Commit

Chunk 3 실패 → Rollback

재시작 → Chunk 3 시작 위치


\---

\## 20.3.14 Chunk 크기 판단

| 작은 Chunk | 큰 Chunk |
|---|---|
| Rollback 범위 작음 | 처리량 증가 가능 |
| Commit 횟수 증가 | DB Lock·Undo 증가 |
| Checkpoint 자주 저장 | 장애 시 재처리량 증가 |
| 네트워크 왕복 증가 | 메모리 사용 증가 |
| 재시작 세밀 | 장시간 Transaction |

초기값을 정한 뒤 실제 데이터로 측정한다.

예:

\`\`\`text id="6ukp6f"
100

500

1,000

5,000

처리시간·Heap·DB Lock·Redo·Pool 사용률을 비교한다.

## 20.3.15 JDBC Batch

반복 단건 SQL:

text id="hqfa8s" 1건 UPDATE × 10,000회

JDBC·MyBatis Batch는 네트워크 왕복을 줄일 수 있다.

주의:

\`\`\`text id=“stzpvm” Batch 결과 배열 해석

실패 Item 위치

Driver별 동작

Generated Key

메모리 Flush

Chunk와 Batch 크기 정합



Batch SQL을 사용한다고 재시작 가능한 Batch Job이 되는 것은 아니다.

\---

\## 20.3.16 Checkpoint

Checkpoint 예:

\`\`\`text id="f32vsf"
lastProcessedCustomerNo

lastFileLineNo

lastEventId

processedChunkNo

readCount

writeCount

Checkpoint 저장 시점:

text id="skm5o9" Chunk DB Commit ↓ 같은 Transaction 또는 원자성 보장 방식 ↓ Checkpoint 완료

업무 데이터는 Commit됐는데 Checkpoint가 저장되지 않으면 재시작 때 같은 Item을 다시 처리할 수 있다.

따라서 Writer의 Item 멱등성이 필요하다.

## 20.3.17 재시작과 재실행

| 구분 | 의미 |
| --- | --- |
| Restart | 같은 Job Instance의 실패 지점부터 재개 |
| Re-run | 새로운 Job Instance로 처음부터 실행 |
| Retry | 같은 Item·Chunk 안에서 재시도 |
| Reprocess | 실패 Item만 별도 처리 |
| Compensation | 이미 반영된 결과를 업무적으로 취소 |

운영자가 “재실행” 버튼을 눌렀을 때 무엇을 의미하는지 명확히 표시해야 한다.

## 20.3.18 Batch Item 멱등성

예:

sql id="9ycqwf" MERGE INTO SV\_DAILY\_RESULT T USING ( SELECT #{baseDate} AS BASE\_DATE, #{branchCode} AS BRANCH\_CODE, #{resultType} AS RESULT\_TYPE, #{resultAmount} AS RESULT\_AMOUNT FROM DUAL ) S ON ( T.BASE\_DATE = S.BASE\_DATE AND T.BRANCH\_CODE = S.BRANCH\_CODE AND T.RESULT\_TYPE = S.RESULT\_TYPE ) WHEN MATCHED THEN UPDATE SET T.RESULT\_AMOUNT = S.RESULT\_AMOUNT, T.UPDATED\_AT = SYSTIMESTAMP WHEN NOT MATCHED THEN INSERT (...) VALUES (...);

또는 업무 키 Unique Constraint와 처리이력으로 중복을 차단한다.

## 20.3.19 Retry·Skip·Fail Matrix

| 오류 | Retry | Skip | Job 결과 |
| --- | --- | --- | --- |
| 일시 DB 연결 | O | X | 재시도 초과 시 FAIL |
| Lock Timeout | 제한적 O | X | FAIL |
| 외부 503 | 제한적 O | 정책 | PARTIAL·FAIL |
| 입력 형식 오류 | X | O 가능 | PARTIAL |
| 필수 업무 데이터 없음 | X | 정책 | PARTIAL·FAIL |
| 권한 오류 | X | X | FAIL |
| SQL 문법 오류 | X | X | FAIL |
| 중복 데이터 | X | 업무정책 | PARTIAL·FAIL |
| 개인정보 파기 실패 | 제한적 | X | FAIL |
| 알 수 없는 오류 | X | X | FAIL |

Skip 상한을 정한다.

\`\`\`text id=“6yfq1x” 허용 Skip 10건

또는 전체의 0.1%



상한을 넘으면 Job을 실패시킨다.

\---

\## 20.3.20 Batch 이력

Job 이력:

\`\`\`text id="94udlp"
jobExecutionId

jobId

jobInstanceKey

businessDate

parameters

startedAt

endedAt

status

readCount

processCount

writeCount

skipCount

retryCount

rollbackCount

durationMs

instanceId

errorCode

message

현재 프로젝트의 OM\_BATCH\_HISTORY는 기본 실행 결과를 저장한다.

대량 업무 Batch에서는 Step·Checkpoint·오류 상세 테이블을 확장해야 한다.

## 20.3.21 수동 실행

수동 실행은 자동 실행과 같은 Launcher를 사용해야 한다.

\`\`\`text id=“l285rm” Scheduler ↓ BatchJobLauncher

OM 수동 실행 ↓ BatchJobLauncher



수동 API가 Job Service를 직접 호출해 중복 실행·이력·Lock을 우회하면 안 된다.

\---

\## 20.3.22 Batch 시간창

\`\`\`text id="y37t7h"
온라인 피크
09:00~18:00

대량 Batch
01:00~05:00

확인:

\`\`\`text id=“c1mlv6” 업무 온라인 Peak

DB Backup

ETL

통계 수집

인덱스 작업

파일 수신

DR 복제 지연



프로젝트 점검 기준도 Batch 시간창, 자원 분리, 재처리·Checkpoint를 별도 검증 대상으로 둔다.

\---

\## 20.3.23 자원 격리

| 자원 | 분리·통제 기준 |
|---|---|
| JVM | 대량 Job은 별도 Batch WAR·VM 검토 |
| Thread | Batch Executor 상한 |
| DB Pool | 온라인 Pool과 분리 또는 상한 |
| SQL | Resource Manager·Timeout |
| 파일 I/O | 전용 경로·용량 |
| Network | 외부 전송 동시성 |
| Heap | Chunk·Buffer 제한 |
| CPU | 병렬 Partition 수 제한 |

현재 \`tcf-batch\`처럼 작은 Pool을 독립적으로 사용하는 구조는 온라인 Pool 보호에 유리하다.

\---

\# 20.4 부분 실패를 다루는 방법

\## 20.4.1 부분 실패란 무엇인가

부분 실패는 전체 작업 중 일부만 성공하고 일부는 실패한 상태다.

예:

\`\`\`text id="8cd4g4"
입력 1,000건

성공 970건

형식 오류 10건

외부 Timeout 15건

중복 5건

이 Job을 단순 SUCCESS 또는 FAIL 하나로만 표현하면 실제 상태를 알 수 없다.

## 20.4.2 부분 실패 허용 여부

### 전체 원자성 필요

\`\`\`text id=“esg780” 회계 마감

잔액 이관

승인 확정

원장 대체



일부만 성공하면 업무적으로 무효다.

\`\`\`text id="f93jeg"
한 건 실패
→ 전체 FAIL
→ 전체 Rollback 또는 보상

### Item 독립성 존재

\`\`\`text id=“t7hj54” AP 상태 수집

고객별 알림 발송

개별 파일 Row 검증

독립 고객 데이터 갱신



일부 실패를 격리하고 나머지를 처리할 수 있다.

\`\`\`text id="3ny24y"
성공 Item Commit

실패 Item Error Table

Job PARTIAL

## 20.4.3 부분 성공 계약

다음 항목을 설계서에 명시한다.

\`\`\`text id=“ymp7dh” 부분 성공 허용 여부

허용 실패 유형

허용 실패 건수·비율

실패 Item 저장위치

재처리 방법

최종 Job 상태

사용자·운영 알림

대사 완료 기준


\---

\## 20.4.4 Item 오류 테이블

\`\`\`sql id="viim4p"
CREATE TABLE OM\_BATCH\_ITEM\_ERROR (
ERROR\_ID VARCHAR2(50) NOT NULL,
JOB\_EXECUTION\_ID VARCHAR2(50) NOT NULL,
JOB\_ID VARCHAR2(50) NOT NULL,
STEP\_ID VARCHAR2(50) NOT NULL,
ITEM\_KEY VARCHAR2(200),
SOURCE\_POSITION VARCHAR2(200),
ERROR\_TYPE VARCHAR2(30) NOT NULL,
ERROR\_CODE VARCHAR2(50) NOT NULL,
ERROR\_MESSAGE VARCHAR2(1000),
RETRYABLE\_YN CHAR(1) NOT NULL,
RETRY\_COUNT NUMBER(5) NOT NULL,
ITEM\_STATUS VARCHAR2(20) NOT NULL,
CREATED\_AT TIMESTAMP NOT NULL,
UPDATED\_AT TIMESTAMP NOT NULL,
CONSTRAINT PK\_OM\_BATCH\_ITEM\_ERROR
PRIMARY KEY (ERROR\_ID)
);

민감한 원문 전체를 오류 테이블에 저장하지 않는다.

## 20.4.5 실패 Item 상태

\`\`\`text id=“3fnn3w” FAILED

RETRY\_READY

RETRYING

REPROCESSED

SKIPPED

MANUAL\_REVIEW

CANCELLED


\---

\## 20.4.6 부분 실패 처리 흐름

\`\`\`text id="x16su2"
Item 처리
↓
오류 발생
↓
오류 분류
├─ Retryable
│ ↓
│ 제한 재시도
│ ├─ 성공 → 정상 처리
│ └─ 실패 → Error Table
│
├─ Skippable
│ ↓
│ Error Table
│ ↓
│ 다음 Item
│
└─ Fatal
↓
Chunk Rollback
↓
Job FAIL

## 20.4.7 PARTIAL 상태

\`\`\`text id=“67p20e” SUCCESS → 실패·Skip 0

PARTIAL → 일부 성공 + 일부 실패·Skip

FAIL → 업무 완료조건 미충족

UNKNOWN → 실제 반영상태를 확정할 수 없음



\`PARTIAL\`을 성공처럼 무시해서는 안 된다.

경고·재처리·대사 대상이다.

\---

\## 20.4.8 실패 격리와 의존 데이터

Item이 서로 독립적이지 않다면 Skip하면 안 된다.

예:

\`\`\`text id="qagv5m"
고객 Master 실패

고객 Detail은 성공

불완전한 데이터가 만들어진다.

같은 Aggregate의 Master·Detail은 같은 Chunk 내에서 하나의 Item Transaction으로 처리한다.

## 20.4.9 부분 실패와 순서

이벤트 순서가 중요한 경우:

\`\`\`text id=“na3urp” 이벤트 100 → 고객 생성

이벤트 101 → 고객 상태 변경



100 실패 후 101을 처리하면 상태 전이가 잘못된다.

대안:

\`\`\`text id="10my50"
업무 키별 순서 보장

선행 실패 시 후속 Item 보류

Partition Key 동일 처리

Version·상태 검증

Dead Letter Queue

## 20.4.10 재처리

실패 Item만 재처리할 때 다음을 확인한다.

\`\`\`text id=“kjb28p” 원본 데이터가 수정됐는가?

오류 원인이 제거됐는가?

이미 성공 처리되지 않았는가?

같은 업무 키가 존재하는가?

Item 멱등성이 보장되는가?

재처리 실행 ID는 무엇인가?

원 Job과 어떻게 연결되는가?


\---

\## 20.4.11 보상 처리

분산 처리:

\`\`\`text id="ej28tq"
내부 DB Commit 성공

외부 시스템 A 성공

외부 시스템 B 실패

로컬 Rollback으로 이미 성공한 A를 되돌릴 수 없다.

보상 예:

\`\`\`text id=“ua62wi” 외부 A 취소 요청

내부 상태 COMPENSATION\_REQUIRED

운영자 수동 보정

대사 후 완료



보상도 실패할 수 있으므로 상태와 재시도를 관리한다.

\---

\## 20.4.12 대사 공식

기본 건수 대사:

\`\`\`text id="mfokmz"
입력 건수
\=
성공 건수
\+ 실패 건수
\+ 제외 건수

Reader 필터가 있는 경우:

text id="q620ez" 원천 건수 = 처리 대상 건수 + 비대상 건수

처리 대상:

text id="b0o9d3" 처리 대상 = 성공 + 실패 + Skip

## 20.4.13 금액 대사

금융·실적 Batch:

\`\`\`text id=“vtc3lb” 입력 금액 합계

출력 금액 합계

성공 금액

실패 금액

조정 금액

차이 금액



완료 기준:

\`\`\`text id="e38o7t"
차이 건수 = 0

차이 금액 = 0

또는
승인된 허용오차 이내

건수만 맞고 금액이 틀릴 수 있으므로 둘 다 확인한다.

## 20.4.14 Hash 대사

파일·대형 데이터:

\`\`\`text id=“mxr86r” 원본 파일 Hash

수신 파일 Hash

처리 입력 Hash

출력 파일 Hash



전송 중 변조·누락 여부를 확인한다.

\---

\## 20.4.15 결과 대사 테이블

\`\`\`sql id="1z9qnz"
CREATE TABLE OM\_BATCH\_RECONCILIATION (
RECONCILIATION\_ID VARCHAR2(50) NOT NULL,
JOB\_EXECUTION\_ID VARCHAR2(50) NOT NULL,
SOURCE\_COUNT NUMBER(20),
TARGET\_COUNT NUMBER(20),
SUCCESS\_COUNT NUMBER(20),
FAIL\_COUNT NUMBER(20),
SKIP\_COUNT NUMBER(20),
SOURCE\_AMOUNT NUMBER(20,2),
TARGET\_AMOUNT NUMBER(20,2),
DIFFERENCE\_COUNT NUMBER(20),
DIFFERENCE\_AMOUNT NUMBER(20,2),
RECON\_STATUS VARCHAR2(20) NOT NULL,
CHECKED\_AT TIMESTAMP NOT NULL,
CONSTRAINT PK\_OM\_BATCH\_RECON
PRIMARY KEY (RECONCILIATION\_ID)
);

## 20.4.16 Job 완료 기준

\`\`\`text id=“wb9on7” 모든 Step 종료

장기 RUNNING 없음

Chunk Commit 완료

Checkpoint 저장 완료

입력·출력 대사 완료

실패 Item 분류 완료

허용 Skip 이내

후속 Outbox 상태 확인

실행이력 종료상태 기록

운영 알림 전송



Scheduler 메서드가 예외 없이 반환한 것만으로 완료 처리하지 않는다.

\---

\# 목표 Batch 아키텍처

\`\`\`text id="1ikitt"
OM\_BATCH\_JOB
├─ Job ID
├─ Cron
├─ Use Y/N
├─ Chunk Size
├─ Retry·Skip
├─ Timeout
└─ 동시 실행 정책
↓
BatchScheduleScanner
↓
BatchJobLauncher
├─ Job Instance 생성
├─ Distributed Lock
├─ Execution History
└─ Handler Registry
↓
BatchJobHandler
↓
Step
├─ Reader
├─ Processor
├─ Writer
└─ Chunk Transaction
↓
Checkpoint
↓
Item Error
↓
Reconciliation
↓
SUCCESS·PARTIAL·FAIL

# 정상 온라인 트랜잭션 흐름

text id="ybzmmd" 1. Handler가 Facade를 호출한다. 2. Facade가 변경 Transaction을 시작한다. 3. Service가 업무 데이터를 조회한다. 4. Rule이 현재 상태와 권한을 검증한다. 5. Master·Detail·History·Outbox를 변경한다. 6. 영향 행 수를 확인한다. 7. Service가 정상 반환한다. 8. Spring이 Transaction을 Commit한다. 9. ETF가 성공 응답과 거래로그를 종료한다. 10. Commit 이후 Publisher가 Outbox를 처리한다.

# 온라인 Rollback 흐름

text id="cw3f4y" Master UPDATE 성공 ↓ Detail INSERT 성공 ↓ History INSERT 실패 ↓ Runtime Exception ↓ Facade Transaction Rollback ↓ Master·Detail 원상복구 ↓ ETF 시스템 오류 ↓ Idempotency FAIL·UNKNOWN 정책

# 정상 Batch 흐름

text id="x0irmu" 1. Scheduler가 Job Launcher를 호출한다. 2. Launcher가 Job Instance와 중복 실행을 확인한다. 3. Job 이력을 RUNNING으로 등록한다. 4. Reader가 첫 Chunk를 읽는다. 5. Processor가 Item을 검증·변환한다. 6. Writer가 Bulk DML을 수행한다. 7. 영향 행 수를 확인한다. 8. Chunk를 Commit한다. 9. Checkpoint를 저장한다. 10. 다음 Chunk를 반복한다. 11. 입력·출력 건수와 금액을 대사한다. 12. Job을 SUCCESS로 종료한다. 13. OM 이력과 Metric을 갱신한다.

# Batch 재시작 흐름

\`\`\`text id=“q1uatu” Chunk 1 Commit

Chunk 2 Commit

Chunk 3 실패·Rollback

Checkpoint → Chunk 2 완료 위치

운영자 Restart

→ 같은 Job Instance

→ Chunk 3 시작 위치부터 처리

→ Item 멱등성 확인

→ 대사

→ SUCCESS·PARTIAL


\---

\# 부분 실패 흐름

\`\`\`text id="fq2ipn"
Item 1~50 성공

Item 51 형식 오류
→ Error Table
→ Skip

Item 52~100 성공

Chunk Commit

Job 종료

입력 100
성공 99
실패·Skip 1

Job Status = PARTIAL

운영 경고
실패 Item 재처리 대기

# Timeout·장애 흐름

## 온라인 Timeout

text id="fg4r46" Facade Transaction ↓ Slow SQL ↓ Query Timeout ↓ Exception ↓ Rollback ↓ Connection 반환 ↓ TCF Timeout 응답

## Batch DB 장애

text id="ew3pr1" Chunk 처리 ↓ DB 연결 단절 ↓ Retry ↓ 재시도 초과 ↓ Chunk Rollback ↓ Checkpoint 이전 위치 유지 ↓ Job FAIL ↓ 재시작 대기

## Scheduler 중복 실행

\`\`\`text id=“nq70yz” 인스턴스 A Lock 성공 → Job 실행

인스턴스 B Lock 0건 → Skip → 중복 실행 로그


\---

\# 정상 예시

\## 온라인 Transaction

\`\`\`java id="hh13px"
@Transactional(timeout = 5)
public CampaignApproveResponse approveCampaign(
CampaignApproveRequest request,
TransactionContext context) {

return campaignService.approveCampaign(
request,
context
);
}

## Batch Chunk 개념 코드

\`\`\`java id=“74vzc0” public BatchResult execute( BatchExecutionContext context) {

String checkpoint =
context.lastCheckpoint();

while (true) {

List<CustomerBatchItem> items =
reader.readNext(
checkpoint,
context.chunkSize()
);

if (items.isEmpty()) {
break;
}

List<CustomerCommand> commands =
processor.process(items);

chunkTransaction.executeWithoutResult(
status -> {
int written =
writer.write(commands);

if (written != commands.size()) {
throw new BatchIntegrityException(
"영향 건수가 일치하지 않습니다."
);
}

checkpointRepository.save(
context.executionId(),
items.get(items.size() - 1)
.customerNo()
);
}
);

checkpoint =
items.get(items.size() - 1)
.customerNo();
}

return reconciliationService.complete(
context.executionId()
);

}


\---

\# 금지 예시

\`\`\`text id="z77rf4"
Handler·Service·DAO 모든 계층에 @Transactional을 붙인다.

예외를 catch한 뒤 false를 반환한다.

Checked Exception Rollback 정책을 확인하지 않는다.

Facade가 오류 응답 DTO를 정상 반환한다.

DB Lock을 보유한 채 외부 API를 장시간 호출한다.

다중 DB 변경을 하나의 로컬 Transaction이라고 설명한다.

REQUIRES\_NEW를 이유 없이 사용한다.

Commit 전에 Cache를 변경한다.

AFTER\_COMMIT 실패를 복구 방법 없이 무시한다.

전체 Batch 대상을 List에 한 번에 적재한다.

온라인 요청에서 수십만 건을 반복 처리한다.

Job마다 @Scheduled를 붙이고 운영 등록을 분산한다.

다중 서버에서 JVM synchronized로 Job 중복을 막는다.

Chunk 없이 Job 전체를 한 Transaction으로 처리한다.

ORDER BY 없이 Checkpoint 기반 Reader를 구현한다.

Checkpoint를 Commit 전에 확정한다.

Checkpoint가 없는데 재시작 가능하다고 설명한다.

성공 Item이 재실행돼도 중복 반영되게 한다.

모든 오류를 Retry한다.

모든 오류를 Skip한다.

Skip 상한 없이 PARTIAL로 계속 진행한다.

Scheduler 실행 로그만 보고 Batch 성공으로 판단한다.

입력·출력 건수와 금액 대사를 생략한다.

PARTIAL 상태를 정상 성공으로 숨긴다.

실패 Item 원문에 개인정보를 그대로 저장한다.

재시작과 전체 재실행의 차이를 운영자에게 표시하지 않는다.

온라인 Peak와 대형 Batch를 같은 Pool에서 무제한 실행한다.

# 책임 경계와 RACI

| 활동 | 업무분석 | 업무개발 | AA | DA·DBA | FW·Batch | 운영 | 보안·감사 | QA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 업무 Transaction 정의 | A/R | R | A | C | C | I | C | C |
| Facade Transaction | I | R | A | C | C | I | I | C |
| Rollback 예외정책 | C | R | A | I | C | I | C | C |
| Job 정의 | A/R | R | C | C | C | A/C | C | C |
| Chunk 설계 | C | R | A | C | R/C | C | I | C |
| Checkpoint | C | R | A | C | R | C | I | C |
| 중복 실행 Lock | I | C | A | R/C | R | R/C | I | C |
| Retry·Skip | A/C | R | A | C | C | R/C | C | C |
| 부분 실패 | A/R | R | A | C | C | R | C | C |
| 대사 | A/R | R | C | R/C | C | R | C | A/C |
| Batch 시간창 | I | C | A | R/C | C | A/R | I | C |
| 자원 격리 | I | C | A | C | R | R | I | C |
| 재처리 Runbook | C | C | C | C | C | A/R | C | C |
| 감사·보존 | C | C | C | I | C | R/C | A/R | C |

# 데이터 및 상태관리

## Job 상태

\`\`\`text id=“7h6cvz” READY → RUNNING → SUCCESS

READY → RUNNING → PARTIAL

READY → RUNNING → FAIL → RESTARTING → SUCCESS


\## Step 상태

\`\`\`text id="gqoj0k"
STARTING
RUNNING
COMPLETED
FAILED
STOPPED

## Item 상태

text id="1h2kjv" READY SUCCESS FAILED SKIPPED RETRY\_READY REPROCESSED

## Checkpoint

Checkpoint는 업무 결과가 아니다.

\`\`\`text id=“e948zf” Checkpoint = 어디까지 Commit됐는가

업무 대사 = 처리결과가 정확한가



둘 다 필요하다.

\---

\# 성능·용량·확장성

\## Batch 처리량

\`\`\`text id="gdumam"
처리량
\=
Chunk당 건수
× 초당 Chunk 수

실제 성능은 다음 영향을 받는다.

\`\`\`text id=“8rnom0” Reader SQL

Processor CPU

Writer Batch 크기

Commit 시간

Index·Constraint

Redo·Undo

DB Pool

병렬도

외부 호출


\---

\## 병렬 처리

Partition 예:

\`\`\`text id="9xjx0t"
고객번호 구간

지점코드

일자 Partition

파일 Block

Hash Mod

확인:

\`\`\`text id=“h2z1ej” Partition 간 데이터 중복 없음

같은 업무 키 동시 변경 없음

DB Pool 용량

Thread 수

Lock 경합

결과 대사



병렬도를 높인다고 DB 처리량이 선형 증가하지 않는다.

\---

\## 온라인·Batch Pool

예:

\`\`\`text id="i3atqt"
온라인 Hikari Pool
→ 사용자 거래 전용

Batch Hikari Pool
→ Batch 동시성 상한

같은 DB 전체 Connection 수 한도 안에서 산정한다.

## Chunk 메모리

text id="ak0qeb" Chunk Size × Item 크기 × 중간 객체 수 × 병렬 Partition

Heap 사용량을 계산한다.

대형 CLOB·BLOB을 Chunk DTO에 전체 적재하지 않는다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| Job 실행권한 | 운영자·업무 관리자 |
| 수동 재처리 | 승인·사유 필수 |
| Parameter | 허용값 검증 |
| 파일 | 악성코드·Hash·경로 통제 |
| 개인정보 | 최소 Reader·마스킹·파기 |
| 오류 Item | 원문 최소화·암호화 검토 |
| 로그 | Token·비밀번호·개인정보 금지 |
| 감사 | 실행·중지·재시작·Skip·보상 |
| Job Lock | 관리자 임의 해제 감사 |
| 파기 Batch | 건수·대상·증적·이중 승인 |
| 보존 | Job·오류·대사별 기간 정의 |

수동 Batch 실행 API를 일반 사용자에게 노출하지 않는다.

# 운영·모니터링·장애 대응

## 권장 Metric

\`\`\`text id=“ws1ki2” batch.job.running.count

batch.job.success.count

batch.job.partial.count

batch.job.fail.count

batch.chunk.duration

batch.item.read.count

batch.item.write.count

batch.item.skip.count

batch.item.retry.count

batch.rollback.count

batch.checkpoint.lag

batch.reconciliation.difference

batch.lock.conflict.count


\---

\## 권장 로그

\`\`\`text id="vxrb2n"
event=BATCH\_JOB\_COMPLETED
jobId=BAT-CM-001
jobExecutionId=BE-20260718-0001
jobInstanceKey=BAT-CM-001:20260718:01
status=PARTIAL
readCount=100000
writeCount=99980
skipCount=20
retryCount=15
rollbackCount=2
lastCheckpoint=CUST-099999
durationMs=842351
instanceId=batch-01

## 장애 점검 순서

text id="sfw4vs" Job ID ↓ Job Execution 상태 ↓ 마지막 성공 Step ↓ 마지막 Checkpoint ↓ 실패 Chunk ↓ 오류 Item ↓ DB Lock·Pool ↓ 재시도·Skip 정책 ↓ 입출력 대사 ↓ 재시작·보상 판단

## 장기 RUNNING

text id="mxzkzn" RUNNING + Heartbeat 만료 ↓ 프로세스 상태 확인 ↓ DB Session·Lock 확인 ↓ 실제 작업 지속 여부 확인 ↓ UNKNOWN·FAILED 전환 승인 ↓ 재시작 결정

상태 Row만 보고 즉시 Lock을 해제하지 않는다.

# 자동검증 및 품질 Gate

## 1\. 온라인 Transaction Gate

\`\`\`text id=“7cqauw” @Transactional 위치 = Facade

조회 = readOnly

변경 = 영향 행 수

예외 숨김 없음

외부 호출 Lock 범위 검토

Timeout 정합


\---

\## 2. Rollback Gate

다음 테스트가 없으면 변경 거래 배포를 차단한다.

\`\`\`text id="0dfqg9"
Master·Detail 부분 실패

History 실패

Outbox 실패

Runtime Exception

Checked Exception

Timeout

Commit 실패

## 3\. Batch 등록 Gate

\`\`\`text id=“993x5b” Job ID

소유 업무

Parameter

중복 실행 정책

Chunk Size

Checkpoint

Retry·Skip

Timeout

대사

운영자


\---

\## 4. SQL Gate

\`\`\`text id="5jtvg4"
조건 없는 대량 UPDATE·DELETE 금지

ORDER BY 없는 Reader 금지

전체 List 적재 금지

영향 행 수 미검증 금지

Query Timeout 누락 금지

업무 키 없는 MERGE·UPSERT 검토

## 5\. 재시작 Gate

\`\`\`text id=“b2yrpg” Checkpoint 저장

안정된 처리 순서

Item 멱등성

동일 Job Instance 식별

재시작 테스트

전체 재실행과 구분


\---

\## 6. 부분 실패 Gate

\`\`\`text id="95zm58"
허용 오류 유형

Skip 상한

Error Table

PARTIAL 상태

재처리 절차

대사 완료

## 7\. 자원 Gate

\`\`\`text id=“snmgc0” Batch Thread 상한

DB Pool 상한

온라인 Peak 중첩 분석

Chunk Heap 계산

파일 Disk 용량

외부 호출 동시성


\---

\## 8. 운영 Gate

Architecture Gate에서도 배치 재시작·온라인 경합·입출력 대사와 계층별 Timeout을 필수 검증 대상으로 둔다.

\`\`\`text id="nchhqt"
OM Job 등록

실행이력

중복 Lock

알림

Runbook

재처리 승인

대사 증적

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| TXB-001 | 온라인 정상 변경 | Commit |
| TXB-002 | Master 후 Detail 실패 | 전체 Rollback |
| TXB-003 | History INSERT 실패 | 전체 Rollback |
| TXB-004 | Outbox INSERT 실패 | 전체 Rollback |
| TXB-005 | Runtime Exception | Rollback |
| TXB-006 | Checked Exception 기본 | 정책 확인 |
| TXB-007 | rollbackFor 적용 | Rollback |
| TXB-008 | 예외 변환 | 원인·Rollback 보존 |
| TXB-009 | 예외 catch 후 false 반환 | 품질 Gate 실패 |
| TXB-010 | Rollback-only 후 정상 반환 | 예외 확인 |
| TXB-011 | 조회 Transaction 변경 SQL | Gate 실패 |
| TXB-012 | 외부 호출 지연 | Lock·Timeout 확인 |
| TXB-013 | Transaction Timeout | Rollback·Pool 반환 |
| TXB-014 | REQUIRES\_NEW 감사 | Main Rollback 정합 확인 |
| TXB-015 | 다중 DB 변경 | 분산정책 확인 |
| TXB-016 | Commit 후 Cache Evict | Commit 이후 수행 |
| TXB-017 | Cache Evict 실패 | 복구정책 |
| TXB-018 | Job 정상 실행 | SUCCESS |
| TXB-019 | Scheduler Trigger | Job 실행과 구분 |
| TXB-020 | 동일 Job 동시 실행 | 한 인스턴스만 실행 |
| TXB-021 | Lock 만료 전 중복 | 차단 |
| TXB-022 | 비정상 종료 후 Lock | 운영 확인 |
| TXB-023 | Chunk 100 정상 | 100건 Commit |
| TXB-024 | Chunk 중간 Item 오류 | Chunk Rollback |
| TXB-025 | 이전 Chunk | Commit 유지 |
| TXB-026 | Checkpoint 저장 | 마지막 Commit 위치 |
| TXB-027 | Checkpoint 후 재시작 | 실패 위치부터 실행 |
| TXB-028 | Checkpoint 누락 | Gate 실패 |
| TXB-029 | 전체 List 적재 | 메모리 Gate 실패 |
| TXB-030 | ORDER BY 없는 Reader | Gate 실패 |
| TXB-031 | 같은 Item 재실행 | 중복 반영 없음 |
| TXB-032 | 일시 DB 오류 | 제한 Retry |
| TXB-033 | SQL 문법 오류 | 즉시 FAIL |
| TXB-034 | 형식 오류 Item | Skip·PARTIAL |
| TXB-035 | Skip 상한 이하 | PARTIAL |
| TXB-036 | Skip 상한 초과 | FAIL |
| TXB-037 | 일부 대상 수집 실패 | PARTIAL |
| TXB-038 | 모든 대상 실패 | FAIL |
| TXB-039 | 입력·성공·실패 대사 | 합계 일치 |
| TXB-040 | 금액 대사 | 차이 0 |
| TXB-041 | 대사 불일치 | FAIL·운영 경보 |
| TXB-042 | 실패 Item 재처리 | REPROCESSED |
| TXB-043 | 재처리 중복 | 멱등성 차단 |
| TXB-044 | 외부 A 성공·B 실패 | 보상상태 |
| TXB-045 | 보상 성공 | 정합성 복구 |
| TXB-046 | 보상 실패 | 수동조치 상태 |
| TXB-047 | Batch·온라인 동시 부하 | 온라인 p95 기준 |
| TXB-048 | Batch Pool 상한 | 온라인 Pool 보호 |
| TXB-049 | Job Timeout | 안전한 중단 |
| TXB-050 | 재시작 후 최종 대사 | SUCCESS·PARTIAL 확정 |

# 따라 하는 실무 절차

## 1단계. 업무 불변식을 정의한다

\`\`\`text id=“0pyw2a” 함께 성공해야 하는 Table

함께 Rollback할 데이터

외부 시스템

이력·감사·Outbox


\---

\## 2단계. 온라인과 Batch를 선택한다

\`\`\`text id="b2i245"
건수

예상시간

응답 필요

재시작

진행률

부분 실패

## 3단계. 트랜잭션 경계를 그린다

\`\`\`text id=“93cmz4” Transaction 시작

SQL 순서

외부 호출 위치

Commit

After Commit


\---

\## 4단계. Rollback 정책을 작성한다

\`\`\`text id="28go12"
업무 예외

시스템 예외

Checked Exception

Timeout

감사·Outbox 실패

## 5단계. Job을 정의한다

\`\`\`text id=“im2e0t” Job ID

Parameter

Job Instance Key

스케줄

소유자

완료 기준


\---

\## 6단계. Reader·Processor·Writer를 분리한다

\`\`\`text id="0s7nhq"
Reader
→ 안정된 순서·Checkpoint

Processor
→ 검증·변환

Writer
→ Bulk DML·영향 행 수

## 7단계. Chunk 크기를 측정한다

\`\`\`text id=“d8521f” 100

500

1,000

5,000



비교:

\`\`\`text id="ijc6qe"
처리량

Commit 시간

Heap

Lock

재시작 비용

## 8단계. Checkpoint와 멱등성을 구현한다

\`\`\`text id=“wlcet7” 마지막 처리 Key

Job Execution ID

업무 Unique

재실행 중복 방지


\---

\## 9단계. Retry·Skip·Fail Matrix를 작성한다

완료 증적:

\`\`\`text id="nk3ru7"
오류코드

재시도 여부

횟수

Skip 여부

최종 상태

## 10단계. 부분 실패와 대사를 구현한다

\`\`\`text id=“6nj0ky” Error Table

PARTIAL

입력·성공·실패·제외

금액

재처리


\---

\## 11단계. 중복 실행과 장애복구를 시험한다

\`\`\`text id="mckq0g"
다중 인스턴스

강제 종료

DB 장애

Checkpoint Restart

Lock 복구

## 12단계. 운영 증적을 작성한다

\`\`\`text id=“fnokkt” Job 실행이력

Step·Chunk

Checkpoint

오류 Item

대사

Metric

Runbook


\---

\# 완료 체크리스트

\## 트랜잭션

| 확인 항목 | 완료 |
|---|:---:|
| 업무 불변식을 정의했다. | □ |
| Facade에 트랜잭션이 있다. | □ |
| 조회·변경 트랜잭션을 구분했다. | □ |
| 외부 호출과 DB Lock 범위를 검토했다. | □ |
| 다중 DB 정책을 정의했다. | □ |
| Commit 이후 처리방식을 정의했다. | □ |
| Transaction Timeout이 있다. | □ |
| Query Timeout과 정합하다. | □ |

\## Rollback·예외

| 확인 항목 | 완료 |
|---|:---:|
| Runtime Exception Rollback을 확인했다. | □ |
| Checked Exception 정책이 있다. | □ |
| 예외 변환 후 원인을 보존한다. | □ |
| 예외를 catch하고 숨기지 않는다. | □ |
| 부분 Commit 테스트가 있다. | □ |
| Timeout Rollback을 검증했다. | □ |
| Connection 반환을 확인했다. | □ |
| Idempotency 상태를 확인했다. | □ |

\## Batch 설계

| 확인 항목 | 완료 |
|---|:---:|
| Job ID가 정의됐다. | □ |
| Job Instance Key가 정의됐다. | □ |
| Job Parameter가 정의됐다. | □ |
| Reader·Processor·Writer가 분리됐다. | □ |
| Chunk 크기가 정의됐다. | □ |
| Checkpoint가 있다. | □ |
| 안정된 처리순서가 있다. | □ |
| Item 멱등성이 있다. | □ |
| 중복 실행 Lock이 있다. | □ |
| Scheduler와 Job 성공을 구분한다. | □ |

\## 실패·재처리

| 확인 항목 | 완료 |
|---|:---:|
| Retryable 오류를 정의했다. | □ |
| Skippable 오류를 정의했다. | □ |
| Fatal 오류를 정의했다. | □ |
| Retry 횟수·간격이 있다. | □ |
| Skip 상한이 있다. | □ |
| PARTIAL 상태가 있다. | □ |
| 실패 Item을 저장한다. | □ |
| 재시작과 재실행을 구분한다. | □ |
| 실패 Item 재처리 절차가 있다. | □ |
| 보상 처리정책이 있다. | □ |

\## 대사·운영

| 확인 항목 | 완료 |
|---|:---:|
| 입력 건수를 기록한다. | □ |
| 성공·실패·Skip 건수를 기록한다. | □ |
| 금액 대사가 있다. | □ |
| 대사 차이를 경보한다. | □ |
| OM Job·History가 등록됐다. | □ |
| Job 실행·중지·재시작 권한이 있다. | □ |
| Batch 시간창을 정의했다. | □ |
| 온라인과 자원경합을 검증했다. | □ |
| 운영 Runbook이 있다. | □ |
| 다른 운영자가 재처리 절차를 재현했다. | □ |

\---

\# 변경·호환성·폐기 관리

\## Transaction 경계 변경

\`\`\`text id="mbgupg"
기존
Master + Detail

신규
Master + Detail + 외부 호출

영향:

\`\`\`text id=“q9c61q” Lock 시간

Timeout

Rollback 의미

외부 중복

보상

성능



ADR과 장애시험이 필요하다.

\---

\## 예외 계층 변경

Runtime Exception을 Checked Exception으로 변경하면 Rollback 정책이 바뀔 수 있다.

예외 타입 변경은 단순 리팩터링이 아니다.

\---

\## Chunk 크기 변경

\`\`\`text id="yc9gnn"
500
→ 5,000

영향:

\`\`\`text id=“tezj3h” 메모리

Lock

Commit 시간

Rollback 범위

재시작 비용

DB 부하



성능시험 후 적용한다.

\---

\## Checkpoint Key 변경

\`\`\`text id="5g8rv7"
고객번호
→ 등록일시 + 고객번호

기존 실패 Job의 재시작 호환성을 검토한다.

진행 중 Job을 신규 버전으로 재시작할 수 있는지 명시한다.

## Retry 정책 변경

Retry 횟수 증가:

\`\`\`text id=“p2vjhz” 장애 시 부하 증가

Job 시간 증가

외부 중복 위험



Retryable 오류 범위를 확대할 때 멱등성을 다시 검토한다.

\---

\## Job Parameter 변경

Job Instance 식별 Parameter를 변경하면 같은 업무일 Job이 신규 실행으로 판단될 수 있다.

기존 실행이력·중복 실행과 호환성을 검토한다.

\---

\## Job 폐기

\`\`\`text id="b48cl5"
신규 스케줄 중지
↓
진행 중 실행 완료·중단
↓
실패·PARTIAL 재처리
↓
대사 완료
↓
OM 비활성
↓
수동 실행 차단
↓
코드·SQL 제거
↓
이력·오류·파일 보존

Job 코드 삭제 전에 미완료 실행과 실패 Item을 정리한다.

# 시사점

## 핵심 아키텍처 판단

첫째, 트랜잭션 경계는 호출 계층이 아니라 업무 불변식을 기준으로 정해야 한다.

둘째, 예외가 발생했다는 사실보다 예외가 트랜잭션 경계 밖으로 어떻게 전달되는지가 Rollback을 결정한다.

text id="rvq1hg" 예외 발생 + catch 후 정상 반환 = Commit 위험

셋째, 외부 시스템과 여러 DB를 포함하는 변경을 로컬 DB 트랜잭션으로 해결하려 하지 않는다.

\`\`\`text id=“jaz7v9” Outbox

보상

재처리

대사



가 필요하다.

넷째, Batch는 전체 Job을 하나의 장시간 트랜잭션으로 처리해서는 안 된다.

\`\`\`text id="cv9ak7"
Chunk Commit
\+ Checkpoint
\+ Item 멱등성

이 재시작의 핵심이다.

다섯째, @Scheduled는 실행 시각을 제공할 뿐 Job의 중복 실행·재시작·대사·부분 실패를 해결하지 않는다.

여섯째, 부분 성공을 허용하려면 PARTIAL이라는 상태만 추가할 것이 아니라 실패 Item·재처리·대사·운영 책임까지 정의해야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 트랜잭션 과대 | Lock·Pool 장기 점유 |
| 트랜잭션 과소 | 부분 Commit |
| 예외 숨김 | 실패 데이터 Commit |
| Checked Exception 미검토 | 예상 밖 Commit |
| REQUIRES\_NEW 남용 | 상태 불일치 |
| DB Lock 중 외부 호출 | 장애 전파 |
| 다중 DB 원자성 오해 | 분산 불일치 |
| 전체 Batch 단일 Transaction | Rollback 비용·재시작 불가 |
| 전체 데이터 메모리 적재 | Heap 고갈 |
| Checkpoint 없음 | 처음부터 재실행 |
| Item 멱등성 없음 | 재시작 중복 |
| JVM Lock | 다중 서버 중복 실행 |
| 무제한 Retry | 장애 부하 증폭 |
| 무제한 Skip | 품질 저하 은폐 |
| PARTIAL 무시 | 실패 데이터 방치 |
| 대사 없음 | 결과 정확성 증명 불가 |
| 온라인·Batch 자원 공유 | 사용자 거래 지연 |
| 실패 원문 저장 | 개인정보 노출 |

## 우선 보완 과제

1.  Facade 중심 트랜잭션 위치를 Architecture Gate로 강제한다.
2.  Checked Exception과 예외 변환 Rollback 정책을 표준화한다.
3.  예외 catch 후 정상 반환 패턴을 정적검사로 차단한다.
4.  외부 호출을 포함한 DB Transaction을 특별 리뷰 대상으로 지정한다.
5.  Commit 이후 중요 후속 처리를 Outbox로 전환한다.
6.  tcf-batch에 운영형 Job Registry와 Launcher를 추가한다.
7.  OM Job 기준정보와 코드 Handler를 자동 대조한다.
8.  다중 인스턴스 Job Lock을 DB 기반으로 구현한다.
9.  대량 업무 Batch에 Chunk·Checkpoint 표준을 적용한다.
10.  Item 업무 키와 멱등성 기준을 Job별로 정의한다.
11.  Retry·Skip·Fail Matrix를 운영 기준정보로 관리한다.
12.  실패 Item 테이블과 재처리 상태를 구현한다.
13.  PARTIAL Job의 자동 경보와 미처리 목록을 제공한다.
14.  입력·출력 건수·금액 대사를 Batch 완료 Gate로 만든다.
15.  온라인·Batch Thread·DB Pool·시간창을 분리한다.

## 중장기 발전 방향

text id="s3jjfe" 개별 @Scheduled Job ↓ OM 기반 Job Registry ↓ 공통 BatchJobLauncher ↓ 분산 중복 실행 Lock ↓ Chunk·Checkpoint 표준 ↓ Retry·Skip·부분 실패 관리 ↓ 입출력 자동 대사 ↓ 재시작·재처리 자동화 ↓ 온라인·Batch 통합 운영 관측

# 마무리말

트랜잭션과 일괄 처리를 설계하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id=“irjzhl” 어떤 데이터가 반드시 함께 성공해야 하는가?

트랜잭션은 어디에서 시작하고 끝나는가?

예외가 변환돼도 Rollback되는가?

예외를 잡고 숨기는 코드는 없는가?

DB Lock을 보유한 채 외부 호출을 기다리는가?

여러 DB·외부 시스템 변경을 어떻게 복구하는가?

온라인으로 처리해야 하는가, Batch로 처리해야 하는가?

Job을 식별하는 Parameter는 무엇인가?

같은 Job이 동시에 두 번 실행되지 않는가?

한 번에 몇 건씩 Commit하는가?

어디까지 성공했는지 Checkpoint가 있는가?

재시작해도 같은 데이터가 중복 반영되지 않는가?

어떤 오류를 Retry하고 어떤 오류를 Skip하는가?

부분 실패를 허용한다면 실패 Item은 어디에 있는가?

입력과 출력 건수·금액이 정확히 일치하는가?

운영자는 어떤 절차로 재시작·재처리·보상하는가?



제20장의 핵심 흐름은 다음과 같다.

\`\`\`text id="trzjlj"
업무 불변식
↓
트랜잭션 경계
↓
Rollback 예외정책
↓
온라인·Batch 선택
↓
Job·Chunk·Checkpoint
↓
Retry·Skip·부분 실패
↓
재시작·재처리
↓
입출력 대사
↓
운영 완료 증명

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“d955hg” @Transactional이 있다고 데이터 정합성이 보장되는 것은 아니다.

@Scheduled가 실행됐다고 Batch가 성공한 것도 아니다.

명확한 트랜잭션 경계, Chunk와 Checkpoint, 멱등한 재시작, 부분 실패와 대사가 함께 있어야

장애가 발생해도 어디까지 처리됐는지 설명하고 안전하게 다시 시작할 수 있다. \`\`\`
