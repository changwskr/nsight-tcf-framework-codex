<!-- source: ztcf-집필본/NSIGHT TCF Chapter 19- Modifications, Deletions, and State Transitions.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제19장. 변경·삭제와 상태 전이

## 이 장을 시작하며

제18장에서는 새로운 업무 데이터를 생성하는 등록 거래를 구현했다.

등록 거래에서는 다음 항목을 중요하게 다루었다.

업무 키

서버 생성정보

DB Unique Constraint

Idempotency Key

Master·Detail Transaction

등록 결과 검증

이번 장에서는 이미 존재하는 데이터를 수정하거나 삭제하고, 업무 상태를 다음 단계로 전환하는 거래를 구현한다.

조회 거래는 현재 데이터를 읽는다.

등록 거래는 새로운 데이터를 만든다.

변경 거래는 기존 데이터의 의미를 바꾼다.

조회
→ 현재 상태를 확인한다.

등록
→ 새로운 상태를 만든다.

변경
→ 기존 상태를 다른 상태로 바꾼다.

삭제
→ 더 이상 일반 업무에서 사용하지 못하게 한다.

복구
→ 삭제·취소된 상태를 승인된 상태로 되돌린다.

초보 개발자는 변경 거래를 다음과 같이 구현하기 쉽다.

UPDATE CM\_CAMPAIGN
SET CAMPAIGN\_NAME = #{campaignName}
WHERE CAMPAIGN\_ID = #{campaignId}

그리고 Mapper가 1을 반환하면 성공으로 처리한다.

그러나 실제 운영 시스템에서는 다음 질문에 답해야 한다.

현재 데이터가 존재하는가?

사용자가 이 데이터를 수정할 권한이 있는가?

현재 상태에서 수정이 허용되는가?

다른 사용자가 먼저 수정하지 않았는가?

화면에서 조회한 이후 상태가 바뀌지 않았는가?

어떤 필드를 수정할 수 있는가?

변경 전 값과 변경 후 값을 감사할 수 있는가?

같은 변경 요청이 다시 들어오면 어떻게 하는가?

UPDATE 결과가 0건이면 무슨 의미인가?

삭제는 실제 Row를 제거하는가?

논리 삭제한 데이터는 조회에서 어떻게 제외하는가?

삭제한 데이터는 다시 복구할 수 있는가?

개인정보 파기와 일반 논리 삭제는 같은가?

관련 Detail·Cache·검색색인·외부 시스템은 어떻게 처리하는가?

변경 거래에서 가장 위험한 상황은 **Lost Update**, 즉 다른 사용자의 변경을 조용히 덮어쓰는 것이다.

사용자 A
→ Version 3 데이터 조회

사용자 B
→ Version 3 데이터 조회

사용자 A
→ 데이터 수정
→ Version 4

사용자 B
→ 이전 Version 3 화면으로 수정
→ A의 변경을 덮어씀

Version 조건이 없는 UPDATE에서는 사용자 B의 변경도 성공할 수 있다.

UPDATE CM\_CAMPAIGN
SET CAMPAIGN\_NAME = #{campaignName}
WHERE CAMPAIGN\_ID = #{campaignId}

안전한 변경은 다음 조건을 함께 확인한다.

업무 식별자

현재 상태

Version

삭제 여부

데이터권한

필요한 경우 기존 값

권장 SQL:

UPDATE CM\_CAMPAIGN
SET CAMPAIGN\_NAME = #{campaignName},
DESCRIPTION = #{description},
UPDATED\_BY = #{updatedBy},
UPDATED\_AT = #{updatedAt},
VERSION\_NO = VERSION\_NO + 1
WHERE CAMPAIGN\_ID = #{campaignId}
AND STATUS\_CODE = #{expectedStatus}
AND VERSION\_NO = #{expectedVersion}
AND DELETE\_YN = 'N'

updated = 0은 단순한 실패가 아니다.

데이터가 존재하지 않는다.

이미 삭제됐다.

다른 사용자가 먼저 수정했다.

상태가 바뀌었다.

권한 범위가 달라졌다.

입력한 Version이 잘못됐다.

Service는 이 원인을 정책에 따라 구분해야 한다.

원본 제19장도 수정과 삭제 시 현재 상태와 Version을 조건으로 사용하고, 영향 행 0건을 성공으로 처리하지 않도록 정의한다.

## 핵심 관점

변경 거래는
새로운 값을 저장하는 기능이 아니다.

현재 상태와 Version을 기준으로
이 사용자가 지금 이 변경을 수행할 수 있는지 판단하고,

변경 전후의 의미와 책임을
데이터·이력·감사로그에 남기는
통제된 상태 전이 과정이다.

## 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 단순 필드 수정과 상태 전이의 차이를 설명한다. |
| 2 | 수정 가능한 상태를 업무 규칙으로 정의한다. |
| 3 | 상태 전이표와 상태 다이어그램을 작성한다. |
| 4 | 현재 상태와 목표 상태를 구분한다. |
| 5 | 변경 권한과 상태별 권한을 구분한다. |
| 6 | Request의 Version을 이용해 낙관적 잠금을 구현한다. |
| 7 | Lost Update가 발생하는 원인을 설명한다. |
| 8 | UPDATE SQL에 ID·상태·Version·삭제 여부를 조건으로 적용한다. |
| 9 | UPDATE 영향 행 수 0건의 원인을 구분한다. |
| 10 | 수정과 상태 변경 ServiceId를 분리할지 판단한다. |
| 11 | 변경 거래에 Idempotency를 적용한다. |
| 12 | 동일 요청 재전송과 동시 수정 충돌을 구분한다. |
| 13 | 논리 삭제와 물리 삭제의 차이를 설명한다. |
| 14 | 일반 삭제와 개인정보 파기를 구분한다. |
| 15 | 논리 삭제된 데이터를 모든 조회에서 제외한다. |
| 16 | 삭제 데이터의 Unique Key 재사용 정책을 결정한다. |
| 17 | 삭제 복구 가능 여부와 복구 상태를 정의한다. |
| 18 | 변경 전후 이력을 저장하는 방법을 설명한다. |
| 19 | 거래로그·감사로그·업무이력의 차이를 설명한다. |
| 20 | 중요 변경정보를 마스킹하여 감사한다. |
| 21 | Master·Detail 변경을 하나의 트랜잭션으로 처리한다. |
| 22 | Cache·검색색인·외부 연계 후속 처리를 설계한다. |
| 23 | 동시 수정·부분 실패·Rollback 테스트를 작성한다. |
| 24 | 변경·삭제·복구 계약의 호환성과 폐기를 판단한다. |

# 한눈에 보는 변경 거래 흐름

┌────────────────────────────────────────────────────────────┐
│ 1. 화면 │
│ 변경값 + 현재 상태 + Version + Idempotency Key │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. STF │
│ 인증·기능권한·거래통제·Idempotency │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. Handler │
│ Request DTO 변환·형식 검증 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. Facade │
│ 변경 트랜잭션 시작 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. Service │
│ 현재 데이터 조회 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. Rule │
│ 존재·권한·현재 상태·허용 전이 판단 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 7. UPDATE │
│ ID + 상태 + Version + 삭제 여부 조건 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 8. 영향 행 수 검증 │
│ 1건 성공·0건 원인판단·2건 이상 정합성 오류 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 9. 이력·감사·Outbox │
│ 변경 전후 값·행위·후속 이벤트 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 10. Commit │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 11. Response │
│ 신규 상태 + 신규 Version + 변경시각 │
└────────────────────────────────────────────────────────────┘

# 대표 변경 거래

제18장에서 등록한 캠페인을 예로 사용한다.

| 구분 | 값 |
| --- | --- |
| 업무 | CM |
| 수정 ServiceId | CM.Campaign.update |
| 승인요청 ServiceId | CM.Campaign.requestApproval |
| 승인 ServiceId | CM.Campaign.approve |
| 취소 ServiceId | CM.Campaign.cancel |
| 삭제 ServiceId | CM.Campaign.delete |
| 복구 ServiceId | CM.Campaign.restore |
| 처리유형 | COMMAND |
| Idempotency | 필수 |
| 감사 | 필수 |
| Transaction | 변경 Transaction |
| Version | 필수 |

# 캠페인 상태 모델

DRAFT
├─ 수정 → DRAFT
├─ 승인요청 → REQUESTED
└─ 삭제 → DELETED

REQUESTED
├─ 승인 → APPROVED
├─ 반려 → REJECTED
└─ 요청취소 → DRAFT

REJECTED
├─ 수정 → DRAFT
└─ 삭제 → DELETED

APPROVED
├─ 실행시작 → RUNNING
└─ 승인취소 → DRAFT 또는 CANCELLED

RUNNING
├─ 완료 → COMPLETED
└─ 중도취소 → CANCELLED

COMPLETED
└─ 일반 수정 금지

CANCELLED
└─ 재개 정책에 따라 별도 거래

DELETED
└─ 복구 가능 기간 내 RESTORE

모든 상태에서 모든 변경을 허용하지 않는다.

# 현재 구현과 목표 구조

현재 기준 자료에서는 상태 전이·Version 조건·논리 삭제·감사정보가 변경 거래의 설계 기준으로 제시돼 있다.

다만 실제 업무 소스에서 모든 도메인에 동일하게 구현됐는지는 거래별 확인이 필요하다.

