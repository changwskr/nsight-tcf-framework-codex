package com.nh.nsight.marketing.ep.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class EpDatabaseMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(EpDatabaseMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public EpDatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS EP_USER_EVENT (
                    EVENT_ID     VARCHAR(50) PRIMARY KEY,
                    USER_ID      VARCHAR(50) NOT NULL,
                    EVENT_TYPE   VARCHAR(30) NOT NULL,
                    RECEIVED_AT  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        log.debug("EP schema migration applied.");
    }
}
