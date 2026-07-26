# ic-service — Individual Customer

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `ic-service` |
| 업무코드 | `IC` |
| 메인 클래스 | `com.nh.nsight.marketing.ic.NsightIcServiceApplication` |
| bootRun 포트 | **8082** |
| WAR (bootWar) | `ic.war` |
| Tomcat context | `/ic` |

## 개요

NSIGHT 마케팅 플랫폼 **Individual Customer (IC)** 업무 서비스입니다. 모든 거래는 TCF 파이프라인(`POST /online` 또는 `POST /ic/online`)을 통해 처리됩니다.

## 샘플 거래

| serviceId | 설명 |
|-----------|------|
| `IC.Sample.inquiry` | 샘플 조회 |

## 실행

```bash
gradle :ic-service:bootRun
tcf-scripts/run-local.bat ic
```

## API

```bash
# bootRun
curl -X POST http://localhost:8082/ic/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/ic-sample-inquiry.json

# ztomcat
curl -X POST http://localhost:8080/ic/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/ic-sample-inquiry.json
```

배포: `ztomcat/deploy-wars.bat ic` — [ztomcat/README.md](../ztomcat/README.md)

## tcf-ui

| 모드 | URL |
|------|-----|
| bootRun | http://localhost:8099/ic/index.html |
| ztomcat | http://localhost:8080/ui/ic/index.html |

## 의존성

`tcf-util`, `tcf-core`, `tcf-web`

## 패키지 구조

```text
com.nh.nsight.marketing.ic
├── NsightIcServiceApplication       # extends NsightWarBootstrap
├── application/
│   ├── service/       IcSampleService
│   └── rule/          IcSampleRule
├── config/
├── entry/
│   ├── handler/       IcSampleHandler (도메인당 1개)
│   └── facade/        IcSampleFacade
└── persistence/
    ├── dao/           IcSampleDao
    └── mapper/        IcSampleMapper

처리 흐름: entry/handler → entry/facade → application/service → application/rule → persistence
```