## 구현·설계 상태 구분

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| Handler→Facade→Service→Rule→DAO→Mapper | 구현 기준 확인 | 변경 거래도 동일 계층 적용 |
| Facade 트랜잭션 | 구현 기준 확인 | 변경 유스케이스 경계 |
| 영향 행 수 Service 판정 | 설계 기준 확인 | 필수 적용 |
| 상태 전이 Rule | 설계 기준 | 도메인별 구현 필요 |
| Version 조건 UPDATE | 설계 기준 | 업무 테이블별 확인 필요 |
| 논리 삭제 | 설계 기준 | 조회 SQL 전체 영향검토 필요 |
| Idempotency 공통 처리 | 부분 구현 | 운영 공유 저장소 보완 필요 |
| 변경 전후 이력 | 프로젝트 확인 필요 | 도메인별 이력테이블 확인 |
| 감사로그 | 공통 기준 확인 | 중요 거래 등록 필요 |
| Cache Evict | 프로젝트 확인 필요 | 변경 데이터 Cache 여부 확인 |
| Outbox·이벤트 | 권장 확장 | 외부 부수효과가 있을 때 적용 |
| 복구 ServiceId | 프로젝트 확인 필요 | 삭제정책에 따라 별도 설계 |
| 물리 파기 Batch | 프로젝트 확인 필요 | 보존·개인정보 정책 연계 |

상태 전이와 낙관적 잠금의 대표 SQL 및 영향 행 0건 처리 기준은 프로젝트 자료에 명시돼 있다.

# 변경 요청 계약

## 수정 요청

{
"header": {
"businessCode": "CM",
"serviceId": "CM.Campaign.update",
"transactionCode": "CM-CMD-0002",
"processingType": "COMMAND",
"idempotencyKey": "IDEMP-CM-UPD-20260718-001"
},
"body": {
"campaignId": "CMP-20260718-000001",
"expectedStatus": "DRAFT",
"expectedVersion": 3,
"campaignName": "2026 하반기 우수고객 캠페인 변경",
"startDate": "2026-08-05",
"endDate": "2026-08-31",
"description": "변경된 캠페인 설명"
}
}

## 수정 응답

{
"result": {
"resultStatus": "SUCCESS",
"resultCode": "0000"
},
"body": {
"campaignId": "CMP-20260718-000001",
"campaignStatus": "DRAFT",
"versionNo": 4,
"updatedAt": "2026-07-18T14:20:00+09:00"
}
}

## 상태 전이 요청

{
"header": {
"serviceId": "CM.Campaign.approve",
"transactionCode": "CM-APR-0001",
"idempotencyKey": "IDEMP-CM-APR-20260718-001"
},
"body": {
"campaignId": "CMP-20260718-000001",
"expectedStatus": "REQUESTED",
"expectedVersion": 4,
"approvalComment": "승인합니다."
}
}

# 19.1 수정 가능 상태

## 19.1.1 수정은 현재 상태에 따라 달라진다

같은 데이터라도 상태에 따라 허용되는 변경이 달라진다.

| 상태 | 일반정보 수정 | 대상 변경 | 승인 | 삭제 |
| --- | --- | --- | --- | --- |
| DRAFT | O | O | X | O |
| REQUESTED | X | X | O | X |
| REJECTED | O | O | X | O |
| APPROVED | 제한 | 제한 | X | X |
| RUNNING | 원칙적 X | X | X | X |
| COMPLETED | X | X | X | X |
| CANCELLED | 제한 | X | X | 정책 |
| DELETED | X | X | X | 복구만 |

수정 가능 여부를 화면 버튼 표시만으로 통제하지 않는다.

UI
→ 수정 버튼 비활성화

서버
→ 상태 Rule로 다시 검증

## 19.1.2 상태 전이표

| 현재 상태 | 행위 | 목표 상태 | 필요 권한 | 추가 조건 |
| --- | --- | --- | --- | --- |
| DRAFT | 승인요청 | REQUESTED | 작성자 | 필수항목 완료 |
| REQUESTED | 승인 | APPROVED | 승인자 | 작성자와 분리 |
| REQUESTED | 반려 | REJECTED | 승인자 | 반려사유 필수 |
| REQUESTED | 요청취소 | DRAFT | 작성자 | 승인 전 |
| APPROVED | 실행 | RUNNING | 운영자 | 시작일 도래 |
| RUNNING | 완료 | COMPLETED | 시스템·운영자 | 실행결과 정상 |
| RUNNING | 취소 | CANCELLED | 관리자 | 사유 필수 |
| DRAFT | 삭제 | DELETED | 작성자·관리자 | 참조 없음 |
| DELETED | 복구 | DRAFT | 관리자 | 보존기간 내 |

## 19.1.3 상태 전이의 책임

Handler
→ 행위별 ServiceId 분기

Facade
→ 전이 유스케이스 Transaction

Service
→ 현재 데이터 조회·처리 순서

Rule
→ 현재 상태에서 행위 허용 여부

DAO·Mapper
→ 상태·Version 조건 UPDATE

ETF
→ 성공·업무 오류·시스템 오류 표준화

Mapper가 상태 전이 규칙 전체를 결정하지 않는다.

## 19.1.4 Rule 구현

@Component
public class CampaignStateRule {

private static final Map<CampaignStatus,
Set<CampaignAction>> ALLOWED\_ACTIONS =
Map.of(
CampaignStatus.DRAFT,
Set.of(
CampaignAction.UPDATE,
CampaignAction.REQUEST\_APPROVAL,
CampaignAction.DELETE
),
CampaignStatus.REQUESTED,
Set.of(
CampaignAction.APPROVE,
CampaignAction.REJECT,
CampaignAction.CANCEL\_REQUEST
),
CampaignStatus.REJECTED,
Set.of(
CampaignAction.UPDATE,
CampaignAction.DELETE
),
CampaignStatus.APPROVED,
Set.of(
CampaignAction.START,
CampaignAction.CANCEL\_APPROVAL
),
CampaignStatus.RUNNING,
Set.of(
CampaignAction.COMPLETE,
CampaignAction.CANCEL
),
CampaignStatus.DELETED,
Set.of(
CampaignAction.RESTORE
)
);

public void validateAction(
CampaignStatus currentStatus,
CampaignAction action) {

Set<CampaignAction> allowed =
ALLOWED\_ACTIONS.getOrDefault(
currentStatus,
Set.of()
);

if (!allowed.contains(action)) {
throw new BusinessException(
"E-CM-STATE-0001",
"현재 상태에서는 요청한 처리를 할 수 없습니다."
);
}
}
}

## 19.1.5 행위별 권한

단순 수정권한 하나만으로 모든 상태 전이를 허용하지 않는다.

CM\_CAMPAIGN\_UPDATE

CM\_CAMPAIGN\_REQUEST\_APPROVAL

CM\_CAMPAIGN\_APPROVE

CM\_CAMPAIGN\_REJECT

CM\_CAMPAIGN\_CANCEL

CM\_CAMPAIGN\_DELETE

CM\_CAMPAIGN\_RESTORE

승인 거래에서는 작성자와 승인자를 분리할 수 있다.

if (campaign.createdBy()
.equals(auth.getUserId())) {

throw new BusinessException(
"E-CM-AUTH-0002",
"본인이 작성한 캠페인은 직접 승인할 수 없습니다."
);
}

## 19.1.6 수정 가능 필드

상태뿐 아니라 필드별 수정 가능 여부를 정의한다.

| 필드 | DRAFT | REQUESTED | APPROVED | RUNNING |
| --- | --- | --- | --- | --- |
| 캠페인명 | O | X | 제한 | X |
| 설명 | O | X | 제한 | X |
| 시작일 | O | X | 조건부 | X |
| 종료일 | O | X | 조건부 | 제한 |
| 대상등급 | O | X | X | X |
| 실행상태 | 직접 변경 금지 | 직접 변경 금지 | 직접 변경 금지 | 전이 거래만 |

하나의 범용 updateCampaign 거래가 모든 필드를 임의 변경하게 하지 않는다.

## 19.1.7 일반 수정과 상태 전이 ServiceId 분리

### 분리 권장

CM.Campaign.update
→ 일반정보 수정

CM.Campaign.requestApproval
→ DRAFT → REQUESTED

CM.Campaign.approve
→ REQUESTED → APPROVED

CM.Campaign.cancel
→ 승인·실행 취소

장점:

권한 분리

오류코드 분리

감사 행위 명확

Timeout·중요도 별도 관리

화면 이벤트 추적 용이

## 19.1.8 범용 상태 변경 거래의 위험

금지에 가까운 구조:

{
"campaignId": "...",
"status": "APPROVED"
}

클라이언트가 목표 상태를 임의 지정한다.

권장:

ServiceId가 행위를 표현한다.

목표 상태는 서버가 결정한다.

CampaignStatus targetStatus =
CampaignStatus.APPROVED;

Request에서 targetStatus를 받지 않는 것이 기본이다.

## 19.1.9 현재 데이터 조회

변경 전에 현재 데이터를 조회할 수 있다.

CampaignCurrentResult current =
campaignDao.findCurrent(
request.campaignId())
.orElseThrow(
CampaignNotFoundException::new
);

확인:

존재

삭제 여부

현재 상태

현재 Version

업무 소유자

데이터권한

참조 상태

## 19.1.10 조회 후 UPDATE의 Race Condition

현재 데이터 조회
→ 상태 DRAFT 확인

다른 사용자 승인요청
→ 상태 REQUESTED

첫 요청 UPDATE

