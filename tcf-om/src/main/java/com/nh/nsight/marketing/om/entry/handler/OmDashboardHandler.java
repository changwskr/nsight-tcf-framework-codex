package com.nh.nsight.marketing.om.entry.handler;

import com.nh.nsight.marketing.om.entry.facade.OmDashboardFacade;
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
 * OM 대시보드 도메인 핸들러. OM.Dashboard.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class OmDashboardHandler implements TransactionHandler {

    private static final String INQUIRY = "OM.Dashboard.inquiry";
    private static final String RESET = "OM.Dashboard.reset";

    private final OmDashboardFacade facade;

    public OmDashboardHandler(OmDashboardFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY, RESET);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            case RESET -> facade.reset(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "OmDashboardHandler 미지원 serviceId: " + serviceId);
        };
    }
}
