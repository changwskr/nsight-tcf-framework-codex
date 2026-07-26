package com.nh.nsight.tcf.web.application.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.timeout.TimeoutContextHolder;
import com.nh.nsight.tcf.core.support.timeout.TimeoutPolicy;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;

class PolicyDrivenTransactionAttributeSourceTest {

    interface SampleFacade {
        @Transactional(timeout = 30)
        void update();

        @Transactional(readOnly = true, timeout = 10)
        void inquiry();
    }

    @AfterEach
    void tearDown() {
        TimeoutContextHolder.clear();
    }

    @Test
    void keepsAnnotationTimeoutWhenPolicyDisabled() throws Exception {
        TcfProperties properties = new TcfProperties();
        properties.setTimeoutPolicyEnabled(false);
        PolicyDrivenTransactionAttributeSource source =
                new PolicyDrivenTransactionAttributeSource(properties);

        Method method = SampleFacade.class.getMethod("update");
        TimeoutPolicy policy = new TimeoutPolicy();
        policy.setTxTimeoutSec(120);
        TimeoutContextHolder.set(policy);

        TransactionAttribute attr = source.getTransactionAttribute(method, SampleFacade.class);

        assertNotNull(attr);
        assertEquals(30, attr.getTimeout());
    }

    @Test
    void overridesAnnotationTimeoutFromContextHolder() throws Exception {
        TcfProperties properties = new TcfProperties();
        properties.setTimeoutPolicyEnabled(true);
        PolicyDrivenTransactionAttributeSource source =
                new PolicyDrivenTransactionAttributeSource(properties);

        Method method = SampleFacade.class.getMethod("update");
        TimeoutPolicy policy = new TimeoutPolicy();
        policy.setTxTimeoutSec(120);
        TimeoutContextHolder.set(policy);

        TransactionAttribute attr = source.getTransactionAttribute(method, SampleFacade.class);

        assertNotNull(attr);
        assertEquals(120, attr.getTimeout());
    }

    @Test
    void preservesReadOnlyWhenOverridingTimeout() throws Exception {
        TcfProperties properties = new TcfProperties();
        properties.setTimeoutPolicyEnabled(true);
        PolicyDrivenTransactionAttributeSource source =
                new PolicyDrivenTransactionAttributeSource(properties);

        Method method = SampleFacade.class.getMethod("inquiry");
        TimeoutPolicy policy = new TimeoutPolicy();
        policy.setTxTimeoutSec(3);
        TimeoutContextHolder.set(policy);

        TransactionAttribute attr = source.getTransactionAttribute(method, SampleFacade.class);

        assertNotNull(attr);
        assertEquals(3, attr.getTimeout());
        assertEquals(true, attr.isReadOnly());
    }

    @Test
    void keepsAnnotationTimeoutWhenContextEmpty() throws Exception {
        TcfProperties properties = new TcfProperties();
        properties.setTimeoutPolicyEnabled(true);
        PolicyDrivenTransactionAttributeSource source =
                new PolicyDrivenTransactionAttributeSource(properties);

        Method method = SampleFacade.class.getMethod("update");

        TransactionAttribute attr = source.getTransactionAttribute(method, SampleFacade.class);

        assertNotNull(attr);
        assertEquals(30, attr.getTimeout());
    }
}
