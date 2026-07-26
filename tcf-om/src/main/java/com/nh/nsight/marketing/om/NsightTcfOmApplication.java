package com.nh.nsight.marketing.om;

import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.om.persistence.mapper")
public class NsightTcfOmApplication extends NsightWarBootstrap {
    public NsightTcfOmApplication() {
        super(NsightTcfOmApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightTcfOmApplication.class, args);
    }
}
