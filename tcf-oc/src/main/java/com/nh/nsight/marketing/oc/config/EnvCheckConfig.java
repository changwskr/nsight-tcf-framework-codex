package com.nh.nsight.marketing.oc.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EnvCheckProperties.class)
public class EnvCheckConfig {
}
