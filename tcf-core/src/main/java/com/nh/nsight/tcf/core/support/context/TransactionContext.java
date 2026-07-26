package com.nh.nsight.tcf.core.support.context;

import com.nh.nsight.tcf.core.support.message.StandardHeader;
import java.util.HashMap;
import java.util.Map;

public class TransactionContext {
    private final StandardHeader header;
    private final StandardHeader clientHeader;
    private final long startTimeMillis;
    private final Map<String, Object> attributes = new HashMap<>();

    public TransactionContext(StandardHeader header) {
        this(header, StandardHeader.copyOf(header));
    }

    public TransactionContext(StandardHeader header, StandardHeader clientHeader) {
        this.header = header;
        this.clientHeader = clientHeader != null ? clientHeader : StandardHeader.copyOf(header);
        this.startTimeMillis = System.currentTimeMillis();
    }

    /** STF·Dispatcher 등 내부 처리용 Header (normalize·보완 반영). */
    public StandardHeader getHeader() { return header; }

    /** 클라이언트 응답 echo용 Header (원본 유지). */
    public StandardHeader getClientHeader() { return clientHeader; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long elapsedMillis() { return System.currentTimeMillis() - startTimeMillis; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void put(String key, Object value) { attributes.put(key, value); }
    public Object get(String key) { return attributes.get(key); }
}
