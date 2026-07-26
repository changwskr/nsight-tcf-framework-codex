package com.nh.nsight.aimethodology;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * NSIGHT Model Studio — Spring Boot entry.
 * <p>IDE에서 JpaRepository ClassNotFound가 나면 Gradle 프로젝트 미반영입니다.
 * Run and Debug에서 {@code tcf-ai-methodology (Gradle project)} 를 쓰거나,
 * {@code ./gradlew :tcf-ai-methodology:bootRun} / Task {@code tcf-ai-methodology:bootRun} 을 사용하세요.
 */
@SpringBootApplication
public class AiMethodologyApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AiMethodologyApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AiMethodologyApplication.class, args);
    }
}
