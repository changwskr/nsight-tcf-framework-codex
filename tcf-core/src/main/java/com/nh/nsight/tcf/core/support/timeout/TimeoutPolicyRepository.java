package com.nh.nsight.tcf.core.support.timeout;

import java.util.Optional;

public interface TimeoutPolicyRepository {
    Optional<TimeoutPolicy> findPolicy(String serviceId, String transactionCode, String businessCode);
}
