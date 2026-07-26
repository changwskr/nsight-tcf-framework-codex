package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.util.DateTimeUtil;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OmCommonCodeService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;
    private final OmCommonCodeCacheService commonCodeCacheService;

    public OmCommonCodeService(OmOperationRule rule,
                               OmOperationDao dao,
                               OmChangeRecorder recorder,
                               OmCommonCodeCacheService commonCodeCacheService) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
        this.commonCodeCacheService = commonCodeCacheService;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        copyIfPresent(body, criteria, "codeGroup", "code", "codeName", "useYn", "pageNo", "pageSize");
        rule.normalizePaging(criteria);

        List<Map<String, Object>> rows;
        int totalCount;
        boolean fromCache = false;
        if (isGroupListInquiry(criteria)) {
            rows = buildCodeGroupRows(commonCodeCacheService.loadAllCodeGroupNames());
            totalCount = rows.size();
            rows = paginate(rows, criteria);
            fromCache = true;
        } else if (isCacheableInquiry(criteria)) {
            String codeGroup = OmBodySupport.stringValue(criteria, "codeGroup");
            List<Map<String, Object>> cached = commonCodeCacheService.loadByCodeGroup(codeGroup);
            List<Map<String, Object>> filtered = filterByUseYn(cached, OmBodySupport.stringValue(criteria, "useYn"));
            totalCount = filtered.size();
            rows = paginate(filtered, criteria);
            fromCache = true;
        } else {
            rows = dao.searchCommonCodes(criteria);
            totalCount = dao.countCommonCodes(criteria);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "공통코드 관리");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("fromCache", fromCache);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> detail(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "codeGroup");
        rule.requireField(body, "code");

        Map<String, Object> key = keyOf(body);
        Map<String, Object> row = commonCodeCacheService.findInGroup(
                String.valueOf(key.get("codeGroup")),
                String.valueOf(key.get("code")));
        if (row == null) {
            row = dao.selectCommonCodeByKey(key);
        }
        if (row == null) {
            throw new BusinessException("E-OM-BIZ-0002", "공통코드를 찾을 수 없습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "공통코드 상세");
        result.put("row", row);
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "codeGroup");
        rule.requireField(body, "code");
        rule.requireField(body, "codeName");
        rule.requireReason(body, "changeReason");

        Map<String, Object> key = keyOf(body);
        if (dao.selectCommonCodeByKey(key) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 공통코드입니다.");
        }

        String now = DateTimeUtil.nowKst();
        Map<String, Object> row = toRow(body, now, now);
        dao.insertCommonCode(row);
        commonCodeCacheService.evictCodeGroup(String.valueOf(row.get("codeGroup")));
        recorder.recordAuthHistory(context, "COMMON_CODE",
                row.get("codeGroup") + ":" + row.get("code"),
                null, String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("공통코드 등록", row, "REGISTER");
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "codeGroup");
        rule.requireField(body, "code");
        rule.requireField(body, "codeName");
        rule.requireReason(body, "changeReason");

        Map<String, Object> key = keyOf(body);
        Map<String, Object> before = dao.selectCommonCodeByKey(key);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "수정할 공통코드를 찾을 수 없습니다.");
        }

        String now = DateTimeUtil.nowKst();
        Map<String, Object> row = toRow(body, String.valueOf(before.get("createdAt")), now);
        dao.updateCommonCode(row);
        commonCodeCacheService.evictCodeGroup(String.valueOf(row.get("codeGroup")));
        recorder.recordAuthHistory(context, "COMMON_CODE",
                row.get("codeGroup") + ":" + row.get("code"),
                String.valueOf(before), String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("공통코드 수정", row, "UPDATE");
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "codeGroup");
        rule.requireField(body, "code");
        rule.requireReason(body, "changeReason");

        Map<String, Object> key = keyOf(body);
        Map<String, Object> before = dao.selectCommonCodeByKey(key);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 공통코드를 찾을 수 없습니다.");
        }

        Map<String, Object> row = new HashMap<>(key);
        row.put("updatedAt", DateTimeUtil.nowKst());
        int updated = dao.disableCommonCode(row);
        if (updated == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 공통코드를 찾을 수 없습니다.");
        }

        commonCodeCacheService.evictCodeGroup(String.valueOf(key.get("codeGroup")));
        recorder.recordAuthHistory(context, "COMMON_CODE",
                row.get("codeGroup") + ":" + row.get("code"),
                String.valueOf(before), "USE_YN=N", OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "공통코드 삭제");
        result.put("deleted", true);
        result.put("codeGroup", key.get("codeGroup"));
        result.put("code", key.get("code"));
        return result;
    }

    private boolean isGroupListInquiry(Map<String, Object> criteria) {
        if (OmBodySupport.stringValue(criteria, "codeGroup") != null) {
            return false;
        }
        if (OmBodySupport.stringValue(criteria, "code") != null) {
            return false;
        }
        return OmBodySupport.stringValue(criteria, "codeName") == null;
    }

    private List<Map<String, Object>> buildCodeGroupRows(List<String> groupNames) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String groupName : groupNames) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("codeGroup", groupName);
            row.put("code", groupName);
            row.put("codeName", groupName);
            rows.add(row);
        }
        return rows;
    }

    private boolean isCacheableInquiry(Map<String, Object> criteria) {
        if (OmBodySupport.stringValue(criteria, "codeGroup") == null) {
            return false;
        }
        if (OmBodySupport.stringValue(criteria, "code") != null) {
            return false;
        }
        return OmBodySupport.stringValue(criteria, "codeName") == null;
    }

    private List<Map<String, Object>> filterByUseYn(List<Map<String, Object>> rows, String useYn) {
        if (useYn == null) {
            return rows;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get("useYn");
            if (value != null && useYn.equalsIgnoreCase(String.valueOf(value))) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    private List<Map<String, Object>> paginate(List<Map<String, Object>> rows, Map<String, Object> criteria) {
        int pageNo = (Integer) criteria.get("pageNo");
        int pageSize = (Integer) criteria.get("pageSize");
        int from = (pageNo - 1) * pageSize;
        if (from >= rows.size()) {
            return List.of();
        }
        int to = Math.min(from + pageSize, rows.size());
        return new ArrayList<>(rows.subList(from, to));
    }

    private Map<String, Object> keyOf(Map<String, Object> body) {
        Map<String, Object> key = new HashMap<>();
        key.put("codeGroup", OmBodySupport.stringValue(body, "codeGroup"));
        key.put("code", OmBodySupport.stringValue(body, "code"));
        return key;
    }

    private Map<String, Object> toRow(Map<String, Object> body, String createdAt, String updatedAt) {
        Map<String, Object> row = new HashMap<>();
        row.put("codeGroup", OmBodySupport.stringValue(body, "codeGroup"));
        row.put("code", OmBodySupport.stringValue(body, "code"));
        row.put("codeName", OmBodySupport.stringValue(body, "codeName"));
        row.put("sortOrder", OmBodySupport.intValue(body, "sortOrder", 0));
        row.put("useYn", OmBodySupport.stringValue(body, "useYn") != null ? body.get("useYn") : "Y");
        row.put("description", OmBodySupport.stringValue(body, "description"));
        row.put("createdAt", createdAt);
        row.put("updatedAt", updatedAt);
        return row;
    }

    private Map<String, Object> savedResult(String screen, Map<String, Object> row, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        result.put("saved", true);
        result.put("mode", mode);
        result.put("codeGroup", row.get("codeGroup"));
        result.put("code", row.get("code"));
        return result;
    }

    private void copyIfPresent(Map<String, Object> body, Map<String, Object> criteria, String... keys) {
        if (body == null) {
            return;
        }
        for (String key : keys) {
            if (!body.containsKey(key) || body.get(key) == null) {
                continue;
            }
            if ("pageNo".equals(key) || "pageSize".equals(key)) {
                criteria.put(key, body.get(key));
                continue;
            }
            String value = OmBodySupport.stringValue(body, key);
            if (value != null) {
                criteria.put(key, value);
            }
        }
    }
}
