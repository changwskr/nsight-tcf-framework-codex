package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import com.nh.nsight.gateway.support.RouteResult;
import com.nh.nsight.gateway.support.GatewayProxyTrace;
import com.nh.nsight.gateway.support.ProxyResponseSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public abstract class AbstractBusinessProxyController {
    private final BusinessRouteService routeService;
    private final String businessCode;

    protected AbstractBusinessProxyController(BusinessRouteService routeService, String businessCode) {
        this.routeService = routeService;
        this.businessCode = businessCode;
    }

    protected String businessCode() {
        return businessCode;
    }

    @PostMapping("/online")
    public ResponseEntity<String> proxyOnline(
            @RequestBody String requestBody,
            HttpServletRequest request,
            @RequestParam(value = "deploymentMode", required = false) String deploymentMode,
            @RequestParam(value = "bootrunHost", required = false) String bootrunHost,
            @RequestParam(value = "tomcatGatewayUrl", required = false) String tomcatGatewayUrl) {
        String phase = "Gateway." + businessCode + ".proxyOnline";
        GatewayProxyTrace.start(phase);
        GatewayProxyTrace.log(phase, "deploymentMode=" + deploymentMode
                + " bootrunHost=" + bootrunHost
                + " tomcatGatewayUrl=" + tomcatGatewayUrl);
        GatewayProxyTrace.log(phase, "GRF.forwardOnline");
        RouteResult result = routeService.forwardOnline(
                businessCode,
                requestBody,
                request.getHeader("Cookie"),
                request.getHeader(HttpHeaders.AUTHORIZATION),
                deploymentMode,
                bootrunHost,
                tomcatGatewayUrl
        );
        GatewayProxyTrace.log(phase, "targetUrl=" + result.targetUrl()
                + " httpStatus=" + result.httpStatus()
                + " elapsedMs=" + result.elapsedMs());
        GatewayProxyTrace.log(phase, "ProxyResponseSupport.toResponse");
        ResponseEntity<String> response = ProxyResponseSupport.toResponse(result);
        GatewayProxyTrace.end(phase);
        return response;
    }
}
