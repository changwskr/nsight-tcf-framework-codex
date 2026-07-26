package com.nh.nsight.tcf.core.support.timeout;

import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import java.sql.SQLTimeoutException;
import java.util.Optional;

/** Timeout 계열 예외를 TCF 표준 오류코드로 변환한다. */
public final class TimeoutExceptionResolver {

    private static final String TX_TIMED_OUT =
            "org.springframework.transaction.TransactionTimedOutException";
    private static final String QUERY_TIMED_OUT =
            "org.springframework.dao.QueryTimeoutException";

    private TimeoutExceptionResolver() {}

    public static Optional<BusinessException> toBusinessException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return Optional.of(businessException);
            }
            if (current instanceof SQLTimeoutException) {
                return Optional.of(dbQueryTimeout(current));
            }
            String className = current.getClass().getName();
            if (TX_TIMED_OUT.equals(className) || isTransactionTimedOut(current)) {
                return Optional.of(new BusinessException(
                        ErrorCode.TIMEOUT_TRANSACTION, "트랜잭션 처리 시간을 초과했습니다.", current));
            }
            if (QUERY_TIMED_OUT.equals(className)) {
                return Optional.of(dbQueryTimeout(current));
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private static BusinessException dbQueryTimeout(Throwable cause) {
        return new BusinessException(ErrorCode.TIMEOUT_DB_QUERY, "DB 쿼리 실행 시간을 초과했습니다.", cause);
    }

    private static boolean isTransactionTimedOut(Throwable throwable) {
        for (Class<?> type = throwable.getClass(); type != null; type = type.getSuperclass()) {
            if ("TransactionTimedOutException".equals(type.getSimpleName())) {
                return true;
            }
        }
        return false;
    }
}
