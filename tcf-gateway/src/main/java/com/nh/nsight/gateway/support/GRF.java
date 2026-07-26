package com.nh.nsight.gateway.support;

import com.nh.nsight.gateway.application.rule.GatewayAuthException;
import com.nh.nsight.gateway.application.service.GatewayForwardResponse;
import com.nh.nsight.gateway.application.service.GatewayTransactionLogRecorder;
import com.nh.nsight.gateway.client.GatewayRouteDispatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GRF {
    private static final String PHASE = "GRF.forwardOnline";

    private final GSF gsf;
    private final GatewayRouteDispatcher dispatcher;
    private final GEF gef;
    private final GatewayTransactionLogRecorder transactionLogRecorder;

    public GRF(GSF gsf, GatewayRouteDispatcher dispatcher, GEF gef,
               GatewayTransactionLogRecorder transactionLogRecorder) {
        this.gsf = gsf;
        this.dispatcher = dispatcher;
        this.gef = gef;
        this.transactionLogRecorder = transactionLogRecorder;
    }

    public RouteResult forwardOnline(String businessCode,
                                     String requestBody,
                                     String cookieHeader,
                                     String authorizationHeader,
                                     String deploymentMode,
                                     String bootrunHost,
                                     String tomcatGatewayUrl) {
        GatewayProxyTrace.start(PHASE);
        RouteContext context = null;
        RouteResult result = null;
        String logPhase = null;
        try {
            GatewayProxyTrace.log(PHASE, "GSF.preProcess START");
            context = gsf.preProcess(
                    businessCode, requestBody, cookieHeader, authorizationHeader,
                    deploymentMode, bootrunHost, tomcatGatewayUrl);
            GatewayProxyTrace.log(PHASE, "GSF.preProcess END");

            GatewayProxyTrace.log(PHASE, "GatewayRouteDispatcher.dispatch START");
            GatewayForwardResponse response = dispatcher.dispatch(context, cookieHeader);
            GatewayProxyTrace.log(PHASE, "GatewayRouteDispatcher.dispatch END");

            GatewayProxyTrace.log(PHASE, "GEF.success START");
            result = gef.success(context, response);
            logPhase = "SUCCESS";
            GatewayProxyTrace.log(PHASE, "GEF.success END");
            return result;
        } catch (GatewayRouteNotFoundException e) {
            GatewayProxyTrace.log(PHASE, "GEF.routeNotFound START");
            result = gef.routeNotFound(e);
            logPhase = "ROUTE_NOT_FOUND";
            GatewayProxyTrace.log(PHASE, "GEF.routeNotFound END");
            return result;
        } catch (GatewayAuthException e) {
            GatewayProxyTrace.log(PHASE, "GEF.authFail START");
            result = gef.authFail(businessCode, context, e);
            logPhase = "AUTH_FAIL";
            GatewayProxyTrace.log(PHASE, "GEF.authFail END");
            return result;
        } catch (RestClientResponseException e) {
            if (context == null) {
                throw e;
            }
            GatewayProxyTrace.log(PHASE, "GEF.httpError START");
            result = gef.httpError(context, e);
            logPhase = "HTTP_ERROR";
            GatewayProxyTrace.log(PHASE, "GEF.httpError END");
            return result;
        } catch (Exception e) {
            if (context == null) {
                throw e;
            }
            GatewayProxyTrace.log(PHASE, "GEF.connectionError START");
            result = gef.connectionError(context, e);
            logPhase = "CONNECTION_ERROR";
            GatewayProxyTrace.log(PHASE, "GEF.connectionError END");
            return result;
        } finally {
            transactionLogRecorder.record(businessCode, requestBody, cookieHeader, context, result, logPhase);
            GatewayProxyTrace.end(PHASE);
        }
    }
}
