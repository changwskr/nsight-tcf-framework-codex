package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.support.GatewayRouteCatalog;
import com.nh.nsight.gateway.support.GatewayRoute;
import com.nh.nsight.gateway.application.service.GatewayRouteAdminService;
import com.nh.nsight.gateway.application.service.GatewayRouteResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/routes")
public class GatewayRouteAdminController {
    private final GatewayRouteAdminService adminService;
    private final GatewayRouteResolver resolver;

    public GatewayRouteAdminController(GatewayRouteAdminService adminService, GatewayRouteResolver resolver) {
        this.adminService = adminService;
        this.resolver = resolver;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("currentEnvCode", resolver.currentEnvCode());
        meta.put("envCodes", List.of("LOCAL", "DEV", "PRD"));
        meta.put("routeGroupCodes", GatewayRouteCatalog.routeGroupViews());
        meta.put("businessCodes", GatewayRouteCatalog.businessCodeViews());
        return meta;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String envCode) {
        return adminService.listRoutes(envCode);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String routeId) {
        return adminService.getRoute(routeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/preview/{businessCode}")
    public ResponseEntity<Map<String, Object>> preview(
            @PathVariable String businessCode,
            @RequestParam(required = false) String envCode) {
        return adminService.preview(businessCode, envCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody GatewayRoute route) {
        return adminService.saveRoute(route);
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String routeId,
            @RequestBody GatewayRoute route) {
        GatewayRoute updated = new GatewayRoute(
                routeId,
                route.envCode(),
                route.routeGroupCode(),
                route.routeGroupName(),
                route.businessCode(),
                route.businessName(),
                route.targetBaseUrl(),
                route.contextPath(),
                route.onlinePath(),
                route.healthCheckPath(),
                route.connectTimeoutMs(),
                route.readTimeoutMs(),
                route.useYn(),
                route.sortOrder(),
                route.description()
        );
        return ResponseEntity.ok(adminService.saveRoute(updated));
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> delete(@PathVariable String routeId) {
        return adminService.deleteRoute(routeId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
