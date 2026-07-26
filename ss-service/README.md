# ss-service — Self Service

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `ss-service` |
| 업무코드 | `SS` |
| 메인 클래스 | `com.nh.nsight.marketing.ss.NsightSsServiceApplication` |
| bootRun 포트 | **8093** |
| WAR (bootWar) | `ss.war` |
| Tomcat context | `/ss` |

## 개요

NSIGHT 마케팅 플랫폼 **Self Service (SS)** 업무 서비스입니다. 모든 거래는 TCF 파이프라인(`POST /online` 또는 `POST /ss/online`)을 통해 처리됩니다.

## 샘플 거래

| serviceId | 설명 |
|-----------|------|
| `SS.Sample.inquiry` | 샘플 조회 |

## 실행

```bash
gradle :ss-service:bootRun
tcf-scripts/run-local.bat ss
```

## API

```bash
# bootRun
curl -X POST http://localhost:8093/ss/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/ss-sample-inquiry.json

# ztomcat
curl -X POST http://localhost:8080/ss/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/ss-sample-inquiry.json
```

배포: `ztomcat/deploy-wars.bat ss` — [ztomcat/README.md](../ztomcat/README.md)

## tcf-ui

| 모드 | URL |
|------|-----|
| bootRun | http://localhost:8099/ss/index.html |
| ztomcat | http://localhost:8080/ui/ss/index.html |

## 의존성

`tcf-util`, `tcf-core`, `tcf-web`

## 패키지 구조

```text
com.nh.nsight.marketing.ss
├── NsightSsServiceApplication       # extends NsightWarBootstrap
├── application/
│   ├── service/       SsSampleService
│   └── rule/          SsSampleRule
├── config/
├── entry/
│   ├── handler/       SsSampleHandler (도메인당 1개)
│   └── facade/        SsSampleFacade
└── persistence/
    ├── dao/           SsSampleDao
    └── mapper/        SsSampleMapper

처리 흐름: entry/handler → entry/facade → application/service → application/rule → persistence
```