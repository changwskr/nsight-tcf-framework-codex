# PDMK-OM (`pdmk-om`)

PDMK **OM(운영관리)** 샘플 애플리케이션입니다. `pdmk-service`를 복사해 **프로젝트 환경만 OM 전용**으로 분리한 모듈이며, **공통 FW는 [`pdmk-fw`](../pdmk-fw/README.md)에 의존**합니다.

> 현재 업무 패키지/서비스 ID는 당분간 `nhnis.mk` / `mkcoa*` 를 유지합니다. (환경 분리 A안)  
> 추후 `nhnis.om` / `omcoa*` 및 계층 패키지 재배치는 B안으로 진행합니다.

```text
ServicePreventionInterceptor (pdmk-fw)
  → BizPrePostAspect (nhnis.mk.co.common)
    → Controller (nhnis.mk.co.a.controller.*)
      → Service (nhnis.mk.co.a.service.*)
```

## 로컬 포트

| 모듈 | 포트 |
|---|---|
| `pdmk-service` | 8080 |
| **`pdmk-om`** | **8081** |
| `pdmk-ui` | 8090 |

## 샘플 프로그램 (당분간 MK ID 유지)

| 프로그램 | API | 설명 |
|---|---|---|
| `mkcoa7777` | `POST /mkcoa7777S0`, `POST /mkcoa7777D0` | 이미지로그 조회/삭제 |
| `mkcoa6666` | `S0`·`I0`·`U0`·`D0`·`E0`·`S2`·`U1` | Catalog + 평가 + 집계 + 상태변경 |
| `mkcoa5530` | `POST /mkcoa5530S0` | 마케팅희망고객(안내항목) 목록 |
| `mkcoa9999` | `POST /mkcoa9999S0` | 영업팁 실적 목록 |

요청 Body 예: `{"hdr_nhnis":{...},"dto":{...}}` 또는 local에서 `{"dto":{...}}`.

## 패키지 구성

| 패키지 / 모듈 | 설명 |
|---|---|
| `nhnis.mk.PdmkOmApplication` | Boot 진입점 (OM) |
| `nhnis.mk.*` | 업무 CRUD, Security/MyBatis (당분간 MK 패키지 유지) |
| `nhnis.mk.co.common` | `BizPrePostAspect` 업무 공통 선·후처리 |
| `pdmk-fw` (`nhnis.fw.*`) | `ServicePreventionInterceptor`, commons |

## 빌드 / 실행

```powershell
cd pdmk-om
.\gradlew.bat compileJava
.\gradlew.bat bootWar
.\RUN.bat
# 또는
.\script\run.bat
```

기동 URL: http://localhost:8081

`settings.gradle`이 형제 프로젝트 `../pdmk-fw`를 `include` 합니다.
