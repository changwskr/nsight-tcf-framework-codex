# Batch / Scheduler 명명규칙

> **NSIGHT TCF 명명규칙 상세** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

Batch / Scheduler 명명규칙 설계기준
## 1. 도입 전 안내말

Batch / Scheduler 명명규칙은 단순히 배치 프로그램 이름을 정하는 기준이 아니다.NSIGHT TCF에서는 Batch 이름이 Job 등록, 스케줄 등록, 실행 이력, Step 이력, Lock, 재처리, 감사로그, 장애 알림, OM 운영관리 화면과 연결되어야 한다.
NSIGHT Batch 구조는 Scheduler와 Batch를 분리하는 것이 기준이다. Scheduler는 “언제 실행할 것인가”를 결정하고, Batch는 “무엇을 실행할 것인가”를 처리하며, OM은 배치 등록·실행·중지·이력·재처리를 관리한다.또한 tcf-batch는 AP/DB/세션/배포 상태 수집과 같은 운영 대시보드 수집 배치를 수행하는 모듈로 정의되어 있고, 향후 DB 기반 Job/스케줄 관리, 실행 이력, 중복 실행 방지, 실패 재처리 구조를 보강하는 방향이 적합하다.

## 2. Batch / Scheduler 명명 최상위 원칙

| 원칙 | 설계 기준 |
| --- | --- |
| Batch와 Scheduler 분리 | Scheduler는 실행 시점, Batch는 처리 로직을 담당한다 |
| JobId 중심 | 모든 배치는 JOB_ID를 기준으로 등록·실행·이력·재처리한다 |
| 업무코드 포함 | 업무 배치는 SV, CM, MG, OM, UD 등 업무코드를 포함한다 |
| 실행유형 분리 | 자동실행, 수동실행, 재처리 실행을 RUN_TYPE으로 구분한다 |
| Step 단위 추적 | 장시간·대량 배치는 Step 단위로 명명하고 이력을 남긴다 |
| Lock 명확화 | 중복 실행 방지를 위한 Lock 이름을 JobId와 기준일자로 생성한다 |
| 환경 분리 | local/dev/stg/prd 환경별 Scheduler 설정을 분리한다 |
| 로그 추적 가능 | 배치 로그에는 jobId, executionId, stepId, guid, traceId를 포함한다 |
| 재처리 가능성 표시 | 재처리 가능한 Job은 RETRY_YN 또는 Retry 정책을 명확히 둔다 |
| 감사 대상 관리 | 수동 실행, 강제 중지, 재실행, Lock 해제, 스케줄 변경은 감사로그 대상이다 |

## 3. Batch / Scheduler 기본 연결 구조

```text
[OM 배치관리 화면]
  - Job 등록
  - 스케줄 등록
  - 수동 실행
  - 실행 이력 조회
  - 실패 재처리
  - 강제 중지
        ↓
[OM_BATCH_JOB]
        ↓

```

```text
[OM_BATCH_SCHEDULE]
        ↓
[tcf-batch Scheduler Runner]
        ↓

```

```text
[BatchJobLauncher]
        ↓
[BatchJobHandler]
        ↓

```

```text
[BatchStep]
        ↓
```

[OM_BATCH_EXECUTION / OM_BATCH_STEP_EXECUTION / OM_BATCH_LOCK]

운영 기준에서는 모든 Job이 실행 이력을 남기고, FAIL/TIMEOUT/UNKNOWN 상태에 대해서만 재처리를 허용하며, 다중 WAS 또는 다중 Batch 인스턴스 환경에서는 DB Lock 또는 Quartz Cluster 기반 중복 실행 방지가 필요하다.

## 4. Batch JobId 명명규칙

### 4.1 기본 형식

{OWNER_CODE}-BAT-{NNNN}

| 구성요소 | 설명 | 예시 |
| --- | --- | --- |
| OWNER_CODE | 업무코드 또는 공통영역 코드 | SV, CM, OM, UD, TCF, LOG |
| BAT | Batch 식별자 | 고정 |
| NNNN | 4자리 일련번호 | 0001 |
| 예시: | JobId | 의미 |
| OM-BAT-0001 | OM AP 상태 수집 | OM-BAT-0002 |
| OM DB 상태 수집 | OM-BAT-0003 | OM 세션 현황 수집 |
| OM-BAT-0004 | OM 배포 현황 수집 | CM-BAT-0001 |
| 캠페인 대상 추출 | MG-BAT-0001 | 메시지 발송 결과 집계 |
| UD-BAT-0001 | 파일 보관기간 만료 정리 | LOG-BAT-0001 |
| 거래로그 보관/아카이브 | TCF-BAT-0001 | UNKNOWN 거래 상태 확인 |

