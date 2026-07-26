# eb-service — Event Bridge

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `eb-service` |
| 업무코드 | `EB` |
| 메인 클래스 | `com.nh.nsight.marketing.eb.NsightEbServiceApplication` |
| bootRun 포트 | **8089** |
| WAR (bootWar) | `eb.war` |
| Tomcat context | `/eb` |

## 개요

NSIGHT 마케팅 플랫폼 **Event Bridge (EB)** 업무 서비스입니다. 모든 거래는 TCF 파이프라인(`POST /online` 또는 `POST /eb/online`)을 통해 처리됩니다.

## 거래

| serviceId | transactionCode | 설명 |
|-----------|-----------------|------|
| `EB.Sample.inquiry` | EB-INQ-0001 | 샘플 조회 |
| `EB.User.inquiry` | EB-USR-0002 | 사용자 목록 조회 (최근 이벤트 포함) |
| `EB.User.create` | EB-USR-0001 | 사용자 등록 + `EB_EVENT`(READY) 생성 |
| `EB.Event.inquiry` | EB-EVT-0001 | Outbox 이벤트 목록·상태 집계 조회 |
| `EB.Batch.inquiry` | EB-BAT-0001 | EP 발행 배치 설정·상태 집계 조회 |
| `EB.SystemTx.inquiry` | EB-STX-0001 | 시스템 거래 현황(화면 19410) |

## EB → EP 이벤트 발행 (Outbox)

```text
EB.User.create  →  EB_USER + EB_EVENT(READY)
@Scheduled 배치  →  POST ep-service /ep/online (EP.UserEvent.receive)
성공/실패        →  EB_EVENT = SENT / FAIL
```

| 테이블 | 역할 |
|--------|------|
| `EB_USER` | 사용자 정보 |
| `EB_EVENT` | EP 발행 대기 (`READY` / `SENT` / `FAIL`) |
| `EB_SYSTEM_TX` | 시스템 거래 현황(화면 19410) |

로컬 설정: `application-local.yml` → `nsight.eb.event-publish` (기본 60초, EP URL `8090`)

## 실행

```bash
gradle :eb-service:bootRun
tcf-scripts/run-local.bat eb
```

## API

```bash
# 사용자 등록 (이벤트 READY 생성)
curl -X POST http://localhost:8089/eb/online \
  -H "Content-Type: application/json" \
  -d "{\"header\":{\"businessCode\":\"EB\",\"serviceId\":\"EB.User.create\",\"transactionCode\":\"EB-USR-0001\",\"processingType\":\"CREATE\",\"channelId\":\"WEBTOP\"},\"body\":{\"userId\":\"U001\",\"userName\":\"홍길동\",\"branchId\":\"001\"}}"

# 샘플 조회
curl -X POST http://localhost:8089/eb/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/eb-sample-inquiry.json

# ztomcat
curl -X POST http://localhost:8080/eb/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/eb-sample-inquiry.json
```

배포: `ztomcat/deploy-wars.bat eb` — [ztomcat/README.md](../ztomcat/README.md)

## tcf-ui

| 모드 | URL |
|------|-----|
| bootRun | http://localhost:8099/eb/index.html |
| ztomcat | http://localhost:8080/ui/eb/index.html |
| 시스템 거래 현황(19410) | http://localhost:8099/eb/system-tx-status.html |

## 의존성

`tcf-util`, `tcf-core`, `tcf-web`

## 패키지 구조

```text
com.nh.nsight.marketing.eb
├── NsightEbServiceApplication       # extends NsightWarBootstrap
├── application/
│   ├── service/       EbUserService, EbEventService, EbEventPublishService, EbBatchService, EbSystemTxService
│   ├── rule/          EbUserRule, EbEventRule, EbSampleRule, EbSystemTxRule
│   └── scheduler/     EbEventPublishScheduler (Outbox → EP 발행)
├── client/            EpOnlineClient (POST ep-service /ep/online)
├── config/            EbSchedulerConfiguration, EbEventPublishProperties
├── entry/
│   ├── handler/       EbSampleHandler, EbUserHandler, EbBatchHandler, EbEventHandler, EbSystemTxHandler
│   └── facade/        EbSampleFacade, EbUserFacade, EbEventFacade, EbBatchFacade, EbSystemTxFacade
├── persistence/
│   ├── dao/           EbUserDao, EbEventDao, EbSampleDao, EbSystemTxDao
│   └── mapper/        EbUserMapper, EbEventMapper, EbSampleMapper, EbSystemTxMapper
└── support/           EbEventStatus

처리 흐름: entry/handler → entry/facade → application/service → application/rule → persistence
```