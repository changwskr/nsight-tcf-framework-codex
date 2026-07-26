# om-service — Operation Management (운영, 레거시)

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `om-service` |
| 업무코드 | `OM` |
| 메인 클래스 | `com.nh.nsight.marketing.om.NsightOmServiceApplication` |
| bootRun 포트 | **8097** |
| WAR (bootWar) | `om.war` |
| Tomcat context | `/om` (레거시 — **tcf-om 사용 권장**) |

## 개요

**운영관리(OM)** 업무 WAR 모듈입니다. TCF 마이그레이션 완료본은 **`tcf-om`** 모듈을 사용하세요.

> `buildBusinessWars`, `ztomcat/deploy-wars`는 **`tcf-om`** 만 빌드·배포합니다. `om-service` WAR는 파이프라인에 포함되지 않습니다.

> `tcf-om`과 동일 포트(8097)를 사용합니다. UD·OM admin 기능은 **tcf-om**에 통합되어 있습니다. 동시 기동 불가.

## 실행

`om-service`는 `settings.gradle`에는 포함되어 있지만 `buildBusinessWars`와 CI/CD 배포 대상에서는 제외된 레거시 모듈입니다. 신규 개발·배포에는 **`tcf-om`** 을 사용하세요.

```bash
gradle :tcf-om:bootRun
tcf-om/scripts/run-local.bat
```

`om-service/scripts/run-local.bat` 은 호환용으로 `:tcf-om:bootRun` 을 실행합니다.

## API

| Method | Path |
|--------|------|
| POST | `/online`, `/om/online` |

## tcf-om과의 관계

| 모듈 | 설명 |
|------|------|
| `tcf-om` | TCF 마이그레이션 완료본 (**권장**) — OM Admin 22 서비스, UD 내장, **6계층 패키지** |
| `om-service` | 샘플 Handler만 포함한 레거시 모듈 (`settings.gradle` 등록, 일괄 빌드·CI/CD 배포 제외) |

## 패키지 구조 (tcf-om 기준)

레거시 `om-service`는 flat 패키지(`handler/`, `service/` 등)를 사용합니다. 신규 개발·배포는 **`tcf-om`** 의 6계층 구조를 따르세요. 상세: [tcf-om/README.md](../tcf-om/README.md)

## 의존성

`tcf-util`, `tcf-core`, `tcf-web`
