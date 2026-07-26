package com.nh.nsight.gateway.support;

/** SESSIONDB 검증 후 downstream header 보정에 사용 */
public record GatewaySessionContext(
        String sessionId,
        String userId,
        String branchId,
        String channelId) {
}
