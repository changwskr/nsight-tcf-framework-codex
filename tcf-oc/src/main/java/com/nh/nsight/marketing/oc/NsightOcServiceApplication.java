package com.nh.nsight.marketing.oc;

import com.nh.nsight.tcf.web.support.NsightWarBootstrap;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan({
        "com.nh.nsight.marketing.oc.persistence.mapper",
        "com.nh.nsight.marketing.oc.capnew.persistence.mapper"
})
public class NsightOcServiceApplication extends NsightWarBootstrap {
    public NsightOcServiceApplication() {
        super(NsightOcServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(NsightOcServiceApplication.class, args);
    }
}
