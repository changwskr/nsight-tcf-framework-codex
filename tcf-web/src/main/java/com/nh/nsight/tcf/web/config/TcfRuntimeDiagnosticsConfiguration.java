package com.nh.nsight.tcf.web.config;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.runtime.ActiveTransactionRegistry;
import com.nh.nsight.tcf.core.support.runtime.SlowSqlTracker;
import com.nh.nsight.tcf.web.support.runtime.TcfSqlMonitorInterceptor;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "nsight.tcf", name = "runtime-monitor-enabled", havingValue = "true", matchIfMissing = true)
public class TcfRuntimeDiagnosticsConfiguration {

    @Bean
    TcfSqlMonitorInterceptor tcfSqlMonitorInterceptor(
            TcfProperties properties,
            ActiveTransactionRegistry registry,
            SlowSqlTracker slowSqlTracker) {
        return new TcfSqlMonitorInterceptor(properties, registry, slowSqlTracker);
    }

    @Bean
    ConfigurationCustomizer tcfSqlMonitorCustomizer(TcfSqlMonitorInterceptor interceptor) {
        return configuration -> configuration.addInterceptor(interceptor);
    }
}
