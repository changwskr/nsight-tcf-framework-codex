package nhnis.mg.jw.a.application.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import nhnis.fw.exception.BizException;
import nhnis.fw.exception.ExceptionCodeProperties;
import nhnis.mg.jw.a.config.JwtRuntimePolicy;
import nhnis.mg.jw.a.config.JwtSecurityProperties;
import nhnis.mg.jw.a.dto.mgjwa1000C0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000C0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1000C1DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000C1DTOout;
import nhnis.mg.jw.a.dto.mgjwa1000D0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000D0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1000D1DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000D1DTOout;
import nhnis.mg.jw.a.dto.mgjwa1000U0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000U0DTOout;
import nhnis.mg.jw.a.persistence.dao.mgjwa1000DAO;
import nhnis.mg.jw.a.support.DateTimeUtil;
import nhnis.mg.jw.a.support.JwtClientContext;
import nhnis.mg.jw.a.support.JwtInternalCallValidator;
import nhnis.mg.jw.a.support.JwtSupport;
import nhnis.mg.jw.a.support.JwtTokenIssuer;
import nhnis.mg.jw.a.support.JwtTokenStore;

/**
 * JWT 인증(로그인/갱신/폐기/로그아웃/SSO).
 */
@Service
public class mgjwa1000Service {

    private final mgjwa1000DAO dao;
    private final mgjwa1002Service loginHistoryService;
    private final JwtTokenIssuer tokenIssuer;
    private final JwtTokenStore tokenStore;
    private final JwtSecurityProperties properties;
    private final JwtRuntimePolicy runtimePolicy;
    private final PasswordEncoder passwordEncoder;
    private final JwtInternalCallValidator internalCallValidator;
    private final ExceptionCodeProperties exceptionCodes;

    public mgjwa1000Service(mgjwa1000DAO dao, mgjwa1002Service loginHistoryService,
            JwtTokenIssuer tokenIssuer, JwtTokenStore tokenStore,
            JwtSecurityProperties properties, JwtRuntimePolicy runtimePolicy,
            PasswordEncoder passwordEncoder, JwtInternalCallValidator internalCallValidator,
            ExceptionCodeProperties exceptionCodes) {
        this.dao = dao;
        this.loginHistoryService = loginHistoryService;
        this.tokenIssuer = tokenIssuer;
        this.tokenStore = tokenStore;
        this.properties = properties;
        this.runtimePolicy = runtimePolicy;
        this.passwordEncoder = passwordEncoder;
        this.internalCallValidator = internalCallValidator;
        this.exceptionCodes = exceptionCodes;
    }

    public mgjwa1000C0DTOout mgjwa1000C0(mgjwa1000C0DTOin input) {
        String userId = input == null ? null : input.getUserId();
        try {
            if (!StringUtils.hasText(userId)) {
                throw new BizException("E-JWT-VAL-0001", "userId");
            }
            if (!StringUtils.hasText(input.getPassword())) {
                throw new BizException("E-JWT-VAL-0001", "password");
            }
            Map<String, Object> user = dao.selectUserForLogin(userId);
            if (user == null || !"Y".equalsIgnoreCase(String.valueOf(user.get("useYn")))) {
                throw new BizException("E-JWT-AUTH-0001", "아이디 또는 비밀번호가 올바르지 않습니다.");
            }
            String passwordHash = JwtSupport.stringValue(user, "passwordHash");
            if (passwordHash == null || !passwordEncoder.matches(input.getPassword(), passwordHash)) {
                throw new BizException("E-JWT-AUTH-0001", "아이디 또는 비밀번호가 올바르지 않습니다.");
            }
            dao.updateUserLastLoginTime(Map.of("userId", userId, "lastLoginTime", DateTimeUtil.nowKst()));
            loginHistoryService.recordLoginHistory(userId, "SUCCESS", null);
            return issueTokenPair(new mgjwa1000C0DTOout(), user,
                    JwtClientContext.channelId(), JwtClientContext.clientIp(), null);
        } catch (BizException e) {
            loginHistoryService.recordLoginHistory(userId, "FAIL",
                    exceptionCodes.message(e.getCode(), e.getArgs()));
            throw e;
        }
    }

