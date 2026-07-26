package com.nh.nsight.tcf.batch.support;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ScheduledCollectSupport {
    private final BatchCollectWarmupGate warmupGate;
    private final BatchContextShutdownGate shutdownGate;

    public ScheduledCollectSupport(BatchCollectWarmupGate warmupGate, BatchContextShutdownGate shutdownGate) {
        this.warmupGate = warmupGate;
        this.shutdownGate = shutdownGate;
    }

    /** warmup 또는 context 종료 중이면 스케줄 실행을 건너뛴다. */
    public boolean skipIfUnavailable(String jobId, Logger log) {
        if (shutdownGate.isShuttingDown()) {
            log.debug("Skipping scheduled collect jobId={} — batch context shutting down", jobId);
            return true;
        }
        return skipIfWarmingUp(jobId, log);
    }

    public boolean skipIfWarmingUp(String jobId, Logger log) {
        if (warmupGate.isReady()) {
            return false;
        }
        log.info("Skipping scheduled collect jobId={} — Tomcat WAR warmup ({} ms remaining)",
                jobId, warmupGate.remainingMs());
        return true;
    }
}
