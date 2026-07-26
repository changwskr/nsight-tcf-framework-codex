package com.nh.nsight.tcf.core.support.security;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.util.http.GatewaySessionHeaderRulesUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthenticationContextValidator {
    private static final String LOG_PREFIX = "******* [TCF-AUTH-CTX] ";
    private static final String CTX_ATTR_JTI = "jwtJti";

    private final TcfProperties properties;

    public AuthenticationContextValidator(TcfProperties properties) {
        this.properties = properties;
    }

    public void validate(StandardHeader header, TransactionContext context) {
        if (!properties.isAuthenticationContextValidationEnabled()) {
            return;
        }
        AuthenticationContext auth = AuthenticationContextHolder.get();
        if (auth == null) {
            return;
        }
        System.out.println(LOG_PREFIX + "validate start userId=" + auth.userId()
                + " headerUserId=" + header.getUserId());
        validateClaimMatch("userId", auth.userId(), header.getUserId(), true);
        validateClaimMatch("branchId", auth.branchId(), header.getBranchId(), false);
        validateClaimMatch("channelId", auth.channelId(), header.getChannelId(), false);
        if (StringUtils.hasText(auth.jti())) {
            context.put(CTX_ATTR_JTI, auth.jti());
            MDC.put("jti", auth.jti());
        }
        System.out.println(LOG_PREFIX + "validate pass jti=" + auth.jti());
    }

    private static void validateClaimMatch(
            String claimName,
            String authValue,
            String headerValue,
            boolean userIdClaim) {
        if (!StringUtils.hasText(authValue)) {
            return;
        }
        if (!StringUtils.hasText(headerValue)) {
            return;
        }
        if (userIdClaim && GatewaySessionHeaderRulesUtils.isPlaceholderUserId(headerValue)) {
            return;
        }
        if (!authValue.trim().equalsIgnoreCase(headerValue.trim())) {
            throw new BusinessException(
                    ErrorCode.JWT_HEADER_CLAIM_MISMATCH,
                    "JWT " + claimName + "와 전문 Header " + claimName + "가 일치하지 않습니다.");
        }
    }
}
