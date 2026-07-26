package com.nh.nsight.auth.jwt.persistence.dao;

import com.nh.nsight.auth.jwt.persistence.mapper.JwtTokenMapper;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class JwtTokenDao {
    private final JwtTokenMapper mapper;

    public JwtTokenDao(JwtTokenMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> selectUserForLogin(String userId) {
        return mapper.selectUserForLogin(userId);
    }

    public void updateUserLastLoginTime(Map<String, Object> params) {
        mapper.updateUserLastLoginTime(params);
    }

    public void insertJwtToken(Map<String, Object> params) {
        mapper.insertJwtToken(params);
    }

    public void revokeJwtToken(Map<String, Object> params) {
        mapper.revokeJwtToken(params);
    }

    public void insertRefreshToken(Map<String, Object> params) {
        mapper.insertRefreshToken(params);
    }

    public Map<String, Object> selectRefreshTokenByHash(String tokenHash) {
        return mapper.selectRefreshTokenByHash(tokenHash);
    }

    public void markRefreshTokenRotated(String refreshTokenId) {
        mapper.markRefreshTokenRotated(refreshTokenId);
    }

    public void revokeRefreshToken(Map<String, Object> params) {
        mapper.revokeRefreshToken(params);
    }

    public void insertDenylist(Map<String, Object> params) {
        mapper.insertDenylist(params);
    }

    public boolean isDenied(String issuer, String jti) {
        return mapper.countDenylist(Map.of("issuer", issuer, "jti", jti)) > 0;
    }
}
