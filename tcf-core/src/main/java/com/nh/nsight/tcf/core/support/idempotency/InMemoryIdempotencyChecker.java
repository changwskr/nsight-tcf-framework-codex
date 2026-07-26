package com.nh.nsight.tcf.core.support.idempotency;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InMemoryIdempotencyChecker implements IdempotencyChecker {
    private final TcfProperties properties;
    private final Map<String, RequestState> states = new ConcurrentHashMap<>();

    public InMemoryIdempotencyChecker(TcfProperties properties) {
        this.properties = properties;
    }

    @Override
    public void checkAndMarkProcessing(StandardHeader header) {
        if (!properties.isIdempotencyEnabled()) {
            return;
        }
        String key = buildKey(header);
        if (!StringUtils.hasText(key)) {
            return;
        }
        RequestState newState = new RequestState("PROCESSING", Instant.now());
        RequestState old = states.putIfAbsent(key, newState);
        if (old != null && "PROCESSING".equals(old.status())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST, "동일 요청이 처리 중입니다.");
        }
    }

    @Override
    public void markSuccess(StandardHeader header) {
        update(header, "SUCCESS");
    }

    @Override
    public void markFail(StandardHeader header) {
        update(header, "FAIL");
    }

    private void update(StandardHeader header, String status) {
        String key = buildKey(header);
        if (StringUtils.hasText(key)) {
            states.put(key, new RequestState(status, Instant.now()));
        }
    }

    private String buildKey(StandardHeader header) {
        if (header == null) {
            return null;
        }
        if (StringUtils.hasText(header.getIdempotencyKey())) {
            return header.getIdempotencyKey();
        }
        return header.getGuid();
    }

    private record RequestState(String status, Instant updatedAt) {}
}
