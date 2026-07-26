package com.nh.nsight.marketing.ep.entry.handler;

import com.nh.nsight.marketing.ep.entry.facade.EpSampleFacade;
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
 * EP 샘플 도메인 핸들러. EP.Sample.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class EpSampleHandler implements TransactionHandler {

    private static final String INQUIRY = "EP.Sample.inquiry";

    private final EpSampleFacade facade;

    public EpSampleHandler(EpSampleFacade facade) {
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
                    "EpSampleHandler 미지원 serviceId: " + serviceId);
        };
    }
}
