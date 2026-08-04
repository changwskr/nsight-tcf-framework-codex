# PDMP Service (`pdmp-service`)

PDMP 업무 샘플 애플리케이션입니다. 공통 FW 소스는 `nhnis.fw.*`와 `com.ims.superspring.*` stub을 포함합니다.

## 패키지 구성

| 패키지 | 설명 |
|---|---|
| `nhnis.mp.*` | Boot 앱, 샘플 CRUD, Security/MyBatis 설정 |
| `nhnis.fw.commons.*` | 레거시 공통 FW |
| `nhnis.fw.tcf.*` | TCF/STF/ETF |
| `nhnis.fw.exception.*` | TCF 예외 처리 |
| `com.ims.superspring.*` | SuperSpring 호환 DTO stub |

생성 DTO의 `@Generated` 값은 `com.imssoft.sts4.codegen...` 을 사용합니다.

프레임워크만 재사용하려면 형제 프로젝트 [`pdmp-fw`](../pdmp-fw/README.md)를 사용하세요.

## 빌드

```powershell
cd pdmp-service
.\gradlew.bat compileJava
# 또는
.\gradlew.bat bootWar
```

독립 Gradle 루트(`settings.gradle` → `rootProject.name = 'pdmp'`)입니다. 저장소 루트 `settings.gradle`에는 include되어 있지 않습니다.
