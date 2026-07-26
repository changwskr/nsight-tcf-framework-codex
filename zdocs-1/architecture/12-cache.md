# 12. 캐시 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 12 |
| 제목 | Cache Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [07-DAO.md](07-DAO.md), [10-session.md](10-session.md), [11-login.md](11-login.md) |
| 구현 모듈 | `tcf-cache`, `tcf-om` |
| 대상 | 프레임워크·업무·운영 개발자 |

---

## 1. 개요

NSIGHT TCF의 캐시는 `tcf-cache` 모듈이 제공하는 **Spring Cache + Ehcache 3 (JCache)** 기반 공통 구조를 사용한다.

| 목적 | 설명 |
|------|------|
| 성능 개선 | 반복 조회 데이터(DB hit) 감소 |
| 안정성 | 일시적 DB 부하 완충 |
| 운영 제어 | OM 화면에서 조회/삭제(무효화) 가능 |

핵심 설계는 “애플리케이션 캐시(Ehcache)”와 “운영 메타/이력(OM)”를 분리하는 것이다.

---

## 2. 모듈 구조

```text
tcf-cache (라이브러리)
  ├─ ehcache.xml                      # 캐시 영역/TTL 정의
  ├─ TcfCacheNames                    # 캐시 이름 상수
  ├─ TcfCacheSupport                  # 프로그램 방식 조회/evict
  └─ AutoConfiguration                # Spring Cache 연동

tcf-om
  ├─ OmCommonCodeCacheService         # @Cacheable/@CacheEvict 사용
  ├─ OmCacheService                   # OM.Cache.inquiry/delete 처리
  └─ om-admin cache.html              # 운영자 캐시 관리 화면
```

---

## 3. 기술 스택

| 계층 | 기술 |
|------|------|
| 캐시 추상화 | Spring Cache (`@Cacheable`, `@CacheEvict`) — AOP: [32-AOP.md](32-AOP.md) |
| 캐시 구현체 | Ehcache 3 |
| 표준 API | JCache (`javax.cache`) |
| 운영 연동 | `TcfCacheSupport` + OM 거래(`OM.Cache.*`) |

---

## 4. 캐시 구성과 기본 설정

`tcf-om` 기준 설정:

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
      config-location: classpath:ehcache.xml
```

비활성화:

```yaml
nsight:
  tcf:
    cache:
      enabled: false
```

비활성화 시 캐시 관련 빈(`TcfCacheSupport`)이 없을 수 있으며, OM 캐시 조회/삭제는 DB fallback 경로를 탄다.

---

## 5. 캐시 영역(Region) 설계

`ehcache.xml`의 기본 캐시:

| alias | TTL | heap(entries) | 용도 |
|-------|-----|---------------|------|
| `commonCode` | 30분 | 200 | 공통코드 조회 |
| `serviceCatalog` | 60분 | 100 | ServiceId 카탈로그 |
| `sessionRegion` | 10분 | 100 | 세션/리전 보조 데이터 |

템플릿:

- `tcf-default`: TTL 30분, heap 500 entries

각 캐시는 alias별로 템플릿을 오버라이드한다.

---

## 6. 접근 패턴 아키텍처

## 6.1 선언적 캐시 (`@Cacheable`)

`OmCommonCodeCacheService` 패턴:

- `loadByCodeGroup(codeGroup)` → `commonCode` 캐시에 group별 저장
- `loadAllCodeGroupNames()` → `__ALL_GROUPS__` 키로 그룹 인덱스 캐싱

```text
Cache hit  → 메모리 반환
Cache miss → DAO 조회 후 캐시에 저장
```

## 6.2 무효화 (`@CacheEvict`)

공통코드 변경 시:

- 해당 `codeGroup` 엔트리 evict
- `__ALL_GROUPS__` 인덱스도 함께 evict

목적:

- 코드그룹 상세와 그룹목록 불일치 방지

## 6.3 프로그램 방식 제어 (`TcfCacheSupport`)

지원 기능:

- `evict(cacheName, key)`
- `evictAll(cacheName)`
- `snapshot()`
- `snapshotEntries(cacheNameFilter, cacheKeyFilter)`

운영 화면/관리 API에서 직접 호출해 캐시 상태 확인과 정리를 수행한다.

---

## 7. 운영 관리 아키텍처 (OM Cache)

OM 거래:

| serviceId | 역할 |
|-----------|------|
| `OM.Cache.inquiry` | 캐시 목록/엔트리 조회 |
| `OM.Cache.delete` | 캐시 엔트리/전체 무효화 |

`OmCacheService` 동작:

1. `TcfCacheSupport`가 있으면 Ehcache 실시간 스냅샷/evict 사용
2. 없으면 `OmOperationDao.searchCacheStatus/deleteCache` fallback
3. 삭제 성공 시 감사/권한이력 기록

운영 반환 필드:

- `fromEhCache` (실제 Ehcache 조회 여부)
- `rows`, `totalCount`
- `deletedCount`, `cacheName`

---

## 8. 캐시 상태 데이터 모델

운영 화면에서는 아래 형태로 캐시를 표현한다.

| 필드 | 의미 |
|------|------|
| `cacheName` | 캐시 영역명 (`commonCode` 등) |
| `cacheKey` | 엔트리 키 (`*` 또는 실제 키) |
| `entryCount` | 엔트리 수/값 크기 |
| `ttlSec` | 기본 TTL(초) |
| `source` | 데이터 소스 (`ehcache`) |

`TcfCacheSupport`의 TTL 표준값:

- `commonCode` = 1800초
- `serviceCatalog` = 3600초
- `sessionRegion` = 600초

---

## 9. DB/거래 계층과의 관계

캐시는 DB를 대체하지 않고 **조회 가속 계층**으로 동작한다.

```text
Service
  ├─ cache hit  → 즉시 반환
  └─ cache miss → DAO → Mapper → DB → cache put