기존 설계에서 사용한 BAT-BATCH-001, BAT-TX-001, BAT-LOG-001, CM-BAT-001, MG-BAT-001, UD-BAT-001 형식은 기존 Job 식별자로 유지할 수 있다. 다만 신규 표준은 업무코드와 일련번호 자리수를 명확히 하기 위해 {OWNER_CODE}-BAT-{NNNN} 형식을 권장한다.

## 5. OWNER_CODE 표준

OWNER_CODE

| 의미 | 적용 예 | TCF |
| --- | --- | --- |
| TCF Framework 공통 배치 | 거래상태 확인, Timeout 후처리 | OM |

운영관리 배치
AP/DB/세션/배포 상태 수집

| GW | Gateway 배치 |
| --- | --- |
| Route 상태 점검, Target Health 수집 | LOG |

로그 관리 배치
거래로그 아카이브, 오류로그 집계

| UD | 파일관리 배치 |
| --- | --- |
| 파일 만료 삭제, 파일 메타 정리 | SV |

Single View 업무 배치
고객조회 통계, 고객정보 집계

| CM | 캠페인 업무 배치 |
| --- | --- |
| 캠페인 대상 추출, 캠페인 상태 변경 | MG |

메시지 업무 배치
발송결과 수집, 실패 재처리

| EB | 이벤트 업무 배치 |
| --- | --- |
| 이벤트 수집, 이벤트 정리 | EP |

이벤트 처리 배치
이벤트 후처리, 실시간 실패 보정
BT
Batch 공통
공통 배치 유틸리티성 Job

## 6. Batch Job명 명명규칙

JobId는 시스템 식별자이고, Job명은 운영자가 이해하는 이름이다.
{업무명} {처리대상} {처리행위} 배치

| JobId | Job명 |
| --- | --- |
| OM-BAT-0001 | AP 상태 수집 배치 |
| OM-BAT-0002 | DB 상태 수집 배치 |
| OM-BAT-0003 | 세션 현황 수집 배치 |
| OM-BAT-0004 | 배포 상태 수집 배치 |
| CM-BAT-0001 | 캠페인 대상 추출 배치 |
| MG-BAT-0001 | 메시지 발송결과 수집 배치 |
| UD-BAT-0001 | 파일 보관기간 만료 정리 배치 |
| LOG-BAT-0001 | 거래로그 아카이브 배치 |
| TCF-BAT-0001 | UNKNOWN 거래상태 확인 배치 |

## 7. ScheduleId 명명규칙

### 7.1 기본 형식

SCH-{ENV_CODE}-{JOB_ID}

| 구성요소 | 설명 | 예시 |
| --- | --- | --- |
| SCH | Schedule 식별 Prefix | 고정 |
| ENV_CODE | 실행 환경 | LOCAL, DEV, STG, PRD |
| JOB_ID | 배치 JobId | OM-BAT-0001 |
| 예시: | ScheduleId | 의미 |
| SCH-PRD-OM-BAT-0001 | 운영 AP 상태 수집 스케줄 | SCH-PRD-CM-BAT-0001 |
| 운영 캠페인 대상 추출 스케줄 | SCH-STG-LOG-BAT-0001 | 검증 거래로그 아카이브 스케줄 |
| SCH-DEV-UD-BAT-0001 | 개발 파일 정리 스케줄 |  |

## 8. Cron / 주기 명명규칙

스케줄 주기는 이름과 Cron 표현식을 함께 관리한다.
ScheduleType
| 의미 | 예시 |
| --- | --- |
| CRON | Cron 표현식 기반 |
| 0 0/5 * * * ? | FIXED_DELAY |
| 이전 실행 종료 후 지연 실행 | 5분 후 |
| FIXED_RATE | 고정 주기 실행 |
| 1분마다 | MANUAL_ONLY |
| 수동 실행 전용 | OM에서만 실행 |
| ONCE | 1회 실행 |
| 특정일 1회 | EVENT_TRIGGER |
| 이벤트 기반 실행 | 파일 수신 후 실행 |
| 스케줄명은 다음 형식을 권장한다. | {JOB_NAME}_{주기명} |

