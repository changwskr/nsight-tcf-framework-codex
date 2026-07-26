package com.nh.nsight.tcf.core.support.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class TimeoutPolicyResolverTest {

    private TimeoutPolicyResolver resolver;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<TimeoutPolicyRepository> provider = mock(ObjectProvider.class);
        TimeoutPolicyRepository repository = mock(TimeoutPolicyRepository.class);
        when(provider.getIfAvailable()).thenReturn(repository);
        TimeoutPolicy custom = new TimeoutPolicy();
        custom.setServiceId("SV.Sample.inquiry");
        custom.setTransactionCode("SV-INQ-0001");
        custom.setBusinessCode("SV");
        custom.setOnlineTimeoutSec(7);
        custom.setTxTimeoutSec(7);
        custom.setTimeoutAction(TcfServiceTimeoutConstants.TIMEOUT_ACTION_FAIL);
        when(repository.findPolicy("SV.Sample.inquiry", "SV-INQ-0001", "SV")).thenReturn(Optional.of(custom));
        resolver = new TimeoutPolicyResolver(provider);
    }

    @AfterEach
    void tearDown() {
        TimeoutContextHolder.clear();
    }

    @Test
    void returnsRepositoryPolicyWhenPresent() {
        StandardHeader header = new StandardHeader();
        header.setServiceId("SV.Sample.inquiry");
        header.setTransactionCode("SV-INQ-0001");
        header.setBusinessCode("SV");
        TimeoutPolicy policy = resolver.resolve(header);
        assertEquals(7, policy.getOnlineTimeoutSec());
        assertEquals(TcfServiceTimeoutConstants.TIMEOUT_ACTION_FAIL, policy.getTimeoutAction());
    }

    @Test
    void returnsDefaultWhenMissing() {
        StandardHeader header = new StandardHeader();
        header.setServiceId("OM.HealthCheck.inquiry");
        header.setTransactionCode("OM-HLT-0001");
        header.setBusinessCode("OM");
        TimeoutPolicy policy = resolver.resolve(header);
        assertEquals(TcfServiceTimeoutConstants.DEFAULT_ONLINE_TIMEOUT_SEC, policy.getOnlineTimeoutSec());
    }
}

class TimeoutPolicyServiceTest {

    @AfterEach
    void tearDown() {
        TimeoutContextHolder.clear();
    }

    @Test
    void bindsPolicyToContextAndHolder() {
        TcfProperties properties = new TcfProperties();
        properties.setTimeoutPolicyEnabled(true);
        TimeoutPolicyResolver resolver = mock(TimeoutPolicyResolver.class);
        TimeoutPolicy policy = new TimeoutPolicy();
        policy.setServiceId("SV.Sample.inquiry");
        policy.setTxTimeoutSec(8);
        when(resolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(policy);

        TimeoutPolicyService service = new TimeoutPolicyService(properties, resolver);
        StandardHeader header = new StandardHeader();
        header.setServiceId("SV.Sample.inquiry");
        TransactionContext context = new TransactionContext(header);

        TimeoutPolicy resolved = service.resolveAndApply(header, context);
        assertEquals(8, resolved.getTxTimeoutSec());
        assertNotNull(TimeoutContextHolder.get());
        assertEquals(policy, context.get(TcfServiceTimeoutConstants.CONTEXT_ATTR));
    }
}
