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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OmFunctionAuthService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmFunctionAuthService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        copyIfPresent(body, criteria, "authGroupId", "menuId");

        List<Map<String, Object>> rows = dao.searchFunctionAuths(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "기능권한 관리");
        result.put("rows", rows);
        result.put("totalCount", rows.size());
        return result;
    }

    public Map<String, Object> detail(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "authId");

        String authId = OmBodySupport.stringValue(body, "authId");
        Map<String, Object> row = dao.selectFunctionAuthById(authId);
        if (row == null) {
            throw new BusinessException("E-OM-BIZ-0002", "기능권한을 찾을 수 없습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "기능권한 상세");
        result.put("row", row);
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "authGroupId");
        rule.requireField(body, "menuId");
        rule.requireReason(body, "changeReason");

        String authGroupId = OmBodySupport.stringValue(body, "authGroupId");
        String menuId = OmBodySupport.stringValue(body, "menuId");
        validateReferences(authGroupId, menuId);

        String authId = OmBodySupport.stringValue(body, "authId");
        if (!StringUtils.hasText(authId)) {
            authId = "FA-" + authGroupId + "-" + menuId;
        }
        if (dao.selectFunctionAuthById(authId) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 기능권한 ID입니다.");
        }
        if (dao.countFunctionAuthByGroupAndMenu(authGroupId, menuId, null) > 0) {
            throw new BusinessException("E-OM-BIZ-0003", "동일 권한그룹·메뉴 조합이 이미 있습니다.");
        }

        Map<String, Object> row = toRow(body, authId);
        dao.insertFunctionAuth(row);
        recorder.recordAuthHistory(context, "FUNCTION_AUTH", authId,
                null, String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("기능권한 등록", authId, "REGISTER");
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "authId");
        rule.requireField(body, "authGroupId");
        rule.requireField(body, "menuId");
        rule.requireReason(body, "changeReason");

        String authId = OmBodySupport.stringValue(body, "authId");
        Map<String, Object> before = dao.selectFunctionAuthById(authId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "수정할 기능권한을 찾을 수 없습니다.");
        }

        String authGroupId = OmBodySupport.stringValue(body, "authGroupId");
        String menuId = OmBodySupport.stringValue(body, "menuId");
        validateReferences(authGroupId, menuId);
        if (dao.countFunctionAuthByGroupAndMenu(authGroupId, menuId, authId) > 0) {
            throw new BusinessException("E-OM-BIZ-0003", "동일 권한그룹·메뉴 조합이 이미 있습니다.");
        }

        Map<String, Object> row = toRow(body, authId);
        dao.updateFunctionAuth(row);
        recorder.recordAuthHistory(context, "FUNCTION_AUTH", authId,
                String.valueOf(before), String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("기능권한 수정", authId, "UPDATE");
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "authId");
        rule.requireReason(body, "changeReason");

        String authId = OmBodySupport.stringValue(body, "authId");
        Map<String, Object> before = dao.selectFunctionAuthById(authId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 기능권한을 찾을 수 없습니다.");
        }

        int deleted = dao.deleteFunctionAuthById(authId);
        if (deleted == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 기능권한을 찾을 수 없습니다.");
        }

        recorder.recordAuthHistory(context, "FUNCTION_AUTH", authId,
                String.valueOf(before), "DELETED", OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "기능권한 삭제");
        result.put("deleted", true);
        result.put("authId", authId);
        return result;
    }

    private void validateReferences(String authGroupId, String menuId) {
        if (dao.selectAuthGroupById(authGroupId) == null) {
            throw new BusinessException("E-OM-BIZ-0002", "권한그룹을 찾을 수 없습니다: " + authGroupId);
        }
        if (dao.selectMenuById(menuId) == null) {
            throw new BusinessException("E-OM-BIZ-0002", "메뉴를 찾을 수 없습니다: " + menuId);
        }
    }

    private Map<String, Object> toRow(Map<String, Object> body, String authId) {
        Map<String, Object> row = new HashMap<>();
        row.put("authId", authId);
        row.put("authGroupId", OmBodySupport.stringValue(body, "authGroupId"));
        row.put("menuId", OmBodySupport.stringValue(body, "menuId"));
        row.put("canInquiry", yn(body, "canInquiry"));
        row.put("canRegister", yn(body, "canRegister"));
        row.put("canUpdate", yn(body, "canUpdate"));
        row.put("canDelete", yn(body, "canDelete"));
        row.put("canDownload", yn(body, "canDownload"));
        return row;
    }

    private String yn(Map<String, Object> body, String key) {
        String value = OmBodySupport.stringValue(body, key);
        return "Y".equalsIgnoreCase(value) ? "Y" : "N";
    }

    private Map<String, Object> savedResult(String screen, String authId, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        result.put("saved", true);
        result.put("mode", mode);
        result.put("authId", authId);
        return result;
    }

    private void copyIfPresent(Map<String, Object> body, Map<String, Object> criteria, String... keys) {
        if (body == null) {
            return;
        }
        for (String key : keys) {
            String value = OmBodySupport.stringValue(body, key);
            if (StringUtils.hasText(value)) {
                criteria.put(key, value);
            }
        }
    }
}
