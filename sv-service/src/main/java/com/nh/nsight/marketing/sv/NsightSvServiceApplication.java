package com.nh.nsight.marketing.sv;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.sv.persistence.mapper")
public class NsightSvServiceApplication extends NsightWarBootstrap {
    public NsightSvServiceApplication() {
        super(NsightSvServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightSvServiceApplication.class, args);
    }
}
