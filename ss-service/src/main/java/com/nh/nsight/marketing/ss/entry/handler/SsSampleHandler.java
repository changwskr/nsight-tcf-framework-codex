package com.nh.nsight.marketing.ss.entry.handler;

import com.nh.nsight.marketing.ss.entry.facade.SsSampleFacade;
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
 * SS 샘플 도메인 핸들러. SS.Sample.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class SsSampleHandler implements TransactionHandler {

    private static final String INQUIRY = "SS.Sample.inquiry";

    private final SsSampleFacade facade;

    public SsSampleHandler(SsSampleFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "SsSampleHandler 미지원 serviceId: " + serviceId);
        };
    }
}
