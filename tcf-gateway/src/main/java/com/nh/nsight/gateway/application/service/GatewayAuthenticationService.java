package com.nh.nsight.gateway.application.service;

import com.nh.nsight.gateway.application.rule.GatewayAuthException;
import com.nh.nsight.gateway.application.rule.GatewayAuthExemptions;
import com.nh.nsight.gateway.application.rule.GatewaySessionValidator;
import com.nh.nsight.gateway.config.GatewayProperties;
import com.nh.nsight.gateway.support.GatewayProxyTrace;
import com.nh.nsight.gateway.support.GatewayRequestUserReader;
import com.nh.nsight.gateway.support.GatewaySessionContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class GatewayAuthenticationService {
    private static final String PHASE = "GatewayAuthenticationService.authenticate";
    private static final String JWT_LOG = "******* [GW-AUTH-JWT] ";

    private final GatewayProperties properties;
    private final GatewaySessionValidator sessionValidator;
    private final ObjectProvider<GatewayJwtValidator> jwtValidatorProvider;
    private final GatewayRequestUserReader requestUserReader;

    public GatewayAuthenticationService(GatewayProperties properties,
            GatewaySessionValidator sessionValidator,
            ObjectProvider<GatewayJwtValidator> jwtValidatorProvider,
            GatewayRequestUserReader requestUserReader) {
        this.properties = properties;
        this.sessionValidator = sessionValidator;
        this.jwtValidatorProvider = jwtValidatorProvider;
        this.requestUserReader = requestUserReader;
    }

    public GatewaySessionContext authenticate(String businessCode,
            String cookieHeader,
            String authorizationHeader,
            String requestBody) {
        GatewayProxyTrace.start(PHASE);
        try {
            if (!properties.getAuth().isLoginRequired()) {
                GatewayProxyTrace.log(PHASE, "login check skipped");
                return null;
            }
            String serviceId = requestUserReader.serviceId(requestBody).orElse(null);
            System.out.println("[GW-AUTH] businessCode=" + businessCode
                    + " serviceId=" + serviceId
                    + " bearerPresent=" + (authorizationHeader != null && authorizationHeader.regionMatches(true, 0, "Bearer", 0, 6)));
            if (GatewayAuthExemptions.isLoginExempt(serviceId)) {
                GatewayProxyTrace.log(PHASE, "login exempt serviceId=" + serviceId);
                System.out.println("[GW-AUTH] login exempt, skip auth serviceId=" + serviceId);
                return null;
            }
            GatewayJwtValidator jwtValidator = jwtValidatorProvider.getIfAvailable();
            boolean jwtEnabled = properties.getAuth().getJwt().isEnabled() && jwtValidator != null;
            if (jwtEnabled && "OM".equalsIgnoreCase(businessCode)) {
                GatewayProxyTrace.log(PHASE, "om jwt validation (required)");
                System.out.println(JWT_LOG + "OM jwt required serviceId=" + serviceId
                        + " bearerPresent=" + jwtValidator.hasBearerToken(authorizationHeader));
                if (!jwtValidator.hasBearerToken(authorizationHeader)) {
                    System.out.println(JWT_LOG + "OM jwt missing Bearer → 401");
                    throw new GatewayAuthException(401, "OM 거래는 Authorization Bearer JWT가 필요합니다.");
                }
                return jwtValidator.validate(authorizationHeader, requestBody);
            }
            if (jwtEnabled && jwtValidator.hasBearerToken(authorizationHeader)) {
                GatewayProxyTrace.log(PHASE, "jwt validation");
                System.out.println(JWT_LOG + "jwt validation serviceId=" + serviceId);
                return jwtValidator.validate(authorizationHeader, requestBody);
            }
            GatewayProxyTrace.log(PHASE, "session validation");
            System.out.println("[GW-AUTH] session validation businessCode=" + businessCode + " serviceId=" + serviceId);
            return sessionValidator.validate(businessCode, cookieHeader, requestBody);
        } finally {
            GatewayProxyTrace.end(PHASE);
        }
    }
}
