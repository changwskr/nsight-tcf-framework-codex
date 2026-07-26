package com.nh.nsight.tcf.core.support.control;

import com.nh.nsight.tcf.core.support.message.StandardHeader;
import java.util.Optional;

/**
 * Header {@code serviceName}을 업무 DB(예: OM_SERVICE_CATALOG)에서 보완할 때 구현한다.
 */
public interface TransactionControlServiceNameResolver {

    Optional<String> resolve(StandardHeader header);
}
