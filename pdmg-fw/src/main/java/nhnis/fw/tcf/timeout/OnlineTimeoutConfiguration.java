package nhnis.fw.tcf.timeout;

import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.annotation.PreDestroy;

/**
 * 온라인 타임아웃 Executor 구성.
 *
 * <p>{@code nhnis.fw.timeout.enabled=true} 일 때 Worker Pool + TransactionTemplate 경로를 활성한다.
 * TransactionManager는 업무 애플리케이션의 {@code rdwTransactionManager} 를 주입받는다.
 */
@Configuration
@EnableConfigurationProperties(OnlineTimeoutProperties.class)
public class OnlineTimeoutConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OnlineTimeoutConfiguration.class);

    private ThreadPoolTaskExecutor onlineTaskExecutor;

    @Bean
    @ConditionalOnProperty(name = "nhnis.fw.timeout.enabled", havingValue = "false", matchIfMissing = true)
    public OnlineTimeoutExecutor syncOnlineTimeoutExecutor() {
        log.info("[ONLINE-TIMEOUT] disabled — SyncOnlineTimeoutExecutor");
        return new SyncOnlineTimeoutExecutor();
    }

    @Bean(name = "pdmgOnlineTimeoutTaskExecutor")
    @ConditionalOnProperty(name = "nhnis.fw.timeout.enabled", havingValue = "true")
    public ThreadPoolTaskExecutor pdmgOnlineTimeoutTaskExecutor(OnlineTimeoutProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("pdmg-online-");
        executor.setCorePoolSize(properties.getPoolSize());
        executor.setMaxPoolSize(properties.getPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        this.onlineTaskExecutor = executor;
        log.info("[ONLINE-TIMEOUT] pool ready poolSize={} queueCapacity={} timeoutMs={}",
                properties.getPoolSize(), properties.getQueueCapacity(), properties.getMilliseconds());
        return executor;
    }

    @Bean
    @ConditionalOnProperty(name = "nhnis.fw.timeout.enabled", havingValue = "true")
    public OnlineTimeoutExecutor defaultOnlineTimeoutExecutor(
            OnlineTimeoutProperties properties,
            @Qualifier("pdmgOnlineTimeoutTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
            @Qualifier("rdwTransactionManager") PlatformTransactionManager transactionManager) {
        return new DefaultOnlineTimeoutExecutor(properties, taskExecutor, transactionManager);
    }

    @PreDestroy
    void shutdown() {
        if (onlineTaskExecutor != null) {
            onlineTaskExecutor.shutdown();
        }
    }
}
