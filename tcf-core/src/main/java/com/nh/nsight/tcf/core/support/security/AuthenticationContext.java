package com.nh.nsight.tcf.core.support.security;

/**
 * JWT Filter 또는 Gateway에서 검증된 사용자 인증 스냅샷.
 * tcf-core STF는 이 값과 표준 전문 Header 정합성을 비교한다.
 */
public record AuthenticationContext(
        String userId,
        String branchId,
        String channelId,
        String jti) {
}
