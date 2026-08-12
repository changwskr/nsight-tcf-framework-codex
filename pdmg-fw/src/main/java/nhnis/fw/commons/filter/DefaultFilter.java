package nhnis.fw.commons.filter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.fw.commons.jwt.JwtProvider;
import nhnis.fw.commons.log.PdmgTxFlowLog;

/**
 * PDMG 공통 요청 필터. ServiceContext / GUID / JWT(비-local)를 준비한다.
 *
 * <p>{@code nhnis.fw.commons.filter.enabled=true} 일 때 활성.
 * local 프로파일에서는 {@code hdr_nhnis} 없이 {@code {"dto":...}} 만 와도 합성 Header로 통과시킨다.
 * {@link OncePerRequestFilter} 로 FORWARD/ERROR 디스패치에서 중복 실행을 막는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "nhnis.fw.commons.filter.enabled", havingValue = "true")
public class DefaultFilter extends OncePerRequestFilter {

    private final ClientHttpConnector connector; // reserved for APIGW/OCR relay hooks

    private static final String GUID = "guid";
    private static final String IP = "ip";
    private static final String USER_ID = "userId";
    private static final String SERVICE_ID = "serviceId";
    private static final String HEADER = "hdr_nhnis";
    private static final String MULTI_PART = "multipart/";
    /** Interceptor에서 요청 전문을 읽기 위한 request attribute 키. */
    public static final String REQUEST_BODY_ATTR = "PDMG_REQUEST_BODY";

    @Value("${spring.profiles.active:local}")
    private String active;

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private JwtProvider jwtProvider;

    DefaultFilter(ClientHttpConnector connector) {
        this.connector = connector;
    }

    /**
     * CORS preflight(OPTIONS)는 Body/JWT 검사가 없고 CorsFilter가 처리한다.
     * 여기서 막으면 브라우저 직접 호출이 실패한다.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {
        PdmgTxFlowLog.enter(log, DefaultFilter.class, "doFilter");
        try {
            doFilterInternal0(request, response, filterChain);
        } finally {
            PdmgTxFlowLog.leave(log, DefaultFilter.class, "doFilter");
        }
    }

    private void doFilterInternal0(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {

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
                filterChain.doFilter(request, response);
            } else {
                CachedBodyHttpServletRequest wrapper =
                        new CachedBodyHttpServletRequest(request);

                if (!"local".equalsIgnoreCase(active)) {
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

                Map<String, Object> rootMap;
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

                @SuppressWarnings("unchecked")
                Map<String, Object> headerMap =
                        (Map<String, Object>) rootMap.get(HEADER);

                hdr_nhnis header;
                if (headerMap == null) {
                    if (!"local".equalsIgnoreCase(active)) {
                        response.sendError(
                                HttpServletResponse.SC_BAD_REQUEST,
                                "공통 Header 정보가 없습니다."
                        );
                        return;
                    }
                    header = syntheticLocalHeader(wrapper, serviceId);
                } else {
                    header = convertMapToDto(
                            mapper,
                            headerMap,
                            hdr_nhnis.class
                    );
                    enrichMissingSysComm(header, wrapper, serviceId);
                }

                String guid = header.getSys_comm().getStd_gbl_id();

                ThreadContext.put(GUID, guid);
                if (header.getSys_comm().getTr_trm_ipadr() != null) {
                    ThreadContext.put(IP, header.getSys_comm().getTr_trm_ipadr());
                }
                if (header.getSys_comm().getOptr_eno() != null) {
                    ThreadContext.put(USER_ID, header.getSys_comm().getOptr_eno());
                }
                if (header.getSys_comm().getRms_svc_c() != null) {
                    ThreadContext.put(SERVICE_ID, header.getSys_comm().getRms_svc_c());
                }

                ServiceContext serviceContext = new ServiceContext(
                        applicationName,
                        guid,
                        active,
                        requestHeaders,
                        wrapper,
                        response,
                        header
                );
                serviceContext.setRequestBody(requestBody);
                ServiceContextHolder.setInstance(serviceContext);
                wrapper.setAttribute(REQUEST_BODY_ATTR, requestBody);

                filterChain.doFilter(wrapper, response);
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

    private hdr_nhnis syntheticLocalHeader(HttpServletRequest request, String serviceId) {
        String guid = UUID.randomUUID().toString().replace("-", "");
        sys_comm sysComm = new sys_comm();
        sysComm.setStd_gbl_id(guid);
        fillSysCommFromRequest(sysComm, request, serviceId);
        hdr_nhnis header = new hdr_nhnis();
        header.setSys_comm(sysComm);
        return header;
    }

    /** 헤더는 있으나 이미지로그 핵심 필드가 비어 있으면 요청 정보로 보강한다. */
    private void enrichMissingSysComm(hdr_nhnis header, HttpServletRequest request, String serviceId) {
        if (header == null) {
            return;
        }
        if (header.getSys_comm() == null) {
            header.setSys_comm(new sys_comm());
        }
        fillSysCommFromRequest(header.getSys_comm(), request, serviceId);
    }

    private void fillSysCommFromRequest(sys_comm sys, HttpServletRequest request, String serviceId) {
        if (isBlank(sys.getRms_svc_c()) && !isBlank(serviceId)) {
            sys.setRms_svc_c(serviceId);
        }
        if (isBlank(sys.getScid())) {
            sys.setScid(deriveScreenId(sys.getRms_svc_c()));
        }
        if (isBlank(sys.getTr_trm_ipadr())) {
            sys.setTr_trm_ipadr(resolveClientIp(request));
        }
        if (isBlank(sys.getOptr_eno())) {
            sys.setOptr_eno("LOCAL");
        }
        if (isBlank(sys.getTr_sysid())) {
            sys.setTr_sysid(applicationName);
        }
    }

    private static String deriveScreenId(String serviceId) {
        if (isBlank(serviceId)) {
            return null;
        }
        // mgcoa8888S0 / mkpca5530S1 → mgcoa8888 / mkpca5530
        return serviceId.replaceFirst("[SD]\\d+$", "");
    }

    private static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (!isBlank(forwarded)) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return isBlank(remote) ? "127.0.0.1" : remote;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
