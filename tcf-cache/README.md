# tcf-cache — TCF 공통 EhCache 모듈

Spring Cache + EhCache 3(JCache) 기반 캐시 환경을 제공합니다. `tcf-om` 공통코드 캐싱 등에서 사용합니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-cache` |
| 패키지 | `com.nh.nsight.tcf.cache` |
| 산출물 | JAR (라이브러리) |

## 의존성 추가

```gradle
dependencies {
    implementation project(':tcf-cache')
}
```

## 기본 설정

`tcf-cache.jar`에 포함된 기본값:

```yaml
spring:
  cache:
    type: jcache
    jcache:
      config: classpath:ehcache.xml

nsight:
  tcf:
    cache:
      enabled: true
```

비활성화:

```yaml
nsight:
  tcf:
    cache:
      enabled: false
```

## 캐시 영역 (ehcache.xml)

| alias | 용도 | TTL |
|-------|------|-----|
| `commonCode` | 공통코드 | 30분 |
| `serviceCatalog` | ServiceId 카탈로그 | 60분 |
| `sessionRegion` | 세션/리전 | 10분 |

상수: `com.nh.nsight.tcf.cache.support.TcfCacheNames`

## 사용 예

```java
@Cacheable(cacheNames = TcfCacheNames.COMMON_CODE, key = "#codeGroup")
public List<Map<String, Object>> findByGroup(String codeGroup) { ... }

@CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, allEntries = true)
public void save(...) { ... }
```

프로그램 방식 제어: `TcfCacheSupport.evict()`, `evictAll()`, `snapshotEntries()`

OM Cache 관리 화면:

- bootRun: http://localhost:8099/om/admin/cache.html
- ztomcat: http://localhost:8080/ui/om/admin/cache.html

## 공통코드 캐시 샘플

EhCache `commonCode` 영역에 공통코드를 저장·조회·Evict 하는 **참고 구현**과 **데모 화면**이 포함되어 있습니다.

### Java 샘플

| 클래스 | 설명 |
|--------|------|
| `sample.model.CommonCodeEntry` | 공통코드 행 모델 |
| `sample.support.CommonCodeCacheKeys` | 캐시 키 규칙 (`__ALL_GROUPS__`) |
| `sample.persistence.InMemoryCommonCodeSampleRepository` | 인메모리 DB (테스트·데모) |
| `sample.application.service.CommonCodeCacheSampleService` | `@Cacheable` / `@CacheEvict` 패턴 |

실제 OM 구현: `tcf-om` → `OmCommonCodeCacheService`

```bash
gradle :tcf-cache:test --tests CommonCodeCacheSampleServiceTest
```

### 데모 화면

| 위치 | URL (bootRun) | URL (ztomcat) |
|------|---------------|---------------|
| `samples/common-code-cache/index.html` | 파일 직접 열기 | — |
| `tcf-ui` 연동 | http://localhost:8099/cache/common-code-sample.html | http://localhost:8080/ui/cache/common-code-sample.html |

로컬 시뮬: browser localStorage = DB, JS Map = `commonCode` EhCache  
tcf-om 기동 시: **OM DB 저장** · **실제 EhCache 스냅샷** 버튼으로 Relay 연동

## 빌드

```bash
gradle :tcf-cache:build
```
