package com.nh.nsight.tcf.core.support.dispatch;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.TcfConsoleLog;
import com.nh.nsight.tcf.core.support.transaction.TransactionHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TransactionDispatcher {
    private static final Logger log = LoggerFactory.getLogger(TransactionDispatcher.class);
    private final Map<String, TransactionHandler> handlerMap = new ConcurrentHashMap<>();

    public TransactionDispatcher(List<TransactionHandler> handlers) {
        TcfConsoleLog.boundary("Dispatcher", "init", "START");
        for (TransactionHandler handler : handlers) {
            java.util.Collection<String> serviceIds = handler.serviceIds();
            if (serviceIds == null || serviceIds.isEmpty()) {
                log.warn("Handler declares no serviceId, skipped: {}", handler.getClass().getName());
                continue;
            }
            for (String serviceId : serviceIds) {
                TransactionHandler previous = handlerMap.put(serviceId, handler);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate serviceId detected: " + serviceId);
                }
                TcfConsoleLog.step("Dispatcher", "init", "register", serviceId);
                log.info("Registered NSIGHT handler. serviceId={} handler={}",
                        serviceId, handler.getClass().getSimpleName());
            }
        }
        System.out.println(" ============================[Dispatcher] handlerMap dump (size=" + handlerMap.size() + ")");
        handlerMap.forEach((serviceId, mappedHandler) ->
                System.out.println("    " + serviceId + " -> " + mappedHandler.getClass().getName()));
        System.out.println(" ============================[Dispatcher] handlerMap dump end");
        TcfConsoleLog.boundary("Dispatcher", "init", "END", "handlers=" + handlerMap.size());
    }

    public Object dispatch(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        TcfConsoleLog.boundary("Dispatcher", "dispatch", "START");
        String serviceId = request.getHeader() == null ? null : request.getHeader().getServiceId();
        TcfConsoleLog.step("Dispatcher", "dispatch", "resolve serviceId", serviceId);
        if (!StringUtils.hasText(serviceId)) {
            TcfConsoleLog.boundary("Dispatcher", "dispatch", "END", "invalid serviceId");
            throw new BusinessException(ErrorCode.INVALID_HEADER, "serviceId가 없습니다.");
        }
        TransactionHandler handler = handlerMap.get(serviceId);
        if (handler == null) {
            TcfConsoleLog.boundary("Dispatcher", "dispatch", "END", "handler not found");
            throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND, "등록되지 않은 serviceId입니다: " + serviceId);
        }
        TcfConsoleLog.step("Dispatcher", "dispatch", "handler.handle", serviceId);
        Object result = handler.handle(request, context);
        TcfConsoleLog.boundary("Dispatcher", "dispatch", "END");
        return result;
    }

    public Map<String, TransactionHandler> handlers() {
        return Map.copyOf(handlerMap);
    }
}
