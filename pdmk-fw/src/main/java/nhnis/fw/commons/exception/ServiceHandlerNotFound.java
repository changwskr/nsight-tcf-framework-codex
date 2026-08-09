package nhnis.fw.commons.exception;

/**
 * <PRE>
 * 서비스 핸들러 찾을 수 없음 예외
 * </PRE>
 */
public class ServiceHandlerNotFound extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceHandlerNotFound(String message) {
        super(message);
    }

    public ServiceHandlerNotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceHandlerNotFound(Throwable cause) {
        super(cause);
    }
}
