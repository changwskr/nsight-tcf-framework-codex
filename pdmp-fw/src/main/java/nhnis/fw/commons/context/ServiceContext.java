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
}
