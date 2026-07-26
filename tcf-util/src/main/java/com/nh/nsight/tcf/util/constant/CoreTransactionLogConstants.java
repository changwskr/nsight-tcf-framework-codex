package com.nh.nsight.tcf.util.constant;

import com.nh.nsight.tcf.util.meta.CopiedFrom;
import com.nh.nsight.tcf.util.meta.CopiedUtilityFlag;
import com.nh.nsight.tcf.util.meta.UtilCategory;

/**
 * tcf-core {@code TcfTransactionLogConstants} 복사본.
 */
@CopiedFrom(module = "tcf-core", sourceClass = "TcfTransactionLogConstants", category = UtilCategory.CONSTANT)
public final class CoreTransactionLogConstants implements CopiedUtilityFlag {

    public static final String COPIED_FROM_MODULE = "tcf-core";
    public static final String COPIED_FROM_CLASS = "TcfTransactionLogConstants";

    public static final String TABLE_NAME = "TCF_TX_LOG";

    public static final String DEFAULT_DATASOURCE_URL =
            "jdbc:h2:file:./data/nsight-txlog/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false";

    public static final String DEFAULT_DATASOURCE_URL_TEMPLATE =
            "jdbc:h2:file:${nsight.txlog.path:./data/nsight-txlog}/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false";

    private CoreTransactionLogConstants() {
    }
}
