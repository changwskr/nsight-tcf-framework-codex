package nhnis.fw.commons.resolver;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import nhnis.fw.commons.log.PdmkTxFlowLog;

@Component
@RequiredArgsConstructor
public class RequestBodyArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger(RequestBodyArgumentResolver.class);

    private final ObjectMapper objectMapper;
    private static final String MULTI_PART = "multipart/";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith(MULTI_PART)) {
            return false;
        }
        return parameter.hasParameterAnnotation(RequestBody.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        PdmkTxFlowLog.enter(log, RequestBodyArgumentResolver.class, "resolveArgument");
        try {
            objectMapper.configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false
            );
            objectMapper.configure(
                    DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                    true
            );

            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            String requestBody = request.getReader().lines().collect(Collectors.joining());

            JsonNode root = objectMapper.readTree(requestBody);
            JsonNode bodyNode = root == null ? null : root.get("dto");
            if (bodyNode == null || bodyNode.isNull()) {
                bodyNode = objectMapper.createObjectNode();
            }

            Class<?> dtoClass = parameter.getParameterType();

            return objectMapper.treeToValue(bodyNode, dtoClass);
        } finally {
            PdmkTxFlowLog.leave(log, RequestBodyArgumentResolver.class, "resolveArgument");
        }
    }
}