조회 시점과 UPDATE 시점 사이에 데이터가 바뀔 수 있다.

따라서 Rule에서 조회한 상태를 확인하는 것만으로 부족하다.

UPDATE WHERE에 상태와 Version을 다시 포함한다.

Service Rule 검증
+
DB 조건 검증

## 19.1.11 상태 전이 SQL

<update
id="approveCampaign"
parameterType="CampaignStateChangeCommand"
timeout="2">

UPDATE CM\_CAMPAIGN
SET STATUS\_CODE = 'APPROVED',
APPROVED\_BY = #{updatedBy},
APPROVED\_AT = #{updatedAt},
UPDATED\_BY = #{updatedBy},
UPDATED\_AT = #{updatedAt},
VERSION\_NO = VERSION\_NO + 1
WHERE CAMPAIGN\_ID = #{campaignId}
AND STATUS\_CODE = 'REQUESTED'
AND VERSION\_NO = #{expectedVersion}
AND DELETE\_YN = 'N'

</update>

목표 상태를 Parameter로 받기보다 승인 거래의 SQL에서 명확히 표현할 수 있다.

## 19.1.12 상태 변경 영향 행 수

int updated =
campaignDao.approveCampaign(command);

if (updated == 1) {
return;
}

if (updated > 1) {
throw new DataIntegrityException(
"하나의 캠페인 승인에서 여러 건이 변경되었습니다."
);
}

throw resolveUpdateFailure(command);

## 19.1.13 상태 전이와 부수효과

승인 후 다음 작업이 필요할 수 있다.

승인 알림

실행 Scheduler 등록

Cache Evict

검색색인 변경

외부 시스템 전송

Outbox Event

부수효과가 업무 Transaction과 반드시 함께 성공해야 하는지 판단한다.

권장:

상태 UPDATE

\+ Outbox INSERT

→ 동일 Transaction Commit

→ 별도 Publisher가 후속 처리

## 19.1.14 상태 전이의 멱등성

같은 승인 요청이 재전송됐다.

현재 상태가 이미 APPROVED다.

다음 두 경우를 구분한다.

같은 Idempotency Key
→ 기존 성공 응답 반환

다른 Idempotency Key
→ 이미 승인된 상태 오류 또는 멱등 성공 정책

행위 자체를 상태 기준으로 멱등하게 설계할 수 있다.

APPROVED 상태에서 approve 재요청
→ “이미 승인되었습니다.”

또는
→ 기존 승인 결과를 SUCCESS로 반환

프로젝트 전체에서 일관된 정책을 사용한다.

# 19.2 낙관적 잠금과 동시 수정

## 19.2.1 낙관적 잠금이 필요한 이유

낙관적 잠금은 데이터를 읽은 후 변경할 때 다른 사용자가 먼저 변경했는지 Version으로 확인하는 방식이다.

조회
→ VERSION\_NO = 3

수정 요청
→ expectedVersion = 3

UPDATE 성공
→ VERSION\_NO = 4

다른 요청이 이미 Version을 4로 변경했다면 Version 3 조건의 UPDATE는 0건이다.

## 19.2.2 Lost Update

시각 T1
사용자 A·B가 Version 7 조회

시각 T2
사용자 A가 이름 변경
Version 8

시각 T3
사용자 B가 설명 변경
Version 조건 없음

결과
사용자 A의 일부 변경이 B의 오래된 화면값으로 덮어써짐

낙관적 잠금은 이 문제를 차단한다.

## 19.2.3 Request Version

public record CampaignUpdateRequest(

@NotBlank
String campaignId,

@NotNull
CampaignStatus expectedStatus,

@NotNull
@Positive
Long expectedVersion,

@NotBlank
@Size(max = 200)
String campaignName,

@NotNull
LocalDate startDate,

@NotNull
LocalDate endDate,

@Size(max = 2000)
String description
) {}

Version을 클라이언트가 임의 생성하는 값으로 보지는 않는다.

이 값은 서버가 이전 조회 응답에 제공한 현재 Version이다.

## 19.2.4 조회 응답 Version

{
"campaignId": "CMP-20260718-000001",
"campaignStatus": "DRAFT",
"versionNo": 3
}

화면은 수정 요청에 이 Version을 다시 보낸다.

## 19.2.5 변경 Command

public record CampaignUpdateCommand(
String campaignId,
CampaignStatus expectedStatus,
long expectedVersion,
String campaignName,
LocalDate startDate,
LocalDate endDate,
String description,
String updatedBy,
String updatedBranchCode,
LocalDateTime updatedAt
) {}

## 19.2.6 낙관적 잠금 SQL

UPDATE CM\_CAMPAIGN
SET CAMPAIGN\_NAME = #{campaignName},
START\_DATE = #{startDate},
END\_DATE = #{endDate},
DESCRIPTION = #{description},
UPDATED\_BY = #{updatedBy},
UPDATED\_BRANCH\_CODE = #{updatedBranchCode},
UPDATED\_AT = #{updatedAt},
VERSION\_NO = VERSION\_NO + 1
WHERE CAMPAIGN\_ID = #{campaignId}
AND STATUS\_CODE = #{expectedStatus}
AND VERSION\_NO = #{expectedVersion}
AND DELETE\_YN = 'N'

## 19.2.7 영향 행 0건의 원인

UPDATE 결과 0
↓
데이터 미존재

이미 삭제됨

상태가 변경됨

Version 불일치

데이터권한 조건 불일치

업무 키 조건 불일치

0건을 무조건 NOT\_FOUND로 처리하면 실제 동시 수정이 숨겨진다.

## 19.2.8 원인 구분 방식

### 방식 1: 재조회

private RuntimeException resolveUpdateFailure(
CampaignUpdateCommand command,
AuthenticationContext auth) {

CampaignCurrentResult current =
campaignDao.findCurrentIncludingDeleted(
command.campaignId())
.orElseThrow(
CampaignNotFoundException::new
);

if (current.deleted()) {
return new CampaignDeletedException();
}

if (!campaignRule.canAccess(
current,
auth)) {
return new AuthorizationException(
"변경 권한이 없습니다."
);
}

if (current.versionNo()
!= command.expectedVersion()) {
return new ConcurrentModificationException(
"다른 사용자가 먼저 변경했습니다. 다시 조회해 주세요."
);
}

if (current.status()
!= command.expectedStatus()) {
return new InvalidStateTransitionException(
"처리 상태가 변경되었습니다. 다시 조회해 주세요."
);
}

return new DataIntegrityException(
"변경 결과를 확인할 수 없습니다."
);
}

### 방식 2: 보안상 동일 메시지

존재 여부를 숨겨야 하는 데이터는 다음처럼 동일한 응답을 사용할 수 있다.

대상이 없거나 변경할 수 없습니다.

운영 로그에서는 내부 원인을 분리한다.

## 19.2.9 0건 재조회 시 Race Condition

영향 행 0건 후 재조회 결과도 다시 바뀔 수 있다.

재조회는 사용자 메시지를 더 정확하게 만들기 위한 보조 수단이다.

최종 무결성은 UPDATE 조건과 트랜잭션 결과에 의해 보장된다.

## 19.2.10 영향 행 2건 이상

PK 조건 UPDATE에서 2건 이상이 변경됐다면 심각한 정합성 오류다.

PK·Unique Constraint 문제

잘못된 WHERE 조건

테이블·View 구조 문제

Mapper Parameter 오류

절대 성공으로 처리하지 않는다.

## 19.2.11 Version 증가 시점

등록
→ Version 1

일반 수정
→ Version 2

승인요청
→ Version 3

승인
→ Version 4

삭제
→ Version 5

복구
→ Version 6

업무적으로 의미 있는 상태·내용 변경마다 Version을 증가시키는 것이 기본이다.

단순 조회시각이나 통계 캐시 갱신으로 업무 Version을 증가시키지 않는다.

## 19.2.12 부분 필드 수정

PATCH 스타일 수정에서 필드 미전송과 null을 구분해야 한다.

미전송
→ 기존 값 유지

null
→ 값 제거

빈 문자열
→ 빈 값으로 변경

MyBatis 동적 UPDATE:

<update id="updateCampaignPartial">
UPDATE CM\_CAMPAIGN
<set>
<if test="campaignNamePresent">
CAMPAIGN\_NAME = #{campaignName},
</if>

<if test="descriptionPresent">
DESCRIPTION = #{description},
</if>

UPDATED\_BY = #{updatedBy},
UPDATED\_AT = #{updatedAt},
VERSION\_NO = VERSION\_NO + 1
</set>
WHERE CAMPAIGN\_ID = #{campaignId}
AND VERSION\_NO = #{expectedVersion}
AND STATUS\_CODE = 'DRAFT'
AND DELETE\_YN = 'N'
</update>

그러나 과도한 범용 PATCH는 필드별 권한과 상태 규칙을 복잡하게 한다.

## 19.2.13 전체 교체와 부분 수정

| 방식 | 특징 | 주의 |
| --- | --- | --- |
| 전체 교체 | 모든 수정 필드 전송 | 오래된 값 덮어쓰기 |
| 부분 수정 | 변경 필드만 전송 | null·미전송 구분 |
| 행위별 Command | 업무 목적별 필드 | ServiceId 증가 |
| Domain Method | 상태와 불변식 보호 | 도메인 모델 필요 |

복잡한 상태 업무에서는 행위별 Command가 더 안전할 수 있다.

## 19.2.14 낙관적 잠금과 Idempotency의 차이

