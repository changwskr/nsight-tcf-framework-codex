package com.nh.nsight.gateway.config;

import com.nh.nsight.gateway.application.service.GatewayRouteResolver;
import com.nh.nsight.gateway.persistence.dao.GatewayRouteDao;
import com.nh.nsight.gateway.support.GatewayRoute;
import com.nh.nsight.gateway.support.GatewayRouteCatalog;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * data.sql MERGE 이후에도 H2에 누락된 LOCAL 라우트(예: JWT)를 카탈로그 기준으로 보충한다.
 */
@Component
public class GatewayRouteLocalCatalogSeeder {
    private static final Logger log = LoggerFactory.getLogger(GatewayRouteLocalCatalogSeeder.class);

    private final GatewayProperties properties;
    private final GatewayRouteDao dao;
    private final GatewayRouteResolver resolver;

    public GatewayRouteLocalCatalogSeeder(
            GatewayProperties properties,
            GatewayRouteDao dao,
            GatewayRouteResolver resolver) {
        this.properties = properties;
        this.dao = dao;
        this.resolver = resolver;
    }

    public void seedMissingRoutes() {
        String env = properties.getEnvCode().toUpperCase(Locale.ROOT);
        if (!"LOCAL".equals(env)) {
            return;
        }
        boolean changed = false;
        for (GatewayRouteCatalog.BusinessCode business : GatewayRouteCatalog.businessCodes()) {
            if (dao.findActive(env, business.code()).isPresent()) {
                continue;
            }
            GatewayRoute route = toLocalRoute(env, business);
            if (dao.findById(route.routeId()).isPresent()) {
                dao.update(route);
            } else {
                dao.insert(route);
            }
            changed = true;
            log.info("Seeded missing gateway route envCode={} businessCode={} targetUrl={}",
                    env, business.code(), route.targetUrl());
        }
        if (changed) {
            resolver.evictCache();
        }
    }

    private GatewayRoute toLocalRoute(String env, GatewayRouteCatalog.BusinessCode business) {
        GatewayRouteCatalog.RouteGroupCode group = GatewayRouteCatalog.findGroup(business.routeGroupCode())
                .orElseThrow(() -> new IllegalStateException("Unknown route group: " + business.routeGroupCode()));
        String contextPath = StringUtils.hasText(business.contextPath()) ? business.contextPath() : "";
        return new GatewayRoute(
                env + "-GW-" + business.code(),
                env,
                group.code(),
                group.name(),
                business.code(),
                business.name(),
                business.localTargetBaseUrl(),
                contextPath,
                "/online",
                business.localHealthCheckPath(),
                3000,
                5000,
                "Y",
                business.sortOrder(),
                "LOCAL " + business.code() + " catalog seed"
        );
    }
}
