package com.nh.nsight.tcf.core.support.message.catalog;

import com.nh.nsight.tcf.core.support.message.ProcessingType;
import com.nh.nsight.tcf.core.support.message.Result;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TCF 표준 전문(준문) 카탈로그 — {@code tcf-core} message 패키지의 Single Source of Truth.
 * OM 전문구조 frameworkInquiry·시드·문서가 이 정의를 따른다.
 */
public final class TcfStandardMessageCatalog {

    public static final String STRUCT_STD_REQ_HEADER = "STD-REQ-HEADER";
    public static final String STRUCT_STD_RES_HEADER = "STD-RES-HEADER";
    public static final String STRUCT_STD_RES_RESULT = "STD-RES-RESULT";
    public static final String STRUCT_STD_REQ_BODY = "STD-REQ-BODY";

    private static final String HEADER_CLASS = StandardHeader.class.getName();
    private static final String RESULT_CLASS = Result.class.getName();
    private static final String REQUEST_CLASS = StandardRequest.class.getName();

    private static final List<TcfMessageStructDefinition> STRUCTURES = List.of(
            struct(
                    STRUCT_STD_REQ_HEADER,
                    null,
                    null,
                    null,
                    "REQUEST",
                    "HEADER",
                    "표준 요청 Header (StandardHeader)",
                    "StandardHeader — STF StandardHeaderValidator 필수·선택 필드",
                    HEADER_CLASS,
                    defineRequestHeaderFields()),
            struct(
                    STRUCT_STD_RES_HEADER,
                    null,
                    null,
                    null,
                    "RESPONSE",
                    "HEADER",
                    "표준 응답 Header (StandardHeader)",
                    "요청 Header의 guid/traceId 유지 — StandardResponse.header",
                    HEADER_CLASS,
                    defineResponseHeaderFields()),
            struct(
                    STRUCT_STD_RES_RESULT,
                    null,
                    null,
                    null,
                    "RESPONSE",
                    "RESULT",
                    "표준 응답 Result",
                    "result.resultCode == S0000 이면 업무 성공 — StandardResponse.result",
                    RESULT_CLASS,
                    defineResultFields()),
            struct(
                    STRUCT_STD_REQ_BODY,
                    null,
                    null,
                    null,
                    "REQUEST",
                    "BODY",
                    "요청 Body (업무 가변)",
                    "업무·serviceId별 자유 정의 — StandardRequest.body (Map)",
                    REQUEST_CLASS + "<T>",
                    List.of(field(
                            "sampleKey",
                            "샘플키",
                            "STRING",
                            "N",
                            100,
                            null,
                            "SV-SAMPLE",
                            null,
                            "업무 샘플 필드 예시",
                            1))));

    private TcfStandardMessageCatalog() {
    }

    public static List<TcfMessageStructDefinition> all() {
        return STRUCTURES;
    }

    public static Optional<TcfMessageStructDefinition> findByStructCode(String structCode) {
        if (structCode == null || structCode.isBlank()) {
            return Optional.empty();
        }
        return STRUCTURES.stream()
                .filter(def -> structCode.equals(def.structCode()))
                .findFirst();
    }

    public static List<TcfMessageFieldDefinition> requestHeaderFields() {
        return findByStructCode(STRUCT_STD_REQ_HEADER)
                .map(TcfMessageStructDefinition::fields)
                .orElse(List.of());
    }

