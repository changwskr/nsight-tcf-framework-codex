package com.nh.nsight.tcf.core.support.message;

import java.io.Serializable;

public class StandardRequest<T> implements Serializable {
    private StandardHeader header;
    private T body;

    public StandardRequest() {
    }

    public StandardRequest(StandardHeader header, T body) {
        this.header = header;
        this.body = body;
    }

    public StandardHeader getHeader() { return header; }
    public void setHeader(StandardHeader header) { this.header = header; }
    public T getBody() { return body; }
    public void setBody(T body) { this.body = body; }
}
