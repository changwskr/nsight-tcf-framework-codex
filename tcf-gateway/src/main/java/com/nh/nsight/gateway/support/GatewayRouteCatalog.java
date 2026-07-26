package com.nh.nsight.gateway.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Gateway 라우팅 관리 화면용 코드성 데이터 */
public final class GatewayRouteCatalog {
    private GatewayRouteCatalog() {
    }

    public record RouteGroupCode(String code, String name) {
    }

    public record BusinessCode(
            String code,
            String name,
            String routeGroupCode,
            String contextPath,
            int sortOrder,
            int localBootrunPort,
            String localTargetBaseUrl,
            String localHealthCheckPath
    ) {
    }

    private static final List<RouteGroupCode> ROUTE_GROUPS = List.of(
            new RouteGroupCode("MSA-A", "공통/고객 기본 그룹"),
            new RouteGroupCode("MSA-B", "고객/마케팅 조회 그룹"),
            new RouteGroupCode("GATEWAY", "Gateway/운영관리")
    );

    private static final List<BusinessCode> BUSINESS_CODES = List.of(
            business("CC", "Common", "MSA-A", 10),
            business("IC", "Integration Customer", "MSA-A", 20),
            business("PC", "Private Customer", "MSA-A", 30),
            business("BC", "Business Customer", "MSA-B", 40),
            business("MS", "Mini Single View", "MSA-B", 50),
            business("SV", "Single View", "MSA-B", 60),
            business("PD", "Product", "MSA-B", 70),
            business("OM", "Operation Management", "GATEWAY", 80),
            business("EB", "Enterprise Banking", "GATEWAY", 90),
            business("EP", "Enterprise Product", "GATEWAY", 100),
            business("MG", "Marketing", "GATEWAY", 110),
            business("SS", "Self Service", "GATEWAY", 120),
            business("JWT", "JWT Auth", "GATEWAY", 130)
    );

    private static final Map<String, RouteGroupCode> GROUP_BY_CODE = indexGroups();
    private static final Map<String, BusinessCode> BUSINESS_BY_CODE = indexBusiness();

    public static List<RouteGroupCode> routeGroups() {
        return ROUTE_GROUPS;
    }

    public static List<BusinessCode> businessCodes() {
        return BUSINESS_CODES;
    }

    public static Optional<RouteGroupCode> findGroup(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(GROUP_BY_CODE.get(code.toUpperCase(Locale.ROOT)));
    }

    public static Optional<BusinessCode> findBusiness(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BUSINESS_BY_CODE.get(code.toUpperCase(Locale.ROOT)));
    }

    public static List<Map<String, Object>> routeGroupViews() {
        return ROUTE_GROUPS.stream().map(group -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("code", group.code());
            view.put("name", group.name());
            return view;
        }).toList();
    }

    public static List<Map<String, Object>> businessCodeViews() {
        return BUSINESS_CODES.stream().map(business -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("code", business.code());
            view.put("name", business.name());
            view.put("routeGroupCode", business.routeGroupCode());
            view.put("contextPath", business.contextPath());
            view.put("sortOrder", business.sortOrder());
            view.put("localBootrunPort", business.localBootrunPort());
            view.put("localTargetBaseUrl", business.localTargetBaseUrl());
            view.put("healthCheckPath", business.localHealthCheckPath());
            view.put("localHealthCheckPath", business.localHealthCheckPath());
            return view;
        }).toList();
    }

    private static BusinessCode business(String code, String name, String routeGroupCode, int sortOrder) {
        GatewayBusinessModules.Module module = GatewayBusinessModules.require(code);
        String contextPath = localContextPath(module);
        int port = module.bootrunPort();
        String baseUrl = "http://127.0.0.1:" + port;
        String healthCheckPath = localHealthCheckPath(module, contextPath);
        return new BusinessCode(code, name, routeGroupCode, contextPath, sortOrder, port, baseUrl, healthCheckPath);
    }

    private static String localContextPath(GatewayBusinessModules.Module module) {
        if ("JWT".equals(module.code())) {
            return "";
        }
        return module.pathPrefix();
    }

    private static String localHealthCheckPath(GatewayBusinessModules.Module module, String contextPath) {
        if ("JWT".equals(module.code())) {
            return "/actuator/health";
        }
        return contextPath + "/actuator/health";
    }

    private static Map<String, RouteGroupCode> indexGroups() {
        Map<String, RouteGroupCode> map = new LinkedHashMap<>();
        for (RouteGroupCode group : ROUTE_GROUPS) {
            map.put(group.code(), group);
        }
        return Map.copyOf(map);
    }

    private static Map<String, BusinessCode> indexBusiness() {
        Map<String, BusinessCode> map = new LinkedHashMap<>();
        for (BusinessCode business : BUSINESS_CODES) {
            map.put(business.code(), business);
        }
        return Map.copyOf(map);
    }
}
