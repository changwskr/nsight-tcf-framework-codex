package com.nh.nsight.tcf.web.config;

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
@ConditionalOnProperty(prefix = "nsight.tcf.web.jwt", name = "enabled", havingValue = "true")
public class TcfWebJwtConfiguration {
    @Bean
    public JwtDecoder tcfWebJwtDecoder(TcfWebJwtProperties jwtProperties) {
        if (!StringUtils.hasText(jwtProperties.getJwkSetUri())) {
            throw new IllegalStateException(
                    "nsight.tcf.web.jwt.jwk-set-uri is required when JWT filter is enabled");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwtProperties.getJwkSetUri()).build();
        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(jwtProperties.getIssuer());
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>("aud",
                        audience -> audience != null && audience.contains(jwtProperties.getAudience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }
}