| 구분 | 낙관적 잠금 | Idempotency |
| --- | --- | --- |
| 질문 | 다른 변경이 먼저 있었는가 | 같은 요청이 다시 왔는가 |
| 기준 | Version | Idempotency Key |
| 대상 | 동시 사용자 수정 | 중복 전송 |
| 처리 | 충돌 오류 | 기존 결과 반환·처리 중 차단 |
| DB 조건 | VERSION\_NO | Idempotency Store Unique |
| 둘 다 필요 | O | O |

## 19.2.15 비관적 잠금

SELECT ...
FROM CM\_CAMPAIGN
WHERE CAMPAIGN\_ID = #{campaignId}
FOR UPDATE

사용 가능 상황:

짧은 거래

충돌이 매우 잦음

재고·순번·한도 등 즉시 직렬화 필요

Lock 순서가 명확함

위험:

Lock 대기

Deadlock

DB Connection 장기 점유

외부 호출 지연 확산

Timeout

일반 화면 수정에는 낙관적 잠금을 우선 검토한다.

## 19.2.16 변경 Transaction

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

변경 Transaction 안에서 다음을 수행한다.

현재 상태 확인

Rule 검증

Master UPDATE

Detail 변경

이력 INSERT

Outbox INSERT

영향 행 수 확인

## 19.2.17 Detail 동기화 전략

대상 등급을 변경한다고 가정한다.

대안 1:

기존 Detail 전체 삭제
→ 신규 전체 등록

단순하지만 이력·영향 행 수·대량 Detail 비용을 검토해야 한다.

대안 2:

기존과 신규 비교
→ 추가
→ 삭제
→ 유지

장점:

실제 변경만 수행

상세 이력 명확

DML 감소 가능

복잡도는 증가한다.

## 19.2.18 변경 실패와 Rollback

Master UPDATE 성공
↓
Detail UPDATE 실패
↓
History INSERT 전 오류
↓
전체 Rollback

Master만 변경되고 Detail은 이전 상태로 남아서는 안 된다.

# 19.3 논리 삭제와 물리 삭제

## 19.3.1 삭제의 의미

삭제는 단순히 Row를 제거하는 SQL이 아니다.

업무 사용 중지

화면 비노출

일반 조회 제외

변경 금지

연계 중단

복구 가능성

보관기간

최종 파기

를 함께 결정하는 데이터 생명주기 정책이다.

## 19.3.2 논리 삭제

Row는 유지하고 삭제 상태를 기록한다.

UPDATE CM\_CAMPAIGN
SET DELETE\_YN = 'Y',
STATUS\_CODE = 'DELETED',
DELETED\_BY = #{deletedBy},
DELETED\_AT = #{deletedAt},
DELETE\_REASON = #{deleteReason},
UPDATED\_BY = #{deletedBy},
UPDATED\_AT = #{deletedAt},
VERSION\_NO = VERSION\_NO + 1
WHERE CAMPAIGN\_ID = #{campaignId}
AND STATUS\_CODE IN ('DRAFT', 'REJECTED')
AND VERSION\_NO = #{expectedVersion}
AND DELETE\_YN = 'N'

장점:

복구 가능

감사 가능

참조관계 유지

운영 원인 분석 가능

주의:

모든 조회 SQL 필터 필요

Unique Key 재사용 문제

데이터량 증가

개인정보 보관 지속

삭제된 데이터 오노출 위험

논리 삭제에서는 조회조건·유일 제약·복구·보존·개인정보 파기정책을 함께 검토해야 한다.

## 19.3.3 물리 삭제

DELETE
FROM CM\_CAMPAIGN
WHERE CAMPAIGN\_ID = #{campaignId}
AND VERSION\_NO = #{expectedVersion}

적합할 수 있는 경우:

임시 데이터

법적 보관 의무 없음

다른 데이터 참조 없음

복구 요구 없음

감사 이력 별도 보존

개인정보 파기 완료

위험:

복구 불가

참조 무결성 오류

사후 원인 분석 어려움

감사 증거 손실

Batch·보고서 영향

## 19.3.4 일반 삭제와 개인정보 파기

논리 삭제
\= 일반 업무에서 보이지 않게 함

개인정보 파기
\= 복구 불가능하게 삭제·익명화함

논리 삭제만으로 개인정보 파기 요구를 충족했다고 판단하지 않는다.

파기 대안:

민감 컬럼 null 처리

비가역 Hash

Tokenization 폐기

별도 보관영역 이동

암호화키 폐기

물리 삭제

법률·내부규정·보존정책에 따라 결정한다.

## 19.3.5 삭제 가능 상태

| 상태 | 삭제 |
| --- | --- |
| DRAFT | O |
| REJECTED | O |
| REQUESTED | 요청취소 후 가능 |
| APPROVED | 원칙적 금지 |
| RUNNING | 금지 |
| COMPLETED | 보존정책 적용 |
| CANCELLED | 정책에 따라 가능 |
| DELETED | 중복 삭제 |

상태 확인 없이 모든 캠페인을 삭제할 수 없게 한다.

## 19.3.6 참조 데이터 확인

삭제 전 다음을 확인할 수 있다.

실행 이력

대상 고객 생성

승인 이력

파일 결과

외부 발송

통계 사용

다른 캠페인 참조

참조가 존재하면 다음 대안을 검토한다.

삭제 금지

취소 상태 전환

종료 처리

참조 데이터도 함께 비활성화

보관상태 전환

## 19.3.7 Cascade Delete 주의

DB ON DELETE CASCADE는 편리하지만 중요한 업무 데이터에서 대규모 Detail이 의도치 않게 삭제될 수 있다.

확인:

삭제 범위

감사 가능성

영향 행 수

복구 가능성

Batch·보고서 영향

업무 삭제는 Service에서 명확한 순서와 예상 건수를 검증하는 것이 좋다.

## 19.3.8 논리 삭제 조회조건

일반 조회:

WHERE DELETE\_YN = 'N'

누락되기 쉬운 곳:

단건 조회

목록 조회

Count SQL

중복 조회

권한 조회

배치

다운로드

Cache Loader

외부 연계

View

Count와 List 모두 같은 삭제조건을 사용해야 한다.

## 19.3.9 관리자 포함 조회

일반 사용자:

DELETE\_YN = N

관리자 복구 화면:

DELETE\_YN = Y

같은 Mapper에서 임의 Flag로 전체 조회를 허용하기보다 별도 ServiceId·권한·감사를 적용한다.

CM.Campaign.selectDeletedList

CM.Campaign.restore

## 19.3.10 Unique Key와 논리 삭제

업무 키가 CAMPAIGN\_CODE이고 삭제 Row가 남아 있다면 같은 Code 재등록이 Unique Constraint에 걸린다.

대안:

### 재사용 금지

삭제돼도 캠페인코드는 영구 재사용 금지

가장 단순하고 추적성이 높다.

### Version 포함

CAMPAIGN\_CODE
\+ CAMPAIGN\_VERSION

### 활성 식별값 분리

DB 제품 기능과 데이터 모델을 검토해 활성 Row만 유일하도록 설계한다.

### 삭제 시 업무 키 변경

CAMPAIGN\_CODE
→ CAMPAIGN\_CODE || ':DEL:' || CAMPAIGN\_ID

원본 업무 키가 바뀌어 감사·연계에 문제가 생길 수 있어 신중해야 한다.

## 19.3.11 복구

복구 요청:

{
"campaignId": "CMP-20260718-000001",
"expectedVersion": 5,
"restoreReason": "업무 담당자 오삭제"
}

복구 SQL:

UPDATE CM\_CAMPAIGN
SET DELETE\_YN = 'N',
STATUS\_CODE = 'DRAFT',
DELETED\_BY = NULL,
DELETED\_AT = NULL,
DELETE\_REASON = NULL,
UPDATED\_BY = #{restoredBy},
UPDATED\_AT = #{restoredAt},
VERSION\_NO = VERSION\_NO + 1
WHERE CAMPAIGN\_ID = #{campaignId}
AND STATUS\_CODE = 'DELETED'
AND DELETE\_YN = 'Y'
AND VERSION\_NO = #{expectedVersion}

복구 시 확인:

복구 가능 기간

업무 키 충돌

관련 Detail 상태

관련 Cache

승인 이력 보존

개인정보 파기 여부

복구 권한

복구 감사로그

## 19.3.12 삭제 후 Cache

DB 논리 삭제 성공
↓
Cache에 기존 데이터 유지
↓
삭제된 데이터가 화면에 계속 노출

대안:

Transaction Commit 후 Cache Evict

Outbox 기반 Cache Evict

짧은 TTL

Cache Key Version

DB Rollback 전에 Cache를 먼저 삭제하면 DB에는 데이터가 남고 Cache만 비워질 수 있다.

Commit 이후 처리 순서를 보장해야 한다.

## 19.3.13 검색색인·Read Model

Elasticsearch·검색용 Mart·Cache·Read Model을 사용하는 경우 삭제 이벤트를 전달해야 한다.

업무 DB DELETE\_YN = Y

\+ Outbox DELETE Event

→ 검색색인 제거

→ Read Model 비활성화

## 19.3.14 보존과 물리 파기

논리 삭제
↓
보존기간
↓
파기 대상 선정
↓
참조·감사·법적 보존 확인
↓
개인정보 파기
↓
물리 삭제·익명화
↓
파기 결과 대사

