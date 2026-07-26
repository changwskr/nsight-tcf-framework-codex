package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OmServiceCatalogService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmServiceCatalogService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        copyIfPresent(body, criteria, "businessCode", "serviceId", "useYn", "pageNo", "pageSize");
        rule.normalizePaging(criteria);

        List<Map<String, Object>> rows = dao.searchServiceCatalog(criteria);
        int totalCount = dao.countServiceCatalog(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "ServiceId / 거래코드 관리");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> detail(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "catalogId");

        Map<String, Object> row = dao.selectServiceCatalogByKey(keyOf(body));
        if (row == null) {
            throw new BusinessException("E-OM-BIZ-0002", "서비스 카탈로그를 찾을 수 없습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "ServiceId 상세");
        result.put("row", row);
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "businessCode");
        rule.requireField(body, "serviceId");
        rule.requireField(body, "transactionCode");
        rule.requireField(body, "processingType");
        rule.requireReason(body, "changeReason");

        String serviceId = OmBodySupport.stringValue(body, "serviceId");
        if (dao.selectServiceCatalogByServiceId(serviceId) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 ServiceId입니다.");
        }

        String catalogId = OmBodySupport.stringValue(body, "catalogId");
        if (!StringUtils.hasText(catalogId)) {
            catalogId = "CAT-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (dao.selectServiceCatalogByKey(Map.of("catalogId", catalogId)) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 CatalogId입니다.");
        }

        Map<String, Object> row = toRow(body, catalogId);
        dao.insertServiceCatalog(row);
        recorder.recordAuthHistory(context, "SERVICE_CATALOG", catalogId,
                null, String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("ServiceId 등록", row, "REGISTER");
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "catalogId");
        rule.requireField(body, "businessCode");
        rule.requireField(body, "serviceId");
        rule.requireField(body, "transactionCode");
        rule.requireField(body, "processingType");
        rule.requireReason(body, "changeReason");

        Map<String, Object> key = keyOf(body);
        Map<String, Object> before = dao.selectServiceCatalogByKey(key);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "수정할 서비스 카탈로그를 찾을 수 없습니다.");
        }

        String serviceId = OmBodySupport.stringValue(body, "serviceId");
        Map<String, Object> existing = dao.selectServiceCatalogByServiceId(serviceId);
        if (existing != null && !key.get("catalogId").equals(existing.get("catalogId"))) {
            throw new BusinessException("E-OM-BIZ-0003", "다른 항목에서 사용 중인 ServiceId입니다.");
        }

        Map<String, Object> row = toRow(body, OmBodySupport.stringValue(body, "catalogId"));
        dao.updateServiceCatalog(row);
        recorder.recordAuthHistory(context, "SERVICE_CATALOG", String.valueOf(key.get("catalogId")),
                String.valueOf(before), String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("ServiceId 수정", row, "UPDATE");
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "catalogId");
        rule.requireReason(body, "changeReason");

        Map<String, Object> key = keyOf(body);
        Map<String, Object> before = dao.selectServiceCatalogByKey(key);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 서비스 카탈로그를 찾을 수 없습니다.");
        }

        int updated = dao.disableServiceCatalog(key);
        if (updated == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 서비스 카탈로그를 찾을 수 없습니다.");
        }

        recorder.recordAuthHistory(context, "SERVICE_CATALOG", String.valueOf(key.get("catalogId")),
                String.valueOf(before), "USE_YN=N", OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "ServiceId 삭제");
        result.put("deleted", true);
        result.put("catalogId", key.get("catalogId"));
        return result;
    }

    private Map<String, Object> keyOf(Map<String, Object> body) {
        Map<String, Object> key = new HashMap<>();
        key.put("catalogId", OmBodySupport.stringValue(body, "catalogId"));
        return key;
    }

    private Map<String, Object> toRow(Map<String, Object> body, String catalogId) {
        Map<String, Object> row = new HashMap<>();
        row.put("catalogId", catalogId);
        row.put("businessCode", OmBodySupport.stringValue(body, "businessCode"));
        row.put("serviceId", OmBodySupport.stringValue(body, "serviceId"));
        row.put("transactionCode", OmBodySupport.stringValue(body, "transactionCode"));
        row.put("processingType", OmBodySupport.stringValue(body, "processingType"));
        row.put("handlerClass", OmBodySupport.stringValue(body, "handlerClass"));
        row.put("authCode", OmBodySupport.stringValue(body, "authCode"));
        String auditYn = OmBodySupport.stringValue(body, "auditYn");
        row.put("auditYn", auditYn != null ? auditYn : "N");
        row.put("timeoutSec", OmBodySupport.intValue(body, "timeoutSec", 5));
        String useYn = OmBodySupport.stringValue(body, "useYn");
        row.put("useYn", useYn != null ? useYn : "Y");
        row.put("description", OmBodySupport.stringValue(body, "description"));
        return row;
    }

    private Map<String, Object> savedResult(String screen, Map<String, Object> row, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        result.put("saved", true);
        result.put("mode", mode);
        result.put("catalogId", row.get("catalogId"));
        result.put("serviceId", row.get("serviceId"));
        return result;
    }

    private void copyIfPresent(Map<String, Object> body, Map<String, Object> criteria, String... keys) {
        for (String key : keys) {
            String value = OmBodySupport.stringValue(body, key);
            if (value != null) {
                criteria.put(key, value);
            }
        }
    }
}
