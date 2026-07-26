package com.nh.nsight.tcf.cache.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;

public class TcfCacheSupport {
    private static final Map<String, Integer> DEFAULT_TTL_SEC = Map.of(
            TcfCacheNames.COMMON_CODE, 1800,
            TcfCacheNames.SERVICE_CATALOG, 3600,
            TcfCacheNames.SESSION_REGION, 600
    );

    private final JCacheCacheManager springCacheManager;

    public TcfCacheSupport(JCacheCacheManager springCacheManager) {
        this.springCacheManager = springCacheManager;
    }

    public void evict(String cacheName, Object key) {
        var cache = springCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    public void evictAll(String cacheName) {
        var cache = springCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public List<Map<String, Object>> snapshot() {
        CacheManager jcacheManager = springCacheManager.getCacheManager();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String cacheName : jcacheManager.getCacheNames()) {
            Cache<Object, Object> cache = jcacheManager.getCache(cacheName);
            if (cache == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cacheName", cacheName);
            row.put("cacheKey", "*");
            row.put("entryCount", sizeOf(cache));
            row.put("ttlSec", defaultTtlSec(cacheName));
            row.put("source", "ehcache");
            rows.add(row);
        }
        return rows;
    }

    public List<Map<String, Object>> snapshotEntries(String cacheNameFilter, String cacheKeyFilter) {
        CacheManager jcacheManager = springCacheManager.getCacheManager();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String cacheName : jcacheManager.getCacheNames()) {
            if (cacheNameFilter != null && !cacheNameFilter.isEmpty()
                    && !cacheName.contains(cacheNameFilter)) {
                continue;
            }
            Cache<Object, Object> cache = jcacheManager.getCache(cacheName);
            if (cache == null) {
                continue;
            }
            for (Cache.Entry<Object, Object> entry : cache) {
                String cacheKey = String.valueOf(entry.getKey());
                if (cacheKeyFilter != null && !cacheKeyFilter.isEmpty() && !cacheKey.equals(cacheKeyFilter)) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("cacheName", cacheName);
                row.put("cacheKey", cacheKey);
                row.put("entryCount", entryCount(entry.getValue()));
                row.put("ttlSec", defaultTtlSec(cacheName));
                row.put("lastUpdated", "-");
                row.put("source", "ehcache");
                rows.add(row);
            }
        }
        return rows;
    }

    private int entryCount(Object value) {
        if (value instanceof List<?> list) {
            return list.size();
        }
        return value != null ? 1 : 0;
    }

    private int defaultTtlSec(String cacheName) {
        return DEFAULT_TTL_SEC.getOrDefault(cacheName, 1800);
    }

    private long sizeOf(Cache<Object, Object> cache) {
        long count = 0;
        Iterator<Cache.Entry<Object, Object>> iterator = cache.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }
}
