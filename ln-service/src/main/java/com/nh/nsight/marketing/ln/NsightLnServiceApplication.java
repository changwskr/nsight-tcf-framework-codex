package com.nh.nsight.marketing.ln;

import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.ln.persistence.mapper")
public class NsightLnServiceApplication extends NsightWarBootstrap {
    public NsightLnServiceApplication() {
        super(NsightLnServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightLnServiceApplication.class, args);
    }
}
