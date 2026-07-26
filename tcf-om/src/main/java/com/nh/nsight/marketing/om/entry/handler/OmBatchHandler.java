package com.nh.nsight.marketing.om.entry.handler;

import com.nh.nsight.marketing.om.entry.facade.OmBatchFacade;
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
 * OM 배치 도메인 핸들러. OM.Batch.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class OmBatchHandler implements TransactionHandler {

    private static final String INQUIRY = "OM.Batch.inquiry";
    private static final String EXECUTE = "OM.Batch.execute";
    private static final String DELETE_ALL = "OM.Batch.deleteAll";

    private final OmBatchFacade facade;

    public OmBatchHandler(OmBatchFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY, EXECUTE, DELETE_ALL);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            case EXECUTE -> facade.execute(request.getBody(), context);
            case DELETE_ALL -> facade.deleteAllHistories(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "OmBatchHandler 미지원 serviceId: " + serviceId);
        };
    }
}
