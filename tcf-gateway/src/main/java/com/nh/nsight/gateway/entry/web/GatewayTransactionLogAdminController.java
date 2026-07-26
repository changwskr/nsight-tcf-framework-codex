package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.support.GatewayRouteCatalog;
import com.nh.nsight.gateway.config.GatewayProperties;
import com.nh.nsight.gateway.application.service.GatewayTransactionLogService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/transaction-log")
public class GatewayTransactionLogAdminController {
    private final GatewayTransactionLogService transactionLogService;
    private final GatewayProperties properties;

    public GatewayTransactionLogAdminController(GatewayTransactionLogService transactionLogService,
                                                GatewayProperties properties) {
        this.transactionLogService = transactionLogService;
        this.properties = properties;
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("currentEnvCode", properties.getEnvCode());
        meta.put("businessCodes", GatewayRouteCatalog.businessCodeViews());
        return meta;
    }

    @GetMapping
    public Map<String, Object> inquiry(
            @RequestParam(required = false) String businessCode,
            @RequestParam(required = false) String envCode,
            @RequestParam(required = false) String guid,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String transactionCode,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String resultStatus,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        Map<String, String> filters = new LinkedHashMap<>();
        putIfPresent(filters, "businessCode", businessCode);
        putIfPresent(filters, "envCode", envCode);
        putIfPresent(filters, "guid", guid);
        putIfPresent(filters, "traceId", traceId);
        putIfPresent(filters, "serviceId", serviceId);
        putIfPresent(filters, "transactionCode", transactionCode);
        putIfPresent(filters, "userId", userId);
        putIfPresent(filters, "branchId", branchId);
        putIfPresent(filters, "resultStatus", resultStatus);
        putIfPresent(filters, "errorCode", errorCode);
        putIfPresent(filters, "fromDate", fromDate);
        putIfPresent(filters, "toDate", toDate);
        return transactionLogService.inquiry(filters, pageNo, pageSize);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAll(@RequestBody Map<String, String> body) {
        try {
            String confirmCode = body == null ? null : body.get("confirmCode");
            String deleteReason = body == null ? null : body.get("deleteReason");
            return ResponseEntity.ok(transactionLogService.deleteAll(confirmCode, deleteReason));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    private void putIfPresent(Map<String, String> filters, String key, String value) {
        if (value != null && !value.isBlank()) {
            filters.put(key, value.trim());
        }
    }
}
