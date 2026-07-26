package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import com.nh.nsight.tcf.cache.support.TcfCacheSupport;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class OmCacheService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;
    private final ObjectProvider<TcfCacheSupport> tcfCacheSupport;

    public OmCacheService(OmOperationRule rule,
                          OmOperationDao dao,
                          OmChangeRecorder recorder,
                          ObjectProvider<TcfCacheSupport> tcfCacheSupport) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
        this.tcfCacheSupport = tcfCacheSupport;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        putIfPresent(body, criteria, "cacheName", "cacheKey");

        TcfCacheSupport cacheSupport = tcfCacheSupport.getIfAvailable();
        List<Map<String, Object>> rows;
        boolean fromEhCache = false;
        if (cacheSupport != null) {
            rows = cacheSupport.snapshotEntries(
                    OmBodySupport.stringValue(criteria, "cacheName"),
                    OmBodySupport.stringValue(criteria, "cacheKey"));
            fromEhCache = true;
        } else {
            rows = dao.searchCacheStatus(criteria);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "Cache 조회");
        result.put("rows", rows);
        result.put("totalCount", rows.size());
        result.put("fromEhCache", fromEhCache);
        return result;
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "cacheName");
        rule.requireReason(body, "deleteReason");

        Map<String, Object> criteria = new HashMap<>();
        String cacheName = OmBodySupport.stringValue(body, "cacheName");
        criteria.put("cacheName", cacheName);
        String cacheKey = OmBodySupport.stringValue(body, "cacheKey");
        if (cacheKey != null) {
            criteria.put("cacheKey", cacheKey);
        }

        TcfCacheSupport cacheSupport = tcfCacheSupport.getIfAvailable();
        int deleted;
        if (cacheSupport != null) {
            if (cacheKey != null) {
                cacheSupport.evict(cacheName, cacheKey);
                deleted = 1;
            } else {
                cacheSupport.evictAll(cacheName);
                deleted = 1;
            }
        } else {
            deleted = dao.deleteCache(criteria);
        }

        if (deleted == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 Cache 항목이 없습니다.");
        }

        String reason = OmBodySupport.stringValue(body, "deleteReason");
        recorder.recordAdminAudit(context, "CACHE_DELETE", "Cache 삭제", reason, "SUCCESS");
        recorder.recordAuthHistory(context, "CACHE", cacheName,
                "exists", "deleted:" + deleted, reason);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "Cache 삭제");
        result.put("deletedCount", deleted);
        result.put("cacheName", cacheName);
        result.put("fromEhCache", cacheSupport != null);
        return result;
    }

    private void putIfPresent(Map<String, Object> body, Map<String, Object> criteria, String... keys) {
        for (String key : keys) {
            String value = OmBodySupport.stringValue(body, key);
            if (value != null) {
                criteria.put(key, value);
            }
        }
    }
}
