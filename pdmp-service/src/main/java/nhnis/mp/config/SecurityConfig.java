package nhnis.mp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import nhnis.fw.tcf.web.JwtAuthenticationFilter;
import nhnis.fw.tcf.web.JwtProperties;
import nhnis.fw.tcf.web.TcfAuthenticationEntryPoint;

/**
 * 무상태 보안 설정.
 *
 * <p>JWT 필터를 Bean으로 만들지 않고 여기서 직접 생성한다. Spring Boot는 Filter 타입 Bean을
 * 서블릿 컨테이너에도 자동 등록해서 Security 체인과 함께 두 번 실행되기 때문이다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtProperties jwtProperties,
            TcfAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/mp/co/a/8888/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(new JwtAuthenticationFilter(jwtProperties, authenticationEntryPoint),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
