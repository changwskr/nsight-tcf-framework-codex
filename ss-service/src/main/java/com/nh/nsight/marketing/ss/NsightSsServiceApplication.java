package com.nh.nsight.marketing.ss;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.ss.persistence.mapper")
public class NsightSsServiceApplication extends NsightWarBootstrap {
    public NsightSsServiceApplication() {
        super(NsightSsServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightSsServiceApplication.class, args);
    }
}
