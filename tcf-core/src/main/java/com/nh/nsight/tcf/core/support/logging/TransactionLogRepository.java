package com.nh.nsight.tcf.core.support.logging;

/**
 * 트랜잭션 로그 DB 적재 SPI. JdbcTemplate 등 구현체는 tcf-web에서 등록한다.
 */
public interface TransactionLogRepository {

    void save(TransactionLogRecord record);
}
