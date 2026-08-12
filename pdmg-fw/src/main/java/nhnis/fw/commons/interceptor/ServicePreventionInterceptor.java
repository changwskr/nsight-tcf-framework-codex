/***************************************************************************
 * Copyright 2026 by Nonghyup. All rights reserved. Nonghyup 의 사전 승인 없이
 * 본 내용의 전부 또는 일부에 대한 복사, 배포, 사용을 금합니다. Nonghyup의 사전 승인
 * 없이 소스코드를 변경하여 사용하는 경우 소스코드에 대한 품질과 성능을 보장하지 않습니다.
 *
 * If you modify this source without Nonghyup’s approval. Nonghyup does
 * not guarantee the quality and performance of source.
 ***************************************************************************/
package nhnis.fw.commons.interceptor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.fw.commons.filter.CachedBodyHttpServletRequest;
import nhnis.fw.commons.imagelog.ImageLogHandler;
import nhnis.fw.commons.log.PdmgMessagePrinter;
import nhnis.fw.commons.log.PdmgTxFlowLog;
import nhnis.fw.commons.log.PdmgTxLog;

/**
 * 시스템 선/후처리 Interceptor.
 *
 * <p>{@code log.info} 는 이 클래스에서 직접 호출한다. 그래야 운영 로그의
 * {@code [ServicePreventionInterceptor.postHandle]} 위치가 유지된다.
 *
 * <p>전문 헤더 이미지로그는 {@link ImageLogHandler} 로 DB에 남긴다.
 * GUID({@code std_gbl_id})가 없으면 선처리에서 강제 채번한다.
 */
