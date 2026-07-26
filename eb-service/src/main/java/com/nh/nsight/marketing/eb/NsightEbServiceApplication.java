package com.nh.nsight.marketing.eb;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.eb.persistence.mapper")
public class NsightEbServiceApplication extends NsightWarBootstrap {
    public NsightEbServiceApplication() {
        super(NsightEbServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightEbServiceApplication.class, args);
    }
}
