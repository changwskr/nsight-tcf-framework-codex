package com.nh.nsight.tcf.core.support.processor;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.idempotency.IdempotencyChecker;
import com.nh.nsight.tcf.core.support.logging.AuditLogService;
import com.nh.nsight.tcf.core.support.logging.TransactionLogService;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.message.StandardResponse;
import com.nh.nsight.tcf.core.support.metrics.TransactionMetricService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.nh.nsight.tcf.core.support.TcfConsoleLog;

@Component
public class ETF {
    private static final Logger log = LoggerFactory.getLogger(ETF.class);
    private final TransactionLogService transactionLogService;
    private final AuditLogService auditLogService;
    private final TransactionMetricService metricService;
    private final IdempotencyChecker idempotencyChecker;

    public ETF(TransactionLogService transactionLogService,
            AuditLogService auditLogService,
            TransactionMetricService metricService,
            IdempotencyChecker idempotencyChecker) {
        this.transactionLogService = transactionLogService;
        this.auditLogService = auditLogService;
        this.metricService = metricService;
        this.idempotencyChecker = idempotencyChecker;
    }

    public StandardResponse<Object> success(StandardRequest<Map<String, Object>> request, Object body,
            TransactionContext context, StandardHeader clientHeader) {
        System.out.println("==============================[ETF.success] start");
        StandardHeader header = responseHeaderOf(request, context, clientHeader);
        if (context != null) {
            StandardHeader processingHeader = context.getHeader();
            System.out.println(" ==============================[ETF.success] idempotencyChecker.markSuccess");
            idempotencyChecker.markSuccess(processingHeader);
            System.out.println(" ==============================[ETF.success] transactionLogService.end");
            transactionLogService.end(context, "S0000", null);
            System.out.println(" ==============================[ETF.success] auditLogService.audit");
            auditLogService.audit(context, "S0000");
            System.out.println(" ==============================[ETF.success] metricService.record");
            metricService.record(context, "S0000");
        }
        System.out.println(" ==============================[ETF.success] end");
        return StandardResponse.success(header, body);
    }

    public StandardResponse<Object> businessFail(StandardRequest<Map<String, Object>> request, BusinessException e,
            TransactionContext context, StandardHeader clientHeader) {
        System.out.println("\n ==============================[ETF.businessFail] start");
        StandardHeader header = responseHeaderOf(request, context, clientHeader);
        if (context != null) {
            StandardHeader processingHeader = context.getHeader();
            System.out.println(" ==============================[ETF.businessFail] idempotencyChecker.markFail");
            idempotencyChecker.markFail(processingHeader);
            System.out.println(" ==============================[ETF.businessFail] transactionLogService.end");
            transactionLogService.end(context, "E0001", e.getErrorCode());
            System.out.println(" ==============================[ETF.businessFail] auditLogService.audit");
            auditLogService.audit(context, "E0001");
            System.out.println(" ==============================[ETF.businessFail] metricService.record");
            metricService.record(context, "E0001");
        }
        System.out.println(" ==============================[ETF.businessFail] end");
        return StandardResponse.fail(header, e.getErrorCode(), e.getMessage(), null);
    }

    public StandardResponse<Object> systemError(StandardRequest<Map<String, Object>> request, Exception e,
            TransactionContext context, StandardHeader clientHeader) {
        System.out.println("\n ==============================[ETF.systemError] start");
        StandardHeader header = responseHeaderOf(request, context, clientHeader);
        log.error("TCF system error. serviceId={}", header == null ? null : header.getServiceId(), e);
        if (context != null) {
            StandardHeader processingHeader = context.getHeader();
            System.out.println(" ==============================[ETF.systemError] idempotencyChecker.markFail");
            idempotencyChecker.markFail(processingHeader);
            System.out.println(" ==============================[ETF.systemError] transactionLogService.end");
            transactionLogService.end(context, "E0001", ErrorCode.SYSTEM_ERROR);
            System.out.println(" ==============================[ETF.systemError] auditLogService.audit");
            auditLogService.audit(context, "E0001");
            System.out.println(" ==============================[ETF.systemError] metricService.record");
            metricService.record(context, "E0001");
        }
        System.out.println(" ==============================[ETF.systemError] end");
        return StandardResponse.fail(header, ErrorCode.SYSTEM_ERROR, "시스템 오류가 발생했습니다.", e.getClass().getSimpleName());
    }

    private StandardHeader responseHeaderOf(StandardRequest<Map<String, Object>> request,
            TransactionContext context,
            StandardHeader clientHeader) {
        if (context != null) {
            return context.getClientHeader();
        }
        if (clientHeader != null) {
            return clientHeader;
        }
        return request == null ? null : StandardHeader.copyOf(request.getHeader());
    }
}
