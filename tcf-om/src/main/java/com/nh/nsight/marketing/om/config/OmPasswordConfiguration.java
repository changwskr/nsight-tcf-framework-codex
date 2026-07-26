package com.nh.nsight.marketing.om.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class OmPasswordConfiguration {

    @Bean
    PasswordEncoder omPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
