package nhnis.fw.tcf.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nhnis.fw.tcf.etf.ETF;

/**
 * 인증 실패 응답. 거래에 진입하기 전이라 AOP가 볼 수 없는 지점이므로 여기서 직접 전문을 쓴다.
 *
 * <p>
 * 본문은 ETF가 만든 표준 실패 전문이라 클라이언트는 성공·실패를 같은 방식으로 파싱하면 된다.
 * 다만 HTTP 상태는 200이 아니라 401이다. 거래 결과가 아니라 거래 진입 자체가 거절된 것이고,
 * 게이트웨이 같은 앞단 인프라가 상태 코드로 판단할 수 있어야 하기 때문이다.
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class TcfAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** exceptionCode.yml FW0401 */
    public static final String CODE_UNAUTHENTICATED = "FW0401";

    private final ETF etf;
    private final ObjectMapper objectMapper;

    public TcfAuthenticationEntryPoint(ETF etf, ObjectMapper objectMapper) {
        this.etf = etf;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        System.out.println("=========[TcfAuthenticationEntryPoint][commence][START] "
                + "uri=" + request.getRequestURI()
                + " exception=" + authException.getClass().getSimpleName());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), etf.fail(CODE_UNAUTHENTICATED));
        System.out.println("=========[TcfAuthenticationEntryPoint][commence][END] "
                + "uri=" + request.getRequestURI()
                + " status=" + response.getStatus());
    }
}
