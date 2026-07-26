package com.nh.nsight.marketing.ms;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.ms.persistence.mapper")
public class NsightMsServiceApplication extends NsightWarBootstrap {
    public NsightMsServiceApplication() {
        super(NsightMsServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightMsServiceApplication.class, args);
    }
}
