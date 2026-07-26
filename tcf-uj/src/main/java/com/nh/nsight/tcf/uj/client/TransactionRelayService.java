package com.nh.nsight.tcf.uj.client;

import com.nh.nsight.tcf.uj.support.RelayResult;
import org.springframework.stereotype.Service;

@Service
public class TransactionRelayService {
    private final GatewayRelayService gatewayRelayService;

    public TransactionRelayService(GatewayRelayService gatewayRelayService) {
        this.gatewayRelayService = gatewayRelayService;
    }

    public String resolveTargetUrl(String businessCode, RelayOptions options) {
        return gatewayRelayService.resolveGatewayOnlineUrl(businessCode, options);
    }

    public RelayResult relay(String businessCode, String requestBody, RelayOptions options) {
        return relay(businessCode, requestBody, options, null);
    }

    public RelayResult relay(String businessCode, String requestBody, RelayOptions options, String cookieHeader) {
        return gatewayRelayService.relayOnline(businessCode, requestBody, options, cookieHeader, null);
    }

    public record RelayOptions(String deploymentMode, String bootrunHost, String tomcatGatewayUrl) {
    }
}
