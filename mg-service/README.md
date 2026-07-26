# mg-service — Marketing Gateway

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `mg-service` |
| 업무코드 | `MG` |
| 메인 클래스 | `com.nh.nsight.marketing.mg.NsightMgServiceApplication` |
| bootRun 포트 | **8096** |
| WAR (bootWar) | `mg.war` |
| Tomcat context | `/mg` |

## 개요

NSIGHT 마케팅 플랫폼 **Marketing Gateway (MG)** 업무 서비스입니다. 모든 거래는 TCF 파이프라인(`POST /online` 또는 `POST /mg/online`)을 통해 처리됩니다.

## 샘플 거래

| serviceId | 설명 |
|-----------|------|
| `MG.Sample.inquiry` | 샘플 조회 |

## 실행

```bash
gradle :mg-service:bootRun
tcf-scripts/run-local.bat mg
```

## API

```bash
# bootRun
curl -X POST http://localhost:8096/mg/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/mg-sample-inquiry.json

# ztomcat
curl -X POST http://localhost:8080/mg/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/mg-sample-inquiry.json
```

배포: `ztomcat/deploy-wars.bat mg` — [ztomcat/README.md](../ztomcat/README.md)

## tcf-ui

| 모드 | URL |
|------|-----|
| bootRun | http://localhost:8099/mg/index.html |
| ztomcat | http://localhost:8080/ui/mg/index.html |

## 의존성

`tcf-util`, `tcf-core`, `tcf-web`

## 패키지 구조

```text
com.nh.nsight.marketing.mg
├── NsightMgServiceApplication       # extends NsightWarBootstrap
├── application/
│   ├── service/       MgSampleService
│   └── rule/          MgSampleRule
├── config/
├── entry/
│   ├── handler/       MgSampleHandler (도메인당 1개)
│   └── facade/        MgSampleFacade
└── persistence/
    ├── dao/           MgSampleDao
    └── mapper/        MgSampleMapper

처리 흐름: entry/handler → entry/facade → application/service → application/rule → persistence
```