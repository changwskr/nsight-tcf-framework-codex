package com.nh.nsight.tcf.eai.client;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.eai.model.IntegrationCallRequest;
import com.nh.nsight.tcf.eai.model.IntegrationCallResult;
import java.util.Map;

/**
 * 서비스 간 표준 전문 호출 공통 Client.
 *
 * <p>업무 서비스는 다른 업무 서비스의 Java 코드를 직접 참조하지 않고,
 * 이 Client 를 통해 {@code POST /{businessCode}/online} + serviceId 방식으로 호출한다.
 *
 * <p>대상별 Adapter 없이 한 줄로 호출하려면 {@link #call(String, String, String, Map, TransactionContext)}
 * 또는 {@link #callForBody(String, String, String, Map, TransactionContext)} 를 사용한다.
 */
public interface TcfServiceClient {

    /**
     * 대상 서비스를 호출한다. 호출 측 컨텍스트의 GUID/TraceId/사용자 정보를 전파한다.
     *
     * @param request       대상 식별 정보와 body
     * @param callerContext 호출 측 거래 컨텍스트 (null 허용 — 신규 GUID 생성)
     * @return 정규화된 연동 결과
     */
    IntegrationCallResult call(IntegrationCallRequest request, TransactionContext callerContext);

    /**
     * 대상별 Adapter 없이 한 줄로 호출하는 편의 메서드 (INQUIRY 기본).
     *
     * <pre>
     * IntegrationCallResult r = tcfServiceClient.call(
     *         "SV", "SV.Customer.selectSummary", "SV-INQ-0002",
     *         Map.of("customerNo", customerNo), context);
     * </pre>
     */
    default IntegrationCallResult call(String targetBusinessCode,
                                       String targetServiceId,
                                       String targetTransactionCode,
                                       Map<String, Object> body,
                                       TransactionContext callerContext) {
        IntegrationCallRequest request = IntegrationCallRequest.builder()
                .targetBusinessCode(targetBusinessCode)
                .targetServiceId(targetServiceId)
                .targetTransactionCode(targetTransactionCode)
                .body(body)
                .build();
        return call(request, callerContext);
    }

    /**
     * 성공 시 대상 응답 body(Map)만 바로 반환하는 편의 메서드.
     *
     * <p>업무 실패/Timeout/시스템 오류는 {@code IntegrationException} 계열로 전파된다.
     *
     * <pre>
     * Map&lt;String, Object&gt; summary = tcfServiceClient.callForBody(
     *         "SV", "SV.Customer.selectSummary", "SV-INQ-0002",
     *         Map.of("customerNo", customerNo), context);
     * </pre>
     */
    default Map<String, Object> callForBody(String targetBusinessCode,
                                            String targetServiceId,
                                            String targetTransactionCode,
                                            Map<String, Object> body,
                                            TransactionContext callerContext) {
        return call(targetBusinessCode, targetServiceId, targetTransactionCode, body, callerContext)
                .getBody();
    }
}
