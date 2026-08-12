package com.ims.superspring.dto.engine.base;

import org.w3c.dom.Node;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.exception.MarshalException;
import com.ims.superspring.dto.engine.exception.UnmarshalException;

/**
 * IMS SuperSpring JsonMessage 스텁.
 * 사내 SuperSpring 의존성이 있으면 이 클래스를 제거하고 원본을 사용한다.
 */
public abstract class JsonMessage {

    protected String encoding = "UTF-8";

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public abstract byte[] marshal(DataObject obj) throws MarshalException;

    public abstract void marshal(DataObject obj, Node node) throws MarshalException;

    public abstract DataObject unmarshal(Node node) throws UnmarshalException;

    public abstract int unmarshal(byte[] bytes, int offset, DataObject dto) throws Exception;
}