예시:

| ScheduleName | 설명 |
| --- | --- |
| AP상태수집_5분주기 | 5분마다 AP 상태 수집 |
| DB상태수집_5분주기 | 5분마다 DB 상태 수집 |
| 거래로그아카이브_일1회 | 매일 1회 거래로그 아카이브 |
| 캠페인대상추출_수동 | 수동 실행 전용 |
| 파일만료정리_일1회 | 매일 파일 만료 정리 |

## 9. ExecutionId 명명규칙

ExecutionId는 실제 실행 1건을 식별한다.
EXE-{yyyyMMdd}-{JOB_ID}-{NNNNNN}

예시:

| ExecutionId | 의미 |
| --- | --- |
| EXE-20260705-OM-BAT-0001-000001 | 2026-07-05 AP 상태 수집 1번째 실행 |
| EXE-20260705-CM-BAT-0001-000001 | 2026-07-05 캠페인 대상 추출 1번째 실행 |
| EXE-20260705-UD-BAT-0001-000002 | 2026-07-05 파일 정리 2번째 실행 |

ExecutionId는 다음 항목과 함께 관리한다.
| 항목 | 설명 |
| --- | --- |
| JOB_ID | 실행된 Job |
| SCHEDULE_ID | 어떤 스케줄로 실행되었는지 |
| RUN_TYPE | 자동/수동/재처리 |
| BUSINESS_DATE | 업무 기준일자 |
| START_DTM | 시작일시 |
| END_DTM | 종료일시 |
| STATUS | 실행상태 |
| GUID | 추적 GUID |
| TRACE_ID | Trace ID |

## 10. StepId 명명규칙

대량 배치 또는 복합 배치는 Step 단위로 나눈다.
{JOB_ID}-STEP-{NN}

예시:

| StepId | 의미 |
| --- | --- |
| CM-BAT-0001-STEP-01 | 캠페인 대상 조회 |
| CM-BAT-0001-STEP-02 | 대상 조건 검증 |
| CM-BAT-0001-STEP-03 | 대상 저장 |
| CM-BAT-0001-STEP-04 | 결과 파일 생성 |
| LOG-BAT-0001-STEP-01 | 아카이브 대상 로그 조회 |
| LOG-BAT-0001-STEP-02 | 압축 파일 생성 |
| LOG-BAT-0001-STEP-03 | 원본 로그 삭제 |
Step명은 다음 형식을 권장한다.

| {처리대상}_{처리행위} | |

예시:

| StepId | StepName |
| --- | --- |
| CM-BAT-0001-STEP-01 | CampaignTarget_Select |
| CM-BAT-0001-STEP-02 | CampaignTarget_Validate |
| CM-BAT-0001-STEP-03 | CampaignTarget_Save |
| UD-BAT-0001-STEP-01 | ExpiredFile_Select |
| UD-BAT-0001-STEP-02 | ExpiredFile_Delete |

## 11. LockId 명명규칙

LockId는 중복 실행 방지 기준이다.
LOCK-{JOB_ID}-{BUSINESS_DATE}

예시:

| LockId | 의미 |
| --- | --- |
| LOCK-OM-BAT-0001-20260705 | 2026-07-05 AP 상태 수집 Lock |
| LOCK-CM-BAT-0001-20260705 | 2026-07-05 캠페인 대상 추출 Lock |
| LOCK-LOG-BAT-0001-20260705 | 2026-07-05 로그 아카이브 Lock |

다중 WAS 또는 다중 Batch 인스턴스 환경에서는 중복 실행 방지가 필수이며, DB Lock 또는 Quartz Cluster를 검토해야 한다.

## 12. RunType 명명규칙

RunType
| 의미 | 설명 | SCHEDULED | 자동 스케줄 실행 |
| --- | --- | --- | --- |
| Scheduler가 정해진 주기에 실행 | MANUAL | 수동 실행 | OM 관리자가 즉시 실행 |
| RETRY | 실패 재처리 | 실패/Timeout/Unknown 건 재실행 | RERUN |
| 전체 재실행 | 동일 기준일자 전체 재수행 | RECOVERY | 복구 실행 |
| 장애 후 상태 보정 | TEST | 테스트 실행 | 운영에서는 사용 금지 또는 제한 |

