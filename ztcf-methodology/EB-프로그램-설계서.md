# EB(Event Bridge) 프로그램 설계서

> 대상 모듈: `eb-service` (업무코드 **EB**) · 기준 소스: `eb-service/src/main/java/com/nh/nsight/marketing/eb/`
> 작성일: 2026-07-25 · 문서 위치: `ztcf-methodology/EB-프로그램-설계서.md`

---

## 1. 시스템 개요

| 항목 | 내용 |
|------|------|
| 모듈명 | `eb-service` — NSIGHT 마케팅 플랫폼 Event Bridge 업무 서비스 |
| 업무코드 | `EB` (`nsight.tcf.runtime.business-code=EB`) |
| 메인 클래스 | `com.nh.nsight.marketing.eb.NsightEbServiceApplication` |
| 실행 형태 | Spring Boot WAR (`eb.war`) · bootRun 포트 8089 · Tomcat context `/eb` |
| 거래 엔드포인트 | `POST /online` (bootRun) / `POST /eb/online` (ztomcat) — TCF 파이프라인 단일 진입 |
| DB | H2(local, MODE=Oracle) · MyBatis mapper XML (`classpath:/mapper/**/*.xml`) |
| 연계 시스템 | ep-service (`POST http://127.0.0.1:8090/ep/online`, `EP.UserEvent.receive`) — Outbox 이벤트 발행 |
| 트랜잭션 정책 | 온라인 거래 5초, DB 쿼리 3초 timeout · auto-commit=false |

## 2. 아키텍처(레이어) 설계

| 레이어 | 패키지 | 역할 | 규칙 |
|--------|--------|------|------|
| Handler | `entry.handler` | serviceId 라우팅 진입점, `TransactionHandler` 구현 | Service 도메인당 1개, switch로 serviceId 분기, 미지원 시 `SERVICE_NOT_FOUND` |
| Facade | `entry.facade` | 트랜잭션 경계(`@Transactional`), Map↔DTO 변환 | 조회 `readOnly=true, timeout=5`, 변경 `timeout=5` |
| Service | `application.service` | 업무 오케스트레이션 (Rule 검증 → DAO 호출 → Response 조립) | 비즈니스 흐름만 담당 |
| Rule | `application.rule` | 입력 검증·검색조건(Criteria) 생성·값 보정 | 위반 시 `BusinessException(BUSINESS_ERROR)` |
| DAO | `persistence.dao` | Mapper 위임 데이터 접근 | Mapper 인터페이스 1:1 위임 |
| Mapper | `persistence.mapper` + XML | MyBatis SQL 실행 | XML: `resources/mapper/eb/*.xml` |
| DTO | `application.dto`, `persistence.dto` | 요청/응답/Row 객체 | 요청은 `fromMap()`, 응답은 `toMap()` 제공 |
| Client | `client` | 외부 시스템 HTTP 연계 (`RestClient`) | EP 표준전문 조립·결과코드 판정 |
| Scheduler | `application.scheduler` | 배치 트리거 (`@Scheduled`) | 주기·건수는 프로퍼티로 제어 |
| Config/Support | `config`, `support` | 프로퍼티·스케줄링 설정·상수·DB 마이그레이션 | - |

### 2.1 처리 흐름(공통)

| 단계 | 컴포넌트 | 처리 |
|------|----------|------|
| 1 | TCF STF/Dispatcher | `POST /eb/online` 표준전문 수신 → header.serviceId로 Handler 라우팅 |
| 2 | Handler | serviceId switch → Facade 메서드 호출 |
| 3 | Facade | 트랜잭션 시작, body(Map) → Request DTO 변환 |
| 4 | Service | Rule 검증 → Criteria 생성 → DAO 조회/변경 → Response DTO 조립 |
| 5 | Facade | Response DTO → `toMap()` 반환 |
| 6 | TCF ETF | 표준응답(result + body) 생성·거래로그 기록 |

## 3. 거래(serviceId) 목록

