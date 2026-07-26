package com.nh.nsight.auth.jwt.application.service;

import com.nh.nsight.auth.jwt.config.JwtRuntimePolicy;
import com.nh.nsight.auth.jwt.persistence.dao.JwtAdminDao;
import com.nh.nsight.auth.jwt.support.JwtClientContext;
import com.nh.nsight.auth.jwt.support.JwtSupport;
import com.nh.nsight.auth.jwt.support.JwtTokenStore;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@DependsOn("jwtSchemaInitializer")
public class JwtAdminService {
    private static final String POLICY_ID = "DEFAULT";

    private final JwtAdminDao adminDao;
    private final JwtTokenStore tokenStore;
    private final JwtRuntimePolicy runtimePolicy;

    public JwtAdminService(JwtAdminDao adminDao, JwtTokenStore tokenStore, JwtRuntimePolicy runtimePolicy) {
        this.adminDao = adminDao;
        this.tokenStore = tokenStore;
        this.runtimePolicy = runtimePolicy;
    }

    @PostConstruct
    void loadPolicyFromDatabase() {
        Map<String, Object> row = adminDao.selectSecurityPolicy(POLICY_ID);
        if (row != null && !row.isEmpty()) {
            runtimePolicy.applyFromRow(row);
        } else {
            seedDefaultPolicy("SYSTEM");
        }
    }

    public Map<String, Object> inquiryTokens(Map<String, Object> body, TransactionContext context) {
        Map<String, Object> criteria = buildCriteria(body);
        List<Map<String, Object>> rows = adminDao.searchJwtTokens(criteria);
        return pageResult("토큰 현황", criteria, adminDao.countJwtTokens(criteria), rows);
    }

