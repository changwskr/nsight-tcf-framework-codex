package com.nh.nsight.tcf.core.support.logging;

/**
 * 프레임워크 공통 트랜잭션 로그 테이블 정의.
 * tcf-om 조회와 TCF STF/ETF 적재가 동일 테이블을 사용한다.
 */
public final class TcfTransactionLogConstants {

    public static final String TABLE_NAME = "TCF_TX_LOG";

    public static final String DEFAULT_DATASOURCE_URL =
            "jdbc:h2:file:./data/nsight-txlog/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false";

    /** {@link org.springframework.core.env.Environment#resolveRequiredPlaceholders(String)} 용 템플릿 */
    public static final String DEFAULT_DATASOURCE_URL_TEMPLATE =
            "jdbc:h2:file:${nsight.txlog.path:./data/nsight-txlog}/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false";

    private TcfTransactionLogConstants() {
    }
}
