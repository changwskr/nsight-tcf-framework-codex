package com.nh.nsight.tcf.core.support.timeout;

import com.nh.nsight.tcf.core.support.message.StandardHeader;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TimeoutPolicyResolver {
    private final ObjectProvider<TimeoutPolicyRepository> repositoryProvider;

    public TimeoutPolicyResolver(ObjectProvider<TimeoutPolicyRepository> repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    public TimeoutPolicy resolve(StandardHeader header) {
        if (header == null) {
            return defaultPolicy(null, null, null, null);
        }
        String serviceId = trim(header.getServiceId());
        String transactionCode = trim(header.getTransactionCode());
        String businessCode = trim(header.getBusinessCode());

        TimeoutPolicyRepository repository = repositoryProvider.getIfAvailable();
        if (repository != null && StringUtils.hasText(serviceId)) {
            Optional<TimeoutPolicy> found = repository.findPolicy(serviceId, transactionCode, businessCode);
            if (found.isPresent()) {
                return found.get();
            }
        }
        return defaultPolicy(serviceId, transactionCode, businessCode, header.getServiceName());
    }

    private static TimeoutPolicy defaultPolicy(
            String serviceId, String transactionCode, String businessCode, String serviceName) {
        TimeoutPolicy policy = new TimeoutPolicy();
        policy.setServiceId(serviceId);
        policy.setTransactionCode(transactionCode);
        policy.setBusinessCode(businessCode);
        policy.setServiceName(serviceName);
        return policy;
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