| serviceId | transactionCode | 유형 | Handler | Facade 메서드 | 설명 |
|-----------|-----------------|------|---------|---------------|------|
| `EB.Sample.inquiry` | EB-INQ-0001 | 조회 | `EbSampleHandler` | `EbSampleFacade.inquiry` | 샘플 조회 |
| `EB.User.create` | EB-USR-0001 | 등록 | `EbUserHandler` | `EbUserFacade.create` | 사용자 등록 + Outbox 이벤트(READY) 생성 |
| `EB.User.inquiry` | EB-USR-0002 | 조회 | `EbUserHandler` | `EbUserFacade.inquiry` | 사용자 목록(최근 이벤트 포함) 페이징 조회 |
| `EB.Event.inquiry` | EB-EVT-0001 | 조회 | `EbEventHandler` | `EbEventFacade.inquiry` | Outbox 이벤트 목록·상태 집계 조회 |
| `EB.Batch.inquiry` | EB-BAT-0001 | 조회 | `EbBatchHandler` | `EbBatchFacade.inquiry` | EP 발행 배치 설정·집계 조회 |
| `EB.SystemTx.inquiry` | EB-STX-0001 | 조회 | `EbSystemTxHandler` | `EbSystemTxFacade.inquiry` | 시스템 거래 현황(화면 19410) 페이징 조회 |

## 4. 프로그램(클래스) 명세

### 4.1 Handler (entry.handler)

| 클래스 | 담당 serviceId | 의존성 | 비고 |
|--------|----------------|--------|------|
| `EbUserHandler` | `EB.User.create`, `EB.User.inquiry` | `EbUserFacade` | 도메인 내 다건 serviceId 처리 예시 |
| `EbEventHandler` | `EB.Event.inquiry` | `EbEventFacade` | - |
| `EbBatchHandler` | `EB.Batch.inquiry` | `EbBatchFacade` | - |
| `EbSystemTxHandler` | `EB.SystemTx.inquiry` | `EbSystemTxFacade` | 화면 19410 전용 |
| `EbSampleHandler` | `EB.Sample.inquiry` | `EbSampleFacade` | 템플릿/테스트용 |

### 4.2 Facade (entry.facade)

| 클래스 | 메서드 | 트랜잭션 속성 | 입력 → 출력 |
|--------|--------|---------------|-------------|
| `EbUserFacade` | `inquiry` | readOnly, timeout=5 | body Map → `UserInquiryRequest` → 응답 Map |
| `EbUserFacade` | `create` | timeout=5 | body Map → `UserCreateRequest` → 응답 Map |
| `EbEventFacade` | `inquiry` | readOnly, timeout=5 | body Map → `EventInquiryRequest` → 응답 Map |
| `EbBatchFacade` | `inquiry` | readOnly, timeout=5 | (body 미사용) → 응답 Map |
| `EbSystemTxFacade` | `inquiry` | readOnly, timeout=5 | body Map → `SystemTxInquiryRequest` → 응답 Map |
| `EbSampleFacade` | `inquiry` | readOnly, timeout=5 | body Map → `SampleInquiryRequest` → 응답 Map |

### 4.3 Service (application.service)

| 클래스 | 메서드 | 처리 로직 |
|--------|--------|-----------|
| `EbUserService` | `inquiry` | Rule 검증 → Criteria 생성 → `searchUsers`+`countUsers` → `UserInquiryResponse.of` |
| `EbUserService` | `create` | Rule 검증 → 중복 검사(`existsByUserId`, 중복 시 BUSINESS_ERROR) → `EB_USER` insert → GUID 이벤트ID 생성 → `EB_EVENT`(USER_CREATED/READY) insert → `UserCreateResponse.of` |
| `EbEventService` | `inquiry` | Rule 검증 → `searchEvents`+`countEvents`+`countEventsByStatus` → `EventInquiryResponse.of`(statusSummary 포함) |
| `EbBatchService` | `inquiry` | `EbEventPublishProperties` + `countEventsByStatus` → `BatchInquiryResponse.of` |
| `EbSystemTxService` | `inquiry` | Rule 검증 → Criteria 생성(일시 보정) → `search`+`count` → `SystemTxInquiryResponse.of`(rowNo 부여) |
| `EbSampleService` | `inquiry` | sampleKey 조회 → `SampleInquiryResponse` |
| `EbEventPublishService` | `publishReadyEvents` | enabled 확인 → READY 이벤트 batchSize건 조회 → 건별 EP 전송(`publishOne`) → 성공 SENT / 실패 FAIL 갱신 |

