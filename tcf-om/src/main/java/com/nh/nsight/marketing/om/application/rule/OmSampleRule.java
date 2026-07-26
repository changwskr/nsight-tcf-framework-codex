package com.nh.nsight.marketing.om.application.rule;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OmSampleRule {
    public void validateInquiry(Map<String, Object> body, TransactionContext context) {
        if (!"OM".equalsIgnoreCase(context.getHeader().getBusinessCode())) {
            throw new BusinessException("E-OM-BIZ-0001", "업무코드가 OM가 아닙니다.");
        }
    }
}



