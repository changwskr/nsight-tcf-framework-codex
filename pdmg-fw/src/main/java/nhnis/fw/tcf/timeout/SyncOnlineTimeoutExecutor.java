package nhnis.fw.tcf.timeout;

import java.util.concurrent.Callable;

/**
 * 타임아웃 비활성 시 현재 Thread에서 그대로 실행한다.
 */
public class SyncOnlineTimeoutExecutor implements OnlineTimeoutExecutor {

    @Override
    public <T> T execute(Callable<T> action) throws Exception {
        return action.call();
    }
}