### 4.4 Rule (application.rule)

| 클래스 | 메서드 | 검증·보정 규칙 |
|--------|--------|----------------|
| `EbUserRule` | `validateCreate` / `validateInquiry` / `buildSearchCriteria` | 필수값(userId, userName) 검증, 페이징 기본값(pageNo=1, pageSize) 보정 |
| `EbSystemTxRule` | `validateInquiry` | pageSize 최대 100 초과 시 오류, pageNo 1 미만 오류 |
| `EbSystemTxRule` | `buildSearchCriteria` | 페이징 기본값(pageNo=1, pageSize=20)·offset 계산, 일시 보정(날짜만 → 00:00:00/23:59:59, `T` → 공백, 분까지 → `:00` 추가) |
| `EbSampleRule` | `validateInquiry` | sampleKey 검증 |

### 4.5 DAO / Mapper (persistence)

| DAO | Mapper (XML) | 주요 메서드 | SQL 요약 |
|-----|--------------|-------------|----------|
| `EbUserDao` | `EbUserMapper` (`EbUserMapper.xml`) | `insertUser` | `INSERT INTO EB_USER (USER_ID, USER_NAME, BRANCH_ID)` |
| | | `existsByUserId`(countByUserId) | `SELECT COUNT(1) FROM EB_USER WHERE USER_ID = ?` |
| | | `searchUsers` | `EB_USER` LEFT JOIN 최근 이벤트 1건(ROW_NUMBER OVER PARTITION BY USER_ID) · 동적 WHERE · CREATED_AT DESC · OFFSET/FETCH 페이징 |
| | | `countUsers` | 동일 WHERE 건수 |
| `EbEventDao` | `EbEventMapper` (`EbEventMapper.xml`) | `insertEvent` | `INSERT INTO EB_EVENT (EVENT_ID, USER_ID, EVENT_TYPE, EVENT_STATUS, RETRY_COUNT)` |
| | | `updateEventStatus` | 상태 갱신, `SENT`일 때만 `SENT_AT=CURRENT_TIMESTAMP` |
| | | `selectReadyEvents` | `EVENT_STATUS='READY'` CREATED_AT 오름차순 상위 N건 |
| | | `searchEvents` / `countEvents` | 동적 WHERE(eventId·userId LIKE, eventType·eventStatus =) · CREATED_AT DESC · 페이징 |
| | | `countEventsByStatus` | `GROUP BY EVENT_STATUS` 상태별 건수 |
| `EbSystemTxDao` | `EbSystemTxMapper` (`EbSystemTxMapper.xml`) | `search` | `EB_SYSTEM_TX` 동적 WHERE(기간 TIMESTAMP 비교, txType =, 나머지 LIKE) · REQUEST_AT DESC · 페이징 |
| | | `count` | 동일 WHERE 건수 |
| `EbSampleDao` | `EbSampleMapper` (`EbSampleMapper.xml`) | `selectSample` | 샘플 데이터 조회 |

### 4.6 Client / Scheduler / Config

