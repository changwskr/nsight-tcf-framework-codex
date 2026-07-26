package com.nh.nsight.marketing.om.entry.handler;

import com.nh.nsight.marketing.om.entry.facade.OmAuthHistoryFacade;
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
 * OM 인증이력 도메인 핸들러. OM.AuthHistory.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class OmAuthHistoryHandler implements TransactionHandler {

    private static final String INQUIRY = "OM.AuthHistory.inquiry";
    private static final String DELETE_ALL = "OM.AuthHistory.deleteAll";

    private final OmAuthHistoryFacade facade;

    public OmAuthHistoryHandler(OmAuthHistoryFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY, DELETE_ALL);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            case DELETE_ALL -> facade.deleteAll(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "OmAuthHistoryHandler 미지원 serviceId: " + serviceId);
        };
    }
}
