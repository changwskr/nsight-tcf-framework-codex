package nhnis.fw.tcf.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nhnis.fw.tcf.TcfMdcKeys;
import nhnis.fw.tcf.TcfProperties;

/**
 * 거래 추적 필터. 어떤 거래인지 몰라도 해야 하는 일만 담당한다.
 *
 * <p>
 * guid/traceId를 채번해 MDC에 심고 총 응답시간을 잰다. Security 체인보다 먼저 돌아야
 * 인증 실패 로그에도 guid가 남으므로 최우선 순위로 등록한다.
 *
 * <p>
 * MDC는 심은 곳에서 지운다. 톰캣이 워커 스레드를 재사용하므로 정리를 다른 필터에 맡기면
 * 예외 경로에서 이전 거래의 guid가 다음 요청 로그에 새어 나간다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TcfTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TcfTraceFilter.class);

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    private static final String UNKNOWN = "-";

    private final TcfProperties properties;

    public TcfTraceFilter(TcfProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String guid = orElse(request.getHeader(HEADER_REQUEST_ID), newId());
        String traceId = orElse(request.getHeader(HEADER_TRACE_ID), newId());

        MDC.put(TcfMdcKeys.GUID, guid);
        MDC.put(TcfMdcKeys.TRACE_ID, traceId);
        MDC.put(TcfMdcKeys.USER_ID, orElse(request.getHeader(HEADER_USER_ID), UNKNOWN));
        MDC.put(TcfMdcKeys.SERVICE_ID, request.getRequestURI());
        MDC.put(TcfMdcKeys.IP, clientIp(request));

        // 커밋 전에 실어야 클라이언트가 응답만 보고도 로그를 찾을 수 있다.
        response.setHeader(HEADER_REQUEST_ID, guid);
        response.setHeader(HEADER_TRACE_ID, traceId);

        long startedAt = System.nanoTime();
        System.out.println("=========[TcfTraceFilter][doFilterInternal][START] "
                + "method=" + request.getMethod()
                + " uri=" + request.getRequestURI()
                + " guid=" + guid
                + " traceId=" + traceId
                + " userId=" + orElse(request.getHeader(HEADER_USER_ID), UNKNOWN)
                + " clientIp=" + clientIp(request));
        try {
            chain.doFilter(request, response);
        } finally {
            recordTotalElapsed(request, response, startedAt);
            MDC.clear();
        }
    }

    /**
     * 직렬화·필터 체인까지 포함한 총 응답시간. AOP가 재는 업무 처리시간과의 차이가 크면
     * 병목이 업무 밖에 있다는 뜻이다.
     */
    private void recordTotalElapsed(HttpServletRequest request, HttpServletResponse response, long startedAt) {
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        long threshold = properties.getSlowTransactionMs();
        if (threshold > 0 && elapsedMs >= threshold) {
            log.warn("[TRACE] 지연 응답 {} {} status={} total={}ms threshold={}ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs, threshold);
            System.out.println("=========[TcfTraceFilter][recordTotalElapsed][END] "
                    + "method=" + request.getMethod()
                    + " uri=" + request.getRequestURI()
                    + " status=" + response.getStatus()
                    + " total=" + elapsedMs + "ms"
                    + " guid=" + MDC.get(TcfMdcKeys.GUID)
                    + " traceId=" + MDC.get(TcfMdcKeys.TRACE_ID));
            return;
        }
        log.info("[TRACE] {} {} status={} total={}ms",
                request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
        System.out.println("=========[TcfTraceFilter][recordTotalElapsed][END] "
                + "method=" + request.getMethod()
                + " uri=" + request.getRequestURI()
                + " status=" + response.getStatus()
                + " total=" + elapsedMs + "ms"
                + " guid=" + MDC.get(TcfMdcKeys.GUID)
                + " traceId=" + MDC.get(TcfMdcKeys.TRACE_ID));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_FORWARDED_FOR);
        if (forwarded == null || forwarded.isBlank()) {
            return orElse(request.getRemoteAddr(), UNKNOWN);
        }
        return forwarded.split(",")[0].trim();
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String orElse(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