| 클래스 | 구분 | 명세 |
|--------|------|------|
| `EpOnlineClient` | 연계 Client | `RestClient`로 EP 표준전문(header: GUID·일시 포함 + body: eventId/eventType/userId) POST 전송, 응답 `result.resultCode == "S0000"` 여부로 성공 판정 |
| `EbEventPublishScheduler` | 배치 스케줄러 | `@Scheduled(fixedDelayString=${nsight.eb.event-publish.fixed-delay-ms:60000})` → `publishReadyEvents()` 호출 |
| `EbSchedulerConfiguration` | 설정 | `@EnableScheduling` + `EbEventPublishProperties` 활성화 |
| `EbEventPublishProperties` | 프로퍼티 | prefix `nsight.eb.event-publish`: enabled(true) / fixedDelayMs(60000) / batchSize(50) / epOnlineUrl(`http://127.0.0.1:8090/ep/online`) |
| `EbEventStatus` | 상수 | 이벤트 상태 `READY`/`SENT`/`FAIL`, 유형 `USER_CREATED` |
| `EbDatabaseMigration` | 지원 | 로컬 스키마 초기화 지원(schema.sql, `spring.sql.init.mode=always`) |

## 5. DTO(전문) 설계

### 5.1 요청 DTO

| DTO | 거래 | 필드 | 비고 |
|-----|------|------|------|
| `UserCreateRequest` | EB.User.create | userId*, userName*, branchId | `fromMap()` trim 처리 |
| `UserInquiryRequest` | EB.User.inquiry | pageNo, pageSize, userId, userName, branchId | 빈 문자열 → null |
| `EventInquiryRequest` | EB.Event.inquiry | pageNo, pageSize, eventId, userId, eventType, eventStatus | - |
| `SystemTxInquiryRequest` | EB.SystemTx.inquiry | pageNo, pageSize, txDateFrom, txDateTo, txType, txSeqNo, empNo, screenId(=screenNo 허용), serviceId | `screenId`/`screenNo` 이중 키 지원 |
| `SampleInquiryRequest` | EB.Sample.inquiry | sampleKey | - |

### 5.2 응답 DTO

| DTO | 거래 | 필드(toMap) | 비고 |
|-----|------|-------------|------|
| `UserCreateResponse` | EB.User.create | businessCode, serviceId, guid, userId, userName, branchId, eventId, eventType, eventStatus | 생성된 Outbox 이벤트 정보 포함 |
| `UserInquiryResponse` | EB.User.inquiry | businessCode, serviceId, guid, rows[], totalCount, pageNo, pageSize | row에 최근 이벤트(eventId/eventType/eventStatus) 포함 |
| `EventInquiryResponse` | EB.Event.inquiry | businessCode, serviceId, guid, rows[], totalCount, pageNo, pageSize, statusSummary | statusSummary: 상태별 건수 Map |
| `BatchInquiryResponse` | EB.Batch.inquiry | enabled, fixedDelayMs, batchSize, schedulerClass/Method, epOnlineUrl, readyCount 등 + statusSummary | 배치 설정·집계 |
| `SystemTxInquiryResponse` | EB.SystemTx.inquiry | businessCode("EB"), serviceId, guid, screenNo("19410"), rows[], totalCount, pageNo, pageSize | rowNo 서버 계산 부여 |

### 5.3 Persistence Row DTO

| Row DTO | 테이블 | 필드 |
|---------|--------|------|
| `UserRow` / `UserInsertRow` | EB_USER | userId, userName, branchId, createdAt (+ 조인된 eventId/eventType/eventStatus) |
| `EventRow` / `EventInsertRow` | EB_EVENT | eventId, userId, eventType, eventStatus, retryCount, createdAt, sentAt |
| `EventStatusCountRow` | EB_EVENT | eventStatus, count (GROUP BY 집계) |
| `SystemTxRow` | EB_SYSTEM_TX | rowNo, txSeqNo, txDate, screenId, serviceId, globalId, requestAt, responseAt, elapsedSec, inputContent, empNo, branchCode, terminalIp, txType |
| `SampleRow` | (샘플) | sampleKey 등 |

## 6. 테이블 설계

