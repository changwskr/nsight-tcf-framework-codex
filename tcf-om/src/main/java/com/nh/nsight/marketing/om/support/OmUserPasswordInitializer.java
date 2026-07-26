package com.nh.nsight.marketing.om.support;

import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class OmUserPasswordInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(OmUserPasswordInitializer.class);
    private static final String DEV_INITIAL_PASSWORD = "nsight01!";

    private final OmOperationDao dao;
    private final PasswordEncoder passwordEncoder;

    public OmUserPasswordInitializer(OmOperationDao dao, PasswordEncoder passwordEncoder) {
        this.dao = dao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> users = dao.selectUsersWithoutPasswordHash();
        if (users.isEmpty()) {
            return;
        }
        String encoded = passwordEncoder.encode(DEV_INITIAL_PASSWORD);
        for (Map<String, Object> user : users) {
            Map<String, Object> row = new HashMap<>();
            row.put("userId", user.get("userId"));
            row.put("passwordHash", encoded);
            dao.updateUserPasswordHash(row);
            log.info("Initialized OM user password hash. userId={}", user.get("userId"));
        }
    }
}
