package com.nh.nsight.gateway.application.scheduler;

import com.nh.nsight.gateway.application.service.GatewayUserSessionSyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GatewayUserSessionSyncScheduler {
    private final GatewayUserSessionSyncService syncService;

    public GatewayUserSessionSyncScheduler(GatewayUserSessionSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(fixedRateString = "${nsight.gateway.user-session-sync.fixed-rate-ms:10000}")
    public void syncActiveSessions() {
        syncService.syncActiveSessions();
    }
}
