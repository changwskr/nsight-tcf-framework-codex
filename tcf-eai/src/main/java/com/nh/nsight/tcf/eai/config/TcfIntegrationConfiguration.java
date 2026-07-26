package com.nh.nsight.tcf.eai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.nsight.tcf.eai.client.DefaultTcfServiceClient;
import com.nh.nsight.tcf.eai.client.TcfServiceClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * tcf-eai 연동 모듈 빈 구성.
 *
 * <p>업무 서비스 애플리케이션은 {@code scanBasePackages = "com.nh.nsight"} 로 이 구성을 자동 스캔한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TcfIntegrationProperties.class)
public class TcfIntegrationConfiguration {

    @Bean
    public TcfServiceClient tcfServiceClient(TcfIntegrationProperties properties, ObjectMapper objectMapper) {
        return new DefaultTcfServiceClient(properties, objectMapper);
    }
}
