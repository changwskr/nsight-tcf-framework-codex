package nhnis.fw.commons.exception;

import java.util.Arrays;

import org.apache.logging.log4j.ThreadContext;

import nhnis.fw.commons.exception.NhBaseException.TYPE;

public class NhExceptionProvider {

    private static final String ERROR_CODE = "errCode";

    public static NhBaseException nhExceptionBuilder(Exception e, TYPE type) {
        NhBaseException nhException = (NhBaseException) e;
        ThreadContext.put(ERROR_CODE, nhException.getStdErrCode());

        StackTraceElement[] stack = e.getStackTrace();
        if (stack != null && stack.length > 0) {
            StackTraceElement element = stack[0];
            nhException.setErrMethodName(element.getMethodName());
            nhException.setErrClassName(element.getClassName());
            nhException.setErrFileName(element.getFileName());
            nhException.setPgmLineNo(element.getLineNumber());
        }

        if (nhException.getErrMsgType() == null) {
            nhException.setErrMsgType(type);
        }
        return nhException;
    }

    public static NhBaseException exceptionBuilder(Exception e) {
        String errorCode = "FW9999";

        StackTraceElement[] stack = e.getStackTrace();
        StackTraceElement target = Arrays.stream(stack)
                .filter(el -> el.getClassName() != null && el.getClassName().startsWith("nhnis"))
                .findFirst()
                .orElse(stack.length > 0 ? stack[0] : null);

        NhBaseException exception = new NhBaseException(errorCode);
        ThreadContext.put(ERROR_CODE, errorCode);
        exception.setErrMsgType(TYPE.RUNTIME);

        if (target != null) {
            exception.setErrMethodName(target.getMethodName());
            exception.setErrClassName(target.getClassName());
            exception.setErrFileName(target.getFileName());
            exception.setPgmLineNo(target.getLineNumber());
        }

        // 원인 메시지를 stdErrMsgContents 에 포함
        String causeMessage = extractCauseMessage(e);
        if (causeMessage != null) {
            exception.setAddMsgContents(causeMessage);
        }
        return exception;
    }

    /**
     * <PRE>
     * 원인 예외 메시지를 재귀적으로 추출
     * 최대 3 단계까지 확인하여 가장 구체적인 메시지 반환
     * </PRE>
     */
    private static String extractCauseMessage(Exception e) {
        int depth = 0;
        String lastMessage = null;
        Throwable cause = e;

        while (cause != null && depth < 3) {
            String message = cause.getMessage();
            if (message != null && !message.isEmpty()) {
                lastMessage = message;
            }
            cause = cause.getCause();
            depth++;
        }

        // 마지막에 찾은 메시지 또는 예외 클래스명 포함
        if (lastMessage != null) {
            return lastMessage;
        }
        // 메시지 없으면 예외 클래스명 반환
        return e.getClass().getSimpleName();
    }
}
