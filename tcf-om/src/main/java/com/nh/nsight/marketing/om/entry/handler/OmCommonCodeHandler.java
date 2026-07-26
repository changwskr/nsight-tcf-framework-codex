package com.nh.nsight.marketing.om.entry.handler;

import com.nh.nsight.marketing.om.entry.facade.OmCommonCodeFacade;
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
 * OM 공통코드 도메인 핸들러. OM.CommonCode.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class OmCommonCodeHandler implements TransactionHandler {

    private static final String INQUIRY = "OM.CommonCode.inquiry";
    private static final String DETAIL = "OM.CommonCode.detail";
    private static final String SAVE = "OM.CommonCode.save";
    private static final String UPDATE = "OM.CommonCode.update";
    private static final String DELETE = "OM.CommonCode.delete";

    private final OmCommonCodeFacade facade;

    public OmCommonCodeHandler(OmCommonCodeFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY, DETAIL, SAVE, UPDATE, DELETE);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            case DETAIL -> facade.detail(request.getBody(), context);
            case SAVE -> facade.save(request.getBody(), context);
            case UPDATE -> facade.update(request.getBody(), context);
            case DELETE -> facade.delete(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "OmCommonCodeHandler 미지원 serviceId: " + serviceId);
        };
    }
}
