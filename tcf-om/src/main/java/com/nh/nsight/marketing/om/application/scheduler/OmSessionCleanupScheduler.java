package com.nh.nsight.marketing.om.application.scheduler;

import com.nh.nsight.marketing.om.application.service.OmSessionCleanupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OmSessionCleanupScheduler {
    private final OmSessionCleanupService sessionCleanupService;

    public OmSessionCleanupScheduler(OmSessionCleanupService sessionCleanupService) {
        this.sessionCleanupService = sessionCleanupService;
    }

    @Scheduled(fixedRateString = "${nsight.om.session-cleanup.fixed-rate-ms:10000}")
    public void cleanupExpiredSessions() {
        sessionCleanupService.runScheduled();
    }
}
