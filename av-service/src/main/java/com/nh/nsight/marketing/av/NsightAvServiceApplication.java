package com.nh.nsight.marketing.av;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.av.persistence.mapper")
public class NsightAvServiceApplication extends NsightWarBootstrap {
    public NsightAvServiceApplication() {
        super(NsightAvServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightAvServiceApplication.class, args);
    }
}
