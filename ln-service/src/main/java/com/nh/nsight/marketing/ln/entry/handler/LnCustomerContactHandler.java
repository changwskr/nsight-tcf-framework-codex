package com.nh.nsight.marketing.ln.entry.handler;

import com.nh.nsight.marketing.ln.entry.facade.LnCustomerContactFacade;
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
 * LN 고객연락처 도메인 핸들러. LN.CustomerContact.* 거래를 한 핸들러가 처리한다.
 */
@Component
public class LnCustomerContactHandler implements TransactionHandler {

    private static final String SELECT_LIST = "LN.CustomerContact.selectList";
    private static final String SELECT_DETAIL = "LN.CustomerContact.selectDetail";

    private final LnCustomerContactFacade facade;

    public LnCustomerContactHandler(LnCustomerContactFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(SELECT_LIST, SELECT_DETAIL);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case SELECT_LIST -> facade.selectList(request.getBody(), context);
            case SELECT_DETAIL -> facade.selectDetail(request.getBody(), context);
            default -> throw new BusinessException(
                    ErrorCode.SERVICE_NOT_FOUND,
                    "LnCustomerContactHandler 미지원 serviceId: " + serviceId);
        };
    }
}
