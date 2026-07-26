package com.nh.nsight.tcf.web.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nh.nsight.tcf.core.config.TcfProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TcfDataSourceUrlSupportTest {

    @Test
    void transactionControlReusesPrimaryWhenSeparateIsFalse() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url",
                "jdbc:h2:tcp://127.0.0.1:9092/./nsight_om;MODE=Oracle;DATABASE_TO_UPPER=false");
        environment.setProperty("nsight.tcf.transaction-log-datasource.separate", "false");
        environment.setProperty("nsight.tcf.transaction-control-datasource.url",
                "jdbc:h2:tcp://127.0.0.1:9092/./nsight_om;MODE=Oracle;DATABASE_TO_UPPER=false");

        TcfProperties properties = new TcfProperties();
        properties.getTransactionLogDatasource().setSeparate(false);

        assertTrue(TcfDataSourceUrlSupport.transactionControlReusesPrimary(properties, environment));
    }

    @Test
    void transactionControlUsesSeparatePoolWhenUrlsDiffer() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:h2:mem:primary;DB_CLOSE_DELAY=-1");
        environment.setProperty("nsight.tcf.transaction-control-datasource.url",
                "jdbc:h2:mem:control;DB_CLOSE_DELAY=-1");

        TcfProperties properties = new TcfProperties();
        properties.getTransactionLogDatasource().setSeparate(true);

        assertFalse(TcfDataSourceUrlSupport.transactionControlReusesPrimary(properties, environment));
    }
}
