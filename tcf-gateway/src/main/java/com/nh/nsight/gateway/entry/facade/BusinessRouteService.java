package com.nh.nsight.gateway.entry.facade;

import com.nh.nsight.gateway.support.GRF;
import com.nh.nsight.gateway.support.RouteResult;
import org.springframework.stereotype.Service;

@Service
public class BusinessRouteService {
    private final GRF grf;

    public BusinessRouteService(GRF grf) {
        this.grf = grf;
    }

    public RouteResult forwardOnline(String businessCode,
                                     String requestBody,
                                     String cookieHeader,
                                     String authorizationHeader,
                                     String deploymentMode,
                                     String bootrunHost,
                                     String tomcatGatewayUrl) {
        return grf.forwardOnline(
                businessCode,
                requestBody,
                cookieHeader,
                authorizationHeader,
                deploymentMode,
                bootrunHost,
                tomcatGatewayUrl
        );
    }
}
