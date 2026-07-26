package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OmFileDownloadService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;

    public OmFileDownloadService(OmOperationRule rule, OmOperationDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>(OmBodySupport.searchCriteria(body));
        rule.normalizePaging(criteria);
        putIfPresent(body, criteria, "userId", "businessCode", "resultStatus", "fileName");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "파일 다운로드 이력");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", dao.countFileDownloadLogs(criteria));
        result.put("rows", dao.searchFileDownloadLogs(criteria));
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


