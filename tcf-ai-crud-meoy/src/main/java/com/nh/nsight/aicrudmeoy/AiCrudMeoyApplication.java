package com.nh.nsight.aicrudmeoy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * NSIGHT CRUD Meoy — C-MASTER / C00~C18 절차 위저드.
 * <p>IDE에서 {@code SpringApplication cannot be resolved} / {@code jdt.ls-java-project} classpath면
 * Gradle 프로젝트 미반영입니다. {@code ./gradlew :tcf-ai-crud-meoy:bootRun}, {@code run.bat},
 * 또는 Run and Debug의 {@code tcf-ai-crud-meoy (Gradle bootRun)} 을 사용하세요.
 * <p>UI: http://127.0.0.1:8788
 */
@SpringBootApplication
public class AiCrudMeoyApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AiCrudMeoyApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AiCrudMeoyApplication.class, args);
    }
}
