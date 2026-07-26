package com.nh.nsight.marketing.pd;

import org.mybatis.spring.annotation.MapperScan;
import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.pd.persistence.mapper")
public class NsightPdServiceApplication extends NsightWarBootstrap {
    public NsightPdServiceApplication() {
        super(NsightPdServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightPdServiceApplication.class, args);
    }
}
