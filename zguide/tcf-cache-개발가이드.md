# tcf-cache 개발자 가이드

> **역할:** Spring Cache + **EhCache 3 (JCache)** 공통 캐시  
> **유형:** 라이브러리 JAR

---

## 1. 이 모듈이 하는 일

tcf-om 공통코드·ServiceId Catalog 등에서 사용하는 **공통 캐시 환경**을 제공합니다.

---

## 2. 의존성 추가

```gradle
implementation project(':tcf-cache')
```

사용 모듈: **tcf-om** (주), 기타 필요 WAR

---

## 3. 설정

기본 (JAR 포함):

```yaml
spring:
  cache:
    type: jcache
    jcache:
      config: classpath:ehcache.xml

nsight:
  tcf:
    cache:
      enabled: true   # false 로 전체 비활성화
```

---

## 4. 캐시 영역

| alias | 용도 | TTL |
|-------|------|-----|
| `commonCode` | 공통코드 | 30분 |
| `serviceCatalog` | ServiceId Catalog | 60분 |
| `sessionRegion` | 세션/리전 | 10분 |

상수: `com.nh.nsight.tcf.cache.support.TcfCacheNames`

---

## 5. 사용 예

```java
@Cacheable(cacheNames = TcfCacheNames.COMMON_CODE, key = "#codeGroup")
public List<Map<String, Object>> findByGroup(String codeGroup) { ... }

@CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, allEntries = true)
public void save(...) { ... }
```

프로그램 제어: `TcfCacheSupport.evict()`, `evictAll()`, `snapshotEntries()`

---

## 6. OM Cache 관리 UI

| 모드 | URL |
|------|-----|
| bootRun | http://localhost:8099/om/admin/cache.html |
| ztomcat | http://localhost:8080/ui/om/admin/cache.html |

---

## 7. 빌드

```bash
gradle :tcf-cache:build
```

---

## 8. 참고

| | |
|---|---|
| [tcf-cache/README.md](../tcf-cache/README.md) | |
| [tcf-om-개발가이드.md](./tcf-om-개발가이드.md) | OmCacheHandler |
| [zman/12-OM운영관리.md](../zman/12-OM운영관리.md) | Cache 관리 |
