# PDMK Framework (`pdmk-fw`)

`pdmk-service`에서 공통/프레임워크 계층을 분리한 **라이브러리 프로젝트**입니다.
(`pdmp-fw`를 복사한 뒤 PDMK 환경에 맞게 식별자를 정리했습니다.)

아키텍처 **정본**: [`pdmk-service/docs/PDMK_아키텍처_정의서.md`](../pdmk-service/docs/PDMK_아키텍처_정의서.md) (v2.0).

## 포함 범위

| 패키지 | 설명 |
|---|---|
| `nhnis.fw.commons.*` | 필터, 헤더 DTO, JWT, FOS, 메시지, 유틸 등 레거시 FW |
| `nhnis.fw.tcf.*` | TCF/STF/ETF, 표준 전문, Trace/JWT 필터 |
| `nhnis.fw.exception.*` | TCF 예외 처리 |
| `com.ims.superspring.*` | SuperSpring 호환 DTO 컴파일용 stub (원본 JAR 있으면 제거) |

업무 모듈(`nhnis.mk.*`)과 Boot 애플리케이션은 **포함하지 않습니다.**

생성 DTO의 `@Generated` 값은 `com.imssoft.sts4.codegen...` 을 사용합니다.

## 빌드

```powershell
cd pdmk-fw
.\gradlew.bat jar
```

산출물: `build/libs/pdmk-fw-0.0.1-SNAPSHOT.jar`

## 소비 측 연결 예시 (`pdmk-service`)

```gradle
dependencies {
    implementation project(':pdmk-fw')
    // 또는
    // implementation files('../pdmk-fw/build/libs/pdmk-fw-0.0.1-SNAPSHOT.jar')
}
```

루트 `settings.gradle`에 `include 'pdmk-fw'`를 추가하거나, `pdmk-service`처럼 독립 Gradle 루트로 사용합니다.

## 주의

- `commons.configuration.SecurityConfig`와 TCF/앱 Security가 동시에 로드되면 필터 체인이 충돌할 수 있습니다. 앱에서 하나를 비활성화하세요.
- `ClientHttpConnector` Bean은 앱(또는 FW 설정)에서 제공해야 `DefaultFilter` 등이 기동됩니다.
- `com.ims.superspring` stub은 사내 SuperSpring JAR로 교체하는 것을 권장합니다.

## 출처

`pdmp-fw` / `pdmp-service`의 `nhnis.fw`, `com.ims.superspring` 및 `exceptionCode.yml`을 기준으로 분리·복제한 뒤 PDMK용으로 재명명했습니다.
