package com.nh.nsight.auth.jwt.persistence.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JwtTokenMapper {
    Map<String, Object> selectUserForLogin(String userId);

    int updateUserLastLoginTime(Map<String, Object> params);

    int updateUserPasswordHash(Map<String, Object> params);

    List<Map<String, Object>> selectUsersWithoutPasswordHash();

    int insertJwtToken(Map<String, Object> params);

    int revokeJwtToken(Map<String, Object> params);

    int insertRefreshToken(Map<String, Object> params);

    Map<String, Object> selectRefreshTokenByHash(String tokenHash);

    int markRefreshTokenRotated(String refreshTokenId);

    int revokeRefreshToken(Map<String, Object> params);

    int insertDenylist(Map<String, Object> params);

    int countDenylist(Map<String, Object> params);
}
