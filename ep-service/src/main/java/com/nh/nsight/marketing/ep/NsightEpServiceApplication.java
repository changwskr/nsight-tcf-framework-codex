package com.nh.nsight.marketing.ep;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.ep.persistence.mapper")
public class NsightEpServiceApplication extends NsightWarBootstrap {
    public NsightEpServiceApplication() {
        super(NsightEpServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightEpServiceApplication.class, args);
    }
}
