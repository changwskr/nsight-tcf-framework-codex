package com.nh.nsight.marketing.eb.entry.handler;

import com.nh.nsight.marketing.eb.entry.facade.EbUserFacade;
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
 * EB 사용자 도메인 핸들러. EB.User.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class EbUserHandler implements TransactionHandler {

    private static final String CREATE = "EB.User.create";
    private static final String INQUIRY = "EB.User.inquiry";

    private final EbUserFacade facade;

    public EbUserHandler(EbUserFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(CREATE, INQUIRY);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case CREATE -> facade.create(request.getBody(), context);
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "EbUserHandler 미지원 serviceId: " + serviceId);
        };
    }
}
