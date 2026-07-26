package com.nh.nsight.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class GatewaySecurityConfiguration {

    private static final String[] PROXY_PATHS = {
            "/cc/**", "/bc/**",
            "/om/**", "/eb/**", "/ep/**", "/ic/**", "/mg/**",
            "/ms/**", "/pc/**", "/pd/**", "/ss/**", "/sv/**", "/jwt/**"
    };

    /** STATELESS — Gateway HttpSession 미사용, SESSIONDB 쿠키만 GSF에서 검증·전달 */
    @Bean
    public SecurityFilterChain gatewaySecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/admin/**", "/api/admin/**", "/h2-console/**").permitAll()
                        .requestMatchers(PROXY_PATHS).permitAll()
                        .anyRequest().permitAll());
        return http.build();
    }
}
