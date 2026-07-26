package com.nh.nsight.tcf.core.support.processor;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.context.TransactionContextHolder;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.message.StandardResponse;
import com.nh.nsight.tcf.core.support.dispatch.TransactionDispatcher;
import com.nh.nsight.tcf.core.support.runtime.TcfRuntimeTransactionHook;
import com.nh.nsight.tcf.core.support.security.AuthenticationContextHolder;
import com.nh.nsight.tcf.core.support.timeout.OnlineTransactionTimeoutExecutor;
import com.nh.nsight.tcf.core.support.timeout.TimeoutContextHolder;
import com.nh.nsight.tcf.core.support.timeout.TimeoutExceptionResolver;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class TCF {
    private final STF stf;
    private final TransactionDispatcher dispatcher;
    private final ETF etf;
    private final OnlineTransactionTimeoutExecutor onlineTransactionTimeoutExecutor;
    private final TcfRuntimeTransactionHook runtimeTransactionHook;

    public TCF(STF stf,
            TransactionDispatcher dispatcher,
            ETF etf,
            OnlineTransactionTimeoutExecutor onlineTransactionTimeoutExecutor,
            TcfRuntimeTransactionHook runtimeTransactionHook) {
        this.stf = stf;
        this.dispatcher = dispatcher;
        this.etf = etf;
        this.onlineTransactionTimeoutExecutor = onlineTransactionTimeoutExecutor;
        this.runtimeTransactionHook = runtimeTransactionHook;
    }

    public StandardResponse<Object> process(StandardRequest<Map<String, Object>> request) {
        TransactionContext context = null;
        StandardHeader clientHeader = StandardHeader.copyOf(request == null ? null : request.getHeader());
        System.out.println("======================================================[TCF.process] start");
        try {
            logClientRequest(request);

            System.out.println(" ============================[TCF.process] STF START");
            context = stf.preProcess(request, clientHeader);
            runtimeTransactionHook.onTransactionStart(context);
            System.out.println(" ============================[TCF.process] STF END");

            System.out.println(" ============================[TCF.process] DISPATCHER  START");
            TransactionContext dispatchContext = context;
            Object body = onlineTransactionTimeoutExecutor.execute(
                    () -> dispatcher.dispatch(request, dispatchContext));
            System.out.println(" ============================[TCF.process] DISPATCHER END");

            System.out.println(" ============================[TCF.process] ETF START");
            StandardResponse<Object> response = etf.success(request, body, context, clientHeader);
            System.out.println(" ============================[TCF.process] ETF END");

            logClientResponse(response);
            System.out.println(" ========================================================[TCF.process] end (success)");
            return response;
        } catch (BusinessException e) {
            System.out.println(" ============================[TCF.process] ETF.businessFail START");
            StandardResponse<Object> response = etf.businessFail(request, e, context, clientHeader);
            logClientResponse(response);
            System.out.println(" ============================[TCF.process] end (businessFail)");
            return response;
        } catch (Exception e) {
            var timeoutError = TimeoutExceptionResolver.toBusinessException(e);
            if (timeoutError.isPresent()) {
                System.out.println(" ========================[TCF.process] ETF.businessFail (timeout)");
                StandardResponse<Object> response = etf.businessFail(request, timeoutError.get(), context,
                        clientHeader);
                logClientResponse(response);
                System.out.println(" =======================[TCF.process] end (timeoutFail)");
                return response;
            }
            System.out.println(" ===========================[TCF.process] etf.systemError");
            StandardResponse<Object> response = etf.systemError(request, e, context, clientHeader);
            logClientResponse(response);
            System.out.println(" ==========================================[TCF.process] end (systemError)");
            return response;
        } finally {
            System.out.println(" =========================================[TCF.process] cleanup");
            runtimeTransactionHook.onTransactionEnd(context);
            TransactionContextHolder.clear();
            AuthenticationContextHolder.clear();
            TimeoutContextHolder.clear();
            MDC.clear();
        }
    }

    private void logClientRequest(StandardRequest<Map<String, Object>> request) {
        System.out.println(" ======================================[TCF.logClientRequest] client request (incoming)");
        if (request == null) {
            System.out.println("  request=null");
            return;
        }
        StandardHeader header = request.getHeader();
        if (header == null) {
            System.out.println("  [header] null");
        } else {
            System.out.println("  [header]");
            printHeaderFields(header);
        }
        System.out.println("  [body]");
        System.out.println(formatPayload(request.getBody()));
    }

    private void logClientResponse(StandardResponse<Object> response) {
        System.out.println(" ======================================[TCF.logClientResponse] client response (outgoing)");
        if (response == null) {
            System.out.println("  response=null");
            return;
        }
        StandardHeader header = response.getHeader();
        if (header == null) {
            System.out.println("  [header] null");
        } else {
            System.out.println("  [header] (client echo)");
            printHeaderFields(header);
        }
        if (response.getResult() != null) {
            System.out.println("  [result]");
            System.out.println("resultCode=" + response.getResult().getResultCode());
            System.out.println("resultMessage=" + response.getResult().getResultMessage());
            System.out.println("errorCode=" + response.getResult().getErrorCode());
            System.out.println("errorMessage=" + response.getResult().getErrorMessage());
        }
        System.out.println("  [body]");
        System.out.println(formatPayload(response.getBody()));
    }

    private void printHeaderFields(StandardHeader header) {
        System.out.println("systemId=" + header.getSystemId());
        System.out.println("businessCode=" + header.getBusinessCode());
        System.out.println("serviceId=" + header.getServiceId());
        System.out.println("serviceName=" + header.getServiceName());
        System.out.println("transactionCode=" + header.getTransactionCode());
        System.out.println("processingType=" + header.getProcessingType());
        System.out.println("guid=" + header.getGuid());
        System.out.println("traceId=" + header.getTraceId());
        System.out.println("channelId=" + header.getChannelId());
        System.out.println("userId=" + header.getUserId());
        System.out.println("branchId=" + header.getBranchId());
        System.out.println("centerId=" + header.getCenterId());
        System.out.println("requestTime=" + header.getRequestTime());
        System.out.println("clientIp=" + header.getClientIp());
        System.out.println("idempotencyKey=" + header.getIdempotencyKey());
    }

    private String formatPayload(Object value) {
        if (value == null) {
            return "    null";
        }
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                return "    {}";
            }
            StringBuilder sb = new StringBuilder("    {");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(System.lineSeparator())
                        .append("      ")
                        .append(entry.getKey())
                        .append('=')
                        .append(entry.getValue());
                first = false;
            }
            sb.append(System.lineSeparator()).append("    }");
            return sb.toString();
        }
        return "    " + value;
    }
}
