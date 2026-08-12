package nhnis.mg.jw.a.support;

import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.fw.tcf.core.context.TransactionContext;
import org.springframework.util.StringUtils;

/**
 * 현재 거래(TransactionContext)의 채널/IP/사용자 정보를 조회한다.
 *
 * <p>Handler 가 진입 시 {@link #bind(TransactionContext)} 로 현재 스레드에 컨텍스트를 묶고,
 * 처리가 끝나면 반드시 {@link #clear()} 로 해제한다.
 * <pre>
 * try {
 *     JwtClientContext.bind(context);
 *     return facade.xxx(dtoBody);
 * } finally {
 *     JwtClientContext.clear();
 * }
 * </pre>
 * Service/Facade 는 인자 없는 {@link #clientIp()}/{@link #channelId()}/{@link #userId()} 를 사용한다.
 */
public final class JwtClientContext {

    private static final ThreadLocal<TransactionContext> CURRENT = new ThreadLocal<>();

    private JwtClientContext() {
    }

    public static void bind(TransactionContext context) {
        CURRENT.set(context);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static TransactionContext current() {
        return CURRENT.get();
    }

    public static String clientIp() {
        return clientIp(current());
    }

    public static String channelId() {
        return channelId(current());
    }

    public static String userId() {
        return userId(current());
    }

    public static String clientIp(TransactionContext context) {
        sys_comm sys = sys(context);
        if (sys == null) {
            return null;
        }
        return sys.getTr_trm_ipadr();
    }

    public static String channelId(TransactionContext context) {
        sys_comm sys = sys(context);
        if (sys == null || !StringUtils.hasText(sys.getScid())) {
            return "WEBTOP";
        }
        return sys.getScid();
    }

    public static String userId(TransactionContext context) {
        sys_comm sys = sys(context);
        if (sys == null || !StringUtils.hasText(sys.getOptr_eno())) {
            return null;
        }
        return sys.getOptr_eno();
    }

    private static sys_comm sys(TransactionContext context) {
        if (context == null) {
            return null;
        }
        hdr_nhnis header = context.getHeader();
        return header == null ? null : header.getSys_comm();
    }
}
