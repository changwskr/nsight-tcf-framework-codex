package com.nh.nsight.marketing.om.application.rule;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.nh.nsight.marketing.om.support.OmBodySupport;

@Component
public class OmOperationRule {
    public void validateOperation(TransactionContext context) {
        if (!"OM".equalsIgnoreCase(context.getHeader().getBusinessCode())) {
            throw new BusinessException("E-OM-BIZ-0001", "업무코드가 OM이 아닙니다.");
        }
    }

    public void normalizePaging(Map<String, Object> body) {
        int pageNo = Math.max(1, toInt(body.get("pageNo"), 1));
        int pageSize = toInt(body.get("pageSize"), 20);
        if (pageSize > 100) {
            pageSize = 100;
        }
        body.put("pageNo", pageNo);
        body.put("pageSize", pageSize);
        body.put("offset", (pageNo - 1) * pageSize);
    }

    public void requireReason(Map<String, Object> body, String fieldName) {
        String reason = OmBodySupport.stringValue(body, fieldName);
        if (reason == null || reason.length() < 5) {
            throw new BusinessException("E-OM-VAL-0001", fieldName + "을(를) 5자 이상 입력하세요.");
        }
    }

    public void requireField(Map<String, Object> body, String fieldName) {
        if (OmBodySupport.stringValue(body, fieldName) == null) {
            throw new BusinessException("E-OM-VAL-0002", fieldName + "은(는) 필수입니다.");
        }
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}



