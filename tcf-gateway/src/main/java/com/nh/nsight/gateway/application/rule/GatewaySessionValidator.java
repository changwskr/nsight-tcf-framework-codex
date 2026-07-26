package com.nh.nsight.gateway.application.rule;

import com.nh.nsight.gateway.support.GatewaySessionContext;
import com.nh.nsight.gateway.application.service.GatewaySessionValidationService;
import org.springframework.stereotype.Component;

@Component
public class GatewaySessionValidator {
    private final GatewaySessionValidationService validationService;

    public GatewaySessionValidator(GatewaySessionValidationService validationService) {
        this.validationService = validationService;
    }

    public GatewaySessionContext validate(String businessCode, String cookieHeader, String requestBody) {
        return validationService.validate(businessCode, cookieHeader, requestBody);
    }
}
