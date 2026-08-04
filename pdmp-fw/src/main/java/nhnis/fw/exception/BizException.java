package nhnis.fw.exception;

public class BizException extends RuntimeException {

    private final String code;
    private final transient Object[] args;

    public BizException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args == null ? new Object[0] : args.clone();
    }
}