```

원칙:

1. 영속 정합성 기준은 DB
2. 변경 시 명시적 evict로 정합성 보장
3. 캐시 miss 로직은 기존 DAO 경로 재사용

---

## 10. 장애/운영 시나리오

| 시나리오 | 동작 | 대응 |
|----------|------|------|
| 캐시 비활성 | DB 직접 조회 증가 | 성능 저하 모니터링 후 재활성 |
| 캐시 오염/오래된 데이터 | OM.Cache.delete로 즉시 evict | 변경 이벤트 후 자동 evict 점검 |
| Ehcache 빈 미생성 | `fromEhCache=false` fallback | 설정(`spring.cache`, `nsight.tcf.cache`) 확인 |
| 대량 key 누적 | entryCount 증가 | TTL/heap 튜닝, 키 전략 점검 |

---

## 11. 캐시 키 설계 원칙

1. 조회 조건의 최소 식별자만 키로 사용 (`codeGroup`)
2. 전체 인덱스 키는 명시적 예약 키 사용 (`__ALL_GROUPS__`)
3. 고카디널리티(요청별 GUID 등) 키는 금지
4. 동일 도메인은 동일 키 규약 유지

---

## 12. 보안/운영 가이드

- 캐시에 비밀번호/토큰/민감정보 저장 금지
- 캐시 삭제 API(`OM.Cache.delete`)는 권한코드(`ROLE_OM_CACHE`)로 제한
- 운영 화면에서 삭제 사유(`deleteReason`)를 받아 감사 로그에 남김
- 환경별 TTL 차이는 문서/카탈로그와 함께 관리

---

## 13. 체크리스트

- [ ] `spring.cache.type=jcache`가 적용되었는가
- [ ] `ehcache.xml` alias와 `TcfCacheNames` 상수가 일치하는가
- [ ] 변경 거래 후 대응 캐시 evict가 구현되었는가
- [ ] OM.Cache.inquiry/delete가 Ehcache 실시간 상태를 반영하는가
- [ ] 캐시 비활성 시 fallback 경로가 정상 동작하는가

---

## 14. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-cache/src/main/resources/ehcache.xml` | 캐시 영역/TTL/heap |
| `tcf-cache/.../support/TcfCacheSupport.java` | snapshot/evict API |
| `tcf-cache/.../support/TcfCacheNames.java` | 캐시 이름 상수 |
| `tcf-om/.../service/OmCommonCodeCacheService.java` | `@Cacheable/@CacheEvict` 적용 |
| `tcf-om/.../service/OmCacheService.java` | 운영 캐시 조회/삭제 |
| `tcf-om/src/main/resources/application.yml` | 캐시 활성화 설정 |
| `tcf-cache/README.md` | 모듈 사용 가이드 |

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — tcf-cache 기반 캐시 구조 및 OM 운영 관리 흐름 정리 |
