/****************************************************************************
 * Copyright 2026 by Nonghyup. All rights reserved. Nonghyup 의 사전 승인 없이
 * 본 내용의 전부 또는 일부에 대한 복사, 배포, 사용을 금합니다.
 ****************************************************************************/
package nhnis.fw.commons.log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 온라인 전문을 로그용으로 가독성 있게 출력한다.
 *
 * <p>JSON이면 pretty-print, 아니면 원문을 그대로 반환한다.
 */
public final class PdmgMessagePrinter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /** 클라이언트로 나가는 전문과 동일하게 직렬화(압축 JSON, 가공 없음). */
    private static final ObjectMapper WIRE_MAPPER = new ObjectMapper();

    private static final String DTO_LOGICAL_NAME = "dtoLogicalName";
    private static final String FIELD_PROPERTY_MAP = "fieldPropertyMap";

    private PdmgMessagePrinter() {
    }

    /** 수신 원문을 가공 없이 그대로 반환. */
    public static String asIs(String raw) {
        if (raw == null || raw.isBlank()) {
            return "(empty)";
        }
        return raw;
    }

    /** 응답 객체를 클라이언트로 내려가는 JSON 원문(압축)으로 반환. */
    public static String asIsWireJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence cs) {
            return asIs(cs.toString());
        }
        try {
            return WIRE_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * 업무 DTO 로그용. fieldPropertyMap / *List / *Array 를 제거하고 한 줄 JSON으로 출력한다.
     * (DataObject 메타가 로그를 오염·절단시키는 것을 방지)
     */
    public static String businessDto(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence cs) {
            return ofRaw(cs.toString());
        }
        try {
            JsonNode node = WIRE_MAPPER.valueToTree(value);
            sanitize(node);
            return WIRE_MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /** 원문 JSON 문자열을 들여쓰기해 반환. JSON이 아니면 원문 그대로. */
    public static String ofRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return "(empty)";
        }
        try {
            JsonNode node = MAPPER.readTree(raw);
            sanitize(node);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            return raw;
        }
    }

    /** 객체(DTO 등)를 들여쓰기 JSON으로 반환. */
    public static String ofObject(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence cs) {
            return ofRaw(cs.toString());
        }
        try {
            JsonNode node = MAPPER.valueToTree(value);
            sanitize(node);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /** 컨트롤러 인자 중 업무 전문만 골라 출력. */
    public static String ofArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "(no args)";
        }
        List<Object> payloads = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            String name = arg.getClass().getName();
            if (name.startsWith("jakarta.servlet.")
                    || name.startsWith("javax.servlet.")
                    || name.startsWith("org.springframework.")) {
                continue;
            }
            payloads.add(arg);
        }
        if (payloads.isEmpty()) {
            return "(no business payload)";
        }
        if (payloads.size() == 1) {
            return ofObject(payloads.get(0));
        }
        return ofObject(payloads);
    }

    @SuppressWarnings("deprecation")
    private static void sanitize(JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.remove(DTO_LOGICAL_NAME);
            objectNode.remove(FIELD_PROPERTY_MAP);

            List<String> fieldsToRemove = new ArrayList<>();
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.endsWith("List") || fieldName.endsWith("Array")) {
                    fieldsToRemove.add(fieldName);
                }
            }
            fieldsToRemove.forEach(objectNode::remove);

            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                sanitize(fields.next().getValue());
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                sanitize(element);
            }
        }
    }
}
