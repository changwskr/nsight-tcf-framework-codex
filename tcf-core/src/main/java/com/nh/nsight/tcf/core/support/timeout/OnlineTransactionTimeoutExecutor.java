package com.nh.nsight.tcf.core.support.timeout;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.error.SystemException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OnlineTransactionTimeoutExecutor {

    private final TcfProperties properties;
    private final ExecutorService executorService;

    public OnlineTransactionTimeoutExecutor(
            TcfProperties properties,
            @Autowired(required = false) @Qualifier("onlineTransactionTimeoutThreadPool") ExecutorService executorService) {
        this.properties = properties;
        this.executorService = executorService;
    }

    public Object execute(Supplier<Object> action) {
        if (!properties.isTimeoutPolicyEnabled() || executorService == null) {
            return action.get();
        }
        TimeoutPolicy policy = TimeoutContextHolder.get();
        int timeoutSec = policy != null && policy.getOnlineTimeoutSec() > 0
                ? policy.getOnlineTimeoutSec()
                : TcfServiceTimeoutConstants.DEFAULT_ONLINE_TIMEOUT_SEC;

        TimeoutThreadContext.Snapshot contextSnapshot = TimeoutThreadContext.capture();
        Future<Object> future = executorService.submit(
                () -> TimeoutThreadContext.runWithSnapshot(contextSnapshot, action));
        try {
            return future.get(timeoutSec, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BusinessException(
                    ErrorCode.TIMEOUT_ONLINE,
                    "온라인 거래 처리 시간(" + timeoutSec + "초)을 초과했습니다.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "온라인 거래 처리가 중단되었습니다.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "온라인 거래 처리 중 오류가 발생했습니다.", cause);
        }
    }
}
