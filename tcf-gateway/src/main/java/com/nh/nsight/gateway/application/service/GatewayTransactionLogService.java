package com.nh.nsight.gateway.application.service;

import com.nh.nsight.gateway.config.GatewayProperties;
import com.nh.nsight.gateway.persistence.dao.GatewayTransactionLogDao;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GatewayTransactionLogService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final GatewayTransactionLogDao dao;
    private final GatewayProperties properties;

    public GatewayTransactionLogService(GatewayTransactionLogDao dao, GatewayProperties properties) {
        this.dao = dao;
        this.properties = properties;
    }

    public Map<String, Object> inquiry(Map<String, String> filters, int pageNo, int pageSize) {
        Map<String, Object> criteria = new HashMap<>();
        if (filters != null) {
            filters.forEach((key, value) -> {
                if (StringUtils.hasText(value)) {
                    criteria.put(key, value.trim());
                }
            });
        }
        int normalizedPageNo = pageNo < 1 ? 1 : pageNo;
        int normalizedPageSize = normalizePageSize(pageSize);
        criteria.put("pageNo", normalizedPageNo);
        criteria.put("pageSize", normalizedPageSize);
        criteria.put("offset", (normalizedPageNo - 1) * normalizedPageSize);

        List<Map<String, Object>> rows = dao.search(criteria);
        int totalCount = dao.count(criteria);
        Map<String, Object> summary = dao.summarize(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("screen", "Gateway 거래로그 조회");
        result.put("envCode", properties.getEnvCode());
        result.put("pageNo", normalizedPageNo);
        result.put("pageSize", normalizedPageSize);
        result.put("totalCount", totalCount);
        result.put("summary", summary);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> deleteAll(String confirmCode, String deleteReason) {
        if (!"DELETE_ALL".equals(confirmCode)) {
            throw new IllegalArgumentException("confirmCode=DELETE_ALL 이 필요합니다.");
        }
        if (!StringUtils.hasText(deleteReason) || deleteReason.trim().length() < 5) {
            throw new IllegalArgumentException("삭제 사유를 5자 이상 입력해야 합니다.");
        }
        int deletedCount = dao.deleteAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("screen", "Gateway 거래로그 전체 삭제");
        result.put("deletedCount", deletedCount);
        return result;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
