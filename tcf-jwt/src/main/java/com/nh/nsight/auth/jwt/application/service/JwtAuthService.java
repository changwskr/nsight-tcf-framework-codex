package com.nh.nsight.auth.jwt.application.service;

import com.nh.nsight.auth.jwt.config.JwtRuntimePolicy;
import com.nh.nsight.auth.jwt.config.JwtSecurityProperties;
import com.nh.nsight.auth.jwt.persistence.dao.JwtTokenDao;
import com.nh.nsight.auth.jwt.support.JwtClientContext;
import com.nh.nsight.auth.jwt.support.JwtInternalCallValidator;
import com.nh.nsight.auth.jwt.support.JwtSupport;
import com.nh.nsight.auth.jwt.support.JwtTokenIssuer;
import com.nh.nsight.auth.jwt.support.JwtTokenStore;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.util.DateTimeUtil;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtAuthService {
    private final JwtTokenDao dao;
    private final JwtTokenIssuer tokenIssuer;
    private final JwtTokenStore tokenStore;
    private final JwtSecurityProperties properties;
    private final JwtRuntimePolicy runtimePolicy;
    private final JwtAdminService adminService;
    private final PasswordEncoder passwordEncoder;
    private final JwtInternalCallValidator internalCallValidator;

    public JwtAuthService(JwtTokenDao dao, JwtTokenIssuer tokenIssuer, JwtTokenStore tokenStore,
                          JwtSecurityProperties properties, JwtRuntimePolicy runtimePolicy,
                          JwtAdminService adminService, PasswordEncoder passwordEncoder,
                          JwtInternalCallValidator internalCallValidator) {
        this.dao = dao;
        this.tokenIssuer = tokenIssuer;
        this.tokenStore = tokenStore;
        this.properties = properties;
        this.runtimePolicy = runtimePolicy;
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
        this.internalCallValidator = internalCallValidator;
    }

    public Map<String, Object> login(Map<String, Object> body, TransactionContext context) {
        String userId = JwtSupport.stringValue(body, "userId");
        try {
            JwtSupport.requireField(body, "userId");
            JwtSupport.requireField(body, "password");

            userId = JwtSupport.stringValue(body, "userId");
            String password = JwtSupport.stringValue(body, "password");
            Map<String, Object> user = dao.selectUserForLogin(userId);
            if (user == null || !"Y".equalsIgnoreCase(String.valueOf(user.get("useYn")))) {
                throw new BusinessException("E-JWT-AUTH-0001", "아이디 또는 비밀번호가 올바르지 않습니다.");
            }
            String passwordHash = JwtSupport.stringValue(user, "passwordHash");
            if (passwordHash == null || !passwordEncoder.matches(password, passwordHash)) {
                throw new BusinessException("E-JWT-AUTH-0001", "아이디 또는 비밀번호가 올바르지 않습니다.");
            }

            String now = DateTimeUtil.nowKst();
            dao.updateUserLastLoginTime(Map.of("userId", userId, "lastLoginTime", now));

            adminService.recordLoginHistory(userId, "SUCCESS", null, context);
            return issueTokenPair(user, JwtClientContext.channelId(context),
                    JwtClientContext.clientIp(context), null);
        } catch (BusinessException e) {
            adminService.recordLoginHistory(userId, "FAIL", e.getMessage(), context);
            throw e;
        }
    }

    public Map<String, Object> ssoIssue(Map<String, Object> body, TransactionContext context) {
        try {
            internalCallValidator.validate(body);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("E-JWT-INT-0001", "내부 호출 검증에 실패했습니다.");
        }

        JwtSupport.requireField(body, "userId");
        String userId = JwtSupport.stringValue(body, "userId");
        Map<String, Object> user = resolveSsoUser(body);

        String now = DateTimeUtil.nowKst();
        if (dao.selectUserForLogin(userId) != null) {
            dao.updateUserLastLoginTime(Map.of("userId", userId, "lastLoginTime", now));
        }

        String auditNote = "issuer=" + JwtSupport.stringValue(body, "issuer")
                + ", assertion=" + JwtSupport.stringValue(body, "ssoAssertionId");
        adminService.recordLoginHistory(userId, "SSO_SUCCESS", auditNote, context);

        Map<String, Object> result = issueTokenPair(user, JwtClientContext.channelId(context),
                JwtClientContext.clientIp(context), null);
        result.put("authType", "sso-jwt");
        result.put("issuer", JwtSupport.stringValue(body, "issuer"));
        result.put("ssoSubject", JwtSupport.stringValue(body, "ssoSubject"));
        result.put("ssoAssertionId", JwtSupport.stringValue(body, "ssoAssertionId"));
        return result;
    }

    public Map<String, Object> refresh(Map<String, Object> body, TransactionContext context) {
        JwtSupport.requireField(body, "refreshToken");
        String plainRefresh = JwtSupport.stringValue(body, "refreshToken");
        String hash = JwtSupport.sha256Hex(plainRefresh);
        Map<String, Object> stored = dao.selectRefreshTokenByHash(hash);
        if (stored == null) {
            throw new BusinessException("E-JWT-AUTH-0002", "Refresh Token이 유효하지 않습니다.");
        }
        if ("Y".equalsIgnoreCase(String.valueOf(stored.get("revokedYn")))
                || "Y".equalsIgnoreCase(String.valueOf(stored.get("rotatedYn")))) {
            throw new BusinessException("E-JWT-AUTH-0002", "Refresh Token이 이미 사용되었거나 폐기되었습니다.");
        }
        Instant expiresAt = ((Timestamp) stored.get("expiresAt")).toInstant();
        if (Instant.now().isAfter(expiresAt)) {
            throw new BusinessException("E-JWT-AUTH-0002", "Refresh Token이 만료되었습니다.");
        }

        String refreshTokenId = JwtSupport.stringValue(stored, "refreshTokenId");
        String userId = JwtSupport.stringValue(stored, "userId");
        if (runtimePolicy.isRefreshTokenRotationEnabled()) {
            dao.markRefreshTokenRotated(refreshTokenId);
        }

        Map<String, Object> user = dao.selectUserForLogin(userId);
        if (user == null || !"Y".equalsIgnoreCase(String.valueOf(user.get("useYn")))) {
            throw new BusinessException("E-JWT-AUTH-0001", "사용자를 찾을 수 없습니다.");
        }

        String familyId = JwtSupport.stringValue(stored, "tokenFamilyId");
        return issueTokenPair(user, JwtClientContext.channelId(context),
                JwtClientContext.clientIp(context), familyId);
    }

    public Map<String, Object> revoke(Map<String, Object> body, TransactionContext context) {
        String accessToken = JwtSupport.stringValue(body, "accessToken");
        String jti = JwtSupport.stringValue(body, "jti");
        String reason = JwtSupport.stringValue(body, "reason");
        if (reason == null || reason.isBlank()) {
            reason = "REVOKE";
        }
        if (accessToken != null && !accessToken.isBlank()) {
            tokenStore.denylistFromAccessToken(stripBearer(accessToken), reason);
        } else if (jti != null && !jti.isBlank()) {
            tokenStore.denylist(jti, null, Instant.now().plusSeconds(3600), reason);
        } else {
            throw new BusinessException("E-JWT-VAL-0001", "accessToken 또는 jti 가 필요합니다.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "JWT");
        result.put("revoked", true);
        result.put("reason", reason);
        return result;
    }

    public Map<String, Object> logout(Map<String, Object> body, TransactionContext context) {
        String accessToken = JwtSupport.stringValue(body, "accessToken");
        String refreshToken = JwtSupport.stringValue(body, "refreshToken");
        if (accessToken != null && !accessToken.isBlank()) {
            tokenStore.denylistFromAccessToken(stripBearer(accessToken), "LOGOUT");
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            Map<String, Object> stored = dao.selectRefreshTokenByHash(JwtSupport.sha256Hex(refreshToken));
            if (stored != null) {
                Map<String, Object> revoke = new HashMap<>();
                revoke.put("refreshTokenId", stored.get("refreshTokenId"));
                revoke.put("revokedAt", Timestamp.from(Instant.now()));
                dao.revokeRefreshToken(revoke);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "JWT");
        result.put("loggedOut", true);
        return result;
    }

    private Map<String, Object> resolveSsoUser(Map<String, Object> body) {
        String userId = JwtSupport.stringValue(body, "userId");
        Map<String, Object> stored = dao.selectUserForLogin(userId);
        if (stored != null && "Y".equalsIgnoreCase(String.valueOf(stored.get("useYn")))) {
            return stored;
        }
        String issuer = JwtSupport.stringValue(body, "issuer");
        if (issuer == null || !issuer.startsWith("OM-SSO")) {
            throw new BusinessException("E-JWT-AUTH-0001", "SSO 사용자를 찾을 수 없습니다.");
        }
        Map<String, Object> trusted = new LinkedHashMap<>();
        trusted.put("userId", userId);
        trusted.put("userName", JwtSupport.stringValue(body, "userName"));
        trusted.put("branchId", JwtSupport.stringValue(body, "branchId"));
        trusted.put("authGroupId", JwtSupport.stringValue(body, "authGroupId"));
        trusted.put("authGroupName", JwtSupport.stringValue(body, "authGroupName"));
        trusted.put("useYn", "Y");
        return trusted;
    }

    private Map<String, Object> issueTokenPair(Map<String, Object> user, String channelId,
                                               String clientIp, String existingFamilyId) {
        String jti = JwtSupport.newJti();
        JwtTokenIssuer.IssuedAccessToken access = tokenIssuer.issueAccessToken(
                user, jti, channelId, clientIp, null);
        tokenStore.saveAccessToken(access);

        String plainRefresh = JwtSupport.newRefreshTokenPlain();
        Instant refreshIssued = Instant.now();
        Instant refreshExpires = refreshIssued.plusSeconds(runtimePolicy.getRefreshTokenValidHours() * 3600L);
        String familyId = existingFamilyId != null ? existingFamilyId : JwtSupport.newId();
        tokenStore.saveRefreshToken(
                access.userId(), plainRefresh, familyId, refreshIssued, refreshExpires, clientIp, null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "JWT");
        result.put("screen", "JWT 로그인");
        result.put("authType", "jwt");
        result.put("userId", access.userId());
        result.put("userName", JwtSupport.stringValue(user, "userName"));
        result.put("branchId", access.branchId());
        result.put("authGroupId", access.authGroupId());
        result.put("authGroupName", JwtSupport.stringValue(user, "authGroupName"));
        result.put("accessToken", access.tokenValue());
        result.put("refreshToken", plainRefresh);
        result.put("tokenType", properties.getTokenPrefix());
        result.put("expiresIn", runtimePolicy.getAccessTokenValidMinutes() * 60);
        result.put("issuer", runtimePolicy.getIssuer());
        result.put("audience", runtimePolicy.getAudience());
        result.put("jti", access.jti());
        return result;
    }

    private String stripBearer(String token) {
        String prefix = properties.getTokenPrefix() + " ";
        if (token.startsWith(prefix)) {
            return token.substring(prefix.length()).trim();
        }
        return token.trim();
    }
}
