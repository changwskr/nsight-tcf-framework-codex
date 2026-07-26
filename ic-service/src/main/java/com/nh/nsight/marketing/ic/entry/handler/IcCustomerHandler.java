package com.nh.nsight.marketing.ic.entry.handler;

import com.nh.nsight.marketing.ic.entry.facade.IcCustomerFacade;
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
 * IC 고객 도메인 핸들러. IC.Customer.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 *
 * <p>새 고객 거래를 추가할 때 {@link #serviceIds()} 와 {@link #doHandle} 의 분기만 확장한다.
 */
@Component
public class IcCustomerHandler implements TransactionHandler {

    private static final String INQUIRY = "IC.Customer.inquiry";

    private final IcCustomerFacade facade;

    public IcCustomerHandler(IcCustomerFacade facade) {
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
            case INQUIRY -> facade.inquiryCustomerDetail(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "IcCustomerHandler 미지원 serviceId: " + serviceId);
        };
    }
}
