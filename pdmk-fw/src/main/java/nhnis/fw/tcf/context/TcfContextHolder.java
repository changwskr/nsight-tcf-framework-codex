package nhnis.fw.tcf.context;

/**
 * 진행 중인 거래 컨텍스트. Service 계층이 guid·userId를 파라미터로 넘겨받지 않고도 읽을 수 있게 한다.
 *
 * <p>심은 쪽이 반드시 {@link #clear()}로 지운다. 톰캣이 워커 스레드를 재사용하므로
 * 남겨두면 다음 거래가 이전 컨텍스트를 보게 된다.
 */
public final class TcfContextHolder {

    private static final ThreadLocal<TcfContext> HOLDER = new ThreadLocal<>();

    private TcfContextHolder() {
    }

    public static void set(TcfContext context) {
        HOLDER.set(context);
    }

    public static TcfContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