온라인 삭제 거래가 즉시 모든 물리 데이터를 제거하기보다 승인된 Batch 파기절차로 넘길 수 있다.

# 19.4 이력과 감사 정보

## 19.4.1 세 가지 기록을 구분한다

| 기록 | 목적 | 주요 사용자 |
| --- | --- | --- |
| 업무이력 | 데이터가 어떻게 변경됐는지 복원 | 업무 담당자 |
| 감사로그 | 누가 어떤 중요행위를 했는지 증명 | 감사·보안 |
| 거래로그 | 거래 성공·실패·성능 추적 | 운영·개발 |
| 애플리케이션 로그 | 기술 오류 분석 | 개발·운영 |
| SQL 로그 | DB 실행·성능 확인 | 개발·DBA |

하나의 로그가 모든 목적을 대신하지 않는다.

## 19.4.2 업무이력 테이블

CREATE TABLE CM\_CAMPAIGN\_HIST (
HISTORY\_ID VARCHAR2(50) NOT NULL,
CAMPAIGN\_ID VARCHAR2(40) NOT NULL,
HISTORY\_SEQUENCE NUMBER(10) NOT NULL,
ACTION\_CODE VARCHAR2(30) NOT NULL,
BEFORE\_STATUS\_CODE VARCHAR2(20),
AFTER\_STATUS\_CODE VARCHAR2(20),
BEFORE\_VERSION\_NO NUMBER(10),
AFTER\_VERSION\_NO NUMBER(10),
CHANGE\_REASON VARCHAR2(1000),
CHANGED\_BY VARCHAR2(50) NOT NULL,
CHANGED\_BRANCH\_CODE VARCHAR2(30) NOT NULL,
CHANGED\_AT TIMESTAMP NOT NULL,
GUID VARCHAR2(100) NOT NULL,
CONSTRAINT PK\_CM\_CAMPAIGN\_HIST
PRIMARY KEY (HISTORY\_ID)
);

필요하면 변경 전후 상세 Snapshot 또는 변경항목 목록을 별도 저장한다.

## 19.4.3 Snapshot 이력

변경 전 전체 Snapshot

변경 후 전체 Snapshot

장점:

복원 용이

시점별 상태 재현

구현 단순

단점:

저장용량 증가

개인정보 중복 저장

Schema 변경 영향

민감정보 보관 증가

## 19.4.4 Delta 이력

{
"changedFields": \[
{
"field": "campaignName",
"before": "기존 캠페인",
"after": "변경 캠페인"
},
{
"field": "endDate",
"before": "2026-08-30",
"after": "2026-08-31"
}
\]
}

장점:

실제 변경항목 명확

저장량 감소

감사 가독성

주의:

전체 시점 복원 복잡

Schema 변경 대응 필요

민감값 마스킹 필요

## 19.4.5 변경 전후 값 생성

Service가 현재 데이터를 조회한 결과와 변경 Command를 비교한다.

CampaignChangeSet changeSet =
campaignChangeDetector.detect(
current,
command
);

if (changeSet.isEmpty()) {
throw new BusinessException(
"E-CM-UPD-0003",
"변경된 내용이 없습니다."
);
}

변경이 없는 요청을 성공으로 처리할지 오류로 처리할지 계약으로 정한다.

## 19.4.6 이력 저장 시점

현재 상태 조회

→ Rule 검증

→ Main UPDATE

→ History INSERT

→ Outbox INSERT

→ Transaction Commit

Main UPDATE와 업무이력은 같은 Transaction에서 저장하는 것이 기본이다.

## 19.4.7 감사로그 필수 항목

중요 변경 거래 감사정보:

| 항목 | 설명 |
| --- | --- |
| Audit ID | 감사 이벤트 식별자 |
| GUID | 거래 연결 |
| TraceId | 시스템 간 연결 |
| ServiceId | 수행 기능 |
| 거래코드 | 운영 분류 |
| 사용자 ID | 수행자 |
| 지점 ID | 수행 조직 |
| 역할·권한 | 승인 근거 |
| 대상 유형 | CAMPAIGN |
| 대상 ID | campaignId |
| 행위 | UPDATE·APPROVE·DELETE·RESTORE |
| 이전 상태 | 변경 전 상태 |
| 신규 상태 | 변경 후 상태 |
| 이전 Version | 변경 전 Version |
| 신규 Version | 변경 후 Version |
| 변경사유 | 승인·삭제·복구 사유 |
| 결과 | SUCCESS·FAIL |
| 수행시각 | 감사시각 |
| Client IP | 접근 위치 |
| App Version | 실행 프로그램 버전 |
| Idempotency Key | 안전한 식별값 |

프로젝트 감사 기준도 GUID·ServiceId·사용자·대상·행위·결과와 중요 변경 전후 값을 연결하도록 정의한다.

## 19.4.8 변경 전후 개인정보

감사로그에 전체 개인정보 원문을 저장하지 않는다.

예:

고객등급
VIP → GOLD

연락처
010-12\*\*-56\*\* → 010-98\*\*-54\*\*

금지:

비밀번호 전후 값

Access Token

주민번호 전체

계좌번호 전체

암호화키

인증서 원문

비밀번호는 변경 여부만 기록한다.

passwordChanged = true

## 19.4.9 실패 감사

권한이 없는 사용자가 삭제를 시도했다면 데이터는 변경되지 않았어도 감사 대상일 수 있다.

행위
DELETE\_ATTEMPT

결과
DENIED

대상
campaignId

사용자
userId

오류
AUTHORIZATION

성공 감사만 저장하지 않는다.

## 19.4.10 감사로그와 트랜잭션

### 동기 필수 감사

감사 저장 실패
→ 업무 Rollback

중요한 권한·정책·개인정보 거래에 적용할 수 있다.

### Outbox 감사

업무 변경
\+ Audit Outbox
→ 같은 Transaction

별도 Consumer
→ 감사 저장

대량·고성능 환경에서 검토할 수 있다.

단순 비동기 호출로 감사 손실을 허용하지 않는다.

## 19.4.11 이력과 감사의 보존기간

업무이력과 감사로그는 보존 목적이 다를 수 있다.

업무이력
→ 업무 복원·변경 추적

감사로그
→ 법적·내부통제 증명

거래로그
→ 장애·성능 분석

각각 보존기간·Archive·파기·접근권한을 별도로 정의한다.

## 19.4.12 이력 조회 권한

변경 이력에는 과거 개인정보와 내부 상태가 포함될 수 있다.

따라서 일반 상세조회 권한과 이력조회 권한을 분리한다.

CM\_CAMPAIGN\_VIEW

CM\_CAMPAIGN\_HISTORY\_VIEW

CM\_CAMPAIGN\_AUDIT\_VIEW

## 19.4.13 이력 기반 복원

과거 Snapshot을 그대로 Main 테이블에 덮어쓰지 않는다.

복원 거래:

복원 대상 Version 선택

현재 Version 확인

복원 권한

복원 가능 상태

현재 데이터와 충돌 검증

새로운 Version으로 복원

복원 이력·감사 생성

복원도 하나의 새로운 변경이다.

# 변경 거래 목표 구현

## Handler

@Component
@RequiredArgsConstructor
public class CampaignHandler
implements TransactionHandler {

private static final String UPDATE =
"CM.Campaign.update";

private static final String APPROVE =
"CM.Campaign.approve";

private static final String DELETE =
"CM.Campaign.delete";

private static final String RESTORE =
"CM.Campaign.restore";

private final CampaignFacade facade;
private final TransactionBodyConverter converter;

@Override
public Set<String> serviceIds() {
return Set.of(
UPDATE,
APPROVE,
DELETE,
RESTORE
);
}

@Override
public Object handle(
StandardRequest<?> request,
TransactionContext context) {

String serviceId =
context.getHeader().getServiceId();

return switch (serviceId) {
case UPDATE ->
facade.updateCampaign(
converter.convertAndValidate(
request.getBody(),
CampaignUpdateRequest.class
),
context
);

case APPROVE ->
facade.approveCampaign(
converter.convertAndValidate(
request.getBody(),
CampaignApproveRequest.class
),
context
);

case DELETE ->
facade.deleteCampaign(
converter.convertAndValidate(
request.getBody(),
CampaignDeleteRequest.class
),
context
);

case RESTORE ->
facade.restoreCampaign(
converter.convertAndValidate(
request.getBody(),
CampaignRestoreRequest.class
),
context
);

default ->
throw new BusinessException(
ErrorCode.SERVICE\_NOT\_FOUND
);
};
}
}

## Facade

@Component
@RequiredArgsConstructor
public class CampaignFacade {

private final CampaignService service;

@Transactional(timeout = 5)
public CampaignUpdateResponse updateCampaign(
CampaignUpdateRequest request,
TransactionContext context) {

return service.updateCampaign(
request,
context
);
}

@Transactional(timeout = 5)
public CampaignStateResponse approveCampaign(
CampaignApproveRequest request,
TransactionContext context) {

return service.approveCampaign(
request,
context
);
}

@Transactional(timeout = 5)
public CampaignStateResponse deleteCampaign(
CampaignDeleteRequest request,
TransactionContext context) {

return service.deleteCampaign(
request,
context
);
}
}

## Service 수정 흐름

