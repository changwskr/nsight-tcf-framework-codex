package com.nh.nsight.tcf.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class NsightTcfUiApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(NsightTcfUiApplication.class);
    }

    public static void main(String[] args) {
        if (System.getProperty("server.port") == null) {
            System.setProperty("server.port", "8099");
        }
        SpringApplication.run(NsightTcfUiApplication.class, args);
    }
}
