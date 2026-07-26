# 76. Batch Job 샘플

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 76. Batch Job 샘플

### 76.1 도입 전 안내말

NSIGHT에서 Batch Job은 단순히 @Scheduled로 메서드 하나를 주기 실행하는 기능이 아니다.Batch는 운영자가 등록·중지·재실행·이력조회·실패분석을 할 수 있어야 하는 운영 통제 대상이다.
NSIGHT 배치·스케줄러 기준에서는 다음처럼 역할을 분리한다.
Scheduler = 언제 실행할 것인가를 판단
Batch Job = 무엇을 처리할 것인가를 수행
OM        = 등록, 실행, 중지, 이력, 재처리 관리

현재 tcf-batch 기준으로는 BAT-BATCH-001 AP 상태 수집, BAT-BATCH-002 DB 상태 수집, BAT-BATCH-003 세션 현황 수집, BAT-BATCH-004 배포 현황 수집 Job을 우선 배치로 정의할 수 있다.

### 76.2 샘플 Batch Job 정의

이번 샘플은 AP 상태 수집 Batch Job으로 잡는다.
| 항목 | 내용 |
| --- | --- |
| 모듈 | tcf-batch |
| Job ID | BAT-BATCH-001 |
| Job명 | AP 상태 수집 |
| 목적 | OM 대시보드에 표시할 AP 상태 수집 |
| 실행 주기 | 5분 |
| 실행 방식 | Scheduler 자동 실행 + OM 수동 실행 |
| 결과 저장 | OM_AP_STATUS |
| 실행 이력 | TCF_BATCH_EXECUTION |
| 중복 실행 방지 | TCF_BATCH_LOCK |
| Timeout | 300초 |
| 상태 | SUCCESS, FAIL, TIMEOUT, UNKNOWN |

### 76.3 전체 처리 흐름

```text
[BatchScheduleRunner]
        ↓
```

## 1. 실행 대상 스케줄 조회

```text
        ↓
```

## 2. Job 사용 여부 확인

```text
        ↓
```

## 3. 중복 실행 Lock 획득

```text
        ↓
```

## 4. Batch Execution 생성

```text
        ↓
```

## 5. BatchJobHandler 실행

```text
        ↓
```

## 6. AP 상태 수집

```text
        ↓
```

## 7. OM_AP_STATUS 저장

```text
        ↓
```

## 8. Execution 결과 갱신

```text
        ↓
```

## 9. Lock 해제

```text
        ↓
```

## 10. OM 대시보드 반영

핵심은 다음이다.
Batch Job은 반드시 실행 이력을 먼저 만들고, 실행 후 성공·실패·Timeout 상태를 남긴다.

### 76.4 패키지 구조

tcf-batch
```text
└─ src/main/java/com/nh/nsight/tcf/batch
   ├─ scheduler
   │   └─ BatchScheduleRunner
   │
   ├─ launcher
   │   └─ BatchJobLauncher
   │
   ├─ registry
   │   └─ BatchJobHandlerRegistry
   │
   ├─ handler
   │   ├─ BatchJobHandler
   │   └─ ApStatusCollectJobHandler
   │
   ├─ lock
   │   └─ BatchLockService
   │
   ├─ repository
   │   ├─ BatchExecutionRepository
   │   └─ ApStatusRepository
   │
   ├─ model
   │   ├─ BatchContext
   │   ├─ BatchResult
   │   └─ BatchStatus
   │
   └─ web
       └─ BatchJobController
```

### 76.5 BatchJobHandler 인터페이스

모든 Batch Job은 같은 인터페이스를 구현한다.
```java
public interface BatchJobHandler {
    /**
     * Job ID를 반환한다.
     * 예: BAT-BATCH-001
     */
    String jobId();
    /**
     * Batch Job을 실행한다.
     */
    BatchResult execute(BatchContext context);
}
```

### 76.6 BatchContext

