package com.nh.nsight.tcf.core.support.security;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthorizationValidator {
    private final TcfProperties properties;

    public AuthorizationValidator(TcfProperties properties) {
        this.properties = properties;
    }

    public void validate(StandardHeader header) {
        if (!properties.isAuthorizationValidationEnabled()) {
            return;
        }
        if (!StringUtils.hasText(header.getBranchId())) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_DENIED, "지점 권한 정보를 확인할 수 없습니다.");
        }
    }
}
