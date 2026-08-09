package nhnis.fw.exception;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * exceptionCode.yml의 {@code nhnis.exception.*} 메시지 사전.
 *
 * <p>Spring의 완화된 바인딩은 맵 키를 소문자로 정규화할 수 있어 조회는 대소문자를 가리지 않는다.
 */
@ConfigurationProperties("nhnis")
public class ExceptionCodeProperties {

    /** 미분류 오류에 사용할 기본 코드. */
    public static final String DEFAULT_CODE = "FW9999";

    private static final String FALLBACK_MESSAGE = "시스템 오류가 발생하였습니다.";

    private Map<String, String> exception = new LinkedHashMap<>();

    public Map<String, String> getException() {
        return exception;
    }

    public void setException(Map<String, String> exception) {
        Map<String, String> caseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (exception != null) {
            caseInsensitive.putAll(exception);
        }
        this.exception = caseInsensitive;
    }

    public String message(String code, Object... args) {
        String template = exception.get(code);
        if (template == null) {
            template = exception.getOrDefault(DEFAULT_CODE, FALLBACK_MESSAGE);
        }
        if (args == null || args.length == 0) {
            return template;
        }
        return MessageFormat.format(template, args);
    }
}