    public static Set<String> requiredRequestHeaderFieldKeys() {
        return requestHeaderFields().stream()
                .filter(field -> "Y".equals(field.requiredYn()))
                .map(TcfMessageFieldDefinition::fieldKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String readHeaderField(StandardHeader header, String fieldKey) {
        if (header == null || fieldKey == null) {
            return null;
        }
        return switch (fieldKey) {
            case "systemId" -> header.getSystemId();
            case "businessCode" -> header.getBusinessCode();
            case "serviceId" -> header.getServiceId();
            case "serviceName" -> header.getServiceName();
            case "transactionCode" -> header.getTransactionCode();
            case "processingType" -> header.getProcessingType();
            case "guid" -> header.getGuid();
            case "traceId" -> header.getTraceId();
            case "channelId" -> header.getChannelId();
            case "userId" -> header.getUserId();
            case "branchId" -> header.getBranchId();
            case "centerId" -> header.getCenterId();
            case "requestTime" -> header.getRequestTime();
            case "clientIp" -> header.getClientIp();
            case "idempotencyKey" -> header.getIdempotencyKey();
            default -> throw new IllegalArgumentException("Unknown StandardHeader field: " + fieldKey);
        };
    }

    public static List<Map<String, Object>> templatesAsMaps() {
        List<Map<String, Object>> templates = new ArrayList<>();
        for (TcfMessageStructDefinition definition : STRUCTURES) {
            templates.add(definition.toMap());
        }
        return templates;
    }

    public static String processingTypeValidationRule() {
        return Arrays.stream(ProcessingType.values())
                .map(Enum::name)
                .collect(Collectors.joining("|"));
    }

    private static TcfMessageStructDefinition struct(
            String structCode,
            String businessCode,
            String serviceId,
            String transactionCode,
            String messageType,
            String segmentType,
            String structName,
            String description,
            String sourceClass,
            List<TcfMessageFieldDefinition> fields) {
        return new TcfMessageStructDefinition(
                structCode,
                businessCode,
                serviceId,
                transactionCode,
                messageType,
                segmentType,
                structName,
                description,
                sourceClass,
                fields);
    }

    private static TcfMessageFieldDefinition field(
            String fieldKey,
            String fieldLabel,
            String dataType,
            String requiredYn,
            Integer maxLength,
            String defaultValue,
            String sampleValue,
            String validationRule,
            String description,
            int sortOrder) {
        return new TcfMessageFieldDefinition(
                fieldKey,
                fieldLabel,
                dataType,
                requiredYn,
                maxLength,
                defaultValue,
                sampleValue,
                validationRule,
                description,
                sortOrder);
    }

    private static List<TcfMessageFieldDefinition> defineRequestHeaderFields() {
        String processingTypes = processingTypeValidationRule();
        return List.of(
                field("systemId", "연계 시스템 ID", "STRING", "N", 30, "NSIGHT-MP", "NSIGHT-MP", null,
                        "미입력 시 NSIGHT-MP (StandardHeader.normalize)", 1),
                field("businessCode", "업무코드", "STRING", "Y", 10, null, "SV", "대문자",
                        "WAR/context 식별", 2),
                field("serviceId", "서비스 ID", "STRING", "Y", 100, null, "SV.Sample.inquiry", null,
                        "Dispatcher 라우팅 키", 3),
                field("serviceName", "서비스명", "STRING", "N", 200, null, "SV 샘플 조회", null,
                        "표시·감사용 (선택)", 4),
                field("transactionCode", "거래코드", "STRING", "Y", 50, null, "SV-INQ-0001", null,
                        "화면·거래 설계 ID", 5),
                field("processingType", "처리유형", "STRING", "Y", 20, null, "INQUIRY", processingTypes,
                        "ProcessingType enum", 6),
                field("guid", "거래 GUID", "STRING", "N", 64, null, null, "UUID",
                        "빈 값이면 STF 자동 생성", 7),
                field("traceId", "추적 ID", "STRING", "N", 80, null, null, null,
                        "빈 값이면 STF 자동 생성", 8),
                field("channelId", "채널 ID", "STRING", "Y", 30, null, "WEBTOP", null,
                        "채널·포털 식별", 9),
                field("userId", "사용자 ID", "STRING", "N", 50, null, "U123456", null,
                        "세션·권한·감사 (@JsonAlias user)", 10),
                field("branchId", "부점코드", "STRING", "N", 20, null, "001234", null,
                        "@JsonAlias branch", 11),
                field("centerId", "센터코드", "STRING", "N", 20, null, "DC1", null, null, 12),
                field("requestTime", "요청시각", "STRING", "N", 40, null, null, "ISO-8601",
                        "미입력 시 서버 현재 시각", 13),
                field("clientIp", "클라이언트 IP", "STRING", "N", 50, null, "127.0.0.1", null,
                        "Controller 보완", 14),
                field("idempotencyKey", "멱등키", "STRING", "N", 100, null, null, null,
                        "중복 요청 방지", 15));
    }

    private static List<TcfMessageFieldDefinition> defineResponseHeaderFields() {
        return List.of(
                field("systemId", "연계 시스템 ID", "STRING", "N", 30, "NSIGHT-MP", "NSIGHT-MP", null,
                        "요청과 동일 유지", 1),
                field("businessCode", "업무코드", "STRING", "Y", 10, null, "SV", null, null, 2),
                field("serviceId", "서비스 ID", "STRING", "Y", 100, null, "SV.Sample.inquiry", null, null, 3),
                field("serviceName", "서비스명", "STRING", "N", 200, null, null, null, null, 4),
                field("transactionCode", "거래코드", "STRING", "N", 50, null, "SV-INQ-0001", null, null, 5),
                field("processingType", "처리유형", "STRING", "N", 20, null, "INQUIRY", null, null, 6),
                field("guid", "거래 GUID", "STRING", "Y", 64, null, null, null, "요청과 동일", 7),
                field("traceId", "추적 ID", "STRING", "N", 80, null, null, null, "요청과 동일", 8),
                field("channelId", "채널 ID", "STRING", "N", 30, null, "WEBTOP", null, null, 9),
                field("requestTime", "요청시각", "STRING", "N", 40, null, null, "ISO-8601", null, 10));
    }

    private static List<TcfMessageFieldDefinition> defineResultFields() {
        return List.of(
                field("resultCode", "결과코드", "STRING", "Y", 10, "S0000", "S0000", "S0000=성공",
                        "HTTP Status가 아닌 업무 성공 기준", 1),
                field("resultMessage", "결과메시지", "STRING", "Y", 500, null, "정상 처리되었습니다.", null, null, 2),
                field("errorCode", "오류코드", "STRING", "N", 50, null, null, null, "실패 시", 3),
                field("errorMessage", "오류메시지", "STRING", "N", 500, null, null, null, null, 4),
                field("errorDetail", "오류상세", "STRING", "N", 2000, null, null, null, null, 5),
                field("errorSystemId", "오류시스템", "STRING", "N", 30, "NSIGHT-MP", null, null, null, 6),
                field("errorDateTime", "오류시각", "STRING", "N", 40, null, null, "ISO-8601", null, 7));
    }
}
