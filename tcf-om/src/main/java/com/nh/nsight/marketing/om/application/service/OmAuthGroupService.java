package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OmAuthGroupService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmAuthGroupService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        copyIfPresent(body, criteria, "authGroupId", "authGroupName", "useYn");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "권한그룹 관리");
        result.put("rows", dao.searchAuthGroups(criteria));
        return result;
    }

    public Map<String, Object> detail(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "authGroupId");

        String authGroupId = OmBodySupport.stringValue(body, "authGroupId");
        Map<String, Object> row = dao.selectAuthGroupById(authGroupId);
        if (row == null) {
            throw new BusinessException("E-OM-BIZ-0002", "권한그룹을 찾을 수 없습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "권한그룹 상세");
        result.put("row", row);
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "authGroupId");
        rule.requireField(body, "authGroupName");
        rule.requireReason(body, "changeReason");

        String authGroupId = OmBodySupport.stringValue(body, "authGroupId");
        if (dao.selectAuthGroupById(authGroupId) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 권한그룹 ID입니다.");
        }

        Map<String, Object> row = toRow(body);
        dao.insertAuthGroup(row);
        recorder.recordAuthHistory(context, "AUTH_GROUP", authGroupId,
                null, String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("권한그룹 등록", authGroupId, "REGISTER");
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "authGroupId");
        rule.requireField(body, "authGroupName");
        rule.requireReason(body, "changeReason");

        String authGroupId = OmBodySupport.stringValue(body, "authGroupId");
        Map<String, Object> before = dao.selectAuthGroupById(authGroupId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "수정할 권한그룹을 찾을 수 없습니다.");
        }

        Map<String, Object> row = toRow(body);
        dao.updateAuthGroup(row);
        recorder.recordAuthHistory(context, "AUTH_GROUP", authGroupId,
                String.valueOf(before), String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("권한그룹 수정", authGroupId, "UPDATE");
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "authGroupId");
        rule.requireReason(body, "changeReason");

        String authGroupId = OmBodySupport.stringValue(body, "authGroupId");
        Map<String, Object> before = dao.selectAuthGroupById(authGroupId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 권한그룹을 찾을 수 없습니다.");
        }

        validateDeletable(authGroupId);

        Map<String, Object> key = new HashMap<>();
        key.put("authGroupId", authGroupId);
        int updated = dao.disableAuthGroup(key);
        if (updated == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 권한그룹을 찾을 수 없습니다.");
        }

        recorder.recordAuthHistory(context, "AUTH_GROUP", authGroupId,
                String.valueOf(before), "USE_YN=N", OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "권한그룹 삭제");
        result.put("deleted", true);
        result.put("authGroupId", authGroupId);
        return result;
    }

    private void validateDeletable(String authGroupId) {
        Map<String, Object> userCriteria = new HashMap<>();
        userCriteria.put("authGroupId", authGroupId);
        userCriteria.put("useYn", "Y");
        if (dao.countUsers(userCriteria) > 0) {
            throw new BusinessException("E-OM-BIZ-0003", "사용 중인 사용자가 있는 권한그룹은 삭제할 수 없습니다.");
        }
        if (dao.countFunctionAuthByGroup(authGroupId) > 0) {
            throw new BusinessException("E-OM-BIZ-0003", "기능권한이 연결된 권한그룹은 삭제할 수 없습니다.");
        }
        if (dao.countDataAuthByGroup(authGroupId) > 0) {
            throw new BusinessException("E-OM-BIZ-0003", "데이터권한이 연결된 권한그룹은 삭제할 수 없습니다.");
        }
    }

    private Map<String, Object> toRow(Map<String, Object> body) {
        Map<String, Object> row = new HashMap<>();
        row.put("authGroupId", OmBodySupport.stringValue(body, "authGroupId"));
        row.put("authGroupName", OmBodySupport.stringValue(body, "authGroupName"));
        row.put("description", OmBodySupport.stringValue(body, "description"));
        row.put("useYn", OmBodySupport.stringValue(body, "useYn") != null ? body.get("useYn") : "Y");
        return row;
    }

    private Map<String, Object> savedResult(String screen, String authGroupId, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        result.put("saved", true);
        result.put("mode", mode);
        result.put("authGroupId", authGroupId);
        return result;
    }

    private void copyIfPresent(Map<String, Object> body, Map<String, Object> criteria, String... keys) {
        if (body == null) {
            return;
        }
        for (String key : keys) {
            String value = OmBodySupport.stringValue(body, key);
            if (value != null) {
                criteria.put(key, value);
            }
        }
    }
}
