# tcf-util — TCF 공통 유틸리티

Spring에 의존하지 않는 순수 Java 유틸리티 모듈입니다.  
각 프로젝트(`tcf-core`, `tcf-gateway`, `tcf-om`, `tcf-jwt`, `tcf-cache`, `tcf-uj` 등)에 분산된 **유틸리티성 기능을 카테고리별로 복사·통합**합니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-util` |
| 루트 패키지 | `com.nh.nsight.tcf.util` |
| 산출물 | JAR (라이브러리) |

## 복사 출처 플래그

모든 유틸 클래스는 **어디서 복사되었는지** 다음 3가지로 표시합니다.

| 플래그 | 설명 | 예 |
|--------|------|-----|
| `@CopiedFrom` | 런타임/문서용 어노테이션 | `@CopiedFrom(module="tcf-gateway", sourceClass="GatewayCookieParser")` |
| `COPIED_FROM_MODULE` | 원본 Gradle 모듈 상수 | `"tcf-gateway"` |
| `COPIED_FROM_CLASS` | 원본 클래스명 상수 | `"GatewayCookieParser"` |
| `CopiedUtilityFlag` | 복사 유틸 마커 인터페이스 | `implements CopiedUtilityFlag` |

**클래스명 규칙:** `{원본모듈접두}{기능}Utils` — 예: `GatewayCookieParserUtils`, `OmMapBodyUtils`, `JwtHashUtils`

`tcf-util` 최초 정의(native)는 `@CopiedFrom(..., nativeUtility=true)` 로 구분합니다.

전체 목록은 `TcfUtilRegistry.ENTRIES` 로 조회할 수 있습니다.

## 카테고리 구조

```text
com.nh.nsight.tcf.util
├── meta/          CopiedFrom, UtilCategory, TcfUtilRegistry
├── datetime/      (루트 DateTimeUtil — native)
├── id/            GuidGenerator(native), JwtIdUtils(tcf-jwt)
├── masking/       (루트 MaskingUtils — native)
├── string/        TcfStringUtils(native)
├── map/           OmMapBodyUtils, JwtMapValueUtils
├── crypto/        JwtHashUtils
├── json/          TpcJsonEscapeUtils
├── http/          GatewayCookieParserUtils, GatewaySessionIdResolverUtils, ...
├── logging/       CoreTcfConsoleLog, GatewayProxyTraceUtils
├── response/      OmUpdownloadResponseUtils
├── security/      GatewayAuthExemptionUtils, CoreTransactionControlExemptionUtils
├── catalog/       GatewayBusinessModuleCatalog, UjBusinessModuleDefinitions
├── constant/      TcfCacheNameConstants, CoreTransactionLogConstants, CoreErrorCodeConstants
└── tpmutil/       tpcutil (TPM HTTP 클라이언트 — native)
```

## 모듈별 복사 현황

| 원본 모듈 | tcf-util 클래스 | 카테고리 |
|-----------|-----------------|----------|
| **tcf-util** (native) | `DateTimeUtil`, `GuidGenerator`, `MaskingUtils`, `TcfStringUtils`, `tpcutil` | DATETIME, ID, MASKING, STRING, TPM |
| **tcf-gateway** | `GatewayCookieParserUtils`, `GatewaySessionIdResolverUtils`, `GatewaySessionHeaderRulesUtils`, `GatewayProxyTraceUtils`, `GatewayAuthExemptionUtils`, `GatewayBusinessModuleCatalog` | HTTP, LOGGING, SECURITY, CATALOG |
| **tcf-om** | `OmMapBodyUtils`, `OmUpdownloadResponseUtils` | MAP, RESPONSE |
| **tcf-jwt** | `JwtIdUtils`, `JwtMapValueUtils`, `JwtHashUtils` | ID, MAP, CRYPTO |
| **tcf-core** | `CoreTcfConsoleLog`, `CoreTransactionControlExemptionUtils`, `CoreTransactionLogConstants`, `CoreErrorCodeConstants` | LOGGING, SECURITY, CONSTANT |
| **tcf-cache** | `TcfCacheNameConstants` | CONSTANT |
| **tcf-uj** | `UjBusinessModuleDefinitions` | CATALOG |

> **참고:** `*-service` 업무 WAR 9개 모듈에는 별도 유틸 클래스가 없어 복사 대상이 없습니다.  
> Spring `@Component`/`@Service` Support, Domain Mapper, Seed Data는 유틸이 아니므로 제외했습니다.

## 의존 관계

```text
tcf-util  (최하위, Spring 없음, slf4j-api만)
   ↑
tcf-core
```

## 빌드

```bash
gradle :tcf-util:build
```

## 사용 예

```java
import com.nh.nsight.tcf.util.http.GatewayCookieParserUtils;
import com.nh.nsight.tcf.util.meta.TcfUtilRegistry;

// 복사 유틸 사용
Optional<String> sessionId = GatewayCookieParserUtils.sessionId(cookieHeader);

// 카탈로그 조회
TcfUtilRegistry.byModule("tcf-gateway").forEach(System.out::println);
```

## 마이그레이션 (향후)

원본 모듈의 유틸 클래스는 **당장 삭제하지 않습니다.**  
신규 코드는 tcf-util 복사본을 우선 사용하고, 점진적으로 원본을 tcf-util 위임으로 전환할 수 있습니다.
