package com.nh.nsight.marketing.eb.entry.handler;

import com.nh.nsight.marketing.eb.entry.facade.EbSystemTxFacade;
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
 * 시스템 거래 현황(화면 19410) 도메인 핸들러.
 */
@Component
public class EbSystemTxHandler implements TransactionHandler {

    public static final String INQUIRY = "EB.SystemTx.inquiry";

    private final EbSystemTxFacade facade;

    public EbSystemTxHandler(EbSystemTxFacade facade) {
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
                    "EbSystemTxHandler 미지원 serviceId: " + serviceId);
        };
    }
}
