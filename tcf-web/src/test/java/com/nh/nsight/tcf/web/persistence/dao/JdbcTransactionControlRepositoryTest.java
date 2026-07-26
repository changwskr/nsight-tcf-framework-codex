package com.nh.nsight.tcf.web.persistence.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.control.TransactionControlHeader;
import com.nh.nsight.tcf.core.support.control.TransactionControlRule;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcTransactionControlRepositoryTest {

    private JdbcTransactionControlRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:txctrl;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE TCF_TRANSACTION_CONTROL (
                    SERVICE_ID VARCHAR(100) NOT NULL,
                    TRANSACTION_CODE VARCHAR(50) NOT NULL,
                    BUSINESS_CODE VARCHAR(10) NOT NULL,
                    SERVICE_NAME VARCHAR(200) NOT NULL,
                    USER_ID VARCHAR(50) NOT NULL,
                    CHANNEL_ID VARCHAR(30) NOT NULL,
                    BRANCH_ID VARCHAR(30) NOT NULL,
                    CONTROL_TYPE VARCHAR(20) NOT NULL,
                    BLOCK_YN CHAR(1) NOT NULL,
                    PRIMARY KEY (
                        SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE, SERVICE_NAME,
                        USER_ID, CHANNEL_ID, BRANCH_ID
                    )
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO TCF_TRANSACTION_CONTROL (
                    SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE, SERVICE_NAME,
                    USER_ID, CHANNEL_ID, BRANCH_ID, CONTROL_TYPE, BLOCK_YN
                ) VALUES ('*', '*', 'SV', '*', '*', '*', '*', 'BUSINESS', 'Y')
                """);

        TcfProperties properties = new TcfProperties();
        repository = new JdbcTransactionControlRepository(jdbcTemplate, properties);
    }

    @Test
    void findsBusinessBlockRule() {
        TransactionControlHeader header = new TransactionControlHeader();
        header.setServiceId("SV.Sample.inquiry");
        header.setTransactionCode("SV-INQ-0001");
        header.setBusinessCode("SV");
        header.setServiceName("SV 샘플 조회");
        header.setUser("U123456");
        header.setChannelId("WEBTOP");
        header.setBranch("001234");
        header.setClientIp("10.10.10.10");

        Optional<TransactionControlRule> rule = repository.findRule(header);

        assertTrue(rule.isPresent());
        assertEquals("BUSINESS", rule.get().controlType());
        assertTrue(rule.get().isBlocking());
    }
}
