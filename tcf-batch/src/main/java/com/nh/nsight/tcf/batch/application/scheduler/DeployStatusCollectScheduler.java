package com.nh.nsight.tcf.batch.application.scheduler;

import com.nh.nsight.tcf.batch.config.DeployStatusBatchProperties;
import com.nh.nsight.tcf.batch.support.model.DeployStatusCollectResult;
import com.nh.nsight.tcf.batch.application.service.DeployStatusCollectService;
import com.nh.nsight.tcf.batch.support.ScheduledCollectSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeployStatusCollectScheduler {
    private static final Logger log = LoggerFactory.getLogger(DeployStatusCollectScheduler.class);

    private final DeployStatusBatchProperties properties;
    private final DeployStatusCollectService collectService;
    private final ScheduledCollectSupport scheduledCollectSupport;

    public DeployStatusCollectScheduler(DeployStatusBatchProperties properties,
                                        DeployStatusCollectService collectService,
                                        ScheduledCollectSupport scheduledCollectSupport) {
        this.properties = properties;
        this.collectService = collectService;
        this.scheduledCollectSupport = scheduledCollectSupport;
    }

    @Scheduled(cron = "${nsight.batch.deploy-status.cron:55 */5 * * * *}")
    public void runScheduled() {
        if (scheduledCollectSupport.skipIfUnavailable(properties.getJobId(), log)) {
            return;
        }
        log.info("Scheduled deploy status collect started jobId={}", properties.getJobId());
        DeployStatusCollectResult result = collectService.collectAndPersist();
        log.info("Scheduled deploy status collect finished jobId={} status={} message={}",
                result.jobId(), result.runStatus(), result.message());
    }
}