@Getter
@Builder
```java
public class BatchContext {
    private String executionId;
    private String jobId;
    /**
     * SCHEDULED, MANUAL, RETRY
     */
    private String runType;
    /**
     * 기준일자
     * 예: 20260705
     */
    private String businessDate;
    private Map<String, Object> parameters;
    private String requestedBy;
    private String guid;
    private String traceId;
    private LocalDateTime startTime;
}
```

### 76.7 BatchResult

@Getter
@Builder
```java
public class BatchResult {
    /**
     * SUCCESS, FAIL, TIMEOUT, UNKNOWN
     */
    private String status;
    private int readCount;
    private int writeCount;
    private int skipCount;
    private int errorCount;
    private String message;
    public static BatchResult success(int readCount, int writeCount, String message) {
        return BatchResult.builder()
                .status("SUCCESS")
                .readCount(readCount)
                .writeCount(writeCount)
                .skipCount(0)
                .errorCount(0)
                .message(message)
                .build();
    }
    public static BatchResult fail(int readCount, int errorCount, String message) {
        return BatchResult.builder()
                .status("FAIL")
                .readCount(readCount)
                .writeCount(0)
                .skipCount(0)
                .errorCount(errorCount)
                .message(message)
                .build();
    }
}
```

### 76.8 AP 상태 수집 Job Handler 샘플

```java
@Component
@RequiredArgsConstructor
public class ApStatusCollectJobHandler implements BatchJobHandler {
    private static final String JOB_ID = "BAT-BATCH-001";
    private final ApStatusClient apStatusClient;
    private final ApStatusRepository apStatusRepository;
    @Override
    public String jobId() {
        return JOB_ID;
    }
    @Override
    public BatchResult execute(BatchContext context) {
        List<ApStatusTarget> targets =
                apStatusRepository.selectApStatusTargets();
        int readCount = 0;
        int writeCount = 0;
        int errorCount = 0;
        for (ApStatusTarget target : targets) {
            readCount++;
            try {
                ApStatusSnapshot snapshot =
                        apStatusClient.collect(target);
                apStatusRepository.mergeApStatus(snapshot);
                writeCount++;
            } catch (Exception e) {
                errorCount++;
                apStatusRepository.insertApStatusError(
                        context.getExecutionId(),
                        target.getApId(),
                        e.getClass().getSimpleName(),
                        e.getMessage()
                );
            }
        }
        if (errorCount > 0 && writeCount == 0) {
            return BatchResult.fail(
                    readCount,
                    errorCount,
                    "AP 상태 수집이 모두 실패했습니다."
            );
        }
        return BatchResult.success(
                readCount,
                writeCount,
                "AP 상태 수집이 완료되었습니다."
        );
    }
}
```

### 76.9 AP 상태 수집 Client 샘플

```java
@Component
@RequiredArgsConstructor
public class ApStatusClient {
    private final WebClient.Builder webClientBuilder;
    public ApStatusSnapshot collect(ApStatusTarget target) {
        WebClient webClient = webClientBuilder
                .baseUrl(target.getBaseUrl())
                .build();
        HealthResponse health = webClient.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(HealthResponse.class)
                .timeout(Duration.ofSeconds(5))
                .block();
        if (health == null) {
            throw new SystemException(
                    "E-BAT-AP-0001",
                    "AP Health 응답이 없습니다."
            );
        }
        return ApStatusSnapshot.builder()
                .apId(target.getApId())
                .hostName(target.getHostName())
                .baseUrl(target.getBaseUrl())
                .healthStatus(health.getStatus())
                .collectTime(LocalDateTime.now())
                .build();
    }
}
```

운영 대시보드 기준으로 AP 상태, DB 상태, 세션 상태, 배포 상태는 주요 지표로 관리되어야 한다. 기존 OM 운영 구조에서도 AP 상태, DB 상태, 세션 상태, 거래 상태, 배포 상태, Health 상태를 대시보드 주요 지표로 본다.