수동 실행, 재실행, 강제 중지, Lock 해제는 감사로그 대상으로 관리한다.

## 13. Batch Status 명명규칙

배치 실행 상태는 다음 값을 사용한다.
Status
| 의미 | 후속 처리 | READY | 실행 대기 |
| --- | --- | --- | --- |
| 실행 가능 | RUNNING | 실행 중 | 중복 실행 차단 |
| SUCCESS | 정상 종료 | 후속 Job 실행 가능 | FAIL |
| 실패 | 재처리 가능 여부 판단 | TIMEOUT | 제한시간 초과 |
| 상태 확인 필요 | UNKNOWN | 결과 불명확 | 운영자 확인 또는 상태 보정 |
| CANCELED | 운영자 중지 | 재실행 여부 판단 | SKIPPED |
| 선행조건 미충족 | 다음 주기 재시도 | LOCKED | Lock 획득 실패 |
| 중복 실행 방지로 Skip | RETRYING | 재처리 중 | 재처리 이력 연결 |

기존 Batch 설계에서도 READY, RUNNING, SUCCESS, FAIL, TIMEOUT, UNKNOWN, CANCELED, SKIPPED 상태를 기준으로 후속 처리와 재처리 여부를 판단하도록 정리한다.

## 14. Batch Handler Class 명명규칙

### 14.1 기본 형식

{업무대상}{처리행위}JobHandler

| JobId | Handler Class |
| --- | --- |
| OM-BAT-0001 | ApStatusCollectJobHandler |
| OM-BAT-0002 | DbStatusCollectJobHandler |
| OM-BAT-0003 | SessionStatusCollectJobHandler |
| OM-BAT-0004 | DeployStatusCollectJobHandler |
| CM-BAT-0001 | CampaignTargetExtractJobHandler |
| MG-BAT-0001 | MessageSendResultCollectJobHandler |
| UD-BAT-0001 | ExpiredFileCleanupJobHandler |
| LOG-BAT-0001 | TransactionLogArchiveJobHandler |
| TCF-BAT-0001 | TransactionStatusCheckJobHandler |

### 14.2 금지 예시

| 금지 Class명 | 사유 | 표준 Class명 |
| --- | --- | --- |
| Batch001 | 의미 없음 | ApStatusCollectJobHandler |

JobService
대상·행위 불명확
CampaignTargetExtractJobHandler

| TestBatch | 임시명 | 정식 Job명 사용 |
| --- | --- | --- |
| CmBatch | 처리행위 없음 | CampaignTargetExtractJobHandler |

## 15. Scheduler Class 명명규칙

Scheduler는 실행 시점과 대상 Job을 결정하는 클래스다.
{대상}ScheduleRunner
{대상}ScheduleResolver
{대상}DueJobScanner

| Class명 | 역할 | BatchScheduleRunner |
| --- | --- | --- |
| 실행 대상 스케줄을 주기적으로 확인 | BatchScheduleResolver | Cron, FixedDelay, Manual 여부 해석 |

| BatchDueJobScanner | 실행 시점이 된 Job 조회 | BatchJobLauncher |
| --- | --- | --- |
| Job 실행 시작 | BatchExecutionFactory | ExecutionId 생성 및 실행 컨텍스트 생성 |

| BatchExecutionValidator | 실행 가능 여부 검증 | BatchLockService |
| --- | --- | --- |
| 중복 실행 Lock 관리 | JdbcBatchLockService | DB 기반 Lock 구현 |

기존 패키지 구조에서도 scheduler, launcher, registry, handler, step, lock, repository, model, web 계층으로 분리하는 구조가 제시되어 있다.

## 16. Package 명명규칙

com.nh.nsight.tcf.batch.{layer}
com.nh.nsight.{businessCodeLower}.batch.{layer}

