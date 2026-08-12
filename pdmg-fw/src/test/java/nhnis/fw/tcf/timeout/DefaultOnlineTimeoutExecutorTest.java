package nhnis.fw.tcf.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class DefaultOnlineTimeoutExecutorTest {

    private ThreadPoolTaskExecutor taskExecutor;
    private OnlineTimeoutProperties properties;
    private RecordingTransactionManager transactionManager;
    private DefaultOnlineTimeoutExecutor executor;

    @BeforeEach
    void setUp() {
        properties = new OnlineTimeoutProperties();
        properties.setEnabled(true);
        properties.setMilliseconds(200);
        properties.setPoolSize(2);
        properties.setQueueCapacity(1);

        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("pdmg-online-test-");
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(2);
        taskExecutor.setQueueCapacity(1);
        taskExecutor.initialize();

        transactionManager = new RecordingTransactionManager();
        executor = new DefaultOnlineTimeoutExecutor(properties, taskExecutor, transactionManager);
    }

    @AfterEach
    void tearDown() {
        taskExecutor.shutdown();
    }

    @Test
    void returnsResultWithinTimeout() throws Exception {
        String result = executor.execute(() -> "ok");
        assertThat(result).isEqualTo("ok");
        assertThat(transactionManager.commitCount.get()).isEqualTo(1);
        assertThat(transactionManager.rollbackCount.get()).isZero();
    }

    @Test
    void throwsTimeoutWhenCallableExceedsLimit() {
        assertThatThrownBy(() -> executor.execute(() -> {
            Thread.sleep(500);
            return "late";
        })).isInstanceOf(OnlineTimeoutException.class);

        awaitWorkerQuiet();
        assertThat(transactionManager.rollbackCount.get() + transactionManager.commitCount.get())
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void propagatesBusinessRuntimeExceptionAndRollsBack() {
        assertThatThrownBy(() -> executor.execute(() -> {
            throw new IllegalStateException("biz-fail");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("biz-fail");

        assertThat(transactionManager.rollbackCount.get()).isEqualTo(1);
    }

    @Test
    void disabledPathRunsOnCallerThread() throws Exception {
        SyncOnlineTimeoutExecutor sync = new SyncOnlineTimeoutExecutor();
        String name = Thread.currentThread().getName();
        String seen = sync.execute(() -> Thread.currentThread().getName());
        assertThat(seen).isEqualTo(name);
    }

    @Test
    void overloadWhenPoolAndQueueFull() throws Exception {
        properties.setPoolSize(1);
        properties.setQueueCapacity(0);
        taskExecutor.shutdown();
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("pdmg-online-overload-");
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.setQueueCapacity(0);
        taskExecutor.initialize();
        executor = new DefaultOnlineTimeoutExecutor(properties, taskExecutor, transactionManager);

        AtomicBoolean hold = new AtomicBoolean(true);
        Thread blocker = new Thread(() -> {
            try {
                executor.execute(() -> {
                    while (hold.get()) {
                        Thread.sleep(20);
                    }
                    return null;
                });
            } catch (Exception ignored) {
                // timeout or interrupt after test
            }
        });
        blocker.start();
        Thread.sleep(50);

        assertThatThrownBy(() -> executor.execute(() -> "x"))
                .isInstanceOf(OnlineOverloadException.class);

        hold.set(false);
        blocker.join(1000);
    }

    @Test
    void lateWorkerMarksRollbackOnlyOnDeadline() throws Exception {
        AtomicBoolean entered = new AtomicBoolean();
        AtomicBoolean finishedWithoutCancel = new AtomicBoolean();

        Thread caller = new Thread(() -> {
            try {
                executor.execute(() -> {
                    entered.set(true);
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    finishedWithoutCancel.set(!Thread.currentThread().isInterrupted());
                    return "done";
                });
            } catch (OnlineTimeoutException expected) {
                // request thread timeout
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        caller.start();
        caller.join(2000);
        awaitWorkerQuiet();

        assertThat(entered).isTrue();
        assertThat(transactionManager.rollbackOnlyObserved.get() || transactionManager.rollbackCount.get() > 0)
                .isTrue();
    }

    private void awaitWorkerQuiet() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class RecordingTransactionManager implements PlatformTransactionManager {
        private final AtomicInteger commitCount = new AtomicInteger();
        private final AtomicInteger rollbackCount = new AtomicInteger();
        private final AtomicBoolean rollbackOnlyObserved = new AtomicBoolean();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus() {
                @Override
                public void setRollbackOnly() {
                    super.setRollbackOnly();
                    rollbackOnlyObserved.set(true);
                }
            };
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            if (status.isRollbackOnly()) {
                rollbackCount.incrementAndGet();
                return;
            }
            commitCount.incrementAndGet();
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
            rollbackCount.incrementAndGet();
        }
    }
}
