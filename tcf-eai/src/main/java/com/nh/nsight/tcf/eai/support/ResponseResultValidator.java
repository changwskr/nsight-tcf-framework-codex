package com.nh.nsight.tcf.eai.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.tcf.eai.exception.IntegrationBusinessException;
import com.nh.nsight.tcf.eai.exception.IntegrationErrorCode;
import com.nh.nsight.tcf.eai.exception.IntegrationException;
import com.nh.nsight.tcf.eai.model.IntegrationCallResult;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 대상 서비스 표준 응답 전문을 파싱/검증하여 {@link IntegrationCallResult} 로 변환한다.
 *
 * <p>성공 판정 기준은 {@code result.resultCode == "S0000"} 이며,
 * 업무 실패 시 {@link IntegrationBusinessException} 으로 전파한다.
 */
public final class ResponseResultValidator {

    /** TCF 표준 성공 코드. */
    public static final String SUCCESS_CODE = "S0000";

    private ResponseResultValidator() {
    }

    @SuppressWarnings("unchecked")
    public static IntegrationCallResult parseAndValidate(ObjectMapper objectMapper,
                                                         String targetServiceId,
                                                         String responseBody,
                                                         long elapsedMs) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
        } catch (Exception e) {
            throw new IntegrationException(IntegrationErrorCode.MESSAGE, targetServiceId,
                    "연동 응답 전문 파싱 실패: " + e.getMessage(), e);
        }

        JsonNode resultNode = root.path("result");
        if (resultNode.isMissingNode() || resultNode.isNull()) {
            throw new IntegrationException(IntegrationErrorCode.MESSAGE, targetServiceId,
                    "연동 응답에 result 가 없습니다.");
        }

        String resultCode = resultNode.path("resultCode").asText("");
        String message = resultNode.path("message").asText("");

        Map<String, Object> body;
        JsonNode bodyNode = root.path("body");
        if (bodyNode.isMissingNode() || bodyNode.isNull()) {
            body = new LinkedHashMap<>();
        } else {
            try {
                body = objectMapper.convertValue(bodyNode, Map.class);
            } catch (Exception e) {
                body = new LinkedHashMap<>();
            }
        }

        boolean success = SUCCESS_CODE.equals(resultCode);
        if (!success) {
            throw new IntegrationBusinessException(targetServiceId, resultCode,
                    message.isBlank() ? ("대상 서비스 업무 실패: " + resultCode) : message, body);
        }

        return new IntegrationCallResult(true, resultCode, message, body, elapsedMs);
    }
}