### 76.10 BatchJobHandlerRegistry

Job ID로 실행 Handler를 찾는다.
```java
@Component
public class BatchJobHandlerRegistry {
    private final Map<String, BatchJobHandler> handlerMap;
    public BatchJobHandlerRegistry(List<BatchJobHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        BatchJobHandler::jobId,
                        Function.identity()
                ));
    }
    public BatchJobHandler getHandler(String jobId) {
        BatchJobHandler handler = handlerMap.get(jobId);
        if (handler == null) {
            throw new BusinessException(
                    "E-BAT-JOB-0001",
                    "등록되지 않은 Batch Job입니다. jobId=" + jobId
            );
        }
        return handler;
    }
}
```

### 76.11 BatchJobLauncher 샘플

```java
@Component
@RequiredArgsConstructor
public class BatchJobLauncher {
    private final BatchJobHandlerRegistry handlerRegistry;
    private final BatchExecutionRepository executionRepository;
    private final BatchLockService batchLockService;
    public BatchResult launch(BatchLaunchRequest request) {
        String executionId = UUID.randomUUID().toString();
        String lockKey = request.getJobId() + ":" + request.getBusinessDate();
        boolean locked = batchLockService.tryLock(
                lockKey,
                request.getJobId(),
                Duration.ofSeconds(600)
        );
        if (!locked) {
            throw new BusinessException(
                    "E-BAT-LOCK-0001",
                    "이미 실행 중인 Batch Job입니다. jobId=" + request.getJobId()
            );
        }
        BatchContext context = BatchContext.builder()
                .executionId(executionId)
                .jobId(request.getJobId())
                .runType(request.getRunType())
                .businessDate(request.getBusinessDate())
                .parameters(request.getParameters())
                .requestedBy(request.getRequestedBy())
                .guid(UUID.randomUUID().toString())
                .traceId(UUID.randomUUID().toString())
                .startTime(LocalDateTime.now())
                .build();
        try {
            executionRepository.insertExecutionStart(context);
            BatchJobHandler handler =
                    handlerRegistry.getHandler(request.getJobId());
            BatchResult result = handler.execute(context);
            executionRepository.updateExecutionEnd(
                    executionId,
                    result.getStatus(),
                    result.getReadCount(),
                    result.getWriteCount(),
                    result.getSkipCount(),
                    result.getErrorCount(),
                    result.getMessage()
            );
            return result;
        } catch (Exception e) {
            executionRepository.updateExecutionFail(
                    executionId,
                    "FAIL",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            throw e;
        } finally {
            batchLockService.unlock(lockKey);
        }
    }
}
```

### 76.12 Batch Lock Service 샘플

다중 Batch 인스턴스 환경에서는 같은 Job이 동시에 실행되면 안 된다.
```java
@Service
@RequiredArgsConstructor
public class BatchLockService {
    private final BatchLockRepository batchLockRepository;
    private final BatchProperties batchProperties;
    public boolean tryLock(String lockKey, String jobId, Duration leaseTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockUntil = now.plus(leaseTime);
        int inserted = batchLockRepository.insertLockIfAbsent(
                lockKey,
                jobId,
                batchProperties.getInstanceId(),
                lockUntil
        );
        if (inserted == 1) {
            return true;
        }
        int updated = batchLockRepository.updateExpiredLock(
                lockKey,
                batchProperties.getInstanceId(),
                now,
                lockUntil
        );
        return updated == 1;
    }
    public void unlock(String lockKey) {
        batchLockRepository.deleteLock(
                lockKey,
                batchProperties.getInstanceId()
        );
    }
}
```

Batch Lock의 핵심 기준은 동일 Job + 동일 기준일자 + 동일 예정시각은 한 번만 실행하는 것이다.

### 76.13 Scheduler Runner 샘플

