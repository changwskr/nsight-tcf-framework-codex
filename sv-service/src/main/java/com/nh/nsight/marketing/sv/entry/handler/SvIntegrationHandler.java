package com.nh.nsight.marketing.sv.entry.handler;

import com.nh.nsight.marketing.sv.entry.facade.SvIntegrationFacade;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.transaction.TransactionHandler;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * SV 연동 도메인 핸들러. SV.Integration.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 *
 * <p>SV.Integration.icSample — SV 가 IC 샘플을 표준 전문으로 연동 조회하는 데모 거래.
 */
@Component
public class SvIntegrationHandler implements TransactionHandler {

    private static final String IC_SAMPLE = "SV.Integration.icSample";

    private final SvIntegrationFacade facade;

    public SvIntegrationHandler(SvIntegrationFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(IC_SAMPLE);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case IC_SAMPLE -> facade.inquiryIcSample(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "SvIntegrationHandler 미지원 serviceId: " + serviceId);
        };
    }
}
