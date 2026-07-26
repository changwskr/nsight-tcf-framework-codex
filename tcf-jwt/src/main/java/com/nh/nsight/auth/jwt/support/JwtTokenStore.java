package com.nh.nsight.auth.jwt.support;

import com.nh.nsight.auth.jwt.config.JwtRuntimePolicy;
import com.nh.nsight.auth.jwt.persistence.dao.JwtTokenDao;
import com.nimbusds.jwt.SignedJWT;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenStore {
    private final JwtTokenDao dao;
    private final JwtRuntimePolicy runtimePolicy;

    public JwtTokenStore(JwtTokenDao dao, JwtRuntimePolicy runtimePolicy) {
        this.dao = dao;
        this.runtimePolicy = runtimePolicy;
    }

    public void saveAccessToken(JwtTokenIssuer.IssuedAccessToken token) {
        Map<String, Object> row = new HashMap<>();
        row.put("tokenId", token.tokenId());
        row.put("jti", token.jti());
        row.put("issuer", runtimePolicy.getIssuer());
        row.put("userId", token.userId());
        row.put("branchId", token.branchId());
        row.put("channelId", token.channelId());
        row.put("authGroupId", token.authGroupId());
        row.put("tokenType", "ACCESS");
        row.put("audience", runtimePolicy.getAudience());
        row.put("issuedAt", Timestamp.from(token.issuedAt()));
        row.put("expiresAt", Timestamp.from(token.expiresAt()));
        row.put("clientIp", token.clientIp());
        row.put("userAgent", token.userAgent());
        dao.insertJwtToken(row);
    }

    public String saveRefreshToken(String userId, String plainToken, String familyId,
                                   Instant issuedAt, Instant expiresAt,
                                   String clientIp, String userAgent) {
        String refreshTokenId = JwtSupport.newId();
        Map<String, Object> row = new HashMap<>();
        row.put("refreshTokenId", refreshTokenId);
        row.put("userId", userId);
        row.put("tokenHash", JwtSupport.sha256Hex(plainToken));
        row.put("tokenFamilyId", familyId);
        row.put("issuedAt", Timestamp.from(issuedAt));
        row.put("expiresAt", Timestamp.from(expiresAt));
        row.put("clientIp", clientIp);
        row.put("userAgent", userAgent);
        dao.insertRefreshToken(row);
        return refreshTokenId;
    }

    public void denylist(String jti, String userId, Instant expiresAt, String reason) {
        Map<String, Object> row = new HashMap<>();
        row.put("issuer", runtimePolicy.getIssuer());
        row.put("jti", jti);
        row.put("userId", userId);
        row.put("expiresAt", Timestamp.from(expiresAt));
        row.put("revokedAt", Timestamp.from(Instant.now()));
        row.put("revokeReason", reason);
        dao.insertDenylist(row);

        Map<String, Object> revoke = new HashMap<>();
        revoke.put("issuer", runtimePolicy.getIssuer());
        revoke.put("jti", jti);
        revoke.put("revokedAt", Timestamp.from(Instant.now()));
        revoke.put("revokeReason", reason);
        dao.revokeJwtToken(revoke);
    }

    public void denylistFromAccessToken(String accessToken, String reason) {
        try {
            SignedJWT jwt = SignedJWT.parse(accessToken);
            String jti = jwt.getJWTClaimsSet().getJWTID();
            Instant exp = jwt.getJWTClaimsSet().getExpirationTime().toInstant();
            String sub = jwt.getJWTClaimsSet().getSubject();
            denylist(jti, sub, exp, reason);
        } catch (Exception e) {
            throw new com.nh.nsight.tcf.core.support.error.BusinessException(
                    "E-JWT-AUTH-0003", "유효하지 않은 Access Token입니다.");
        }
    }

    public boolean isDenied(String jti) {
        if (!runtimePolicy.isDenylistCheckEnabled()) {
            return false;
        }
        return dao.isDenied(runtimePolicy.getIssuer(), jti);
    }
}
