/***************************************************************************
 * Copyright 2026 by Nonghyup. All rights reserved. Nonghyup 의 사전 승인 없이
 * 본 내용의 전부 또는 일부에 대한 복사, 배포, 사용을 금합니다. Nonghyup의 사전 승인
 * 없이 소스코드를 변경하여 사용하는 경우 소스코드에 대한 품질과 성능을 보장하지 않습니다.
 *
 * If you modify this source without Nonghyup’s approval. Nonghyup does
 * not guarantee the quality and performance of source.
 ***************************************************************************/
/***************************************************************************
 * 이 력 사 항
 ***************************************************************************
 * 일 자 버 전 작성자 변경사항
 ***************************************************************************
 * 2026. 7. 3. 1.0 CS549257 최초작성
 ***************************************************************************/
package nhnis.fw.commons.interceptor;

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
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.exception.NhBaseException;

/**
 * <PRE>
 * 시스템 선/후처리 Interceptor
 * </PRE>
 *
 * @author CS549257
 * @version 1.0, 2026. 7. 3.
 * @logicalName
 */
@Component
public class ServicePreventationInterceptor implements HandlerInterceptor {

    private Logger LOGGER = LoggerFactory.getLogger(ServicePreventationInterceptor.class);

    private static final String MULTI_PART = "multipart";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[ServicePreventationInterceptor] SystemPreProcessor Start!");
        }

        if (request.getContentType().startsWith(MULTI_PART)) return true;
        if (ServiceContextHolder.getInstance() == null) {
            LOGGER.error("[ServicePreventationInterceptor] Service Context is null...!!");
            return false;
        } else {
            hdr_nhnis header = ServiceContextHolder.getInstance().getHeader();
            String guid = header.getSys_comm().getStd_gbl_id();

            // 1. GUID 체크
            if (guid == null) {
                // generate GUID
            } else {
                LOGGER.info("[ServicePreventationInterceptor] GUID: {}", guid);
            }

            return true;
        }

        // 2. Pre ImageLog

        // 3. 거래제어
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[ServicePreventationInterceptor] SystemPostProcessor");
        }

        if (request.getContentType().startsWith(MULTI_PART)) {
            // 파일 업로드
        } else {
            // 파일 다운로드
        }
        // Post ImageLog

        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) throws Exception {
        if (ex != null) {
            LOGGER.error("[ServicePreventationInterceptor] SystemErrorProcessor");

            // Exception ImageLog

            ServiceContextHolder.removeInstance();
            throw ex;
        }
        HandlerInterceptor.super.afterCompletion(request, response, handler, (NhBaseException)ex);
    }

    public <T> T convertMapToDto(Object input, Class<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return (T)mapper.convertValue(input, type);
    }
}
