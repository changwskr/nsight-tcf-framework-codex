package com.nh.nsight.tcf.batch.application.scheduler;

import com.nh.nsight.tcf.batch.config.DbStatusBatchProperties;
import com.nh.nsight.tcf.batch.support.model.DbStatusCollectResult;
import com.nh.nsight.tcf.batch.application.service.DbStatusCollectService;
import com.nh.nsight.tcf.batch.support.ScheduledCollectSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DbStatusCollectScheduler {
    private static final Logger log = LoggerFactory.getLogger(DbStatusCollectScheduler.class);

    private final DbStatusBatchProperties properties;
    private final DbStatusCollectService collectService;
    private final ScheduledCollectSupport scheduledCollectSupport;

    public DbStatusCollectScheduler(DbStatusBatchProperties properties, DbStatusCollectService collectService,
                                    ScheduledCollectSupport scheduledCollectSupport) {
        this.properties = properties;
        this.collectService = collectService;
        this.scheduledCollectSupport = scheduledCollectSupport;
    }

    @Scheduled(cron = "${nsight.batch.db-status.cron:30 */5 * * * *}")
    public void runScheduled() {
        if (scheduledCollectSupport.skipIfUnavailable(properties.getJobId(), log)) {
            return;
        }
        log.info("Scheduled DB status collect started jobId={}", properties.getJobId());
        DbStatusCollectResult result = collectService.collectAndPersist();
        log.info("Scheduled DB status collect finished jobId={} status={} message={}",
                result.jobId(), result.runStatus(), result.message());
    }
}
