package nhnis.fw.commons.filter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.fw.commons.jwt.JwtProvider;

@Slf4j
@Component
public class DefaultFilter implements Filter {

    private final ClientHttpConnector connector;

    private static final String GUID = "guid";
    private static final String IP = "ip";
    private static final String USER_ID = "userId";
    private static final String SERVICE_ID = "serviceId";
    private static final String HEADER = "hdr_nhnis";
    private static final String MULTI_PART = "multipart/";

    @Value("${spring.profiles.active:local}")
    private String active;

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private JwtProvider jwtProvider;

    DefaultFilter(ClientHttpConnector connector) {
        this.connector = connector;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        if (log.isDebugEnabled()) {
            log.debug("Application DefaultFilter is loaded");
        }
    }

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain
    ) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // ThreadContext 세팅
        String uri = request.getRequestURI();
        String serviceId = uri.contains("/")
                ? uri.substring(uri.lastIndexOf("/") + 1)
                : "UNKNOWN";
        ThreadContext.put(SERVICE_ID, serviceId);

        HttpHeaders requestHeaders = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            requestHeaders.add(name, request.getHeader(name));
        }

        try {
            if (request.getContentType() != null
                    && request.getContentType().startsWith(MULTI_PART)) {
                String guid = request.getHeader("X-GUID");
                sys_comm sysComm = new sys_comm();
                sysComm.setStd_gbl_id(guid);
                hdr_nhnis header = new hdr_nhnis();
                header.setSys_comm(sysComm);
                ThreadContext.put(GUID, guid);

                ServiceContext serviceContext = new ServiceContext(
                        applicationName,
                        guid,
                        active,
                        requestHeaders,
                        request,
                        response,
                        header
                );
                ServiceContextHolder.setInstance(serviceContext);
                filterChain.doFilter(request, servletResponse);
            } else {
                CachedBodyHttpServletRequest wrapper =
                        new CachedBodyHttpServletRequest(request);

                /* SSO Check */
                if (!active.toLowerCase().equals("local")) {
                    String authorization =
                            request.getHeader(HttpHeaders.AUTHORIZATION);
                    if (authorization == null
                            || !authorization.startsWith("Bearer ")) {
                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Access Token이 없습니다."
                        );
                        return;
                    }

                    String token = authorization.substring(7);

                    if (!jwtProvider.validate(token)) {
                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "유효하지 않은 Token입니다."
                        );
                        return;
                    } else if (!jwtProvider.isAccessToken(token)) {
                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Access Token이 아닙니다."
                        );
                        return;
                    }
                    String ssoId = jwtProvider.getSsoId(token);
                    request.setAttribute("ssoId", ssoId);
                }

                String requestBody = wrapper.getReader()
                        .lines()
                        .collect(Collectors.joining(
                                System.lineSeparator()
                        ));

                if (requestBody == null || requestBody.isEmpty()) {
                    response.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Request Body 입력 전문이 비어있습니다."
                    );
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false
                );

                Map<String, Object> rootMap = null;
                try {
                    rootMap = mapper.readValue(
                            requestBody,
                            new TypeReference<Map<String, Object>>() {}
                    );
                } catch (JsonProcessingException e) {
                    log.error(
                            "Request Body JSON 파싱 실패 : {}",
                            e.getMessage(),
                            e
                    );
                    response.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "잘못된 JSON 형식 입니다."
                    );
                    return;
                }

                Map<String, Object> headerMap =
                        (Map<String, Object>) rootMap.get(HEADER);

                if (headerMap == null) {
                    response.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "공통 Header 정보가 없습니다."
                    );
                }

                hdr_nhnis header = convertMapToDto(
                        mapper,
                        headerMap,
                        hdr_nhnis.class
                );

                String guid = header.getSys_comm().getStd_gbl_id();

                ThreadContext.put(
                        GUID,
                        header.getSys_comm().getStd_gbl_id()
                );
                ThreadContext.put(
                        IP,
                        header.getSys_comm().getTr_trm_ipadr()
                );
                ThreadContext.put(
                        USER_ID,
                        header.getSys_comm().getOptr_eno()
                );

                ServiceContext serviceContext = new ServiceContext(
                        applicationName,
                        guid,
                        active,
                        requestHeaders,
                        request,
                        response,
                        header
                );
                ServiceContextHolder.setInstance(serviceContext);

                // 서블릿 호출
                filterChain.doFilter(wrapper, servletResponse);
            }
        } catch (Exception e) {
            log.error(
                    "Filter 처리 중 Exception 발생: {}",
                    e.getMessage(),
                    e
            );
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Filter 처리 중 Exception 발생"
            );
            return;
        } finally {
            if (ServiceContextHolder.getInstance() != null) {
                ServiceContextHolder.removeInstance();
            }
            if (!ThreadContext.isEmpty()) {
                ThreadContext.clearAll();
            }
        }
    }

    private <T> T convertMapToDto(
            ObjectMapper mapper,
            Map<String, Object> map,
            Class<T> clazz
    ) {
        map.remove("dtoLogicalName");
        return mapper.convertValue(map, clazz);
    }
}