운영형 구조에서는 각 Job에 @Scheduled를 직접 붙이지 않고, 스케줄 조회 Runner만 주기 실행한다.
```java
@Component
@RequiredArgsConstructor
public class BatchScheduleRunner {
    private final BatchScheduleResolver scheduleResolver;
    private final BatchJobLauncher batchJobLauncher;
    @Scheduled(fixedDelayString = "${nsight.batch.scheduler.scan-interval-ms:30000}")
    public void scanAndRunDueJobs() {
        List<BatchSchedule> dueSchedules =
                scheduleResolver.findDueSchedules(LocalDateTime.now());
        for (BatchSchedule schedule : dueSchedules) {
            BatchLaunchRequest request = BatchLaunchRequest.builder()
                    .jobId(schedule.getJobId())
                    .scheduleId(schedule.getScheduleId())
                    .runType("SCHEDULED")
                    .businessDate(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE))
                    .requestedBy("SYSTEM")
                    .parameters(schedule.getParameters())
                    .build();
            batchJobLauncher.launch(request);
        }
    }
}
```

운영형 구조에서는 @Scheduled가 개별 Job을 직접 실행하지 않고 DB 스케줄을 스캔하는 Runner만 실행하는 방식이 더 좋다.

### 76.14 수동 실행 Controller 샘플

OM 화면에서 수동 실행할 때 사용한다.
@RestController
```java
@RequiredArgsConstructor
@RequestMapping("/batch/jobs")
public class BatchJobController {
    private final BatchJobLauncher batchJobLauncher;
    @PostMapping("/{jobId}/run")
    public BatchRunResponse runJob(
            @PathVariable String jobId,
            @RequestBody BatchManualRunRequest request
    ) {
        BatchLaunchRequest launchRequest = BatchLaunchRequest.builder()
                .jobId(jobId)
                .runType("MANUAL")
                .businessDate(request.getBusinessDate())
                .requestedBy(request.getRequestedBy())
                .parameters(request.getParameters())
                .build();
        BatchResult result = batchJobLauncher.launch(launchRequest);
        return BatchRunResponse.builder()
                .jobId(jobId)
                .status(result.getStatus())
                .readCount(result.getReadCount())
                .writeCount(result.getWriteCount())
                .errorCount(result.getErrorCount())
                .message(result.getMessage())
                .build();
    }
}
```

수동 실행은 반드시 OM 권한자만 허용해야 한다.

| 실행 유형 | 실행자 | 감사로그 |
| --- | --- | --- |
| 자동 실행 | SYSTEM | 선택 |

| 수동 실행 | 운영자 | 필수 |
| --- | --- | --- |
| 실패 재처리 | 운영자 | 필수 |
| 강제 중지 | 운영자 | 필수 |

### 76.15 Repository 샘플

```java
@Repository
@RequiredArgsConstructor
public class BatchExecutionRepository {
    private final BatchExecutionMapper mapper;
    public void insertExecutionStart(BatchContext context) {
        mapper.insertExecutionStart(context);
    }
    public void updateExecutionEnd(
            String executionId,
            String status,
            int readCount,
            int writeCount,
            int skipCount,
            int errorCount,
            String message
    ) {
        mapper.updateExecutionEnd(
                executionId,
                status,
                readCount,
                writeCount,
                skipCount,
                errorCount,
                message
        );
    }
    public void updateExecutionFail(
            String executionId,
            String status,
            String errorCode,
            String errorMessage
    ) {
        mapper.updateExecutionFail(
                executionId,
                status,
                errorCode,
                errorMessage
        );
    }
}
```

### 76.16 Mapper 샘플

```java
@Mapper
public interface BatchExecutionMapper {
    int insertExecutionStart(BatchContext context);
    int updateExecutionEnd(
            @Param("executionId") String executionId,
            @Param("status") String status,
            @Param("readCount") int readCount,
            @Param("writeCount") int writeCount,
            @Param("skipCount") int skipCount,
            @Param("errorCount") int errorCount,
            @Param("message") String message
    );
    int updateExecutionFail(
            @Param("executionId") String executionId,
            @Param("status") String status,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );
}
```

