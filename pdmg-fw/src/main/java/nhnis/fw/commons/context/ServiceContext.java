package nhnis.fw.commons.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import nhnis.fw.commons.dto.header.hdr_nhnis;

@Getter
public class ServiceContext {

    private String applicationName;
    private String guid;
    private String active;
    private HttpHeaders requestHeaders;
    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;
    private hdr_nhnis header;
    private Map<String, Object> userContext = new HashMap<>();
    /** 브라우저/클라이언트에서 수신한 온라인 요청 전문 원문. */
    private String requestBody;
    /** 시스템 후처리에서 조립한 응답 전문 원문. */
    private String responseBody;

    public ServiceContext(String applicationName, String guid, String active, HttpHeaders requestHeaders,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            hdr_nhnis header) {
        this.applicationName = applicationName;
        this.guid = guid;
        this.active = active;
        this.requestHeaders = requestHeaders;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.header = header;
    }

    /** 시스템 선처리에서 GUID 강제 채번 시 컨텍스트를 맞춘다. */
    public void setGuid(String guid) {
        this.guid = guid;
    }

    public void setHeader(hdr_nhnis header) {
        this.header = header;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}