| 테이블 | 역할 | 주요 컬럼 |
|--------|------|-----------|
| `EB_USER` | 사용자 정보 | USER_ID(PK), USER_NAME, BRANCH_ID, CREATED_AT |
| `EB_EVENT` | EP 발행 Outbox | EVENT_ID(PK), USER_ID, EVENT_TYPE, EVENT_STATUS(READY/SENT/FAIL), RETRY_COUNT, CREATED_AT, SENT_AT |
| `EB_SYSTEM_TX` | 시스템 거래 현황(19410) | TX_SEQ_NO, TX_DATE, SCREEN_ID, SERVICE_ID, GLOBAL_ID, REQUEST_AT, RESPONSE_AT, ELAPSED_SEC, INPUT_CONTENT, EMP_NO, BRANCH_CODE, TERMINAL_IP, TX_TYPE |

## 7. 배치 설계 — EB → EP 이벤트 발행 (Outbox 패턴)

| 단계 | 컴포넌트 | 처리 | 결과 |
|------|----------|------|------|
| 1 | `EbUserService.create` | 사용자 등록과 동일 트랜잭션으로 `EB_EVENT` READY 적재 | Outbox 기록 |
| 2 | `EbEventPublishScheduler` | fixedDelay(기본 60초)마다 tick 실행 | 배치 트리거 |
| 3 | `EbEventPublishService.publishReadyEvents` | enabled 확인 → READY 이벤트 최대 batchSize(50)건 조회 | 대상 선정 |
| 4 | `EpOnlineClient.sendUserEvent` | EP 표준전문 POST (`EP.UserEvent.receive`) | HTTP 연계 |
| 5 | `EbEventDao.updateEventStatus` | `resultCode=S0000` → SENT(SENT_AT 기록), 그 외/예외 → FAIL | 상태 확정 |

| 배치 예외 정책 | 내용 |
|----------------|------|
| 건별 실패 | 해당 이벤트만 FAIL 처리 후 다음 건 계속 진행 |
| 배치 전체 예외 | warn 로그 후 skip (다음 tick에서 재시도 가능 구조) |
| 비활성화 | `nsight.eb.event-publish.enabled=false` 시 즉시 종료 |

## 8. 오류 처리 설계

| 오류 상황 | ErrorCode | 발생 위치 | 메시지 예 |
|-----------|-----------|-----------|-----------|
| 미지원 serviceId | `SERVICE_NOT_FOUND` | 각 Handler default 분기 | "EbUserHandler 미지원 serviceId: ..." |
| 필수값 누락·규칙 위반 | `BUSINESS_ERROR` | Rule | "pageSize는 최대 100 입니다." |
| 사용자 중복 | `BUSINESS_ERROR` | `EbUserService.create` | "이미 등록된 사용자입니다: U001" |
| EP 연계 실패 | (거래 오류 아님) | `EbEventPublishService` | 이벤트 FAIL 상태 기록 + warn 로그 |

## 9. 환경 설정 요약

| 설정 키 | local 값 | 설명 |
|---------|----------|------|
| `server.port` | 8089 | bootRun 포트 |
| `spring.datasource.url` | `jdbc:h2:mem:nsight_eb;MODE=Oracle` | 업무 DB (schema.sql 자동 초기화) |
| `nsight.tcf.transaction-log-datasource.url` | `jdbc:h2:file:.../nsight_om` | 거래로그(OM) 별도 DB |
| `nsight.tcf.idempotency-enabled` | true | 중복 거래 방지 |
| `nsight.tcf.audit-enabled` / `transaction-log-enabled` | true | 감사·거래로그 |
| `nsight.tcf.runtime-slow-sql-threshold-ms` | 1000 | 슬로우 SQL 감지 |
| `nsight.eb.event-publish.*` | enabled=true, 60000ms, 50건, EP 8090 | Outbox 발행 배치 |
| `nsight.timeout.online-transaction-seconds` / `db-query-seconds` | 5 / 3 | 거래·쿼리 타임아웃 |
| `mybatis.configuration.default-statement-timeout` | 3 | MyBatis 문장 타임아웃 |