### 76.17 MyBatis XML 샘플

<mapper namespace="com.nh.nsight.tcf.batch.repository.BatchExecutionMapper">

    <insert id="insertExecutionStart"
            parameterType="com.nh.nsight.tcf.batch.model.BatchContext">
        INSERT INTO TCF_BATCH_EXECUTION (
              EXECUTION_ID
            , JOB_ID
            , RUN_TYPE
            , BUSINESS_DATE
            , STATUS
            , START_TIME
            , REQUESTED_BY
            , GUID
            , TRACE_ID
        ) VALUES (
              #{executionId}
            , #{jobId}
            , #{runType}
            , #{businessDate}
            , 'RUNNING'
            , CURRENT_TIMESTAMP
            , #{requestedBy}
            , #{guid}
            , #{traceId}
        )
    </insert>

    <update id="updateExecutionEnd">
        UPDATE TCF_BATCH_EXECUTION
           SET STATUS      = #{status}
             , END_TIME    = CURRENT_TIMESTAMP
             , DURATION_MS = TIMESTAMPDIFF(
                               SQL_TSI_MILLISECOND,
                               START_TIME,
                               CURRENT_TIMESTAMP
                             )
             , READ_COUNT  = #{readCount}
             , WRITE_COUNT = #{writeCount}
             , SKIP_COUNT  = #{skipCount}
             , ERROR_COUNT = #{errorCount}
             , MESSAGE     = #{message}
         WHERE EXECUTION_ID = #{executionId}
    </update>

    <update id="updateExecutionFail">
        UPDATE TCF_BATCH_EXECUTION
           SET STATUS        = #{status}
             , END_TIME      = CURRENT_TIMESTAMP
             , ERROR_CODE    = #{errorCode}
             , ERROR_MESSAGE = #{errorMessage}
         WHERE EXECUTION_ID = #{executionId}
    </update>

</mapper>

### 76.18 AP 상태 저장 테이블 예시

CREATE TABLE OM_AP_STATUS (
    AP_ID           VARCHAR2(50)   NOT NULL,
    HOST_NAME       VARCHAR2(100)  NOT NULL,
    BASE_URL        VARCHAR2(300)  NOT NULL,
    HEALTH_STATUS   VARCHAR2(20)   NOT NULL,
    CPU_USAGE       NUMBER(5,2),
    MEMORY_USAGE    NUMBER(5,2),
    THREAD_USAGE    NUMBER(5,2),
    COLLECT_TIME    TIMESTAMP      NOT NULL,
    UPDATED_AT      TIMESTAMP      NOT NULL,
    CONSTRAINT PK_OM_AP_STATUS PRIMARY KEY (AP_ID)
);

CREATE TABLE TCF_BATCH_EXECUTION (
    EXECUTION_ID    VARCHAR2(50)   NOT NULL,
    JOB_ID          VARCHAR2(50)   NOT NULL,
    SCHEDULE_ID     VARCHAR2(50),
    RUN_TYPE        VARCHAR2(20)   NOT NULL,
    BUSINESS_DATE   VARCHAR2(8),
    STATUS          VARCHAR2(20)   NOT NULL,
    START_TIME      TIMESTAMP      NOT NULL,
    END_TIME        TIMESTAMP,
    DURATION_MS     NUMBER,
    READ_COUNT      NUMBER,
    WRITE_COUNT     NUMBER,
    SKIP_COUNT      NUMBER,
    ERROR_COUNT     NUMBER,
    REQUESTED_BY    VARCHAR2(50),
    GUID            VARCHAR2(100),
    TRACE_ID        VARCHAR2(100),
    ERROR_CODE      VARCHAR2(50),
    ERROR_MESSAGE   VARCHAR2(1000),
    MESSAGE         VARCHAR2(1000),
    CONSTRAINT PK_TCF_BATCH_EXECUTION PRIMARY KEY (EXECUTION_ID)
);

