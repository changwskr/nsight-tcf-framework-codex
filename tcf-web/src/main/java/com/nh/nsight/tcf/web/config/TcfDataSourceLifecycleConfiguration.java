package com.nh.nsight.tcf.web.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;

/**
 * ztomcat WAR 재배포 시 Hikari housekeeper 스레드가 classloader 종료 후에도 남지 않도록
 * 컨텍스트 종료 직후 모든 Hikari 풀을 닫는다.
 */
@AutoConfiguration
public class TcfDataSourceLifecycleConfiguration {

    @org.springframework.context.annotation.Bean
    ApplicationListener<ContextClosedEvent> hikariPoolShutdownListener() {
        return new HikariPoolShutdownListener();
    }

    static final class HikariPoolShutdownListener implements ApplicationListener<ContextClosedEvent>, Ordered {

        private static final Logger log = LoggerFactory.getLogger(HikariPoolShutdownListener.class);

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            event.getApplicationContext().getBeansOfType(HikariDataSource.class).values().forEach(ds -> {
                if (!ds.isRunning()) {
                    return;
                }
                String poolName = ds.getPoolName();
                try {
                    ds.close();
                    log.info("Hikari pool closed on context shutdown pool={}", poolName);
                } catch (Exception e) {
                    log.warn("Hikari pool close failed on context shutdown pool={} cause={}", poolName, e.getMessage());
                }
            });
        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }
    }
}
