package com.nh.nsight.tcf.batch.support;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/** Tomcat WAR 재배포·종료 시 스케줄 수집이 닫힌 DataSource에 접근하지 않도록 한다. */
@Component
public class BatchContextShutdownGate implements ApplicationListener<ContextClosedEvent> {
    private volatile boolean shuttingDown;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        shuttingDown = true;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }
}
