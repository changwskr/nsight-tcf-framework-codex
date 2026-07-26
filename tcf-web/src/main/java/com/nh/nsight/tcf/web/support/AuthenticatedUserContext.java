package com.nh.nsight.tcf.web.support;

import org.springframework.security.oauth2.jwt.Jwt;

public record AuthenticatedUserContext(
        String userId,
        String branchId,
        String channelId,
        String jti,
        Jwt jwt
) {
}
