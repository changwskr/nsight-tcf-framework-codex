# PDMG Framework (`pdmg-fw`)

`pdmg-service`에서 공통/프레임워크 계층을 분리한 **라이브러리 프로젝트**입니다.
(`pdmk-fw`를 복사한 뒤 PDMG 환경에 맞게 식별자를 재구성했습니다.)

네이밍 정본: [`pdmg-service/docs/MG-NAMING_CONVENTION.md`](../pdmg-service/docs/MG-NAMING_CONVENTION.md).

## 포함 범위

| 패키지 | 설명 |
|---|---|
| `nhnis.fw.commons.*` | 필터, 헤더 DTO, JWT, FOS, 메시지, 유틸 등 레거시 FW |
| `nhnis.fw.tcf.*` | TCF/STF/ETF, 표준 전문, Trace/JWT 필터 |
| `nhnis.fw.exception.*` | TCF 예외 처리 |
| `com.ims.superspring.*` | SuperSpring 호환 DTO 컴파일용 stub (원본 JAR 있으면 제거) |

업무 모듈(`nhnis.mg.*`)과 Boot 애플리케이션은 **포함하지 않습니다.**

생성 DTO의 `@Generated` 값은 `com.imssoft.sts4.codegen...` 을 사용합니다.

## 빌드

```powershell
cd pdmg-fw
.\gradlew.bat jar
```

산출물: `build/libs/pdmg-fw-0.0.1-SNAPSHOT.jar`

## 소비 측 연결 예시 (`pdmg-service`)

```gradle
dependencies {
    implementation project(':pdmg-fw')
    // 또는
    // implementation files('../pdmg-fw/build/libs/pdmg-fw-0.0.1-SNAPSHOT.jar')
}
```

루트 `settings.gradle`에 `include 'pdmg-fw'`를 추가하거나, `pdmg-service`처럼 독립 Gradle 루트로 사용합니다.

## 주의

- `commons.configuration.SecurityConfig`와 TCF/앱 Security가 동시에 로드되면 필터 체인이 충돌할 수 있습니다. 앱에서 하나를 비활성화하세요.
- `ClientHttpConnector` Bean은 앱(또는 FW 설정)에서 제공해야 `DefaultFilter` 등이 기동됩니다.
- `com.ims.superspring` stub은 사내 SuperSpring JAR로 교체하는 것을 권장합니다.

## 출처

`pdmk-fw`를 복사한 뒤 PDMG용으로 재구성했습니다. 로그 유틸은 `PdmgTxLog` / `PdmgTxFlowLog` / `PdmgMessagePrinter` 를 사용합니다.
