package com.nh.nsight.marketing.mg.entry.handler;

import com.nh.nsight.marketing.mg.entry.facade.MgSampleFacade;
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
 * MG 샘플 도메인 핸들러. MG.Sample.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class MgSampleHandler implements TransactionHandler {

    private static final String INQUIRY = "MG.Sample.inquiry";

    private final MgSampleFacade facade;

    public MgSampleHandler(MgSampleFacade facade) {
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
                    "MgSampleHandler 미지원 serviceId: " + serviceId);
        };
    }
}
