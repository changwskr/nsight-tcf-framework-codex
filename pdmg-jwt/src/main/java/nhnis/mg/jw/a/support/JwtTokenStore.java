package nhnis.mg.jw.a.support;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.nimbusds.jwt.SignedJWT;

import nhnis.mg.jw.a.config.JwtRuntimePolicy;
import nhnis.mg.jw.a.persistence.dao.mgjwa1000DAO;

@Component
public class JwtTokenStore {
    private final mgjwa1000DAO dao;
    private final JwtRuntimePolicy runtimePolicy;

    public JwtTokenStore(mgjwa1000DAO dao, JwtRuntimePolicy runtimePolicy) {
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
            Instant issuedAt, Instant expiresAt, String clientIp, String userAgent) {
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
            throw new nhnis.fw.exception.BizException("E-JWT-AUTH-0003");
        }
    }

    public boolean isDenied(String jti) {
        if (!runtimePolicy.isDenylistCheckEnabled()) {
            return false;
        }
        return dao.countDenylist(Map.of("issuer", runtimePolicy.getIssuer(), "jti", jti)) > 0;
    }
}