public CampaignUpdateResponse updateCampaign(
CampaignUpdateRequest request,
TransactionContext context) {

AuthenticationContext auth =
context.getAuthenticationContext();

CampaignCurrentResult current =
campaignDao.findCurrent(
request.campaignId())
.orElseThrow(
CampaignNotFoundException::new
);

campaignRule.validateAccess(
current,
auth
);

campaignStateRule.validateAction(
current.status(),
CampaignAction.UPDATE
);

campaignRule.validateUpdate(
current,
request
);

CampaignUpdateCommand command =
CampaignUpdateCommand.from(
request,
auth,
businessClock.now()
);

CampaignChangeSet changeSet =
changeDetector.detect(
current,
command
);

int updated =
campaignDao.updateCampaign(command);

validateUpdatedResult(
updated,
command,
auth
);

campaignDao.insertHistory(
CampaignHistoryCommand.update(
current,
command,
changeSet,
context
)
);

campaignDao.insertOutbox(
CampaignOutboxCommand.updated(
current,
command,
context
)
);

return CampaignUpdateResponse.of(
command.campaignId(),
current.status(),
command.expectedVersion() + 1L,
command.updatedAt()
);
}

# 정상 처리 흐름

1\. 사용자가 Version 3 캠페인을 조회한다.
2\. 수정 요청에 expectedStatus=DRAFT, expectedVersion=3을 보낸다.
3\. STF가 인증·권한·Idempotency를 검증한다.
4\. Handler가 Request DTO를 검증한다.
5\. Facade가 변경 Transaction을 시작한다.
6\. Service가 현재 캠페인을 조회한다.
7\. Rule이 상태·권한·수정 가능 필드를 검증한다.
8\. UPDATE가 ID·상태·Version·삭제 여부를 조건으로 실행된다.
9\. 영향 행 수 1건을 확인한다.
10\. Version이 4로 증가한다.
11\. 변경 이력과 Outbox가 저장된다.
12\. Transaction이 Commit된다.
13\. Idempotency가 SUCCESS로 변경된다.
14\. ETF가 신규 Version과 변경시각을 응답한다.
15\. 거래로그·감사로그가 성공 종료된다.

# 동시 수정 흐름

사용자 A
→ Version 3 수정 성공
→ Version 4

사용자 B
→ expectedVersion 3으로 수정

UPDATE
→ 0건

재조회
→ 현재 Version 4

결과
→ CONCURRENT\_MODIFICATION
→ “다른 사용자가 먼저 변경했습니다.”

# 상태 변경 충돌 흐름

사용자 A
→ DRAFT 화면 조회

사용자 B
→ 승인요청
→ REQUESTED

사용자 A
→ DRAFT 상태 수정 요청

UPDATE
WHERE STATUS = DRAFT
→ 0건

재조회
→ REQUESTED

결과
→ INVALID\_STATE

# 논리 삭제 흐름

삭제 요청
↓
상태·Version·권한 검증
↓
참조관계 확인
↓
DELETE\_YN = Y
STATUS = DELETED
VERSION + 1
↓
삭제 이력·감사
↓
Outbox
↓
Commit
↓
일반 조회 제외

# 복구 흐름

관리자 복구 요청
↓
DELETED 상태 조회
↓
복구 가능 기간 확인
↓
업무 키 충돌 확인
↓
DELETE\_YN = N
STATUS = DRAFT
VERSION + 1
↓
복구 이력·감사
↓
Cache·검색색인 복구

# 오류·Timeout·장애 흐름

## UPDATE 영향 행 0건

UPDATE
→ 0건
→ 현재 상태 재조회
→ 미존재·삭제·Version·상태·권한 분류
→ 업무 오류
→ Rollback

## DB Lock

변경 SQL
→ 다른 Transaction Lock 대기
→ Query Timeout
→ Transaction Rollback
→ Connection 반환
→ TCF Timeout·시스템 오류

## 이력 저장 실패

Main UPDATE 성공
→ History INSERT 실패
→ 전체 Rollback
→ Main UPDATE 취소
→ 시스템 오류

## Outbox 실패

상태 UPDATE 성공
→ Outbox INSERT 실패
→ 전체 Rollback
→ 외부 이벤트 미발행

# 정상 예시

ServiceId
CM.Campaign.approve

현재 상태
REQUESTED

목표 상태
APPROVED

expectedVersion
4

UPDATE 조건
campaignId
\+ status=REQUESTED
\+ version=4
\+ deleteYn=N

영향 행
1

신규 Version
5

업무이력
REQUESTED → APPROVED

감사
APPROVE SUCCESS

응답
APPROVED, version=5

# 금지 예시

Request에서 목표 상태를 자유롭게 받는다.

화면에서 버튼을 숨겼으므로 서버 상태 검증을 생략한다.

현재 데이터 조회 결과만 믿고 UPDATE WHERE에 상태를 넣지 않는다.

Version 조건 없이 UPDATE한다.

updated=0을 성공으로 반환한다.

updated=0을 모두 데이터 미존재로 처리한다.

PK UPDATE에서 2건 이상 변경돼도 성공 처리한다.

클라이언트가 보낸 updatedBy를 저장한다.

다른 업무 소유 테이블을 직접 수정한다.

상태 전이 Rule을 Mapper SQL 안에만 구현한다.

승인자와 작성자를 분리하지 않는다.

변경 Transaction 중 외부 API를 장시간 호출한다.

Main UPDATE와 이력 INSERT를 다른 Transaction으로 처리한다.

변경 전후 개인정보 전체를 감사로그에 저장한다.

논리 삭제 후 조회 SQL에서 DELETE\_YN 조건을 누락한다.

삭제된 Row 때문에 업무 키가 충돌해도 정책 없이 Unique를 제거한다.

일반 사용자에게 삭제 데이터 조회 Flag를 제공한다.

삭제와 개인정보 파기를 같은 개념으로 처리한다.

물리 삭제 전에 Batch·BI·외부 참조를 확인하지 않는다.

Cache를 Commit 전에 Evict하고 Rollback을 고려하지 않는다.

Timeout 후 새 Idempotency Key로 상태 변경을 다시 실행한다.

과거 이력 Snapshot을 현재 Row에 Version 확인 없이 덮어쓴다.

# 연계 규칙

## 다른 업무 WAR

다른 업무가 캠페인 상태를 변경해야 한다면 CM Mapper를 직접 호출하지 않는다.

호출 업무 Service
↓
TcfServiceClient
↓
CM.Campaign.cancel
↓
CM 업무 WAR
↓
CampaignService
↓
CM Mapper

## Cache

업무 Commit
↓
Transaction After Commit
↓
Cache Evict

또는 Outbox 이벤트를 사용한다.

## 외부 시스템

상태 전이와 외부 전송이 함께 필요한 경우:

DB 상태 전이
\+ Outbox INSERT
→ Commit
→ Publisher
→ 외부 시스템
→ 전송 결과 관리

## Batch

Batch가 같은 상태를 변경하는 경우 온라인 거래와 동일한 상태·Version 규칙을 사용해야 한다.

온라인
→ VERSION\_NO 조건

Batch
→ VERSION\_NO·현재 상태 조건

Batch가 Version을 무시하고 덮어쓰지 않도록 한다.

# 책임 경계와 RACI

| 활동 | 업무분석 | 업무개발 | AA | DA·DBA | FW | 보안·감사 | QA | 운영 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 상태 모델 정의 | A/R | C | C | C | I | C | C | I |
| 상태 전이표 | A/R | R | C | C | I | C | C | I |
| 수정 가능 필드 | A | R | C | C | I | C | C | I |
| Version 설계 | C | R | A | A/R | C | I | C | C |
| UPDATE SQL | I | R | C | A/R | I | I | C | C |
| 영향 행 판정 | C | R | A | C | I | I | C | I |
| 논리 삭제 | A/R | R | A | R | I | C | C | C |
| 물리 파기 | C | C | C | A/R | I | A/R | C | R |
| 업무이력 | A | R | C | C | I | C | C | I |
| 감사로그 | C | C | C | I | C | A/R | C | R |
| Idempotency | I | R | A | C | R | C | C | R/C |
| 동시성 테스트 | I | R | C | C | C | I | A/R | C |
| Cache·Outbox | I | R | A | I | C | I | C | R |
| 복구 승인 | A | C | C | C | I | A/C | C | R |

# 데이터 및 상태관리

## 상태 컬럼

STATUS\_CODE

허용 상태를 공통코드·도메인 Enum·DB Check Constraint로 일치시킨다.

## Version 컬럼

VERSION\_NO NUMBER NOT NULL

등록 시 1, 변경마다 +1을 기본으로 한다.

## 삭제 컬럼

DELETE\_YN

DELETED\_BY

DELETED\_AT

DELETE\_REASON

삭제 여부만 있고 삭제자·삭제시각·사유가 없으면 운영 추적성이 부족하다.

## 변경 컬럼

UPDATED\_BY

UPDATED\_BRANCH\_CODE

UPDATED\_AT

최종 수정정보는 빠른 조회용이며, 전체 변경과정을 대신하지 않는다.

## 이력

Main Table
→ 현재 상태

History Table
→ 과거 상태

Audit Log
→ 사용자 행위 증명

# 성능·용량·확장성

## 낙관적 잠금

Version 조건은 일반적으로 PK와 함께 사용되므로 Index 비용이 크지 않다.

WHERE CAMPAIGN\_ID = ?
AND VERSION\_NO = ?

PK로 Row를 찾은 뒤 Version을 비교한다.

## 이력 증가

변경이 잦은 업무는 이력 데이터가 Main보다 훨씬 커질 수 있다.

확인:

일평균 변경 건수

