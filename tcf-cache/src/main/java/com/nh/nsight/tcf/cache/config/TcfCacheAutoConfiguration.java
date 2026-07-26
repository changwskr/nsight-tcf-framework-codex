package com.nh.nsight.tcf.cache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

@AutoConfiguration(after = CacheAutoConfiguration.class)
@EnableConfigurationProperties(TcfCacheProperties.class)
@ConditionalOnProperty(prefix = "nsight.tcf.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TcfCacheAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(TcfCacheAutoConfiguration.class);

    @Bean
    TcfCacheInitializer tcfCacheInitializer(TcfCacheProperties properties, ResourceLoader resourceLoader) {
        String location = properties.getConfigLocation();
        if (StringUtils.hasText(location)) {
            log.info("TCF EhCache config={}", location);
            resourceLoader.getResource(location);
        }
        return new TcfCacheInitializer();
    }

    static final class TcfCacheInitializer {}
}
