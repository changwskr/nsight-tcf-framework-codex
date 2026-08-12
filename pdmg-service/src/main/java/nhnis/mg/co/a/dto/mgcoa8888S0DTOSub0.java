package nhnis.mg.co.a.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

/**
 * 이미지로그 목록 Sub DTO (mgcoa8888S0).
 */
public class mgcoa8888S0DTOSub0 extends DataObject {

    private static final long serialVersionUID = 1L;

    private String guid;
    private String serviceId;
    private String screenId;
    private String optrEno;
    private String clientIp;
    private String requestTime;
    private String responseTime;
    private String exceptionType;
    private String exceptionCode;
    private String exceptionMsg;
    private String requestMsg;
    private String responseMsg;

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public String getOptrEno() {
        return optrEno;
    }

    public void setOptrEno(String optrEno) {
        this.optrEno = optrEno;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }

    public String getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(String responseTime) {
        this.responseTime = responseTime;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getExceptionCode() {
        return exceptionCode;
    }

    public void setExceptionCode(String exceptionCode) {
        this.exceptionCode = exceptionCode;
    }

    public String getExceptionMsg() {
        return exceptionMsg;
    }

    public void setExceptionMsg(String exceptionMsg) {
        this.exceptionMsg = exceptionMsg;
    }

    public String getRequestMsg() {
        return requestMsg;
    }

    public void setRequestMsg(String requestMsg) {
        this.requestMsg = requestMsg;
    }

    public String getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(String responseMsg) {
        this.responseMsg = responseMsg;
    }

    @Override
    public Object clone() {
        mgcoa8888S0DTOSub0 copy = new mgcoa8888S0DTOSub0();
        copy.clone(this);
        return copy;
    }

    public void clone(DataObject src) {
        if (this == src) {
            return;
        }
        mgcoa8888S0DTOSub0 in = (mgcoa8888S0DTOSub0) src;
        this.guid = in.guid;
        this.serviceId = in.serviceId;
        this.screenId = in.screenId;
        this.optrEno = in.optrEno;
        this.clientIp = in.clientIp;
        this.requestTime = in.requestTime;
        this.responseTime = in.responseTime;
        this.exceptionType = in.exceptionType;
        this.exceptionCode = in.exceptionCode;
        this.exceptionMsg = in.exceptionMsg;
        this.requestMsg = in.requestMsg;
        this.responseMsg = in.responseMsg;
    }

    @Override
    public String toString() {
        return "guid : " + guid
                + " serviceId : " + serviceId
                + " screenId : " + screenId
                + " optrEno : " + optrEno
                + " requestTime : " + requestTime
                + " exceptionType : " + exceptionType;
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new LinkedHashMap<>();
        putString("guid");
        putString("serviceId");
        putString("screenId");
        putString("optrEno");
        putString("clientIp");
        putString("requestTime");
        putString("responseTime");
        putString("exceptionType");
        putString("exceptionCode");
        putString("exceptionMsg");
        putString("requestMsg");
        putString("responseMsg");
    }

    private static void putString(String name) {
        fieldPropertyMap.put(name, FieldProperty.builder()
                .setPhysicalName(name).setLogicalName(name)
                .setType(FieldProperty.TYPE_OBJECT_STRING).setDecimal(-1)
                .setIsNullable(true).setIsEncrypt(false).build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}