| 패키지 | 설명 |
| --- | --- |
| com.nh.nsight.tcf.batch.scheduler | 스케줄 실행 판단 |
| com.nh.nsight.tcf.batch.launcher | Job 실행 |
| com.nh.nsight.tcf.batch.registry | Job Handler Registry |
| com.nh.nsight.tcf.batch.handler | 공통 Job Handler |
| com.nh.nsight.tcf.batch.step | Step 실행 |
| com.nh.nsight.tcf.batch.lock | 중복 실행 Lock |
| com.nh.nsight.tcf.batch.repository | Job/Execution/Lock Repository |
| com.nh.nsight.tcf.batch.model | BatchContext, BatchResult |
| com.nh.nsight.cm.batch.handler | CM 업무 배치 Handler |
| com.nh.nsight.ud.batch.handler | UD 파일 배치 Handler |

## 17. Batch 테이블 명명규칙

OM에서 관리하는 기준정보 테이블은 OM_BATCH_ Prefix를 사용하고, Framework 공통 실행 테이블은 TCF_BATCH_ Prefix를 사용할 수 있다. NSIGHT 운영관리 관점에서는 OM이 통제 지점이므로 관리 테이블은 OM_BATCH_를 우선 권장한다.

| 테이블명 | 설명 |
| --- | --- |
| OM_BATCH_JOB | Batch Job 마스터 |
| OM_BATCH_SCHEDULE | Batch 스케줄 마스터 |
| OM_BATCH_EXECUTION | Job 실행 이력 |
| OM_BATCH_STEP_EXECUTION | Step 실행 이력 |
| OM_BATCH_LOCK | 중복 실행 Lock |
| OM_BATCH_DEPENDENCY | Job 선후행 관계 |
| OM_BATCH_PARAMETER | Job 실행 파라미터 |
| OM_BATCH_RETRY_POLICY | 재처리 정책 |
| OM_BATCH_ALERT_POLICY | 실패 알림 정책 |
| OM_BATCH_CHANGE_HISTORY | Job/스케줄 변경 이력 |

## 18. 주요 컬럼 명명규칙

| 컬럼명 | 설명 |
| --- | --- |
| JOB_ID | 배치 Job ID |
| JOB_NAME | 배치 Job명 |
| JOB_TYPE | COLLECT, EXTRACT, ARCHIVE, CLEANUP, RETRY |
| OWNER_CODE | 업무코드 또는 공통영역 코드 |
| SCHEDULE_ID | 스케줄 ID |
| SCHEDULE_TYPE | CRON, FIXED_DELAY, MANUAL_ONLY |
| CRON_EXPRESSION | Cron 표현식 |
| EXECUTION_ID | 실행 ID |
| STEP_ID | Step ID |
| BUSINESS_DATE | 업무 기준일자 |
| RUN_TYPE | SCHEDULED, MANUAL, RETRY |
| STATUS | 실행 상태 |
| READ_COUNT | 읽은 건수 |
| WRITE_COUNT | 처리 건수 |
| SKIP_COUNT | Skip 건수 |
| ERROR_COUNT | 오류 건수 |
| TIMEOUT_SECONDS | Timeout 기준 |
| RETRY_YN | 재처리 가능 여부 |
| MAX_RETRY_COUNT | 최대 재시도 횟수 |
| LOCK_ID | Lock ID |
| LOCK_OWNER | Lock 획득 인스턴스 |

LOCK_EXPIRE_DTM
Lock 만료일시

| GUID | 추적 GUID |
| --- | --- |
| TRACE_ID | Trace ID |

## 19. Batch JobType 명명규칙

JobType
| 의미 | 예시 |
| --- | --- |
| COLLECT | 상태/결과 수집 |
| AP 상태, DB 상태, 발송결과 수집 | EXTRACT |
| 대상 추출 | 캠페인 대상 추출 |
| AGGREGATE | 집계 |
| 일별 통계 집계 | ARCHIVE |
| 보관 | 거래로그 아카이브 |
| CLEANUP | 정리/삭제 |
| 만료 파일 삭제 | RETRY |
| 재처리 | 실패 메시지 재발송 |
| CHECK | 상태 확인 |
| UNKNOWN 거래 상태 확인 | SYNC |
| 동기화 | 기준정보 동기화 |
| REPORT | 리포트 생성 |
| 운영 리포트 생성 | IF |
| 인터페이스 | 외부 파일 송수신 |

## 20. Batch 로그 명명규칙

### 20.1 로그 파일명

