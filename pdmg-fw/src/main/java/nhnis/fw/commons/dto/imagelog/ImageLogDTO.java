package nhnis.fw.commons.dto.imagelog;

import java.util.Collections;
import java.util.Map;

import com.ims.superspring.dto.DataObject;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

@jakarta.annotation.Generated(
        value = "com.imssoft.sts4.codegen.dto.DtoGenerator",
        date = "26. 7. 28. 오후 4:34",
        comments = "ImageLogDTO"
)
public class ImageLogDTO extends DataObject {
    private static final long serialVersionUID = 1L;

    private String guid = null;

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        if (guid == null) {
            this.guid = null;
        } else {
            this.guid = guid;
        }
    }

    private String serviceId = null;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        if (serviceId == null) {
            this.serviceId = null;
        } else {
            this.serviceId = serviceId;
        }
    }

    private String screenId = null;

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        if (screenId == null) {
            this.screenId = null;
        } else {
            this.screenId = screenId;
        }
    }

    private String optrEno = null;

    public String getOptrEno() {
        return optrEno;
    }

    public void setOptrEno(String optrEno) {
        if (optrEno == null) {
            this.optrEno = null;
        } else {
            this.optrEno = optrEno;
        }
    }

    private String clientIp = null;

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        if (clientIp == null) {
            this.clientIp = null;
        } else {
            this.clientIp = clientIp;
        }
    }

    private String requestTime = null;

    public String getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        if (requestTime == null) {
            this.requestTime = null;
        } else {
            this.requestTime = requestTime;
        }
    }

    private String responseTime = null;

    public String getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(String responseTime) {
        if (responseTime == null) {
            this.responseTime = null;
        } else {
            this.responseTime = responseTime;
        }
    }

    private String exceptionType = null;

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        if (exceptionType == null) {
            this.exceptionType = null;
        } else {
            this.exceptionType = exceptionType;
        }
    }

    private String exceptionCode = null;

    public String getExceptionCode() {
        return exceptionCode;
    }

    public void setExceptionCode(String exceptionCode) {
        if (exceptionCode == null) {
            this.exceptionCode = null;
        } else {
            this.exceptionCode = exceptionCode;
        }
    }

    private String exceptionMsg = null;

    public String getExceptionMsg() {
        return exceptionMsg;
    }

    public void setExceptionMsg(String exceptionMsg) {
        if (exceptionMsg == null) {
            this.exceptionMsg = null;
        } else {
            this.exceptionMsg = exceptionMsg;
        }
    }

    private String requestMsg = null;

    public String getRequestMsg() {
        return requestMsg;
    }

    public void setRequestMsg(String requestMsg) {
        this.requestMsg = requestMsg;
    }

    private String responseMsg = null;

    public String getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(String responseMsg) {
        this.responseMsg = responseMsg;
    }

    public Object clone() {
        ImageLogDTO copyObj = new ImageLogDTO();
        copyObj.clone(this);
        return copyObj;
    }

    public void clone(DataObject _imageLogDTO) {
        if (this == _imageLogDTO)
            return;

        ImageLogDTO __imageLogDTO = (ImageLogDTO) _imageLogDTO;
        this.setGuid(__imageLogDTO.getGuid());
        this.setServiceId(__imageLogDTO.getServiceId());
        this.setScreenId(__imageLogDTO.getScreenId());
        this.setOptrEno(__imageLogDTO.getOptrEno());
        this.setClientIp(__imageLogDTO.getClientIp());
        this.setRequestTime(__imageLogDTO.getRequestTime());
        this.setResponseTime(__imageLogDTO.getResponseTime());
        this.setExceptionType(__imageLogDTO.getExceptionType());
        this.setExceptionCode(__imageLogDTO.getExceptionCode());
        this.setExceptionMsg(__imageLogDTO.getExceptionMsg());
        this.setRequestMsg(__imageLogDTO.getRequestMsg());
        this.setResponseMsg(__imageLogDTO.getResponseMsg());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("guid : ").append(guid).append("\n");
        buffer.append("serviceId : ").append(serviceId).append("\n");
        buffer.append("screenId : ").append(screenId).append("\n");
        buffer.append("optrEno : ").append(optrEno).append("\n");
        buffer.append("clientIp : ").append(clientIp).append("\n");
        buffer.append("requestTime : ").append(requestTime).append("\n");
        buffer.append("responseTime : ").append(responseTime).append("\n");
        buffer.append("exceptionType : ").append(exceptionType).append("\n");
        buffer.append("exceptionCode : ").append(exceptionCode).append("\n");
        buffer.append("exceptionMsg : ").append(exceptionMsg).append("\n");
        buffer.append("requestMsg : ").append(requestMsg).append("\n");
        buffer.append("responseMsg : ").append(responseMsg).append("\n");
        return buffer.toString();
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new java.util.LinkedHashMap<String, FieldProperty>(10);
        fieldPropertyMap.put("guid", FieldProperty.builder()
                .setPhysicalName("guid")
                .setLogicalName("거래고유ID")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("serviceId", FieldProperty.builder()
                .setPhysicalName("serviceId")
                .setLogicalName("서비스ID")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("screenId", FieldProperty.builder()
                .setPhysicalName("screenId")
                .setLogicalName("화면ID")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("optrEno", FieldProperty.builder()
                .setPhysicalName("optrEno")
                .setLogicalName("사용자ID")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("clientIp", FieldProperty.builder()
                .setPhysicalName("clientIp")
                .setLogicalName("클라이언트IP")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("requestTime", FieldProperty.builder()
                .setPhysicalName("requestTime")
                .setLogicalName("요청시간")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("responseTime", FieldProperty.builder()
                .setPhysicalName("responseTime")
                .setLogicalName("응답시간")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("exceptionType", FieldProperty.builder()
                .setPhysicalName("exceptionType")
                .setLogicalName("예외타입")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("exceptionCode", FieldProperty.builder()
                .setPhysicalName("exceptionCode")
                .setLogicalName("예외코드")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("exceptionMsg", FieldProperty.builder()
                .setPhysicalName("exceptionMsg")
                .setLogicalName("예외메시지")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("requestMsg", FieldProperty.builder()
                .setPhysicalName("requestMsg")
                .setLogicalName("요청전문")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("responseMsg", FieldProperty.builder()
                .setPhysicalName("responseMsg")
                .setLogicalName("응답전문")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
    }

    public Map<String, FieldProperty> getFieldPropertyMap() {
        return Collections.unmodifiableMap(fieldPropertyMap);
    }
}
