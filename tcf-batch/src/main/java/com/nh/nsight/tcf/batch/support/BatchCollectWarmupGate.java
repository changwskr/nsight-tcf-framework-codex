package com.nh.nsight.tcf.batch.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tomcat 순차 WAR 배포 중 cron 수집이 선행 실행되는 것을 막습니다.
 * {@code nsight.batch.startup-collect.initial-delay-ms}와 동일 시점까지 스케줄을 보류합니다.
 */
@Component
public class BatchCollectWarmupGate {
    private final long readyAtEpochMs;

    public BatchCollectWarmupGate(
            @Value("${nsight.batch.startup-collect.initial-delay-ms:0}") long initialDelayMs) {
        this.readyAtEpochMs = System.currentTimeMillis() + Math.max(0, initialDelayMs);
    }

    public boolean isReady() {
        return System.currentTimeMillis() >= readyAtEpochMs;
    }

    public long remainingMs() {
        return Math.max(0, readyAtEpochMs - System.currentTimeMillis());
    }
}
