package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.config.GatewayProperties;
import com.nh.nsight.gateway.application.service.GatewayUserSessionAdminService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sessions")
public class GatewayUserSessionAdminController {
    private final GatewayUserSessionAdminService adminService;
    private final GatewayProperties properties;

    public GatewayUserSessionAdminController(GatewayUserSessionAdminService adminService,
                                             GatewayProperties properties) {
        this.adminService = adminService;
        this.properties = properties;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("currentEnvCode", properties.getEnvCode());
        meta.put("tableName", "TCF_USER_SESSION");
        return meta;
    }

    @GetMapping
    public Map<String, Object> inquiry(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "Y") String activeOnly,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return adminService.inquiry(userId, "Y".equalsIgnoreCase(activeOnly), pageNo, pageSize);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String sessionId) {
        return adminService.get(sessionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> forceLogout(@PathVariable String sessionId,
                                       @RequestBody(required = false) Map<String, String> body) {
        try {
            String deleteReason = body == null ? null : body.get("deleteReason");
            return ResponseEntity.ok(adminService.forceLogout(sessionId, deleteReason));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
