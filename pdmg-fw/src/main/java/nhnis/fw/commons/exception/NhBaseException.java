package nhnis.fw.commons.exception;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NhBaseException extends Exception {

    private static final long serialVersionUID = 1L;

    private String stdErrCode;
    private String stdErrMsgContents;
    private String addMsgContents;
    private String serviceUri;
    private String errFileName;
    private String errClassName;
    private String errMethodName;
    private int pgmLineNo;
    private TYPE errMsgType;
    private String[] messageValue;
    private String errorPageUrl;

    public enum TYPE {
        RUNTIME, COMMON, BIZ, AUTH, SERVICE;
    }

    public NhBaseException(TYPE type) {
        this.errMsgType = type;
    }

    public NhBaseException(String _stdErrCode, Throwable _e) {
        super(_e);
        setStdErrCode(_stdErrCode);
        getErrClassInfo(_e);
    }

    public NhBaseException(String _stdErrCode) {
        setStdErrCode(_stdErrCode);
    }

    public NhBaseException(String _stdErrCode, String[] _messageValue, Throwable _e) {
        super(_e);
        setStdErrCode(_stdErrCode);
        setMessageValue(_messageValue);
        getErrClassInfo(_e);
    }

    public NhBaseException(String _stdErrCode, String[] _messageValue) {
        setStdErrCode(_stdErrCode);
        setMessageValue(_messageValue);
    }

    public NhBaseException(String _stdErrCode, TYPE type) {
        setStdErrCode(_stdErrCode);
        setErrMsgType(type);
    }

    public NhBaseException(String _stdErrCode, String _stdErrContent, Throwable _e) {
        super(_e);
        setStdErrCode(_stdErrCode);
        setStdErrMsgContents(_stdErrContent);
        getErrClassInfo(_e);
    }

    public NhBaseException(String _stdErrCode, String[] messageValue, Throwable _e, TYPE type) {
        super(_e);
        setStdErrCode(_stdErrCode);
        setMessageValue(messageValue);
        setErrMsgType(type);
        getErrClassInfo(_e);
    }

    private void getErrClassInfo(Throwable e) {
        if (e != null) {
            NhBaseException nbe = (NhBaseException) e;
            this.errMsgType = nbe.getErrMsgType();
            this.errFileName = nbe.getErrFileName();
            this.addMsgContents = nbe.getAddMsgContents();
            this.errMethodName = nbe.getErrMethodName();
            this.pgmLineNo = nbe.getPgmLineNo();
            this.errClassName = nbe.getErrClassName();
            this.serviceUri = nbe.getServiceUri();
        }
    }

    public TYPE getErrMsgType() {
        return this.errMsgType;
    }

    public void setErrMsgType(TYPE errMsgType) {
        this.errMsgType = errMsgType;
    }

    public String getStdErrCode() {
        return this.stdErrCode;
    }

    public void setStdErrCode(String stdErrCode) {
        this.stdErrCode = stdErrCode;
    }

    public String getStdErrMsgContents() {
        return this.stdErrMsgContents;
    }

    public void setStdErrMsgContents(String stdErrMsgContents) {
        this.stdErrMsgContents = stdErrMsgContents;
    }

    public String getAddMsgContents() {
        return this.addMsgContents;
    }

    public void setAddMsgContents(String addMsgContents) {
        this.addMsgContents = addMsgContents;
    }

    public String getServiceUri() {
        return this.serviceUri;
    }

    public void setServiceUri(String serviceUri) {
        this.serviceUri = serviceUri;
    }

    public String getErrClassName() {
        return this.errClassName;
    }

    public void setErrClassName(String errClassName) {
        this.errClassName = errClassName;
    }

    public String getErrFileName() {
        return this.errFileName;
    }

    public void setErrFileName(String errFileName) {
        this.errFileName = errFileName;
    }

    public String getErrMethodName() {
        return this.errMethodName;
    }

    public void setErrMethodName(String errMethodName) {
        this.errMethodName = errMethodName;
    }

    public int getPgmLineNo() {
        return this.pgmLineNo;
    }

    public void setPgmLineNo(int pgmLineNo) {
        this.pgmLineNo = pgmLineNo;
    }

    public String[] getMessageValue() {
        return this.messageValue;
    }

    public void setMessageValue(String[] messageValue) {
        this.messageValue = messageValue;
    }

    public String getErrorPageUrl() {
        return this.errorPageUrl;
    }

    public void setErrorPageUrl(String errorPageUrl) {
        this.errorPageUrl = errorPageUrl;
    }

    @Override
    public String toString() {
        return "NhBaseException [stdErrCode=" + stdErrCode + ", stdErrMsgContents="
                + stdErrMsgContents
                + ", addMsgContents=" + addMsgContents + ", serviceUri="
                + serviceUri + ", errFileName=" + errFileName
                + ", errClassName=" + errClassName + ", errMethodName="
                + errMethodName + ", pgmLineNo=" + pgmLineNo
                + ", errMsgType=" + errMsgType + ", messageValue="
                + Arrays.toString(messageValue) + ", errorPageUrl="
                + errorPageUrl + "]";
    }
}
