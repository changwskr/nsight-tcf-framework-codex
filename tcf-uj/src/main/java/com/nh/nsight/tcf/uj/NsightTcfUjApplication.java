package com.nh.nsight.tcf.uj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class NsightTcfUjApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(NsightTcfUjApplication.class);
    }

    public static void main(String[] args) {
        System.setProperty("server.port", "8102");
        SpringApplication.run(NsightTcfUjApplication.class, args);
    }
}