    public mgjwa1000C1DTOout mgjwa1000C1(mgjwa1000C1DTOin input) {
        if (input == null) {
            input = new mgjwa1000C1DTOin();
        }
        Map<String, Object> validationBody = new LinkedHashMap<>();
        validationBody.put("userId", input.getUserId());
        validationBody.put("issuer", input.getIssuer());
        validationBody.put("ssoSubject", input.getSsoSubject());
        validationBody.put("ssoAssertionId", input.getSsoAssertionId());
        try {
            internalCallValidator.validate(validationBody);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("E-JWT-INT-0001", "내부 호출 검증에 실패했습니다.");
        }
        if (!StringUtils.hasText(input.getUserId())) {
            throw new BizException("E-JWT-VAL-0001", "userId");
        }
        String userId = input.getUserId();
        Map<String, Object> user = resolveSsoUser(input);
        if (dao.selectUserForLogin(userId) != null) {
            dao.updateUserLastLoginTime(Map.of("userId", userId, "lastLoginTime", DateTimeUtil.nowKst()));
        }
        String auditNote = "issuer=" + input.getIssuer() + ", assertion=" + input.getSsoAssertionId();
        loginHistoryService.recordLoginHistory(userId, "SSO_SUCCESS", auditNote);

        mgjwa1000C1DTOout output = issueTokenPair(new mgjwa1000C1DTOout(), user,
                JwtClientContext.channelId(), JwtClientContext.clientIp(), null);
        output.setAuthType("sso-jwt");
        output.setIssuer(input.getIssuer());
        output.setSsoSubject(input.getSsoSubject());
        output.setSsoAssertionId(input.getSsoAssertionId());
        return output;
    }

    public mgjwa1000U0DTOout mgjwa1000U0(mgjwa1000U0DTOin input) {
        if (input == null || !StringUtils.hasText(input.getRefreshToken())) {
            throw new BizException("E-JWT-VAL-0001", "refreshToken");
        }
        String plainRefresh = input.getRefreshToken();
        Map<String, Object> stored = dao.selectRefreshTokenByHash(JwtSupport.sha256Hex(plainRefresh));
        if (stored == null) {
            throw new BizException("E-JWT-AUTH-0002", "Refresh Token이 유효하지 않습니다.");
        }
        if ("Y".equalsIgnoreCase(String.valueOf(stored.get("revokedYn")))
                || "Y".equalsIgnoreCase(String.valueOf(stored.get("rotatedYn")))) {
            throw new BizException("E-JWT-AUTH-0002", "Refresh Token이 이미 사용되었거나 폐기되었습니다.");
        }
        Instant expiresAt = ((Timestamp) stored.get("expiresAt")).toInstant();
        if (Instant.now().isAfter(expiresAt)) {
            throw new BizException("E-JWT-AUTH-0002", "Refresh Token이 만료되었습니다.");
        }
        if (runtimePolicy.isRefreshTokenRotationEnabled()) {
            dao.markRefreshTokenRotated(JwtSupport.stringValue(stored, "refreshTokenId"));
        }
        String userId = JwtSupport.stringValue(stored, "userId");
        Map<String, Object> user = dao.selectUserForLogin(userId);
        if (user == null || !"Y".equalsIgnoreCase(String.valueOf(user.get("useYn")))) {
            throw new BizException("E-JWT-AUTH-0001", "사용자를 찾을 수 없습니다.");
        }
        return issueTokenPair(new mgjwa1000U0DTOout(), user,
                JwtClientContext.channelId(), JwtClientContext.clientIp(),
                JwtSupport.stringValue(stored, "tokenFamilyId"));
    }

