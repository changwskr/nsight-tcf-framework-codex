package com.nh.nsight.auth.jwt.support;

import com.nh.nsight.auth.jwt.config.JwtInternalCallProperties;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.util.security.NsightInternalCallSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class JwtInternalCallValidator {
    private final JwtInternalCallProperties properties;

    public JwtInternalCallValidator(JwtInternalCallProperties properties) {
        this.properties = properties;
    }

    public void validate(java.util.Map<String, Object> body) {
        HttpServletRequest request = currentRequest()
                .orElseThrow(() -> new BusinessException("E-JWT-INT-0001", "내부 호출 HTTP 컨텍스트가 없습니다."));

        if (!"true".equalsIgnoreCase(request.getHeader(NsightInternalCallSupport.HEADER_INTERNAL_CALL))) {
            throw new BusinessException("E-JWT-INT-0001", "내부 호출이 아닙니다.");
        }
        String service = request.getHeader(NsightInternalCallSupport.HEADER_INTERNAL_SERVICE);
        if (!properties.getAllowedService().equalsIgnoreCase(service == null ? null : service.strip())) {
            throw new BusinessException("E-JWT-INT-0001", "허용되지 않은 내부 서비스입니다.");
        }

        String timestampHeader = request.getHeader(NsightInternalCallSupport.HEADER_INTERNAL_TIMESTAMP);
        if (!StringUtils.hasText(timestampHeader)) {
            throw new BusinessException("E-JWT-INT-0001", "내부 호출 timestamp가 없습니다.");
        }
        long timestampMillis;
        try {
            timestampMillis = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("E-JWT-INT-0001", "내부 호출 timestamp 형식이 올바르지 않습니다.");
        }
        long skewMillis = Duration.ofSeconds(properties.getTimestampSkewSeconds()).toMillis();
        if (Math.abs(Instant.now().toEpochMilli() - timestampMillis) > skewMillis) {
            throw new BusinessException("E-JWT-INT-0001", "내부 호출 timestamp가 만료되었습니다.");
        }

        String signature = request.getHeader(NsightInternalCallSupport.HEADER_INTERNAL_SIGNATURE);
        String canonical = NsightInternalCallSupport.canonicalSsoIssueBody(body);
        if (!NsightInternalCallSupport.verify(canonical, timestampMillis, signature,
                properties.getSharedSecret())) {
            throw new BusinessException("E-JWT-INT-0001", "내부 호출 서명이 올바르지 않습니다.");
        }

        validateClientIp(request.getRemoteAddr(), properties.getAllowedIps());
    }

    private void validateClientIp(String remoteAddr, List<String> allowedIps) {
        if (allowedIps == null || allowedIps.isEmpty()) {
            return;
        }
        String clientIp = remoteAddr == null ? "" : remoteAddr.trim();
        for (String allowed : allowedIps) {
            if (allowed != null && allowed.equalsIgnoreCase(clientIp)) {
                return;
            }
        }
        throw new BusinessException("E-JWT-INT-0001", "허용되지 않은 내부 호출 IP입니다.");
    }

    private java.util.Optional<HttpServletRequest> currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(attrs.getRequest());
    }
}
