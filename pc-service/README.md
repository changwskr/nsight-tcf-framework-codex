# pc-service — Private Customer (고객)

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `pc-service` |
| 업무코드 | `PC` |
| 메인 클래스 | `com.nh.nsight.marketing.pc.NsightPcServiceApplication` |
| bootRun 포트 | **8083** |
| WAR (bootWar) | `pc.war` |
| Tomcat context | `/pc` |

## 개요

NSIGHT 마케팅 플랫폼 **Private Customer (PC)** 업무 서비스입니다. 모든 거래는 TCF 파이프라인(`POST /online` 또는 `POST /pc/online`)을 통해 처리됩니다.

## 샘플 거래

| serviceId | 설명 |
|-----------|------|
| `PC.Sample.inquiry` | 샘플 조회 |

## 실행

```bash
gradle :pc-service:bootRun
tcf-scripts/run-local.bat pc
```

## API

```bash
# bootRun
curl -X POST http://localhost:8083/pc/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/pc-sample-inquiry.json

# ztomcat
curl -X POST http://localhost:8080/pc/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/pc-sample-inquiry.json
```

배포: `ztomcat/deploy-wars.bat pc` — [ztomcat/README.md](../ztomcat/README.md)

## tcf-ui

| 모드 | URL |
|------|-----|
| bootRun | http://localhost:8099/pc/index.html |
| ztomcat | http://localhost:8080/ui/pc/index.html |

## 의존성

`tcf-util`, `tcf-core`, `tcf-web`

## 패키지 구조

```text
com.nh.nsight.marketing.pc
├── NsightPcServiceApplication       # extends NsightWarBootstrap
├── application/
│   ├── service/       PcSampleService
│   └── rule/          PcSampleRule
├── config/
├── entry/
│   ├── handler/       PcSampleHandler (도메인당 1개)
│   └── facade/        PcSampleFacade
└── persistence/
    ├── dao/           PcSampleDao
    └── mapper/        PcSampleMapper

처리 흐름: entry/handler → entry/facade → application/service → application/rule → persistence
```