Snapshot 크기

보존기간

Partition

압축

Archive

파기

## 논리 삭제 증가

삭제 Row가 계속 쌓이면 다음에 영향을 준다.

Index 크기

조회 선택도

통계정보

Backup

Batch

Unique Key

Archive·Partition·물리 파기 정책이 필요하다.

## Lock과 Timeout

Query Timeout
<
Facade Transaction Timeout
≤
TCF 전체 Timeout

변경 Transaction 안에서 대량 조회·외부 호출·파일 처리를 수행하지 않는다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| 변경자 | 인증 Context |
| 지점 | 검증된 조직정보 |
| 상태 권한 | 행위별 기능권한 |
| 데이터권한 | 대상 소유·조직범위 |
| 승인 분리 | 작성자·승인자 분리 |
| Version | 서버 조회값 기반 |
| 감사 | 중요 변경·삭제·복구 필수 |
| 전후 값 | 최소·마스킹 |
| 삭제 데이터 | 별도 관리자 권한 |
| 물리 파기 | 승인·대사·증적 |
| 로그 | Token·개인정보 원문 금지 |
| 복구 | 관리자 권한·사유 필수 |

# 운영·모니터링·장애 대응

## 권장 Metric

command.update.success.count

command.update.concurrentConflict.count

command.update.invalidState.count

command.update.notFound.count

command.update.affectedRowsMismatch.count

command.delete.success.count

command.restore.success.count

command.history.failure.count

command.audit.failure.count

state.transition.byServiceId

## 권장 로그

event=STATE\_TRANSITION\_COMPLETED
guid=G-...
serviceId=CM.Campaign.approve
targetType=CAMPAIGN
targetId=CMP-...
fromStatus=REQUESTED
toStatus=APPROVED
fromVersion=4
toVersion=5
affectedRows=1
userId=U12345
elapsedMs=182
result=SUCCESS

## 운영 확인 질문

동시 수정 오류가 어느 화면에서 증가했는가?

사용자가 오래된 화면을 계속 사용하고 있는가?

특정 ServiceId의 상태 오류가 증가했는가?

삭제 데이터가 일반 조회에 노출됐는가?

PROCESSING 상태 변경 거래가 장시간 남아 있는가?

Main 데이터와 이력의 Version이 일치하는가?

감사로그 누락 거래가 존재하는가?

Outbox 미처리 이벤트가 쌓였는가?

삭제 Row가 보존기간을 초과했는가?

# 자동검증 및 품질 Gate

## 1\. 변경 거래 Gate

processingType = COMMAND

Idempotency 필수

Facade Transaction 존재

감사 여부 등록

Version 정책 존재

영향 행 수 검증

## 2\. SQL Gate

변경 SQL에 다음 조건이 있는지 확인한다.

업무 식별자

Version

현재 상태 또는 수정 가능 상태

삭제 여부

필요한 권한범위

## 3\. 금지 SQL

PK 없는 UPDATE

조건 없는 DELETE

Version 없는 중요 데이터 변경

사용자 입력 목표 상태 직접 SET

다른 도메인 테이블 UPDATE

DELETE\_YN 조건 없는 일반 조회

## 4\. 상태 전이 Gate

현재 상태

행위

목표 상태

필요 권한

실패코드

감사 여부

가 상태 전이표에 존재해야 한다.

## 5\. 이력 Gate

중요 변경은 다음을 만족한다.

Main UPDATE

History INSERT

Audit 기록

같은 GUID

같은 Transaction 또는 보장된 Outbox

## 6\. 논리 삭제 Gate

일반 조회 DELETE\_YN=N

Count SQL DELETE\_YN=N

중복검사 삭제정책 반영

복구 거래 존재 여부

보존·파기 정책

Cache Evict

## 7\. 동시성 Gate

동일 Version 동시 수정 테스트

상태 변경 충돌 테스트

Batch·온라인 동시 변경 테스트

UPDATE 0건 원인 처리 테스트

## 8\. 민감정보 Gate

감사·이력·로그에 다음 원문 저장을 차단한다.

Token

비밀번호

주민번호

계좌번호 전체

Private Key

인증서

개인정보 Body 전체

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| UDT-001 | DRAFT 정상 수정 | Version 증가 |
| UDT-002 | REQUESTED 일반 수정 | 상태 오류 |
| UDT-003 | RUNNING 수정 | 상태 오류 |
| UDT-004 | 없는 대상 수정 | 미존재 오류 |
| UDT-005 | 삭제 대상 수정 | 삭제 오류 |
| UDT-006 | 타 지점 데이터 수정 | 권한 오류 |
| UDT-007 | 수정권한 없음 | 권한 오류 |
| UDT-008 | expectedVersion 누락 | Validation 오류 |
| UDT-009 | 현재 Version 일치 | UPDATE 1건 |
| UDT-010 | 오래된 Version | 동시 수정 오류 |
| UDT-011 | 동일 Version 동시 요청 | 한 건 성공 |
| UDT-012 | UPDATE 2건 | 정합성 오류 |
| UDT-013 | 변경내용 없음 | 정책상 오류·성공 |
| UDT-014 | 작성자 승인 | 분리권한 오류 |
| UDT-015 | REQUESTED 승인 | APPROVED |
| UDT-016 | DRAFT 승인 | 상태 오류 |
| UDT-017 | 이미 APPROVED 재승인 동일 Key | 기존 결과 |
| UDT-018 | 이미 APPROVED 신규 Key | 정책상 처리 |
| UDT-019 | 같은 Key 다른 Body | 멱등성 오류 |
| UDT-020 | Main UPDATE 후 Detail 실패 | 전체 Rollback |
| UDT-021 | History INSERT 실패 | 전체 Rollback |
| UDT-022 | Outbox INSERT 실패 | 전체 Rollback |
| UDT-023 | DB Lock | Timeout·Rollback |
| UDT-024 | Timeout 후 재요청 | 상태 확인 |
| UDT-025 | DRAFT 논리 삭제 | DELETED |
| UDT-026 | APPROVED 삭제 | 상태 오류 |
| UDT-027 | 삭제 Version 불일치 | 동시 수정 오류 |
| UDT-028 | 중복 삭제 동일 Key | 기존 결과 |
| UDT-029 | 삭제 후 일반 상세조회 | 미노출 |
| UDT-030 | 삭제 후 목록 Count | 제외 |
| UDT-031 | 관리자 삭제목록 | 권한 시 조회 |
| UDT-032 | 삭제 업무 키 재사용 | 정책대로 처리 |
| UDT-033 | 보존기간 내 복구 | DRAFT 복구 |
| UDT-034 | 보존기간 초과 복구 | 거절 |
| UDT-035 | 복구 시 업무 키 충돌 | 거절 |
| UDT-036 | 복구 후 Version | 증가 |
| UDT-037 | Cache에 삭제 데이터 | Evict 확인 |
| UDT-038 | 검색색인 삭제 | 이벤트 처리 |
| UDT-039 | 개인정보 파기 | 복구 불가 |
| UDT-040 | 감사로그 변경 전후 | 마스킹 |
| UDT-041 | 권한 실패 감사 | DENIED 기록 |
| UDT-042 | 거래로그 Version | 전후 연결 |
| UDT-043 | Batch와 온라인 동시 수정 | Version 충돌 |
| UDT-044 | 다른 도메인 UPDATE | Gate 실패 |
| UDT-045 | Version 없는 SQL | Gate 실패 |
| UDT-046 | 일반 조회 삭제조건 누락 | Gate 실패 |
| UDT-047 | History와 Main 대사 | Version 일치 |
| UDT-048 | 물리 파기 | 승인·대사·증적 |
| UDT-049 | Rollback 후 DB 상태 | 원상 유지 |
| UDT-050 | 다른 개발자 재현 | 동일 결과 |

# 따라 하는 실무 절차

## 1단계. 상태 모델을 작성한다

완료 증적:

상태 목록

상태 정의

최초 상태

종료 상태

삭제 상태

## 2단계. 상태 전이표를 작성한다

현재 상태

행위

목표 상태

권한

전제조건

오류코드

## 3단계. 수정 가능 필드를 정의한다

상태별 수정 필드

변경 금지 필드

서버 생성 필드

개인정보 필드

## 4단계. Version을 설계한다

등록 초기값

증가 시점

Request 전달

UPDATE 조건

충돌 응답

## 5단계. Request·Command를 작성한다

업무 ID

expectedStatus

expectedVersion

변경값

변경사유

## 6단계. Rule을 구현한다

존재

권한

상태

필드

전이

참조

## 7단계. UPDATE SQL을 작성한다

ID

상태

Version

삭제 여부

영향 행 수

## 8단계. 이력·감사를 설계한다

변경 전후

행위

사유

사용자

GUID

보존기간

## 9단계. 논리 삭제·복구를 구현한다

삭제 가능 상태

조회 제외

Unique 정책

복구 조건

파기 정책

## 10단계. 동시성 테스트를 실행한다

동일 Version 두 요청

상태 변경 충돌

Batch 동시 변경

영향 행 0건

## 11단계. Rollback을 검증한다

Master

Detail

History

Outbox

감사

## 12단계. 운영 증적을 확인한다

Response

DB

History

Audit

Idempotency

거래로그

Cache·Outbox

# 완료 체크리스트

## 상태·업무 규칙

