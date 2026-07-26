package com.nh.nsight.tcf.cache.config;

import com.nh.nsight.tcf.cache.support.TcfCacheSupport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {CacheAutoConfiguration.class, TcfCacheAutoConfiguration.class})
@ConditionalOnProperty(prefix = "nsight.tcf.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(CacheManager.class)
@EnableCaching
public class TcfCacheSupportAutoConfiguration {

    @Bean
    TcfCacheSupport tcfCacheSupport(CacheManager cacheManager) {
        if (!(cacheManager instanceof JCacheCacheManager jcache)) {
            throw new IllegalStateException(
                    "TCF cache requires JCacheCacheManager but got: " + cacheManager.getClass().getName());
        }
        return new TcfCacheSupport(jcache);
    }
}
