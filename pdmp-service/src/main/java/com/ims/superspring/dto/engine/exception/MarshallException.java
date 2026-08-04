package com.ims.superspring.dto.engine.exception;

/**
 * IMS SuperSpring MarshallException 스텁.
 * 사내 SuperSpring 의존성이 있으면 이 클래스를 제거하고 원본을 사용한다.
 */
public class MarshallException extends Exception {

    private static final long serialVersionUID = 1L;

    public MarshallException() {
        super();
    }

    public MarshallException(String message) {
        super(message);
    }

    public MarshallException(String message, Throwable cause) {
        super(message, cause);
    }

    public MarshallException(Throwable cause) {
        super(cause);
    }
}
