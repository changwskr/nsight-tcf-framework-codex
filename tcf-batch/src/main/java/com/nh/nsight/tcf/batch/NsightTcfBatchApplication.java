package com.nh.nsight.tcf.batch;

import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class NsightTcfBatchApplication extends NsightWarBootstrap {
    public NsightTcfBatchApplication() {
        super(NsightTcfBatchApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightTcfBatchApplication.class, args);
    }
}
