package com.nh.nsight.gateway.support;

import com.nh.nsight.gateway.application.rule.GatewayAuthException;
import com.nh.nsight.gateway.application.service.GatewayForwardResponse;
import com.nh.nsight.gateway.application.service.GatewaySessionRegistry;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GEF {
    private static final String PHASE = "GEF";

    private final GatewaySessionRegistry sessionRegistry;

    public GEF(GatewaySessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public RouteResult success(RouteContext context, GatewayForwardResponse response) {
        String phase = PHASE + ".success";
        GatewayProxyTrace.start(phase);
        GatewayProxyTrace.log(phase, "targetUrl=" + context.targetUrl());
        GatewayProxyTrace.log(phase, "httpStatus=" + response.httpStatus() + " elapsedMs=" + response.elapsedMs());

        if (response.httpStatus() >= 200 && response.httpStatus() < 300) {
            GatewayProxyTrace.log(phase, ">>>>>>>> registerSessionDb on login >>>>>>>>>>>> ");
            sessionRegistry.tryRegisterOmLogin(context.enrichedBody(), response.responseBody(), response.setCookies());
        }
        GatewayProxyTrace.log(phase, "RouteResult");
        RouteResult result = new RouteResult(
                context.targetUrl(),
                response.httpStatus(),
                response.elapsedMs(),
                response.responseBody(),
                response.setCookies());
        GatewayProxyTrace.end(phase);
        return result;
    }

    public RouteResult routeNotFound(GatewayRouteNotFoundException error) {
        String phase = PHASE + ".routeNotFound";
        GatewayProxyTrace.start(phase);
        GatewayProxyTrace.log(phase, "envCode=" + error.envCode() + " businessCode=" + error.businessCode());
        GatewayProxyTrace.log(phase, "RouteResult");
        RouteResult result = new RouteResult(
                "",
                404,
                0L,
                routeNotFoundJson(error),
                List.of());
        GatewayProxyTrace.end(phase);
        return result;
    }

    public RouteResult authFail(String businessCode, RouteContext context, GatewayAuthException error) {
        String phase = PHASE + ".authFail";
        GatewayProxyTrace.start(phase);
        String targetUrl = context == null ? "" : context.targetUrl();
        long elapsedMs = context == null ? 0L : System.currentTimeMillis() - context.startedAtMillis();
        GatewayProxyTrace.log(phase, "businessCode=" + businessCode + " httpStatus=" + error.httpStatus());
        GatewayProxyTrace.log(phase, "message=" + error.getMessage());
        GatewayProxyTrace.log(phase, "RouteResult");
        RouteResult result = new RouteResult(
                targetUrl,
                error.httpStatus(),
                elapsedMs,
                authErrorJson(businessCode, error),
                List.of());
        GatewayProxyTrace.end(phase);
        return result;
    }

    public RouteResult httpError(RouteContext context, RestClientResponseException error) {
        String phase = PHASE + ".httpError";
        GatewayProxyTrace.start(phase);
        GatewayProxyTrace.log(phase, "targetUrl=" + context.targetUrl());
        GatewayProxyTrace.log(phase, "httpStatus=" + error.getStatusCode().value());
        GatewayProxyTrace.log(phase, "RouteResult");
        List<String> setCookies = error.getResponseHeaders() == null
                ? List.of()
                : error.getResponseHeaders().getOrEmpty(org.springframework.http.HttpHeaders.SET_COOKIE);
        RouteResult result = new RouteResult(
                context.targetUrl(),
                error.getStatusCode().value(),
                System.currentTimeMillis() - context.startedAtMillis(),
                error.getResponseBodyAsString(),
                setCookies);
        GatewayProxyTrace.end(phase);
        return result;
    }

    public RouteResult connectionError(RouteContext context, Exception error) {
        String phase = PHASE + ".connectionError";
        GatewayProxyTrace.start(phase);
        GatewayProxyTrace.log(phase, "targetUrl=" + context.targetUrl());
        GatewayProxyTrace.log(phase, "error=" + error.getClass().getSimpleName() + " message=" + error.getMessage());
        GatewayProxyTrace.log(phase, "connectionErrorJson");
        RouteResult result = new RouteResult(
                context.targetUrl(),
                502,
                System.currentTimeMillis() - context.startedAtMillis(),
                connectionErrorJson(context, error.getMessage()),
                List.of());
        GatewayProxyTrace.end(phase);
        return result;
    }

    private String routeNotFoundJson(GatewayRouteNotFoundException error) {
        return """
                {"error":"%s","envCode":"%s","businessCode":"%s","hint":"TCF_GATEWAY_ROUTE에 ENV_CODE=%s, BUSINESS_CODE=%s 라우팅을 등록하세요."}
                """.formatted(
                error.getMessage().replace("\\", "\\\\").replace("\"", "\\\""),
                error.envCode(),
                error.businessCode(),
                error.envCode(),
                error.businessCode());
    }

    private String authErrorJson(String businessCode, GatewayAuthException error) {
        String safeMessage = error.getMessage() == null ? ""
                : error.getMessage().replace("\\", "\\\\").replace("\"", "\\\"");
        return """
                {"error":"%s","businessCode":"%s","hint":"gateway 인증에 실패했습니다."}
                """.formatted(safeMessage, businessCode.toUpperCase(Locale.ROOT));
    }

    private String connectionErrorJson(RouteContext context, String message) {
        String safeMessage = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        String safeUrl = context.targetUrl() == null ? ""
                : context.targetUrl().replace("\\", "\\\\").replace("\"", "\\\"");
        String hint = context.module().bootrunPort() > 0
                ? "%s(포트 %d) 기동 상태를 확인하세요.".formatted(
                context.module().serviceHint(), context.module().bootrunPort())
                : "downstream Target(%s) 기동 상태를 확인하세요.".formatted(safeUrl);
        return """
                {"error":"%s","targetUrl":"%s","hint":"%s"}
                """.formatted(safeMessage, safeUrl, hint.replace("\\", "\\\\").replace("\"", "\\\""));
    }
}
