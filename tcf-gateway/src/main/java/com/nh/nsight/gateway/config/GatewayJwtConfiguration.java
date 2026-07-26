package com.nh.nsight.gateway.config;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(prefix = "nsight.gateway.auth.jwt", name = "enabled", havingValue = "true")
public class GatewayJwtConfiguration {

    @Bean
    public JwtDecoder gatewayJwtDecoder(GatewayProperties properties) {
        GatewayProperties.Jwt jwt = properties.getAuth().getJwt();
        if (!StringUtils.hasText(jwt.getJwkSetUri())) {
            throw new IllegalStateException(
                    "nsight.gateway.auth.jwt.jwk-set-uri is required when JWT auth is enabled");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwt.getJwkSetUri()).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(jwt.getIssuer());
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud",
                audience -> audience != null && audience.contains(jwt.getAudience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }
}