nsight-{envCode}-bt-{jobId}.log

예시:

| 로그 파일명 | 설명 |
| --- | --- |
| nsight-prd-bt-OM-BAT-0001.log | 운영 AP 상태 수집 로그 |
| nsight-prd-bt-CM-BAT-0001.log | 운영 캠페인 대상 추출 로그 |
| nsight-prd-bt-UD-BAT-0001.log | 운영 파일 정리 로그 |
| nsight-stg-bt-LOG-BAT-0001.log | 검증 로그 아카이브 로그 |

### 20.2 로그 이벤트 코드

| 이벤트 코드 | 의미 |
| --- | --- |
| BATCH-START | Batch 시작 |
| BATCH-END | Batch 종료 |
| BATCH-FAIL | Batch 실패 |
| BATCH-TIMEOUT | Batch Timeout |
| BATCH-SKIP | 선행조건 미충족 또는 Lock으로 Skip |
| BATCH-RETRY | 재처리 시작 |
| BATCH-CANCEL | 운영자 강제 중지 |
| BATCH-LOCK-ACQUIRE | Lock 획득 |
| BATCH-LOCK-FAIL | Lock 획득 실패 |
| BATCH-LOCK-RELEASE | Lock 해제 |

## 21. Batch 오류코드 명명규칙

E-BAT-{CATEGORY}-{NNNN}

| 오류코드 | 의미 |
| --- | --- |
| E-BAT-JOB-0001 | Job 미등록 |
| E-BAT-JOB-0002 | Job 사용 중지 |
| E-BAT-SCH-0001 | 스케줄 미등록 |
| E-BAT-SCH-0002 | Cron 표현식 오류 |
| E-BAT-LOCK-0001 | Lock 획득 실패 |
| E-BAT-LOCK-0002 | Lock 잔류 |
| E-BAT-EXEC-0001 | 실행 이력 생성 실패 |
| E-BAT-EXEC-0002 | 실행 중 오류 |
| E-BAT-TIME-0001 | Job Timeout |
| E-BAT-STEP-0001 | Step 실패 |
| E-BAT-RETRY-0001 | 재처리 불가 상태 |
| E-BAT-DB-0001 | Batch DB 오류 |

## 22. Batch 설정 Key 명명규칙

Spring 환경설정에서는 다음 Key 체계를 사용한다.
```yaml
nsight:
  batch:
    enabled: true
    instance-id: ${HOSTNAME:local}
    db-lock-enabled: true
    scheduler:
      enabled: true
```

| 설정 Key | 설명 |
| --- | --- |
| nsight.batch.enabled | Batch 기능 활성화 |
| nsight.batch.instance-id | Batch 인스턴스 식별 |
| nsight.batch.db-lock-enabled | DB Lock 사용 여부 |
| nsight.batch.scheduler.enabled | Scheduler 활성화 |
| nsight.batch.execution.pool.core-size | 실행 Thread Core 수 |
| nsight.batch.execution.pool.max-size | 실행 Thread Max 수 |
| nsight.batch.execution.queue-capacity | 실행 대기 Queue |
| nsight.batch.default-timeout-seconds | 기본 Timeout |
| nsight.batch.retry.default-max-count | 기본 재시도 횟수 |

Spring 설정 기준에서도 spring.task.scheduling.pool.size, spring.task.execution.pool.core-size, nsight.batch.enabled, nsight.batch.instance-id, nsight.batch.db-lock-enabled 등을 Batch/Scheduler 설정 항목으로 둔다.

## 23. 대표 Job 명명 예시

| 영역 | JobId | JobName |
| --- | --- | --- |
| Handler | OM | OM-BAT-0001 |
| AP 상태 수집 배치 | ApStatusCollectJobHandler | OM |
| OM-BAT-0002 | DB 상태 수집 배치 | DbStatusCollectJobHandler |
| OM | OM-BAT-0003 | 세션 현황 수집 배치 |
| SessionStatusCollectJobHandler | OM | OM-BAT-0004 |
| 배포 상태 수집 배치 | DeployStatusCollectJobHandler | CM |
| CM-BAT-0001 | 캠페인 대상 추출 배치 | CampaignTargetExtractJobHandler |
| MG | MG-BAT-0001 | 메시지 발송결과 수집 배치 |
| MessageSendResultCollectJobHandler | UD | UD-BAT-0001 |
| 만료 파일 정리 배치 | ExpiredFileCleanupJobHandler | LOG |
| LOG-BAT-0001 | 거래로그 아카이브 배치 | TransactionLogArchiveJobHandler |
| TCF | TCF-BAT-0001 | UNKNOWN 거래상태 확인 배치 |

