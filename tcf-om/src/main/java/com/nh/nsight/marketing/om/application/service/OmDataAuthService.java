package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OmDataAuthService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;

    public OmDataAuthService(OmOperationRule rule, OmOperationDao dao) {
        this.rule = rule;
        this.dao = dao;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        String authGroupId = OmBodySupport.stringValue(body, "authGroupId");
        if (authGroupId != null) {
            criteria.put("authGroupId", authGroupId);
        }
        List<Map<String, Object>> rows = dao.searchDataAuths(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "데이터권한 관리");
        result.put("rows", rows);
        result.put("totalCount", rows.size());
        return result;
    }
}


