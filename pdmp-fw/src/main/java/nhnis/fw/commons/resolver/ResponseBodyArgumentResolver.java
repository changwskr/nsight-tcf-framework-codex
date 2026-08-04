package nhnis.fw.commons.resolver;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ims.superspring.dto.engine.exception.MarshalException;

import nhnis.fw.commons.configuration.WebConfiguration;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.context.SpringContext;
import nhnis.fw.commons.dto.NH_NIS_ERR_DTO;
import nhnis.fw.commons.dto.NH_NIS_ERR_DTOMsgJson;
import nhnis.fw.commons.exception.NhBaseException;
import nhnis.fw.commons.exception.NhBaseException.TYPE;
import nhnis.fw.commons.message.MessageCache;

@ControllerAdvice
public class ResponseBodyArgumentResolver implements ResponseBodyAdvice<Object> {

    private final WebConfiguration webConfiguration;

    private final ClientHttpConnector connector;

    @Autowired
    private MessageCache messageCache;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PREFIX = "nhnis.exception.";
    private static final String HEADER = "hdr_nhnis";
    private static final String DTO = "dto";
    private static final String DTO_LOGICAL_NAME = "dtoLogicalName";
    private static final String FIELD_PROPERTY_MAP = "fieldPropertyMap";
    private static final String SYS_COMM = "sys_comm";
    private static final String MULTI_PART = "multipart";

    ResponseBodyArgumentResolver(ClientHttpConnector connector, WebConfiguration webConfiguration) {
        this.connector = connector;
        this.webConfiguration = webConfiguration;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        ObjectNode responseBody = objectMapper.createObjectNode();
        try {
            if (request.getHeaders().getContentType().toString().startsWith(MULTI_PART)) {
                return body;
            }

            /* Response Header */
            if (ServiceContextHolder.getInstance() != null) {
                ObjectNode headerNode = objectMapper.valueToTree(ServiceContextHolder.getInstance().getHeader());
                headerNode.remove(DTO_LOGICAL_NAME);
                responseBody.set(HEADER, headerNode);
            }

            /* Response Body */
            if (body instanceof NH_NIS_ERR_DTO) {
                NH_NIS_ERR_DTOMsgJson json = new NH_NIS_ERR_DTOMsgJson();
                try {
                    byte[] errorJson = json.marshal((NH_NIS_ERR_DTO) body);
                    responseBody.set(DTO, objectMapper.readTree(errorJson));
                } catch (MarshalException | IOException e) {
                    responseBody.set(DTO, objectMapper.valueToTree(body));
                }
            } else {
                JsonNode bodyNode = objectMapper.valueToTree(body);

                // 최상위 및 중첩된 모든 DTO 에서 불필요한 필드 제거
                removeUnwantedFields(bodyNode);

                responseBody.set(DTO, bodyNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseBody;
    }

    @ExceptionHandler(NhBaseException.class)
    public ResponseEntity<NH_NIS_ERR_DTO> errorProcessor(NhBaseException e) {
        NH_NIS_ERR_DTO errorDto = new NH_NIS_ERR_DTO();
        errorDto.setErrMethodName(e.getErrMethodName());
        errorDto.setStdErrCode(e.getStdErrCode());
        errorDto.setErrFileName(e.getErrFileName());
        errorDto.setErrClassName(e.getErrClassName());
        errorDto.setErrLineNo(e.getPgmLineNo());
        if (e.getAddMsgContents() != null) {
            errorDto.setAddMsgContents(e.getAddMsgContents());
        }
        if (e.getErrMsgType() == null) {
            errorDto.setErrType(TYPE.RUNTIME.name());
        } else {
            errorDto.setErrType(e.getErrMsgType().name());
        }
        if (e.getStackTrace() != null) {
            errorDto.setStackTrace(getTop15StackTrace(e.getStackTrace()));
        }
        if (e.getStdErrMsgContents() == null) {
            switch (e.getErrMsgType()) {
                case TYPE.RUNTIME, TYPE.COMMON, TYPE.AUTH -> {
                    String template = SpringContext.getProperty(PREFIX + e.getStdErrCode());
                    String errorMessage = MessageFormat.format(template, e.getMessageValue());
                    errorDto.setStdErrMsgCntn(errorMessage);
                }
                case TYPE.SERVICE, TYPE.BIZ -> {
                    // DB 조회 후 데이터 DB 버전으로 세팅
                    String errorMessage = messageCache.getMessage(e.getStdErrCode(), e.getMessageValue());
                    errorDto.setStdErrMsgCntn(errorMessage);
                }
            }
        } else {
            errorDto.setStdErrMsgCntn(e.getStdErrMsgContents());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    private List<String> getTop15StackTrace(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).limit(15)
                .map(StackTraceElement::toString).collect(Collectors.toList());
    }

    /**
     * <PRE>
     * JSON 트리를 재귀적으로 순회하며 dtoLogicalName, fieldPropertyMap, 그리고 *List/*Array 로
     * 끝나는 필드를 제거
     * - ObjectNode: 필드 제거 후 하위 필드 재귀 처리
     * - ArrayNode: 배열 요소별 재귀 처리
     * </PRE>
     *
     * @param node 처리할 JSON 노드
     */
    @SuppressWarnings("deprecation")
    private void removeUnwantedFields(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            // 불필요한 필드 제거
            objectNode.remove(DTO_LOGICAL_NAME);
            objectNode.remove(FIELD_PROPERTY_MAP);

            // *List 와 *Array 로 끝나는 필드 제거 (ConcurrentModificationException 방지를
            // 위해 먼저 리스트로 수집)
            List<String> fieldsToRemove = new java.util.ArrayList<>();
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.endsWith("List") || fieldName.endsWith("Array")) {
                    fieldsToRemove.add(fieldName);
                }
            }
            fieldsToRemove.forEach(objectNode::remove);

            // 하위 필드 재귀 처리
            Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
                removeUnwantedFields(entry.getValue());
            }
        } else if (node.isArray()) {
            // 배열 요소별 재귀 처리
            for (com.fasterxml.jackson.databind.JsonNode element : node) {
                removeUnwantedFields(element);
            }
        }
    }
}
