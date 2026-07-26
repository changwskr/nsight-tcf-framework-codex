package com.nh.nsight.tcf.core.support.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import java.sql.SQLTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionTimedOutException;

class TimeoutExceptionResolverTest {

    @Test
    void mapsSqlTimeoutToDbQueryError() {
        BusinessException resolved = TimeoutExceptionResolver.toBusinessException(
                new SQLTimeoutException("query timed out")).orElseThrow();

        assertEquals(ErrorCode.TIMEOUT_DB_QUERY, resolved.getErrorCode());
    }

    @Test
    void mapsWrappedSqlTimeoutToDbQueryError() {
        BusinessException resolved = TimeoutExceptionResolver.toBusinessException(
                new RuntimeException("mapper failed", new SQLTimeoutException("timeout"))).orElseThrow();

        assertEquals(ErrorCode.TIMEOUT_DB_QUERY, resolved.getErrorCode());
    }

    @Test
    void mapsTransactionTimedOutException() {
        BusinessException resolved = TimeoutExceptionResolver.toBusinessException(
                new TransactionTimedOutException("tx timeout")).orElseThrow();

        assertEquals(ErrorCode.TIMEOUT_TRANSACTION, resolved.getErrorCode());
    }
}