| 확인 항목 | 완료 |
| --- | --- |
| 상태 목록을 정의했다. | □ |
| 상태별 의미가 명확하다. | □ |
| 상태 전이표가 있다. | □ |
| 허용되지 않은 전이를 정의했다. | □ |
| 상태별 수정 가능 필드가 있다. | □ |
| 행위별 권한이 분리됐다. | □ |
| 작성자·승인자 분리 기준이 있다. | □ |
| 목표 상태를 서버가 결정한다. | □ |

## Version·동시성

| 확인 항목 | 완료 |
| --- | --- |
| Version 컬럼이 있다. | □ |
| 등록 초기값이 정의됐다. | □ |
| 변경마다 Version이 증가한다. | □ |
| Request에 expectedVersion이 있다. | □ |
| UPDATE WHERE에 Version이 있다. | □ |
| UPDATE WHERE에 현재 상태가 있다. | □ |
| UPDATE 0건 원인을 구분한다. | □ |
| UPDATE 2건 이상을 오류 처리한다. | □ |
| Idempotency와 Version을 함께 적용한다. | □ |
| 동시 수정 테스트가 있다. | □ |

## 삭제·복구

| 확인 항목 | 완료 |
| --- | --- |
| 논리·물리 삭제 기준이 있다. | □ |
| 삭제 가능 상태를 정의했다. | □ |
| 일반 조회에 삭제조건이 있다. | □ |
| Count SQL에 삭제조건이 있다. | □ |
| Unique Key 재사용 정책이 있다. | □ |
| 삭제자·삭제시각·사유를 저장한다. | □ |
| 복구 가능 여부를 정의했다. | □ |
| 복구 가능 기간이 있다. | □ |
| 개인정보 파기정책이 별도다. | □ |
| 보존·Archive·물리 파기 기준이 있다. | □ |

## 이력·감사

| 확인 항목 | 완료 |
| --- | --- |
| 업무이력과 감사로그를 구분했다. | □ |
| 변경 전후 상태를 기록한다. | □ |
| 변경 전후 Version을 기록한다. | □ |
| 변경사유를 기록한다. | □ |
| 사용자·지점·GUID를 기록한다. | □ |
| 개인정보를 마스킹한다. | □ |
| 실패·권한거절 감사정책이 있다. | □ |
| Main과 History가 같은 Transaction이다. | □ |
| 보존기간과 접근권한이 있다. | □ |
| 이력 기반 복원정책이 있다. | □ |

## 연계·운영

| 확인 항목 | 완료 |
| --- | --- |
| Cache Evict 시점이 Commit 이후다. | □ |
| 검색색인·Read Model 반영이 있다. | □ |
| 외부 부수효과는 Outbox를 검토했다. | □ |
| 거래로그에 상태·Version이 있다. | □ |
| 동시 수정 Metric이 있다. | □ |
| 장기 PROCESSING 점검이 있다. | □ |
| Main·History 대사 절차가 있다. | □ |
| Rollback 결과를 DB에서 확인했다. | □ |

# 변경·호환성·폐기 관리

## 상태 추가

기존
DRAFT → REQUESTED

신규
DRAFT → REVIEWING → REQUESTED

영향:

UI 버튼

ServiceId

Rule

DB Check Constraint

공통코드

Batch

보고서

감사

기존 데이터

## 상태 코드명 변경

APPROVED
→ CONFIRMED

단순 Enum 리팩터링이 아니다.

DB 데이터·외부 계약·로그·보고서·통계에 영향을 준다.

## 전이 규칙 변경

기존
APPROVED에서 취소 가능

신규
APPROVED에서 취소 금지

기존 운영 데이터와 진행 중 요청의 처리정책을 정의해야 한다.

## Version 도입

기존 테이블에 Version을 추가할 때:

기존 Row 초기 Version 설정

NOT NULL 전환

기존 UI Version 전달

구버전 Client 호환

변경 SQL 일괄 수정

Batch 수정

점진적 적용계획이 필요하다.

## 물리 삭제에서 논리 삭제로 변경

영향:

조회 SQL

Count

Unique

용량

Index

Batch

BI

파기

복구 화면

## 감사 범위 확대

변경 전후 Snapshot을 신규 저장하면 개인정보 보관량과 저장용량이 증가한다.

보안·감사·DBA 승인이 필요하다.

## ServiceId 폐기

CM.Campaign.update
→ 신규 행위별 ServiceId로 분리

기존 소비자를 조사하고 호출량이 0이 될 때까지 병행 운영한다.

# 시사점

## 핵심 아키텍처 판단

첫째, 수정 가능 여부는 화면이 아니라 서버의 현재 상태 Rule이 결정한다.

둘째, Rule에서 현재 상태를 확인한 것만으로 충분하지 않다.

UPDATE WHERE
→ 현재 상태 + Version

을 다시 검증해야 한다.

셋째, 낙관적 잠금과 Idempotency는 서로 다른 문제를 해결하며 변경 거래에는 둘 다 필요할 수 있다.

넷째, UPDATE 0건은 성공이 아니며 하나의 원인으로 단정할 수도 없다.

미존재

삭제

상태 변경

Version 충돌

권한

을 정책에 따라 해석해야 한다.

다섯째, 논리 삭제는 DELETE\_YN 컬럼을 추가하는 것으로 끝나지 않는다.

조회

Unique

복구

보존

파기

Cache

감사

를 함께 설계해야 한다.

여섯째, 변경 이력과 감사로그는 목적이 다르며 하나로 통합해서는 안 된다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 상태 검증 없음 | 허용되지 않은 변경 |
| Version 없음 | Lost Update |
| UPDATE 0건 성공 처리 | 변경 유실 |
| 모든 0건을 미존재 처리 | 원인 왜곡 |
| 목표 상태 사용자 입력 | 권한 우회 |
| 범용 상태 변경 API | 전이 통제 약화 |
| 이력 별도 Transaction | Main·History 불일치 |
| 논리 삭제 조회조건 누락 | 삭제정보 노출 |
| Unique 정책 부재 | 재등록 장애 |
| 삭제=파기 오해 | 개인정보 잔존 |
| Cache Commit 전 변경 | DB·Cache 불일치 |
| Timeout 후 무조건 재처리 | 이중 상태 전이 |
| 감사 원문 저장 | 개인정보 노출 |
| 다른 도메인 UPDATE | 소유권 붕괴 |
| Batch Version 무시 | 온라인 변경 덮어쓰기 |

## 우선 보완 과제

1.  업무별 상태 전이표를 공식 산출물로 관리한다.
2.  모든 중요 변경 테이블에 Version 적용 여부를 점검한다.
3.  UPDATE SQL의 상태·Version·삭제 조건을 자동검사한다.
4.  영향 행 0건의 표준 오류 분류를 정의한다.
5.  행위별 변경 ServiceId와 기능권한을 분리한다.
6.  변경·삭제·승인 거래에 Idempotency를 필수화한다.
7.  Main 변경과 업무이력 저장을 같은 Transaction으로 통합한다.
8.  감사로그의 변경 전후 값 마스킹 기준을 확정한다.
9.  논리 삭제 조회조건 누락을 SQL Gate로 탐지한다.
10.  삭제 데이터의 Unique Key 재사용 정책을 확정한다.
11.  복구 ServiceId·권한·보존기간을 설계한다.
12.  개인정보 파기와 일반 삭제 절차를 분리한다.
13.  Cache·검색색인 변경을 Outbox 또는 After Commit으로 처리한다.
14.  온라인·Batch 동시 변경 테스트를 추가한다.
15.  Main·History·Audit·Idempotency 자동 대사를 구현한다.

## 중장기 발전 방향

단순 UPDATE SQL
↓
상태·Version 조건 표준화
↓
행위별 Command·ServiceId
↓
상태 전이표 자동검증
↓
업무이력·감사 공통화
↓
Outbox 기반 부수효과
↓
동시성·Rollback 자동 테스트
↓
상태·Version 기반 운영 이상 탐지

# 마무리말

변경·삭제와 상태 전이를 설계하는 과정은 다음 질문에 답하는 일이다.

현재 상태는 무엇인가?

이 상태에서 어떤 행위가 허용되는가?

누가 그 행위를 할 수 있는가?

어떤 필드를 바꿀 수 있는가?

화면이 조회한 이후 다른 사용자가 수정하지 않았는가?

UPDATE 조건에 상태와 Version이 포함됐는가?

영향 행 0건이면 어떤 원인을 확인할 것인가?

삭제는 논리 삭제인가 물리 삭제인가?

삭제된 데이터는 조회·Unique·Cache에서 어떻게 처리되는가?

복구는 가능한가?

개인정보 파기는 언제 어떻게 수행하는가?

변경 전후 값을 어떤 이력과 감사정보로 증명하는가?

Main·Detail·History·Outbox가 함께 Commit되는가?

Timeout 후 실제 상태를 어떻게 확인하는가?

제19장의 핵심 흐름은 다음과 같다.

현재 데이터 조회
↓
상태·권한 Rule
↓
Version 확인
↓
ID + 상태 + Version 조건 UPDATE
↓
영향 행 수 판정
↓
이력·감사·Outbox
↓
Commit
↓
신규 상태·Version 응답

가장 중요한 원칙은 다음과 같다.

UPDATE 문이 실행됐다고
변경이 안전한 것은 아니다.

현재 상태와 Version,
영향 행 수와 변경 이력,
삭제·복구·감사정책이 함께 있어야

다른 사용자의 변경을 보호하고
운영에서 설명할 수 있는
신뢰 가능한 변경 거래가 된다.
