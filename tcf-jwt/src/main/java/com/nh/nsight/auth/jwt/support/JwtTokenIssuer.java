package com.nh.nsight.auth.jwt.support;

import com.nh.nsight.auth.jwt.config.JwtKeyConfiguration;
import com.nh.nsight.auth.jwt.config.JwtRuntimePolicy;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer {
    private final JwtRuntimePolicy runtimePolicy;
    private final RSAPrivateKey privateKey;

    public JwtTokenIssuer(JwtRuntimePolicy runtimePolicy, RSAPrivateKey jwtPrivateKey) {
        this.runtimePolicy = runtimePolicy;
        this.privateKey = jwtPrivateKey;
    }

    public IssuedAccessToken issueAccessToken(Map<String, Object> user, String jti, String channelId,
                                              String clientIp, String userAgent) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(runtimePolicy.getAccessTokenValidMinutes() * 60L);
        String userId = JwtSupport.stringValue(user, "userId");

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(runtimePolicy.getIssuer())
                .subject(userId)
                .audience(runtimePolicy.getAudience())
                .jwtID(jti)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("userId", userId)
                .claim("userName", JwtSupport.stringValue(user, "userName"))
                .claim("branchId", JwtSupport.stringValue(user, "branchId"))
                .claim("authGroupId", JwtSupport.stringValue(user, "authGroupId"))
                .claim("channelId", channelId)
                .build();

        try {
            SignedJWT signedJwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(JwtKeyConfiguration.KEY_ID)
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims);
            signedJwt.sign(new RSASSASigner(privateKey));
            return new IssuedAccessToken(
                    JwtSupport.newId(),
                    jti,
                    signedJwt.serialize(),
                    issuedAt,
                    expiresAt,
                    userId,
                    JwtSupport.stringValue(user, "branchId"),
                    channelId,
                    JwtSupport.stringValue(user, "authGroupId"),
                    clientIp,
                    userAgent);
        } catch (Exception e) {
            throw new IllegalStateException("JWT signing failed", e);
        }
    }

    public record IssuedAccessToken(
            String tokenId,
            String jti,
            String tokenValue,
            Instant issuedAt,
            Instant expiresAt,
            String userId,
            String branchId,
            String channelId,
            String authGroupId,
            String clientIp,
            String userAgent) {
    }
}
