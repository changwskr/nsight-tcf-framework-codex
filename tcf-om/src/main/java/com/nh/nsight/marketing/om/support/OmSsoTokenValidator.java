package com.nh.nsight.marketing.om.support;

import com.nh.nsight.marketing.om.config.OmSsoProperties;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OmSsoTokenValidator {
    private final OmSsoProperties properties;

    public OmSsoTokenValidator(OmSsoProperties properties) {
        this.properties = properties;
    }

    public String validateAndResolveUserId(Map<String, Object> body) {
        String ssoToken = OmBodySupport.stringValue(body, "ssoToken");
        if (!StringUtils.hasText(ssoToken)) {
            throw new BusinessException("E-OM-SSO-0001", "SSO token이 필요합니다.");
        }

        String ssoSubject = OmBodySupport.stringValue(body, "ssoSubject");
        String userId = OmBodySupport.stringValue(body, "userId");
        String resolvedUserId = StringUtils.hasText(ssoSubject) ? ssoSubject.trim() : userId;
        if (!StringUtils.hasText(resolvedUserId)) {
            throw new BusinessException("E-OM-SSO-0001", "SSO subject 또는 userId가 필요합니다.");
        }

        if (properties.isMockEnabled()) {
            if (ssoToken.trim().length() < 8) {
                throw new BusinessException("E-OM-SSO-0001", "SSO token이 유효하지 않습니다.");
            }
            return resolvedUserId.trim();
        }

        throw new BusinessException("E-OM-SSO-0002", "SSO IdP 연동이 구성되지 않았습니다.");
    }
}
