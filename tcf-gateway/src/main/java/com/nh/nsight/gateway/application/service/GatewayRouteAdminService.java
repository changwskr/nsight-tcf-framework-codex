package com.nh.nsight.gateway.application.service;

import com.nh.nsight.gateway.persistence.dao.GatewayRouteDao;
import com.nh.nsight.gateway.support.GatewayRoute;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GatewayRouteAdminService {
    private final GatewayRouteDao dao;
    private final GatewayRouteResolver resolver;

    public GatewayRouteAdminService(GatewayRouteDao dao, GatewayRouteResolver resolver) {
        this.dao = dao;
        this.resolver = resolver;
    }

    public List<Map<String, Object>> listRoutes(String envCode) {
        String env = StringUtils.hasText(envCode) ? envCode.toUpperCase(Locale.ROOT) : resolver.currentEnvCode();
        return dao.findByEnv(env).stream().map(this::toView).toList();
    }

    public Optional<Map<String, Object>> getRoute(String routeId) {
        return dao.findById(routeId).map(this::toView);
    }

    public Map<String, Object> saveRoute(GatewayRoute route) {
        GatewayRoute normalized = normalize(route);
        if (dao.findById(normalized.routeId()).isPresent()) {
            dao.update(normalized);
        } else {
            dao.insert(normalized);
        }
        resolver.evictCache();
        return toView(normalized);
    }

    public boolean deleteRoute(String routeId) {
        int deleted = dao.delete(routeId);
        if (deleted > 0) {
            resolver.evictCache();
        }
        return deleted > 0;
    }

    public Optional<Map<String, Object>> preview(String businessCode, String envCode) {
        String env = StringUtils.hasText(envCode) ? envCode.toUpperCase(Locale.ROOT) : resolver.currentEnvCode();
        return dao.findActive(env, businessCode.toUpperCase(Locale.ROOT)).map(this::toView);
    }

    private GatewayRoute normalize(GatewayRoute route) {
        return new GatewayRoute(
                route.routeId(),
                route.envCode().toUpperCase(Locale.ROOT),
                route.routeGroupCode(),
                route.routeGroupName(),
                route.businessCode().toUpperCase(Locale.ROOT),
                route.businessName(),
                route.targetBaseUrl(),
                route.contextPath(),
                StringUtils.hasText(route.onlinePath()) ? route.onlinePath() : "/online",
                route.healthCheckPath(),
                route.connectTimeoutMs() > 0 ? route.connectTimeoutMs() : 3000,
                route.readTimeoutMs() > 0 ? route.readTimeoutMs() : 5000,
                StringUtils.hasText(route.useYn()) ? route.useYn().toUpperCase(Locale.ROOT) : "Y",
                route.sortOrder(),
                route.description()
        );
    }

    private Map<String, Object> toView(GatewayRoute route) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("routeId", route.routeId());
        view.put("envCode", route.envCode());
        view.put("routeGroupCode", route.routeGroupCode());
        view.put("routeGroupName", route.routeGroupName());
        view.put("businessCode", route.businessCode());
        view.put("businessName", route.businessName());
        view.put("targetBaseUrl", route.targetBaseUrl());
        view.put("contextPath", route.contextPath());
        view.put("onlinePath", route.onlinePath());
        view.put("targetUrl", route.targetUrl());
        view.put("healthCheckPath", route.healthCheckPath());
        view.put("connectTimeoutMs", route.connectTimeoutMs());
        view.put("readTimeoutMs", route.readTimeoutMs());
        view.put("useYn", route.useYn());
        view.put("sortOrder", route.sortOrder());
        view.put("description", route.description());
        return view;
    }
}
