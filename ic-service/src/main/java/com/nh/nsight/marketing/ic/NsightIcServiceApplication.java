package com.nh.nsight.marketing.ic;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.ic.persistence.mapper")
public class NsightIcServiceApplication extends NsightWarBootstrap {
    public NsightIcServiceApplication() {
        super(NsightIcServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightIcServiceApplication.class, args);
    }
}
