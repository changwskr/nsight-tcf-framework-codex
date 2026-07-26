package com.nh.nsight.gateway.support;

/** OM 로그인 응답에서 추출한 세션 등록 스냅샷 (Gateway 로컬 세션 아님) */
public record OmLoginSnapshot(
        String sessionId,
        String userId,
        String userName,
        String branchId,
        String authGroupId,
        String authGroupName) {
}
