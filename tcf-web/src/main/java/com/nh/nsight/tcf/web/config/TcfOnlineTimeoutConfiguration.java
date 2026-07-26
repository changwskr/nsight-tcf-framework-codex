package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.web.support.PolicyDrivenQueryTimeoutInterceptor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "nsight.tcf", name = "timeout-policy-enabled", havingValue = "true", matchIfMissing = true)
public class TcfOnlineTimeoutConfiguration {

    @Bean(name = "onlineTransactionTimeoutThreadPool", destroyMethod = "shutdown")
    public ExecutorService onlineTransactionTimeoutThreadPool() {
        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("tcf-online-timeout-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newCachedThreadPool(threadFactory);
    }

    @Bean
    PolicyDrivenQueryTimeoutInterceptor policyDrivenQueryTimeoutInterceptor(TcfProperties properties) {
        return new PolicyDrivenQueryTimeoutInterceptor(properties);
    }

    @Bean
    ConfigurationCustomizer policyDrivenQueryTimeoutCustomizer(
            PolicyDrivenQueryTimeoutInterceptor interceptor) {
        return configuration -> configuration.addInterceptor(interceptor);
    }
}
