package com.nh.nsight.tcf.core.support.timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class OnlineTransactionTimeoutExecutorTest {

    private ExecutorService executorService;

    @AfterEach
    void tearDown() {
        TimeoutContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    void throwsOnlineTimeoutBusinessException() {
        TcfProperties properties = new TcfProperties();
        properties.setTimeoutPolicyEnabled(true);
        executorService = Executors.newSingleThreadExecutor();
        OnlineTransactionTimeoutExecutor executor =
                new OnlineTransactionTimeoutExecutor(properties, executorService);

        TimeoutPolicy policy = new TimeoutPolicy();
        policy.setOnlineTimeoutSec(1);
        TimeoutContextHolder.set(policy);

        BusinessException ex = assertThrows(BusinessException.class, () -> executor.execute(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
            return "done";
        }));

        assertEquals(ErrorCode.TIMEOUT_ONLINE, ex.getErrorCode());
    }

    @Test
    void propagatesHttpRequestContextToWorkerThread() {
        TcfProperties properties = new TcfProperties();
        properties.setTimeoutPolicyEnabled(true);
        executorService = Executors.newSingleThreadExecutor();
        OnlineTransactionTimeoutExecutor executor =
                new OnlineTransactionTimeoutExecutor(properties, executorService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/om/tcf");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Object result = executor.execute(() -> {
            HttpServletRequest workerRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                    .getRequest();
            return workerRequest.getRequestURI();
        });

        assertEquals("/om/tcf", result);
        assertNotNull(RequestContextHolder.getRequestAttributes());
    }
}
