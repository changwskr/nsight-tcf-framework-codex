package com.nh.nsight.tcf.batch.application.scheduler;

import com.nh.nsight.tcf.batch.config.SessionStatusBatchProperties;
import com.nh.nsight.tcf.batch.support.model.SessionStatusCollectResult;
import com.nh.nsight.tcf.batch.application.service.SessionStatusCollectService;
import com.nh.nsight.tcf.batch.support.ScheduledCollectSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionStatusCollectScheduler {
    private static final Logger log = LoggerFactory.getLogger(SessionStatusCollectScheduler.class);

    private final SessionStatusBatchProperties properties;
    private final SessionStatusCollectService collectService;
    private final ScheduledCollectSupport scheduledCollectSupport;

    public SessionStatusCollectScheduler(SessionStatusBatchProperties properties,
                                         SessionStatusCollectService collectService,
                                         ScheduledCollectSupport scheduledCollectSupport) {
        this.properties = properties;
        this.collectService = collectService;
        this.scheduledCollectSupport = scheduledCollectSupport;
    }

    @Scheduled(cron = "${nsight.batch.session-status.cron:45 */5 * * * *}")
    public void runScheduled() {
        if (scheduledCollectSupport.skipIfUnavailable(properties.getJobId(), log)) {
            return;
        }
        log.info("Scheduled session status collect started jobId={}", properties.getJobId());
        SessionStatusCollectResult result = collectService.collectAndPersist();
        log.info("Scheduled session status collect finished jobId={} status={} message={}",
                result.jobId(), result.runStatus(), result.message());
    }
}