### 76.19 application.yml 샘플

```yaml
server:
  port: 8098
  servlet:
    context-path: /batch
spring:
  application:
    name: tcf-batch
  task:
    scheduling:
      pool:
        size: 4
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 100
nsight:
  batch:
    enabled: true
    instance-id: ${HOSTNAME:local-batch-01}
    scheduler:
      enabled: true
      scan-interval-ms: 30000
      timezone: Asia/Seoul
    execution:
      max-concurrent-jobs: 8
      default-timeout-sec: 300
      fail-on-timeout: true
      history-retention-days: 90
    lock:
      enabled: true
      timeout-sec: 600
    ap-status:
      job-id: BAT-BATCH-001
      cron: "0 */5 * * * *"
      connect-timeout-ms: 3000
      read-timeout-ms: 5000
```

Batch 설정은 spring.task.scheduling.pool.size, spring.task.execution.pool.core-size, nsight.batch.enabled, nsight.batch.instance-id, nsight.batch.db-lock-enabled 등을 기준으로 관리한다.

### 76.20 오류코드 기준

| 오류 상황 | 오류코드 | 설명 |
| --- | --- | --- |
| Job 미등록 | E-BAT-JOB-0001 | 등록되지 않은 Job ID |
| Job 사용 중지 | E-BAT-JOB-0002 | 비활성 Job 실행 요청 |
| Lock 획득 실패 | E-BAT-LOCK-0001 | 동일 Job 실행 중 |
| Handler 실행 실패 | E-BAT-RUN-0001 | Job Handler 처리 오류 |
| Timeout | E-BAT-TIME-0001 | Job 제한시간 초과 |
| AP Health 실패 | E-BAT-AP-0001 | AP 상태 수집 실패 |
| 실행 이력 저장 실패 | E-BAT-LOG-0001 | Execution 기록 실패 |

### 76.21 운영 점검 기준

| 점검 항목 | 확인 기준 | Scheduler 활성화 |
| --- | --- | --- |
| nsight.batch.scheduler.enabled=true | Job 등록 | TCF_BATCH_JOB 등록 여부 |
| 스케줄 등록 | TCF_BATCH_SCHEDULE 등록 여부 | 중복 실행 방지 |
| TCF_BATCH_LOCK 정상 동작 | 실행 이력 | TCF_BATCH_EXECUTION 기록 여부 |
| Timeout | Job별 Timeout 설정 여부 | 수동 실행 권한 |
| OM 관리자만 실행 가능 | 실패 재처리 | FAIL, TIMEOUT, UNKNOWN만 재처리 |
| 대시보드 반영 | OM_AP_STATUS 정상 갱신 | 감사로그 |

수동 실행·중지·재처리 이력 기록

### 76.22 마무리말

Batch Job 샘플의 핵심은 다음이다.
Batch Job
= Job ID
+ Handler
+ Execution
+ Lock
+ Result
+ OM 이력

따라서 Batch Job은 단순히 주기적으로 실행되는 Java 메서드가 아니라, 운영자가 추적 가능한 실행 단위로 설계해야 한다.
최종 기준은 다음과 같다.
스케줄러는 실행 시점을 판단한다.
Job Launcher는 실행 이력과 Lock을 관리한다.
Job Handler는 실제 업무를 처리한다.
Execution Repository는 성공·실패·Timeout 결과를 남긴다.
OM은 등록·수동실행·중지·재처리를 통제한다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (70).docx`

| [배치관리.md](../zdoc/배치관리.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [75. 파일 다운로드 샘플](./75-파일-다운로드-샘플.md) · [77. 오류처리 샘플](./77-오류처리-샘플.md) →