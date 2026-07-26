package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.client.OmJwtSsoClient;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import com.nh.nsight.marketing.om.application.rule.OmOperationRule;
import com.nh.nsight.marketing.om.support.OmAuthSessionSupport;
import com.nh.nsight.marketing.om.support.OmBodySupport;
import com.nh.nsight.marketing.om.support.OmSsoTokenValidator;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.util.DateTimeUtil;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class OmAuthService {
    private final OmOperationRule rule;
    private final OmOperationDao dao;
    private final PasswordEncoder passwordEncoder;
    private final OmAuthSessionSupport sessionSupport;
    private final OmSsoTokenValidator ssoTokenValidator;
    private final OmJwtSsoClient jwtSsoClient;

    public OmAuthService(OmOperationRule rule, OmOperationDao dao, PasswordEncoder passwordEncoder,
                         OmAuthSessionSupport sessionSupport, OmSsoTokenValidator ssoTokenValidator,
                         OmJwtSsoClient jwtSsoClient) {
        this.rule = rule;
        this.dao = dao;
        this.passwordEncoder = passwordEncoder;
        this.sessionSupport = sessionSupport;
        this.ssoTokenValidator = ssoTokenValidator;
        this.jwtSsoClient = jwtSsoClient;
    }

    public Map<String, Object> login(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        rule.requireField(body, "userId");
        rule.requireField(body, "password");

        String userId = OmBodySupport.stringValue(body, "userId");
        String password = OmBodySupport.stringValue(body, "password");

        Map<String, Object> user = dao.selectUserForLogin(userId);
        if (user == null || !"Y".equalsIgnoreCase(String.valueOf(user.get("useYn")))) {
            throw new BusinessException("E-OM-AUTH-0001", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String passwordHash = OmBodySupport.stringValue(user, "passwordHash");
        if (passwordHash == null || !passwordEncoder.matches(password, passwordHash)) {
            throw new BusinessException("E-OM-AUTH-0001", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String now = DateTimeUtil.nowKst();
        Map<String, Object> update = new HashMap<>();
        update.put("userId", userId);
        update.put("lastLoginTime", now);
        dao.updateUserLastLoginTime(update);

        HttpSession session = sessionSupport.createSession(user);
        user.put("lastLoginTime", now);

        Map<String, Object> result = new LinkedHashMap<>(sessionSupport.toSessionBody(
                sessionSupport.currentUser().orElse(null), true));
        result.put("screen", "OM 로그인");
        result.put("sessionId", session.getId());
        return result;
    }

    public Map<String, Object> ssoLogin(Map<String, Object> body, TransactionContext context) {
        rule.validateOperation(context);
        String userId = ssoTokenValidator.validateAndResolveUserId(body);

        Map<String, Object> user = dao.selectUserForLogin(userId);
        if (user == null || !"Y".equalsIgnoreCase(String.valueOf(user.get("useYn")))) {
            throw new BusinessException("E-OM-SSO-0001", "SSO 사용자를 찾을 수 없습니다.");
        }

        String now = DateTimeUtil.nowKst();
        Map<String, Object> update = new HashMap<>();
        update.put("userId", userId);
        update.put("lastLoginTime", now);
        dao.updateUserLastLoginTime(update);

        String channelId = context.getHeader() != null ? context.getHeader().getChannelId() : null;
        if (channelId == null || channelId.isBlank()) {
            channelId = OmBodySupport.stringValue(body, "channelId");
        }
        if (channelId == null || channelId.isBlank()) {
            channelId = "WEBTOP";
        }

        HttpSession session = sessionSupport.createSession(user, "SSO", channelId);
        user.put("lastLoginTime", now);

        Map<String, Object> tokenBody = jwtSsoClient.issueSsoToken(user, body, context);

        Map<String, Object> result = new LinkedHashMap<>(sessionSupport.toSessionBody(
                sessionSupport.currentUser().orElse(null), true));
        result.put("screen", "OM SSO 로그인");
        result.put("loginType", "SSO");
        result.put("channelId", channelId);
        result.put("sessionId", session.getId());
        result.put("accessToken", tokenBody.get("accessToken"));
        result.put("refreshToken", tokenBody.get("refreshToken"));
        result.put("tokenType", tokenBody.getOrDefault("tokenType", "Bearer"));
        result.put("expiresIn", tokenBody.get("expiresIn"));
        return result;
    }

    public Map<String, Object> logout(TransactionContext context) {
        rule.validateOperation(context);
        sessionSupport.invalidateSession();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "OM");
        result.put("screen", "OM 로그아웃");
        result.put("loggedIn", false);
        result.put("loggedOut", true);
        return result;
    }

    public Map<String, Object> session(TransactionContext context) {
        rule.validateOperation(context);
        return sessionSupport.toSessionBody(sessionSupport.currentUser().orElse(null),
                sessionSupport.currentUser().isPresent());
    }
}
