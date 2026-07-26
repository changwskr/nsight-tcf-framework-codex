package com.nh.nsight.marketing.om;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.om.persistence.mapper")
public class NsightOmServiceApplication extends NsightWarBootstrap {
    public NsightOmServiceApplication() {
        super(NsightOmServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightOmServiceApplication.class, args);
    }
}
