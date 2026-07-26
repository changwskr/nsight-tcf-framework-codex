package com.nh.nsight.auth.jwt.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JwtAdminMapper {
    List<Map<String, Object>> searchJwtTokens(Map<String, Object> criteria);

    int countJwtTokens(Map<String, Object> criteria);

    Map<String, Object> selectJwtTokenByJti(Map<String, Object> params);

    List<Map<String, Object>> searchRefreshTokens(Map<String, Object> criteria);

    int countRefreshTokens(Map<String, Object> criteria);

    List<Map<String, Object>> searchLoginHistories(Map<String, Object> criteria);

    int countLoginHistories(Map<String, Object> criteria);

    int insertLoginHistory(Map<String, Object> params);

    Map<String, Object> selectSecurityPolicy(String policyId);

    int mergeSecurityPolicy(Map<String, Object> params);
}
