package nhnis.fw.commons.exception;

/**
 * <PRE>
 * 서비스 통신 예외
 * 서비스 간 통신 실패 시 발생
 * </PRE>
 */
public class ServiceCommunicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceCommunicationException(String message) {
        super(message);
    }

    public ServiceCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
