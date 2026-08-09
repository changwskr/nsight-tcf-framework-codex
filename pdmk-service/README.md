# PDMK Service (`pdmk-service`)

PDMK 업무 샘플 애플리케이션입니다. **공통 FW는 [`pdmk-fw`](../pdmk-fw/README.md)에 의존**합니다.

아키텍처 **정본**은 [`docs/PDMK_아키텍처_정의서.md`](docs/PDMK_아키텍처_정의서.md) (v2.0). 입문은 [`docs/아키텍처 정의서.md`](docs/아키텍처%20정의서.md), 문서 목록은 [`docs/README.md`](docs/README.md), 네이밍은 [`docs/네이밍원칙.md`](docs/네이밍원칙.md) · [`docs/MK-NAMING_CONVENTION.md`](docs/MK-NAMING_CONVENTION.md).

```text
ServicePreventionInterceptor (pdmk-fw)
  → BizPrePostAspect (nhnis.mk.co.common)
    → Controller (nhnis.mk.co.a.controller.*)
      → Service (nhnis.mk.co.a.service.*)
```

## 샘플 프로그램

| 프로그램 | API | 설명 |
|---|---|---|
| `mkcoa8888` | `POST /mkcoa8888S0`, `POST /mkcoa8888D0` | 이미지로그 조회/삭제 |
| `mkcoa5530` | `POST /mkcoa5530S0` | 마케팅희망고객(안내항목) 목록 |
| `mkcoa9999` | `POST /mkcoa9999S0` | 영업팁 실적 목록 |

요청 Body 예: `{"hdr_nhnis":{...},"dto":{...}}` 또는 local에서 `{"dto":{...}}`.

## 패키지 구성

| 패키지 / 모듈 | 설명 |
|---|---|
| `nhnis.mk.*` | Boot 앱, 업무 CRUD, Security/MyBatis |
| `nhnis.mk.co.common` | `BizPrePostAspect` 업무 공통 선·후처리 |
| `pdmk-fw` (`nhnis.fw.*`) | `ServicePreventionInterceptor`, commons |

## 빌드

```powershell
cd pdmk-service
.\gradlew.bat compileJava
.\gradlew.bat bootWar
```

`settings.gradle`이 형제 프로젝트 `../pdmk-fw`를 `include` 합니다.
