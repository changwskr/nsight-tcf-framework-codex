package com.nh.nsight.gateway.client;

import com.nh.nsight.gateway.application.service.GatewayForwardResponse;
import com.nh.nsight.gateway.support.RouteContext;
import com.nh.nsight.gateway.support.GatewayProxyTrace;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class GatewayRouteDispatcher {
    private static final String PHASE = "GatewayRouteDispatcher.dispatch";
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 5000;

    public GatewayForwardResponse dispatch(RouteContext context, String cookieHeader) {
        GatewayProxyTrace.start(PHASE);
        try {
            int connectTimeoutMs = effectiveTimeout(context.connectTimeoutMs(), DEFAULT_CONNECT_TIMEOUT_MS);
            int readTimeoutMs = effectiveTimeout(context.readTimeoutMs(), DEFAULT_READ_TIMEOUT_MS);
            GatewayProxyTrace.log(PHASE, "targetUrl=" + context.targetUrl()
                    + " connectTimeoutMs=" + connectTimeoutMs
                    + " readTimeoutMs=" + readTimeoutMs);
            GatewayProxyTrace.log(PHASE, "restClient.post");
            RestClient restClient = restClient(connectTimeoutMs, readTimeoutMs);
            return restClient.post()
                    .uri(URI.create(context.targetUrl()))
                    .headers(headers -> {
                        if (StringUtils.hasText(cookieHeader)) {
                            headers.set(HttpHeaders.COOKIE, cookieHeader);
                        }
                        if (StringUtils.hasText(context.authorizationHeader())) {
                            headers.set(HttpHeaders.AUTHORIZATION, context.authorizationHeader());
                        }
                    })
                    .body(context.enrichedBody())
                    .exchange((request, response) -> {
                        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        List<String> setCookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
                        return new GatewayForwardResponse(
                                response.getStatusCode().value(),
                                System.currentTimeMillis() - context.startedAtMillis(),
                                responseBody == null ? "" : responseBody,
                                setCookies
                        );
                    });
        } catch (RuntimeException e) {
            GatewayProxyTrace.log(PHASE, "error=" + e.getClass().getSimpleName() + " message=" + e.getMessage());
            throw e;
        } finally {
            GatewayProxyTrace.end(PHASE);
        }
    }

    private RestClient restClient(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                .build();
    }

    private int effectiveTimeout(int value, int defaultMs) {
        return value > 0 ? value : defaultMs;
    }
}
