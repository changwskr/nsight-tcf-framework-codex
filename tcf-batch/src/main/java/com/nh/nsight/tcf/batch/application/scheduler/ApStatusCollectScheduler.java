package com.nh.nsight.tcf.batch.application.scheduler;

import com.nh.nsight.tcf.batch.config.ApStatusBatchProperties;
import com.nh.nsight.tcf.batch.support.model.ApStatusCollectResult;
import com.nh.nsight.tcf.batch.application.service.ApStatusCollectService;
import com.nh.nsight.tcf.batch.support.ScheduledCollectSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApStatusCollectScheduler {
    private static final Logger log = LoggerFactory.getLogger(ApStatusCollectScheduler.class);

    private final ApStatusBatchProperties properties;
    private final ApStatusCollectService collectService;
    private final ScheduledCollectSupport scheduledCollectSupport;

    public ApStatusCollectScheduler(ApStatusBatchProperties properties, ApStatusCollectService collectService,
                                      ScheduledCollectSupport scheduledCollectSupport) {
        this.properties = properties;
        this.collectService = collectService;
        this.scheduledCollectSupport = scheduledCollectSupport;
    }

    @Scheduled(cron = "${nsight.batch.ap-status.cron:0 */5 * * * *}")
    public void runScheduled() {
        if (scheduledCollectSupport.skipIfUnavailable(properties.getJobId(), log)) {
            return;
        }
        log.info("Scheduled AP status collect started jobId={}", properties.getJobId());
        ApStatusCollectResult result = collectService.collectAndPersist();
        log.info("Scheduled AP status collect finished jobId={} status={} message={}",
                result.jobId(), result.runStatus(), result.message());
    }
}
