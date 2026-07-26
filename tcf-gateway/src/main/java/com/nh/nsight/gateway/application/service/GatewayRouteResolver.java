package com.nh.nsight.gateway.application.service;

import com.nh.nsight.gateway.config.GatewayProperties;
import com.nh.nsight.gateway.persistence.dao.GatewayRouteDao;
import com.nh.nsight.gateway.support.GatewayRoute;
import com.nh.nsight.gateway.support.GatewayProxyTrace;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class GatewayRouteResolver {
    private static final String PHASE = "GatewayRouteResolver.resolve";

    private final GatewayProperties properties;
    private final GatewayRouteDao dao;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public GatewayRouteResolver(GatewayProperties properties, GatewayRouteDao dao) {
        this.properties = properties;
        this.dao = dao;
    }

    public Optional<GatewayRoute> resolve(String businessCode) {
        String envCode = properties.getEnvCode();
        String code = businessCode.toUpperCase(Locale.ROOT);
        String cacheKey = envCode + ":" + code;
        if (properties.getRouteTable().isCacheEnabled()) {
            CacheEntry cached = cache.get(cacheKey);
            if (cached != null && !cached.expired(properties.getRouteTable().getCacheTtlSeconds())) {
                GatewayProxyTrace.log(PHASE, "cacheHit envCode=" + envCode + " businessCode=" + code);
                return cached.route();
            }
        }
        Optional<GatewayRoute> route = dao.findActive(envCode, code);
        GatewayProxyTrace.log(PHASE, "envCode=" + envCode + " businessCode=" + code
                + " tableHit=" + route.isPresent()
                + (route.isPresent() ? " targetUrl=" + route.get().targetUrl() : ""));
        if (properties.getRouteTable().isCacheEnabled()) {
            cache.put(cacheKey, new CacheEntry(route, System.currentTimeMillis()));
        }
        return route;
    }

    public void evictCache() {
        cache.clear();
    }

    public String currentEnvCode() {
        return properties.getEnvCode();
    }

    private record CacheEntry(Optional<GatewayRoute> route, long loadedAtMillis) {
        boolean expired(long ttlSeconds) {
            if (ttlSeconds <= 0) {
                return false;
            }
            return System.currentTimeMillis() - loadedAtMillis > ttlSeconds * 1000L;
        }
    }
}
