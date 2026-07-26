package com.nh.nsight.marketing.om.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OmSsoProperties.class)
public class OmSsoConfiguration {
}
