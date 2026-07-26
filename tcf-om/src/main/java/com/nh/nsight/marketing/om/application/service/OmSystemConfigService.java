package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmSystemConfigRuntimeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OmSystemConfigService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmSystemConfigRuntimeSupport runtimeSupport;

    public OmSystemConfigService(OmOperationRule rule, OmOperationDao dao,
                                 OmSystemConfigRuntimeSupport runtimeSupport) {
        this.rule = rule;
        this.dao = dao;
        this.runtimeSupport = runtimeSupport;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        String category = OmBodySupport.stringValue(body, "configCategory");
        if (category != null) {
            criteria.put("configCategory", category);
        }

        String deploymentMode = runtimeSupport.resolveDeploymentMode();
        List<Map<String, Object>> rows = mergeRows(deploymentMode, dao.searchSystemConfigs(criteria), category);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "환경설정 조회");
        result.put("deploymentMode", deploymentMode);
        result.put("readOnlyNotice", deploymentModeNotice(deploymentMode));
        result.put("rows", rows);
        result.put("totalCount", rows.size());
        return result;
    }

    private List<Map<String, Object>> mergeRows(String deploymentMode,
                                                List<Map<String, Object>> seedRows,
                                                String categoryFilter) {
        List<Map<String, Object>> merged = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();

        for (Map<String, Object> runtime : runtimeSupport.buildRuntimeRows(deploymentMode)) {
            if (matchesCategory(runtime, categoryFilter)) {
                merged.add(runtime);
                keys.add(String.valueOf(runtime.get("configKey")));
            }
        }

        for (Map<String, Object> seed : seedRows) {
            String key = String.valueOf(seed.get("configKey"));
            String seedCategory = String.valueOf(seed.get("configCategory"));
            if (keys.contains(key)) {
                continue;
            }
            if (!runtimeSupport.includeSeedRow(key, seedCategory, deploymentMode)) {
                continue;
            }
            if (!matchesCategory(seed, categoryFilter)) {
                continue;
            }
            merged.add(seed);
            keys.add(key);
        }
        return merged;
    }

    private boolean matchesCategory(Map<String, Object> row, String categoryFilter) {
        if (categoryFilter == null || categoryFilter.isBlank()) {
            return true;
        }
        return categoryFilter.equalsIgnoreCase(String.valueOf(row.get("configCategory")));
    }

    private String deploymentModeNotice(String deploymentMode) {
        if ("tomcat".equals(deploymentMode)) {
            return "Tomcat WAR 배포 기준 런타임 값입니다. 게이트웨이(:8080)와 context path(/om)를 포함하며 화면에서 직접 수정할 수 없습니다.";
        }
        return "Spring bootRun 기준 런타임 값입니다. 내장 Tomcat·개별 포트(8097 등) 설정을 표시하며 화면에서 직접 수정할 수 없습니다.";
    }
}


