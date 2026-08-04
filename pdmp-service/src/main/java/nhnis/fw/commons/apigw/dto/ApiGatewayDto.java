package nhnis.fw.commons.apigw.dto;

import com.ims.superspring.dto.DataObject;

import java.util.Collections;
import java.util.Map;
import com.ims.superspring.dto.engine.dto.record.common.FieldProperty;

@jakarta.annotation.Generated(
        value = "com.imssoft.sts4.codegen.dto.DtoGenerator",
        date = "26. 7. 21. 오전 11:15",
        comments = "ApiGatewayDto"
)
public class ApiGatewayDto extends DataObject {
    private static final long serialVersionUID = 1L;

    private String url = null;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null) {
            this.url = null;
        } else {
            this.url = url;
        }
    }

    private String contentType = null;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        if (contentType == null) {
            this.contentType = null;
        } else {
            this.contentType = contentType;
        }
    }

    private String interfaceId = null;

    public String getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(String interfaceId) {
        if (interfaceId == null) {
            this.interfaceId = null;
        } else {
            this.interfaceId = interfaceId;
        }
    }

    private String recvTrxName = null;

    public String getRecvTrxName() {
        return recvTrxName;
    }

    public void setRecvTrxName(String recvTrxName) {
        if (recvTrxName == null) {
            this.recvTrxName = null;
        } else {
            this.recvTrxName = recvTrxName;
        }
    }

    private String recvType = null;

    public String getRecvType() {
        return recvType;
    }

    public void setRecvType(String recvType) {
        if (recvType == null) {
            this.recvType = null;
        } else {
            this.recvType = recvType;
        }
    }

    private String replyType = null;

    public String getReplyType() {
        return replyType;
    }

    public void setReplyType(String replyType) {
        if (replyType == null) {
            this.replyType = null;
        } else {
            this.replyType = replyType;
        }
    }

    private String body = null;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        if (body == null) {
            this.body = null;
        } else {
            this.body = body;
        }
    }

    public Object clone() {
        ApiGatewayDto copyObj = new ApiGatewayDto();
        copyObj.clone(this);
        return copyObj;
    }

    public void clone(DataObject _apiGatewayDto) {
        if (this == _apiGatewayDto)
            return;

        ApiGatewayDto __apiGatewayDto = (ApiGatewayDto) _apiGatewayDto;
        this.setUrl(__apiGatewayDto.getUrl());
        this.setContentType(__apiGatewayDto.getContentType());
        this.setInterfaceId(__apiGatewayDto.getInterfaceId());
        this.setRecvTrxName(__apiGatewayDto.getRecvTrxName());
        this.setRecvType(__apiGatewayDto.getRecvType());
        this.setReplyType(__apiGatewayDto.getReplyType());
        this.setBody(__apiGatewayDto.getBody());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("url : ").append(url).append("\n");
        buffer.append("contentType : ").append(contentType).append("\n");
        buffer.append("interfaceId : ").append(interfaceId).append("\n");
        buffer.append("recvTrxName : ").append(recvTrxName).append("\n");
        buffer.append("recvType : ").append(recvType).append("\n");
        buffer.append("replyType : ").append(replyType).append("\n");
        buffer.append("body : ").append(body).append("\n");
        return buffer.toString();
    }

    private static final Map<String, FieldProperty> fieldPropertyMap;

    static {
        fieldPropertyMap = new java.util.LinkedHashMap<String, FieldProperty>(7);
        fieldPropertyMap.put("url", FieldProperty.builder()
                .setPhysicalName("url")
                .setLogicalName("url")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("contentType", FieldProperty.builder()
                .setPhysicalName("contentType")
                .setLogicalName("contentType")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("interfaceId", FieldProperty.builder()
                .setPhysicalName("interfaceId")
                .setLogicalName("interfaceId")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("recvTrxName", FieldProperty.builder()
                .setPhysicalName("recvTrxName")
                .setLogicalName("recvTrxName")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("recvType", FieldProperty.builder()
                .setPhysicalName("recvType")
                .setLogicalName("recvType")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("replyType", FieldProperty.builder()
                .setPhysicalName("replyType")
                .setLogicalName("replyType")
                .setType(FieldProperty.TYPE_OBJECT_STRING)
                .setDecimal(-1)
                .setIsNullable(true)
                .setIsEncrypt(false)
                .build());
        fieldPropertyMap.put("body", FieldProperty.builder()
                .setPhysicalName("body")
                .setLogicalName("body")
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
