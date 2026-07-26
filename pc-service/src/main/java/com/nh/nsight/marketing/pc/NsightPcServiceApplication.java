package com.nh.nsight.marketing.pc;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.pc.persistence.mapper")
public class NsightPcServiceApplication extends NsightWarBootstrap {
    public NsightPcServiceApplication() {
        super(NsightPcServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightPcServiceApplication.class, args);
    }
}