TransactionStatusCheckJobHandler

## 24. 금지 명명규칙

| 금지 예시 | 문제 | 표준 예시 |
| --- | --- | --- |
| BATCH001 | 업무·처리영역 식별 불가 | OM-BAT-0001 |
| JOB1 | 의미 없음 | CM-BAT-0001 |
| TEST_BATCH | 운영 이력 혼선 | 정식 JobId 채번 |
| CM001 | Batch 여부 불명확 | CM-BAT-0001 |
| AP_COLLECT | 업무코드·일련번호 없음 | OM-BAT-0001 |
| SCHEDULE1 | 어떤 Job 스케줄인지 알 수 없음 | SCH-PRD-OM-BAT-0001 |

LOCK1
중복 실행 대상 식별 불가
LOCK-OM-BAT-0001-20260705
BatchService
처리대상·행위 불명확
ApStatusCollectJobHandler
run()만 있는 배치
이력·재처리 추적 어려움
BatchJobHandler.execute(context)
운영에서 TEST RunType 사용
감사·운영 혼선
SCHEDULED, MANUAL, RETRY

## 25. 검토 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| JobId가 {OWNER_CODE}-BAT-{NNNN} 형식인가? | □ |
| ScheduleId가 SCH-{ENV_CODE}-{JOB_ID} 형식인가? | □ |
| ExecutionId가 실행 1건을 유일하게 식별하는가? | □ |
| StepId가 {JOB_ID}-STEP-{NN} 형식인가? | □ |
| LockId가 LOCK-{JOB_ID}-{BUSINESS_DATE} 형식인가? | □ |
| Batch와 Scheduler의 책임이 분리되어 있는가? | □ |
| 수동 실행, 재실행, 중지는 OM 권한으로 통제되는가? | □ |
| 실행 이력이 OM_BATCH_EXECUTION에 남는가? | □ |
| 주요 Step 이력이 OM_BATCH_STEP_EXECUTION에 남는가? | □ |
| 다중 실행 방지를 위한 OM_BATCH_LOCK이 적용되는가? | □ |
| RUN_TYPE이 SCHEDULED, MANUAL, RETRY 등으로 구분되는가? | □ |
| FAIL, TIMEOUT, UNKNOWN 상태만 재처리 대상인가? | □ |
| Job / Step / SQL Timeout 기준이 있는가? | □ |
| Batch 로그에 jobId, executionId, guid, traceId가 남는가? | □ |
| 수동 실행, 강제 중지, 재실행, Lock 해제가 감사로그에 남는가? | □ |

## 26. 마무리말

Batch / Scheduler 명명규칙의 핵심은 다음과 같다.
Scheduler는 시간을 식별하고,
Batch Job은 처리 업무를 식별하며,
Execution은 실행 1건을 식별하고,
Step은 처리 단계를 식별하며,
Lock은 중복 실행을 통제한다.

따라서 NSIGHT에서는 다음 네 가지 이름을 반드시 표준화해야 한다.
JOB_ID       = {OWNER_CODE}-BAT-{NNNN}
SCHEDULE_ID  = SCH-{ENV_CODE}-{JOB_ID}
EXECUTION_ID = EXE-{yyyyMMdd}-{JOB_ID}-{NNNNNN}
LOCK_ID      = LOCK-{JOB_ID}-{BUSINESS_DATE}

이 기준을 적용하면 운영자는 OM 배치관리 화면에서 어떤 Job이 언제, 어떤 기준일자로, 어느 인스턴스에서, 어떤 결과로 실행되었는지 즉시 추적할 수 있다. 배치의 목적은 자동 실행이 아니라 운영 통제, 실패 복구, 중복 방지, 감사 가능성 확보이다.

---

## 관련 Manual 장

- [45장](./45-Batch-Scheduler-개발.md)

## 원본

- [`znsight-guide-word`](../znsight-guide-word/) — `명명규칙 상세 (19).docx`
