package com.nh.nsight.auth.jwt.persistence.dao;

import com.nh.nsight.auth.jwt.persistence.mapper.JwtAdminMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class JwtAdminDao {
    private final JwtAdminMapper mapper;

    public JwtAdminDao(JwtAdminMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> searchJwtTokens(Map<String, Object> criteria) {
        return mapper.searchJwtTokens(criteria);
    }

    public int countJwtTokens(Map<String, Object> criteria) {
        return mapper.countJwtTokens(criteria);
    }

    public Map<String, Object> selectJwtTokenByJti(String issuer, String jti) {
        return mapper.selectJwtTokenByJti(Map.of("issuer", issuer, "jti", jti));
    }

    public List<Map<String, Object>> searchRefreshTokens(Map<String, Object> criteria) {
        return mapper.searchRefreshTokens(criteria);
    }

    public int countRefreshTokens(Map<String, Object> criteria) {
        return mapper.countRefreshTokens(criteria);
    }

    public List<Map<String, Object>> searchLoginHistories(Map<String, Object> criteria) {
        return mapper.searchLoginHistories(criteria);
    }

    public int countLoginHistories(Map<String, Object> criteria) {
        return mapper.countLoginHistories(criteria);
    }

    public void insertLoginHistory(Map<String, Object> params) {
        mapper.insertLoginHistory(params);
    }

    public Map<String, Object> selectSecurityPolicy(String policyId) {
        return mapper.selectSecurityPolicy(policyId);
    }

    public void mergeSecurityPolicy(Map<String, Object> params) {
        mapper.mergeSecurityPolicy(params);
    }
}
