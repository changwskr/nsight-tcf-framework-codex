package com.nh.nsight.marketing.mg;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.mg.persistence.mapper")
public class NsightMgServiceApplication extends NsightWarBootstrap {
    public NsightMgServiceApplication() {
        super(NsightMgServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightMgServiceApplication.class, args);
    }
}
