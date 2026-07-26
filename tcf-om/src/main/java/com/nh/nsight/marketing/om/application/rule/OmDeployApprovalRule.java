package com.nh.nsight.marketing.om.application.rule;

import com.nh.nsight.tcf.core.support.error.BusinessException;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OmDeployApprovalRule {
    private static final Set<String> BUILD_TYPES = Set.of("BUILD", "DEPLOY", "ROLLBACK");
    private static final Set<String> ENVS = Set.of("DEV", "STG", "PRD", "LOCAL");

    public void validateRequestType(String requestType) {
        if (requestType == null || !BUILD_TYPES.contains(requestType.toUpperCase())) {
            throw new BusinessException("E-OM-VAL-0002", "requestType은 BUILD, DEPLOY, ROLLBACK 중 하나여야 합니다.");
        }
    }

    public void validateEnvCode(String envCode) {
        if (envCode == null || !ENVS.contains(envCode.toUpperCase())) {
            throw new BusinessException("E-OM-VAL-0002", "envCode는 DEV, STG, PRD, LOCAL 중 하나여야 합니다.");
        }
    }

    public void requireStatus(Map<String, Object> request, String expected) {
        String status = request == null ? null : String.valueOf(request.get("status"));
        if (status == null || !expected.equalsIgnoreCase(status)) {
            throw new BusinessException("E-OM-BIZ-0003",
                    "요청 상태가 " + expected + " 이(가) 아닙니다. 현재: " + status);
        }
    }

    public void requireAnyStatus(Map<String, Object> request, String... expected) {
        String status = request == null ? null : String.valueOf(request.get("status"));
        if (status == null) {
            throw new BusinessException("E-OM-BIZ-0003", "배포 요청을 찾을 수 없습니다.");
        }
        for (String e : expected) {
            if (e.equalsIgnoreCase(status)) {
                return;
            }
        }
        throw new BusinessException("E-OM-BIZ-0003", "허용되지 않은 요청 상태입니다: " + status);
    }
}
