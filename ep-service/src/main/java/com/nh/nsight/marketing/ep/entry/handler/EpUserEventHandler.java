package com.nh.nsight.marketing.ep.entry.handler;

import com.nh.nsight.marketing.ep.entry.facade.EpUserEventFacade;
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
 * EP 사용자이벤트 도메인 핸들러. EP.UserEvent.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class EpUserEventHandler implements TransactionHandler {

    private static final String INQUIRY = "EP.UserEvent.inquiry";
    private static final String RECEIVE = "EP.UserEvent.receive";

    private final EpUserEventFacade facade;

    public EpUserEventHandler(EpUserEventFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY, RECEIVE);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            case RECEIVE -> facade.receive(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "EpUserEventHandler 미지원 serviceId: " + serviceId);
        };
    }
}
