package nhnis.fw.tcf.context;

import nhnis.fw.tcf.TcfTransaction;
import nhnis.fw.tcf.dto.StandardHeaderDto;

/**
 * 거래 1건의 처리 상태. STF가 만들고 ETF가 소비한다.
 *
 * <p>{@code header}는 정규화·보완을 거친 처리용 Header이고,
 * {@code clientHeader}는 응답에 그대로 되돌려줄 클라이언트 원본이다.
 */
public class TcfContext {

    private final TcfTransaction transaction;
    private final StandardHeaderDto header;
    private final StandardHeaderDto clientHeader;
    private final long startedAtNanos;

    public TcfContext(TcfTransaction transaction, StandardHeaderDto header, StandardHeaderDto clientHeader) {
        this.transaction = transaction;
        this.header = header;
        this.clientHeader = clientHeader;
        this.startedAtNanos = System.nanoTime();
    }

    public TcfTransaction getTransaction() {
        return transaction;
    }

    public StandardHeaderDto getHeader() {
        return header;
    }

    public StandardHeaderDto getClientHeader() {
        return clientHeader;
    }

    /** 업무 처리에 걸린 시간. 필터가 재는 총 응답시간과 달리 직렬화는 빠져 있다. */
    public long elapsedMs() {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    public String serviceId() {
        return header == null ? "UNKNOWN" : header.safeServiceId();
    }
}
