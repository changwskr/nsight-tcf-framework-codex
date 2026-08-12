package nhnis.fw.tcf.timeout;

import java.util.concurrent.Callable;

/**
 * 온라인 거래 실행을 공통 제한시간으로 감싼다.
 */
public interface OnlineTimeoutExecutor {

    <T> T execute(Callable<T> action) throws Exception;
}
