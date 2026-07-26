package com.nh.nsight.marketing.om.entry.handler;

import com.nh.nsight.marketing.om.entry.facade.OmSystemConfigFacade;
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
 * OM 시스템설정 도메인 핸들러. OM.SystemConfig.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class OmSystemConfigHandler implements TransactionHandler {

    private static final String INQUIRY = "OM.SystemConfig.inquiry";

    private final OmSystemConfigFacade facade;

    public OmSystemConfigHandler(OmSystemConfigFacade facade) {
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
                    "OmSystemConfigHandler 미지원 serviceId: " + serviceId);
        };
    }
}