    public Map<String, Object> revokeTokenByJti(Map<String, Object> body, TransactionContext context) {
        JwtSupport.requireField(body, "jti");
        String jti = JwtSupport.stringValue(body, "jti");
        String reason = JwtSupport.stringValue(body, "reason");
        if (reason == null || reason.isBlank()) {
            reason = "ADMIN_REVOKE";
        }
        Map<String, Object> stored = adminDao.selectJwtTokenByJti(runtimePolicy.getIssuer(), jti);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        String userId = null;
        if (stored != null) {
            Object exp = stored.get("expiresAt");
            if (exp instanceof Timestamp ts) {
                expiresAt = ts.toInstant();
            }
            userId = JwtSupport.stringValue(stored, "userId");
        }
        tokenStore.denylist(jti, userId, expiresAt, reason);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "JWT");
        result.put("screen", "토큰 강제폐기");
        result.put("revoked", true);
        result.put("jti", jti);
        result.put("reason", reason);
        return result;
    }

    public Map<String, Object> inquiryLoginHistory(Map<String, Object> body, TransactionContext context) {
        Map<String, Object> criteria = buildCriteria(body);
        copyIfPresent(body, criteria, "loginResult", "fromDate", "toDate");
        List<Map<String, Object>> rows = adminDao.searchLoginHistories(criteria);
        return pageResult("로그인 이력", criteria, adminDao.countLoginHistories(criteria), rows);
    }

    public Map<String, Object> inquiryRefreshTokens(Map<String, Object> body, TransactionContext context) {
        Map<String, Object> criteria = buildCriteria(body);
        List<Map<String, Object>> rows = adminDao.searchRefreshTokens(criteria);
        return pageResult("Refresh Token 관리", criteria, adminDao.countRefreshTokens(criteria), rows);
    }

    public Map<String, Object> inquirySecurityPolicy(Map<String, Object> body, TransactionContext context) {
        Map<String, Object> row = adminDao.selectSecurityPolicy(POLICY_ID);
        if (row == null || row.isEmpty()) {
            row = new LinkedHashMap<>(runtimePolicy.toMap());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "JWT");
        result.put("screen", "보안정책 관리");
        result.put("policy", row);
        return result;
    }

    public Map<String, Object> updateSecurityPolicy(Map<String, Object> body, TransactionContext context) {
        if (body == null) {
            throw new BusinessException("E-JWT-VAL-0001", "요청 본문이 필요합니다.");
        }
        int accessMinutes = intValue(body, "accessTokenValidMinutes", runtimePolicy.getAccessTokenValidMinutes());
        int refreshHours = intValue(body, "refreshTokenValidHours", runtimePolicy.getRefreshTokenValidHours());
        if (accessMinutes < 1 || accessMinutes > 1440) {
            throw new BusinessException("E-JWT-VAL-0002", "Access Token 유효시간은 1~1440분이어야 합니다.");
        }
        if (refreshHours < 1 || refreshHours > 720) {
            throw new BusinessException("E-JWT-VAL-0002", "Refresh Token 유효시간은 1~720시간이어야 합니다.");
        }
        String issuer = stringValue(body, "issuer", runtimePolicy.getIssuer());
        String audience = stringValue(body, "audience", runtimePolicy.getAudience());
        String algorithm = stringValue(body, "algorithm", runtimePolicy.getAlgorithm());
        int clockSkew = intValue(body, "clockSkewSeconds", runtimePolicy.getClockSkewSeconds());
        boolean denylist = ynValue(body, "denylistCheckEnabled", runtimePolicy.isDenylistCheckEnabled());
        boolean rotation = ynValue(body, "refreshTokenRotationEnabled", runtimePolicy.isRefreshTokenRotationEnabled());

        String updatedBy = "SYSTEM";
        if (context != null && context.getHeader() != null
                && StringUtils.hasText(context.getHeader().getUserId())) {
            updatedBy = context.getHeader().getUserId();
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("policyId", POLICY_ID);
        row.put("issuer", issuer);
        row.put("audience", audience);
        row.put("accessTokenValidMinutes", accessMinutes);
        row.put("refreshTokenValidHours", refreshHours);
        row.put("algorithm", algorithm);
        row.put("clockSkewSeconds", clockSkew);
        row.put("denylistCheckEnabled", denylist ? "Y" : "N");
        row.put("refreshTokenRotationEnabled", rotation ? "Y" : "N");
        row.put("updatedAt", Timestamp.from(Instant.now()));
        row.put("updatedBy", updatedBy);
        adminDao.mergeSecurityPolicy(row);

        runtimePolicy.apply(issuer, audience, accessMinutes, refreshHours, algorithm, clockSkew, denylist, rotation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "JWT");
        result.put("screen", "보안정책 관리");
        result.put("updated", true);
        result.put("policy", adminDao.selectSecurityPolicy(POLICY_ID));
        return result;
    }

    public void recordLoginHistory(String userId, String loginResult, String failReason, TransactionContext context) {
        Map<String, Object> row = new HashMap<>();
        row.put("logId", JwtSupport.newId());
        row.put("userId", userId == null ? "UNKNOWN" : userId);
        row.put("loginResult", loginResult);
        row.put("failReason", failReason);
        row.put("channelId", JwtClientContext.channelId(context));
        row.put("clientIp", JwtClientContext.clientIp(context));
        row.put("loginTime", Timestamp.from(Instant.now()));
        adminDao.insertLoginHistory(row);
    }

    private void seedDefaultPolicy(String updatedBy) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("policyId", POLICY_ID);
        row.put("issuer", runtimePolicy.getIssuer());
        row.put("audience", runtimePolicy.getAudience());
        row.put("accessTokenValidMinutes", runtimePolicy.getAccessTokenValidMinutes());
        row.put("refreshTokenValidHours", runtimePolicy.getRefreshTokenValidHours());
        row.put("algorithm", runtimePolicy.getAlgorithm());
        row.put("clockSkewSeconds", runtimePolicy.getClockSkewSeconds());
        row.put("denylistCheckEnabled", runtimePolicy.isDenylistCheckEnabled() ? "Y" : "N");
        row.put("refreshTokenRotationEnabled", runtimePolicy.isRefreshTokenRotationEnabled() ? "Y" : "N");
        row.put("updatedAt", Timestamp.from(Instant.now()));
        row.put("updatedBy", updatedBy);
        adminDao.mergeSecurityPolicy(row);
    }

    private Map<String, Object> pageResult(String screen, Map<String, Object> criteria, int totalCount,
                                           List<Map<String, Object>> rows) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessCode", "JWT");
        result.put("screen", screen);
        result.put("pageNo", criteria.get("pageNo"));
        result.put("pageSize", criteria.get("pageSize"));
        result.put("totalCount", totalCount);
        result.put("rows", rows);
        return result;
    }

    private Map<String, Object> buildCriteria(Map<String, Object> body) {
        Map<String, Object> criteria = new HashMap<>();
        int pageNo = intValue(body, "pageNo", 1);
        int pageSize = intValue(body, "pageSize", 20);
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        criteria.put("pageNo", pageNo);
        criteria.put("pageSize", pageSize);
        criteria.put("offset", (pageNo - 1) * pageSize);
        copyIfPresent(body, criteria, "userId", "jti", "revokedYn", "activeOnly");
        return criteria;
    }

    private void copyIfPresent(Map<String, Object> body, Map<String, Object> criteria, String... keys) {
        if (body == null) {
            return;
        }
        for (String key : keys) {
            String value = JwtSupport.stringValue(body, key);
            if (value != null && !value.isBlank()) {
                criteria.put(key, value);
            }
        }
    }

    private static String stringValue(Map<String, Object> body, String key, String fallback) {
        String value = JwtSupport.stringValue(body, key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int intValue(Map<String, Object> body, String key, int fallback) {
        if (body == null || !body.containsKey(key) || body.get(key) == null) {
            return fallback;
        }
        Object raw = body.get(key);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean ynValue(Map<String, Object> body, String key, boolean fallback) {
        String value = JwtSupport.stringValue(body, key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }
}
