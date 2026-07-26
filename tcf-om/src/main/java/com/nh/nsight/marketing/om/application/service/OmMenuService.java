package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmChangeRecorder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OmMenuService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmMenuService(OmOperationRule rule, OmOperationDao dao, OmChangeRecorder recorder) {
        this.rule = rule;
        this.dao = dao;
        this.recorder = recorder;
    }

    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        Map<String, Object> criteria = new HashMap<>();
        copyIfPresent(body, criteria, "menuId", "menuName", "useYn");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "메뉴 관리");
        result.put("rows", dao.searchMenus(criteria));
        return result;
    }

    public Map<String, Object> detail(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "menuId");

        String menuId = OmBodySupport.stringValue(body, "menuId");
        Map<String, Object> row = dao.selectMenuById(menuId);
        if (row == null) {
            throw new BusinessException("E-OM-BIZ-0002", "메뉴를 찾을 수 없습니다.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "메뉴 상세");
        result.put("row", row);
        return result;
    }

    public Map<String, Object> save(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "menuId");
        rule.requireField(body, "menuName");
        rule.requireReason(body, "changeReason");

        String menuId = OmBodySupport.stringValue(body, "menuId");
        if (dao.selectMenuById(menuId) != null) {
            throw new BusinessException("E-OM-BIZ-0003", "이미 등록된 메뉴 ID입니다.");
        }

        validateParentMenu(body, menuId);
        Map<String, Object> row = toRow(body);
        dao.insertMenu(row);
        recorder.recordAuthHistory(context, "MENU", menuId,
                null, String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("메뉴 등록", menuId, "REGISTER");
    }

    public Map<String, Object> update(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "menuId");
        rule.requireField(body, "menuName");
        rule.requireReason(body, "changeReason");

        String menuId = OmBodySupport.stringValue(body, "menuId");
        Map<String, Object> before = dao.selectMenuById(menuId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "수정할 메뉴를 찾을 수 없습니다.");
        }

        validateParentMenu(body, menuId);
        Map<String, Object> row = toRow(body);
        dao.updateMenu(row);
        recorder.recordAuthHistory(context, "MENU", menuId,
                String.valueOf(before), String.valueOf(row), OmBodySupport.stringValue(body, "changeReason"));

        return savedResult("메뉴 수정", menuId, "UPDATE");
    }

    public Map<String, Object> delete(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "menuId");
        rule.requireReason(body, "changeReason");

        String menuId = OmBodySupport.stringValue(body, "menuId");
        Map<String, Object> before = dao.selectMenuById(menuId);
        if (before == null) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 메뉴를 찾을 수 없습니다.");
        }
        if (dao.countChildMenus(menuId) > 0) {
            throw new BusinessException("E-OM-BIZ-0003", "하위 메뉴가 있는 메뉴는 삭제할 수 없습니다.");
        }

        Map<String, Object> key = new HashMap<>();
        key.put("menuId", menuId);
        int updated = dao.disableMenu(key);
        if (updated == 0) {
            throw new BusinessException("E-OM-BIZ-0002", "삭제할 메뉴를 찾을 수 없습니다.");
        }

        recorder.recordAuthHistory(context, "MENU", menuId,
                String.valueOf(before), "USE_YN=N", OmBodySupport.stringValue(body, "changeReason"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "메뉴 삭제");
        result.put("deleted", true);
        result.put("menuId", menuId);
        return result;
    }

    private void validateParentMenu(Map<String, Object> body, String menuId) {
        String parentMenuId = OmBodySupport.stringValue(body, "parentMenuId");
        if (!StringUtils.hasText(parentMenuId)) {
            return;
        }
        if (menuId.equals(parentMenuId)) {
            throw new BusinessException("E-OM-VAL-0002", "상위 메뉴는 자기 자신일 수 없습니다.");
        }
        if (dao.selectMenuById(parentMenuId) == null) {
            throw new BusinessException("E-OM-BIZ-0002", "상위 메뉴를 찾을 수 없습니다: " + parentMenuId);
        }
    }

    private Map<String, Object> toRow(Map<String, Object> body) {
        Map<String, Object> row = new HashMap<>();
        row.put("menuId", OmBodySupport.stringValue(body, "menuId"));
        row.put("menuName", OmBodySupport.stringValue(body, "menuName"));
        row.put("menuUrl", OmBodySupport.stringValue(body, "menuUrl"));
        String parentMenuId = OmBodySupport.stringValue(body, "parentMenuId");
        row.put("parentMenuId", StringUtils.hasText(parentMenuId) ? parentMenuId : null);
        row.put("sortOrder", OmBodySupport.intValue(body, "sortOrder", 0));
        row.put("useYn", OmBodySupport.stringValue(body, "useYn") != null ? body.get("useYn") : "Y");
        return row;
    }

    private Map<String, Object> savedResult(String screen, String menuId, String mode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", screen);
        result.put("saved", true);
        result.put("mode", mode);
        result.put("menuId", menuId);
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
