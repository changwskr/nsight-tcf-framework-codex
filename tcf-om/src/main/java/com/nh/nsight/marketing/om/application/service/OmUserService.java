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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OmUserService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;
    private final PasswordEncoder passwordEncoder;

    public OmUserService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder,
                         PasswordEncoder passwordEncoder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        putIfPresent(body, criteria, "userId", "userName", "branchId", "authGroupId", "useYn");
        rule.normalizePaging(criteria);

        List<Map<String, Object>> rows = dao.searchUsers(criteria);
        int totalCount = dao.countUsers(criteria);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "사용자 관리");
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> detail(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "userId");

        Map<String, Object> row = dao.selectUserById(OmBodySupport.stringValue(body, "userId"));
        if (row == null) {
            throw new BusinessException("E-OM-BIZ-0002", "사용자를 찾을 수 없습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "사용자 상세");
        result.put("row", sanitizeRow(row));
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "userId");
        rule.requireField(body, "userName");
        rule.requireField(body, "authGroupId");
        rule.requireField(body, "password");
        rule.requireReason(body, "changeReason");

        String userId = OmBodySupport.stringValue(body, "userId");
        if (dao.selectUserById(userId) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 사용자 ID입니다.");
        }

        validateAuthGroup(OmBodySupport.stringValue(body, "authGroupId"));

        Map<String, Object> row = toRow(body);
        row.put("passwordHash", passwordEncoder.encode(OmBodySupport.stringValue(body, "password")));
        row.put("lastLoginTime", null);
        dao.insertUser(row);

        recorder.recordAuthHistory(context, "USER", userId, null, String.valueOf(sanitizeRow(row)),
                OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("사용자 등록", row, "REGISTER");
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "userId");
        rule.requireField(body, "userName");
        rule.requireField(body, "authGroupId");
        rule.requireReason(body, "changeReason");

        String userId = OmBodySupport.stringValue(body, "userId");
        Map<String, Object> before = dao.selectUserById(userId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "수정할 사용자를 찾을 수 없습니다.");
        }

        validateAuthGroup(OmBodySupport.stringValue(body, "authGroupId"));

        Map<String, Object> row = toRow(body);
        row.put("lastLoginTime", before.get("lastLoginTime"));
        String password = OmBodySupport.stringValue(body, "password");
        if (StringUtils.hasText(password)) {
            row.put("passwordHash", passwordEncoder.encode(password));
            dao.updateUserWithPassword(row);
        } else {
            dao.updateUser(row);
        }

        recorder.recordAuthHistory(context, "USER", userId, String.valueOf(sanitizeRow(before)),
                String.valueOf(sanitizeRow(row)), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("사용자 수정", row, "UPDATE");
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "userId");
        rule.requireReason(body, "changeReason");

        String userId = OmBodySupport.stringValue(body, "userId");
        Map<String, Object> before = dao.selectUserById(userId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 사용자를 찾을 수 없습니다.");
        }

        Map<String, Object> row = new HashMap<>();
        row.put("userId", userId);
        int updated = dao.disableUser(row);
        if (updated == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 사용자를 찾을 수 없습니다.");
        }

        recorder.recordAuthHistory(context, "USER", userId, String.valueOf(sanitizeRow(before)),
                "USE_YN=N", OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "사용자 삭제");
        result.put("deleted", true);
        result.put("userId", userId);
        return result;
    }

    private void validateAuthGroup(String authGroupId) {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("authGroupId", authGroupId);
        List<Map<String, Object>> groups = dao.searchAuthGroups(criteria);
        if (groups.isEmpty()) {
            throw new BusinessException("E-OM-VAL-0003", "존재하지 않는 권한그룹입니다.");
        }
    }

    private Map<String, Object> toRow(Map<String, Object> body) {
        Map<String, Object> row = new HashMap<>();
        row.put("userId", OmBodySupport.stringValue(body, "userId"));
        row.put("userName", OmBodySupport.stringValue(body, "userName"));
        row.put("branchId", OmBodySupport.stringValue(body, "branchId"));
        row.put("authGroupId", OmBodySupport.stringValue(body, "authGroupId"));
        row.put("useYn", OmBodySupport.stringValue(body, "useYn") != null ? body.get("useYn") : "Y");
        return row;
    }

    private Map<String, Object> sanitizeRow(Map<String, Object> row) {
        Map<String, Object> safe = new LinkedHashMap<>(row);
        safe.remove("passwordHash");
        safe.remove("PASSWORD_HASH");
        return safe;
    }

    private Map<String, Object> savedResult(String screen, Map<String, Object> row, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        result.put("saved", true);
        result.put("mode", mode);
        result.put("userId", row.get("userId"));
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
