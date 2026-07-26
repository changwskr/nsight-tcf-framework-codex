package com.nh.nsight.tcf.web.support;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 외부 Tomcat WAR 배포 시 Spring Boot 컨텍스트를 기동합니다.
 */
public abstract class NsightWarBootstrap extends SpringBootServletInitializer {
    private final Class<?> source;

    protected NsightWarBootstrap(Class<?> source) {
        this.source = source;
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(source);
    }
}
