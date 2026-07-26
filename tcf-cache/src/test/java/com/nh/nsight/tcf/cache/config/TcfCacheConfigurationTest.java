package com.nh.nsight.tcf.cache.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TcfCacheConfigurationTest {

    @Test
    void defaultProperties() {
        TcfCacheProperties properties = new TcfCacheProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getConfigLocation()).isEqualTo("classpath:ehcache.xml");
    }

    @Test
    void loadsEhCacheXmlFromClasspath() {
        assertThat(getClass().getClassLoader().getResource("ehcache.xml")).isNotNull();
    }
}
