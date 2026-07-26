package com.nh.nsight.auth.jwt.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtKeyConfiguration {
    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfiguration.class);
    public static final String KEY_ID = "nsight-jwt-rs256";

    @Bean
    public RSAKey jwtSigningKey() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048)
                .keyID(KEY_ID)
                .generate();
        log.info("JWT RS256 signing key generated. kid={}", KEY_ID);
        return key;
    }

    @Bean
    public RSAPrivateKey jwtPrivateKey(RSAKey jwtSigningKey) throws JOSEException {
        return jwtSigningKey.toRSAPrivateKey();
    }

    @Bean
    public RSAPublicKey jwtPublicKey(RSAKey jwtSigningKey) throws JOSEException {
        return jwtSigningKey.toRSAPublicKey();
    }

    @Bean
    public JWKSet jwtJwkSet(RSAKey jwtSigningKey) {
        return new JWKSet(jwtSigningKey.toPublicJWK());
    }
}