@Component
public class ServicePreventionInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicePreventionInterceptor.class);
    private static final String MULTI_PART = "multipart";
    private static final String GUID = "guid";

    private final ImageLogHandler imageLogHandler;

    public ServicePreventionInterceptor(ImageLogHandler imageLogHandler) {
        this.imageLogHandler = imageLogHandler;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        PdmgTxFlowLog.enter(LOGGER, ServicePreventionInterceptor.class, "preHandle");
        try {
            return preHandleInternal(request, response, handler);
        } finally {
            PdmgTxFlowLog.leave(LOGGER, ServicePreventionInterceptor.class, "preHandle");
        }
    }

    private boolean preHandleInternal(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 거래 구분 라인피드 + 시스템 선처리 시작
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(PdmgTxLog.txGap() + PdmgTxLog.systemPreStart());
        }

        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith(MULTI_PART)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(PdmgTxLog.systemPreEnd());
            }
            return true;
        }

        ServiceContext ctx = ServiceContextHolder.getInstance();
        if (ctx == null) {
            LOGGER.warn(PdmgTxLog.systemContextNull());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(PdmgTxLog.systemPreEnd());
            }
            return true;
        }

        hdr_nhnis header = ensureHeader(ctx);
        String guid = ensureGuid(ctx, header);
        enrichHeaderFromRequest(request, header);
        LOGGER.info(PdmgTxLog.systemGuid(guid));

        // 요청 온라인 전문 — 시스템 선처리 구간에서 반드시 출력 (종료 전)
        String requestRaw = resolveRequestBodyRaw(ctx, request);
        logRequestMessage(requestRaw);

        // Pre ImageLog — 시스템 전문 헤더 + 요청 전문
        imageLogHandler.preImagelog(header, requestRaw);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(PdmgTxLog.systemPreEnd());
        }
        return true;
    }

    /** 클라이언트 요청 온라인 전문을 CORE 로그에 남긴다. */
    private void logRequestMessage(String raw) {
        try {
            LOGGER.info(PdmgTxLog.onlineRequestAsIs());
            LOGGER.info("[요청전문] {}", raw);
        } catch (Exception e) {
            LOGGER.warn("[요청전문] 출력 실패: {}", e.toString());
        }
    }

    /** Filter/CachedBody/request attribute 에서 요청 전문 원문을 찾는다. */
    private static String resolveRequestBodyRaw(ServiceContext ctx, HttpServletRequest request) {
        String raw = ctx != null ? ctx.getRequestBody() : null;

        if ((raw == null || raw.isBlank()) && request != null) {
            Object attr = request.getAttribute(nhnis.fw.commons.filter.DefaultFilter.REQUEST_BODY_ATTR);
            if (attr instanceof String s && !s.isBlank()) {
                raw = s;
            }
        }

        if (raw == null || raw.isBlank()) {
            HttpServletRequest current = request;
            while (current != null) {
                if (current instanceof CachedBodyHttpServletRequest cached) {
                    raw = new String(cached.getCachedBody(), StandardCharsets.UTF_8);
                    break;
                }
                if (current instanceof jakarta.servlet.http.HttpServletRequestWrapper wrapper) {
                    jakarta.servlet.ServletRequest inner = wrapper.getRequest();
                    current = inner instanceof HttpServletRequest http ? http : null;
                } else {
                    break;
                }
            }
        }

        if (raw == null || raw.isBlank()) {
            return "(empty-request-body)";
        }
        return PdmgMessagePrinter.asIs(raw);
    }

    /**
     * 이미지로그에 남을 서비스/화면/사용자/IP 가 비어 있으면 요청에서 채운다.
     * (local 합성 헤더·부분 헤더 요청 대응)
     */
    private void enrichHeaderFromRequest(HttpServletRequest request, hdr_nhnis header) {
        sys_comm sys = header.getSys_comm();
        String pathService = extractServiceId(request);

        if (isBlank(sys.getRms_svc_c()) && !isBlank(pathService)) {
            sys.setRms_svc_c(pathService);
        }
        if (isBlank(sys.getScid())) {
            String svc = sys.getRms_svc_c();
            if (!isBlank(svc)) {
                sys.setScid(svc.replaceFirst("[SD]\\d+$", ""));
            }
        }
        if (isBlank(sys.getTr_trm_ipadr())) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (!isBlank(forwarded)) {
                int comma = forwarded.indexOf(',');
                sys.setTr_trm_ipadr((comma > 0 ? forwarded.substring(0, comma) : forwarded).trim());
            } else if (!isBlank(request.getRemoteAddr())) {
                sys.setTr_trm_ipadr(request.getRemoteAddr());
            }
        }
        if (isBlank(sys.getOptr_eno())) {
            Object mdcUser = ThreadContext.get("userId");
            if (mdcUser != null && !String.valueOf(mdcUser).isBlank()) {
                sys.setOptr_eno(String.valueOf(mdcUser));
            } else {
                sys.setOptr_eno("LOCAL");
            }
        }
    }

    private static String extractServiceId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return null;
        }
        int slash = uri.lastIndexOf('/');
        return slash >= 0 ? uri.substring(slash + 1) : uri;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** 헤더/sys_comm 이 없으면 생성한다. */
    private hdr_nhnis ensureHeader(ServiceContext ctx) {
        hdr_nhnis header = ctx.getHeader();
        if (header == null) {
            header = new hdr_nhnis();
            ctx.setHeader(header);
        }
        if (header.getSys_comm() == null) {
            header.setSys_comm(new sys_comm());
        }
        return header;
    }

    /**
     * GUID가 없거나 공백이면 강제 채번해 헤더·컨텍스트·MDC에 반영한다.
     */
    private String ensureGuid(ServiceContext ctx, hdr_nhnis header) {
        sys_comm sys = header.getSys_comm();
        String guid = sys.getStd_gbl_id();
        if (guid == null || guid.isBlank()) {
            guid = UUID.randomUUID().toString().replace("-", "");
            sys.setStd_gbl_id(guid);
            ctx.setGuid(guid);
            ThreadContext.put(GUID, guid);
            LOGGER.info("[ServicePreventionInterceptor] GUID generated in SystemPreProcessor: {}", guid);
        } else {
            ctx.setGuid(guid);
            ThreadContext.put(GUID, guid);
        }
        return guid;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
        PdmgTxFlowLog.enter(LOGGER, ServicePreventionInterceptor.class, "postHandle");
        try {
            // 시스템 후처리 시작/종료·응답 전문은 ResponseBodyAdvice 에서 출력한다.
            // (@ResponseBody 는 postHandle 보다 먼저 응답을 쓰므로 순서를 맞추기 위함)
            HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
        } finally {
            PdmgTxFlowLog.leave(LOGGER, ServicePreventionInterceptor.class, "postHandle");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) throws Exception {
        PdmgTxFlowLog.enter(LOGGER, ServicePreventionInterceptor.class, "afterCompletion");
        try {
            if (ex != null) {
                LOGGER.error(PdmgTxLog.systemErrorProcessor());
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(PdmgTxLog.systemPostStart());
                    LOGGER.info(PdmgTxLog.systemPostEnd());
                }
                ServiceContext ctx = ServiceContextHolder.getInstance();
                hdr_nhnis header = ctx != null ? ctx.getHeader() : null;
                String responseBody = ctx != null ? ctx.getResponseBody() : null;
                // Exception ImageLog — 동일 TB_FW_IMAGE_LOG
                imageLogHandler.exceptionImagelog(header, responseBody, ex);
                ServiceContextHolder.removeInstance();
                throw ex;
            }

            // 응답 후처리: 성공/오류(result) 모두 동일 이미지로그 테이블에 기록
            String contentType = request.getContentType();
            if (contentType == null || !contentType.startsWith(MULTI_PART)) {
                ServiceContext ctx = ServiceContextHolder.getInstance();
                if (ctx != null) {
                    imageLogHandler.postImagelog(ctx.getHeader(), ctx.getResponseBody());
                }
            }
            HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
        } finally {
            PdmgTxFlowLog.leave(LOGGER, ServicePreventionInterceptor.class, "afterCompletion");
        }
    }

    public <T> T convertMapToDto(Object input, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.convertValue(input, type);
    }
}