    public mgjwa1000D0DTOout mgjwa1000D0(mgjwa1000D0DTOin input) {
        String accessToken = input == null ? null : input.getAccessToken();
        String jti = input == null ? null : input.getJti();
        String reason = input == null ? null : input.getReason();
        if (!StringUtils.hasText(reason)) {
            reason = "REVOKE";
        }
        if (StringUtils.hasText(accessToken)) {
            tokenStore.denylistFromAccessToken(stripBearer(accessToken), reason);
        } else if (StringUtils.hasText(jti)) {
            tokenStore.denylist(jti, null, Instant.now().plusSeconds(3600), reason);
        } else {
            throw new BizException("E-JWT-VAL-0002", "accessToken 또는 jti 가 필요합니다.");
        }
        mgjwa1000D0DTOout output = new mgjwa1000D0DTOout();
        output.setBusinessCode("JW");
        output.setRevoked(true);
        output.setReason(reason);
        return output;
    }

    public mgjwa1000D1DTOout mgjwa1000D1(mgjwa1000D1DTOin input) {
        String accessToken = input == null ? null : input.getAccessToken();
        String refreshToken = input == null ? null : input.getRefreshToken();
        if (StringUtils.hasText(accessToken)) {
            tokenStore.denylistFromAccessToken(stripBearer(accessToken), "LOGOUT");
        }
        if (StringUtils.hasText(refreshToken)) {
            Map<String, Object> stored = dao.selectRefreshTokenByHash(JwtSupport.sha256Hex(refreshToken));
            if (stored != null) {
                Map<String, Object> revoke = new HashMap<>();
                revoke.put("refreshTokenId", stored.get("refreshTokenId"));
                revoke.put("revokedAt", Timestamp.from(Instant.now()));
                dao.revokeRefreshToken(revoke);
            }
        }
        mgjwa1000D1DTOout output = new mgjwa1000D1DTOout();
        output.setBusinessCode("JW");
        output.setLoggedOut(true);
        return output;
    }

    private Map<String, Object> resolveSsoUser(mgjwa1000C1DTOin input) {
        String userId = input.getUserId();
        Map<String, Object> stored = dao.selectUserForLogin(userId);
        if (stored != null && "Y".equalsIgnoreCase(String.valueOf(stored.get("useYn")))) {
            return stored;
        }
        String issuer = input.getIssuer();
        if (issuer == null || !issuer.startsWith("OM-SSO")) {
            throw new BizException("E-JWT-AUTH-0001", "SSO 사용자를 찾을 수 없습니다.");
        }
        Map<String, Object> trusted = new LinkedHashMap<>();
        trusted.put("userId", userId);
        trusted.put("userName", input.getUserName());
        trusted.put("branchId", input.getBranchId());
        trusted.put("authGroupId", input.getAuthGroupId());
        trusted.put("authGroupName", input.getAuthGroupName());
        trusted.put("useYn", "Y");
        return trusted;
    }

    /**
     * Access/Refresh Token 쌍을 발급하고, {@code output}(공통 필드는 {@link mgjwa1000C0DTOout} 상속)에 채워 반환한다.
     */
    private <T extends mgjwa1000C0DTOout> T issueTokenPair(T output, Map<String, Object> user, String channelId,
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

        output.setBusinessCode("JW");
        output.setScreen("JWT 로그인");
        output.setAuthType("jwt");
        output.setUserId(access.userId());
        output.setUserName(JwtSupport.stringValue(user, "userName"));
        output.setBranchId(access.branchId());
        output.setAuthGroupId(access.authGroupId());
        output.setAuthGroupName(JwtSupport.stringValue(user, "authGroupName"));
        output.setAccessToken(access.tokenValue());
        output.setRefreshToken(plainRefresh);
        output.setTokenType(properties.getTokenPrefix());
        output.setExpiresIn(runtimePolicy.getAccessTokenValidMinutes() * 60);
        output.setIssuer(runtimePolicy.getIssuer());
        output.setAudience(runtimePolicy.getAudience());
        output.setJti(access.jti());
        return output;
    }

    private String stripBearer(String token) {
        String prefix = properties.getTokenPrefix() + " ";
        if (token.startsWith(prefix)) {
            return token.substring(prefix.length()).trim();
        }
        return token.trim();
    }
}
