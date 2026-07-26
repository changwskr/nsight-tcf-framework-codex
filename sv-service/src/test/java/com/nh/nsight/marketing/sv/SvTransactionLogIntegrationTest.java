package com.nh.nsight.marketing.sv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nh.nsight.tcf.core.support.logging.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@SpringBootTest
@AutoConfigureMockMvc
class SvTransactionLogIntegrationTest {

    @Autowired
    private ObjectProvider<TransactionLogRepository> repositoryProvider;

    @Autowired
    @Qualifier("transactionLogJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void transactionLogRepositoryIsRegistered() {
        assertThat(repositoryProvider.getIfAvailable()).isNotNull();
    }

    @Test
    void transactionLogTableExistsAfterStartup() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM TCF_TX_LOG", Integer.class);
        assertThat(count).isNotNull();
    }

    @Test
    void onlineCallPersistsTransactionLog() throws Exception {
        String requestJson = StreamUtils.copyToString(
                new ClassPathResource("sv-sample-inquiry.json").getInputStream(),
                StandardCharsets.UTF_8);

        int before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TCF_TX_LOG", Integer.class);

        mockMvc.perform(post("/online")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isOk());

        int after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TCF_TX_LOG", Integer.class);
        assertThat(after).isGreaterThan(before);
    }
}
