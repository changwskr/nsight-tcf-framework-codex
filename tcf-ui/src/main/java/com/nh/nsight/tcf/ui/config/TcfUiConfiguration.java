package com.nh.nsight.tcf.ui.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TcfUiProperties.class)
public class TcfUiConfiguration {
}
