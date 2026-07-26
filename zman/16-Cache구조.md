# 16장. Cache 구조 — 설명

## 설계서 절 목차

16.1~16.2 개요 · 16.3~16.5 위치·대상·Region · 16.6~16.10 모듈·Evict · 16.11~16.17 OM·STF·운영

---

## 핵심 결론

> **DB = 원장**, **Cache = 기준정보 가속**. **업무 데이터 캐시 금지**.

---

## Cache 위치 (16.3)

```
STF (Catalog, 공통코드, Timeout 조회)
→ @Cacheable (tcf-cache)
→ Cache Miss → DAO → OMDB
```

tcf-cache = **JAR** (WAR 아님), Spring Cache + EhCache 3.

## 캐시 대상 (16.4)

| O | X |
|---|---|
| OM_COMMON_CODE | 고객 잔액·거래 결과 |
| OM_SERVICE_CATALOG | 실시간 업무 데이터 |
| Timeout/통제 정책 | |
| sessionRegion (보조) | |

## Region·TTL (16.5)

| Region | TTL (예) |
|--------|----------|
| commonCode | 30분 (1800s) |
| serviceCatalog | 60분 (3600s) |
| sessionRegion | 10분 (600s) |

설정: `tcf-cache/src/main/resources/ehcache.xml`, `TcfCacheNames`

## 모듈 구성 (16.6)

```
tcf-cache/
├── ehcache.xml
├── TcfCacheNames
├── TcfCacheSupport  (evict, evictAll, snapshot)
└── AutoConfiguration

tcf-om/
├── OmCommonCodeCacheService (@Cacheable/@CacheEvict)
└── OmCacheHandler (OM.Cache.inquiry/delete)
```

## Evict (16.x)

OM 기준정보 변경 → **즉시 Evict** (`TcfCacheSupport.evict`)

## STF 연계

Catalog·공통코드 Cache Hit → DB 부하↓, STF latency↓

## 코드베이스

- `tcf-cache/.../TcfCacheSupport.java`
- `docs/architecture/12-cache.md`

## 운영 체크리스트

- [ ] OM 변경 후 Evict 확인  
- [ ] TTL vs 운영 변경 빈도  
- [ ] snapshot으로 Region 모니터링  

## 이전 · 다음

← [15장 로그](./15-거래로그-감사로그.md) · [17장 Batch](./17-Batch-Scheduler.md) →
