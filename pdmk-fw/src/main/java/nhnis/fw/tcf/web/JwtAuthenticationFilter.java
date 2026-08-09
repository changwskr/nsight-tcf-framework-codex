package nhnis.fw.tcf.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT 검증 필터. Spring Security 체인 안에서 동작한다.
 *
 * <p>
 * 전문 선처리(STF)에 인증을 넣지 않은 이유는 SecurityContext 이중 관리를 피하기 위해서다.
 * 여기서 SecurityContext만 채워 두면 {@code authorizeHttpRequests}나
 * {@code @PreAuthorize} 같은
 * 표준 권한 기능을 그대로 쓸 수 있고, STF는 그 결과를 전문 Header의 userId로 옮기기만 하면 된다.
 *
 * <p>
 * Bean으로 등록하지 않는다. Spring Boot는 Filter 타입 Bean을 서블릿 컨테이너에도 자동 등록하므로
 * Security 체인과 컨테이너에 이중 등록되어 두 번 실행된다. SecurityConfig에서 직접 생성해 넣는다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties properties;
    private final TcfAuthenticationEntryPoint entryPoint;
    private final SecretKey signingKey;

    public JwtAuthenticationFilter(JwtProperties properties, TcfAuthenticationEntryPoint entryPoint) {
        this.properties = properties;
        this.entryPoint = entryPoint;
        this.signingKey = buildSigningKey(properties);
    }

    private static SecretKey buildSigningKey(JwtProperties properties) {
        if (!properties.isEnabled()) {
            return null;
        }
        String secret = properties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("jwt.enabled=true 이면 jwt.secret이 있어야 한다.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        System.out.println("=========[JwtAuthenticationFilter][doFilterInternal][START] "
                + "method=" + request.getMethod()
                + " uri=" + request.getRequestURI()
                + " authHeaderPresent=" + StringUtils.hasText(request.getHeader(HEADER_AUTHORIZATION)));
        try {
            if (!properties.isEnabled()) {
                chain.doFilter(request, response);
                return;
            }

            String token = bearerToken(request);
            if (token == null) {
                // 토큰이 없는 요청은 익명으로 흘려보내고, 접근 여부는 인가 규칙이 판단한다.
                chain.doFilter(request, response);
                return;
            }

            try {
                authenticate(request, token);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                log.warn("[JWT] 토큰 검증 실패 uri={} reason={}", request.getRequestURI(), e.getMessage());
                entryPoint.commence(request, response, new BadCredentialsException("invalid token", e));
                return;
            }
            chain.doFilter(request, response);
        } finally {
            System.out.println("=========[JwtAuthenticationFilter][doFilterInternal][END] "
                    + "method=" + request.getMethod()
                    + " uri=" + request.getRequestURI()
                    + " status=" + response.getStatus());
        }
    }

    private void authenticate(HttpServletRequest request, String token) {
        System.out.println("=========[JwtAuthenticationFilter][authenticate][START] "
                + "uri=" + request.getRequestURI()
                + " tokenPresent=" + (token != null));
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                claims.getSubject(), null, authoritiesOf(claims));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("=========[JwtAuthenticationFilter][authenticate][END] "
                + "uri=" + request.getRequestURI()
                + " subject=" + claims.getSubject());
    }

    private List<GrantedAuthority> authoritiesOf(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        if (roles == null) {
            return List.of();
        }
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast).toList();
        }
        return List.of(new SimpleGrantedAuthority(String.valueOf(roles)));
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
