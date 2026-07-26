package com.nh.nsight.marketing.eb.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(EbEventPublishProperties.class)
public class EbSchedulerConfiguration {
}
