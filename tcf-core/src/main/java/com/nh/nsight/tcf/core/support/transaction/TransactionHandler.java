package com.nh.nsight.tcf.core.support.transaction;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.TcfConsoleLog;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 온라인 거래 핸들러.
 *
 * <p>매핑 방식은 두 가지를 지원한다(하위호환).
 * <ul>
 *   <li><b>단일 거래</b>: {@link #serviceId()} 하나만 재정의 → 그 serviceId 하나에 매핑.</li>
 *   <li><b>도메인 묶음</b>: {@link #serviceIds()} 를 재정의 → 여러 serviceId 를 한 핸들러가 처리.
 *       이때 {@link #doHandle} 내부에서 {@code context.getHeader().getServiceId()} 로 분기한다.</li>
 * </ul>
 */
public interface TransactionHandler {

    /** 단일 거래 매핑용 serviceId. 도메인 묶음 핸들러는 재정의하지 않아도 된다. */
    default String serviceId() {
        return null;
    }

    /**
     * 이 핸들러가 처리하는 serviceId 목록. 기본은 {@link #serviceId()} 단일값을 감싼다.
     * 여러 거래를 한 핸들러가 담당하려면 이 메서드를 재정의한다.
     */
    default Collection<String> serviceIds() {
        String id = serviceId();
        return id == null ? List.of() : List.of(id);
    }

    default Object handle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String id = resolveLogId(request);
        TcfConsoleLog.boundary("Handler", id, "START");
        try {
            Object result = doHandle(request, context);
            TcfConsoleLog.boundary("Handler", id, "END");
            return result;
        } catch (RuntimeException e) {
            TcfConsoleLog.boundary("Handler", id, "END", "error");
            throw e;
        }
    }

    private String resolveLogId(StandardRequest<Map<String, Object>> request) {
        if (request != null && request.getHeader() != null && request.getHeader().getServiceId() != null) {
            return request.getHeader().getServiceId();
        }
        String id = serviceId();
        return id != null ? id : getClass().getSimpleName();
    }

    Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context);
}
